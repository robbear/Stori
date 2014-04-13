//
//  EditPlayController.m
//  Stori
//
//  Created by Rob Bearman on 4/1/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayController.h"
#import "EditPlayFragmentController.h"
#import "AmazonSharedPreferences.h"
#import "STOPreferences.h"
#import "STOUtilities.h"
#import "StoriListItem.h"
#import "MBProgressHUD.h"

#define ALERTVIEW_DIALOG_CREATENEW 1
#define ALERTVIEW_DIALOG_STORITITLE 2
#define ALERTVIEW_DIALOG_PUBLISH 3
#define ALERTVIEW_DIALOG_SHARE 4

@interface EditPlayController ()
@property (strong, nonatomic) AWSS3Provider *awsS3Provider;
@property (strong, nonatomic) MBProgressHUD *progressHUD;

- (EditPlayFragmentController *)viewControllerAtIndex:(NSUInteger)index;
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

- (void)viewDidLoad {
    HFLogDebug(@"EditPlayController.viewDidLoad");
    
    [super viewDidLoad];
    
    self.currentSlideIndex = 0;
}

- (void)viewWillAppear:(BOOL)animated {
    [self.navigationController.navigationBar setHidden:YES];
}

- (void)viewWillDisappear:(BOOL)animated {
    [self.navigationController.navigationBar setHidden:NO];
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"EditPlayController.viewDidAppear");
    
    [super viewDidAppear:animated];
    
    if (_userNeedsAuthentication) {
        //
        // Remember: Use the shared instance versions of AmazonClientManager and
        // GPPSignIn for the user-interactive sign in flow. See also LoginViewController.
        //
        [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
        if (![[AmazonClientManager sharedInstance] silentSharedGPlusLogin]) {
            HFLogDebug(@"EditPlayController.viewDidAppear: silentSharedGPlusLogin failed");
            [self googleSignInComplete:FALSE];
        }
    }
}

- (void)didReceiveMemoryWarning {
    HFLogDebug(@"EditPlayController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)initializePageView {
    HFLogDebug(@"EditPlayController.initializePageView");
    
    self.userUuid = [AmazonSharedPreferences userName];
    self.slideShareName = [STOPreferences getEditPlayName];
    
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
    
#if TOAST_IMPLEMENTED
    int count = [self.ssj getSlideCount];
    ...
#endif
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

#if TOAST_IMPLEMENTED
    int count = [self.ssj getSlideCount];
    ...
#endif
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

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (void)googleSignInComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.googleSignInComplete: success=%d", success);
    
    _userNeedsAuthentication = !success;
    
    if (_userNeedsAuthentication) {
        HFLogDebug(@"EditPlayController.googleSignInComplete - _userNeedsAuthentication is still TRUE, so that means login UI is needed");
        
        UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
        self.loginViewController = (LoginViewController *)[storyboard instantiateViewControllerWithIdentifier:@"LoginViewController"];
        [self presentViewController:self.loginViewController animated:YES completion:nil];
    }
    else {
        if (self.loginViewController) {
            [self.loginViewController dismissViewControllerAnimated:NO completion:nil];
            self.loginViewController = nil;
        }
        
        [self initializePageView];
    }
}

- (void) googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.googleDisconnectComplete: success=%d", success);
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
        self.progressHUD = [[MBProgressHUD alloc] initWithView:self.navigationController.view];
        [self.navigationController.view addSubview:self.progressHUD];
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


@end
