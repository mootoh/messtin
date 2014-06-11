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
    UITextView *tv = (UITextView *)[self.view viewWithTag:1];
    tv.text = self.book.parseObject[@"description"];
    // Do any additional setup after loading the view from its nib.
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

@end
