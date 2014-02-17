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
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = self.book.dict[@"name"];

    self.currentPage = [[NSUserDefaults standardUserDefaults] integerForKey:[NSString stringWithFormat:@"%@/page", self.book.dict[@"id"]]];

    [self downloadPage:self.currentPage show:YES];
    [self downloadPage:self.currentPage-1 show:NO];
    [self downloadPage:self.currentPage+1 show:NO];

    UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
    [self.view addGestureRecognizer:tgr];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [self saveCurrentPage];
}

- (void)downloadPage:(NSInteger)page show:(BOOL)toShow
{
    if (page < 0 || page >= [self.book.dict[@"pages"] integerValue])
        return;

    NSFileManager *fileManager= [NSFileManager defaultManager];

    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:self.book.dict[@"id"]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%d.jpg", page]];

    if ([fileManager fileExistsAtPath:path]) {
        if (toShow)
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
        return;
    }
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
    
    NSURL *url = [NSURL URLWithString:[API_SERVER stringByAppendingFormat:@"/book/%@/%d.jpg", self.book.dict[@"id"], page]];
    NSURLRequest *req = [NSURLRequest requestWithURL:url];
    
    NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:req progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        NSURL *ret = [NSURL fileURLWithPath:path];
        return ret;
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];
        }
    }];
    [downloadTask resume];
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

- (void)nextPage
{
    [self downloadPage:++self.currentPage show:YES];
    [self downloadPage:self.currentPage+1 show:NO];
}

- (void)prevPage
{
    [self downloadPage:--self.currentPage show:YES];
    [self downloadPage:self.currentPage-1 show:NO];
}

- (void)saveCurrentPage
{
    NSUserDefaults *ud = [NSUserDefaults standardUserDefaults];
    [[NSUserDefaults standardUserDefaults] setInteger:self.currentPage forKey:[NSString stringWithFormat:@"%@/page", self.book.dict[@"id"]]];
}

#pragma mark UIScrollViewDelegate

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView
{
    return self.pageImageView;
}

@end
