package com.stori.stori.cloudproviders;

import android.content.SharedPreferences;

import com.stori.stori.StoriListItem;

import java.util.ArrayList;
import java.util.HashMap;

public interface ICloudProvider {
    public void initializeProvider(String containerName, SharedPreferences prefs) throws Exception;
    public void deleteVirtualDirectory(String directoryName) throws Exception;
    public void uploadFile(String folder, String fileName, String contentType) throws Exception;
    public void uploadDirectoryEntry(String folder, String title, int count) throws Exception;
    public ArrayList<StoriListItem> getStoriItems() throws Exception;
}
