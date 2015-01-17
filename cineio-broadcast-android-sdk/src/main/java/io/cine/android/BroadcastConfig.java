package io.cine.android;

import android.util.Log;

/**
 * Created by thomas on 1/16/15.
 */
public class BroadcastConfig {
    private Integer width;
    private Integer height;
    private String requestedCamera;
    private String lockedOrientation;


    public int getWidth() {
        if (width == null){
            return -1;
        }else{
            return width;
        }
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        if (height == null){
            return -1;
        }else{
            return height;
        }
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getLockedOrientation() {
        return lockedOrientation;
    }

    public void lockOrientation(String lockedOrientation) {
        if(lockedOrientation.equals("landscape") || lockedOrientation.equals("portrait") || lockedOrientation == null){
            this.lockedOrientation = lockedOrientation;
        } else {
            throw new RuntimeException("Orientation must be \"landscape\" or \"portrait\"");
        }
    }

    public String getRequestedCamera() {
        return requestedCamera;
    }

    public void selectCamera(String camera) {
        if(camera.equals("back") || camera.equals("front") || camera == null){
            this.requestedCamera = camera;
        } else {
            throw new RuntimeException("Camera must be \"front\" or \"back\"");
        }
    }
}
