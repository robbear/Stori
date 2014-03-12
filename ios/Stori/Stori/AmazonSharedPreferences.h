//
//  AmazonSharedPreferences.h
//  Stori
//
//  Created by Rob Bearman on 3/12/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AWSRuntime/AWSRuntime.h>
#import <AWSSecurityTokenService/AWSSecurityTokenService.h>

@interface AmazonSharedPreferences : NSObject {
}

+(void)storeUserName:(NSString *)userName;
+(NSString *)userName;

+(void)storeUserEmail:(NSString *)userEmail;
+(NSString *)userEmail;

+(void)wipe;

@end
