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

#import "KRScrollView.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "KRWrapperView.h"
#import "KRMultiDelegateProxy.h"
#import "KRConvertUtil.h"
#import "KRScrollViewOffsetAnimator.h"
#import "KRScrollView+NestedScroll.h"
#import "NSObject+KR.h"

/*
 * @brief 暴露给Kotlin侧调用的Scoller组件
 */
@interface KRScrollView()<UIScrollViewDelegate, KRScrollViewOffsetAnimatorDelegate>

/** attr is bouncesEnable  */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(bouncesEnable);
/** attr is pagingEnabled  */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(pagingEnabled);
/** attr is scrollEnabled  */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(scrollEnabled);
/** attr is showScrollerIndicator  */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(showScrollerIndicator);
/** attr is directionRow  */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(directionRow);
/** attr is css_dynamicSyncScrollDisable */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(dynamicSyncScrollDisable);
/** attr is minContentOffset */
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(limitHeaderBounces);
/** attr nestedScroll */
@property (nonatomic, strong) NSString *KUIKLY_PROP(nestedScroll);
/** event is scroll  */
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(scroll);
/** event is dragBegin  */
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(dragBegin);
/** event is dragEnd  */
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(dragEnd);
/** event is willDragEnd  */
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(willDragEnd);
/** event is scrollEnd  */
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(scrollEnd);


@end

@implementation KRScrollView {
    /** scrollEventCallback */
    KuiklyRenderCallback _scrollEventCallback;
    /** 松手时offsetY小于insetTop设置该contentInset for 下拉刷新组件 */
    UIEdgeInsets _contentInsetWhenEndDrag;
    /* wrapper self view*/
    __weak KRWrapperView *_wrapperView;
    /* 一对多代理转发 */
    KRMultiDelegateProxy *_delegateProxy;
    /** 松手时吸附位置 */
    CGPoint *_targetContentOffset;
    /** is first layout */
    BOOL _didLayout;
    /** 下次列表滚动动画结束回调 */
    dispatch_block_t _nextEndScrollingAnimationCallback;
    /**是否正在拖拽中，因系统isDragging不准，所以独立维护**/
    BOOL _isCurrentlyDragging;
    /** displaylink驱动的offset动画器 */
    KRScrollViewOffsetAnimator *_offsetAnimator;
    /**忽略分发ScrollEvent**/
    BOOL _ignoreDispatchScrollEvent;
}
@synthesize hr_rootView;
@synthesize lastContentOffset = _lastContentOffset;
@synthesize lContentOffset;
@synthesize activeInnerScrollView;
@synthesize activeOuterScrollView;
@synthesize nestedGestureDelegate;
@synthesize cascadeLockForNestedScroll;
@synthesize isLockedInNestedScroll;

#pragma mark - init

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame: frame]) {
        if (@available(iOS 13.0, *)) {
            self.automaticallyAdjustsScrollIndicatorInsets = NO;
        } else {
            // Fallback on earlier versions
        }
        if (@available(iOS 11.0, *)) {
            self.contentInsetAdjustmentBehavior = UIScrollViewContentInsetAdjustmentNever;
        } else {
            // Fallback on earlier versions
        }
        self.alwaysBounceVertical = YES;
        _delegateProxy = [KRMultiDelegateProxy alloc];
        [_delegateProxy addDelegate:self];
        self.delegate = (id<UIScrollViewDelegate>)_delegateProxy;
        self.delaysContentTouches = NO;
    }
    return self;
    
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:@"contentOffset"]) {
        [self css_contentOffsetWithParams:params];
    } else if ([method isEqualToString:@"contentInset"]) {
        [self css_contentInsetWithParams:params];
    } else if ([method isEqualToString:@"contentInsetWhenEndDrag"]) {
        [self css_contentInsetWhenEndDragWithParams:params];
    }
}

#pragma mark - pubilc

 
/*
 * 添加滚动监听
 */
- (void)addScrollViewDelegate:(id<UIScrollViewDelegate>)scrollViewDelegate {
    [_delegateProxy addDelegate:scrollViewDelegate];
}
/*
 * 删除滚动监听
 */
- (void)removeScrollViewDelegate:(id<UIScrollViewDelegate>)scrollViewDelegate {
    [_delegateProxy removeDelegate:scrollViewDelegate];
}

#pragma mark - override

