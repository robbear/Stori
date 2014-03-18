//
//  StoriListItem.m
//  Stori
//
//  Created by Rob Bearman on 3/18/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "StoriListItem.h"

@implementation StoriListItem

- (id)initWithSlideShareName:(NSString *)slideShareName withTitle:(NSString *)title withDate:(NSString *)modifiedDate withCount:(int)count {
    self = [super init];
    if (!self) {
        return nil;
    }
    
    _slideShareName = slideShareName;
    _title = title;
    _modifiedDate = modifiedDate;
    _countSlides = count;
    
    return self;
}


@end
