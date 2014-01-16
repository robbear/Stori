package com.hyperfine.neodori;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class NeodoriService extends Service {
    public static final String TAG = "NeodoriService";

    @Override
    public void onCreate() {
        if(D)Log.d(TAG, "NeodoriService.onCreate");
    }

    @Override
    public void onDestroy() {
        if(D)Log.d(TAG, "NeodoriService.onDestroy");
    }

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
