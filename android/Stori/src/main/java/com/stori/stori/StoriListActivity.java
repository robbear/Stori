package com.stori.stori;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.stori.stori.adapters.StoriListAdapter;
import com.stori.stori.cloudproviders.AmazonSharedPreferencesWrapper;

import java.util.ArrayList;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class StoriListActivity extends ListActivity implements StoriService.ReadStoriItemsStateListener {
    public final static String TAG = "StoriListActivity";

    public final static String EXTRA_DOWNLOAD_FOR_EDIT = "extra_download_for_edit";

    private String m_userUuid = null;
    private StoriListAdapter m_adapter;
    private SharedPreferences m_prefs;
    private StoriService m_storiService = null;
    private ProgressDialog m_progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onCreate");

        super.onCreate(savedInstanceState);

        m_prefs = getSharedPreferences(SSPreferences.PREFS(this), Context.MODE_PRIVATE);
        m_userUuid = AmazonSharedPreferencesWrapper.getUsername(m_prefs);

        m_adapter = new StoriListAdapter(this, m_userUuid);
        setListAdapter(m_adapter);

        initializeStoriService();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "StoriListActivity.onSaveInstanceState");

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        if(D)Log.d(TAG, "StoriListActivity.onDestroy");

        super.onDestroy();

        uninitializeStoriService();
    }

    @Override
    protected void onStart() {
        if(D)Log.d(TAG, "StoriListActivity.onStart");

        super.onStart();

        initializeStoriService();
    }

    @Override
    protected void onStop() {
        if(D)Log.d(TAG, "StoriListActivity.onStop");

        super.onStop();
    }

    @Override
    protected void onResume() {
        if(D)Log.d(TAG, "StoriListActivity.onResume");

        super.onResume();
    }

    @Override
    protected void onPause() {
        if(D)Log.d(TAG, "StoriListActivity.onPause");

        super.onPause();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("StoriListActivity.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == EditPlayActivity.REQUEST_DOWNLOAD_FOR_EDIT) {
            if(D)Log.d(TAG, "StoriListActivity.onActivityResult - returned from DownloadActivity for REQUEST_DOWNLOAD_FOR_EDIT.");
            if (resultCode == RESULT_OK) {
                setResult(EditPlayActivity.RESULT_EDITDOWNLOAD_OK);
                finish();
            }
        }
        else {
            if(D)Log.d(TAG, "StoriListActivity.onActivityResult - passing on to super.onActivityResult");
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onReadStoriItemsComplete(ArrayList<StoriListItem> storiListItems) {
        if(D)Log.d(TAG, "StoriListActivity.onReadStoriItemsComplete");

        if (storiListItems == null) {
            if(D)Log.d(TAG, "StoriListActivity.onReadStoriItemsComplete: storiListItems is null");
        }
        else {
            if(D)Log.d(TAG, String.format("StoriListActivity.onReadStoriItemsComplete: found %d items", storiListItems.size()));
        }

        if (storiListItems != null) {
            for (int i = 0; i < storiListItems.size(); i++) {
                StoriListItem sli = storiListItems.get(i);
                if(D)Log.d(TAG, String.format("****** slideShareName=%s, title=%s, slideCount=%d, date=%s", sli.getSlideShareName(), sli.getTitle(), sli.getSlideCount(), sli.getModifiedDate()));
            }
        }

        m_adapter.setStoriListItems(storiListItems);
        m_adapter.notifyDataSetChanged();
        getListView().requestFocus();
    }

    public void downloadForPlay(StoriListItem sli) {
        if(D)Log.d(TAG, "StoriListActivity.downloadForPlay");

        Intent intent = new Intent(this, DownloadActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(PlaySlidesActivity.EXTRA_INTENTFROMSTORIAPP, true);
        String urlString = Utilities.buildShowWebPageUrlString(m_userUuid, sli.getSlideShareName());
        if(D)Log.d(TAG, String.format("StorListActivity.downloadForPlay: urlString=%s", urlString));
        Uri uri = Uri.parse(urlString);
        intent.setData(uri);
        startActivity(intent);
    }

    public void downloadForEdit(StoriListItem sli) {
        if(D)Log.d(TAG, "StoriListActivity.downloadForEdit");

        final StoriListItem sliFinal = sli;

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.storilistactivity_downloadedit_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.storilistactivity_downloadedit_message));
        adb.setPositiveButton(getString(R.string.ok_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                Intent intent = new Intent(StoriListActivity.this, DownloadActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(StoriListActivity.EXTRA_DOWNLOAD_FOR_EDIT, true);
                String urlString = Utilities.buildShowWebPageUrlString(m_userUuid, sliFinal.getSlideShareName());
                if(D)Log.d(TAG, String.format("StoriListActivity.downloadForEdit: urlString=%s", urlString));
                Uri uri = Uri.parse(urlString);
                intent.setData(uri);
                StoriListActivity.this.startActivityForResult(intent, EditPlayActivity.REQUEST_DOWNLOAD_FOR_EDIT);
            }
        });
        adb.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    public void deleteStoris(ArrayList<StoriListItem> storiListItems) {
        if(D)Log.d(TAG, "StoriListActivity.deleteStori");

        final ArrayList<StoriListItem> itemsFinal = storiListItems;

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(getString(R.string.storilistactivity_delete_title));
        adb.setCancelable(true);
        adb.setMessage(getString(R.string.storilistactivity_delete_message));
        adb.setPositiveButton(getString(R.string.storilistactivity_delete_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                deleteStorisAsync(itemsFinal);
            }
        });
        adb.setNegativeButton(getString(R.string.cancel_text), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog ad = adb.create();
        ad.show();
    }

    private class DeleteStoriItemsTask extends AsyncTask<Object, Void, ArrayList<StoriListItem>> {
        @Override
        public ArrayList<StoriListItem> doInBackground(Object... params) {
            if(D)Log.d(TAG, "StoriListActivity.DeleteStoriItemsTask.doInBackground");

            ArrayList<StoriListItem> items = (ArrayList<StoriListItem>)params[0];

            CloudStore cloudStore = new CloudStore(StoriListActivity.this, m_userUuid, null, Config.CLOUD_STORAGE_PROVIDER, null);

            return cloudStore.deleteStoriItemsAndReturnItems(items);
        }

        @Override
        public void onPostExecute(ArrayList<StoriListItem> returnedItems) {
            if(D)Log.d(TAG, "StoriListActivity.DeleteStoriItemsTask.onPostExecute");

            // Hide progress dialog.
            if (m_progressDialog != null) {
                m_progressDialog.dismiss();
                m_progressDialog = null;
            }

            if (returnedItems != null) {
                if (m_storiService != null) {
                    m_storiService.resetStoriItems(returnedItems);
                }

                m_adapter.setStoriListItems(returnedItems);
                m_adapter.notifyDataSetChanged();
            }
        }
    }

    private void deleteStorisAsync(ArrayList<StoriListItem> items) {
        if(D)Log.d(TAG, "StoriListActivity.deleteStorisAsync");

        // Launch progress dialog...
        m_progressDialog = new ProgressDialog(this);
        m_progressDialog.setTitle(getString(R.string.delete_dialog_title));
        m_progressDialog.setCancelable(false);
        m_progressDialog.setIndeterminate(true);
        m_progressDialog.show();

        boolean success = false;

        try {
            new DeleteStoriItemsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, items);
            success = true;
        }
        catch (Exception e) {
            if(D)Log.e(TAG, "StoriListActivity.deleteStorisAsync", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(D)Log.e(TAG, "StoriListActivity.deleteStorisAsync", e);
            e.printStackTrace();
        }

        if (!success) {
            m_progressDialog.dismiss();
            m_progressDialog = null;
        }
    }

    protected void initializeStoriService()
    {
        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService");

        Intent service = new Intent(this, StoriService.class);

        if(D)Log.d(TAG, "StoriListActivity.initializeStoriService - calling startService and bindService");
        startService(service);
        bindService(service, m_connection, Context.BIND_AUTO_CREATE);
    }

    protected void uninitializeStoriService()
    {
        if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService");

        if (m_storiService != null) {
            m_storiService.unregisterReadStoriItemsStateListener(this);
        }

        // Always call unbindService
        if (m_storiService != null && m_connection != null)
        {
            if(D)Log.d(TAG, "StoriListActivity.uninitializeStoriService - calling unbindService");
            unbindService(m_connection);
        }

        m_storiService = null;
    }

    public ServiceConnection m_connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceConnected");

            m_storiService = ((StoriService.StoriServiceBinder)service).getService();
            m_storiService.registerReadStoriItemsStateListener(StoriListActivity.this);

            ArrayList<StoriListItem> sli = m_storiService.getStoriListItems();
            if (sli == null || sli.size() <= 0) {
                if(D)Log.d(TAG, "StoriListActivity.onServiceConnected - no StoriListItems, so asking StoriService to update");
                m_storiService.readStoriItemsAsync(StoriListActivity.this, m_userUuid);
            }
            else {
                if(D)Log.d(TAG, "StoriListActivity.onServiceConnected - using StoriService StoriListItems cache");
                onReadStoriItemsComplete(sli);
            }
        }

        public void onServiceDisconnected(ComponentName className)
        {
            if(D)Log.d(TAG, "StoriListActivity.onServiceDisconnected");

            m_storiService = null;
        }
    };
}
