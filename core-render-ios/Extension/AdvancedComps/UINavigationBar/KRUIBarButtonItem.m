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

#import "KRUIBarButtonItem.h"
#import "KRComponentDefine.h"
#import "KRUIBarImage.h"
#import "KRUINavigationBarItemPlacement.h"
#import "KRUINavigationBarKit.h"
#import "KRUIBarButtonItemKit.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"

#if !TARGET_OS_OSX // [macOS]
@interface KRUIBarButtonItem ()
@property (nonatomic, copy) NSString *css_placement;
@property (nonatomic, copy) NSString *css_symbol;
@property (nonatomic, copy) NSString *css_src;
@property (nonatomic, strong) id css_template;
@property (nonatomic, copy) NSString *css_title;
@property (nonatomic, copy) NSString *css_index;
@property (nonatomic, copy) NSString *css_itemGroup;
@property (nonatomic, copy) NSString *css_axis;
@property (nonatomic, strong) id css_tintColor;
@property (nonatomic, strong) UIBarButtonItem *barButtonItem;
@property (nonatomic, copy) NSString *itemIdentifier;
@property (nonatomic, assign) NSInteger animationToken;
@property (nonatomic, assign) NSInteger imageToken;
@property (nonatomic, assign) BOOL syncFlushScheduled;
@property (nonatomic, copy) NSString *appliedPlacement;
@property (nonatomic, assign) NSInteger appliedGroup;
@property (nonatomic, assign) NSInteger appliedIndex;
@property (nonatomic, copy) NSString *appliedImageKey;
@property (nonatomic, assign) BOOL installedOnNavigationBar;
@end

@implementation KRUIBarButtonItem

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.hidden = YES;
        self.alpha = 0;
        self.userInteractionEnabled = NO;
        self.backgroundColor = [UIColor clearColor];
        _css_placement = @"trailing";
        _css_symbol = @"ellipsis.circle";
        _css_src = @"";
        _css_template = @"true";
        _css_title = @"";
        _css_index = @"0";
        _css_itemGroup = @"0";
        _css_axis = @"automatic";
        _itemIdentifier = [NSString stringWithFormat:@"kr_nav_kit_%p", self];
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

- (void)hrv_prepareForeReuse {
    [self p_uninstallFromNavigationItem];
}

