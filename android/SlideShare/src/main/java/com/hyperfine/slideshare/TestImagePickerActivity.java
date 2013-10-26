package com.hyperfine.slideshare;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.hyperfine.slideshare.fragments.ImagePickerFragment;

import java.io.File;
import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class TestImagePickerActivity extends FragmentActivity implements ViewSwitcher.ViewFactory {

    public final static String TAG = "TestImagePickerActivity";

    private SharedPreferences m_prefs;
    private ImagePickerFragment m_imagePickerFragment;
    private File m_slideShareDirectory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "TestImagePickerActivity.onCreate");

        super.onCreate(savedInstanceState);
        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);

        setContentView(R.layout.activity_testimagepicker);

        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        // BUGBUG TODO: Replace with dialog to create/fetch SlideShare name
        m_slideShareDirectory = Utilities.createOrGetSlideShareDirectory(this, slideShareName);
        if (m_slideShareDirectory == null) {
            if(D)Log.d(TAG, "TestImagePickerActivity.onCreate - m_slideShareDirectory is null. Bad!!!");
        }

        FragmentManager fm = getSupportFragmentManager();
        m_imagePickerFragment = (ImagePickerFragment)fm.findFragmentByTag(ImagePickerFragment.class.toString());
        if (m_imagePickerFragment != null) {
            m_imagePickerFragment.setSlideShareName(slideShareName);
        }

        //
        // BUGBUG
        //
        runTests();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if(D)Log.d(TAG, "TestImagePickerActivity.onAttachFragment");

        if (fragment instanceof ImagePickerFragment) {
            if(D)Log.d(TAG, "TestImagePickerActivity.onAttachFragment - found our ImagePickerFragment");
            String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);
            m_imagePickerFragment = (ImagePickerFragment)fragment;
            m_imagePickerFragment.setSlideShareName(slideShareName);
        }
    }

    @Override
    public View makeView() {
        if(D)Log.d(TAG, "TestImagePickerActivity.makeView");

        ImageView view = new ImageView(this);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setLayoutParams(new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return view;
    }

    private void runTests() {
        if(D)Log.d(TAG, "TestImagePickerActivity.runTests");

        File rootDir = Utilities.getRootFilesDirectory(this);
        String slideShareName = m_prefs.getString(SSPreferences.PREFS_SSNAME, SSPreferences.DEFAULT_SSNAME);

        //
        // BUGBUG - TEST to list all files and directories
        //
        Utilities.listAllFilesAndDirectories(this, rootDir);

        //
        // BUGBUG - TEST to generate or load user UUID from preferences
        //
        UUID userUuid;
        String userUuidString = m_prefs.getString(SSPreferences.PREFS_USERUUID, null);
        if (userUuidString == null) {
            UUID uuid = UUID.randomUUID();
            if(D)Log.d(TAG, String.format("TestImagePickerActivity.onAttach - generated uuid=%s", uuid.toString()));
            userUuid = uuid;
        }
        else {
            userUuid = UUID.fromString(userUuidString);
        }
        if(D)Log.d(TAG, String.format("TestImagePickerActivity.onAttach - userUuid=%s", userUuid.toString()));
        SharedPreferences.Editor editor = m_prefs.edit();
        editor.putString(SSPreferences.PREFS_USERUUID, userUuid.toString());
        editor.commit();

        //
        // BUGBUG - TEST for SlideShareJSON methods
        //
        SlideShareJSON ssj;
        try {
            ssj = new SlideShareJSON();
            if(D)Log.d(TAG, String.format("Default SlideShareJSON: %s", ssj.toString()));
            String urlBase = Config.baseSlideShareUrl + userUuid.toString() + "/" + slideShareName + "/";

            String lastSlideUuid = "";
            for (int i = 0; i < 5; i++) {
                lastSlideUuid = UUID.randomUUID().toString();
                ssj.upsertSlide(lastSlideUuid, -1, String.format("%s%d.jpg", urlBase, i), String.format("%s%d.3gp", urlBase, i));
            }
            if(D)Log.d(TAG, String.format("SlideShareJSON after upsert (add) %d slides: %s", ssj.getSlides().length(), ssj.toString()));
            ssj.upsertSlide(lastSlideUuid, -1, String.format("%slastslide.jpg", urlBase), String.format("%slastslide.3gp", urlBase));
            if(D)Log.d(TAG, String.format("SlideShareJSON after upsert (update) %d slides: %s", ssj.getSlides().length(), ssj.toString()));
            SlideJSON slide = ssj.getSlide(lastSlideUuid);
            if(D)Log.d(TAG, String.format("SlideShareJSON getSlide(%s) returns %s", lastSlideUuid, slide.toString()));
            ssj.removeSlide(lastSlideUuid);
            if(D)Log.d(TAG, String.format("SlideShareJSON after remove. %d slides: %s", ssj.getSlides().length(), ssj.toString()));

            // Save and load tests
            boolean retVal = ssj.save(this, slideShareName, "test.json");
            if(D)Log.d(TAG, String.format("Saved SlideShareJSON with retVal=%b", retVal));
            Utilities.listAllFilesAndDirectories(this, rootDir);
            ssj = SlideShareJSON.load(this, slideShareName, "test.json");
            if(D)Log.d(TAG, String.format("SlideShareJSON after load: %s", ssj == null ? "null" : ssj.toString()));
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "TestImagePickerActivity.onAttach", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "TestImagePickerActivity.onAttach", e);
            e.printStackTrace();
        }
    }
}
