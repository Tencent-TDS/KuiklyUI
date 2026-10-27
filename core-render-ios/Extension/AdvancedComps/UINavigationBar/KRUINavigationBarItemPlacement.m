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

#import "KRUINavigationBarItemPlacement.h"
#import "KRUINavigationBarKit.h"
#import "KRUIBarButtonItemKit.h"
#import <objc/runtime.h>

#if !TARGET_OS_OSX // [macOS]
static NSString *const kKRNavTrailingTopSpacerId = @"kr_nav_trailing_top_spacer";
static NSString *const kKRNavTrailingClusterGapId = @"kr_nav_trailing_cluster_gap";
static NSString *const kKRNavLeadingInsetSpacerId = @"kr_nav_leading_inset_spacer";
static NSString *const kKRNavTrailingInsetSpacerId = @"kr_nav_trailing_inset_spacer";
static const CGFloat kKRNavTrailingClusterGapWidth = 10.0;
static const void *kKRNavTrailingCustomViewCountKey = &kKRNavTrailingCustomViewCountKey;
static const void *kKRNavLeadingInsetKey = &kKRNavLeadingInsetKey;
static const void *kKRNavTrailingInsetKey = &kKRNavTrailingInsetKey;

static BOOL KRIsTrailingTopSpacerItem(UIBarButtonItem *item) {
    return [item.accessibilityIdentifier isEqualToString:kKRNavTrailingTopSpacerId];
}

static BOOL KRIsTrailingClusterGapItem(UIBarButtonItem *item) {
    return [item.accessibilityIdentifier isEqualToString:kKRNavTrailingClusterGapId];
}

static BOOL KRIsEdgeInsetSpacerItem(UIBarButtonItem *item) {
    NSString *identifier = item.accessibilityIdentifier;
    return [identifier isEqualToString:kKRNavLeadingInsetSpacerId]
        || [identifier isEqualToString:kKRNavTrailingInsetSpacerId];
}

static CGFloat KRNavigationInset(UIViewController *viewController, BOOL leading) {
    const void *key = leading ? kKRNavLeadingInsetKey : kKRNavTrailingInsetKey;
    return MAX(0, [objc_getAssociatedObject(viewController, key) doubleValue]);
}

static UIBarButtonItem *KRMakeLeadingInsetSpacer(CGFloat width) {
    CGFloat safeWidth = MAX(0, width);
    UIView *spacerView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, safeWidth, 44.0)];
    spacerView.backgroundColor = [UIColor clearColor];
    spacerView.userInteractionEnabled = NO;
    UIBarButtonItem *spacer = [[UIBarButtonItem alloc] initWithCustomView:spacerView];
    spacer.width = safeWidth;
    spacer.accessibilityIdentifier = kKRNavLeadingInsetSpacerId;
    KRUIBarButtonItemApplyGlassBackground(spacer, NO);
    return spacer;
}

static UIBarButtonItem *KRMakeEdgeInsetSpacer(NSString *identifier, CGFloat width) {
    if ([identifier isEqualToString:kKRNavLeadingInsetSpacerId]) {
        return KRMakeLeadingInsetSpacer(width);
    }
    UIBarButtonItem *spacer =
        [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                                     target:nil
                                                     action:nil];
    spacer.width = MAX(0, width);
    spacer.accessibilityIdentifier = identifier;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        spacer.hidesSharedBackground = YES;
        spacer.sharesBackground = NO;
    }
#endif
    return spacer;
}

static UIBarButtonItem *KRMakeTrailingClusterGapItem(void) {
    UIBarButtonItem *gap = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                                                         target:nil
                                                                         action:nil];
    gap.width = kKRNavTrailingClusterGapWidth;
    gap.accessibilityIdentifier = kKRNavTrailingClusterGapId;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        gap.hidesSharedBackground = YES;
        gap.sharesBackground = NO;
    }
#endif
    return gap;
}

