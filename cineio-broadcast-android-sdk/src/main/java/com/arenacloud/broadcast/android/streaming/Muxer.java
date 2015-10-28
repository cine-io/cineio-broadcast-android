// inspired by: https://github.com/Kickflip/kickflip-android-sdk/blob/e35e0a5bb7161ccffebd564ec1a76a0e2c053fc8/sdk/src/main/java/io/kickflip/sdk/av/Muxer.java
package com.arenacloud.broadcast.android.streaming;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.google.common.eventbus.EventBus;

import java.nio.ByteBuffer;

/**
 * Base Muxer class for interaction with MediaCodec based
 * encoders
 *
 * @hide
 */
public abstract class Muxer {
    private static final String TAG = "Muxer";
    private final int mExpectedNumTracks = 2;           // TODO: Make this configurable?
    protected int mNumTracks;
    protected int mNumTracksFinished;
    protected long mFirstPts;
    protected long mLastPts[];
    private EncodingConfig mConfig;
    private EventBus mEventBus;

    protected Muxer() {
    }

    public static final int OPEN_URL_FAIL = 0;
    public static final int WRITE_PACKET_FAIL = 1;

    public interface OnErrorListener {
        boolean onError(Muxer mx, int what, int extra);
    }
    protected OnErrorListener mOnErrorListener = null;
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public EncodingConfig getConfig() {
        return mConfig;
    }

    public boolean prepare(EncodingConfig config) {
        mConfig = config;
        mNumTracks = 0;
        mNumTracksFinished = 0;
        mFirstPts = 0;
        mLastPts = new long[mExpectedNumTracks];
        for (int i = 0; i < mLastPts.length; i++) {
            mLastPts[i] = 0;
        }
        Log.i(TAG, "Created muxer for output: " + mConfig.getOutputPath());
        return true;
    }

    public void setEventBus(EventBus eventBus) {
        mEventBus = eventBus;
    }

    /**
     * Returns the absolute output path.
     * <p/>
     * e.g /sdcard/app/uuid/index.m3u8
     *
     * @return
     */
    public String getOutputPath() {
        return mConfig.getOutputPath();
    }

    /**
     * Adds the specified track and returns the track index
     *
     * @param trackFormat MediaFormat of the track to add. Gotten from MediaCodec#dequeueOutputBuffer
     *                    when returned status is INFO_OUTPUT_FORMAT_CHANGED
     * @return index of track in output file
     */
    public int addTrack(MediaFormat trackFormat) {
        mNumTracks++;
        return mNumTracks - 1;
    }

    /**
     * Called by the hosting Encoder
     * to notify the Muxer that it should no
     * longer assume the Encoder resources are available.
     */
    public void onEncoderReleased(int trackIndex) {
    }

    public void release() {
    }

    /**
     * Write the MediaCodec output buffer. This method <b>must</b>
     * be overridden by subclasses to release encodedData, transferring
     * ownership back to encoder, by calling encoder.releaseOutputBuffer(bufferIndex, false);
     *
     * @param trackIndex
     * @param encodedData
     * @param bufferInfo
     */
    public void writeSampleData(MediaCodec encoder, int trackIndex, int bufferIndex, ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo) {
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "SIGNAL END OF TRACK");
            signalEndOfTrack();
        }
    }

    protected boolean allTracksFinished() {
        return (mNumTracks == mNumTracksFinished);
    }

    protected boolean allTracksAdded() {
        return (mNumTracks == mExpectedNumTracks);
    }

    /**
     * Muxer will call this itself if it detects BUFFER_FLAG_END_OF_STREAM
     * in writeSampleData.
     */
    public void signalEndOfTrack() {
        mNumTracksFinished++;
    }

    /**
     * Does this Muxer's format require AAC ADTS headers?
     * see http://wiki.multimedia.cx/index.php?title=ADTS
     *
     * @return
     */
    protected boolean formatRequiresADTS() {
        switch (mConfig.getFormat()) {
            case HLS:
                return true;
            case MPEG4:
                return true;
            case RTMP:
                return true;
            default:
                return false;
        }
    }

    /**
     * Does this Muxer's format require
     * copying and buffering encoder output buffers.
     * Generally speaking, is the output a Socket or File?
     *
     * @return
     */
    protected boolean formatRequiresBuffering() {
        switch (mConfig.getFormat()) {
            case RTMP:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return a relative pts given an absolute pts and trackIndex.
     * <p/>
     * This method advances the state of the Muxer, and must only
     * be called once per call to {@link #writeSampleData(android.media.MediaCodec, int, int, java.nio.ByteBuffer, android.media.MediaCodec.BufferInfo)}.
     */
    protected long getNextRelativePts(long absPts, int trackIndex) {
        if (mFirstPts == 0) {
            mFirstPts = absPts;
            return 0;
        }
        return getSafePts(absPts - mFirstPts, trackIndex);
    }

    /**
     * Sometimes packets with non-increasing pts are dequeued from the MediaCodec output buffer.
     * This method ensures that a crash won't occur due to non monotonically increasing packet timestamp.
     */
    private long getSafePts(long pts, int trackIndex) {
        if (mLastPts[trackIndex] >= pts) {
            // Enforce a non-zero minimum spacing
            // between pts
            mLastPts[trackIndex] += 9643;
            return mLastPts[trackIndex];
        }
        mLastPts[trackIndex] = pts;
        return pts;
    }

    public static enum FORMAT {MPEG4, HLS, RTMP}
}
