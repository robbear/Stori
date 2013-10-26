package com.hyperfine.slideshare.fragments;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.hyperfine.slideshare.R;
import com.hyperfine.slideshare.Utilities;

import java.io.File;
import java.io.IOException;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class RecordFragment extends Fragment {
    public final static String TAG = "RecordFragment";

    private boolean m_isRecording = false;
    private boolean m_isPlaying = false;
    private MediaRecorder m_recorder;
    private MediaPlayer m_player;
    private Activity m_activityParent;
    private String m_slideShareName;

    private ImageButton m_recordButton;

    public static RecordFragment newInstance(String slideShareName) {
        if(D)Log.d(TAG, "RecordFragment.newInstance");

        RecordFragment f = new RecordFragment();

        f.setSlideShareName(slideShareName);

        return f;
    }

    public void setSlideShareName(String name) {
        if(D)Log.d(TAG, String.format("RecordFragment.setSlideShareName: %s", name));

        m_slideShareName = name;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "RecordFragment.onCreate");

        super.onCreate(savedInstanceState);

        Bundle argsBundle = getArguments();
        if (argsBundle != null) {
            // Set instance state
        }
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "RecordFragment.onDestroy");

        super.onDestroy();
    }

    @Override
    public void onPause() {
        if(D)Log.d(TAG, "RecordFragment.onPause");

        super.onPause();
    }

    @Override
    public void onResume() {
        if(D)Log.d(TAG, "RecordFragment.onResume");

        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        if(D)Log.d(TAG, "RecordFragment.onAttach");

        super.onAttach(activity);

        m_activityParent = activity;

        // if (activity instanceof SomeActivityInterface) {
        // }
        // else {
        //     throw new ClassCastException(activity.toString() + " must implement SomeActivityInterface");
    }

    @Override
    public void onDetach() {
        if(D)Log.d(TAG, "RecordFragment.onDetach");

        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(D)Log.d(TAG, "RecordFragment.onCreateView");

        View view = inflater.inflate(R.layout.fragment_record, container, false);

        m_recordButton = (ImageButton)view.findViewById(R.id.control_record);
        m_recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(D)Log.d(TAG, "RecordFragment.onRecordButtonClicked");

                if (m_isRecording) {
                    stopRecording();

                    // BUGBUG - test
                    startPlaying();
                }
                else {
                    startRecording();
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "RecordFragment.onActivityCreated");

        super.onActivityCreated(savedInstanceState);
    }

    private String getFilePath() {
        File directory = Utilities.createOrGetSlideShareDirectory(m_activityParent, m_slideShareName);

        String filePath = directory.getAbsolutePath() + "/test.3gp";

        if(D)Log.d(TAG, String.format("RecordFragment.getFilePath returns %s", filePath));

        return filePath;
    }

    private void startRecording() {
        if(D)Log.d(TAG, "RecordFragment.startRecording");

        if (m_isRecording) {
            if(D)Log.d(TAG, "RecordFragment.startRecording - m_isRecording is true, so bailing");
            return;
        }

        m_recorder = new MediaRecorder();
        m_recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        m_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        m_recorder.setOutputFile(getFilePath());

        try {
            m_recorder.prepare();
            m_recorder.start();
            m_isRecording = true;
            m_recordButton.setImageResource(R.drawable.ic_pause);
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "RecordFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "RecordFragment.startRecording", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "RecordFragment.startRecording", e);
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if(D)Log.d(TAG, "RecordFragment.stopRecording");

        if (!m_isRecording) {
            if(D)Log.d(TAG, "RecordFragment.stopRecording - m_isRecording is false so bailing");
            return;
        }

        m_recorder.stop();
        m_recorder.release();
        m_recorder = null;

        m_isRecording = false;
        m_recordButton.setImageResource(R.drawable.ic_play);
    }

    private void startPlaying() {
        if(D)Log.d(TAG, "RecordFragment.startPlaying");

        if (m_isPlaying) {
            if(D)Log.d(TAG, "RecordFragment.startPlaying - m_isPlaying is true, so bailing");
            return;
        }

        m_player = new MediaPlayer();
        m_player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(D)Log.d(TAG, "RecordFragment.onPlaybackCompletion");
                stopPlaying();
            }
        });

        try {
            m_player.setDataSource(getFilePath());
            m_player.prepare();
            m_player.start();

            m_isPlaying = true;
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "RecordFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "RecordFragment.startPlaying", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "RecordFragment.startPlaying", e);
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        if(D)Log.d(TAG, "RecordFragment.stopPlaying");

        if (!m_isPlaying) {
            if(D)Log.d(TAG, "RecordFragment.stopPlaying - m_isPlaying is false, so bailing");
            return;
        }

        m_player.release();
        m_player = null;

        m_isPlaying = false;
    }
}
