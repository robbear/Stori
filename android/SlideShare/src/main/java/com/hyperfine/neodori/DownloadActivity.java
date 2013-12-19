package com.hyperfine.neodori;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class DownloadActivity extends FragmentActivity {
    public final static String TAG = "DownloadActivity";

    private ProgressDialog m_progressDialog;
    private DownloadTask m_downloadTask;
    private String m_slideShareName;
    private String m_userUuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D)Log.d(TAG, "DownloadActivity.onCreate");

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

            String jsonUrl = Config.baseAWSStorageUrl + m_userUuid + "/" + m_slideShareName + "/" + Config.slideShareJSONFilename;

            m_downloadTask = new DownloadTask(this);

            m_progressDialog = new ProgressDialog(this);
            m_progressDialog.setMessage(String.format("Downloading %s", jsonUrl));
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

    private class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context m_context;

        public DownloadTask(Context context) {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask constructor");

            m_context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
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
                        if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.doInBackground: http code = %d", connection.getResponseCode()));
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();
                    }

                    // This will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // Download the file
                    input = connection.getInputStream();

                    File slideShareDirectory = Utilities.createOrGetSlideShareDirectory(DownloadActivity.this, m_slideShareName);
                    slideShareDirectory.mkdir();
                    File file = new File(slideShareDirectory, Config.slideShareJSONFilename);
                    if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.doInBackground - creating output file %s", file.getAbsolutePath()));
                    file.createNewFile();

                    output = new FileOutputStream(file);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // Allow canceling with back button
                        if (isCancelled()) {
                            return null;
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
                    return e.toString();
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

            return null;
        }

        @Override
        protected void onPreExecute() {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPreExecute");

            super.onPreExecute();

            m_progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if(D)Log.d(TAG, String.format("DownloadActivity.DownloadTask.onProgressUpdate: progress=%d", progress[0]));

            super.onProgressUpdate(progress);

            // If we get here, length is known, now set indeterminate to false
            m_progressDialog.setIndeterminate(false);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if(D)Log.d(TAG, "DownloadActivity.DownloadTask.onPostExecute");

            m_progressDialog.dismiss();
            if (result != null) {
                Toast.makeText(m_context, "Download error: " + result, Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(m_context,"File downloaded", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
