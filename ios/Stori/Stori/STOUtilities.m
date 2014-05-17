//
//  STOUtilities.m
//  Stori
//
//  Created by Rob Bearman on 3/6/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOUtilities.h"
#import "STOPreferences.h"

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

+ (void)deleteUnusedDirectories {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    
    NSString *editDir = [STOPreferences getEditPlayName];
    NSString *playDir = [STOPreferences getPlaySlidesName];
    
    NSError *error;
    NSArray *dirs = [fm contentsOfDirectoryAtPath:[rootDir path] error:&error];
    
    for (int i = 0; i < [dirs count]; i++) {
        NSString *val = (NSString *)[dirs objectAtIndex:i];
        if (val && ([val isEqualToString:editDir])) {
            continue;
        }
        if (val && ([val isEqualToString:playDir])) {
            continue;
        }
        if (val) {
            HFLogDebug(@"STOUtilities.deleteUnusedDirectories: deleting %@", val);
            [STOUtilities deleteSlideShareDirectory:val];
        }
    }
}

+ (BOOL)deleteSlideShareDirectory:(NSString *)slideShareName {
    if (!slideShareName) {
        return TRUE;
    }
    
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *slideShareDirectory = [rootDir URLByAppendingPathComponent:slideShareName];
    
    BOOL isDir = FALSE;
    if (![fm fileExistsAtPath:[slideShareDirectory path] isDirectory:&isDir]) {
        HFLogDebug(@"STOUtilities.deleteSlideShareDirectory - no directory exists, so bailing");
        return TRUE;
    }

    NSError *err;
    BOOL retVal = [fm removeItemAtURL:slideShareDirectory error:&err];
    if (!retVal || err) {
        HFLogDebug(@"STOUtilities.deleteSlideShareDirectory failed: err=%@", err);
    }
    
    return retVal;
}

+ (BOOL)deleteFileAtFolder:(NSString *)folder withFileName:(NSString *)fileName {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *folderDirectory = [rootDir URLByAppendingPathComponent:folder];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:fileName];
    
    NSError *err;
    BOOL retVal = [fm removeItemAtURL:fileURL error:&err];
    if (!retVal || err) {
        HFLogDebug(@"STOUtilities.deleteFileAtFolder failed: err=%@", err);
    }
    
    return retVal;
}

+ (BOOL)saveStringToFile:(NSString *)stringData withFolder:(NSString *)folder withFileName:(NSString *)fileName {
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *folderDirectory = [rootDir URLByAppendingPathComponent:folder];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:fileName];
    
    NSError *err;
    BOOL retVal = [stringData writeToURL:fileURL atomically:YES encoding:NSUTF8StringEncoding error:&err];
    if (!retVal || err) {
        HFLogDebug(@"STOUtilities.saveStringToFile failed: err=%@", err);
    }
    
    return retVal;
}

+ (NSString *)loadStringFromFolder:(NSString *)folder withFile:(NSString *)fileName {
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *folderDirectory = [rootDir URLByAppendingPathComponent:folder];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:fileName];
    
    NSError *err;
    NSString *str = [NSString stringWithContentsOfURL:fileURL encoding:NSUTF8StringEncoding error:&err];
    if (!str || err) {
        HFLogDebug(@"STOUtilities.loadStringFromFolder failed: err=%@", err);
    }
    
    return str;
}

+ (BOOL)saveImage:(UIImage *)image inFolder:(NSString *)folder withFileName:(NSString *)fileName {
    NSURL *folderDirectory = [STOUtilities createOrGetSlideShareDirectory:folder];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:fileName];
    
    NSData *imageData = UIImageJPEGRepresentation(image, 100.0);
    BOOL success = [imageData writeToURL:fileURL atomically:NO];
    
#if NEVER // diagnostics
    NSDictionary *fileAttributes = [[NSFileManager defaultManager] attributesOfItemAtPath:[fileURL path] error:nil];
    NSNumber *fileSizeNumber = [fileAttributes objectForKey:NSFileSize];
    long long fileSize = [fileSizeNumber longLongValue];
    HFLogDebug(@"STOUtilities.saveImage: fileSize=%d", (int)fileSize);
