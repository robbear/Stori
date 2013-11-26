package com.hyperfine.slideshare.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageSwitcher;
import android.widget.ViewSwitcher;

import com.hyperfine.slideshare.CloudStore;
import com.hyperfine.slideshare.Config;
import com.hyperfine.slideshare.R;
import com.hyperfine.slideshare.SSPreferences;
import com.hyperfine.slideshare.SlideJSON;
import com.hyperfine.slideshare.SlideShareJSON;
import com.hyperfine.slideshare.Utilities;
import com.hyperfine.slideshare.adapters.ImageGalleryAdapter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class EditSlidesFragment extends Fragment {
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
    private int m_currentSlideIndex = -1;
    private boolean m_isRecording = false;
    private boolean m_isPlaying = false;
    private MediaRecorder m_recorder;
    private MediaPlayer m_player;
    private Activity m_activityParent;
    private String m_slideShareName;
    private String m_slideUuid = null;
    private String m_imageFileName = null;
    private String m_audioFileName = null;
    private ImageGalleryAdapter m_imageGalleryAdapter;
    private Button m_buttonRecord;
    private Button m_buttonPlayStop;
    private Gallery m_gallery;
    private ImageSwitcher m_imageSwitcherSelected;
    private ProgressDialog m_progressDialog = null;
    private String m_currentCameraPhotoFilePath = null;

    private static EditSlidesFragment newInstance(String slideShareName) {
        if(D)Log.d(TAG, "EditSlidesFragment.newInstance");

        EditSlidesFragment f = new EditSlidesFragment();

        f.setSlideShareName(slideShareName);

        return f;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("EditSlidesFragment.setSlideShareName: %s", name));

        m_slideShareName = name;

        //
        // Note: setSlideShareName is called only by the parent activity and is done
        // at the time of onAttachFragment. It's only at this point we can have the
        // parent activity context and load or create the SlideShareJSON file.
        //
        initializeSlideShareJSON();
    }

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

    private void initializeNewSlide() {
        if(D)Log.d(TAG, "EditSlidesFragment.initializeNewSlide");

        m_currentSlideIndex = -1;

        m_imageFileName = null;
        m_audioFileName = null;

        m_buttonPlayStop.setEnabled(false);
        fillImage();

        m_slideUuid = UUID.randomUUID().toString();
    }

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

            int count = m_ssj.getSlideCount();
            int index = m_ssj.getOrderIndex(uuidSlide);

            if (index == 0) {
                // BUGBUG
                //m_buttonPrev.setEnabled(false);
            }
            else if (index < 0 || index == count - 1) {
                // BUGBUG
                //m_buttonPrev.setEnabled(true);
            }
            else {
                // BUGBUG
                //m_buttonPrev.setEnabled(true);
            }

            m_currentSlideIndex = index;
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

    private void deleteSlide() {
        if(D)Log.d(TAG, "EditSlidesFragment.deleteSlide");
        if(D)Log.d(TAG, "Before slide deletion:");
        Utilities.printSlideShareJSON(m_ssj);

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
        Utilities.printSlideShareJSON(m_ssj);
    }

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

        if(D)Log.d(TAG, "EditSlidesFragment.initializeSlideShareJSON: here is the current JSON:");
        Utilities.printSlideShareJSON(m_ssj);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onCreate");

        super.onCreate(savedInstanceState);

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
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "EditSlidesFragment.onAttach");

        super.onAttach(activity);

        m_activityParent = activity;

        m_prefs = m_activityParent.getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
        m_userUuid = m_prefs.getString(SSPreferences.PREFS_USERUUID, null);

        // if (activity instanceof SomeActivityInterface) {
        // }
        // else {
        //     throw new ClassCastException(activity.toString() + " must implement SomeActivityInterface");
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

        m_gallery = (Gallery)view.findViewById(R.id.photo_gallery);
        m_gallery.setAdapter(m_imageGalleryAdapter);
        m_gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(D)Log.d(TAG, String.format("EditSlidesFragment.onGalleryItemSelected: position=%d", position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if(D)Log.d(TAG, String.format("EditSlidesFragment.onGalleryNothingSelected"));
            }
        });

        /* BUGBUG
        m_buttonCamera = (Button)view.findViewById(R.id.control_camera);
        m_buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "CreateSlidesFragment.onCameraButtonClicked");

                String imageFileName = getNewImageFileName();
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SlideShare";
                File pictureDirPath = new File(path);
                if (!pictureDirPath.exists()) {
                    if(D)Log.d(TAG, "****** creating directory");
                    pictureDirPath.mkdir();
                }

                m_currentCameraPhotoFilePath = pictureDirPath + "/" + imageFileName;
                if(D)Log.d(TAG, String.format("CreateSlidesFragment.onCameraButtonClicked: m_currentCameraPhotoFilePath=%s", m_currentCameraPhotoFilePath));

                File imageFile = new File(m_currentCameraPhotoFilePath);

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        });
        if (!Utilities.isCameraAvailable(m_activityParent)) {
            m_buttonCamera.setVisibility(View.GONE);
        }
        */

        /* BUGBUG
        m_buttonSelectImage = (Button)view.findViewById(R.id.control_selectimage);
        m_buttonSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (D) Log.d(TAG, "EditSlidesFragment.onSelectImageButtonClicked");

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
        */

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

        /* BUGBUG
        m_buttonPrev = (Button)view.findViewById(R.id.control_prev);
        m_buttonPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditSlidesFragment.onPrevButtonClicked");

                String uuidSlidePrev = null;
                try {
                    if (m_currentSlideIndex < 0) {
                        uuidSlidePrev = m_ssj.getSlideUuidByOrderIndex(m_ssj.getSlideCount() - 1);
                    }
                    else {
                        uuidSlidePrev = m_ssj.getPreviousSlideUuid(m_slideUuid);
                    }
                }
                catch (Exception e) {
                    if(D)Log.d(TAG, "EditSlidesFragment.onPrevButtonClicked, e");
                    e.printStackTrace();
                }
                catch (OutOfMemoryError e) {
                    if(D)Log.d(TAG, "EditSlidesFragment.onPrevButtonClicked, e");
                    e.printStackTrace();
                }

                if (uuidSlidePrev == null) {
                    if(D)Log.d(TAG, "EditSlidesFragment.onPrevButtonClicked - already at first slide. The button should have been disabled. Doing nothing.");
                    return;
                }
                else {
                    initializeSlide(uuidSlidePrev);
                }
            }
        });

        m_buttonPublish = (Button)view.findViewById(R.id.control_publish);
        m_buttonPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "CreateSlidesFragment.onPublishButtonClicked");

                AlertDialog.Builder adb = new AlertDialog.Builder(m_activityParent);
                adb.setTitle(getString(R.string.publish_dialog_title));
                adb.setCancelable(true);
                adb.setMessage(getString(R.string.publish_dialog_message));
                adb.setPositiveButton(getString(R.string.yes_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        CloudStore cloudStore = new CloudStore(m_activityParent, m_userUuid,
                                m_slideShareName, Config.CLOUD_STORAGE_PROVIDER, CreateSlidesFragment.this);

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
        });

        m_buttonDelete = (Button)view.findViewById(R.id.control_deleteslide);
        m_buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditSlidesFragment.onDeleteButtonClicked");

                // Deletes the slide and sets the current slide to the same index or
                // creates a new slide if at the end of the order array.

                try {
                    int oldIndex = m_ssj.getOrderIndex(m_slideUuid);
                    deleteSlide();

                    String slideUuid = m_ssj.getSlideUuidByOrderIndex(oldIndex);

                    if (slideUuid == null) {
                        initializeNewSlide();
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
            }
        });

        m_buttonNext = (Button)view.findViewById(R.id.control_next);
        m_buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditSlidesFragment.onNextButtonClicked");

                String uuidSlideNext = null;
                try {
                    uuidSlideNext = m_ssj.getNextSlideUuid(m_slideUuid);
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "EditSlidesFragment.onNextButtonClicked.e");
                    e.printStackTrace();
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "EditSlidesFragment.onNextButtonClicked.e");
                    e.printStackTrace();
                }

                if (uuidSlideNext == null) {
                    initializeNewSlide();
                }
                else {
                    initializeSlide(uuidSlideNext);
                }
            }
        });
        */

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditSlidesFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        m_imageSwitcherSelected.setFactory((ViewSwitcher.ViewFactory)m_activityParent);

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
        Utilities.printSlideShareJSON(m_ssj);

        int count = 0;
        try {
            String imageUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, m_imageFileName);
            String audioUrl = Utilities.buildResourceUrlString(m_userUuid, m_slideShareName, m_audioFileName);

            m_ssj.upsertSlide(m_slideUuid, m_currentSlideIndex, imageUrl, audioUrl);
            m_ssj.save(m_activityParent, m_slideShareName, Config.slideShareJSONFilename);

            m_currentSlideIndex = m_ssj.getOrderIndex(m_slideUuid);
            count = m_ssj.getSlideCount();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditSlidesFragment.updateSlideShareJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditSlidesFragment.updateSlideShareJSON", e);
            e.printStackTrace();
        }

        if(D)Log.d(TAG, "After update:");
        Utilities.printSlideShareJSON(m_ssj);

        setDiagnosticOutput(count, m_currentSlideIndex);
    }

    private void setDiagnosticOutput(int count, int index) {
        if(D)Log.d(TAG, "EditSlidesFragment.setDiagnosticOutput");

        /* BUGBUG
        m_textViewCount.setText(String.format("Count: %d", count));
        m_textViewIndex.setText(String.format("Index: %d", index));
        */
    }

    private void fillImage() {
        if(D)Log.d(TAG, "EditSlidesFragment.fillImage");

        // BUGBUG - TEST
        if (m_ssj == null) {
            setDiagnosticOutput(0, -1);
        }
        else {
            try {
                int count = m_ssj.getSlideCount();
                setDiagnosticOutput(count, m_currentSlideIndex);
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

        if (m_imageFileName == null) {
            m_imageSwitcherSelected.setImageDrawable(null);
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_imageFileName));
        Drawable drawableImage = new BitmapDrawable(m_activityParent.getResources(), bitmap);
        m_imageSwitcherSelected.setImageDrawable(drawableImage);
    }

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

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //m_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        m_recorder.setOutputFile(Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName));

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
            m_player.setDataSource(Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName));
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

        m_player.release();
        m_player = null;

        m_isPlaying = false;
        m_buttonPlayStop.setText("Play");
    }

    private boolean isDirty() {
        return hasAudio() || hasImage();
    }

    private boolean hasAudio() {
        return m_audioFileName != null;
    }

    private boolean hasImage() {
        return m_imageFileName != null;
    }

    private static String getNewImageFileName() {
        return UUID.randomUUID().toString() + ".jpg";
    }

    private static String getNewAudioFileName() {
        return UUID.randomUUID().toString() + ".3gp";
    }
}
