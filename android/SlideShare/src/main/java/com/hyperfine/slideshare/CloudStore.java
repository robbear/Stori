package com.hyperfine.slideshare;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.RejectedExecutionException;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class CloudStore {
    public final static String TAG = "CloudStore";

    CloudProviders m_cloudProvider;
    Context m_context;
    String m_slideShareName;
    ICloudStoreCallback m_callback;
    SlideShareJSON m_ssj;
    boolean m_saveInProgress = false;

    public interface ICloudStoreCallback {
        public void onSaveComplete(SaveErrors err);
    }

    public enum SaveErrors {
        None,
        Error_LoadJSON,
        Error_Unknown
    };
    public SaveErrors[] SaveErrorsValues = SaveErrors.values();

    public enum CloudProviders {
        Azure,
        S3
    };
    // Cache TransitionEffects.values() for doing enum-to-int conversions
    public CloudProviders[] CloudProvidersValues = CloudProviders.values();

    protected class SaveTask extends AsyncTask<Object, Void, SaveErrors> {

        @Override
        protected SaveErrors doInBackground(Object... params) {
            if(D)Log.d(TAG, "CloudStore.SaveTask.doInBackground");

            try {
                String[] imageFileNames = m_ssj.getImageFileNames();
                String[] audioFileNames = m_ssj.getAudioFileNames();

                for (String fileName : imageFileNames) {
                    // Save the image file
                }
                for (String fileName : audioFileNames) {
                    // Save the audio file
                }

                // Save the json file
            }
            catch (Exception e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.doInBackground", e);
                e.printStackTrace();
            }
            catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "CloudStore.SaveTask.doInBackground", e);
                e.printStackTrace();
            }

            return SaveErrors.None;
        }

        @Override
        protected void onPostExecute(SaveErrors saveErrors) {
            if(D)Log.d(TAG, String.format("CloudStore.SaveTask.onPostExecute: %s", saveErrors));

            m_callback.onSaveComplete(saveErrors);
        }
    }

    public CloudStore(Context context, String slideShareName, CloudProviders cloudProvider, ICloudStoreCallback callback) {
        if(D)Log.d(TAG, String.format("CloudStore.CloudStore(%s)", cloudProvider));

        m_context = context;
        m_slideShareName = slideShareName;
        m_cloudProvider = cloudProvider;
        m_callback = callback;
    }

    public void saveAsync() {
        if(D)Log.d(TAG, "CloudStore.save");

        m_ssj = SlideShareJSON.load(m_context, m_slideShareName, Config.slideShareJSONFilename);
        if (m_ssj == null) {
            m_callback.onSaveComplete(SaveErrors.Error_LoadJSON);
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
