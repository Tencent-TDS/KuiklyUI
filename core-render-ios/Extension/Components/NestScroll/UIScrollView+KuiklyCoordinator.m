//
//  UIScrollView+KuiklyCoordinator.m
//  QQNews
//
//  Created by 李庭栋 on 2025/9/9.
//  Copyright © 2025 Tencent. All rights reserved.
//

#import "UIScrollView+KuiklyCoordinator.h"
#import <objc/runtime.h>

@implementation UIScrollView (KuiklyCoordinator)

- (BOOL)isParentLocked {
    NSNumber *isParentLocked = objc_getAssociatedObject(self, @selector(isParentLocked));
    return [isParentLocked boolValue];
}

- (void)setIsParentLocked:(BOOL)isParentLocked {
    objc_setAssociatedObject(self,
                             @selector(isParentLocked),
                             @(isParentLocked),
                             OBJC_ASSOCIATION_RETAIN);
}

- (NestedScrollCoordinator *)coordinator {
    return objc_getAssociatedObject(self, @selector(coordinator));;
}

- (void)setCoordinator:(NestedScrollCoordinator *)coordinator {
    objc_setAssociatedObject(self,
                             @selector(coordinator),
                             coordinator,
                             OBJC_ASSOCIATION_ASSIGN);
}

@end
