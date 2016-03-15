//
//  FacebookConnectPlugin.m
//  GapFacebookConnect
//
//  Created by Jesse MacFadyen on 11-04-22.
//  Updated by Mathijs de Bruin on 11-08-25.
//  Updated by Christine Abernathy on 13-01-22
//  Updated by Daniel Reuterwall on 15-03-31
//  Copyright 2011 Nitobi, Mathijs de Bruin. All rights reserved.
//

#import "FacebookConnectPlugin.h"

@interface FacebookConnectPlugin ()

@property (strong, nonatomic) NSString* loginCallbackId;
@property (strong, nonatomic) NSString* dialogCallbackId;
@property (strong, nonatomic) NSString* graphCallbackId;

@end

@implementation FacebookConnectPlugin

- (void)pluginInitialize {
    NSLog(@"Init FacebookConnect Session");
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(applicationDidBecomeActive)
                                                 name:UIApplicationDidBecomeActiveNotification object:nil];
}

- (void)applicationDidBecomeActive {
    // Call the 'activateApp' method to log an app event for use in analytics and advertising reporting.
    [FBSDKAppEvents activateApp];
}

/*
 * Check if a permision is a read permission.
 */
- (BOOL)isPublishPermission:(NSString*)permission {
    return [permission hasPrefix:@"publish"] ||
    [permission hasPrefix:@"manage"] ||
    [permission isEqualToString:@"ads_management"] ||
    [permission isEqualToString:@"create_event"] ||
    [permission isEqualToString:@"rsvp_event"];
}

- (void)getLoginStatus:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                  messageAsDictionary:[self responseObject]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)getAccessToken:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    FBSDKAccessToken *token = [FBSDKAccessToken currentAccessToken];
    if(token) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:
                        token.tokenString];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:
                        @"Not connected."];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

/*
 - (void)logEvent:(CDVInvokedUrlCommand *)command {
 if ([command.arguments count] == 0) {
 // Not enough arguments
 CDVPluginResult *res = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid arguments"];
 [self.commandDelegate sendPluginResult:res callbackId:command.callbackId];
 return;
 }
 [self.commandDelegate runInBackground:^{
 // For more verbose output on logging uncomment the following:
 // [FBSettings setLoggingBehavior:[NSSet setWithObject:FBLoggingBehaviorAppEvents]];
 NSString *eventName = [command.arguments objectAtIndex:0];
 CDVPluginResult *res;
 NSDictionary *params;
 double value;
 
 if ([command.arguments count] == 1) {
 [FBAppEvents logEvent:eventName];
 } else {
 // argument count is not 0 or 1, must be 2 or more
 params = [command.arguments objectAtIndex:1];
 if ([command.arguments count] == 2) {
 // If count is 2 we will just send params
 [FBAppEvents logEvent:eventName parameters:params];
 }
 if ([command.arguments count] == 3) {
 // If count is 3 we will send params and a value to sum
 value = [[command.arguments objectAtIndex:2] doubleValue];
 [FBAppEvents logEvent:eventName valueToSum:value parameters:params];
 }
 }
 res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
 [self.commandDelegate sendPluginResult:res callbackId:command.callbackId];
 }];
 }
 */
/*
 - (void)logPurchase:(CDVInvokedUrlCommand *)command {
 //While calls to logEvent can be made to register purchase events,
 //there is a helper method that explicitly takes a currency indicator.
 
 CDVPluginResult *res;
 if (!command.arguments == 2) {
 res = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Invalid arguments"];
 [self.commandDelegate sendPluginResult:res callbackId:command.callbackId];
 return;
 }
 double value = [[command.arguments objectAtIndex:0] doubleValue];
 NSString *currency = [command.arguments objectAtIndex:1];
 [FBAppEvents logPurchase:value currency:currency];
 
 res = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
 [self.commandDelegate sendPluginResult:res callbackId:command.callbackId];
 }
 */

