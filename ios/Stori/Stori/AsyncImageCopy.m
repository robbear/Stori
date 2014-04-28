//
//  AsyncImageCopy.m
//  Stori
//
//  Created by Rob Bearman on 4/28/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "AsyncImageCopy.h"
#import "STOUtilities.h"

@interface AsyncImageCopy ()
@property (nonatomic) int numImagesToCopy;
@property (nonatomic) int numImagesCopied;
@property (nonatomic) BOOL oneOrMoreCopiesFailed;

- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo: (void *) contextInfo;
- (void)handleCopyError:(NSString *)description;
@end

@implementation AsyncImageCopy

- (id)initWithDelegate:(id<AsyncImageCopyDelegate>)delegate {
    self = [super init];
    if (self) {
        self.delegate = delegate;
        self.numImagesCopied = 0;
        self.numImagesToCopy = 0;
        self.oneOrMoreCopiesFailed = FALSE;
    }
    
    return self;
}

- (void)handleCopyError:(NSString *)description {
    NSMutableDictionary* details = [NSMutableDictionary dictionary];
    [details setValue:description forKey:NSLocalizedDescriptionKey];
    NSError *error = [NSError errorWithDomain:@"com.stori-app" code:200 userInfo:details];
    [self image:nil didFinishSavingWithError:error contextInfo:nil];
}

- (void)copyImageFiles:(NSArray *)imageFiles atFolder:(NSString *)folder {
    if (!imageFiles || !folder || [imageFiles count] <= 0) {
        [self handleCopyError:@"Invalid argument"];
        return;
    }
    
    int count = (int)[imageFiles count];
    self.numImagesToCopy = count;
    
    for (int i = 0; i < count; i++) {
        NSString *imageFileName = (NSString *)[imageFiles objectAtIndex:i];
        
        NSURL *imageFileUrl = [STOUtilities getAbsoluteFilePathWithFolder:folder withFileName:imageFileName];
        if (!imageFileUrl) {
            [self handleCopyError:@"Invalid argument"];
            continue;
        }
        
        UIImage *image = [UIImage imageWithData:[NSData dataWithContentsOfURL:imageFileUrl]];
        if (!image) {
            [self handleCopyError:@"Image cannot be found"];
            continue;
        }
        
        UIImageWriteToSavedPhotosAlbum(image, self, @selector(image:didFinishSavingWithError:contextInfo:), nil);
    }
}

- (void)image:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo: (void *) contextInfo {
    HFLogDebug(@"AsyncImageCopy.image:didFinishSavingWithError:error=%@", error ? error.description : nil);
    
    if (error) {
        self.oneOrMoreCopiesFailed = TRUE;
    }
    
    self.numImagesCopied++;
    
    if (self.numImagesCopied >= self.numImagesToCopy) {
        BOOL success = !self.oneOrMoreCopiesFailed;
        
        if ([self.delegate respondsToSelector:@selector(onAsyncImageCopyComplete:)]) {
            [self.delegate onAsyncImageCopyComplete:success];
        }
    }
}

@end
