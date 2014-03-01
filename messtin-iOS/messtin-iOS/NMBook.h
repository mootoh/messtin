//
//  NMBook.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/16/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NMBook : NSObject

@property NSString *identifier;
@property NSString *title;
@property NSURL *baseURL;
@property NSInteger pages;

- (id)initWithDictionary:(NSDictionary *)dict;

@end
