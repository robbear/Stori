package com.hyperfine.neodori.cloudproviders;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.hyperfine.neodori.MainActivity;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class GoogleLoginHelper {
    public static final String TAG = "GoogleLoginHelper";

    private Context m_context;
    private SharedPreferences m_prefs;
    private String m_userAccountEmail;

    public GoogleLoginHelper(Context context, SharedPreferences prefs) {
        if(D)Log.d(TAG, "GoogleLoginHelper constructor");

        m_context = context;
        m_prefs = prefs;
        m_userAccountEmail = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);
    }

    public boolean getAndUseAuthToken() {
        if(D)Log.d(TAG, "GoogleLoginHelper.getAndUseAuthToken");

        boolean retVal = false;

        try {
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(m_context);
            if (result != ConnectionResult.SUCCESS) {
                // BUGBUG TODO: Handle error appropriately
                throw new Exception("Google Play is not available, make sure you are not running in an emulator",
                        new GooglePlayServicesNotAvailableException(result));
            }

            String scope = "audience:server:client_id:" + MainActivity.s_amazonClientManager.getGoogleClientID();
            if(D)Log.d(TAG, String.format("GoogleLoginHelper.getAndUseAuthToken - scope=%s", scope));

            String token = GoogleAuthUtil.getToken(m_context, m_userAccountEmail, scope);
            if(D)Log.d(TAG, String.format("GoogleLoginHelper.getAndUseAuthToken - using token for login: %s", token));

            MainActivity.s_amazonClientManager.loginSynchronous(new GoogleIDP(m_context, token));
            retVal = true;
        }
        catch (UserRecoverableAuthException e) {
            if(D)Log.d(TAG, "GoogleLoginHelper.getAndUseAuthToken - app hasn't been authorized by user");
            if(E)Log.e(TAG, "GoogleLoginHelper.getAndUseAuthToken", e);
            e.printStackTrace();
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "GoogleLoginHelper.getAndUseAuthToken", e);
            e.printStackTrace();
        }

        return retVal;
    }

    protected class GoogleIDP implements WIFIdentityProvider {
        private Context m_googleContext;
        private String m_googleToken;

        public GoogleIDP(Context context, String token) {
            m_googleContext = context;
            m_googleToken = token;
        }

        @Override
        public String getToken() {
            return m_googleToken;
        }

        @Override
        public String getProviderID() {
            // google uses a null provider ID
            return null;
        }

        @Override
        public String getRoleARN() {
            return MainActivity.s_amazonClientManager.getGoogleRoleARN();
        }

        @Override
        public void logout() {
            GoogleAuthUtil.invalidateToken(m_googleContext, m_googleToken);
        }
    }
}
