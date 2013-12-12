
package com.hyperfine.neodori.cloudproviders;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.auth.WebIdentityFederationSessionCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.hyperfine.neodori.Config;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

/**
 * This class is used to get clients to the various AWS services.  Before accessing a client 
 * the credentials should be checked to ensure validity.
 */
public class AmazonClientManager {
    public final static String TAG = "AmazonClientManager";

    private AmazonS3Client m_s3Client = null;
    private WebIdentityFederationSessionCredentialsProvider m_wif = null;
    private WIFIdentityProvider m_idp = null;
    private SharedPreferences m_sharedPreferences = null;
    private String m_bucketName = null;
    private String m_fbRoleARN = null;
    private String m_googleRoleARN = null;
    private String m_amazonRoleARN = null;
    private String m_googleClientID = null;

    public AmazonClientManager(SharedPreferences settings) {
        if(D)Log.d(TAG, "AmazonClientManager constructor");

        m_sharedPreferences = settings;
        m_bucketName = Config.AWS_BUCKET_NAME;
        m_fbRoleARN = Config.FB_ROLE_ARN;;
        m_googleRoleARN = Config.GOOGLE_ROLE_ARN;
        m_amazonRoleARN = Config.AMAZON_ROLE_ARN;
        m_googleClientID = Config.GOOGLE_CLIENT_ID;
    }

    public AmazonS3Client s3() {
        return m_s3Client;
    }

    public boolean hasCredentials() {
        boolean retVal = !(m_fbRoleARN.equals("ROLE_ARN") && m_googleRoleARN.equals("ROLE_ARN") && m_amazonRoleARN.equals("ROLE_ARN"));
        if(D)Log.d(TAG, String.format("AmazonClientManager.hasCredentials returning %b", retVal));

        return retVal;
    }

    public boolean isLoggedIn() {
        boolean retVal = (m_s3Client != null);
        if(D)Log.d(TAG, String.format("AmazonClientManager.isLoggedIn returning %b", retVal));

        return retVal;
    }

    public void login(WIFIdentityProvider wifIDP, final AlertActivity activity) {
        if(D)Log.d(TAG, "AmazonClientManager.login");

        m_idp = wifIDP;
        m_wif = new WebIdentityFederationSessionCredentialsProvider(m_idp.getToken(), m_idp.getProviderID(), m_idp.getRoleARN());

        if(D)Log.d(TAG, String.format("AmazonClientManger.login: token=%s, providerID=%s, roleARN=%s", m_idp.getToken(), m_idp.getProviderID(), m_idp.getRoleARN()));

        //call refresh to login
        new AsyncTask<Void, Void, Throwable>() {
            @Override
            protected Throwable doInBackground(Void... arg0) {
                try {
                    if(D)Log.d(TAG, "AmazonClientManager.login.doInBackground - calling WIF.refresh");
                    m_wif.refresh();
                }
                catch (Throwable t) {
                    return t;
                }

                return null;
            }

            @Override
            protected void onPostExecute(Throwable t) {
                if(D)Log.d(TAG, "AmazonClientManager.login.onPostExecute");

                if (t != null) {
                    if(E)Log.e(TAG, "AmazonClientManager.login - Unable to login.", t);
                    activity.setResult(Activity.RESULT_CANCELED);
                    activity.alertUser(t);
                }
                else {
                    m_s3Client = new AmazonS3Client(m_wif);
                    AmazonSharedPreferencesWrapper.storeUsername(m_sharedPreferences, m_wif.getSubjectFromWIF());
                    if(D)Log.d(TAG, String.format("AmazonClientManager.login - Logged in with user id %s", m_wif.getSubjectFromWIF()));
                    activity.setResult(Activity.RESULT_OK);
                    activity.finish();
                }
            }
        }.execute();
    }

    public String getBucketName() {
        if(D)Log.d(TAG, String.format("AmazonClientManager.getBucketName: %s", m_bucketName));

        return m_bucketName;
    }

    public String getUsername() {
        String userName = AmazonSharedPreferencesWrapper.getUsername(m_sharedPreferences);
        if(D)Log.d(TAG, String.format("AmazonClientManager.getUsername: %s", userName));
        return userName;
    }

    public String getAmazonRoleARN() {
        if(D)Log.d(TAG, String.format("AmazonClientManager.getAmazonRoleARN: %s", m_amazonRoleARN));

        return m_amazonRoleARN;
    }

    public String getGoogleRoleARN() {
        if(D)Log.d(TAG, String.format("AmazonClientManager.getGoogleRoleARN: %s", m_googleRoleARN));

        return m_googleRoleARN;
    }

    public String getFacebookRoleARN() {
        if(D)Log.d(TAG, String.format("AmazonClientManager.getFacebookRoleARN: %s", m_fbRoleARN));

        return m_fbRoleARN;
    }

    public String getGoogleClientID() {
        if(D)Log.d(TAG, String.format("AmazonClientManager.getGoogleClientID: %s", m_googleClientID));

        return m_googleClientID;
    }

    public void clearCredentials() {
        if(D)Log.d(TAG, "AmazonClientManager.clearCredentials");

        m_s3Client = null;
        if (m_idp != null) {
            m_idp.logout();
            m_idp = null;
        }
    }

    public void wipe() {
        if(D)Log.d(TAG, "AmazonClientManager.wipe");

        AmazonSharedPreferencesWrapper.wipe(m_sharedPreferences );
    }
}
