//
//  NMAppDelegate.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 2/15/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <UIKit/UIKit.h>

@class NMGoogleDrive;

@interface NMAppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) NSString *storageServerURLBase;
@end
