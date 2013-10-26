package com.hyperfine.slideshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class MainActivity extends Activity {

    public final static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "MainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
        String userUuidString = prefs.getString(SSPreferences.PREFS_USERUUID, null);
        if (userUuidString == null) {
            UUID uuid = UUID.randomUUID();
            if(D)Log.d(TAG, String.format("MainActivity.onCreate - generated USERUUID=%s and setting PREFS_USERUUID", uuid.toString()));

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SSPreferences.PREFS_USERUUID, uuid.toString());
            editor.commit();
        }
        else {
            if(D)Log.d(TAG, String.format("MainActivity.onCreate - USERUUID=%s", userUuidString));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(D)Log.d(TAG, "MainActivity.onCreateOptionsMenu");

        super.onCreateOptionsMenu(menu);

        // BUGBUG
        MenuItem csa = menu.add("Create slides");
        csa.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, CreateSlidesActivity.class);
                MainActivity.this.startActivity(intent);
                return true;
            }
        });

        // BUGBUG
        MenuItem psa = menu.add("Play slides");
        psa.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, PlaySlidesActivity.class);
                MainActivity.this.startActivity(intent);
                return true;
            }
        });

        // BUGBUG - test menu item
        MenuItem trp = menu.add("TestRecordPlay");
        trp.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, TestRecordPlayActivity.class);
                MainActivity.this.startActivity(intent);
                return true;
            }
        });

        // BUGBUG - test menu item
        MenuItem tip = menu.add("TestImagePicker");
        tip.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, TestImagePickerActivity.class);
                MainActivity.this.startActivity(intent);
                return true;
            }
        });

        return true;
    }
}
