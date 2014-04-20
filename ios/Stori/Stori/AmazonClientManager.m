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

#import "AmazonClientManager.h"
#import <AWSRuntime/AWSRuntime.h>
#import "AmazonSharedPreferences.h"
#import <AWSSecurityTokenService/AWSSecurityTokenService.h>

@implementation AmazonClientManager

AmazonS3Client                    *_s3  = nil;
AmazonWIFCredentialsProvider      *_wif = nil;

static GTMOAuth2Authentication  *_auth;

+ (AmazonClientManager *)sharedInstance {
    HFLogDebug(@"AmazonClientManager.sharedInstance");
    
    static AmazonClientManager *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        HFLogDebug(@"AmazonClientManager.sharedInstance - allocating sharedInstance");
        sharedInstance = [[AmazonClientManager alloc] init];
    });
    
    HFLogDebug(@"AmazonClientManager.sharedInstance - returning sharedInstance");
    return sharedInstance;
}

-(AmazonS3Client *)s3 {
    return _s3;
}

-(bool)hasCredentials {
    return _signIn ? [_signIn authentication] : [[GPPSignIn sharedInstance] authentication];
}

-(bool)isLoggedIn {
    return ([AmazonSharedPreferences userName] != nil && _wif != nil);
}

-(void)initClients {
    if (_wif != nil) {
        _s3  = [[AmazonS3Client alloc] initWithCredentialsProvider:_wif];
    }
}

-(void)wipeAllCredentials {
    @synchronized(self)
    {
        _s3  = nil;
        [AmazonSharedPreferences wipe];
    }
}

- (void)initGPlusLogin {
    HFLogDebug(@"AmazonClientManager.initGPlusLogin");
    
    _signIn = [[GPPSignIn alloc] init];
    _signIn.delegate = self;

    _signIn.shouldFetchGooglePlusUser = YES;
    _signIn.shouldFetchGoogleUserEmail = YES;
    _signIn.shouldFetchGoogleUserID = YES;
    
    _signIn.clientID = GOOGLE_CLIENT_ID;
    _signIn.scopes = @[@"profile"];
}

- (void)initSharedGPlusLogin {
    HFLogDebug(@"AmazonClientManager.initSharedGPlusLogin");
    
    GPPSignIn *signIn = [GPPSignIn sharedInstance];
    signIn.delegate = self;
    
    signIn.shouldFetchGooglePlusUser = YES;
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.shouldFetchGoogleUserID = YES;
    
    signIn.clientID = GOOGLE_CLIENT_ID;
    signIn.scopes = @[@"profile"];
}

- (BOOL)silentGPlusLogin {
    HFLogDebug(@"AmazonClientManager.silentGPlusLogin");
    
    [self initGPlusLogin];
    return [_signIn trySilentAuthentication];
}

- (BOOL)silentSharedGPlusLogin {
    HFLogDebug(@"AmazonClientManager.silentSharedGPlusLogin");
    
    [self initSharedGPlusLogin];
    return [[GPPSignIn sharedInstance] trySilentAuthentication];
}

- (void)reloadGSession {
    HFLogDebug(@"AmazonClientManager.reloadGSession");
    
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:[NSURL URLWithString:@"profile"]];
    
    [_auth authorizeRequest:request
                     completionHandler:^(NSError *error) {
                         if (error == nil) {
                             [self completeGLogin];
                         }
                         else {
                             if ([self.amazonClientManagerGoogleAccountDelegate respondsToSelector:@selector(googleSignInComplete:withError:)]) {
                                 [self.amazonClientManagerGoogleAccountDelegate googleSignInComplete:FALSE withError:error];
                             }
                         }
                     }];
}

- (void)finishedWithAuth:(GTMOAuth2Authentication *)auth error:(NSError *)error {
    HFLogDebug(@"AmazonClientManager.finishedWithAuth: auth=%@", auth);
    
    _auth = auth;
    
    if (error != nil) {
        if ([self.amazonClientManagerGoogleAccountDelegate respondsToSelector:@selector(googleSignInComplete:withError:)]) {
            [self.amazonClientManagerGoogleAccountDelegate googleSignInComplete:FALSE withError:error];
        }
    }
    else {
        if (_auth.accessToken == nil) {
            [self reloadGSession];
        }
        else {
            [self completeGLogin];
        }
    }
}

- (void)completeGLogin {
    HFLogDebug(@"AmazonClientManager.completeGLogin");
    
    NSString *idToken = [_auth.parameters objectForKey:@"id_token"];
    
    _wif = [[AmazonWIFCredentialsProvider alloc] initWithRole:GOOGLE_ROLE_ARN
                                          andWebIdentityToken:idToken
                                                 fromProvider:nil];
    
    // If we have an id, we are logged in
    if (_wif.subjectFromWIF != nil) {
        [AmazonSharedPreferences storeUserName:_wif.subjectFromWIF];
        [AmazonSharedPreferences storeUserEmail:_signIn ? _signIn.userEmail : [GPPSignIn sharedInstance].userEmail];
        
        HFLogDebug(@"AmazonClientManager.completeGLogin: userID: %@, userEmail: %@", _wif.subjectFromWIF, _signIn.userEmail);
        
        [self initClients];
        
        if ([self.amazonClientManagerGoogleAccountDelegate respondsToSelector:@selector(googleSignInComplete:withError:)]) {
            [self.amazonClientManagerGoogleAccountDelegate googleSignInComplete:TRUE withError:nil];
        }
    }
    else {
        if ([self.amazonClientManagerGoogleAccountDelegate respondsToSelector:@selector(googleSignInComplete:withError:)]) {
            NSError *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Unable to assume role. Please check logs for error"}];
            [self.amazonClientManagerGoogleAccountDelegate googleSignInComplete:FALSE withError:error];
        }
    }
}

- (void)disconnectFromSharedGoogle {
    HFLogDebug(@"AmazonClientManager.disconnectFromSharedGoogle");
    
    [[GPPSignIn sharedInstance] disconnect];
}

- (void)didDisconnectWithError:(NSError *)error {
    HFLogDebug(@"AmazonClientManager.didDisconnectWithError: error=%@", error);
    
    if ([self.amazonClientManagerGoogleAccountDelegate respondsToSelector:@selector(googleDisconnectComplete:)]) {
        [self.amazonClientManagerGoogleAccountDelegate googleDisconnectComplete:(error == nil)];
    }
}

@end
