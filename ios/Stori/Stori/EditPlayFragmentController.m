//
//  EditPlayFragmentController.m
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayFragmentController.h"
#import "AmazonSharedPreferences.h"
#import "STOUtilities.h"

@interface EditPlayFragmentController ()
@property (nonatomic) UIImagePickerController *imagePickerController;
- (BOOL)hasImage;
- (void)selectImageFromGallery;
- (void)selectImageFromCamera;
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
     
- (IBAction)onDeleteSlideButtonClicked:(id)sender {
    [self.editPlayController deleteSlide:self.slideUuid withImage:self.imageFileName withAudio:self.audioFileName];
}

- (IBAction)onTrashButtonClicked:(id)sender {
    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:NSLocalizedString(@"menu_editplay_trash_title", nil)
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeslide", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeimage", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeaudio", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];
}

- (IBAction)onEditTextButtonClicked:(id)sender {
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_slidetext_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_slidetext_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
    dialog.alertViewStyle = UIAlertViewStylePlainTextInput;
    dialog.tag = 1;
    [dialog show];
}

- (IBAction)onSelectPhotoButtonClicked:(id)sender {
    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:NSLocalizedString(@"menu_editplay_image_title", nil)
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    [popup addButtonWithTitle:[self hasImage] ? NSLocalizedString(@"menu_editplay_image_replacepicture", nil) : NSLocalizedString(@"menu_editplay_image_choosepicture", nil)];
    [popup addButtonWithTitle:[self hasImage] ? NSLocalizedString(@"menu_editplay_image_replacecamera", nil) : NSLocalizedString(@"menu_editplay_image_usecamera", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];
}

- (void)selectImageFromGallery {
    UIImagePickerController *imagePickerController = [[UIImagePickerController alloc] init];
    imagePickerController.delegate = self;
    imagePickerController.modalPresentationStyle = UIModalPresentationCurrentContext;
    imagePickerController.sourceType =  UIImagePickerControllerSourceTypePhotoLibrary;
    
    self.imagePickerController = imagePickerController;
    [self presentViewController:self.imagePickerController animated:YES completion:nil];
}

- (void)selectImageFromCamera {
    UIImagePickerController *imagePickerController = [[UIImagePickerController alloc] init];
    imagePickerController.delegate = self;
    imagePickerController.sourceType = UIImagePickerControllerSourceTypeCamera;
    
    // BUGBUG: What, if any, settings allow the saving of the image to the system picture folder?
    
    self.imagePickerController = imagePickerController;
    [self presentViewController:imagePickerController animated:YES completion:nil];
}

- (BOOL)hasImage {
    return self.imageFileName != nil;
}

//
// UIImagePickerControllerDelegate methods
//

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    HFLogDebug(@"EditPlayFragmentController.imagePickerController:didFinishPickingMediaWithInfo");
    
    UIImage *image = [info valueForKey:UIImagePickerControllerOriginalImage];
    //[self.imageView setImage:image];
    NSString *imageFileName = self.imageFileName;
    if (!imageFileName) {
        imageFileName = [[NSUUID UUID] UUIDString];
    }
    
    [self dismissViewControllerAnimated:YES completion:NULL];
    self.imagePickerController = nil;
    
    BOOL success = [STOUtilities saveImage:image inFolder:self.slideSharename withFileName:imageFileName];
    if (success) {
        self.imageFileName = imageFileName;
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:self.imageFileName withAudioFileName:self.audioFileName withText:self.slideText];
    }
    else {
        HFLogDebug(@"EditPlayFragmentController.imagePickerController:didFinishPickingMediaWithInfo - failed. Bailing");
    }
    
    [self refreshInterface];
}


- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    HFLogDebug(@"EditPlayFragmentController.imagePickerControllerDidCancel");
    
    [self dismissViewControllerAnimated:YES completion:NULL];
    self.imagePickerController = nil;
}

//
// UIAlertViewDelegate methods
//

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        UITextField *textField = [alertView textFieldAtIndex:0];
        NSString *text = textField.text;
        
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:nil withAudioFileName:nil withText:text];
        self.slideText = [self.editPlayController getSlideText:self.slideUuid];
        
        [self refreshInterface];
    }
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
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_image_choosepicture", nil)] ||
             [buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_image_replacepicture", nil)]) {
        [self selectImageFromGallery];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_image_usecamera", nil)] ||
             [buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_image_replacecamera", nil)]) {
        [self selectImageFromCamera];
    }
    else if ([buttonTitle isEqual:NSLocalizedString(@"menu_editplay_trash_removeslide", nil)]) {
        [self.editPlayController deleteSlide:self.slideUuid withImage:self.imageFileName withAudio:self.audioFileName];
    }
    else if ([buttonTitle isEqual:NSLocalizedString(@"menu_editplay_trash_removeimage", nil)]) {
    }
    else if ([buttonTitle isEqual:NSLocalizedString(@"menu_editplay_trash_removeaudio", nil)]) {
    }
}

//
// Test stuff
//

- (void)refreshInterface {
    HFLogDebug(@"EditPlayFragmentController.refreshInterface");
    //self.tempSlideTextLabel.text = self.slideText;
    
    NSURL *fileURL = [STOUtilities getAbsoluteFilePathWithFolder:self.slideSharename withFileName:self.imageFileName];
    UIImage *image = [UIImage imageWithContentsOfFile:[fileURL path]];
    [self.imageView setImage:image];
}

@end
