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

- (void)silentLogin {
    HFLogDebug(@"AWSS3Provider.silentLogin");
    
    [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
    if (![[AmazonClientManager sharedInstance] silentGPlusLogin]) {
        HFLogDebug(@"AWSS3Provider.slientLogin silentGPlusLogin failed");
        [self googleSignInComplete:FALSE];
    }
}

-(void)executeAWSS3ProviderBlock {
    HFLogDebug(@"AWSS3Provider.executeAWSS3ProviderBlock");
    
    if ([self awsS3ProviderBlock]) {
        HFLogDebug(@"AWSS3Provider.executeAWSS3ProviderBlock - executing");
        [self awsS3ProviderBlock]();
        self.awsS3ProviderBlock = nil;
    }
}

- (void)initializeProvider:(NSString *)userUuid withDelegate:(id<AWSS3ProviderDelegate>)delgate {
    HFLogDebug(@"AWSS3Provider.initializeProvider: userUuid=%@", userUuid);
    
    _userUuid = userUuid;
    self.awsS3ProviderDelegate = delgate;
}

- (void)getStoriItemsAsync {
    HFLogDebug(@"AWSS3Provider.getStoriItemsAsync");
    
    id<AmazonServiceRequestDelegate> amazonServiceRequestDelegate = self;
    NSString *userUuid = _userUuid;
    [self setAwsS3ProviderBlock:^{
        HFLogDebug(@"AWSS3Provider.getStoriItemsBlock");
        
        S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:@"hfstori"];
        request.prefix = [NSString stringWithFormat:@"%@/", userUuid];
        request.delegate = amazonServiceRequestDelegate;
        request.requestTag = @"getStoriItemsAsync";
        
        @try {
            [[[AmazonClientManager sharedInstance] s3] listObjects:request];
        }
        @catch (AmazonClientException *exception) {
            HFLogDebug(@"Exception = %@", exception);
            [[Constants errorAlert:[NSString stringWithFormat:@"Error list objects: %@", exception.message]] show];
            
            // TODO: handle error correctly
        }
    }];
    
    [self silentLogin];
}

- (void)onGetStoriItemsComplete:(AmazonServiceRequest *)request withResponse:(AmazonServiceResponse *)response withException:(NSException *)exception {
    HFLogDebug(@"AWSS3Provider.onGetStoriItemsComplete");
    
    NSMutableArray *objects;

    if (!exception) {
        S3ListObjectsResponse *listObjectResponse = (S3ListObjectsResponse *)response;
        S3ListObjectsResult   *listObjectsResults = listObjectResponse.listObjectsResult;
    
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
    }
    else {
        HFLogDebug(@"AWSS3Provider.onGetStoriItemsComplete: exception=%@", exception.debugDescription);
    }

    HFLogDebug(@"AWSS3Provider.onGetStoriItemsComplete - found %d S3 objects under %@", [objects count], _userUuid);

    if ([self.awsS3ProviderDelegate respondsToSelector:@selector(getStoriItemsComplete:)]) {
        [self.awsS3ProviderDelegate getStoriItemsComplete:objects];
    }
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
    
    [self executeAWSS3ProviderBlock];
 }

- (void)googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleDisconnectComplete: success=%d", success);
}

//
// AmazonRequestService delegate calls
//
- (void)request:(AmazonServiceRequest *)request didCompleteWithResponse:(AmazonServiceResponse *)response {
    HFLogDebug(@"AWSS3Provider.request:didCompleteWithResponse");

    if ([request.requestTag isEqualToString:@"getStoriItemsAsync"]) {
        [self onGetStoriItemsComplete:request withResponse:response withException:nil];
    }
    else {
        HFLogDebug(@"AWSS3Provider.request:DidCompleteWithResponse - no matching request found");
    }
}

- (void)request:(AmazonServiceRequest *)request didFailWithServiceException:(NSException *)exception {
    HFLogDebug(@"AWSS3Provider.request:didFailWithServiceException: %@", exception);

    if ([request.requestTag isEqualToString:@"getStoriItemsAsync"]) {
        [self onGetStoriItemsComplete:request withResponse:nil withException:exception];
    }
    else {
        HFLogDebug(@"AWSS3Provider.request:DidCompleteWithResponse - no matching request found");
    }
}

@end