@implementation KRUINavigationBarItemPlacement

+ (BOOL)isLeadingPlacement:(NSString *)placement {
    return ![placement isEqualToString:@"trailing"];
}

+ (NSArray<UIBarButtonItem *> *)p_itemsByRemovingEdgeInsetSpacers:(NSArray<UIBarButtonItem *> *)items {
    NSMutableArray<UIBarButtonItem *> *result = [NSMutableArray array];
    for (UIBarButtonItem *item in items) {
        if (!KRIsEdgeInsetSpacerItem(item)) {
            [result addObject:item];
        }
    }
    return result;
}

+ (NSArray<UIBarButtonItemGroup *> *)p_groupsByRemovingEdgeInsetSpacers:
    (NSArray<UIBarButtonItemGroup *> *)groups API_AVAILABLE(ios(16.0)) {
    NSMutableArray<UIBarButtonItemGroup *> *result = [NSMutableArray array];
    for (UIBarButtonItemGroup *group in groups) {
        NSArray<UIBarButtonItem *> *items = [self p_itemsByRemovingEdgeInsetSpacers:group.barButtonItems];
        if (items.count > 0) {
            [result addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:items
                                                               representativeItem:group.representativeItem]];
        }
    }
    return result;
}

+ (void)p_applyEdgeInset:(CGFloat)inset
                  leading:(BOOL)leading
           viewController:(UIViewController *)viewController {
    UINavigationItem *navigationItem = viewController.navigationItem;
    NSString *identifier = leading ? kKRNavLeadingInsetSpacerId : kKRNavTrailingInsetSpacerId;
    UIBarButtonItem *spacer = inset > 0.5 ? KRMakeEdgeInsetSpacer(identifier, inset) : nil;
    if (@available(iOS 16.0, *)) {
        NSArray<UIBarButtonItemGroup *> *currentGroups =
            leading ? navigationItem.leadingItemGroups : navigationItem.trailingItemGroups;
        if (currentGroups.count > 0) {
            NSMutableArray<UIBarButtonItemGroup *> *nextGroups =
                [[self p_groupsByRemovingEdgeInsetSpacers:currentGroups] mutableCopy];
            if (spacer) {
                UIBarButtonItemGroup *spacerGroup =
                    [[UIBarButtonItemGroup alloc] initWithBarButtonItems:@[spacer]
                                                     representativeItem:nil];
                [nextGroups insertObject:spacerGroup atIndex:0];
            }
            if (leading) {
                navigationItem.leadingItemGroups = nextGroups;
            } else {
                navigationItem.trailingItemGroups = nextGroups;
            }
            return;
        }
    }
    NSArray<UIBarButtonItem *> *current =
        leading ? navigationItem.leftBarButtonItems : navigationItem.rightBarButtonItems;
    NSMutableArray<UIBarButtonItem *> *next =
        [[self p_itemsByRemovingEdgeInsetSpacers:current] mutableCopy];
    if (spacer) {
        [next insertObject:spacer atIndex:0];
    }
    if (leading) {
        [navigationItem setLeftBarButtonItems:next animated:NO];
    } else {
        [navigationItem setRightBarButtonItems:next animated:NO];
    }
}

+ (BOOL)p_viewController:(UIViewController *)viewController
    hasInstalledItemIdentifier:(NSString *)itemIdentifier
                     placement:(NSString *)placement {
    if (!viewController || itemIdentifier.length == 0) {
        return NO;
    }
    BOOL replacing = NO;
    BOOL leading = [self isLeadingPlacement:placement];
    [self p_flattenedItemsForViewController:viewController
                                    leading:leading
                                 identifier:itemIdentifier
                                  replacing:&replacing];
    return replacing;
}

