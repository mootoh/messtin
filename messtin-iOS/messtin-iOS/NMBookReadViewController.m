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

    self.title = self.book.dict[@"name"];
    [self downloadPage:self.currentPage];

    UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
    [self.view addGestureRecognizer:tgr];
}

- (void)downloadPage:(NSUInteger)page
{
    NSFileManager *fileManager= [NSFileManager defaultManager];

    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:self.book.dict[@"id"]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%d.jpg", page]];

    if ([fileManager fileExistsAtPath:path]) {
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
        self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];
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
    [self downloadPage:++self.currentPage];
}

- (void)prevPage
{
    if (self.currentPage == 0) {
        return;
    }
    [self downloadPage:--self.currentPage];
}

@end
