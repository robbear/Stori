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

- (NSString *)urlEncode {
    NSMutableString * output = [NSMutableString string];
    const unsigned char * source = (const unsigned char *)[self UTF8String];
    int sourceLen = strlen((const char *)source);
    for (int i = 0; i < sourceLen; ++i) {
        const unsigned char thisChar = source[i];
        if (thisChar == ' '){
            [output appendString:@"+"];
        }
        else if (thisChar == '.' || thisChar == '-' || thisChar == '_' || thisChar == '~' ||
                   (thisChar >= 'a' && thisChar <= 'z') ||
                   (thisChar >= 'A' && thisChar <= 'Z') ||
                   (thisChar >= '0' && thisChar <= '9')) {
            [output appendFormat:@"%c", thisChar];
        }
        else {
            [output appendFormat:@"%%%02X", thisChar];
        }
    }
    
    return output;
}

@end
