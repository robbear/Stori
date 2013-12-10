package com.hyperfine.neodori.cloudproviders;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.hyperfine.neodori.MainActivity;
import com.hyperfine.neodori.SSPreferences;

import static com.hyperfine.neodori.Config.D;
import static com.hyperfine.neodori.Config.E;

public class GoogleLogin extends AlertActivity {
    public static final String TAG = "GoogleLogin";
    public static int ACCOUNT_PICKER_RESULT = 1;
    public static int USER_AUTHORIZATION_RESULT = 2;

    private SharedPreferences m_prefs;
    private String m_userAccountEmail;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D)Log.d(TAG, "GoogleLogin.onCreate");

        m_prefs = getSharedPreferences(SSPreferences.PREFS, Context.MODE_PRIVATE);
        m_userAccountEmail = m_prefs.getString(SSPreferences.PREFS_USERACCOUNT, null);

        if (m_userAccountEmail == null) {
            if(D)Log.d(TAG, "GoogleLogin.onCreate - calling AccountPicker");
            Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
            startActivityForResult(intent, ACCOUNT_PICKER_RESULT);
            return;
        }

        getAndUseAuthTokenInAsyncTask();
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("GoogleLogin.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        if (requestCode == ACCOUNT_PICKER_RESULT && resultCode == RESULT_OK) {
            m_userAccountEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if(D)Log.d(TAG, String.format("GoogleLogin.onActivityResult: m_userAccountEmail=%s", m_userAccountEmail));

            getAndUseAuthTokenInAsyncTask();
        }
        else if (requestCode == USER_AUTHORIZATION_RESULT && resultCode == RESULT_OK) {
            if(D)Log.d(TAG, "GoogleLogin.onActivityResult - user authorized the application");

            getAndUseAuthTokenInAsyncTask();
        }
        else {
            if(D)Log.d(TAG, "GoogleLogin.onActivityResult - nothing selected, so finishing activity");
            finish();
        }
    }

    void getAndUseAuthTokenBlocking() {
        if(D)Log.d(TAG, "GoogleLogin.getAndUseAuthTokenBlocking");

        try {
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (result != ConnectionResult.SUCCESS) {
                // BUGBUG TODO: Handle error appropriately
                throw new Exception("Google Play is not available, make sure you are not running in an emulator",
                        new GooglePlayServicesNotAvailableException(result));
            }

            String token = GoogleAuthUtil.getToken(getApplicationContext(), m_userAccountEmail,
                    "oauth2:audience:server:client_id:" + MainActivity.s_amazonClientManager.getGoogleClientID() + ":api_scope:" + Scopes.PLUS_LOGIN);

            if(D)Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - using token for login: %s", token));

            MainActivity.s_amazonClientManager.login(new GoogleIDP(getApplicationContext(), token), this);
        }
        catch (UserRecoverableAuthException e) {
            if(D)Log.d(TAG, "GoogleLogin.getAndUserAuthTokenBlocking - app hasn't been authorized by user");
            startActivityForResult(e.getIntent(), USER_AUTHORIZATION_RESULT);
        }
        catch (Exception e) {
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
