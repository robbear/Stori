//
//  StoriListController.m
//  Stori
//
//  Created by Rob Bearman on 3/20/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "StoriListController.h"
#import "StoriListTableViewCell.h"
#import "StoriListItem.h"
#import "AmazonSharedPreferences.h"
#import "MBProgressHUD.h"

@interface StoriListController ()

@property (strong, nonatomic) MBProgressHUD *progressHUD;

@end

@implementation StoriListController

NSArray *_storiListItems;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil {
    HFLogDebug(@"StoriListController.initWithNibName: %@", nibNameOrNil);
    
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    HFLogDebug(@"StoriListController.viewDidLoad");
    
    [super viewDidLoad];
    
    self.progressHUD = [[MBProgressHUD alloc] initWithView:self.navigationController.view];
    [self.navigationController.view addSubview:self.progressHUD];
    [self.progressHUD show:TRUE];
    
    AWSS3Provider *awsS3Provider = [[AWSS3Provider alloc] init];
    [awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
    [awsS3Provider getStoriItemsAsync];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)viewDidAppear:(BOOL)animated {
    HFLogDebug(@"StoriListController.viewDidAppear");
    
    [super viewDidAppear:animated];
    if (!self.tableView) {
        HFLogDebug(@"StoriListController.viewDidAppear - tableView is nil");
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


//
// UITableViewDelegate methods
//

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    HFLogDebug(@"StoriListController.tableView:numberOfRowsInSection");
    
    return [_storiListItems count];
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 1;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    //HFLogDebug(@"StoriListController.tableView:cellForRowAtIndexPath");

    static NSString *cellIdentifier = @"storiListTableViewCell";
    
    StoriListTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentifier];
    
    if (cell == nil) {
        cell = [[StoriListTableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:cellIdentifier];
    }

    StoriListItem *sli = (StoriListItem *)[_storiListItems objectAtIndex:indexPath.row];
    
    //
    // Convert S3 modified date UTC-based NSDate
    //
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"];
    [dateFormatter setTimeZone:[NSTimeZone timeZoneWithName:@"UTC"]];
    NSDate *utcDate = [dateFormatter dateFromString:sli.modifiedDate];
    
    //
    // Convert UTC-based NSDate to local formatted date
    //
    [dateFormatter setDateFormat:@"MMM dd, yyyy h:mm:ss a"];
    [dateFormatter setTimeZone:[NSTimeZone systemTimeZone]];
    cell.modifiedDateLabel.text = [dateFormatter stringFromDate:utcDate];
    
    cell.titleLabel.text = sli.title;
    int count = sli.countSlides;
    cell.slideCountLabel.text = count == 1 ? [NSString stringWithFormat:@"%d slide", count] : [NSString stringWithFormat:@"%d slides", count];
    
    return cell;
}

//
// AWSS3ProviderDelegate methods
//

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"StoriListController.getStoriItemsComplete");
    
    // Sort by date decending
    _storiListItems = [arrayItems sortedArrayUsingComparator:
                            ^NSComparisonResult(StoriListItem *item1, StoriListItem *item2) {
                                return [item2.modifiedDate compare:item1.modifiedDate];
                            }];
    
    if (!self.tableView) {
        HFLogDebug(@"****** tableView is nil");
    }
    [self.tableView reloadData];
    
    if (_storiListItems.count <= 0) {
        self.tableView.tableHeaderView = self.headerView;
    }
    else {
        self.tableView.tableHeaderView = nil;
    }
    
    [self.progressHUD hide:TRUE];
}

- (void)deleteVirtualDirectoryComplete {
    
}

- (void)uploadComplete:(BOOL)success {
    
}

@end
