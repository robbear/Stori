//
//  STOSettingsController.m
//  Stori
//
//  Created by Rob Bearman on 4/19/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STOSettingsController.h"
#import "STOPreferences.h"
#import "AmazonSharedPreferences.h"

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
    [self.disconnectButton setTitle:NSLocalizedString(@"settings_disconnect_button", nil) forState:UIControlStateNormal];
    [self.aboutTitleLable setText:NSLocalizedString(@"settings_about_title", nil)];
    
    NSString *version = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleVersion"];
    NSString *buildText = [NSString stringWithFormat:NSLocalizedString(@"settings_about_subtitle_format", nil), version];
    [self.aboutSubtitleLabel setText:buildText];
    
    NSString *email = [AmazonSharedPreferences userEmail];
    NSString *accountString = [NSString stringWithFormat:NSLocalizedString(@"settings_disconnect_account_format", nil), email];
    [self.currentAccountLabel setText:accountString];
    
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

- (IBAction)onDisconnectButtonClicked:(id)sender {
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"settings_disconnect_dialog_title", nil)
                                                     message:NSLocalizedString(@"settings_disconnect_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"settings_disconnect_button", nil), nil];
    dialog.tag = 1;
    [dialog show];
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"settings_disconnect_button", nil)]) {
        [self.navigationController popViewControllerAnimated:YES];        
        [self.editPlayController disconnectFromGoogle];
    }
}

@end
