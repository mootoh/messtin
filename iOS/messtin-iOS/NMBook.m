//
//  NMBook.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBook.h"
#import <Parse/Parse.h>

@implementation NMBook

- (id)initWithDictionary:(NSDictionary *)dict;
{
    self = [super init];
    if (! self)
        return nil;
    
    self.identifier = dict[@"id"];
    self.title = dict[@"title"];
    self.gd_id = dict[@"gd_id"];
    self.pages = [dict[@"pages"] integerValue];
    self.cover_img_gd_id = dict[@"cover_img_gd_id"];

    return self;
}

- (id)initWithParseObject:(PFObject *)object {
    self = [super init];
    if (! self)
        return nil;

    self.identifier = object[@"objectId"];
    self.title = object[@"title"];
    self.gd_id = object[@"gd_id"];
    self.pages = [object[@"pages"] integerValue];
    self.cover_img_gd_id = object[@"cover_img_gd_id"];
    self.parseObject = object;

    return self;
}

@end