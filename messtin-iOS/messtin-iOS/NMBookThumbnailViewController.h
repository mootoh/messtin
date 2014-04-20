//
//  NMBookThumbnailViewController.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 3/1/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <UIKit/UIKit.h>
@class NMBook;

@interface NMBookThumbnailViewController : UICollectionViewController
@property NMBook *book;
@property NSArray *thumbnailInfos;

- (void) downloadThumbnails;

@end
