//
//  HFProgressHUD.m
//  Stori
//
//  Created by Rob Bearman on 4/28/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "HFProgressHUD.h"

@implementation HFProgressHUD

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

- (id)initWithView:(UIView *)view {
    self = [super initWithView:view];
    [view addSubview:self];
    
    return self;
}

- (void)hide:(BOOL)animated {
    [super hide:animated];
    [self removeFromSuperview];
}

@end
