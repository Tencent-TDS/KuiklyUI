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

#import "KuiklyRenderView.h"
#import "KuiklyRenderCore.h"
#import "KRConvertUtil.h"

#if !TARGET_OS_OSX
#if defined(__IPHONE_OS_VERSION_MAX_ALLOWED) && __IPHONE_OS_VERSION_MAX_ALLOWED >= 270100
#if __has_include(<UIKit/UIHingeInteraction.h>)
#import <UIKit/UIHingeInteraction.h>
#define KR_HAS_UIHINGE 1
#endif
#if __has_include(<UIKit/UIViewReservedRegion.h>)
#import <UIKit/UIViewReservedRegion.h>
#define KR_HAS_RESERVED_REGION 1
#endif
#endif
#endif

/** RootView尺寸变化事件名. */
NSString *const KRRootViewSizeDidChangedEventKey = @"rootViewSizeDidChanged";
/** 字典key常量 */
NSString *const KRRootViewWidthKey = @"rootViewWidth";
NSString *const KRRootViewHeightKey = @"rootViewHeight";
NSString *const KRUrlKey = @"url";
NSString *const KRStatusBarHeightKey = @"statusBarHeight";
NSString *const KRPlatformKey = @"platform";
NSString *const KRDeviceWidthKey = @"deviceWidth";
NSString *const KRDeviceHeightKey = @"deviceHeight";
NSString *const KRActivityWidthKey = @"activityWidth";
NSString *const KRActivityHeightKey = @"activityHeight";
NSString *const KROsVersionKey = @"osVersion";
NSString *const KRAppVersionKey = @"appVersion";
NSString *const KRParamKey = @"param";
NSString *const KRWidthKey = @"width";
NSString *const KRHeightKey = @"height";
NSString *const KRNativeBuild = @"nativeBuild";
NSString *const KRSafeAreaInsets = @"safeAreaInsets";
NSString *const KRAccessibilityRunning = @"isAccessibilityRunning";
NSString *const KRDensity = @"density";
NSString *const KRHingeStatusKey = @"hingeStatus";
NSString *const KRReservedRegionsKey = @"reservedRegions";

@interface KuiklyRenderView()<KuiklyRenderCoreDelegate>
/** 渲染核心实现者对象 */
@property (nonatomic, strong) KuiklyRenderCore *renderCore;
/** 页面注册名 */
@property (nonatomic, strong) NSString *pageName;
/** 上次自身view尺寸 */
@property (nonatomic, assign) CGSize lastViewSize;
@property (nonatomic, copy) NSString *lastSafeAreaInsets;
/** 上次已发送的 region JSON，layoutSubviews 时与最新读值比对 */
@property (nonatomic, copy) NSString *lastPushedRegionsJSON;
@property (nonatomic, assign) NSInteger lastNotifiedHingeStatus;
@property (nonatomic, strong) id hingeInteraction;
/** 内容视图是否完成加载过 */
@property (nonatomic, assign, getter=isContentViewDidLoad) BOOL contentViewDidLoad;
/** delegate for KuiklyRenderView. */
@property (nonatomic, weak, readwrite) id<KuiklyRenderViewDelegate> delegate;

- (void)p_notifyRootViewMetrics;
- (void)p_notifyRootViewMetricsIfRegionsChanged;
- (UIEdgeInsets)p_pagerSafeAreaInsets;
- (void)p_installHingeInteractionIfNeeded;
- (void)p_removeHingeInteractionIfNeeded;
- (NSString *)p_reservedRegionsJSON;

@end

@implementation KuiklyRenderView {
    CFTimeInterval _beginTime;
    NSMutableArray<dispatch_block_t> *_dellocTasks;
}

#pragma mark - init
- (nonnull instancetype)initWithSize:(CGSize)size
                         contextCode:(id)contextCode
                        contextParam:(nonnull KuiklyContextParam *)contextParam
                              params:(NSDictionary * _Nullable)params
                             delegate:(nonnull id<KuiklyRenderViewDelegate>)delegate {
    if (self = [super init]) {
        _pageName = contextParam.pageName;
        _contextParam = contextParam;
        _delegate = delegate;
        // 生成Core所需要的参数
        NSDictionary *coreParams = [self p_generateWithParams:params size:size];
        _renderCore = [[KuiklyRenderCore alloc] initWithRootView:self
                                                     contextCode:contextCode
                                                    contextParam:contextParam
                                                          params:coreParams
                                                        delegate:self];
    }
    return self;
}


