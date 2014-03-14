//
//  AWSS3Provider.m
//  Stori
//
//  Created by Rob Bearman on 3/14/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AWSS3Provider.h"

@implementation AWSS3Provider

NSString *_userUuid;

- (void)initializeProvider:(NSString *)userUuid {
    HFLogDebug(@"AWSS3Provider.initializeProvider: userUuid=%@", userUuid);
    
    _userUuid = userUuid;

#if NEVER
    GPPSignIn *signIn = [GPPSignIn sharedInstance];
    signIn.shouldFetchGooglePlusUser = YES;
    signIn.shouldFetchGoogleUserEmail = YES;
    signIn.shouldFetchGoogleUserID = YES;
    signIn.clientID = GOOGLE_CLIENT_ID;
    signIn.scopes = @[kGTLAuthScopePlusLogin];
    
    [signIn trySilentAuthentication];
    
    GTMOAuth2Authentication *auth = [signIn authentication];
    
    if (!auth) {
        HFLogDebug(@"AWSS3Provider.initializeProvider - no authentication object!! Bailing.");
        return;
    }
    
    [[AmazonClientManager sharedInstance] finishedWithAuth:auth error:nil];
#endif
}

- (NSArray *)getStoriItems {
    HFLogDebug(@"AWSS3Provider.getStoriItems");
    
    @try {
        S3ListObjectsRequest  *listObjectRequest = [[S3ListObjectsRequest alloc] initWithName:@"hfstori"];
        listObjectRequest.prefix = [NSString stringWithFormat:@"%@/", _userUuid];
        
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
        
        HFLogDebug(@"AWSS3Provider.getStoriItems - found %d S3 objects under %@", [objects count], _userUuid);
    }
    @catch (AmazonClientException *exception)
    {
        HFLogDebug(@"Exception = %@", exception);
        [[Constants errorAlert:[NSString stringWithFormat:@"Error list objects: %@", exception.message]] show];
    }

    return nil;
}

- (BOOL)deleteVirtualDirectory:(NSString *)directoryName {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectory: directoryName=%@", directoryName);
    
    return FALSE;
}

- (void)uploadFile:(NSString *)folder withFileName:(NSString *)fileName withType:(NSString *)contentType {
    HFLogDebug(@"AWSS3Provider.uploadFile: folder=%@, fileName=%@, contentType=%@", folder, fileName, contentType);
}

- (void)uploadDirectoryEntry:(NSString *)folder withTitle:(NSString *)title withCount:(int)count {
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry: folder=%@, title=%@", folder, title);
}

- (void)googleSignInComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleSignInComplete: success=%d", success);
    
}

- (void)googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleDisconnectComplete: success=%d", success);
}
@end