+ (void)setLeadingInset:(CGFloat)leadingInset
          trailingInset:(CGFloat)trailingInset
         viewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    CGFloat safeLeadingInset = MAX(0, leadingInset);
    CGFloat safeTrailingInset = MAX(0, trailingInset);
    CGFloat previousLeading = KRNavigationInset(viewController, YES);
    CGFloat previousTrailing = KRNavigationInset(viewController, NO);
    if (fabs(previousLeading - safeLeadingInset) < 0.5 && fabs(previousTrailing - safeTrailingInset) < 0.5) {
        return;
    }
    objc_setAssociatedObject(viewController,
                             kKRNavLeadingInsetKey,
                             @(safeLeadingInset),
                             OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController,
                             kKRNavTrailingInsetKey,
                             @(safeTrailingInset),
                             OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [self p_applyEdgeInset:safeLeadingInset leading:YES viewController:viewController];
    [self p_applyEdgeInset:safeTrailingInset leading:NO viewController:viewController];
}

+ (NSArray<UIBarButtonItem *> *)p_updatedItems:(NSArray<UIBarButtonItem *> *)items
                                      withItem:(UIBarButtonItem *)item
                                    identifier:(NSString *)itemIdentifier
                                         index:(NSInteger)target {
    NSMutableArray<UIBarButtonItem *> *next = [NSMutableArray array];
    for (UIBarButtonItem *existing in items) {
        if (![existing.accessibilityIdentifier isEqualToString:itemIdentifier]) {
            [next addObject:existing];
        }
    }
    NSUInteger insertAt = next.count;
    for (NSUInteger idx = 0; idx < next.count; idx++) {
        if (KRUINavigationBarButtonItemSortIndex(next[idx]) > target) {
            insertAt = idx;
            break;
        }
    }
    [next insertObject:item atIndex:insertAt];
    return next;
}

+ (NSArray<UIBarButtonItem *> *)p_flattenedItemsForViewController:(UIViewController *)viewController
                                                          leading:(BOOL)leading
                                                       identifier:(NSString *)itemIdentifier
                                                        replacing:(BOOL *)replacing {
    NSMutableArray<UIBarButtonItem *> *items = [NSMutableArray array];
    if (@available(iOS 16.0, *)) {
        NSArray<UIBarButtonItemGroup *> *groups = leading
            ? viewController.navigationItem.leadingItemGroups
            : viewController.navigationItem.trailingItemGroups;
        for (UIBarButtonItemGroup *group in groups) {
            for (UIBarButtonItem *existing in group.barButtonItems) {
                if (KRIsEdgeInsetSpacerItem(existing)) {
                    continue;
                }
                if ([existing.accessibilityIdentifier isEqualToString:itemIdentifier]) {
                    if (replacing) {
                        *replacing = YES;
                    }
                }
                if (![items containsObject:existing]) {
                    [items addObject:existing];
                }
            }
        }
    }
    NSArray<UIBarButtonItem *> *barItems = leading
        ? viewController.navigationItem.leftBarButtonItems
        : viewController.navigationItem.rightBarButtonItems;
    for (UIBarButtonItem *existing in barItems) {
        if (KRIsEdgeInsetSpacerItem(existing)) {
            continue;
        }
        if ([existing.accessibilityIdentifier isEqualToString:itemIdentifier] && replacing) {
            *replacing = YES;
        }
        if (![items containsObject:existing]) {
            [items addObject:existing];
        }
    }
    return items;
}

+ (void)p_sortBarButtonItemsBySortIndex:(NSMutableArray<UIBarButtonItem *> *)items {
    [items sortUsingComparator:^NSComparisonResult(UIBarButtonItem *a, UIBarButtonItem *b) {
        NSInteger aIndex = KRUINavigationBarButtonItemSortIndex(a);
        NSInteger bIndex = KRUINavigationBarButtonItemSortIndex(b);
        if (aIndex < bIndex) {
            return NSOrderedAscending;
        }
        if (aIndex > bIndex) {
            return NSOrderedDescending;
        }
        return NSOrderedSame;
    }];
}

