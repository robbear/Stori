//
//  ViewController.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "ViewController.h"
#import "AmazonSharedPreferences.h"

@interface ViewController ()

- (void)refreshInterface;

@end

@implementation ViewController

- (void)viewDidLoad
{
    HFLogDebug(@"ViewController.viewDidLoad");
    
    [super viewDidLoad];

    [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
    [self refreshInterface];
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

    if (self.loginViewController) {
        [self.loginViewController dismissViewControllerAnimated:NO completion:nil];
        self.loginViewController = nil;
    }

    [self refreshInterface];
}

- (void) googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"ViewController.googleDisconnectComplete: success=%d", success);
    
    [self refreshInterface];
}

- (IBAction)onTestS3ButtonClicked:(id)sender {
    HFLogDebug(@"ViewController.onTestS3ButtonClicked");
    
    @try {
        S3ListObjectsRequest  *listObjectRequest = [[S3ListObjectsRequest alloc] initWithName:@"hfstori"];
        listObjectRequest.prefix = [NSString stringWithFormat:@"%@/", [AmazonSharedPreferences userName]];
        
        S3ListObjectsResponse *listObjectResponse = [[[AmazonClientManager sharedInstance] s3] listObjects:listObjectRequest];
        S3ListObjectsResult   *listObjectsResults = listObjectResponse.listObjectsResult;
        
        NSMutableArray *objects;
        
        if (objects == nil) {
            objects = [[NSMutableArray alloc] initWithCapacity:[listObjectsResults.objectSummaries count]];
        }
        else {
            [objects removeAllObjects];
        }
        for (S3ObjectSummary *objectSummary in listObjectsResults.objectSummaries) {
            [objects addObject:[objectSummary key]];
        }
        [objects sortUsingSelector:@selector(compare:)];
        
        HFLogDebug(@"ViewController.onTestS3ButtonClicked - found %d S3 objects under %@", [objects count], [AmazonSharedPreferences userName]);
    }
    @catch (AmazonClientException *exception)
    {
        HFLogDebug(@"Exception = %@", exception);
        [[Constants errorAlert:[NSString stringWithFormat:@"Error list objects: %@", exception.message]] show];
    }
}

@end
