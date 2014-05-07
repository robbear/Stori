//
//  EditPlayController.m
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayController.h"
#import "EditPlayFragmentController.h"
#import "AmazonClientManager.h"
#import "AmazonSharedPreferences.h"
#import "STOPreferences.h"
#import "STOUtilities.h"
#import "StoriListItem.h"
#import "HFProgressHUD.h"
#import "iToast.h"

#define ALERTVIEW_DIALOG_CREATENEW 1
#define ALERTVIEW_DIALOG_STORITITLE 2
#define ALERTVIEW_DIALOG_PUBLISH 3
#define ALERTVIEW_DIALOG_SHARE 4
#define ALERTVIEW_DIALOG_SIGNIN 5

@interface EditPlayController ()
@property (nonatomic) BOOL forceToPortrait;
@property (strong, nonatomic) AWSS3Provider *awsS3Provider;
@property (strong, nonatomic) HFProgressHUD *progressHUD;
@property (nonatomic) BOOL disconnectInProgress;
@property (strong, nonatomic) NSString *downloadSlideShareName;
@property (strong, nonatomic) NSString *downloadUserUuid;
@property (nonatomic) BOOL downloadIsForEdit;
@property (strong, nonatomic) StoriDownload *storiDownload;
@property (strong, nonatomic) AsyncImageCopy *asyncImageCopy;

- (EditPlayFragmentController *)viewControllerAtIndex:(NSUInteger)index;
- (void)initiateGoogleSignIn:(BOOL)useErrorMessage;
- (void)initializePageView;
- (void)initializeSlideShareJSON;
- (void)initializeNewSlide:(int)slideIndex;
- (void)initializeNewSlideShow;
- (void)enterStoriTitleAndRecreate;
- (void)finalizeNewStori:(NSString *)title;
- (void)updatePageViewController;
- (void)alertViewForCreateNew:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForStoriTitle:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForPublish:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForShare:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)alertViewForSignIn:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)showToast:(NSString *)toastString;
- (void)editPlayImageTapDetected;
@end

@implementation EditPlayController

bool _userNeedsAuthentication = TRUE;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)onPlayRequestedForUserId:(NSString *)userUuid withStori:(NSString *)slideShareName {
    HFLogDebug(@"EditPlayController.onPlayRequestedForUserId:%@ withStori:%@", userUuid, slideShareName);

    [[PlayStoriNotifier sharedInstance] reset];

    self.downloadIsForEdit = FALSE;
    [self download:FALSE withUserUuid:userUuid withName:slideShareName];
}

