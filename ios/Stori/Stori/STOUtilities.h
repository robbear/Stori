//
//  STOUtilities.h
//  Stori
//
//  Created by Rob Bearman on 3/6/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface STOUtilities : NSObject

+ (NSURL *)getRootFilesDirectory;
+ (NSURL *)getAbsoluteFilePathWithFolder:(NSString *)folder withFileName:(NSString *)fileName;
+ (NSURL *)createOrGetSlideShareDirectory:(NSString *)slideShareName;
+ (BOOL)deleteSlideShareDirectory:(NSString *)slideShareName;
+ (BOOL)deleteFileAtFolder:(NSString *)folder withFileName:(NSString *)fileName;
+ (BOOL)saveStringToFile:(NSString *)stringData withFolder:(NSString *)folder withFileName:(NSString *)fileName;
+ (NSString *)loadStringFromFolder:(NSString *)folder withFile:(NSString *)fileName;
+ (void)shareShow:(UIViewController *)viewController withUserUuid:(NSString *)userUuid withSlideShareName:(NSString *)slideShareName withTitle:(NSString *)title;
+ (NSString *)buildShowWebPageUrlString:(NSString *)userUuid withSlideShareName:(NSString *)slideShareName;
+ (NSURLConnection *)downloadUrlAsync:(NSString *)urlString withDelegate:(id<NSURLConnectionDelegate>)delegate;

@end
