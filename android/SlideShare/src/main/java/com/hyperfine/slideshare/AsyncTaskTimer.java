package com.hyperfine.slideshare;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.RejectedExecutionException;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class AsyncTaskTimer extends AsyncTask<Object, Void, AsyncTaskTimer.AsyncTaskTimerParams> {
    public final static String TAG = "AsyncTaskTimer";

    public interface IAsyncTaskTimerCallback {
        void onAsyncTaskTimerComplete(long cookie);
    }

    public static class AsyncTaskTimerParams {
        public AsyncTaskTimerParams(long cookie, int delayMillis, IAsyncTaskTimerCallback callback) {
            m_cookie = cookie;
            m_delayMillis = delayMillis;
            m_callback = callback;
        }

        public long m_cookie;
        public int m_delayMillis;
        public IAsyncTaskTimerCallback m_callback;
    }

    @Override
    protected AsyncTaskTimerParams doInBackground(Object... params) {
        if(D)Log.d(TAG, "AsyncTaskTimer.doInBackground");

        AsyncTaskTimerParams attp = (AsyncTaskTimerParams)params[0];

        try {
            Thread.sleep(attp.m_delayMillis);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "AsyncTaskTimer.doInBackground", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "AsyncTaskTimer.doInBackground", e);
            e.printStackTrace();
        }

        return attp;
    }

    @Override
    protected void onPostExecute(AsyncTaskTimerParams attp) {
        if(D)Log.d(TAG, "AsyncTaskTimer.onPostExecute");

        if (attp.m_callback != null) {
            attp.m_callback.onAsyncTaskTimerComplete(attp.m_cookie);
        }
    }

    public static void startAsyncTaskTimer(long cookie, int delayMillis, IAsyncTaskTimerCallback callback) {
        if(D)Log.d(AsyncTaskTimer.TAG, String.format("AsyncTaskTimer.startAsyncTaskTimer(%d)", delayMillis));

        AsyncTaskTimerParams attp = new AsyncTaskTimerParams(cookie, delayMillis, callback);

        try {
            new AsyncTaskTimer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attp);
        }
        catch (RejectedExecutionException e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
        }
    }
}
