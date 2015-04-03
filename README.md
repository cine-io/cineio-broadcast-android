# cine.io android sdk

[![Build Status](https://travis-ci.org/cine-io/cineio-broadcast-android.svg?branch=master)](https://travis-ci.org/cine-io/cineio-broadcast-android)

The [android library](http://mvnrepository.com/artifact/io.cine/cineio-broadcast-android-sdk) for [cine.io broadcast](https://www.cine.io/products/broadcast).

> This library is unstable on Android 5.0+ (Lollipop). We apologize for the inconvenience. If you fix it before we do, we love [pull requests](#contributing).

## Installation

Add the following to your `build.gradle`.

```groovy
dependencies {
  compile 'io.cine:cineio-broadcast-android-sdk:0.0.15'
}
```

Ensure [Maven central](http://search.maven.org/) is included in your `build.gradle`. This should happen by default when building a project with Google's recommended Android IDE, [Android Studio](https://developer.android.com/sdk/installing/studio.html).

```
apply plugin: 'android'
buildscript {
  repositories {
    mavenCentral()
  }
}
repositories {
  mavenCentral()
}
```

Download cineio-broadcast-android-sdk to your application with `./gradlew build`.

Then we need to let your application know about the cine.io `BroadcastActivity`.

Add the following to permissions your `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<uses-feature android:glEsVersion="0x00020000" android:required="true" />
```

Add the following Activity your `AndroidManifest.xml` within the `<application>` tag.
```xml
<activity
  android:name="io.cine.android.BroadcastActivity"
  android:label="CineIOBroadcastActivity" >
</activity>
```

For a complete example checkout our [example application AndroidManifest.xml](https://github.com/cine-io/cineio-broadcast-android/blob/master/CineIOExampleApp/src/main/AndroidManifest.xml).

Now you're all set up!

## Usage

### Initialization

```java
import io.cine.android.*;

// Some other potential imports:
// import org.json.JSONException;
// import org.json.JSONObject;
// import java.util.ArrayList;
```

```java
String SECRET_KEY = "YOUR_SECRET_KEY";
// String MASTER_KEY = "YOUR_MASTER_KEY";
CineIoConfig config = new CineIoConfig();
config.setSecretKey(SECRET_KEY);
// config.setMasterKey(MASTER_KEY);
CineIoClient client = new CineIoClient(config);
```

### Broadcast a live stream

Broadcasting a live stream will launch the Cine.io BroadcastActivity. If users pause your Application during a live stream and return to it, they will be placed back into the BroadcastActivity. They can return to the parent Activity by pressing the back button.

```java
String streamId = "STREAM_ID";
client.broadcast(streamId, this);
// `this` is a Context, such as your instance of an Activity.
```

```java
// To use a custom width, height, and orientation
import io.cine.android.BroadcastConfig;

String streamId = "STREAM_ID";
BroadcastConfig config = new BroadcastConfig();
config.setWidth(640);
config.setHeight(480);
config.lockOrientation("landscape"); //values are "landscape" and "portrait". Not setting the value will allow the view to switch depending on device orientation
config.selectCamera("back"); //values are "back" and "front". Not setting the value will default to front facing
config.setBroadcastActivityLayout(R.layout.my_activity_broadcast_capture); // change the layout used for the broadcast activity

client.broadcast(streamId, config, this);
// `this` is a Context, such as your instance of an Activity.
```

### Play

Playing a live stream will launch the default Video Player a user has configured, or ask the user which application to launch for playing a live video.
Most android phones come with a default video player.

```java
String streamId = "STREAM_ID";
client.play(streamId, this);
// this is a Context, such as your instance of an Activity.
```

If you wish to build your own video player, here is some sample code to get the [HLS](http://en.wikipedia.org/wiki/HTTP_Live_Streaming) url.
```java
client.getStream(id, new StreamResponseHandler(){
  public void onSuccess(Stream stream) {
    String hlsUrl = stream.getHLSUrl();
    // start your video player here with the hlsUrl
  }
});
```

### Play a stream Recording

Playing a stream recording will launch the default Video Player a user has configured, or ask the user which application to launch for playing a recorded video.
Most android phones come with a default video player.

```java
// With a recordingName
String streamId = "STREAM_ID";
String recordingName = "recordingName";
client.playRecording(streamId, recordingName, this);
// this is a Context, such as your instance of an Activity.
```

```java
// With a stringRecording object (such as returned from getStreamRecordings)
String streamId = "STREAM_ID";
StreamRecording streamRecording;
client.playRecording(streamId, streamRecording, this);
// this is a Context, such as your instance of an Activity.
```

If you wish to build your own video player, here is some sample code to get the MP4 url.
```java
String streamId = "STREAM_ID";
client.getStreamRecordings(streamId, new StreamRecordingsResponseHandler(){
  public void onSuccess(ArrayList<StreamRecording> streamRecordings) {
    if (streamRecordings.size() == 0){
      //TODO handle no recording
      return;
    }
    String url = streamRecordings.get(0).getUrl(); //defaulting to the first recording
    //TODO start your video player here with the mp4Url
  }
});
```

### API calls

#### Projects

To get data about your projects:

```java
client.getProjects(new ProjectsResponseHandler(){
  @Override
  public void onSuccess(ArrayList<Project> projects) {
      //TODO handle projects
  }
});
```

To get data about your project:

```java
client.getProject(new ProjectResponseHandler(){
  @Override
  public void onSuccess(Project project) {
      //TODO handle project
  }
});
```

To update your project attributes:

```java
JSONObject params = new JSONObject();
try {
  params.put("name", "new name");
} catch (JSONException e) {}

client.updateProject(params, new ProjectResponseHandler(){
  @Override
  public void onSuccess(Project project) {
      //TODO handle project
  }
});
```

#### Streams

To get all your streams:

```java
client.getStreams(new StreamsResponseHandler(){
  @Override
  public void onSuccess(ArrayList<Stream> streams) {
    //TODO handle streams
  }
});
```

To get all your streams by name:

```java
JSONObject params = new JSONObject();
try {
  params.put("name", "a stream name");
} catch (JSONException e) {}

client.getStreams(params, new StreamsResponseHandler(){
  @Override
  public void onSuccess(ArrayList<Stream> streams) {
    //TODO handle streams
  }
});
```

To get a specific stream:

```java
String streamId = "STREAM_ID";

client.getStream(streamId, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
}
```

To create a new stream:

```java
client.createStream(new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

```java
// can optionally take params
// params:
//  name: 'an optional stream name'
//  record: true|false (default false). record: true will save recordings of all streaming sessions
JSONObject params = new JSONObject();
try {
  params.put("name", "a new stream");
  params.put("record", true);
} catch (JSONException e) {}

client.createStream(params, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

To update a stream:

```java
// params:
//  name: 'a new stream name'
//  record: true|false (updating a stream from true to false will not delete old stream recordings)
String streamId = "STREAM_ID";
JSONObject params = new JSONObject();
try {
  params.put("name", "new name");
  params.put("record", true);
} catch (JSONException e) {}

client.updateStream(streamId, params, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

To delete a stream:

```java
String streamId = "STREAM_ID";
client.deleteStream(streamId, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

#### Stream Recordings

To get all the recordings for a stream:

```java
String streamId = "STREAM_ID";
client.getStreamRecordings(streamId, new StreamsRecordingsResponseHandler(){
  @Override
  public void onSuccess(ArrayList<StreamRecording> streamRecordings) {
    //TODO handle streamRecordings
  }
});
```

To delete a stream recording:

```java
String streamId = "STREAM_ID";
String recordingName = "recording-name";
client.deleteStreamRecording(streamId, recordingName, new StreamRecordingResponseHandler(){
  @Override
  public void onSuccess(StreamRecording streamRecording) {
    //TODO handle deleted stream recording
  }
});
```

## Thanks

* Thanks [Google Grafika](https://github.com/google/grafika) for their example application of recording a [SurfaceTexture](http://developer.android.com/reference/android/graphics/SurfaceTexture.html) to a [MediaMuxer](http://developer.android.com/reference/android/media/MediaMuxer.html).
* Thanks to [Kickflip](https://github.com/Kickflip/kickflip-android-sdk/) for their example of using [AudioRecord](http://developer.android.com/reference/android/media/AudioRecord.html) in a [Thread](http://developer.android.com/reference/java/lang/Thread.html).
* Thanks to [FFmpeg Muxing example](https://ffmpeg.org/doxygen/trunk/muxing_8c-source.html) for their example of writing individual frames to [ffmpeg](https://ffmpeg.org/).
* Thanks to [lgorse](https://github.com/lgorse) for various contributions including screenshot saving.

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
