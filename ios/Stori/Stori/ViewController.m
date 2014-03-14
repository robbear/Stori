//
//  ViewController.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "ViewController.h"
#import "AmazonSharedPreferences.h"
#import "AWSS3Provider.h"

@interface ViewController ()

- (void)refreshInterface;

@end

@implementation ViewController

bool _needsAuthentication = TRUE;

- (void)viewDidLoad
{
    HFLogDebug(@"ViewController.viewDidLoad");
    
    [super viewDidLoad];
    
    [self refreshInterface];
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"ViewController.viewDidAppear");
    
    [super viewDidAppear:animated];
    
    if (_needsAuthentication) {
        [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
        if (![[AmazonClientManager sharedInstance] silentGPlusLogin]) {
            HFLogDebug(@"ViewController.viewDidAppear: silentGPlusLogin failed");
            [self googleSignInComplete:FALSE];
        }
    }
}

- (void)didReceiveMemoryWarning
{
    HFLogDebug(@"ViewController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#if NEVER
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
        [self refreshInterface];
    }
}
#endif

- (void)refreshInterface {
    HFLogDebug(@"ViewController.refreshInterface");

    HFLogDebug(@"ViewController.refreshInterface: userName = %@", [AmazonSharedPreferences userName]);
    if ([AmazonSharedPreferences userName]) {
        // The user is signed in
        self.amazonLoginButton.hidden = YES;
        self.disconnectButton.hidden = NO;
        self.testS3Button.hidden = NO;
    }
    else {
        self.amazonLoginButton.hidden = NO;
        self.disconnectButton.hidden = YES;
        self.testS3Button.hidden = YES;
    }
    
    self.userEmailLabel.text = [AmazonSharedPreferences userEmail];
    self.userIDLabel.text = [AmazonSharedPreferences userName];
}

- (IBAction)onDisconnectClick:(id)sender {
    HFLogDebug(@"ViewController.onDisconnectClick");

    [[AmazonClientManager sharedInstance] disconnectFromGoogle];
}

- (IBAction)onAmazonLoginButtonClicked:(id)sender {
    self.loginViewController = [[LoginViewController alloc] init];
    [self presentViewController:self.loginViewController animated:YES completion:nil];
}

- (void) googleSignInComplete:(BOOL)success {
    HFLogDebug(@"ViewController.googleSignInComplete: success=%d", success);
    
    _needsAuthentication = !success;

    if (_needsAuthentication) {
        HFLogDebug(@"ViewController.googleSignInComplete - _needsAuthentication is still TRUE, so that means login UI is needed");
        self.loginViewController = [[LoginViewController alloc] init];
        [self presentViewController:self.loginViewController animated:YES completion:nil];
    }
    else {
        if (self.loginViewController) {
            [self.loginViewController dismissViewControllerAnimated:NO completion:nil];
            self.loginViewController = nil;
        }
    }

    [self refreshInterface];
}

- (void) googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"ViewController.googleDisconnectComplete: success=%d", success);
    
    [self refreshInterface];
}

- (IBAction)onTestS3ButtonClicked:(id)sender {
    HFLogDebug(@"ViewController.onTestS3ButtonClicked");
    
    AWSS3Provider *provider = [[AWSS3Provider alloc] init];
    [provider initializeProvider:[AmazonSharedPreferences userName]];
    [provider getStoriItems];
}

@end
