package com.arenacloud.broadcast.android;

/**
 * Created by WilliamShi on 15/10/10.
 */
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.arenacloud.broadcast.android.streaming.AspectFrameLayout;
import com.arenacloud.broadcast.android.streaming.AudioEncoderConfig;
import com.arenacloud.broadcast.android.streaming.CameraSurfaceRenderer2;
import com.arenacloud.broadcast.android.streaming.CameraUtils;
import com.arenacloud.broadcast.android.streaming.EncodingConfig;
import com.arenacloud.broadcast.android.streaming.FFmpegMuxer;
import com.arenacloud.broadcast.android.streaming.MicrophoneEncoder;
import com.arenacloud.broadcast.android.streaming.Muxer;
import com.arenacloud.broadcast.android.streaming.TextureMovieEncoder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class BroadcastView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener, EncodingConfig.EncodingCallback , Muxer.OnErrorListener{

    private static final String TAG = "BroadcastView";

    public BroadcastView(Context context) {
        super(context);
    }

    public BroadcastView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private WeakReference<Activity> weakFatherActivity = null;
    public void initBroadcast(Bundle extras, Activity fatherActivity)
    {
        if (weakFatherActivity!=null)
        {
            weakFatherActivity.clear();
            weakFatherActivity = null;
        }

        weakFatherActivity = new WeakReference<Activity>(fatherActivity);

        initializeEncodingConfig(extras);
        initializeMuxer();
        initializeAudio();
        initializeVideo();
        initializeGLView();

        fatherActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public double getAspectRatio()
    {
        if (mEncodingConfig!=null)
        {
            return mEncodingConfig.getAspectRatio();
        }

        return 0;
    }

    public void toggleRecording()
    {
        if(is_reconnect_status) return;

        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void switchCamera()
    {
        releaseCamera();

        if (requestedCamera.equals("back")) {
            requestedCamera = "front";
        } else {
            requestedCamera = "back";
        }

        openCamera();

        if (weakSurfaceTexture != null) {
            SurfaceTexture st = weakSurfaceTexture.get();
            st.setOnFrameAvailableListener(BroadcastView.this);
            try {
                mCamera.setPreviewTexture(st);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
            mCamera.startPreview();
        }

        handleSetCameraOrientation();
    }



    public void takeScreenShot()
    {
        if (mCamera!=null)
        {
//            mCamera.takePicture(null,rawPictureCallback,null,jpegPictureCallback);

            this.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.takeScreenShot();
                }
            });
        }
    }

    private Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.v(TAG,"rawPictureCallback");
        }
    };
    private Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (screenShotCallback!=null)
            {
                if (mOrientation.equals("landscape"))
                {
                    screenShotCallback.onScreenShotEvent(BitmapFactory.decodeByteArray(data,0,data.length));
                }else{
                    Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);

                    Matrix m = new Matrix();
                    m.postRotate(90);
                    bmp = Bitmap.createBitmap(bmp, 0, 0, mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight(), m, false);

                    screenShotCallback.onScreenShotEvent(bmp);
                }
            }
        }
    };

    private ScreenShotCallback screenShotCallback = null;
    public void setScreenShotCallback(ScreenShotCallback callback)
    {
//        screenShotCallback = callback;

        mRenderer.setScreenShotCallback(callback);
    }

    public interface ScreenShotCallback {
        public void onScreenShotEvent(Bitmap bitmap);
    }

    public void onResumeHandler()
    {
        openCamera();

        // Set the preview aspect ratio.
//        mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        this.onResume();
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight(), mEncodingConfig.isLandscape());
            }
        });
    }

    public void onPauseHandler()
    {
        lastRecordingStatus = mRecordingEnabled;

        if (mRecordingEnabled) {
            stopRecording();
        }

        releaseCamera();
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        this.onPause();
    }

    public void onDestroyHandler()
    {
        mCameraHandler.invalidateHandler();     // paranoia

        if (weakSurfaceTexture!=null)
        {
            weakSurfaceTexture.clear();
            weakSurfaceTexture = null;
        }

        if (weakFatherActivity!=null)
        {
            weakFatherActivity.clear();
            weakFatherActivity = null;
        }
    }

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    /**
     * Opens a camera, and attempts to establish preview mode at the specified width and height.
     * <p/>
     * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
     */
    private void openCamera() {
        if (mCamera != null) {
            throw new RuntimeException("camera already initialized");
        }

        Camera.CameraInfo info = new Camera.CameraInfo();

        // Try to find a front-facing camera (e.g. for videoconferencing).
        int numCameras = Camera.getNumberOfCameras();
        int cameraToFind;
        if(requestedCamera != null && requestedCamera.equals("back")){
            cameraToFind = Camera.CameraInfo.CAMERA_FACING_BACK;
        }else{
            cameraToFind = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraToFind) {
                mCameraInfo = info;
                mCamera = Camera.open(i);
                break;
            } else {
                mCameraInfo = info;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        int realMachineFps = CameraUtils.chooseFixedPreviewFps(parms, mEncodingConfig.getMachineVideoFps());
        mEncodingConfig.setMachineVideoFps(realMachineFps);
        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate.
        parms.setRecordingHint(true);
        List<Integer> supportedFormats = parms.getSupportedPictureFormats();
        Log.d(TAG, "TOTAL SUPPORTED FORMATS: " + supportedFormats.size());
        for (Integer i : supportedFormats) {
            Log.d(TAG, "SUPPORTED FORMAT: " + i);
        }
        parms.setPreviewFormat(ImageFormat.NV21);

        // leave the frame rate set to default
        mCamera.setParameters(parms);
    }

    /**
     * Stops camera preview, and releases the camera to the system.
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            Log.d(TAG, "releaseCamera -- done");
        }
    }

    private void startRecording() {
        mRecordingEnabled = true;
        boolean ret = mMuxer.prepare(mEncodingConfig);
        if (ret)
        {
            mAudioEncoder.startRecording();
            this.queueEvent(new Runnable() {
                @Override
                public void run() {
                    // notify the renderer that we want to change the encoder's state
                    mRenderer.changeRecordingState(mRecordingEnabled);
                }
            });
        }
    }

    private void stopRecording() {
        mRecordingEnabled = false;
        mAudioEncoder.stopRecording();
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeRecordingState(mRecordingEnabled);
            }
        });
    }

    private EncodingConfig mEncodingConfig;
    private String requestedCamera;
    private String mOrientation;
    private void initializeEncodingConfig(Bundle extras) {
        String outputString;
        int width = -1;
        int height = -1;
        String orientation = null;

        if (extras != null) {
            outputString = extras.getString("PUBLISH_URL");
            width = extras.getInt("WIDTH", -1);
            height = extras.getInt("HEIGHT", -1);
            orientation = extras.getString("ORIENTATION");
            this.requestedCamera = extras.getString("CAMERA");
        }else{
            outputString = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cineio-recording.mp4";
        }

        mOrientation = orientation;
        Activity fatherActivity = weakFatherActivity.get();
        if(orientation != null && orientation.equals("landscape") && fatherActivity!=null){
            fatherActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if(orientation != null && orientation.equals("portrait") && fatherActivity!=null){
            fatherActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        mEncodingConfig = new EncodingConfig(this);
        mEncodingConfig.forceOrientation(orientation);
        if(width != -1){
            Log.v(TAG, "SETTING WIDTH TO: " + width);
            mEncodingConfig.setWidth(width);
        }
        if(height != -1){
            Log.v(TAG, "SETTING HEIGHT TO: " + height);
            mEncodingConfig.setHeight(height);
        }
        mEncodingConfig.setOutput(outputString);
    }

    private Muxer mMuxer;
    private void initializeMuxer(){
        mMuxer = new FFmpegMuxer();
        mMuxer.setOnErrorListener(this);
    }

    private AudioEncoderConfig mAudioConfig;
    private MicrophoneEncoder mAudioEncoder;
    private void initializeAudio() {
        mAudioConfig = AudioEncoderConfig.createDefaultProfile();
        mEncodingConfig.setAudioEncoderConfig(mAudioConfig);
        mAudioEncoder = new MicrophoneEncoder(mMuxer);
    }

    private CameraHandler mCameraHandler;
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private boolean mRecordingEnabled;      // controls button state
    private void initializeVideo() {
        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);
        mRecordingEnabled = sVideoEncoder.isRecording();
    }

    private CameraSurfaceRenderer2 mRenderer;
    private void initializeGLView() {
        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        this.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new CameraSurfaceRenderer2(mCameraHandler, sVideoEncoder, mMuxer);
        this.setRenderer(mRenderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Handles camera operation requests from other threads.  Necessary because the Camera
     * must only be accessed from one thread.
     * <p/>
     * The object is created on the UI thread, and all handlers run there.  Messages are
     * sent from other threads, using sendMessage().
     */
    public static class CameraHandler extends Handler {
        public static final int MSG_SET_SURFACE_TEXTURE = 0;
        public static final int MSG_SURFACE_CHANGED = 1;
        public static final int MSG_CAPTURE_FRAME = 2;
        public static final int MSG_RECONNECT = 3;

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<BroadcastView> mWeakView;

        public CameraHandler(BroadcastView view) {
            mWeakView = new WeakReference<BroadcastView>(view);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakView.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            BroadcastView view = mWeakView.get();
            if (view == null) {
                Log.w(TAG, "CameraHandler.handleMessage: view is null");
                return;
            }

            switch (what) {
                case MSG_SURFACE_CHANGED:
                    view.handleSetCameraOrientation();
                    break;
                case MSG_SET_SURFACE_TEXTURE:
                    view.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    view.handleResumeRecording();
                    break;
                case MSG_CAPTURE_FRAME:
                    view.handleSaveFrameMessage(inputMessage);
                    break;
                case MSG_RECONNECT:
                    view.handleReconnect();
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    private void handleSetCameraOrientation()
    {
        setEncoderOrientation();
        Log.d(TAG, "handle setting camera orientation");
        int degrees = getDeviceRotationDegrees();
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }

        Camera.Parameters parms = mCamera.getParameters();

        Log.d(TAG, "SETTING ASPECT RATIO: " + mEncodingConfig.getAspectRatio());
//        mFrameLayout.setAspectRatio(mEncodingConfig.getAspectRatio());

        CameraUtils.choosePreviewSize(parms, mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());

//        parms.setPreviewSize(1280, 720);

        mCamera.setParameters(parms);

        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight(), mEncodingConfig.isLandscape());
//                mRenderer.setCameraPreviewSize(1280, 720);
            }
        });

        mCamera.setDisplayOrientation(result);
    }

    private void setEncoderOrientation() {
        Activity fatherActivit = weakFatherActivity.get();
        if(fatherActivit!=null)
        {
            mEncodingConfig.setOrientation(fatherActivit.getWindowManager().getDefaultDisplay().getRotation());
        }
    }

    private int getDeviceRotationDegrees() {
        // fake out the forced orientation
        if (this.mEncodingConfig.hasForcedOrientation()){
            if (this.mEncodingConfig.forcedLandscape()){
                return 90;
            } else {
                return 0;
            }
        }

        Activity fatherActivit = weakFatherActivity.get();
        if(fatherActivit!=null)
        {
            switch (fatherActivit.getWindowManager().getDefaultDisplay().getRotation()) {
                // normal portrait
                case Surface.ROTATION_0:
                    return 0;
                // expected landscape
                case Surface.ROTATION_90:
                    return 90;
                // upside down portrait
                case Surface.ROTATION_180:
                    return 180;
                // "upside down" landscape
                case Surface.ROTATION_270:
                    return 270;
            }
            return 0;
        }
        return 0;
    }

    private WeakReference<SurfaceTexture> weakSurfaceTexture = null;
    private void handleSetSurfaceTexture(SurfaceTexture st)
    {
        if (weakSurfaceTexture!=null)
        {
            weakSurfaceTexture.clear();
            weakSurfaceTexture = null;
        }
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        mCamera.startPreview();

        weakSurfaceTexture = new WeakReference<SurfaceTexture>(st);
    }

    private boolean lastRecordingStatus = false;
    private void handleResumeRecording()
    {
        if (lastRecordingStatus)
        {
            startRecording();
        }
    }

    private void handleSaveFrameMessage(Message inputMessage)
    {

    }

    private void handleReconnect()
    {
        Activity fatherActivity = weakFatherActivity.get();
        if (fatherActivity!=null)
        {
            Toast.makeText(fatherActivity, "网络连接断开, 正在尝试重连...",
                    Toast.LENGTH_LONG).show();
        }

        reconnect();
    }

    private volatile boolean is_reconnect_status = false;
    private void reconnect()
    {
        lastRecordingStatus = mRecordingEnabled;

        if (mRecordingEnabled) {
            stopRecording();
        }

        releaseCamera();
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        this.onPause();
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        updateControls();
        openCamera();

        // Set the preview aspect ratio.
//        mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        this.onResume();
        this.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight(), mEncodingConfig.isLandscape());
            }
        });
    }


    public void setBroadcastStatusCallback(BroadcastStatusCallback callback)
    {
        broadcastStatusCallback = callback;
    }

    private BroadcastStatusCallback broadcastStatusCallback = null;
    @Override
    public void muxerStatusUpdate(EncodingConfig.MUXER_STATE muxerState) {
        if (broadcastStatusCallback!=null)
        {
            BROADCAST_STATE broadcast_state;
            switch (muxerState){
                case PREPARING:
                    broadcast_state = BROADCAST_STATE.PREPARING;
                    break;
                case CONNECTING:
                    broadcast_state = BROADCAST_STATE.CONNECTING;
                    break;
                case READY:
                    broadcast_state = BROADCAST_STATE.READY;
                    break;
                case STREAMING:
                    broadcast_state = BROADCAST_STATE.STREAMING;
                    is_reconnect_status = false;
                    break;
                case SHUTDOWN:
                    broadcast_state = BROADCAST_STATE.READY;
                    break;
                default:
                    broadcast_state = BROADCAST_STATE.READY;
                    break;
            }
            broadcastStatusCallback.broadcastStatusUpdate(broadcast_state);
        }
    }

    @Override
    public boolean onError(Muxer mx, int what, int extra) {{
        if (what==Muxer.OPEN_URL_FAIL && !is_reconnect_status)
        {
            Activity fatherActivity = weakFatherActivity.get();
            if (fatherActivity!=null)
            {
                Toast.makeText(fatherActivity, "打开推流地址失败, 请查看网络连接!",
                        Toast.LENGTH_LONG).show();
                fatherActivity.finish();
            }

            return true;
        }

        if (what==Muxer.OPEN_URL_FAIL && is_reconnect_status)
        {
            //send message for reconnect
            mCameraHandler.sendEmptyMessageDelayed(CameraHandler.MSG_RECONNECT,10000);
            return true;
        }

        if (what==Muxer.WRITE_PACKET_FAIL)
        {
            is_reconnect_status = true;

            //send message for reconnect
            mCameraHandler.sendEmptyMessageDelayed(CameraHandler.MSG_RECONNECT,10000);
            return true;
        }

        return true;
    }}

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }

    public static enum BROADCAST_STATE {PREPARING, READY, CONNECTING, STREAMING, SHUTDOWN}

    public interface BroadcastStatusCallback {
        public void broadcastStatusUpdate(BROADCAST_STATE state);
    }
}
