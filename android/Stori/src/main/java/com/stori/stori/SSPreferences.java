package com.stori.stori;

import android.content.Context;

public class SSPreferences {
    public static String PREFS(Context context) { return context.getString(R.string.PREFS); }
    public static String PREFS_EDITPROJECTNAME(Context context) { return context.getString(R.string.PREFS_EDITPROJECTNAME); }
    public static String PREFS_PLAYSLIDESNAME(Context context) { return context.getString(R.string.PREFS_PLAYSLIDESNAME); }
    public static String PREFS_PLAYSLIDESAUTOAUDIO(Context context) { return context.getString(R.string.PREFS_PLAYSLIDESAUTOAUDIO); }

    public final static String DEFAULT_EDITPROJECTNAME = null;
    public final static String DEFAULT_PLAYSLIDESNAME = null;
    public final static boolean DEFAULT_PLAYSLIDESAUTOAUDIO = false;
}
