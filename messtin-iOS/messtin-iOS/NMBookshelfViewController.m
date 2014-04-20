//
//  NMBookshelfViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/15/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookshelfViewController.h"
#import "AFNetworking/AFNetworking.h"
#import "NMBookReadViewController.h"
#import "NMBook.h"
#import "NMAppDelegate.h"
#import "NMGoogleDrive.h"

static NSString *kCellID = @"bookCellId";

@interface NMBookshelfViewController ()
@end

@implementation NMBookshelfViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.title = @"Bookshelf";
    self.books = [NSMutableArray array];
}

- (void) viewDidAppear:(BOOL)animated
{
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    if (![app.googleDrive isAuthorized])
    {
        // Not yet authorized, request authorization and push the login UI onto the navigation stack.
        [self presentViewController:[app.googleDrive createAuthController:@selector(viewController:finishedWithAuth:error:)] animated:YES completion:nil];
    } else {
        //    [AFMGR GET:[API_SERVER stringByAppendingString:@"/books.json"] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
        [AFMGR GET:[API_SERVER stringByAppendingString:@"/books"] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
            NSArray *books = (NSArray *)responseObject;
            for (NSDictionary *info in books) {
                NMBook *book = [[NMBook alloc] initWithDictionary:info];
                [self.books addObject:book];
            }
            [self.collectionView reloadData];
        } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
            NSLog(@"Error: %@", error);
        }];
    }
}

- (void) retrieveCoverImageFromProxyServer:(NMBook *)book callback:(void(^)(NSError *, UIImage *image))callback
{
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
    NSAssert([app.googleDrive isAuthorized], @"should be authorized");

    [AFMGR GET:[API_SERVER stringByAppendingFormat:@"/book/%d", [book.identifier intValue]] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
//        NSString *coverImageUrlString = @"https://doc-04-6s-docs.googleusercontent.com/docs/securesc/279dtk3gcgbpq9io0fr435qttor7uq2a/bo810mf8v6o9r8j2j31q6e9rde3rsam0/1397959200000/12153878646635434502/12153878646635434502/0B0v3qwjLutgMSnhseUh1ak9SVms?h=16653014193614665626&e=download&gd=true"; // or
        NSString *coverImageUrl = responseObject[@"cover_img_url"];
        NSLog(@"cover image url = %@", coverImageUrl);
        
        NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
        [app.googleDrive fetch:coverImageUrl callback:^(NSData *data, NSError *error) {
            callback(error, [UIImage imageWithData:data]);
        }];
    } failure: ^(AFHTTPRequestOperation *operation, NSError *error) {
        NSLog(@"Error in accessing proxy: %@", error);
        callback(error, nil);
    }];

    
//    [self retrievePages:obj[@"gd_id"]];
}

- (NSInteger)collectionView:(UICollectionView *)view numberOfItemsInSection:(NSInteger)section;
{
    return self.books.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)cv cellForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    UICollectionViewCell *cell = [cv dequeueReusableCellWithReuseIdentifier:kCellID forIndexPath:indexPath];

    NMBook *book = [self.books objectAtIndex:indexPath.row];
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:[book.identifier stringValue]];
    NSURL *documentsDirectoryPath = [NSURL fileURLWithPath:path];

    NSFileManager *fileManager= [NSFileManager defaultManager];
    if(![fileManager fileExistsAtPath:path])
        if(![fileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:NULL])
                                 NSLog(@"Error: Create folder failed %@", path);
/*
    if ([fileManager fileExistsAtPath:[path stringByAppendingPathComponent:kCoverImage]]) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[documentsDirectoryPath URLByAppendingPathComponent:kCoverImage]]];
        return cell;
    }
 */ 
    /*
       NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
       AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];

       NSURLRequest *req = [NSURLRequest requestWithURL:book.cover_img_url];

       NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:req progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
                NSURL *ret = [documentsDirectoryPath URLByAppendingPathComponent:[response suggestedFilename]];
                return ret;
                } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];

        }];
        [downloadTask resume];
        */
    [self retrieveCoverImageFromProxyServer:book callback:^(NSError *error, UIImage *image) {
        if (error) {
            NSLog(@"failed in fetching cover image: %@", error);
            return;
        }
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = image;
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
