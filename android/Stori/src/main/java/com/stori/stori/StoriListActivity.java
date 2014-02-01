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

    public final static String INSTANCE_STATE_ORIENTATION_CHANGED = "instance_state_orientation_changed";

    private String m_userUuid = null;
    private StoriListAdapter m_adapter;
    private SharedPreferences m_prefs;
    private int m_orientation;
    private boolean m_fOrientationChanged = false;
    private StoriService m_storiService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onCreate");

        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, SSPreferences.PREFS(this), Context.MODE_PRIVATE, R.xml.settings_screen, false);
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);

        if (savedInstanceState != null) {
            m_fOrientationChanged = savedInstanceState.getBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, false);
        }

        m_adapter = new StoriListAdapter(this);
        setListAdapter(m_adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        int orientation = getResources().getConfiguration().orientation;
        m_fOrientationChanged = m_orientation != orientation;

        savedInstanceState.putBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, m_fOrientationChanged);
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

        // Call startService always, unless we are dealing with an orientation change.
        if (!m_fOrientationChanged) {
            if(D)Log.d(TAG, "StoriListActivity.initialzeStoriService - calling startService in order to stay connected due to orientation change");
            startService(service);
        }

        m_fOrientationChanged = false;

        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService - calling bindService");
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeStoriService()
    {
        if(D)Log.d(TAG, String.format("StoriListActivity.uninitializeStoriService: m_fOrientationChanged=%b", m_fOrientationChanged));

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
