package com.hyperfine.neodori;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hyperfine.neodori.cloudproviders.AmazonClientManager;
import com.hyperfine.neodori.cloudproviders.AmazonSharedPreferencesWrapper;
import com.hyperfine.neodori.cloudproviders.GoogleLogin;
import com.hyperfine.neodori.fragments.EditSlidesFragment;

import java.io.File;
import java.util.UUID;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditSlidesActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {
    public final static String TAG = "EditSlidesActivity";
    public final static String EXTRA_TITLE = "extra_title";

    private SharedPreferences m_prefs;
    private boolean m_fragmentNeedsCreateNew = false;

    public final static int REQUEST_GOOGLE_PLAY_SERVICES_ERROR = 1;
    public final static int REQUEST_GOOGLE_LOGIN = 2;
    public final static int REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT = 3;

    public static AmazonClientManager s_amazonClientManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesActivity.onCreate");

        super.onCreate(savedInstanceState);

        int retVal = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (retVal != ConnectionResult.SUCCESS) {
            if(D)Log.d(TAG, String.format("EditSlidesActivity.onCreate - isGooglePlayServicesAvailable failed with %d", retVal));
            GooglePlayServicesUtil.getErrorDialog(retVal, this, REQUEST_GOOGLE_PLAY_SERVICES_ERROR);
            if(D)Log.d(TAG, "EditSlidesActivity.onCreate - called GooglePlayServicesUtil.getErrorDialog, and now exiting");
        }

        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        String userUuidString = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
        String userEmail = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);

        s_amazonClientManager = new AmazonClientManager(m_prefs);

        if (userUuidString == null || userEmail == null) {
            if(D)Log.d(TAG, String.format("EditSlidesActivity.onCreate: userUuidString=%s, userEmail=%s, so calling GoogleLogin", userUuidString, userEmail));
            Intent intent = new Intent(this, GoogleLogin.class);
            startActivityForResult(intent, REQUEST_GOOGLE_LOGIN);
        }

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
        if(D)Log.d(TAG, String.format("EditSlidesActivity.onCreate: slideShareName=%s", slideShareName));

        if (slideShareName == null) {
            if(D)Log.d(TAG, "EditSlidesActivity.onCreate - null slideShareName. Creating one and saving it to prefs.");
            slideShareName = UUID.randomUUID().toString();

            SharedPreferences.Editor edit = m_prefs.edit();
            edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, slideShareName);
            edit.commit();
        }

        File slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, slideShareName);
        if (slideShareDirectory == null) {
            if(D)Log.d(TAG, "EditSlidesActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
            finish();
            return;
        }
        SharedPreferences.Editor editor = m_prefs.edit();
        editor.putString(SSPreferences.PREFS_EDITPROJECTNAME, slideShareName);
        editor.commit();

        setContentView(R.layout.activity_editslides);

        /* NEVER
        FragmentManager fm = getSupportFragmentManager();
        m_editSlidesFragment = (EditSlidesFragment)fm.findFragmentByTag(EditSlidesFragment.class.toString());
        if (m_editSlidesFragment != null) {
            m_editSlidesFragment.setSlideShareName(slideShareName);
        }
        */
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "EditSlidesActivity.onAttachFragment");

        if (fragment instanceof EditSlidesFragment) {
            if(D)Log.d(TAG, "EditSlidesActivity.onAttachFragment - found our editSlidesFragment");
            String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
            EditSlidesFragment editSlidesFragment = (EditSlidesFragment)fragment;
            editSlidesFragment.setSlideShareName(slideShareName);
        }
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "EditSlidesActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("EditSlidesActivity.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES_ERROR) {
            if(D)Log.d(TAG, "EditSlidesActivity.onActivityResult - returned from Google Play Services error dialog. Finishing.");
            finish();
        }
        else if (requestCode == REQUEST_GOOGLE_LOGIN) {
            if (resultCode == RESULT_OK) {
                if(D)Log.d(TAG, "EditSlidesActivity.onActivityResult - return from successful Google login.");
            }
            else {
                // BUGBUG - handle login failure
                if(D)Log.d(TAG, "EditSlidesActivity.onActivityResult - failed to login to Google. Finishing. TODO: handle with grace.");
                finish();
            }
        }
        else if (requestCode == REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT) {
            if (resultCode == RESULT_OK) {
                if(D)Log.d(TAG, "EditSlidesActivity.onActivityResult - handling REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT");
                m_fragmentNeedsCreateNew = true;
            }
            else {
                // BUGBUG - handle login failure
                if(D)Log.d(TAG, "EditSlidesActivity.onActivityResult - failed to login to Google. Finishing. TODO: handle with grace.");
                finish();
            }
        }
        else {
            if(D)Log.d(TAG, "EditSlidesFragment.onActivityResult - passing on to super.onActivityResult");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "EditSlidesActivity.onStart");

        super.onStart();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "EditSlidesActivity.onStop");

        super.onStop();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "EditSlidesActivity.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "EditSlidesActivity.onResume");

        super.onResume();

        if (m_fragmentNeedsCreateNew) {
            if(D)Log.d(TAG, "EditSlidesActivity.onResume - EditSlidesFragment needs to initializeForChangeInAccount");
            m_fragmentNeedsCreateNew = false;

            FragmentManager fm = getSupportFragmentManager();
            EditSlidesFragment editSlidesFragment = (EditSlidesFragment)fm.findFragmentById(R.id.editslides_fragment);
            editSlidesFragment.initializeForChangeInAccount();
        }
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "EditSlidesActivity.onDestroy");

        super.onDestroy();
    }
}
