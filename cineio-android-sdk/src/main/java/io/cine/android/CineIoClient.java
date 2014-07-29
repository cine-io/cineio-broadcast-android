package io.cine.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.cine.android.api.JsonToParams;
import io.cine.android.api.Project;
import io.cine.android.api.ProjectResponseHandler;
import io.cine.android.api.Stream;
import io.cine.android.api.StreamResponseHandler;
import io.cine.android.api.StreamsResponseHandler;

public class CineIoClient {

    private final static String TAG = "CineIoClient";
    private final static String BASE_URL = "https://www.cine.io/api/1/-";
    private String secretKey;

    public CineIoClient(String secretKey){
        Log.v(TAG, secretKey);
        this.secretKey = secretKey;
    }

    public void broadcast(String id, final Context context){
        final Intent intent = new Intent(context, BroadcastActivity.class);

        getStream(id, new StreamResponseHandler(){
            public void onSuccess(Stream stream) {
                Log.d(TAG, "Starting publish intent: " + stream.getId());
                intent.putExtra("PUBLISH_URL", stream.getPublishUrl());
                context.startActivity(intent);
            }

        });
    }
    public void play(String id, final Context context){

        getStream(id, new StreamResponseHandler(){
            public void onSuccess(Stream stream) {
                Log.d(TAG, "Starting default play intent: " + stream.getId());

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(stream.getHLSUrl()), "video/*");
                context.startActivity(intent);
            }

        });
    }

    public void getProject(final ProjectResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/project";
        RequestParams rq = JsonToParams.toRequestParams(secretKey);
        client.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String response) {
                try {
                    Project project = new Project(new JSONObject(response));
                    handler.onSuccess(project);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void updateProject(JSONObject params, final ProjectResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/project";
        RequestParams rq = JsonToParams.toRequestParams(secretKey, params);
        client.put(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String response) {
                try {
                    Project project = new Project(new JSONObject(response));
                    handler.onSuccess(project);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void getStreams(final StreamsResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/streams";
        RequestParams rq = JsonToParams.toRequestParams(secretKey);
        client.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String response) {
                try {
                    ArrayList<Stream> streams = new ArrayList<Stream>();
                    JSONArray obj = new JSONArray(response);
                    for(int i = 0; i < obj.length(); i++){
                        Stream stream= new Stream(obj.getJSONObject(i));
                        streams.add(stream);
                    }
                    handler.onSuccess(streams);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void getStream(String id, final StreamResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(secretKey);
        rq.add("id", id);
        client.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String response) {
                try {
                    Stream stream= new Stream(new JSONObject(response));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void updateStream(String id, JSONObject params, final StreamResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(secretKey, params);
        rq.add("id", id);
        client.put(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Stream stream= new Stream(new JSONObject(response));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void createStream(final StreamResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(secretKey);
        client.post(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Stream stream= new Stream(new JSONObject(response));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void createStream(JSONObject params, final StreamResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(secretKey, params);
        client.post(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Stream stream= new Stream(new JSONObject(response));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

    public void deleteStream(String id, final StreamResponseHandler handler){
        AsyncHttpClient client = new AsyncHttpClient();
        String url = BASE_URL + "/stream?secretKey="+secretKey+ "&id="+id;
        client.delete(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(String response) {
                try {
                    Stream stream= new Stream(new JSONObject(response));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
        });
    }

}
