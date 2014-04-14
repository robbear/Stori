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
#import "UIImage+Resize.h"

#define ALERTVIEW_DIALOG_SLIDETEXT 1
#define ALERTVIEW_DIALOG_STORITITLE 2
#define ALERTVIEW_DIALOG_OVERWRITE_AUDIO 3

@interface EditPlayFragmentController ()
@property (strong, nonatomic) AVAudioRecorder *audioRecorder;
@property (strong, nonatomic) AVAudioPlayer *audioPlayer;
@property (nonatomic) BOOL isRecording;
@property (nonatomic) BOOL isPlaying;
@property (nonatomic) UIImagePickerController *imagePickerController;
- (void)deleteSlideData;
- (BOOL)hasImage;
- (BOOL)hasAudio;
- (BOOL)hasText;
- (void)selectImageFromGallery;
- (void)selectImageFromCamera;
- (void)displaySlideTitleAndPosition;
- (void)displayNextPrevControls;
- (void)displayPlayStopControl;
- (void)displaySlideTextControl;
- (void)renderImage;
- (void)renameStori;
- (void)alertViewForSlideText:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForStoriTitle:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForOverwriteAudio:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)startRecording;
- (void)stopRecording;
- (void)startPlaying;
- (void)stopPlaying;
- (NSString *)getNewImageFileName;
- (NSString *)getNewAudioFileName;
- (void)enableControlsWhileRecordingOrPlaying:(BOOL)enabled;
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
    
    //
    // Set the overlay view's background color alpha, rather than setting the UIView's alpha directly in Storyboard.
    // This technique allows controls layered on top of the overlay view to be fully opaque.
    //
    [self.overlayView setBackgroundColor:[UIColor colorWithRed:0.0 green:0.0 blue:0.0 alpha:OVERLAY_ALPHA]];
    
    [self setIsRecording:FALSE];
    [self setIsPlaying:FALSE];
    
    [self renderImage];
    [self displaySlideTitleAndPosition];
    [self displayNextPrevControls];
    [self displayPlayStopControl];
    [self displaySlideTextControl];
}

