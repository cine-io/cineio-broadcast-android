# cine.io android sdk

[![Build Status](https://travis-ci.org/cine-io/cineio-android.svg?branch=master)](https://travis-ci.org/cine-io/cineio-android)

The android module for [cine.io](cine.io).

## Installation

Currently being published to maven.

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
String SECRET_KEY = "YOUR_SECRET_KEY"
CineIoClient client = new CineIoClient(SECRET_KEY);
```

### Functions

#### Projects

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
JSONObject params = new JSONObject();
try {
  params.put("name", "a new stream");
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
String streamId = "STREAM_ID";
JSONObject params = new JSONObject();
try {
  params.put("name", "new name");
} catch (JSONException e) {}

client.updateStream(streamId, params, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

To delete a specific new stream:
```java
String streamId = "STREAM_ID";
client.deleteStream(streamId, new StreamResponseHandler(){
  @Override
  public void onSuccess(Stream stream) {
    //TODO handle stream
  }
});
```

## Contributing

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
