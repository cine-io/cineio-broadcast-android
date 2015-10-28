// inspired by: https://github.com/Kickflip/kickflip-android-sdk/blob/e35e0a5bb7161ccffebd564ec1a76a0e2c053fc8/sdk/src/main/java/io/kickflip/sdk/av/AudioEncoderCore.java
package com.arenacloud.broadcast.android.streaming;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * @hide
 */
public class AudioEncoderCore extends AndroidEncoder {

    protected static final String MIME_TYPE = "audio/mp4a-latm";                    // AAC Low Overhead Audio Transport Multiplex
    private static final String TAG = "AudioEncoderCore";
    private static final boolean VERBOSE = false;


    /**
     * Configures encoder and muxer state
     */
    public AudioEncoderCore(Muxer muxer) {
        super(muxer);
        AudioEncoderConfig config = muxer.getConfig().getAudioEncoderConfig();
        mBufferInfo = new MediaCodec.BufferInfo();

        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE, config.getSampleRate(), config.getChannelConfig());

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, config.getSampleRate());
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, config.getNumChannels());
        format.setInteger(MediaFormat.KEY_BIT_RATE, config.getBitrate());
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384); //16k

        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEncoder.start();
        } catch (IOException e) {

        }

        mTrackIndex = -1;
    }

    public MediaCodec getMediaCodec() {
        return mEncoder;
    }

    @Override
    protected boolean isSurfaceInputEncoder() {
        return false;
    }

}
