//
//  StoriListController.m
//  Stori
//
//  Created by Rob Bearman on 3/20/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "StoriListController.h"
#import "StoriListItem.h"
#import "AmazonSharedPreferences.h"

@interface StoriListController ()

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
    HFLogDebug(@"StoriListController.tableView:cellForRowAtIndexPath");
    
    static NSString *simpleTableIdentifier = @"Stori List";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:simpleTableIdentifier];
    
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:simpleTableIdentifier];
    }
    
    StoriListItem *sli = (StoriListItem *)[_storiListItems objectAtIndex:indexPath.row];
    
    cell.textLabel.text = sli.title;
    return cell;
}

//
// AWSS3ProviderDelegate methods
//

- (void)getStoriItemsComplete:(NSArray *)arrayItems {
    HFLogDebug(@"StoriListController.getStoriItemsComplete");
    
    _storiListItems = arrayItems;
    if (!self.tableView) {
        HFLogDebug(@"****** tableView is nil");
    }
    [self.tableView reloadData];
}

- (void)deleteVirtualDirectoryComplete {
    
}

- (void)uploadComplete:(BOOL)success {
    
}

@end
