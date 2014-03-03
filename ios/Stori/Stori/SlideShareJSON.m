//
//  SlideShareJSON.m
//  Stori
//
//  Created by Rob Bearman on 3/3/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "SlideShareJSON.h"

static NSString *const KEY_TITLE = @"title";
static NSString *const KEY_DESCRIPTION = @"description";
static NSString *const KEY_VERSION = @"version";
static NSString *const KEY_TRANSITIONEFFECT = @"transitionEffect";
static NSString *const KEY_SLIDES = @"slides";
static NSString *const KEY_IMAGE = @"image";
static NSString *const KEY_AUDIO = @"audio";
static NSString *const KEY_TEXT = @"text";
static NSString *const KEY_ORDER = @"order";

@implementation SlideShareJSON

- (id)init {
    self = [super init];
    if (self) {
        HFLogDebug(@"SlideShareJSON constructor");
        
        _jsonDictionary = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                           @"Default title", KEY_TITLE,
                           @"Default description", KEY_DESCRIPTION,
                           [NSNumber numberWithInteger:1], KEY_VERSION,
                           [NSNumber numberWithInteger:0], KEY_TRANSITIONEFFECT,
                           nil];
        
        NSMutableDictionary *slides = [[NSMutableDictionary alloc] init];
        [_jsonDictionary setObject:slides forKey:KEY_SLIDES];
        
        NSMutableArray *orderArray = [[NSMutableArray alloc] init];
        [_jsonDictionary setObject:orderArray forKey:KEY_ORDER];
    }
    
    return self;
}

@end
