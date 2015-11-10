package com.arenacloud.broadcast.android.api;

import org.json.JSONException;

public class ProjectResponseHandler {

	public void onSuccess(Project project) {
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
