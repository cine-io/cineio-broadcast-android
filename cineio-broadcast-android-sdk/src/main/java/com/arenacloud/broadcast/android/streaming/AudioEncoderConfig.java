// inspired by: https://github.com/Kickflip/kickflip-android-sdk/blob/e35e0a5bb7161ccffebd564ec1a76a0e2c053fc8/sdk/src/main/java/io/kickflip/sdk/av/AudioEncoderConfig.java
package com.arenacloud.broadcast.android.streaming;

import android.media.AudioFormat;
import android.util.Log;

/**
 * @hide
 */
public class AudioEncoderConfig {

    private static final String TAG = "AudioEncoderConfig";
    protected final int mNumChannels;
    protected final int mSampleRate;
    protected final int mBitrate;

    public AudioEncoderConfig(int channels, int sampleRate, int bitRate) {
        mNumChannels = channels;
        mBitrate = bitRate;
        mSampleRate = sampleRate;
    }

    public static AudioEncoderConfig createDefaultProfile() {
        return new AudioEncoderConfig(1, 44100, 128000);
    }

    public int getNumChannels() {
        return mNumChannels;
    }

    public int getSampleRate() {
        return mSampleRate;
    }

    public int getBitrate() {
        return mBitrate;
    }

    public int getChannelConfig() {
        switch (mNumChannels) {
            case 1:
                Log.d(TAG, "SETTING CHANNEL MONO");
                return AudioFormat.CHANNEL_IN_MONO;
            case 2:
                Log.d(TAG, "SETTING CHANNEL STEREO");
                return AudioFormat.CHANNEL_IN_STEREO;
            default:
                throw new IllegalArgumentException("Invalid channel count. Must be 1 or 2");
        }

    }

    @Override
    public String toString() {
        return "AudioEncoderConfig: " + mNumChannels + " channels totaling " + mBitrate + " bps @" + mSampleRate + " Hz";
    }
}
