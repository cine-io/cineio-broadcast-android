package io.cine.android;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.cine.android.api.JsonToParams;
import io.cine.android.api.Project;
import io.cine.android.api.ProjectResponseHandler;
import io.cine.android.api.ProjectsResponseHandler;
import io.cine.android.api.Stream;
import io.cine.android.api.StreamRecording;
import io.cine.android.api.StreamRecordingResponseHandler;
import io.cine.android.api.StreamRecordingsResponseHandler;
import io.cine.android.api.StreamResponseHandler;
import io.cine.android.api.StreamsResponseHandler;

public class CineIoClient {

    private final String VERSION = "0.0.15";
    private final static String TAG = "CineIoClient";
    private final static String BASE_URL = "https://www.cine.io/api/1/-";
    private final AsyncHttpClient mClient;
    private CineIoConfig mConfig;



    public CineIoClient(CineIoConfig config){
        this.mConfig = config;
        this.mClient = new AsyncHttpClient();
        mClient.setUserAgent("cineio-broadcast-android version-"+VERSION);
    }

    public String getSecretKey() {
        return mConfig.getSecretKey();
    }

    // Use default config
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

    //pass in custom values
    public void broadcast(String id, final BroadcastConfig config,  final Context context){
        final Intent intent = new Intent(context, BroadcastActivity.class);

        getStream(id, new StreamResponseHandler(){
            public void onSuccess(Stream stream) {
               // Log.d(TAG, "Starting publish intent: " + stream.getId());
                intent.putExtra("PUBLISH_URL", stream.getPublishUrl());
                if(config.getWidth() != -1){
                    intent.putExtra("WIDTH", config.getWidth());
                }
                if(config.getHeight() != -1){
                    intent.putExtra("HEIGHT", config.getHeight());
                }
                if(config.getLockedOrientation() != null){
                    intent.putExtra("ORIENTATION", config.getLockedOrientation());
                }
                if(config.getRequestedCamera() != null){
                    intent.putExtra("CAMERA", config.getRequestedCamera());
                }
                if (config.getBroadcastActivityLayout() != -1){
                    intent.putExtra("LAYOUT", config.getBroadcastActivityLayout());
                }
                context.startActivity(intent);
            }

        });
    }

