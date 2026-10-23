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

#import "KRUIToolbarItem.h"
#import "KRUIToolbar.h"
#import "KRComponentDefine.h"
#import "KRUIBarImage.h"
#import "KRUIBarButtonItemKit.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import <objc/runtime.h>

#if !TARGET_OS_OSX // [macOS]

@interface KRUIToolbarItem ()
@property (nonatomic, copy) NSString *css_symbol;
@property (nonatomic, copy) NSString *css_src;
@property (nonatomic, strong) id css_template;
@property (nonatomic, copy) NSString *css_title;
@property (nonatomic, copy) NSString *css_kind;
@property (nonatomic, copy) NSString *css_spaceWidth;
@property (nonatomic, copy) NSString *css_index;
@property (nonatomic, copy) NSString *css_axis;
@property (nonatomic, strong) id css_tintColor;
@property (nonatomic, strong) UIBarButtonItem *barButtonItem;
@property (nonatomic, copy) NSString *itemIdentifier;
@property (nonatomic, assign) NSInteger animationToken;
@property (nonatomic, assign) NSInteger imageToken;
/** 建 [barButtonItem] 那一刻的「结构性」属性。只有这些变了才需要重建 UIBarButtonItem。 */
@property (nonatomic, copy) NSString *builtIdentity;
@end

@implementation KRUIToolbarItem

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.hidden = YES;
        self.alpha = 0;
        self.userInteractionEnabled = NO;
        self.backgroundColor = [UIColor clearColor];
        _css_symbol = @"square.and.arrow.up";
        _css_src = @"";
        _css_template = @"true";
        _css_title = @"";
        _css_kind = @"item";
        _css_spaceWidth = @"";
        _css_index = @"0";
        _css_axis = @"automatic";
        _itemIdentifier = [NSString stringWithFormat:@"kr_toolbar_%p", self];
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

- (void)setCss_symbol:(NSString *)css_symbol {
    _css_symbol = [css_symbol copy];
    [self p_syncToToolbar];
}

- (void)setCss_src:(NSString *)css_src {
    _css_src = [css_src copy];
    [self p_syncToToolbar];
}

- (void)setCss_template:(id)css_template {
    _css_template = css_template;
    [self p_syncToToolbar];
}

- (void)setCss_title:(NSString *)css_title {
    _css_title = [css_title copy];
    [self p_syncToToolbar];
}

- (void)setCss_kind:(NSString *)css_kind {
    _css_kind = [css_kind copy];
    [self p_syncToToolbar];
}

- (void)setCss_spaceWidth:(NSString *)css_spaceWidth {
    NSString *width = css_spaceWidth ?: @"";
    if ([_css_spaceWidth isEqualToString:width]) {
        return;
    }
    _css_spaceWidth = [width copy];
    if (![self p_isSpaceKind]) {
        [self p_syncToToolbar];
        return;
    }
    if (![self p_shouldShow]) {
        return;
    }
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    // 只改 width 不会让 iOS 26 的玻璃胶囊重排，要换成新的 fixed space 再强制布局。
    [self p_putItem:[self p_makeItemWithImage:nil]];
    UIToolbar *toolbar = viewController.navigationController.toolbar;
    [toolbar setNeedsLayout];
    [toolbar layoutIfNeeded];
}

- (void)setCss_index:(NSString *)css_index {
    _css_index = [css_index copy];
    [self p_syncToToolbar];
}

- (void)setCss_axis:(NSString *)css_axis {
    _css_axis = [css_axis copy];
    [self p_syncToToolbar];
}

- (void)setCss_tintColor:(id)css_tintColor {
    _css_tintColor = css_tintColor;
    [self p_syncToToolbar];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_syncToToolbar];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview) {
        [self p_syncToToolbar];
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.hidden = YES;
    self.alpha = 0;
    self.userInteractionEnabled = NO;
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_syncToToolbar];
        __weak typeof(self) weakSelf = self;
        [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
            [weakSelf p_uninstallFromToolbar];
        }];
    } else {
        [self p_uninstallFromToolbar];
    }
}

- (void)removeFromSuperview {
    [self p_uninstallFromToolbar];
    [super removeFromSuperview];
}

#pragma mark - private

- (UIViewController *)p_hostViewController {
    return self.kr_viewController ?: self.hr_rootView.kr_viewController;
}

- (BOOL)p_shouldShow {
    if (self.css_visibility != nil) {
        return self.css_visibility.boolValue;
    }
    return YES;
}

- (NSInteger)p_itemIndex {
    return self.css_index.length ? self.css_index.integerValue : 0;
}

- (BOOL)p_useTemplate {
    if (!self.css_template) {
        return YES;
    }
    return [UIView css_bool:self.css_template];
}

