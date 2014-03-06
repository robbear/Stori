//
//  STOPreferences.h
//  Stori
//
//  Created by Rob Bearman on 3/6/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface STOPreferences : NSObject

+ (void)initializeDefaults;
+ (NSString *)getEditPlayName;
+ (void)saveEditPlayName:(NSString *)editPlayName;
+ (NSString *)getPlaySlidesName;
+ (void)savePlaySlidesName:(NSString *)playSlidesName;
+ (BOOL)getPlaySlidesAutoAudio;
+ (void)savePlaySlidesAutoAudio:(BOOL)isAutoAudio;

@end
