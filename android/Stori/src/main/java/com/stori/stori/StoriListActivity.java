package com.stori.stori;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListActivity extends ListActivity {
    public final static String TAG = "StoriListActivity";

    private String m_userUuid = null;
    private SharedPreferences m_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onCreate");

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, SSPreferences.PREFS(this), Context.MODE_PRIVATE, R.xml.settings_screen, false);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    protected void onDestroy() {
        if(D)Log.d(TAG, "StoriListActivity.onDestroy");

        super.onDestroy();
    }

    @Override
    protected void onStart() {
        if(D)Log.d(TAG, "StoriListActivity.onStart");

        super.onStart();
    }

    @Override
    protected void onStop() {
        if(D)Log.d(TAG, "StoriListActivity.onStop");

        super.onStop();
    }

    @Override
    protected void onResume() {
        if(D)Log.d(TAG, "StoriListActivity.onResume");

        super.onResume();
    }

    @Override
    protected void onPause() {
        if(D)Log.d(TAG, "StoriListActivity.onPause");

        super.onPause();
    }
}
