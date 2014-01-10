package com.hyperfine.neodori.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.PopupMenu;
import android.widget.ViewSwitcher;

import com.hyperfine.neodori.AsyncTaskTimer;
import com.hyperfine.neodori.Config;
import com.hyperfine.neodori.EditPlayActivity;
import com.hyperfine.neodori.R;
import com.hyperfine.neodori.SlideJSON;
import com.hyperfine.neodori.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditPlayFragment extends Fragment implements AsyncTaskTimer.IAsyncTaskTimerCallback {
    public final static String TAG = "EditPlayFragment";

    private final static String INSTANCE_STATE_IMAGEFILENAME = "instance_state_imagefilename";
    private final static String INSTANCE_STATE_AUDIOFILENAME = "instance_state_audiofilename";
    private final static String INSTANCE_STATE_SLIDESHARENAME = "instance_state_slidesharename";
    private final static String INSTANCE_STATE_TABPOSITION = "instance_state_tabposition";
    private final static String INSTANCE_STATE_SELECTEDTABPOSITION = "instance_state_selectedtabposition";
    private final static String INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH = "instance_state_currentcameraphotofilepath";

    private int m_tabPosition = -1;
    private int m_selectedTabPosition = 0;
    private EditPlayActivity m_editPlayActivity;
    private String m_slideShareName;
    private ImageSwitcher m_imageSwitcher;
    private Button m_insertBeforeControl;
    private Button m_insertAfterControl;
    private Button m_selectPhotoControl;
    private Button m_cameraControl;
    private Button m_recordControl;
    private Button m_playstopControl;
    private Button m_moreControl;
    private String m_imageFileName;
    private String m_audioFileName;
    private String m_slideUuid;
    private MediaPlayer m_player;
    private MediaRecorder m_recorder;

    private FileInputStream m_fileInputStream;
    private boolean m_isPlaying = false;
    private boolean m_isRecording = false;
    private int m_displayWidth = 0;
    private int m_displayHeight = 0;
    private String m_currentCameraPhotoFilePath = null;
    private EditPlayActivity.EditPlayMode m_editPlayMode = EditPlayActivity.EditPlayMode.Edit;

    public static EditPlayFragment newInstance(EditPlayActivity editPlayActivity, int position, String slideShareName, String slideUuid, SlideJSON sj) {
        if(D)Log.d(TAG, "EditPlayFragment.newInstance");

        EditPlayFragment f = new EditPlayFragment();

        f.setTabPosition(position);
        f.setSlideShareName(slideShareName);
        f.setSlideJSON(slideUuid, sj);
        f.setEditPlayActivity(editPlayActivity);

        return f;
    }

    public void setTabPosition(int position) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.setTabPosition(%d)", position));

        m_tabPosition = position;
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

            m_tabPosition = savedInstanceState.getInt(INSTANCE_STATE_TABPOSITION, -1);
            m_selectedTabPosition = savedInstanceState.getInt(INSTANCE_STATE_SELECTEDTABPOSITION, -1);
            m_audioFileName = savedInstanceState.getString(INSTANCE_STATE_AUDIOFILENAME);
            m_imageFileName = savedInstanceState.getString(INSTANCE_STATE_IMAGEFILENAME);
            m_slideShareName = savedInstanceState.getString(INSTANCE_STATE_SLIDESHARENAME);
            m_currentCameraPhotoFilePath = savedInstanceState.getString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "EditPlayFragment.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(INSTANCE_STATE_TABPOSITION, m_tabPosition);
        savedInstanceState.putInt(INSTANCE_STATE_SELECTEDTABPOSITION, m_selectedTabPosition);
        savedInstanceState.putString(INSTANCE_STATE_AUDIOFILENAME, m_audioFileName);
        savedInstanceState.putString(INSTANCE_STATE_IMAGEFILENAME, m_imageFileName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDESHARENAME, m_slideShareName);
        savedInstanceState.putString(INSTANCE_STATE_CURRENTCAMERAPHOTOFILEPATH, m_currentCameraPhotoFilePath);
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "EditPlayFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "EditPlayFragment.onPause");

        super.onPause();

        stopPlaying();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "EditPlayFragment.onResume");

        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "EditPlayFragment.onAttach");

        super.onAttach(activity);

        m_editPlayActivity = (EditPlayActivity)activity;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        m_displayWidth = size.x;
        m_displayHeight = size.y;

        if(D)Log.d(TAG, String.format("EditPlayFragment.onAttach: displayWidth=%d, displayHeight=%d", m_displayWidth, m_displayHeight));
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

        m_moreControl = (Button)view.findViewById(R.id.control_more);
        m_moreControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(D)Log.d(TAG, "EditPlayFragment.onMoreControlClicked");

                PopupMenu pm = new PopupMenu(m_editPlayActivity, view);
                pm.inflate(R.menu.menu_editplay_more);
                Menu menu = pm.getMenu();

                MenuItem deleteSlide = menu.findItem(R.id.menu_editplay_more_deleteslide);
                deleteSlide.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        m_editPlayActivity.deleteSlide(m_slideUuid, m_imageFileName, m_audioFileName);
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
                            m_playstopControl.setEnabled(false);
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

        m_selectPhotoControl = (Button)view.findViewById(R.id.select_from_gallery_control);
        m_selectPhotoControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onSelectPhotoControlClicked");

                selectImageFromGallery();
            }
        });

        m_recordControl = (Button)view.findViewById(R.id.control_record);
        m_recordControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, String.format("EditPlayFormat.onRecordButtonClicked: %s recording", m_isRecording ? "Stopping" : "Starting"));

                if (m_isRecording) {
                    stopRecording();
                }
                else {
                    startRecording();
                }
            }
        });

        m_playstopControl = (Button)view.findViewById(R.id.control_playback);
        m_playstopControl.setEnabled(hasAudio());
        m_playstopControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, String.format("EditPlayFragment.onPlayStopButtonClicked: %s playing", m_isPlaying ? "Stopping" : "Starting"));

                if (m_isPlaying) {
                    stopPlaying();
                }
                else {
                    startPlaying();
                }
            }
        });

        m_cameraControl = (Button)view.findViewById(R.id.camera_control);
        m_cameraControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onCameraClicked");

                selectImageFromCamera();
            }
        });

        m_insertBeforeControl = (Button)view.findViewById(R.id.edit_prev_control);
        m_insertBeforeControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onInsertBeforeButtonClicked");
                m_editPlayActivity.initializeNewSlide(m_selectedTabPosition);
            }
        });

        m_insertAfterControl = (Button)view.findViewById(R.id.edit_next_control);
        m_insertAfterControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onInsertAfterButtonClicked");
                m_editPlayActivity.initializeNewSlide(m_selectedTabPosition + 1);
            }
        });

        m_imageSwitcher = (ImageSwitcher)view.findViewById(R.id.current_image);
        m_imageSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "EditPlayFragment.onImageClicked");

                switch (m_editPlayMode) {
                    case Edit:
                        m_editPlayMode = EditPlayActivity.EditPlayMode.PlayEdit;
                        setActivityEditPlayMode(m_editPlayMode);
                        updateOverlay();
                        startPlaying();
                        break;

                    case PlayEdit:
                        m_editPlayMode = EditPlayActivity.EditPlayMode.Edit;
                        setActivityEditPlayMode(m_editPlayMode);
                        updateOverlay();
                        stopPlaying();
                        break;

                    case Play:
                        if (m_audioFileName != null) {
                            if (m_isPlaying) {
                                stopPlaying();
                            } else {
                                startPlaying();
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

        if (savedInstanceState == null) {
            asyncStartAudio();
        }
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

            boolean success = Utilities.copyGalleryImageToJPG(m_editPlayActivity, m_slideShareName, imageFileName, intent);

            if (success) {
                // Display the image only upon successful save
                m_imageFileName = imageFileName;
                renderImage();
            }
            else {
                // Clean up - remove the image file
                Utilities.deleteFile(m_editPlayActivity, m_slideShareName, imageFileName);
                m_imageFileName = null;
            }

            m_editPlayActivity.updateSlideShareJSON(m_slideUuid, m_imageFileName, m_audioFileName);
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

            boolean success = Utilities.copyExternalStorageImageToJPG(m_editPlayActivity, m_slideShareName, imageFileName, m_currentCameraPhotoFilePath);
            m_currentCameraPhotoFilePath = null;

            if (success) {
                // Display the image only upon successful save
                m_imageFileName = imageFileName;
                renderImage();
            }
            else {
                // Clean up - remove the image file
                Utilities.deleteFile(m_editPlayActivity, m_slideShareName, imageFileName);
                m_imageFileName = null;
            }

            m_editPlayActivity.updateSlideShareJSON(m_slideUuid, m_imageFileName, m_audioFileName);
        }
        else {
            super.onActivityResult(requestCode, resultCode, intent);
        }
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

    public void onTabPageSelected(int position) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.onTabPageSelected: m_tabPosition=%d, position=%d", m_tabPosition, position));

        m_editPlayMode = getActivityEditPlayMode();
        updateOverlay();

        m_selectedTabPosition = position;

        if (m_tabPosition == position) {
            if(D)Log.d(TAG, "EditPlayFragment.onTabPageSelected - starting audio timer");
            asyncStartAudio();
        }
        else {
            stopPlaying();
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
        if(D)Log.d(TAG, String.format(
                "EditPlayFragment.onAsyncTaskTimerComplete m_selectedTabPosition=%d, m_tabPosition=%d",
                m_selectedTabPosition, m_tabPosition));

        if (m_selectedTabPosition == m_tabPosition && m_editPlayMode != EditPlayActivity.EditPlayMode.Edit) {
            startPlaying();
        }
        else {
            stopPlaying();
        }
    }

    private void updateOverlay() {
        if(D)Log.d(TAG, String.format("EditPlayFragment.updateOverlay: %s", m_editPlayMode.toString()));

        View view = getView().findViewById(R.id.overlay_panel);
        if (m_editPlayMode == EditPlayActivity.EditPlayMode.Edit) {
            view.setVisibility(View.VISIBLE);
        }
        else {
            view.setVisibility(View.GONE);
        }
    }

    private void renderImage() {
        if(D)Log.d(TAG, "EditPlayFragment.renderImage");

        if (m_imageFileName == null) {
            if (m_editPlayMode == EditPlayActivity.EditPlayMode.Play) {
                m_imageSwitcher.setImageDrawable(null);
            }
            else {
                m_imageSwitcher.setImageResource(R.drawable.ic_defaultslideimage);
            }
        }
        else {
            try {
                int targetW = m_imageSwitcher.getWidth();
                int targetH = m_imageSwitcher.getHeight();

                // BUGBUG - note that getWidth/getHeight always returns zero at
                // this point of the fragment life cycle. As a temporary work around,
                // use the screen dimensions if this is the case.
                // See issue #9
                if (targetW == 0 || targetH == 0) {
                    targetW = m_displayWidth;
                    targetH = m_displayHeight;
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
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SlideShare";
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

        if (m_isRecording) {
            if(D)Log.d(TAG, "EditPlayFragment.startRecording - m_isRecording is true, so bailing");
            return;
        }

        if (m_audioFileName == null) {
            m_audioFileName = getNewAudioFileName();
            m_editPlayActivity.updateSlideShareJSON(m_slideUuid, m_imageFileName, m_audioFileName);
        }

        String filePath = Utilities.getAbsoluteFilePath(m_editPlayActivity, m_slideShareName, m_audioFileName);
        if(D)Log.d(TAG, String.format("EditPlayFragment.startRecording: filePath=%s", filePath));

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        m_recorder.setOutputFile(filePath);

        try {
            m_recorder.prepare();
            m_recorder.start();
            m_isRecording = true;
            m_recordControl.setText("Stop recording");
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "EditPlayFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayFragment.startRecording", e);
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if(D)Log.d(TAG, "EditPlayFragment.stopRecording");

        if (!m_isRecording) {
            if(D)Log.d(TAG, "EditPlayFragment.stopRecording - m_isRecording is false so bailing");
            return;
        }

        m_recorder.stop();
        m_recorder.release();
        m_recorder = null;

        m_isRecording = false;
        m_recordControl.setText("Record");

        m_playstopControl.setEnabled(true);
    }

    private void startPlaying() {
        if(D)Log.d(TAG, "EditPlayFragment.startPlaying");

        if (m_isPlaying) {
            if(D)Log.d(TAG, "EditPlayFragment.startPlaying - m_isPlaying is true, so bailing");
            return;
        }

        if (m_audioFileName == null) {
            if(D)Log.d(TAG, "EditPlayFragment.startPlaying - m_audioFileName is null, so bailing");
            return;
        }

        m_player = new MediaPlayer();
        m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(D)Log.d(TAG, "EditPlayFragment.onPlaybackCompletion");
                stopPlaying();
            }
        });

        try {
            String filePath = Utilities.getAbsoluteFilePath(m_editPlayActivity, m_slideShareName, m_audioFileName);
            if(D)Log.d(TAG, String.format("EditPlayFragment.startPlaying: filePath=%s", filePath));

            File file = new File(filePath);
            boolean retVal = file.setReadable(true, false);
            if(D)Log.d(TAG, String.format("EditPlayFragment.startPlaying - set readable permissions on audio file returns %b", retVal));
            m_fileInputStream = new FileInputStream(file);

            m_player.setDataSource(m_fileInputStream.getFD());
            m_player.prepare();
            m_player.start();

            m_isPlaying = true;
            m_playstopControl.setText("Stop playing");
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "EditPlayFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayFragment.startPlaying", e);
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "EditPlayFragment.stopPlaying");

        if (!m_isPlaying) {
            if(D)Log.d(TAG, "EditPlayFragment.stopPlaying - m_isPlaying is false, so bailing");
            return;
        }

        try {
            if (m_fileInputStream != null) {
                m_fileInputStream.close();
            }
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "EditPlayFragment.stopPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "EditPlayFragment.stopPlaying", e);
            e.printStackTrace();
        }
        finally {
            m_fileInputStream = null;
        }

        if (m_player != null) {
            m_player.release();
            m_player = null;
        }

        m_isPlaying = false;
        m_playstopControl.setText("Play");
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
