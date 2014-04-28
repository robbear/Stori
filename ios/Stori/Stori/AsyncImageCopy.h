//
//  AsyncImageCopy.h
//  Stori
//
//  Created by Rob Bearman on 4/28/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol AsyncImageCopyDelegate <NSObject>
- (void)onAsyncImageCopyComplete:(BOOL)success;
@end

@interface AsyncImageCopy : NSObject
@property (weak, nonatomic) id<AsyncImageCopyDelegate> delegate;

- (id)initWithDelegate:(id<AsyncImageCopyDelegate>)delegate;
- (void)copyImageFiles:(NSArray *)imageFiles atFolder:(NSString *)folder;
@end