- (BOOL)p_isSpaceKind {
    NSString *kind = self.css_kind.length ? self.css_kind : @"item";
    return [kind isEqualToString:@"flexibleSpace"] || [kind isEqualToString:@"fixedSpace"];
}

/**
 * 只有这几项会改变 UIBarButtonItem 本体（图片 / 空白 / 宽度）。
 * title、index、tintColor、axis 都能就地改，不需要重建。
 */
- (NSString *)p_identitySignature {
    return [NSString stringWithFormat:@"%@|%@|%@|%@|%@",
            self.css_kind ?: @"",
            self.css_symbol ?: @"",
            self.css_src ?: @"",
            self.css_template ?: @"",
            self.css_spaceWidth ?: @""];
}

/**
 * 页面一次刷新里每颗按钮的属性是逐个下发的，原来每个属性都会重建一次 UIBarButtonItem
 * 并 setToolbarItems:，iOS 26 的玻璃胶囊被反复重建后会有按钮接不到手势。
 * 结构性属性没变时，直接把可就地改的属性写回已装上去的那颗，不再刷 UIToolbar。
 */
- (BOOL)p_tryUpdateInstalledItem {
    UIBarButtonItem *item = self.barButtonItem;
    if (!item || [self p_isSpaceKind]) {
        return NO;
    }
    if (![[self p_identitySignature] isEqualToString:self.builtIdentity]) {
        return NO;
    }
    // 整组被外部清掉了（换页、重置）就按新增重建，别就地改一个已经不在栏上的 item。
    UIViewController *viewController = [self p_hostViewController];
    BOOL installed = NO;
    for (UIBarButtonItem *installedItem in viewController.toolbarItems ?: @[]) {
        if (installedItem == item) {
            installed = YES;
            break;
        }
    }
    if (!installed) {
        return NO;
    }
    item.accessibilityLabel = self.css_title.length ? self.css_title : self.css_symbol;
    UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
    if (tint) {
        item.tintColor = tint;
    }
    KRUIBarButtonItemApplyAxis(item, self.css_axis ?: @"automatic");
    NSInteger index = [self p_itemIndex];
    if (item.tag == index) {
        // 顺序也没变，连 setToolbarItems: 都不用调。
        self.imageToken += 1;
        UIToolbar *toolbar = viewController.navigationController.toolbar;
        [toolbar setNeedsLayout];
        return YES;
    }
    item.tag = index;
    self.imageToken += 1;
    [self p_reorderInstalledItems];
    return YES;
}

/** index 变了才需要整组重排一次。 */
- (void)p_reorderInstalledItems {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    NSMutableArray<UIBarButtonItem *> *items =
        [(viewController.toolbarItems ?: @[]) mutableCopy];
    [items sortUsingComparator:^NSComparisonResult(UIBarButtonItem *left, UIBarButtonItem *right) {
        if (left.tag < right.tag) {
            return NSOrderedAscending;
        }
        if (left.tag > right.tag) {
            return NSOrderedDescending;
        }
        return NSOrderedSame;
    }];
    KRUIToolbarSyncActionItemsChrome(items, viewController);
    [viewController setToolbarItems:items animated:NO];
}

- (UIBarButtonItem *)p_makeItemWithImage:(UIImage *)image {
    NSString *kind = self.css_kind.length ? self.css_kind : @"item";
    UIBarButtonItem *item = nil;
    if ([kind isEqualToString:@"flexibleSpace"]) {
        item = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace
                                                             target:nil
                                                             action:nil];
    } else if ([kind isEqualToString:@"fixedSpace"]) {
        CGFloat width = self.css_spaceWidth.length ? self.css_spaceWidth.doubleValue : 16;
        if (@available(iOS 14.0, *)) {
            item = [UIBarButtonItem fixedSpaceItemOfWidth:width];
        } else {
            item = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                                                 target:nil
                                                                 action:nil];
            item.width = width;
        }
    } else {
        NSString *symbol = self.css_symbol.length ? self.css_symbol : @"square.and.arrow.up";
        UIImage *configured = image;
        if (!configured) {
            if (@available(iOS 13.0, *)) {
                UIImage *fallback = [UIImage systemImageNamed:symbol];
                UIImageSymbolConfiguration *config =
                    [UIImageSymbolConfiguration configurationWithPointSize:17 weight:UIImageSymbolWeightRegular];
                configured = [fallback imageWithConfiguration:config] ?: fallback;
            }
        }
        item = [[UIBarButtonItem alloc] initWithImage:configured
                                                style:UIBarButtonItemStylePlain
                                               target:self
                                               action:@selector(p_onItem:)];
        // 液态玻璃栏只走 primaryAction，target/action 不会回到 Kuikly。
        if (@available(iOS 14.0, *)) {
            __weak typeof(self) weakSelf = self;
            item.primaryAction = [UIAction actionWithHandler:^(__kindof UIAction * _Nonnull action) {
                [weakSelf p_onItem:action.sender];
            }];
        }
        UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
        if (tint) {
            item.tintColor = tint;
        }
        KRUIBarButtonItemApplyGlassBackground(item, YES);
        KRUIBarButtonItemApplyAxis(item, self.css_axis ?: @"automatic");
    }
    item.accessibilityIdentifier = self.itemIdentifier;
    item.accessibilityLabel = self.css_title.length ? self.css_title : self.css_symbol;
    item.tag = [self p_itemIndex];
    return item;
}