+ (BOOL)p_shouldUseTrailingItemGroups:(NSDictionary<NSNumber *, NSArray<UIBarButtonItem *> *> *)buckets {
    for (NSArray<UIBarButtonItem *> *items in buckets.allValues) {
        if (items.count > 1) {
            return YES;
        }
    }
    if (buckets.count >= 2) {
        return YES;
    }
    for (NSNumber *key in buckets) {
        if (key.integerValue != 0) {
            return YES;
        }
    }
    return NO;
}

+ (BOOL)p_shouldRetainTrailingBarButtonItem:(UIBarButtonItem *)item
                             viewController:(UIViewController *)viewController {
    if (!item) {
        return NO;
    }
    return KRUINavigationBarShouldRetainManagedTrailingBarItem(viewController,
                                                                item.accessibilityIdentifier);
}

+ (void)p_pruneStaleManagedTrailingItemsForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    UINavigationItem *navigationItem = viewController.navigationItem;
    if (@available(iOS 16.0, *)) {
        NSArray<UIBarButtonItemGroup *> *groups = navigationItem.trailingItemGroups;
        if (groups.count > 0) {
            NSMutableArray<UIBarButtonItemGroup *> *nextGroups = [NSMutableArray array];
            for (UIBarButtonItemGroup *group in groups) {
                NSMutableArray<UIBarButtonItem *> *items = [NSMutableArray array];
                for (UIBarButtonItem *item in group.barButtonItems) {
                    if ([self p_shouldRetainTrailingBarButtonItem:item viewController:viewController]) {
                        [items addObject:item];
                    }
                }
                if (items.count > 0) {
                    [nextGroups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:items
                                                                           representativeItem:group.representativeItem]];
                }
            }
            navigationItem.trailingItemGroups = nextGroups;
        }
    }
    NSArray<UIBarButtonItem *> *flat = navigationItem.rightBarButtonItems;
    if (flat.count > 0) {
        NSMutableArray<UIBarButtonItem *> *nextFlat = [NSMutableArray array];
        for (UIBarButtonItem *item in flat) {
            if ([self p_shouldRetainTrailingBarButtonItem:item viewController:viewController]) {
                [nextFlat addObject:item];
            }
        }
        [navigationItem setRightBarButtonItems:nextFlat.count > 0 ? nextFlat : nil animated:NO];
    }
}

+ (NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *)p_trailingBucketsForViewController:(UIViewController *)viewController
                                                                                            excludingIdentifier:(NSString *)itemIdentifier {
    [self p_pruneStaleManagedTrailingItemsForViewController:viewController];
    NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *buckets = [NSMutableDictionary dictionary];
    void (^addItem)(UIBarButtonItem *) = ^(UIBarButtonItem *item) {
        if (!item || KRIsTrailingTopSpacerItem(item) || KRIsTrailingClusterGapItem(item)
            || KRIsEdgeInsetSpacerItem(item)) {
            return;
        }
        if (![self p_shouldRetainTrailingBarButtonItem:item viewController:viewController]) {
            return;
        }
        if (itemIdentifier.length > 0
            && [item.accessibilityIdentifier isEqualToString:itemIdentifier]) {
            return;
        }
        NSInteger groupId = KRUINavigationBarButtonItemGroupId(item);
        NSNumber *key = @(groupId);
        NSMutableArray<UIBarButtonItem *> *bucket = buckets[key];
        if (!bucket) {
            bucket = [NSMutableArray array];
            buckets[key] = bucket;
        }
        for (UIBarButtonItem *existing in bucket) {
            if (existing == item
                || [existing.accessibilityIdentifier isEqualToString:item.accessibilityIdentifier]) {
                return;
            }
        }
        [bucket addObject:item];
    };
    if (@available(iOS 16.0, *)) {
        for (UIBarButtonItemGroup *group in viewController.navigationItem.trailingItemGroups) {
            for (UIBarButtonItem *item in group.barButtonItems) {
                addItem(item);
            }
        }
    }
    for (UIBarButtonItem *item in viewController.navigationItem.rightBarButtonItems) {
        addItem(item);
    }
    return buckets;
}

