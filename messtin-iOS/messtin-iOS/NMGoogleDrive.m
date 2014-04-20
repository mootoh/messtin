//
//  NMGoogleDrive.m
//  messtin-iOS
//
//  Created by Motohiro Takayama on 3/15/14.
//  Copyright (c) 2014 Motohiro Takayama. All rights reserved.
//

#import "NMGoogleDrive.h"

#import <MobileCoreServices/MobileCoreServices.h>
#import "GTMOAuth2ViewControllerTouch.h"
#import "GTLDrive.h"

static NSString *const kKeychainItemName = @"Google Drive Quickstart";
static NSString *const kClientID = @"752439311564-l7gerkejmno69o06jq54nrhf3t30karu.apps.googleusercontent.com";
static NSString *const kClientSecret = @"2-Ha8VeMaW76sPRtv-gpBzyi";

@interface NMGoogleDrive()
@end

@implementation NMGoogleDrive

- (id) init {
    self = [super init];
    if (self) {
        self.driveService = [[GTLServiceDrive alloc] init];
        self.driveService.authorizer = [GTMOAuth2ViewControllerTouch authForGoogleFromKeychainForName:kKeychainItemName
                                                                                             clientID:kClientID
                                                                                         clientSecret:kClientSecret];
    }
    return self;
}


- (void) query:(NSString *)q callback:(void(^)(NSData *data, NSError *error))callback {
    GTLQueryDrive *query = [GTLQueryDrive queryForFilesList];
    query.maxResults = 1000;
    query.q = q;
    [self.driveService executeQuery:query completionHandler:^(GTLServiceTicket *ticket,
                                                              GTLDriveFileList *fileList,
                                                              NSError *error) {
        NSArray *sorted = [fileList.items sortedArrayUsingComparator:^NSComparisonResult(id obj1, id obj2) {
            GTLDriveFile *f1 = (GTLDriveFile *)obj1;
            GTLDriveFile *f2 = (GTLDriveFile *)obj2;
            return [f1.title compare:f2.title];
        }];
        for (GTLDriveFile *f in sorted) {
            NSLog(@"file %@ %@", f.title, f.downloadUrl);
        }
    }];
    
}

- (void) fetch:(NSString *)urlString callback:(void(^)(NSData *data, NSError *error))callback {
    GTMHTTPFetcher *fetcher = [self.driveService.fetcherService fetcherWithURLString:urlString];
    [fetcher beginFetchWithCompletionHandler:callback];
}

// Helper to check if user is authorized
- (BOOL)isAuthorized
{
    return [((GTMOAuth2Authentication *)self.driveService.authorizer) canAuthorize];
}

// Creates the auth controller for authorizing access to Google Drive.
- (GTMOAuth2ViewControllerTouch *)createAuthController:(SEL)finishedSelector
{
    GTMOAuth2ViewControllerTouch *authController;
    authController = [[GTMOAuth2ViewControllerTouch alloc] initWithScope:kGTLAuthScopeDrive
                                                                clientID:kClientID
                                                            clientSecret:kClientSecret
                                                        keychainItemName:kKeychainItemName
                                                                delegate:self
                                                        finishedSelector:@selector(viewController:finishedWithAuth:error:)];
    return authController;
}

// Handle completion of the authorization process, and updates the Drive service
// with the new credentials.
- (void)viewController:(GTMOAuth2ViewControllerTouch *)viewController
      finishedWithAuth:(GTMOAuth2Authentication *)authResult
                 error:(NSError *)error
{
    if (error != nil)
    {
//        [self showAlert:@"Authentication Error" message:error.localizedDescription];
        NSLog(@"error in auth %@", error);
        self.driveService.authorizer = nil;
    }
    else
    {
        self.driveService.authorizer = authResult;
    }
}

@end