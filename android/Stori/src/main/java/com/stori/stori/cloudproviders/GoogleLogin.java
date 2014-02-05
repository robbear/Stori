package com.stori.stori.cloudproviders;

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
import com.stori.stori.EditPlayActivity;
import com.stori.stori.R;
import com.stori.stori.SSPreferences;
import com.stori.stori.Utilities;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

public class GoogleLogin extends AlertActivity {
    public static final String TAG = "GoogleLogin";
    public static int ACCOUNT_PICKER_RESULT = 1;
    public static int USER_AUTHORIZATION_RESULT = 2;

    public final static String EXTRA_FORCE_ACCOUNT_PICKER = "extra_force_account_picker";

    private final static String INSTANCE_STATE_ACCOUNT_PICKER = "instance_state_account_picker";
    private final static String INSTANCE_STATE_FORCE_ACCOUNT_PICKER = "instance_state_force_account_picker";

    private SharedPreferences m_prefs;
    private boolean m_fAccountPickerUp = false;
    private boolean m_fForceAccountPicker = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D)Log.d(TAG, String.format("GoogleLogin.onCreate: savedInstanceState %s null", savedInstanceState == null ? "is" : "is not"));

        setContentView(R.layout.activity_googlelogin);

        m_fForceAccountPicker = getIntent().getBooleanExtra(EXTRA_FORCE_ACCOUNT_PICKER, false);

        if (savedInstanceState != null) {
            m_fAccountPickerUp = savedInstanceState.getBoolean(INSTANCE_STATE_ACCOUNT_PICKER);
            m_fForceAccountPicker = savedInstanceState.getBoolean(INSTANCE_STATE_FORCE_ACCOUNT_PICKER);
        }

        m_prefs = getSharedPreferences(SSPreferences.PREFS(this), Context.MODE_PRIVATE);
        String userAccountEmail = AmazonSharedPreferencesWrapper.getUserEmail(m_prefs);
        if(D)Log.d(TAG, String.format("GoogleLogin.onCreate - m_userAccountEmail=%s, m_fForceAccountPicker=%b", userAccountEmail, m_fForceAccountPicker));

        if (m_fAccountPickerUp) {
            if(D)Log.d(TAG, "GoogleLogin.onCreate - account picker is already up, so bailing");
        }
        else {
            if (userAccountEmail == null || m_fForceAccountPicker) {
                if(D)Log.d(TAG, "GoogleLogin.onCreate - m_userAccountEmail is null or m_fForceAccountPicker is true, so calling AccountPicker");
                m_fAccountPickerUp = true;
                Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
                startActivityForResult(intent, ACCOUNT_PICKER_RESULT);
                return;
            }

            getAndUseAuthTokenInAsyncTask(userAccountEmail);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if(D)Log.d(TAG, "GoogleLogin.onSaveInstanceState");

        savedInstanceState.putBoolean(INSTANCE_STATE_ACCOUNT_PICKER, m_fAccountPickerUp);
        savedInstanceState.putBoolean(INSTANCE_STATE_FORCE_ACCOUNT_PICKER, m_fForceAccountPicker);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if(D)Log.d(TAG, String.format("GoogleLogin.onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode));

        String userAccountEmail = data == null ? null : data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

        if (requestCode == ACCOUNT_PICKER_RESULT && resultCode == RESULT_OK) {
            if(D)Log.d(TAG, "GoogleLogin.onActivityResult - handling RESULT_OK on ACCOUNT_PICKER_RESULT. Clearing data and prepping for EditActivity reset.");
            Utilities.clearAllData(this, m_prefs);

            if(D)Log.d(TAG, String.format("GoogleLogin.onActivityResult: userAccountEmail=%s", userAccountEmail));
            AmazonSharedPreferencesWrapper.storeUserEmail(m_prefs, userAccountEmail);

            getAndUseAuthTokenInAsyncTask(userAccountEmail);
        }
        else if (requestCode == USER_AUTHORIZATION_RESULT && resultCode == RESULT_OK) {
            if(D)Log.d(TAG, "GoogleLogin.onActivityResult - user authorized the application");

            getAndUseAuthTokenInAsyncTask(userAccountEmail);
        }
        else {
            if(D)Log.d(TAG, "GoogleLogin.onActivityResult - nothing selected, so finishing activity");
            finish();
        }
    }

    void getAndUseAuthTokenBlocking(String userAccountEmail) {
        if(D)Log.d(TAG, "GoogleLogin.getAndUseAuthTokenBlocking");

        try {
            int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
            if (result != ConnectionResult.SUCCESS) {
                // BUGBUG TODO: Handle error appropriately
                throw new Exception("Google Play is not available, make sure you are not running in an emulator",
                        new GooglePlayServicesNotAvailableException(result));
            }

            String scope = "audience:server:client_id:" + EditPlayActivity.s_amazonClientManager.getGoogleClientID();
            if(D)Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - scope=%s", scope));

            String token = GoogleAuthUtil.getToken(getApplicationContext(), userAccountEmail, scope);
            if(D)Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenBlocking - using token for login: %s", token));

            EditPlayActivity.s_amazonClientManager.login(new GoogleIDP(getApplicationContext(), token), this);
        }
        catch (UserRecoverableAuthException e) {
            if(D)Log.d(TAG, "GoogleLogin.getAndUseAuthTokenBlocking - app hasn't been authorized by user");
            startActivityForResult(e.getIntent(), USER_AUTHORIZATION_RESULT);
        }
        catch (Exception e) {
            if(E)Log.e(TAG, "GoogleLogin.getAndUseAuthTokenBlocking", e);
            e.printStackTrace();
            alertUser(e);
        }
    }

    void getAndUseAuthTokenInAsyncTask(final String userAccountEmail) {
        if(D)Log.d(TAG, String.format("GoogleLogin.getAndUseAuthTokenInAsyncTask: userAccountEmail=%s", userAccountEmail));

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getAndUseAuthTokenBlocking(userAccountEmail);
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
            return EditPlayActivity.s_amazonClientManager.getGoogleRoleARN();
        }

        @Override
        public void logout() {
            GoogleAuthUtil.invalidateToken(m_googleContext, m_googleToken);
        }
    }
}
