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

static NSString *kCellID = @"bookCellId";
static NSString *kCoverImage = @"001.jpg";

@interface NMBookshelfViewController ()

@end

@implementation NMBookshelfViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.title = @"Bookshelf";
    self.books = [NSMutableArray array];

    [AFMGR GET:[API_SERVER stringByAppendingString:@"/books.json"] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
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

- (NSInteger)collectionView:(UICollectionView *)view numberOfItemsInSection:(NSInteger)section;
{
    return self.books.count;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)cv cellForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    UICollectionViewCell *cell = [cv dequeueReusableCellWithReuseIdentifier:kCellID forIndexPath:indexPath];

    NMBook *book = [self.books objectAtIndex:indexPath.row];
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:book.identifier];
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

    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];

    NSURL *coverURL = [book.baseURL URLByAppendingPathComponent:kCoverImage];
    NSURLRequest *req = [NSURLRequest requestWithURL:coverURL];

    NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:req progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        NSURL *ret = [documentsDirectoryPath URLByAppendingPathComponent:[response suggestedFilename]];
        return ret;
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];

    }];
    [downloadTask resume];

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

@end
