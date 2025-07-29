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

#import "KuiklyRenderViewControllerBaseDelegator.h"
#import "KuiklyRenderViewControllerBaseDelegator+Extension.h"
#import "KuiklyRenderView.h"
#import "KRSnapshotModule.h"
#import "KRHttpRequestTool.h"
#import "KRConvertUtil.h"
#import "KuiklyRenderFrameworkContextHandler.h"
#import "KuiklyContextParam.h"
#import "KRPerformanceManager.h"
#import "KRPerformanceModule.h"
#import "KRWeakObject.h"
#import "NSObject+KR.h"
#import "KuiklyRenderCore.h"
#import "KuiklyRenderFrameworkContextHandler.h"
#import "KuiklyCoreDefine.h"
#define KRWeakSelf __weak typeof(self) weakSelf = self;

#define VIEW_DID_APPEAR @"viewDidAppear"
#define VIEW_DID_DISAPPEAR @"viewDidDisappear"
#define PAGE_FIRST_FRAME_PAINT @"pageFirstFramePaint"

NSString *const KRPageDataSnapshotKey = @"kr_snapshotKey";
@interface KuiklyRenderViewControllerBaseDelegator()<KuiklyRenderViewDelegate>

@property (nonatomic, strong) NSDictionary *pageData;
@property (nullable, nonatomic, strong) UIView * loadingView;
@property (nullable, nonatomic, strong) UIView * errorView;
@property (nonatomic, assign) BOOL fetchContextCoding;
@property (nonatomic, strong, readwrite) KuiklyRenderView *renderView;
@property (nonatomic, assign, getter=isViewDidAppear) BOOL viewDidAppear;
@property (nonatomic, strong) NSMutableArray<dispatch_block_t> *eventLazyTasks;
@property (nonatomic, strong) NSMutableSet<KRWeakObject *> *lifeCycleListenerSet;
@property (nonatomic, assign) BOOL contentViewDidLoad;
@property (nonatomic, copy) NSString *frameworkName;
@property (nonatomic, strong) KuiklyBaseContextMode *contextMode;

/** 首屏快照过渡视图 */
@property (nonatomic, strong) UIImageView *snapshotView;
@end

@implementation KuiklyRenderViewControllerBaseDelegator {
    KRPerformanceManager *_performanceManager;
}

- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData {
    return [self initWithPageName:pageName pageData:pageData frameworkName:nil];
}

- (instancetype)initWithPageName:(NSString *)pageName
                        pageData:(NSDictionary *)pageData
                   frameworkName:(NSString *)frameworkName {
    if (self = [super init]) {
        self.pageName = pageName;
        _pageData = pageData;
        _frameworkName = frameworkName;
        _performanceManager = [[KRPerformanceManager alloc] initWithPageName:pageName];
        [self addDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)_performanceManager];
        _eventLazyTasks = [[NSMutableArray alloc] init];
        [self p_addNotifications];
    }
    return self;
}


- (void)p_addNotifications {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                            selector:@selector(onReceiveApplicationDidBecomeActiveNotification:)
                                                 name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onReceiveApplicationWillResignActiveNotification:)
                                                 name:UIApplicationWillResignActiveNotification object:nil];
}


#pragma mark - public

- (void)viewDidLoadWithView:(UIView *)view {
    self.view = view;
    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewDidLoad) object:nil];
    [self loadKuiklyRenderView];
    if (_pageData[KRPageDataSnapshotKey]) {
        [self loadSnapshotViewWithKey:_pageData[KRPageDataSnapshotKey]];
    }
}

- (void)viewDidLayoutSubviews {
    _renderView.frame = self.view.bounds;
    _snapshotView.frame = self.view.bounds;
    _loadingView.frame = self.view.bounds;
    _errorView.frame = self.view.bounds;
    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewDidLayoutSubviews) object:nil];
}

- (void)viewWillAppear {
    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewWillAppear) object:nil];
}

- (void)viewDidAppear {
    self.viewDidAppear = YES;
    [self sendWithEvent:VIEW_DID_APPEAR data:@{}];

    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewDidAppear) object:nil];
}

- (void)viewWillDisappear {
    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewWillDisappear) object:nil];
}

- (void)viewDidDisappear {
    self.viewDidAppear = NO;
    [self sendWithEvent:VIEW_DID_DISAPPEAR data:@{}];
    [self p_disptachDelegatorLifeCycleWithSel:@selector(viewDidDisappear) object:nil];
}

- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data {
    __weak typeof(&*self) weakSelf = self;
    dispatch_block_t task = ^{
        [weakSelf.renderView sendWithEvent:event
                                      data:data];
        [weakSelf p_disptachDelegatorLifeCycleWithSel:@selector(didSendEvent:) object:event];
    };
    if (_renderView) {
        task();
    } else {
        [_eventLazyTasks addObject:task];
    }
}

- (void)addDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener {
    for (KRWeakObject *object in self.lifeCycleListenerSet) {
        if (object == lifeCycleListener || !lifeCycleListener) {
            return ;
        }
    }
    if ([lifeCycleListener respondsToSelector:@selector(setDelegator:)]) {
        lifeCycleListener.delegator = self;
    }
    [self.lifeCycleListenerSet addObject:[[KRWeakObject alloc] initWithObject:lifeCycleListener]];
}

- (void)removeDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener {
    for (KRWeakObject *object in [self.lifeCycleListenerSet copy]) {
        if (object == lifeCycleListener) {
            [self.lifeCycleListenerSet removeObject:object];
            break;
        }
    }
}

+ (BOOL)isPageExistWithPageName:(NSString *)pageName frameworkName:(NSString *)frameworkName {
    return [KuiklyRenderFrameworkContextHandler isPageExistWithPageName:pageName frameworkName:frameworkName];
}

#pragma mark - private

- (KuiklyBaseContextMode *)createContextMode:(NSString * _Nullable) contextCode {
    return [[KuiklyBaseContextMode alloc] initFrameworkMode];
}

- (void)fetchContextCodeWithResultCallback:(KuiklyContextCodeCallback)callback {
    if ([self.delegate respondsToSelector:@selector(fetchContextCodeWithPageName:resultCallback:)]) {
        [self.delegate fetchContextCodeWithPageName:self.pageName resultCallback:callback];
    } else if (_frameworkName.length) {
        if (callback) {
            callback(_frameworkName, nil);
        }
    } else {
        NSAssert(false, @"fetchContextCodeWithPageName:resultCallback:代理接口未实现 or frameworkName未传参");
    }
}

- (void)initRenderViewWithContextCode:(NSString *)contextCode {
    [self p_disptachDelegatorLifeCycleWithSel:@selector(willInitRenderView) object:nil];
    KuiklyContextParam *contextParam = [KuiklyContextParam newWithPageName:self.pageName];
    contextParam.contextMode = self.contextMode;

    _renderView = [[KuiklyRenderView alloc] initWithSize:self.view.bounds.size
                                             contextCode:contextCode
                                            contextParam:contextParam
                                                  params:[self contextPageData]
                                                delegate:self];
    [self setExceptionBlock:_renderView];
    // 接收当前rootview传过来的通知
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(pageLoadEventFromKotlin:)
                                                 name:kKuiklyPageLoadTimeFromKotlinNotification
                                               object:_renderView];
    [self.view addSubview:_renderView];
    _renderView.frame = self.view.bounds;
    if ([self.delegate respondsToSelector:@selector(renderViewDidCreated)]) {
        [self.delegate renderViewDidCreated];
    }
    [_renderView didCreateRenderView];
    if (!_renderView.subviews.count) {
        if ([self shouldSyncRenderingView:
             [KuiklyRenderFrameworkContextHandler isFrameworkWithContextCode:contextCode]]) {
            [_renderView syncFlushAllRenderTasks]; // 同步渲染
        }
    }
   
    [self p_performEventLazyTasks];
    [self p_bringSnapshotViewToFront];
    [self p_disptachDelegatorLifeCycleWithSel:@selector(didInitRenderView) object:nil];
}

- (NSDictionary *)contextPageData {
    if ([self.delegate respondsToSelector:@selector(contextPageData)]) {
        NSDictionary *contentxData = [self.delegate contextPageData];
        if ([contentxData isKindOfClass:[NSDictionary class]]) {
            NSMutableDictionary *resData = [_pageData mutableCopy] ?: [NSMutableDictionary new];
            [resData addEntriesFromDictionary:contentxData];
            return resData;
        }
    }
    return _pageData;
}

