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

@interface EditPlayController ()
- (EditPlayFragmentController *)viewControllerAtIndex:(NSUInteger)index;
- (void)initializePageView;
- (void)initializeSlideShareJSON;
- (void)initializeNewSlide:(int)slideIndex;
- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText;
- (void)updateSlideShareJSON:(NSString *)slideUuid withImageFileName:(NSString *)imageFileName withAudioFileName:(NSString *)audioFileName withText:(NSString *)slideText withForcedNulls:(BOOL)forceNulls;
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
    
    EditPlayFragmentController *startingViewController = [self viewControllerAtIndex:0];
    NSArray *viewControllers = @[startingViewController];
    [self.pageViewController setViewControllers:viewControllers direction:UIPageViewControllerNavigationDirectionForward animated:NO completion:nil];
    
    // Change the size of page view controller
    self.pageViewController.view.frame = CGRectMake(0, 0, self.view.frame.size.width, self.view.frame.size.height - 30);
    
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

- (void)initializeNewSlide:(int)slideIndex {
    HFLogDebug(@"EditPlayController.initializeNewSlide:%d", slideIndex);
    
    if (slideIndex < 0) {
        self.currentSlideIndex = 0;
    }
    else {
        self.currentSlideIndex = slideIndex;
    }
    
    [self updateSlideShareJSON:[[NSUUID UUID] UUIDString] withImageFileName:nil withAudioFileName:nil withText:nil];
    
    // m_viewPager.setCurrentItem(m_currentTabPosition); ???
}

#if NEVER
public void addSlide(int newIndex) {
    if(D)Log.d(TAG, String.format("EditPlayActivity.addSlide: newIndex=%d", newIndex));
    
    initializeNewSlide(newIndex);
    
    int count = 0;
    
    try {
        count = m_ssj.getSlideCount();
    }
    catch (Exception e) {
        if(E)Log.d(TAG, "EditPlayActivity.addSlide", e);
        e.printStackTrace();
    }
    catch (OutOfMemoryError e) {
        if(E)Log.d(TAG, "EditPlayActivity.addSlide", e);
        e.printStackTrace();
    }
    
    String toastString = String.format(getString(R.string.toast_addslide_format), m_currentTabPosition + 1, count);
    showToast(toastString);
}
#endif

- (void)addSlide:(int)newIndex {
    HFLogDebug(@"EditPlayController.addSlide:%d", newIndex);
    
    [self initializeNewSlide:newIndex];
    
#if TOAST_IMPLEMENTED
    int count = [self.ssj getSlideCount];
    ...
#endif
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
        // ??? - initializeViewPager();
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
}

- (void)deleteVirtualDirectoryComplete {
    HFLogDebug(@"EditPlayController.deleteVirtualDirectoryComplete");
}

- (void)deleteStoriItemsAndReturnItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"EditPlayController.deleteStoriItemsAndReturnItemsComplete");
}

- (void)uploadComplete:(BOOL)success {
    HFLogDebug(@"EditPlayController.uploadComplete: success=%d", success);
}

- (EditPlayFragmentController *)viewControllerAtIndex:(NSUInteger)index {
    if (([self.ssj getSlideCount] == 0) || (index >= [self.ssj getSlideCount])) {
        HFLogDebug(@"EditPlayController.viewControllerAtIndex:%d - index out of range", index);
        
        return nil;
    }
    
    //
    // Create a new view EditPlayFragmentController and initialize it.
    //
    EditPlayFragmentController *editPlayFragmentController = [self.storyboard instantiateViewControllerWithIdentifier:@"EditPlayFragmentController"];
    
    SlideJSON *sj = [self.ssj getSlideAtIndex:index];
    NSString *slideUuid = [self.ssj getSlideUuidByOrderIndex:index];
    [editPlayFragmentController initializeWithSlideJSON:sj withSlideShareName:self.slideShareName withUuid:slideUuid fromController:self];
    return editPlayFragmentController;
}

//
// UIPageViewControllerDataSource methods
//

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController {
    int index = [self.ssj getOrderIndexForSlide:((EditPlayFragmentController *)viewController).slideUuid];
    
    if (index <= 0) {
        return nil;
    }
    
    index--;
    return [self viewControllerAtIndex:index];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController
{
    int index = [self.ssj getOrderIndexForSlide:((EditPlayFragmentController *)viewController).slideUuid];
    int slideCount = [self.ssj getSlideCount];
    
    if ((index >= slideCount) || (index < 0)) {
        return nil;
    }
    
    index++;
    if (index >= slideCount) {
        return nil;
    }
    
    return [self viewControllerAtIndex:index];
}

- (NSInteger)presentationCountForPageViewController:(UIPageViewController *)pageViewController {
    return [self.ssj getSlideCount];
}

- (NSInteger)presentationIndexForPageViewController:(UIPageViewController *)pageViewController {
    return self.currentSlideIndex;
}


@end
