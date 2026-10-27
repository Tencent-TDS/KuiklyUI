/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import "KRUINavigationBarKit.h"
#import "KuiklyRenderView.h"
#import <objc/runtime.h>

#if !TARGET_OS_OSX // [macOS]
static const void *kKRUINavigationBarCustomTitleKey = &kKRUINavigationBarCustomTitleKey;
static const void *kKRNavItemGroupKey = &kKRNavItemGroupKey;
static const void *kKRNavItemSortIndexKey = &kKRNavItemSortIndexKey;
static const void *kKRNavTrailingRegisteredIdsKey = &kKRNavTrailingRegisteredIdsKey;

const NSInteger KRUINavigationBarButtonItemSortIndexTrailingTopSpacer = NSIntegerMin;

BOOL KRUINavigationBarItemUsesCustomTitle(UINavigationItem *item) {
    if (!item) {
        return NO;
    }
    return [objc_getAssociatedObject(item, kKRUINavigationBarCustomTitleKey) boolValue];
}

void KRUINavigationBarItemSetUsesCustomTitle(UINavigationItem *item, BOOL usesCustomTitle) {
    if (!item) {
        return;
    }
    objc_setAssociatedObject(item, kKRUINavigationBarCustomTitleKey, @(usesCustomTitle), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

BOOL KRUINavigationBarUsesVerticalBar(UIViewController *viewController) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270100
    if (@available(iOS 27.1, *)) {
        if (!viewController) {
            return NO;
        }
        return viewController.traitCollection.verticalBarEdge != UIVerticalBarEdgeUnspecified;
    }
#endif
    (void)viewController;
    return NO;
}

CGFloat KRUINavigationBarVerticalTrailingTopInset(UIViewController *viewController) {
    if (!viewController || !KRUINavigationBarUsesVerticalBar(viewController)) {
        return 0;
    }
    CGFloat top = viewController.view.safeAreaInsets.top;
    UINavigationBar *bar = viewController.navigationController.navigationBar;
    if (bar && bar.safeAreaInsets.top > top) {
        top = bar.safeAreaInsets.top;
    }
    // 与 KRUINavigationBar `p_titleTrailingLimit` 一致：竖栏右侧状态列约 88pt。
    if (top < 56.0) {
        top = 56.0;
    }
    return top;
}

UIScrollView *KRUINavigationBarScrollViewForNativeRef(id hr_rootView, NSInteger nativeRef) {
    if (nativeRef <= 0 || !hr_rootView) {
        return nil;
    }
    if (![hr_rootView isKindOfClass:[KuiklyRenderView class]]) {
        return nil;
    }
    id handler = [(KuiklyRenderView *)hr_rootView viewWithRefTag:@(nativeRef)];
    if ([handler isKindOfClass:[UIScrollView class]]) {
        return (UIScrollView *)handler;
    }
    return nil;
}

NSInteger KRUINavigationBarButtonItemGroupId(UIBarButtonItem *item) {
    id value = objc_getAssociatedObject(item, kKRNavItemGroupKey);
    return [value isKindOfClass:[NSNumber class]] ? [(NSNumber *)value integerValue] : 0;
}

void KRUINavigationBarSetBarButtonItemGroupId(UIBarButtonItem *item, NSInteger group) {
    if (!item) {
        return;
    }
    objc_setAssociatedObject(item, kKRNavItemGroupKey, @(group), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

NSInteger KRUINavigationBarButtonItemSortIndex(UIBarButtonItem *item) {
    id value = objc_getAssociatedObject(item, kKRNavItemSortIndexKey);
    return [value isKindOfClass:[NSNumber class]] ? [(NSNumber *)value integerValue] : 0;
}

void KRUINavigationBarSetBarButtonItemSortIndex(UIBarButtonItem *item, NSInteger sortIndex) {
    if (!item) {
        return;
    }
    objc_setAssociatedObject(item, kKRNavItemSortIndexKey, @(sortIndex), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

BOOL KRUINavigationBarIsManagedTrailingBarItemIdentifier(NSString *identifier) {
    return identifier.length > 0 && [identifier hasPrefix:@"kr_nav_kit"];
}

static NSMutableSet<NSString *> *KRRegisteredTrailingBarItemIdentifiers(UIViewController *viewController) {
    NSMutableSet<NSString *> *set = objc_getAssociatedObject(viewController, kKRNavTrailingRegisteredIdsKey);
    if (!set) {
        set = [NSMutableSet set];
        objc_setAssociatedObject(viewController,
                                 kKRNavTrailingRegisteredIdsKey,
                                 set,
                                 OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return set;
}

void KRUINavigationBarRegisterTrailingBarItemIdentifier(UIViewController *viewController,
                                                       NSString *itemIdentifier) {
    if (!viewController || !KRUINavigationBarIsManagedTrailingBarItemIdentifier(itemIdentifier)) {
        return;
    }
    [KRRegisteredTrailingBarItemIdentifiers(viewController) addObject:itemIdentifier];
}

void KRUINavigationBarUnregisterTrailingBarItemIdentifier(UIViewController *viewController,
                                                             NSString *itemIdentifier) {
    if (!viewController || itemIdentifier.length == 0) {
        return;
    }
    [KRRegisteredTrailingBarItemIdentifiers(viewController) removeObject:itemIdentifier];
}

BOOL KRUINavigationBarShouldRetainManagedTrailingBarItem(UIViewController *viewController,
                                                            NSString *itemIdentifier) {
    if (!KRUINavigationBarIsManagedTrailingBarItemIdentifier(itemIdentifier)) {
        return YES;
    }
    if (!viewController) {
        return NO;
    }
    return [KRRegisteredTrailingBarItemIdentifiers(viewController) containsObject:itemIdentifier];
}

void KRUINavigationBarClearRegisteredTrailingBarItemIdentifiers(UIViewController *viewController) {
    if (!viewController) {
        return;
    }
    [KRRegisteredTrailingBarItemIdentifiers(viewController) removeAllObjects];
}

void KRUINavigationBarScheduleCoalescedMainFlush(id host, BOOL *scheduledToken, dispatch_block_t block) {
    if (!host || !scheduledToken || !block) {
        return;
    }
    if (*scheduledToken) {
        return;
    }
    *scheduledToken = YES;
    __weak id weakHost = host;
    dispatch_async(dispatch_get_main_queue(), ^{
        __strong id strongHost = weakHost;
        if (!strongHost) {
            return;
        }
        *scheduledToken = NO;
        block();
    });
}
#endif // [macOS]