+ (BOOL)p_trailingBucketsContainIdentifier:(NSDictionary<NSNumber *, NSArray<UIBarButtonItem *> *> *)buckets
                                identifier:(NSString *)itemIdentifier {
    for (NSArray<UIBarButtonItem *> *bucket in buckets.allValues) {
        for (UIBarButtonItem *item in bucket) {
            if ([item.accessibilityIdentifier isEqualToString:itemIdentifier]) {
                return YES;
            }
        }
    }
    return NO;
}

+ (void)p_applyTrailingBuckets:(NSDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *)buckets
                viewController:(UIViewController *)viewController
                      animated:(BOOL)animated {
    if (![self p_shouldUseTrailingItemGroups:buckets]) {
        NSMutableArray<UIBarButtonItem *> *flat = [NSMutableArray array];
        NSMutableArray<UIBarButtonItem *> *groupZero = [buckets[@(0)] mutableCopy];
        if (groupZero) {
            [self p_sortBarButtonItemsBySortIndex:groupZero];
            [flat addObjectsFromArray:groupZero];
        }
        CGFloat trailingInset = KRNavigationInset(viewController, NO);
        if (trailingInset > 0.5) {
            [flat insertObject:KRMakeEdgeInsetSpacer(kKRNavTrailingInsetSpacerId, trailingInset) atIndex:0];
        }
        if (@available(iOS 16.0, *)) {
            viewController.navigationItem.trailingItemGroups = @[];
        }
        [viewController.navigationItem setRightBarButtonItems:flat animated:animated];
        return;
    }
    if (@available(iOS 16.0, *)) {
        NSArray<NSNumber *> *keys = [[buckets allKeys] sortedArrayUsingComparator:^NSComparisonResult(NSNumber *a, NSNumber *b) {
            return [a compare:b];
        }];
        NSMutableArray<UIBarButtonItemGroup *> *groups = [NSMutableArray array];
        CGFloat trailingInset = KRNavigationInset(viewController, NO);
        if (trailingInset > 0.5) {
            UIBarButtonItem *spacer = KRMakeEdgeInsetSpacer(kKRNavTrailingInsetSpacerId, trailingInset);
            [groups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:@[spacer]
                                                               representativeItem:nil]];
        }
        NSUInteger contentGroupCount = 0;
        for (NSUInteger clusterIndex = 0; clusterIndex < keys.count; clusterIndex++) {
            NSNumber *key = keys[clusterIndex];
            NSMutableArray<UIBarButtonItem *> *items = [buckets[key] mutableCopy];
            if (items.count == 0) {
                continue;
            }
            [self p_sortBarButtonItemsBySortIndex:items];
            if (contentGroupCount > 0) {
                [groups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:@[KRMakeTrailingClusterGapItem()]
                                                                    representativeItem:nil]];
            }
            [groups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:items
                                                                representativeItem:nil]];
            contentGroupCount += 1;
        }
        if (contentGroupCount == 0) {
            viewController.navigationItem.trailingItemGroups = @[];
            [viewController.navigationItem setRightBarButtonItems:nil animated:animated];
            return;
        }
        viewController.navigationItem.trailingItemGroups = groups;
        viewController.navigationItem.rightBarButtonItems = nil;
        return;
    }
    NSMutableArray<UIBarButtonItem *> *flat = [NSMutableArray array];
    NSArray<NSNumber *> *keys = [[buckets allKeys] sortedArrayUsingComparator:^NSComparisonResult(NSNumber *a, NSNumber *b) {
        return [a compare:b];
    }];
    for (NSNumber *key in keys) {
        NSMutableArray<UIBarButtonItem *> *items = [buckets[key] mutableCopy];
        [self p_sortBarButtonItemsBySortIndex:items];
        [flat addObjectsFromArray:items];
    }
    CGFloat trailingInset = KRNavigationInset(viewController, NO);
    if (trailingInset > 0.5) {
        [flat insertObject:KRMakeEdgeInsetSpacer(kKRNavTrailingInsetSpacerId, trailingInset) atIndex:0];
    }
    [viewController.navigationItem setRightBarButtonItems:flat animated:animated];
}

