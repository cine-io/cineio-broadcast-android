package com.arenacloud.broadcast.android.streaming;

import android.util.Log;
import android.view.Surface;

import com.arenacloud.broadcast.ffmpegbridge.FFmpegBridge;

/**
 * Created by thomas on 7/1/14.
 */
public class EncodingConfig {

    private String camera;
    private String forcedOrientation;

    public void forceOrientation(String forcedOrientation) {
        this.forcedOrientation = forcedOrientation;
    }

    public boolean hasForcedOrientation() {
        return this.forcedOrientation != null;
    }

    public interface EncodingCallback {
        public void muxerStatusUpdate(MUXER_STATE muxerState);
    }
    private static final String TAG = "EncodingConfig";

    private int customWidth;
    private static final int LANDSCAPE_CAMERA_WIDTH = 1280;
    private int customHeight;
    private static final int LANDSCAPE_CAMERA_HEIGHT = 720;
    private static int DEFAULT_BIT_RATE = 700000/*1500000*//*750000*/;
    public static int DEFAULT_HUMAN_FPS = 15;
    private EncodingCallback mEncodingCallback;
    private MUXER_STATE mMuxerState;

    public static enum MUXER_STATE {PREPARING, READY, CONNECTING, STREAMING, SHUTDOWN}

    private Muxer.FORMAT mFormat;
    private String mOutputString;
    private AudioEncoderConfig mAudioEncoderConfig;
    private int mOrientation;
    private int mMachineVideoFps;

    public EncodingConfig(EncodingCallback encodingCallback) {
        mEncodingCallback = encodingCallback;
        mOrientation = Surface.ROTATION_0;
        mMachineVideoFps = DEFAULT_HUMAN_FPS * 1000;
        setDefaultValues();
    }

    public EncodingConfig(){
        setDefaultValues();
    }
    private void setDefaultValues(){
        this.customHeight = -1;
        this.customWidth = -1;
    }

    public void setWidth(int width){
        this.customWidth = width;
    }

    public void setHeight(int height){
        this.customHeight = height;
    }

    public int getWidth() {
        if (isLandscape()) {
            return getOrientationAgnosticWidth();
        } else {
            return getOrientationAgnosticHeight();
        }
    }

    public int getHeight() {
        if (isLandscape()) {
            return getOrientationAgnosticHeight();
        } else {
            return getOrientationAgnosticWidth();
        }
    }

    public int getLandscapeWidth() {
        return getOrientationAgnosticWidth();
    }

    public int getLandscapeHeight() {
        return getOrientationAgnosticHeight();
    }

    private int getOrientationAgnosticWidth(){
        return this.customWidth == -1 ? LANDSCAPE_CAMERA_WIDTH : this.customWidth;
    }

    private int getOrientationAgnosticHeight(){
        return this.customHeight == -1 ? LANDSCAPE_CAMERA_HEIGHT : this.customHeight;
    }

    public int getBitrate() {
        return DEFAULT_BIT_RATE;
    }

    public boolean forcedLandscape(){
        return forcedOrientation.equals("landscape");
    }
    public boolean isLandscape() {
        if (forcedOrientation != null){
            return forcedLandscape();
        }
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
        if (outputString!=null)
        {
            mOutputString = outputString;
            mFormat = EncodingConfig.calculateFormat(outputString);
        }
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

    public void setMuxerState(MUXER_STATE muxerState) {
        if (mMuxerState == muxerState){
            return;
        }
        mMuxerState = muxerState;
        mEncodingCallback.muxerStatusUpdate(muxerState);
    }
}
