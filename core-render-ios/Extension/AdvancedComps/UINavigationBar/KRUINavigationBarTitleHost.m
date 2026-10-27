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

#import "KRUINavigationBarTitleHost.h"
#import "KRComponentDefine.h"
#import "KRUINavigationBarKit.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"

#if !TARGET_OS_OSX // [macOS]

static NSString *const kKRUINavigationBarTitleHostId = @"kuikly_ui_navigation_bar_title_host";

@interface KRUINavigationBarTitleHost ()
@property (nonatomic, strong) UIView *contentView;
@property (nonatomic, strong) UIView *titleContainer;
@property (nonatomic, assign) BOOL syncingAttachment;
@property (nonatomic, assign) BOOL syncFlushScheduled;
@end

@implementation KRUINavigationBarTitleHost

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _contentView = [[UIView alloc] initWithFrame:CGRectZero];
        _contentView.backgroundColor = [UIColor clearColor];
        [self addSubview:_contentView];
        self.backgroundColor = [UIColor clearColor];
        self.clipsToBounds = NO;
        self.accessibilityIdentifier = kKRUINavigationBarTitleHostId;
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
    [self p_detachTitleView];
}

- (void)css_syncTitleAttachment:(NSDictionary *)args {
    [self p_requestSyncTitleAttachment];
    if (self.titleContainer) {
        [self.titleContainer setNeedsLayout];
        [self.titleContainer layoutIfNeeded];
    }
}

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    UIView *view = subView.kr_commonWrapperView ?: subView;
    if (index < 0 || index >= (NSInteger)_contentView.subviews.count) {
        [_contentView addSubview:view];
    } else {
        [_contentView insertSubview:view atIndex:index];
    }
    [self p_requestSyncTitleAttachment];
}

#pragma mark - lifecycle

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    if (_contentView.superview == self) {
        _contentView.frame = self.bounds;
    }
    [self p_updateTitleContainerFrame];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    // 留在 Kuikly 树里占位，不参与页面布局；真正显示在 navigationItem.titleView。
    self.hidden = YES;
    self.alpha = 0;
    self.userInteractionEnabled = NO;
    // vif 拆掉 title 子树时不会走 insertSubview，需靠 layout 触发 detach。
    [self p_requestSyncTitleAttachment];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    [self p_requestSyncTitleAttachment];
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_requestSyncTitleAttachment];
        __weak typeof(self) weakSelf = self;
        [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
            [weakSelf p_detachTitleView];
        }];
    } else {
        [self p_detachTitleView];
    }
}

- (void)removeFromSuperview {
    [self p_detachTitleView];
    [super removeFromSuperview];
}

#pragma mark - private

- (UIViewController *)p_hostViewController {
    return self.kr_viewController ?: self.hr_rootView.kr_viewController;
}

- (CGSize)p_effectiveSize {
    CGFloat width = CGRectGetWidth(self.bounds);
    CGFloat height = CGRectGetHeight(self.bounds);
    if (width < 1.0) {
        width = 240.0;
    }
    if (height < 1.0) {
        height = 44.0;
    }
    return CGSizeMake(width, height);
}

- (void)p_updateTitleContainerFrame {
    if (!self.titleContainer) {
        return;
    }
    CGSize size = [self p_effectiveSize];
    self.titleContainer.frame = CGRectMake(0, 0, size.width, size.height);
    if (_contentView.superview == self.titleContainer) {
        _contentView.frame = self.titleContainer.bounds;
    }
}

- (void)p_requestSyncTitleAttachment {
    __weak typeof(self) weakSelf = self;
    KRUINavigationBarScheduleCoalescedMainFlush(self, &_syncFlushScheduled, ^{
        [weakSelf p_syncTitleAttachment];
    });
}

- (BOOL)p_hasAttachedTitleContent {
    return _contentView.subviews.count > 0;
}

- (void)p_syncTitleAttachment {
    if (self.syncingAttachment) {
        return;
    }
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || !self.window) {
        return;
    }
    if (![self p_hasAttachedTitleContent]) {
        [self p_detachTitleView];
        return;
    }
    self.syncingAttachment = YES;
    CGSize size = [self p_effectiveSize];
    if (!self.titleContainer) {
        self.titleContainer = [[UIView alloc] initWithFrame:CGRectMake(0, 0, size.width, size.height)];
        self.titleContainer.accessibilityIdentifier = kKRUINavigationBarTitleHostId;
        self.titleContainer.backgroundColor = [UIColor clearColor];
        self.titleContainer.userInteractionEnabled = YES;
    }
    self.titleContainer.frame = CGRectMake(0, 0, size.width, size.height);
    // 只把 contentView 挂到 titleView，host 留在 Kuikly 树里，避免和渲染层抢 superview。
    if (_contentView.superview != self.titleContainer) {
        [_contentView removeFromSuperview];
        _contentView.frame = self.titleContainer.bounds;
        _contentView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        [self.titleContainer addSubview:_contentView];
    } else {
        _contentView.frame = self.titleContainer.bounds;
    }
    UINavigationItem *item = viewController.navigationItem;
    if (item.titleView != self.titleContainer) {
        item.titleView = self.titleContainer;
    }
    KRUINavigationBarItemSetUsesCustomTitle(item, YES);
    item.title = @"";
    viewController.title = @"";
    self.syncingAttachment = NO;
}

- (void)p_detachTitleView {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    UINavigationItem *item = viewController.navigationItem;
    if (item.titleView == self.titleContainer) {
        item.titleView = nil;
    }
    KRUINavigationBarItemSetUsesCustomTitle(item, NO);
    // 字符串标题由 KRUINavigationBar 的 css_title 驱动；vif 切到 title.text 时由 Kotlin 槽位写入，此处不再 refresh，避免用空 css_title 盖掉后续 apply。
    if (_contentView.superview == self.titleContainer) {
        [_contentView removeFromSuperview];
        _contentView.frame = self.bounds;
        _contentView.autoresizingMask = UIViewAutoresizingNone;
        [self addSubview:_contentView];
    }
    self.titleContainer = nil;
}

@end

#endif // [macOS]
