//
//  NMBookshelfViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/15/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookshelfViewController.h"
#import "NMBookReadViewController.h"
#import "NMBookInfoViewController.h"
#import "NMBook.h"
#import "NMAppDelegate.h"
#import <Parse/Parse.h>

static NSString *kCellID = @"bookCellId";

@interface NMBookshelfViewController ()
@property NSIndexPath *prevIndexPath;
@property UIPopoverController *popoverForBook;
@end

@implementation NMBookshelfViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = @"Bookshelf";
    self.books = [NSMutableArray array];

    [self.collectionView addGestureRecognizer:[[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(longPressOnCell:)]];
    [self fetchBooks];
}


- (void) longPressOnCell:(UIGestureRecognizer *)gr {
    CGPoint p = [gr locationInView:self.collectionView];
    NSIndexPath *indexPath = [self.collectionView indexPathForItemAtPoint:p];
    if (self.prevIndexPath == indexPath)
        return;
    
    self.prevIndexPath = indexPath;

    CGRect inRect = CGRectMake(p.x, p.y, 4, 4);

    NMBookInfoViewController *bivc = [[NMBookInfoViewController alloc] initWithNibName:@"NMBookInfoViewController" bundle:nil];
    bivc.book = self.books[indexPath.row];

    UIPopoverController *pc = [[UIPopoverController alloc] initWithContentViewController:bivc];
    [pc presentPopoverFromRect:inRect inView:self.collectionView permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    self.popoverForBook = pc;
}

- (void) fetchBooks {
    PFQuery *query = [PFQuery queryWithClassName:@"Book"];
    [query findObjectsInBackgroundWithBlock:^(NSArray *objects, NSError *error) {
        if (error) {
            NSLog(@"failed in retrieving books from Parse");
            return;
        }
        
        for (PFObject *obj in objects) {
            NMBook *book = [[NMBook alloc] initWithParseObject:obj];
            [self.books addObject:book];
        }
        [self.collectionView reloadData];
    }];
}

- (void) retrieveCoverImage:(NMBook *)book callback:(void(^)(NSError *, UIImage *image))callback
{
    NMAppDelegate *app = [UIApplication sharedApplication].delegate;
    NSString *str = [NSString stringWithFormat:@"%@/%@/cover.jpg", app.storageServerURLBase, book.parseObject.objectId];
    NSURL *url = [NSURL URLWithString:str];
    [NSURLConnection sendAsynchronousRequest:[NSURLRequest requestWithURL:url] queue:[NSOperationQueue currentQueue] completionHandler:^(NSURLResponse *response, NSData *data, NSError *connectionError) {
        UIImage *image = [UIImage imageWithData:data];
        callback(connectionError, image);
    }];
}

- (NSInteger)collectionView:(UICollectionView *)view numberOfItemsInSection:(NSInteger)section;
{
    return self.books.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)cv cellForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    UICollectionViewCell *cell = [cv dequeueReusableCellWithReuseIdentifier:kCellID forIndexPath:indexPath];

    NMBook *book = [self.books objectAtIndex:indexPath.row];
    NSString *dir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    dir = [dir stringByAppendingPathComponent:book.parseObject.objectId];

    NSFileManager *fileManager= [NSFileManager defaultManager];
    if(![fileManager fileExistsAtPath:dir])
        if(![fileManager createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:NULL])
                                 NSLog(@"Error: Create folder failed %@", dir);

    NSString *coverImagePath = [dir stringByAppendingPathComponent:k_COVER_IMAGE_FILENAME];
    
    if ([fileManager fileExistsAtPath:coverImagePath]) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL fileURLWithPath:coverImagePath]]];
        return cell;
    }

    [self retrieveCoverImage:book callback:^(NSError *error, UIImage *image) {
        if (error) {
            NSLog(@"failed in fetching cover image: %@", error);
            return;
        }
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = image;
        
        
        if (! [fileManager createFileAtPath:coverImagePath contents:UIImageJPEGRepresentation(image, 1.0) attributes:nil]) {
            NSLog(@"failed in saving the cover image: %@", error);
            return;
        }
    }];
    return cell;
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:@"shelfToRead"]) {
        NSIndexPath *indexPath = [[self.collectionView indexPathsForSelectedItems] firstObject];
        NMBookReadViewController *vc = (NMBookReadViewController *)segue.destinationViewController;
        vc.book = self.books[indexPath.row];
    }
}

// Helper for showing an alert
- (void)showAlert:(NSString *)title message:(NSString *)message
{
    UIAlertView *alert;
    alert = [[UIAlertView alloc] initWithTitle: title
                                       message: message
                                      delegate: nil
                             cancelButtonTitle: @"OK"
                             otherButtonTitles: nil];
    [alert show];
}

// Helper for showing a wait indicator in a popup
- (UIAlertView*)showWaitIndicator:(NSString *)title
{
    UIAlertView *progressAlert;
    progressAlert = [[UIAlertView alloc] initWithTitle:title
                                               message:@"Please wait..."
                                              delegate:nil
                                     cancelButtonTitle:nil
                                     otherButtonTitles:nil];
    [progressAlert show];

    UIActivityIndicatorView *activityView;
    activityView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhite];
    activityView.center = CGPointMake(progressAlert.bounds.size.width / 2,
            progressAlert.bounds.size.height - 45);

    [progressAlert addSubview:activityView];
    [activityView startAnimating];
    return progressAlert;
}

@end