- (void)viewDidLoad {
    HFLogDebug(@"EditPlayController.viewDidLoad");
    
    [super viewDidLoad];
    
    // Create NavBar buttons
    UIButton *button1 = [[UIButton alloc] initWithFrame:CGRectMake(0.0, 0.0, 48.0, 48.0)];
    [button1 setImage:[UIImage imageNamed:@"ic_stackmenu.png"] forState:UIControlStateNormal];
    [button1 addTarget:self action:@selector(onMainMenuButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [button1 setImageEdgeInsets:UIEdgeInsetsMake(8.0, 8.0, 8.0, 8.0)];
    self.mainMenuButton = [[UIBarButtonItem alloc] initWithCustomView:button1];
    
    UIButton *button2 = [[UIButton alloc] initWithFrame:CGRectMake(0.0, 0.0, 48.0, 48.0)];
    [button2 setImage:[UIImage imageNamed:@"ic_trash.png"] forState:UIControlStateNormal];
    [button2 addTarget:self action:@selector(onTrashButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [button2 setImageEdgeInsets:UIEdgeInsetsMake(8.0, 8.0, 8.0, 8.0)];
    self.trashButton = [[UIBarButtonItem alloc] initWithCustomView:button2];
    
    UIButton *button3 = [[UIButton alloc] initWithFrame:CGRectMake(0.0, 0.0, 48.0, 48.0)];
    [button3 setImage:[UIImage imageNamed:@"ic_edit.png"] forState:UIControlStateNormal];
    [button3 addTarget:self action:@selector(onEditButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [button3 setImageEdgeInsets:UIEdgeInsetsMake(8.0, 8.0, 8.0, 8.0)];
    self.editButton = [[UIBarButtonItem alloc] initWithCustomView:button3];
    
    UIButton *button4 = [[UIButton alloc] initWithFrame:CGRectMake(0.0, 0.0, 48.0, 48.0)];
    [button4 setImage:[UIImage imageNamed:@"ic_record.png"] forState:UIControlStateNormal];
    [button4 addTarget:self action:@selector(onRecordButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [button4 setImageEdgeInsets:UIEdgeInsetsMake(8.0, 8.0, 8.0, 8.0)];
    self.recordButton = [[UIBarButtonItem alloc] initWithCustomView:button4];
    
    UIButton *button5 = [[UIButton alloc] initWithFrame:CGRectMake(0.0, 0.0, 48.0, 48.0)];
    [button5 setImage:[UIImage imageNamed:@"ic_selectimage.png"] forState:UIControlStateNormal];
    [button5 addTarget:self action:@selector(onSelectPhotoButtonClicked:) forControlEvents:UIControlEventTouchUpInside];
    [button5 setImageEdgeInsets:UIEdgeInsetsMake(8.0, 8.0, 8.0, 8.0)];
    self.selectPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:button5];
    
    NSArray *actionButtonItems = @[self.mainMenuButton, self.trashButton, self.editButton, self.recordButton, self.selectPhotoButton];
    self.navigationItem.rightBarButtonItems = actionButtonItems;
    // End create NavBar buttons
    
    self.extendedLayoutIncludesOpaqueBars = TRUE;
    
    [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleLightContent];
    
    // At startup, take the opportunity to clean up our data folder
    // in case we've erred our way to leaving junk directories around.
    [STOUtilities deleteUnusedDirectories];
    
    if (self.editPlayMode == editPlayModeEdit) {
        // Register our PlayStoriNotifierDelegate
        [PlayStoriNotifier sharedInstance].delegate = self;
    }
    else {
        self.navigationItem.leftBarButtonItem = nil;
#if NEVER
        [self.recordButton setHidden:YES];
        [self.editButton setHidden:YES];
        [self.trashButton setHidden:YES];
#endif
    }

    self.currentSlideIndex = 0;
    self.forceToPortrait = (self.editPlayMode != editPlayModePreview);
    self.shouldDisplayOverlay = TRUE;
    self.disconnectInProgress = FALSE;
    self.downloadSlideShareName = nil;
    self.downloadUserUuid = nil;

    UITapGestureRecognizer *singleTapImage = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(editPlayImageTapDetected)];
    singleTapImage.numberOfTapsRequired = 1;
    self.editPlayImageView.userInteractionEnabled = YES;
    [self.editPlayImageView addGestureRecognizer:singleTapImage];

    if ([[AmazonClientManager sharedInstance] isLoggedIn]) {
        [self.editPlayImageView setHidden:YES];
    }
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"EditPlayController.viewDidAppear");
    
    [super viewDidAppear:animated];
    
#if NEVER // test
    UIBarButtonItem *itemLeft = self.navigationItem.leftBarButtonItem;
    UIView *viewLeft = [itemLeft valueForKey:@"view"];
    // REM: handle case if view is nil, in case valueForKey fails in future versions
    CGRect rectLeftItem = viewLeft.frame;
    
    UIBarButtonItem *itemRight = self.navigationItem.rightBarButtonItem;
    UIView *viewRight = [itemRight valueForKeyPath:@"view"];
    CGRect rectRightItem = viewRight.frame;
#endif

    if (self.editPlayMode != editPlayModeEdit) {
        HFLogDebug(@"EditPlayController.viewDidAppear - in editPlayModePreview - skipping authentication");
        [self initializePageView];
        return;
    }
    else {
        [self.selectPhotoButton setImage:[UIImage imageNamed:@"ic_selectimage.png"]];
    }
    
    if (_userNeedsAuthentication && !self.disconnectInProgress) {
        self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
        self.progressHUD.mode = MBProgressHUDModeIndeterminate;
        [self.progressHUD show:TRUE];
        
        //
        // Remember: Use the shared instance versions of AmazonClientManager and
        // GPPSignIn for the user-interactive sign in flow. See also LoginViewController.
        //
        [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
        if (![[AmazonClientManager sharedInstance] silentSharedGPlusLogin]) {
            HFLogDebug(@"EditPlayController.viewDidAppear: silentSharedGPlusLogin failed");
            [self initiateGoogleSignIn:FALSE];
        }
    }
    else if (self.downloadUserUuid && self.downloadSlideShareName) {
        NSString *userUuid = self.downloadUserUuid;
        NSString *slideShareName = self.downloadSlideShareName;
        
        self.downloadUserUuid = nil;
        self.downloadSlideShareName = nil;
        
        [self download:self.downloadIsForEdit withUserUuid:userUuid withName:slideShareName];
    }
}

- (void)viewWillLayoutSubviews {
    HFLogDebug(@"EditPlayController.viewWillLayoutSubviews");
    
    CGRect screenFrame = [[UIScreen mainScreen] bounds];
    
    if (UIInterfaceOrientationIsLandscape([[UIApplication sharedApplication] statusBarOrientation])) {
        screenFrame = CGRectMake(0, 0, screenFrame.size.height, screenFrame.size.width);
    }
    
    self.pageViewController.view.frame = screenFrame;
}

- (void)didReceiveMemoryWarning {
    HFLogDebug(@"EditPlayController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)editPlayImageTapDetected {
    HFLogDebug(@"EditPlayController.editPlayImageTapDetected");
    
    // Regenerate the OOBE
    [self initiateGoogleSignIn:FALSE];
}

- (void)initializePageView {
    HFLogDebug(@"EditPlayController.initializePageView");
    
    self.userUuid = [AmazonSharedPreferences userName];
    if (self.editPlayMode == editPlayModePlay) {
        self.slideShareName = [STOPreferences getPlaySlidesName];
    }
    else {
        self.slideShareName = [STOPreferences getEditPlayName];
    }
    
    if (!self.slideShareName) {
        self.slideShareName = [[NSUUID UUID] UUIDString];
        
        [STOPreferences saveEditPlayName:self.slideShareName];
    }
    
    [STOUtilities createOrGetSlideShareDirectory:self.slideShareName];
    
    HFLogDebug(@"EditPlayController.initializePageView - in edit mode slideShareName=%@", self.slideShareName);
    
    [self initializeSlideShareJSON];
    
    //
    // Check for OOBE
    //
    int count = [self.ssj getSlideCount];
    if (count <= 0) {
        HFLogDebug(@"EditPlayController.initializePageView - OOBE case. Create first slide");
        [self initializeNewSlide:self.currentSlideIndex];
    }
    
    // Remove any previous UIPageViewController from the view tree
    for (UIView *subview in self.view.subviews) {
        if ([subview isKindOfClass:[self.pageViewController.view class]]) {
            [subview removeFromSuperview];
        }
    }
    
    // Create page view controller
    self.pageViewController = [self.storyboard instantiateViewControllerWithIdentifier:@"EditPlayPageViewController"];
    self.pageViewController.dataSource = self;
    self.pageViewController.delegate = self;
    
    [self updatePageViewController];
    
    [self addChildViewController:_pageViewController];
    [self.view addSubview:_pageViewController.view];
    [self.pageViewController didMoveToParentViewController:self];
}


- (void)initializeSlideShareJSON {
    HFLogDebug(@"EditPlayController.initializeSlideShareJSON");
    
    self.ssj = [SlideShareJSON loadFromFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
    if (!self.ssj) {
        self.ssj = [[SlideShareJSON alloc] init];
        [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
    }
    
    HFLogDebug(@"EditPlayController.initializeSlideShareJSON - here is the JSON:");
    [STOUtilities printSlideShareJSON:self.ssj];
}

- (void)updatePageViewController {
    HFLogDebug(@"EditPlayController.updatePageViewController");
    
    EditPlayFragmentController *startingViewController = [self viewControllerAtIndex:self.currentSlideIndex];
    NSArray *viewControllers = @[startingViewController];

    // See: http://stackoverflow.com/questions/13633059/uipageviewcontroller-how-do-i-correctly-jump-to-a-specific-page-without-messing
    __weak UIPageViewController* pvcw = self.pageViewController;
    [self.pageViewController setViewControllers:viewControllers
                  direction:UIPageViewControllerNavigationDirectionForward
                   animated:NO completion:^(BOOL finished) {
                       UIPageViewController* pvcs = pvcw;
                       if (!pvcs) return;
                       dispatch_async(dispatch_get_main_queue(), ^{
                           [pvcs setViewControllers:viewControllers
                                          direction:UIPageViewControllerNavigationDirectionForward
                                           animated:NO completion:nil];
                       });
                   }];
}

- (void)notifyForDownloadRequest:(BOOL)downloadIsForEdit withUserUuid:(NSString *)userUuid withName:(NSString *)slideShareName {
    self.downloadIsForEdit = downloadIsForEdit;
    self.downloadUserUuid = userUuid == nil ? self.userUuid : userUuid;
    self.downloadSlideShareName = slideShareName;
}

- (void)download:(BOOL)downloadIsForEdit withUserUuid:(NSString *)userUuid withName:(NSString *)slideShareName {
    HFLogDebug(@"EditPlayController.download: downloadIsForEdit:%d, userUuid=%@, name=%@", downloadIsForEdit, userUuid, slideShareName);
    
    if (!userUuid || !slideShareName) {
        HFLogDebug(@"EditPlayController.download - invalid userUuid or slideShareName. Bailing");
        return;
    }

    self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:TRUE];

    if (!self.storiDownload) {
        self.storiDownload = [[StoriDownload alloc] initWithDelegate:self];
    }

    [self.storiDownload startDownload:userUuid withName:slideShareName downloadIsForEdit:downloadIsForEdit];
}

- (void)initializeNewSlideShow {
    HFLogDebug(@"EditPlayController.initializeNewSlideShow");
    
    if (self.slideShareName) {
        [STOUtilities deleteSlideShareDirectory:self.slideShareName];
    }
    
    self.slideShareName = [[NSUUID UUID] UUIDString];
    [STOPreferences saveEditPlayName:self.slideShareName];
    [STOUtilities createOrGetSlideShareDirectory:self.slideShareName];
    [self enterStoriTitleAndRecreate];
}

- (void)enterStoriTitleAndRecreate {
    HFLogDebug(@"EditPlayController.enterStoriTitleAndRecreate");
    
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_storititle_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_storititle_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"usedefault_button", nil)
                                           otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
    dialog.alertViewStyle = UIAlertViewStylePlainTextInput;
    dialog.tag = ALERTVIEW_DIALOG_STORITITLE;
    UITextField *textField = [dialog textFieldAtIndex:0];
    textField.tag = ALERTVIEW_DIALOG_STORITITLE;
    textField.text = NSLocalizedString(@"default_stori_title", nil);
    [textField setSelected:TRUE];
    
    [dialog show];
}

- (void)finalizeNewStori:(NSString *)title {
    HFLogDebug(@"EditPlayController.finalizeNewStori");

    [self initializeSlideShareJSON];
    [self setSlideShareTitle:title];
    
    self.currentSlideIndex = 0;
    [self initializeNewSlide:self.currentSlideIndex];
}

- (void)initializeNewSlide:(int)slideIndex {
    HFLogDebug(@"EditPlayController.initializeNewSlide:%d", slideIndex);
    
    if (slideIndex < 0) {
        self.currentSlideIndex = 0;
    }
    else {
        self.currentSlideIndex = slideIndex;
    }
    
    [self updateSlideShareJSON:[[NSUUID UUID] UUIDString] withImageFileName:nil withAudioFileName:nil withText:nil];
    [self updatePageViewController];
}

- (void)showToast:(NSString *)toastString {
    [[[[iToast makeText:toastString] setGravity:iToastGravityCenter offsetLeft:0 offsetTop:-60] setDuration:iToastDurationNormal] show];
}

- (void)createNewSlideShow {
    HFLogDebug(@"EditPlayController.createNewSlideShow");

    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_createnew_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_createnew_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"editplay_createnew_button", nil), nil];
    dialog.tag = ALERTVIEW_DIALOG_CREATENEW;
    [dialog show];
}

- (void)addSlide:(int)newIndex {
    HFLogDebug(@"EditPlayController.addSlide:%d", newIndex);
    
    [self initializeNewSlide:newIndex];
    
    int count = [self.ssj getSlideCount];
    NSString *toast = [NSString stringWithFormat:NSLocalizedString(@"toast_addslide_format", nil), self.currentSlideIndex + 1, count];
    [self showToast:toast];
}

- (void)deleteSlide:(NSString *)slideUuid withImage:(NSString *)imageFileName withAudio:audioFileName {
    HFLogDebug(@"EditPlayController.deleteSlide:%@ withImage:%@ withAudio:%@", slideUuid, imageFileName, audioFileName);
    HFLogDebug(@"Before slide deletion:");
    [STOUtilities printSlideShareJSON:self.ssj];
    
    int count = 0;
    
    if (imageFileName) {
        [STOUtilities deleteFileAtFolder:self.slideShareName withFileName:imageFileName];
    }
    
    if (audioFileName) {
        [STOUtilities deleteFileAtFolder:self.slideShareName withFileName:audioFileName];
    }
    
    [self.ssj removeSlideBySlideId:slideUuid];
    [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
    count = [self.ssj getSlideCount];
    
    HFLogDebug(@"After slide deletion");
    [STOUtilities printSlideShareJSON:self.ssj];

    if (self.currentSlideIndex >= count) {
        self.currentSlideIndex--;
    }
    
    [self updatePageViewController];

    NSString *toast = [NSString stringWithFormat:NSLocalizedString(@"toast_deleteslide_format", nil), self.currentSlideIndex + 1, count];
    [self showToast:toast];
}

- (void)copyImageFilesToPhotosFolder:(NSString *)slideUuid {
    self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:TRUE];
    
    NSMutableArray *arrayImageFileNames = [[NSMutableArray alloc] init];
    
    if (slideUuid) {
        SlideJSON *sj = [self.ssj getSlideBySlideId:slideUuid];
        [arrayImageFileNames addObject:sj.getImageFilename];
    }
    else {
        [arrayImageFileNames addObjectsFromArray:[self.ssj getImageFileNames]];
    }
    
    self.asyncImageCopy = [[AsyncImageCopy alloc] initWithDelegate:self];
    [self.asyncImageCopy copyImageFiles:arrayImageFileNames atFolder:self.slideShareName];
}

- (void)onAsyncImageCopyComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.onAsyncImageCopyComplete: success=%d", success);
    
    self.asyncImageCopy = nil;
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    
    if (!success) {
        UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"copy_images_error_dialog_title", nil)
                                                         message:NSLocalizedString(@"copy_images_error_dialog_message", nil)
                                                        delegate:self
                                               cancelButtonTitle:nil
                                               otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
        dialog.tag = -1;
        [dialog show];
    }
}

- (void)deleteImage:(NSString *)slideUuid withImage:(NSString *)imageFileName {
    if (imageFileName) {
        [STOUtilities deleteFileAtFolder:self.slideShareName withFileName:imageFileName];
    }

    NSString *audioFileName = nil;
    NSString *slideText = nil;
    
    SlideJSON *sj = [self.ssj getSlideBySlideId:slideUuid];
    audioFileName = [sj getAudioFilename];
    slideText = [sj getText];
    
    [self updateSlideShareJSON:slideUuid withImageFileName:nil withAudioFileName:audioFileName withText:slideText withForcedNulls:TRUE];
}

- (void)deleteAudio:(NSString *)slideUuid withAudio:(NSString *)audioFileName {
    if (audioFileName) {
        [STOUtilities deleteFileAtFolder:self.slideShareName withFileName:audioFileName];
    }
    
    NSString *imageFileName = nil;
    NSString *slideText = nil;
        
    SlideJSON *sj = [self.ssj getSlideBySlideId:slideUuid];
    imageFileName = [sj getImageFilename];
    slideText = [sj getText];
        
    [self updateSlideShareJSON:slideUuid withImageFileName:imageFileName withAudioFileName:nil withText:slideText withForcedNulls:TRUE];
}

- (NSString *)getSlideText:(NSString *)slideUuid {
    SlideJSON *sj = [self.ssj getSlideBySlideId:slideUuid];
    return sj.getText;
}

- (int)getSlideCount {
    return [self.ssj getSlideCount];
}

- (int)getSlidePosition:(NSString *)slideUuid {
    return [self.ssj getOrderIndexForSlide:slideUuid];
}

- (NSString *)getSlidesTitle {
    return [self.ssj getTitle];
}

- (void)setSlideShareTitle:(NSString *)title {
    if (!title || title.length <= 1) {
        title = NSLocalizedString(@"default_stori_title", nil);
    }
    
    [self.ssj setTitle:title];
    [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
}

- (void)setCurrentSlidePosition:(int)position {
    [self setCurrentSlideIndex:position];
    [self updatePageViewController];
}

- (BOOL)isPublished {
    return [self.ssj isPublished];
}

- (void)shareSlides {
    NSString *title = [self.ssj getTitle];
    [STOUtilities shareShow:self withUserUuid:self.userUuid withSlideShareName:self.slideShareName withTitle:title];
}

- (void)publishSlides {
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_publish_dialog_title", nil)
                                                     message:NSLocalizedString(@"editplay_publish_dialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                                           otherButtonTitles:NSLocalizedString(@"editplay_publish_button", nil), nil];
    dialog.tag = ALERTVIEW_DIALOG_PUBLISH;
    [dialog show];
}

- (void)reorderCurrentSlideTo:(int)slideIndex {
    if (self.currentSlideIndex == slideIndex) {
        return;
    }
    
    [self.ssj reorderWithCurrentPosition:self.currentSlideIndex atNewPosition:slideIndex];
    [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
    
    self.currentSlideIndex = slideIndex;
    [self updatePageViewController];

    int count = [self.ssj getSlideCount];
    NSString *toast = [NSString stringWithFormat:NSLocalizedString(@"toast_reorderslides_format", nil), self.currentSlideIndex + 1, count];
    [self showToast:toast];
}

- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText {
    [self updateSlideShareJSON:slideUuid withImageFileName:imageFileName withAudioFileName:audioFileName withText:slideText withForcedNulls:NO];
}

- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText withForcedNulls:(BOOL)forceNulls {
    
    HFLogDebug(@"EditPlayController.updateSlideShareJSON:%@ withImageFileName:%@ withAudioFileName:%@ withText:%@ withForcedNulls:%d", slideUuid, imageFileName, audioFileName, slideText, forceNulls);
    
    HFLogDebug(@"Current JSON:");
    [STOUtilities printSlideShareJSON:self.ssj];
    
    BOOL needsUpdate = FALSE;
    
    NSString *imageUrl = [STOUtilities buildResourceUrlString:self.userUuid withSlideShareName:self.slideShareName withFileName:imageFileName];
    NSString *audioUrl = [STOUtilities buildResourceUrlString:self.userUuid withSlideShareName:self.slideShareName withFileName:audioFileName];
    
    needsUpdate = ([self.ssj getSlideBySlideId:slideUuid] == nil);
    
    [self.ssj upsertSlideWithSlideId:slideUuid atIndex:self.currentSlideIndex withImageUrl:imageUrl withAudioUrl:audioUrl withSlideText:slideText forceNulls:forceNulls];
    [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
    
    if (needsUpdate) {
        [self updatePageViewController];
    }
    
    HFLogDebug(@"After update:");
    [STOUtilities printSlideShareJSON:self.ssj];
}

- (void)disconnectFromGoogle {
    HFLogDebug(@"EditPlayController.disconnectFromGoogle");
    
    self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:TRUE];
    
    self.disconnectInProgress = TRUE;
    
    //
    // Remember: Use the shared instance versions of
    // AmazonClientManager and GPSignIn for the user-interactive
    // sign in and sign out flows.
    //
    [[AmazonClientManager sharedInstance] disconnectFromSharedGoogle];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    if ([segue.identifier isEqualToString:@"SegueToEditPlayController"]) {
        EditPlayController *epc = (EditPlayController *)segue.destinationViewController;
        epc.editPlayMode = editPlayModePlay;
    }
}

- (void)initiateGoogleSignIn:(BOOL)useErrorMessage {
    HFLogDebug(@"EditPlayController.initiateGoogleSignIn");

    NSString *message = NSLocalizedString(@"login_dialog_message", nil);
    if (useErrorMessage) {
        message = NSLocalizedString(@"login_dialog_error_message", nil);
    }
    
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"login_dialog_title", nil)
                                                     message:message
                                                    delegate:self
                                           cancelButtonTitle:nil
                                           otherButtonTitles:NSLocalizedString(@"login_dialog_continue_button", nil), nil];
    dialog.tag = ALERTVIEW_DIALOG_SIGNIN;
    [dialog show];
}

- (void)googleSignInComplete:(BOOL)success withError:(NSError *)error {
    HFLogDebug(@"EditPlayController.googleSignInComplete: success=%d, error=%@", success, error == nil ? @"nil" : error.description);
    
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    
    _userNeedsAuthentication = !success;
    if (success) {
        self.disconnectInProgress = FALSE;
    }
    
    if (_userNeedsAuthentication) {
        HFLogDebug(@"EditPlayController.googleSignInComplete - _userNeedsAuthentication is still TRUE, so that means login UI is needed");
        
        [self initiateGoogleSignIn:(error != nil)];
    }
    else {
        // Hide the EditPlayController's icon image view, letting the background go fully to black
        [self.editPlayImageView setHidden:YES];
        
        [self initializePageView];
 
        //
        // Check if we have a pending play notification.
        //
        PlayStoriNotifier *psn = [PlayStoriNotifier sharedInstance];
        if ([psn hasPendingRequest]) {
            HFLogDebug(@"EditPlayController.googleSignInComplete - have pending play request. Executing it.");
            [self onPlayRequestedForUserId:psn.userUuid withStori:psn.slideShareName];
        }
    }
}

- (void)googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.googleDisconnectComplete: success=%d", success);
    
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    
    if (success) {
        [STOUtilities deleteSlideShareDirectory:self.slideShareName];
        self.slideShareName = nil;
        [STOPreferences saveEditPlayName:nil];

        [[AmazonClientManager sharedInstance] wipeAllCredentials];

        // Clear the view
        self.currentSlideIndex = 0;
        [self initializePageView];

        [self initiateGoogleSignIn:FALSE];
    }
    else {
        UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_disconnect_failure_dialog_title", nil)
                                                         message:NSLocalizedString(@"editplay_disconnect_failure_dialog_message", nil)
                                                        delegate:nil
                                               cancelButtonTitle:nil
                                               otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
        [dialog show];
    }
}

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"EditPlayController.getStoriItemsComplete - found %d S3 objects", [arrayItems count]);
    
    BOOL isEditOfPublished = FALSE;
    if (arrayItems) {
        for (int i = 0; i < arrayItems.count; i++) {
            StoriListItem *sli = (StoriListItem *)[arrayItems objectAtIndex:i];
            NSString *slideShareName = [sli slideShareName];
            if ([slideShareName isEqualToString:self.slideShareName]) {
                HFLogDebug(@"EditPlayController.getStoriItemsComplete - this Stori is a republish");
                isEditOfPublished = TRUE;
                break;
            }
        }
    }
    
    if (!isEditOfPublished && arrayItems && arrayItems.count >= MAX_PUBLISHED_FOR_FREE) {
        HFLogDebug(@"EditPlayController.getStoriItemsComplete - MAX_PUBLISHED_FOR_FREE is exceeded. Don't publish.");
        
        [self.progressHUD hide:TRUE];
        self.progressHUD = nil;
        [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
        
        NSString *message = [NSString stringWithFormat:NSLocalizedString(@"editplay_maxpublishedexceeded_dialog_message", nil), MAX_PUBLISHED_FOR_FREE];
        UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_maxpublishedexcceeded_dialog_title", nil)
                                                         message:message
                                                        delegate:self
                                               cancelButtonTitle:nil
                                               otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
        dialog.tag = -1;
        [dialog show];
        self.awsS3Provider = nil;
        return;
    }
    
    // Upload...
    HFLogDebug(@"EditPlayController.getStoriItemsComplete - ready to upload...");
    [self.awsS3Provider uploadAsync:self.slideShareName];
}

- (void)deleteVirtualDirectoryComplete {
    HFLogDebug(@"EditPlayController.deleteVirtualDirectoryComplete");

    self.awsS3Provider = nil;
}

- (void)deleteStoriItemsAndReturnItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"EditPlayController.deleteStoriItemsAndReturnItemsComplete");

    self.awsS3Provider = nil;
}

