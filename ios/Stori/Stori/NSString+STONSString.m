//
//  NSString+STONSString.m
//  Stori
//
//  Created by Rob Bearman on 3/18/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "NSString+STONSString.h"

@implementation NSString (STONSString)

- (NSString *)urlDecode {
    NSString *result = [(NSString *)self stringByReplacingOccurrencesOfString:@"+" withString:@" "];
    result = [result stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    return result;
}

@end
