//
//  SlideShareJSON.h
//  Stori
//
//  Created by Rob Bearman on 3/3/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "SlideJSON.h"

@interface SlideShareJSON : NSObject

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
- (void)upsertSlideWithSlideId:(NSString *)uuidString atIndex:(int)index withSlide:(SlideJSON *)slide;
- (void)upsertSlideWithSlideId:(NSString *)uuidString atIndex:(int)index withImageUrl:(NSString *)imageUrl withAudioUrl:(NSString *)audioUrl withSlideText:(NSString *)slideText forceNulls:(BOOL)forceNulls;
- (void)reorderWithCurrentPosition:(int)currentPosition atNewPosition:(int)newPosition;
- (void)removeSlideBySlideId:(NSString *)uuidSlide;
- (SlideJSON *)getSlideAtIndex:(int)index;
- (SlideJSON *)getSlideBySlideId:(NSString *)uuidSlide;
- (int)getOrderIndexForSlide:(NSString *)uuidSlide;
- (int)getSlideCount;
- (NSString *)getSlideUuidByOrderIndex:(int)index;
- (NSArray *)getImageFileNames;
- (NSArray *)getAudioFileNames;
- (BOOL)saveToFolder:(NSString *)folder withFileName:(NSString *)fileName;

+ (SlideShareJSON *)loadFromFolder:(NSString *)folder withFileName:(NSString *)fileName;
+ (NSString *)getSlideShareTitle:(NSString *)folder;
+ (BOOL)isSlideSharePublished:(NSString *)folder;

@end
