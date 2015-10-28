package com.arenacloud.broadcast.android.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Project {

	private JSONObject data;

	public Project(JSONObject data){
		this.data = data;
	}

	public String getId(){
		try {
			return this.data.getString("id");
		} catch (JSONException e) {
			return null;
		}
	}

	public String getName(){
		try {
			return this.data.getString("name");
		} catch (JSONException e) {
			return null;
		}
	}

	public JSONObject getData(){
		return this.data;
	}

	public String dataString() {
		return this.data.toString();
	}
}
