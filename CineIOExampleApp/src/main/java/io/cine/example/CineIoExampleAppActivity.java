package io.cine.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import io.cine.android.CineIoClient;
import io.cine.android.api.Stream;
import io.cine.android.api.StreamsResponseHandler;

public class CineIoExampleAppActivity extends Activity {

    private final static String TAG = "CineIoConsumer";
    private final static String SECRET_KEY = "SECRET_KEY";

    private CineIoClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cine_io_consumer);
        final CineIoExampleAppActivity me = this;
        mClient = new CineIoClient(SECRET_KEY);
        mClient.getStreams(new StreamsResponseHandler(){
            @Override
            public void onSuccess(ArrayList<Stream> streams) {
                me.setStreams(streams);
            }
        });
    }

    public void setStreams(ArrayList<Stream> streams){
        final CineIoExampleAppActivity me = this;
        LinearLayout layout = (LinearLayout) findViewById(R.id.streamBroadcasts);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        for(final Stream stream : streams){
            String labelText;
            String name = stream.getName();
            if (name != null){
                labelText = stream.getName() + ": " + stream.getId();
            }else{
                labelText = stream.getId();
            }
            TextView v = new TextView(this);
            v.setText(labelText);
            v.setLayoutParams(wrapParams);
            layout.addView(v);

            Button broadcastButton = new Button(this);
            broadcastButton.setText("Start Broadcaster");
            broadcastButton.setLayoutParams(wrapParams);
            broadcastButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Starting broadcast for " + stream.getId());
                    mClient.broadcast(stream.getId(), me);
                }
            });
            layout.addView(broadcastButton);

            Button playButton = new Button(this);
            playButton.setText("Start Player");
            playButton.setLayoutParams(wrapParams);
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Starting player for " + stream.getId());
                    mClient.play(stream.getId(), me);
                }
            });
            layout.addView(playButton);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.cine_io_consumer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

}
