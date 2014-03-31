//
//  EditPlayController.m
//  Stori
//
//  Created by Rob Bearman on 3/31/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "EditPlayController.h"
#import "AmazonSharedPreferences.h"

@interface EditPlayController ()
- (void)refreshInterface;
@end

@implementation EditPlayController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [self refreshInterface];
}

- (void)didReceiveMemoryWarning
{
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

- (void)initializeWithSlideJSON:(SlideJSON *)sj withSlideShareName:(NSString *)slideShareName
                     withUuid:(NSString *)slideUuid fromPageController:(EditPlayPageController *)editPlayPageController {
    self.slideSharename = slideShareName;
    self.slideUuid = slideUuid;
    self.editPlayPageController = editPlayPageController;
    self.imageFileName = [sj getImageFilename];
    self.audioFileName = [sj getAudioFilename];
    self.slideText = [sj getText];
}

//
// Test stuff
//

- (void)refreshInterface {
    HFLogDebug(@"EditPlayController.refreshInterface");
    
    HFLogDebug(@"EditPlayController.refreshInterface: userName = %@", [AmazonSharedPreferences userName]);
    
    self.userEmailLabel.text = [AmazonSharedPreferences userEmail];
    self.userIDLabel.text = [AmazonSharedPreferences userName];
}

@end
