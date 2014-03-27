//
//  AWSS3Provider.m
//  Stori
//
//  Created by Rob Bearman on 3/14/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AWSS3Provider.h"
#import "StoriListItem.h"
#import "STOUtilities.h"
#import "SlideShareJSON.h"

@interface AWSS3Provider()
+ (NSString *)getStringSegmentFromManifestUrlString:(NSString *)urlString withSegment:(NSString *)segment;
+ (NSArray *)getStoriItemsHelper:(NSString *)userUuid;
+ (void)deleteVirtualDirectoryHelper:(NSString *)userUuid withFolder:(NSString *)directoryName;
+ (void)uploadFile:(NSString *)userUuid withFolder:(NSString *)folder withFileName:(NSString *)fileName withContentType:(NSString *)contentType;
+ (void)uploadDirectoryEntry:(NSString *)userUuid withFolder:(NSString *)slideShareName withTitle:(NSString *)title withCount:(int)count;
@end

AmazonClientManager *_amazonClientManager;

@implementation AWSS3Provider

NSString *_userUuid;

- (void)silentLogin {
    HFLogDebug(@"AWSS3Provider.silentLogin");
    
    _amazonClientManager.amazonClientManagerGoogleAccountDelegate = self;
    if (![_amazonClientManager silentGPlusLogin]) {
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
    
    //
    // Instantiate a per-AWSS3Provider instance of AmazonClientManager.
    // This allows us to perform a per-S3 call reinstantiation of Google Login.
    // At some future point, we will work at managing a persistent cache
    // of the Google auth and refresh token so we don't have to re-signin
    // on each call.
    //
    _amazonClientManager = [[AmazonClientManager alloc] init];
    
    _userUuid = userUuid;
    self.awsS3ProviderDelegate = delgate;
}

+ (NSArray *)getStoriItemsHelper:(NSString *)userUuid {
    HFLogDebug(@"AWSS3Provider.getStoriItemsHelper");
    
    NSMutableArray *objects = nil;
    S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
    request.prefix = [NSString stringWithFormat:@"%@/%@", userUuid, DIRECTORY_ENTRY_SEGMENT_STRING];
    
    @try {
        S3ListObjectsResponse *response = [[_amazonClientManager s3] listObjects:request];
        S3ListObjectsResult   *results = response.listObjectsResult;
        
        if (objects == nil) {
            objects = [[NSMutableArray alloc] initWithCapacity:[results.objectSummaries count]];
        }
        else {
            [objects removeAllObjects];
        }
        
        for (S3ObjectSummary *objectSummary in results.objectSummaries) {
            int slideCount = 0;
            NSString *key = [objectSummary key];
            NSString *slideShareName = [AWSS3Provider getStringSegmentFromManifestUrlString:key withSegment:DIRECTORY_ENTRY_SEGMENT_STRING];
            NSString *title = [AWSS3Provider getStringSegmentFromManifestUrlString:key withSegment:TITLE_SEGMENT_STRING];
            NSString *decodedTitle = nil;
            if (title) {
                decodedTitle = [title urlDecode];
            }
            NSString *countString = [AWSS3Provider getStringSegmentFromManifestUrlString:key withSegment:SLIDE_COUNT_SEGMENT_STRING];
            if (countString) {
                slideCount = countString.intValue;
            }
            NSString *dateString = objectSummary.lastModified;
            
            StoriListItem *sli = [[StoriListItem alloc] initWithSlideShareName:slideShareName withTitle:decodedTitle withDate:dateString withCount:slideCount];
            [objects addObject:sli];
        }
    }
    @catch (AmazonClientException *exception) {
        HFLogDebug(@"AWSS3Provider.getStoriItemsHelper: exception = %@", exception);
    }
    
    return objects;
}

- (void)getStoriItemsAsync {
    HFLogDebug(@"AWSS3Provider.getStoriItemsAsync");
    
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    NSString *userUuid = _userUuid;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            NSArray *objects = nil;
            S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
            request.prefix = [NSString stringWithFormat:@"%@/%@", userUuid, DIRECTORY_ENTRY_SEGMENT_STRING];
            
            objects = [AWSS3Provider getStoriItemsHelper:userUuid];
           
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(getStoriItemsComplete:)]) {
                    [awsS3ProviderDelegate getStoriItemsComplete:objects];
                }
            });
        });
    }];
    
    [self silentLogin];
}

+ (NSString *)getStringSegmentFromManifestUrlString:(NSString *)urlString withSegment:(NSString *)segment {
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

+ (void)deleteVirtualDirectoryHelper:(NSString *)userUuid withFolder:(NSString *)directoryName {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectoryHelper: userUuid=%@, directoryName=%@", userUuid, directoryName);
    
    @try {
        NSString *prefix = [NSString stringWithFormat:@"%@/%@", userUuid, directoryName];
        
        S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
        request.prefix = prefix;
        
        S3ListObjectsResponse *response = [[_amazonClientManager s3] listObjects:request];
        S3ListObjectsResult *results = response.listObjectsResult;
        
        for (S3ObjectSummary *summary in results.objectSummaries) {
            [[_amazonClientManager s3] deleteObjectWithKey:summary.key withBucket:BUCKET_NAME];
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
        
        response = [[_amazonClientManager s3] listObjects:request];
        results = response.listObjectsResult;
        
        for (S3ObjectSummary *summary in results.objectSummaries) {
            [[_amazonClientManager s3] deleteObjectWithKey:summary.key withBucket:BUCKET_NAME];
        }
    }
    @catch (AmazonClientException *exception) {
        HFLogDebug(@"AWSS3Provider.deleteVirtualDirectoryHelper: exception = %@", exception);
    }
}

- (void)deleteVirtualDirectoryAsync:(NSString *)directoryName {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectory: directoryName=%@", directoryName);
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:directoryName];
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(deleteVirtualDirectoryComplete)]) {
                    [awsS3ProviderDelegate deleteVirtualDirectoryComplete];
                }
            });
        });
    }];

    [self silentLogin];
}

