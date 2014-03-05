//
//  SlideJSON.m
//  Stori
//
//  Created by Rob Bearman on 3/4/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "SlideJSON.h"

@implementation SlideJSON

- (id)init {
    self = [super init];
    
    if (self) {
        HFLogDebug(@"SlideJSON constructor");
        
        _jsonDictionary = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                           NULL, KEY_IMAGE,
                           NULL, KEY_AUDIO,
                           NULL, KEY_TEXT,
                           nil];
    }
    
    return self;
}

- (id)initWithString:(NSString *)jsonString {
    self = [super init];
    
    if (self) {
        HFLogDebug(@"SlideJSON.initWithString: %@", jsonString);
        
        NSError *err;
        
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonString dataUsingEncoding:NSUTF8StringEncoding]
                                                             options:NSJSONReadingMutableContainers error:&err];
        if (err) {
            HFLogDebug(@"SlideJSON.initWithString: error=%@", err);
        }
        
        _jsonDictionary = [dict mutableCopy];
    }
    
    return self;
}

- (id)initWithSlide:(NSDictionary *)slide {
    self = [super init];
    
    if (self) {
        HFLogDebug(@"SlideJSON.initWithSlide");
        
        _jsonDictionary = [[NSMutableDictionary alloc] init];
        
        [_jsonDictionary setObject:[slide objectForKey:KEY_IMAGE] forKey:KEY_IMAGE];
        [_jsonDictionary setObject:[slide objectForKey:KEY_AUDIO] forKey:KEY_AUDIO];
        [_jsonDictionary setObject:[slide objectForKey:KEY_TEXT] forKey:KEY_TEXT];
    }
    
    return self;
}

- (NSString *)getImageUrlString {
    return [_jsonDictionary objectForKey:KEY_IMAGE];
}

- (NSString *)getImageFilename {
    NSString * fileName;
    NSString *imageUrlString = [self getImageUrlString];
    
    if (imageUrlString) {
        NSURL *url = [[NSURL alloc] initWithString:imageUrlString];
        NSArray *segments = [url pathComponents];
        
        fileName = (NSString *)segments.lastObject;
    }
    
    return fileName;
}

- (NSString *)getAudioUrlString {
    return [_jsonDictionary objectForKey:KEY_AUDIO];
}

- (NSString *)getAudioFilename {
    NSString * fileName;
    NSString *audioUrlString = [self getAudioUrlString];
    
    if (audioUrlString) {
        NSURL *url = [[NSURL alloc] initWithString:audioUrlString];
        NSArray *segments = [url pathComponents];
        
        fileName = (NSString *)segments.lastObject;
    }
    
    return fileName;
}

- (NSString *)getText {
    return [_jsonDictionary objectForKey:KEY_TEXT];
}

@end
