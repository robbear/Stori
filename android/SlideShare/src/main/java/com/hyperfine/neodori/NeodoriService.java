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

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class NeodoriService extends Service {
    public static final String TAG = "NeodoriService";

    private MediaPlayer m_mediaPlayer;
    private boolean m_isAudioPlaying = false;
    private String m_audioFileName = null;
    private FileInputStream m_audioFileInputStream;
    private IMediaPlayerNotifications m_mediaPlayerNotifications;

    public interface IMediaPlayerNotifications {
        public void onAudioPlaybackComplete(String audioFileName);
    }

    @Override
    public void onCreate() {
        if(D)Log.d(TAG, "NeodoriService.onCreate");
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "NeodoriService.onDestroy");
    }

    //********************************************************************************
    //
    // Audio methods
    //
    //********************************************************************************

    public void onAudioPlaybackComplete(MediaPlayer mp) {
        if(D)Log.d(TAG, "NeodoriService.onAudioPlaybackComplete");

        String audioFileName = m_audioFileName;
        stopAudio(m_audioFileName);

        if (m_mediaPlayerNotifications != null) {
            m_mediaPlayerNotifications.onAudioPlaybackComplete(audioFileName);
        }
    }

    public void startAudio(String slideShareName, String audioFileName, IMediaPlayerNotifications mpn) {
        if(D)Log.d(TAG, String.format("NeodoriService.startAudio: %s", audioFileName));

        if (m_isAudioPlaying) {
            if(D)Log.d(TAG, "NeodoriService.startAudio - m_isAudioPlaying is true, so bailing");
            return;
        }
        if (audioFileName == null) {
            if(D)Log.d(TAG, "NeodoriService.startAudio - audioFileName is null, so bailing");
            return;
        }

        m_audioFileName = audioFileName;
        m_mediaPlayerNotifications = mpn;

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

            m_isAudioPlaying = true;
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

        if (!m_isAudioPlaying) {
            if(D)Log.d(TAG, "NeodoriService.stopAudio - m_isAudioPlaying is false, so bailing");
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

        m_isAudioPlaying = false;
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
