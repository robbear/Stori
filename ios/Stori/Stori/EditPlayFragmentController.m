//
//  EditPlayFragmentController.m
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayFragmentController.h"
#import "AmazonSharedPreferences.h"

@interface EditPlayFragmentController ()

@end

@implementation EditPlayFragmentController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    HFLogDebug(@"EditPlayFragmentController.viewDidLoad");
    
    [super viewDidLoad];
    
    [self refreshInterface];
}

- (void)didReceiveMemoryWarning {
    HFLogDebug(@"EditPlayFragmentController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (void)initializeWithSlideJSON:(SlideJSON *)sj withSlideShareName:(NSString *)slideShareName
                       withUuid:(NSString *)slideUuid fromController:(EditPlayController *)editPlayController {
    HFLogDebug(@"EditPlayFragmentController.initializeWithSlideJSON");
    
    self.slideSharename = slideShareName;
    self.slideUuid = slideUuid;
    self.editPlayController = editPlayController;
    self.imageFileName = [sj getImageFilename];
    self.audioFileName = [sj getAudioFilename];
    self.slideText = [sj getText];
}

- (IBAction)onMainMenuButtonClicked:(id)sender {
    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:NSLocalizedString(@"menu_editplay_title", nil)
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_preview", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_rename", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_publish", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_share", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_createnew", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_list", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_settings", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];    
}

- (IBAction)onInsertBeforeButtonClicked:(id)sender {
    int selectedPosition = self.editPlayController.currentSlideIndex;
    [self.editPlayController addSlide:selectedPosition];
}
     
- (IBAction)onInsertAfterButtonClicked:(id)sender {
    int selectedPosition = self.editPlayController.currentSlideIndex;
    [self.editPlayController addSlide:selectedPosition + 1];
}
     

//
// UIActionSheetDelegate methods
//

- (void)actionSheet:(UIActionSheet *)popup clickedButtonAtIndex:(NSInteger)index {
    HFLogDebug(@"EditPlayFragmentController.actionSheet:clickedButtonAtIndex %d, menutitle=%@", index, [popup buttonTitleAtIndex:index]);
    
    NSString *buttonTitle = [popup buttonTitleAtIndex:index];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_preview", nil)]) {
        HFLogDebug(@"preview...");
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_rename", nil)]) {
        HFLogDebug(@"rename...");
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_publish", nil)]) {
        HFLogDebug(@"publish...");
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_share", nil)]) {
        HFLogDebug(@"share...");
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_createnew", nil)]) {
        HFLogDebug(@"create new...");
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_list", nil)]) {
        HFLogDebug(@"list my storis...");
       [self performSegueWithIdentifier: @"SegueToStoriListController" sender: self];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_settings", nil)]) {
        HFLogDebug(@"settings...");
    }
}

//
// Test stuff
//

- (void)refreshInterface {
    HFLogDebug(@"EditPlayFragmentController.refreshInterface");
    
    HFLogDebug(@"EditPlayFragmentController.refreshInterface: userName = %@", [AmazonSharedPreferences userName]);
    
    self.userEmailLabel.text = [AmazonSharedPreferences userEmail];
    self.userIDLabel.text = [AmazonSharedPreferences userName];
}

@end