- (void)uploadComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.uploadComplete: success=%d", success);
    
    self.awsS3Provider = nil;

    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
    
    if (success) {
        int curVersion = [self.ssj getVersion];
        [self.ssj setVersion:curVersion + 1];
        [self.ssj saveToFolder:self.slideShareName withFileName:SLIDESHARE_JSON_FILENAME];
        HFLogDebug(@"SlideShareJSON after publish:");
        [STOUtilities printSlideShareJSON:self.ssj];

        UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_upload_dialog_complete_title", nil)
                                                        message:NSLocalizedString(@"editplay_upload_dialog_complete_message", nil)
                                                        delegate:self
                                                        cancelButtonTitle:NSLocalizedString(@"menu_no", nil)
                                                        otherButtonTitles:NSLocalizedString(@"editplay_share_button", nil), nil];
        dialog.tag = ALERTVIEW_DIALOG_SHARE;
        [dialog show];
    }
    else {
        // BUGBUG - need failure message
        NSString *message = [NSString stringWithFormat:NSLocalizedString(@"editplay_upload_dialog_failure_message_format", nil), "Failed"];
        UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"editplay_upload_dialog_failure_title", nil)
                                                         message:message
                                                        delegate:self
                                               cancelButtonTitle:nil
                                               otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
        [dialog show];
    }
}