#pragma mark - public
/*
 * @brief 通过KuiklyRenderView发送事件到KuiklyKotlin侧（支持多线程调用）.
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data {
    [_renderCore sendWithEvent:event data:data];
}

- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data sync:(BOOL)sync {
    [_renderCore sendWithEvent:event data:data sync:sync];
}
/*
 * @brief 获取模块对应的实例（仅支持在主线程调用）.
 * @param moduleName 模块名
 */
- (id<TDFModuleProtocol> _Nullable)moduleWithName:(NSString *)moduleName {
    NSAssert([NSThread isMainThread], @"should run on main thread");
    return [_renderCore moduleWithName:moduleName];
}

/*
 * @brief 获取tag对应的View实例（仅支持在主线程调用）.
 * @param tag view对应的索引
 * @return view实例
 */
- (id<KuiklyRenderViewExportProtocol> _Nullable)viewWithRefTag:(NSNumber *)tag {
    NSAssert([NSThread isMainThread], @"should run on main thread");
    return [_renderCore viewWithTag:tag];
}

/*
 * @brief 响应kotlin侧闭包
 * @param callbackID GlobalFunctions.createFunction返回的callback id
 * @param data 调用闭包传参
 */
- (void)fireCallbackWithID:(NSString *)callbackID data:(NSDictionary *)data {
    [_renderCore fireCallbackWithID:callbackID data:data];
}

/*
 * @brief 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
 */
- (void)syncFlushAllRenderTasks {
    [_renderCore syncFlushAllRenderTasks];
    [self p_dispatchContentViewDidLoadDelegateIfNeed];
}

/*
 * @brief 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
 * @param task 主线程任务
*/
- (void)performWhenViewDidLoadWithTask:(dispatch_block_t)task {
    [_renderCore performWhenViewDidLoadWithTask:task];
}

/*
 * @brief 执行任务当RenderView销毁（仅支持在主线程调用）
 * @param task 销毁时所执行的任务
*/
- (void)performWhenRenderViewDeallocWithTask:(dispatch_block_t)task {
    NSAssert([NSThread isMainThread], @"should run on main thread");
    if (!_dellocTasks) {
        _dellocTasks = [NSMutableArray new];
    }
    if (task) {
        [_dellocTasks addObject:task];
    }
}
/*
 * @brief RenderView完全创建后调用
*/
- (void)didCreateRenderView {
    [_renderCore didInitCore];
}

#pragma mark - override

- (void)setOnExceptionBlock:(OnUnhandledExceptionBlock)onExceptionBlock {
    _onExceptionBlock = onExceptionBlock;
    _renderCore.onExceptionBlock = onExceptionBlock;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    if (!CGSizeEqualToSize(_lastViewSize, self.bounds.size)) {
        _lastViewSize = self.bounds.size;
        [self p_notifyRootViewMetrics];
    }
}

#if !TARGET_OS_OSX
- (void)safeAreaInsetsDidChange {
    [super safeAreaInsetsDidChange];
    if (_lastSafeAreaInsets != nil) {
        NSString *insetsString = [KRConvertUtil stringWithInsets:[self p_pagerSafeAreaInsets]];
        if (![insetsString isEqualToString:_lastSafeAreaInsets]) {
            [self p_notifyRootViewMetrics];
        }
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_installHingeInteractionIfNeeded];
    } else {
        [self p_removeHingeInteractionIfNeeded];
    }
}
#endif

- (void)p_removeHingeInteractionIfNeeded {
#if KR_HAS_UIHINGE
    if (@available(iOS 27.1, *)) {
        if (self.hingeInteraction) {
            [self removeInteraction:self.hingeInteraction];
            self.hingeInteraction = nil;
        }
    }
#endif
}

- (void)p_installHingeInteractionIfNeeded {
#if KR_HAS_UIHINGE
    if (self.hingeInteraction) {
        return;
    }
    if (@available(iOS 27.1, *)) {
        __weak typeof(self) weakSelf = self;
        UIHingeInteraction *interaction =
            [[UIHingeInteraction alloc] initWithUpdateHandler:^(__unused UIHingeInteraction *hingeInteraction, UIHingeInteractionUpdate *update) {
                __strong typeof(weakSelf) strongSelf = weakSelf;
                if (!strongSelf) {
                    return;
                }
                NSInteger status = UIHingeStatusUnknown;
                if (update.hinge) {
                    status = update.hinge.status;
                }
                BOOL statusChanged = (status != strongSelf.lastNotifiedHingeStatus);
                strongSelf.lastNotifiedHingeStatus = status;
                NSString *regionsJSON = [strongSelf p_reservedRegionsJSON];
                BOOL regionsChanged = ![regionsJSON isEqualToString:strongSelf.lastPushedRegionsJSON];
                if (statusChanged || regionsChanged) {
                    [strongSelf p_notifyRootViewMetrics];
                }
            }];
        [self addInteraction:interaction];
        self.hingeInteraction = interaction;
    }
#endif
}

