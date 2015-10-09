package com.arenacloud.broadcast.android.api;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by thomas on 8/25/14.
 */
public class ProjectsResponseHandler {
    public void onSuccess(ArrayList<Project> projects) {
        // TODO Auto-generated method stub
    }

    public void onFailure(JSONException e) {
        // TODO Auto-generated method stub
        e.printStackTrace();
    }

    public void onFailure(Throwable throwable) {
        // TODO Auto-generated method stub
        throwable.printStackTrace();
    }

}
