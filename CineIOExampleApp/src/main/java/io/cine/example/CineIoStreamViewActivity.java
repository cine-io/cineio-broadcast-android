package io.cine.example;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import io.cine.android.BroadcastConfig;
import io.cine.android.CineIoClient;
import io.cine.android.CineIoConfig;
import io.cine.android.api.Stream;

public class CineIoStreamViewActivity extends Activity {

//    private Stream stream;

    private final static String TAG = "CineIoStreamViewActivity";

    private CineIoClient mClient;

    private String roomId = null;
    private String password = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cine_io_stream_view);
        Bundle extras = getIntent().getExtras();
        CineIoConfig config = new CineIoConfig();
        config.setSecretKey(extras.getString("SECRET_KEY"));
        mClient = new CineIoClient(config);
//        try {
//            stream = new Stream(new JSONObject(extras.getString("STREAM_DATA")));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        SharedPreferences sp = getSharedPreferences("USERINFO", MODE_PRIVATE);
        roomId = sp.getString(SettingActivity.KEY_ROOMID, "");
        password = sp.getString(SettingActivity.KEY_PASSWORD,"");

        initLayout();
    }

    private void initLayout() {
        final Activity me = this;
        String labelText;
//        String name = stream.getName();
//        if (name != null) {
//            labelText = stream.getName() + ": " + stream.getId();
//        } else {
//            labelText = stream.getId();
//        }


        labelText = "ROOM ID : "+roomId;
        TextView v = (TextView) findViewById(R.id.streamName);
        v.setText(labelText);

        Button broadcastButton = (Button) findViewById(R.id.broadcastStream);
        broadcastButton.setText("Start Broadcaster");
        broadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d(TAG, "Starting broadcast for " + stream.getId());
                BroadcastConfig config = new BroadcastConfig();
                //TO SET A CUSTOM WIDTH AND HEIGHT
                config.setWidth(1280);
                config.setHeight(720);
                //TO LOCK AN ORIENTATION
//                config.lockOrientation("landscape");
                config.lockOrientation("portrait");
                //TO SELECT A CAMERA
                config.selectCamera("back");
                //TO CHANGE THE BROADCAST LAYOUT
                //config.setBroadcastActivityLayout(R.layout.my_activity_broadcast_capture);
                mClient.broadcast(/*stream.getId()*/roomId+":"+password, config, me);
            }
        });

//        Button playButton = (Button) findViewById(R.id.playStream);
//        playButton.setText("Start Player");
//        playButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "Starting player for " + stream.getId());
//                mClient.play(stream.getId(), me);
//            }
//        });
//
//        Button seeRecordingsButton = (Button) findViewById(R.id.showRecordings);
//        seeRecordingsButton.setText("Recordings");
//        seeRecordingsButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "Fetching recordings for " + stream.getId());
//                Intent intent = new Intent(me, CineIoStreamRecordingsListActivity.class);
//                intent.putExtra("STREAM_DATA", stream.dataString());
//                intent.putExtra("SECRET_KEY", mClient.getSecretKey());
//                startActivity(intent);
//            }
//        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cine_io_stream_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
