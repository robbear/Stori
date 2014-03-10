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

//
// AWS credentials
//
NSString *const AWS_BUCKET_NAME = @"hfstori";
NSString *const AMAZON_ROLE_ARN = @"ROLE_ARN";
NSString *const FACEBOOK_ROLE_ARN = @"ROLE_ARN";
NSString *const GOOGLE_ROLE_ARN = @"arn:aws:iam::226207118720:role/GoogleWIFS3FileStoreForStoriForiOS";
NSString *const GOOGLE_CLIENT_ID = @"578112260663-ppl46i6su6bnrt6d6qvrnj0slji5kovo.apps.googleusercontent.com";