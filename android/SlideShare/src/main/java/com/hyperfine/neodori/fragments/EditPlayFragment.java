package com.hyperfine.neodori.fragments;

import android.app.Activity;
import android.graphics.Point;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
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

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class EditPlayFragment extends Fragment implements AsyncTaskTimer.IAsyncTaskTimerCallback {
    public final static String TAG = "EditPlayFragment";

    private final static String INSTANCE_STATE_IMAGEFILENAME = "instance_state_imagefilename";
    private final static String INSTANCE_STATE_AUDIOFILENAME = "instance_state_audiofilename";
    private final static String INSTANCE_STATE_SLIDESHARENAME = "instance_state_slidesharename";
    private final static String INSTANCE_STATE_TABPOSITION = "instance_state_tabposition";
    private final static String INSTANCE_STATE_SELECTEDTABPOSITION = "instance_state_selectedtabposition";

    private int m_tabPosition = -1;
    private int m_selectedTabPosition = 0;
    private Activity m_activityParent;
    private String m_slideShareName;
    private ImageSwitcher m_imageSwitcher;
    private String m_imageFileName;
    private String m_audioFileName;
    private MediaPlayer m_player;
    private FileInputStream m_fileInputStream;
    private boolean m_isPlaying = false;
    private int m_displayWidth = 0;
    private int m_displayHeight = 0;
    private EditPlayActivity.EditPlayMode m_editPlayMode = EditPlayActivity.EditPlayMode.Edit;

    public static EditPlayFragment newInstance(Activity activityParent, int position, String slideShareName, SlideJSON sj) {
        if(D)Log.d(TAG, "EditPlayFragment.newInstance");

        EditPlayFragment f = new EditPlayFragment();

        f.setTabPosition(position);
        f.setSlideShareName(slideShareName);
        f.setSlideJSON(sj);
        f.setActivityParent(activityParent);

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

    public void setSlideJSON(SlideJSON sj) {
        if(D)Log.d(TAG, "EditPlayFragment.setSlideJSON");

        try {
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

    public void setActivityParent(Activity activityParent) {
        if(D)Log.d(TAG, "EditPlayFragment.setActivityParent");

        m_activityParent = activityParent;
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

        m_activityParent = activity;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        m_displayWidth = size.x;
        m_displayHeight = size.y;

        if(D)Log.d(TAG, String.format("EditPlayFragment.onAttach: displayWidth=%d, displayHeight=%d", m_displayWidth, m_displayHeight));

        // if (activity instanceof SomeActivityInterface) {
        // }
        // else {
        //     throw new ClassCastException(activity.toString() + " must implement SomeActivityInterface");
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

        m_imageSwitcher.setFactory((ViewSwitcher.ViewFactory)m_activityParent);

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

    private EditPlayActivity.EditPlayMode getActivityEditPlayMode() {
        EditPlayActivity.EditPlayMode epm = ((EditPlayActivity)m_activityParent).getEditPlayMode();
        if(D)Log.d(TAG, String.format("EditPlayFragment.getActivityEditPlayMode: %s", epm.toString()));

        return epm;
    }

    private void setActivityEditPlayMode(EditPlayActivity.EditPlayMode editPlayMode) {
        if(D)Log.d(TAG, String.format("EditPlayFragment.setActivityEditPlayMode: %s", editPlayMode.toString()));
        ((EditPlayActivity)m_activityParent).setEditPlayMode(editPlayMode);
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
            m_imageSwitcher.setImageDrawable(null);
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

                String filePath = Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_imageFileName);
                Bitmap bitmap = Utilities.getConstrainedBitmap(filePath, targetW, targetH);

                Drawable drawableImage = new BitmapDrawable(m_activityParent.getResources(), bitmap);
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
            String filePath = Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName);
            if(D)Log.d(TAG, String.format("EditPlayFragment.startPlaying: filePath=%s", filePath));

            File file = new File(filePath);
            boolean retVal = file.setReadable(true, false);
            if(D)Log.d(TAG, String.format("EditPlayFragment.startPlaying - set readable permissions on audio file returns %b", retVal));
            m_fileInputStream = new FileInputStream(file);

            m_player.setDataSource(m_fileInputStream.getFD());
            m_player.prepare();
            m_player.start();

            m_isPlaying = true;
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
    }
}
