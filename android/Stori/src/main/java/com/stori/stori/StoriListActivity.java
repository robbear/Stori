package com.stori.stori;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stori.stori.adapters.StoriListAdapter;
import com.stori.stori.cloudproviders.AmazonSharedPreferencesWrapper;

import java.util.ArrayList;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListActivity extends ListActivity implements StoriService.ReadStoriItemsStateListener {
    public final static String TAG = "StoriListActivity";

    private String m_userUuid = null;
    private StoriListAdapter m_adapter;
    private SharedPreferences m_prefs;
    private StoriService m_storiService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onCreate");

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, SSPreferences.PREFS(this), Context.MODE_PRIVATE, R.xml.settings_screen, false);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);

        m_adapter = new StoriListAdapter(this, m_userUuid);
        setListAdapter(m_adapter);

        initializeStoriService();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
    }

    @Override
    protected void onDestroy() {
        if(D)Log.d(TAG, "StoriListActivity.onDestroy");

        super.onDestroy();

        uninitializeStoriService();
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

    public void onReadStoriItemsComplete(ArrayList<StoriListItem> storiListItems) {
        if(D)Log.d(TAG, "StoriListActivity.onReadStoriItemsComplete");

        if (storiListItems == null) {
            if(D)Log.d(TAG, "StoriListActivity.onReadStoriItemsComplete: storiListItems is null");
        }
        else {
            if(D)Log.d(TAG, String.format("StoriListActivity.onReadStoriItemsComplete: found %d items", storiListItems.size()));
        }

        if (storiListItems != null) {
            for (int i = 0; i < storiListItems.size(); i++) {
                StoriListItem sli = storiListItems.get(i);
                if(D)Log.d(TAG, String.format("****** slideShareName=%s, title=%s, slideCount=%d, date=%s", sli.getSlideShareName(), sli.getTitle(), sli.getSlideCount(), sli.getModifiedDate()));
            }
        }

        m_adapter.setStoriListItems(storiListItems);
        m_adapter.notifyDataSetChanged();
        getListView().requestFocus();
    }

    protected void initializeStoriService()
    {
        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService");

        Intent service = new Intent(this, StoriService.class);

        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService - calling startService and bindService");
        startService(service);
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeStoriService()
    {
        if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService");

        if (m_storiService != null) {
            m_storiService.unregisterReadStoriItemsStateListener(this);
        }

        // Always call unbindService
        if (m_storiService != null && m_connection != null)
        {
            if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService - calling unbindService");
            unbindService(m_connection);
        }

        m_storiService = null;
    }

    public ServiceConnection m_connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceConnected");

            m_storiService = ((StoriService.StoriServiceBinder)service).getService();
            m_storiService.registerReadStoriItemsStateListener(StoriListActivity.this);

            ArrayList<StoriListItem> sli = m_storiService.getStoriListItems();
            if (sli == null || sli.size() <= 0) {
                if(D)Log.d(TAG, "StoriListActivity.onServiceConnected - no StoriListItems, so asking StoriService to update");
                m_storiService.readStoriItemsAsync(StoriListActivity.this, m_userUuid);
            }
            else {
                if(D)Log.d(TAG, "StoriListActivity.onServiceConnected - using StoriService StoriListItems cache");
                onReadStoriItemsComplete(sli);
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceDisconnected");

            m_storiService = null;
        }
    };
}
