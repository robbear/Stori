package com.hyperfine.slideshare;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class MainActivity extends Activity {

    public final static String TAG = "MainActivity";

    private SharedPreferences m_prefs;
    private Button m_buttonCreate;
    private Button m_buttonEdit;
    private Button m_buttonPreview;
    private String m_slideShareTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "MainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
        String userUuidString = m_prefs.getString(SSPreferences.PREFS_USERUUID, null);
        if (userUuidString == null) {
            UUID uuid = UUID.randomUUID();
            if(D)Log.d(TAG, String.format("MainActivity.onCreate - generated USERUUID=%s and setting PREFS_USERUUID", uuid.toString()));

            SharedPreferences.Editor editor = m_prefs.edit();
            editor.putString(SSPreferences.PREFS_USERUUID, uuid.toString());
            editor.commit();
        }
        else {
            if(D)Log.d(TAG, String.format("MainActivity.onCreate - USERUUID=%s", userUuidString));
        }

        String currentSlideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
        if(D)Log.d(TAG, String.format("MainActivity.onCreate: currentSlideShareName=%s", currentSlideShareName));

        m_buttonCreate = (Button)findViewById(R.id.create_button);
        m_buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "MainActivity.onCreateButtonClicked");

                final String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
                if (slideShareName != null) {
                    AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                    adb.setTitle(getString(R.string.main_create_alert_title));
                    adb.setCancelable(true);
                    adb.setMessage(getString(R.string.main_create_alert_message));
                    adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            //
                            // Delete the old slide share directory and create a new one.
                            // Create a new m_slideShareName
                            //

                            Utilities.deleteSlideShareDirectory(MainActivity.this, slideShareName);
                            String ssname = UUID.randomUUID().toString();

                            if(D)Log.d(TAG, String.format("MainActivity.onCreateButtonClicked - new slideShareName: %s", ssname));

                            Editor edit = m_prefs.edit();
                            edit.putString(SSPreferences.PREFS_SSNAME, ssname);
                            edit.commit();

                            enterSlideShareTitleAndLaunchCreate();
                        }
                    });
                    adb.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog ad = adb.create();
                    ad.show();
                }
                else {
                    launchCreateSlideShareActivity(m_slideShareTitle);
                }
            }
        });

        m_buttonEdit = (Button)findViewById(R.id.edit_button);
        m_buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "MainActivity.onEditButtonClicked");

                launchCreateSlideShareActivity(m_slideShareTitle);
            }
        });

        m_buttonPreview = (Button)findViewById(R.id.preview_button);
        m_buttonPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "MainActivity.onPreviewButtonClicked");

                Intent intent = new Intent(MainActivity.this, PlaySlidesActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "MainActivity.onStart");

        super.onStart();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "MainActivity.onStop");

        super.onStop();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "MainActivity.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "MainActivity.onResume");

        super.onResume();

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
        m_slideShareTitle = SlideShareJSON.getSlideShareTitle(this, slideShareName);
        if(D)Log.d(TAG, String.format("MainActivity.onResume: m_slideShareTitle = %s", m_slideShareTitle));

        String title = m_slideShareTitle == null ? getString(R.string.default_slideshare_title) : m_slideShareTitle;

        m_buttonEdit.setVisibility(slideShareName == null ? View.GONE : View.VISIBLE);
        m_buttonEdit.setText(String.format(getString(R.string.main_edit_button_format), title));

        m_buttonPreview.setVisibility(slideShareName == null ? View.GONE : View.VISIBLE);
        m_buttonPreview.setText(String.format(getString(R.string.main_preview_button_format), title));
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "MainActivity.onDestroy");

        super.onDestroy();
    }

    private void launchCreateSlideShareActivity(String title) {
        if(D)Log.d(TAG, "MainActivity.launchCreateSlideShareActivity");

        Intent intent = new Intent(this, CreateSlidesActivity.class);
        intent.putExtra(CreateSlidesActivity.EXTRA_TITLE, title);
        startActivity(intent);
    }

    private void enterSlideShareTitleAndLaunchCreate() {
        if(D)Log.d(TAG, "MainActivity.enterSlideShareTitleAndLaunchCreate");

        final EditText titleText = new EditText(this);
        titleText.setHint(getString(R.string.main_new_title_hint));
        titleText.setSingleLine();

        AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
        adb.setTitle(getString(R.string.main_new_title_title));
        adb.setCancelable(false);
        adb.setView(titleText);
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleText.getText().toString();

                dialog.dismiss();

                // Switch to CreateSlidesActivity
                launchCreateSlideShareActivity(title);
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
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
