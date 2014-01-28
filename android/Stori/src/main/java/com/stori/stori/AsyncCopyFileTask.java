package com.stori.stori;

//
// Async copy file class
//

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class AsyncCopyFileTask extends AsyncTask<Object, Void, AsyncCopyFileTask.CopyFileTaskParams> {
    public final static String TAG = "AsyncCopyFileTask";

    private ProgressDialog m_copyFileProgressDialog = null;

    public interface AsyncCopyFileTaskCallbacks {
        public void onCopyComplete(boolean success, String fileName, String slideShareName);
    }

    public enum CopyFileTaskType {
        Gallery,
        Camera,
        File
    }

    public class CopyFileTaskParams {

        public CopyFileTaskParams(CopyFileTaskType fileType, String slideShareName, String fileName, Uri imageUri, String cameraPhotoFilePath, Activity activity, AsyncCopyFileTaskCallbacks cb) {
            m_fileType = fileType;
            m_slideShareName = slideShareName;
            m_fileName = fileName;
            m_imageUri = imageUri;
            m_cameraPhotoFilePath = cameraPhotoFilePath;
            m_success = false;
            m_callbacks = cb;
            m_activityParent = activity;
        }

        public CopyFileTaskType m_fileType;
        public String m_slideShareName;
        public String m_fileName;
        public Uri m_imageUri;
        public String m_cameraPhotoFilePath;
        public Boolean m_success;
        public AsyncCopyFileTaskCallbacks m_callbacks;
        public Activity m_activityParent;
    }

    public void copyFile(CopyFileTaskType fileType, Activity activity, AsyncCopyFileTaskCallbacks cb, String slideShareName, String fileName, Uri uri, String cameraPhotoFilePath) {
        if(D)Log.d(TAG, "AsyncCopyFileTask.copyFile");

        Utilities.freezeActivityOrientation(activity);

        m_copyFileProgressDialog = new ProgressDialog(activity);
        m_copyFileProgressDialog.setTitle(activity.getString(R.string.editplay_copydialog_title));
        m_copyFileProgressDialog.setCancelable(false);
        m_copyFileProgressDialog.setIndeterminate(true);
        m_copyFileProgressDialog.show();

        CopyFileTaskParams cftp = new CopyFileTaskParams(fileType, slideShareName, fileName, uri, cameraPhotoFilePath, activity, cb);

        boolean fFail = false;

        try {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, cftp);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "AsyncCopyFileTask.copyFile", e);
            e.printStackTrace();
            fFail = true;
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "AsyncCopyFileTask.copyFile", e);
            e.printStackTrace();
            fFail = true;
        }

        if (fFail) {
            m_copyFileProgressDialog.dismiss();
            m_copyFileProgressDialog = null;

            if (cb != null) {
                cb.onCopyComplete(false, cftp.m_fileName, cftp.m_slideShareName);
            }
        }
    }

    @Override
    protected CopyFileTaskParams doInBackground(Object... params) {
        if(D) Log.d(TAG, "AsyncCopyFileTask.doInBackground");

        CopyFileTaskParams cftp = (CopyFileTaskParams)params[0];

        switch (cftp.m_fileType) {
            case Gallery:
                cftp.m_success = Utilities.copyGalleryImageToJPG(cftp.m_activityParent, cftp.m_slideShareName, cftp.m_fileName, cftp.m_imageUri);
                break;

            case Camera:
                cftp.m_success = Utilities.copyExternalStorageImageToJPG(cftp.m_activityParent, cftp.m_slideShareName, cftp.m_fileName, cftp.m_cameraPhotoFilePath);
                break;

            case File:
                break;
        }

        return cftp;
    }

    @Override
    protected void onPostExecute(AsyncCopyFileTask.CopyFileTaskParams cftp) {
        if(D)Log.d(TAG, String.format("AsyncCopyFileTask.onPostExecute: success=%b", cftp.m_success));

        m_copyFileProgressDialog.dismiss();
        m_copyFileProgressDialog = null;

        Utilities.unfreezeOrientation(cftp.m_activityParent);

        if (cftp.m_callbacks != null) {
            cftp.m_callbacks.onCopyComplete(cftp.m_success, cftp.m_fileName, cftp.m_slideShareName);
        }
    }
}
