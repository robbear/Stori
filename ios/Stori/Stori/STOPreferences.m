//
//  STOPreferences.m
//  Stori
//
//  Created by Rob Bearman on 3/6/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOPreferences.h"

@implementation STOPreferences

+ (void)initializeDefaults {
    HFLogDebug(@"STOPreferences.initializeDefaults");
    
    [[NSUserDefaults standardUserDefaults]
        registerDefaults:[NSDictionary dictionaryWithContentsOfFile:[[NSBundle mainBundle]
        pathForResource:@"PrefsDefaults" ofType:@"plist"]]];
}

+ (NSString *)getEditPlayName {
    return [[NSUserDefaults standardUserDefaults] stringForKey:PREFS_EDITPLAYNAME];
}

+ (void)saveEditPlayName:(NSString *)editPlayName {
    [[NSUserDefaults standardUserDefaults] setObject:editPlayName forKey:PREFS_EDITPLAYNAME];
}

+ (NSString *)getPlaySlidesName {
    return [[NSUserDefaults standardUserDefaults] stringForKey:PREFS_PLAYSLIDESNAME];
}

+ (void)savePlaySlidesName:(NSString *)playSlidesName {
    [[NSUserDefaults standardUserDefaults] setObject:playSlidesName forKey:PREFS_PLAYSLIDESNAME];
}

+ (BOOL)getPlaySlidesAutoAudio {
    return [[NSUserDefaults standardUserDefaults] boolForKey:PREFS_PLAYSLIDESAUTOAUDIO];
}

+ (void)savePlaySlidesAutoAudio:(BOOL)isAutoAudio {
    [[NSUserDefaults standardUserDefaults] setBool:isAutoAudio forKey:PREFS_PLAYSLIDESAUTOAUDIO];
}

@end