- (void)layoutSubviews {
    [super layoutSubviews];
    if (!_didLayout && [self.hr_rootView isKindOfClass:[KuiklyRenderView class]]) {
        _didLayout = YES;
        KuiklyRenderView *renderView = (KuiklyRenderView*)self.hr_rootView;
        if ([renderView.delegate respondsToSelector:@selector(scrollViewDidLayout:renderView:)]) {
            [renderView.delegate scrollViewDidLayout:self renderView:renderView];
        }
    }
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview && self.superview != _wrapperView) {
        [_wrapperView moveToSuperview:self.superview];
    }
}

- (void)insertSubview:(UIView *)view atIndex:(NSInteger)index {
    [super insertSubview:view atIndex:index];
}

- (void)removeFromSuperview {
    [super removeFromSuperview];
    if (_wrapperView.superview) {
        [_wrapperView removeFromSuperview];
    }
}

- (void)setContentOffset:(CGPoint)contentOffset {
    if (self.autoAdjustContentOffsetDisable) {
        return ;
    }
    if ([_css_limitHeaderBounces boolValue]) { // 禁止顶部回弹
        if ([_css_directionRow boolValue]) {
            contentOffset = CGPointMake(MAX(contentOffset.x, 0), contentOffset.y);
        } else {
            contentOffset = CGPointMake(contentOffset.x, MAX(contentOffset.y, 0));
        }
    }
    [super setContentOffset:contentOffset];
    [self p_dispatchScrollEventIfNeed];
}

- (void)setUserInteractionEnabled:(BOOL)userInteractionEnabled {
    [super setUserInteractionEnabled:userInteractionEnabled];
    [_wrapperView setUserInteractionEnabled:userInteractionEnabled];
}

- (BOOL)touchesShouldCancelInContentView:(UIView *)view {
    BOOL cancel = [super touchesShouldCancelInContentView:view];
    if ([view isKindOfClass:[UIControl class]] || view.kr_canCancelInScrollView) {
        return YES;
    }
    return cancel;
}


