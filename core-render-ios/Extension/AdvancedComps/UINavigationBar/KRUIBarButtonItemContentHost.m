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

#import "KRUIBarButtonItemContentHost.h"
#import "UIView+CSS.h"
#import "KRComponentDefine.h"
#import "KRUINavigationBarItemPlacement.h"
#import "KRUINavigationBarKit.h"
#import "KRUIBarButtonItemKit.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"

#if !TARGET_OS_OSX // [macOS]
/// 与系统 `UIBarButtonItem` 可点区域对齐，避免 customView 偏小导致液态玻璃圆角偏心。
static const CGFloat kKRNavItemContentStandardSide = 44.0;

@interface KRUIBarButtonItemContentHost ()
@property (nonatomic, strong) UIView *contentView;
@property (nonatomic, strong) UIView *itemContainer;
@property (nonatomic, copy) NSString *css_placement;
@property (nonatomic, copy) NSString *css_title;
@property (nonatomic, copy) NSString *css_index;
@property (nonatomic, copy) NSString *css_itemGroup;
@property (nonatomic, copy) NSString *css_axis;
@property (nonatomic, copy) NSString *css_edgeInsetTop;
@property (nonatomic, strong) UIBarButtonItem *barButtonItem;
@property (nonatomic, copy) NSString *itemIdentifier;
@property (nonatomic, assign) BOOL syncingAttachment;
@property (nonatomic, assign) BOOL syncFlushScheduled;
@property (nonatomic, copy) NSString *appliedPlacement;
@property (nonatomic, assign) NSInteger appliedGroup;
@property (nonatomic, assign) NSInteger appliedIndex;
@property (nonatomic, assign) CGFloat appliedTopInset;
@property (nonatomic, assign) BOOL appliedToNavigationBar;
@end

@implementation KRUIBarButtonItemContentHost

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _contentView = [[UIView alloc] initWithFrame:CGRectZero];
        _contentView.backgroundColor = [UIColor clearColor];
        [self addSubview:_contentView];
        self.backgroundColor = [UIColor clearColor];
        self.clipsToBounds = NO;
        self.hidden = YES;
        self.alpha = 0;
        self.userInteractionEnabled = NO;
        _css_placement = @"trailing";
        _css_title = @"";
        _css_index = @"0";
        _css_itemGroup = @"0";
        _css_axis = @"automatic";
        _itemIdentifier = [NSString stringWithFormat:@"kr_nav_kit_content_%p", self];
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    if ([propKey isEqualToString:@"click"]) {
        if ([self css_setPropWithKey:propKey value:propValue]) {
            [self p_forwardHostClickToContainer];
        }
        return;
    }
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

- (void)hrv_prepareForeReuse {
    [self p_uninstallFromNavigationItem];
}

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    UIView *view = subView.kr_commonWrapperView ?: subView;
    if (index < 0 || index >= (NSInteger)_contentView.subviews.count) {
        [_contentView addSubview:view];
    } else {
        [_contentView insertSubview:view atIndex:index];
    }
    [self p_enqueueNavigationItemContentApply];
}

#pragma mark - css

- (void)setCss_placement:(NSString *)css_placement {
    BOOL changed = ![(_css_placement ?: @"") isEqualToString:(css_placement ?: @"")];
    if (changed && self.barButtonItem) {
        [self p_uninstallFromNavigationItem];
    }
    _css_placement = [css_placement copy];
    [self p_enqueueNavigationItemContentApply];
}

- (void)setCss_title:(NSString *)css_title {
    _css_title = [css_title copy];
    if (self.barButtonItem) {
        self.barButtonItem.accessibilityLabel = _css_title.length ? _css_title : @"custom";
    }
}

- (void)setCss_index:(NSString *)css_index {
    _css_index = [css_index copy];
    [self p_enqueueNavigationItemContentApply];
}

- (void)setCss_itemGroup:(NSString *)css_itemGroup {
    _css_itemGroup = [css_itemGroup copy];
    [self p_enqueueNavigationItemContentApply];
}

