//
// JNI FFmpeg bridge for muxing H.264 and AAC streams into an FLV container
// for streaming over RTMP from an Android device.
//
// This file requires that libffmpegbridge.so is installed in src/main/jniLibs.
//
// Copyright (c) 2014, cine.io. All rights reserved.
//

package com.arenacloud.broadcast.ffmpegbridge;

import java.nio.ByteBuffer;

/**
 * A bridge to the FFmpeg C libraries.
 * <p/>
 * Based on: http://ffmpeg.org/doxygen/trunk/muxing_8c_source.html
 * Inspired by: http://www.roman10.net/how-to-build-android-applications-based-on-ffmpeg-by-an-example/
 * https://github.com/OnlyInAmerica/FFmpegTest
 * <p/>
 * As this is designed to complement Android's MediaCodec class, the only supported formats for
 * jData in writeAVPacketFromEncodedData are: H264 (YUV420P pixel format) / AAC (16 bit signed
 * integer samples, one center channel)
 * <p/>
 * Methods of this class must be called in the following order:
 * 1. init
 * 2. setAudioCodecExtraData and setVideoCodecExtraData
 * 3. writeHeader
 * 4. (repeat for each packet) writePacket
 * 5. finalize
 */
public class FFmpegBridge {

    static {
        System.loadLibrary("ffmpeg");
        System.loadLibrary("ffmpegbridge");
    }

    public native int init(AVOptions jOpts);

    public native void setAudioCodecExtraData(byte[] jData, int jSize);

    public native void setVideoCodecExtraData(byte[] jData, int jSize);

    public native void writeHeader();

    public native int writePacket(ByteBuffer jData, int jSize, long jPts, int jIsVideo, int jIsVideoKeyframe);

    public native void releaseResource();

    /**
     * Used to configure the muxer's options. Note the name of this class's
     * fields have to be hardcoded in the native method for retrieval.
     */
    static public class AVOptions {
        public String outputFormatName = "flv";
        public String outputUrl = "test.flv";

        public int videoHeight = 1280;
        public int videoWidth = 720;
        public int videoFps = 30;
        public int videoBitRate = 1500000;

        public int audioSampleRate = 44100;
        public int audioNumChannels = 1;
        public int audioBitRate = 128000;
    }
}
