//
//  SlideJSON.h
//  Stori
//
//  Created by Rob Bearman on 3/4/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SlideJSON : NSObject

@property (readonly) NSMutableDictionary *jsonDictionary;

- (id)initWithString:(NSString *)jsonString;
- (id)initWithSlide:(NSDictionary *)slide;
- (NSString *)getImageUrlString;
- (NSString *)getImageFilename;
- (NSString *)getAudioUrlString;
- (NSString *)getAudioFilename;
- (NSString *)getText;

@end
