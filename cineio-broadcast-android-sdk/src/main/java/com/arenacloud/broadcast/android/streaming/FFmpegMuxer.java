// inspired by: https://github.com/Kickflip/kickflip-android-sdk/blob/e35e0a5bb7161ccffebd564ec1a76a0e2c053fc8/sdk/src/main/java/io/kickflip/sdk/av/FFmpegMuxer.java
package com.arenacloud.broadcast.android.streaming;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;

import com.arenacloud.broadcast.ffmpegbridge.FFmpegBridge;

//TODO: Remove hard-coded track indexes
//      Remove 2 track assumption
public class FFmpegMuxer extends Muxer implements Runnable {
    private static final String TAG = "FFmpegMuxer";
    private static final boolean VERBOSE = false;        // Lots of logging

    private static final int INPUTQUEUE_ALLOCLENGTH_PERTRACK = 2*EncodingConfig.DEFAULT_HUMAN_FPS;

    // MuxerHandler message types
    private static final int MSG_WRITE_FRAME = 1;
    private static final int MSG_ADD_TRACK = 2;

    private final Object mReadyFence = new Object();    // Synchronize muxing thread readiness
    private final Object mEncoderReleasedSync = new Object();
    private final int mVideoTrackIndex = 0;
    private final int mAudioTrackIndex = 1;
    // Related to crafting ADTS headers
    private final int ADTS_LENGTH = 7;          // ADTS Header length (bytes)
    private final int profile = 2;              // AAC LC
    // Queue encoded buffers when muxing to stream
    ArrayList<ArrayDeque<ByteBuffer>> mMuxerInputQueue;
    private ArrayList<Integer> mMuxerInputQueueAllocSize;

    private boolean mReady;                             // Is muxing thread ready
    private boolean mRunning;                           // Is muxer thread running
    private FFmpegHandler mHandler;
    private boolean mEncoderReleased;                   // TODO: Account for both encoders
    private int freqIdx = 4;                    // 44.1KHz
    private int chanCfg = 1;                    // MPEG-4 Audio Channel Configuration. 1 Channel front-center
    private int mInPacketSize;                  // Pre ADTS Header
    private int mOutPacketSize;                 // Post ADTS Header
    private byte[] mCachedAudioPacket;
    // Related to extracting H264 SPS + PPS from MediaCodec
    private ByteBuffer mH264Keyframe;
    private int mH264MetaSize;                   // Size of SPS + PPS data
    private FFmpegBridge mFFmpeg;
    private boolean mStarted;
    private byte[] videoConfig;
    private byte[] audioConfig;


    public FFmpegMuxer() {
        mFFmpeg = new FFmpegBridge();
    }

    @Override
    public boolean prepare(EncodingConfig config) {
        super.prepare(config);
        getConfig().setMuxerState(EncodingConfig.MUXER_STATE.PREPARING);
        mReady = false;

        videoConfig = null;
        audioConfig = null;
        mH264Keyframe = null;
        mH264MetaSize = -1;
        mStarted = false;
        mEncoderReleased = false;
        int ret = mFFmpeg.init(getConfig().getAVOptions());

        if(ret<0)
        {
            mFFmpeg.releaseResource();

            if(mOnErrorListener!=null)
                mOnErrorListener.onError(this,OPEN_URL_FAIL,ret);

            return false;
        }

        if (formatRequiresADTS())
            mCachedAudioPacket = new byte[1024];

        if (formatRequiresBuffering()) {
            mMuxerInputQueue = new ArrayList<ArrayDeque<ByteBuffer>>();
            mMuxerInputQueueAllocSize = new ArrayList<Integer>();
            startMuxingThread();
        } else {
            getConfig().setMuxerState(EncodingConfig.MUXER_STATE.READY);
            mReady = true;
        }

        return true;
    }

