package com.hyperfine.neodori;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hyperfine.neodori.cloudproviders.AmazonClientManager;
import com.hyperfine.neodori.cloudproviders.AmazonSharedPreferencesWrapper;
import com.hyperfine.neodori.cloudproviders.GoogleLogin;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class MainActivity extends Activity implements CloudStore.ICloudStoreCallback {

    public final static String TAG = "MainActivity";
    public final static int RESULT_GOOGLE_PLAY_SERVICES_ERROR = 1;
    public final static int RESULT_GOOGLE_LOGIN = 2;

    private SharedPreferences m_prefs;
    private Button m_buttonCreate;
    private Button m_buttonEdit;
    private Button m_buttonPreview;
    private Button m_buttonShare;
    private Button m_buttonPublish;
    private String m_slideShareTitle;
    private ProgressDialog m_progressDialog = null;

    public static AmazonClientManager s_amazonClientManagerXXX = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "MainActivity.onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int retVal = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (retVal != ConnectionResult.SUCCESS) {
            if(D)Log.d(TAG, String.format("MainActivity.onCreate - isGooglePlayServicesAvailable failed with %d", retVal));
            GooglePlayServicesUtil.getErrorDialog(retVal, this, RESULT_GOOGLE_PLAY_SERVICES_ERROR);
            if(D)Log.d(TAG, "MainActivity.onCreate - called GooglePlayServicesUtil.getErrorDialog, and now exiting");
        }

        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        String userUuidString = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
        String userEmail = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);

        s_amazonClientManagerXXX = new AmazonClientManager(m_prefs);

        if (userUuidString == null || userEmail == null) {
            if(D)Log.d(TAG, String.format("MainActivity.onCreate: userUuidString=%s, userEmail=%s, so calling GoogleLogin", userUuidString, userEmail));
            Intent intent = new Intent(this, GoogleLogin.class);
            startActivityForResult(intent, RESULT_GOOGLE_LOGIN);
        }

        String currentSlideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
        if(D)Log.d(TAG, String.format("MainActivity.onCreate: currentSlideShareName=%s", currentSlideShareName));

        m_buttonCreate = (Button)findViewById(R.id.create_button);
        m_buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "MainActivity.onCreateButtonClicked");

                final String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
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
                            edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, ssname);
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

        m_buttonShare = (Button)findViewById(R.id.share_button);
        m_buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
                String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, null);

                if (userUuid != null && slideShareName != null) {
                    Utilities.shareShow(MainActivity.this, userUuid, slideShareName);
                }
            }
        });

        m_buttonPublish = (Button)findViewById(R.id.publish_button);
        m_buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
                String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, null);

                if (userUuid != null && slideShareName != null) {
                    publishSlides(userUuid, slideShareName);
                }
            }
        });
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("MainActivity.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == RESULT_GOOGLE_PLAY_SERVICES_ERROR) {
            if(D)Log.d(TAG, "MainActivity.onActivityResult - returned from Google Play Services error dialog. Finishing.");
            finish();
        }
        else if (requestCode == RESULT_GOOGLE_LOGIN) {
            if (resultCode == RESULT_OK) {
                if(D)Log.d(TAG, "MainActivity.onActivityResult - return from successful Google login.");
            }
            else {
                // BUGBUG - handle login failure
                if(D)Log.d(TAG, "MainActivity.onActivityResult - failed to login to Google. Finishing. TODO: handle with grace.");
                finish();
            }
        }
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

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
        m_slideShareTitle = SlideShareJSON.getSlideShareTitle(this, slideShareName);
        if(D)Log.d(TAG, String.format("MainActivity.onResume: m_slideShareTitle = %s", m_slideShareTitle));

        String title = m_slideShareTitle == null ? getString(R.string.default_neodori_title) : m_slideShareTitle;

        m_buttonEdit.setVisibility(slideShareName == null ? View.GONE : View.VISIBLE);
        m_buttonEdit.setText(String.format(getString(R.string.main_edit_button_format), title));

        m_buttonPreview.setVisibility(slideShareName == null ? View.GONE : View.VISIBLE);
        m_buttonPreview.setText(String.format(getString(R.string.main_preview_button_format), title));

        boolean isPublished = SlideShareJSON.isSlideSharePublished(this, slideShareName);
        m_buttonShare.setVisibility(slideShareName == null || !isPublished ? View.GONE : View.VISIBLE);
        m_buttonShare.setText(String.format(getString(R.string.main_share_button_format), title));

        m_buttonPublish.setVisibility(slideShareName == null ? View.GONE : View.VISIBLE);
        m_buttonPublish.setText(String.format(getString(R.string.main_publish_button_format), title));
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "MainActivity.onDestroy");

        super.onDestroy();
    }

    private void launchCreateSlideShareActivity(String title) {
        if(D)Log.d(TAG, "MainActivity.launchCreateSlideShareActivity");

        Intent intent = new Intent(this, EditSlidesActivity.class);
        intent.putExtra(EditSlidesActivity.EXTRA_TITLE, title);
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

                // Switch to EditSlidesActivity
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

        MenuItem sa = menu.add("Switch account");
        sa.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                adb.setTitle(getString(R.string.switch_account_dialog_title));
                adb.setCancelable(true);
                adb.setMessage(getString(R.string.switch_account_dialog_message));
                adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(D)Log.d(TAG, "MainActivity.onMenuClick - switching account");
                        dialog.dismiss();

                        String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);

                        Utilities.deleteSlideShareDirectory(MainActivity.this, slideShareName);

                        if(D)Log.d(TAG, "MainActivity.onMenuClick - switching account: nulling out PREFS_EDITPROJECTNAME");
                        Editor edit = m_prefs.edit();
                        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, null);
                        edit.commit();

                        s_amazonClientManagerXXX.clearCredentials();
                        s_amazonClientManagerXXX.wipe();

                        Intent intent = new Intent(MainActivity.this, GoogleLogin.class);
                        MainActivity.this.startActivity(intent);
                    }
                });
                adb.setNegativeButton(getString(R.string.no_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog ad = adb.create();
                ad.show();

                return true;
            }
        });

        /* NEVER
        MenuItem li = menu.add("Login");
        li.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, GoogleLogin.class);
                MainActivity.this.startActivity(intent);
                return true;
            }
        });

        MenuItem lo = menu.add("Logout");
        lo.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                s_amazonClientManager.clearCredentials();
                s_amazonClientManager.wipe();
                return true;
            }
        });

        // BUGBUG
        MenuItem es = menu.add("EditSlidesActivity");
        es.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(MainActivity.this, EditSlidesActivity.class);
                intent.putExtra(EditSlidesActivity.EXTRA_TITLE, m_slideShareTitle);
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
        */

        return true;
    }

    public void publishSlides(String userUuid, String slideShareName) {
        if(D)Log.d(TAG, "MainActivity.publishSlides");

        final String _slideShareName = slideShareName;
        final String _userUuid = userUuid;

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.publish_dialog_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.publish_dialog_message));
        adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                CloudStore cloudStore = new CloudStore(MainActivity.this, _userUuid,
                        _slideShareName, Config.CLOUD_STORAGE_PROVIDER, MainActivity.this);

                cloudStore.saveAsync();

                m_progressDialog = new ProgressDialog(MainActivity.this);
                m_progressDialog.setTitle(getString(R.string.upload_dialog_title));
                m_progressDialog.setCancelable(false);
                m_progressDialog.setIndeterminate(true);
                m_progressDialog.show();
            }
        });
        adb.setNegativeButton(getString(R.string.no_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    public void onSaveComplete(CloudStore.SaveErrors se, SlideShareJSON ssj) {
        if(D)Log.d(TAG, String.format("CreateSlidesFragment.onSaveComplete: se=%s", se));

        final String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
        final String userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);

        if (m_progressDialog != null) {
            m_progressDialog.dismiss();
            m_progressDialog = null;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setCancelable(false);

        if (se == CloudStore.SaveErrors.Success) {
            adb.setTitle(getString(R.string.upload_dialog_complete_title));
            adb.setMessage(getString(R.string.upload_dialog_complete_message_format));
            adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    Utilities.shareShow(MainActivity.this, userUuid, slideShareName);
                }
            });
            adb.setNegativeButton(getString(R.string.no_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        else {
            adb.setTitle(getString(R.string.upload_dialog_failure_title));
            adb.setMessage(String.format(getString(R.string.upload_dialog_failure_message_format), se.toString()));
            adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }

        AlertDialog ad = adb.create();
        ad.show();
    }
}
