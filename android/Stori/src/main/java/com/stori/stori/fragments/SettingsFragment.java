package com.stori.stori.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.stori.stori.R;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class SettingsFragment extends PreferenceFragment {
    public final static String TAG = "SettingsFragment";

    public SettingsFragment() {
        if(D) Log.d(TAG, "SettingsFragment constructor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "SettingsFragment.onCreate");

        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_screen);
    }
}
