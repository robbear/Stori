package com.hyperfine.slideshare;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.hyperfine.slideshare.fragments.RecordFragment;

import java.io.File;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class TestRecordPlayActivity extends FragmentActivity {

    public final static String TAG = "TestRecordPlayActivity";

    private SharedPreferences m_prefs;
    private RecordFragment m_recordFragment;
    private File m_slideShareDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "TestRecordPlayActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_testrecordplay);

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        // BUGBUG TODO: Replace with dialog to create/fetch SlideShare name
        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "TestRecordPlayActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
        }

        FragmentManager fm = getSupportFragmentManager();
        m_recordFragment = (RecordFragment)fm.findFragmentByTag(RecordFragment.class.toString());
        if (m_recordFragment != null) {
            m_recordFragment.setSlideShareName(slideShareName);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "TestRecordPlayActivity.onAttachFragment");

        if (fragment instanceof RecordFragment) {
            if(D)Log.d(TAG, "TestRecordPlayActivity.onAttachFragment - found our RecordFragment");
            String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
            m_recordFragment = (RecordFragment)fragment;
            m_recordFragment.setSlideShareName(slideShareName);
         }
    }
}