- (void)login:(CDVInvokedUrlCommand *)command {
    NSString *permissionsErrorMessage = @"";
    NSArray *permissions = nil;
    CDVPluginResult *pluginResult;
    if ([command.arguments count] > 0) {
        permissions = command.arguments;
    }
    if (permissions == nil) {
        // We need permissions
        permissionsErrorMessage = @"No permissions specified at login";
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:permissionsErrorMessage];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    // save the callbackId for the login callback
    self.loginCallbackId = command.callbackId;
    
    FBSDKAccessToken *token = [FBSDKAccessToken currentAccessToken];
    
    NSMutableArray *newPermissions = [[NSMutableArray alloc] initWithArray:@[]];
    // Filter out already granted permissions
    if(token) {
        for (NSString *permission in permissions){
            if (![token.permissions containsObject:permission]) {
                [newPermissions addObject:permission];
            }
        }
    }
    
    if([newPermissions count] == 0) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsDictionary:[self responseObject]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    
    BOOL publishPermissionFound = NO;
    BOOL readPermissionFound = NO;
    
    for (NSString *p in newPermissions) {
        if ([self isPublishPermission:p]) {
            publishPermissionFound = YES;
        } else {
            readPermissionFound = YES;
        }
        
        // If we've found one of each we can stop looking
        if (publishPermissionFound && readPermissionFound) {
            permissionsErrorMessage = @"Cannot request both read and publish permissions";
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:permissionsErrorMessage];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            return;
        }
    }
    
    void (^loginHandler)(FBSDKLoginManagerLoginResult *result, NSError *error)
    = ^(FBSDKLoginManagerLoginResult *result, NSError *error){
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:[error localizedDescription]];
        } else if (result.isCancelled) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                             messageAsString:@"User cancelled login"];
        } else {
            // If multiple permissions are requested, the user might not have approved all of them
            // the the response objects for approved and declined permissions
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                         messageAsDictionary:[self responseObject]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.loginCallbackId];
    };
    
    FBSDKLoginManager *login = [[FBSDKLoginManager alloc] init];
    if(readPermissionFound) {
        [login logInWithReadPermissions:newPermissions handler:loginHandler];
    }
    else {
        [login logInWithPublishPermissions:newPermissions handler:loginHandler];
    }
}

- (void) logout:(CDVInvokedUrlCommand*)command
{
    FBSDKLoginManager *manager = [[FBSDKLoginManager alloc] init];
    [manager logOut];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


- (void) showDialog:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult *pluginResult;
    // Save the callback ID
    self.dialogCallbackId = command.callbackId;
    
    NSMutableDictionary *options = [[command.arguments lastObject] mutableCopy];
    NSString* method = [[NSString alloc] initWithString:[options objectForKey:@"method"]];
    if ([options objectForKey:@"method"]) {
        [options removeObjectForKey:@"method"];
    }
    __block BOOL paramsOK = YES;
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    [options enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        if ([obj isKindOfClass:[NSString class]]) {
            params[key] = obj;
        } else {
            NSError *error;
            NSData *jsonData = [NSJSONSerialization
                                dataWithJSONObject:obj
                                options:0
                                error:&error];
            if (!jsonData) {
                paramsOK = NO;
                // Error
                *stop = YES;
            }
            params[key] = [[NSString alloc]
                           initWithData:jsonData
                           encoding:NSUTF8StringEncoding];
        }
    }];
    
    if (!paramsOK) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                         messageAsString:@"Error completing dialog."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
    } else {
        if([method isEqualToString:@"appInvite"]) {
            FBSDKAppInviteDialog *dialog = [FBSDKAppInviteDialog alloc];
            
            if([[params objectForKey:@"canShow"] isEqualToString:@"true"]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                 messageAsString:[NSString stringWithFormat:@"%@", [dialog canShow] ? @"true" : @"false"]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
                return;
            }
            
            
            if(![params objectForKey:@"link"]) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                 messageAsString:@"App invite dialog requires a link param."];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
            }
            FBSDKAppInviteContent *content =[[FBSDKAppInviteContent alloc] init];
            content.appLinkURL = [NSURL URLWithString:[params objectForKey:@"link"]];

            //optionally set previewImageURL
            if(![params objectForKey:@"picture"]) {
                content.appInvitePreviewImageURL = [NSURL URLWithString:[params objectForKey:@"picture"]];
            }
            
            dialog.content = content;
            dialog.delegate = self;
            
            // present the dialog. Assumes self implements protocol `FBSDKAppInviteDialogDelegate`
            [dialog show];
            return;
        }
