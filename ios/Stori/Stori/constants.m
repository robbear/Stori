//
//  constants.m
//  Stori
//
//  Created by Rob Bearman on 3/4/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "constants.h"

//
//  Constants for SlideShareJSON and SlideJSON
//
NSString *const KEY_TITLE = @"title";
NSString *const KEY_DESCRIPTION = @"description";
NSString *const KEY_VERSION = @"version";
NSString *const KEY_TRANSITIONEFFECT = @"transitionEffect";
NSString *const KEY_SLIDES = @"slides";
NSString *const KEY_IMAGE = @"image";
NSString *const KEY_AUDIO = @"audio";
NSString *const KEY_TEXT = @"text";
NSString *const KEY_ORDER = @"order";

//
// STOPreferences keys
//
NSString *const PREFS_EDITPLAYNAME = @"prefs_editplayname";
NSString *const PREFS_PLAYSLIDESNAME = @"prefs_playslidesname";
NSString *const PREFS_PLAYSLIDESAUTOAUDIO = @"prefs_playslidesautoaudio";

NSString *const SLIDESHARE_JSON_FILENAME = @"stori.json";

//
// AWS SDK constants
//
NSString *const AMAZONSHAREDPREFERENCES_USERNAME = @"amazonsharedpreferences_username";
NSString *const AMAZONSHAREDPREFERENCES_USEREMAIL = @"amazonsharedpreferences_useremail";
NSString *const DIRECTORY_ENTRY_SEGMENT_STRING = @"manifests/";
NSString *const TITLE_SEGMENT_STRING = @"title/";
NSString *const SLIDE_COUNT_SEGMENT_STRING = @"count/";

//
// Maximum text lengths
//
int const MAX_SLIDE_TEXT_CHARACTERS = 140;

//
// Default web site base url
//
NSString *const BASE_WEB_URL = @"http://stori-app.com/";
NSString *const SLIDES_DIRECTORY_NAME = @"slides/";
int const WEB_URL_SEGMENT_COUNT = 3;


@implementation Constants

+(UIAlertView *)credentialsAlert
{
    return [[UIAlertView alloc] initWithTitle:@"AWS Credentials" message:CREDENTIALS_ALERT_MESSAGE delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
}

+(UIAlertView *)errorAlert:(NSString *)message
{
    return [[UIAlertView alloc] initWithTitle:@"Error" message:message delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
}

+(UIAlertView *)expiredCredentialsAlert
{
    return [[UIAlertView alloc] initWithTitle:@"AWS Credentials" message:@"Credentials Expired, retry your request." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
}

+ (NSString *)baseAWSStorageURL {
    return [NSString stringWithFormat:@"https://s3-us-west-2.amazonaws.com/%@/", BUCKET_NAME];
}

+ (NSString *)baseWebSlidesUrl {
    return [NSString stringWithFormat:@"%@%@", BASE_WEB_URL, SLIDES_DIRECTORY_NAME];
}

@end