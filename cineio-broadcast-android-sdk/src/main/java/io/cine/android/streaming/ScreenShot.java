package io.cine.android.streaming;

import android.os.Environment;

import java.io.File;

/**
 * Created by lgorse on 2/11/15.
 */
public class ScreenShot {

    private float scale;
    private String filePath;
    private String prefix;

    public ScreenShot(){
        this.scale = 1f;
        this.prefix = "";
        this.filePath =  Environment.getExternalStorageDirectory() + "/cineio/" ;
        initiateDirectory();
    }

    public ScreenShot(float scale, String filePath, String prefix){
        this.scale = scale;
        this.filePath = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = prefix;
        initiateDirectory();
    }

    public ScreenShot(float scale, String filePath){
        this.scale = scale;
        this.filePath = Environment.getExternalStorageDirectory() + "/" + filePath + "/";
        this.prefix = "";
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


}
