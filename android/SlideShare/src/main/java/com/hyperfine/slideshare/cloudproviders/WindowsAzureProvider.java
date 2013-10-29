package com.hyperfine.slideshare.cloudproviders;

import android.content.Context;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

//
// NOTE:
// The current work done in the WindowsAzureProvider depends on the
// Windows Azure SDK for Java Developers which is not compatible with
// Android's DVM. If we were to develop an Azure provider, we'd need
// either to:
//
// 1. Use the Azure Blob Services REST API, or
// 2. Wait for Azure Mobile Services for Android to support blob services APIs.
//

public class WindowsAzureProvider implements ICloudProvider {

    public final static String TAG = "WindowsAzureProvider";

    private Context m_context;
    /* NEVER
    private CloudBlobClient m_cloudBlobClient;
    private CloudBlobContainer m_cloudBlobContainer;
    */

    public WindowsAzureProvider(Context context) {
        if(D)Log.d(TAG, "WindowsAzureProvider.WindowsAzureProvider");

        m_context = context;
    }

    // Note: containerName is the userUuid string
    public void initializeProvider(String containerName) throws Exception {
        if(D)Log.d(TAG, String.format("WindowsAzureProvider.initializeProvider: containerName=%s", containerName));

        /* NEVER
        Exception ex = null;

        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(WindowsAzureConnectionStrings.storageConnectionString);

            // Create the blob client
            m_cloudBlobClient = storageAccount.createCloudBlobClient();
            createOrGetContainer(containerName);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.initializeProvider", e);
            e.printStackTrace();

            ex = e;
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.initializeProvider", e);
            e.printStackTrace();

            ex = new Exception(e.getMessage());
        }

        if (ex != null) {
            throw ex;
        }
        */
    }

    public void deleteVirtualDirectory(String directoryName) throws Exception {
        if(D)Log.d(TAG, String.format("WindowsAzureProvider.deleteVirtualDirectory: directoryName=%s", directoryName));

        /* NEVER
        if (m_cloudBlobContainer == null) {
            throw new Exception("CloudBloblContainer not initialized");
        }

        CloudBlobDirectory cloudDir = m_cloudBlobContainer.getDirectoryReference(directoryName);
        for (ListBlobItem item : cloudDir.listBlobs()) {
            String uriPath = item.getUri().toString();
            if(D)Log.d(TAG, String.format("WindowsAzureProvider.deleteVirtualDirectory - deleting %s", uriPath));
            CloudBlockBlob blob = m_cloudBlobContainer.getBlockBlobReference(uriPath);
            blob.delete();
        }
        */
    }

    public void uploadFile(String folder, String fileName, HashMap<String, String> metaData) throws Exception {
        if(D)Log.d(TAG, String.format("WindowsAzureProvider.uploadFile: folder=%s, fileName=%s"));

        Exception ex = null;

        try {
            uploadBlob(folder, fileName, metaData);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.uploadFile", e);
            e.printStackTrace();

            ex = e;
        }
        catch (OutOfMemoryError e) {
            if(E)Log.e(TAG, "WindowsAzureProvider.uploadFile", e);
            e.printStackTrace();

            ex = new Exception(e.getMessage());
        }

        if (ex != null) {
            throw ex;
        }
    }

    private void createOrGetContainer(String containerName) throws URISyntaxException, IOException {
        if(D)Log.d(TAG, String.format("WindowsAzureProvider.createOrGetContainer(%s)", containerName));

        /* NEVER
        m_cloudBlobContainer = m_cloudBlobClient.getContainerReference(containerName);
        boolean containerExists = m_cloudBlobContainer.exists();
        m_cloudBlobContainer.createIfNotExist();

        if (!containerExists) {
            setContainerPermissions();
        }
        */
    }

    private void setContainerPermissions() throws IOException {
        if(D)Log.d(TAG, String.format("WindowsAzureProvider.setContainerPermissions"));

        /* NEVER
        if (m_cloudBlobContainer == null) {
            throw new IOException("CloudBlobContainer not initialized");
        }

        BlobContainerPermissions perms = new BlobContainerPermissions();
        perms.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
        m_cloudBlobContainer.uploadPermissions(perms);
        */
    }

    private void uploadBlob(String folder, String fileName, HashMap<String, String> metaData)
            throws URISyntaxException, FileNotFoundException, IOException {

        if(D)Log.d(TAG, String.format("WindowsAzureProvider.uploadBlob: folder=%s, fileName=%s", folder, fileName));

        /* NEVER
        if (m_cloudBlobContainer == null) {
            throw new IOException("CloudBlobContainer not initialized");
        }

        CloudBlockBlob blob = m_cloudBlobContainer.getBlockBlobReference(folder + "/" + fileName);
        File sourceFile = new File(Utilities.getAbsoluteFilePath(m_context, folder, fileName));
        blob.upload(new FileInputStream(sourceFile), sourceFile.length());
        blob.setMetadata(metaData);
        */
    }

    private URI[] listBlobItems() throws IOException {
        if(D)Log.d(TAG, "WindowsAzureProvider.listBlobItems");

        ArrayList<URI> arrayList = new ArrayList<URI>();

        /* NEVER
        if (m_cloudBlobContainer == null) {
            throw new IOException("CloudBlobContainer not initialized");
        }

        for (ListBlobItem blobItem : m_cloudBlobContainer.listBlobs()) {
            arrayList.add(blobItem.getUri());
        }
        */

        return arrayList.toArray(new URI[arrayList.size()]);
    }
}
