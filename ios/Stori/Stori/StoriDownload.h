//
//  StoriDownload.h
//  Stori
//
//  Created by Rob Bearman on 4/22/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "TCBlobDownload.h"

@protocol StoriDownloadDelegate <NSObject>
- (void)download:(NSString *)urlString didStopWithError:(NSError *)error;
- (void)didFinishWithSuccess:(BOOL)success;
@end

@interface StoriDownload : NSObject <TCBlobDownloaderDelegate>

- (id)initWithDelegate:(id<StoriDownloadDelegate>)delegate;
- (void)startDownload:(NSString *)userUuid withName:(NSString *)slideShareName downloadIsForEdit:(BOOL)downloadIsForEdit;
- (void)cancelDownload;

@end
