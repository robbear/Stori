//
//  EditPlayFragmentController.h
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "SlideJSON.h"
#import "EditPlayController.h"

@interface EditPlayFragmentController : UIViewController <UIActionSheetDelegate, UIAlertViewDelegate>

@property (weak, nonatomic) IBOutlet UILabel *userIDLabel;
@property (weak, nonatomic) IBOutlet UILabel *userEmailLabel;
@property (weak, nonatomic) IBOutlet UIButton *listStorisButton;
@property (weak, nonatomic) IBOutlet UIButton *mainMenuButton;
@property (weak, nonatomic) IBOutlet UIButton *insertBeforeButton;
@property (weak, nonatomic) IBOutlet UIButton *insertAfterButton;
@property (weak, nonatomic) IBOutlet UIButton *deleteSlideButton;
@property (weak, nonatomic) IBOutlet UIButton *editTextButton;
@property (weak, nonatomic) IBOutlet UILabel *tempSlideTextLabel;

@property (strong, nonatomic) LoginViewController *loginViewController;

@property (strong, nonatomic) IBOutlet UIImageView *imageView;
@property (strong, nonatomic) NSString *imageFileName;
@property (strong, nonatomic) NSString *audioFileName;
@property (strong, nonatomic) NSString *slideText;
@property (strong, nonatomic) NSString *slideUuid;
@property (strong, nonatomic) NSString *slideSharename;
@property (weak, nonatomic) EditPlayController *editPlayController;

- (void)initializeWithSlideJSON:(SlideJSON *)sj withSlideShareName:(NSString *)slideShareName withUuid:(NSString *)slideUuid
                 fromController:(EditPlayController *)editPlayController;

@end
