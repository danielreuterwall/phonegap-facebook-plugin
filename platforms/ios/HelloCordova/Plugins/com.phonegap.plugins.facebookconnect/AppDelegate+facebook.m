//
//  AppDelegate+facebook.m
//
//  Created by Daniel Reuterwall on 31/03/15.
//

#import "AppDelegate+facebook.h"
#import "FacebookConnectPlugin.h"
#import <objc/runtime.h>

@implementation AppDelegate (facebook)

- (id) getCommandInstance:(NSString*)className
{
  return [self.viewController getCommandInstance:className];
}

// Add call code block to application:openURL:sourceApplication:annotation: by method swizzling.
+ (void)load
{
    Method openUrlOriginal, openUrlSwizzled, finishLaunchingOriginal, finishLaunchingSwizzled;
    
    openUrlOriginal = class_getInstanceMethod(self, @selector(application:openURL:sourceApplication:annotation:));
    openUrlSwizzled = class_getInstanceMethod(self, @selector(swizzled_application:openURL:sourceApplication:annotation:));
    method_exchangeImplementations(openUrlOriginal, openUrlSwizzled);

    finishLaunchingOriginal = class_getInstanceMethod(self, @selector(application:didFinishLaunchingWithOptions:));
    finishLaunchingSwizzled = class_getInstanceMethod(self, @selector(swizzled_application:didFinishLaunchingWithOptions:));
    method_exchangeImplementations(finishLaunchingOriginal, finishLaunchingSwizzled);
}

- (BOOL)swizzled_application:(UIApplication*)application openURL:(NSURL*)url sourceApplication:(NSString*)sourceApplication annotation:(id)annotation
{
    [[FBSDKApplicationDelegate sharedInstance]
     application:application
     openURL:url
     sourceApplication:sourceApplication
     annotation:annotation];
    
    return [self swizzled_application:application openURL:url sourceApplication: sourceApplication annotation:annotation];
}

- (BOOL)swizzled_application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
  [[FBSDKApplicationDelegate sharedInstance] application:application
                                  didFinishLaunchingWithOptions:launchOptions];

  return [self swizzled_application:application didFinishLaunchingWithOptions:launchOptions];
}

@end
