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

    id<AWSS3ProviderDelegate> delegate = self.awsS3ProviderDelegate;
    [self setAwsS3ProviderBlock:^{
        NSArray *arrayItems = [AWSS3Provider getStoriItems:_userUuid];

        if ([delegate respondsToSelector:@selector(getStoriItemsComplete:)]) {
            [delegate getStoriItemsComplete:arrayItems];
        }
    }];
    
    [self silentLogin];
}

+ (NSArray *)getStoriItems:(NSString *)userUuid {
    HFLogDebug(@"AWSS3Provider.getStoriItems");
    
    NSArray *returnObjects = nil;
    
    @try {
        S3ListObjectsRequest  *listObjectRequest = [[S3ListObjectsRequest alloc] initWithName:@"hfstori"];
        listObjectRequest.prefix = [NSString stringWithFormat:@"%@/", userUuid];
        
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
        
        returnObjects = objects;
        
        HFLogDebug(@"AWSS3Provider.getStoriItems - found %d S3 objects under %@", [returnObjects count], userUuid);
    }
    @catch (AmazonClientException *exception)
    {
        HFLogDebug(@"Exception = %@", exception);
        [[Constants errorAlert:[NSString stringWithFormat:@"Error list objects: %@", exception.message]] show];
    }

    return returnObjects;
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
@end
