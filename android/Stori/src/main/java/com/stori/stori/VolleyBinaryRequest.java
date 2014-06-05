package com.stori.stori;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * A canned request for getting an image at a given URL and calling
 * back with a decoded Bitmap.
 */
public class VolleyBinaryRequest extends Request<byte[]> {
    /** Socket timeout in milliseconds for binary requests */
    private static final int BINARY_TIMEOUT_MS = 1000;

    /** Default number of retries for binary requests */
    private static final int BINARY_MAX_RETRIES = 2;

    /** Default backoff multiplier for binary requests */
    private static final float BINARY_BACKOFF_MULT = 2f;

    private final Response.Listener<byte[]> mListener;

    /** Decoding lock so that we don't decode more than one binary at a time (to avoid OOM's) */
    private static final Object sDecodeLock = new Object();

    /**
     * Creates a new binary request
     *
     * @param url URL of the image
     * @param listener Listener to receive the decoded bitmap
     * @param errorListener Error listener, or null to ignore errors
     */
    public VolleyBinaryRequest(String url, Response.Listener<byte[]> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(
                new DefaultRetryPolicy(BINARY_TIMEOUT_MS, BINARY_MAX_RETRIES, BINARY_BACKOFF_MULT));
        mListener = listener;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap usage.
        synchronized (sDecodeLock) {
            try {
                return doParse(response);
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Response<byte[]> doParse(NetworkResponse response) {
        byte[] data = response.data;
        if (data == null) {
            return Response.error(new ParseError(response));
        }
        else {
            return Response.success(data, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }
}
