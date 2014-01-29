package com.stori.stori;

import android.content.Context;

public class SSPreferences {
    public static String PREFS(Context context) { return context.getString(R.string.PREFS); }
    public static String PREFS_EDITPROJECTNAME(Context context) { return context.getString(R.string.PREFS_EDITPROJECTNAME); }
    public static String PREFS_PLAYSLIDESNAME(Context context) { return context.getString(R.string.PREFS_PLAYSLIDESNAME); }
    public static String PREFS_PLAYSLIDESAUTOAUDIO(Context context) { return context.getString(R.string.PREFS_PLAYSLIDESAUTOAUDIO); }

    public static String DEFAULT_EDITPROJECTNAME(Context context) {
        String s = context.getString(R.string.DEFAULT_EDITPROJECTNAME);
        if (s == null || s.isEmpty()) {
            return null;
        }
        else {
            return s;
        }
    }

    public static String DEFAULT_PLAYSLIDESNAME(Context context) {
        String s = context.getString(R.string.DEFAULT_PLAYSLIDESNAME);
        if (s == null || s.isEmpty()) {
            return null;
        }
        else {
            return s;
        }
    }

    public static boolean DEFAULT_PLAYSLIDESAUTOAUDIO(Context context) {
        String s = context.getString(R.string.DEFAULT_PLAYSLIDESAUTOAUDIO);
        if (s == null || s.equalsIgnoreCase("false")) {
            return false;
        }
        else {
            return true;
        }
    }
}
