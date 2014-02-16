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
        self.dict = dict;
    }
    return self;
}

@end
