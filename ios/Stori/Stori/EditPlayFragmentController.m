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

#define ALERTVIEW_DIALOG_SLIDETEXT 1
#define ALERTVIEW_DIALOG_STORITITLE 2
#define ALERTVIEW_DIALOG_OVERWRITE_AUDIO 3

@interface EditPlayFragmentController ()
@property (strong, nonatomic) AVAudioRecorder *audioRecorder;
@property (strong, nonatomic) AVAudioPlayer *audioPlayer;
@property (nonatomic) BOOL isRecording;
@property (nonatomic) UIImagePickerController *imagePickerController;
- (void)deleteSlideData;
- (BOOL)hasImage;
- (BOOL)hasAudio;
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
- (NSString *)getNewImageFileName;
- (NSString *)getNewAudioFileName;
@end

@implementation EditPlayFragmentController

//
// Audio hints
//
#if NEVER

The container format is determined by the extension of the URL you specify when creating the AVAudioRecorder.

NSString *tempDir = NSTemporaryDirectory ();
NSString *soundFilePath = [tempDir stringByAppendingString: @"sound.mp4"];
You can see here that the extension specified is mp4. Upon creating the recorder, we specify the codec format (here it is plain AAC):

[[AVAudioSession sharedInstance] setCategory: AVAudioSessionCategoryRecord error: nil];


