//
//  STONavigationController.m
//  Stori
//
//  Created by Rob Bearman on 4/15/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "STONavigationController.h"

@interface STONavigationController ()
@end

@implementation STONavigationController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    HFLogDebug(@"STONavigationController.viewDidLoad");
    
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    HFLogDebug(@"STONavigationController.didReceiveMemoryWarning");
    
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (BOOL)shouldAutorotate {
    HFLogDebug(@"STONavigationController.shouldAutorotate");
    
    // Implement shouldAutorotate and pass to our EditPlayController
    // as part of addressing issue #72.
    
    UIViewController *top = self.topViewController;
    if (top) {
        return [top shouldAutorotate];
    }
    else {
        return NO;
    }
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

@end