#if KR_HAS_RESERVED_REGION
/// 上报 frame + margins：frame 是含 margins 的最终避让范围，margins 是其中为交互内容预留的部分。
/// 不上报 region.identifier：该类在 SDK 里不透明，只能取到内存地址，且会跨 region 复用。
- (void)p_collectReservedRegionsOfKind:(UIViewReservedRegionKind *)kind
                                  name:(NSString *)name
                                  into:(NSMutableArray *)items API_AVAILABLE(ios(27.1)) {
    NSArray<UIViewReservedRegion *> *regions =
        [self reservedRegionsOfKind:kind options:UIViewReservedRegionQueryOptionsIncludeInactive];
    for (UIViewReservedRegion *region in regions) {
        CGRect frame = region.frame;
        UIEdgeInsets margins = region.margins;
        [items addObject:@{
            @"kind": name ?: @"",
            @"active": @(region.active ? 1 : 0),
            @"x": @(CGRectGetMinX(frame)),
            @"y": @(CGRectGetMinY(frame)),
            @"width": @(CGRectGetWidth(frame)),
            @"height": @(CGRectGetHeight(frame)),
            @"marginTop": @(margins.top),
            @"marginLeft": @(margins.left),
            @"marginBottom": @(margins.bottom),
            @"marginRight": @(margins.right),
        }];
    }
}
#endif

/// 当前根视图上的避让区域，序列化为 JSON 数组字符串。恒不返回 nil，无区域时返回 "[]"。
/// 用字符串而非数组：Kotlin 侧以 optString + JSONArray 解码，与 safeAreaInsets 一致。
- (NSString *)p_reservedRegionsJSON {
#if KR_HAS_RESERVED_REGION
    if (@available(iOS 27.1, *)) {
        NSMutableArray *items = [NSMutableArray array];
        [self p_collectReservedRegionsOfKind:[UIViewReservedRegionKind occlusionRegionKind]
                                        name:@"occlusion"
                                        into:items];
        [self p_collectReservedRegionsOfKind:[UIViewReservedRegionKind divisionRegionKind]
                                        name:@"division"
                                        into:items];
        if (items.count == 0) {
            return @"[]";
        }
        NSData *data = [NSJSONSerialization dataWithJSONObject:items options:0 error:nil];
        if (data.length > 0) {
            NSString *json = [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
            if (json.length > 0) {
                return json;
            }
        }
    }
#endif
    return @"[]";
}

- (void)notifyRootViewMetrics {
    [self p_notifyRootViewMetrics];
}

- (UIEdgeInsets)p_pagerSafeAreaInsets {
#if TARGET_OS_OSX // [macOS]
    NSWindow *hostWindow = [self.delegate viewControllerHostWindow];
    if (hostWindow) {
        if (@available(macOS 11.0, *)) {
            return hostWindow.contentView.safeAreaInsets;
        }
    }
    return [KRConvertUtil currentSafeAreaInsets];
#else
    if (@available(iOS 11.0, *)) {
        UIWindow *hostWindow = nil;
        if ([self.delegate respondsToSelector:@selector(viewControllerHostWindow)]) {
            hostWindow = [self.delegate viewControllerHostWindow];
        }
        if (hostWindow) {
            return hostWindow.safeAreaInsets;
        }
        return [KRConvertUtil currentSafeAreaInsets];
    }
    return UIEdgeInsetsMake([KRConvertUtil statusBarHeight], 0, 0, 0);
#endif
}

/// 只在 region 实际变化时才推事件，供 updateProperties 使用。
- (void)p_notifyRootViewMetricsIfRegionsChanged {
    NSString *regionsJSON = [self p_reservedRegionsJSON];
    if ([regionsJSON isEqualToString:self.lastPushedRegionsJSON]) {
        return;
    }
    [self p_notifyRootViewMetrics];
}

- (void)p_notifyRootViewMetrics {
    if (CGSizeEqualToSize(self.bounds.size, CGSizeZero) && CGSizeEqualToSize(_lastViewSize, CGSizeZero)) {
        return;
    }
    UIEdgeInsets insets = [self p_pagerSafeAreaInsets];
    NSString *insetsString = [KRConvertUtil stringWithInsets:insets];
    // 空结果必须是 "[]" 而不是缺省：Kotlin 侧用 data.has(RESERVED_REGIONS) 判定，
    // key 缺失会被当成「本次事件没带 region」而保留旧值，region 消失时就清不掉了。
    NSString *regionsJSON = [self p_reservedRegionsJSON];
    _lastSafeAreaInsets = [insetsString copy];
    _lastPushedRegionsJSON = [regionsJSON copy];
    
    CGSize screenSize = ({
#if TARGET_OS_OSX
        NSScreen *screen = [NSScreen mainScreen];
        screen ? screen.frame.size : CGSizeZero;
#else
        [UIScreen mainScreen].bounds.size;
#endif
    });
    UIViewController *viewController = [self getViewController];
    NSDictionary *data = @{
        KRWidthKey: @(CGRectGetWidth(self.bounds)),
        KRHeightKey: @(CGRectGetHeight(self.bounds)),
        KRDeviceWidthKey: @(screenSize.width),
        KRDeviceHeightKey: @(screenSize.height),
        KRActivityWidthKey: @(CGRectGetWidth(viewController.view.bounds)),
        KRActivityHeightKey: @(CGRectGetHeight(viewController.view.bounds)),
        KRSafeAreaInsets: insetsString,
        KRHingeStatusKey: @(_lastNotifiedHingeStatus),
        KRReservedRegionsKey: regionsJSON,
    };
    BOOL sync = [self p_syncSendEvent:KRRootViewSizeDidChangedEventKey];
    [_renderCore sendWithEvent:KRRootViewSizeDidChangedEventKey
                          data:data
                          sync:sync];
}


- (BOOL)p_syncSendEvent:(NSString *)event {
    if ([self.delegate respondsToSelector:@selector(syncSendEvent:)]) {
        return [self.delegate syncSendEvent:event];
    }
    return NO;
}

- (void)insertSubview:(UIView *)view atIndex:(NSInteger)index {
    [super insertSubview:view atIndex:index];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self p_dispatchContentViewDidLoadDelegateIfNeed];
    });
    
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self p_dispatchContentViewDidLoadDelegateIfNeed];
}

