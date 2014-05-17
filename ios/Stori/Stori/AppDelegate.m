//
//  AppDelegate.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AppDelegate.h"
#import <GooglePlus/GooglePlus.h>
#import "SlideShareJSON.h"
#import "STOPreferences.h"
#import "STOUtilities.h"
#import "PlayStoriNotifier.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    HFLogDebug(@"AppDelegate.didFinishLaunchingWithOptions");
    
    // Initialize user preference defaults
    [STOPreferences initializeDefaults];
    
    // Override point for customization after application launch.
    return YES;
}
							
- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
    
    HFLogDebug(@"AppDelegate.applicationWillResignActive");
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later. 
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    
    HFLogDebug(@"AppDelegate.applicationDidEnterBackground -- clearing the cache");
    [[NSURLCache sharedURLCache] removeAllCachedResponses];
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
    
    HFLogDebug(@"AppDelegate.applicationWillEnterForeground");
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    
    HFLogDebug(@"AppDelegate.applicationDidBecomeActive");

    // Set up the audio session
    [STOUtilities configureAudioSession];    
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    
    HFLogDebug(@"AppDelegate.applicationWillTerminate");
}

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    HFLogDebug(@"AppDelegate.application:openURL:%@ sourceApplication:%@ annotation", [url absoluteString], sourceApplication);

    if ([[url scheme] isEqualToString:@"stori-app"]) {
        HFLogDebug(@"-- handling url scheme: %@", [url scheme]);
        
        NSString *userUuid = nil;
        NSString *slideShareName = nil;
        
        BOOL success = [STOUtilities parseSharedStoriUrl:url returningUserId:&userUuid returningStori:&slideShareName];
        if (!success) {
            return FALSE;
        }
        
        HFLogDebug(@"-- got userUuid=%@ and slideShareName=%@", userUuid, slideShareName);
        [[PlayStoriNotifier sharedInstance] notifyPlayRequestForUserId:userUuid withStori:slideShareName];
        
        return TRUE;
    }
    
    return [GPPURLHandler handleURL:url sourceApplication:sourceApplication annotation:annotation];
}

@end
