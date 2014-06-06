package com.stori.stori;

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

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class DownloadActivity extends FragmentActivity {
    public final static String TAG = "DownloadActivity";
    public final static String REQUEST_TAG = "VolleyBinaryRequestTag";

    private RequestQueue m_requestQueue;
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
        m_requestQueue = Volley.newRequestQueue(this);

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
                // Check to see if we're editing this m_slideShareName. If so, we don't want to download
                // and overwrite it. See issue #52
                String slideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(this), null);
                if (m_slideShareName.equalsIgnoreCase(slideShareName)) {
                    if(D)Log.d(TAG, "DownloadActivity.onCreate - this Stori is currently under edit. Launch Stori to EditPlayActivity->PlaySlidesActivity rather than downloading");

                    Intent intentLaunch = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    intentLaunch.putExtra(EditPlayActivity.EXTRA_LAUNCH_INTO_PLAY, true);
                    startActivity(intentLaunch);

                    finish();
                    return;
                }

                // Check to see if we already have this m_slideShareName downloaded.
                // We also need to check whether files for this actually exist. See issue #63
                slideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(this), null);
                if (m_slideShareName.equalsIgnoreCase(slideShareName)) {
                    if (Utilities.doStoriFilesExist(this, m_slideShareName)) {
                        if(D)Log.d(TAG, "DownloadActivity.onCreate - we already have this Stori downloaded. Use it rather than refetching");

                        Intent intentPlay = new Intent(this, PlaySlidesActivity.class);
                        intentPlay.putExtra(PlaySlidesActivity.EXTRA_FROMURL, true);
                        intentPlay.putExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, m_intentFromStoriApp);
                        startActivity(intentPlay);

                        finish();
                        return;
                    }
                }
            }

            String jsonUrl = Config.baseAWSStorageUrl + m_userUuid + "/" + m_slideShareName + "/" + Config.slideShareJSONFilename;

            /* NEVER
            m_downloadTask = new DownloadTask(this);
            */

            m_progressDialog = new ProgressDialog(this);
            m_progressDialog.setMessage(getString(R.string.download_dialog_message));
            m_progressDialog.setIndeterminate(true);
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setCancelable(true);
            m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if(D)Log.d(TAG, "DownloadActivity.onCancel: progress dialog canceled");
                    /* NEVER
                    m_downloadTask.cancel(true);
                    */
                }
            });

            VolleyBinaryRequest binaryRequest = new VolleyBinaryRequest(jsonUrl, new ResponseListener(jsonUrl), new ErrorListener(jsonUrl));
            binaryRequest.setTag(REQUEST_TAG);
            m_requestQueue.add(binaryRequest);

            /* NEVER
            m_downloadTask.execute(jsonUrl);
            */
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

                    byte data[] = new byte[Config.downloadBufferSize];
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

                    // Update the version
                    // Fixes issue #61
                    try {
                        int version = m_ssj.getVersion();
                        m_ssj.setVersion(version + 1);
                        m_ssj.save(DownloadActivity.this, m_slideShareName, Config.slideShareJSONFilename);
                    }
                    catch (Exception e) {
                        if(E)Log.e(TAG, "DownloadActivity.DownloadTask.onPostExecute", e);
                        e.printStackTrace();
                    }
                    catch (OutOfMemoryError e) {
                        if(E)Log.e(TAG, "DownloadActivity.DownloadTask.onPostExecute", e);
                        e.printStackTrace();
                    }

                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                else {
                    String oldSlideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(DownloadActivity.this), SSPreferences.DEFAULT_PLAYSLIDESNAME(DownloadActivity.this));
                    String currentEditSlideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), SSPreferences.DEFAULT_EDITPROJECTNAME(DownloadActivity.this));

                    // Need to test for the case where the current Edit slideShareName is the same as the old Play slideShareName.
                    // This will occur when the user is editing "Foo" which has previously been published, then downloads Foo for play.
                    // In this case, both Edit and Play slideshares are the same. If the user then downloads another Stori for play,
                    // we do NOT want to delete the old Play directory, because this is the same directory being used for the current Edit.
                    // See Issue #51

                    // We also test for oldSlideShareName being the same as m_slideShareName. In this case we also don't want to delete
                    // the old directory because it's the one we're currently using.

                    if (oldSlideShareName != null && !oldSlideShareName.equals(m_slideShareName) && !oldSlideShareName.equals(currentEditSlideShareName)) {
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
                    "DownloadActivity.DownloadTask.onPostExecute: m_numberOfResources=%d, m_currentResourceDownloadIndex=%d, downloading %s",
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
        m_urlsToDownload = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            SlideJSON sj = m_ssj.getSlide(i);

            if (sj.getAudioUrlString() != null) {
                m_numberOfResources++;
                m_urlsToDownload.add(sj.getAudioUrlString());
            }
            if (sj.getImageUrlString() != null) {
                m_numberOfResources++;
                m_urlsToDownload.add(sj.getImageUrlString());
            }
        }

        m_currentResourceDownloadIndex = 0;
        if(D)Log.d(TAG, String.format("DownloadActivity.calculateDownloadStats: m_numberOfResources=%d", m_numberOfResources));
    }

    private void handleDownloadError() {
        if(D)Log.d(TAG, "DownloadActivity.handleDownloadError");

        m_requestQueue.cancelAll(REQUEST_TAG);

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


    //
    // VolleyBinaryRequest listeners
    //

    private class ResponseListener implements Response.Listener<byte[]> {
        private String m_urlString;

        public ResponseListener(String urlString) {
            this.m_urlString = urlString;
        }

        @Override
        public void onResponse(byte[] response) {
            if(D)Log.d(TAG, String.format("DownloadActivity.onResponse - got %d bytes for %s", response == null ? 0 : response.length, this.m_urlString));

            boolean success = saveBytesToFile(response);
            if (!success) {
                if(D)Log.d(TAG, String.format("DownloadActivity.onResponse - failed to save to file, bailing: %s", this.m_urlString));
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

                    for (int i = 0; i < m_urlsToDownload.size(); i++) {
                        String url = m_urlsToDownload.get(i);

                        VolleyBinaryRequest vbr = new VolleyBinaryRequest(url, new ResponseListener(url), new ErrorListener(url));
                        vbr.setTag(REQUEST_TAG);
                        m_requestQueue.add(vbr);
                    }
                }
                catch (Exception e) {
                    if(E)Log.e(TAG, "DownloadActivity.onResponse", e);
                    e.printStackTrace();

                    handleDownloadError();
                }
                catch (OutOfMemoryError e) {
                    if(E)Log.e(TAG, "DownloadActivity.onResponse", e);
                    e.printStackTrace();

                    handleDownloadError();
                }
            }
            else {
                m_numberOfResources--;
                if(D)Log.d(TAG, String.format("DownloadActivity.onResponse: remaining URLs to download: %d", m_numberOfResources));

                if (m_numberOfResources <= 0) {
                    if(D)Log.d(TAG, "DownloadActivity.onResponse - all downloads complete");

                    if (m_downloadForEdit) {
                        String oldSlideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), SSPreferences.DEFAULT_EDITPROJECTNAME(DownloadActivity.this));
                        if (oldSlideShareName != null && !oldSlideShareName.equals(m_slideShareName)) {
                            if(D)Log.d(TAG, String.format("DownloadActivity.onResponse: deleting old slideshare editplay directory for %s", oldSlideShareName));
                            Utilities.deleteSlideShareDirectory(DownloadActivity.this, oldSlideShareName);
                        }

                        SharedPreferences.Editor edit = m_prefs.edit();
                        edit.putString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), m_slideShareName);
                        edit.commit();

                        // Update the version
                        // Fixes issue #61
                        try {
                            int version = m_ssj.getVersion();
                            m_ssj.setVersion(version + 1);
                            m_ssj.save(DownloadActivity.this, m_slideShareName, Config.slideShareJSONFilename);
                        }
                        catch (Exception e) {
                            if(E)Log.e(TAG, "DownloadActivity.onResponse", e);
                            e.printStackTrace();
                        }
                        catch (OutOfMemoryError e) {
                            if(E)Log.e(TAG, "DownloadActivity.onResponse", e);
                            e.printStackTrace();
                        }

                        m_progressDialog.dismiss();
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    else {
                        String oldSlideShareName = m_prefs.getString(SSPreferences.PREFS_PLAYSLIDESNAME(DownloadActivity.this), SSPreferences.DEFAULT_PLAYSLIDESNAME(DownloadActivity.this));
                        String currentEditSlideShareName = m_prefs.getString(SSPreferences.PREFS_EDITPROJECTNAME(DownloadActivity.this), SSPreferences.DEFAULT_EDITPROJECTNAME(DownloadActivity.this));

                        // Need to test for the case where the current Edit slideShareName is the same as the old Play slideShareName.
                        // This will occur when the user is editing "Foo" which has previously been published, then downloads Foo for play.
                        // In this case, both Edit and Play slideshares are the same. If the user then downloads another Stori for play,
                        // we do NOT want to delete the old Play directory, because this is the same directory being used for the current Edit.
                        // See Issue #51

                        // We also test for oldSlideShareName being the same as m_slideShareName. In this case we also don't want to delete
                        // the old directory because it's the one we're currently using.

                        if (oldSlideShareName != null && !oldSlideShareName.equals(m_slideShareName) && !oldSlideShareName.equals(currentEditSlideShareName)) {
                            if(D)Log.d(TAG, String.format("DownloadActivity.onResponse: deleting old slideshare playslide directory for %s", oldSlideShareName));
                            Utilities.deleteSlideShareDirectory(DownloadActivity.this, oldSlideShareName);
                        }

                        SharedPreferences.Editor edit = m_prefs.edit();
                        edit.putString(SSPreferences.PREFS_PLAYSLIDESNAME(DownloadActivity.this), m_slideShareName);
                        edit.commit();

                        Intent intent = new Intent(DownloadActivity.this, PlaySlidesActivity.class);
                        intent.putExtra(PlaySlidesActivity.EXTRA_FROMURL, true);
                        intent.putExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, m_intentFromStoriApp);
                        DownloadActivity.this.startActivity(intent);

                        m_progressDialog.dismiss();
                        finish();
                        return;
                    }
                }
                else {
                    m_progressDialog.setMessage(String.format(getString(R.string.download_dialog_message_format), m_currentResourceDownloadIndex + 1, m_urlsToDownload.size()));
                    m_currentResourceDownloadIndex++;
                }
            }
        }

        private boolean saveBytesToFile(byte[] data) {
            if(D)Log.d(TAG, String.format("DownloadActivity.saveBytesToFile for %s", this.m_urlString));

            boolean success = false;

            OutputStream output = null;

            try {
                URL url = new URL(this.m_urlString);

                File slideShareDirectory = Utilities.createOrGetSlideShareDirectory(DownloadActivity.this, m_slideShareName);

                if (!slideShareDirectory.exists()) {
                    slideShareDirectory.mkdir();
                }

                String fileName = url.getFile();
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                if(D)Log.d(TAG, String.format("DownloadActivity.saveBytesToFile - creating output file for file name: %s", fileName));

                File file = new File(slideShareDirectory, fileName);
                if(D)Log.d(TAG, String.format("DownloadActivity.saveBytesToFile - creating output file %s", file.getAbsolutePath()));
                file.createNewFile();

                output = new FileOutputStream(file);
                output.write(data, 0, data.length);

                success = true;
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "DownloadActivity.saveBytesToFile", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "DownloadActivity.DownloadTask.doInBackground", e);
                e.printStackTrace();
            }
            finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                }
                catch (IOException ignored) { }
            }

            return success;
        }
    }

    private class ErrorListener implements Response.ErrorListener{
        private String m_urlString;

        public ErrorListener(String urlString) {
            m_urlString = urlString;
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if(D)Log.d(TAG, String.format("DownloadActivity.onErrorResponse = %s got error: %s", this.m_urlString, error.toString()));
            m_progressDialog.dismiss();

            handleDownloadError();
        }
    }
}
