// started from https://github.com/google/grafika/blob/f3c8c3dee60153f471312e21acac8b3a3cddd7dc/src/com/android/grafika/BroadcastActivity.java
/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cine.android;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import io.cine.android.streaming.AspectFrameLayout;
import io.cine.android.streaming.AudioEncoderConfig;
import io.cine.android.streaming.CameraSurfaceRenderer;
import io.cine.android.streaming.CameraUtils;
import io.cine.android.streaming.EncodingConfig;
import io.cine.android.streaming.FFmpegMuxer;
import io.cine.android.streaming.MicrophoneEncoder;
import io.cine.android.streaming.Muxer;
import io.cine.android.streaming.TextureMovieEncoder;

/**
 * Shows the camera preview on screen while simultaneously recording it to a .mp4 file.
 * <p/>
 * Every time we receive a frame from the camera, we need to:
 * <ul>
 * <li>Render the frame to the SurfaceView, on GLSurfaceView's renderer thread.
 * <li>Render the frame to the mediacodec's input surface, on the encoder thread, if
 * recording is enabled.
 * </ul>
 * <p/>
 * At any given time there are four things in motion:
 * <ol>
 * <li>The UI thread, embodied by this Activity.  We must respect -- or work around -- the
 * app lifecycle changes.  In particular, we need to release and reacquire the Camera
 * so that, if the user switches away from us, we're not preventing another app from
 * using the camera.
 * <li>The Camera, which will busily generate preview frames once we hand it a
 * SurfaceTexture.  We'll get notifications on the main UI thread unless we define a
 * Looper on the thread where the SurfaceTexture is created (the GLSurfaceView renderer
 * thread).
 * <li>The video encoder thread, embodied by TextureMovieEncoder.  This needs to share
 * the Camera preview external texture with the GLSurfaceView renderer, which means the
 * EGLContext in this thread must be created with a reference to the renderer thread's
 * context in hand.
 * <li>The GLSurfaceView renderer thread, embodied by CameraSurfaceRenderer.  The thread
 * is created for us by GLSurfaceView.  We don't get callbacks for pause/resume or
 * thread startup/shutdown, though we could generate messages from the Activity for most
 * of these things.  The EGLContext created on this thread must be shared with the
 * video encoder, and must be used to create a SurfaceTexture that is used by the
 * Camera.  As the creator of the SurfaceTexture, it must also be the one to call
 * updateTexImage().  The renderer thread is thus at the center of a multi-thread nexus,
 * which is a bit awkward since it's the thread we have the least control over.
 * </ol>
 * <p/>
 * GLSurfaceView is fairly painful here.  Ideally we'd create the video encoder, create
 * an EGLContext for it, and pass that into GLSurfaceView to share.  The API doesn't allow
 * this, so we have to do it the other way around.  When GLSurfaceView gets torn down
 * (say, because we rotated the device), the EGLContext gets tossed, which means that when
 * it comes back we have to re-create the EGLContext used by the video encoder.  (And, no,
 * the "preserve EGLContext on pause" feature doesn't help.)
 * <p/>
 * We could simplify this quite a bit by using TextureView instead of GLSurfaceView, but that
 * comes with a performance hit.  We could also have the renderer thread drive the video
 * encoder directly, allowing them to work from a single EGLContext, but it's useful to
 * decouple the operations, and it's generally unwise to perform disk I/O on the thread that
 * renders your UI.
 * <p/>
 * We want to access Camera from the UI thread (setup, teardown) and the renderer thread
 * (configure SurfaceTexture, start preview), but the API says you can only access the object
 * from a single thread.  So we need to pick one thread to own it, and the other thread has to
 * access it remotely.  Some things are simpler if we let the renderer thread manage it,
 * but we'd really like to be sure that Camera is released before we leave onPause(), which
 * means we need to make a synchronous call from the UI thread into the renderer thread, which
 * we don't really have full control over.  It's less scary to have the UI thread own Camera
 * and have the renderer call back into the UI thread through the standard Handler mechanism.
 * <p/>
 * (The <a href="http://developer.android.com/training/camera/cameradirect.html#TaskOpenCamera">
 * camera docs</a> recommend accessing the camera from a non-UI thread to avoid bogging the
 * UI thread down.  Since the GLSurfaceView-managed renderer thread isn't a great choice,
 * we might want to create a dedicated camera thread.  Not doing that here.)
 * <p/>
 * With three threads working simultaneously (plus Camera causing periodic events as frames
 * arrive) we have to be very careful when communicating state changes.  In general we want
 * to send a message to the thread, rather than directly accessing state in the object.
 * <p/>
 * &nbsp;
 * <p/>
 * To exercise the API a bit, the video encoder is required to survive Activity restarts.  In the
 * current implementation it stops recording but doesn't stop time from advancing, so you'll
 * see a pause in the video.  (We could adjust the timer to make it seamless, or output a
 * "paused" message and hold on that in the recording, or leave the Camera running so it
 * continues to generate preview frames while the Activity is paused.)  The video encoder object
 * is managed as a static property of the Activity.
 */
