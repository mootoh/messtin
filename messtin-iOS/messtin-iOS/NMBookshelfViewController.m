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

NSString *kCellID = @"bookCellId";

@interface NMBookshelfViewController ()

@end

@implementation NMBookshelfViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.books = [NSMutableArray array];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    [AFMGR GET:[API_SERVER stringByAppendingString:@"/books.json"] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
        self.books = [NSMutableArray arrayWithArray:responseObject];
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
    NSDictionary *book = [self.books objectAtIndex:indexPath.row];

    // TODO: Download the cover and store it to local storage
//    [AFMGR GET:[API_SERVER stringByAppendingFormat:@"/book/@/@", book[@"name"], book[@"cover_img"]] parameters:nil success:^(AFHTTPRequestOperation *operation, id responseObject) {
//        
//    } failure:^(AFHTTPRequestOperation *operation, NSError *error) {
//        NSLog(@"Error: %@", error);
//    }];

    UIImageView *iv = (UIImageView *)[cell viewWithTag:1];
    iv.image = [UIImage imageWithData:[NSData dataWithContentsOfURL:[NSURL URLWithString:[API_SERVER stringByAppendingFormat:@"/book/%@/%@", book[@"id"], book[@"cover_img"]]]]];
    cell.frame = CGRectMake(0, 0, iv.image.size.width, iv.image.size.height);
    
    return cell;
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([segue.identifier isEqualToString:@"shelfToRead"]) {
        NSIndexPath *indexPath = [[self.collectionView indexPathsForSelectedItems] firstObject];
        NMBookReadViewController *vc = (NMBookReadViewController *)segue.destinationViewController;
        NMBook *book = [[NMBook alloc] initWithDictionary:self.books[indexPath.row]];
        vc.book = book;
    }
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    NSDictionary *book = [self.books objectAtIndex:indexPath.row];
    
}

@end
