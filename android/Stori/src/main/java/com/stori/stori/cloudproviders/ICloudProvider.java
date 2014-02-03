package com.stori.stori.cloudproviders;

import android.content.SharedPreferences;

import com.stori.stori.StoriListItem;

import java.util.ArrayList;

public interface ICloudProvider {
    public void initializeProvider(String containerName, SharedPreferences prefs) throws Exception;
    public boolean deleteVirtualDirectory(String directoryName) throws Exception;
    public void uploadFile(String folder, String fileName, String contentType) throws Exception;
    public void uploadDirectoryEntry(String folder, String title, int count) throws Exception;
    public ArrayList<StoriListItem> getStoriItems() throws Exception;
}
