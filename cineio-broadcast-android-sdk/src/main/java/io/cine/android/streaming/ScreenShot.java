package io.cine.android.streaming;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.cine.android.BroadcastActivity;
import io.cine.android.streaming.gles.GlUtil;

/**
 * Created by lgorse on 2/11/15.
 */
public class ScreenShot {
    protected static final String TAG = GlUtil.TAG;

    private float scale;
    private String filePath;
    private String prefix;
    private BroadcastActivity.CameraHandler mCameraHandler;

    public static final int SAVING_FRAME = 0;
    public static final int SAVED_FRAME = 1;
    public static final int FAILED_FRAME = 2;

    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler){
        this.scale = 1f;
        this.prefix = "";
        this.filePath =  Environment.getExternalStorageDirectory() + "/cineio/" ;
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler, float scale, String filePath, String prefix){
        this.scale = scale;
        this.filePath = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = prefix;
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler, float scale, String filePath){
        this.scale = scale;
        this.filePath = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = "";
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    private void initiateDirectory() {
        File directory = new File(filePath);
        directory.mkdirs();
    }

    public File getPhotoFile(){
        File saveFile = new File(filePath, prefix + System.currentTimeMillis() + ".png");
        return saveFile;
    }

    public void setScale(float scale){
        this.scale = scale;
    }

    public float getScale(){
        return this.scale;
    }

    public void setFilePath(String filePath){
        this.filePath = filePath;
    }

    public String getFilePath(){
        return this.filePath;
    }

    public void setPrefix(String prefix){
        this.prefix = prefix;
    }

    public String getPrefix(){
        return this.prefix;
    }

    public BroadcastActivity.CameraHandler getmCameraHandler(){
        return this.mCameraHandler;
    }


    public void savingMessage() {
        sendCameraHandlerMessage(SAVING_FRAME);

    }

    public void savedMessage() {
        sendCameraHandlerMessage(SAVED_FRAME);
    }

    public void failedFrameMessage(){
        sendCameraHandlerMessage(FAILED_FRAME);
    }

    private void sendCameraHandlerMessage(int status){
        Message message = new Message();
        message.what = mCameraHandler.MSG_CAPTURE_FRAME;
        message.arg1 = status;
        switch(status){
            case SAVING_FRAME:
                message.obj = "Saving";
                mCameraHandler.sendMessage(message);
                break;
            case SAVED_FRAME:
                message.obj = getPhotoFile().toString();
                mCameraHandler.sendMessage(message);
                break;
            case FAILED_FRAME:
                message.obj = "Could not save frame";
                mCameraHandler.sendMessage(message);
                break;
        }
    }

    // glReadPixels fills in a "direct" ByteBuffer with what is essentially big-endian RGBA
    // data (i.e. a byte of red, followed by a byte of green...).  While the Bitmap
    // constructor that takes an int[] wants little-endian ARGB (blue/red swapped), the
    // Bitmap "copy pixels" method wants the same format GL provides.
    //
    // Ideally we'd have some way to re-use the ByteBuffer, especially if we're calling
    // here often.
    //
    // Making this even more interesting is the upside-down nature of GL, which means
    // our output will look upside down relative to what appears on screen if the
    // typical GL conventions are used.

    /**
     *
     * @param buf must be rewound prior to being passed to this method.
     *            Essentially buff must be declared, and GLReadPixels called, then the buffer rewound
     *            Then it is ready to be passed ot this method.
     *            It's important to do these preparation steps in an EGL context, so we keep this separate r
     *            from this method
     * @param width
     * @param height
     * @throws IOException
     */

    public void saveBitmapFromBuffer(ByteBuffer buf, int width, int height) throws IOException {
        BufferedOutputStream bos = null;
        try {
            Long startTime = System.currentTimeMillis();
            savingMessage();
            bos = new BufferedOutputStream(new FileOutputStream(getPhotoFile().toString()));
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);
            Matrix m = new Matrix();
            m.preScale(-getScale(), getScale());
            m.postRotate(180);
            bmp = Bitmap.createBitmap(bmp, 0, 0, width, height, m, false);
            bmp.compress(Bitmap.CompressFormat.PNG, 50, bos);
            bmp.recycle();
            Log.i("time elapsed", String.valueOf(System.currentTimeMillis() - startTime) + " milliseconds");
        }catch (IOException e){
            failedFrameMessage();
            throw e;
        } finally {
            savedMessage();
            if (bos != null){
                bos.close();
                bos.flush();
            }
        }
        Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + getPhotoFile().toString() + "'");

    }


}