+ (NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *)p_trailingBucketsByRemovingItem:(UIBarButtonItem *)currentItem
                                                                                               identifier:(NSString *)itemIdentifier
                                                                                           fromViewController:(UIViewController *)viewController {
    NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *buckets =
        [self p_trailingBucketsForViewController:viewController excludingIdentifier:nil];
    for (NSNumber *key in [buckets.allKeys copy]) {
        NSMutableArray<UIBarButtonItem *> *bucket = buckets[key];
        NSMutableArray<UIBarButtonItem *> *next = [NSMutableArray array];
        for (UIBarButtonItem *item in bucket) {
            BOOL isOurs = (item == currentItem)
                || [item.accessibilityIdentifier isEqualToString:itemIdentifier];
            if (!isOurs) {
                [next addObject:item];
            }
        }
        if (next.count > 0) {
            buckets[key] = next;
        } else {
            [buckets removeObjectForKey:key];
        }
    }
    return buckets;
}

+ (void)p_applyItems:(NSArray<UIBarButtonItem *> *)items
      viewController:(UIViewController *)viewController
             leading:(BOOL)leading
            animated:(BOOL)animated {
    NSMutableArray<UIBarButtonItem *> *contentItems = [items mutableCopy] ?: [NSMutableArray array];
    CGFloat inset = KRNavigationInset(viewController, leading);
    if (@available(iOS 16.0, *)) {
        if (leading) {
            NSMutableArray<UIBarButtonItemGroup *> *groups = [NSMutableArray array];
            if (inset > 0.5) {
                UIBarButtonItem *spacer = KRMakeLeadingInsetSpacer(inset);
                [groups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:@[spacer]
                                                                     representativeItem:nil]];
            }
            if (contentItems.count > 0) {
                [groups addObject:[[UIBarButtonItemGroup alloc] initWithBarButtonItems:contentItems
                                                                     representativeItem:nil]];
            }
            viewController.navigationItem.leadingItemGroups = groups;
            [viewController.navigationItem setLeftBarButtonItems:nil animated:animated];
            return;
        }
        viewController.navigationItem.trailingItemGroups = @[];
    }
    NSMutableArray<UIBarButtonItem *> *itemsWithInset = contentItems;
    if (inset > 0.5) {
        NSString *identifier = leading ? kKRNavLeadingInsetSpacerId : kKRNavTrailingInsetSpacerId;
        [itemsWithInset insertObject:KRMakeEdgeInsetSpacer(identifier, inset) atIndex:0];
    }
    if (leading) {
        [viewController.navigationItem setLeftBarButtonItems:itemsWithInset animated:animated];
    } else {
        [viewController.navigationItem setRightBarButtonItems:itemsWithInset animated:animated];
    }
}

+ (NSArray<UIBarButtonItem *> *)p_itemsByRemoving:(NSArray<UIBarButtonItem *> *)items
                                      currentItem:(UIBarButtonItem *)currentItem
                                       identifier:(NSString *)itemIdentifier {
    NSMutableArray<UIBarButtonItem *> *next = [NSMutableArray array];
    for (UIBarButtonItem *item in items) {
        BOOL isOurs = (item == currentItem)
            || [item.accessibilityIdentifier isEqualToString:itemIdentifier];
        if (!isOurs) {
            [next addObject:item];
        }
    }
    return next;
}