#pragma mark - UIScrollViewDelegate

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView {
    _isCurrentlyDragging = YES;
    if (_css_dragBegin) {
       _css_dragBegin([self p_generateEventBaseParams]);
    }
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
    _isCurrentlyDragging = NO;
    if (!decelerate) { // 滑动结束
        if (_css_scrollEnd) {
            _css_scrollEnd([self p_generateEventBaseParams]);
        }
    }
    if (_css_dragEnd) {
        _css_dragEnd([self p_generateEventBaseParams]);
    }
    if (!UIEdgeInsetsEqualToEdgeInsets(_contentInsetWhenEndDrag, UIEdgeInsetsZero)
        && scrollView.contentOffset.y < -_contentInsetWhenEndDrag.top
        ) {
        UIEdgeInsets insets = _contentInsetWhenEndDrag;
        self.contentInset = insets;
    }
    
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView {
    if (_css_scrollEnd) {
        _css_scrollEnd([self p_generateEventBaseParams]);
    }
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    // need imp this method
}

- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView {
    if (_css_scrollEnd) {
        _css_scrollEnd([self p_generateEventBaseParams]);
    }
    if (_nextEndScrollingAnimationCallback) {
        _nextEndScrollingAnimationCallback();
        _nextEndScrollingAnimationCallback = nil;
    }
}

- (void)scrollViewWillEndDragging:(UIScrollView *)scrollView withVelocity:(CGPoint)velocity targetContentOffset:(inout CGPoint *)targetContentOffset {
    if (_css_willDragEnd) {
        _targetContentOffset = targetContentOffset;
        NSMutableDictionary *params = [[self p_generateEventBaseParams] mutableCopy];
        params[@"velocityX"] = @(velocity.x);
        params[@"velocityY"] = @(velocity.y);
        params[@"targetContentOffsetX"] = @((*targetContentOffset).x);
        params[@"targetContentOffsetY"] = @((*targetContentOffset).y);
        _css_willDragEnd(params); /// setContentOffset ()
        _targetContentOffset = nil;
    }
}


#pragma mark - css method

- (void)css_contentOffsetWithParams:(NSString *)params {
    NSArray<NSString *> *points = [params componentsSeparatedByString:@" "];
    BOOL animated = [points count] > 2 ? [points[2] boolValue] : NO;
    CGFloat duration = [points count] > 3 ? [points[3] floatValue] : 0;
    CGFloat damping = [points count] > 4 ? [points[4] floatValue] : 0;
    CGFloat velocity = [points count] > 5 ? [points[5] floatValue] : 0;
    CGPoint contentOffset = CGPointMake([points.firstObject doubleValue], [points[1] doubleValue]);
    [self p_setTargetContentOffsetIfNeed:contentOffset];
    if (damping) {
        [self p_springAnimationWithContentOffset:contentOffset duration:duration damping:damping velocity:velocity];
        return ;
    }
    UIEdgeInsets newContentInsets = [self maxEdgeInsetsWithContentOffset:contentOffset];
    if (!UIEdgeInsetsEqualToEdgeInsets(self.contentInset, newContentInsets)) {
        self.contentInset = newContentInsets;
    }
    [self setContentOffset:contentOffset animated:animated];
}

- (void)css_contentInsetWithParams:(NSString *)params {
    NSArray<NSString *> *points = [params componentsSeparatedByString:@" "];
    BOOL animated = [points count] > 4 ? [points[4] boolValue] : NO;
    UIEdgeInsets contentInset = UIEdgeInsetsMake([points[0] doubleValue], [points[1] doubleValue], [points[2] doubleValue], [points[3] doubleValue]);
    if (animated) {
        CGPoint maxContentOffset = [self p_maxContentOffsetInContentInset:contentInset];
        if (!CGPointEqualToPoint(self.contentOffset, maxContentOffset)) {
            [self setContentOffset:maxContentOffset animated:YES];
            __weak typeof(self) weakSelf = self;
            _nextEndScrollingAnimationCallback = ^{
                weakSelf.contentInset = contentInset;
            };
        } else {
            self.contentInset = contentInset;
        }
    } else {
        self.autoAdjustContentOffsetDisable = YES;
        self.contentInset = contentInset;
        self.autoAdjustContentOffsetDisable = NO;
    }
}
    

- (void)css_contentInsetWhenEndDragWithParams:(NSString *)params {
    NSArray<NSString *> *points = [params componentsSeparatedByString:@" "];
    UIEdgeInsets contentInset = UIEdgeInsetsMake([points[0] doubleValue], [points[1] doubleValue], [points[2] doubleValue], [points[3] doubleValue]);
    _contentInsetWhenEndDrag = contentInset;
}


#pragma mark - setter (css property)

- (void)setCss_bouncesEnable:(NSNumber *)css_bouncesEnable {
    if (self.css_bouncesEnable != css_bouncesEnable) {
        _css_bouncesEnable = css_bouncesEnable;
        self.bounces = _css_bouncesEnable ? [css_bouncesEnable boolValue] : YES;
    }
}

- (void)setCss_pagingEnabled:(NSNumber *)css_pagingEnabled {
    if (self.css_pagingEnabled != css_pagingEnabled) {
        _css_pagingEnabled = css_pagingEnabled;
        self.pagingEnabled = [css_pagingEnabled boolValue];
    }
}

- (void)setCss_scrollEnabled:(NSNumber *)css_scrollEnabled {
    if (self.css_scrollEnabled != css_scrollEnabled) {
        _css_scrollEnabled = css_scrollEnabled;
        self.scrollEnabled = [css_scrollEnabled boolValue];
    }
}


- (void)setCss_showScrollerIndicator:(NSNumber *)css_showScrollerIndicator {
    if (self.css_showScrollerIndicator != css_showScrollerIndicator) {
        _css_showScrollerIndicator = css_showScrollerIndicator;
        self.showsVerticalScrollIndicator = [css_showScrollerIndicator boolValue];
        self.showsHorizontalScrollIndicator = [css_showScrollerIndicator boolValue];
    }
}

- (void)setCss_directionRow:(NSNumber *)css_directionRow {
    if (self.css_directionRow != css_directionRow) {
        _css_directionRow = css_directionRow;
        self.alwaysBounceHorizontal = [_css_directionRow boolValue];
        self.alwaysBounceVertical = !self.alwaysBounceHorizontal;
    }
}

- (void)parseScrollMode:(NSString *)modeStr forward:(BOOL)isForward {
    NestedScrollPriority pri = NestedScrollPriorityUndefined;
    if ([modeStr isEqualToString:@"SELF_ONLY"]) {
        pri = NestedScrollPrioritySelfOnly;
    } else if ([modeStr isEqualToString:@"SELF_FIRST"]) {
        pri = NestedScrollPrioritySelf;
    } else if ([modeStr isEqualToString:@"PARENT_FIRST"]) {
        pri = NestedScrollPriorityParent;
    }
    // 垂直
    if (isForward && ![self horizontal]) {
        [self setNestedScrollTopPriority:pri];
    } else if (isForward && [self horizontal]) {
        [self setNestedScrollLeftPriority:pri];
    } else if (!isForward && ![self horizontal]) {
        [self setNestedScrollBottomPriority:pri];
    } else if (!isForward && [self horizontal]) {
        [self setNestedScrollRightPriority:pri];
    }
}

- (void)setCss_nestedScroll:(NSString *)css_nestedScroll {
    if (![self.css_nestedScroll isEqualToString:css_nestedScroll]) {
        _css_nestedScroll = css_nestedScroll;
        NSDictionary *dic = [css_nestedScroll kr_stringToDictionary];
        NSString *forwardStr = [dic objectForKey:@"forward"];
        NSString *backwardStr = [dic objectForKey:@"backward"];
        [self parseScrollMode:forwardStr forward:YES];
        [self parseScrollMode:backwardStr forward:NO];
    }
}

- (void)setCss_dynamicSyncScrollDisable:(NSNumber *)css_dynamicSyncScrollDisable {
    if (self.css_dynamicSyncScrollDisable != css_dynamicSyncScrollDisable) {
        _css_dynamicSyncScrollDisable = css_dynamicSyncScrollDisable;
    }
}

- (void)setCss_frame:(NSValue *)css_frame {
    [super setCss_frame:css_frame];
    _wrapperView.frame = self.frame;
}


- (NSString *)css_borderRadius {
    if (_wrapperView.css_borderRadius) {
        return _wrapperView.css_borderRadius;
    }
    return [super css_borderRadius];
}

- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    if (_wrapperView) { // 垫一层wrapperview来设置圆角，避免scrollView的layer.mask过裁内容
        _wrapperView.css_borderRadius = css_borderRadius;
    } else {
        [super setCss_borderRadius:css_borderRadius];
        if (self.layer.mask) {
            [super setCss_borderRadius:nil];
            [self p_generateWrapperViewIfNeed];
            _wrapperView.css_borderRadius = css_borderRadius;
        }
    }
}

