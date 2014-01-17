package com.hyperfine.neodori;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

public class EditPlayActivity extends FragmentActivity implements ViewSwitcher.ViewFactory, CloudStore.ICloudStoreCallback {
    public final static String TAG = "EditPlayActivity";
    public final static String EXTRA_FROMURL = "extra_from_url";

    private final static String INSTANCE_STATE_CURRENT_TAB = "instance_state_current_tab";
    private final static String INSTANCE_STATE_EDITPLAYMODE = "instance_state_editplaymode";
    private final static String INSTANCE_STATE_ORIENTATION_CHANGED = "instance_state_orientation_changed";

    private SharedPreferences m_prefs;
    private String m_userUuid = null;
    private SlideShareJSON m_ssj;
    private EditPlayPagerAdapter m_editPlayPagerAdapter;
    private ViewPager.OnPageChangeListener m_pageChangeListener;
    private ViewPager m_viewPager;
    private File m_slideShareDirectory;
    private String m_slideShareName;
    private int m_currentTabPosition = 0;
    private int m_orientation;
    private boolean m_fOrientationChanged = false;
    private boolean m_fStartingPlaySlidesActivity = false;
    private boolean m_loadedFromSavedInstanceState = false;
    private EditPlayMode m_editPlayMode = EditPlayMode.Edit;
    private ProgressDialog m_progressDialog = null;
    private NeodoriService m_neodoriService = null;

    public final static int REQUEST_GOOGLE_PLAY_SERVICES_ERROR = 1;
    public final static int REQUEST_GOOGLE_LOGIN = 2;
    public final static int REQUEST_GOOGLE_LOGIN_FROM_SWITCHACCOUNT = 3;
    public final static int REQUEST_IMAGE = 4;
    public final static int REQUEST_CAMERA = 5;

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
                if(D)Log.d(TAG, String.format("EditPlayActivity.onCreate: userUuidString=%s, userEmail=%s, so calling GoogleLogin", userUuidString, userEmail));
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

