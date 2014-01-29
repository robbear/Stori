package com.stori.stori;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.stori.stori.cloudproviders.AWSS3Provider;
import com.stori.stori.cloudproviders.AmazonClientManager;
import com.stori.stori.cloudproviders.ICloudProvider;

import java.util.concurrent.RejectedExecutionException;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class CloudStore {
    public final static String TAG = "CloudStore";

    Config.CloudStorageProviders m_cloudProvider;
    Context m_context;
    String m_slideShareName;
    String m_userUuid;
    ICloudStoreCallback m_callback;
    SlideShareJSON m_ssj;
    boolean m_saveInProgress = false;

    public interface ICloudStoreCallback {
        public void onSaveComplete(SaveErrors err, SlideShareJSON ssj);
    }

    public enum SaveErrors {
        Success,
        Error_LoadJSON,
        Error_UploadFile,
        Error_OutOfMemory,
        Error_Unknown
    };
    public SaveErrors[] SaveErrorsValues = SaveErrors.values();

    protected class SaveTask extends AsyncTask<Object, Void, SaveErrors> {

        @Override
        protected SaveErrors doInBackground(Object... params) {
            if(D)Log.d(TAG, "CloudStore.SaveTask.doInBackground");

            SaveErrors se = SaveErrors.Success;

            try {
                String[] imageFileNames = m_ssj.getImageFileNames();
                String[] audioFileNames = m_ssj.getAudioFileNames();

                ICloudProvider icp;

                switch (Config.CLOUD_STORAGE_PROVIDER) {
                    default:
                    case AWS:
                        icp = new AWSS3Provider(m_context);
                        break;
                }

                icp.initializeProvider(m_userUuid, m_context.getSharedPreferences(SSPreferences.PREFS(m_context), Context.MODE_PRIVATE));
                icp.deleteVirtualDirectory(m_slideShareName);

                for (String fileName : imageFileNames) {
                    icp.uploadFile(m_slideShareName, fileName, "image/jpeg");
                }
                for (String fileName : audioFileNames) {
                    icp.uploadFile(m_slideShareName, fileName, "audio/mp4");
                }

                icp.uploadFile(m_slideShareName, Config.slideShareJSONFilename, "application/json");
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.doInBackground", e);
                e.printStackTrace();

                se = SaveErrors.Error_UploadFile;
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.doInBackground", e);
                e.printStackTrace();

                se = SaveErrors.Error_OutOfMemory;
            }

            return se;
        }

        @Override
        protected void onPostExecute(SaveErrors saveErrors) {
            if(D)Log.d(TAG, String.format("CloudStore.SaveTask.onPostExecute: %s", saveErrors));

            // Update the version value and save the JSON locally
            try {
                int curVersion = m_ssj.getVersion();
                m_ssj.setVersion(curVersion + 1);
                m_ssj.save(m_context, m_slideShareName, Config.slideShareJSONFilename);
                if(D)Log.d(TAG, "SlideShareJSON after publish:");
                Utilities.printSlideShareJSON(TAG, m_ssj);
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.onPostExecute", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.onPostExecute", e);
                e.printStackTrace();
            }

            m_callback.onSaveComplete(saveErrors, m_ssj);
        }
    }

    public CloudStore(Context context, String userUuid, String slideShareName, Config.CloudStorageProviders cloudProvider, ICloudStoreCallback callback) {
        if(D)Log.d(TAG, String.format("CloudStore.CloudStore: userUuid=%s, slideShareName=%s, cloudProvider=%s", userUuid, slideShareName, cloudProvider));

        m_context = context;
        m_userUuid = userUuid;
        m_slideShareName = slideShareName;
        m_cloudProvider = cloudProvider;
        m_callback = callback;
    }

    public void saveAsync() {
        if(D)Log.d(TAG, "CloudStore.save");

        m_ssj = SlideShareJSON.load(m_context, m_slideShareName, Config.slideShareJSONFilename);
        if (m_ssj == null) {
            m_callback.onSaveComplete(SaveErrors.Error_LoadJSON, null);
            return;
        }

        try {
            new SaveTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        catch (RejectedExecutionException e) {
            if(E)Log.e(TAG, "CloudStore.saveAsync", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "CloudStore.saveAsync", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "CloudStore.saveAsync", e);
            e.printStackTrace();
        }
    }
}