- (void)setCss_placement:(NSString *)css_placement {
    BOOL placementChanged = ![(_css_placement ?: @"") isEqualToString:(css_placement ?: @"")];
    if (placementChanged && self.barButtonItem) {
        [self p_uninstallFromNavigationItem];
    }
    _css_placement = [css_placement copy];
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_symbol:(NSString *)css_symbol {
    _css_symbol = [css_symbol copy];
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_src:(NSString *)css_src {
    _css_src = [css_src copy];
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_template:(id)css_template {
    _css_template = css_template;
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_title:(NSString *)css_title {
    _css_title = [css_title copy];
    if (self.barButtonItem) {
        NSString *symbol = self.css_symbol.length ? self.css_symbol : @"ellipsis.circle";
        self.barButtonItem.accessibilityLabel = _css_title.length ? _css_title : symbol;
        return;
    }
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_index:(NSString *)css_index {
    _css_index = [css_index copy];
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_itemGroup:(NSString *)css_itemGroup {
    _css_itemGroup = [css_itemGroup copy];
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_axis:(NSString *)css_axis {
    _css_axis = [css_axis copy];
    if (self.barButtonItem) {
        KRUIBarButtonItemApplyAxis(self.barButtonItem, _css_axis);
        return;
    }
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_tintColor:(id)css_tintColor {
    _css_tintColor = css_tintColor;
    if (self.barButtonItem && [self p_shouldShow]) {
        UIColor *tint = css_tintColor ? [UIView css_color:css_tintColor] : nil;
        if (tint) {
            self.barButtonItem.tintColor = tint;
            return;
        }
    }
    [self p_enqueueNavigationItemApply];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_enqueueNavigationItemApply];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview) {
        [self p_enqueueNavigationItemApply];
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
        [self p_enqueueNavigationItemApply];
        __weak typeof(self) weakSelf = self;
        [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
            [weakSelf p_uninstallFromNavigationItem];
        }];
    } else {
        [self p_uninstallFromNavigationItem];
    }
}

- (void)removeFromSuperview {
    [self p_uninstallFromNavigationItem];
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

- (NSInteger)p_itemGroup {
    return self.css_itemGroup.length ? self.css_itemGroup.integerValue : 0;
}

- (BOOL)p_useTemplate {
    if (!self.css_template) {
        return YES;
    }
    return [UIView css_bool:self.css_template];
}

- (NSString *)p_imageResolveKey {
    NSString *symbol = self.css_symbol.length ? self.css_symbol : @"ellipsis.circle";
    NSString *template = self.css_template ? [NSString stringWithFormat:@"%@", self.css_template] : @"true";
    return [NSString stringWithFormat:@"src=%@|sym=%@|tpl=%@", self.css_src ?: @"", symbol, template];
}

- (BOOL)p_structureMatchesAppliedState {
    if (!self.barButtonItem || !self.installedOnNavigationBar) {
        return NO;
    }
    NSString *placement = self.css_placement ?: @"trailing";
    if (![placement isEqualToString:(self.appliedPlacement ?: @"")]) {
        return NO;
    }
    if ([self p_itemGroup] != self.appliedGroup || [self p_itemIndex] != self.appliedIndex) {
        return NO;
    }
    return YES;
}

- (void)p_recordAppliedInstallState {
    self.appliedPlacement = [self.css_placement copy] ?: @"trailing";
    self.appliedGroup = [self p_itemGroup];
    self.appliedIndex = [self p_itemIndex];
    self.appliedImageKey = [self p_imageResolveKey];
    self.installedOnNavigationBar = YES;
}

- (void)p_clearAppliedInstallState {
    self.appliedPlacement = nil;
    self.appliedGroup = 0;
    self.appliedIndex = 0;
    self.appliedImageKey = nil;
    self.installedOnNavigationBar = NO;
}

- (UIImage *)p_configuredImageFromResolved:(UIImage *)image {
    NSString *symbol = self.css_symbol.length ? self.css_symbol : @"ellipsis.circle";
    UIImage *configured = image;
    if (!configured) {
        if (@available(iOS 13.0, *)) {
            UIImage *fallback = [UIImage systemImageNamed:symbol];
            UIImageSymbolConfiguration *config =
                [UIImageSymbolConfiguration configurationWithPointSize:17
                                                                weight:(UIImageSymbolWeight)KRUIBarImageSymbolFontWeightRegular];
            configured = [fallback imageWithConfiguration:config] ?: fallback;
        }
    }
    return configured;
}

- (void)p_applyResolvedImage:(UIImage *)image toBarButtonItem:(UIBarButtonItem *)item {
    if (!item) {
        return;
    }
    item.image = [self p_configuredImageFromResolved:image];
    NSString *symbol = self.css_symbol.length ? self.css_symbol : @"ellipsis.circle";
    item.accessibilityLabel = self.css_title.length ? self.css_title : symbol;
    UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
    if (tint) {
        item.tintColor = tint;
    }
    KRUIBarButtonItemApplyAxis(item, self.css_axis);
    self.appliedImageKey = [self p_imageResolveKey];
}

- (UIBarButtonItem *)p_makeItemWithImage:(UIImage *)image {
    NSString *symbol = self.css_symbol.length ? self.css_symbol : @"ellipsis.circle";
    UIImage *configured = [self p_configuredImageFromResolved:image];
    UIBarButtonItem *item = [[UIBarButtonItem alloc] initWithImage:configured
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
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        item.sharesBackground = YES;
        item.hidesSharedBackground = NO;
    }
#endif
    KRUIBarButtonItemApplyAxis(item, self.css_axis);
    item.accessibilityIdentifier = self.itemIdentifier;
    item.accessibilityLabel = self.css_title.length ? self.css_title : symbol;
    KRUINavigationBarSetBarButtonItemSortIndex(item, [self p_itemIndex]);
    return item;
}

- (void)p_resolveImageWithCompletion:(void (^)(UIImage *image))completion {
    NSInteger token = ++self.imageToken;
    __weak typeof(self) weakSelf = self;
    [KRUIBarImage resolveSrc:self.css_src
                          symbol:self.css_symbol.length ? self.css_symbol : @"ellipsis.circle"
                  symbolFontWeight:KRUIBarImageSymbolFontWeightRegular
                       useTemplate:[self p_useTemplate]
                          rootView:self.hr_rootView
                        completion:^(UIImage *image) {
        __strong typeof(weakSelf) self = weakSelf;
        if (!self || token != self.imageToken || ![self p_shouldShow]) {
            return;
        }
        if (completion) {
            completion(image);
        }
    }];
}

- (void)p_reloadItem {
    [self p_resolveImageWithCompletion:^(UIImage *image) {
        [self p_putItem:[self p_makeItemWithImage:image]];
    }];
}

- (void)p_refreshInstalledItemImageIfNeeded {
    NSString *imageKey = [self p_imageResolveKey];
    if ([imageKey isEqualToString:self.appliedImageKey]) {
        return;
    }
    [self p_resolveImageWithCompletion:^(UIImage *image) {
        if (!self.barButtonItem || ![self p_structureMatchesAppliedState]) {
            return;
        }
        [self p_applyResolvedImage:image toBarButtonItem:self.barButtonItem];
    }];
}

- (BOOL)p_canApplyToHostedNavigationItem {
    return [self p_hostViewController] != nil && self.window != nil;
}

- (void)p_enqueueNavigationItemApply {
    if ([self p_canApplyToHostedNavigationItem]) {
        [self p_applyNavigationItemState];
        return;
    }
    __weak typeof(self) weakSelf = self;
    KRUINavigationBarScheduleCoalescedMainFlush(self, &_syncFlushScheduled, ^{
        [weakSelf p_applyNavigationItemState];
    });
}

- (void)p_applyNavigationItemState {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    BOOL show = [self p_shouldShow];
    if (!show) {
        self.imageToken += 1;
        if (self.barButtonItem) {
            [self p_uninstallFromNavigationItem];
        }
        return;
    }
    if (self.barButtonItem && [self p_structureMatchesAppliedState]) {
        [self p_refreshInstalledItemImageIfNeeded];
        return;
    }
    if (self.barButtonItem) {
        [self p_uninstallFromNavigationItem];
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
    [KRUINavigationBarItemPlacement installItem:item
                                       identifier:self.itemIdentifier
                                        placement:self.css_placement ?: @"trailing"
                                            group:[self p_itemGroup]
                                            index:[self p_itemIndex]
                                   viewController:viewController];
    [self p_recordAppliedInstallState];
}

- (void)p_uninstallFromNavigationItem {
    self.animationToken += 1;
    self.imageToken += 1;
    UIViewController *viewController = [self p_hostViewController];
    UIBarButtonItem *currentItem = self.barButtonItem;
    self.barButtonItem = nil;
    [self p_clearAppliedInstallState];
    if (!viewController || !currentItem) {
        return;
    }
    [KRUINavigationBarItemPlacement uninstallItem:currentItem
                                         identifier:self.itemIdentifier
                                          placement:self.css_placement ?: @"trailing"
                                     viewController:viewController];
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
