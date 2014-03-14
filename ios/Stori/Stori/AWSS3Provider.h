//
//  AWSS3Provider.h
//  Stori
//
//  Created by Rob Bearman on 3/14/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "AmazonClientManager.h"

@interface AWSS3Provider : NSObject <AmazonClientManagerGoogleAccountDelegate>

- (void)initializeProvider:(NSString *)userUuid;
- (NSArray *)getStoriItems;
- (BOOL)deleteVirtualDirectory:(NSString *)directoryName;
- (void)uploadFile:(NSString *)folder withFileName:(NSString *)fileName withType:(NSString *)contentType;
- (void)uploadDirectoryEntry:(NSString *)folder withTitle:(NSString *)title withCount:(int)count;

@end
