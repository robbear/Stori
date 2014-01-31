package com.stori.stori;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListActivity extends ListActivity {
    public final static String TAG = "StoriListActivity";

    public final static String INSTANCE_STATE_ORIENTATION_CHANGED = "instance_state_orientation_changed";
    public final static String INSTANCE_STATE_STORILISTITEMS = "instance_state_storilistitems";

    private String m_userUuid = null;
    private SharedPreferences m_prefs;
    private int m_orientation;
    private boolean m_fOrientationChanged = false;
    private ArrayList<StoriListItem> m_storiListItems = new ArrayList<StoriListItem>();
    private StoriService m_storiService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onCreate");

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, SSPreferences.PREFS(this), Context.MODE_PRIVATE, R.xml.settings_screen, false);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState != null) {
            m_fOrientationChanged = savedInstanceState.getBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, false);
            m_storiListItems = savedInstanceState.getParcelableArrayList(INSTANCE_STATE_STORILISTITEMS);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        m_fOrientationChanged = m_orientation != orientation;

        savedInstanceState.putBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, m_fOrientationChanged);
        savedInstanceState.putParcelableArrayList(INSTANCE_STATE_STORILISTITEMS, m_storiListItems);
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

        initializeStoriService();
    }

    @Override
    protected void onStop() {
        if(D)Log.d(TAG, "StoriListActivity.onStop");

        super.onStop();

        uninitializeStoriService();
    }

    @Override
    protected void onResume() {
        if(D)Log.d(TAG, "StoriListActivity.onResume");

        super.onResume();

        m_orientation = getResources().getConfiguration().orientation;
        if(D)Log.d(TAG, String.format("StoriListActivity.onResume: orientation = %d", m_orientation));
    }

    @Override
    protected void onPause() {
        if(D)Log.d(TAG, "StoriListActivity.onPause");

        super.onPause();
    }

    private class ReadStoriItemsTask extends AsyncTask<Object, Void, StoriListItem[]> {
        @Override
        public StoriListItem[] doInBackground(Object... params) {
            if(D)Log.d(TAG, "StoriListActivity.ReadStoriItemsTask.doInBackground");

            return null;
        }

        @Override
        public void onPostExecute(StoriListItem[] items) {
            if(D)Log.d(TAG, "StoriListActivity.ReadStoriItemsTask.onPostExecute");
        }
    }

    private void readStoriItemsAsync() {
        if(D)Log.d(TAG, "StoriListActivity.readStoriItemsAsync");

        try {
            new ReadStoriItemsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriListActivity.readStoriItemsAsync", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriListActivity.readStoriItemsAsync", e);
            e.printStackTrace();
        }
    }

    protected void initializeStoriService()
    {
        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService");

        Intent service = new Intent(this, StoriService.class);

        // Call startService always, unless we are dealing with an orientation change.
        if (!m_fOrientationChanged) {
            if(D)Log.d(TAG, "StoriListActivity.initializeStoriService - calling startService in order to stay connected due to orientation change");
            startService(service);
        }

        m_fOrientationChanged = false;

        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService - calling bindService");
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeStoriService()
    {
        if(D)Log.d(TAG, String.format("StoriListActivity.uninitializeStoriService: m_fOrientationChanged=%b", m_fOrientationChanged));

        // Always call unbindService
        if (m_storiService != null && m_connection != null)
        {
            if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService - calling unbindService");
            unbindService(m_connection);
        }

        // Call stopService if we're not dealing with an orientation change
        if (!m_fOrientationChanged)
        {
            if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService - calling stopService");
            Intent service = new Intent(this, StoriService.class);
            stopService(service);
        }

        m_storiService = null;
    }

    public ServiceConnection m_connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceConnected");

            m_storiService = ((StoriService.StoriServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceDisconnected");

            m_storiService = null;
        }
    };
}
