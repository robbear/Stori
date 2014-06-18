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
+ (NSArray *)getStoriItemsHelper:(NSString *)userUuid withError:(NSError **)error;
+ (void)deleteVirtualDirectoryHelper:(NSString *)userUuid withFolder:(NSString *)directoryName withError:(NSError **)error;
+ (void)uploadFile:(NSString *)userUuid withFolder:(NSString *)folder withFileName:(NSString *)fileName withContentType:(NSString *)contentType withError:(NSError **)error;
+ (void)uploadDirectoryEntry:(NSString *)userUuid withFolder:(NSString *)slideShareName withTitle:(NSString *)title withCount:(int)count withError:(NSError **)error;
@end

AmazonClientManager *_amazonClientManager;

@implementation AWSS3Provider

NSString *_userUuid;

- (void)silentLogin {
    HFLogDebug(@"AWSS3Provider.silentLogin");
    
    _amazonClientManager.amazonClientManagerGoogleAccountDelegate = self;
    if (![_amazonClientManager silentGPlusLogin]) {
        HFLogDebug(@"AWSS3Provider.slientLogin silentGPlusLogin failed");
        NSError *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Failed to sign in to Google"}];
        [self googleSignInComplete:FALSE withError:error];
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

+ (NSArray *)getStoriItemsHelper:(NSString *)userUuid withError:(NSError *__autoreleasing *)error {
    HFLogDebug(@"AWSS3Provider.getStoriItemsHelper");
    
    *error = nil;

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
        
        if (!response) {
            *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Failed call to listObjects"}];
            return objects;
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
        *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": [NSString stringWithFormat:@"Exception: %@", exception.message]}];
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
            
            NSError *error;
            objects = [AWSS3Provider getStoriItemsHelper:userUuid withError:&error];
           
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(getStoriItemsComplete:withError:)]) {
                    [awsS3ProviderDelegate getStoriItemsComplete:objects withError:error];
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
    
    int index = (int)(range.location + range.length);
    NSString *partial = [urlString substringFromIndex:index];
    
    range = [partial rangeOfString:@"/"];
    if (range.location == NSNotFound || range.location <= 0) {
        index = (int)[partial length];
    }
    else {
        index = (int)range.location;
    }

    range.location = 0;
    range.length = index;
    return [partial substringWithRange:range];
}

+ (void)deleteVirtualDirectoryHelper:(NSString *)userUuid withFolder:(NSString *)directoryName withError:(NSError *__autoreleasing *)error {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectoryHelper: userUuid=%@, directoryName=%@", userUuid, directoryName);
    
    *error = nil;
    
    @try {
        NSString *prefix = [NSString stringWithFormat:@"%@/%@", userUuid, directoryName];
        
        S3ListObjectsRequest *request = [[S3ListObjectsRequest alloc] initWithName:BUCKET_NAME];
        request.prefix = prefix;
        
        S3ListObjectsResponse *response = [[_amazonClientManager s3] listObjects:request];
        S3ListObjectsResult *results = response.listObjectsResult;
        
        if (!response) {
            *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Failed call to listObjects"}];
            return;
        }
        
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
        *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": [NSString stringWithFormat:@"Exception: %@", exception.message]}];
    }
}

- (void)deleteVirtualDirectoryAsync:(NSString *)directoryName {
    HFLogDebug(@"AWSS3Provider.deleteVirtualDirectory: directoryName=%@", directoryName);
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            NSError *error;
            [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:directoryName withError:&error];
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(deleteVirtualDirectoryComplete:)]) {
                    [awsS3ProviderDelegate deleteVirtualDirectoryComplete:error];
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
            NSArray *returnedObjects = nil;
            
            NSError *error;
            for (StoriListItem *sli in arrayItems) {
                [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:sli.slideShareName withError:&error];
                if (error) {
                    break;
                }
            }
            
            if (!error) {
                returnedObjects = [AWSS3Provider getStoriItemsHelper:userUuid withError:&error];
            }
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(deleteStoriItemsAndReturnItemsComplete:withError:)]) {
                    [awsS3ProviderDelegate deleteStoriItemsAndReturnItemsComplete:returnedObjects withError:error];
                }
            });
        });
    }];
    
    [self silentLogin];
}

+ (void)uploadDirectoryEntry:(NSString *)userUuid withFolder:(NSString *)folder withTitle:(NSString *)title withCount:(int)count withError:(NSError *__autoreleasing *)error {
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry");
    
    *error = nil;

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
    if (response == nil) {
        *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Call to putObject failed"}];
    }
    HFLogDebug(@"AWSS3Provider.uploadDirectoryEntry: response=%@", response);
}

