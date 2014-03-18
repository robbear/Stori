//
//  AWSS3Provider.m
//  Stori
//
//  Created by Rob Bearman on 3/14/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AWSS3Provider.h"
#import "StoriListItem.h"

@interface AWSS3Provider()
- (void)onGetStoriItemsComplete:(AmazonServiceRequest *)request withResponse:(AmazonServiceResponse *)response withException:(NSException *)exception;
- (NSString *)getStringSegmentFromManifestUrlString:(NSString *)urlString withSegment:(NSString *)segment;
@end

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
        
        S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
        request.prefix = [NSString stringWithFormat:@"%@/%@", userUuid, DIRECTORY_ENTRY_SEGMENT_STRING];
        request.delegate = amazonServiceRequestDelegate;
        request.requestTag = @"getStoriItemsAsync";
        
        @try {
            [[[AmazonClientManager sharedInstance] s3] listObjects:request];
        }
        @catch (AmazonClientException *exception) {
            HFLogDebug(@"AWSS3Provider.getStoriItemsAsync: exception = %@", exception);
            //[[Constants errorAlert:[NSString stringWithFormat:@"Error list objects: %@", exception.message]] show];
            
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
            int slideCount = 0;
            NSString *key = [objectSummary key];
            NSString *slideShareName = [self getStringSegmentFromManifestUrlString:key withSegment:DIRECTORY_ENTRY_SEGMENT_STRING];
            NSString *title = [self getStringSegmentFromManifestUrlString:key withSegment:TITLE_SEGMENT_STRING];
            NSString *decodedTitle = nil;
            if (title) {
                decodedTitle = [title urlDecode];
            }
            NSString *countString = [self getStringSegmentFromManifestUrlString:key withSegment:SLIDE_COUNT_SEGMENT_STRING];
            if (countString) {
                slideCount = countString.intValue;
            }
            NSString *dateString = objectSummary.lastModified;
            
            StoriListItem *sli = [[StoriListItem alloc] initWithSlideShareName:slideShareName withTitle:decodedTitle withDate:dateString withCount:slideCount];
            [objects addObject:sli];
        }
    }
    else {
        HFLogDebug(@"AWSS3Provider.onGetStoriItemsComplete: exception=%@", exception.debugDescription);
    }

    HFLogDebug(@"AWSS3Provider.onGetStoriItemsComplete - found %d S3 objects under %@", [objects count], _userUuid);

    if ([self.awsS3ProviderDelegate respondsToSelector:@selector(getStoriItemsComplete:)]) {
        [self.awsS3ProviderDelegate getStoriItemsComplete:objects];
    }
}

- (NSString *)getStringSegmentFromManifestUrlString:(NSString *)urlString withSegment:(NSString *)segment {
    NSRange range = [urlString rangeOfString:segment];
    if (range.location == NSNotFound || range.location <= 0) {
        return nil;
    }
    
    int index = range.location + range.length;
    NSString *partial = [urlString substringFromIndex:index];
    
    range = [partial rangeOfString:@"/"];
    if (range.location == NSNotFound || range.location <= 0) {
        index = [partial length];
    }
    else {
        index = range.location;
    }

    range.location = 0;
    range.length = index;
    return [partial substringWithRange:range];
}

- (void)deleteVirtualDirectoryAsync:(NSString *)directoryName {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectory: directoryName=%@", directoryName);
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            //Background Thread
            @try {
                NSString *prefix = [NSString stringWithFormat:@"%@/%@", userUuid, directoryName];

                S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
                request.prefix = prefix;
                
                S3ListObjectsResponse *response = [[[AmazonClientManager sharedInstance] s3] listObjects:request];
                S3ListObjectsResult *results = response.listObjectsResult;
                
                for (S3ObjectSummary *summary in results.objectSummaries) {
                    [[[AmazonClientManager sharedInstance] s3] deleteObjectWithKey:summary.key withBucket:BUCKET_NAME];
                }
                
                //
                // Delete the directory entry
                //
                
                prefix = nil;
                request = nil;
                response = nil;
                results = nil;
                prefix = [NSString stringWithFormat:@"%@/%@%@", userUuid, DIRECTORY_ENTRY_SEGMENT_STRING, directoryName];
                request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
                request.prefix = prefix;
                
                response = [[[AmazonClientManager sharedInstance] s3] listObjects:request];
                results = response.listObjectsResult;
                
                for (S3ObjectSummary *summary in results.objectSummaries) {
                    [[[AmazonClientManager sharedInstance] s3] deleteObjectWithKey:summary.key withBucket:BUCKET_NAME];
                }
            }
            @catch (AmazonClientException *exception) {
                HFLogDebug(@"AWSS3Provider.deleteVirtualDirectory: exception = %@", exception);
            }
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(deleteVirtualDirectoryComplete)]) {
                    [awsS3ProviderDelegate deleteVirtualDirectoryComplete];
                }
            });
        });
    }];

    [self silentLogin];
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