            m_fOrientationChanged = savedInstanceState.getBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, false);
        }

        initializeViewPager();

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

                setActionBarTitle();
            }

            @Override
            public void onPageScrollStateChanged(int position) {
            }
        };
        m_viewPager.setOnPageChangeListener(m_pageChangeListener);

        // Check for OOBE
        try {
            int count = m_ssj.getSlideCount();
            if (count <= 0) {
                if(D)Log.d(TAG, "EditPlayActivity.onCreate - OOBE case. Create first slide");
                initializeNewSlide(m_currentTabPosition);
            }
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.onCreate", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.onCreate", e);
            e.printStackTrace();
        }

        if (m_editPlayMode != EditPlayMode.Edit) {
            getActionBar().hide();
        }
    }

    private void setActionBarTitle() {
        if(D)Log.d(TAG, "EditPlayActivity.setActionBarTitle");

        try {
            int count = m_ssj.getSlideCount();
            String title = m_ssj.getTitle();
            title = (title == null) ? getString(R.string.default_neodori_title) : title;

            String format = getString(R.string.editplay_actionbar_format);
            getActionBar().setTitle(String.format(format, m_currentTabPosition + 1, count, title));
        }
        catch (Exception e) {
            if(E)Log.d(TAG, "EditPlayActivity.onPageSelected", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.d(TAG, "EditPlayActivity.onPageSelected", e);
            e.printStackTrace();
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

        int orientation = getResources().getConfiguration().orientation;
        m_fOrientationChanged = m_orientation != orientation;

        savedInstanceState.putInt(INSTANCE_STATE_CURRENT_TAB, m_currentTabPosition);
        savedInstanceState.putInt(INSTANCE_STATE_EDITPLAYMODE, m_editPlayMode.getValue());
        savedInstanceState.putBoolean(INSTANCE_STATE_ORIENTATION_CHANGED, m_fOrientationChanged);
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

        m_fStartingPlaySlidesActivity = false;
        initializeNeodoriService();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "EditPlayActivity.onStop");

        super.onStop();

        uninitializeNeodoriService();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "EditPlayActivity.onResume");

        super.onResume();

        m_orientation = getResources().getConfiguration().orientation;
        if(D)Log.d(TAG, String.format("EditPlayActivity.onResume: orientation = %d", m_orientation));

        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
        if(D)Log.d(TAG, String.format("EditPlayActivity.onResume: m_userUuid=%s", m_userUuid));

        m_viewPager.setCurrentItem(m_currentTabPosition);

        setActionBarTitle();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "EditPlayActivity.onAttachFragment");
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES_ERROR) {
            if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - returned from Google Play Services error dialog. Finishing.");
            finish();
        }
        else if (requestCode == REQUEST_GOOGLE_LOGIN) {
            if (resultCode == RESULT_OK) {
                if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - return from successful Google login.");
            }
            else {
                // BUGBUG - handle login failure
                if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - failed to login to Google. Finishing. TODO: handle with grace.");
                finish();
            }
        }
        else if (requestCode == REQUEST_GOOGLE_LOGIN_FROM_SWITCHACCOUNT) {
            if (resultCode == RESULT_OK) {
                if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - handling REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT");
                initializeForChangeInAccount();
            }
            else {
                // BUGBUG - handle login failure
                if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - failed to login to Google. Finishing. TODO: handle with grace.");
                finish();
            }
        }
        else {
            if(D)Log.d(TAG, "EditPlayActivity.onActivityResult - passing on to super.onActivityResult");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(D)Log.d(TAG, "EditPlayActivity.onPrepareOptionsMenu");

        boolean isPublished = SlideShareJSON.isSlideSharePublished(this, m_slideShareName);

        menu.findItem(R.id.menu_editplayactivity_share).setVisible(isPublished);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(D)Log.d(TAG, "EditPlayActivity.onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_editplayactivity, menu);

        MenuItem preview = menu.findItem(R.id.menu_editplayactivity_preview);
        preview.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                m_fStartingPlaySlidesActivity = true;
                Intent intent = new Intent(EditPlayActivity.this, PlaySlidesActivity.class);
                startActivity(intent);
                return true;
            }
        });

        MenuItem publish = menu.findItem(R.id.menu_editplayactivity_publish);
        publish.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                publishSlides();
                return true;
            }
        });

        MenuItem shareSlides = menu.findItem(R.id.menu_editplayactivity_share);
        shareSlides.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Utilities.shareShow(EditPlayActivity.this, m_userUuid, m_slideShareName);
                return true;
            }
        });

        MenuItem createNew = menu.findItem(R.id.menu_editplayactivity_createnew);
        createNew.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                createNewSlideShow();
                return true;
            }
        });

        MenuItem switchAccount = menu.findItem(R.id.menu_editplayactivity_switchaccount);
        switchAccount.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder adb = new AlertDialog.Builder(EditPlayActivity.this);
                adb.setTitle(getString(R.string.switch_account_dialog_title));
                adb.setCancelable(true);
                adb.setMessage(getString(R.string.switch_account_dialog_message));
                adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(D)Log.d(TAG, "EditPlayActivity.onMenuClick - switching account");
                        dialog.dismiss();

                        String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);

                        Utilities.deleteSlideShareDirectory(EditPlayActivity.this, slideShareName);

                        if(D)Log.d(TAG, "EditPlayActivity.onMenuClick - switching account: nulling out PREFS_EDITPROJECTNAME");
                        SharedPreferences.Editor edit = m_prefs.edit();
                        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, null);
                        edit.commit();

                        s_amazonClientManager.clearCredentials();
                        s_amazonClientManager.wipe();

                        Intent intent = new Intent(EditPlayActivity.this, GoogleLogin.class);
                        startActivityForResult(intent, REQUEST_GOOGLE_LOGIN_FROM_SWITCHACCOUNT);
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

        MenuItem about = menu.findItem(R.id.menu_editplayactivity_about);
        about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(EditPlayActivity.this, AboutActivity.class);
                startActivity(intent);

                return true;
            }
        });

        return true;
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "EditPlayActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    //
    // setSlideShareTitle
    // Sets the title in the SlideShareJSON file.
    //
    private void setSlideShareTitle(String title) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.setSlideShareTitle: %s", title));

        if (title != null) {
            try {
                m_ssj.setTitle(title);
                m_ssj.save(this, m_slideShareName, Config.slideShareJSONFilename);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "EditPlayActivity.setSlideShareTitle", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "EditPlayActivity.setSlideShareTitle", e);
                e.printStackTrace();
            }
        }
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

        setActionBarTitle();

        if(D)Log.d(TAG, "EditPlayActivity.initializeSlideShareJSON: here is the JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    //
    // initializeNewSlideShow
    // Helper method called by initializeForChangeInAccount and createNewSlideShow.
    // This method is critical for initializing state, similar to the fragment
    // lifecycle flow, but without recreating the activity/fragment.
    //
    private void initializeNewSlideShow() {
        if(D)Log.d(TAG, "EditPlayActivity.initializeNewSlideShow");

        //
        // Delete the old slide share directory and create a new one.
        // Create a new m_slideShareName
        //

        if (m_slideShareName != null) {
            Utilities.deleteSlideShareDirectory(this, m_slideShareName);
        }
        m_slideShareName = UUID.randomUUID().toString();

        if(D)Log.d(TAG, String.format("EditPlayActivity.initializeNewSlideShow - new slideShareName: %s", m_slideShareName));

        SharedPreferences.Editor edit = m_prefs.edit();
        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, m_slideShareName);
        edit.commit();

        Utilities.createOrGetSlideShareDirectory(this, m_slideShareName);

        enterSlideShareTitleAndRecreate();
    }

    //
    // enterSlideShareTitleAndRecreate
    // Part of the newly created show flow, provides UI for entering a title,
    // then kicks off the initialization flow via a call to setSlideShareName.
    //
    private void enterSlideShareTitleAndRecreate() {
        if(D)Log.d(TAG, "EditPlayActivity.enterSlideShareTitleAndRecreate");

        final EditText titleText = new EditText(this);
        titleText.setHint(getString(R.string.main_new_title_hint));
        titleText.setSingleLine();

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.main_new_title_title)); // BUGBUG - TODO - rename from main
        adb.setCancelable(false);
        adb.setView(titleText);
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleText.getText().toString();

                dialog.dismiss();

                initializeSlideShareJSON();
                setSlideShareTitle(title);

                m_currentTabPosition = 0;
                initializeViewPager();
                initializeNewSlide(m_currentTabPosition);

                setActionBarTitle();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    public int getSlideCount() {
        if(D)Log.d(TAG, "EditPlayActivity.getSlideCount");

        int count = 0;
        try {
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.getSlideCount", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.getSlideCount", e);
            e.printStackTrace();
        }

        return count;
    }

    //
    // initializeForChangeInAccount
    // Called in response to the user selecting a new account.
    //
    public void initializeForChangeInAccount() {
        if(D)Log.d(TAG, "EditPlayActivity.initializeForChangeInAccount");

        initializeNewSlideShow();
    }

    //
    // createNewSlideShow
    // Called in response to the user choosing to create a new slide show.
    //
    private void createNewSlideShow() {
        if(D)Log.d(TAG, "EditPlayActivity.createNewSlideShow");

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.main_create_alert_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.main_create_alert_message));
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                initializeNewSlideShow();
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

    public void deleteAudio(String slideUuid, String audioFileName) {
        if(D)Log.d(TAG, "EditPlayActivity.deleteAudio");
        if(D)Log.d(TAG, "Before audio deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        if (audioFileName != null) {
            Utilities.deleteFile(this, m_slideShareName, audioFileName);
        }

        String imageFileName = null;

        try {
            SlideJSON sj = m_ssj.getSlide(slideUuid);
            imageFileName = sj.getImageFilename();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteAudio", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteAudio", e);
            e.printStackTrace();
        }

        updateSlideShareJSON(slideUuid, imageFileName, null, true);
    }

    public void deleteImage(String slideUuid, String imageFileName) {
        if(D)Log.d(TAG, "EditPlayActivity.deleteImage");
        if(D)Log.d(TAG, "Before image deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        if (imageFileName != null) {
            Utilities.deleteFile(this, m_slideShareName, imageFileName);
        }

        String audioFileName = null;

        try {
            SlideJSON sj = m_ssj.getSlide(slideUuid);
            audioFileName = sj.getAudioFilename();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteImage", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteImage", e);
            e.printStackTrace();
        }

        updateSlideShareJSON(slideUuid, null, audioFileName, true);
    }

    public void deleteSlide(String slideUuid, String imageFileName, String audioFileName) {
        if(D)Log.d(TAG, "EditPlayActivity.deleteSlide");
        if(D)Log.d(TAG, "Before slide deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        int count = 0;

        if (imageFileName != null) {
            Utilities.deleteFile(this, m_slideShareName, imageFileName);
        }

        if (audioFileName != null) {
            Utilities.deleteFile(this, m_slideShareName, audioFileName);
        }

        try {
            m_ssj.removeSlide(slideUuid);
            m_ssj.save(this, m_slideShareName, Config.slideShareJSONFilename);
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteSlide", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.deleteSlide", e);
            e.printStackTrace();
        }

        if(D)Log.d(TAG, "After slide deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        initializeViewPager();

        if (m_currentTabPosition >= count) {
            m_currentTabPosition--;
        }
        m_viewPager.setCurrentItem(m_currentTabPosition);
        setActionBarTitle();
    }

    public void publishSlides() {
        if(D)Log.d(TAG, "EditPlayActivity.publishSlides");

        AlertDialog.Builder adb = new AlertDialog.Builder(EditPlayActivity.this);
        adb.setTitle(getString(R.string.publish_dialog_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.publish_dialog_message));
        adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                CloudStore cloudStore = new CloudStore(EditPlayActivity.this, m_userUuid,
                        m_slideShareName, Config.CLOUD_STORAGE_PROVIDER, EditPlayActivity.this);

                m_progressDialog = new ProgressDialog(EditPlayActivity.this);
                m_progressDialog.setTitle(getString(R.string.upload_dialog_title));
                m_progressDialog.setCancelable(false);
                m_progressDialog.setIndeterminate(true);
                m_progressDialog.show();

                cloudStore.saveAsync();
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

    public void onSaveComplete(CloudStore.SaveErrors se, SlideShareJSON ssj) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.onSaveComplete: se=%s", se));

        if (ssj != null) {
            m_ssj = ssj;
        }

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

                    Utilities.shareShow(EditPlayActivity.this, m_userUuid, m_slideShareName);
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

    public void updateSlideShareJSON(String slideUuid, String imageFileName, String audioFileName) {
        updateSlideShareJSON(slideUuid, imageFileName, audioFileName, false);
    }

    public void updateSlideShareJSON(String slideUuid, String imageFileName, String audioFileName, boolean forceNulls) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.updateSlideShareJSON: slideUuid=%s, imageFileName=%s, audioFileName=%s, forceNulls=%b", slideUuid, imageFileName, audioFileName, forceNulls));
        if(D)Log.d(TAG, "Current JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        boolean needsAdapterUpdate = false;

        try {
            String imageUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, imageFileName);
            String audioUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, audioFileName);

            needsAdapterUpdate = (m_ssj.getSlide(slideUuid) == null);

            m_ssj.upsertSlide(slideUuid, m_currentTabPosition, imageUrl, audioUrl, forceNulls);
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

        if (needsAdapterUpdate) {
            initializeViewPager();
            //m_editPlayPagerAdapter.notifyDataSetChanged();
        }

        if(D)Log.d(TAG, "After update:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    public int getSlidePosition(String slideUuid) {
        if(D)Log.d(TAG, String.format("EditPlayActivity.getSlidePosition: slideUuid=%s", slideUuid));

        int position = -1;

        try {
            position = m_ssj.getOrderIndex(slideUuid);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayActivity.getSlidePosition", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayActivity.getSlidePosition", e);
            e.printStackTrace();
        }

        return position;
    }

    public int getCurrentTabPosition() {
        if(D)Log.d(TAG, String.format("EditPlayActivity.getCurrentTabPosition: returning %d", m_currentTabPosition));

        return m_currentTabPosition;
    }

    public NeodoriService getNeodoriService() {
        return m_neodoriService;
    }

    protected void initializeNeodoriService()
    {
        if(D)Log.d(TAG, "EditPlayActivity.initializeNeodoriService");

        Intent service = new Intent(this, NeodoriService.class);

        // We call startService always, unless this is due to a change in orientation.
        if (!m_fOrientationChanged) {
            if(D)Log.d(TAG, "EditPlayActivity.initializeNeodoriService - calling startService in order to stay connected due to orientation change");
            startService(service);
        }

        m_fOrientationChanged = false;

        // We always call bindService.
        if(D)Log.d(TAG, "EditPlayActivity.initializeNeodoriService - calling bindService");
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeNeodoriService()
    {
        if(D)Log.d(TAG, String.format("EditPlayActivity.uninitializeNeodoriService: m_fOrientationChanged=%b", m_fOrientationChanged));

        // We always call unbindService.
        if (m_neodoriService != null && m_connection != null)
        {
            if(D)Log.d(TAG, "EditPlayActivity.uninitializeNeodoriService - calling unbindService");
            unbindService(m_connection);
        }

        // We call stopService UNLESS we're dealing with an orientation change, OR we're
        // launching PlaySlidesActivity. This will keep the instance of NeodoriService running
        // in transitions between EditPlayActivity and PlaySlidesActivity.
        //
        // Note: We might consider allowing NeodoriService to shut down and restart between
        // transitions between the two activities. This would be possible if there are no
        // Neodori service semantics that need to run during the transition. If that's the
        // case, we can remove the check for !m_fStartingPlaySlidesActivity in the conditional.
        if (!m_fOrientationChanged && !m_fStartingPlaySlidesActivity)
        {
            if(D)Log.d(TAG, "EditPlayActivity.uninitializeNeodoriService - calling stopService");
            Intent service = new Intent(this, NeodoriService.class);
            stopService(service);
        }

        m_neodoriService = null;
    }

    public ServiceConnection m_connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if(D)Log.d(TAG, "EditPlayActivity.onServiceConnected");

            m_neodoriService = ((NeodoriService.NeodoriServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            if(D)Log.d(TAG, "EditPlayActivity.onServiceDisconnected");

            m_neodoriService = null;
        }
    };
}
