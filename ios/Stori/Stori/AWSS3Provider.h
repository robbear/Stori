//
//  AWSS3Provider.h
//  Stori
//
//  Created by Rob Bearman on 3/14/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "AmazonClientManager.h"

typedef void (^AWSS3ProviderBlockType)(void);
@protocol AWSS3ProviderDelegate;

@interface AWSS3Provider : NSObject <AmazonClientManagerGoogleAccountDelegate, AmazonServiceRequestDelegate>

- (void)initializeProvider:(NSString *)userUuid withDelegate:(id<AWSS3ProviderDelegate>)delgate;
- (void)getStoriItemsAsync;
- (void)deleteVirtualDirectoryAsync:(NSString *)directoryName;
- (void)uploadFile:(NSString *)folder withFileName:(NSString *)fileName withType:(NSString *)contentType;
- (void)uploadDirectoryEntry:(NSString *)folder withTitle:(NSString *)title withCount:(int)count;

@property (nonatomic, copy) AWSS3ProviderBlockType awsS3ProviderBlock;
@property (nonatomic, weak) id<AWSS3ProviderDelegate> awsS3ProviderDelegate;
-(void)executeAWSS3ProviderBlock;

@end

@protocol AWSS3ProviderDelegate <NSObject>

- (void)getStoriItemsComplete:(NSArray *)arrayItems;
- (void)deleteVirtualDirectoryComplete;

@end