- (void)configureAudioSession {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    [session overrideOutputAudioPort:AVAudioSessionPortOverrideSpeaker error:nil];
    [session setActive:TRUE error:nil];
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

- (void)onEditPlayFragmentWillBeSelected {
    HFLogAlert(@"EditPlayFragmentController.onEditPlayFragmentWillBeSelected");
    
    [self displaySlideTitleAndPosition];
    [self displayNextPrevControls];
    [self displaySlideTextControl];
}

- (void)onEditPlayFragmentWillBeDeselected {
    HFLogDebug(@"EditPlayFragmentController.onEditPlayFragmentWillBeDeselected");
    
    [self stopPlaying];
    [self stopRecording];
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
    if ([self.editPlayController isPublished]) {
        [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_share", nil)];
    }
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_createnew", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_list", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_settings", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];    
}

- (IBAction)onPlayStopButtonClicked:(id)sender {
    if (![self hasAudio]) {
        return;
    }
    
    if (!self.isPlaying) {
        [self startPlaying];
    }
    else {
        [self stopPlaying];
    }
}

- (IBAction)onRecordingButtonClicked:(id)sender {
    if (self.isRecording) {
        [self stopRecording];
    }
    else {
        if (self.hasAudio) {
            UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_overwriteaudio_title", nil)
                                                        message:NSLocalizedString(@"editplay_overwriteaudio_message", nil)
                                                        delegate:self
                                                        cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                                        otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
            dialog.tag = ALERTVIEW_DIALOG_OVERWRITE_AUDIO;
            [dialog show];
        }
        else {
            [self startRecording];
        }
    }
}

- (IBAction)onLeftArrowButtonClicked:(id)sender {
    int position = [self.editPlayController getSlidePosition:self.slideUuid];
    
    position--;
    if (position < 0) {
        return;
    }
    
    [self.editPlayController setCurrentSlidePosition:position];
}

- (IBAction)onRightArrowButtonClicked:(id)sender {
    int position = [self.editPlayController getSlidePosition:self.slideUuid];
    int count = [self.editPlayController getSlideCount];
    
    position++;
    if (position >= count) {
        return;
    }
    
    [self.editPlayController setCurrentSlidePosition:position];
}

- (IBAction)onInsertBeforeButtonClicked:(id)sender {
    int selectedPosition = self.editPlayController.currentSlideIndex;
    [self.editPlayController addSlide:selectedPosition];
}
     
- (IBAction)onInsertAfterButtonClicked:(id)sender {
    int selectedPosition = self.editPlayController.currentSlideIndex;
    [self.editPlayController addSlide:selectedPosition + 1];
}
     
- (IBAction)onEditButtonClicked:(id)sender {
    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:NSLocalizedString(@"menu_editplay_edit_title", nil)
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_edit_text", nil)];
    if ([self.editPlayController getSlideCount] > 1) {
        [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_edit_reorder", nil)];
    }
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];
}

- (IBAction)onTrashButtonClicked:(id)sender {
    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:NSLocalizedString(@"menu_editplay_trash_title", nil)
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeslide", nil)];
    if ([self hasImage]) {
        [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeimage", nil)];
    }
    if ([self hasAudio]) {
        [popup addButtonWithTitle:NSLocalizedString(@"menu_editplay_trash_removeaudio", nil)];
    }
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];
}

- (void)deleteSlideData {
    HFLogDebug(@"EditPlayFragmentController.deleteSlideData");
    
    [self.editPlayController deleteImage:self.slideUuid withImage:self.imageFileName];
    self.imageFileName = nil;
    [self.editPlayController deleteAudio:self.slideUuid withAudio:self.audioFileName];
    self.audioFileName = nil;
    self.slideText = nil;
    
    [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:nil withAudioFileName:nil withText:nil withForcedNulls:TRUE];
    
    [self displayPlayStopControl];
    [self displaySlideTitleAndPosition];
    [self displaySlideTextControl];
    [self renderImage];
}

- (void)enterSlideText {
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_slidetext_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_slidetext_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
    dialog.alertViewStyle = UIAlertViewStylePlainTextInput;
    dialog.tag = ALERTVIEW_DIALOG_SLIDETEXT;
    
    UITextField *textField = [dialog textFieldAtIndex:0];
    textField.tag = ALERTVIEW_DIALOG_SLIDETEXT;
    textField.text = [self.editPlayController getSlideText:self.slideUuid];
    [textField setSelected:TRUE];
    textField.delegate = self;
    
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

- (void)renameStori {
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_storititle_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_storititle_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
    dialog.alertViewStyle = UIAlertViewStylePlainTextInput;
    dialog.tag = ALERTVIEW_DIALOG_STORITITLE;
    UITextField *textField = [dialog textFieldAtIndex:0];
    textField.tag = ALERTVIEW_DIALOG_STORITITLE;
    textField.text = [self.editPlayController getSlidesTitle];
    [textField setSelected:TRUE];
    textField.delegate = self;
    
    [dialog show];
}

- (BOOL)hasImage {
    return self.imageFileName != nil;
}

- (BOOL)hasAudio {
    return self.audioFileName != nil;
}

- (BOOL)hasText {
    return (self.slideText != nil && ([self.slideText length] > 0));
}

- (void)enableControlsWhileRecordingOrPlaying:(BOOL)enabled {
    HFLogDebug(@"EditPlayFragmentController.enableControlsWhileRecordingOrPlaying: %d", enabled);

    [self.playStopButton setEnabled:enabled];
    [self.recordingButton setEnabled:enabled];
    [self.selectPhotoButton setEnabled:enabled];
    [self.mainMenuButton setEnabled:enabled];
    [self.insertAfterButton setEnabled:enabled];
    [self.insertBeforeButton setEnabled:enabled];
    [self.trashButton setEnabled:enabled];
    [self.leftArrowButton setEnabled:enabled];
    [self.rightArrowButton setEnabled:enabled];
    [self.editButton setEnabled:enabled];
}

- (void)startRecording {
    HFLogAlert(@"EditPlayFragmentController.startRecording");
    
    if (!self.audioFileName) {
        [self setAudioFileName:[self getNewAudioFileName]];
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:self.imageFileName withAudioFileName:self.audioFileName withText:self.slideText];
    }
    
    NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                    [NSNumber numberWithFloat: 16000], AVSampleRateKey,
                                    [NSNumber numberWithInt: kAudioFormatMPEG4AAC], AVFormatIDKey,
                                    [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                    nil];
    
    NSURL *folderDirectory = [STOUtilities createOrGetSlideShareDirectory:self.slideSharename];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:self.audioFileName];
    
    // BUGBUG - handle error
    self.audioRecorder = [[AVAudioRecorder alloc] initWithURL:fileURL settings:recordSettings error:nil];
    self.audioRecorder.delegate = self;
    self.audioRecorder.meteringEnabled = YES;
    [self.audioRecorder recordForDuration:MAX_RECORDING_SECONDS];
    [self.audioRecorder prepareToRecord];
    
    BOOL success = [self.audioRecorder record];
    if (success) {
        [self enableControlsWhileRecordingOrPlaying:FALSE];
        [self.recordingButton setEnabled:TRUE];
        [self setIsRecording:TRUE];
        [self.recordingButton setImage:[UIImage imageNamed:@"ic_stoprecording.png"] forState:UIControlStateNormal];
    }
}

- (void)stopRecording {
    HFLogDebug(@"EditPlayFragmentController.stopRecording");
    
    if (self.isRecording) {
        [self.audioRecorder stop];
        [self setIsRecording:FALSE];
    }
    
    [self.recordingButton setImage:[UIImage imageNamed:@"ic_record.png"] forState:UIControlStateNormal];
    [self displayPlayStopControl];
    [self enableControlsWhileRecordingOrPlaying:TRUE];
}

- (NSString *)getNewImageFileName {
    return [NSString stringWithFormat:@"%@.jpg", [[NSUUID UUID] UUIDString]];
}

- (NSString *)getNewAudioFileName {
    return [NSString stringWithFormat:@"%@.3gp", [[NSUUID UUID] UUIDString]];
}

- (void)startPlaying {
    NSURL *folderDirectory = [STOUtilities createOrGetSlideShareDirectory:self.slideSharename];
    NSURL *fileURL = [folderDirectory URLByAppendingPathComponent:self.audioFileName];
    
    self.audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:fileURL error:nil];
    self.audioPlayer.delegate = self;
    self.isPlaying = TRUE;
    [self.audioPlayer play];
    
    [self enableControlsWhileRecordingOrPlaying:FALSE];
    [self.playStopButton setEnabled:TRUE];
    [self.playStopButton setImage:[UIImage imageNamed:@"ic_stopplaying.png"] forState:UIControlStateNormal];
}

