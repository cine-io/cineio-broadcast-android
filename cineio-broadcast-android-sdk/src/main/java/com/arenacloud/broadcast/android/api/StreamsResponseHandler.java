package com.arenacloud.broadcast.android.api;

import org.json.JSONException;

import java.util.ArrayList;

public class StreamsResponseHandler{

	public void onSuccess(ArrayList<Stream> streams) {
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