/*    // Check method
    else if ([method isEqualToString:@"send"]) {
        
        // Send private message dialog
        // Create native params
        FBLinkShareParams *fbparams = [[FBLinkShareParams alloc] init];
        fbparams.link = [NSURL URLWithString:[params objectForKey:@"link"]];
        fbparams.name = [params objectForKey:@"name"];
        fbparams.caption = [params objectForKey:@"caption"];
        fbparams.picture = [NSURL URLWithString:[params objectForKey:@"picture"]];
        fbparams.linkDescription = [params objectForKey:@"description"];
        if([FBSession activeSession].state != FBSessionStateOpen){
            NSLog(@"No open active FB session, opening new session...");
            
            BOOL result = [FBSession openActiveSessionWithAllowLoginUI:NO];
            if(result)
            {
                NSLog(@"Successfully opened new FB session");
            }
            else {
                NSLog(@"Failed to open new FB session");
            }
        }
        else {
            NSLog(@"Got open active FB session");
        }
        // Do we have the messaging app installed?
        if ([FBDialogs canPresentMessageDialogWithParams:fbparams]) {
            // We cannot use the Web Dialog Builder API, must use FBDialog for messaging
            // Present message dialog
            if ([[options objectForKey:@"peek"] isEqualToString:@"true"]) {
                NSLog(@"Facebook Messanger available, invoking callback due to peek param");
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Messaging available."];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
            }
            else {
                [FBDialogs presentMessageDialogWithLink:[NSURL URLWithString:[params objectForKey:@"link"]]
                                                handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                                                    CDVPluginResult *pluginResult = nil;
                                                    if (error) {
                                                        // An error occurred, we need to handle the error
                                                        // See: https://developers.facebook.com/docs/ios/errors
                                                        NSLog(@"Error messaging link: %@", error.localizedDescription);
                                                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Error messaging link."];
                                                    } else {
                                                        // Success
                                                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:results];
                                                    }
                                                }];
            }
        } else {
            // Do not have the messaging application installed
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Messaging unavailable."];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
        }
        return;
    } else if ([method isEqualToString:@"share"] || [method isEqualToString:@"share_open_graph"]) {
        // Create native params
        FBLinkShareParams *fbparams = [[FBLinkShareParams alloc] init];
        fbparams.link = [NSURL URLWithString:[params objectForKey:@"href"]];
        fbparams.name = [params objectForKey:@"name"];
        fbparams.caption = [params objectForKey:@"caption"];
        fbparams.picture = [NSURL URLWithString:[params objectForKey:@"picture"]];
        fbparams.linkDescription = [params objectForKey:@"description"];
        
        // If the Facebook app is installed and we can present the share dialog
        if ([FBDialogs canPresentShareDialogWithParams:fbparams]) {
            // Present the share dialog
            [FBDialogs presentShareDialogWithLink:fbparams.link
                                          handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                                              CDVPluginResult *pluginResult = nil;
                                              if ([[results objectForKey:@"completionGesture"] isEqualToString:@"cancel"]) {
                                                  // User cancelled
                                                  pluginResult = [CDVPluginResult resultWithStatus:
                                                                  CDVCommandStatus_ERROR messageAsString:@"User cancelled."];
                                              } else {
                                                  if (error) {
                                                      // An error occurred, we need to handle the error
                                                      // See: https://developers.facebook.com/docs/ios/errors
                                                      pluginResult = [CDVPluginResult resultWithStatus:
                                                                      CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error: %@", error.description]];
                                                  } else {
                                                      // Success
                                                      pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:results];
                                                  }
                                              }
                                              [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
                                          }];
            return;
        } // Else we run through into the WebDialog
    }
*/
    // Show the web dialog
/*    [FBWebDialogs
     presentDialogModallyWithSession:FBSession.activeSession
     dialog:method parameters:params
     handler:^(FBWebDialogResult result, NSURL *resultURL, NSError *error) {
         CDVPluginResult* pluginResult = nil;
         if (error) {
             // Dialog failed with error
             pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                              messageAsString:@"Error completing dialog."];
         } else {
             if (result == FBWebDialogResultDialogNotCompleted) {
                 // User clicked the "x" icon to Cancel
                 pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                                  messageAsString:@"User cancelled."];
             } else {
                 // Send the URL parameters back, for a requests dialog, the "request" parameter
                 // will include the resluting request id. For a feed dialog, the "post_id"
                 // parameter will include the resulting post id.
                 NSDictionary *params = [self parseURLParams:[resultURL query]];
                 pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:params];
             }
         }
         [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
     }];
*/
}

// For optional ARC support
#if __has_feature(objc_arc)
#else
[method release];
[params release];
[options release];
#endif
}

