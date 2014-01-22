package com.stori-app.stori.fragments;

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

import com.stori-app.stori.AsyncTaskTimer;
import com.stori-app.stori.Config;
import com.stori-app.stori.StoriService;
import com.stori-app.stori.PlaySlidesActivity;
import com.stori-app.stori.R;
import com.stori-app.stori.SlideJSON;
import com.stori-app.stori.Utilities;

import static com.stori-app.stori.Config.D;
import static com.stori-app.stori.Config.E;

public class PlaySlidesFragment extends Fragment implements
        AsyncTaskTimer.IAsyncTaskTimerCallback, StoriService.PlaybackStateListener, StoriService.StoriServiceConnectionListener {

    public final static String TAG = "PlaySlidesFragment";

    private final static String INSTANCE_STATE_IMAGEFILENAME = "instance_state_imagefilename";
    private final static String INSTANCE_STATE_AUDIOFILENAME = "instance_state_audiofilename";
    private final static String INSTANCE_STATE_SLIDESHARENAME = "instance_state_slidesharename";
    private final static String INSTANCE_STATE_SLIDUUID = "instance_state_slideuuid";

    private PlaySlidesActivity m_playSlidesActivity;
    private String m_slideShareName;
    private String m_slideUuid;
    private ImageSwitcher m_imageSwitcher;
    private String m_imageFileName;
    private String m_audioFileName;
    private int m_displayWidth = 0;
    private int m_displayHeight = 0;
    private StoriService m_storiService = null;

    public static PlaySlidesFragment newInstance(PlaySlidesActivity playSlidesActivity, int position, String slideShareName, String slideUuid, SlideJSON sj) {
        if(D)Log.d(TAG, "PlaySlidesFragment.newInstance");

        PlaySlidesFragment f = new PlaySlidesFragment();

        f.setSlideShareName(slideShareName);
        f.setSlideJSON(slideUuid, sj);
        f.setPlaySlidesActivity(playSlidesActivity);

        return f;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.setSlideShareName: %s", name));

        m_slideShareName = name;
    }

    public void setSlideJSON(String slideUuid, SlideJSON sj) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.setSlideJSON: slideUuid=%s", slideUuid));

        try {
            m_slideUuid = slideUuid;
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

    public void setPlaySlidesActivity(PlaySlidesActivity playSlidesActivity) {
        if(D)Log.d(TAG, "PlaySlidesFragment.setPlaySlidesActivity");

        m_playSlidesActivity = playSlidesActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onCreate");

        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if(D)Log.d(TAG, "PlaySlidesFragment.onCreate - populating from savedInstanceState");

            m_audioFileName = savedInstanceState.getString(INSTANCE_STATE_AUDIOFILENAME);
            m_imageFileName = savedInstanceState.getString(INSTANCE_STATE_IMAGEFILENAME);
            m_slideShareName = savedInstanceState.getString(INSTANCE_STATE_SLIDESHARENAME);
            m_slideUuid = savedInstanceState.getString(INSTANCE_STATE_SLIDUUID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(INSTANCE_STATE_AUDIOFILENAME, m_audioFileName);
        savedInstanceState.putString(INSTANCE_STATE_IMAGEFILENAME, m_imageFileName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDESHARENAME, m_slideShareName);
        savedInstanceState.putString(INSTANCE_STATE_SLIDUUID, m_slideUuid);
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onStart() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onStart");

        super.onStart();

        initializeStoriService();
    }

    @Override
    public void onStop() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onStop");

        super.onStop();

        if (!m_playSlidesActivity.getOrientationChangedFlag()) {
            stopPlaying();
        }

        uninitializeStoriService();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onPause");

        super.onPause();
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

        m_playSlidesActivity = (PlaySlidesActivity)activity;

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        m_displayWidth = size.x;
        m_displayHeight = size.y;

        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onAttach: displayWidth=%d, displayHeight=%d", m_displayWidth, m_displayHeight));

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

                if (m_audioFileName != null && m_storiService != null) {
                    StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

                    if (playbackState == StoriService.PlaybackState.Playing) {
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

        m_imageSwitcher.setFactory((ViewSwitcher.ViewFactory)m_playSlidesActivity);

        // Seed the ImageSwitcher with a black background in order to inflate it
        // to non-zero dimensions.
        m_imageSwitcher.setImageResource(R.drawable.ic_black);
        renderImage();

        if (savedInstanceState == null) {
            AsyncTaskTimer.startAsyncTaskTimer(1, Config.audioDelayMillis, this);
        }
    }

    public void onTabPageSelected(int position) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onTabPageSelected: position=%d", position));

        int tabPosition = m_playSlidesActivity.getSlidePosition(m_slideUuid);

        if (tabPosition == position) {
            if(D)Log.d(TAG, "PlaySlidesFragment.onTabPageSelected - starting audio timer");
            AsyncTaskTimer.startAsyncTaskTimer(1, Config.audioDelayMillis, this);
        }
        else {
            stopPlaying();
        }
    }

    public void onAsyncTaskTimerComplete(long cookie) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onAsyncTaskTimerComplete");

        int selectedTabPosition = m_playSlidesActivity.getCurrentTabPosition();
        int tabPosition = m_playSlidesActivity.getSlidePosition(m_slideUuid);

        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onAsyncTaskTimerComplete: selectedTabPosition=%d, tabPosition=%d", selectedTabPosition, tabPosition));

        if (selectedTabPosition == tabPosition) {
            startPlaying();
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

                String filePath = Utilities.getAbsoluteFilePath(m_playSlidesActivity, m_slideShareName, m_imageFileName);
                Bitmap bitmap = Utilities.getConstrainedBitmap(filePath, targetW, targetH);

                Drawable drawableImage = new BitmapDrawable(m_playSlidesActivity.getResources(), bitmap);
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

    private void startPlaying() {
        if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying");

        if (m_storiService == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.startPlaying - m_storiService is null, so bailing");
            return;
        }

        StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

        if (playbackState == StoriService.PlaybackState.Playing) {
            if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying - m_isPlaying is true, so bailing");
            return;
        }

        if (m_audioFileName == null) {
            if(D)Log.d(TAG, "PlaySlidesFragment.startPlaying - m_audioFileName is null, so bailing");
            return;
        }

        m_storiService.startAudio(m_slideShareName, m_audioFileName);
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "PlaySlidesFragment.stopPlaying");

        if (m_storiService == null) {
            if(D)Log.d(TAG, "PlaySlidesActivity.stopPlaying - m_storiService is null, so bailing");
            return;
        }

        StoriService.PlaybackState playbackState = m_storiService.getAudioPlaybackState(m_audioFileName);

        if (playbackState == StoriService.PlaybackState.Stopped) {
            if(D)Log.d(TAG, "PlaySlidesFragment.stopPlaying - playbackState is Stopped, so bailing");
            return;
        }

        m_storiService.stopAudio(m_audioFileName);
    }

    @Override
    public void onAudioStopped(String audioFileName) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onAudioStopped: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));
    }

    @Override
    public void onAudioPaused(String audioFileName) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onAudioPaused: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));
    }

    @Override
    public void onAudioPlaying(String audioFileName) {
        if(D)Log.d(TAG, String.format("PlaySlidesFragment.onAudioPlaying: m_audioFileName=%s, audioFileName=%s", m_audioFileName, audioFileName));
    }

    protected void initializeStoriService() {
        if(D)Log.d(TAG, "PlaySlidesFragment.initializeStoriService");

        m_playSlidesActivity.registerStoriServiceConnectionListener(this);
    }

    protected void uninitializeStoriService() {
        if(D)Log.d(TAG, "PlaySlidesFragment.uninitializeStoriService");

        if (m_storiService != null) {
            m_storiService.unregisterPlaybackStateListener(this);
        }

        m_playSlidesActivity.unregisterStoriServiceConnectionListener(this);
    }


    @Override
    public void onServiceConnected(StoriService service) {
        if(D)Log.d(TAG, "PlaySlidesFragment.onServiceConnected");

        m_storiService = service;

        m_storiService.registerPlaybackStateListener(this);
    }

    @Override
    public void onServiceDisconnected() {
        if(D)Log.d(TAG, "PlaySlidesFragment.onServiceDisconnected");

        m_storiService = null;
    }
}