- (IBAction)onSelectPhotoButtonClicked:(id)sender {
    HFLogDebug(@"EditPlayController.onSelectPhotoButtonClicked");

    if (self.editPlayNavBarButtonDelegate) {
        if ([self.editPlayNavBarButtonDelegate respondsToSelector:@selector(onNavBarSelectPhotoButtonClicked)]) {
            [self.editPlayNavBarButtonDelegate onNavBarSelectPhotoButtonClicked];
        }
    }
}

- (IBAction)onRecordButtonClicked:(id)sender {
    HFLogDebug(@"EditPlayController.onRecordButtonClicked");

    if (self.editPlayNavBarButtonDelegate) {
        if ([self.editPlayNavBarButtonDelegate respondsToSelector:@selector(onNavBarRecordingButtonClicked)]) {
            [self.editPlayNavBarButtonDelegate onNavBarRecordingButtonClicked];
        }
    }
}

- (IBAction)onEditButtonClicked:(id)sender {
    HFLogDebug(@"EditPlayController.onEditButtonClicked");

    if (self.editPlayNavBarButtonDelegate) {
        if ([self.editPlayNavBarButtonDelegate respondsToSelector:@selector(onNavBarEditButtonClicked)]) {
            [self.editPlayNavBarButtonDelegate onNavBarEditButtonClicked];
        }
    }
}

