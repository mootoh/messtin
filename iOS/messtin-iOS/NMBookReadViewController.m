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
#import "NMAppDelegate.h"
#import "NMGoogleDrive.h"
#import "GTLDrive.h"

@interface NMBookReadViewController ()
@end

@implementation NMBookReadViewController


- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = self.book.title;

    self.currentPage = [[NSUserDefaults standardUserDefaults] integerForKey:[NSString stringWithFormat:@"%@/page", self.book.identifier]];
    NSLog(@"current page = %d", self.currentPage);
    
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    NSAssert([app.googleDrive isAuthorized], @"should be authorized");

    UIImage *cachedImage = [self cachedImage:self.currentPage];
    if (cachedImage) {
        self.pageImageView.image = cachedImage;
    }

    [self fetchPageMetaInfos:^(NSError *error) {
        UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
        [self.pageImageView addGestureRecognizer:tgr];
        
        [self downloadPage:self.currentPage show:YES];
    }];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationWillResign)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:NULL];
}


- (void) fetchPageMetaInfos:(void(^)(NSError *))callback {
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    NSAssert([app.googleDrive isAuthorized], @"should be authorized");

    GTLQueryDrive *query = [GTLQueryDrive queryForFilesList];
    query.maxResults = 1000;
    query.q = [NSString stringWithFormat:@"title != 'tm' and title != '%@' and '%@' in parents", k_COVER_IMAGE_FILENAME, self.book.gd_id];
    [app.googleDrive.driveService executeQuery:query completionHandler:^(GTLServiceTicket *ticket,
                                                              GTLDriveFileList *fileList,
                                                              NSError *error) {
        if (error) {
            NSLog(@"failed in querying GDrive; %@", error);
            callback(error);
            return;
        }
        NSArray *sorted = [fileList.items sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {
            GTLDriveFile *f1 = (GTLDriveFile *)obj1;
            GTLDriveFile *f2 = (GTLDriveFile *)obj2;
            return [f1.title compare:f2.title];
        }];
        self.pages = sorted;
        
        callback(nil);
    }];
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

- (UIImage *)cachedImage:(NSInteger)page {
    if (page < 0 || page >= self.book.pages)
        return nil;

    NSFileManager *fileManager= [NSFileManager defaultManager];
    
    NSString *cacheRoot = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *path = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%d", [self.book.identifier intValue]]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%03d.jpg", page]];
    
    if ([fileManager fileExistsAtPath:path]) {
        return [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
    }
    return nil;
}

- (void)downloadPage:(NSInteger)page show:(BOOL)toShow
{
    if (page < 0 || page >= self.book.pages)
        return;

    NSFileManager *fileManager= [NSFileManager defaultManager];

    NSString *cacheRoot = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *path = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%d", [self.book.identifier intValue]]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%03d.jpg", page]];

    if ([fileManager fileExistsAtPath:path]) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
        }
        return;
    }

    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    NSAssert([app.googleDrive isAuthorized], @"should be authorized");
    GTLDriveFile *driveFile = self.pages[self.currentPage];
    [app.googleDrive fetch:driveFile.downloadUrl callback:^(NSData *data, NSError *error) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:data];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
        }

        NSString *bookDir = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%d", [self.book.identifier intValue]]];
        if (! [fileManager fileExistsAtPath:bookDir]) {
            NSError *error = nil;
            [fileManager createDirectoryAtPath:bookDir withIntermediateDirectories:YES attributes:nil error:&error];
            if (error) {
                NSLog(@"Failed in creating a book directory at %@", bookDir);
                return;
            }
        }

        if (! [fileManager createFileAtPath:path contents:data attributes:nil]) {
            NSLog(@"failed in saving the downloaded page image: %d", self.currentPage);
        }
    }];
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
        [vc downloadThumbnails];
    }
}


@end
