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
#import "STOPreferences.h"
#import "AmazonSharedPreferences.h"
#import "MBProgressHUD.h"

@interface StoriListController ()

@property (strong, nonatomic) MBProgressHUD *progressHUD;

- (void)handleStoriItemDelete:(StoriListItem *)sli;

@end

@implementation StoriListController

NSArray *_storiListItems;
StoriListItem *_selectedStoriListItem;

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
    
    // Clear any previous list. Note that this logic will be replaced
    // with an app-cached array to avoid unnecessary network traffic.
    _storiListItems = nil;
    self.tableView.tableHeaderView = nil;
    
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

- (void)viewDidDisappear:(BOOL)animated {
    HFLogDebug(@"StoriListController.viewDidDisappear");
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

- (void)handleStoriItemDelete:(StoriListItem *)sli {
    HFLogDebug(@"StoriListController.handleStoriItemDelete");
    
    UIAlertView *alert = [[UIAlertView alloc]
                          initWithTitle:NSLocalizedString(@"storilistcontroller_delete_title", nil)
                          message:NSLocalizedString(@"storilistcontroller_delete_message", nil)
                          delegate:self cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                          otherButtonTitles:NSLocalizedString(@"storilistcontroller_delete_button", nil), nil];
    alert.tag = 1; // BUGBUG - need file-static enum
    [alert show];
}


//
// UITableViewDelegate methods
//

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    HFLogDebug(@"StoriListController.tableView:didSelectRowAtIndexPath: %d", indexPath.item);
    
    _selectedStoriListItem = _storiListItems[indexPath.item];

    UIActionSheet *popup = [[UIActionSheet alloc]
                            initWithTitle:_selectedStoriListItem.title
                            delegate:self
                            cancelButtonTitle:nil
                            destructiveButtonTitle:nil
                            otherButtonTitles:nil];
    
    NSString *currentEditSlideShareName = [STOPreferences getEditPlayName];

    // Don't allow download and playof current edit item
    if (![currentEditSlideShareName isEqualToString:_selectedStoriListItem.slideShareName]) {
        [popup addButtonWithTitle:NSLocalizedString(@"menu_storilistitem_play", nil)];
    }
    [popup addButtonWithTitle:NSLocalizedString(@"menu_storilistitem_edit", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_storilistitem_share", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_storilistitem_delete", nil)];
    [popup addButtonWithTitle:NSLocalizedString(@"menu_cancel", nil)];
    
    popup.cancelButtonIndex = popup.numberOfButtons - 1;
    popup.destructiveButtonIndex = popup.numberOfButtons - 2;
    
    popup.tag = 1;
    [popup showInView:[UIApplication sharedApplication].keyWindow];
}

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
// UIAlertViewDelegate methods
//

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    HFLogDebug(@"StoriListController.alertView:%d didDismissWithButtonIndex:%d", alertView.tag, buttonIndex);
}

//
// UIActionSheetDelegate methods
//

- (void)actionSheet:(UIActionSheet *)popup clickedButtonAtIndex:(NSInteger)index {
    HFLogDebug(@"StoriListController.actionSheet:clickedButtonAtIndex %d, menutitle=%@", index, [popup buttonTitleAtIndex:index]);
    
    NSString *buttonTitle = [popup buttonTitleAtIndex:index];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_play", nil)]) {
        HFLogDebug(@"Play clicked for %@", _selectedStoriListItem.title);
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_edit", nil)]) {
        HFLogDebug(@"Edit clicked for %@", _selectedStoriListItem.title);
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_share", nil)]) {
        HFLogDebug(@"Share clicked for %@", _selectedStoriListItem.title);
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_delete", nil)]) {
        HFLogDebug(@"Delete clicked for %@", _selectedStoriListItem.title);
        
        [self handleStoriItemDelete:_selectedStoriListItem];
    }
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