#pragma mark - KRScrollViewOffsetAnimatorDelegate


- (void)animateContentOffsetDidChanged:(CGPoint)contentOffset {
    [self dispatchScrollEventWithCurOffset:contentOffset];
}

#pragma mark - private
/// 是否有足够多的可见内容视图
- (BOOL)p_hasEnoughVisibleContentViews {
    UIView *contentView = self.subviews.firstObject;
    if (!contentView
        || MAX(CGRectGetHeight(contentView.frame), CGRectGetWidth(contentView.frame))
        <=  MAX(CGRectGetHeight(self.frame), CGRectGetWidth(self.frame))) {
        return YES;
    }
    CGPoint offset = self.contentOffset;
    BOOL hasTopViewInVisibleFrame = NO;
    BOOL hasBottomViewInVisibleFrame = NO;
    CGRect visibleFrame = CGRectMake(0, 0, CGRectGetWidth(self.frame), CGRectGetHeight(self.frame));
    for (UIView *subView in contentView.subviews) {
        CGRect subViewFrame = subView.frame;
        subViewFrame.origin.x -= offset.x;
        subViewFrame.origin.y -= offset.y;
        
        if (CGRectGetWidth(contentView.frame) < CGRectGetHeight(contentView.frame)) { // 纵向布局
            CGRect topAreaRect = CGRectMake(1, 1, CGRectGetWidth(visibleFrame) - 2, CGRectGetHeight(visibleFrame) * 0.3);
            CGRect bottomAreaRect = CGRectMake(1,
                                               CGRectGetHeight(visibleFrame) * 0.5 - 1,
                                               CGRectGetWidth(visibleFrame) - 2,
                                               CGRectGetHeight(visibleFrame) * 0.5);
            
            if (CGRectContainsRect(subViewFrame, topAreaRect) || CGRectContainsRect(topAreaRect, subViewFrame)
                || CGRectIntersectsRect(subViewFrame, topAreaRect)){
                hasTopViewInVisibleFrame = YES;
            }
            if (CGRectContainsRect(subViewFrame, bottomAreaRect) || CGRectContainsRect(bottomAreaRect, subViewFrame)
                || CGRectIntersectsRect(subViewFrame, bottomAreaRect)){
                hasBottomViewInVisibleFrame = YES;
            }
            if (hasTopViewInVisibleFrame && hasBottomViewInVisibleFrame) {
                return YES;
            }
        } else { // 横向布局
            return YES;
        }
    }
    
    return hasTopViewInVisibleFrame && hasBottomViewInVisibleFrame;
}