#endif
    
    return success;
}

+ (NSString *)buildResourceUrlString:(NSString *)userUuid withSlideShareName:(NSString *)slideShareName withFileName:(NSString *)fileName {
    if (!fileName || [fileName length] <= 0) {
        HFLogDebug(@"STOUtilities.buildResourceUrlString - returning nil");
        return nil;
    }
    
    NSString *urlString = [NSString stringWithFormat:@"%@%@/%@/%@", [Constants baseAWSStorageURL], userUuid, slideShareName, fileName];
    HFLogDebug(@"STOUtilities.buildResourceUrlString: %@", urlString);
    
    return urlString;
}

+ (NSString *)buildShowWebPageUrlString:(NSString *)userUuid withSlideShareName:(NSString *)slideShareName {
    NSString *urlString = [NSString stringWithFormat:@"%@%@/%@", [Constants baseWebSlidesUrl], userUuid, slideShareName];
    
    HFLogDebug(@"STOUtilities.buildShowWebPageUrlString: %@", urlString);
    
    return urlString;
}

+ (void) shareShow:(UIViewController *)viewController withUserUuid:(NSString *)userUuid withSlideShareName:(NSString *)slideShareName withTitle:(NSString *)title {
    HFLogDebug(@"STOUtilities.shareShow");
    
    NSString *appName = NSLocalizedString(@"app_name", nil);
    NSString *urlString = [STOUtilities buildShowWebPageUrlString:userUuid withSlideShareName:slideShareName];
    NSString *message = [NSString stringWithFormat:NSLocalizedString(@"share_email_body_format", nil), appName, title, urlString];
    NSString *subject = [NSString stringWithFormat:NSLocalizedString(@"share_email_subject_format", nil), appName, title];
    
    UIActivityViewController *shareController = [[UIActivityViewController alloc] initWithActivityItems:@[message] applicationActivities:nil];
    [shareController setValue:subject forKey:@"subject"];
    [viewController presentViewController:shareController animated:TRUE completion:nil];
}

+ (BOOL)isHeadsetPluggedIn {
    AVAudioSessionRouteDescription *route = [[AVAudioSession sharedInstance] currentRoute];
    for (AVAudioSessionPortDescription *desc in [route outputs]) {
        if ([[desc portType] isEqualToString:AVAudioSessionPortHeadphones]) {
            return YES;
        }
    }
    
    return NO;
}

+ (void)configureAudioSession {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    if (![STOUtilities isHeadsetPluggedIn]) {
        [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
    }
    [session setActive:TRUE error:nil];
}

+ (BOOL)parseSharedStoriUrl:(NSURL *)url returningUserId:(NSString **)userUuid returningStori:(NSString **)slideShareName {
    HFLogDebug(@"STOUtilities.parseSharedStoriUrl: %@", [url absoluteString]);
    
    if (!url) {
        return FALSE;
    }
    
    NSArray *pathComponents = [url pathComponents];
    // Remember that the initial "/" is the first path component, "slides" is the second
    if (!pathComponents || ([pathComponents count] != 4)) {
        return FALSE;
    }
    
    *userUuid = (NSString *)[pathComponents objectAtIndex:2];
    *slideShareName = (NSString *)[pathComponents objectAtIndex:3];
    
    return TRUE;
}

+ (NSString *)limitStringWithEllipses:(NSString *)string toNumChars:(int)numChars {
    if (!string) {
        return nil;
    }
    
    if (string && ([string length] <= numChars)) {
        return string;
    }
    
    return [NSString stringWithFormat:@"%@...", [string substringToIndex:numChars]];
}

+ (void)printSlideShareJSON:(SlideShareJSON *)ssj {
    HFLogDebug(@"%@", [ssj toString]); 
}

@end
