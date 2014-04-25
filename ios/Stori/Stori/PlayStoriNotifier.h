//
//  PlayStoriNotifier.h
//  Stori
//
//  Created by Rob Bearman on 4/25/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol PlayStoriNotifierDelegate <NSObject>
- (void)onPlayRequestedForUserId:(NSString *)userUuid withStori:(NSString *)slideShareName;
@end

@interface PlayStoriNotifier : NSObject
@property (strong, nonatomic) NSString *userUuid;
@property (strong, nonatomic) NSString *slideShareName;
@property (weak, nonatomic) id<PlayStoriNotifierDelegate> delegate;

- (void)notifyPlayRequestForUserId:(NSString *)userUuid withStori:(NSString *)slideShareName;
- (void)reset;
- (BOOL)hasPendingRequest;
+ (PlayStoriNotifier *)sharedInstance;
@end