    //pass in custom values including custom BroadcastActivity
    public void broadcast(String id, final BroadcastConfig config,  final Context context, final Class<? extends BroadcastActivity> broadcastActivity){
        final Intent intent = new Intent(context, broadcastActivity);

        getStream(id, new StreamResponseHandler(){
            public void onSuccess(Stream stream) {
                // Log.d(TAG, "Starting publish intent: " + stream.getId());
                intent.putExtra("PUBLISH_URL", stream.getPublishUrl());
                if(config.getWidth() != -1){
                    intent.putExtra("WIDTH", config.getWidth());
                }
                if(config.getHeight() != -1){
                    intent.putExtra("HEIGHT", config.getHeight());
                }
                if(config.getLockedOrientation() != null){
                    intent.putExtra("ORIENTATION", config.getLockedOrientation());
                }
                if(config.getRequestedCamera() != null){
                    intent.putExtra("CAMERA", config.getRequestedCamera());
                }
                if (config.getBroadcastActivityLayout() != -1){
                    intent.putExtra("LAYOUT", config.getBroadcastActivityLayout());
                }
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

    public void playRecording(final String id, final String recordingName, final Context context){
        getStreamRecordings(id, new StreamRecordingsResponseHandler(){
            @Override
            public void onSuccess(ArrayList<StreamRecording> streamRecordings) {
                String recordingUrl = null;
                for (int i = 0; i < streamRecordings.size(); i++){
                    StreamRecording recording = streamRecordings.get(i);
                    Log.d(TAG, "RECORDING");
                    Log.d(TAG, recordingName);
                    Log.d(TAG, recording.getName());
                    Log.d(TAG, recording.getName().equals(recordingName) ? "EQUAL" : "NOT EQUAL");
                    if (recording.getName().equals(recordingName)){
                        recordingUrl = recording.getUrl();
                        break;
                    }
                }
                if (recordingUrl == null){
                    Exception e = new Exception("recordingUrl not found for name: "+ recordingName + " and Stream id: "+ id);
                    onFailure(e);
                }else{
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(recordingUrl), "video/*");
                    context.startActivity(intent);
                }
            }
        });
    }
    public void playRecording(StreamRecording recording, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(recording.getUrl()), "video/*");
        context.startActivity(intent);
    }

    public void getProject(final ProjectResponseHandler handler){
        String url = BASE_URL + "/project";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey());
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Project project = new Project(new JSONObject(new String(response)));
                    handler.onSuccess(project);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }
        });
    }

    public void getProjects(final ProjectsResponseHandler handler){
        String url = BASE_URL + "/projects";
        RequestParams rq = JsonToParams.toRequestParamsWithMasterKey(mConfig.getMasterKey());
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<Project> projects = new ArrayList<Project>();
                    JSONArray obj = new JSONArray(new String(response));
                    for(int i = 0; i < obj.length(); i++){
                        Project project = new Project(obj.getJSONObject(i));
                        projects.add(project);
                    }
                    handler.onSuccess(projects);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void updateProject(JSONObject params, final ProjectResponseHandler handler){
        String url = BASE_URL + "/project";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey(), params);
        mClient.put(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Project project = new Project(new JSONObject(new String(response)));
                    handler.onSuccess(project);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void getStreams(final StreamsResponseHandler handler){
        String url = BASE_URL + "/streams";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey());
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<Stream> streams = new ArrayList<Stream>();
                    JSONArray obj = new JSONArray(new String(response));
                    for(int i = 0; i < obj.length(); i++){
                        Stream stream= new Stream(obj.getJSONObject(i));
                        streams.add(stream);
                    }
                    handler.onSuccess(streams);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void getStreams(JSONObject params, final StreamsResponseHandler handler){
        String url = BASE_URL + "/streams";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey(), params);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<Stream> streams = new ArrayList<Stream>();
                    JSONArray obj = new JSONArray(new String(response));
                    for(int i = 0; i < obj.length(); i++){
                        Stream stream= new Stream(obj.getJSONObject(i));
                        streams.add(stream);
                    }
                    handler.onSuccess(streams);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void getStreamRecordings(String id, final StreamRecordingsResponseHandler handler){
        String url = BASE_URL + "/stream/recordings";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey());
        rq.add("id", id);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<StreamRecording> streamRecordings = new ArrayList<StreamRecording>();
                    JSONArray obj = new JSONArray(new String(response));
                    for(int i = 0; i < obj.length(); i++){
                        StreamRecording streamRecording = new StreamRecording(obj.getJSONObject(i));
                        streamRecordings.add(streamRecording);
                    }
                    handler.onSuccess(streamRecordings);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void deleteStreamRecording(String id, String recordingName, final StreamRecordingResponseHandler handler){
        String url = BASE_URL + "/stream/recording?secretKey="+getSecretKey()+ "&id="+id+"&name="+recordingName;
        mClient.delete(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    StreamRecording streamRecording= new StreamRecording(new JSONObject(new String(response)));
                    handler.onSuccess(streamRecording);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void getStream(String id, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey());
        rq.add("id", id);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream= new Stream(new JSONObject(new String(response)));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void updateStream(String id, JSONObject params, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey(), params);
        rq.add("id", id);
        mClient.put(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream= new Stream(new JSONObject(new String(response)));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void createStream(final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey());
        mClient.post(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream= new Stream(new JSONObject(new String(response)));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void createStream(JSONObject params, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParams(getSecretKey(), params);
        mClient.post(url, rq, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream= new Stream(new JSONObject(new String(response)));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

    public void deleteStream(String id, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream?secretKey="+getSecretKey()+ "&id="+id;
        mClient.delete(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream= new Stream(new JSONObject(new String(response)));
                    handler.onSuccess(stream);
                } catch (JSONException e) {
                    handler.onFailure(e);
                }
            }
            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                handler.onFailure(throwable);
            }

        });
    }

}
