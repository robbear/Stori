//
//  PlayStoriNotifier.m
//  Stori
//
//  Created by Rob Bearman on 4/25/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "PlayStoriNotifier.h"

@implementation PlayStoriNotifier

+ (PlayStoriNotifier *)sharedInstance {
    static PlayStoriNotifier *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[PlayStoriNotifier alloc] init];
        sharedInstance.userUuid = nil;
        sharedInstance.slideShareName = nil;
    });
    
    return sharedInstance;
}

- (void)reset {
    self.userUuid = nil;
    self.slideShareName = nil;
}

- (BOOL)hasPendingRequest {
    return ((self.userUuid != nil) && (self.slideShareName != nil));
}

- (void)notifyPlayRequestForUserId:(NSString *)userUuid withStori:(NSString *)slideShareName {
    HFLogDebug(@"PlayStoriNotifier.notifyPlayRequestForUserId:%@ withStori:%@", userUuid, slideShareName);
    
    self.userUuid = userUuid;
    self.slideShareName = slideShareName;
    
    if (self.delegate) {
        if ([self.delegate respondsToSelector:@selector(onPlayRequestedForUserId:withStori:)]) {
            [self.delegate onPlayRequestedForUserId:userUuid withStori:slideShareName];
        }
    }
}

@end
