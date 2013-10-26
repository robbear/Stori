package com.hyperfine.slideshare.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ViewSwitcher;

import com.hyperfine.slideshare.AsyncTaskTimer;
import com.hyperfine.slideshare.Config;
import com.hyperfine.slideshare.R;
import com.hyperfine.slideshare.SlideJSON;
import com.hyperfine.slideshare.Utilities;

import java.io.IOException;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class PlaySlidesFragment extends Fragment implements AsyncTaskTimer.IAsyncTaskTimerCallback {

    public final static String TAG = "PlaySlidesFragment";

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
    private boolean m_isPlaying = false;
    private boolean m_ignoreAudio = false;

    public static PlaySlidesFragment newInstance(Activity activityParent, int position, String slideShareName, SlideJSON sj) {
        if(D)Log.d(TAG, "PlaySlidesFragment.newInstance");

        PlaySlidesFragment f = new PlaySlidesFragment();

        f.setTabPosition(position);
        f.setSlideShareName(slideShareName);
        f.setSlideJSON(sj);
        f.setActivityParent(activityParent);

        return f;
    }

    public void setTabPosition(int position) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.setTabPosition(%d)", position));

        m_tabPosition = position;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.setSlideShareName: %s", name));

        m_slideShareName = name;
    }

    public void setSlideJSON(SlideJSON sj) {
        if(D)Log.d(TAG, "PlaySlidesFragment.setSlideJSON");

        try {
            m_imageFileName = sj.getImageFilename();
            m_audioFileName = sj.getAudioFilename();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesFragment.setSlideJSON", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesFragment.setSlideJSON", e);
            e.printStackTrace();
        }
    }

    public void setActivityParent(Activity activityParent) {
        if(D)Log.d(TAG, "PlaySlidesFragment.setActivityParent");

        m_activityParent = activityParent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onCreate");

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if(D)Log.d(TAG, "PlaySlidesFragment.onCreate - populating from savedInstanceState");

            m_tabPosition = savedInstanceState.getInt(INSTANCE_STATE_TABPOSITION, -1);
            m_selectedTabPosition = savedInstanceState.getInt(INSTANCE_STATE_SELECTEDTABPOSITION, -1);
            m_audioFileName = savedInstanceState.getString(INSTANCE_STATE_AUDIOFILENAME);
            m_imageFileName = savedInstanceState.getString(INSTANCE_STATE_IMAGEFILENAME);
            m_slideShareName = savedInstanceState.getString(INSTANCE_STATE_SLIDESHARENAME);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putInt(INSTANCE_STATE_TABPOSITION, m_tabPosition);
        savedInstanceState.putInt(INSTANCE_STATE_SELECTEDTABPOSITION, m_selectedTabPosition);
        savedInstanceState.putString(INSTANCE_STATE_AUDIOFILENAME, m_audioFileName);
        savedInstanceState.putString(INSTANCE_STATE_IMAGEFILENAME, m_imageFileName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDESHARENAME, m_slideShareName);
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onPause");

        super.onPause();

        stopPlaying();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onResume");

        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onAttach");

        super.onAttach(activity);

        m_activityParent = activity;

        // if (activity instanceof SomeActivityInterface) {
        // }
        // else {
        //     throw new ClassCastException(activity.toString() + " must implement SomeActivityInterface");
    }

    @Override
    public void onDetach() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onDetach");

        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onCreateView");

        View view = inflater.inflate(R.layout.fragment_playslides, container, false);

        m_imageSwitcher = (ImageSwitcher)view.findViewById(R.id.current_image);
        m_imageSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "PlaySlidesFragment.onImageClicked");

                if (m_audioFileName != null) {
                    if (m_isPlaying) {
                        stopPlaying();
                    }
                    else {
                        startPlaying();
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);

        m_imageSwitcher.setFactory((ViewSwitcher.ViewFactory)m_activityParent);

        renderImage();

        if (savedInstanceState == null) {
            AsyncTaskTimer.startAsyncTaskTimer(1, Config.audioDelayMillis, this);
        }
    }

    public void onTabPageSelected(int position) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onTabPageSelected: this=%d, position=%d", m_tabPosition, position));

        m_selectedTabPosition = position;

        if (m_tabPosition == position) {
            AsyncTaskTimer.startAsyncTaskTimer(1, Config.audioDelayMillis, this);
        }
        else {
            stopPlaying();
        }
    }

    public void onAsyncTaskTimerComplete(long cookie) {
        if(D)Log.d(TAG, String.format(
                "PlaySlidesFragment.onAsyncTaskTimerComplete m_selectedTabPosition=%d, m_tabPosition=%d",
                m_selectedTabPosition, m_tabPosition));

        if (m_selectedTabPosition == m_tabPosition) {
            renderAudio();
        }
        else {
            stopPlaying();
        }
    }

    private void renderImage() {
        if(D)Log.d(TAG, "PlaySlidesFragment.renderImage");

        if (m_imageFileName == null) {
            m_imageSwitcher.setImageDrawable(null);
        }
        else {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_imageFileName));
                Drawable drawableImage = new BitmapDrawable(m_activityParent.getResources(), bitmap);
                m_imageSwitcher.setImageDrawable(drawableImage);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "PlaySlidesFragment.renderImage", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "PlaySlidesFragment.renderImage", e);
                e.printStackTrace();
            }
        }
    }

    private void renderAudio() {
        if(D)Log.d(TAG, "PlaySlidesFragment.renderAudio");

        if (m_audioFileName != null) {
            startPlaying();
        }
    }

    private void startPlaying() {
        if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying");

        if (m_isPlaying) {
            if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying - m_isPlaying is true, so bailing");
            return;
        }

        if (m_ignoreAudio) {
            if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying - m_ignoreAudio is true, so skipping audio playback.");
            m_ignoreAudio = false;
            return;
        }

        m_player = new MediaPlayer();
        m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(D)Log.d(TAG, "PlaySlidesFragment.onPlaybackCompletion");
                stopPlaying();
            }
        });

        try {
            m_player.setDataSource(Utilities.getAbsoluteFilePath(m_activityParent, m_slideShareName, m_audioFileName));
            m_player.prepare();
            m_player.start();

            m_isPlaying = true;
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "PlaySlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "PlaySlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "PlaySlidesFragment.startPlaying", e);
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "PlaySlidesFragment.stopPlaying");

        if (!m_isPlaying) {
            if(D)Log.d(TAG, "PlaySlidesFragment.stopPlaying - m_isPlaying is false, so bailing");
            return;
        }

        if (m_player != null) {
            m_player.release();
            m_player = null;
        }

        m_isPlaying = false;
    }
}
