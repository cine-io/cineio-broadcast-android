package com.arenacloud.broadcast.android;

import android.content.pm.ActivityInfo;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnCompletionListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnErrorListener;
import tv.danmaku.ijk.media.player.IMediaPlayer.OnPreparedListener;
import tv.danmaku.ijk.media.widget.VideoView;


public class PlayActivity extends ActionBarActivity {

    private String playUrl_rtmp = "";
    private String playUrl_hls = "";
    private int playTtl = 0;

    private VideoView mVideoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Bundle extras = getIntent().getExtras();
        playUrl_rtmp = extras.getString("PLAY_URL_RTMP");
        playUrl_hls = extras.getString("PLAY_URL_HLS");
        playTtl = extras.getInt("PLAY_TTL");

        setContentView(R.layout.activity_play);

        mVideoView = (VideoView) findViewById(R.id.video_view);

        mVideoView.setOnPreparedListener(mPreparedListener);
        mVideoView.setOnErrorListener(mErrorListener);
        mVideoView.setOnCompletionListener(mCompletionListener);

//        mVideoView.setDataSourceType(VideoView.LOWDELAY_LIVE_STREAMING_TYPE);
        mVideoView.setDataCache(10000);

        mVideoView.setVideoPath(playUrl_rtmp);

        // http://v.iask.com/v_play_ipad.php?vid=99264895
        //for test
//        mVideoView.setVideoPath("http://v.iask.com/v_play_ipad.php?vid=99264895");
    }

    private OnPreparedListener mPreparedListener = new OnPreparedListener() {

        @Override
        public void onPrepared(IMediaPlayer mp) {
        }
    };

    private OnErrorListener mErrorListener = new OnErrorListener() {
        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            mVideoView.stopPlayback();
            finish();
            return true;
        }
    };

    private OnCompletionListener mCompletionListener = new OnCompletionListener() {
        @Override
        public void onCompletion(IMediaPlayer mp) {
            mVideoView.stopPlayback();
            finish();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_play, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
