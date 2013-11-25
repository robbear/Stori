package com.hyperfine.slideshare;

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

import com.hyperfine.slideshare.fragments.CreateSlidesFragment;

import java.io.File;
import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class CreateSlidesActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {

    public final static String TAG = "CreateSlidesActivity";
    public final static String EXTRA_TITLE = "extra_title";

    private SharedPreferences m_prefs;
    private CreateSlidesFragment m_createSlidesFragment;
    private File m_slideShareDirectory;
    private String m_slideShareTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "CreateSlidesActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        Intent intent = getIntent();
        m_slideShareTitle = intent.getStringExtra(EXTRA_TITLE);
        getActionBar().setTitle(m_slideShareTitle == null ? getString(R.string.default_slideshare_title) : m_slideShareTitle);

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        if (slideShareName == null) {
            if(D)Log.d(TAG, "CreateSlidesActivity.onCreate - null slideShareName. Creating one and saving it to prefs.");
            slideShareName = UUID.randomUUID().toString();

            SharedPreferences.Editor edit = m_prefs.edit();
            edit.putString(SSPreferences.PREFS_SSNAME, slideShareName);
            edit.commit();
        }

        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "CreateSlidesActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
            finish();
            return;
        }

        setContentView(R.layout.activity_createslides);

        FragmentManager fm = getSupportFragmentManager();
        m_createSlidesFragment = (CreateSlidesFragment)fm.findFragmentByTag(CreateSlidesFragment.class.toString());
        if (m_createSlidesFragment != null) {
            m_createSlidesFragment.setSlideShareName(slideShareName);
            m_createSlidesFragment.setSlideShareTitle(m_slideShareTitle);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "CreateSlidesActivity.onAttachFragment");

        if (fragment instanceof CreateSlidesFragment) {
            if(D)Log.d(TAG, "CreateSlidesActivity.onAttachFragment - found our CreateSlidesFragment");
            String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
            m_createSlidesFragment = (CreateSlidesFragment)fragment;
            m_createSlidesFragment.setSlideShareName(slideShareName);
            m_createSlidesFragment.setSlideShareTitle(m_slideShareTitle);
        }
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "CreateSlidesActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }
}
