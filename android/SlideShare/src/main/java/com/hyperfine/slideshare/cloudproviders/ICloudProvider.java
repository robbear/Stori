package com.hyperfine.slideshare.cloudproviders;

public interface ICloudProvider {
    public void initializeProvider(String containerName) throws Exception;
    public void uploadFile(String folder, String fileName) throws Exception;
}
