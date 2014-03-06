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

@end
