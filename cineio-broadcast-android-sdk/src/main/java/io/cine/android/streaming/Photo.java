package io.cine.android.streaming;

import android.os.Environment;

/**
 * Created by lgorse on 2/11/15.
 */
public class Photo {

    private float scale;
    private String filePath;
    private String prefix;

    public Photo(){
        this.scale = 1f;
        this.filePath =  Environment.getExternalStorageDirectory() + "/cineio/" ;
        this.prefix = "";
    }

    public Photo(float scale, String filePath, String prefix){
        this.scale = scale;
        this.filePath = filePath;
        this.prefix = prefix;
    }

    public String setFilePath(){

    }


}
