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

- (void)upsertSlideWithSlideId:(NSString *)uuidString atIndex:(int)index withSlide:(SlideJSON *)slide {
    HFLogDebug(@"SlideShareJSON.upsertSlideWithSlideId: uuidString=%@, index=%d", uuidString, index);
    
    NSString *imageUrl = [slide getImageUrlString];
    NSString *audioUrl = [slide getAudioUrlString];
    NSString *slideText = [slide getText];

    [self upsertSlideWithSlideId:uuidString atIndex:index withImageUrl:imageUrl withAudioUrl:audioUrl withSlideText:slideText forceNulls:FALSE];
}

- (void)upsertSlideWithSlideId:(NSString *)uuidString atIndex:(int)index withImageUrl:(NSString *)imageUrl
                      withAudioUrl:(NSString *)audioUrl withSlideText:(NSString *)slideText forceNulls:(BOOL)forceNulls {
    HFLogDebug(@"SlideShareJSON.upsertSlideWithSlideId: uuidString=%@, index=%d, imageUrl=%@, audioUrl=%@, slideText=%@, forceNulls=%d",
               uuidString, index, imageUrl, audioUrl, slideText, forceNulls);
    
    NSMutableDictionary *slides = [self getSlides];
    NSMutableDictionary *slide = [slides objectForKey:uuidString];
    
    if (slide) {
        HFLogDebug(@"SlideShareJSON.upsertSlideWithSlideId - found slide and updating for uuid=%@", uuidString);
        
        if (forceNulls || imageUrl) {
            [slide setObject:imageUrl forKey:KEY_IMAGE];
        }
        
        if (forceNulls || audioUrl) {
            [slide setObject:audioUrl forKey:KEY_AUDIO];
        }
        
        if (forceNulls || slideText) {
            [slide setObject:slideText forKey:KEY_TEXT];
        }
    }
    else {
        HFLogDebug(@"SlideShareJSON.upsertSlideWithSlideId - no slide found for %@, so creating new slide", uuidString);
        
        NSMutableArray *orderArray = [self getOrder];
        
        NSMutableDictionary *paths = [[NSMutableDictionary alloc] init];
        [paths setObject:imageUrl forKey:KEY_IMAGE];
        [paths setObject:audioUrl forKey:KEY_AUDIO];
        [paths setObject:slideText forKey:KEY_TEXT];
        
        int oldCount = [orderArray count];
        [slides setObject:paths forKey:uuidString];
        if (index < 0 || index >= oldCount) {
            // Put the new item at the end
            [orderArray addObject:uuidString];
        }
        else {
            // Insert the new item at the specified index
            [orderArray insertObject:uuidString atIndex:index];
        }
    }
}

- (void)reorderWithCurrentPosition:(int)currentPosition atNewPosition:(int)newPosition {
    HFLogDebug(@"SlideShareJSON.reorderWithCurrentPostion: currentPosition=%d, newPosition=%d", currentPosition, newPosition);
    
    NSMutableArray *orderArray = [self getOrder];
    int count = [orderArray count];
    
    if ((currentPosition >= count) || (currentPosition < 0) || (newPosition >= count) || (newPosition < 0)) {
        HFLogDebug(@"SlideShareJSON.reorderWithCurrentPosition: bailing with currentPosition or newPosition out of range");
        return;
    }
    
    NSString *uuid = [orderArray objectAtIndex:currentPosition];
    [orderArray removeObjectAtIndex:currentPosition];
    [orderArray insertObject:uuid atIndex:newPosition];
}

- (void)removeSlideBySlideId:(NSString *)uuidSlide {
    HFLogDebug(@"SlideShareJSON.removeSlideBySlideId: %@", uuidSlide);
    
    NSMutableDictionary *slides = [self getSlides];
    NSMutableDictionary *slide = [slides objectForKey:uuidSlide];
    
    if (slide) {
        HFLogDebug(@"SlideShareJSON.removeSlideBySlideId - removing slide and order entry");
        
        NSMutableArray *orderArray = [self getOrder];
        [orderArray removeObject:uuidSlide];
        
        [slides removeObjectForKey:uuidSlide];
    }
    else {
        HFLogDebug(@"SlideShareJSON.removeSlideBySlideId - no slide found. Bailing");
    }
}

- (SlideJSON *)getSlideBySlideId:(NSString *)uuidSlide {
    HFLogDebug(@"SlideShareJSON.getSlideBySlideId: %@", uuidSlide);
    
    NSMutableDictionary *slides = [self getSlides];
    return [slides objectForKey:uuidSlide];
}

- (SlideJSON *)getSlideAtIndex:(int)index {
    HFLogDebug(@"SlideShareJSON.getSlideAtIndex: %d", index);
    
    NSString *uuid = [self getSlideUuidByOrderIndex:index];
    return [self getSlideBySlideId:uuid];
}

- (int)getOrderIndexForSlide:(NSString *)uuidSlide {
    HFLogDebug(@"SlideShareJSON.getOrderIndexForSlide: %@", uuidSlide);
    
    NSMutableArray *orderArray = [self getOrder];
    
    for (int i = 0; i < orderArray.count; i++) {
        if ([uuidSlide caseInsensitiveCompare:[orderArray objectAtIndex:i]]) {
            HFLogDebug(@"SlideShareJSON.getOrderIndexForSlide returning %d", i);
            return i;
        }
    }
    
    HFLogDebug(@"SlideShareJSON.getOrderIndexForSlide - no slide found");
    return -1;
}

- (int)getSlideCount {
    return [[self getOrder] count];
}

- (NSString *)getSlideUuidByOrderIndex:(int)index {
    HFLogDebug(@"SlideShareJSON.getSlideUuidByOrderIndex: index=%d", index);
    
    int count = [self getSlideCount];
    
    if (index < 0 || index > count - 1) {
        return nil;
    }
    
    return [[self getOrder] objectAtIndex:index];
}

- (NSArray *)getImageFileNames {
    NSMutableArray *imageFileNames = [[NSMutableArray alloc] init];
    SlideJSON *sj;
    
    int count = [self getSlideCount];
    for (int i = 0; i < count; i++) {
        sj = [self getSlideAtIndex:i];
        NSString *fileName = [sj getImageFilename];
        if (fileName) {
            [imageFileNames addObject:fileName];
        }
    }
    
    return imageFileNames;
}

- (NSArray *)getAudioFileNames {
    NSMutableArray *audioFileNames = [[NSMutableArray alloc] init];
    SlideJSON *sj;
    
    int count = [self getSlideCount];
    for (int i = 0; i < count; i++) {
        sj = [self getSlideAtIndex:i];
        NSString *fileName = [sj getAudioFilename];
        if (fileName) {
            [audioFileNames addObject:fileName];
        }
    }
    
    return audioFileNames;
}

- (BOOL)saveToFolder:(NSString *)folder withFileName:(NSString *)fileName {
    HFLogDebug(@"SlideShareJSON.saveToFolder: folder=%@, fileName=%@", folder, fileName);
    
    // BUGBUG: TODO
    // return [Utilities.saveStringToFile(_jsonDictionary.toString, folder, fileName);
    
    return FALSE;
}

+ (SlideShareJSON *)loadFromFolder:(NSString *)folder withFileName:(NSString *)fileName {
    // BUGBUG: TODO
    return nil;
}

+ (NSString *)getSlideShareTitle:(NSString *)folder {
    // BUGBUG: TODO
    return nil;
}

+ (BOOL)isSlideSharePublished:(NSString *)folder {
    // BUGBUG: TODO
    return FALSE;
}

@end
