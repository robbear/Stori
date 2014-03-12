//
//  AmazonSharedPreferences.m
//  Stori
//
//  Created by Rob Bearman on 3/12/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AmazonSharedPreferences.h"

@implementation AmazonSharedPreferences

+ (void)storeUserName:(NSString *)userName {
    [[NSUserDefaults standardUserDefaults] setObject:userName forKey:AMAZONSHAREDPREFERENCES_USERNAME];
}

+ (NSString *)userName {
    return [[NSUserDefaults standardUserDefaults] stringForKey:AMAZONSHAREDPREFERENCES_USERNAME];
}

+ (void)storeUserEmail:(NSString *)userEmail {
    [[NSUserDefaults standardUserDefaults] setObject:userEmail forKey:AMAZONSHAREDPREFERENCES_USEREMAIL];
}

+ (NSString *)userEmail {
    return [[NSUserDefaults standardUserDefaults] stringForKey:AMAZONSHAREDPREFERENCES_USEREMAIL];
}

+ (void)wipe {
    [[NSUserDefaults standardUserDefaults] setObject:nil forKey:AMAZONSHAREDPREFERENCES_USERNAME];
    [[NSUserDefaults standardUserDefaults] setObject:nil forKey:AMAZONSHAREDPREFERENCES_USEREMAIL];
}

@end
