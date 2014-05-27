//
//  TapDetectingImageView.m
//  Stori
//
//  Created by Rob Bearman on 5/27/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "TapDetectingImageView.h"

#define DOUBLE_TAP_DELAY 0.35

CGPoint midpointBetweenPoints(CGPoint a, CGPoint b);

@interface TapDetectingImageView ()
- (void)handleSingleTap;
- (void)handleDoubleTap;
- (void)handleTwoFingerTap;
@end

@implementation TapDetectingImageView

- (id)initWithImage:(UIImage *)image {
    self = [super initWithImage:image];
    if (self) {
        [self setUserInteractionEnabled:YES];
        [self setMultipleTouchEnabled:YES];
        self.twoFingerTapIsPossible = YES;
        self.multipleTouches = NO;
    }
    
    return self;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    // Cancel any pending handleSingleTap messages
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(handleSingleTap) object:nil];
    
    // Update our touch state
    if ([[event touchesForView:self] count] > 1) {
        self.multipleTouches = YES;
    }
    if ([[event touchesForView:self] count] > 2) {
        self.twoFingerTapIsPossible = NO;
    }
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    BOOL allTouchesEnded = ([touches count] == [[event touchesForView:self] count]);
    
    // First check for plain single/double tap, which is possible only if we haven't seen multiple touches
    if (!self.multipleTouches) {
        UITouch *touch = [touches anyObject];
        self.tapLocation = [touch locationInView:self];
        
        if ([touch tapCount] == 1) {
            [self performSelector:@selector(handleSingleTap) withObject:nil afterDelay:DOUBLE_TAP_DELAY];
        }
        else if ([touch tapCount] == 2) {
            [self handleDoubleTap];
        }
    }
    
    // Check for 2-finger tap if we've seen multiple touches and haven't yet ruled out that possibility
    else if (self.multipleTouches && self.twoFingerTapIsPossible) {
        
        // Case 1: this is the end of both touches at once
        if ([touches count] == 2 && allTouchesEnded) {
            int i = 0;
            int tapCounts[2];
            CGPoint tapLocations[2];
            for (UITouch *touch in touches) {
                tapCounts[i]    = (int)[touch tapCount];
                tapLocations[i] = [touch locationInView:self];
                i++;
            }
            if (tapCounts[0] == 1 && tapCounts[1] == 1) {    // it's a two-finger tap if they're both single taps
                self.tapLocation = midpointBetweenPoints(tapLocations[0], tapLocations[1]);
                [self handleTwoFingerTap];
            }
        }
        
        // Case 2: this is the end of one touch, and the other hasn't ended yet
        else if ([touches count] == 1 && !allTouchesEnded) {
            UITouch *touch = [touches anyObject];
            if ([touch tapCount] == 1) {
                // If touch is a single tap, store its location so we can average it with the second touch location
                self.tapLocation = [touch locationInView:self];
            }
            else {
                self.twoFingerTapIsPossible = NO;
            }
        }
        
        // Case 3: this is the end of the second of the two touches
        else if ([touches count] == 1 && allTouchesEnded) {
            UITouch *touch = [touches anyObject];
            if ([touch tapCount] == 1) {
                // If the last touch up is a single tap, this was a 2-finger tap
                self.tapLocation = midpointBetweenPoints(self.tapLocation, [touch locationInView:self]);
                [self handleTwoFingerTap];
            }
        }
    }
    
    // If all touches are up, reset touch monitoring state
    if (allTouchesEnded) {
        self.twoFingerTapIsPossible = YES;
        self.multipleTouches = NO;
    }
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    self.twoFingerTapIsPossible = YES;
    self.multipleTouches = NO;
}

- (void)handleSingleTap {
    if ([self.delegate respondsToSelector:@selector(tapDetectingImageView:gotSingleTapAtPoint:)]) {
        [self.delegate tapDetectingImageView:self gotSingleTapAtPoint:self.tapLocation];
    }
}

- (void)handleDoubleTap {
    if ([self.delegate respondsToSelector:@selector(tapDetectingImageView:gotDoubleTapAtPoint:)]) {
        [self.delegate tapDetectingImageView:self gotDoubleTapAtPoint:self.tapLocation];
    }
}

- (void)handleTwoFingerTap {
    if ([self.delegate respondsToSelector:@selector(tapDetectingImageView:gotTwoFingerTapAtPoint:)]) {
        [self.delegate tapDetectingImageView:self gotTwoFingerTapAtPoint:self.tapLocation];
    }
}

@end

CGPoint midpointBetweenPoints(CGPoint a, CGPoint b) {
    CGFloat x = (a.x + b.x) / 2.0;
    CGFloat y = (a.y + b.y) / 2.0;
    return CGPointMake(x, y);
}
