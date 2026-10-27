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

#import "KRUIToolbarItemContentHost.h"
#import "UIView+CSS.h"
#import "KRUIBarButtonItemKit.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import <objc/runtime.h>

#if !TARGET_OS_OSX // [macOS]

static const CGFloat kKRToolbarItemContentMinWidth = 28.0;
static const CGFloat kKRToolbarItemContentMinHeight = 36.0;

@interface KRUIToolbarItemContentHost ()
@property (nonatomic, strong) UIView *contentView;
@property (nonatomic, strong) UIView *itemContainer;
@property (nonatomic, copy) NSString *css_title;
@property (nonatomic, copy) NSString *css_index;
@property (nonatomic, copy) NSString *css_axis;
@property (nonatomic, strong) UIBarButtonItem *barButtonItem;
@property (nonatomic, copy) NSString *itemIdentifier;
@property (nonatomic, assign) BOOL syncingAttachment;
@end

@implementation KRUIToolbarItemContentHost

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
        _css_title = @"";
        _css_index = @"0";
        _css_axis = @"automatic";
        _itemIdentifier = [NSString stringWithFormat:@"kr_toolbar_content_%p", self];
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

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    UIView *view = subView.kr_commonWrapperView ?: subView;
    if (index < 0 || index >= (NSInteger)_contentView.subviews.count) {
        [_contentView addSubview:view];
    } else {
        [_contentView insertSubview:view atIndex:index];
    }
    [self p_syncToToolbar];
}

#pragma mark - css

- (void)setCss_title:(NSString *)css_title {
    _css_title = [css_title copy];
    if (self.barButtonItem) {
        self.barButtonItem.accessibilityLabel = _css_title.length ? _css_title : @"custom";
    }
}

- (void)setCss_index:(NSString *)css_index {
    _css_index = [css_index copy];
    [self p_syncToToolbar];
}

- (void)setCss_axis:(NSString *)css_axis {
    _css_axis = [css_axis copy];
    [self p_syncToToolbar];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_syncToToolbar];
}

#pragma mark - lifecycle

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    if (_contentView.superview == self) {
        _contentView.frame = self.bounds;
    }
    [self p_syncToToolbar];
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
        [self p_syncToToolbar];
    }
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

- (CGSize)p_effectiveSize {
    CGFloat width = CGRectGetWidth(self.bounds);
    CGFloat height = CGRectGetHeight(self.bounds);
    if (width < 1.0) {
        width = 44.0;
    }
    if (height < 1.0) {
        height = 44.0;
    }
    width = MAX(width, kKRToolbarItemContentMinWidth);
    height = MAX(height, kKRToolbarItemContentMinHeight);
    return CGSizeMake(width, height);
}

- (void)p_forwardHostClickToContainer {
    if (!self.itemContainer) {
        return;
    }
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
    item.tag = [self p_itemIndex];
    KRUIBarButtonItemApplyAxis(item, self.css_axis ?: @"automatic");
    KRUIBarButtonItemApplyGlassBackground(item, YES);
    return item;
}

- (BOOL)p_isInstalledOnToolbar {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || !self.barButtonItem) {
        return NO;
    }
    for (UIBarButtonItem *installedItem in viewController.toolbarItems ?: @[]) {
        if (installedItem == self.barButtonItem
            || [installedItem.accessibilityIdentifier isEqualToString:self.itemIdentifier]) {
            return YES;
        }
    }
    return NO;
}

- (BOOL)p_tryUpdateInstalledItem {
    if (!self.barButtonItem || ![self p_isInstalledOnToolbar]) {
        return NO;
    }
    [self p_attachContentToContainer];
    self.barButtonItem.accessibilityLabel = self.css_title.length ? self.css_title : @"custom";
    KRUIBarButtonItemApplyAxis(self.barButtonItem, self.css_axis ?: @"automatic");
    NSInteger index = [self p_itemIndex];
    if (self.barButtonItem.tag != index) {
        self.barButtonItem.tag = index;
        [self p_reorderInstalledItems];
        return YES;
    }
    if (self.barButtonItem.customView == self.itemContainer) {
        UIViewController *viewController = [self p_hostViewController];
        [self.barButtonItem.customView setNeedsLayout];
        UIToolbar *toolbar = viewController.navigationController.toolbar;
        [toolbar setNeedsLayout];
        [toolbar layoutIfNeeded];
        KRUIToolbarSyncActionItemsChrome(viewController.toolbarItems, viewController);
    }
    return YES;
}

- (void)p_syncToToolbar {
    if (self.syncingAttachment) {
        return;
    }
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || !self.window) {
        return;
    }
    if (![self p_shouldShow]) {
        if (self.barButtonItem) {
            [self p_uninstallFromToolbar];
        }
        return;
    }
    if ([self p_tryUpdateInstalledItem]) {
        return;
    }
    self.syncingAttachment = YES;
    UIBarButtonItem *item = [self p_makeCustomBarItem];
    self.barButtonItem = item;
    [self p_putItem:item];
    self.syncingAttachment = NO;
}

- (void)p_putItem:(UIBarButtonItem *)item {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
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
    if (!inserted) {
        UIToolbar *toolbar = viewController.navigationController.toolbar;
        [toolbar setNeedsLayout];
        [toolbar layoutIfNeeded];
    }
}

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
    [viewController setToolbarItems:items animated:NO];
    KRUIToolbarSyncActionItemsChrome(items, viewController);
}

- (void)p_uninstallFromToolbar {
    UIBarButtonItem *currentItem = self.barButtonItem;
    self.barButtonItem = nil;
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || !currentItem) {
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
