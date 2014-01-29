package com.stori.stori;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class SettingsActivity extends PreferenceActivity {
    public final static String TAG = "SettingsActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "SettingsActivity.onCreate");

        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public class SettingsFragment extends PreferenceFragment {
        public final static String TAG = "SettingsFragment";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(D)Log.d(TAG, "SettingsFragment.onCreate");

            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screen);
        }
    }
}
