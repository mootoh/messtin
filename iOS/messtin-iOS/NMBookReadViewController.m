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
#import "NMAppDelegate.h"
#import <Parse/Parse.h>

@interface NMBookReadViewController ()
@end

@implementation NMBookReadViewController


- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = self.book.title;

    self.currentPage = [[NSUserDefaults standardUserDefaults] integerForKey:[NSString stringWithFormat:@"%@/page", self.book.parseObject.objectId]];
    if (self.currentPage == 0) self.currentPage = 1;
    NSLog(@"current page = %d", self.currentPage);
    
    self.pageImageView.contentMode = UIViewContentModeScaleAspectFit;
    
    UIImage *cachedImage = [self cachedImage:self.currentPage];
    if (cachedImage) {
        self.pageImageView.image = cachedImage;
    }

    UITapGestureRecognizer *tgr = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(pageTapped:)];
    [self.pageImageView addGestureRecognizer:tgr];
        
    [self downloadPage:self.currentPage show:YES];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationWillResign)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:NULL];

    [[UIApplication sharedApplication] setStatusBarHidden:YES withAnimation:UIStatusBarAnimationFade];
}


- (void) fetchPageMetaInfos:(void(^)(NSError *))callback {
/*
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
 */
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
    NSString *path = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%@", self.book.parseObject.objectId]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%03d.jpg", page]];
    
    if ([fileManager fileExistsAtPath:path]) {
        return [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
    }
    return nil;
}

- (void)downloadPage:(NSInteger)page show:(BOOL)toShow
{
    if (page <= 0 || page >= self.book.pages)
        return;

    NSFileManager *fileManager= [NSFileManager defaultManager];

    NSString *cacheRoot = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    NSString *path = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%@", self.book.parseObject.objectId]];
    path = [path stringByAppendingPathComponent:[NSString stringWithFormat:@"%03d.jpg", page]];

    if ([fileManager fileExistsAtPath:path]) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:path]]];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
        }
        return;
    }

    self.activityIndicator.hidden = NO;
    [self.activityIndicator startAnimating];

    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    
    NSURL *url = [NSURL URLWithString:[NSString stringWithFormat:@"%@/%@/%03d.jpg", app.storageServerURLBase, self.book.parseObject.objectId, page]];
    [NSURLConnection sendAsynchronousRequest:[NSURLRequest requestWithURL:url] queue:[NSOperationQueue currentQueue] completionHandler:^(NSURLResponse *response, NSData *data, NSError *connectionError) {
        if (toShow) {
            self.pageImageView.image = [UIImage imageWithData:data];
            self.title = [self.book.title stringByAppendingFormat:@" %d/%d", page, self.book.pages];
        }
        self.activityIndicator.hidden = YES;
        [self.activityIndicator stopAnimating];
        
        NSString *bookDir = [cacheRoot stringByAppendingPathComponent:[NSString stringWithFormat:@"%@", self.book.parseObject.objectId]];
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
    CGFloat left = self.pageImageView.frame.size.width / 4;
    CGFloat right = self.pageImageView.frame.size.width * 3 / 4;
    CGFloat top = self.pageImageView.frame.size.height / 4;
    CGFloat bottom = self.pageImageView.frame.size.height * 3 / 4;
    if (touchLoc.x < left) {
        [self prevPage];
    } else if (touchLoc.x > right) {
        [self nextPage];
    } else if (touchLoc.y > top && touchLoc.y < bottom) {
        [self toggleUI];
    }
}

- (void) toggleUI {
    if (self.navigationController.navigationBarHidden) {
        [self.navigationController setNavigationBarHidden:NO animated:YES];
        [self.navigationController setToolbarHidden:NO animated:YES];
    } else {
        [self.navigationController setNavigationBarHidden:YES animated:YES];
        [self.navigationController setToolbarHidden:YES animated:YES];
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
    [[NSUserDefaults standardUserDefaults] setInteger:self.currentPage forKey:[NSString stringWithFormat:@"%@/page", self.book.parseObject.objectId]];
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

- (IBAction)saveBookmark:(id)sender {
    PFObject *bookmark = [PFObject objectWithClassName:@"Bookmark"];
    bookmark[@"page"] = [NSNumber numberWithInt:self.currentPage];
    bookmark[@"book"] = self.book.parseObject;
    [bookmark saveInBackground];
}

- (IBAction)showBookmarks:(id)sender {
    PFQuery *query = [PFQuery queryWithClassName:@"Bookmark"];
    [query whereKey:@"book" equalTo:self.book.parseObject];
    [query findObjectsInBackgroundWithBlock:^(NSArray *objects, NSError *error) {
        if  (error) {
            NSLog(@"failed in querying : %@", error);
            return;
        }
        for (PFObject *obj in objects) {
            NSLog(@"obj : %@", obj[@"page"]);
        }
    }];

}

@end
