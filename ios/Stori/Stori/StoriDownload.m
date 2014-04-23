//
//  StoriDownload.m
//  Stori
//
//  Created by Rob Bearman on 4/22/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "StoriDownload.h"
#import "SlideShareJSON.h"
#import "SlideJSON.h"
#import "STOUtilities.h"
#import "STOPreferences.h"

@interface StoriDownload ()
@property (strong, nonatomic) NSString *slideShareName;
@property (strong, nonatomic) NSString *userUuid;
@property (weak, nonatomic) id<StoriDownloadDelegate> delegate;
@property (nonatomic) BOOL forEdit;
@property (strong, nonatomic) SlideShareJSON *ssj;
@property (nonatomic) int resourcesToDownload;
@property (nonatomic) int resourcesDownloaded;
@property (nonatomic) BOOL downloadFailed;
@end

@implementation StoriDownload

- (id)initWithDelegate:(id<StoriDownloadDelegate>)delegate {
    self = [super init];
    if (self) {
        self.delegate = delegate;
    }
    
    return self;
}

- (void)startDownload:(NSString *)userUuid withName:(NSString *)slideShareName downloadIsForEdit:(BOOL)downloadIsForEdit {
    //
    // First, check to see if we've already got this Stori ready for play in the case that downloadIsForEdit is false.
    //
    if (!downloadIsForEdit) {
        NSString *playVal = [STOPreferences getPlaySlidesName];
        if (playVal && [playVal isEqualToString:slideShareName]) {
            HFLogDebug(@"StoriDownload.startDownload - we already have this Stori, ready to play. Notifying the delegate.");
            dispatch_async(dispatch_get_main_queue(), ^{
                if ([self.delegate respondsToSelector:@selector(didFinishWithSuccess:withName:)]) {
                    [self.delegate didFinishWithSuccess:TRUE withName:slideShareName];
                }
            });
            
            return;
        }
    }

    // Delete the current slide share directory
    NSString *slideShareNameToDelete = downloadIsForEdit ? [STOPreferences getEditPlayName] : [STOPreferences getPlaySlidesName];
    [STOUtilities deleteSlideShareDirectory:slideShareNameToDelete];    
    
    //
    // Initialize instance variables
    //
    self.userUuid = userUuid;
    self.slideShareName = slideShareName;
    self.forEdit = downloadIsForEdit;
    self.resourcesToDownload = -1;
    self.resourcesDownloaded = -2;
    self.downloadFailed = FALSE;
    self.ssj = nil;
    
    NSString *urlString = [NSString stringWithFormat:@"%@%@/%@/%@", [Constants baseAWSStorageURL], self.userUuid, self.slideShareName, SLIDESHARE_JSON_FILENAME];
    NSURL *url = [[NSURL alloc] initWithString:urlString];
    
    NSURL *pathUrl = [STOUtilities createOrGetSlideShareDirectory:self.slideShareName];
    
    TCBlobDownloadManager *manager = [TCBlobDownloadManager sharedInstance];
    [manager startDownloadWithURL:url customPath:[pathUrl path] delegate:self];
}

- (void)cancelDownload {
    
}

//
// TCBlobDownloaderDelegate methods
//

- (void)download:(TCBlobDownloader *)blobDownload didReceiveFirstResponse:(NSURLResponse *)response {
    //HFLogDebug(@"StoriDownload.download:didReceiveFirstResponse");
}

- (void)download:(TCBlobDownloader *)blobDownload didReceiveData:(uint64_t)receivedLength onTotal:(uint64_t)totalLength {
    //HFLogDebug(@"StoriDownload.download:didReceiveData");
}

- (void)download:(TCBlobDownloader *)blobDownload didStopWithError:(NSError *)error {
    HFLogDebug(@"StoriDownload.download:didStopWithError:%@", error);
    
    self.downloadFailed = TRUE;
    
    if (!self.ssj || (self.resourcesToDownload > self.resourcesDownloaded)) {
        HFLogDebug(@"StoriDownload.download:didStopWithError - removing all files");
        [[TCBlobDownloadManager sharedInstance] cancelAllDownloadsAndRemoveFiles:YES];
    }
    
    if ([self.delegate respondsToSelector:@selector(download:didStopWithError:)]) {
        [self.delegate download:blobDownload.fileName didStopWithError:error];
    }
}

- (void)download:(TCBlobDownloader *)blobDownload didFinishWithSuccess:(BOOL)downloadFinished atPath:(NSString *)pathToFile {
    HFLogDebug(@"StoriDownload.download.didFinishWithSuccess:downloadFinished=%d atPath:%@", downloadFinished, pathToFile);
    
    if (self.downloadFailed) {
        HFLogDebug(@"StoriDownload.download:didFinishWithSuccess - self.downloadFailed is TRUE, so ignoring");
        return;
    }
    
    if (self.ssj) {
        self.resourcesDownloaded++;
    }

    if (self.ssj && (self.resourcesDownloaded == self.resourcesToDownload)) {
        HFLogDebug(@"StoriDownload.download:didFinishWithSuccess - all done!");
        if ([self.delegate respondsToSelector:@selector(didFinishWithSuccess:withName:)]) {
            [self.delegate didFinishWithSuccess:downloadFinished withName:self.slideShareName];
        }
        
        return;
    }
    
    if (!self.ssj) {
        self.resourcesToDownload = 0;
        self.resourcesDownloaded = 0;
        
        NSMutableArray *arrayResources = [[NSMutableArray alloc] init];
        
        self.ssj = [SlideShareJSON loadFromFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
        for (int i = 0; i < [self.ssj getSlideCount]; i++) {
            SlideJSON *sj = [self.ssj getSlideAtIndex:i];
            
            NSString *imageUrl = [sj getImageUrlString];
            NSString *audioUrl = [sj getAudioUrlString];
            
            if (imageUrl) {
                self.resourcesToDownload++;
                [arrayResources addObject:imageUrl];
            }
            if (audioUrl) {
                self.resourcesToDownload++;
                [arrayResources addObject:audioUrl];
            }
        }
        
        HFLogDebug(@"StoriDownload -- beginning download of image/audio resources");
        for (int i = 0; i < [arrayResources count]; i++) {
            NSURL *url = [[NSURL alloc] initWithString:(NSString *)[arrayResources objectAtIndex:i]];
            
            NSURL *pathUrl = [STOUtilities createOrGetSlideShareDirectory:self.slideShareName];
            
            TCBlobDownloadManager *manager = [TCBlobDownloadManager sharedInstance];
            [manager startDownloadWithURL:url customPath:[pathUrl path] delegate:self];
        }
    }
}

@end
