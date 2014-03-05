//
//  AppDelegate.m
//  Stori
//
//  Created by Rob Bearman on 3/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AppDelegate.h"
#import "SlideShareJSON.h"

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    HFLogDebug(@"AppDelegate.didFinishLaunchingWithOptions");
    
    [self initialTests];
    
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
    
    HFLogDebug(@"AppDelegate.applicationDidEnterBackground");
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
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    
    HFLogDebug(@"AppDelegate.applicationWillTerminate");
}

//
// Tests
//

- (void)initialTests {
    HFLogDebug(@"Beginning initialTests");
    
    //NSString *slideShareName = [[NSUUID UUID] UUIDString];
    
    // Note: jsonString without keys having quotes will fail to create the ssj.jsonDictionary.
    NSString *jsonString = @"{\"title\":\"My Stori\", \"description\":\"My description\"}";
    HFLogDebug(@"Calling SlideShareJSON.initWithString: jsonString=%@", jsonString);
    SlideShareJSON *ssj = [[SlideShareJSON alloc] initWithString:jsonString];
    HFLogDebug(@"ssj.getJsonDictionary: %@", [ssj toString]);
    
    HFLogDebug(@"Calling SlideShareJSON.init");
    ssj = [[SlideShareJSON alloc] init];
    HFLogDebug(@"ssj.getJsonDictionary: %@", [ssj toString]);
    HFLogDebug(@"Create 5 slides");
    for (int i = 0; i < 5; i++) {
        NSString *slideUuid = [[NSUUID UUID] UUIDString];
        NSString *imageUrl = [NSString stringWithFormat:@"http://stori-app.com/foobar/image%d.jpg", i];
        NSString *audioUrl = [NSString stringWithFormat:@"http://stori-app.com/foobar/audio%d.m4p", i];
        [ssj upsertSlideWithSlideId:slideUuid atIndex:i withImageUrl:imageUrl withAudioUrl:audioUrl withSlideText:nil forceNulls:TRUE];
    }
    HFLogDebug(@"ssj.getJsonDictionary: %@", [ssj toString]);
}

@end
