package com.stori.stori;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.File;

import static com.stori.stori.Config.D;
import static com.stori.stori.Config.E;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class VolleyBinaryRequest extends Request<byte[]> {
    public static final String TAG = "VolleyBinaryRequest";

    /** Socket timeout in milliseconds for binary requests */
    private static final int BINARY_TIMEOUT_MS = Config.downloadConnectionTimeoutMilliseconds;

    /** Default number of retries for binary requests */
    private static final int BINARY_MAX_RETRIES = 2;

    /** Default backoff multiplier for binary requests */
    private static final float BINARY_BACKOFF_MULT = 2f;

    private final Response.Listener<byte[]> mListener;

    private final File mOutputDirectory;

    /** Decoding lock so that we don't decode more than one binary at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    /**
     * Creates a new binary request
     *
     * @param url URL of the image
     * @param outputDirectory Directory File for streamed binary files
     * @param listener Listener to receive the decoded bitmap
     * @param errorListener Error listener, or null to ignore errors
     */
    public VolleyBinaryRequest(String url, File outputDirectory, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        if(D)Log.d(TAG, "VolleyBinaryRequest constructor");

        setRetryPolicy(
                new DefaultRetryPolicy(BINARY_TIMEOUT_MS, BINARY_MAX_RETRIES, BINARY_BACKOFF_MULT));
        mListener = listener;
        mOutputDirectory = outputDirectory;
    }

    public File getOutputDirectory() {
        return mOutputDirectory;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        if(D)Log.d(TAG, "VolleyBinaryRequest.parseNetworkResponse");

        // Serialize all decode on a global lock to reduce concurrent heap usage.
        // NO!!!
        //synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                if(E)Log.e(TAG, "VolleyBinaryRequest.parseNetworkResponse", e);
                e.printStackTrace();

                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        //}
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Response<byte[]> doParse(NetworkResponse response) {
        if(D)Log.d(TAG, "VolleyBinaryRequest.doParse");

        byte[] data = response.data;
        if (data == null) {
            if(D)Log.d(TAG, "VolleyBinaryRequest.doParse - returned data is null");
            return Response.error(new ParseError(response));
        }
        else {
            return Response.success(data, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override
    protected void deliverResponse(byte[] response) {
        if(D)Log.d(TAG, "VolleyBinaryRequest.deliverResponse");

        mListener.onResponse(response);
    }
}
