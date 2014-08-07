//
//  NMBookThumbnailViewController.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 3/1/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBookThumbnailViewController.h"
#import "NMBookReadViewController.h"
#import "NMBook.h"
#import "NMAppDelegate.h"

static NSString *kCellID = @"bookThumbnailCellId";

@implementation NMBookThumbnailViewController

- (void)viewDidLoad
{
    [super viewDidLoad];
}

- (void) downloadThumbnails {
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;
/*
    GTLQueryDrive *query = [GTLQueryDrive queryForFilesList];
    query.maxResults = 1000;
    query.q = [NSString stringWithFormat:@"title = 'tm' and '%@' in parents", self.book.gd_id];
    [app.googleDrive.driveService executeQuery:query completionHandler:^(GTLServiceTicket *ticket,
                                                                         GTLDriveFileList *fileList,
                                                                         NSError *error) {
        if (error) {
            NSLog(@"failed in querying GDrive; %@", error);
            return;
        }
        
        GTLDriveFile *thumbnailDir = fileList[0];
        NSLog(@"thumbnailDir = %@", thumbnailDir.identifier);
        
        GTLQueryDrive *query2 = [GTLQueryDrive queryForFilesList];
        query2.maxResults = 1000;
        query2.q = [NSString stringWithFormat:@"title != 'tm' and '%@' in parents", thumbnailDir.identifier];
        [app.googleDrive.driveService executeQuery:query2 completionHandler:^(GTLServiceTicket *ticket2,
                                                                             GTLDriveFileList *fileList2,
                                                                             NSError *error2) {
            NSArray *sorted = [fileList2.items sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {
                GTLDriveFile *f1 = (GTLDriveFile *)obj1;
                GTLDriveFile *f2 = (GTLDriveFile *)obj2;
                return [f1.title compare:f2.title];
            }];
            self.thumbnailInfos = sorted;
            for (GTLDriveFile *file in sorted) {
                [self downloadThumbnail:file];
            }
        }];
    }];
 */
}
/*
- (void) downloadThumbnail:(GTLDriveFile *)file {
    NMAppDelegate *app = (NMAppDelegate *)[UIApplication sharedApplication].delegate;

    [app.googleDrive fetch:file.downloadUrl callback:^(NSData *data, NSError *error) {
        NSString *path = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
        path = [path stringByAppendingPathComponent:self.book.gd_id];
        path = [path stringByAppendingPathComponent:@"tm"];

        NSFileManager *fileManager= [NSFileManager defaultManager];
        if(![fileManager fileExistsAtPath:path])
            if(![fileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:NULL])
                NSLog(@"Error: Create folder failed %@", path);
     
        NSString *fileName = file.title;
        path = [path stringByAppendingPathComponent:fileName];

        if (! [fileManager createFileAtPath:path contents:data attributes:nil]) {
            NSLog(@"failed in creating thumbnail file: %@", path);
            return;
        }
        
        [self.collectionView reloadData]; // FIXME: only reload the downloaded item.
    }];
}
*/

- (NSInteger)collectionView:(UICollectionView *)view numberOfItemsInSection:(NSInteger)section; {
    return self.book.pages;
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)cv cellForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    UICollectionViewCell *cell = [cv dequeueReusableCellWithReuseIdentifier:kCellID forIndexPath:indexPath];
    
    NSString *path = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) firstObject];
    path = [path stringByAppendingPathComponent:self.book.gd_id];
    path = [path stringByAppendingPathComponent:@"tm"];
    NSURL *documentsDirectoryPath = [NSURL fileURLWithPath:path];
    
    NSFileManager *fileManager= [NSFileManager defaultManager];
    if(![fileManager fileExistsAtPath:path])
        if(![fileManager createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:NULL])
            NSLog(@"Error: Create folder failed %@", path);
    
    if (! self.thumbnailInfos)
        return cell;
    /*
    NSString *fileName = ((GTLDriveFile *)self.thumbnailInfos[indexPath.row]).title;

    if ([fileManager fileExistsAtPath:[path stringByAppendingPathComponent:fileName]]) {
        UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
        iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[documentsDirectoryPath URLByAppendingPathComponent:fileName]]];
        return cell;
    }
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
