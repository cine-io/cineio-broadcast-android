package com.arenacloud.broadcast.android.api;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

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

    public static RequestParams toRequestParamsWithMasterKey(String masterKey){
        RequestParams requestParams = new RequestParams();
        requestParams.add("masterKey", masterKey);
        return requestParams;
    }

	public static RequestParams toRequestParamsWithPublicKey(String publicKey){
		RequestParams requestParams = new RequestParams();
		requestParams.add("publicKey", publicKey);
		return requestParams;
	}
}
