package io.cine.android.streaming;

import android.util.Log;
import android.view.Surface;

import io.cine.ffmpegbridge.FFmpegBridge;

/**
 * Created by thomas on 7/1/14.
 */
public class EncodingConfig {
    private static final String TAG = "EncodingConfig";

    private static final int LANDSCAPE_CAMERA_WIDTH = 1280;
    private static final int LANDSCAPE_CAMERA_HEIGHT = 720;
    private static int DEFAULT_BIT_RATE = 1500000;
    private static int DEFAULT_HUMAN_FPS = 15;

    private Muxer.FORMAT mFormat;
    private String mOutputString;
    private AudioEncoderConfig mAudioEncoderConfig;
    private int mOrientation;
    private int mMachineVideoFps;
    public EncodingConfig() {
        mOrientation = Surface.ROTATION_0;
        mMachineVideoFps = DEFAULT_HUMAN_FPS * 1000;
    }

    public int getWidth() {
        if (isLandscape()) {
            return LANDSCAPE_CAMERA_WIDTH;
        } else {
            return LANDSCAPE_CAMERA_HEIGHT;
        }
    }

    public int getHeight() {
        if (isLandscape()) {
            return LANDSCAPE_CAMERA_HEIGHT;
        } else {
            return LANDSCAPE_CAMERA_WIDTH;
        }
    }

    public int getLandscapeWidth() {
        return LANDSCAPE_CAMERA_WIDTH;
    }

    public int getLandscapeHeight() {
        return LANDSCAPE_CAMERA_HEIGHT;
    }

    public int getBitrate() {
        return DEFAULT_BIT_RATE;
    }

    public boolean isLandscape() {
        boolean isLandscape = false;
        switch (mOrientation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                isLandscape = false;
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                isLandscape = true;
                break;
        }
        Log.d(TAG, "IS LANDSCAPE: " + isLandscape);
        return isLandscape;
    }

    public void setOutput(String outputString) {
        mOutputString = outputString;
        mFormat = EncodingConfig.calculateFormat(outputString);
    }

    private static Muxer.FORMAT calculateFormat(String outputString) {
        if (outputString.startsWith("rtmp://")) {
            return Muxer.FORMAT.RTMP;
        } else if (outputString.endsWith(".mp4")) {
            return Muxer.FORMAT.MPEG4;
        } else if (outputString.endsWith(".m3u8")) {
            return Muxer.FORMAT.HLS;
        } else {
            return null;
        }
    }

    public String getOutputPath() {
        return mOutputString;
    }

    public Muxer.FORMAT getFormat() {
        return mFormat;
    }

    public AudioEncoderConfig getAudioEncoderConfig() {
        return mAudioEncoderConfig;
    }

    public void setAudioEncoderConfig(AudioEncoderConfig audioEncoderConfig) {
        mAudioEncoderConfig = audioEncoderConfig;
    }

    public FFmpegBridge.AVOptions getAVOptions() {
        FFmpegBridge.AVOptions opts = new FFmpegBridge.AVOptions();
        switch (mFormat) {
            case MPEG4:
                opts.outputFormatName = "mp4";
                break;
            case HLS:
                opts.outputFormatName = "hls";
                break;
            case RTMP:
                opts.outputFormatName = "flv";
                break;
            default:
                throw new IllegalArgumentException("Unrecognized format!");
        }
        opts.outputUrl = getOutputPath();
        opts.videoHeight = getHeight();
        opts.videoWidth = getWidth();
        opts.videoFps = getHumanFPS();
        opts.videoBitRate = getBitrate();

        opts.audioSampleRate = mAudioEncoderConfig.getSampleRate();
        opts.audioNumChannels = mAudioEncoderConfig.getNumChannels();
        opts.audioBitRate = mAudioEncoderConfig.getBitrate();

        return opts;
    }

    public int getHumanFPS() {
        return getMachineVideoFps() / 1000;
    }

    // this works because getWidth and getHeight change depending on if it is landscape mode.
    public double getAspectRatio() {
        return (double) getWidth() / getHeight();
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int mOrientation) {
        this.mOrientation = mOrientation;
    }

    public int getMachineVideoFps() {
        return mMachineVideoFps;
    }

    public void setMachineVideoFps(int machineVideoFps) {
        this.mMachineVideoFps = machineVideoFps;
    }
}
