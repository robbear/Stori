//
//  EditPlayController.h
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <GooglePlus/GooglePlus.h>
#import "AmazonClientManager.h"
#import "AWSS3Provider.h"
#import "LoginViewController.h"
#import "SlideShareJSON.h"

@interface EditPlayController : UIViewController <UIPageViewControllerDataSource, UIPageViewControllerDelegate, AmazonClientManagerGoogleAccountDelegate, AWSS3ProviderDelegate>

@property (strong, nonatomic) UIPageViewController *pageViewController;
@property (strong, nonatomic) SlideShareJSON *ssj;
@property (strong, nonatomic) NSString *slideShareName;
@property (strong, nonatomic) NSString *userUuid;
@property (nonatomic) int currentSlideIndex;
@property (nonatomic) int pendingSlideIndex;
@property (strong, nonatomic) LoginViewController *loginViewController;

- (void)addSlide:(int)newIndex;
- (void)deleteSlide:(NSString *)slideUuid withImage:(NSString *)imageFileName withAudio:audioFileName;
- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText;
- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText withForcedNulls:(BOOL)forceNulls;
- (NSString *)getSlideText:(NSString *)slideUuid;
- (int)getSlideCount;
- (int)getSlidePosition:(NSString *)slideUuid;
- (NSString *)getSlidesTitle;

@end
