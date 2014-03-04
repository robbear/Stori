//
//  SlideShareJSON.h
//  Stori
//
//  Created by Rob Bearman on 3/3/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface SlideShareJSON : NSDictionary

@property (readonly) NSMutableDictionary *jsonDictionary;

- (id)initWithString:(NSString *)jsonString;
- (void)setTitle:(NSString *)title;
- (NSString *)getTitle;
- (void)setDescription:(NSString *)description;
- (NSString *)getDescription;
- (void)setVersion:(int)version;
- (int)getVersion;
- (BOOL)isPublished;
- (void)setSlides:(NSMutableDictionary *)slides;
- (NSMutableDictionary *)getSlides;
- (void)setOrder:(NSMutableArray *)order;
- (NSMutableArray *)getOrder;

@end
