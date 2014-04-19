//
//  STOSettingsController.m
//  Stori
//
//  Created by Rob Bearman on 4/19/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOSettingsController.h"
#import "STOPreferences.h"

@interface STOSettingsController ()

@end

@implementation STOSettingsController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    HFLogDebug(@"STOSettingsController.viewDidLoad");
    
    [super viewDidLoad];
    
    [self.autoPlayTitleLabel setText:NSLocalizedString(@"settings_autoplay_title", nil)];
    [self.autoPlaySubtitleLabel setText:NSLocalizedString(@"settings_autoplay_subtitle", nil)];
    [self.disconnectTitleLabel setText:NSLocalizedString(@"settings_disconnect_title", nil)];
    [self.disconnectSubtitleLabel setText:NSLocalizedString(@"settings_disconnect_subtitle", nil)];
    [self.aboutTitleLable setText:NSLocalizedString(@"settings_about_title", nil)];
    
    NSString *version = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    NSString *buildText = [NSString stringWithFormat:NSLocalizedString(@"settings_about_subtitle_format", nil), version];
    [self.aboutSubtitleLabel setText:buildText];
    
    BOOL usingAutoPlay = [STOPreferences getPlaySlidesAutoAudio];
    [self.autoPlaySwitch setOn:usingAutoPlay];
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"STOSettingsController.viewDidAppear");
    
    self.navigationController.navigationBar.backItem.title = NSLocalizedString(@"settings_navbutton_title", nil);
}

- (void)didReceiveMemoryWarning {
    HFLogDebug(@"STOSettingsController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (IBAction)onAutoPlaySwitchValueChanged:(id)sender {
    HFLogDebug(@"STOSettingsController.onAutoPlaySwitchValueChanged");
    
    BOOL usingAutoPlay = self.autoPlaySwitch.on;
    [STOPreferences savePlaySlidesAutoAudio:usingAutoPlay];
}

@end