- (void)loadKuiklyRenderView {
    if (self.fetchContextCoding) {
        return ;
    }

    __weak typeof(&*self) weakSelf = self;
    self.fetchContextCoding = YES;
    dispatch_async(dispatch_get_main_queue(), ^{
        if (self.fetchContextCoding) {
            [self p_showLoadingView];
        }
    });
    [self p_disptachDelegatorLifeCycleWithSel:@selector(willFetchContextCode) object:nil];
    [self fetchContextCodeWithResultCallback:^(NSString * _Nullable contextCode, NSError * _Nullable error) {
        if (!weakSelf) {
            return ;
        }
        [weakSelf p_performOnMainThreadWithBlock:^{
            weakSelf.contextMode = [self createContextMode:contextCode];
            [weakSelf p_disptachDelegatorLifeCycleWithSel:@selector(didFetchContextCode) object:nil];
            weakSelf.fetchContextCoding = NO;
            [weakSelf p_hideAllStatusView];
            if (contextCode.length) {
                [weakSelf initRenderViewWithContextCode:contextCode];
            } else {
                [weakSelf p_showErrorView];
                NSError *e = error;
                if (!e) {
                    e = [NSError errorWithDomain:KuiklyLoadErrorDomain
                                                         code:KuiklyLoadError_fetchContextCode
                                                     userInfo:nil];
                }
                [weakSelf onPageLoadComplete:NO error:e];
            }
        }];
    }];
}

- (void)loadSnapshotViewWithKey:(NSString *)snapshotKey {
    UIImage *snapshotImage = [KRSnapshotModule snapshotPagerWithSnapshotKey:snapshotKey];
    if (!snapshotImage) {
        return ;
    }
    UIImageView *imageView = [[UIImageView alloc] initWithImage:snapshotImage];
    [self.view addSubview:imageView];
    _snapshotView = imageView;
    _snapshotView.userInteractionEnabled = NO;
    imageView.frame = self.view.bounds;
    // 超时隐藏
    KRWeakSelf;
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [weakSelf p_hideSnapshotView];
    });
}

// 是否同步渲染，默认同步，对齐Native
- (BOOL)shouldSyncRenderingView:(BOOL)frameworkMode {
    if (frameworkMode && [self.delegate respondsToSelector:@selector(syncRenderingWhenPageAppear)]) {
        return [self.delegate syncRenderingWhenPageAppear];
    }
    return frameworkMode;
}


#pragma mark - notifications

- (void)onReceiveApplicationDidBecomeActiveNotification:(NSNotification *)notification {
    if (self.isViewDidAppear) {
        [self.renderView sendWithEvent:VIEW_DID_APPEAR data:@{ @"app" : @(1) }];
        [self p_disptachDelegatorLifeCycleWithSel:@selector(onReceiveApplicationDidBecomeActive) object:nil];
    }
}

- (void)onReceiveApplicationWillResignActiveNotification:(NSNotification *)notification {
    if (self.isViewDidAppear) {
        [self.renderView sendWithEvent:VIEW_DID_DISAPPEAR data:@{ @"app" : @(1) }];
        [self p_disptachDelegatorLifeCycleWithSel:@selector(onReceiveApplicationWillResignActive) object:nil];
    }
}


#pragma mark - KuiklyRenderViewDelegate

- (void)contentViewDidLoadWithrenderView:(KuiklyRenderView *)kuiklyRenderView {
    _contentViewDidLoad = YES;
    if ([self.delegate respondsToSelector:@selector(contentViewDidLoad)]) {
        [self.delegate contentViewDidLoad];
    }
    [self onPageLoadComplete:YES error:nil];
    [self p_disptachDelegatorLifeCycleWithSel:@selector(contentViewDidLoad) object:nil];
    [self sendWithEvent:PAGE_FIRST_FRAME_PAINT data:@{}];
    [self p_hideSnapshotView];
}

- (void)scrollViewDidLayout:(UIScrollView *)scrollView renderView:(KuiklyRenderView *)renderView {
    if ([self.delegate respondsToSelector:@selector(scrollViewDidLayout:)]) {
        [self.delegate scrollViewDidLayout:scrollView];
    }
}

- (NSString *)turboDisplayKey {
    if ([self.delegate respondsToSelector:@selector(turboDisplayKey)]) {
        return [self.delegate turboDisplayKey];
    }
    return nil;
}

#pragma mark - exception handle

