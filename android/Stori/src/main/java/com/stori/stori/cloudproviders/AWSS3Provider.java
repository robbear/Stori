package com.stori.stori.cloudproviders;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.stori.stori.Config;
import com.stori.stori.EditPlayActivity;
import com.stori.stori.Utilities;

import java.io.File;
import java.io.FileInputStream;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class AWSS3Provider implements ICloudProvider {
    public final static String TAG = "AWSS3Provider";
    public final static String BUCKET_NAME = Config.AWS_BUCKET_NAME;

    private AmazonS3Client m_s3Client;
    private Context m_context;
    private String m_userUuid;

    public AWSS3Provider(Context context) {
        if(D)Log.d(TAG, "AWSS3Provider.AWSS3Provider");

        m_context = context;
    }

    //
    // Note: This is a blocking call and should not be made on the UI thread
    //
    public void initializeProvider(String userUuid, SharedPreferences prefs) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.initializeProvider: userUuid=%s", userUuid));

        m_userUuid = userUuid;

        GoogleLoginHelper glh = new GoogleLoginHelper(m_context, prefs);
        glh.getAndUseAuthToken();

        m_s3Client = EditPlayActivity.s_amazonClientManager.s3();
        m_s3Client.setRegion(Region.getRegion(Regions.US_WEST_2));
    }

    public void deleteVirtualDirectory(String directoryName) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.deleteVirtualDirectory: directoryName=%s", directoryName));

        String prefix = m_userUuid + "/" + directoryName;
        ObjectListing objects = m_s3Client.listObjects(BUCKET_NAME, prefix);
        for (S3ObjectSummary summary : objects.getObjectSummaries()) {
            m_s3Client.deleteObject(BUCKET_NAME, summary.getKey());
        }
    }

    /* NEVER
    // Note: This method does not seem to work with the WIF permissions scheme. It's unfortunate, because
    // I'd prefer to set object permissions for public access. But using the PutObjectRequest object seems
    // to cause the upload to fail with AccessDenied errors.  The only way that seems to work
    // is by providing an InputStream and calling putObject with that.
    //
    // Instead, we have to add a bucket policy allowing public read access. This is done in the
    // AWS S3 console.
    //
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
    */

    public void uploadFile(String folder, String fileName, String contentType) throws Exception {
        if(D)Log.d(TAG, String.format("AWSS3Provider.uploadFile: folder=%s, fileName=%s, contentType=%s", folder, fileName, contentType));

        // Note: Public access policy is applied as a bucket policy to hfstori in the AWS Console

        String relPath = m_userUuid + "/" + folder + "/" + fileName;
        if(D)Log.d(TAG, String.format("AWSS3Provider.uploadFile: relPath=%s", relPath));

        File sourceFile = new File(Utilities.getAbsoluteFilePath(m_context, folder, fileName));
        if (!sourceFile.exists()) {
            if(E)Log.e(TAG, "AWSS3Provider.uploadFile - file doesn't exist");
            return;
        }

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(sourceFile);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setHeader("Access-Control-Allow-Origin", "*");
            metadata.setCacheControl("If-None-Match");
            metadata.setContentLength(sourceFile.length());
            m_s3Client.putObject(BUCKET_NAME, relPath, stream, metadata);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "AWSS3Provider.uploadFile", e);
            e.printStackTrace();

            throw e;
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
