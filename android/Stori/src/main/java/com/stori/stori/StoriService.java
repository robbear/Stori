package com.stori.stori;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriService extends Service implements AsyncTaskTimer.IAsyncTaskTimerCallback {
    public static final String TAG = "StoriService";

    private MediaPlayer m_mediaPlayer;
    private PlaybackState m_playbackState = PlaybackState.Stopped;
    private String m_audioFileName = null;
    private FileInputStream m_audioFileInputStream;
    private ArrayList<StoriListItem> m_storiListItems = new ArrayList<StoriListItem>();
    private ArrayList<PlaybackStateListener> m_playbackStateListeners = new ArrayList<PlaybackStateListener>();
    private ArrayList<ReadStoriItemsStateListener> m_readStoriItemsStateListeners = new ArrayList<ReadStoriItemsStateListener>();

    @Override
    public void onCreate() {
        if(D)Log.d(TAG, "StoriService.onCreate");
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "**************************************************");
        if(D)Log.d(TAG, "StoriService.onDestroy");
        if(D)Log.d(TAG, "**************************************************");
    }

    public interface StoriServiceConnectionListener {
        void onServiceConnected(StoriService service);
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
        if(D)Log.d(TAG, "StoriService.registerPlaybackStateListener");

        if (!m_playbackStateListeners.contains(listener)) {
            m_playbackStateListeners.add(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.registerPlaybackStateListener: now have %d listeners", m_playbackStateListeners.size()));

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
        if(D)Log.d(TAG, "StoriService.unregisterPlaybackStateListener");

        if (m_playbackStateListeners.contains(listener)) {
            m_playbackStateListeners.remove(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.unregisterPlaybackStateListener: now have %d listeners", m_playbackStateListeners.size()));
    }

    public void reportPlaybackStateChanged(String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.reportPlaybackStateChanged: %s, audioFileName=%s", m_playbackState.toString(), audioFileName));

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
        if(D)Log.d(TAG, "StoriService.onAudioPlaybackComplete");

        String audioFileName = m_audioFileName;
        stopAudio(m_audioFileName);

        // m_playbackState set in stopAudio method
        reportPlaybackStateChanged(audioFileName);
    }

    public PlaybackState getAudioPlaybackState(String audioFileName) {
        if(D)Log.d(TAG, "StoriService.getAudioPlaybackState");

        if (m_audioFileName != null && m_audioFileName.equals(audioFileName)) {
            return m_playbackState;
        }
        else {
            return PlaybackState.Stopped;
        }
    }

    public void startAudio(String slideShareName, String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.startAudio: %s", audioFileName));

        if (m_playbackState == PlaybackState.Playing) {
            if(D)Log.d(TAG, "StoriService.startAudio - m_playbackState is Playing, so bailing");
            return;
        }
        if (audioFileName == null) {
            if(D)Log.d(TAG, "StoriService.startAudio - audioFileName is null, so bailing");
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
            if(D)Log.d(TAG, String.format("StoriService.startAudio: filePath=%s", filePath));

            File file = new File(filePath);
            boolean retVal = file.setReadable(true, false);
            if(D)Log.d(TAG, String.format("StoriService.startAudio - set readable permissions on audio file returns %b", retVal));
            m_audioFileInputStream = new FileInputStream(file);

            m_mediaPlayer.setDataSource(m_audioFileInputStream.getFD());
            m_mediaPlayer.prepare();
            m_mediaPlayer.start();

            m_playbackState = PlaybackState.Playing;
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "StoriService.startAudio", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriService.startAudio", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriService.startAudio", e);
            e.printStackTrace();
        }
    }

    public void stopAudio(String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.stopAudio: %s", audioFileName));

        if (m_playbackState == PlaybackState.Stopped) {
            if(D)Log.d(TAG, "StoriService.stopAudio - m_playbackState is Stopped, so bailing");
            return;
        }
        if (m_audioFileName == null) {
            if(D)Log.d(TAG, "StoriService.stopAudio - m_audioFileName is null, so bailing");
            return;
        }
        if (!m_audioFileName.equals(audioFileName)) {
            if(D)Log.d(TAG, String.format("StoriService.stopAudio - m_audioFileName (%s) does not match audioFileName (%s), so bailing", m_audioFileName, audioFileName));
            return;
        }

        try {
            if (m_audioFileInputStream != null) {
                m_audioFileInputStream.close();
            }
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriService.stopAudio", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriService.stopAudio", e);
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
    // Recorder methods
    //
    //********************************************************************************

    private MediaRecorder m_mediaRecorder;
    private boolean m_isRecording = false;
    private String m_recorderAudioFileName = null;
    private AsyncTaskTimer m_recordingLimitAsyncTaskTimer = null;
    private boolean m_lastRecordingSuccess = false;
    private ArrayList<RecordingStateListener> m_recordingStateListeners = new ArrayList<RecordingStateListener>();

    public interface RecordingStateListener {
        public void onRecordingTimeLimit(boolean success, String audioFileName);
    }

    private void reportRecordingTimeOut(String audioFileName) {
        if(D)Log.d(TAG, "StoriService.reportRecordingTimeOut");

        // Clone the arraylist so that mods on the array list due to actions in the callback do
        // not result in a ConcurrentModificationException.
        ArrayList<RecordingStateListener> recordingStateListeners = new ArrayList<RecordingStateListener>(m_recordingStateListeners);

        for (RecordingStateListener rsl : recordingStateListeners) {
            rsl.onRecordingTimeLimit(m_lastRecordingSuccess, audioFileName);
        }
    }

    public void registerRecordingStateListener(RecordingStateListener listener) {
        if(D)Log.d(TAG, "StoriService.registerRecordingStateListener");

        if (!m_recordingStateListeners.contains(listener)) {
            m_recordingStateListeners.add(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.registerRecordingStateListener: now have %d listeners", m_recordingStateListeners.size()));

        if (m_recordingLimitAsyncTaskTimer == null) {
            if(D)Log.d(TAG, "StoriService.registerRecordingStateListener: m_recordingLimitAsyncTaskTimer is null, so notifying listener");
            listener.onRecordingTimeLimit(m_lastRecordingSuccess, m_recorderAudioFileName);
        }
    }

    public void unregisterRecordingStateListener(RecordingStateListener listener) {
        if(D)Log.d(TAG, "StoriService.unregisterRecordingStateListener");

        if (m_recordingStateListeners.contains(listener)) {
            m_recordingStateListeners.remove(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.unregisterRecordingkStateListener: now have %d listeners", m_recordingStateListeners.size()));
    }

    public boolean startRecording(String slideShareName, String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.startRecording: slideShareName=%s, audioFileName=%s", slideShareName, audioFileName));

        if (m_isRecording) {
            if(D)Log.d(TAG, "StoriService.startRecording - m_isRecording is true, so bailing");
            return true;
        }

        if (audioFileName == null) {
            if(D)Log.d(TAG, "StoriService.startRecording - audioFileName is null, so bailing");
            return false;
        }

        boolean success = false;
        m_recorderAudioFileName = audioFileName;

        String filePath = Utilities.getAbsoluteFilePath(getApplicationContext(), slideShareName, m_recorderAudioFileName);
        if(D)Log.d(TAG, String.format("StoriService.startRecording: filePath=%s", filePath));

        m_mediaRecorder = new MediaRecorder();
        m_mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        m_mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        m_mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        m_mediaRecorder.setOutputFile(filePath);

        try {
            m_mediaRecorder.prepare();
            m_mediaRecorder.start();
            m_isRecording = true;
            success = true;

            m_recordingLimitAsyncTaskTimer = AsyncTaskTimer.startAsyncTaskTimer(1, Config.recordingTimeSegmentMillis, Config.numRecordingSegments, this);
        }
        catch (IOException e) {
            if(E)Log.e(TAG, "StoriService.startRecording", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriService.startRecording", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriService.startRecording", e);
            e.printStackTrace();
        }

        return success;
    }

    public boolean stopRecording(String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.stopRecording: audioFileName=%s", audioFileName));

        if (m_recordingLimitAsyncTaskTimer != null) {
            if(D)Log.d(TAG, "StoriService.stopRecording - killing m_recordingLimitAsyncTaskTimer");
            m_recordingLimitAsyncTaskTimer.cancel(false);
            m_recordingLimitAsyncTaskTimer = null;
        }

        if (!m_isRecording) {
            if(D)Log.d(TAG, "StoriService.stopRecording - m_isRecording is false so bailing");
            return true;
        }
        if (m_recorderAudioFileName == null || !m_recorderAudioFileName.equals(audioFileName)) {
            if(D)Log.d(TAG, "StoriService.stopRecording - m_recorderAudioFileName != audioFileName, so bailing");
            return true;
        }

        m_lastRecordingSuccess = false;

        try {
            m_mediaRecorder.stop();
            m_lastRecordingSuccess = true;
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriService.stopRecording", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriService.stopRecording", e);
            e.printStackTrace();
        }
        m_mediaRecorder.release();
        m_mediaRecorder = null;

        m_isRecording = false;
        m_recorderAudioFileName = null;

        return m_lastRecordingSuccess;
    }

    @Override
    public void onAsyncTaskTimerComplete(long cookie) {
        if(D)Log.d(TAG, String.format("StoriService.onAsyncTaskTimerComplete: cookie=%d", cookie));

        String audioFileName = m_recorderAudioFileName;

        // Prevent stopRecording from attempting to cancel the timer by nulling it here.
        m_recordingLimitAsyncTaskTimer = null;
        stopRecording(audioFileName);

        reportRecordingTimeOut(audioFileName);
    }

    public boolean isRecording(String audioFileName) {
        if(D)Log.d(TAG, String.format("StoriService.isRecording: %s", audioFileName));

        return (m_recorderAudioFileName != null && m_recorderAudioFileName.equals(audioFileName) && m_isRecording);
    }

    //********************************************************************************
    //
    // ReadStoriItems methods
    //
    //********************************************************************************

    public interface ReadStoriItemsStateListener {
        public void onReadStoriItemsComplete(ArrayList<StoriListItem> storiListItems);
    }

    public ArrayList<StoriListItem> getStoriListItems() {
        if(D)Log.d(TAG, "StoriService.getStoriListItems");

        return m_storiListItems;
    }

    private void reportReadStoriItemsComplete() {
        if(D)Log.d(TAG, "StoriService.reportReadStoriItemsComplete");

        // Clone the arraylist so that mods on the array list due to actions in the callback do
        // not result in a ConcurrentModificationException.
        ArrayList<ReadStoriItemsStateListener> readStoriItemsStateListeners = new ArrayList<ReadStoriItemsStateListener>(m_readStoriItemsStateListeners);

        for (ReadStoriItemsStateListener rsl : readStoriItemsStateListeners) {
            rsl.onReadStoriItemsComplete(m_storiListItems);
        }
    }

    public void registerReadStoriItemsStateListener(ReadStoriItemsStateListener listener) {
        if(D)Log.d(TAG, "StoriService.registerReadStoriItemsStateListener");

        if (!m_readStoriItemsStateListeners.contains(listener)) {
            m_readStoriItemsStateListeners.add(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.registerReadStoriItemsStateListener: now have %d listeners", m_readStoriItemsStateListeners.size()));
    }

    public void unregisterReadStoriItemsStateListener(ReadStoriItemsStateListener listener) {
        if(D)Log.d(TAG, "StoriService.unregisterReadStoriItemsStateListener");

        if (m_readStoriItemsStateListeners.contains(listener)) {
            m_readStoriItemsStateListeners.remove(listener);
        }
        if(D)Log.d(TAG, String.format("StoriService.unregisterReadStoriItemsStateListener: now have %d listeners", m_readStoriItemsStateListeners.size()));
    }

    private class ReadStoriItemsTaskParams {
        public Context m_context;
        public String m_userUuid;

        public ReadStoriItemsTaskParams(Context context, String userUuid) {
            m_context = context;
            m_userUuid = userUuid;
        }
    }

    private class ReadStoriItemsTask extends AsyncTask<Object, Void, ArrayList<StoriListItem>> {
        @Override
        public ArrayList<StoriListItem> doInBackground(Object... params) {
            if(D)Log.d(TAG, "StoriService.ReadStoriItemsTask.doInBackground");

            if (m_storiListItems != null && m_storiListItems.size() > 1) {
                if(D)Log.d(TAG, "StoriService.ReadStoriItemsTask.doInBackground - reusing from cache");

                return m_storiListItems;
            }

            ReadStoriItemsTaskParams rsitp = (ReadStoriItemsTaskParams)params[0];

            CloudStore cloudStore = new CloudStore(rsitp.m_context, rsitp.m_userUuid, null, Config.CLOUD_STORAGE_PROVIDER, null);

            ArrayList<StoriListItem> items = cloudStore.readStoriItems();

            // Now sort the items by date descending, taking advantage of the fact that the
            // modifiedDate is a comparator-capable string, formatted in YYYY-MM-DDTHH:MM:SS.sssz
            // UTC time format.

            if (items != null) {
                Collections.sort(items, new StoriListItemComparator());
            }

            return items;
        }

        @Override
        public void onPostExecute(ArrayList<StoriListItem> items) {
            if(D)Log.d(TAG, "StoriService.ReadStoriItemsTask.onPostExecute");

            m_storiListItems = items;
            reportReadStoriItemsComplete();
        }
    }

    public void readStoriItemsAsync(Context context, String userUuid) {
        if(D)Log.d(TAG, "StoriService.readStoriItemsAsync");

        ReadStoriItemsTaskParams rsitp = new ReadStoriItemsTaskParams(context, userUuid);

        try {
            new ReadStoriItemsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, rsitp);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "StoriService.readStoriItemsAsync", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "StoriService.readStoriItemsAsync", e);
            e.printStackTrace();
        }
    }

    public void resetStoriItems(ArrayList<StoriListItem> items) {
        if(D)Log.d(TAG, "StoriService.resetStoriItems");

        if (items == null) {
            if(D)Log.d(TAG, "StoriService.resetStoriItems: passing in null, so clearing m_storiList");
            m_storiListItems = new ArrayList<StoriListItem>();
        }
        else {
            if(D)Log.d(TAG, "StoriService.resetStoriItems: setting m_storiListItems to passed in item list");
            m_storiListItems = items;
        }
    }

    //********************************************************************************
    //
    // Binder methods
    //
    //********************************************************************************

    public class StoriServiceBinder extends Binder
    {
        public StoriService getService()
        {
            if(D)Log.d(TAG, "StoriServiceBinder.getService");
            return StoriService.this;
        }
    }

    private final IBinder binder = new StoriServiceBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        if(D)Log.d(TAG, "StoriService.onBind");

        return binder;
    }
}