- (void)stopPlaying {
    [self.audioPlayer stop];
    self.audioPlayer = nil;
    [self.playStopButton setImage:[UIImage imageNamed:@"ic_play.png"] forState:UIControlStateNormal];
    [self enableControlsWhileRecordingOrPlaying:TRUE];
    self.isPlaying = FALSE;
}


//
// AVAudioRecorderDelegate methods
//
- (void)audioRecorderDidFinishRecording:(AVAudioRecorder *)recorder successfully:(BOOL)flag {
    HFLogDebug(@"EditPlayFragmentController.audioRecorderDidFinishRecording:successfully:%d", flag);
    
    [self setIsRecording:FALSE];
    [self stopRecording];
}

//
// AVAudioPlayerDelegate methods
//
- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    HFLogDebug(@"EditPlayFragmentController.audioPlayerDidFinishPlaying:successfully:%d", flag);
    
    [self stopPlaying];
}

//
// UIImagePickerControllerDelegate methods
//

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    HFLogDebug(@"EditPlayFragmentController.imagePickerController:didFinishPickingMediaWithInfo");
    
    UIImage *image = [info valueForKey:UIImagePickerControllerOriginalImage];
    
    //
    // Resize image if necessary
    //
    CGSize sz;
    if (image.size.width > image.size.height) {
        // Landscape
        sz = CGSizeMake(IMAGE_DISPLAY_WIDTH_LANDSCAPE, IMAGE_DISPLAY_HEIGHT_LANDSCAPE);
    }
    else {
        // Portrait
        sz = CGSizeMake(IMAGE_DISPLAY_WIDTH_PORTRAIT, IMAGE_DISPLAY_HEIGHT_PORTRAIT);
    }
    UIImage *resizedImage = [image resizedImageToFitInSize:sz scaleIfSmaller:FALSE];
    
    NSString *imageFileName = self.imageFileName;
    if (!imageFileName) {
        imageFileName = [self getNewImageFileName];
    }
    
    [self dismissViewControllerAnimated:YES completion:NULL];
    self.imagePickerController = nil;
    
    BOOL success = [STOUtilities saveImage:resizedImage inFolder:self.slideSharename withFileName:imageFileName];
    if (success) {
        self.imageFileName = imageFileName;
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:self.imageFileName withAudioFileName:self.audioFileName withText:self.slideText];
    }
    else {
        HFLogDebug(@"EditPlayFragmentController.imagePickerController:didFinishPickingMediaWithInfo - failed. Bailing");
    }
    
    [self renderImage];
}


- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker {
    HFLogDebug(@"EditPlayFragmentController.imagePickerControllerDidCancel");
    
    [self dismissViewControllerAnimated:YES completion:NULL];
    self.imagePickerController = nil;
}

