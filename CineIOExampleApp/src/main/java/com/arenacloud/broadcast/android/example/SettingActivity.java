package com.arenacloud.broadcast.android.example;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class SettingActivity extends ActionBarActivity {

    public static final String KEY_ROOMID = "ROOMID";
    public static final String KEY_PASSWORD = "PASSWORD";

    private final static String SECRET_KEY = "SECRET_KEY";

    private EditText roomIdEditText = null;
    private EditText passwordEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        roomIdEditText = (EditText)findViewById(R.id.username_edit);
        passwordEditText = (EditText)findViewById(R.id.password_edit);

        SharedPreferences sp = getSharedPreferences("USERINFO", MODE_PRIVATE);

        String oldRoomId = sp.getString(KEY_ROOMID, "");
        String oldPassword = sp.getString(KEY_PASSWORD,"");

        roomIdEditText.setText(oldRoomId);
        passwordEditText.setText(oldPassword);

        Button saveButton = (Button)findViewById(R.id.signin_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomId = roomIdEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                if (roomId!=null && !roomId.isEmpty())
                {
                    saveUserInfo(roomId, password);

                    Intent intent = new Intent(SettingActivity.this, ArenaCloudStreamViewActivity.class);
                    intent.putExtra("SECRET_KEY", SECRET_KEY);
                    startActivity(intent);
                }else{
                    Toast toast = Toast.makeText(SettingActivity.this,
                            "room id can't be empty", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }

            }
        });
    }

    private void saveUserInfo(String roomId, String password)
    {
        SharedPreferences.Editor sharedata = getSharedPreferences("USERINFO", MODE_PRIVATE).edit();
        sharedata.putString(KEY_ROOMID, roomId);
        sharedata.putString(KEY_PASSWORD, password);
        sharedata.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting, menu);
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
