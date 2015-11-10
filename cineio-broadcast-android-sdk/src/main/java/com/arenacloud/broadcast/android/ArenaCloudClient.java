package com.arenacloud.broadcast.android;

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

import com.arenacloud.broadcast.android.api.JsonToParams;
import com.arenacloud.broadcast.android.api.Project;
import com.arenacloud.broadcast.android.api.ProjectResponseHandler;
import com.arenacloud.broadcast.android.api.ProjectsResponseHandler;
import com.arenacloud.broadcast.android.api.Stream;
import com.arenacloud.broadcast.android.api.StreamRecording;
import com.arenacloud.broadcast.android.api.StreamRecordingResponseHandler;
import com.arenacloud.broadcast.android.api.StreamRecordingsResponseHandler;
import com.arenacloud.broadcast.android.api.StreamResponseHandler;
import com.arenacloud.broadcast.android.api.StreamsResponseHandler;

import com.arenacloud.broadcast.android.ArenaCloudConfig;

public class ArenaCloudClient {

    private final String VERSION = "0.0.15";
    private final static String TAG = "ArenaCloudClient";
    private final static String BASE_URL = "http://api.arenacloud.com/broadcast/1/-";
    private final AsyncHttpClient mClient;
    private ArenaCloudConfig mConfig;



    public ArenaCloudClient(ArenaCloudConfig config){
        this.mConfig = config;
        this.mClient = new AsyncHttpClient();
        mClient.setUserAgent("arenacloud-broadcast-android version-"+VERSION);
    }

    public String getSecretKey() {
        return mConfig.getSecretKey();
    }