- (IBAction)onTrashButtonClicked:(id)sender {
    HFLogDebug(@"EditPlayController.onTrashButtonClicked");
    
    if (self.editPlayNavBarButtonDelegate) {
        if ([self.editPlayNavBarButtonDelegate respondsToSelector:@selector(onNavBarTrashButtonClicked)]) {
            [self.editPlayNavBarButtonDelegate onNavBarTrashButtonClicked];
        }
    }
}

- (IBAction)onMainMenuButtonClicked:(id)sender {
    HFLogDebug(@"EditPlayController.onMainMenuButtonClicked");
    
    if (self.editPlayNavBarButtonDelegate) {
        if ([self.editPlayNavBarButtonDelegate respondsToSelector:@selector(onNavBarMainMenuButtonClicked)]) {
            [self.editPlayNavBarButtonDelegate onNavBarMainMenuButtonClicked];
        }
    }
}

- (EditPlayFragmentController *)viewControllerAtIndex:(NSUInteger)index {
    if (!self.ssj) {
        return nil;
    }
    
    if (([self.ssj getSlideCount] == 0) || (index >= [self.ssj getSlideCount])) {
        HFLogDebug(@"EditPlayController.viewControllerAtIndex:%d - index out of range", index);
        
        return nil;
    }
    
    //
    // Create a new view EditPlayFragmentController and initialize it.
    //
    EditPlayFragmentController *editPlayFragmentController = [self.storyboard instantiateViewControllerWithIdentifier:@"EditPlayFragmentController"];
    
    SlideJSON *sj = [self.ssj getSlideAtIndex:(int)index];
    NSString *slideUuid = [self.ssj getSlideUuidByOrderIndex:(int)index];
    [editPlayFragmentController initializeWithSlideJSON:sj withSlideShareName:self.slideShareName withUuid:slideUuid fromController:self];
    return editPlayFragmentController;
}

