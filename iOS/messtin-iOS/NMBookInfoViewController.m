//
//  NMBookInfoViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 6/12/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookInfoViewController.h"
#import "NMBook.h"
#import <Parse/Parse.h>

@interface NMBookInfoViewController ()

@end

@implementation NMBookInfoViewController

- (instancetype)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
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
    UILabel *titleLabel = (UILabel *)[self.view viewWithTag:1];
    titleLabel.text = self.book.title;
    UITextView *tv = (UITextView *)[self.view viewWithTag:2];
    tv.text = self.book.parseObject[@"description"];

    UISwitch *pinSwitch = (UISwitch *)[self.view viewWithTag:3];
    [pinSwitch addTarget:self action:@selector(togglePin:) forControlEvents:UIControlEventValueChanged];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) togglePin:(id)sender
{
    UISwitch *pinSwitch = (UISwitch *)sender;
    if (pinSwitch.on) {
        // mark this book as "pinned": prevent system from purging this book.
    } else {
        // unpin this book.
    }
}

@end
