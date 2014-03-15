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

#import <MobileCoreServices/MobileCoreServices.h>
#import "GTMOAuth2ViewControllerTouch.h"
#import "GTLDrive.h"

static NSString *const kKeychainItemName = @"Google Drive Quickstart";
static NSString *const kClientID = @"752439311564-l7gerkejmno69o06jq54nrhf3t30karu.apps.googleusercontent.com";
static NSString *const kClientSecret = @"2-Ha8VeMaW76sPRtv-gpBzyi";

static NSString *kCellID = @"bookCellId";
static NSString *kCoverImage = @"001.jpg";

@interface NMBookshelfViewController ()
@property GTLServiceDrive *driveService;
@end

@implementation NMBookshelfViewController

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.driveService = [[GTLServiceDrive alloc] init];
    self.driveService.authorizer = [GTMOAuth2ViewControllerTouch authForGoogleFromKeychainForName:kKeychainItemName
                                                                                         clientID:kClientID
                                                                                     clientSecret:kClientSecret];

    self.title = @"Bookshelf";
    self.books = [NSMutableArray array];
}

- (void) viewDidAppear:(BOOL)animated
{
    if (![self isAuthorized])
    {
        // Not yet authorized, request authorization and push the login UI onto the navigation stack.
        [self presentViewController:[self createAuthController] animated:YES completion:nil];
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

- (void) retrieveCoverImageFromProxyServer:(void(^)(NSError *, UIImage *image))callback
{
    NSAssert([self isAuthorized], @"should be authorized");

    NSURLRequest *req = [NSURLRequest requestWithURL:[NSURL URLWithString:@"http://localhost:3000/book/1"]];
    NSURLResponse *response;
    NSError *error;
    NSData *data = [NSURLConnection sendSynchronousRequest:req returningResponse:&response error:&error];
    NSAssert(error == nil, @"error in connection");

    NSDictionary *obj = [NSJSONSerialization JSONObjectWithData:data options:NSJSONReadingAllowFragments error:&error];
    NSAssert(error == nil, @"error in parsing JSON");

    GTMHTTPFetcher *fetcher = [self.driveService.fetcherService fetcherWithURLString:obj[@"cover_img_url"]];
    [fetcher beginFetchWithCompletionHandler:^(NSData *data, NSError *error) {
        callback(error, [UIImage imageWithData:data]);
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

    if ([fileManager fileExistsAtPath:[path stringByAppendingPathComponent:kCoverImage]]) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[documentsDirectoryPath URLByAppendingPathComponent:kCoverImage]]];
        return cell;
    }
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
    [self retrieveCoverImageFromProxyServer:^(NSError *error, UIImage *image) {
        NSAssert(error == nil, @"failed in fetching cover image");
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

// Helper to check if user is authorized
- (BOOL)isAuthorized
{
    return [((GTMOAuth2Authentication *)self.driveService.authorizer) canAuthorize];
}

// Creates the auth controller for authorizing access to Google Drive.
- (GTMOAuth2ViewControllerTouch *)createAuthController
{
    GTMOAuth2ViewControllerTouch *authController;
    authController = [[GTMOAuth2ViewControllerTouch alloc] initWithScope:kGTLAuthScopeDrive
                                                                clientID:kClientID
                                                            clientSecret:kClientSecret
                                                        keychainItemName:kKeychainItemName
                                                                delegate:self
                                                        finishedSelector:@selector(viewController:finishedWithAuth:error:)];
    return authController;
}

// Handle completion of the authorization process, and updates the Drive service
// with the new credentials.
- (void)viewController:(GTMOAuth2ViewControllerTouch *)viewController
      finishedWithAuth:(GTMOAuth2Authentication *)authResult
                 error:(NSError *)error
{
    if (error != nil)
    {
        [self showAlert:@"Authentication Error" message:error.localizedDescription];
        self.driveService.authorizer = nil;
    }
    else
    {
        self.driveService.authorizer = authResult;
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