- (void)playCurrentPlayStori {
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle: nil];
    EditPlayController *epc = [storyboard instantiateViewControllerWithIdentifier:@"EditPlayController"];
    epc.editPlayMode = editPlayModePlay;
    [self.navigationController pushViewController:epc animated:YES];
}

//
// UIPageViewControllerDelegate methods
//

- (void)pageViewController:(UIPageViewController *)pageViewController didFinishAnimating:(BOOL)finished previousViewControllers:(NSArray *)previousViewControllers transitionCompleted:(BOOL)completed {
    HFLogDebug(@"EditPlayController:pageViewController:didFinishAnimating:transitionCompleted:%d", completed);

    self.currentSlideIndex = self.pendingSlideIndex;
    HFLogDebug(@"EditPlayController.pageViewController:didFinishAnimating - currentSlideIndex=%d", self.currentSlideIndex);
    
    EditPlayFragmentController *epfc = (EditPlayFragmentController *)previousViewControllers[0];
    [epfc onEditPlayFragmentWillBeDeselected];
}

- (void)pageViewController:(UIPageViewController *)pageViewController willTransitionToViewControllers:(NSArray *)pendingViewControllers {
    HFLogDebug(@"EditPlayController.willTransitionToControllers");

    EditPlayFragmentController *epfc = (EditPlayFragmentController *)pendingViewControllers[0];
    NSString *slideUuid = epfc.slideUuid;
    self.pendingSlideIndex = [self.ssj getOrderIndexForSlide:slideUuid];
    
    [epfc onEditPlayFragmentWillBeSelected];
    
    HFLogDebug(@"EditPlayController.willTransitionToControllers: pendingSlideIndex=%d", self.pendingSlideIndex);
}

