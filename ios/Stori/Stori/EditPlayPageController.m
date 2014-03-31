//
//  EditPlayPageController.m
//  Stori
//
//  Created by Rob Bearman on 3/31/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayPageController.h"
#import "SlideJSON.h"
#import "AmazonSharedPreferences.h"
#import "StoriListItem.h"
#import "StoriListController.h"

@interface EditPlayPageController ()
- (EditPlayController *)viewControllerAtIndex:(NSUInteger)index;
@end

@implementation EditPlayPageController

bool _userNeedsAuthentication = TRUE;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    HFLogDebug(@"EditPlayPageController.initWithNibName");
    
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    HFLogDebug(@"EditPlayPageController.viewDidLoad");
    
    [super viewDidLoad];
    
    self.currentSlideIndex = 0;
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"EditPlayPageController.viewDidAppear");
    
    [super viewDidAppear:animated];
    
    if (_userNeedsAuthentication) {
        //
        // Remember: Use the shared instance versions of AmazonClientManager and
        // GPPSignIn for the user-interactive sign in flow. See also LoginViewController.
        //
        [AmazonClientManager sharedInstance].amazonClientManagerGoogleAccountDelegate = self;
        if (![[AmazonClientManager sharedInstance] silentSharedGPlusLogin]) {
            HFLogDebug(@"EditPlayPageController.viewDidAppear: silentSharedGPlusLogin failed");
            [self googleSignInComplete:FALSE];
        }
    }
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
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

- (void)refreshInterface {
    HFLogDebug(@"EditPlayPageController.refreshInterface");
    
    HFLogDebug(@"EditPlayPageController.refreshInterface: userName = %@", [AmazonSharedPreferences userName]);
    
#if NEVER
    if ([AmazonSharedPreferences userName]) {
        // The user is signed in
        self.amazonLoginButton.hidden = YES;
        self.disconnectButton.hidden = NO;
        self.testS3Button.hidden = NO;
    }
    else {
        self.amazonLoginButton.hidden = NO;
        self.disconnectButton.hidden = YES;
        self.testS3Button.hidden = YES;
    }
    
    self.userEmailLabel.text = [AmazonSharedPreferences userEmail];
    self.userIDLabel.text = [AmazonSharedPreferences userName];
#endif
}


- (void)googleSignInComplete:(BOOL)success {
    HFLogDebug(@"EditPlayPagerController.googleSignInComplete: success=%d", success);
    
    _userNeedsAuthentication = !success;
    
    if (_userNeedsAuthentication) {
        HFLogDebug(@"EditPlayPagerController.googleSignInComplete - _userNeedsAuthentication is still TRUE, so that means login UI is needed");
        
        UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
        self.loginViewController = (LoginViewController *)[storyboard instantiateViewControllerWithIdentifier:@"LoginViewController"];
        [self presentViewController:self.loginViewController animated:YES completion:nil];
    }
    else {
        if (self.loginViewController) {
            [self.loginViewController dismissViewControllerAnimated:NO completion:nil];
            self.loginViewController = nil;
        }
    }
    
    [self refreshInterface];
}

- (void) googleDisconnectComplete:(BOOL)success {
    HFLogDebug(@"EditPlayPageController.googleDisconnectComplete: success=%d", success);
    
    [self refreshInterface];
}

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"EditPlayPageController.getStoriItemsComplete - found %d S3 objects", [arrayItems count]);
}

- (void)deleteVirtualDirectoryComplete {
    HFLogDebug(@"EditPlayPagerController.deleteVirtualDirectoryComplete");
    
    self.awsS3Provider = nil;
}

- (void)deleteStoriItemsAndReturnItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"EditPlayPagerController.deleteStoriItemsAndReturnItemsComplete");
}

- (void)uploadComplete:(BOOL)success {
    HFLogDebug(@"EditPlayPagerController.uploadComplete: success=%d", success);
}

- (EditPlayController *)viewControllerAtIndex:(NSUInteger)index {
    if (([self.ssj getSlideCount] == 0) || (index >= [self.ssj getSlideCount])) {
        HFLogDebug(@"EditPlayPageController.viewControllerAtIndex:%@ - index out of range", index);
        
        return nil;
    }
    
    //
    // Create a new view EditPlayController and initialize it.
    //
    EditPlayController *editPlayController = [self.storyboard instantiateViewControllerWithIdentifier:@"EditPlayController"];
    
    SlideJSON *sj = [self.ssj getSlideAtIndex:index];
    NSString *slideUuid = [self.ssj getSlideUuidByOrderIndex:index];
    [editPlayController initializeWithSlideJSON:sj withSlideShareName:self.slideShareName withUuid:slideUuid fromPageController:self];
    return editPlayController;
}

//
// UIPageViewControllerDataSource methods
//

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerBeforeViewController:(UIViewController *)viewController {
    int index = [self.ssj getOrderIndexForSlide:((EditPlayController *)viewController).slideUuid];
    
    if (index <= 0) {
        return nil;
    }
    
    index--;
    return [self viewControllerAtIndex:index];
}

- (UIViewController *)pageViewController:(UIPageViewController *)pageViewController viewControllerAfterViewController:(UIViewController *)viewController
{
    int index = [self.ssj getOrderIndexForSlide:((EditPlayController *)viewController).slideUuid];
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
