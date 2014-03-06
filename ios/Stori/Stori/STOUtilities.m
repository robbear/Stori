//
//  STOUtilities.m
//  Stori
//
//  Created by Rob Bearman on 3/6/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOUtilities.h"

@implementation STOUtilities

// Note: Files should be stored in a directory under <Application_Home>/Library/Application Supprt/
// and apply the com.apple.MobileBackup attribute to the subdirectories. This will prevent backup
// of those files. We don't want to back up either the EditPlay files or the PlaySlides files.

+ (NSURL *)getRootFilesDirectory {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = nil;
    
    NSArray *appSupportDir = [fm URLsForDirectory:NSApplicationSupportDirectory inDomains:NSUserDomainMask];
    if ([appSupportDir count] > 0) {
        rootDir = [appSupportDir objectAtIndex:0];
    }
    
    return rootDir;
}

+ (NSURL *)getAbsoluteFilePathWithFolder:(NSString *)folder withFileName:(NSString *)fileName {
    NSURL *absPath;
    NSString *relPath = [NSString stringWithFormat:@"%@/%@", folder, fileName];
    
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    absPath = [rootDir URLByAppendingPathComponent:relPath];
    
    return absPath;
}

+ (NSURL *)createOrGetSlideShareDirectory:(NSString *)slideShareName {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *slideShareDirectory = [rootDir URLByAppendingPathComponent:slideShareName];
    
    NSError *err;
    BOOL retVal = [fm createDirectoryAtURL:slideShareDirectory withIntermediateDirectories:YES attributes:nil error:&err];
    if (!retVal || err) {
        HFLogDebug(@"STOUtilities.createOrGetSlideShareDirectory: err=%@", err);
        return nil;
    }
    
    retVal = [slideShareDirectory setResourceValue:[NSNumber numberWithBool:YES] forKey:NSURLIsExcludedFromBackupKey error:&err];
    
    return slideShareDirectory;
}

@end
