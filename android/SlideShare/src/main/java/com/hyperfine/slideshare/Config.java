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

    // Default web site base url
    public static final String baseWebUrl = "http://slidesharedotcom.jit.su/";
    public static final String baseWebSlidesUrl = baseWebUrl + "slides/";

    // Default cloud storage provider
    public static final CloudStorageProviders CLOUD_STORAGE_PROVIDER = CloudStorageProviders.AWS;
    public static final String baseCloudUrl = getBaseCloudUrl();

    //
    // Base cloud urls - support for CLOUD_STORAGE_PROVIDER setting
    //
    public static final String baseAzureStorageUrl = "http://slideshare.blob.core.windows.net/";
    public static final String baseAWSStorageUrl = "https://s3-us-west-2.amazonaws.com/hfslideshare/";
    public enum CloudStorageProviders {
        Azure,
        AWS
    }
    private static String getBaseCloudUrl() {
        switch (CLOUD_STORAGE_PROVIDER) {
            default:
            case Azure:
                return baseAzureStorageUrl;
            case AWS:
                return baseAWSStorageUrl;
        }
    }
    // End base cloud urls

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