    @Override
    public int addTrack(MediaFormat trackFormat) {
        // With FFmpeg, we want to write the encoder's
        // BUFFER_FLAG_CODEC_CONFIG buffer directly via writeSampleData
        // Whereas with MediaMuxer this call handles that.
        // TODO: Ensure addTrack isn't called more times than it should be...
        // TODO: Make an FFmpegWrapper API that sets mVideo/AudioTrackIndex instead of hard-code
        int trackIndex;
        if (trackFormat.getString(MediaFormat.KEY_MIME).compareTo("video/avc") == 0)
            trackIndex = mVideoTrackIndex;
        else
            trackIndex = mAudioTrackIndex;
        if (formatRequiresBuffering()) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_TRACK, trackFormat));
            synchronized (mMuxerInputQueue) {
                while (mMuxerInputQueue.size() < trackIndex + 1) {
                    mMuxerInputQueue.add(new ArrayDeque<ByteBuffer>());
                    mMuxerInputQueueAllocSize.add(0);
                }
            }
        } else {
            handleAddTrack(trackFormat);
        }
        return trackIndex;
    }

    public void handleAddTrack(MediaFormat trackFormat) {
        super.addTrack(trackFormat);
        mStarted = true;
    }

    @Override
    public void onEncoderReleased(int trackIndex) {
        // For now assume both tracks will be
        // released in close proximity
        synchronized (mEncoderReleasedSync) {
            mEncoderReleased = true;
        }
    }

    /**
     * Shutdown this Muxer
     * Must be called from Muxer thread
     */
    private void shutdown() {
        if (!mReady || !mStarted){
            return;
        }
        Log.i(TAG, "Shutting down");
        mFFmpeg.releaseResource();
        mStarted = false;
        release();
        if (formatRequiresBuffering()) {
            Looper.myLooper().quit();
            mHandler = null;
        }
        getConfig().setMuxerState(EncodingConfig.MUXER_STATE.SHUTDOWN);
    }

    private boolean isNeedWait = true;
    @Override
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {

        Log.i("encodedData.capacity", String.valueOf(encodedData.capacity()));

        synchronized (mReadyFence) {
            if (mReady) {
                ByteBuffer muxerInput = null;
                if (formatRequiresBuffering()) {
                    // Copy encodedData into another ByteBuffer, recycling if possible
                    Log.i("THE ENCODED DATA", encodedData.toString());
                    Log.i("THE TRACK INDEX", String.valueOf(trackIndex));

                    while(isNeedWait)
                    {
                        synchronized (mMuxerInputQueue) {
//                        muxerInput = mMuxerInputQueue.get(trackIndex).isEmpty() ?
//                                ByteBuffer.allocateDirect(encodedData.capacity()) : mMuxerInputQueue.get(trackIndex).remove();

                            if (mMuxerInputQueue.get(trackIndex).isEmpty()) {
                                Log.i("mMuxerInputQueue", "isEmpty");

                                int size = mMuxerInputQueueAllocSize.get(trackIndex);

                                if (size > INPUTQUEUE_ALLOCLENGTH_PERTRACK) {
                                    isNeedWait = true;
                                } else {
                                    muxerInput = ByteBuffer.allocateDirect(encodedData.capacity());
                                    size++;
                                    mMuxerInputQueueAllocSize.set(trackIndex, size);
                                    isNeedWait = false;
                                }

                            } else {
                                muxerInput = mMuxerInputQueue.get(trackIndex).remove();
                                Log.i("mMuxerInputQueue", "remove");
                                isNeedWait = false;
                            }

                            if (isNeedWait) {
                                //wait
                                try {
                                    mMuxerInputQueue.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    isNeedWait = true;

                    muxerInput.put(encodedData);
                    muxerInput.position(0);
                    encoder.releaseOutputBuffer(bufferIndex, false);
                    mHandler.sendMessage(mHandler.obtainMessage(MSG_WRITE_FRAME,
                            new WritePacketData(encoder, trackIndex, bufferIndex, muxerInput, bufferInfo)));
                } else {
                    handleWriteSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);
                }

            } else {
                Log.w(TAG, "Dropping frame because Muxer not ready!");
                releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);
                if (formatRequiresBuffering())
                    encoder.releaseOutputBuffer(bufferIndex, false);
            }
        }
    }

    private boolean haveSendWritePacketError = false;
    private boolean isMuxerInputQueueFull = false;
    public void handleWriteSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        super.writeSampleData(encoder, trackIndex, bufferIndex, encodedData, bufferInfo);

        if (((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
            if (VERBOSE) Log.i(TAG, "handling BUFFER_FLAG_CODEC_CONFIG for track " + trackIndex);
            if (trackIndex == mVideoTrackIndex) {
                // Capture H.264 SPS + PPS Data
                Log.d(TAG, "Capture SPS + PPS");

                captureH264MetaData(encodedData, bufferInfo);
                mFFmpeg.setVideoCodecExtraData(videoConfig, videoConfig.length);
            } else {
                captureAACMetaData(encodedData, bufferInfo);

                Log.d(TAG, "AUDIO CONFIG LENGTH: " + audioConfig.length);
                mFFmpeg.setAudioCodecExtraData(audioConfig, audioConfig.length);
            }
            if (videoConfig != null && audioConfig != null) {
                getConfig().setMuxerState(EncodingConfig.MUXER_STATE.CONNECTING);
                mFFmpeg.writeHeader();
            }
            releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);
            return;
        }

        if (trackIndex == mAudioTrackIndex && formatRequiresADTS()) {
            addAdtsToByteBuffer(encodedData, bufferInfo);
        }

        // adjust the ByteBuffer values to match BufferInfo (not needed?)
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + bufferInfo.size);

        bufferInfo.presentationTimeUs = getNextRelativePts(bufferInfo.presentationTimeUs, trackIndex);

        if (!allTracksAdded()) {
            if (trackIndex == mVideoTrackIndex) {
                Log.d(TAG, "RECEIVED VIDEO DATA NOT ALL TRACKS ADDED");
            } else {
                Log.d(TAG, "RECEIVED AUDIO DATA NOT ALL TRACKS ADDED");
            }
        }
        if (!allTracksFinished() && allTracksAdded()) {
            boolean isVideo = trackIndex == mVideoTrackIndex;
            int ret = 0;

/*            synchronized (mMuxerInputQueue)
            {
                if (isVideo) {

                    Log.v("mMuxerInputQueue Size",String.valueOf(mMuxerInputQueue.get(mVideoTrackIndex).size()));

                    if (mMuxerInputQueue.get(mVideoTrackIndex).size() >= 2 * EncodingConfig.DEFAULT_HUMAN_FPS) {
                        isMuxerInputQueueFull = true;
                    }

                    if (mMuxerInputQueue.get(mVideoTrackIndex).size() <= 1 * EncodingConfig.DEFAULT_HUMAN_FPS
                            && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)) {
                        isMuxerInputQueueFull = false;
                    }
                }
            }*/

            if (!isMuxerInputQueueFull)
            {
                if (isVideo && ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0)) {
                    getConfig().setMuxerState(EncodingConfig.MUXER_STATE.STREAMING);
                    Log.d(TAG, "WRITING VIDEO KEYFRAME");
                    packageH264Keyframe(encodedData, bufferInfo);
                    ret = mFFmpeg.writePacket(mH264Keyframe, bufferInfo.size + mH264MetaSize, bufferInfo.presentationTimeUs, cBoolean(isVideo), cBoolean(true));
                } else {
                    Log.d(TAG, "WRITING " + (isVideo ? "VIDEO" : "AUDIO") + " DATA");
                    ret = mFFmpeg.writePacket(encodedData, bufferInfo.size, bufferInfo.presentationTimeUs, cBoolean(isVideo), cBoolean(false));
                }
            }


            if(ret<0 && !haveSendWritePacketError)
            {
                if(mOnErrorListener!=null) {
                    mOnErrorListener.onError(this, WRITE_PACKET_FAIL, ret);
                    haveSendWritePacketError = true;
                }
            }
        }
        releaseOutputBufer(encoder, encodedData, bufferIndex, trackIndex);

        if (allTracksFinished()) {
            shutdown();
        }
    }

    private int cBoolean(boolean value) {
        return value ? 1 : 0;
    }

    private void releaseOutputBufer(MediaCodec encoder, ByteBuffer encodedData, int bufferIndex, int trackIndex) {
        synchronized (mEncoderReleasedSync) {
            if (!mEncoderReleased) {
                if (formatRequiresBuffering()) {
                    encodedData.clear();
                    synchronized (mMuxerInputQueue) {
                        mMuxerInputQueue.get(trackIndex).add(encodedData);
                        Log.i("mMuxerInputQueue", "add");

                        mMuxerInputQueue.notify();
                    }
                } else {
                    encoder.releaseOutputBuffer(bufferIndex, false);
                }
            }
        }
    }

    /**
     * Should only be called once, when the encoder produces
     * an output buffer with the BUFFER_FLAG_CODEC_CONFIG flag.
     * For H264 output, this indicates the Sequence Parameter Set
     * and Picture Parameter Set are contained in the buffer.
     * These NAL units are required before every keyframe to ensure
     * playback is possible in a segmented stream.
     *
     * @param encodedData
     * @param bufferInfo
     */
    private void captureH264MetaData(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mH264MetaSize = bufferInfo.size;
        mH264Keyframe = ByteBuffer.allocateDirect(encodedData.capacity());
        videoConfig = new byte[bufferInfo.size];
        encodedData.get(videoConfig, bufferInfo.offset, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        encodedData.put(videoConfig, 0, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        mH264Keyframe.put(videoConfig, 0, bufferInfo.size);
    }

    private void captureAACMetaData(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        audioConfig = new byte[bufferInfo.size];
        encodedData.get(audioConfig, bufferInfo.offset, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
        encodedData.put(audioConfig, 0, bufferInfo.size);
        encodedData.position(bufferInfo.offset);
    }

    /**
     * Adds the SPS + PPS data to the ByteBuffer containing a h264 keyframe
     *
     * @param encodedData
     * @param bufferInfo
     */
    private void packageH264Keyframe(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mH264Keyframe.position(mH264MetaSize);
        mH264Keyframe.put(encodedData); // BufferOverflow
    }

    private void addAdtsToByteBuffer(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        mInPacketSize = bufferInfo.size;
        mOutPacketSize = mInPacketSize + ADTS_LENGTH;
        addAdtsToPacket(mCachedAudioPacket, mOutPacketSize);
        encodedData.get(mCachedAudioPacket, ADTS_LENGTH, mInPacketSize);
        encodedData.position(bufferInfo.offset);
        encodedData.limit(bufferInfo.offset + mOutPacketSize);
        try {
            encodedData.put(mCachedAudioPacket, 0, mOutPacketSize);
            encodedData.position(bufferInfo.offset);
            bufferInfo.size = mOutPacketSize;
        } catch (BufferOverflowException e) {
            Log.w(TAG, "BufferOverFlow adding ADTS header");
            encodedData.put(mCachedAudioPacket, 0, mOutPacketSize);        // drop last 7 bytes...
        }
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet.
     * This is needed as MediaCodec encoder generates a packet of raw
     * AAC data.
     * <p/>
     * Note the packetLen must count in the ADTS header itself.
     * See: http://wiki.multimedia.cx/index.php?title=ADTS
     * Also: http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Channel_Configurations
     */
    private void addAdtsToPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;        // 11111111          = syncword
        packet[1] = (byte) 0xF9;        // 1111 1 00 1       = syncword MPEG-2 Layer CRC
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private void startMuxingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Muxing thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "FFmpeg").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    @Override
    public void run() {
        Log.d(TAG, "Starting looper");

        Looper.prepare();
        synchronized (mReadyFence) {
            Log.d(TAG, "setting mHandler");
            mHandler = new FFmpegHandler(this);
            Log.d(TAG, "setting mHandler to: " + mHandler);
            mReady = true;
            mReadyFence.notify();
        }
        getConfig().setMuxerState(EncodingConfig.MUXER_STATE.READY);
        Looper.loop();

        synchronized (mReadyFence) {
            mReady = false;
            mHandler = null;
        }
        mRunning = false;
        Log.d(TAG, "shutting down looper");
        Log.d(TAG, "shutting down looper, mHandler: " + mHandler);
    }

    public static class FFmpegHandler extends Handler {
        private WeakReference<FFmpegMuxer> mWeakMuxer;

        public FFmpegHandler(FFmpegMuxer muxer) {
            mWeakMuxer = new WeakReference<FFmpegMuxer>(muxer);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            FFmpegMuxer muxer = mWeakMuxer.get();
            if (muxer == null) {
                Log.w(TAG, "FFmpegHandler.handleMessage: muxer is null");
                return;
            }

            switch (what) {
                case MSG_ADD_TRACK:
                    muxer.handleAddTrack((MediaFormat) obj);
                    break;
                case MSG_WRITE_FRAME:
                    WritePacketData data = (WritePacketData) obj;
                    muxer.handleWriteSampleData(data.mEncoder,
                            data.mTrackIndex,
                            data.mBufferIndex,
                            data.mData,
                            data.getBufferInfo());
                    break;
                default:
                    throw new RuntimeException("Unexpected msg what=" + what);
            }
        }

    }

    /**
     * An object to encapsulate all the data
     * needed for writing a packet, for
     * posting to the Handler
     */
    public static class WritePacketData {

        private static MediaCodec.BufferInfo mBufferInfo;        // Used as singleton since muxer writes only one packet at a time

        public MediaCodec mEncoder;
        public int mTrackIndex;
        public int mBufferIndex;
        public ByteBuffer mData;
        public int offset;
        public int size;
        public long presentationTimeUs;
        public int flags;

        public WritePacketData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer data, MediaCodec.BufferInfo bufferInfo) {
            mEncoder = encoder;
            mTrackIndex = trackIndex;
            mBufferIndex = bufferIndex;
            mData = data;
            offset = bufferInfo.offset;
            size = bufferInfo.size;
            presentationTimeUs = bufferInfo.presentationTimeUs;
            flags = bufferInfo.flags;
        }

        public MediaCodec.BufferInfo getBufferInfo() {
            if (mBufferInfo == null)
                mBufferInfo = new MediaCodec.BufferInfo();
            mBufferInfo.set(offset, size, presentationTimeUs, flags);
            return mBufferInfo;
        }
    }
}
