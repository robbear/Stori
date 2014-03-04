//
//  SlideShareJSON.m
//  Stori
//
//  Created by Rob Bearman on 3/3/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "SlideShareJSON.h"

//
// SlideShareJSON example:
//
//  {
//      title: "Title text",
//      description: "Description text",
//      version: 1,
//      transitionEffect: 0,
//      slides: {
//          guidval1: { image: "http://foo.com/1.jpg", audio: "http://foo.com/1.3gp", text: "user text" },
//          guidval2: { image: "http://foo.com/2.jpg", audio: "http://foo.com/2.3gp", text: "user text" },
//          guidval3: { image: "http://foo.com/3.jpg", audio: "http://foo.com/3.3gp", text: "user text" }
//      },
//      order: [ guidval2, guidval3, guidval1 ]
//  }
//

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
                           NSLocalizedString(@"default_stori_title", nil), KEY_TITLE,
                           NSLocalizedString(@"default_stori_description", nil), KEY_DESCRIPTION,
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

- (id)initWithString:(NSString *)jsonString {
    self = [super init];
    
    if (self) {
        HFLogDebug(@"SlideShareJSON.initWithString: %@", jsonString);
        
        NSError *err;
        
        NSData * dataTest = [jsonString dataUsingEncoding:NSUTF8StringEncoding];
        HFLogDebug(@"SlideShareJSON.initWithString: dataTest=%@", dataTest);
        
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:[jsonString dataUsingEncoding:NSUTF8StringEncoding]
                                                          options:NSJSONReadingMutableContainers error:&err];
        if (err) {
            HFLogDebug(@"SlideShareJSON.initWithString: error=%@", err);
        }
        
        _jsonDictionary = [dict mutableCopy];
    }
    
    return self;
}

- (void)setTitle:(NSString *)title {
    [_jsonDictionary setObject:title forKey:KEY_TITLE];
}

- (NSString *)getTitle {
    return [_jsonDictionary objectForKey:KEY_TITLE];
}

- (void)setDescription:(NSString *)description {
    [_jsonDictionary setObject:description forKey:KEY_DESCRIPTION];
}

- (NSString *)getDescription {
    return [_jsonDictionary objectForKey:KEY_DESCRIPTION];
}

- (void)setVersion:(int)version {
    [_jsonDictionary setObject:[NSNumber numberWithInt:version] forKey:KEY_VERSION];
}

- (int)getVersion {
    return (int)[_jsonDictionary objectForKey:KEY_VERSION];
}

- (BOOL)isPublished {
    int version = [self getVersion];
    
    return (version > 1);
}

- (void)setSlides:(NSMutableDictionary *)slides {
    [_jsonDictionary setObject:slides forKey:KEY_SLIDES];
}

- (NSMutableDictionary *)getSlides {
    return [_jsonDictionary objectForKey:KEY_SLIDES];
}

- (void)setOrder:(NSMutableArray *)order {
    [_jsonDictionary setObject:order forKey:KEY_ORDER];
}

- (NSMutableArray *)getOrder {
    return [_jsonDictionary objectForKey:KEY_ORDER];
}

@end