/// iOS 26+ 属性更新点：region 变化时 UIKit 会自动失效并重跑这里，等价于激活回调。
/// 只在能真正查到 region 的构建里编译，否则每次属性更新都白跑一次比较。
#if KR_HAS_RESERVED_REGION
- (void)updateProperties {
    [super updateProperties];
    if (@available(iOS 26.0, *)) {
        [self p_notifyRootViewMetricsIfRegionsChanged];
    }
}
#endif

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *view = [super hitTest:point withEvent:event];
    if (view) {
        [self.renderCore didHitTest];
    }
    return view;
}

#pragma mark - KuiklyRenderCoreDelegate

- (NSString *)turboDisplayKey {
    if ([self.delegate respondsToSelector:@selector(turboDisplayKey)]) {
        return [self.delegate turboDisplayKey];
    }
    return nil;
}

// KuiklyRenderView.m - 实现传递
- (KRTurboDisplayConfig *)turboDisplayConfig {
    if ([self.delegate respondsToSelector:@selector(turboDisplayConfig)]) {
        return [self.delegate turboDisplayConfig];
    }
    return nil;
}

- (UIWindow *)viewControllerHostWindow {
    if ([self.delegate respondsToSelector:@selector(viewControllerHostWindow)]) {
        return [self.delegate viewControllerHostWindow];
    }
    return nil;
}

#pragma mark - private

