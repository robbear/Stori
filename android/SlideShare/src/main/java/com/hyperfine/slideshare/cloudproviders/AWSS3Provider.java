package com.hyperfine.slideshare.cloudproviders;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.hyperfine.slideshare.Utilities;

import java.io.File;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class AWSS3Provider implements ICloudProvider {
    public final static String TAG = "AWSS3Provider";
    public final static String BUCKET_NAME = "hfslideshare";

    private AmazonS3Client m_s3Client;
    private Context m_context;
    private String m_userUuid;

    public AWSS3Provider(Context context) {
        if(D)Log.d(TAG, "AWSS3Provider.AWSS3Provider");

        m_context = context;
    }

    public void initializeProvider(String userUuid) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.initializeProvider: userUuid=%s", userUuid));

        m_s3Client = new AmazonS3Client(new BasicAWSCredentials(AWSS3ConnectionStrings.ACCESS_KEY_ID, AWSS3ConnectionStrings.SECRET_KEY));
        m_s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));

        m_userUuid = userUuid;
    }

    public void deleteVirtualDirectory(String directoryName) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.deleteVirtualDirectory: directoryName=%s", directoryName));

        String prefix = m_userUuid + "/" + directoryName;
        ObjectListing objects = m_s3Client.listObjects(BUCKET_NAME, prefix);
        for (S3ObjectSummary summary : objects.getObjectSummaries()) {
            m_s3Client.deleteObject(BUCKET_NAME, summary.getKey());
        }
    }

    public void uploadFile(String folder, String fileName, String contentType) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.uploadFile: folder=%s, fileName=%s, contentType=%s", folder, fileName, contentType));

        String relPath = m_userUuid + "/" + folder + "/" + fileName;
        if(D)Log.d(TAG, String.format("AWSS3Provider.uploadFile: relPath=%s", relPath));

        File sourceFile = new File(Utilities.getAbsoluteFilePath(m_context, folder, fileName));
        if (!sourceFile.exists()) {
            if(E)Log.e(TAG, "AWSS3Provider.uploadFile - file doesn't exist");
            return;
        }

        PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, relPath, sourceFile);

        ObjectMetadata om = new ObjectMetadata();
        om.setContentType(contentType);
        om.setHeader("Access-Control-Allow-Origin", "*");
        om.setCacheControl("If-None_Match");

        por.setMetadata(om);
        por.setCannedAcl(CannedAccessControlList.PublicRead);

        m_s3Client.putObject(por);
    }
}
