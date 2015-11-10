package com.arenacloud.broadcast.android.streaming;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.arenacloud.broadcast.android.BroadcastActivity;
import com.arenacloud.broadcast.android.streaming.gles.GlUtil;

/**
 * Created by lgorse on 2/11/15.
 *
 * This class defines the basic configuration required for a screenshot.
 * The relevant variables are the filepath,the file prefix and the scale.
 * FilePath means the folder path.
 * The file name is an aggregate of the prefix, the timestamp of Now() and a png extension.
 * Scale is important for space management - it allows the user to have some control over how
 * much space his thumbnails are going to take.
 */
public class ScreenShot {
    protected static final String TAG = GlUtil.TAG;

    private float scale;
    private String filePath;
    private String fileFolder;
    private String prefix;
    private BroadcastActivity.CameraHandler mCameraHandler;
    private File screenShotFile;

    public static final int SAVING_FRAME = 0;
    public static final int SAVED_FRAME = 1;
    public static final int FAILED_FRAME = 2;

    //Basic screenshot: just give us the CameraHandler, we'll do the rest
    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler){
        this.scale = 1f;
        this.prefix = "";
        this.fileFolder =  Environment.getExternalStorageDirectory() + "/arenacloud/" ;
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    //Screenshot with all variables defined
    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler, float scale, String filePath, String prefix){
        this.scale = scale;
        this.fileFolder = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = prefix;
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    //Screenshot without the prefix (perhaps not required by the user)
    public ScreenShot(BroadcastActivity.CameraHandler mCameraHandler, float scale, String filePath){
        this.scale = scale;
        this.fileFolder = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = "";
        this.mCameraHandler = mCameraHandler;
        initiateDirectory();
    }

    //Ensures that the file Folder destination is generated if it doesn't exist
    private void initiateDirectory() {
        File directory = new File(fileFolder);
        directory.mkdirs();
    }

    //Generates the full File. Useful to get the full filepath of the Bitmap
    public void setScreenShotPNGFile(){
        this.screenShotFile = new File(fileFolder, prefix + System.currentTimeMillis() + ".png");
        setFilePath(this.screenShotFile.toString());
    }

    public void setScreenShotJPEGFile(){
        this.screenShotFile = new File(fileFolder, prefix + System.currentTimeMillis() + ".jpeg");
        setFilePath(this.screenShotFile.toString());
    }

    public File getScreenShotFile(){
        return this.screenShotFile;
    }

    public void setScale(float scale){
        this.scale = scale;
    }

    public float getScale(){
        return this.scale;
    }

    private void setFilePath(String filePath){
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

    public String getFileFolder(){return this.fileFolder;}

    //Turns out we need to get the camerahandler in order to send messages to it
    public BroadcastActivity.CameraHandler getmCameraHandler(){
        return this.mCameraHandler;
    }

    //Notifies camera handler that we are in the process of saving a frame
    public void savingMessage() {
        Message message = new Message();
        message.obj = "Saving";
        sendCameraHandlerMessage(SAVING_FRAME, message);
    }

    //Notifies camera handler that we have saved a frame
    public void savedMessage() {
        Message message = new Message();
        message.obj = this;
        sendCameraHandlerMessage(SAVED_FRAME, message);
    }

    //Notifies camera handler that the frame save has failed
    public void failedFrameMessage(String failureMessage){
        Message message = new Message();
        message.obj = failureMessage;
        sendCameraHandlerMessage(FAILED_FRAME, message);
    }

    /**Handles the save frame messaging back to the camera.
     * Super useful to handle UI changes based on save status.
     * @param status
     */
    private void sendCameraHandlerMessage(int status, Message message){
        message.what = mCameraHandler.MSG_CAPTURE_FRAME;
        message.arg1 = status;
        switch(status){
            case SAVING_FRAME:
                mCameraHandler.sendMessage(message);
                break;
            case SAVED_FRAME:
                mCameraHandler.sendMessage(message);
                break;
            case FAILED_FRAME:
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
     *The comments above are directly from the Grafika library. I shifted a lot of its content
     * to this section so it makes sense to have it as a reminder.
     *
     * This method saves the contents of a Bytebuffer to an output stream, and generates
     * a bitmap from that.
     *
     * It also applies any scaling and rotates/mirrors the image to take into account
     * OpenGL scaling.
     *
     * @param buf must be rewound prior to being passed to this method.
     *            Essentially buff must be declared, and GLReadPixels called, then the buffer rewound
     *            Then it is ready to be passed to this method.
     *            It's important to do these preparation steps in an EGL context, and not on their own.
     *            That's why we make sure not to include them here. Maybe not a good idea?
     * @param width
     * @param height
     * @throws IOException
     */

    public void saveBitmapFromBuffer(ByteBuffer buf, int width, int height) throws IOException {
        BufferedOutputStream bos = null;
        try {
            Long startTime = System.currentTimeMillis();
            savingMessage();
            setScreenShotPNGFile();
            bos = new BufferedOutputStream(new FileOutputStream(getFilePath()));
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
            failedFrameMessage(e.getMessage());
            throw e;
        } finally {
            savedMessage();
            if (bos != null){
                bos.close();
                bos.flush();
            }
        }
        Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + getFilePath() + "'");

    }

    public void saveBitmapFromJpegBuffer(byte[] buffer)
    {
        savingMessage();

        setScreenShotJPEGFile();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getFilePath());
            fos.write(buffer);                                               // Written to the file
            fos.close();

            savedMessage();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
