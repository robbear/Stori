package com.stori.stori;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
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

import com.stori.stori.adapters.PlaySlidesPagerAdapter;
import com.stori.stori.fragments.PlaySlidesFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class PlaySlidesActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {
    public final static String TAG = "PlaySlidesActivity";
    public final static String EXTRA_FROMURL = "extra_from_url";

    private final static String INSTANCE_STATE_CURRENT_TAB = "instance_state_current_tab";
    private final static String INSTANCE_STATE_ORIENTATION_CHANGED = "instance_state_orientation_changed";
    private final static String INSTANCE_STATE_IS_FROM_URL = "instance_state_is_from_url";
    private final static String INSTANCE_STATE_IS_OVERLAY_VISIBLE = "instance_state_is_overlay_visible";

    private SharedPreferences m_prefs;
    private SlideShareJSON m_ssj;
    private PlaySlidesPagerAdapter m_playSlidesPagerAdapter;
    private ViewPager.OnPageChangeListener m_pageChangeListener;
    private ViewPager m_viewPager;
    private File m_slideShareDirectory;
    private String m_slideShareName;
    private int m_currentTabPosition = 0;
    private boolean m_fOverlayVisible = true;
    private int m_orientation;
    private boolean m_fOrientationChanged = false;
    private boolean m_isFromUrl = false;
    private StoriService m_storiService = null;
    private ArrayList<StoriService.StoriServiceConnectionListener> m_storiServiceConnectionListeners = new ArrayList<StoriService.StoriServiceConnectionListener>();

    public boolean getOverlayVisible() {
        return m_fOverlayVisible;
    }

    public void setOverlayVisible(boolean isVisible) {
        m_fOverlayVisible = isVisible;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_playslides);

        m_isFromUrl = getIntent().getBooleanExtra(EXTRA_FROMURL, false);

        if (m_isFromUrl) {
            m_slideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME, SSPreferences.DEFAULT_PLAYSLIDESNAME);
            if(D)Log.d(TAG, String.format("PlaySlidesActivity.onCreate - playing from a downloaded URL reference: %s", m_slideShareName));
        }
        else {
            m_slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);
            if(D)Log.d(TAG, String.format("PlaySlidesActivity.onCreate - playing a preview: %s", m_slideShareName));
        }
        if (m_slideShareName == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.onCreate - m_slideShareName is null, meaning we'll have no SSJ. Bailing.");
            finish();
            return;
        }

        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, m_slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
            finish();
            return;
        }

        initializeSlideShareJSON();

        if (savedInstanceState != null) {
            m_currentTabPosition = savedInstanceState.getInt(INSTANCE_STATE_CURRENT_TAB, 0);
            m_fOrientationChanged = savedInstanceState.getBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, false);
            m_isFromUrl = savedInstanceState.getBoolean(INSTANCE_STATE_IS_FROM_URL, false);
            m_fOverlayVisible = savedInstanceState.getBoolean(INSTANCE_STATE_IS_OVERLAY_VISIBLE, true);
            if(D)Log.d(TAG, String.format("PlaySlidesActivity.onCreate - loading from savedInstanceState. m_isFromUrl=%b", m_isFromUrl));
        }

        m_playSlidesPagerAdapter = new PlaySlidesPagerAdapter(getSupportFragmentManager());
        m_playSlidesPagerAdapter.setSlideShareJSON(m_ssj);
        m_playSlidesPagerAdapter.setSlideShareName(m_slideShareName);
        m_playSlidesPagerAdapter.setPlaySlidesActivity(this);

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

                // Do not notify fragments if this is the result of an orientation change
                if (!m_fOrientationChanged) {
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
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "PlaySlidesActivity.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "PlaySlidesActivity.onStart");

        super.onStart();

        initializeStoriService();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "PlaySlidesActivity.onStop");

        super.onStop();

        uninitializeStoriService();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "PlaySlidesActivity.onResume");

        super.onResume();

        m_orientation = getResources().getConfiguration().orientation;
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.onResume: orientation = %d", m_orientation));
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "PlaySlidesActivity.onPause");

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        if(D)Log.d(TAG, String.format("PlaySlidesActivity.onSaveInstanceState: m_currentTabPosition=%d", m_currentTabPosition));

        int orientation = getResources().getConfiguration().orientation;
        m_fOrientationChanged = m_orientation != orientation;

        savedInstanceState.putInt(INSTANCE_STATE_CURRENT_TAB, m_currentTabPosition);
        savedInstanceState.putBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, m_fOrientationChanged);
        savedInstanceState.putBoolean(INSTANCE_STATE_IS_FROM_URL, m_isFromUrl);
        savedInstanceState.putBoolean(INSTANCE_STATE_IS_OVERLAY_VISIBLE, m_fOverlayVisible);
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
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    public void launchAboutActivity() {
        if(D)Log.d(TAG, "PlaySlidesActivity.launchAboutActivity");

        Intent intent = new Intent(PlaySlidesActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    public String getSlidesTitle() {
        if(D)Log.d(TAG, "PlaySlidesActivity.getSlidesTitle");

        String title = null;
        try {
            title = m_ssj.getTitle();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlidesTitle", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlidesTitle", e);
            e.printStackTrace();
        }

        return title;
    }

    public boolean savePhoto(String slideUuid) {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.savePhoto: slideUuid=%s", slideUuid));

        if (m_ssj == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.savePhoto - m_ssj is null, so bailing.");
            return false;
        }

        boolean success = false;

        try {
            SlideJSON sj = m_ssj.getSlide(slideUuid);
            String imageFileName = sj.getImageFilename();
            if(D)Log.d(TAG, String.format("PlaySlidesActivity.saveSlide: %s", imageFileName));

            File fileSource = new File(m_slideShareDirectory, imageFileName);

            File filePathDest = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Config.copiedImageFolderName);
            if (!filePathDest.exists()) {
                filePathDest.mkdir();
            }

            File fileDest = new File(filePathDest, imageFileName);

            success = Utilities.copyFile(fileSource, fileDest);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.savePhoto", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.savePhoto", e);
            e.printStackTrace();
        }

        return success;
    }

    public boolean saveAllPhotos() {
        if(D)Log.d(TAG, "PlaySlidesActivity.saveAllPhotos");

        return false;
    }

    public int getSlidePosition(String slideUuid) {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.getSlidePosition: slideUuid=%s", slideUuid));

        int position = -1;

        try {
            position = m_ssj.getOrderIndex(slideUuid);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlidePosition", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlidePosition", e);
            e.printStackTrace();
        }

        return position;
    }

    public int getCurrentTabPosition() {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.getCurrentTabPosition: returning %d", m_currentTabPosition));

        return m_currentTabPosition;
    }

    public void setCurrentTabPosition(int position) {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.setCurrentTabPosition: %d", position));

        m_currentTabPosition = position;
        m_viewPager.setCurrentItem(m_currentTabPosition);
    }

    public int getSlideCount() {
        if(D)Log.d(TAG, "PlaySlidesActivity.getSlideCount");

        int count = 0;
        try {
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlideCount", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesActivity.getSlideCount", e);
            e.printStackTrace();
        }

        return count;
    }

    public boolean isFromUrl() {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.isFromUrl: %b", m_isFromUrl));

        return m_isFromUrl;
    }

    public boolean getOrientationChangedFlag() {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.getOrientationChangedFlag: %b", m_fOrientationChanged));

        return m_fOrientationChanged;
    }

    protected void initializeStoriService()
    {
        if(D)Log.d(TAG, "PlaySlidesActivity.initializeStoriService");

        Intent service = new Intent(this, StoriService.class);

        // Call startService always, unless we are dealing with an orientation change. We call
        // startService in both the case of being launched from a URL, as well as being launched
        // from EditPlayActivity.
        if (!m_fOrientationChanged) {
            if(D)Log.d(TAG, "PlaySlidesActivity.initializeStoriService - calling startService in order to stay connected due to orientation change");
            startService(service);
        }

        m_fOrientationChanged = false;

        if(D)Log.d(TAG, "PlaySlidesActivity.initializeStoriService - calling bindService");
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeStoriService()
    {
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.uninitializeStoriService: m_fOrientationChanged=%b", m_fOrientationChanged));

        // Always call unbindService
        if (m_storiService != null && m_connection != null)
        {
            if(D)Log.d(TAG, "EditPlayActivity.uninitializeStoriService - calling unbindService");
            unbindService(m_connection);
        }

        // Call stopService if we're not dealing with an orientation change AND PlaySlidesActivity
        // was launched from a play url. We do not want to call stopService if PlaySlidesActivity
        // was invoked from EditPlayActivity.
        if (!m_fOrientationChanged && m_isFromUrl)
        {
            if(D)Log.d(TAG, "PlaySlidesActivity.uninitializeStoriService - calling stopService");
            Intent service = new Intent(this, StoriService.class);
            stopService(service);
        }

        m_storiService = null;
    }

    public ServiceConnection m_connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if(D)Log.d(TAG, "PlaySlidesActivity.onServiceConnected");

            m_storiService = ((StoriService.StoriServiceBinder)service).getService();

            // Tell subscribing fragments
            // Clone the arraylist so that mods on the array list due to actions in the callback do
            // not result in a ConcurrentModificationException.
            ArrayList<StoriService.StoriServiceConnectionListener> storiServiceConnectionListeners =
                    new ArrayList<StoriService.StoriServiceConnectionListener>(m_storiServiceConnectionListeners);

            for (StoriService.StoriServiceConnectionListener nscl : storiServiceConnectionListeners) {
                nscl.onServiceConnected(m_storiService);
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            if(D)Log.d(TAG, "PlaySlidesActivity.onServiceDisconnected");

            m_storiService = null;

            // Tell subscribing fragments
            // Clone the arraylist so that mods on the array list due to actions in the callback do
            // not result in a ConcurrentModificationException.
            ArrayList<StoriService.StoriServiceConnectionListener> storiServiceConnectionListeners =
                    new ArrayList<StoriService.StoriServiceConnectionListener>(m_storiServiceConnectionListeners);

            for (StoriService.StoriServiceConnectionListener nscl : storiServiceConnectionListeners) {
                nscl.onServiceDisconnected();
            }
        }
    };

    public void registerStoriServiceConnectionListener(StoriService.StoriServiceConnectionListener listener) {
        if(D)Log.d(TAG, "PlaySlidesActivity.registerStoriServiceConnectionListener");

        if (!m_storiServiceConnectionListeners.contains(listener)) {
            m_storiServiceConnectionListeners.add(listener);
        }
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.registerStoriServiceConnectionListener: now have %d listeners", m_storiServiceConnectionListeners.size()));

        if (m_storiService != null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.registerStoriServiceConnectionListener - already have m_storiService, so tell listener about it now");
            listener.onServiceConnected(m_storiService);
        }
    }

    public void unregisterStoriServiceConnectionListener(StoriService.StoriServiceConnectionListener listener) {
        if(D)Log.d(TAG, "PlaySlidesActivity.unregisterStoriServiceConnectionListener");

        if (m_storiServiceConnectionListeners.contains(listener)) {
            m_storiServiceConnectionListeners.remove(listener);
        }
        if(D)Log.d(TAG, String.format("PlaySlidesActivity.unregisterStoriServiceConnectionListener: now have %d listeners", m_storiServiceConnectionListeners.size()));
    }
}