// 生成wrapper view
- (void)p_generateWrapperViewIfNeed {
    if (!_wrapperView) {
        KRWrapperView *wrapperView = [[KRWrapperView alloc] initWithHostView:self];
        _wrapperView = wrapperView;
        dispatch_async(dispatch_get_main_queue(), ^{
            [wrapperView description]; // strong one loop
        });
    }
}
// 分发scroll变化事件到kotlin
- (void)p_dispatchScrollEventIfNeed {
    if (self.isLockedInNestedScroll) {
        self.isLockedInNestedScroll = NO; // reset
        return;
    }
    
    if (_ignoreDispatchScrollEvent) {
        return ;
    }
    [self dispatchScrollEventWithCurOffset:self.contentOffset];
}

- (void)dispatchScrollEventWithCurOffset:(CGPoint)curOffset {
    if (!CGPointEqualToPoint(curOffset, _lastContentOffset)) {
        _lastContentOffset = curOffset;
        if (_css_scroll) {
            dispatch_block_t block = ^{
                BOOL syncCallback = NO;
                if (![self.css_dynamicSyncScrollDisable boolValue] && !self.setContentSizeing) {
                    syncCallback = ![self p_hasEnoughVisibleContentViews];
                }
                NSMutableDictionary *param = [[self p_generateEventBaseParams] mutableCopy];
                param[KR_SYNC_CALLBACK_KEY] = @(syncCallback ? 1 : 0); // 同步加载
                if (self.css_scroll) {
                    self.css_scroll(param);
                }
            };
            if (CGRectEqualToRect(self.frame, CGRectZero)) {
                // 首次setContentOffset->等自身frame在下一个runloop设置
                dispatch_async(dispatch_get_main_queue(), block);
            } else {
                block();
            }
           
        }
    }
}

// 在该contentInset下的列表最大可滚动偏移
- (CGPoint)p_maxContentOffsetInContentInset:(UIEdgeInsets)contentInset {
    CGFloat offsetTop = [_css_directionRow boolValue] ? self.contentOffset.x + contentInset.left : self.contentOffset.y + contentInset.top;
    CGFloat offsetBottom = [_css_directionRow boolValue]
        ? self.contentOffset.x + CGRectGetWidth(self.frame) - (self.contentSize.width + contentInset.right)
        : self.contentOffset.y + CGRectGetHeight(self.frame) - (self.contentSize.height + contentInset.bottom);
    if (offsetTop < 0) {
        if ([_css_directionRow boolValue]) {
            return CGPointMake(self.contentOffset.x - offsetTop, 0);
        } else {
            return CGPointMake(0, self.contentOffset.y - offsetTop);
        }
    } else if (offsetBottom > 0) {
        if ([_css_directionRow boolValue]) {
            return CGPointMake(self.contentOffset.x - offsetBottom, 0);
        } else {
            return CGPointMake(0, self.contentOffset.y - offsetBottom);
        }
    }
    return self.contentOffset;
}

- (UIEdgeInsets)maxEdgeInsetsWithContentOffset:(CGPoint)contentOffset {
    if ([_css_directionRow boolValue]) {
        if (contentOffset.x < -self.contentInset.left) {
            return UIEdgeInsetsMake(self.contentInset.top, -contentOffset.x, self.contentInset.bottom, self.contentInset.right);
        }
    } else {
        if (contentOffset.y < -self.contentInset.top) {
            return UIEdgeInsetsMake(-contentOffset.y, self.contentInset.left, self.contentInset.bottom, self.contentInset.right);
        }
    }
    return self.contentInset;
}


