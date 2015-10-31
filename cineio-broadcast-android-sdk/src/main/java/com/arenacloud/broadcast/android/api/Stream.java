package com.arenacloud.broadcast.android.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Stream {

    private JSONObject data;

    public Stream(JSONObject data) {
        this.data = data;
    }

    public String getId() {
        try {
            return data.getString("id");
        } catch (JSONException e) {
            return null;
        }
    }

    public String getName() {
        try {
            return data.getString("name");
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        String name = getName();
        if (name != null) {
            return name;
        } else {
            return getId();
        }
    }

    public boolean getRecord() {
        try {
            return data.getBoolean("record");
        } catch (JSONException e) {
            return false;
        }
    }

    public JSONObject getData() {
        return data;
    }

    public String dataString() {
        return data.toString();
    }

    public String getPublishUrl() {
        try {
            JSONObject publishData = data.getJSONObject("publish");
            return publishData.get("url").toString() + "/" + publishData.get("stream").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHLSUrl() {
        try {
            JSONObject playData = data.getJSONObject("play");
            return playData.get("hls").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }

    public String getRtmpUrl() {
        try {
            JSONObject playData = data.getJSONObject("play");
            return playData.get("rtmp").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getPlayTTL() {
        try {
            JSONObject playData = data.getJSONObject("play");
            String ttlStr = playData.get("ttl").toString();
            if (ttlStr==null || ttlStr.isEmpty()) return 0;
            else
            {
                int ttl = Integer.parseInt(ttlStr);
                return ttl;
            }
        } catch (JSONException e) {
            return 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getSnapShotUrl() {
        try {
            return data.getString("snapshot");
        } catch (JSONException e) {
            return null;
        }
    }
}
