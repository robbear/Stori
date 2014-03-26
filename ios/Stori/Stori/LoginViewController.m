/*
 * Copyright 2010-2012 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

#import "LoginViewController.h"
#import "AmazonClientManager.h"

@implementation LoginViewController

- (void)viewDidLoad
{
    HFLogDebug(@"LoginViewController.viewDidLoad - prepping Google login button");

    [super viewDidLoad];

    [self.amazonLoginButton setEnabled:NO];
    [self.facebookLoginButton setEnabled:NO];
}

- (void)viewWillAppear:(BOOL)animated
{
    HFLogDebug(@"LoginViewController.viewWillAppear - setting AmazonClientManager viewController to LoginViewController");
    
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    HFLogDebug(@"LoginViewController.viewWillDisappear - setting AmazonClientManager viewController to nil");
    
    [super viewWillDisappear:animated];
}

- (IBAction)onGoogleLoginButtonClicked:(id)sender {
    HFLogDebug(@"LoginViewController.onGoogleLoginButtonClicked");
    
    //
    // Use the shared instance versions of AmazonClientManager and
    // GPPSignIn for the user-interactive sign in flow.
    //
    [[AmazonClientManager sharedInstance] initSharedGPlusLogin];
    [[GPPSignIn sharedInstance] authenticate];
}

@end
