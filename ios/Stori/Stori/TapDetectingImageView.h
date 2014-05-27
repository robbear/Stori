//
//  TapDetectingImageView.h
//  Stori
//
//  Created by Rob Bearman on 5/27/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol TapDetectingImageViewDelegate;

@interface TapDetectingImageView : UIImageView

@property id<TapDetectingImageViewDelegate> delegate;
@property CGPoint tapLocation;            // Needed to record location of single tap
@property BOOL multipleTouches;           // YES if a touch even contains more than one touch; reset when all fingers lifted
@property BOOL twoFingerTapIsPossible;    // Set to NO when 2-finger tap can be ruled out

@end

@protocol TapDetectingImageViewDelegate <NSObject>
- (void)tapDetectingImageView:(TapDetectingImageView *)view gotSingleTapAtPoint:(CGPoint)tapPoint;
- (void)tapDetectingImageView:(TapDetectingImageView *)view gotDoubleTapAtPoint:(CGPoint)tapPoint;
- (void)tapDetectingImageView:(TapDetectingImageView *)view gotTwoFingerTapAtPoint:(CGPoint)tapPoint;
@end
