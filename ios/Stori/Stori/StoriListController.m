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
#import "STOUtilities.h"
#import "AmazonSharedPreferences.h"
#import "AWSS3Provider.h"
#import "HFProgressHUD.h"

#define ALERTVIEW_DIALOG_DELETE    1
#define ALERTVIEW_DIALOG_EDIT      2

@interface StoriListController ()

@property (strong, nonatomic) HFProgressHUD *progressHUD;
@property (strong, nonatomic) AWSS3Provider *awsS3Provider;
@property (strong, nonatomic) NSMutableData *receivedData;

- (void)handleStoriItemDelete:(StoriListItem *)sli;
- (void)alertViewForDelete:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex;
- (void)alertViewForEdit:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex;

@end

@implementation StoriListController

NSArray *_storiListItems;
StoriListItem *_selectedStoriListItem;
long long _expectedBytes;

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
    
    self.navigationItem.title = NSLocalizedString(@"storilist_navbutton_title", nil);
    
    self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:TRUE];
    
    // Clear any previous list. Note that this logic will be replaced
    // with an app-cached array to avoid unnecessary network traffic.
    _storiListItems = nil;
    self.tableView.tableHeaderView = nil;
    
    AWSS3Provider *awsS3Provider = [[AWSS3Provider alloc] init];
    [awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
    [awsS3Provider getStoriItemsAsync];
}

- (void)viewWillAppear:(BOOL)animated {
}

- (void)viewWillDisappear:(BOOL)animated {
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
    
    self.progressHUD = [[HFProgressHUD alloc] initWithView:self.navigationController.view];
    self.progressHUD.labelText = NSLocalizedString(@"storilistcontroller_delete_wait", nil);
    self.progressHUD.mode = MBProgressHUDModeIndeterminate;
    [self.progressHUD show:TRUE];
    
    self.awsS3Provider = [[AWSS3Provider alloc] init];
    [self.awsS3Provider initializeProvider:[AmazonSharedPreferences userName] withDelegate:self];
    [self.awsS3Provider deleteStoriItemsAndReturnItems:@[sli]];
}

- (void)updateWithNewStoriItemList:(NSArray *)arrayItems {
    HFLogDebug(@"StoriListController.updateWithNewStoriItemList");
    
    // Sort by date decending
    _storiListItems = [arrayItems sortedArrayUsingComparator:
                       ^NSComparisonResult(StoriListItem *item1, StoriListItem *item2) {
                           return [item2.modifiedDate compare:item1.modifiedDate];
                       }];
    
    [self.tableView reloadData];
    
    if (_storiListItems.count <= 0) {
        self.tableView.tableHeaderView = self.headerView;
    }
    else {
        self.tableView.tableHeaderView = nil;
    }
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

    // Don't allow download and play of current edit item
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
    
    switch (alertView.tag) {
        case ALERTVIEW_DIALOG_DELETE:
            [self alertViewForDelete:alertView didDismissWithButtonIndex:buttonIndex];
            break;
            
        case ALERTVIEW_DIALOG_EDIT:
            [self alertViewForEdit:alertView didDismissWithButtonIndex:buttonIndex];
            break;
            
        default:
            break;
    }
}

- (void)alertViewForDelete:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    
    if ([buttonTitle isEqualToString:NSLocalizedString(@"storilistcontroller_delete_button", nil)]) {
        [self handleStoriItemDelete:_selectedStoriListItem];
    }
}

- (void)alertViewForEdit:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    NSString *buttonTitle = [alertView buttonTitleAtIndex:buttonIndex];
    
    if (![buttonTitle isEqualToString:NSLocalizedString(@"menu_ok", nil)]) {
        return;
    }
    
    [self.editPlayController notifyForDownloadRequest:YES withUserUuid:nil withName:_selectedStoriListItem.slideShareName];
    [self.navigationController popViewControllerAnimated:YES];
}

//
// UIActionSheetDelegate methods
//

- (void)actionSheet:(UIActionSheet *)popup clickedButtonAtIndex:(NSInteger)index {
    HFLogDebug(@"StoriListController.actionSheet:clickedButtonAtIndex %d, menutitle=%@", index, [popup buttonTitleAtIndex:index]);
    
    NSString *userUuid = [AmazonSharedPreferences userName];
    
    NSString *buttonTitle = [popup buttonTitleAtIndex:index];
    if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_play", nil)]) {
        HFLogDebug(@"Play clicked for %@", _selectedStoriListItem.title);
        
        [self.editPlayController notifyForDownloadRequest:FALSE withUserUuid:userUuid withName:_selectedStoriListItem.slideShareName];
        [self.navigationController popViewControllerAnimated:YES];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_edit", nil)]) {
        HFLogDebug(@"Edit clicked for %@", _selectedStoriListItem.title);

        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:NSLocalizedString(@"storilistcontroller_edit_title", nil)
                              message:NSLocalizedString(@"storilistcontroller_edit_message", nil)
                              delegate:self cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                              otherButtonTitles:NSLocalizedString(@"menu_ok", nil), nil];
        alert.tag = ALERTVIEW_DIALOG_EDIT;
        [alert show];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_share", nil)]) {
        HFLogDebug(@"Share clicked for %@", _selectedStoriListItem.title);
        
        [STOUtilities shareShow:self withUserUuid:userUuid withSlideShareName:_selectedStoriListItem.slideShareName withTitle:_selectedStoriListItem.title];
    }
    else if ([buttonTitle isEqualToString:NSLocalizedString(@"menu_storilistitem_delete", nil)]) {
        HFLogDebug(@"Delete clicked for %@", _selectedStoriListItem.title);
        
        UIAlertView *alert = [[UIAlertView alloc]
                              initWithTitle:NSLocalizedString(@"storilistcontroller_delete_title", nil)
                              message:NSLocalizedString(@"storilistcontroller_delete_message", nil)
                              delegate:self cancelButtonTitle:NSLocalizedString(@"menu_cancel", nil)
                              otherButtonTitles:NSLocalizedString(@"storilistcontroller_delete_button", nil), nil];
        alert.tag = ALERTVIEW_DIALOG_DELETE;
        [alert show];
    }
}


//
// AWSS3ProviderDelegate methods
//

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"StoriListController.getStoriItemsComplete");
    
    [self updateWithNewStoriItemList:arrayItems];
    
    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    self.awsS3Provider = nil;
}

- (void)deleteVirtualDirectoryComplete {
    HFLogDebug(@"StoriListController.deleteVirtualDirectoryComplete");
}

- (void)deleteStoriItemsAndReturnItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"StoriListController.deleteStoriItemsAndReturnItemsComplete");
    
    [self updateWithNewStoriItemList:arrayItems];

    [self.progressHUD hide:TRUE];
    self.progressHUD = nil;
    self.awsS3Provider = nil;
}

- (void)uploadComplete:(BOOL)success {
}

@end
