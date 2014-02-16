package com.stori.stori.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.stori.stori.AsyncCopyFileTask;
import com.stori.stori.AsyncTaskTimer;
import com.stori.stori.Config;
import com.stori.stori.EditPlayActivity;
import com.stori.stori.StoriService;
import com.stori.stori.R;
import com.stori.stori.SlideJSON;
import com.stori.stori.Utilities;

import java.io.File;
import java.util.UUID;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class EditPlayFragment extends Fragment implements
        AsyncTaskTimer.IAsyncTaskTimerCallback, StoriService.PlaybackStateListener,
        StoriService.RecordingStateListener, StoriService.StoriServiceConnectionListener,
        AsyncCopyFileTask.AsyncCopyFileTaskCallbacks {
    public final static String TAG = "EditPlayFragment";

    private final static String INSTANCE_STATE_IMAGEFILENAME = "instance_state_imagefilename";
    private final static String INSTANCE_STATE_AUDIOFILENAME = "instance_state_audiofilename";
    private final static String INSTANCE_STATE_SLIDETEXT = "instance_state_slidetext";
    private final static String INSTANCE_STATE_SLIDESHARENAME = "instance_state_slidesharename";
    private final static String INSTANCE_STATE_SLIDUUID = "instance_state_slideuuid";
    private final static String INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH = "instance_state_currentcameraphotofilepath";

    private EditPlayActivity m_editPlayActivity;
    private String m_slideShareName;
    private ImageSwitcher m_imageSwitcher;
    private ImageButton m_mainMenuControl;
    private ImageButton m_insertBeforeControl;
    private ImageButton m_insertAfterControl;
    private ImageButton m_selectPhotoControl;
    private ImageButton m_editControl;
    private ImageButton m_recordControl;
    private ImageButton m_playstopControl;
    private ImageButton m_trashControl;
    private ImageButton m_nextControl;
    private ImageButton m_prevControl;
    private View m_noPicturePanel;
    private TextView m_slideTextControl;
    private TextView m_slidePositionTextControl;
    private TextView m_titleControl;
    private View m_nextPrevPanel;
    private String m_imageFileName;
    private String m_audioFileName;
    private String m_slideText;
    private String m_slideUuid;
    private StoriService m_storiService = null;

    private String m_currentCameraPhotoFilePath = null;
    private EditPlayActivity.EditPlayMode m_editPlayMode = EditPlayActivity.EditPlayMode.Edit;

    public static EditPlayFragment newInstance(EditPlayActivity editPlayActivity, int position, String slideShareName, String slideUuid, SlideJSON sj) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.newInstance: slideShareName=%s, slideUuid=%s", slideShareName, slideUuid));

        EditPlayFragment f = new EditPlayFragment();

        f.setSlideShareName(slideShareName);
        f.setSlideJSON(slideUuid, sj);
        f.setEditPlayActivity(editPlayActivity);

        return f;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.setSlideShareName: %s", name));

        m_slideShareName = name;
    }

    public void setSlideJSON(String slideUuid, SlideJSON sj) {
        if(D)Log.d(TAG, "EditPlayFragment.setSlideJSON");

        try {
            m_slideUuid = slideUuid;
            m_imageFileName = sj.getImageFilename();
            m_audioFileName = sj.getAudioFilename();
            m_slideText = sj.getText();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayFragment.setSlideJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayFragment.setSlideJSON", e);
            e.printStackTrace();
        }
    }

    public void setEditPlayActivity(EditPlayActivity editPlayActivity) {
        if(D)Log.d(TAG, "EditPlayFragment.setEditPlayActivity");

        m_editPlayActivity = editPlayActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayFragment.onCreate");

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if(D)Log.d(TAG, "EditPlayFragment.onCreate - populating from savedInstanceState");

            m_audioFileName = savedInstanceState.getString(INSTANCE_STATE_AUDIOFILENAME);
            m_imageFileName = savedInstanceState.getString(INSTANCE_STATE_IMAGEFILENAME);
            m_slideText = savedInstanceState.getString(INSTANCE_STATE_SLIDETEXT);
            m_slideShareName = savedInstanceState.getString(INSTANCE_STATE_SLIDESHARENAME);
            m_slideUuid = savedInstanceState.getString(INSTANCE_STATE_SLIDUUID);
            m_currentCameraPhotoFilePath = savedInstanceState.getString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayFragment.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(INSTANCE_STATE_AUDIOFILENAME, m_audioFileName);
        savedInstanceState.putString(INSTANCE_STATE_IMAGEFILENAME, m_imageFileName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDETEXT, m_slideText);
        savedInstanceState.putString(INSTANCE_STATE_SLIDESHARENAME, m_slideShareName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDUUID, m_slideUuid);
        savedInstanceState.putString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH, m_currentCameraPhotoFilePath);
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "EditPlayFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "EditPlayFragment.onStart");

        super.onStart();

        initializeStoriService();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "EditPlayFragment.onStop");

        super.onStop();

        if (!m_editPlayActivity.getOrientationChangedFlag()) {
            stopPlaying();
            stopRecording();
        }

        uninitializeStoriService();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "EditPlayFragment.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onResume: m_slideUuid=%s", m_slideUuid));

        super.onResume();

        displayNextPrevControls();
        displaySlideTitleAndPosition();
        displaySlideTextControl();
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "EditPlayFragment.onAttach");

        super.onAttach(activity);

        m_editPlayActivity = (EditPlayActivity)activity;
    }

    @Override
    public void onDetach() {
        if(D)Log.d(TAG, "EditPlayFragment.onDetach");

        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayFragment.onCreateView");

        View view = inflater.inflate(R.layout.fragment_editplay, container, false);

        m_slidePositionTextControl = (TextView)view.findViewById(R.id.control_slide_position);
        m_titleControl = (TextView)view.findViewById(R.id.control_title);
        m_nextPrevPanel = view.findViewById(R.id.control_nextprev_panel);
        m_slideTextControl = (TextView)view.findViewById(R.id.slidetext_control);

        m_noPicturePanel = view.findViewById(R.id.no_picture_panel);
        m_noPicturePanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popUpChooseImageMenu(view);
            }
        });

        m_nextControl = (ImageButton)view.findViewById(R.id.control_next_slide);
        m_nextControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onNextControlClicked");

                int position = m_editPlayActivity.getSlidePosition(m_slideUuid);
                int count = m_editPlayActivity.getSlideCount();

                position++;
                if (position >= count) {
                    if(D)Log.d(TAG, "EditPlayFragment.onNextControlClicked - error in position, so bailing");
                    return;
                }

                m_editPlayActivity.setCurrentTabPosition(position);
            }
        });

        m_prevControl = (ImageButton)view.findViewById(R.id.control_previous_slide);
        m_prevControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onPrevControlClicked");

                int position = m_editPlayActivity.getSlidePosition(m_slideUuid);
                int count = m_editPlayActivity.getSlideCount();

                position--;
                if (position < 0) {
                    if(D)Log.d(TAG, "EditPlayFragment.onPrevControlClicked - error in position, so bailing");
                    return;
                }

                m_editPlayActivity.setCurrentTabPosition(position);
            }
        });

        m_mainMenuControl = (ImageButton)view.findViewById(R.id.control_main_menu);
        m_mainMenuControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(D)Log.d(TAG, "EditPlayFragment.onMainMenuClicked");

                PopupMenu pm = new PopupMenu(m_editPlayActivity, view);
                pm.inflate(R.menu.menu_editplayactivity);
                Menu menu = pm.getMenu();

                MenuItem preview = menu.findItem(R.id.menu_editplayactivity_preview);
                preview.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.previewSlides();
                        return true;
                    }
                });

                MenuItem publish = menu.findItem(R.id.menu_editplayactivity_publish);
                publish.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.publishSlides();
                        return true;
                    }
                });

                MenuItem rename = menu.findItem(R.id.menu_editplayactivity_rename);
                rename.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.renameStori();
                        return true;
                    }
                });

                if (m_editPlayActivity.isPublished()) {
                    MenuItem share = menu.findItem(R.id.menu_editplayactivity_share);
                    share.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            m_editPlayActivity.shareSlides();
                            return true;
                        }
                    });
                }
                else {
                    menu.removeItem(R.id.menu_editplayactivity_share);
                }

                MenuItem viewPublished = menu.findItem(R.id.menu_editplayactivity_viewpublished);
                viewPublished.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.launchStoriListActivity();
                        return true;
                    }
                });

                MenuItem createNew = menu.findItem(R.id.menu_editplayactivity_createnew);
                createNew.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.createNewSlideShow();
                        return true;
                    }
                });

                MenuItem settings = menu.findItem(R.id.menu_editplayactivity_settings);
                settings.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.launchSettingsActivity();
                        return true;
                    }
                });

                pm.show();
            }
        });

        m_trashControl = (ImageButton)view.findViewById(R.id.control_more);
        m_trashControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(D)Log.d(TAG, "EditPlayFragment.onTrashControlClicked");

                PopupMenu pm = new PopupMenu(m_editPlayActivity, view);
                pm.inflate(R.menu.menu_editplay_trash);
                Menu menu = pm.getMenu();

                MenuItem deleteSlide = menu.findItem(R.id.menu_editplay_more_deleteslide);
                deleteSlide.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int count = m_editPlayActivity.getSlideCount();
                        if (count > 1) {
                            m_editPlayActivity.deleteSlide(m_slideUuid, m_imageFileName, m_audioFileName);
                        }
                        else {
                            deleteSlideData();
                        }
                        return true;
                    }
                });

                if (hasImage()) {
                    MenuItem deleteImage = menu.findItem(R.id.menu_editplay_more_deleteimage);
                    deleteImage.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            m_editPlayActivity.deleteImage(m_slideUuid, m_imageFileName);
                            m_imageFileName = null;
                            renderImage();
                            return true;
                        }
                    });
                }
                else {
                    menu.removeItem(R.id.menu_editplay_more_deleteimage);
                }

                if (hasAudio()) {
                    MenuItem deleteAudio = menu.findItem(R.id.menu_editplay_more_deleteaudio);
                    deleteAudio.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            m_editPlayActivity.deleteAudio(m_slideUuid, m_audioFileName);
                            m_audioFileName = null;
                            displayPlayStopControl();
                            return true;
                        }
                    });
                }
                else {
                    menu.removeItem(R.id.menu_editplay_more_deleteaudio);
                }

                pm.show();
            }
        });

        m_selectPhotoControl = (ImageButton)view.findViewById(R.id.select_from_gallery_control);
        m_selectPhotoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(D)Log.d(TAG, "EditPlayFragment.onSelectPhotoControlClicked");

                popUpChooseImageMenu(view);
            }
        });

        m_editControl = (ImageButton)view.findViewById(R.id.control_edit);
        m_editControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(D)Log.d(TAG, "EditPlayFragment.onEditControlClicked");

                PopupMenu pm = new PopupMenu(m_editPlayActivity, view);
                pm.inflate(R.menu.menu_editplay_edit);
                Menu menu = pm.getMenu();

                MenuItem textMenu = menu.findItem(R.id.menu_editplay_edit_text);
                textMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        enterSlideText();
                        return true;
                    }
                });

                int count = m_editPlayActivity.getSlideCount();
                if (count > 1) {
                    MenuItem reorderMenu = menu.findItem(R.id.menu_editplay_edit_reorder);
                    reorderMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            m_editPlayActivity.reorder();
                            return true;
                        }
                    });
                }
                else {
                    menu.removeItem(R.id.menu_editplay_edit_reorder);
                }

                pm.show();
            }
        });

        m_recordControl = (ImageButton)view.findViewById(R.id.control_record);
        m_recordControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_storiService == null) {
                    return;
                }

                if(D)Log.d(TAG, String.format("EditPlayFormat.onRecordButtonClicked: %s recording", m_storiService.isRecording(m_audioFileName) ? "Stopping" : "Starting"));

                if (m_storiService.isRecording(m_audioFileName)) {
                    stopRecording();
                }
                else {
                    if (hasAudio()) {
                        AlertDialog.Builder adb = new AlertDialog.Builder(m_editPlayActivity);
                        adb.setTitle(getString(R.string.editplay_overwriteaudio_title));
                        adb.setCancelable(true);
                        adb.setMessage(getString(R.string.editplay_overwriteaudio_message));
                        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                startRecording();
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
                    else {
                        startRecording();
                    }
                }
            }
        });

        m_playstopControl = (ImageButton)view.findViewById(R.id.control_playback);
        displayPlayStopControl();
        m_playstopControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onPlayStopButtonClicked");

                if (m_storiService != null) {
                    StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

                    if (playbackState == StoriService.PlaybackState.Playing) {
                        if(D)Log.d(TAG, "EditPlayFragment.onPlayStopButtonClicked - playbackState is Playing. Calling stopPlaying");
                        stopPlaying();
                    }
                    else {
                        if(D)Log.d(TAG, "EditPlayFragment.onPlayStopButtonClicked - playbackState is not Playing. Calling startPlaying");
                        startPlaying();
                    }
                }
            }
        });

        m_insertBeforeControl = (ImageButton)view.findViewById(R.id.edit_prev_control);
        m_insertBeforeControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onInsertBeforeButtonClicked");
                int selectedTabPosition = m_editPlayActivity.getCurrentTabPosition();
                m_editPlayActivity.initializeNewSlide(selectedTabPosition);
            }
        });

        m_insertAfterControl = (ImageButton)view.findViewById(R.id.edit_next_control);
        m_insertAfterControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onInsertAfterButtonClicked");
                int selectedTabPosition = m_editPlayActivity.getCurrentTabPosition();
                m_editPlayActivity.initializeNewSlide(selectedTabPosition + 1);
            }
        });

        m_imageSwitcher = (ImageSwitcher)view.findViewById(R.id.current_image);
        m_imageSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onImageClicked");

                if (m_storiService != null && m_storiService.isRecording(m_audioFileName)) {
                    if(D)Log.d(TAG, "EditPlayFragment.onImageSwitcherClicked - we're recording, so do not allow toggle of overlay. Bailing.");
                    return;
                }

                switch (m_editPlayMode) {
                    case Edit:
                        /* NEVER - no more PlayEdit mode
                        m_editPlayMode = EditPlayActivity.EditPlayMode.PlayEdit;
                        setActivityEditPlayMode(m_editPlayMode);
                        updateOverlay();
                        */
                        break;

                    case PlayEdit:
                        m_editPlayMode = EditPlayActivity.EditPlayMode.Edit;
                        setActivityEditPlayMode(m_editPlayMode);
                        updateOverlay();
                        // No more audio stop upon tapping image
                        //stopPlaying();
                        break;

                    case Play:
                        if (m_audioFileName != null) {
                            if (m_storiService != null) {
                                StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

                                if (playbackState == StoriService.PlaybackState.Playing) {
                                    stopPlaying();
                                }
                                else {
                                    startPlaying();
                                }
                            }
                        }
                        break;
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        m_imageSwitcher.setFactory((ViewSwitcher.ViewFactory)m_editPlayActivity);

        // Seed the ImageSwitcher with a black background in order to inflate it
        // to non-zero dimensions.
        m_imageSwitcher.setImageResource(R.drawable.ic_black);
        renderImage();

        m_editPlayMode = getActivityEditPlayMode();
        updateOverlay();

        /* NEVER
        if (savedInstanceState == null) {
            asyncStartAudio();
        }
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == EditPlayActivity.REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            if(D)Log.d(TAG, String.format("EditPlayFragment.onActivityResult: intent data = %s", intent.getData().toString()));

            String imageFileName = m_imageFileName;
            if (imageFileName == null) {
                imageFileName = getNewImageFileName();
            }

            AsyncCopyFileTask acft = new AsyncCopyFileTask();
            acft.copyFile(AsyncCopyFileTask.CopyFileTaskType.Gallery, m_editPlayActivity, this, m_slideShareName, new String[]{imageFileName}, intent.getData(), null);
        }
        else if (requestCode == EditPlayActivity.REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
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
            m_editPlayActivity.sendBroadcast(mediaScanIntent);

            String imageFileName = m_imageFileName;
            if (imageFileName == null) {
                imageFileName = getNewImageFileName();
            }

            AsyncCopyFileTask acft = new AsyncCopyFileTask();
            acft.copyFile(AsyncCopyFileTask.CopyFileTaskType.Camera, m_editPlayActivity, this, m_slideShareName, new String[] {imageFileName}, null, m_currentCameraPhotoFilePath);
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }

    private void popUpChooseImageMenu(View view) {
        if(D)Log.d(TAG, "EditPlayFragment.popUpChooseImageMenu");

        PopupMenu pm = new PopupMenu(m_editPlayActivity, view);
        pm.inflate(R.menu.menu_editplay_image);
        Menu menu = pm.getMenu();

        MenuItem picture = menu.findItem(R.id.menu_editplay_image_picture);
        picture.setTitle(hasImage() ? getString(R.string.menu_editplay_image_replacepicture) : getString(R.string.menu_editplay_image_choosepicture));
        picture.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectImageFromGallery();;
                return true;
            }
        });

        MenuItem camera = menu.findItem(R.id.menu_editplay_image_camera);
        camera.setTitle(hasImage() ? getString(R.string.menu_editplay_image_replacecamera) : getString(R.string.menu_editplay_image_usecamera));
        camera.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                selectImageFromCamera();
                return true;
            }
        });

        pm.show();
    }

    private EditPlayActivity.EditPlayMode getActivityEditPlayMode() {
        EditPlayActivity.EditPlayMode epm = m_editPlayActivity.getEditPlayMode();
        if(D)Log.d(TAG, String.format("EditPlayFragment.getActivityEditPlayMode: %s", epm.toString()));

        return epm;
    }

    private void setActivityEditPlayMode(EditPlayActivity.EditPlayMode editPlayMode) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.setActivityEditPlayMode: %s", editPlayMode.toString()));
        m_editPlayActivity.setEditPlayMode(editPlayMode);
    }

    public void onTitleUpdated() {
        if(D)Log.d(TAG, "EditPlayFragment.onTitleUpdated");

        displaySlideTitleAndPosition();
    }

    public void onTabPageSelected(int position) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onTabPageSelected: position=%d", position));

        m_editPlayMode = getActivityEditPlayMode();
        updateOverlay();

        displayNextPrevControls();
        displaySlideTitleAndPosition();
        displaySlideTextControl();

        int tabPosition = m_editPlayActivity.getSlidePosition(m_slideUuid);

        if (tabPosition == position) {
            /* NEVER
            if(D)Log.d(TAG, "EditPlayFragment.onTabPageSelected - starting audio timer");
            asyncStartAudio();
            */
        }
        else {
            stopPlaying();
            stopRecording();
        }
    }

    private void asyncStartAudio() {
        if(D)Log.d(TAG, "EditPlayFragment.asyncStartAudio");

        if (m_editPlayMode == EditPlayActivity.EditPlayMode.Edit) {
            if(D)Log.d(TAG, "EditPlayFragment.asyncStartAudio - mode is Edit, so bailing");
            return;
        }

        AsyncTaskTimer.startAsyncTaskTimer(1, Config.audioDelayMillis, this);
    }

    public void onAsyncTaskTimerComplete(long cookie) {
        if(D)Log.d(TAG, "EditPlayFragment.onAsyncTaskTimerComplete");

        int selectedTabPosition = m_editPlayActivity.getCurrentTabPosition();
        int tabPosition = m_editPlayActivity.getSlidePosition(m_slideUuid);

        if(D)Log.d(TAG, String.format("EditPlayFragment.onAsyncTaskTimerComplete: selectedTabPosition=%d, tabPosition=%d", selectedTabPosition, tabPosition));

        if (selectedTabPosition == tabPosition && m_editPlayMode != EditPlayActivity.EditPlayMode.Edit) {
            startPlaying();
        }
        else {
            stopPlaying();
        }
    }

    private void enterSlideText() {
        if(D)Log.d(TAG, String.format("EditPlayFragment.enterSlideText: slideUuid=%s", m_slideUuid));

        String slideText = m_editPlayActivity.getSlideText(m_slideUuid);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(Config.maxSlideTextCharacters);

        final EditText editText = new EditText(m_editPlayActivity);
        editText.setText(slideText);
        editText.setHint(getString(R.string.editplay_entertext_hint));
        editText.setSingleLine(false);
        editText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setLines(Config.numberOfEditTextLinesForSlideText);
        editText.selectAll();
        editText.setFilters(filters);

        AlertDialog.Builder adb = new AlertDialog.Builder(m_editPlayActivity);
        adb.setTitle(getString(R.string.editplay_entertext_title));
        adb.setCancelable(true);
        adb.setView(editText);
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Utilities.unfreezeOrientation(m_editPlayActivity);

                String slideText = editText.getText().toString();
                m_editPlayActivity.updateSlideShareJSON(m_slideUuid, null, null, slideText);

                m_slideText = m_editPlayActivity.getSlideText(m_slideUuid);
                displaySlideTextControl();
            }
        });
        adb.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Utilities.unfreezeOrientation(m_editPlayActivity);
            }
        });
        adb.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                Utilities.unfreezeOrientation(m_editPlayActivity);
            }
        });

        // TODO: Fix #14 and #15. This is a temporary workaround.
        Utilities.freezeActivityOrientation(m_editPlayActivity);

        final AlertDialog ad = adb.create();

        if (!Utilities.deviceHasHardwareKeyboard(m_editPlayActivity)) {
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        if(D)Log.d(TAG, "EditPlayActivity.enterStoriText.editText.onFocusChangeListener");
                        ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    }
                }
            });
        }

        ad.show();
    }

    private void updateOverlay() {
        if(D)Log.d(TAG, String.format("EditPlayFragment.updateOverlay: %s", m_editPlayMode.toString()));

        View view = getView().findViewById(R.id.overlay_panel);
        if (m_editPlayMode == EditPlayActivity.EditPlayMode.Edit) {
            view.setVisibility(View.VISIBLE);
        }
        else {
            view.setVisibility(View.INVISIBLE);
        }
    }

    private void deleteSlideData() {
        if(D)Log.d(TAG, "EditPlayFragment.deleteSlideData");

        m_editPlayActivity.deleteImage(m_slideUuid, m_imageFileName);
        m_imageFileName = null;
        m_editPlayActivity.deleteAudio(m_slideUuid, m_audioFileName);
        m_audioFileName = null;
        m_slideText = null;
        displayPlayStopControl();
        displaySlideTextControl();
        renderImage();
    }

    private void renderImage() {
        if(D)Log.d(TAG, "EditPlayFragment.renderImage");

        if (m_imageFileName == null) {
            m_imageSwitcher.setImageDrawable(null);
            if (m_editPlayMode == EditPlayActivity.EditPlayMode.Play) {
                m_noPicturePanel.setVisibility(View.GONE);
            }
            else {
                m_noPicturePanel.setVisibility(View.VISIBLE);
            }
        }
        else {
            m_noPicturePanel.setVisibility(View.GONE);

            try {
                int targetW = m_imageSwitcher.getWidth();
                int targetH = m_imageSwitcher.getHeight();

                // BUGBUG - note that getWidth/getHeight always returns zero at
                // this point of the fragment life cycle. As a temporary work around,
                // use the screen dimensions if this is the case.
                // See issue #9
                if (targetW == 0 || targetH == 0) {
                    WindowManager wm = (WindowManager)m_editPlayActivity.getSystemService(Context.WINDOW_SERVICE);
                    Point outSize = new Point();
                    wm.getDefaultDisplay().getSize(outSize);
                    targetW = outSize.x;
                    targetH = outSize.y;
                }

                String filePath = Utilities.getAbsoluteFilePath(m_editPlayActivity, m_slideShareName, m_imageFileName);
                Bitmap bitmap = Utilities.getConstrainedBitmap(filePath, targetW, targetH);

                Drawable drawableImage = new BitmapDrawable(m_editPlayActivity.getResources(), bitmap);
                m_imageSwitcher.setImageDrawable(drawableImage);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "EditPlayFragment.renderImage", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "EditPlayFragment.renderImage", e);
                e.printStackTrace();
            }
        }
    }

    private void selectImageFromGallery() {
        if(D)Log.d(TAG, "EditPlayFragment.selectImageFromGallery");

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, EditPlayActivity.REQUEST_IMAGE);
    }

    public void selectImageFromCamera() {
        if(D)Log.d(TAG, "EditPlayFragment.selectImageFromCamera");

        String imageFileName = getNewImageFileName();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + Config.cameraImageFolderName;
        File pictureDirPath = new File(path);
        if (!pictureDirPath.exists()) {
            if(D)Log.d(TAG, "****** creating directory");
            pictureDirPath.mkdir();
        }

        m_currentCameraPhotoFilePath = pictureDirPath + "/" + imageFileName;
        if(D)Log.d(TAG, String.format("EditPlayFragment.selectImageFromCamera: m_currentCameraPhotoFilePath=%s", m_currentCameraPhotoFilePath));

        File imageFile = new File(m_currentCameraPhotoFilePath);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(intent, EditPlayActivity.REQUEST_CAMERA);
    }

    private void startRecording() {
        if(D)Log.d(TAG, "EditPlayFragment.startRecording");

        if (m_audioFileName == null) {
            m_audioFileName = getNewAudioFileName();
            m_editPlayActivity.updateSlideShareJSON(m_slideUuid, m_imageFileName, m_audioFileName, m_slideText);
        }

        boolean success = m_storiService.startRecording(m_slideShareName, m_audioFileName);
        if (success) {
            m_recordControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_stoprecording));
            enableControlsWhileRecording(false);
        }
    }

    private void stopRecording() {
        if(D)Log.d(TAG, "EditPlayFragment.stopRecording");

        if (m_storiService == null) {
            if(D)Log.d(TAG, "EditPlayFragment.stopRecording - m_storiService is null, so bailing");
            return;
        }

        boolean success = m_storiService.stopRecording(m_audioFileName);

        m_recordControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_record));
        enableControlsWhileRecording(true);

        if (!success) {
            if(D)Log.d(TAG, String.format("EditPlayFragment.stopRecording - failure. Cleaning up %s", m_audioFileName));
            m_editPlayActivity.deleteAudio(m_slideUuid, m_audioFileName);
            m_audioFileName = null;
        }

        displayPlayStopControl();
    }

    public void onRecordingTimeLimit(boolean success, String audioFileName) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onRecordingTimeLimit: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));

        if (m_audioFileName == null || !m_audioFileName.equals(audioFileName)) {
            if(D)Log.d(TAG, "EditPlayFragment.onRecordingTimeLimit - audioFileName doesn't match, so bailing");
            return;
        }

        playBeep();
        vibrateDevice();
        m_recordControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_record));
        enableControlsWhileRecording(true);

        if (!success) {
            if(D)Log.d(TAG, String.format("EditPlayFragment.onRecordingTimeLimit - failure. Cleaning up %s", m_audioFileName));
            m_editPlayActivity.deleteAudio(m_slideUuid, m_audioFileName);
            m_audioFileName = null;
        }

        displayPlayStopControl();
    }

    private void playBeep() {
        if(D)Log.d(TAG, "EditPlayFragment.playBeep");

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(m_editPlayActivity, notification);
            r.play();
        }
        catch (Exception e) {}
    }

    private void vibrateDevice() {
        if(D)Log.d(TAG, "EditPlayFragment.vibrateDevice");

        Vibrator v = (Vibrator)m_editPlayActivity.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(Config.recordingTimeoutVibrateMillis);
    }

    private void displaySlideTextControl() {
        if(D)Log.d(TAG, "EditPlayFragment.displaySlideTextControl");

        m_slideTextControl.setText(m_slideText);
        m_slideTextControl.setVisibility(hasText() ? View.VISIBLE : View.INVISIBLE);
    }

    private void displayPlayStopControl() {
        if(D)Log.d(TAG, "EditPlayFragment.displayPlayStopControl");

        m_playstopControl.setVisibility(hasAudio() ? View.VISIBLE : View.INVISIBLE);
    }

    private void displayNextPrevControls() {
        int count = m_editPlayActivity.getSlideCount();
        int position = m_editPlayActivity.getSlidePosition(m_slideUuid);

        if(D)Log.d(TAG, String.format("EditPlayFragment.displayNextPrevControls: position=%d, count=%d", position, count));

        int countSlides = m_editPlayActivity.getSlideCount();
        m_nextPrevPanel.setVisibility(countSlides >= Config.maxSlidesPerStoriForFree ? View.INVISIBLE : View.VISIBLE);

        if (position == 0) {
            m_prevControl.setVisibility(View.INVISIBLE);
            m_nextControl.setVisibility((count == 1) ? View.INVISIBLE : View.VISIBLE);
        }
        else if (position == count - 1) {
            m_nextControl.setVisibility(View.INVISIBLE);
            m_prevControl.setVisibility((count == 1) ? View.INVISIBLE : View.VISIBLE);
        }
        else {
            m_nextControl.setVisibility(View.VISIBLE);
            m_prevControl.setVisibility(View.VISIBLE);
        }
    }

    private void displaySlideTitleAndPosition() {
        int count = m_editPlayActivity.getSlideCount();
        int position = m_editPlayActivity.getSlidePosition(m_slideUuid);
        String title = m_editPlayActivity.getSlidesTitle();

        if(D)Log.d(TAG, String.format("EditPlayFragment.displaySlideTitleAndPosition: position=%d, count=%d, title=%s", position, count, title));

        m_titleControl.setText(title);
        m_slidePositionTextControl.setText(String.format(getString(R.string.slide_position_format), position + 1, count));
    }

    private void startPlaying() {
        if(D)Log.d(TAG, "EditPlayFragment.startPlaying");

        if (m_storiService == null) {
            if(D)Log.d(TAG, "EditPlayFragment.startPlaying - m_storiService is null, so bailing");
            return;
        }

        StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

        if (playbackState == StoriService.PlaybackState.Playing) {
            if(D)Log.d(TAG, "EditPlayFragment.startPlaying - playbackState is Playing, so bailing");
            return;
        }

        if (m_audioFileName == null) {
            if(D)Log.d(TAG, "EditPlayFragment.startPlaying - m_audioFileName is null, so bailing");
            return;
        }

        m_storiService.startAudio(m_slideShareName, m_audioFileName);

        m_playstopControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_stopplaying));
        m_recordControl.setEnabled(false);
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "EditPlayFragment.stopPlaying");

        if (m_storiService == null) {
            if(D)Log.d(TAG, "EditPlayFragment.stopPlaying - m_storiService is null, so bailing");
            return;
        }

        StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

        if (playbackState == StoriService.PlaybackState.Stopped) {
            if(D)Log.d(TAG, "EditPlayFragment.stopPlaying - playbackState is Stopped, so bailing");
            return;
        }

        m_storiService.stopAudio(m_audioFileName);

        m_playstopControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        m_recordControl.setEnabled(true);
    }

    private void enableControlsWhileRecording(boolean enable) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.enableControlsWhileRecording: %b", enable));

        m_playstopControl.setEnabled(enable);
        m_selectPhotoControl.setEnabled(enable);
        m_mainMenuControl.setEnabled(enable);
        m_insertAfterControl.setEnabled(enable);
        m_insertBeforeControl.setEnabled(enable);
        m_trashControl.setEnabled(enable);
        m_nextControl.setEnabled(enable);
        m_prevControl.setEnabled(enable);
        m_editControl.setEnabled(enable);
    }

    private boolean hasAudio() {
        return m_audioFileName != null;
    }

    private boolean hasImage() {
        return m_imageFileName != null;
    }

    private boolean hasText() {
        return !(m_slideText == null || m_slideText.isEmpty());
    }

    private static String getNewImageFileName() {
        return UUID.randomUUID().toString() + ".jpg";
    }

    private static String getNewAudioFileName() {
        return UUID.randomUUID().toString() + ".3gp";
    }

    @Override
    public void onAudioStopped(String audioFileName) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onAudioStopped: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));

        if (m_audioFileName != null && m_audioFileName.equals(audioFileName)) {
            m_playstopControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
            m_recordControl.setEnabled(true);
        }
    }

    @Override
    public void onAudioPaused(String audioFileName) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onAudioPaused: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));

        if (m_audioFileName != null && m_audioFileName.equals(audioFileName)) {
            m_playstopControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_stopplaying));
            m_recordControl.setEnabled(false);
        }
    }

    @Override
    public void onAudioPlaying(String audioFileName) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onAudioPlaying: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));

        if (m_audioFileName != null && m_audioFileName.equals(audioFileName)) {
            m_playstopControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_stopplaying));
            m_recordControl.setEnabled(false);
        }
    }

    public void onCopyComplete(boolean success, String[] fileNames, String slideShareName) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onCopyComplete: success=%b, fileName=%s, slideShareName=%s", success, fileNames[0], slideShareName));

        m_currentCameraPhotoFilePath = null;

        if (success) {
            // Display the image only upon successful save
            m_imageFileName = fileNames[0];
            renderImage();
        }
        else {
            // Clean up - remove the image file
            Utilities.deleteFile(m_editPlayActivity, slideShareName, fileNames[0]);
            m_imageFileName = null;
        }

        m_editPlayActivity.updateSlideShareJSON(m_slideUuid, m_imageFileName, m_audioFileName, m_slideText);
    }

    protected void initializeStoriService() {
        if(D)Log.d(TAG, "EditPlayFragment.initializeStoriService");

        m_editPlayActivity.registerStoriServiceConnectionListener(this);
    }

    protected void uninitializeStoriService() {
        if(D)Log.d(TAG, "EditPlayFragment.uninitializeStoriService");

        if (m_storiService != null) {
            m_storiService.unregisterPlaybackStateListener(this);
            m_storiService.unregisterRecordingStateListener(this);
        }

        m_editPlayActivity.unregisterStoriServiceConnectionListener(this);
    }


    @Override
    public void onServiceConnected(StoriService service) {
        if(D)Log.d(TAG, "EditPlayFragment.onServiceConnected");

        m_storiService = service;

        m_storiService.registerPlaybackStateListener(this);
        m_storiService.registerRecordingStateListener(this);

        if (m_storiService.isRecording(m_audioFileName)) {
            m_recordControl.setImageDrawable(getResources().getDrawable(R.drawable.ic_stoprecording));
            enableControlsWhileRecording(false);
        }
    }

    @Override
    public void onServiceDisconnected() {
        if(D)Log.d(TAG, "EditPlayFragment.onServiceDisconnected");

        m_storiService = null;
    }
}
