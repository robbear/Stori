package com.hyperfine.neodori;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hyperfine.neodori.adapters.EditPlayPagerAdapter;
import com.hyperfine.neodori.cloudproviders.AmazonClientManager;
import com.hyperfine.neodori.cloudproviders.AmazonSharedPreferencesWrapper;
import com.hyperfine.neodori.cloudproviders.GoogleLogin;
import com.hyperfine.neodori.fragments.EditPlayFragment;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditPlayActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {
    public final static String TAG = "EditPlayActivity";
    public final static String EXTRA_FROMURL = "extra_from_url";

    private final static String INSTANCE_STATE_CURRENT_TAB = "instance_state_current_tab";
    private final static String INSTANCE_STATE_EDITPLAYMODE = "instance_state_editplaymode";

    private SharedPreferences m_prefs;
    private boolean m_fragmentNeedsCreateNew = false;
    private String m_userUuid = null;
    private SlideShareJSON m_ssj;
    private EditPlayPagerAdapter m_editPlayPagerAdapter;
    private ViewPager.OnPageChangeListener m_pageChangeListener;
    private ViewPager m_viewPager;
    private File m_slideShareDirectory;
    private String m_slideShareName;
    private int m_currentTabPosition = 0;
    private boolean m_loadedFromSavedInstanceState = false;
    private EditPlayMode m_editPlayMode = EditPlayMode.Edit;

    public final static int REQUEST_GOOGLE_PLAY_SERVICES_ERROR = 1;
    public final static int REQUEST_GOOGLE_LOGIN = 2;
    public final static int REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT = 3;

    public static AmazonClientManager s_amazonClientManager = null;

    public enum EditPlayMode {
        Edit(0),
        PlayEdit(1),
        Play(2);

        private final int value;
        private  EditPlayMode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    };

    public EditPlayMode getEditPlayMode() {
        return m_editPlayMode;
    }

    public void setEditPlayMode(EditPlayMode mode) {
        m_editPlayMode = mode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayActivity.onCreate");

        super.onCreate(savedInstanceState);

        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_editplay);

        boolean isFromUrl = getIntent().getBooleanExtra(EXTRA_FROMURL, false);

        if (isFromUrl) {
            m_editPlayMode = EditPlayMode.Play;
            m_slideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME, SSPreferences.DEFAULT_PLAYSLIDESNAME);
            if(D)Log.d(TAG, String.format("EditPlayActivity.onCreate - playing from a downloaded URL reference: %s", m_slideShareName));

            if (m_slideShareName == null) {
                if(D)Log.d(TAG, "EditPlayActivity.onCreate - m_slideShareName is null, meaning we'll have no SSJ. Bailing.");
                finish();
                return;
            }
        }
        else {
            if(D)Log.d(TAG, "EditPlayActivity.onCreate - in edit mode");

            int retVal = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (retVal != ConnectionResult.SUCCESS) {
                if(D)Log.d(TAG, String.format("EditPlayActivity.onCreate - isGooglePlayServicesAvailable failed with %d", retVal));
                GooglePlayServicesUtil.getErrorDialog(retVal, this, REQUEST_GOOGLE_PLAY_SERVICES_ERROR);
                if(D)Log.d(TAG, "EditPlayActivity.onCreate - called GooglePlayServicesUtil.getErrorDialog, and now exiting");
            }

            String userUuidString = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
            String userEmail = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);

            s_amazonClientManager = new AmazonClientManager(m_prefs);

            if (userUuidString == null || userEmail == null) {
                if(D)Log.d(TAG, String.format("EditSlidesActivity.onCreate: userUuidString=%s, userEmail=%s, so calling GoogleLogin", userUuidString, userEmail));
                Intent intent = new Intent(this, GoogleLogin.class);
                startActivityForResult(intent, REQUEST_GOOGLE_LOGIN);
            }

            m_slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
            if(D)Log.d(TAG, String.format("EditPlayActivity.onCreate - in edit mode: %s", m_slideShareName));

            if (m_slideShareName == null) {
                if(D)Log.d(TAG, "EditPlayActivity.onCreate - null slideShareName. Creating one and saving it to prefs.");
                m_slideShareName = UUID.randomUUID().toString();

                SharedPreferences.Editor edit = m_prefs.edit();
                edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, m_slideShareName);
                edit.commit();
            }
        }

        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, m_slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "EditPlayActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
            finish();
            return;
        }

        initializeSlideShareJSON();

        if (savedInstanceState != null) {
            m_loadedFromSavedInstanceState = true;
            m_currentTabPosition = savedInstanceState.getInt(INSTANCE_STATE_CURRENT_TAB, 0);

            EditPlayMode pemDefault = EditPlayMode.Edit;
            int pemValue = savedInstanceState.getInt(INSTANCE_STATE_EDITPLAYMODE, pemDefault.getValue());
            m_editPlayMode = EditPlayMode.values()[pemValue];
        }

        initializeViewPager();
        /* NEVER
        m_editPlayPagerAdapter = new EditPlayPagerAdapter(getSupportFragmentManager());
        m_editPlayPagerAdapter.setSlideShareJSON(m_ssj);
        m_editPlayPagerAdapter.setSlideShareName(m_slideShareName);
        m_editPlayPagerAdapter.setEditPlayActivity(this);

        m_viewPager = (ViewPager)findViewById(R.id.view_pager);
        try {
            m_viewPager.setOffscreenPageLimit(1);
            m_viewPager.setAdapter(m_editPlayPagerAdapter);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.onCreate", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.onCreate", e);
            e.printStackTrace();
        }
        */

        m_pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int position) {
                if(D)Log.d(TAG, String.format("EditPlayActivity.onPageSelected: %d", position));

                if (m_loadedFromSavedInstanceState) {
                    if(D)Log.d(TAG, "EditPlayActivity.onPageSelected - loaded from SavedInstanceState, so don't notify fragments.");
                    m_loadedFromSavedInstanceState = false;
                }
                else {
                    FragmentManager fm = getSupportFragmentManager();
                    List<Fragment> fragments = fm.getFragments();
                    for (Fragment f : fragments) {
                        EditPlayFragment epf = (EditPlayFragment)f;
                        if (epf != null) {
                            epf.onTabPageSelected(position);
                        }
                    }
                }

                m_viewPager.setCurrentItem(position);
                m_currentTabPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int position) {
            }
        };
        m_viewPager.setOnPageChangeListener(m_pageChangeListener);

        if (m_editPlayMode != EditPlayMode.Edit) {
            getActionBar().hide();
        }
    }

    private void initializeViewPager() {
        if(D)Log.d(TAG, "EditPlayActivity.initializeViewPager");

        m_editPlayPagerAdapter = new EditPlayPagerAdapter(getSupportFragmentManager());
        m_editPlayPagerAdapter.setSlideShareJSON(m_ssj);
        m_editPlayPagerAdapter.setSlideShareName(m_slideShareName);
        m_editPlayPagerAdapter.setEditPlayActivity(this);

        m_viewPager = (ViewPager)findViewById(R.id.view_pager);
        try {
            m_viewPager.setOffscreenPageLimit(1);
            m_viewPager.setAdapter(m_editPlayPagerAdapter);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.initializeViewPager", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.initializeViewPager", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        if(D)Log.d(TAG, String.format("EditPlayActivity.onSaveInstanceState: m_currentTabPosition=%d", m_currentTabPosition));

        savedInstanceState.putInt(INSTANCE_STATE_CURRENT_TAB, m_currentTabPosition);
        savedInstanceState.putInt(INSTANCE_STATE_EDITPLAYMODE, m_editPlayMode.getValue());
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "EditPlayActivity.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "EditPlayActivity.onStart");

        super.onStart();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "EditPlayActivity.onStop");

        super.onStop();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "EditPlayActivity.onResume");

        super.onResume();

        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
        if(D)Log.d(TAG, String.format("EditPlayActivity.onResume: m_userUuid=%s", m_userUuid));

        m_viewPager.setCurrentItem(m_currentTabPosition);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "EditPlayActivity.onAttachFragment");
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "EditPlayActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    private void initializeSlideShareJSON() {
        if(D)Log.d(TAG, "EditPlayActivity.initializeSlideShareJSON");

        m_ssj = SlideShareJSON.load(this, m_slideShareName, Config.slideShareJSONFilename);
        if (m_ssj == null) {
            if (m_editPlayMode == EditPlayMode.Play) {
                if(D)Log.d(TAG, "EditPlayActivity.initializeSlideShareJSON - failed to load json file in Play mode. Bailing.");
                // BUGBUG TODO - feedback?
                return;
            }
            else {
                try {
                    m_ssj = new SlideShareJSON(this);
                    m_ssj.save(this, m_slideShareName, Config.slideShareJSONFilename);
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "EditPlayActivity.initializeSlideShareJSON (FATAL)", e);
                    e.printStackTrace();
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "EditPlayActivity.initializeSlideShareJSON (FATAL)", e);
                    e.printStackTrace();
                }
            }
        }

        try {
            String title = m_ssj.getTitle();
            getActionBar().setTitle(title == null ? getString(R.string.default_neodori_title) : title);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.initializeSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.initializeSlideShareJSON", e);
            e.printStackTrace();
        }

        if(D)Log.d(TAG, "EditPlayActivity.initializeSlideShareJSON: here is the JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    public void initializeNewSlide(int newIndex) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.initializeNewSlide: newIndex=%d", newIndex));

        if (newIndex < 0) {
            m_currentTabPosition = 0;
        }
        else {
            m_currentTabPosition = newIndex;
        }

        updateSlideShareJSON(UUID.randomUUID().toString(), null, null);

        m_viewPager.setCurrentItem(m_currentTabPosition);
    }

    private void updateSlideShareJSON(String slideUuid, String imageFileName, String audioFileName) {
        if(D)Log.d(TAG, "EditPlayActivity.updateSlideShareJSON");
        if(D)Log.d(TAG, "Current JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        try {
            String imageUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, imageFileName);
            String audioUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, audioFileName);

            m_ssj.upsertSlide(slideUuid, m_currentTabPosition, imageUrl, audioUrl);
            m_ssj.save(this, m_slideShareName, Config.slideShareJSONFilename);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.updateSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.updateSlideShareJSON", e);
            e.printStackTrace();
        }

        initializeViewPager();
        //m_editPlayPagerAdapter.notifyDataSetChanged();

        if(D)Log.d(TAG, "After update:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }
}
