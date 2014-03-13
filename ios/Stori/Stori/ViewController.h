//
//  ViewController.h
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <GooglePlus/GooglePlus.h>
#import "AmazonClientManager.h"
#import "LoginViewController.h"

@class GPPSignInButton;

@interface ViewController : UIViewController <AmazonClientManagerGoogleAccountDelegate>

@property (weak, nonatomic) IBOutlet UILabel *userIDLabel;
@property (weak, nonatomic) IBOutlet UILabel *userEmailLabel;
@property (weak, nonatomic) IBOutlet UIButton *amazonLoginButton;
@property (weak, nonatomic) IBOutlet UIButton *disconnectButton;

@property (strong, nonatomic) LoginViewController *loginViewController;

@end
