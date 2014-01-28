package com.stori.stori;

//
// Async copy file class
//

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class AsyncCopyFileTask extends AsyncTask<Object, Void, AsyncCopyFileTask.CopyFileTaskParams> {
    public final static String TAG = "AsyncCopyFileTask";

    private ProgressDialog m_copyFileProgressDialog = null;

    public interface AsyncCopyFileTaskCallbacks {
        public void onCopyComplete(boolean success, String[] fileNames, String slideShareName);
    }

    public enum CopyFileTaskType {
        Gallery,
        Camera,
        File
    }

    public class CopyFileTaskParams {

        public CopyFileTaskParams(CopyFileTaskType fileType, String slideShareName, String[] fileNames, Uri imageUri, String cameraPhotoFilePath, Activity activity, AsyncCopyFileTaskCallbacks cb) {
            m_fileType = fileType;
            m_slideShareName = slideShareName;
            m_fileNames = fileNames;
            m_imageUri = imageUri;
            m_cameraPhotoFilePath = cameraPhotoFilePath;
            m_success = false;
            m_callbacks = cb;
            m_activityParent = activity;
        }

        public CopyFileTaskType m_fileType;
        public String m_slideShareName;
        public String[] m_fileNames;
        public Uri m_imageUri;
        public String m_cameraPhotoFilePath;
        public Boolean m_success;
        public AsyncCopyFileTaskCallbacks m_callbacks;
        public Activity m_activityParent;
    }

    public void copyFile(CopyFileTaskType fileType, Activity activity, AsyncCopyFileTaskCallbacks cb, String slideShareName, String[] fileNames, Uri uri, String cameraPhotoFilePath) {
        if(D)Log.d(TAG, "AsyncCopyFileTask.copyFile");

        Utilities.freezeActivityOrientation(activity);

        m_copyFileProgressDialog = new ProgressDialog(activity);
        m_copyFileProgressDialog.setTitle(activity.getString(R.string.editplay_copydialog_title));
        m_copyFileProgressDialog.setCancelable(false);
        m_copyFileProgressDialog.setIndeterminate(true);
        m_copyFileProgressDialog.show();

        CopyFileTaskParams cftp = new CopyFileTaskParams(fileType, slideShareName, fileNames, uri, cameraPhotoFilePath, activity, cb);

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
                cb.onCopyComplete(false, cftp.m_fileNames, cftp.m_slideShareName);
            }
        }
    }

    @Override
    protected CopyFileTaskParams doInBackground(Object... params) {
        if(D) Log.d(TAG, "AsyncCopyFileTask.doInBackground");

        CopyFileTaskParams cftp = (CopyFileTaskParams)params[0];

        switch (cftp.m_fileType) {
            case Gallery:
                cftp.m_success = Utilities.copyGalleryImageToJPG(cftp.m_activityParent, cftp.m_slideShareName, cftp.m_fileNames[0], cftp.m_imageUri);
                break;

            case Camera:
                cftp.m_success = Utilities.copyExternalStorageImageToJPG(cftp.m_activityParent, cftp.m_slideShareName, cftp.m_fileNames[0], cftp.m_cameraPhotoFilePath);
                break;

            case File:
                cftp.m_success = copyFiles(cftp);
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
            cftp.m_callbacks.onCopyComplete(cftp.m_success, cftp.m_fileNames, cftp.m_slideShareName);
        }
    }

    private boolean copyFiles(CopyFileTaskParams cftp) {
        if(D)Log.d(TAG, "AsyncCopyFileTask.copyFiles");

        if (cftp == null) {
            if(D)Log.d(TAG, "AsyncCopyFileTask.copyFiles - cftp is null, so bailing");
            return false;
        }
        if (cftp.m_fileNames == null || cftp.m_fileNames.length == 0) {
            if(D)Log.d(TAG, "AsyncCopyFileTask.copyFiles - invalid m_fileNames, so bailing");
            return false;
        }

        boolean success = false;
        File slideShareDirectory = Utilities.createOrGetSlideShareDirectory(cftp.m_activityParent, cftp.m_slideShareName);

        for (int i = 0; i < cftp.m_fileNames.length; i++) {
            try {
                File fileSource = new File(slideShareDirectory, cftp.m_fileNames[i]);
                File filePathDest = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Config.copiedImageFolderName);

                if (!filePathDest.exists()) {
                    filePathDest.mkdir();
                }

                File fileDest = new File(filePathDest, cftp.m_fileNames[i]);

                success = Utilities.copyFile(fileSource, fileDest);
                if (!success) {
                    break;
                }
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "AsyncCopyFileTask.copyFiles", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "AsyncCopyFileTask.copyFiles", e);
                e.printStackTrace();
            }
        }

        return success;
    }
}
