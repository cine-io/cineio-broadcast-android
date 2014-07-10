package io.cine.android.api;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.RequestParams;

public class JsonToParams {

	public static RequestParams toRequestParams(String secretKey){
		RequestParams requestParams = new RequestParams();
		requestParams.add("secretKey", secretKey);
		return requestParams;
	}

	public static RequestParams toRequestParams(String secretKey, JSONObject params){
		RequestParams requestParams = new RequestParams();
		requestParams.add("secretKey", secretKey);
        try {
		Iterator<?> keys = params.keys();
		while( keys.hasNext() ){
			String key = (String)keys.next();
			requestParams.add(key, (String) params.get(key));
		}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return requestParams;
	}
}
