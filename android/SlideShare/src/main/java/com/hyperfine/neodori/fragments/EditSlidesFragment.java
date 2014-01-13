package com.hyperfine.neodori.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageSwitcher;
import android.widget.ViewSwitcher;

import com.hyperfine.neodori.AboutActivity;
import com.hyperfine.neodori.CloudStore;
import com.hyperfine.neodori.Config;
import com.hyperfine.neodori.EditPlayActivity;
import com.hyperfine.neodori.EditSlidesActivity;
import com.hyperfine.neodori.HorizontalListView;
import com.hyperfine.neodori.PlaySlidesActivity;
import com.hyperfine.neodori.R;
import com.hyperfine.neodori.SSPreferences;
import com.hyperfine.neodori.SlideJSON;
import com.hyperfine.neodori.SlideShareJSON;
import com.hyperfine.neodori.Utilities;
import com.hyperfine.neodori.adapters.ImageGalleryAdapter;
import com.hyperfine.neodori.cloudproviders.AmazonSharedPreferencesWrapper;
import com.hyperfine.neodori.cloudproviders.GoogleLogin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditSlidesFragment extends Fragment implements CloudStore.ICloudStoreCallback {
    public final static String TAG = "EditSlidesFragment";

    private final static int REQUEST_IMAGE = 1;
    private final static int REQUEST_CAMERA = 2;
    private final static String INSTANCE_STATE_IMAGEFILE = "instance_state_imagefile";
    private final static String INSTANCE_STATE_AUDIOFILE = "instance_state_audiofile";
    private final static String INSTANCE_STATE_SLIDEUUID = "instance_state_slideuuid";
    private final static String INSTANCE_STATE_CURRENTSLIDEINDEX = "instance_state_currentslideindex";
    private final static String INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH = "instance_state_currentcameraphotofilepath";

    private SharedPreferences m_prefs = null;
    private SlideShareJSON m_ssj = null;
    private String m_userUuid = null;
    private int m_currentSlideIndex = 0;
    private boolean m_isRecording = false;
    private boolean m_isPlaying = false;
    private MediaRecorder m_recorder;
    private MediaPlayer m_player;
    private FileInputStream m_fileInputStream;
    private Activity m_activityParent;
    private String m_slideShareName;
    private String m_slideUuid = null;
    private String m_imageFileName = null;
    private String m_audioFileName = null;
    private ImageGalleryAdapter m_imageGalleryAdapter;
    private Button m_buttonRecord;
    private Button m_buttonPlayStop;
    private HorizontalListView m_horizontalListView;
    private ImageSwitcher m_imageSwitcherSelected;
    private ProgressDialog m_progressDialog = null;
    private String m_currentCameraPhotoFilePath = null;
    private int m_displayWidth = 0;
    private int m_displayHeight = 0;

    //**********************************************************************************************
    //
    // Initialization methods
    //
    //**********************************************************************************************

    //
    // newInstance
    // Creates a new instance of the fragment
    //
    private static EditSlidesFragment newInstance(String slideShareName) {
        if(D)Log.d(TAG, "EditSlidesFragment.newInstance");

        EditSlidesFragment f = new EditSlidesFragment();

        f.initializeEditSlidesFragment(slideShareName);

        return f;
    }

    //
    // initializeEditSlidesFragment
    // This method sets the guid name of the slide show, which represents the folder
    // name of the storage path. It then calls initializeSlideShareJSON to kick off
    // the creation or retrieval of the SlideShareJSON file.
    //
    // This method is called by the parent activity at the time of onAttachFragment,
    // and is also called in the flow of reinitializing as a result of the user
    // changing accounts.
    //
    public void initializeEditSlidesFragment(String name) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.initializeEditSlidesFragment: %s", name));

        m_slideShareName = name;

        initializeSlideShareJSON();
    }

    //
    // setSlideShareTitle
    // Sets the title in the SlideShareJSON file.
    //
    public void setSlideShareTitle(String title) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.setSlideShareTitle: %s", title));

        if (title != null) {
            try {
                m_ssj.setTitle(title);
                m_ssj.save(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "EditSlidesFragment.setSlideShareTitle", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "EditSlidesFragment.setSlideShareTitle", e);
                e.printStackTrace();
            }
        }
    }

    //
    // initializeSlideShareJSON
    // Key method in initializing the EditSlidesFragment state. Loads or creates
    // the m_ssj SlideShareJSON structure.
    //
    private void initializeSlideShareJSON() {
        if(D)Log.d(TAG, "EditSlidesFragment.initializeSlideShareJSON");

        m_ssj = SlideShareJSON.load(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);
        if (m_ssj == null) {
            try {
                m_ssj = new SlideShareJSON(m_activityParent);
                m_ssj.save(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "EditSlidesFragment.initializeSlideShareJSON (FATAL)", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "EditSlidesFragment.initializeSlideShareJSON (FATAL)", e);
                e.printStackTrace();
            }
        }

        try {
            String title = m_ssj.getTitle();
            getActivity().getActionBar().setTitle(title == null ? getString(R.string.default_neodori_title) : title);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.initializeSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.initializeSlideShareJSON", e);
            e.printStackTrace();
        }

        if(D)Log.d(TAG, "EditSlidesFragment.initializeSlideShareJSON: here is the current JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    //
    // initializeNewSlideShow
    // Helper method called by initializeForChangeInAccount and createNewSlideShow.
    // This method is critical for initializing state, similar to the fragment
    // lifecycle flow, but without recreating the activity/fragment.
    //
    private void initializeNewSlideShow() {
        if(D)Log.d(TAG, "EditSlidesFragment.initializeNewSlideShow");

        //
        // Delete the old slide share directory and create a new one.
        // Create a new m_slideShareName
        //

        if (m_slideShareName != null) {
            Utilities.deleteSlideShareDirectory(m_activityParent, m_slideShareName);
        }
        m_slideShareName = UUID.randomUUID().toString();

        if(D)Log.d(TAG, String.format("EditSlidesFragment.initializeNewSlideShow - new slideShareName: %s", m_slideShareName));

        SharedPreferences.Editor edit = m_prefs.edit();
        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, m_slideShareName);
        edit.commit();

        Utilities.createOrGetSlideShareDirectory(m_activityParent, m_slideShareName);

        enterSlideShareTitleAndRecreate();
    }

    //
    // enterSlideShareTitleAndRecreate
    // Part of the newly created show flow, provides UI for entering a title,
    // then kicks off the initialization flow via a call to setSlideShareName.
    //
    private void enterSlideShareTitleAndRecreate() {
        if(D)Log.d(TAG, "EditSlidesActivity.enterSlideShareTitleAndRecreate");

        final EditText titleText = new EditText(m_activityParent);
        titleText.setHint(getString(R.string.main_new_title_hint));
        titleText.setSingleLine();

        AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
        adb.setTitle(getString(R.string.main_new_title_title)); // BUGBUG - TODO - rename from main
        adb.setCancelable(false);
        adb.setView(titleText);
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleText.getText().toString();

                dialog.dismiss();

                initializeEditSlidesFragment(m_slideShareName);
                setSlideShareTitle(title);

                m_currentSlideIndex = 0;
                m_imageGalleryAdapter = new ImageGalleryAdapter();
                m_imageGalleryAdapter.setContext(m_activityParent);
                m_imageGalleryAdapter.setSlideShareJSON(m_ssj);
                m_imageGalleryAdapter.setSlideShareName(m_slideShareName);
                m_horizontalListView.setAdapter(m_imageGalleryAdapter);

                selectSlide(m_currentSlideIndex);
                getActivity().getActionBar().setTitle(title == null ? getString(R.string.default_neodori_title) : title);
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    //
    // initializeForChangeInAccount
    // Called in response to the user selecting a new account.
    //
    public void initializeForChangeInAccount() {
        if(D)Log.d(TAG, "EditSlidesFragment.initializeForChangeInAccount");

        initializeNewSlideShow();
    }

    //
    // createNewSlideShow
    // Called in response to the user choosing to create a new slide show.
    //
    private void createNewSlideShow() {
        if(D)Log.d(TAG, "EditSlidesFragment.createNewSlideShow");

        AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
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


    //**********************************************************************************************
    //
    // General helper methods
    //
    //**********************************************************************************************

    public void setImageFileName(String fileName) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.setImageFileName: %s", fileName));

        m_imageFileName = fileName;
    }

    public void setAudioFileName(String fileName) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.setAudioFileName: %s", fileName));

        m_audioFileName = fileName;
    }

    public void setSlideUuid(String s) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.setSlideUuid: %s", s));

        m_slideUuid = s;
    }

    //
    // initializeNewSlide
    // Initializes and creates a new slide through the call to updateSlideShareJSON.
    // Stops any playing audio.
    //
    private void initializeNewSlide(int newIndex) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.initializeNewSlide: newIndex=%d", newIndex));

        if (newIndex < 0) {
            m_currentSlideIndex = 0;
        }
        else {
            m_currentSlideIndex = newIndex;
        }

        m_imageFileName = null;
        m_audioFileName = null;
        m_slideUuid = UUID.randomUUID().toString();

        updateSlideShareJSON();

        m_buttonPlayStop.setEnabled(false);
    }

    //
    // initializeSlide
    // Initializes the slide represented by uuidSlide to be the
    // current slide being viewed.
    //
    private void initializeSlide(String uuidSlide) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.initializeSlide(%s)", uuidSlide));

        m_imageFileName = null;
        m_audioFileName = null;
        m_slideUuid = UUID.randomUUID().toString();

        try {
            SlideJSON sj = m_ssj.getSlide(uuidSlide);
            m_imageFileName = sj.getImageFilename();
            m_audioFileName = sj.getAudioFilename();
            m_slideUuid = uuidSlide;

            m_currentSlideIndex = m_ssj.getOrderIndex(uuidSlide);
            if(D)Log.d(TAG, String.format("EditSlidesFragment.initializeSlide: m_currentSlideIndex=%d", m_currentSlideIndex));

            m_buttonPlayStop.setEnabled(hasAudio());
            fillImage();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.initializeSlide", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.initializeSlide", e);
            e.printStackTrace();
        }
    }

    //
    // deleteSlide
    // Deletes the currenly viewed slide.
    //
    private void deleteSlide() {
        if(D)Log.d(TAG, "EditSlidesFragment.deleteSlide");
        if(D)Log.d(TAG, "Before slide deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        if (m_imageFileName != null) {
            Utilities.deleteFile(m_activityParent, m_slideShareName, m_imageFileName);
            m_imageFileName = null;
        }

        if (m_audioFileName != null) {
            Utilities.deleteFile(m_activityParent, m_slideShareName, m_audioFileName);
            m_audioFileName = null;
        }

        try {
            m_ssj.removeSlide(m_slideUuid);
            m_ssj.save(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.deleteSlide", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.deleteSlide", e);
            e.printStackTrace();
        }

        m_slideUuid = null;

        if(D)Log.d(TAG, "After slide deletion:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    public void publishSlides() {
        if(D)Log.d(TAG, "EditSlidesFragment.publishSlides");

        AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
        adb.setTitle(getString(R.string.publish_dialog_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.publish_dialog_message));
        adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                CloudStore cloudStore = new CloudStore(m_activityParent, m_userUuid,
                        m_slideShareName, Config.CLOUD_STORAGE_PROVIDER, EditSlidesFragment.this);

                cloudStore.saveAsync();

                m_progressDialog = new ProgressDialog(m_activityParent);
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

    public void deleteCurrentSlide() {
        if(D)Log.d(TAG, "EditSlidesFragment.deleteCurrentSlide: position");

        // Deletes the slide and sets the current slide to the same index or
        // creates a new slide if at the end of the order array.

        try {
            int oldIndex = m_ssj.getOrderIndex(m_slideUuid);
            deleteSlide();

            String slideUuid = m_ssj.getSlideUuidByOrderIndex(oldIndex);

            if (slideUuid == null) {
                // Check for whether we deleted from the end of the list
                slideUuid = m_ssj.getSlideUuidByOrderIndex(oldIndex - 1);
                if (slideUuid == null) {
                    initializeNewSlide(0);
                }
                else {
                    initializeSlide(slideUuid);
                }
            }
            else {
                initializeSlide(slideUuid);
            }
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.onDeleteButtonClicked", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.onDeleteButtonClicked", e);
            e.printStackTrace();
        }

        m_imageGalleryAdapter.notifyDataSetChanged();
        fillImage();
    }

    public void selectSlide(int position) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.selectSlide: position=%d", position));

        m_currentSlideIndex = position;

        String uuidSlide = null;
        try {
            uuidSlide = m_ssj.getSlideUuidByOrderIndex(m_currentSlideIndex);
        }
        catch (Exception e) {
            if(D)Log.d(TAG, "EditSlidesFragment.onItemSelected", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(D)Log.d(TAG, "EditSlidesFragment.onItemSelected", e);
            e.printStackTrace();
        }

        if (uuidSlide == null) {
            initializeNewSlide(m_currentSlideIndex);
        }
        else {
            initializeSlide(uuidSlide);
        }
    }

    public void selectImageFromGallery() {
        if(D)Log.d(TAG, "EditSlidesFragment.selectImageFromGallery");

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    public void selectImageFromCamera() {
        if(D)Log.d(TAG, "EditSlidesFragment.selectImageFromCamera");

        String imageFileName = getNewImageFileName();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SlideShare";
        File pictureDirPath = new File(path);
        if (!pictureDirPath.exists()) {
            if(D)Log.d(TAG, "****** creating directory");
            pictureDirPath.mkdir();
        }

        m_currentCameraPhotoFilePath = pictureDirPath + "/" + imageFileName;
        if(D)Log.d(TAG, String.format("CreateSlidesFragment.selectImageFromCamera: m_currentCameraPhotoFilePath=%s", m_currentCameraPhotoFilePath));

        File imageFile = new File(m_currentCameraPhotoFilePath);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    public void onSaveComplete(CloudStore.SaveErrors se, SlideShareJSON ssj) {
        if(D)Log.d(TAG, String.format("CreateSlidesFragment.onSaveComplete: se=%s", se));

        if (ssj != null) {
            m_ssj = ssj;
        }

        if (m_progressDialog != null) {
            m_progressDialog.dismiss();
            m_progressDialog = null;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
        adb.setCancelable(false);

        if (se == CloudStore.SaveErrors.Success) {
            adb.setTitle(getString(R.string.upload_dialog_complete_title));
            adb.setMessage(getString(R.string.upload_dialog_complete_message_format));
            adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    Utilities.shareShow(m_activityParent, m_userUuid, m_slideShareName);
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

    private void updateSlideShareJSON() {
        if(D)Log.d(TAG, "EditSlidesFragment.updateSlideShareJSON");
        if(D)Log.d(TAG, "Current JSON:");
        Utilities.printSlideShareJSON(TAG, m_ssj);

        try {
            String imageUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, m_imageFileName);
            String audioUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, m_audioFileName);

            m_ssj.upsertSlide(m_slideUuid, m_currentSlideIndex, imageUrl, audioUrl);
            m_ssj.save(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);

            m_currentSlideIndex = m_ssj.getOrderIndex(m_slideUuid);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.updateSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.updateSlideShareJSON", e);
            e.printStackTrace();
        }

        m_imageGalleryAdapter.notifyDataSetChanged();
        fillImage();

        if(D)Log.d(TAG, "After update:");
        Utilities.printSlideShareJSON(TAG, m_ssj);
    }

    private void fillImage() {
        if(D)Log.d(TAG, "EditSlidesFragment.fillImage");

        if (m_imageFileName == null) {
            m_imageSwitcherSelected.setImageResource(R.drawable.ic_defaultslideimage);
            return;
        }

        int targetW = m_imageSwitcherSelected.getWidth();
        int targetH = m_imageSwitcherSelected.getHeight();

        // BUGBUG - note that getWidth/getHeight always returns zero at
        // this point of the fragment life cycle. As a temporary work around,
        // use the screen dimensions if this is the case.
        // See issue #9
        if (targetW == 0 || targetH == 0) {
            targetW = m_displayWidth;
            targetH = m_displayHeight;
        }

        try {
            String filePath = Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_imageFileName);
            Bitmap bitmap = Utilities.getConstrainedBitmap(filePath, targetW, targetH);

            Drawable drawableImage = new BitmapDrawable(m_activityParent.getResources(), bitmap);
            m_imageSwitcherSelected.setImageDrawable(drawableImage);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.fillImage", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.fillImage", e);
            e.printStackTrace();
        }
    }

    private static String getNewImageFileName() {
        return UUID.randomUUID().toString() + ".jpg";
    }

    private static String getNewAudioFileName() {
        return UUID.randomUUID().toString() + ".3gp";
    }


    //**********************************************************************************************
    //
    // Fragment lifecycle overrides
    //
    //**********************************************************************************************

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onCreate");

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            if(D)Log.d(TAG, "EditSlidesFragment.onCreate - populating from savedInstanceState");

            setImageFileName(savedInstanceState.getString(INSTANCE_STATE_IMAGEFILE));
            setAudioFileName(savedInstanceState.getString(INSTANCE_STATE_AUDIOFILE));
            setSlideUuid(savedInstanceState.getString(INSTANCE_STATE_SLIDEUUID));
            m_currentCameraPhotoFilePath = savedInstanceState.getString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH);
            m_currentSlideIndex = savedInstanceState.getInt(INSTANCE_STATE_CURRENTSLIDEINDEX);
        }

        if (m_slideUuid == null) {
            m_slideUuid = UUID.randomUUID().toString();
        }

        if(D)Log.d(TAG, String.format("EditSlidesFragment.onCreate - m_slideUuid=%s", m_slideUuid));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(INSTANCE_STATE_IMAGEFILE, m_imageFileName);
        savedInstanceState.putString(INSTANCE_STATE_AUDIOFILE, m_audioFileName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDEUUID, m_slideUuid);
        savedInstanceState.putString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH, m_currentCameraPhotoFilePath);
        savedInstanceState.putInt(INSTANCE_STATE_CURRENTSLIDEINDEX, m_currentSlideIndex);
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "EditSlidesFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "EditSlidesFragment.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "EditSlidesFragment.onResume");

        super.onResume();

        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);
        if(D)Log.d(TAG, String.format("EditSlidesFragment.onResume: m_userUuid=%s", m_userUuid));

        selectSlide(m_currentSlideIndex);
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "EditSlidesFragment.onAttach");

        super.onAttach(activity);

        m_activityParent = activity;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        m_displayWidth = size.x;
        m_displayHeight = size.y;

        if(D)Log.d(TAG, String.format("EditSlidesFragment.onAttach: displayWidth=%d, displayHeight=%d", m_displayWidth, m_displayHeight));

        m_prefs = m_activityParent.getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void onDetach() {
        if(D)Log.d(TAG, "EditSlidesFragment.onDetach");

        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onCreateView");

        View view = inflater.inflate(R.layout.fragment_editslides, container, false);

        m_imageGalleryAdapter = new ImageGalleryAdapter();
        m_imageGalleryAdapter.setContext(m_activityParent);
        m_imageGalleryAdapter.setSlideShareJSON(m_ssj);
        m_imageGalleryAdapter.setSlideShareName(m_slideShareName);

        m_horizontalListView = (HorizontalListView)view.findViewById(R.id.photo_gallery);
        m_horizontalListView.setAdapter(m_imageGalleryAdapter);
        m_horizontalListView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(D)Log.d(TAG, String.format("EditSlidesFragment.onHorizontalListViewItemSelected: position=%d", position));

                if (position != m_currentSlideIndex) {
                    stopPlaying();
                }

                selectSlide(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if(D)Log.d(TAG, "EditSlidesFragment.onHorizontalListViewNothingSelected");
            }
        });

        m_imageSwitcherSelected = (ImageSwitcher)view.findViewById(R.id.selected_image);

        m_buttonRecord = (Button)view.findViewById(R.id.control_record);
        m_buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, String.format("EditSlidesFragment.onRecordButtonClicked: %s recording", m_isRecording ? "Stopping" : "Starting"));

                if (m_isRecording) {
                    stopRecording();
                }
                else {
                    startRecording();
                }
            }
        });

        m_buttonPlayStop = (Button)view.findViewById(R.id.control_playback);
        m_buttonPlayStop.setEnabled(hasAudio());
        m_buttonPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, String.format("EditSlidesFragment.onPlayStopButtonClicked: %s playing", m_isPlaying ? "Stopping" : "Starting"));

                if (m_isPlaying) {
                    stopPlaying();
                }
                else {
                    startPlaying();
                }
            }
        });

        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if(D)Log.d(TAG, "EditSlidesFragment.onPrepareOptionsMenu");

        boolean isPublished = SlideShareJSON.isSlideSharePublished(m_activityParent, m_slideShareName);
        boolean hasCamera = Utilities.isCameraAvailable(m_activityParent);

        menu.findItem(R.id.menu_edit_item_camera).setVisible(hasCamera);
        menu.findItem(R.id.menu_edit_share).setVisible(isPublished);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if(D)Log.d(TAG, "EditSlidesFragment.onCreateOptionsMenu");

        String title = "";
        try {
            title = m_ssj.getTitle();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.onCreateOptionsMenu", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.onCreateOptionsMenu", e);
            e.printStackTrace();
        }

        inflater.inflate(R.menu.menu_slide, menu);

        MenuItem selectImage = menu.findItem(R.id.menu_edit_item_selectimage);
        selectImage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectImageFromGallery();
                return true;
            }
        });

        MenuItem cameraImage = menu.findItem(R.id.menu_edit_item_camera);
        cameraImage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectImageFromCamera();
                return true;
            }
        });

        MenuItem deleteItem = menu.findItem(R.id.menu_edit_item_delete);
        deleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                deleteCurrentSlide();
                return true;
            }
        });

        MenuItem insertBefore = menu.findItem(R.id.menu_edit_item_insertbefore);
        insertBefore.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                initializeNewSlide(m_currentSlideIndex);
                return true;
            }
        });

        MenuItem insertAfter = menu.findItem(R.id.menu_edit_item_insertafter);
        insertAfter.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                initializeNewSlide(m_currentSlideIndex + 1);
                return true;
            }
        });

        MenuItem preview = menu.findItem(R.id.menu_edit_preview);
        preview.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(m_activityParent, PlaySlidesActivity.class);
                m_activityParent.startActivity(intent);
                return true;
            }
        });

        MenuItem publish = menu.findItem(R.id.menu_edit_publish);
        publish.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                publishSlides();
                return true;
            }
        });

        MenuItem shareSlides = menu.findItem(R.id.menu_edit_share);
        shareSlides.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Utilities.shareShow(m_activityParent, m_userUuid, m_slideShareName);
                return true;
            }
        });

        MenuItem createNew = menu.findItem(R.id.menu_edit_create_new);
        createNew.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                createNewSlideShow();
                return true;
            }
        });

        MenuItem switchAccount = menu.findItem(R.id.menu_edit_switch_account);
        switchAccount.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
                adb.setTitle(getString(R.string.switch_account_dialog_title));
                adb.setCancelable(true);
                adb.setMessage(getString(R.string.switch_account_dialog_message));
                adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(D)Log.d(TAG, "EditSlidesActivity.onMenuClick - switching account");
                        dialog.dismiss();

                        String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME, SSPreferences.DEFAULT_EDITPROJECTNAME);

                        Utilities.deleteSlideShareDirectory(m_activityParent, slideShareName);

                        if(D)Log.d(TAG, "EditSlidesActivity.onMenuClick - switching account: nulling out PREFS_EDITPROJECTNAME");
                        SharedPreferences.Editor edit = m_prefs.edit();
                        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME, null);
                        edit.commit();

                        EditPlayActivity.s_amazonClientManager.clearCredentials();
                        EditPlayActivity.s_amazonClientManager.wipe();

                        Intent intent = new Intent(m_activityParent, GoogleLogin.class);
                        m_activityParent.startActivityForResult(intent, EditSlidesActivity.REQUEST_GOOGLE_LOGIN_FROM_FRAGMENT);
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

        MenuItem about = menu.findItem(R.id.menu_edit_about);
        about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(m_activityParent, AboutActivity.class);
                m_activityParent.startActivity(intent);

                return true;
            }
        });

        MenuItem test = menu.findItem(R.id.menu_edit_editplaytest);
        test.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(m_activityParent, EditPlayActivity.class);
                m_activityParent.startActivity(intent);

                return true;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        m_imageSwitcherSelected.setFactory((ViewSwitcher.ViewFactory) m_activityParent);

        fillImage();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            if(D)Log.d(TAG, String.format("EditSlidesFragment.onActivityResult: intent data = %s", intent.getData().toString()));

            String imageFileName = m_imageFileName;
            if (imageFileName == null) {
                imageFileName = getNewImageFileName();
            }

            boolean success = Utilities.copyGalleryImageToJPG(m_activityParent, m_slideShareName, imageFileName, intent);

            if (success) {
                // Display the image only upon successful save
                m_imageFileName = imageFileName;
                fillImage();
            }
            else {
                // Clean up - remove the image file
                Utilities.deleteFile(m_activityParent, m_slideShareName, imageFileName);
                m_imageFileName = null;
            }

            updateSlideShareJSON();
        }
        else if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            if(D)Log.d(TAG, String.format("CreateSlidesFragment.onActivityResult for REQUEST_CAMERA: m_currentCameraPhotoFilePath=%s", m_currentCameraPhotoFilePath));

            if (m_currentCameraPhotoFilePath == null) {
                if(D)Log.d(TAG, "CreateSlidesFragment.onActivityResult - m_currentCameraPhotoFilePath is null. This shouldn't happen. Bailing.");
                return;
            }

            // Inform the gallery of the new photo
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(m_currentCameraPhotoFilePath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            m_activityParent.sendBroadcast(mediaScanIntent);

            String imageFileName = m_imageFileName;
            if (imageFileName == null) {
                imageFileName = getNewImageFileName();
            }

            boolean success = Utilities.copyExternalStorageImageToJPG(m_activityParent, m_slideShareName, imageFileName, m_currentCameraPhotoFilePath);
            m_currentCameraPhotoFilePath = null;

            if (success) {
                // Display the image only upon successful save
                m_imageFileName = imageFileName;
                fillImage();
            }
            else {
                // Clean up - remove the image file
                Utilities.deleteFile(m_activityParent, m_slideShareName, imageFileName);
                m_imageFileName = null;
            }

            updateSlideShareJSON();
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }


    //**********************************************************************************************
    //
    // Audio methods
    //
    //**********************************************************************************************

    private void startRecording() {
        if(D)Log.d(TAG, "EditSlidesFragment.startRecording");

        if (m_isRecording) {
            if(D)Log.d(TAG, "EditSlidesFragment.startRecording - m_isRecording is true, so bailing");
            return;
        }

        if (m_audioFileName == null) {
            m_audioFileName = getNewAudioFileName();
            updateSlideShareJSON();
        }

        String filePath = Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName);
        if(D)Log.d(TAG, String.format("EditSlidesFragment.startRecording: filePath=%s", filePath));

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        m_recorder.setOutputFile(filePath);

        try {
            m_recorder.prepare();
            m_recorder.start();
            m_isRecording = true;
            m_buttonRecord.setText("Stop recording");
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startRecording", e);
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if(D)Log.d(TAG, "EditSlidesFragment.stopRecording");

        if (!m_isRecording) {
            if(D)Log.d(TAG, "EditSlidesFragment.stopRecording - m_isRecording is false so bailing");
            return;
        }

        m_recorder.stop();
        m_recorder.release();
        m_recorder = null;

        m_isRecording = false;
        m_buttonRecord.setText("Record");

        m_buttonPlayStop.setEnabled(true);
    }

    private void startPlaying() {
        if(D)Log.d(TAG, "EditSlidesFragment.startPlaying");

        if (m_isPlaying) {
            if(D)Log.d(TAG, "EditSlidesFragment.startPlaying - m_isPlaying is true, so bailing");
            return;
        }

        m_player = new MediaPlayer();
        m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(D)Log.d(TAG, "EditSlidesFragment.onPlaybackCompletion");
                stopPlaying();
            }
        });

        try {
            String filePath = Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName);
            if(D)Log.d(TAG, String.format("EditSlidesFragment.startPlaying: filePath=%s", filePath));

            File file = new File(filePath);
            boolean retVal = file.setReadable(true, false);
            if(D)Log.d(TAG, String.format("EditSlidesFragment.startPlaying - set readable permissions on audio file returns %b", retVal));
            m_fileInputStream = new FileInputStream(file);

            m_player.setDataSource(m_fileInputStream.getFD());
            m_player.prepare();
            m_player.start();

            m_isPlaying = true;
            m_buttonPlayStop.setText("Stop playing");
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "EditSlidesFragment.stopPlaying");

        if (!m_isPlaying) {
            if(D)Log.d(TAG, "EditSlidesFragment.stopPlaying - m_isPlaying is false, so bailing");
            return;
        }

        try {
            if (m_fileInputStream != null) {
                m_fileInputStream.close();
            }
        }
        catch (Exception e) {
            if(E)Log.d(TAG, "EditSlidesFragment.stopPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.d(TAG, "EditSlidesFragment.stopPlaying", e);
            e.printStackTrace();
        }
        finally {
            m_fileInputStream = null;
        }

        m_player.release();
        m_player = null;

        m_isPlaying = false;
        m_buttonPlayStop.setText("Play");
    }

    private boolean hasAudio() {
        return m_audioFileName != null;
    }
}
