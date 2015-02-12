package io.cine.android.streaming;

import android.os.Environment;
import android.os.Message;

import java.io.File;

import io.cine.android.BroadcastActivity;

/**
 * Created by lgorse on 2/11/15.
 */
public class ScreenShot {

    private float scale;
    private String filePath;
    private String prefix;
    private BroadcastActivity.CameraHandler mCameraHandler;

    public static final int SAVING_FRAME = 0;
    public static final int SAVED_FRAME = 1;

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
        }
    }


}
