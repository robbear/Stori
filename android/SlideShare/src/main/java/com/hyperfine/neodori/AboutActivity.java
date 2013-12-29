package com.hyperfine.neodori;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import static com.hyperfine.neodori.Config.D;

//
// About neodori
//
public class AboutActivity extends Activity
{
    public final static String TAG = "AboutActivity";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(D) Log.d(TAG, "AboutActivity.onOptionsItemSelected");

        switch (item.getItemId()) {
            case android.R.id.home:
                if(D)Log.d(TAG, "AboutActivity.onOptionsItemSelected: up button clicked. Finishing activity.");
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        if(D)Log.d(TAG, "AboutActivity.onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.about_actionbar));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onStart()
    {
        if(D)Log.d(TAG, "AboutActivity.onStart");

        super.onStart();

        String versionName;
        int versionCode = 0;

        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pi.versionName;
            versionCode = pi.versionCode;

            ((TextView)findViewById(R.id.about_text)).setText(String.format(getString(R.string.copyright), versionName, versionCode, Config.buildString));
        }
        catch (Exception ignore)
        {
        }
    }

    @Override
    public void onStop()
    {
        if(D)Log.d(TAG, "AboutActivity.onStop");

        super.onStop();
    }

    @Override
    public void onPause()
    {
        if(D)Log.d(TAG, "AboutActivity.onPause");

        super.onPause();
    }

    @Override
    public void onResume()
    {
        if(D)Log.d(TAG, "AboutActivity.onResume");

        super.onResume();
    }

    @Override
    public void onRestart()
    {
        if(D)Log.d(TAG, "AboutActivity.onRestart");

        super.onRestart();
    }
}
