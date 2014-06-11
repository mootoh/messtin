//
//  NMBook.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <Foundation/Foundation.h>

@class PFObject;

@interface NMBook : NSObject

@property NSString *title;
@property NSString *gd_id;
@property NSInteger pages;
@property NSString *cover_img_gd_id;
@property NSDate *lastOpened;
@property (nonatomic, strong) PFObject *parseObject;

- (id)initWithDictionary:(NSDictionary *)dict;
- (id)initWithParseObject:(PFObject *)object;

@end
