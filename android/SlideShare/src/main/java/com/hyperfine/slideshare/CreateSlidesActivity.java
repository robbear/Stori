package com.hyperfine.slideshare;

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

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class CreateSlidesActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {

    public final static String TAG = "CreateSlidesActivity";

    private SharedPreferences m_prefs;
    private CreateSlidesFragment m_createSlidesFragment;
    private File m_slideShareDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "CreateSlidesActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        // BUGBUG TODO: Replace with dialog to create/fetch SlideShare name
        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "TestImagePickerActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
        }

        setContentView(R.layout.activity_createslides);

        FragmentManager fm = getSupportFragmentManager();
        m_createSlidesFragment = (CreateSlidesFragment)fm.findFragmentByTag(CreateSlidesFragment.class.toString());
        if (m_createSlidesFragment != null) {
            m_createSlidesFragment.setSlideShareName(slideShareName);
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
