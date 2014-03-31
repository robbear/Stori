//
//  EditPlayPageController.h
//  Stori
//
//  Created by Rob Bearman on 3/31/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <GooglePlus/GooglePlus.h>
#import "AmazonClientManager.h"
#import "AWSS3Provider.h"
#import "LoginViewController.h"
#import "SlideShareJSON.h"
#import "EditPlayController.h"

@interface EditPlayPageController : UIPageViewController <UIPageViewControllerDataSource, AmazonClientManagerGoogleAccountDelegate, AWSS3ProviderDelegate>

@property (strong, nonatomic) SlideShareJSON *ssj;
@property (strong, nonatomic) NSString *slideShareName;
@property (nonatomic) int currentSlideIndex;
@property (strong, nonatomic) LoginViewController *loginViewController;
@property (strong, nonatomic) AWSS3Provider *awsS3Provider;

@end
