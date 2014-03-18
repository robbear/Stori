//
//  StoriListItem.h
//  Stori
//
//  Created by Rob Bearman on 3/18/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface StoriListItem : NSObject

@property (readonly) NSString *slideShareName;
@property (readonly) NSString *title;
@property (readonly) NSString *modifiedDate;
@property (readonly) int countSlides;

- (id)initWithSlideShareName:(NSString *)slideShareName withTitle:(NSString *)title withDate:(NSString *)modifiedDate withCount:(int)count;

@end
