//
//  ViewController.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "ViewController.h"
#import <GooglePlus/GooglePlus.h>
#import <GoogleOpenSource/GoogleOpenSource.h>

@interface ViewController ()

- (void)initiateGoogleLogin;
- (void)refreshInterfaceBasedOnSignIn;
- (void)signOut;
- (void)disconnect;

@end

@implementation ViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.signInButton.hidden = NO;
    self.signOutButton.hidden = YES;
    self.disconnectButton.hidden = YES;
    self.userEmailLabel.text = NULL;
    self.userIDLabel.text = NULL;
    
    [self initiateGoogleLogin];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)initiateGoogleLogin {
    HFLogDebug(@"ViewController.initiateGoogleLogin");
    
    GPPSignIn *signIn = [GPPSignIn sharedInstance];
    signIn.shouldFetchGooglePlusUser = YES;
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.shouldFetchGoogleUserID = YES;
    
    signIn.clientID = GOOGLE_CLIENT_ID;
    
    signIn.scopes = @[kGTLAuthScopePlusLogin];
    
    signIn.delegate = self;
    
    [signIn trySilentAuthentication];
}

- (void)finishedWithAuth:(GTMOAuth2Authentication *)auth error:(NSError *)error {
    HFLogDebug(@"ViewController.finishedWithAuth: auth=%@, error=%@", auth, error);
    
    if (error) {
        // TODO: error handling
    }
    else {
        [self refreshInterfaceBasedOnSignIn];
        
        GPPSignIn *signIn = [GPPSignIn sharedInstance];
        
        self.userIDLabel.text = signIn.userID;
        self.userEmailLabel.text = signIn.userEmail;
    }
}

- (void)refreshInterfaceBasedOnSignIn {
    HFLogDebug(@"ViewController.refreshInterfaceBasedOnSignIn");
    
    if ([[GPPSignIn sharedInstance] authentication]) {
        // The user is signed in
        self.signInButton.hidden = YES;
        self.signOutButton.hidden = NO;
        self.disconnectButton.hidden = NO;
    }
    else {
        self.signInButton.hidden = NO;
    }
}

- (void)signOut {
    HFLogDebug(@"ViewController.signOut");
    
    [[GPPSignIn sharedInstance] signOut];
}

- (void)disconnect {
    HFLogDebug(@"ViewController.disconnect");
    
    [[GPPSignIn sharedInstance] disconnect];
}

- (void)didDisconnectWithError:(NSError *)error {
    HFLogDebug(@"ViewController.didDisconnectWithError: error=%@", error);
    
    if (error) {
        // TODO
    }
    else {
        // The user is signed out and disconnected
        // Clean up user data as specified by the Google+ terms
        
        self.signInButton.hidden = NO;
        self.signOutButton.hidden = YES;
        self.disconnectButton.hidden = YES;
    }
}

- (IBAction)onSignOutClick:(id)sender {
    HFLogDebug(@"ViewController.onSignOutClick");
    
    [self signOut];
    self.signInButton.hidden = NO;
    self.signOutButton.hidden = YES;
    self.disconnectButton.hidden = NO;
}

- (IBAction)onDisconnectClick:(id)sender {
    HFLogDebug(@"ViewController.onDisconnectClick");
    
    [self disconnect];
    self.signInButton.hidden = NO;
    self.disconnectButton.hidden = YES;
    self.signOutButton.hidden = YES;
    
    self.userEmailLabel.text = NULL;
    self.userIDLabel.text = NULL;
}

@end
