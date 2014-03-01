//
//  NMBookReadViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookReadViewController.h"
#import "NMBook.h"
#import "NMBookThumbnailViewController.h"
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

    self.title = self.book.title;

    self.currentPage = [[NSUserDefaults standardUserDefaults] integerForKey:[NSString stringWithFormat:@"%@/page", self.book.identifier]];

    [self downloadPage:self.currentPage show:YES];
    [self downloadPage:self.currentPage-1 show:NO];
    [self downloadPage:self.currentPage+1 show:NO];

    UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
    [self.pageImageView addGestureRecognizer:tgr];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationWillResign)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:NULL];
}

- (void)applicationWillResign
{
    [self saveCurrentPage];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [self saveCurrentPage];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
}

- (void)downloadPage:(NSInteger)page show:(BOOL)toShow
{
    if (page < 0 || page >= self.book.pages)
        return;

    NSFileManager *fileManager= [NSFileManager defaultManager];

    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:self.book.identifier];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%03d.jpg", page]];

    if ([fileManager fileExistsAtPath:path]) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
        }

        return;
    }
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
    
    NSURL *url = [NSURL URLWithString:[API_SERVER stringByAppendingFormat:@"/book/%@/%03d.jpg", self.book.identifier, page]];
    NSURLRequest *req = [NSURLRequest requestWithURL:url];
    
    NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:req progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        NSURL *ret = [NSURL fileURLWithPath:path];
        return ret;
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
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
    [[NSUserDefaults standardUserDefaults] setInteger:self.currentPage forKey:[NSString stringWithFormat:@"%@/page", self.book.identifier]];
}

#pragma mark UIScrollViewDelegate

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView
{
    return self.pageImageView;
}

- (IBAction)gotoPage:(id)sender {
    UIAlertView *av = [[UIAlertView alloc] initWithTitle:@"Goto" message:[NSString stringWithFormat:@"page (%d - %d)", 1, self.book.pages] delegate:self cancelButtonTitle:@"Jump" otherButtonTitles:nil];
    av.alertViewStyle = UIAlertViewStylePlainTextInput;
    UITextField *tf = [av textFieldAtIndex:0];
    tf.keyboardType = UIKeyboardTypeNumberPad;
    [av show];
}

- (void)alertView:(UIAlertView *)alertView didDismissWithButtonIndex:(NSInteger)buttonIndex {
    UITextField *tf = [alertView textFieldAtIndex:0];
    NSLog(@"yay %@", tf.text);
    // check page range
    NSInteger pg = [tf.text integerValue];
    if (pg < 0 || pg >= self.book.pages) {
        NSLog(@"out of range");
        return;
    }
    self.currentPage = pg;
    [self downloadPage:pg show:YES];
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:@"toThumbnailBookPages"]) {
        NMBookThumbnailViewController *vc = (NMBookThumbnailViewController *)segue.destinationViewController;
        vc.book = self.book;
    }
}


@end
