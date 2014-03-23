//
//  StoriListTableViewCell.h
//  Stori
//
//  Created by Rob Bearman on 3/23/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface StoriListTableViewCell : UITableViewCell

@property (nonatomic, weak) IBOutlet UILabel *titleLabel;
@property (nonatomic, weak) IBOutlet UILabel *modifiedDateLabel;
@property (nonatomic, weak) IBOutlet UILabel *slideCountLabel;

@end
