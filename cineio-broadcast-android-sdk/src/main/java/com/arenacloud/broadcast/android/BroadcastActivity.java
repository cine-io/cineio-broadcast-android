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

package com.arenacloud.broadcast.android;

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
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.List;

import com.arenacloud.broadcast.android.streaming.AspectFrameLayout;
import com.arenacloud.broadcast.android.streaming.AudioEncoderConfig;
import com.arenacloud.broadcast.android.streaming.CameraSurfaceRenderer;
import com.arenacloud.broadcast.android.streaming.CameraUtils;
import com.arenacloud.broadcast.android.streaming.EncodingConfig;
import com.arenacloud.broadcast.android.streaming.FFmpegMuxer;
import com.arenacloud.broadcast.android.streaming.MicrophoneEncoder;
import com.arenacloud.broadcast.android.streaming.Muxer;
import com.arenacloud.broadcast.android.streaming.ScreenShot;
import com.arenacloud.broadcast.android.streaming.TextureMovieEncoder;
import com.arenacloud.broadcast.android.streaming.gles.EglSurfaceBase;

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
        implements SurfaceTexture.OnFrameAvailableListener, EncodingConfig.EncodingCallback , Muxer.OnErrorListener{
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
    private String requestedCamera;

    private ScreenShot mScreenShot;
    private String mOrientation;

    private boolean lastRecordingStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Bundle extras = getIntent().getExtras();
        int layout = extras.getInt("LAYOUT", R.layout.activity_broadcast_capture);
        setContentView(layout);

        initializeEncodingConfig(extras);
        initializeMuxer();
        initializeAudio();
        initializeVideo();
        initializeGLView();
//        setButtonHolderLayout();
        // http://stackoverflow.com/questions/5975168/android-button-setpressed-after-onclick
        Button toggleRecording = (Button) findViewById(R.id.toggleRecording_button);

        toggleRecording.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // show interest in events resulting from ACTION_DOWN
                if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
                // don't handle event unless its ACTION_UP so "doSomething()" only runs once.
                if (event.getAction() != MotionEvent.ACTION_UP) return false;
                toggleRecordingHandler();
                return true;
            }
        });

/*        toggleRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecordingHandler();
            }
        });*/

        Button switchRecording = (Button) findViewById(R.id.toggleSwitch_button);
        switchRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
/*
                lastRecordingStatus = mRecordingEnabled;

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
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


                if(requestedCamera.equals("back"))
                {
                    requestedCamera = "front";
                }else
                {
                    requestedCamera = "back";
                }

                updateControls();
                openCamera();

                // Set the preview aspect ratio.
                mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

                mGLView.onResume();
                mGLView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());
                    }
                });
*/
                releaseCamera();

                if (requestedCamera.equals("back")) {
                    requestedCamera = "front";
                } else {
                    requestedCamera = "back";
                }

                openCamera();

                if (weakSurfaceTexture != null) {
                    SurfaceTexture st = weakSurfaceTexture.get();
                    st.setOnFrameAvailableListener(BroadcastActivity.this);
                    try {
                        mCamera.setPreviewTexture(st);
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                    mCamera.startPreview();
                }

                handleSetCameraOrientation();
            }
        });

        mScreenShot = new ScreenShot(mCameraHandler);

        Button sceenshotButton = (Button) findViewById(R.id.toggleScreenShot_button);
        sceenshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ScreenShot
                if (mCamera!=null)
                {
                    mCamera.takePicture(null,rawPictureCallback,null,jpegPictureCallback);
                }
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onCreate complete: " + this);
    }

    private Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };
    private Camera.PictureCallback jpegPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (mOrientation.equals("landscape"))
            {
                mScreenShot.saveBitmapFromJpegBuffer(data);
            }else{

            }
        }
    };

    protected TextureMovieEncoder getsVideoEncoder(){
        return sVideoEncoder;
    }

    private void initializeGLView() {
        // Configure the GLSurfaceView.  This will start the Renderer thread, with an
        // appropriate EGL context.
        mGLView = (GLSurfaceView) findViewById(R.id.cameraPreview_surfaceView);
        mGLView.setEGLContextClientVersion(2);     // select GLES 2.0
        mRenderer = new CameraSurfaceRenderer(mCameraHandler, sVideoEncoder, mMuxer);
        mGLView.setRenderer(mRenderer);
        mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void initializeVideo() {
        // Define a handler that receives camera-control messages from other threads.  All calls
        // to Camera must be made on the same thread.  Note we create this before the renderer
        // thread, so we know the fully-constructed object will be visible.
        mCameraHandler = new CameraHandler(this);
        mRecordingEnabled = sVideoEncoder.isRecording();
    }

    private void initializeMuxer(){
        mMuxer = new FFmpegMuxer();
        mMuxer.setOnErrorListener(this);
    }

    private void initializeAudio() {
        mAudioConfig = AudioEncoderConfig.createDefaultProfile();
        mEncodingConfig.setAudioEncoderConfig(mAudioConfig);
        mAudioEncoder = new MicrophoneEncoder(mMuxer);
    }


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
        if(orientation != null && orientation.equals("landscape")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if(orientation != null && orientation.equals("portrait")){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

    private void handleResumeRecording()
    {
        if (lastRecordingStatus)
        {
            startRecording();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume -- acquiring camera");
        super.onResume();

//        initializeEncodingConfig();

        updateControls();
        openCamera();

        // Set the preview aspect ratio.
        mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        mGLView.onResume();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());
            }
        });
        Log.d(TAG, "onResume complete: " + this);

    }