- (void)setExceptionBlock:(KuiklyRenderView *)view {
    __weak typeof(self) wself = self;
    view.onExceptionBlock = ^(NSString *exReason, NSString *callstackStr, KuiklyContextMode mode) {
        if (!wself.contentViewDidLoad) {
            NSDictionary *userInfo = @{
                @"reason": exReason?:@"",
                @"callStack": callstackStr ?: @""
            };
            NSError *error = [NSError errorWithDomain:KuiklyLoadErrorDomain
                                                 code:KuiklyLoadError_fatalException
                                             userInfo:userInfo];
            [wself onPageLoadComplete:NO error:error];
        }
        if ([wself.delegate respondsToSelector:@selector(onUnhandledException:stack:mode:)]) {
            [wself.delegate onUnhandledException:exReason stack:callstackStr mode:mode];
        } else if (wself && wself.contextMode.modeId == KuiklyContextMode_Framework) {
            @throw [NSException exceptionWithName:exReason reason:callstackStr userInfo:nil];
        }
    };
}

- (void)onPageLoadComplete:(BOOL)isSucceed error:(NSError *)error {
    if ([self.delegate respondsToSelector:@selector(onPageLoadComplete:error:mode:)]) {
        [self.delegate onPageLoadComplete:isSucceed error:error mode:self.contextMode.modeId];
    }
}

#pragma mark - private

- (void)pageLoadEventFromKotlin:(NSNotification *)notification {
    [_performanceManager mergeKotlinCreatePageTime:notification.userInfo];
}

- (void)p_showLoadingView {
    if (self.loadingView && !self.loadingView.superview) {
        [self.view addSubview:self.loadingView];
        self.loadingView.frame = self.view.bounds;
    }
    _errorView.hidden = YES;
    self.loadingView.hidden = NO;
}

- (void)p_showErrorView {
    if (self.errorView && !self.errorView.superview) {
        [self.view addSubview:self.errorView];
        self.errorView.frame = self.view.bounds;
    }
    _loadingView.hidden = YES;
    self.errorView.hidden = NO;
}

- (void)p_hideAllStatusView{
    _loadingView.hidden = YES;
    _errorView.hidden = YES;
    [_loadingView removeFromSuperview];
    _loadingView = nil;
    [_errorView removeFromSuperview];
    _errorView = nil;
}

- (void)p_performOnMainThreadWithBlock:(dispatch_block_t)block {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_async(dispatch_get_main_queue(), block);
    }
}
    
- (void)p_performEventLazyTasks {
    if (_eventLazyTasks.count) {
        for (dispatch_block_t task in _eventLazyTasks.copy) {
            task();
        }
        [_eventLazyTasks removeAllObjects];
    }
}

- (void)p_bringSnapshotViewToFront {
    if (_snapshotView) {
        [self.view bringSubviewToFront:_snapshotView];
    }
}

- (void)p_hideSnapshotView {
    if (_snapshotView) {
        [UIView animateWithDuration:0.01 delay:0.2 options:(UIViewAnimationOptionCurveLinear) animations:^{
            self.snapshotView.alpha = 0;
                } completion:^(BOOL finished) {
                    [self.snapshotView removeFromSuperview];
                    self.snapshotView = nil;
                }];
    }
}

- (void)p_disptachDelegatorLifeCycleWithSel:(SEL _Nonnull)selector object:(id _Nullable)object {
    for (KRWeakObject *ele in self.lifeCycleListenerSet) {
        if ([ele.weakObject respondsToSelector:selector]) {
            [ele.weakObject kr_invokeWithSelector:selector args:object];
        }
    }
}


#pragma mark - getter

- (UIView *)loadingView{
    if (!_loadingView
        && [self.delegate respondsToSelector:@selector(createLoadingView)]) {
        _loadingView = [self.delegate createLoadingView];
        _loadingView.frame = self.view.bounds;
        _loadingView.hidden = NO;
    }
    return _loadingView;
}

- (UIView *)errorView{
    if (!_errorView
        && [self.delegate respondsToSelector:@selector(createErrorView)]) {
        _errorView = [self.delegate createErrorView];
        _errorView.hidden = YES;
        _errorView.frame = self.view.bounds;
        _errorView.userInteractionEnabled = YES;
        UITapGestureRecognizer *tapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(onRetryLoading:)];
        [_errorView addGestureRecognizer:tapGR];
    }
    return _errorView;
}

- (NSMutableSet<KRWeakObject *> *)lifeCycleListenerSet {
    if (!_lifeCycleListenerSet) {
        _lifeCycleListenerSet = [[NSMutableSet alloc] init];
    }
    return _lifeCycleListenerSet;
}

#pragma mark - cation

- (void)onRetryLoading:(id)sender {
    [self p_showLoadingView];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.2 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self loadKuiklyRenderView];
    });
}

- (void)dealloc {
    [self p_disptachDelegatorLifeCycleWithSel:@selector(delegatorDealloc) object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end