//
// UITextFieldDelegate methods
//
- (BOOL)textField:(UITextField *)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string {
    NSUInteger newLength = [textField.text length] + [string length] - range.length;
    
    //
    // Restrict text field sizes
    //
    
    switch (textField.tag) {
        case ALERTVIEW_DIALOG_SLIDETEXT:
            return (newLength > MAX_SLIDE_TEXT_CHARACTERS) ? NO : YES;
            break;
            
        case ALERTVIEW_DIALOG_STORITITLE:
            return (newLength > MAX_STORI_TITLE_CHARACTERS) ? NO : YES;
            break;

        default:
            return TRUE;
    }
}


//
// UIAlertViewDelegate methods
//

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    switch (alertView.tag) {
        case ALERTVIEW_DIALOG_SLIDETEXT:
            [self alertViewForSlideText:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_STORITITLE:
            [self alertViewForStoriTitle:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_OVERWRITE_AUDIO:
            [self alertViewForOverwriteAudio:alertView clickedButtonAtIndex:buttonIndex];
            
        default:
            break;
    }
}

- (void)alertViewForOverwriteAudio:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        [self startRecording];
    }
}

- (void)alertViewForSlideText:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        UITextField *textField = [alertView textFieldAtIndex:0];
        NSString *text = textField.text;
        
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:nil withAudioFileName:nil withText:text];
        self.slideText = [self.editPlayController getSlideText:self.slideUuid];
        
        [self displaySlideTextControl];
    }
}

- (void)alertViewForStoriTitle:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        UITextField *textField = [alertView textFieldAtIndex:0];
        NSString *text = textField.text;
        
        [self.editPlayController setSlideShareTitle:text];
        [self displaySlideTitleAndPosition];
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
        [self renameStori];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_publish", nil)]) {
        [self.editPlayController publishSlides];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_share", nil)]) {
        [self.editPlayController shareSlides];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_createnew", nil)]) {
        [self.editPlayController createNewSlideShow];
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
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_trash_removeslide", nil)]) {
        int count = [self.editPlayController getSlideCount];
        if (count > 1) {
            [self.editPlayController deleteSlide:self.slideUuid withImage:self.imageFileName withAudio:self.audioFileName];
        }
        else {
            [self deleteSlideData];
        }
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_trash_removeimage", nil)]) {
        [self.editPlayController deleteImage:self.slideUuid withImage:self.imageFileName];
        self.imageFileName = nil;
        [self renderImage];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_trash_removeaudio", nil)]) {
        [self.editPlayController deleteAudio:self.slideUuid withAudio:self.audioFileName];
        self.audioFileName = nil;
        [self displayPlayStopControl];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_edit_text", nil)]) {
        [self enterSlideText];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_edit_reorder", nil)]) {
        HFLogAlert(@"Reorder...");
    }
}

- (void)displaySlideTextControl {
    [self.slideTextLabel setBackgroundColor:[UIColor colorWithRed:0.0 green:0.0 blue:0.0 alpha:SLIDETEXT_ALPHA]];
    self.slideTextLabel.layer.cornerRadius = 5;
    self.slideTextLabel.layer.masksToBounds = TRUE;
    [self.slideTextLabel setText:self.slideText];
    [self.slideTextLabel sizeToFit];    
    [self.slideTextLabel setHidden:![self hasText]];
}

- (void)displayPlayStopControl {
    [self.playStopButton setHidden:(![self hasAudio])];
}

- (void)displaySlideTitleAndPosition {
    int count = [self.editPlayController getSlideCount];
    int position = [self.editPlayController getSlidePosition:self.slideUuid];
    NSString *title = [self.editPlayController getSlidesTitle];
    
    self.storiTitleLabel.text = title;
    self.slidePositionLabel.text = [NSString stringWithFormat:NSLocalizedString(@"slide_position_format", nil), position + 1, count];
}

- (void)displayNextPrevControls {
    int count = [self.editPlayController getSlideCount];
    int position = [self.editPlayController getSlidePosition:self.slideUuid];

    [self.insertBeforeButton setHidden:(count >= MAX_SLIDES_PER_STORI_FOR_FREE)];
    [self.insertAfterButton setHidden:(count >= MAX_SLIDES_PER_STORI_FOR_FREE)];

    [self.leftArrowButton setHidden:(position <= 0)];
    [self.rightArrowButton setHidden:(position >= count - 1)];
    
}

- (void)renderImage {
    NSURL *fileURL = [STOUtilities getAbsoluteFilePathWithFolder:self.slideSharename withFileName:self.imageFileName];
    UIImage *image = [UIImage imageWithContentsOfFile:[fileURL path]];
    [self.imageView setImage:image];
}

@end