    public String getPublicKey() {
        return mConfig.getPublicKey();
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


    public void broadcast(String id, String password, final BroadcastConfig config,  final Context context){
        final Intent intent = new Intent(context, /*BroadcastActivity*/ArenaCloudBroadcastActivity.class);

        getStreamWithPassword(id, password, new StreamResponseHandler() {
            public void onSuccess(Stream stream) {
                // Log.d(TAG, "Starting publish intent: " + stream.getId());
                intent.putExtra("PUBLISH_URL", stream.getPublishUrl());

                // for test
//                intent.putExtra("PUBLISH_URL","rtmp://wspub.live.hupucdn.com/prod/17f21a6c0ac6bd89ad035bc685520ad0");

                if (config.getWidth() != -1) {
                    intent.putExtra("WIDTH", config.getWidth());
                }
                if (config.getHeight() != -1) {
                    intent.putExtra("HEIGHT", config.getHeight());
                }
                if (config.getLockedOrientation() != null) {
                    intent.putExtra("ORIENTATION", config.getLockedOrientation());
                }
                if (config.getRequestedCamera() != null) {
                    intent.putExtra("CAMERA", config.getRequestedCamera());
                }
                if (config.getBroadcastActivityLayout() != -1) {
                    intent.putExtra("LAYOUT", config.getBroadcastActivityLayout());
                }
                context.startActivity(intent);
            }

        });

/*        // Log.d(TAG, "Starting publish intent: " + stream.getId());
        intent.putExtra("PUBLISH_URL", "rtmp://wspub.live.hupucdn.com/prod/slk");
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
        context.startActivity(intent);*/
    }

    //pass in custom values
    public void broadcast(String id, final BroadcastConfig config,  final Context context){
        final Intent intent = new Intent(context, BroadcastActivity.class);

        getStream(id, new StreamResponseHandler() {
            public void onSuccess(Stream stream) {
                // Log.d(TAG, "Starting publish intent: " + stream.getId());
                intent.putExtra("PUBLISH_URL", stream.getPublishUrl());
                if (config.getWidth() != -1) {
                    intent.putExtra("WIDTH", config.getWidth());
                }
                if (config.getHeight() != -1) {
                    intent.putExtra("HEIGHT", config.getHeight());
                }
                if (config.getLockedOrientation() != null) {
                    intent.putExtra("ORIENTATION", config.getLockedOrientation());
                }
                if (config.getRequestedCamera() != null) {
                    intent.putExtra("CAMERA", config.getRequestedCamera());
                }
                if (config.getBroadcastActivityLayout() != -1) {
                    intent.putExtra("LAYOUT", config.getBroadcastActivityLayout());
                }
                context.startActivity(intent);
            }

        });

/*        // Log.d(TAG, "Starting publish intent: " + stream.getId());
        intent.putExtra("PUBLISH_URL", "rtmp://wspub.live.hupucdn.com/prod/slk");
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
        context.startActivity(intent);*/
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

    public void play(String id, String ticket, final Context context){
        final Intent intent = new Intent(context, PlayActivity.class);

        getStreamWithTicket(id, ticket, new StreamResponseHandler() {
            public void onSuccess(Stream stream) {
                Log.d(TAG, "Starting default play intent: " + stream.getId());

                intent.putExtra("PLAY_URL_RTMP", stream.getRtmpUrl());
                intent.putExtra("PLAY_URL_HLS", stream.getHLSUrl());
                intent.putExtra("PLAY_TTL", stream.getPlayTTL());
                intent.putExtra("SNAPSHOT", stream.getSnapShotUrl());

                context.startActivity(intent);
            }
        });
    }
/*
    public void play(String id, final Context context){

        getStream(id, new StreamResponseHandler() {
            public void onSuccess(Stream stream) {
                Log.d(TAG, "Starting default play intent: " + stream.getId());

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(stream.getHLSUrl()), "video/*");
                context.startActivity(intent);
            }

        });
    }
*/
    public void playRecording(final String id, final String recordingName, final Context context){
        getStreamRecordings(id, new StreamRecordingsResponseHandler() {
            @Override
            public void onSuccess(ArrayList<StreamRecording> streamRecordings) {
                String recordingUrl = null;
                for (int i = 0; i < streamRecordings.size(); i++) {
                    StreamRecording recording = streamRecordings.get(i);
                    Log.d(TAG, "RECORDING");
                    Log.d(TAG, recordingName);
                    Log.d(TAG, recording.getName());
                    Log.d(TAG, recording.getName().equals(recordingName) ? "EQUAL" : "NOT EQUAL");
                    if (recording.getName().equals(recordingName)) {
                        recordingUrl = recording.getUrl();
                        break;
                    }
                }
                if (recordingUrl == null) {
                    Exception e = new Exception("recordingUrl not found for name: " + recordingName + " and Stream id: " + id);
                    onFailure(e);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(recordingUrl), "video/*");
                    context.startActivity(intent);
                }
            }
        });
    }
    /*
    public void playRecording(StreamRecording recording, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(recording.getUrl()), "video/*");
        context.startActivity(intent);
    }*/

    public void playRecording(StreamRecording recording, Context context) {
        Intent intent = new Intent(context, PlayActivity.class);

        intent.putExtra("PLAY_URL_RTMP", recording.getUrl());

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
                    for (int i = 0; i < obj.length(); i++) {
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
                    for (int i = 0; i < obj.length(); i++) {
                        Stream stream = new Stream(obj.getJSONObject(i));
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

    public void getStreamRecordingsWithPassword(String id, String password, final StreamRecordingsResponseHandler handler){
        String url = BASE_URL + "/stream/recordings";
        RequestParams rq = JsonToParams.toRequestParamsWithPublicKey(getPublicKey());
        rq.add("id", id);
        rq.add("password", password);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<StreamRecording> streamRecordings = new ArrayList<StreamRecording>();
                    JSONArray obj = new JSONArray(new String(response));
                    for (int i = 0; i < obj.length(); i++) {
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

    public void getStreamRecordingsWithTicket(String id, String ticket, final StreamRecordingsResponseHandler handler)
    {
        String url = BASE_URL + "/stream/recordings";
        RequestParams rq = JsonToParams.toRequestParamsWithPublicKey(getPublicKey());
        rq.add("id", id);
        rq.add("ticket", ticket);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    ArrayList<StreamRecording> streamRecordings = new ArrayList<StreamRecording>();
                    JSONArray obj = new JSONArray(new String(response));
                    for (int i = 0; i < obj.length(); i++) {
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
                    for (int i = 0; i < obj.length(); i++) {
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
                    StreamRecording streamRecording = new StreamRecording(new JSONObject(new String(response)));
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

    public void getStreamWithTicket(String id, String ticket, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParamsWithPublicKey(getPublicKey());
        rq.add("id", id);
        rq.add("ticket",ticket);
        mClient.get(url, rq, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                try {
                    Stream stream = new Stream(new JSONObject(new String(response)));
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

    public void getStreamWithPassword(String id, String password, final StreamResponseHandler handler){
        String url = BASE_URL + "/stream";
        RequestParams rq = JsonToParams.toRequestParamsWithPublicKey(getPublicKey());
        rq.add("id", id);
        rq.add("password",password);
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
