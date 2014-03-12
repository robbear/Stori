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

@interface ViewController : UIViewController <GPPSignInDelegate, AmazonClientManagerGoogleSignInDelegate>

@property (retain, nonatomic) IBOutlet GPPSignInButton *signInButton;
@property (weak, nonatomic) IBOutlet UIButton *signOutButton;
@property (weak, nonatomic) IBOutlet UIButton *disconnectButton;
@property (weak, nonatomic) IBOutlet UILabel *userIDLabel;
@property (weak, nonatomic) IBOutlet UILabel *userEmailLabel;
@property (weak, nonatomic) IBOutlet UIView *amazonLoginButton;

@property LoginViewController *loginViewController;

@end