- (void)setCss_axis:(NSString *)css_axis {
    _css_axis = [css_axis copy];
    if (self.barButtonItem) {
        KRUIBarButtonItemApplyAxis(self.barButtonItem, _css_axis ?: @"automatic");
        return;
    }
    [self p_enqueueNavigationItemContentApply];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_enqueueNavigationItemContentApply];
}

- (void)setCss_edgeInsetTop:(NSString *)css_edgeInsetTop {
    _css_edgeInsetTop = [css_edgeInsetTop copy];
    [self p_enqueueNavigationItemContentApply];
}

#pragma mark - lifecycle

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    if (_contentView.superview == self) {
        _contentView.frame = self.bounds;
    }
    [self p_updateCustomViewLayoutIfAttached];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.hidden = YES;
    self.alpha = 0;
    self.userInteractionEnabled = NO;
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview) {
        [self p_enqueueNavigationItemContentApply];
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_enqueueNavigationItemContentApply];
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

- (BOOL)p_isLeading {
    return [KRUINavigationBarItemPlacement isLeadingPlacement:self.css_placement ?: @"trailing"];
}

- (CGFloat)p_resolvedTopInset {
    if ([self p_isLeading]) {
        return 0;
    }
    UIViewController *viewController = [self p_hostViewController];
    if (self.css_edgeInsetTop.length > 0) {
        if ([self.css_edgeInsetTop.lowercaseString isEqualToString:@"auto"]) {
            return KRUINavigationBarVerticalTrailingTopInset(viewController);
        }
        return MAX(0, self.css_edgeInsetTop.doubleValue);
    }
    return KRUINavigationBarVerticalTrailingTopInset(viewController);
}

- (CGSize)p_effectiveSize {
    CGFloat width = CGRectGetWidth(self.bounds);
    CGFloat height = CGRectGetHeight(self.bounds);
    if (width < 1.0) {
        width = kKRNavItemContentStandardSide;
    }
    if (height < 1.0) {
        height = kKRNavItemContentStandardSide;
    }
    // 导航栏竖直方向以较高的一边为准，避免扁矩形在玻璃条里偏上/偏下。
    CGFloat side = MAX(width, height);
    side = MAX(side, kKRNavItemContentStandardSide);
    return CGSizeMake(side, side);
}

- (void)p_forwardHostClickToContainer {
    if (!self.itemContainer) {
        return;
    }
    // Host 在 Kuikly 树里不可点；`event { click }` 挂在 host 上，需转到真正上栏的 customView。
    self.itemContainer.css_click = self.css_click;
}

- (void)p_attachContentToContainer {
    CGSize size = [self p_effectiveSize];
    if (!self.itemContainer) {
        self.itemContainer = [[UIView alloc] initWithFrame:CGRectMake(0, 0, size.width, size.height)];
        self.itemContainer.backgroundColor = [UIColor clearColor];
        self.itemContainer.userInteractionEnabled = YES;
    }
    self.itemContainer.frame = CGRectMake(0, 0, size.width, size.height);
    if (_contentView.superview != self.itemContainer) {
        [_contentView removeFromSuperview];
        _contentView.frame = self.itemContainer.bounds;
        _contentView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self.itemContainer addSubview:_contentView];
    } else {
        _contentView.frame = self.itemContainer.bounds;
    }
    [self p_forwardHostClickToContainer];
}

- (UIBarButtonItem *)p_makeCustomBarItem {
    [self p_attachContentToContainer];
    UIBarButtonItem *item = [[UIBarButtonItem alloc] initWithCustomView:self.itemContainer];
    item.accessibilityIdentifier = self.itemIdentifier;
    item.accessibilityLabel = self.css_title.length ? self.css_title : @"custom";
    KRUINavigationBarSetBarButtonItemSortIndex(item, [self p_itemIndex]);
    KRUIBarButtonItemApplyAxis(item, self.css_axis ?: @"automatic");
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        item.sharesBackground = YES;
        item.hidesSharedBackground = NO;
    }
#endif
    return item;
}

