//
//  NMSettingViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 6/3/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMSettingViewController.h"

@interface NMSettingViewController ()

@end

@implementation NMSettingViewController

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
    // Do any additional setup after loading the view.

    NSInteger size = [[NSUserDefaults standardUserDefaults] integerForKey:k_MAXIMUM_CACHE_SIZE_KEY];
    self.maxCacheTextField.text = [NSString stringWithFormat:@"%d", size];
}

- (void) viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
    NSInteger size = [self.maxCacheTextField.text integerValue];
    [[NSUserDefaults standardUserDefaults] setInteger:size forKey:k_MAXIMUM_CACHE_SIZE_KEY];
    [[NSUserDefaults standardUserDefaults] synchronize];
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

#pragma mark - UITextFieldDelegate

@end