public class BroadcastActivity extends Activity
        implements SurfaceTexture.OnFrameAvailableListener, EncodingConfig.EncodingCallback {
    private static final String TAG = "BroadcastActivity";
    private static final boolean VERBOSE = false;
    // this is static so it survives activity restarts
    private static TextureMovieEncoder sVideoEncoder = new TextureMovieEncoder();
    private GLSurfaceView mGLView;
    private CameraSurfaceRenderer mRenderer;
    private Camera mCamera;
    private CameraHandler mCameraHandler;
    private boolean mRecordingEnabled;      // controls button state
    private Muxer mMuxer;
    private AudioEncoderConfig mAudioConfig;
    private MicrophoneEncoder mAudioEncoder;
    private Camera.CameraInfo mCameraInfo;
    private AspectFrameLayout mFrameLayout;
    private EncodingConfig mEncodingConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_broadcast_capture);
//        setButtonHolderLayout();

        Bundle extras = getIntent().getExtras();
        String outputString;
        int width = -1;
        int height = -1;
        if (extras != null) {
            outputString = extras.getString("PUBLISH_URL");
            width = extras.getInt("WIDTH", -1);
            height = extras.getInt("HEIGHT", -1);
        }else{
            outputString = Environment.getExternalStorageDirectory().getAbsolutePath() + "/cineio-recording.mp4";
        }

        mEncodingConfig = new EncodingConfig(this);
        if(width != -1){
            Log.v(TAG, "SETTING WIDTH TO: " + width);
            mEncodingConfig.setWidth(width);
        }
        if(height != -1){
            Log.v(TAG, "SETTING HEIGHT TO: " + height);
            mEncodingConfig.setHeight(height);
        }
        mEncodingConfig.setOutput(outputString);
        mMuxer = new FFmpegMuxer();

        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);

        mRecordingEnabled = sVideoEncoder.isRecording();

        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, mMuxer);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        mAudioConfig = AudioEncoderConfig.createDefaultProfile();
        mEncodingConfig.setAudioEncoderConfig(mAudioConfig);
        mAudioEncoder = new MicrophoneEncoder(mMuxer);
        // http://stackoverflow.com/questions/5975168/android-button-setpressed-after-onclick
        Button toggleRecording = (Button) findViewById(R.id.toggleRecording_button);

        toggleRecording.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // show interest in events resulting from ACTION_DOWN
                if(event.getAction()==MotionEvent.ACTION_DOWN) return true;
                // don't handle event unless its ACTION_UP so "doSomething()" only runs once.
                if(event.getAction()!=MotionEvent.ACTION_UP) return false;
                toggleRecordingHandler();
                return true;
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onCreate complete: " + this);

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume -- acquiring camera");
        super.onResume();
        updateControls();
        openCamera();

        // Set the preview aspect ratio.
        mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        mGLView.onResume();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getWidth(), mEncodingConfig.getHeight());
            }
        });
        Log.d(TAG, "onResume complete: " + this);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();
        if (mRecordingEnabled) {
            stopRecording();
        }
        releaseCamera();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // Tell the renderer that it's about to be paused so it can clean up.
                mRenderer.notifyPausing();
            }
        });
        mGLView.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mCameraHandler.invalidateHandler();     // paranoia
    }

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
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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

    /**
     * onClick handler for "record" button.
     */
    public void toggleRecordingHandler() {
        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        mRecordingEnabled = true;
        mMuxer.prepare(mEncodingConfig);
        mAudioEncoder.startRecording();
        toggleRecording();
    }

    private void stopRecording() {
        mRecordingEnabled = false;
        mAudioEncoder.stopRecording();
        toggleRecording();
    }

    private void toggleRecording() {
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                // notify the renderer that we want to change the encoder's state
                mRenderer.changeRecordingState(mRecordingEnabled);
            }
        });
        updateControls();
    }

    /**
     * Updates the on-screen controls to reflect the current state of the app.
     */
    private void updateControls() {
        Button recordingButton = (Button) findViewById(R.id.toggleRecording_button);
        recordingButton.setPressed(mRecordingEnabled);
    }

    /**
     * Connects the SurfaceTexture to the Camera preview output, and starts the preview.
     */
    private void handleSetSurfaceTexture(SurfaceTexture st) {
        st.setOnFrameAvailableListener(this);
        try {
            mCamera.setPreviewTexture(st);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        mCamera.startPreview();
    }

//    private void setButtonHolderLayout() {
//        LinearLayout buttonHolder = (LinearLayout) findViewById(R.id.camera_button_holder);
//
//        // Checks the orientation of the screen
//
//        int degrees = getDeviceRotationDegrees();
//
//        if (degrees == 90) {
//            buttonHolder.setGravity(Gravity.CENTER | Gravity.RIGHT);
//        } else if (degrees == 270) {
//            buttonHolder.setGravity(Gravity.CENTER | Gravity.RIGHT);
//        } else {
//            buttonHolder.setGravity(Gravity.CENTER | Gravity.BOTTOM);
//        }
//    }

    private int getDeviceRotationDegrees() {

        switch (this.getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private void setEncoderOrientation() {
        mEncodingConfig.setOrientation(this.getWindowManager().getDefaultDisplay().getRotation());
    }

    private void handleSetCameraOrientation() {
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
        mFrameLayout.setAspectRatio(mEncodingConfig.getAspectRatio());

        CameraUtils.choosePreviewSize(parms, mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());

        mCamera.setParameters(parms);

        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());
            }
        });

        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        // The SurfaceTexture uses this to signal the availability of a new frame.  The
        // thread that "owns" the external texture associated with the SurfaceTexture (which,
        // by virtue of the context being shared, *should* be either one) needs to call
        // updateTexImage() to latch the buffer.
        //
        // Once the buffer is latched, the GLSurfaceView thread can signal the encoder thread.
        // This feels backward -- we want recording to be prioritized over rendering -- but
        // since recording is only enabled some of the time it's easier to do it this way.
        //
        // Since GLSurfaceView doesn't establish a Looper, this will *probably* execute on
        // the main UI thread.  Fortunately, requestRender() can be called from any thread,
        // so it doesn't really matter.
        if (VERBOSE) Log.d(TAG, "ST onFrameAvailable");
        mGLView.requestRender();
    }

    @Override
    public void muxerStatusUpdate(EncodingConfig.MUXER_STATE muxerState) {
        updateStatusText(muxerState);
        handleStreamingUpdate(muxerState);
    }

    private void handleStreamingUpdate(final EncodingConfig.MUXER_STATE muxerState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (muxerState){
                    case CONNECTING:
                        int currentOrientation = getResources().getConfiguration().orientation;
                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                        }
                        else {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                        }
                        break;
                    case SHUTDOWN:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        break;
                }
            }
        });
    }

    private void updateStatusText(final EncodingConfig.MUXER_STATE muxerState){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


        TextView fileText = (TextView) findViewById(R.id.streamingStatus);
        String statusText;
        switch (muxerState){
            case PREPARING:
                statusText = "Preparing";
                break;
            case CONNECTING:
                statusText = "Connecting";
                break;
            case READY:
                statusText = "Ready";
                break;
            case STREAMING:
                statusText = "Streaming";
                break;
            case SHUTDOWN:
                statusText = "Ready";
                break;
            default:
                statusText = "Unknown";
                break;
        }
        fileText.setText(statusText);

            }
        });

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

        // Weak reference to the Activity; only access this from the UI thread.
        private WeakReference<BroadcastActivity> mWeakActivity;

        public CameraHandler(BroadcastActivity activity) {
            mWeakActivity = new WeakReference<BroadcastActivity>(activity);
        }

        /**
         * Drop the reference to the activity.  Useful as a paranoid measure to ensure that
         * attempts to access a stale Activity through a handler are caught.
         */
        public void invalidateHandler() {
            mWeakActivity.clear();
        }

        @Override  // runs on UI thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Log.d(TAG, "CameraHandler [" + this + "]: what=" + what);

            BroadcastActivity activity = mWeakActivity.get();
            if (activity == null) {
                Log.w(TAG, "CameraHandler.handleMessage: activity is null");
                return;
            }

            switch (what) {
                case MSG_SURFACE_CHANGED:
                    activity.handleSetCameraOrientation();
                    break;
                case MSG_SET_SURFACE_TEXTURE:
                    activity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }
}

