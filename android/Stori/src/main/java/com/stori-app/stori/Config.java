//
// Copyright (c) 2011 Hyperfine Software Corp.
// All rights reserved
//
package com.stori-app.stori;

import java.net.URI;
import java.util.GregorianCalendar;

import android.os.Build;

public class Config
{
	// Build string to appear in Information page
	public static final String buildString = "g000000000000";

    // Is this an Amazon release?
    public static final boolean isAmazon = false;
    
    // Recent search authority string
    public static final String RECENT_AUTHORITY = "authority.stori.stori-app.com";

    // Default setting for notifications
    public static final boolean NOTIFICATIONS_ON_BY_DEFAULT = true;

    // Use cache space rather than files space
    public static final boolean USE_CACHE = true;

    // Default inSampleSize scale factor for images
    public static final int imageScaleFactor = 4;

    // Recording time limits - recordingTimeSegmentMillis * numRecordingSegments = 1 minute
    // Note that recordingTimeSegmeentMillis indicates the resolution with which check for cancel.
    public static final int recordingTimeSegmentMillis = 500;
    public static final int numRecordingSegments = 120;

    // Recording timeout vibrate duration
    public static final int recordingTimeoutVibrateMillis = 500;

    // Use Google Play Services
    public static final boolean USE_GOOGLE_PLAY_SERVICES = true;

    // Default web site base url
    public static final String baseWebUrl = "http://stori-app.com/";
    public static final String baseWebSlidesUrl = baseWebUrl + "slides/";
    public static final int webUrlSegmentCount = 3;

    // Default cloud storage provider
    public static final CloudStorageProviders CLOUD_STORAGE_PROVIDER = CloudStorageProviders.AWS;
    public static final String baseCloudUrl = getBaseCloudUrl();

    //
    // Amazon S3 settings
    //
    public static final String AWS_BUCKET_NAME = "hfstori";
    public static final String FB_ROLE_ARN = "ROLE_ARN";
    public static final String GOOGLE_ROLE_ARN = "arn:aws:iam::226207118720:role/GoogleWIFS3FileStore";
    public static final String AMAZON_ROLE_ARN = "ROLE_ARN";
    public static final String GOOGLE_CLIENT_ID = "577074179012-7rn9q34trpofjfjesjcm7kad7ta06rmd.apps.googleusercontent.com";

    //
    // Base cloud urls - support for CLOUD_STORAGE_PROVIDER setting
    //
    public static final String baseAWSStorageUrl = "https://s3-us-west-2.amazonaws.com/" + AWS_BUCKET_NAME + "/";
    public enum CloudStorageProviders {
        Azure,
        AWS
    }
    private static String getBaseCloudUrl() {
        switch (CLOUD_STORAGE_PROVIDER) {
            default:
            case AWS:
                return baseAWSStorageUrl;
        }
    }
    // End base cloud urls

    // Standard SlideShareJSON file name
    public static final String slideShareJSONFilename = "stori.json";

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
