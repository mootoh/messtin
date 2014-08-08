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
    
    self.title = dict[@"title"];
    self.pages = [dict[@"pages"] integerValue];

    return self;
}

- (id)initWithParseObject:(PFObject *)object {
    self = [super init];
    if (! self)
        return nil;

    self.title = object[@"title"];
    self.pages = [object[@"pages"] integerValue];
    self.parseObject = object;

    return self;
}

@end