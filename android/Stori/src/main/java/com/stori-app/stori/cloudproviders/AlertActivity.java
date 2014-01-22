package com.stori-app.stori.cloudproviders;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import static com.stori-app.stori.Config.D;
import static com.stori-app.stori.Config.E;

public class AlertActivity extends Activity {
    public final static String TAG = "AlertActivity";
    
    private Throwable error;
    private Handler mHandler;

    //
    // BUGBUG TODO: Use resource strings
    //

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
    }
    protected Runnable displayError = new Runnable(){
        public void run(){
            AlertDialog.Builder confirm = new AlertDialog.Builder( AlertActivity.this );
            confirm.setTitle( "An error occured.");
            confirm.setMessage( error.getMessage() + ". Please review the README and check LogCat for details.");
            confirm.setNegativeButton( "OK", new DialogInterface.OnClickListener() {
                public void onClick( DialogInterface dialog, int which ) {
                    AlertActivity.this.finish();
                }
            } );
            confirm.show();   
        }
    };

    protected Runnable refreshFailed = new Runnable(){
        public void run(){
            AlertDialog.Builder confirm = new AlertDialog.Builder( AlertActivity.this );
            confirm.setTitle( "Failed to refresh credentials");
            confirm.setMessage( "Try again");
            confirm.setNegativeButton( "OK", new DialogInterface.OnClickListener() {
                public void onClick( DialogInterface dialog, int which ) {
                    AlertActivity.this.finish();
                }
            } );
            confirm.show();   
        }
    };

    public void alertUser(Throwable aThrowable){
        if(E)Log.e(TAG, aThrowable.getMessage(), aThrowable);
        error = aThrowable;
        mHandler.post(displayError);
    }

    public void putRefreshError(){
        mHandler.post(refreshFailed);
    }
}
