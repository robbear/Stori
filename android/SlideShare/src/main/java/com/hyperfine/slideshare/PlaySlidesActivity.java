package com.hyperfine.slideshare;

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

import com.hyperfine.slideshare.adapters.PlaySlidesPagerAdapter;
import com.hyperfine.slideshare.fragments.PlaySlidesFragment;

import java.io.File;
import java.util.List;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class PlaySlidesActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {
    public final static String TAG = "PlaySlidesActivity";

    private final static String INSTANCE_STATE_CURRENT_TAB = "instance_state_current_tab";

    private SharedPreferences m_prefs;
    private SlideShareJSON m_ssj;
    private PlaySlidesPagerAdapter m_playSlidesPagerAdapter;
    private ViewPager.OnPageChangeListener m_pageChangeListener;
    private ViewPager m_viewPager;
    private File m_slideShareDirectory;
    private String m_slideShareName;
    private int m_currentTabPosition = 0;
    private boolean m_loadedFromSavedInstanceState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_playslides);

        m_slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        // BUGBUG TODO: Replace with dialog to create/fetch SlideShare name
        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, m_slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
            return;
        }

        initializeSlideShareJSON();

        if (savedInstanceState != null) {
            m_loadedFromSavedInstanceState = true;
            m_currentTabPosition = savedInstanceState.getInt(INSTANCE_STATE_CURRENT_TAB, 0);
        }

        m_playSlidesPagerAdapter = new PlaySlidesPagerAdapter(getSupportFragmentManager());
        m_playSlidesPagerAdapter.setSlideShareJSON(m_ssj);
        m_playSlidesPagerAdapter.setSlideShareName(m_slideShareName);
        m_playSlidesPagerAdapter.setActivityParent(this);

        m_viewPager = (ViewPager)findViewById(R.id.view_pager);
        try {
            m_viewPager.setOffscreenPageLimit(1);
            m_viewPager.setAdapter(m_playSlidesPagerAdapter);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.onCreate", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.onCreate", e);
            e.printStackTrace();
        }

        m_pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {
            }

            @Override
            public void onPageSelected(int position) {
                if(D)Log.d(TAG, String.format("PlaySlidesActivity.onPageSelected: %d", position));

                if (m_loadedFromSavedInstanceState) {
                    if(D)Log.d(TAG, "PlaySlidesActivity.onPageSelected - loaded from SavedInstanceState, so don't notify fragments.");
                    m_loadedFromSavedInstanceState = false;
                }
                else {
                    FragmentManager fm = getSupportFragmentManager();
                    List<Fragment> fragments = fm.getFragments();
                    for (Fragment f : fragments) {
                        PlaySlidesFragment psf = (PlaySlidesFragment)f;
                        if (psf != null) {
                            psf.onTabPageSelected(position);
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

        if (savedInstanceState != null) {
            m_viewPager.setCurrentItem(m_currentTabPosition);
        }

        getActionBar().hide();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        if(D)Log.d(TAG, String.format("PlaySlidesActivity.onSaveInstanceState: m_currentTabPosition=%d", m_currentTabPosition));

        savedInstanceState.putInt(INSTANCE_STATE_CURRENT_TAB, m_currentTabPosition);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "PlaySlidesActivity.onAttachFragment");
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "PlaySlidesActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    private void initializeSlideShareJSON() {
        if(D)Log.d(TAG, "PlaySlidesActivity.initializeSlideShareJSON");

        m_ssj = SlideShareJSON.load(this, m_slideShareName, Config.slideShareJSONFilename);
        if (m_ssj == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.initializeSlideShareJSON - failed to load json file");
            // BUGBUG TODO - feedback?
            return;
        }

        if(D)Log.d(TAG, "PlaySlidesActivity.initializeSlideShareJSON: here is the JSON:");
        Utilities.printSlideShareJSON(m_ssj);
    }
}