- (void)p_reloadItem {
    NSInteger token = ++self.imageToken;
    if ([self p_isSpaceKind]) {
        [self p_putItem:[self p_makeItemWithImage:nil]];
        return;
    }
    __weak typeof(self) weakSelf = self;
    [KRUIBarImage resolveSrc:self.css_src
                          symbol:self.css_symbol.length ? self.css_symbol : @"square.and.arrow.up"
                  symbolFontWeight:KRUIBarImageSymbolFontWeightRegular
                       useTemplate:[self p_useTemplate]
                          rootView:self.hr_rootView
                        completion:^(UIImage *image) {
        __strong typeof(weakSelf) self = weakSelf;
        if (!self || token != self.imageToken || ![self p_shouldShow]) {
            return;
        }
        [self p_putItem:[self p_makeItemWithImage:image]];
    }];
}

- (void)p_syncToToolbar {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    if (![self p_shouldShow]) {
        self.imageToken += 1;
        if (self.barButtonItem) {
            [self p_uninstallFromToolbar];
        }
        return;
    }
    if ([self p_tryUpdateInstalledItem]) {
        return;
    }
    self.animationToken += 1;
    [self p_reloadItem];
}

- (void)p_putItem:(UIBarButtonItem *)item {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    self.barButtonItem = item;
    self.builtIdentity = [self p_identitySignature];
    NSMutableArray<UIBarButtonItem *> *items =
        [(viewController.toolbarItems ?: @[]) mutableCopy];
    NSUInteger existing = NSNotFound;
    for (NSUInteger index = 0; index < items.count; index++) {
        if ([items[index].accessibilityIdentifier isEqualToString:self.itemIdentifier]) {
            existing = index;
            break;
        }
    }
    BOOL inserted = existing == NSNotFound;
    if (!inserted) {
        items[existing] = item;
    } else {
        [items addObject:item];
    }
    // 按 index 重排。空白在最前，宽度才是左列表右边缘，整组按钮让开左边。
    [items sortUsingComparator:^NSComparisonResult(UIBarButtonItem *left, UIBarButtonItem *right) {
        if (left.tag < right.tag) {
            return NSOrderedAscending;
        }
        if (left.tag > right.tag) {
            return NSOrderedDescending;
        }
        return NSOrderedSame;
    }];
    KRUIToolbarSyncActionItemsChrome(items, viewController);
    [viewController setToolbarItems:items animated:NO];
    [KRUIToolbar scheduleApplyLeadingInsetForViewController:viewController];
    if (!inserted) {
        UIToolbar *toolbar = viewController.navigationController.toolbar;
        [toolbar setNeedsLayout];
        [toolbar layoutIfNeeded];
    }
}

- (void)p_uninstallFromToolbar {
    self.animationToken += 1;
    self.imageToken += 1;
    self.builtIdentity = nil;
    UIViewController *viewController = [self p_hostViewController];
    UIBarButtonItem *currentItem = self.barButtonItem;
    self.barButtonItem = nil;
    if (!viewController) {
        return;
    }
    NSMutableArray<UIBarButtonItem *> *items = [NSMutableArray array];
    for (UIBarButtonItem *item in viewController.toolbarItems) {
        BOOL isOurs = (item == currentItem)
            || [item.accessibilityIdentifier isEqualToString:self.itemIdentifier];
        if (!isOurs) {
            [items addObject:item];
        }
    }
    [items sortUsingComparator:^NSComparisonResult(UIBarButtonItem *left, UIBarButtonItem *right) {
        if (left.tag < right.tag) {
            return NSOrderedAscending;
        }
        if (left.tag > right.tag) {
            return NSOrderedDescending;
        }
        return NSOrderedSame;
    }];
    KRUIToolbarSyncActionItemsChrome(items, viewController);
    [viewController setToolbarItems:items animated:NO];
    [KRUIToolbar scheduleApplyLeadingInsetForViewController:viewController];
}

- (void)p_onItem:(id)sender {
    if (self.css_click) {
        self.css_click(@{
            @"x": @0,
            @"y": @0,
            @"pageX": @0,
            @"pageY": @0
        });
    }
}

@end

#endif // [macOS]