/*
 - (void) grantPermissions:(CDVInvokedUrlCommand *)command
 {
 // Save the callback ID
 self.graphCallbackId = command.callbackId;
 
 NSArray *requestedPermissions = [command argumentAtIndex:0];
 // We will store here the missing permissions that we will have to request
 NSMutableArray *newPermissions = [[NSMutableArray alloc] initWithArray:@[]];
 
 // Check if all the permissions we need are present in the user's current permissions
 // If they are not present add them to the permissions to be requested
 for (NSString *permission in requestedPermissions){
 if (![[[FBSession activeSession] permissions] containsObject:permission]) {
 [newPermissions addObject:permission];
 }
 }
 
 BOOL publishPermissionFound = NO;
 BOOL readPermissionFound = NO;
 
 for (NSString *p in newPermissions) {
 if ([self isPublishPermission:p]) {
 publishPermissionFound = YES;
 } else {
 readPermissionFound = YES;
 }
 
 // If we've found one of each we can stop looking.
 if (publishPermissionFound && readPermissionFound) {
 break;
 }
 }
 
 // If we have new permissions to request
 if (publishPermissionFound && readPermissionFound) {
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:@"Your app can't ask for both read and write permissions."];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 else if (readPermissionFound) {
 // Ask for the missing permissions
 [FBSession.activeSession
 requestNewReadPermissions:newPermissions
 completionHandler:^(FBSession *session, NSError *error) {
 if (!error) {
 // Permission granted
 NSLog(@"new permissions %@", [FBSession.activeSession permissions]);
 // We can request the user information
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
 messageAsString:FBSession.activeSession.accessTokenData.accessToken];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 } else {
 // An error occurred, we need to handle the error
 // See: https://developers.facebook.com/docs/ios/errors
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:[error localizedDescription]];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 }];
 }
 else if(publishPermissionFound) {
 [FBSession.activeSession
 requestNewPublishPermissions:newPermissions
 defaultAudience:FBSessionDefaultAudienceFriends
 completionHandler:^(FBSession *session, NSError *error) {
 if (!error) {
 // Permission granted
 NSLog(@"new permissions %@", [FBSession.activeSession permissions]);
 // We can request the user information
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
 messageAsString:FBSession.activeSession.accessTokenData.accessToken];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 } else {
 // An error occurred, we need to handle the error
 // See: https://developers.facebook.com/docs/ios/errors
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:[error localizedDescription]];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 }];
 }
 else {
 // Permissions are present
 // We can request the user information
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
 messageAsString:FBSession.activeSession.accessTokenData.accessToken];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 }
 */
/*
 - (void) graphApi:(CDVInvokedUrlCommand *)command
 {
 // Save the callback ID
 self.graphCallbackId = command.callbackId;
 
 NSString *graphPath = [command argumentAtIndex:0];
 NSArray *permissionsNeeded = [command argumentAtIndex:1];
 
 // We will store here the missing permissions that we will have to request
 NSMutableArray *requestPermissions = [[NSMutableArray alloc] initWithArray:@[]];
 
 // Check if all the permissions we need are present in the user's current permissions
 // If they are not present add them to the permissions to be requested
 for (NSString *permission in permissionsNeeded){
 if (![[[FBSession activeSession] permissions] containsObject:permission]) {
 [requestPermissions addObject:permission];
 }
 }
 
 // If we have permissions to request
 if ([requestPermissions count] > 0){
 // Ask for the missing permissions
 if([self areAllPermissionsReadPermissions:requestPermissions]) {
 [FBSession.activeSession
 requestNewReadPermissions:requestPermissions
 completionHandler:^(FBSession *session, NSError *error) {
 if (!error) {
 // Permission granted
 NSLog(@"new permissions %@", [FBSession.activeSession permissions]);
 // We can request the user information
 [self makeGraphCall:graphPath];
 } else {
 // An error occurred, we need to handle the error
 // See: https://developers.facebook.com/docs/ios/errors
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:[error localizedDescription]];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 }];
 }
 else {
 [FBSession.activeSession
 requestNewPublishPermissions:requestPermissions
 defaultAudience:FBSessionDefaultAudienceFriends
 completionHandler:^(FBSession *session, NSError *error) {
 if (!error) {
 // Permission granted
 NSLog(@"new permissions %@", [FBSession.activeSession permissions]);
 // We can request the user information
 [self makeGraphCall:graphPath];
 } else {
 // An error occurred, we need to handle the error
 // See: https://developers.facebook.com/docs/ios/errors
 CDVPluginResult* pluginResult =
 [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:[error localizedDescription]];
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }
 }];
 }
 } else {
 // Permissions are present
 // We can request the user information
 [self makeGraphCall:graphPath];
 }
 }
 */