- (void)deleteStoriItemsAndReturnItems:(NSArray *)arrayItems {
    HFLogDebug(@"AWSS3Provider.deleteStoriItemsAndReturnItems");
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            NSArray *returnedObjects;
            
            for (StoriListItem *sli in arrayItems) {
                [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:sli.slideShareName];
            }
            
            returnedObjects = [AWSS3Provider getStoriItemsHelper:userUuid];
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(deleteStoriItemsAndReturnItemsComplete:)]) {
                    [awsS3ProviderDelegate deleteStoriItemsAndReturnItemsComplete:returnedObjects];
                }
            });
        });
    }];
    
    [self silentLogin];
}

//
// BUGBUG - Not yet tested
//
+ (void)uploadDirectoryEntry:(NSString *)userUuid withFolder:(NSString *)folder withTitle:(NSString *)title withCount:(int)count {
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry");

    NSString *encodedTitle = [title urlEncode];
    NSString *relPath = [NSString stringWithFormat:@"%@/%@%@/%@%@/%@%d", userUuid, DIRECTORY_ENTRY_SEGMENT_STRING, folder, TITLE_SEGMENT_STRING, encodedTitle, SLIDE_COUNT_SEGMENT_STRING, count];
    
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry: relPath=%@", relPath);
    
    unsigned char buffer[1];
    NSData *data = [NSData dataWithBytes:buffer length:1];

    S3PutObjectRequest *por = [[S3PutObjectRequest alloc] initWithKey:relPath inBucket:BUCKET_NAME];
    [por setCacheControl:@"If-None-Match"];
    [por setContentType:@"application/octet-stream"];
    [por setData:data];
    
    S3PutObjectResponse *response = [[_amazonClientManager s3] putObject:por];
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry: response=%@", response);
}

//
// BUGBUG - Not yet tested
//
+ (void)uploadFile:(NSString *)userUuid withFolder:(NSString *)folder withFileName:(NSString *)fileName withContentType:(NSString *)contentType {
    HFLogDebug(@"AWSS3Provider.uploadFile: userUuid=%@, folder=%@, fileName=%@, contentType=%@", userUuid, folder, fileName, contentType);

    NSString *relPath = [NSString stringWithFormat:@"%@/%@/%@", userUuid, folder, fileName];
    NSURL *fileURL = [STOUtilities getAbsoluteFilePathWithFolder:folder withFileName:fileName];
    NSString *filePath = [fileURL path];
    
    S3PutObjectRequest *por = [[S3PutObjectRequest alloc] initWithKey:relPath inBucket:BUCKET_NAME];
    [por setCacheControl:@"If-None-Match"];
    [por setContentType:contentType];
    [por setFilename:filePath];
    
    S3PutObjectResponse *response = [[_amazonClientManager s3] putObject:por];
    HFLogDebug(@"AWSS3Provider.uploadFile: response=%@", response);
}

//
// BUGBUG - Not yet tested
//
- (void)uploadAsync:(NSString *)folder {
    HFLogDebug(@"AWSS3Provider.uploadAsync: folder=%@", folder);
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            BOOL success = FALSE;
            SlideShareJSON *ssj = [SlideShareJSON loadFromFolder:folder withFileName:SLIDESHARE_JSON_FILENAME];
            
            if (ssj) {
                NSArray *imageFileNames = [ssj getImageFileNames];
                NSArray *audioFileNames = [ssj getAudioFileNames];
                NSString *title = [ssj getTitle];
                int count = [ssj getSlideCount];
                
                [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:folder];

                @try {
                    for (NSString *fileName in imageFileNames) {
                        [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:fileName withContentType:@"image/jpeg"];
                    }
                    for (NSString *fileName in audioFileNames) {
                        [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:fileName withContentType:@"audio/mp4"];
                    }
                    
                    [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:SLIDESHARE_JSON_FILENAME withContentType:@"application/json"];
                    [AWSS3Provider uploadDirectoryEntry:userUuid withFolder:folder withTitle:title withCount:count];
                    
                    success = TRUE;
                }
                @catch (AmazonClientException *exception) {
                    HFLogDebug(@"AWSS3Provider.uploadFilesAsync: exception = %@", exception);
                }
            }
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(uploadComplete:)]) {
                    [awsS3ProviderDelegate uploadComplete:success];
                }
            });
        });
    }];
    
    [self silentLogin];
}

- (void)googleSignInComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleSignInComplete: success=%d", success);
    
    [self executeAWSS3ProviderBlock];
 }

- (void)googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleDisconnectComplete: success=%d", success);
}

@end
