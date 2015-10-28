// inspired by: https://github.com/Kickflip/kickflip-android-sdk/blob/e35e0a5bb7161ccffebd564ec1a76a0e2c053fc8/sdk/src/main/java/io/kickflip/sdk/av/MicrophoneEncoder.java
package com.arenacloud.broadcast.android.streaming;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.os.Trace;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by davidbrodsky on 1/23/14.
 *
 * @hide
 */
public class MicrophoneEncoder implements Runnable {
    protected static final int SAMPLES_PER_FRAME = 1024;                            // AAC frame size. Audio encoder input size is a multiple of this
    protected static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final boolean TRACE = false;
    private static final boolean VERBOSE = false;
    private static final String TAG = "MicrophoneEncoder";
    private final Object mReadyFence = new Object();    // Synchronize audio thread readiness
    private final Muxer mMuxer;
    private final Object mRecordingFence = new Object();
    // Variables recycled between calls to sendAudioToEncoder
    MediaCodec mMediaCodec;
    int audioInputBufferIndex;
    int audioInputLength;
    long audioAbsolutePtsUs;
    long startPTS = 0;
    long totalSamplesNum = 0;
    private boolean mThreadReady;                       // Is audio thread ready
    private boolean mThreadRunning;                     // Is audio thread running
    private AudioRecord mAudioRecord;
    private AudioEncoderCore mEncoderCore;
    private boolean mRecordingRequested;

    public MicrophoneEncoder(Muxer muxer) {
        mMuxer = muxer;
        init();
    }

    private void init() {
        mMediaCodec = null;
        mThreadReady = false;
        mThreadRunning = false;
        mRecordingRequested = false;
    }

    private void reset() {
        audioInputBufferIndex = 0;
        audioInputLength = 0;
        audioAbsolutePtsUs = 0;
    }

    private void setupAudioRecord() {
        AudioEncoderConfig config = getAudioEncoderConfig();

        int minBufferSize = AudioRecord.getMinBufferSize(config.getSampleRate(),
                config.getChannelConfig(), AUDIO_FORMAT);

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC, // source
                config.getSampleRate(),            // sample rate, hz
                config.getChannelConfig(),         // channels
                AUDIO_FORMAT,                        // audio format
                minBufferSize * 4);                  // buffer size (bytes)

    }

    private AudioEncoderConfig getAudioEncoderConfig() {
        return mMuxer.getConfig().getAudioEncoderConfig();
    }

    public void startRecording() {
        if (VERBOSE) Log.i(TAG, "startRecording");
        reset();
        mEncoderCore = new AudioEncoderCore(mMuxer);
        startThread();

        synchronized (mRecordingFence) {
            totalSamplesNum = 0;
            startPTS = 0;
            mRecordingRequested = true;
            mRecordingFence.notify();
        }
    }

    public void stopRecording() {
        Log.i(TAG, "stopRecording");
        synchronized (mRecordingFence) {
            mRecordingRequested = false;
        }
    }

    public boolean isRecording() {
        return mRecordingRequested;
    }

    private void startThread() {
        synchronized (mReadyFence) {
            if (mThreadRunning) {
                Log.w(TAG, "Audio thread running when start requested");
                return;
            }
            Thread audioThread = new Thread(this, "MicrophoneEncoder");
            audioThread.setPriority(Thread.MAX_PRIORITY);
            audioThread.start();
            while (!mThreadReady) {
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
        setupAudioRecord();

        synchronized (mReadyFence) {
            mThreadReady = true;
            mReadyFence.notify();
        }

        synchronized (mRecordingFence) {
            while (!mRecordingRequested) {
                try {
                    mRecordingFence.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mAudioRecord.startRecording();
        mMediaCodec = mEncoderCore.getMediaCodec();

        if (VERBOSE)
            Log.i(TAG, "Begin Audio transmission to encoder. encoder : " + mEncoderCore.mEncoder);

        while (mRecordingRequested) {

            if (TRACE) Trace.beginSection("drainAudio");
            mEncoderCore.drainEncoder(false);
            if (TRACE) Trace.endSection();

            if (TRACE) Trace.beginSection("sendAudio");
            sendAudioToEncoder(false);
            if (TRACE) Trace.endSection();

        }
        mThreadReady = false;
        if (VERBOSE) Log.i(TAG, "Exiting audio encode loop. Draining Audio Encoder");
        if (TRACE) Trace.beginSection("sendAudio");
        sendAudioToEncoder(true);
        if (TRACE) Trace.endSection();
        mAudioRecord.stop();
        if (TRACE) Trace.beginSection("drainAudioFinal");
        mEncoderCore.drainEncoder(true);
        if (TRACE) Trace.endSection();
        mEncoderCore.release();
        mThreadRunning = false;
    }

    private void sendAudioToEncoder(boolean endOfStream) {
        // send current frame data to encoder
        try {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            audioInputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (audioInputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[audioInputBufferIndex];
                inputBuffer.clear();
                audioInputLength = mAudioRecord.read(inputBuffer, SAMPLES_PER_FRAME * 2);
                audioAbsolutePtsUs = (System.nanoTime()) / 1000L;
                // We divide audioInputLength by 2 because audio samples are
                // 16bit.
                audioAbsolutePtsUs = getJitterFreePTS(audioAbsolutePtsUs, audioInputLength / 2);

                if (audioInputLength == AudioRecord.ERROR_INVALID_OPERATION)
                    Log.e(TAG, "Audio read error: invalid operation");
                if (audioInputLength == AudioRecord.ERROR_BAD_VALUE)
                    Log.e(TAG, "Audio read error: bad value");
//                if (VERBOSE)
//                    Log.i(TAG, "queueing " + audioInputLength + " audio bytes with pts " + audioAbsolutePtsUs);
                if (endOfStream) {
                    if (VERBOSE) Log.i(TAG, "EOS received in sendAudioToEncoder");
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    mMediaCodec.queueInputBuffer(audioInputBufferIndex, 0, audioInputLength, audioAbsolutePtsUs, 0);
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "_offerAudioEncoder exception");
            t.printStackTrace();
        }
    }

    /**
     * Ensures that each audio pts differs by a constant amount from the previous one.
     *
     * @param bufferPts        presentation timestamp in us
     * @param bufferSamplesNum the number of samples of the buffer's frame
     * @return
     */
    private long getJitterFreePTS(long bufferPts, long bufferSamplesNum) {
        AudioEncoderConfig config = getAudioEncoderConfig();

        long correctedPts = 0;
        long bufferDuration = (1000000 * bufferSamplesNum) / (config.getSampleRate());
        bufferPts -= bufferDuration; // accounts for the delay of acquiring the audio buffer
        if (totalSamplesNum == 0) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
        }
        correctedPts = startPTS + (1000000 * totalSamplesNum) / (config.getSampleRate());
        if (bufferPts - correctedPts >= 2 * bufferDuration) {
            // reset
            startPTS = bufferPts;
            totalSamplesNum = 0;
            correctedPts = startPTS;
        }
        totalSamplesNum += bufferSamplesNum;
        return correctedPts;
    }
}
