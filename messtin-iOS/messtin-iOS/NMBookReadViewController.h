//
//  NMBookReadViewController.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <UIKit/UIKit.h>

@class NMBook;

@interface NMBookReadViewController : UIViewController
@property NMBook *book;
@property NSUInteger currentPage;

@property (weak, nonatomic) IBOutlet UIImageView *pageImageView;

@end