/*
 - (void) makeGraphCall:(NSString *)graphPath
 {
 NSLog(@"Graph Path = %@", graphPath);
 
 [FBRequestConnection
 startWithGraphPath: graphPath
 completionHandler:^(FBRequestConnection *connection, id result, NSError *error) {
 CDVPluginResult* pluginResult = nil;
 if (!error) {
 NSDictionary *response = (NSDictionary *) result;
 
 pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:response];
 } else {
 pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
 messageAsString:[error localizedDescription]];
 }
 [self.commandDelegate sendPluginResult:pluginResult callbackId:self.graphCallbackId];
 }];
 }
 */

- (NSDictionary *)responseObject {
    NSString *status = @"notConnected";
    NSDictionary *sessionDict = nil;
    
    FBSDKAccessToken *token = [FBSDKAccessToken currentAccessToken];
    
    if (token) {
        NSMutableArray *declinedPermissions = [[NSMutableArray alloc] initWithArray:@[]];
        // Filter out already granted permissions
        if(token) {
            for (NSString *permission in token.declinedPermissions){
                [declinedPermissions addObject:permission];
            }
        }
        
        NSMutableArray *grantedPermissions = [[NSMutableArray alloc] initWithArray:@[]];
        // Filter out already granted permissions
        for (NSString *permission in token.permissions){
            [grantedPermissions addObject:permission];
        }
        
        NSTimeInterval expiresTimeInterval = [token.expirationDate timeIntervalSinceNow];
        NSString *expiresIn = @"0";
        if (expiresTimeInterval > 0) {
            expiresIn = [NSString stringWithFormat:@"%0.0f", expiresTimeInterval];
        }
        
        status = @"connected";
        sessionDict = @{
                        @"accessToken" : token.tokenString,
                        @"declinedPermissions" : declinedPermissions,
                        @"permissions" : grantedPermissions,
                        @"expiresIn" : expiresIn,
                        @"userID" : token.userID
                        };
    }
    
    NSMutableDictionary *statusDict = [NSMutableDictionary dictionaryWithObject:status forKey:@"status"];
    if (nil != sessionDict) {
        [statusDict setObject:sessionDict forKey:@"authResponse"];
    }
    
    return statusDict;
}

/**
 * A method for parsing URL parameters.
 */
- (NSDictionary*)parseURLParams:(NSString *)query {
    NSString *regexStr = @"^(.+)\\[(.*)\\]$";
    NSRegularExpression *regex = [NSRegularExpression regularExpressionWithPattern:regexStr options:0 error:nil];
    
    NSArray *pairs = [query componentsSeparatedByString:@"&"];
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    [pairs enumerateObjectsUsingBlock:
     ^(NSString *pair, NSUInteger idx, BOOL *stop) {
         NSArray *kv = [pair componentsSeparatedByString:@"="];
         NSString *key = [kv[0] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
         NSString *val = [kv[1] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
         
         NSArray *matches = [regex matchesInString:key options:0 range:NSMakeRange(0, [key length])];
         if ([matches count] > 0) {
             for (NSTextCheckingResult *match in matches) {
                 
                 NSString *newKey = [key substringWithRange:[match rangeAtIndex:1]];
                 
                 if ([[params allKeys] containsObject:newKey]) {
                     NSMutableArray *obj = [params objectForKey:newKey];
                     [obj addObject:val];
                     [params setObject:obj forKey:newKey];
                 } else {
                     NSMutableArray *obj = [NSMutableArray arrayWithObject:val];
                     [params setObject:obj forKey:newKey];
                 }
             }
         } else {
             params[key] = val;
         }
         // params[kv[0]] = val;
     }];
    return params;
}

// Implementing FBSDKAppInviteDialogDelegate protocol
- (void)appInviteDialog:(FBSDKAppInviteDialog *)appInviteDialog didCompleteWithResults:(NSDictionary *)results {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                  messageAsDictionary:results];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
}

- (void)appInviteDialog:(FBSDKAppInviteDialog *)appInviteDialog didFailWithError:(NSError *)error {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                      messageAsString:[error localizedDescription]];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.dialogCallbackId];
}

@end