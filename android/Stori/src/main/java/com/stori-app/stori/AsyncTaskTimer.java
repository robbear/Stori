package com.stori-app.stori;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.RejectedExecutionException;

import static com.stori-app.stori.Config.D;
import static com.stori-app.stori.Config.E;

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

        public AsyncTaskTimerParams(long cookie, int delayMillis, int numDelays, IAsyncTaskTimerCallback callback) {
            m_cookie = cookie;
            m_delayMillis = delayMillis;
            m_numDelays = numDelays;
            m_callback = callback;
        }

        public long m_cookie;
        public int m_delayMillis;
        public int m_numDelays = 0;
        public boolean m_isCancelled = false;
        public IAsyncTaskTimerCallback m_callback;
    }

    @Override
    protected AsyncTaskTimerParams doInBackground(Object... params) {
        if(D)Log.d(TAG, "AsyncTaskTimer.doInBackground");

        AsyncTaskTimerParams attp = (AsyncTaskTimerParams)params[0];

        try {
            if (attp.m_numDelays > 0) {
                for (int i = 0; i < attp.m_numDelays; i++) {
                    if (isCancelled()) {
                        if(D)Log.d(TAG, String.format("AsyncTaskTimer.doInBackground - cancelled on %d loop", i));
                        attp.m_isCancelled = true;
                        break;
                    }

                    Thread.sleep(attp.m_delayMillis);
                }
            }
            else {
                Thread.sleep(attp.m_delayMillis);
            }
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
    protected void onCancelled(AsyncTaskTimerParams attp) {
        if(D)Log.d(TAG, String.format("AsyncTaskTimer.onCancelled"));
    }

    @Override
    protected void onPostExecute(AsyncTaskTimerParams attp) {
        if(D)Log.d(TAG, "AsyncTaskTimer.onPostExecute");

        if (attp.m_callback != null) {
            attp.m_callback.onAsyncTaskTimerComplete(attp.m_cookie);
        }
    }

    public static AsyncTaskTimer startAsyncTaskTimer(long cookie, int delayMillis, IAsyncTaskTimerCallback callback) {
        return startAsyncTaskTimer(cookie, delayMillis, 0, callback);
    }

    public static AsyncTaskTimer startAsyncTaskTimer(long cookie, int delayMillis, int numDelays, IAsyncTaskTimerCallback callback) {
        if(D)Log.d(AsyncTaskTimer.TAG, String.format("AsyncTaskTimer.startAsyncTaskTimer: delayMillis=%d, numDelays=%d", delayMillis, numDelays));

        AsyncTaskTimer att = null;
        AsyncTaskTimerParams attp = new AsyncTaskTimerParams(cookie, delayMillis, numDelays, callback);

        try {
            att = new AsyncTaskTimer();
            att.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, attp);
        }
        catch (RejectedExecutionException e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
            att = null;
        }
        catch (Exception e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
            att = null;
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(AsyncTaskTimer.TAG, "AsyncTaskTimer.startAsyncTaskTimer", e);
            e.printStackTrace();
            att = null;
        }

        return att;
    }
}
