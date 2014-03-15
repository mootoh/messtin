//
//  NMBook.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NMBook : NSObject

@property NSNumber *identifier;
@property NSString *title;
@property NSString *gd_id;
@property NSInteger pages;
@property NSURL *cover_img_url;
@property NSDate *lastOpened;

- (id)initWithDictionary:(NSDictionary *)dict;

@end