- (void)p_updateCustomViewLayoutIfAttached {
    if (!self.itemContainer) {
        return;
    }
    CGSize size = [self p_effectiveSize];
    self.itemContainer.frame = CGRectMake(0, 0, size.width, size.height);
    if (_contentView.superview == self.itemContainer) {
        _contentView.frame = self.itemContainer.bounds;
    }
    if (self.barButtonItem.customView == self.itemContainer) {
        [self.barButtonItem.customView setNeedsLayout];
    }
}

- (BOOL)p_structureMatchesAppliedState {
    if (!self.barButtonItem || !self.appliedToNavigationBar) {
        return NO;
    }
    NSString *placement = self.css_placement ?: @"trailing";
    if (![placement isEqualToString:(self.appliedPlacement ?: @"")]) {
        return NO;
    }
    if ([self p_itemGroup] != self.appliedGroup || [self p_itemIndex] != self.appliedIndex) {
        return NO;
    }
    if (fabs([self p_resolvedTopInset] - self.appliedTopInset) > 0.5) {
        return NO;
    }
    return YES;
}

- (void)p_recordAppliedInstallState {
    self.appliedPlacement = [self.css_placement copy] ?: @"trailing";
    self.appliedGroup = [self p_itemGroup];
    self.appliedIndex = [self p_itemIndex];
    self.appliedTopInset = [self p_resolvedTopInset];
    self.appliedToNavigationBar = YES;
}

- (void)p_clearAppliedInstallState {
    self.appliedPlacement = nil;
    self.appliedGroup = 0;
    self.appliedIndex = 0;
    self.appliedTopInset = 0;
    self.appliedToNavigationBar = NO;
}

- (BOOL)p_canApplyToHostedNavigationItem {
    return [self p_hostViewController] != nil && self.window != nil;
}

- (void)p_enqueueNavigationItemContentApply {
    if ([self p_canApplyToHostedNavigationItem]) {
        [self p_syncToNavigationItem];
        return;
    }
    __weak typeof(self) weakSelf = self;
    KRUINavigationBarScheduleCoalescedMainFlush(self, &_syncFlushScheduled, ^{
        [weakSelf p_syncToNavigationItem];
    });
}

- (void)p_syncToNavigationItem {
    if (self.syncingAttachment) {
        return;
    }
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || !self.window) {
        return;
    }
    if (![self p_shouldShow]) {
        if (self.barButtonItem) {
            [self p_uninstallFromNavigationItem];
        }
        return;
    }
    if ([self p_structureMatchesAppliedState]) {
        [self p_attachContentToContainer];
        [self p_updateCustomViewLayoutIfAttached];
        KRUIBarButtonItemApplyAxis(self.barButtonItem, self.css_axis ?: @"automatic");
        return;
    }
    self.syncingAttachment = YES;
    if (self.barButtonItem) {
        [self p_uninstallFromNavigationItem];
    }
    UIBarButtonItem *item = [self p_makeCustomBarItem];
    self.barButtonItem = item;
    [KRUINavigationBarItemPlacement installCustomViewItem:item
                                                 identifier:self.itemIdentifier
                                                  placement:self.css_placement ?: @"trailing"
                                                      group:[self p_itemGroup]
                                                      index:[self p_itemIndex]
                                             viewController:viewController
                                                   topInset:[self p_resolvedTopInset]];
    [self p_recordAppliedInstallState];
    self.syncingAttachment = NO;
}

- (void)p_uninstallFromNavigationItem {
    UIViewController *viewController = [self p_hostViewController];
    UIBarButtonItem *currentItem = self.barButtonItem;
    self.barButtonItem = nil;
    [self p_clearAppliedInstallState];
    if (!viewController || !currentItem) {
        return;
    }
    [KRUINavigationBarItemPlacement uninstallCustomViewItem:currentItem
                                                     identifier:self.itemIdentifier
                                                      placement:self.css_placement ?: @"trailing"
                                                 viewController:viewController
                                                       topInset:[self p_resolvedTopInset]];
    if (_contentView.superview == self.itemContainer) {
        [_contentView removeFromSuperview];
        _contentView.frame = self.bounds;
        _contentView.autoresizingMask = UIViewAutoresizingNone;
        [self addSubview:_contentView];
    }
    self.itemContainer = nil;
}

@end
#endif // [macOS]