//
// UIPageViewControllerDataSource methods
//

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController {
    if (!self.ssj) {
        return nil;
    }
    
    int index = [self.ssj getOrderIndexForSlide:((EditPlayFragmentController *)viewController).slideUuid];
    
    HFLogDebug(@"EditPlayController.pageViewController:viewControllerBeforeViewController - index=%d", index);
    
    if (index <= 0) {
        return nil;
    }
    
    index--;
    return [self viewControllerAtIndex:index];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController
{
    if (!self.ssj) {
        return nil;
    }
    
    int index = [self.ssj getOrderIndexForSlide:((EditPlayFragmentController *)viewController).slideUuid];
    int slideCount = [self.ssj getSlideCount];
    
    HFLogDebug(@"EditPlayController.pageViewController:viewControllerAfterViewController - index=%d", index);

    if ((index >= slideCount) || (index < 0)) {
        return nil;
    }
    
    index++;
    if (index >= slideCount) {
        return nil;
    }
    
    return [self viewControllerAtIndex:index];
}

#if NEVER // Implement only if page "dots" are required
- (NSInteger)presentationCountForPageViewController:(UIPageViewController *)pageViewController {
    int count = [self.ssj getSlideCount];
    
    HFLogDebug(@"EditPlayController.presentationCountForPageViewController: returning %d", count);
    
    return count;
}

- (NSInteger)presentationIndexForPageViewController:(UIPageViewController *)pageViewController {
    HFLogDebug(@"EditPlayController.presentationIndexForPageViewController: returning %d", self.currentSlideIndex);
    
    return self.currentSlideIndex;
}
#endif

//
// UIAlertViewDelegate methods
//

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    switch (alertView.tag) {
        case ALERTVIEW_DIALOG_CREATENEW:
            [self alertViewForCreateNew:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_STORITITLE:
            [self alertViewForStoriTitle:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_PUBLISH:
            [self alertViewForPublish:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_SHARE:
            [self alertViewForShare:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_SIGNIN:
            [self alertViewForSignIn:alertView clickedButtonAtIndex:buttonIndex];
            break;
            
        default:
            break;
    }
}

- (void)alertViewForShare:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"editplay_share_button", nil)]) {
        [self shareSlides];
    }
}

