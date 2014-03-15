//
//  NMBookThumbnailViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 3/1/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "AFNetworking/AFNetworking.h"
#import "NMBookThumbnailViewController.h"
#import "NMBookReadViewController.h"
#import "NMBook.h"

static NSString *kCellID = @"bookThumbnailCellId";

@implementation NMBookThumbnailViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
//    [self downloadThumbnails];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (NSInteger)collectionView:(UICollectionView *)view numberOfItemsInSection:(NSInteger)section;
{
    return self.book.pages;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)cv cellForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    UICollectionViewCell *cell = [cv dequeueReusableCellWithReuseIdentifier:kCellID forIndexPath:indexPath];
    
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:[self.book.identifier stringValue]];
    NSURL *documentsDirectoryPath = [NSURL fileURLWithPath:path];
    
    NSFileManager *fileManager= [NSFileManager defaultManager];
    if(![fileManager fileExistsAtPath:path])
        if(![fileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:NULL])
            NSLog(@"Error: Create folder failed %@", path);
    
    NSString *fileName = [NSString stringWithFormat:@"tm_%03d.jpg", indexPath.row+1];

    if ([fileManager fileExistsAtPath:[path stringByAppendingPathComponent:fileName]]) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[documentsDirectoryPath URLByAppendingPathComponent:fileName]]];
        return cell;
    }
    
    NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
    AFURLSessionManager *manager = [[AFURLSessionManager alloc] initWithSessionConfiguration:configuration];
    /*
    NSURL *url = [self.book.baseURL URLByAppendingPathComponent:[@"tm" stringByAppendingPathComponent:fileName]];
    NSURLRequest *req = [NSURLRequest requestWithURL:url];
    
    NSURLSessionDownloadTask *downloadTask = [manager downloadTaskWithRequest:req progress:nil destination:^NSURL *(NSURL *targetPath, NSURLResponse *response) {
        NSURL *ret = [documentsDirectoryPath URLByAppendingPathComponent:[response suggestedFilename]];
        return ret;
    } completionHandler:^(NSURLResponse *response, NSURL *filePath, NSError *error) {
        NSLog(@"error = %@", error);
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:filePath]];
        
    }];
    [downloadTask resume];
    */
    return cell;
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:@"backToBookReadView"]) {
        NSIndexPath *indexPath = [[self.collectionView indexPathsForSelectedItems] firstObject];
        NMBookReadViewController *vc = (NMBookReadViewController *)segue.destinationViewController;
        vc.currentPage = indexPath.row;
        [vc downloadPage:vc.currentPage show:YES];
//        [self.navigationController popViewControllerAnimated:YES];
    }
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    NMBookReadViewController *vc = (NMBookReadViewController *)[self.navigationController.viewControllers objectAtIndex:self.navigationController.viewControllers.count-2];
    vc.currentPage = indexPath.row;
    [vc downloadPage:vc.currentPage show:YES];
    [self.navigationController popViewControllerAnimated:YES];
}
@end
