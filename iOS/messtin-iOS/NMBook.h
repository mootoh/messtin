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
@property NSInteger pages;
@property NSDate *lastOpened;
@property (nonatomic, strong) PFObject *parseObject;

- (id)initWithDictionary:(NSDictionary *)dict;
- (id)initWithParseObject:(PFObject *)object;

@end
