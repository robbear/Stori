package com.hyperfine.slideshare.cloudproviders;

import java.util.HashMap;

public interface ICloudProvider {
    public void initializeProvider(String containerName) throws Exception;
    public void deleteVirtualDirectory(String directoryName) throws Exception;
    public void uploadFile(String folder, String fileName, String contentType) throws Exception;
}
