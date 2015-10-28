package com.arenacloud.broadcast.android.api;

import java.util.ArrayList;

/**
 * Created by thomas on 7/29/14.
 */
public class StreamRecordingsResponseHandler {

    public void onSuccess(ArrayList<StreamRecording> streamRecordings) {
        // TODO Auto-generated method stub

    }

    public void onFailure(Exception e) {
        // TODO Auto-generated method stub
        e.printStackTrace();
    }

    public void onFailure(Throwable throwable) {
        // TODO Auto-generated method stub
        throwable.printStackTrace();
    }

}
