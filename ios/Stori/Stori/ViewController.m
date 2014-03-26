//
//  ViewController.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "ViewController.h"
#import "AmazonSharedPreferences.h"
#import "StoriListItem.h"
#import "StoriListController.h"

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
        //
        // Remember: Use the shared instance versions of AmazonClientManager and
        // GPPSignIn for the user-interactive sign in flow. See also LoginViewController.
        //
        [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
        if (![[AmazonClientManager sharedInstance] silentSharedGPlusLogin]) {
            HFLogDebug(@"ViewController.viewDidAppear: silentSharedGPlusLogin failed");
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

    //
    // Remember: Use the shared instance versions of
    // AmazonClientManager and GPPSignIn for the user-interactive
    // sign in and sign out flows.
    //
    [[AmazonClientManager sharedInstance] disconnectFromSharedGoogle];
}

- (IBAction)onAmazonLoginButtonClicked:(id)sender {
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
    self.loginViewController = (LoginViewController *)[storyboard instantiateViewControllerWithIdentifier:@"LoginViewController"];
    [self presentViewController:self.loginViewController animated:YES completion:nil];
}

- (void) googleSignInComplete:(BOOL)success {
    HFLogDebug(@"ViewController.googleSignInComplete: success=%d", success);
    
    _needsAuthentication = !success;

    if (_needsAuthentication) {
        HFLogDebug(@"ViewController.googleSignInComplete - _needsAuthentication is still TRUE, so that means login UI is needed");
        
        UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
        self.loginViewController = (LoginViewController *)[storyboard instantiateViewControllerWithIdentifier:@"LoginViewController"];
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

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"ViewController.getStoriItemsComplete - found %d S3 objects", [arrayItems count]);
    
    NSString *slideShareName = nil;
    
    for (StoriListItem *sli in arrayItems) {
        HFLogDebug(@"***\nslideShareName=%@\ntitle=%@\nmodifiedDate=%@\ncountSlides=%d\n\n", sli.slideShareName, sli.title, sli.modifiedDate, sli.countSlides);
        
        if ([sli.title isEqualToString:@"Delete this test"]) {
            slideShareName = sli.slideShareName;
        }
    }
    
    // Release the provider
    self.awsS3Provider = nil;
    
    // BUGBUG - test delete virtual directory
    if (slideShareName) {
        HFLogDebug(@"**** Found slideShareName to delete. Now test deleteVirtualDirectory");
        
        self.awsS3Provider = [[AWSS3Provider alloc] init];
        [self.awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
        [self.awsS3Provider deleteVirtualDirectoryAsync:slideShareName];
    }
}

- (void)deleteVirtualDirectoryComplete {
    HFLogDebug(@"ViewController.deleteVirtualDirectoryComplete");
    
    self.awsS3Provider = nil;
}

- (void)uploadComplete:(BOOL)success {
    HFLogDebug(@"ViewController.uploadComplete: success=%d", success);
}

- (IBAction)onTestS3ButtonClicked:(id)sender {
    HFLogDebug(@"ViewController.onTestS3ButtonClicked");
    
    if (self.awsS3Provider) {
        HFLogDebug(@"ViewController.onTestS3ButtonClicked - ***** self.awsS3Provider is not nil *****");
    }
    
    self.awsS3Provider = [[AWSS3Provider alloc] init];
    [self.awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
    [self.awsS3Provider getStoriItemsAsync];
}
@end
