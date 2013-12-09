package com.hyperfine.slideshare.cloudproviders;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.hyperfine.slideshare.MainActivity;
import com.hyperfine.slideshare.SSPreferences;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

public class GoogleLogin extends AlertActivity {
    public static final String TAG = "GoogleLogin";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D)Log.d(TAG, "GoogleLogin.onCreate");

        getAndUseAuthTokenInAsyncTask();
    }

    void getAndUseAuthTokenBlocking() {
        if(D)Log.d(TAG, "GoogleLogin.getAndUseAuthTokenBlocking");

        try {
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (result != ConnectionResult.SUCCESS) {
                throw new Exception("Google Play is not available, make sure you are not running in an emulator",
                        new GooglePlayServicesNotAvailableException(result));
            }

            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - found %d accounts", accounts.length));
            Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - using %s", accounts[0].name));

            String token = GoogleAuthUtil.getToken(getApplicationContext(), accounts[0].name,
                    "oauth2:audience:server:client_id:" + MainActivity.s_amazonClientManager.getGoogleClientID() + ":api_scope:" + Scopes.PLUS_LOGIN);

            if(D)Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - using token for login: %s", token));

            MainActivity.s_amazonClientManager.login(new GoogleIDP(getApplicationContext(), token), this);
        } catch (Exception e) {
            alertUser(e);
        }
    }

    void getAndUseAuthTokenInAsyncTask() {
        if(D)Log.d(TAG, "GoogleLogin.getAndUseAuthTokenInAsyncTask");

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getAndUseAuthTokenBlocking();
                return null;
            }
        };
        task.execute();
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
