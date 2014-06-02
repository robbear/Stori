//
//  STOOverlayView.m
//  Stori
//
//  Created by Rob Bearman on 6/2/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOOverlayView.h"

@implementation STOOverlayView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    // Drawing code
}
*/

-(id) hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    id hitView = [super hitTest:point withEvent:event];
    if (hitView == self) {
        //HFLogDebug(@"******* hit on self");
        return nil;
    }
    else {
        //HFLogDebug(@"******* hit on subview");
        return hitView;
    }
}

@end
