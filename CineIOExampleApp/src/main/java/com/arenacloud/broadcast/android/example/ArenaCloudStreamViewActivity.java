package com.arenacloud.broadcast.android.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.arenacloud.broadcast.android.ArenaCloudClient;
import com.arenacloud.broadcast.android.BroadcastConfig;
import com.arenacloud.broadcast.android.ArenaCloudConfig;

public class ArenaCloudStreamViewActivity extends Activity {

//    private Stream stream;

    private final static String TAG = "ArenaCloudStreamViewActivity";

    private ArenaCloudClient mClient;

//    private String roomId = null;
//    private String password = null;

    private String publicKey = "1c76aec81ad398126f5f5d2eda531c89";
    private String id = "41667e77ad0cf2d72e100223";
    private String password = "c399a7a8";
    private String ticket = "ZT0xNDc1ODM5NTkwJmM9MTIzNDU2Nzg5MCZzPURLQ1Q1SGxyUFR4cnlkem1hcXF2VkMyWjVoczRWcGxVTzVwbXJ2UHk5YUE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arena_cloud_stream_view);
//        Bundle extras = getIntent().getExtras();
        ArenaCloudConfig config = new ArenaCloudConfig();
//        config.setSecretKey(extras.getString("SECRET_KEY"));
        config.setPublicKey(publicKey);
        mClient = new ArenaCloudClient(config);
//        try {
//            stream = new Stream(new JSONObject(extras.getString("STREAM_DATA")));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

//        SharedPreferences sp = getSharedPreferences("USERINFO", MODE_PRIVATE);
//        roomId = sp.getString(SettingActivity.KEY_ROOMID, "");
//        password = sp.getString(SettingActivity.KEY_PASSWORD,"");

        initLayout();
    }

    private void initLayout() {
        final Activity me = this;
//        String labelText;
//        String name = stream.getName();
//        if (name != null) {
//            labelText = stream.getName() + ": " + stream.getId();
//        } else {
//            labelText = stream.getId();
//        }


//        labelText = "ROOM ID : "+roomId;
//        TextView v = (TextView) findViewById(R.id.streamName);
//        v.setText(labelText);

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
                mClient.broadcast(id,password,config, me);
            }
        });

        Button playButton = (Button) findViewById(R.id.playStream);
        playButton.setText("Start Player");
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Log.d(TAG, "Starting player for " + stream.getId());
                mClient.play(id, ticket, me);
            }
        });
//
//        Button seeRecordingsButton = (Button) findViewById(R.id.showRecordings);
//        seeRecordingsButton.setText("Recordings");
//        seeRecordingsButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.d(TAG, "Fetching recordings for " + stream.getId());
//                Intent intent = new Intent(me, ArenaCloudStreamRecordingsListActivity.class);
//                intent.putExtra("STREAM_DATA", stream.dataString());
//                intent.putExtra("SECRET_KEY", mClient.getSecretKey());
//                startActivity(intent);
//            }
//        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.arena_cloud_stream_view, menu);
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
