package com.stori.stori;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class DownloadActivity extends FragmentActivity {
    public final static String TAG = "DownloadActivity";

    private ProgressDialog m_progressDialog;
    private DownloadTask m_downloadTask;
    private String m_slideShareName;
    private String m_userUuid;
    private SlideShareJSON m_ssj;
    private ArrayList<String> m_urlsToDownload;
    private int m_numberOfResources = 0;
    private int m_currentResourceDownloadIndex = 0;
    private boolean m_intentFromStoriApp = false;
    private boolean m_downloadForEdit = false;
    private SharedPreferences m_prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D)Log.d(TAG, "DownloadActivity.onCreate");

        m_prefs = getSharedPreferences(SSPreferences.PREFS(this), Context.MODE_PRIVATE);

        // Lock the orientation down
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        final Intent intent = getIntent();
        if (intent == null) {
            if(D)Log.d(TAG, "DownloadActivity.onCreate - intent is null, so bailing");
            finish();
            return;
        }

        final String action = intent.getAction();
        if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: action=%s", action));

        m_intentFromStoriApp = intent.getBooleanExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, false);
        if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: m_intentFromStoriApp=%b", m_intentFromStoriApp));

        m_downloadForEdit = intent.getBooleanExtra(StoriListActivity.EXTRA_DOWNLOAD_FOR_EDIT, false);
        if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: m_downloadForEdit=%b", m_downloadForEdit));

        if (Intent.ACTION_VIEW.equals(action)) {
            if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: data=%s", intent.getData().toString()));

            Uri uri = intent.getData();
            if (uri == null) {
                if(D)Log.d(TAG, "DownloadActivity.onCreate - no uri in intent.getData, so bailing");
                finish();
                return;
            }

            List<String> segments = uri.getPathSegments();
            if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: segment count=%d", segments.size()));
            if (segments == null || segments.size() != Config.webUrlSegmentCount) {
                if(D)Log.d(TAG, "DownloadActivity.onCreate - no segments in URI, so bailing");
                finish();
                return;
            }

            m_userUuid = uri.getPathSegments().get(1);
            m_slideShareName = uri.getLastPathSegment();
            if(D)Log.d(TAG, String.format("DownloadActivity.onCreate: m_userUuid=%s, m_slideShareName=%s", m_userUuid, m_slideShareName));

            if (m_userUuid == null || m_slideShareName == null) {
                if(D)Log.d(TAG, "DownloadActivity.onCreate - m_userUuid or m_slideShareNmae is null, so bailing");
                if (m_downloadForEdit) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                else {
                    finish();
                }
                return;
            }

            if (!m_downloadForEdit) {
                // Check to see if we already have this m_slideShareName downloaded.
                String slideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(this), null);
                if (m_slideShareName.equalsIgnoreCase(slideShareName)) {
                    if(D)Log.d(TAG, "DownloadActivity.onCreate - we already have this Stori downloaded. Use it rather than refetching");

                    Intent intentPlay = new Intent(this, PlaySlidesActivity.class);
                    intentPlay.putExtra(PlaySlidesActivity.EXTRA_FROMURL, true);
                    intentPlay.putExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, m_intentFromStoriApp);
                    startActivity(intentPlay);

                    finish();
                    return;
                }
            }

            String jsonUrl = Config.baseAWSStorageUrl + m_userUuid + "/" + m_slideShareName + "/" + Config.slideShareJSONFilename;

            m_downloadTask = new DownloadTask(this);

            m_progressDialog = new ProgressDialog(this);
            m_progressDialog.setMessage(getString(R.string.download_dialog_message));
            m_progressDialog.setIndeterminate(true);
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setCancelable(true);
            m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if(D)Log.d(TAG, "DownloadActivity.onCancel: progress dialog canceled");
                    m_downloadTask.cancel(true);
                }
            });

            m_downloadTask.execute(jsonUrl);
        }
        else {
            if(D)Log.d(TAG, "DownloadActivity.onCreate - not an ACTION_VIEW intent, so bailing");
            finish();
            return;
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, Boolean> {
        private Context m_context;

        public DownloadTask(Context context) {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask constructor");

            m_context = context;
        }

        @Override
        protected Boolean doInBackground(String... sUrl) {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.doInBackground");

            // Take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager)m_context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            wl.acquire();
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.doInBackground - WakeLock acquired");

            try {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0]);
                    connection = (HttpURLConnection)url.openConnection();
                    connection.connect();

                    // Expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        if(D)Log.d(TAG, String.format(
                                "DownloadActivity.DownloadTask.doInBackground: http request failed: code=%d, message=%s",
                                connection.getResponseCode(), connection.getResponseMessage()));
                        return false;
                    }

                    // This will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // Download the file
                    input = connection.getInputStream();

                    File slideShareDirectory = Utilities.createOrGetSlideShareDirectory(DownloadActivity.this, m_slideShareName);

                    if (!slideShareDirectory.exists()) {
                        slideShareDirectory.mkdir();
                    }

                    String fileName = url.getFile();
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.doInBackground - creating output file for file name: %s", fileName));

                    File file = new File(slideShareDirectory, fileName);
                    if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.doInBackground - creating output file %s", file.getAbsolutePath()));
                    file.createNewFile();

                    output = new FileOutputStream(file);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // Allow canceling with back button
                        if (isCancelled()) {
                            return false;
                        }
                        total += count;
                        // Publishing the progress....
                        if (fileLength > 0) { // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        }
                        output.write(data, 0, count);
                    }
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "DownloadActivity.DownloadTask.doInBackground", e);
                    e.printStackTrace();
                    return false;
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "DownloadActivity.DownloadTask.doInBackground", e);
                    e.printStackTrace();
                    return false;
                }
                finally {
                    try {
                        if (output != null) {
                            output.close();
                        }
                        if (input != null) {
                            input.close();
                        }
                    }
                    catch (IOException ignored) { }

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
            finally {
                wl.release();
                if(D)Log.d(TAG, "DownloadActivity.DownloadTask.doInBackground - WakeLock released");
            }

            return true;
        }

        @Override
        protected void onPreExecute() {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPreExecute");

            super.onPreExecute();

            m_progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.onProgressUpdate: progress=%d", progress[0]));

            super.onProgressUpdate(progress);

            // If we get here, length is known, now set indeterminate to false
            m_progressDialog.setIndeterminate(false);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onCancelled() {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onCancelled");

            m_progressDialog.dismiss();
            handleDownloadError();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPostExecute");

            m_progressDialog.dismiss();

            if (!result) {
                if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPostExecute - download failed");
                handleDownloadError();
                return;
            }

            if (m_ssj == null) {
                m_ssj = SlideShareJSON.load(DownloadActivity.this, m_slideShareName, Config.slideShareJSONFilename);
                if (m_ssj == null) {
                    handleDownloadError();
                    return;
                }

                try {
                    calculateDownloadStats();

                    int count = m_ssj.getSlideCount();
                    m_urlsToDownload = new ArrayList<String>();

                    for (int i = 0; i < count; i++) {
                        SlideJSON slide = m_ssj.getSlide(i);

                        String audioUrlString = slide.getAudioUrlString();
                        String imageUrlString = slide.getImageUrlString();

                        if (audioUrlString != null) {
                            m_urlsToDownload.add(audioUrlString);
                        }
                        if (imageUrlString != null) {
                            m_urlsToDownload.add(imageUrlString);
                        }
                    }
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "DownloadActivity.DownloadTask.onPostExecute", e);
                    e.printStackTrace();

                    handleDownloadError();
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "DownloadActivity.DownloadTask.onPostExecute", e);
                    e.printStackTrace();

                    handleDownloadError();
                }
            }

            if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.onPostExecute: remaining URLs to download: %d", m_urlsToDownload.size()));

            if (m_urlsToDownload.size() <= 0) {
                if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPostExecute - all downloads complete");

                if (m_downloadForEdit) {
                    String oldSlideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), SSPreferences.DEFAULT_EDITPROJECTNAME(DownloadActivity.this));
                    if (oldSlideShareName != null && !oldSlideShareName.equals(m_slideShareName)) {
                        if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.onPostExecute: deleting old slideshare editplay directory for %s", oldSlideShareName));
                        Utilities.deleteSlideShareDirectory(DownloadActivity.this, oldSlideShareName);
                    }

                    SharedPreferences.Editor edit = m_prefs.edit();
                    edit.putString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), m_slideShareName);
                    edit.commit();

                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                else {
                    String oldSlideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(DownloadActivity.this), SSPreferences.DEFAULT_PLAYSLIDESNAME(DownloadActivity.this));
                    if (oldSlideShareName != null && !oldSlideShareName.equals(m_slideShareName)) {
                        if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.onPostExecute: deleting old slideshare playslide directory for %s", oldSlideShareName));
                        Utilities.deleteSlideShareDirectory(DownloadActivity.this, oldSlideShareName);
                    }

                    SharedPreferences.Editor edit = m_prefs.edit();
                    edit.putString(SSPreferences.PREFS_PLAYSLIDESNAME(DownloadActivity.this), m_slideShareName);
                    edit.commit();

                    Intent intent = new Intent(DownloadActivity.this, PlaySlidesActivity.class);
                    intent.putExtra(PlaySlidesActivity.EXTRA_FROMURL, true);
                    intent.putExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, m_intentFromStoriApp);
                    DownloadActivity.this.startActivity(intent);

                    finish();
                    return;
                }
            }

            String nextUrl = m_urlsToDownload.get(0);
            m_urlsToDownload.remove(0);

            if(D)Log.d(TAG, String.format(
                    "DownloadActivity.DownloadTask.onPostExecute: m_numberOfResources=%d, m_currentResourceDownloadIndex=%d, downloading %s, m_",
                    m_numberOfResources, m_numberOfResources - m_urlsToDownload.size(), nextUrl));
            m_downloadTask = new DownloadTask(DownloadActivity.this);

            m_progressDialog.setMessage(String.format(getString(R.string.download_dialog_message_format), m_currentResourceDownloadIndex + 1, m_numberOfResources));
            m_currentResourceDownloadIndex++;

            m_downloadTask.execute(nextUrl);
        }
    }

    private void calculateDownloadStats() throws Exception {
        if(D)Log.d(TAG, "DownloadActivity.calculateDownloadStats");

        int count = m_ssj.getSlideCount();

        for (int i = 0; i < count; i++) {
            SlideJSON sj = m_ssj.getSlide(i);

            if (sj.getAudioUrlString() != null) m_numberOfResources++;
            if (sj.getImageUrlString() != null) m_numberOfResources++;
        }

        m_currentResourceDownloadIndex = 0;
        if(D)Log.d(TAG, String.format("DownloadActivity.calculateDownloadStats: m_numberOfResources=%d", m_numberOfResources));
    }

    private void handleDownloadError() {
        if(D)Log.d(TAG, "DownloadActivity.handleDownloadError");

        if (m_slideShareName != null) {
            Utilities.deleteSlideShareDirectory(this, m_slideShareName);
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setCancelable(false);

        adb.setTitle(getString(R.string.download_errordialog_title));
        adb.setMessage(getString(R.string.download_errordialog_message));
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                finish();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }
}
