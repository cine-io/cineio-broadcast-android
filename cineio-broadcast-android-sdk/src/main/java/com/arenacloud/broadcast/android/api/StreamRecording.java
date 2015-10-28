package com.arenacloud.broadcast.android.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thomas on 7/29/14.
 */
public class StreamRecording {

    private JSONObject data;

    public StreamRecording(JSONObject data) {
        this.data = data;
    }

    public String getName() {
        try {
            return data.get("name").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public String getUrl() {
        try {
            return data.get("url").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getDate() {
        try {
            return data.get("date").toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public int getSize() {
        try {
            return Integer.parseInt(data.get("size").toString());
        } catch (JSONException e) {
            return -1;
        }
    }

    public JSONObject getData() {
        return data;
    }

    public String dataString() {
        return data.toString();
    }

}