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

#import "KRUIBarButtonItemKit.h"

#if !TARGET_OS_OSX // [macOS]

static void KRSetBarButtonItemBool(UIBarButtonItem *item, NSString *setterName, BOOL value) {
    SEL selector = NSSelectorFromString(setterName);
    if (![item respondsToSelector:selector]) {
        return;
    }
    void (*imp)(id, SEL, BOOL) = (void (*)(id, SEL, BOOL))[(id)item methodForSelector:selector];
    imp(item, selector, value);
}

void KRUIBarButtonItemApplyGlassBackground(UIBarButtonItem *item, BOOL sharesBackground) {
    if (!item) {
        return;
    }
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if ([item respondsToSelector:@selector(setSharesBackground:)]
            && [item respondsToSelector:@selector(setHidesSharedBackground:)]) {
            KRSetBarButtonItemBool(item, @"setSharesBackground:", sharesBackground);
            KRSetBarButtonItemBool(item, @"setHidesSharedBackground:", !sharesBackground);
            return;
        }
#endif
        @try {
            [item setValue:@(sharesBackground) forKey:@"sharesBackground"];
            [item setValue:@(!sharesBackground) forKey:@"hidesSharedBackground"];
        } @catch (__unused NSException *exception) {
        }
    }
}

BOOL KRUIToolbarBarButtonItemIsAction(UIBarButtonItem *item) {
    if (!item) {
        return NO;
    }
    if (item.customView != nil) {
        return YES;
    }
    if (@available(iOS 14.0, *)) {
        if (item.primaryAction != nil) {
            return YES;
        }
    }
    if (item.target != nil) {
        return YES;
    }
    return NO;
}

void KRUIToolbarSyncActionItemsChrome(NSArray<UIBarButtonItem *> *toolbarItems,
                                          UIViewController *viewController) {
    (void)viewController;
    if (toolbarItems.count == 0) {
        return;
    }
    for (UIBarButtonItem *item in toolbarItems) {
        if (!KRUIToolbarBarButtonItemIsAction(item)) {
            continue;
        }
        NSString *identifier = item.accessibilityIdentifier;
        if (identifier.length == 0 || ![identifier hasPrefix:@"kr_toolbar_"]) {
            continue;
        }
        KRUIBarButtonItemApplyGlassBackground(item, YES);
    }
}

void KRUIBarButtonItemApplyAxis(UIBarButtonItem *item, NSString *axis) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270100
    if (@available(iOS 27.1, *)) {
        NSString *value = axis.lowercaseString ?: @"";
        if ([value isEqualToString:@"horizontalonly"]) {
            item.axisBehavior = UIBarButtonItemAxisBehaviorHorizontalOnly;
        } else if ([value isEqualToString:@"verticalpreferred"]) {
            item.axisBehavior = UIBarButtonItemAxisBehaviorVerticalPreferred;
        } else {
            item.axisBehavior = UIBarButtonItemAxisBehaviorAutomatic;
        }
    }
#else
    (void)item;
    (void)axis;
#endif
}

#else // [macOS] Toolbar 未实现；占位符号供链接，参数用 id 避免 macOS 无 UIBarButtonItem。

void KRUIBarButtonItemApplyGlassBackground(id item, BOOL sharesBackground) {
    (void)item;
    (void)sharesBackground;
}

void KRUIBarButtonItemApplyAxis(id item, NSString *axis) {
    (void)item;
    (void)axis;
}

BOOL KRUIToolbarBarButtonItemIsAction(id item) {
    (void)item;
    return NO;
}

void KRUIToolbarSyncActionItemsChrome(NSArray *toolbarItems, id viewController) {
    (void)toolbarItems;
    (void)viewController;
}

#endif
