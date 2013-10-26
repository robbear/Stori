package com.hyperfine.slideshare;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import static com.hyperfine.slideshare.Config.D;
import static com.hyperfine.slideshare.Config.E;

//
// SlideJSON example (see SlideShareJSON):
//
// { image: "http://foo.com/1.jpg", audio: "http://foo.com/1.3gp" }
//
public class SlideJSON extends JSONObject {
    public final static String TAG = "SlideJSON";

    public SlideJSON() throws JSONException {
        if(D)Log.d(TAG, "SlideJSON.SlideJSON");

        put(SlideShareJSON.KEY_IMAGE, null);
        put(SlideShareJSON.KEY_AUDIO, null);
    }

    public SlideJSON(String json) throws JSONException {
        super(json);

        if(D)Log.d(TAG, String.format("SlideJSON.SlideJSON constructed from string: %s", json));
    }

    public SlideJSON(JSONObject slide) throws JSONException {
        super();

        if(D)Log.d(TAG, "SlideJSON.SlidJSON constructed from JSONObject");

        put(SlideShareJSON.KEY_IMAGE, slide.getString(SlideShareJSON.KEY_IMAGE));
        put(SlideShareJSON.KEY_AUDIO, slide.getString(SlideShareJSON.KEY_AUDIO));
    }

    public String getImageUrlString() throws JSONException {
        String s = getString(SlideShareJSON.KEY_IMAGE);
        if(D)Log.d(TAG, String.format("SlideJSON.getImageUrlString returns %s", s));
        return s;
    }

    public String getImageFilename() throws JSONException, MalformedURLException {
        String s = getImageUrlString();
        Uri uri = Uri.parse(s);
        String fileName = uri.getLastPathSegment();
        if(D)Log.d(TAG, String.format("SlideJSON.getImageFilename returns %s", fileName));
        return fileName;
    }

    public String getAudioUrlString() throws JSONException {
        String s = getString(SlideShareJSON.KEY_AUDIO);
        if(D)Log.d(TAG, String.format("SlideJSON.getAudioUrlString returns %s", s));
        return s;
    }

    public String getAudioFilename() throws JSONException, MalformedURLException {
        String s = getAudioUrlString();
        Uri uri = Uri.parse(s);
        String fileName = uri.getLastPathSegment();
        if(D)Log.d(TAG, String.format("SlideJSON.getAudioFilename returns %s", fileName));
        return fileName;
    }
}