NSDictionary *recordSettings = [[NSDictionary alloc] initWithObjectsAndKeys:
                                [NSNumber numberWithFloat: 32000], AVSampleRateKey, // 16000?
                                [NSNumber numberWithInt: kAudioFormatMPEG4AAC], AVFormatIDKey,
                                [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                
                                nil];

AVAudioRecorder *newRecorder = [[AVAudioRecorder alloc] initWithURL: soundFilePath settings: recordSettings error: nil];
[recordSettings release];
self.soundRecorder = newRecorder;
[newRecorder release];

soundRecorder.delegate = self;
[soundRecorder prepareToRecord];
[soundRecorder recordForDuration:60];
The resulting file will be a AAC compressed stream within an "ISO Media, MPEG v4 system, version 2" container (as of iOS 5.0).

If you specify and extension with "caf" then the container will be Core Audio Format an the codec will still be AAC.


// Also:
We recently completed one app with this kind of functionality recording from both and listening from both iOS and Android, below is code we used for your reference.We relied on our webservice and server for conversion to .mp3 format which can be easily played in both platform.

//iPhone side code. records m4a file format (small in size)

NSDictionary *recordSettings = [NSDictionary dictionaryWithObjectsAndKeys:
                                [NSNumber numberWithInt: kAudioFormatMPEG4AAC], AVFormatIDKey,
                                [NSNumber numberWithFloat:16000.0], AVSampleRateKey,
                                [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                nil];
NSError *error = nil;
audioRecorder = [[AVAudioRecorder alloc] initWithURL:soundFileURL settings:recordSettings error:&error];

if (error)
NSLog(@"error: %@", [error localizedDescription]);
else
[audioRecorder prepareToRecord];


//Android side code. (records mp4 format and it works straight away by giving mp3 extension.)
MediaRecorder recorder = new MediaRecorder();
recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
recorder.setAudioEncodingBitRate(256);
recorder.setAudioChannels(1);
recorder.setAudioSamplingRate(44100);
recorder.setOutputFile(getFilename());

try {
    recorder.prepare();
    recorder.start();
    myCount=new MyCount(thirtysec, onesecond);
    myCount.start();
    recordingDone=true;
    count=0;
    handler.sendEmptyMessage(0);
    if(progressDialog.isShowing())
        progressDialog.dismiss();
} catch (IllegalStateException e) {
    e.printStackTrace();
    StringWriter strTrace = new StringWriter();
    e.printStackTrace(new PrintWriter(strTrace));
    CrashReportActivity.appendLog(
                                  "\nEXCEPTION : \n" + strTrace.toString() + "\n",
                                  Comment.this);
} catch (IOException e) {
    e.printStackTrace();
    StringWriter strTrace = new StringWriter();
    e.printStackTrace(new PrintWriter(strTrace));
    CrashReportActivity.appendLog(
                                  "\nEXCEPTION : \n" + strTrace.toString() + "\n",
                                  Comment.this);
}
You can find out conversion code from m4a to mp3 in .Net easily, look around by googling (lame or faad some utility like that).




// AAC mp4
NSString *tempDir = NSTemporaryDirectory();
NSString *soundFilePath = [tempDir stringByAppendingPathComponent:@"sound.m4a"];

NSURL *soundFileURL = [NSURL fileURLWithPath:soundFilePath];
NSDictionary *recordSettings = [NSDictionary dictionaryWithObjectsAndKeys:
                                [NSNumber numberWithInt:kAudioFormatMPEG4AAC], AVFormatIDKey,
                                [NSNumber numberWithInt:AVAudioQualityMin], AVEncoderAudioQualityKey,
                                [NSNumber numberWithInt:16], AVEncoderBitRateKey,
                                [NSNumber numberWithInt: 1], AVNumberOfChannelsKey,
                                [NSNumber numberWithFloat:8000.0], AVSampleRateKey,
                                [NSNumber numberWithInt:8], AVLinearPCMBitDepthKey,
                                nil];
#endif


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
    
    [self setIsRecording:FALSE];
    
    [self renderImage];
    [self displaySlideTitleAndPosition];
    [self displayNextPrevControls];
    [self displayPlayStopControl];
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

- (IBAction)onPlayStopButtonClicked:(id)sender {
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
     
- (IBAction)onDeleteSlideButtonClicked:(id)sender {
    int count = [self.editPlayController getSlideCount];
    if (count > 1) {
        [self.editPlayController deleteSlide:self.slideUuid withImage:self.imageFileName withAudio:self.audioFileName];
    }
    else {
        [self deleteSlideData];
    }
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

- (void)startRecording {
    HFLogAlert(@"EditPlayFragmentController.startRecording");
    
    if (!self.audioFileName) {
        [self setAudioFileName:[self getNewAudioFileName]];
        [self.editPlayController updateSlideShareJSON:self.slideUuid withImageFileName:self.imageFileName withAudioFileName:self.audioFileName withText:self.slideUuid];
    }
    
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayAndRecord error:nil];
    
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
    [self.audioRecorder prepareToRecord];
    
    [session setActive:YES error:nil];
    BOOL success = [self.audioRecorder record];
    if (success) {
        [self setIsRecording:TRUE];
        [self.recordingButton setImage:[UIImage imageNamed:@"ic_stoprecording.png"] forState:UIControlStateNormal];
    }
}

- (void)stopRecording {
    HFLogAlert(@"EditPlayFragmentController.stopRecording");
    
    if (self.isRecording) {
        [self.audioRecorder stop];
        [self setIsRecording:FALSE];
    }
    
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setActive:NO error:nil];
    
    [self.recordingButton setImage:[UIImage imageNamed:@"ic_record.png"] forState:UIControlStateNormal];
    [self displayPlayStopControl];
}

- (NSString *)getNewImageFileName {
    return [NSString stringWithFormat:@"%@.jpg", [[NSUUID UUID] UUIDString]];
}

- (NSString *)getNewAudioFileName {
    return [NSString stringWithFormat:@"%@.mp4", [[NSUUID UUID] UUIDString]];
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
// UIImagePickerControllerDelegate methods
//

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary *)info {
    HFLogDebug(@"EditPlayFragmentController.imagePickerController:didFinishPickingMediaWithInfo");
    
    UIImage *image = [info valueForKey:UIImagePickerControllerOriginalImage];
    //[self.imageView setImage:image];
    NSString *imageFileName = self.imageFileName;
    if (!imageFileName) {
        imageFileName = [self getNewImageFileName];
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
        
        [self renderImage];
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
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_editplay_trash_removeslide", nil)]) {
        [self.editPlayController deleteSlide:self.slideUuid withImage:self.imageFileName withAudio:self.audioFileName];
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
    // BUGBUG TODO
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
