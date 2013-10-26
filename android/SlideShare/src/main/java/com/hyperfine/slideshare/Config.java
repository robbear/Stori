//
// Copyright (c) 2011 Hyperfine Software Corp.
// All rights reserved
//
package com.hyperfine.slideshare;

import java.util.GregorianCalendar;

import android.os.Build;

public class Config
{
	// Build string to appear in Information page
	public static final String buildString = "g000000000000";

    // Is this an Amazon release?
    public static final boolean isAmazon = false;
    
    // Recent search authority string
    public static final String RECENT_AUTHORITY = "authority.slideshare.hyperfine.com";

    // Default setting for notifications
    public static final boolean NOTIFICATIONS_ON_BY_DEFAULT = true;

    // Use cache space rather than files space
    public static final boolean USE_CACHE = true;

    // Default base url
    public static final String baseSlideShareUrl = "http://hyperfine.com/slideshare/";

    // Standard SlideShareJSON file name
    public static final String slideShareJSONFilename = "slideshare.json";

    // JPG file compression level (0 - 100)
    public static final int jpgCompressionLevel = 25;

    // Delay in milliseconds before slide audio starts playing
    public static final int audioDelayMillis = 1000;

	// Error logs - ship with this set to false
	public static final boolean E = true;
	
	// Debug logs - ship with this set to false
	public static final boolean D = true;
	
	// Verbose logs - ship with this set to false
	public static final boolean V = true;
}