+ (void)uploadFile:(NSString *)userUuid withFolder:(NSString *)folder withFileName:(NSString *)fileName withContentType:(NSString *)contentType withError:(NSError *__autoreleasing *)error {
    HFLogDebug(@"AWSS3Provider.uploadFile: userUuid=%@, folder=%@, fileName=%@, contentType=%@", userUuid, folder, fileName, contentType);
    
    *error = nil;

    NSString *relPath = [NSString stringWithFormat:@"%@/%@/%@", userUuid, folder, fileName];
    NSURL *fileURL = [STOUtilities getAbsoluteFilePathWithFolder:folder withFileName:fileName];
    NSString *filePath = [fileURL path];
    
    S3PutObjectRequest *por = [[S3PutObjectRequest alloc] initWithKey:relPath inBucket:BUCKET_NAME];
    [por setCacheControl:@"If-None-Match"];
    [por setContentType:contentType];
    
    // Note: the iOS AWS S3 SDK will determine content-type from the file extension,
    // regardless of our setting the contentType explicitly. We look for the file extension
    // of 3gp and load the data into memory rather than using setFilename, only for audio files.
    // Without doing so, S3 stores 3gp files with the mime-type of application/octet-stream
    // which causes audio havoc on IE/Firefox.
    
    NSString *ext = [filePath pathExtension];
    
    if ([ext isEqualToString:AUDIO_FILE_EXTENSION]) {
        [por setData:[NSData dataWithContentsOfFile:filePath]];
    }
    else {
        [por setFilename:filePath];
    }
    
    S3PutObjectResponse *response = [[_amazonClientManager s3] putObject:por];
    if (response == nil) {
        *error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": @"Call to putObject failed"}];
    }
    HFLogDebug(@"AWSS3Provider.uploadFile: response=%@", response);
}

- (void)uploadAsync:(NSString *)folder {
    HFLogDebug(@"AWSS3Provider.uploadAsync: folder=%@", folder);
    
    NSString *userUuid = _userUuid;
    id<AWSS3ProviderDelegate> awsS3ProviderDelegate = self.awsS3ProviderDelegate;
    
    [self setAwsS3ProviderBlock:^{
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^(void){
            SlideShareJSON *ssj = [SlideShareJSON loadFromFolder:folder withFileName:SLIDESHARE_JSON_FILENAME];
            
            NSError *error = nil;
            
            if (ssj) {
                NSArray *imageFileNames = [ssj getImageFileNames];
                NSArray *audioFileNames = [ssj getAudioFileNames];
                NSString *title = [ssj getTitle];
                int count = [ssj getSlideCount];
                
                [AWSS3Provider deleteVirtualDirectoryHelper:userUuid withFolder:folder withError:&error];

                if (!error) {
                    @try {
                        for (NSString *fileName in imageFileNames) {
                            [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:fileName withContentType:@"image/jpeg" withError:&error];
                            if (error) {
                                break;
                            }
                        }
                        if (!error) {
                            for (NSString *fileName in audioFileNames) {
                                [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:fileName withContentType:@"audio/mp4" withError:&error];
                                if (error) {
                                    break;
                                }
                            }
                        }
                        
                        if (!error) {
                            [AWSS3Provider uploadFile:userUuid withFolder:folder withFileName:SLIDESHARE_JSON_FILENAME withContentType:@"application/json" withError:&error];
                            
                            if (!error) {
                                [AWSS3Provider uploadDirectoryEntry:userUuid withFolder:folder withTitle:title withCount:count withError:&error];
                            }
                        }
                    }
                    @catch (AmazonClientException *exception) {
                        HFLogDebug(@"AWSS3Provider.uploadFilesAsync: exception = %@", exception);
                        error = [[NSError alloc] initWithDomain:@"stori" code:-1 userInfo:@{@"description": [NSString stringWithFormat:@"Exception: %@", exception.message]}];
                    }
                }
            }
            
            dispatch_async(dispatch_get_main_queue(), ^(void){
                if ([awsS3ProviderDelegate respondsToSelector:@selector(uploadComplete:)]) {
                    [awsS3ProviderDelegate uploadComplete:error];
                }
            });
        });
    }];
    
    [self silentLogin];
}

- (void)googleSignInComplete:(BOOL)success withError:(NSError *)error {
    HFLogDebug(@"AWSS3Provider.googleSignInComplete: success=%d", success);
    
    // We ignore the error and let the AWS call fail and report
    [self executeAWSS3ProviderBlock];
 }

- (void)googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"AWSS3Provider.googleDisconnectComplete: success=%d", success);
}

@end
