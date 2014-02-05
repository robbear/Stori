package com.stori.stori;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stori.stori.cloudproviders.AmazonSharedPreferencesWrapper;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class SettingsActivity extends Activity {
    public final static String TAG = "SettingsActivity";

    private SharedPreferences m_prefs;
    private LinearLayout m_autoPlayPanel;
    private CheckBox m_autoPlayCB;
    private RelativeLayout m_aboutPanel;
    private TextView m_buildStringText;
    private RelativeLayout m_switchAccountPanel;
    private TextView m_currentAccountText;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(D) Log.d(TAG, "SettingsActivity.onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home:
                if(D)Log.d(TAG, "SettingsActivity.onOptionsItemSelected: up button clicked. Finishing activity.");
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "SettingsActivity.onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        m_prefs = getSharedPreferences(SSPreferences.PREFS(this), Context.MODE_PRIVATE);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.settings_actionbar));
        actionBar.setDisplayHomeAsUpEnabled(true);

        m_autoPlayCB = (CheckBox)findViewById(R.id.settings_autoaudio_checkbox);
        m_autoPlayCB.setChecked(m_prefs.getBoolean(SSPreferences.PREFS_PLAYSLIDESAUTOAUDIO(this), SSPreferences.DEFAULT_PLAYSLIDESAUTOAUDIO(this)));
        m_autoPlayCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor edit = m_prefs.edit();
                edit.putBoolean(SSPreferences.PREFS_PLAYSLIDESAUTOAUDIO(SettingsActivity.this), isChecked);
                edit.commit();
            }
        });

        // Allow tapping on full line
        m_autoPlayPanel = (LinearLayout)findViewById(R.id.settings_autoplay_panel);
        m_autoPlayPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_autoPlayCB.toggle();
            }
        });

        m_buildStringText = (TextView)findViewById(R.id.settings_buildstring_text);
        m_buildStringText.setText(String.format(getString(R.string.settings_buildstring_format), Config.buildString));

        m_aboutPanel = (RelativeLayout)findViewById(R.id.settings_about_panel);
        m_aboutPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

        String email = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);
        m_currentAccountText = (TextView)findViewById(R.id.settings_current_account_text);
        m_currentAccountText.setText(String.format(getString(R.string.settings_currentaccount_format), email));

        m_switchAccountPanel = (RelativeLayout)findViewById(R.id.settings_account_panel);
        m_switchAccountPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO
            }
        });
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "SettingsActivity.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "SettingsActivity.onStart");

        super.onStart();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "SettingsActivity.onStop");

        super.onStop();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "SettingsActivity.onResume");

        super.onResume();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "SettingsActivity.onPause");

        super.onPause();
    }
}