//    private Object o = new Object();
    @Override
    protected void onPause() {
        Log.d(TAG, "onPause -- releasing camera");
        super.onPause();

        lastRecordingStatus = mRecordingEnabled;

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
//        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onPause complete");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mCameraHandler.invalidateHandler();     // paranoia

        if (weakSurfaceTexture!=null)
        {
            weakSurfaceTexture.clear();
            weakSurfaceTexture = null;
        }
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

    /**
     * onClick handler for "record" button.
     */
    public void toggleRecordingHandler() {
        if(is_reconnect_status) return;

        mRecordingEnabled = !mRecordingEnabled;
        if (mRecordingEnabled) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        mRecordingEnabled = true;
        boolean ret = mMuxer.prepare(mEncodingConfig);
        if (ret)
        {
            mAudioEncoder.startRecording();
            toggleRecording();
        }
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
    private WeakReference<SurfaceTexture> weakSurfaceTexture = null;
    private void handleSetSurfaceTexture(SurfaceTexture st) {
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
        // fake out the forced orientation
        if (this.mEncodingConfig.hasForcedOrientation()){
            if (this.mEncodingConfig.forcedLandscape()){
                return 90;
            } else {
                return 0;
            }
        }

        switch (this.getWindowManager().getDefaultDisplay().getRotation()) {
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

//        parms.setPreviewSize(1280, 720);

        mCamera.setParameters(parms);

        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());
//                mRenderer.setCameraPreviewSize(1280, 720);
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
//        handleStreamingUpdate(muxerState);
    }

    private void handleStreamingUpdate(final EncodingConfig.MUXER_STATE muxerState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (muxerState){
                    case CONNECTING:
                        int currentOrientation = getResources().getConfiguration().orientation;
                        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                        }
                        else {
//                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
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
                is_reconnect_status = false;
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
     *
     *Sends a message to the encoder.
     * The encoder is directly connected to the EGLSurface, it accepts messages and it guarantees the
     * EGLContext's state so it's convenient to run the command via the encoder.
     * All the user has to do is define a screenshot and call this method. The app takes care of the rest.
     * Only customization he may want is to handle the frame messages (see below) to react to the status
     * of the frame save.
     */
    protected void saveFrame(ScreenShot screenShot){
        Message message = new Message();
        TextureMovieEncoder textureMovieEncoder = getsVideoEncoder();
        message.what = TextureMovieEncoder.MSG_ENCODER_SAVEFRAME;
        message.obj = screenShot;
        TextureMovieEncoder.EncoderHandler mEncoderHandler = textureMovieEncoder.getHandler();
        if (mEncoderHandler != null) {
            mEncoderHandler.sendMessage(message);
        }else{
            Log.d("TextureMovieEncoder EncoderHandler is null", "in plain English you are probably not recording right now");
        }
    }

    /**
     * takes the saveframe status message (Which is returned via the encoder
     * when capture begins, ends or fails).
     * Then it dispatches to 3 methods based on whether the status is beginning, saved or failed.
     * Useful to set it up this way because the 3 handle methods are overridable - you can use them
     * to send a message to your cameraHandler, to update the UI for instance.
     * @param inputMessage
     */
    private void handleSaveFrameMessage(Message inputMessage) {
        switch(inputMessage.arg1){
            case ScreenShot.SAVING_FRAME:
                handleSavingFrame((String) inputMessage.obj);
                break;
            case ScreenShot.SAVED_FRAME:
                handleSavedFrame((ScreenShot) inputMessage.obj);
                break;
            case ScreenShot.FAILED_FRAME:
                handleFailedFrame((String) inputMessage.obj);
            default:
                break;
        }
    }

    protected void handleFailedFrame(String errorString) {
        Log.i("I FAILED TO SAVE", errorString);
    }

    /**
     * When the frame has been saved the message object will contain
     * the file path of the bitmap
     * @param screenShot
     */
    protected void handleSavedFrame(ScreenShot screenShot) {
        Log.i("I SAVED A FRAME", screenShot.getFilePath());

        Toast.makeText(this, "I SAVED A FRAME",
                Toast.LENGTH_LONG).show();
    }

    protected void handleSavingFrame(String savingString) {
        Log.i("I'M SAVING A FRAME", savingString);

        Toast.makeText(this, "I'M SAVING A FRAME",
                Toast.LENGTH_SHORT).show();
    }


    protected CameraHandler getCameraHandler(){
        return mCameraHandler;
    }

    private volatile boolean is_reconnect_status = false;
    @Override
    public boolean onError(Muxer mx, int what, int extra) {
        if (what==Muxer.OPEN_URL_FAIL && !is_reconnect_status)
        {
            Toast.makeText(getApplicationContext(), "打开推流地址失败, 请查看网络连接!",
                    Toast.LENGTH_LONG).show();
            this.finish();
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
    }

    private void reconnect()
    {
        lastRecordingStatus = mRecordingEnabled;

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
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        updateControls();
        openCamera();

        // Set the preview aspect ratio.
        mFrameLayout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);

        mGLView.onResume();
        mGLView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.setCameraPreviewSize(mEncodingConfig.getLandscapeWidth(), mEncodingConfig.getLandscapeHeight());
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
        public static final int MSG_CAPTURE_FRAME = 2;
        public static final int MSG_RECONNECT = 3;

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
                    activity.handleResumeRecording();
                    break;
                case MSG_CAPTURE_FRAME:
                    activity.handleSaveFrameMessage(inputMessage);
                    break;
                case MSG_RECONNECT:
                    Toast.makeText(activity, "网络连接断开, 正在尝试重连...",
                            Toast.LENGTH_LONG).show();
                    activity.reconnect();
                    break;
                default:
                    throw new RuntimeException("unknown msg " + what);
            }
        }
    }

    /*
    @Override
    public void setRequestedOrientation(int requestedOrientation){
        return;
    }*/
}

