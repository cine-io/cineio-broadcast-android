package com.arenacloud.broadcast.android;

/**
 * Created by thomas on 1/16/15.
 */
public class BroadcastConfig {
    private int width;
    private int height;
    private String requestedCamera;
    private String lockedOrientation;
    private int mLayout;

    public BroadcastConfig(){
        this.width = -1;
        this.height = -1;
        this.mLayout = -1;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
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

    public void setBroadcastActivityLayout(int layout){
        this.mLayout = layout;
    }

    public int getBroadcastActivityLayout(){
        return mLayout;
    }
}

