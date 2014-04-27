//
//  NMGoogleDrive.h
//  messtin-iOS
//
//  Created by Motohiro Takayama on 3/15/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import <Foundation/Foundation.h>

@class GTMOAuth2ViewControllerTouch;
@class GTLServiceDrive;
@class GTMOAuth2Authentication;

@interface NMGoogleDrive : NSObject

@property GTLServiceDrive *driveService;

- (void) query:(NSString *)q callback:(void(^)(NSData *data, NSError *error))callback;
- (void) fetch:(NSURL *)url callback:(void(^)(NSData *data, NSError *error))callback;
- (BOOL) isAuthorized;
- (GTMOAuth2ViewControllerTouch *)createAuthController:(SEL)finishedSelector;
- (void)viewController:(GTMOAuth2ViewControllerTouch *)viewController finishedWithAuth:(GTMOAuth2Authentication *)authResult error:(NSError *)error;

@end
