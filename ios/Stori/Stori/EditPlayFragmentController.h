//
//  EditPlayFragmentController.h
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "SlideJSON.h"
#import "EditPlayController.h"
#import "TapDetectingImageView.h"

@interface EditPlayFragmentController : UIViewController
    <UIActionSheetDelegate, UIAlertViewDelegate, UINavigationControllerDelegate, UIImagePickerControllerDelegate, UITextFieldDelegate, AVAudioRecorderDelegate, AVAudioPlayerDelegate, EditPlayControllerNavBarButtonDelegate, UIScrollViewDelegate, TapDetectingImageViewDelegate>

@property (weak, nonatomic) IBOutlet UIButton *insertBeforeButton;
@property (weak, nonatomic) IBOutlet UIButton *insertAfterButton;
@property (weak, nonatomic) IBOutlet UILabel *storiTitleLabel;
@property (weak, nonatomic) IBOutlet UILabel *slidePositionLabel;
@property (weak, nonatomic) IBOutlet UIButton *leftArrowButton;
@property (weak, nonatomic) IBOutlet UIButton *rightArrowButton;
@property (weak, nonatomic) IBOutlet UIButton *playStopButton;
@property (weak, nonatomic) IBOutlet UIView *overlayView;
@property (weak, nonatomic) IBOutlet UITextView *slideTextView;
@property (weak, nonatomic) IBOutlet UIButton *choosePictureLabel;
@property (weak, nonatomic) IBOutlet UIButton *selectPhotoSecondaryButton;

@property (weak, nonatomic) IBOutlet UIScrollView *scrollView;
@property (strong, nonatomic) TapDetectingImageView *imageView;
@property (strong, nonatomic) NSString *imageFileName;
@property (strong, nonatomic) NSString *audioFileName;
@property (strong, nonatomic) NSString *slideText;
@property (strong, nonatomic) NSString *slideUuid;
@property (strong, nonatomic) NSString *slideSharename;
@property (weak, nonatomic) EditPlayController *editPlayController;

- (void)initializeWithSlideJSON:(SlideJSON *)sj withSlideShareName:(NSString *)slideShareName withUuid:(NSString *)slideUuid
                 fromController:(EditPlayController *)editPlayController;
- (void)onEditPlayFragmentWillBeSelected;
- (void)onEditPlayFragmentWillBeDeselected;

@end
