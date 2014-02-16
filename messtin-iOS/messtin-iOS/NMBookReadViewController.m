//
//  NMBookReadViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookReadViewController.h"
#import "NMBook.h"
#import "AFNetworking/AFNetworking.h"

@interface NMBookReadViewController ()

@end

@implementation NMBookReadViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.currentPage = 0;
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
/*
    [AFMGR GET:[API_SERVER stringByAppendingFormat:@"/book/%@/%d.jpg", self.book.dict[@"id"], 0] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {

    } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
        NSLog(@"Error: %@", error);
    }];
*/

    self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL URLWithString:[API_SERVER stringByAppendingFormat:@"/book/%@/%d.jpg", self.book.dict[@"id"], self.currentPage]]]];

    UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
    [self.view addGestureRecognizer:tgr];
}

- (void)pageTapped:(UIGestureRecognizer *)recognizer
{
    CGPoint touchLoc = [recognizer locationInView:self.pageImageView];
    if (touchLoc.x < self.pageImageView.frame.size.width/2) {
        [self prevPage];
    } else {
        [self nextPage];
    }
}

- (void)loadPage:(NSUInteger)page
{
    self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL URLWithString:[API_SERVER stringByAppendingFormat:@"/book/%@/%d.jpg", self.book.dict[@"id"], page]]]];
}

- (void)nextPage
{
    [self loadPage:++self.currentPage];
}

- (void)prevPage
{
    if (self.currentPage == 0) {
        return;
    }
    [self loadPage:--self.currentPage];
}

@end