+ (BOOL)p_trailingItemsContainIdentifier:(NSArray<UIBarButtonItem *> *)items
                                identifier:(NSString *)identifier {
    for (UIBarButtonItem *existing in items) {
        if ([existing.accessibilityIdentifier isEqualToString:identifier]) {
            return YES;
        }
    }
    return NO;
}

+ (void)p_installTrailingTopSpacer:(CGFloat)height
                    viewController:(UIViewController *)viewController {
    if (height < 1.0) {
        return;
    }
    NSArray<UIBarButtonItem *> *current = [self p_flattenedItemsForViewController:viewController
                                                                          leading:NO
                                                                       identifier:kKRNavTrailingTopSpacerId
                                                                        replacing:NULL];
    if ([self p_trailingItemsContainIdentifier:current identifier:kKRNavTrailingTopSpacerId]) {
        return;
    }
    UIView *spacer = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 44.0, height)];
    spacer.backgroundColor = [UIColor clearColor];
    spacer.userInteractionEnabled = NO;
    UIBarButtonItem *space = [[UIBarButtonItem alloc] initWithCustomView:spacer];
    space.accessibilityIdentifier = kKRNavTrailingTopSpacerId;
    KRUINavigationBarSetBarButtonItemSortIndex(space, KRUINavigationBarButtonItemSortIndexTrailingTopSpacer);
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        space.hidesSharedBackground = YES;
        space.sharesBackground = NO;
    }
#endif
    [self installItem:space
           identifier:kKRNavTrailingTopSpacerId
            placement:@"trailing"
                group:0
                index:KRUINavigationBarButtonItemSortIndexTrailingTopSpacer
       viewController:viewController];
}

+ (void)p_removeTrailingTopSpacerIfNeeded:(UIViewController *)viewController {
    NSNumber *count = objc_getAssociatedObject(viewController, kKRNavTrailingCustomViewCountKey);
    if (count.integerValue > 0) {
        return;
    }
    NSArray<UIBarButtonItem *> *current = [self p_flattenedItemsForViewController:viewController
                                                                          leading:NO
                                                                       identifier:kKRNavTrailingTopSpacerId
                                                                        replacing:NULL];
    UIBarButtonItem *spacer = nil;
    for (UIBarButtonItem *item in current) {
        if ([item.accessibilityIdentifier isEqualToString:kKRNavTrailingTopSpacerId]) {
            spacer = item;
            break;
        }
    }
    if (!spacer) {
        return;
    }
    NSArray<UIBarButtonItem *> *next = [self p_itemsByRemoving:current
                                                   currentItem:spacer
                                                    identifier:kKRNavTrailingTopSpacerId];
    [self p_applyItems:next viewController:viewController leading:NO animated:YES];
}

