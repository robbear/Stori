package com.hyperfine.slideshare.cloudproviders;

import android.util.Log;

import com.microsoft.windowsazure.services.core.storage.*;
import com.microsoft.windowsazure.services.blob.client.*;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class WindowsAzureProvider {

    public final static String TAG = "WindowsAzureProvider";

    public WindowsAzureProvider() {
        if(D)Log.d(TAG, "WindowsAzureProvider.WindowsAzureProvider");
    }

    public void initializeProvider() {
        if(D)Log.d(TAG, "WindowsAzureProvider.initializeProvider");

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse("storageConnectionString");

            // Create the blob client
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.initializeProvider", e);
            e.printStackTrace();
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.initializeProvider", e);
            e.printStackTrace();
        }
    }
}
