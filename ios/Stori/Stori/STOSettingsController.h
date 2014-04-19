//
//  STOSettingsController.h
//  Stori
//
//  Created by Rob Bearman on 4/19/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface STOSettingsController : UIViewController
@property (weak, nonatomic) IBOutlet UILabel *autoPlayTitleLabel;
@property (weak, nonatomic) IBOutlet UILabel *autoPlaySubtitleLabel;
@property (weak, nonatomic) IBOutlet UISwitch *autoPlaySwitch;
@property (weak, nonatomic) IBOutlet UILabel *disconnectTitleLabel;
@property (weak, nonatomic) IBOutlet UILabel *disconnectSubtitleLabel;
@property (weak, nonatomic) IBOutlet UIView *disconnectPanel;
@property (weak, nonatomic) IBOutlet UILabel *aboutTitleLable;
@property (weak, nonatomic) IBOutlet UILabel *aboutSubtitleLabel;
@property (weak, nonatomic) IBOutlet UIView *aboutPanel;
@end
