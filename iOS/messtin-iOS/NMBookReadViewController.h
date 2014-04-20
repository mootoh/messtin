//
//  NMBookReadViewController.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <UIKit/UIKit.h>

@class NMBook;

@interface NMBookReadViewController : UIViewController <UIScrollViewDelegate>
@property NMBook *book;
@property NSInteger currentPage;
@property NSArray *pages;

@property (weak, nonatomic) IBOutlet UIImageView *pageImageView;

- (void)downloadPage:(NSInteger)page show:(BOOL)toShow;

@end
