//
//  NMSettingViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 6/3/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMSettingViewController.h"
#import "NMCacheDaemon.h"
@interface NMSettingViewController ()
@property (weak, nonatomic) IBOutlet UISegmentedControl *cacheLimitSegmentedControl;

@end

@implementation NMSettingViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    [self.cacheLimitSegmentedControl addTarget:self action:@selector(cacheSizeChanged:) forControlEvents:UIControlEventValueChanged];
}

- (void) viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
//    NSInteger size = [self.maxCacheTextField.text integerValue];
//    [[NSUserDefaults standardUserDefaults] setInteger:size forKey:k_MAXIMUM_CACHE_SIZE_KEY];
//    [[NSUserDefaults standardUserDefaults] synchronize];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void) cacheSizeChanged:(id)sender
{
    UISegmentedControl *cacheSizeSegments = (UISegmentedControl *)sender;
    [[NSUserDefaults standardUserDefaults] setInteger:cacheSizeSegments.selectedSegmentIndex forKey:k_CACHE_SIZE_TYPE];
    [NMCacheDaemon updateLimit];
}
@end