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
        
        NSString *imageVal = [slide objectForKey:KEY_IMAGE];
        NSString *audioVal = [slide objectForKey:KEY_AUDIO];
        NSString *textVal = [slide objectForKey:KEY_TEXT];
        
        if (imageVal) {
            [_jsonDictionary setObject:imageVal forKey:KEY_IMAGE];
        }
        if (audioVal) {
            [_jsonDictionary setObject:audioVal forKey:KEY_AUDIO];
        }
        if (textVal) {
            [_jsonDictionary setObject:textVal forKey:KEY_TEXT];
        }
    }
    
    return self;
}

- (NSString *)getImageUrlString {
    id typeValue = [_jsonDictionary objectForKey:KEY_IMAGE];
    NSString *value = nil;
    if (typeValue != [NSNull null]) {
        value = (NSString *)typeValue;
    }
    
    return value;
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
    id typeValue = [_jsonDictionary objectForKey:KEY_AUDIO];
    NSString *value = nil;
    if (typeValue != [NSNull null]) {
        value = (NSString *)typeValue;
    }
    
    return value;
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
    id typeValue = [_jsonDictionary objectForKey:KEY_TEXT];
    NSString *value = nil;
    if (typeValue != [NSNull null]) {
        value = (NSString *)typeValue;
    }
    
    return value;
}

@end
