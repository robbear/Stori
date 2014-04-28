//
//  HFProgressHUD.h
//  Stori
//
//  Created by Rob Bearman on 4/28/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "MBProgressHUD.h"

@interface HFProgressHUD : MBProgressHUD
- (id)initWithView:(UIView *)view;
- (void)hide:(BOOL)animated;
@end