- (NSDictionary *)p_generateWithParams:(NSDictionary *)params size:(CGSize)size {
    NSMutableDictionary *mParmas = [[NSMutableDictionary alloc] init];
    mParmas[KRRootViewWidthKey] = @(size.width);
    mParmas[KRRootViewHeightKey] = @(size.height);
    mParmas[KRUrlKey] = _pageName ?: @"";
    mParmas[KRStatusBarHeightKey] = @([KRConvertUtil statusBarHeight]);
    // [macOS] 平台信息与屏幕尺寸/密度
#if TARGET_OS_OSX // [macOS]
    mParmas[KRPlatformKey] = @"macOS";
    NSScreen *mainScreen = [NSScreen mainScreen];
    CGSize deviceSize = mainScreen ? mainScreen.frame.size : CGSizeZero;
    mParmas[KRDeviceWidthKey] = @(deviceSize.width);
    mParmas[KRDeviceHeightKey] = @(deviceSize.height);
    mParmas[KROsVersionKey] = [[NSProcessInfo processInfo] operatingSystemVersionString] ?: @"";
    UIWindow *window = [self.delegate viewControllerHostWindow] ?: [KRConvertUtil keyWindow];
    CGRect windowBounds = window.frame;
    mParmas[KRActivityWidthKey] = @(windowBounds.size.width);
    mParmas[KRActivityHeightKey] = @(windowBounds.size.height);
#else
    mParmas[KRPlatformKey] = @"iOS";
    mParmas[KRDeviceWidthKey] = @(CGRectGetWidth([UIScreen mainScreen].bounds));
    mParmas[KRDeviceHeightKey] = @(CGRectGetHeight([UIScreen mainScreen].bounds));
    mParmas[KROsVersionKey] = [[UIDevice currentDevice] systemVersion] ?: @"";
    UIWindow *window = [self.delegate viewControllerHostWindow] ?: [KRConvertUtil keyWindow];
    CGRect windowBounds = window.bounds;
    mParmas[KRActivityWidthKey] = @(windowBounds.size.width);
    mParmas[KRActivityHeightKey] = @(windowBounds.size.height);
#endif
    mParmas[KRAppVersionKey] = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"] ? : @"1.0.0";
    mParmas[KRParamKey] = params? : @{};
	mParmas[KRNativeBuild] = @(2);
    // 无障碍化开关与安全区域/密度
#if TARGET_OS_OSX // [macOS]
    mParmas[KRAccessibilityRunning] = @(0);
    NSWindow *hostWindow = [self.delegate viewControllerHostWindow];
    if (hostWindow) {
        if (@available(macOS 11.0, *)) {
            mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:hostWindow.contentView.safeAreaInsets];
        } else {
            mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:[KRConvertUtil currentSafeAreaInsets]];
        }
    } else {
        mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:[KRConvertUtil currentSafeAreaInsets]];
    }
    mParmas[KRDensity] = @([NSScreen mainScreen].backingScaleFactor ?: 1.0);
#else
    mParmas[KRAccessibilityRunning] = @(UIAccessibilityIsVoiceOverRunning() ? 1: 0);
    if (@available(iOS 11.0, *)) {
        UIWindow *hostWindow = [self.delegate viewControllerHostWindow];
        if (hostWindow) {
            mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:hostWindow.safeAreaInsets];
        } else {
            mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:[KRConvertUtil currentSafeAreaInsets]];
        }
    } else {
        mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:UIEdgeInsetsMake([KRConvertUtil statusBarHeight], 0, 0, 0)];
        // Fallback on earlier versions
    }
    _lastSafeAreaInsets = [mParmas[KRSafeAreaInsets] copy];
    mParmas[KRDensity] = @([UIScreen mainScreen].scale);
#endif
    mParmas[KRReservedRegionsKey] = [self p_reservedRegionsJSON];
    mParmas[KRHingeStatusKey] = @(_lastNotifiedHingeStatus);
    return mParmas;
}

- (void)p_dispatchContentViewDidLoadDelegateIfNeed {
    if (!_contentViewDidLoad && self.subviews.count) {
        _contentViewDidLoad = YES;
        if ([self.delegate respondsToSelector:@selector(contentViewDidLoadWithrenderView:)]) {
            [self.delegate contentViewDidLoadWithrenderView:self];
        }
    }
}

- (void)p_flushDeallocTasks {
    if (!_dellocTasks) {
        return ;
    }
    for (dispatch_block_t task in _dellocTasks) {
        task();
    }
    _dellocTasks = nil;
}

/**
 * 获取当前RenderView所属的ViewController
 */
- (UIViewController *)getViewController {
    UIResponder *responder = self.nextResponder;
    while (responder) {
        if ([responder isKindOfClass:[UIViewController class]]) {
            return (UIViewController *)responder;
        }
        responder = responder.nextResponder;
    }
    return nil;
}

#pragma mark - dealloc

- (void)dealloc {
    [self p_flushDeallocTasks];
    KuiklyRenderCore *renderCore = _renderCore;
    [renderCore willDealloc];
    // 异步销毁core
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [renderCore description];
    });
}

@end