+ (void)installCustomViewItem:(UIBarButtonItem *)item
                   identifier:(NSString *)itemIdentifier
                    placement:(NSString *)placement
                        group:(NSInteger)group
                        index:(NSInteger)index
               viewController:(UIViewController *)viewController
                     topInset:(CGFloat)topInset {
    BOOL leading = [self isLeadingPlacement:placement];
    BOOL replacing = [self p_viewController:viewController
                   hasInstalledItemIdentifier:itemIdentifier
                                    placement:placement];
    if (!leading && topInset > 0.5 && !replacing) {
        NSNumber *count = objc_getAssociatedObject(viewController, kKRNavTrailingCustomViewCountKey);
        NSInteger next = count.integerValue + 1;
        objc_setAssociatedObject(viewController, kKRNavTrailingCustomViewCountKey, @(next), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        [self p_installTrailingTopSpacer:topInset viewController:viewController];
    }
    [self installItem:item
           identifier:itemIdentifier
            placement:placement
                group:group
                index:index
       viewController:viewController];
}

+ (void)uninstallCustomViewItem:(UIBarButtonItem *)item
                     identifier:(NSString *)itemIdentifier
                      placement:(NSString *)placement
                 viewController:(UIViewController *)viewController
                       topInset:(CGFloat)topInset {
    [self uninstallItem:item identifier:itemIdentifier placement:placement viewController:viewController];
    BOOL leading = [self isLeadingPlacement:placement];
    if (!leading && topInset > 0.5) {
        NSNumber *count = objc_getAssociatedObject(viewController, kKRNavTrailingCustomViewCountKey);
        NSInteger next = MAX(0, count.integerValue - 1);
        objc_setAssociatedObject(viewController, kKRNavTrailingCustomViewCountKey, @(next), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        [self p_removeTrailingTopSpacerIfNeeded:viewController];
    }
}

+ (void)installItem:(UIBarButtonItem *)item
         identifier:(NSString *)itemIdentifier
          placement:(NSString *)placement
              group:(NSInteger)group
              index:(NSInteger)index
     viewController:(UIViewController *)viewController {
    viewController.navigationItem.leftItemsSupplementBackButton = YES;
    BOOL leading = [self isLeadingPlacement:placement];
    if (!leading) {
        KRUINavigationBarRegisterTrailingBarItemIdentifier(viewController, itemIdentifier);
        KRUINavigationBarSetBarButtonItemGroupId(item, group);
        KRUINavigationBarSetBarButtonItemSortIndex(item, index);
        NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *buckets =
            [self p_trailingBucketsForViewController:viewController excludingIdentifier:itemIdentifier];
        BOOL replacing = [self p_trailingBucketsContainIdentifier:buckets identifier:itemIdentifier];
        NSNumber *key = @(group);
        NSMutableArray<UIBarButtonItem *> *bucket = buckets[key];
        if (!bucket) {
            bucket = [NSMutableArray array];
            buckets[key] = bucket;
        }
        NSMutableArray<UIBarButtonItem *> *deduped = [NSMutableArray array];
        for (UIBarButtonItem *existing in bucket) {
            if (![existing.accessibilityIdentifier isEqualToString:itemIdentifier]) {
                [deduped addObject:existing];
            }
        }
        [deduped addObject:item];
        buckets[key] = deduped;
        [self p_applyTrailingBuckets:buckets viewController:viewController animated:!replacing];
        return;
    }
    BOOL replacing = NO;
    NSArray<UIBarButtonItem *> *current = [self p_flattenedItemsForViewController:viewController
                                                                          leading:leading
                                                                       identifier:itemIdentifier
                                                                        replacing:&replacing];
    KRUINavigationBarSetBarButtonItemSortIndex(item, index);
    NSArray<UIBarButtonItem *> *next = [self p_updatedItems:current
                                                   withItem:item
                                                 identifier:itemIdentifier
                                                      index:index];
    [self p_applyItems:next viewController:viewController leading:leading animated:!replacing];
}

+ (void)uninstallItem:(UIBarButtonItem *)item
           identifier:(NSString *)itemIdentifier
            placement:(NSString *)placement
       viewController:(UIViewController *)viewController {
    BOOL leading = [self isLeadingPlacement:placement];
    if (!leading) {
        KRUINavigationBarUnregisterTrailingBarItemIdentifier(viewController, itemIdentifier);
        NSMutableDictionary<NSNumber *, NSMutableArray<UIBarButtonItem *> *> *buckets =
            [self p_trailingBucketsByRemovingItem:item
                                       identifier:itemIdentifier
                               fromViewController:viewController];
        [self p_applyTrailingBuckets:buckets viewController:viewController animated:YES];
        return;
    }
    NSArray<UIBarButtonItem *> *current = [self p_flattenedItemsForViewController:viewController
                                                                          leading:leading
                                                                       identifier:itemIdentifier
                                                                        replacing:NULL];
    NSArray<UIBarButtonItem *> *next = [self p_itemsByRemoving:current
                                                   currentItem:item
                                                    identifier:itemIdentifier];
    [self p_applyItems:next viewController:viewController leading:leading animated:YES];
}

@end
#endif // [macOS]
