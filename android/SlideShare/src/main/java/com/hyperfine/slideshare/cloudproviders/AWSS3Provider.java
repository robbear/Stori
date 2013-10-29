package com.hyperfine.slideshare.cloudproviders;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ResponseHeaderOverrides;

public class AWSS3Provider {
    private AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials("key_id", "secret_key"));
}
