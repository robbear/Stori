//
//  constants.h
//  Stori
//
//  Created by Rob Bearman on 3/4/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

//
// SlideShareJSON and SlideJSON keys
//
extern NSString *const KEY_TITLE;
extern NSString *const KEY_DESCRIPTION;
extern NSString *const KEY_VERSION;
extern NSString *const KEY_TRANSITIONEFFECT;
extern NSString *const KEY_SLIDES;
extern NSString *const KEY_IMAGE;
extern NSString *const KEY_AUDIO;
extern NSString *const KEY_TEXT;
extern NSString *const KEY_ORDER;

//
// STOPreferences keys
//
extern NSString *const PREFS_EDITPLAYNAME;
extern NSString *const PREFS_PLAYSLIDESNAME;
extern NSString *const PREFS_PLAYSLIDESAUTOAUDIO;

extern NSString *const SLIDESHARE_JSON_FILENAME;


//
// AWS SDK constants
//

extern NSString *const AMAZONSHAREDPREFERENCES_USERNAME;
extern NSString *const AMAZONSHAREDPREFERENCES_USEREMAIL;
extern NSString *const BASE_AWS_STORAGE_URL;
extern NSString *const DIRECTORY_ENTRY_SEGMENT_STRING;
extern NSString *const TITLE_SEGMENT_STRING;
extern NSString *const SLIDE_COUNT_SEGMENT_STRING;

/**
 * The Amazon S3 Bucket in your account to use for this application.
 * This bucket should have been previously created.
 */
#define BUCKET_NAME                 @"hfstori"

/**
 * Enables FB Login.
 * Login with Facebook also requires the following things to be set
 *
 * FacebookAppID in App plist file
 * The appropriate URL handler in project (should match FacebookAppID)
 */
#define FB_LOGIN                    0

#if FB_LOGIN

#import <FacebookSDK/FacebookSDK.h>

/**
 * Role that user will assume after logging in.
 * This role should have appropriate policy to restrict actions to only required
 * services and resources.
 */
#define FB_ROLE_ARN @"ROLE_ARN"

#endif

/**
 * Enables Google+
 * Google+ login also requires the following things to be set
 *
 * The appropriate URL handler in project (Should be the same as BUNDLE_ID)
 */
#define GOOGLE_LOGIN                1

#if GOOGLE_LOGIN

#import <GooglePlus/GooglePlus.h>
#import <GoogleOpenSource/GoogleOpenSource.h>

/**
 * Role that user will assume after logging in.
 * This role should have appropriate policy to restrict actions to only required
 * services and resources.
 */
#define GOOGLE_ROLE_ARN             @"arn:aws:iam::226207118720:role/GoogleWIFS3FileStoreForStoriForiOS"

/**
 * Client ID retrieved from Google API console
 */
#define GOOGLE_CLIENT_ID            @"578112260663-ppl46i6su6bnrt6d6qvrnj0slji5kovo.apps.googleusercontent.com"

/**
 * Client scope that will be used with Google+
 */
#define GOOGLE_CLIENT_SCOPE         @"https://www.googleapis.com/auth/userinfo.profile"
#define GOOGLE_OPENID_SCOPE         @"openid"

#endif

/**
 * Enables Amazon
 * Login with Amazon also requires the following things to be set
 *
 * IBAAppAPIKey in App plist file
 * The appropriate URL handler in project (of style amzn-BUNDLE_ID)
 */
#define AMZN_LOGIN                  0

#if AMZN_LOGIN

#import "AIMobileLib.h"
#import "AIAuthenticationDelegate.h"
#import "AIError.h"

/**
 * Role that user will assume after logging in.
 * This role should have appropriate policy to restrict actions to only required
 * services and resources.
 */
#define AMZN_ROLE_ARN @"ROLE_ARN"

#endif

#define IDP_NOT_ENABLED_MESSAGE      @"This provider is not enabled, please refer to Constants.h to enabled this provider"
#define CREDENTIALS_ALERT_MESSAGE    @"Please update the Constants.h file with your Facebook or Google App settings."
#define ACCESS_KEY_ID                @"USED_ONLY_FOR_TESTING"  // Leave this value as is.
#define SECRET_KEY                   @"USED_ONLY_FOR_TESTING"  // Leave this value as is.

@interface Constants:NSObject {
}

+(UIAlertView *)credentialsAlert;
+(UIAlertView *)errorAlert:(NSString *)message;
+(UIAlertView *)expiredCredentialsAlert;

+(NSString *)baseAWSStorageURL;

@end
