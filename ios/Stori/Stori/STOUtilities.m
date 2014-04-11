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

+ (BOOL)deleteSlideShareDirectory:(NSString *)slideShareName {
    NSFileManager *fm = [NSFileManager defaultManager];
    NSURL *rootDir = [STOUtilities getRootFilesDirectory];
    NSURL *slideShareDirectory = [rootDir URLByAppendingPathComponent:slideShareName];

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
    NSString *marketUrl = NSLocalizedString(@"google_play_market_link", nil);
    NSString *message = [NSString stringWithFormat:NSLocalizedString(@"share_email_body_format", nil), appName, title, urlString, marketUrl];
    NSString *subject = [NSString stringWithFormat:NSLocalizedString(@"share_email_subject_format", nil), appName, title];
    
    UIActivityViewController *shareController = [[UIActivityViewController alloc] initWithActivityItems:@[message] applicationActivities:nil];
    [shareController setValue:subject forKey:@"subject"];
    [viewController presentViewController:shareController animated:TRUE completion:nil];
}

+ (NSURLConnection *)downloadUrlAsync:(NSString *)urlString withDelegate:(id<NSURLConnectionDelegate>)delegate {
    HFLogDebug(@"STOUtilities.downloadUrl:%@", urlString);
    
    NSURL *url = [NSURL URLWithString:urlString];
    NSURLRequest *request = [NSURLRequest requestWithURL:url cachePolicy:NSURLRequestReloadIgnoringLocalCacheData timeoutInterval:60];
    
    return [[NSURLConnection alloc] initWithRequest:request delegate:delegate startImmediately:YES];
}

+ (void)configureAudioSession {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
    [session setActive:TRUE error:nil];
}


+ (void)printSlideShareJSON:(SlideShareJSON *)ssj {
    HFLogDebug(@"%@", [ssj toString]); 
}

@end
