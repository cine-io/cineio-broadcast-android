package io.cine.android.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Stream {

  private JSONObject data;

  public Stream(JSONObject data){
    this.data = data;
  }

  public String getId(){
    try {
      return data.get("id").toString();
    } catch (JSONException e) {
      return null;
    }
  }

  public String getName(){
    try {
      return data.get("name").toString();
    } catch (JSONException e) {
      return null;
    }
  }

    @Override
    public String toString() {
        String name = getName();
        if (name != null){
            return name;
        }else{
            return getId();
        }
    }

    public JSONObject getData(){
    return data;
  }

  public String dataString() {
    return data.toString();
  }

    public String getPublishUrl() {
        try {
            JSONObject publishData = (JSONObject) data.get("publish");
            return publishData.get("url").toString() + "/" + publishData.get("stream").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getHLSUrl() {
        try {
            JSONObject playData = (JSONObject) data.get("play");
            return playData.get("hls").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

    }
}