- (void)alertViewForPublish:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"editplay_publish_button", nil)]) {
        self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
        self.progressHUD.labelText = NSLocalizedString(@"editplay_uploadprogress_dialog_title", nil);
        self.progressHUD.mode = MBProgressHUDModeIndeterminate;
        [self.progressHUD show:TRUE];
        [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;
        
        self.awsS3Provider = [[AWSS3Provider alloc] init];
        [self.awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
        [self.awsS3Provider getStoriItemsAsync];
    }
}

- (void)alertViewForCreateNew:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"editplay_createnew_button", nil)]) {
        [self initializeNewSlideShow];
    }
}

- (void)alertViewForStoriTitle:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *title = NSLocalizedString(@"default_stori_title", nil);
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        UITextField *textField = [alertView textFieldAtIndex:0];
        title = textField.text;
    }
    
    [self finalizeNewStori:title];
}

- (void)alertViewForSignIn:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"login_dialog_continue_button", nil)]) {
        [[AmazonClientManager sharedInstance] initSharedGPlusLogin];
        [[GPPSignIn sharedInstance] authenticate];
    }
}

//
// StoriDownloadDelegate methods
//
- (void)download:(NSString *)urlString didStopWithError:(NSError *)error {
    HFLogDebug(@"EditPlayController.download:%@ didStopWithError:%@", urlString, error.description);
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    
    if (self.downloadIsForEdit) {
        [STOPreferences saveEditPlayName:nil];
    }
    else {
        [STOPreferences savePlaySlidesName:nil];
    }
    
    self.currentSlideIndex = 0;
    [self initializePageView];
    
    UIAlertView *dialog = [[UIAlertView alloc] initWithTitle:NSLocalizedString(@"download_errordialog_title", nil)
                                                     message:NSLocalizedString(@"download_errordialog_message", nil)
                                                    delegate:self
                                           cancelButtonTitle:nil
                                           otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
    dialog.tag = -1;
    [dialog show];
}

- (void)didFinishWithSuccess:(BOOL)success withName:(NSString *)slideShareName {
    HFLogDebug(@"EditPlayController.didFinishWithSuccess - download returns success=%d for %@", success, slideShareName);
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    
    if (self.downloadIsForEdit) {
        if (success) {
            [STOPreferences saveEditPlayName:slideShareName];
            self.slideShareName = slideShareName;
        }
        self.currentSlideIndex = 0;
        [self initializePageView];
    }
    else if (success) {
        [STOPreferences savePlaySlidesName:slideShareName];
        [self playCurrentPlayStori];
    }
}

@end
