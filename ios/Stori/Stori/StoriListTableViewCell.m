//
//  StoriListTableViewCell.m
//  Stori
//
//  Created by Rob Bearman on 3/23/14.
//  Copyright (c) 2014 Hyperfine Software. All rights reserved.
//

#import "StoriListTableViewCell.h"

@implementation StoriListTableViewCell

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        // Initialization code
    }
    return self;
}

- (void)awakeFromNib
{
    // Initialization code
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

@end
