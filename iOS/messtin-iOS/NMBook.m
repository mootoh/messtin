//
//  NMBook.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMBook.h"

@implementation NMBook

- (id)initWithDictionary:(NSDictionary *)dict;
{
    self = [super init];
    if (self) {
        self.identifier = dict[@"id"];
        self.title = dict[@"title"];
        self.gd_id = [NSURL URLWithString:dict[@"gd_id"]];
        self.pages = [dict[@"pages"] integerValue];
        self.cover_img_url = [NSURL URLWithString:dict[@"cover_img_url"]];
    }
    return self;
}

@end
