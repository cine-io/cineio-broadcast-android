package io.cine.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import io.cine.android.CineIoClient;
import io.cine.android.CineIoConfig;
import io.cine.android.api.Stream;
import io.cine.android.api.StreamsResponseHandler;

public class CineIoExampleAppActivity extends Activity implements AdapterView.OnItemClickListener {

    private final static String TAG = "CineIoExampleAppActivity";
    private final static String SECRET_KEY = "SECRET_KEY";

    private CineIoClient mClient;
    private ListView streamListView;
    private ArrayList<Stream> mStreams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cine_io_consumer);
        if (SECRET_KEY.equals("SECRET_KEY")){
            CharSequence error = "SECRET_KEY must be set to a cine.io project's secret key. Register for one here: https://www.cine.io";
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
        streamListView = (ListView) findViewById(R.id.streamBroadcasts);
        CineIoConfig config = new CineIoConfig();
        config.setSecretKey(SECRET_KEY);
        mClient = new CineIoClient(config);
        mClient.getStreams(new StreamsResponseHandler(){
            @Override
            public void onSuccess(ArrayList<Stream> streams) {
                setStreams(streams);
            }
        });
    }

    public void setStreams(ArrayList<Stream> streams){
        mStreams = streams;
        final CineIoExampleAppActivity me = this;
        // This is the array adapter, it takes the context of the activity as a
        // first parameter, the type of list view as a second parameter and your
        // array as a third parameter.
        ArrayAdapter<Stream> arrayAdapter = new ArrayAdapter<Stream>(
                this,
                android.R.layout.simple_list_item_1,
                streams);

        streamListView.setAdapter(arrayAdapter);
        streamListView.setOnItemClickListener(this);
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Stream stream = mStreams.get(position);
        Intent intent = new Intent(this, CineIoStreamViewActivity.class);
        intent.putExtra("STREAM_DATA", stream.dataString());
        intent.putExtra("SECRET_KEY", mClient.getSecretKey());
        startActivity(intent);
    }
}
