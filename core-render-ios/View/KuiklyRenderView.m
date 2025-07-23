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
NSString *const KROsVersionKey = @"osVersion";
NSString *const KRAppVersionKey = @"appVersion";
NSString *const KRParamKey = @"param";
NSString *const KRWidthKey = @"width";
NSString *const KRHeightKey = @"height";
NSString *const KRNativeBuild = @"nativeBuild";
NSString *const KRSafeAreaInsets = @"safeAreaInsets";
NSString *const KRAccessibilityRunning = @"isAccessibilityRunning";
NSString *const KRDensity = @"density";

@interface KuiklyRenderView()<KuiklyRenderCoreDelegate>
/** 渲染核心实现者对象 */
@property (nonatomic, strong) KuiklyRenderCore *renderCore;
/** 页面注册名 */
@property (nonatomic, strong) NSString *pageName;
/** 上次自身view尺寸 */
@property (nonatomic, assign) CGSize lastViewSize;
/** 内容视图是否完成加载过 */
@property (nonatomic, assign, getter=isContentViewDidLoad) BOOL contentViewDidLoad;
/** delegate for KuiklyRenderView. */
@property (nonatomic, weak, readwrite) id<KuiklyRenderViewDelegate> delegate;

@end

@implementation KuiklyRenderView {
    CFTimeInterval _beginTime;
    NSMutableArray<dispatch_block_t> *_dellocTasks;
}

#pragma mark - init
- (nonnull instancetype)initWithSize:(CGSize)size
                         contextCode:(NSString *)contextCode
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
                                                     contextCode:(NSString *)contextCode
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
        CGSize screenSize = [UIScreen mainScreen].bounds.size;
        NSDictionary *data = @{KRWidthKey: @(CGRectGetWidth(frame)),
                               KRHeightKey: @(CGRectGetHeight(frame)),
                               KRDeviceWidthKey:@(screenSize.width),
                               KRDeviceHeightKey:@(screenSize.height)
        };
        [_renderCore sendWithEvent:KRRootViewSizeDidChangedEventKey
                              data:data];
    }
  
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

#pragma mark - private

- (NSDictionary *)p_generateWithParams:(NSDictionary *)params size:(CGSize)size {
    NSMutableDictionary *mParmas = [[NSMutableDictionary alloc] init];
    mParmas[KRRootViewWidthKey] = @(size.width);
    mParmas[KRRootViewHeightKey] = @(size.height);
    mParmas[KRUrlKey] = _pageName ?: @"";
    mParmas[KRStatusBarHeightKey] = @([KRConvertUtil statusBarHeight]);
    mParmas[KRPlatformKey] = @"iOS";
    mParmas[KRDeviceWidthKey] = @(CGRectGetWidth([UIScreen mainScreen].bounds));
    mParmas[KRDeviceHeightKey] = @(CGRectGetHeight([UIScreen mainScreen].bounds));
    mParmas[KROsVersionKey] = [[UIDevice currentDevice] systemVersion] ?: @"";
    mParmas[KRAppVersionKey] = [[[NSBundle mainBundle] infoDictionary] objectForKey:@"CFBundleShortVersionString"] ? : @"1.0.0";
    mParmas[KRParamKey] = params? : @{};
	mParmas[KRNativeBuild] = @(2);
    mParmas[KRAccessibilityRunning] = @(UIAccessibilityIsVoiceOverRunning() ? 1: 0); // 无障碍化是否开启
    if (@available(iOS 11.0, *)) {
        mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:[KRConvertUtil currentSafeAreaInsets]];
    } else {
        mParmas[KRSafeAreaInsets] = [KRConvertUtil stringWithInsets:UIEdgeInsetsMake([KRConvertUtil statusBarHeight], 0, 0, 0)];
        // Fallback on earlier versions
    }
    mParmas[KRDensity] = @([UIScreen mainScreen].scale);
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

