//
// Copyright (c) 2011 Hyperfine Software Corp.
// All rights reserved
//
package com.stori.stori;

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
    public static final String RECENT_AUTHORITY = "authority.stori-app.stori.com";

    // Default setting for notifications
    public static final boolean NOTIFICATIONS_ON_BY_DEFAULT = true;

    // Use cache space rather than files space
    public static final boolean USE_CACHE = true;

    // Toast vertical offset to keep it off centered controls
    public static final int toastVerticalOffset = -100;

    // Default dimensions for ideal bitmap display/compression
    public static final int imageDisplayWidthLandcape = 1024;
    public static final int imageDisplayHeightLandscape = 768;
    public static final int imageDisplayWidthPortrait = 768;
    public static final int imageDisplayHeightPortrait = 1024;

    // Image file size floor under which we don't compress
    public static final int imageFileSizeFloorBytes = 800*1024;
    public static final int jpgCompressionValue = 75;

    // Recording time limits - recordingTimeSegmentMillis * numRecordingSegments = 1 minute
    // Note that recordingTimeSegmeentMillis indicates the resolution with which check for cancel.
    public static final int recordingTimeSegmentMillis = 500;
    public static final int numRecordingSegments = 120;

    // Recording timeout vibrate duration
    public static final int recordingTimeoutVibrateMillis = 500;

    // Camera file folder name for snapshots taken within Stori
    public static final String cameraImageFolderName = "Stori";
    public static final String copiedImageFolderName = "StoriDownloads";

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
    public static final String GOOGLE_ROLE_ARN = "arn:aws:iam::226207118720:role/GoogleWIFS3FileStoreForStori";
    public static final String AMAZON_ROLE_ARN = "ROLE_ARN";
    public static final String GOOGLE_CLIENT_ID = "335249838846-jtic9s13ib0u5lv6rmhamn7sbdmq86mc.apps.googleusercontent.com";

    //
    // Base cloud urls - support for CLOUD_STORAGE_PROVIDER setting
    //
    public static final String baseAWSStorageUrl = "https://s3-us-west-2.amazonaws.com/" + AWS_BUCKET_NAME + "/";
    public static final String directoryEntrySegmentString = "manifests/";
    public static final String titleSegmentString = "title/";
    public static final String slideCountSegmentString = "count/";
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

    // Maximum slide text characters
    public static final int maxSlideTextCharacters = 140;

    // Number of lines to display in Slide text dialog
    public static final int numberOfEditTextLinesForSlideText = 5;

    // Delay in milliseconds before slide audio starts playing
    public static final int audioDelayMillis = 1000;

    // Maximum published Storis for free version
    public static final int maxPublishedForFree = 20;

    // Maximum number of slides per Stori for free version
    public static final int maxSlidesPerStoriForFree = 10;

    // Download buffer size in bytes
    public static final int downloadBufferSize = 8*1024;

	// Error logs - ship with this set to false
	public static final boolean E = false;
	
	// Debug logs - ship with this set to false
	public static final boolean D = false;
	
	// Verbose logs - ship with this set to false
	public static final boolean V = false;
}