- (NSDictionary *)p_generateEventBaseParams {
    
    NSMutableArray *touchesParam = [NSMutableArray new];
    for (int i = 0; i < self.panGestureRecognizer.numberOfTouches; i++) {
        CGPoint pagePoint = [self.panGestureRecognizer locationOfTouch:i inView:self.hr_rootView];
        [touchesParam addObject:@{
            @"pageX" : @(pagePoint.x),
            @"pageY" : @(pagePoint.y)
        }];
    }
    
    return @{
        @"offsetX":@(_lastContentOffset.x),
        @"offsetY":@(_lastContentOffset.y),
        @"contentWidth": @(self.contentSize.width),
        @"contentHeight": @(self.contentSize.height),
        @"viewWidth": @(self.frame.size.width),
        @"viewHeight": @(self.frame.size.height),
        @"isDragging":@(_isCurrentlyDragging ? 1 : 0),
        @"touches": touchesParam,
    };
}

- (void)p_setTargetContentOffsetIfNeed:(CGPoint)contentOffset {
    if (_targetContentOffset) {
        *_targetContentOffset = contentOffset;
    }
}

- (void)p_springAnimationWithContentOffset:(CGPoint)contentOffset duration:(CGFloat)duration damping:(CGFloat)damping velocity:(CGFloat)velocity {
    [_offsetAnimator cancel];
    _offsetAnimator = [[KRScrollViewOffsetAnimator alloc] initWithScrollView:self delegate:self];
    [_offsetAnimator animateToOffset:contentOffset withVelocity:CGPointZero];
    KRScrollViewOffsetAnimator *animator = _offsetAnimator;
    _ignoreDispatchScrollEvent = YES;

    [UIView animateWithDuration:duration / 1000.0 delay:0
         usingSpringWithDamping:damping
          initialSpringVelocity:velocity
                        options:(UIViewAnimationOptionCurveEaseOut|UIViewAnimationOptionAllowUserInteraction)
                     animations:^{
            if (contentOffset.y < 0 || contentOffset.x < 0) {
               self.contentInset = UIEdgeInsetsMake(-contentOffset.y,  -contentOffset.x , 0, 0);
            }
            [self setContentOffset:contentOffset];
    } completion:^(BOOL finished) {
        [animator cancel];
    }];
    _ignoreDispatchScrollEvent = NO;
}

- (void)dealloc {
    [_offsetAnimator cancel];
}


@end


@interface KRScrollContentView ()
@property (nonatomic, weak) id<KRScrollContentViewDelegate> delegate;
@end

@implementation KRScrollContentView {
    /* 一对多代理转发 */
    KRMultiDelegateProxy *_delegateProxy;
}


- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _delegateProxy = [KRMultiDelegateProxy alloc];
        [_delegateProxy addDelegate:self];
        self.delegate = (id<KRScrollContentViewDelegate>)_delegateProxy;
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self syncScrollViewContentSize];
    
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    [self syncScrollViewContentSize];
}

- (void)syncScrollViewContentSize {
    if (self.superview) {
        KRScrollView *scrollView = (KRScrollView *)self.superview;
        if ([scrollView isKindOfClass:[KRScrollView class]]) {
            if (scrollView.isDragging) {
                scrollView.autoAdjustContentOffsetDisable = YES;
            }
            scrollView.setContentSizeing = YES;
            scrollView.contentSize = CGSizeMake(CGRectGetWidth(self.frame), CGRectGetHeight(self.frame));
            scrollView.setContentSizeing = NO;
            scrollView.autoAdjustContentOffsetDisable = NO;
        }
    }
}

#pragma mark - pubilc

 
/*
 * 添加滚动监听
 */
- (void)addScrollContentViewDelegate:(id<KRScrollContentViewDelegate>)scrollContentViewDelegate {
    [_delegateProxy addDelegate:scrollContentViewDelegate];
}
/*
 * 删除滚动监听
 */
- (void)removeScrollContentViewDelegate:(id<KRScrollContentViewDelegate>)scrollContentViewDelegate {
    [_delegateProxy removeDelegate:scrollContentViewDelegate];
}

#pragma mark - override

- (BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *)event {
    BOOL result = [super pointInside:point withEvent:event];
    KRScrollView *scrollView = (KRScrollView *)self.superview;
    if ([scrollView isKindOfClass:[KRScrollView class]]) {
        UIEdgeInsets insets = scrollView.contentInset;
        result = CGRectContainsPoint([KRConvertUtil hr_rectInset:self.bounds insets:insets], point);
    }
    return result;
}

- (void)insertSubview:(UIView *)view atIndex:(NSInteger)index {
    [super insertSubview:view atIndex:index];
    if ([self.delegate respondsToSelector:@selector(contentViewDidInsertSubview)]) {
        [self.delegate contentViewDidInsertSubview];
    }
}

@end




