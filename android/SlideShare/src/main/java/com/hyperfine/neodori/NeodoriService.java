package com.hyperfine.neodori;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class NeodoriService extends Service {
    public static final String TAG = "NeodoriService";

    private MediaPlayer m_mediaPlayer;
    private PlaybackState m_playbackState = PlaybackState.Stopped;
    private String m_audioFileName = null;
    private FileInputStream m_audioFileInputStream;
    private ArrayList<PlaybackStateListener> m_playbackStateListeners = new ArrayList<PlaybackStateListener>();

    @Override
    public void onCreate() {
        if(D)Log.d(TAG, "NeodoriService.onCreate");
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "NeodoriService.onDestroy");
    }

    public interface NeodoriServiceConnectionListener {
        void onServiceConnected(NeodoriService service);
        void onServiceDisconnected();
    }

    //********************************************************************************
    //
    // Audio methods
    //
    //********************************************************************************

    public interface PlaybackStateListener {
        void onAudioStopped(String audioFileName);
        void onAudioPaused(String audioFileName);
        void onAudioPlaying(String audioFileName);
    }

    public enum PlaybackState {
        Stopped,
        Paused,
        Playing
    }

    public void registerPlaybackStateListener(PlaybackStateListener listener) {
        if(D)Log.d(TAG, "NeodoriService.registerPlaybackStateListener");

        if (!m_playbackStateListeners.contains(listener)) {
            m_playbackStateListeners.add(listener);
        }
        if(D)Log.d(TAG, String.format("NeodoriService.registerPlaybackStateListener: now have %d listeners", m_playbackStateListeners.size()));

        // Send the state immediately to the newly enlisted listener
        switch (m_playbackState) {
            case Stopped:
                listener.onAudioStopped(m_audioFileName);
                break;

            case Paused:
                listener.onAudioPaused(m_audioFileName);
                break;

            case Playing:
                listener.onAudioPlaying(m_audioFileName);
                break;
        }
    }

    public void unregisterPlaybackStateListener(PlaybackStateListener listener) {
        if(D)Log.d(TAG, "NeodoriService.unregisterPlaybackStateListener");

        if (m_playbackStateListeners.contains(listener)) {
            m_playbackStateListeners.remove(listener);
        }
        if(D)Log.d(TAG, String.format("NeodoriService.unregisterPlaybackStateListener: now have %d listeners", m_playbackStateListeners.size()));
    }

    public void reportPlaybackStateChanged(String audioFileName) {
        if(D)Log.d(TAG, String.format("NeodoriService.reportPlaybackStateChanged: %s, audioFileName=%s", m_playbackState.toString(), audioFileName));

        // Clone the arraylist so that mods on the array list due to actions in the callback do
        // not result in a ConcurrentModificationException.
        ArrayList<PlaybackStateListener> playbackStateListeners = new ArrayList<PlaybackStateListener>(m_playbackStateListeners);

        for (PlaybackStateListener psl : playbackStateListeners) {
            switch (m_playbackState) {
                case Stopped:
                    psl.onAudioStopped(audioFileName);
                    break;

                case Paused:
                    psl.onAudioPaused(audioFileName);
                    break;

                case Playing:
                    psl.onAudioPlaying(audioFileName);
                    break;
            }
        }
    }

    public void onAudioPlaybackComplete(MediaPlayer mp) {
        if(D)Log.d(TAG, "NeodoriService.onAudioPlaybackComplete");

        String audioFileName = m_audioFileName;
        stopAudio(m_audioFileName);

        // m_playbackState set in stopAudio method
        reportPlaybackStateChanged(audioFileName);
    }

    public PlaybackState getAudioPlaybackState(String audioFileName) {
        if(D)Log.d(TAG, "NeodoriService.getAudioPlaybackState");

        if (m_audioFileName != null && m_audioFileName.equals(audioFileName)) {
            return m_playbackState;
        }
        else {
            return PlaybackState.Stopped;
        }
    }

    public void startAudio(String slideShareName, String audioFileName) {
        if(D)Log.d(TAG, String.format("NeodoriService.startAudio: %s", audioFileName));

        if (m_playbackState == PlaybackState.Playing) {
            if(D)Log.d(TAG, "NeodoriService.startAudio - m_playbackState is Playing, so bailing");
            return;
        }
        if (audioFileName == null) {
            if(D)Log.d(TAG, "NeodoriService.startAudio - audioFileName is null, so bailing");
            return;
        }

        m_audioFileName = audioFileName;

        m_mediaPlayer = new MediaPlayer();
        m_mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onAudioPlaybackComplete(mp);
            }
        });

        try {
            String filePath = Utilities.getAbsoluteFilePath(getApplicationContext(), slideShareName, audioFileName);
            if(D)Log.d(TAG, String.format("NeodoriService.startAudio: filePath=%s", filePath));

            File file = new File(filePath);
            boolean retVal = file.setReadable(true, false);
            if(D)Log.d(TAG, String.format("NeodoriService.startAudio - set readable permissions on audio file returns %b", retVal));
            m_audioFileInputStream = new FileInputStream(file);

            m_mediaPlayer.setDataSource(m_audioFileInputStream.getFD());
            m_mediaPlayer.prepare();
            m_mediaPlayer.start();

            m_playbackState = PlaybackState.Playing;
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "NeodoriService.startAudio", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "NeodoriService.startAudio", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "NeodoriService.startAudio", e);
            e.printStackTrace();
        }
    }

    public void stopAudio(String audioFileName) {
        if(D)Log.d(TAG, String.format("NeodoriService.stopAudio: %s", audioFileName));

        if (m_playbackState == PlaybackState.Stopped) {
            if(D)Log.d(TAG, "NeodoriService.stopAudio - m_playbackState is Stopped, so bailing");
            return;
        }
        if (m_audioFileName == null) {
            if(D)Log.d(TAG, "NeodoriService.stopAudio - m_audioFileName is null, so bailing");
            return;
        }
        if (!m_audioFileName.equals(audioFileName)) {
            if(D)Log.d(TAG, String.format("NeodoriService.stopAudio - m_audioFileName (%s) does not match audioFileName (%s), so bailing", m_audioFileName, audioFileName));
            return;
        }

        try {
            if (m_audioFileInputStream != null) {
                m_audioFileInputStream.close();
            }
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "NeodoriService.stopAudio", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "NeodoriService.stopAudio", e);
            e.printStackTrace();
        }
        finally {
            m_audioFileInputStream = null;
        }

        if (m_mediaPlayer != null) {
            m_mediaPlayer.release();
            m_mediaPlayer = null;
        }

        m_playbackState = PlaybackState.Stopped;
        m_audioFileName = null;
    }

    //********************************************************************************
    //
    // Binder methods
    //
    //********************************************************************************

    public class NeodoriServiceBinder extends Binder
    {
        public NeodoriService getService()
        {
            if(D)Log.d(TAG, "NeodoriServiceBinder.getService");
            return NeodoriService.this;
        }
    }

    private final IBinder binder = new NeodoriServiceBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        if(D)Log.d(TAG, "NeodoriService.onBind");

        return binder;
    }
}
