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

#import "KuiklyRenderViewController.h"
#import "KuiklyRenderViewControllerBaseDelegator.h"
#import "KuiklyRenderContextProtocol.h"
#import "KuiklyRenderCore.h"
#import "KRPerformanceDataProtocol.h"
#import "KRPerformanceManager.h"
#import "KRConvertUtil.h"


#pragma mark - Constants

/// 默认的framework名称
static NSString * const kDefaultFrameworkName = @"shared";
/// 默认应用ID
static NSString * const kDefaultAppId = @"1";
/// 平台标识
static NSString * const kPlatformIdentifier = @"macOS";
/// 异常信息键
static NSString * const kExceptionUserInfoKey = @"exception";
/// UI元素尺寸
static const CGFloat kLoadingIndicatorSize = 32.0;
static const CGFloat kErrorLabelFontSize = 16.0;
/// 单位换算
static const NSInteger kBytesToMegabytes = 1024 * 1024;


#pragma mark - KuiklyPageLifeCycleObserver

/**
 * @brief Kuikly页面生命周期观察者
 *
 * 负责监听和响应KuiklyRenderViewControllerBaseDelegator的生命周期事件
 */
@interface KuiklyPageLifeCycleObserver : NSObject <KRControllerDelegatorLifeCycleProtocol>

@end

@implementation KuiklyPageLifeCycleObserver

@synthesize delegator = _delegator;


#pragma mark - KRControllerDelegatorLifeCycleProtocol

- (void)viewDidLoad {
    NSLog(@"[KuiklyPageLifeCycle] viewDidLoad");
}

- (void)willInitRenderView {
    NSLog(@"[KuiklyPageLifeCycle] willInitRenderView");
}

- (void)didInitRenderView {
    NSLog(@"[KuiklyPageLifeCycle] didInitRenderView");
}

- (void)didSendEvent:(NSString *)event {
    NSLog(@"[KuiklyPageLifeCycle] didSendEvent: %@", event);
}

- (void)viewWillAppear {
    NSLog(@"[KuiklyPageLifeCycle] viewWillAppear");
}

- (void)viewDidAppear {
    NSLog(@"[KuiklyPageLifeCycle] viewDidAppear");
}

- (void)viewWillDisappear {
    NSLog(@"[KuiklyPageLifeCycle] viewWillDisappear");
}

- (void)viewDidDisappear {
    NSLog(@"[KuiklyPageLifeCycle] viewDidDisappear");
}

- (void)willFetchContextCode {
    NSLog(@"[KuiklyPageLifeCycle] willFetchContextCode");
}

- (void)didFetchContextCode {
    NSLog(@"[KuiklyPageLifeCycle] didFetchContextCode");
}

- (void)contentViewDidLoad {
    NSLog(@"[KuiklyPageLifeCycle] contentViewDidLoad");
}

- (void)delegatorDealloc {
    NSLog(@"[KuiklyPageLifeCycle] delegatorDealloc");
}

@end


#pragma mark - KuiklyPageViewController

@interface KuiklyRenderViewController () <KuiklyRenderViewControllerBaseDelegatorDelegate>

/// Kuikly渲染代理器
@property (nonatomic, strong, readonly) KuiklyRenderViewControllerBaseDelegator *delegator;

/// 生命周期观察者
@property (nonatomic, strong, readonly) KuiklyPageLifeCycleObserver *lifeCycleObserver;

/// 当前页面名称
@property (nonatomic, copy, readonly) NSString *pageName;

/// 当前页面数据
@property (nonatomic, copy, readonly) NSDictionary<NSString *, id> *pageData;

/// 页面开始加载时间
@property (nonatomic, assign) CFTimeInterval beginTime;

/// 视图是否可见
@property (nonatomic, assign, getter=isViewVisible) BOOL viewVisible;

@end

@implementation KuiklyRenderViewController

#pragma mark - Lifecycle

- (instancetype)initWithPageName:(NSString *)pageName 
                        pageData:(nullable NSDictionary<NSString *, id> *)data {
    NSParameterAssert(pageName.length > 0);
    
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        _pageName = [pageName copy];
        _pageData = [self mergeExtendedParametersWithOriginalParameters:data];
        _lifeCycleObserver = [[KuiklyPageLifeCycleObserver alloc] init];
        _viewVisible = NO;
        
        [self setupDelegatorWithPageName:pageName data:_pageData];
        [self registerNotifications];
    }
    return self;
}

- (void)dealloc {
    [self unregisterNotifications];
    NSLog(@"[KuiklyPageViewController] dealloc - page: %@", self.pageName);
}

#pragma mark - View Lifecycle

- (void)loadView {
    // 设置合理的初始尺寸，避免窗口过小
    // 注意：实际窗口尺寸由包含它的窗口或导航控制器决定
    self.view = [[NSView alloc] initWithFrame:NSMakeRect(0, 0, 800, 600)];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupView];
    [self.delegator viewDidLoadWithView:(id)self.view];
}

- (void)viewDidLayout {
    [super viewDidLayout];
    [self.delegator viewDidLayoutSubviews];
}

- (void)viewWillAppear {
    [super viewWillAppear];
    self.viewVisible = YES;
    [self.delegator viewWillAppear];
}

- (void)viewDidAppear {
    [super viewDidAppear];
    [self.delegator viewDidAppear];
}

- (void)viewWillDisappear {
    [super viewWillDisappear];
    self.viewVisible = NO;
    [self.delegator viewWillDisappear];
}

- (void)viewDidDisappear {
    [super viewDidDisappear];
    [self.delegator viewDidDisappear];
    [self logPerformanceMetrics];
}

#pragma mark - Public Methods

- (void)updateWithPageName:(NSString *)pageName 
                  pageData:(nullable NSDictionary<NSString *, id> *)data {
    NSParameterAssert(pageName.length > 0);
    
    if ([self shouldUpdateWithPageName:pageName data:data]) {
        _pageName = [pageName copy];
        _pageData = [self mergeExtendedParametersWithOriginalParameters:data];
        
        NSLog(@"[KuiklyPageViewController] Update page: %@ with data: %@", pageName, data);
        // TODO: 实现页面动态更新逻辑
    }
}

#pragma mark - Private Setup Methods

- (void)setupDelegatorWithPageName:(NSString *)pageName 
                              data:(NSDictionary<NSString *, id> *)data {
    _delegator = [[KuiklyRenderViewControllerBaseDelegator alloc] 
                  initWithPageName:pageName pageData:data];
    
    [self.delegator.performanceManager setMonitorType:KRMonitorType_ALL];
    self.delegator.delegate = self;
    [self.delegator addDelegatorLifeCycleListener:self.lifeCycleObserver];
}

- (void)setupView {
    self.view.wantsLayer = YES;
    self.view.layer.backgroundColor = NSColor.whiteColor.CGColor;
}

- (void)registerNotifications {
    NSNotificationCenter *center = NSNotificationCenter.defaultCenter;
    [center addObserver:self
               selector:@selector(handleKuiklyException:)
                   name:kKuiklyFatalExceptionNotification
                 object:nil];
}

- (void)unregisterNotifications {
    [NSNotificationCenter.defaultCenter removeObserver:self];
}

#pragma mark - Private Helper Methods

- (BOOL)shouldUpdateWithPageName:(NSString *)pageName 
                            data:(nullable NSDictionary<NSString *, id> *)data {
    BOOL pageNameChanged = ![self.pageName isEqualToString:pageName];
    BOOL dataChanged = ![self.pageData isEqualToDictionary:data ?: @{}];
    return pageNameChanged || dataChanged;
}

- (NSDictionary<NSString *, id> *)mergeExtendedParametersWithOriginalParameters:(nullable NSDictionary<NSString *, id> *)parameters {
    NSMutableDictionary<NSString *, id> *mergedParameters = [parameters ?: @{} mutableCopy];
    // 可在此添加扩展参数
    return [mergedParameters copy];
}

#pragma mark - Performance Logging

- (void)logPerformanceMetrics {
    KRPerformanceManager *manager = self.delegator.performanceManager;
    
    NSDictionary *startTimes = manager.stageStartTimes;
    NSDictionary *durations = manager.stageDurations;
    
    KRMemoryMonitor *memoryMonitor = manager.memoryMonitor;
    NSInteger memoryUsageMB = memoryMonitor.avgIncrementMemory / kBytesToMegabytes;
    
    KRFPSMonitor *mainFPSMonitor = manager.mainFPS;
    KRFPSMonitor *kotlinFPSMonitor = manager.kotlinFPS;
    
    NSLog(@"[Performance] Page: %@", self.pageName);
    NSLog(@"[Performance] Memory: %ld MB, MainFPS: %.2lu, KotlinFPS: %.2lu",
          (long)memoryUsageMB, (unsigned long)mainFPSMonitor.avgFPS, (unsigned long)kotlinFPSMonitor.avgFPS);
    NSLog(@"[Performance] StartTimes: %@", startTimes);
    NSLog(@"[Performance] Durations: %@", durations);
}

#pragma mark - Exception Handling

- (void)handleKuiklyException:(NSNotification *)notification {
    NSDictionary *userInfo = notification.userInfo;
    NSString *exceptionString = userInfo[kExceptionUserInfoKey];
    
    if (exceptionString.length == 0) {
        return;
    }
    
    NSArray<NSString *> *components = [exceptionString componentsSeparatedByString:@"\n"];
    if (components.count == 0) {
        return;
    }
    
    NSString *exceptionName = components.firstObject;
    NSArray<NSString *> *callStackArray = components.count > 1 
        ? [components subarrayWithRange:NSMakeRange(1, components.count - 1)]
        : @[];
    
    NSString *callStack = [callStackArray componentsJoinedByString:@"\n"];
    NSLog(@"[KuiklyException] %@\nStack:\n%@", exceptionName, callStack);
    
    // TODO: 集成崩溃上报系统
}

#pragma mark - KuiklyRenderViewControllerBaseDelegatorDelegate

- (NSView *)createLoadingView {
    NSView *loadingView = [[NSView alloc] initWithFrame:NSZeroRect];
    loadingView.wantsLayer = YES;
    loadingView.layer.backgroundColor = NSColor.whiteColor.CGColor;
    
    NSProgressIndicator *indicator = [self createLoadingIndicator];
    [loadingView addSubview:indicator];
    
    [NSLayoutConstraint activateConstraints:@[
        [indicator.centerXAnchor constraintEqualToAnchor:loadingView.centerXAnchor],
        [indicator.centerYAnchor constraintEqualToAnchor:loadingView.centerYAnchor]
    ]];
    
    return loadingView;
}

- (NSView *)createErrorView {
    NSView *errorView = [[NSView alloc] initWithFrame:NSZeroRect];
    errorView.wantsLayer = YES;
    errorView.layer.backgroundColor = NSColor.whiteColor.CGColor;
    
    NSTextField *errorLabel = [self createErrorLabel];
    [errorView addSubview:errorLabel];
    
    [NSLayoutConstraint activateConstraints:@[
        [errorLabel.centerXAnchor constraintEqualToAnchor:errorView.centerXAnchor],
        [errorLabel.centerYAnchor constraintEqualToAnchor:errorView.centerYAnchor]
    ]];
    
    return errorView;
}

- (void)fetchContextCodeWithPageName:(NSString *)pageName 
                      resultCallback:(KuiklyContextCodeCallback)callback {
    if (callback != nil) {
        // TODO: 根据实际项目配置动态获取framework名称
        callback(kDefaultFrameworkName, nil);
    }
}

- (void)contentViewDidLoad {
    CFTimeInterval loadDuration = (CFAbsoluteTimeGetCurrent() - self.beginTime) * 1000.0;
    NSLog(@"[KuiklyPageViewController] Page loaded in %.2f ms", loadDuration);
}

- (void)renderViewDidCreated {
    self.beginTime = CFAbsoluteTimeGetCurrent();
}

- (void)onUnhandledException:(NSString *)exReason 
                       stack:(NSString *)callstackStr 
                        mode:(KuiklyContextMode)mode {
    NSLog(@"[UnhandledException] Reason: %@\nStack:\n%@\nMode: %ld",
          exReason, callstackStr, (long)mode);
    // TODO: 上报到监控系统
}

- (void)onPageLoadComplete:(BOOL)isSucceed 
                     error:(nullable NSError *)error 
                      mode:(KuiklyContextMode)mode {
    if (error != nil) {
        NSLog(@"[PageLoad] Failed - %@", error.localizedDescription);
    } else {
        NSLog(@"[PageLoad] Success");
    }
    
    // 获取性能数据用于分析
    id<KRPerformanceDataProtocol> performance = self.delegator.performanceManager;
    (void)performance; // 防止unused warning
}

- (NSDictionary<NSString *, NSObject *> *)contextPageData {
    return @{
        @"appId": kDefaultAppId,
        @"sysLang": NSLocale.preferredLanguages.firstObject ?: @"en",
        @"platform": kPlatformIdentifier
    };
}

- (NSString *)turboDisplayKey {
    return self.pageName;
}

#pragma mark - UI Factory Methods

- (NSProgressIndicator *)createLoadingIndicator {
    CGRect frame = CGRectMake(0, 0, kLoadingIndicatorSize, kLoadingIndicatorSize);
    NSProgressIndicator *indicator = [[NSProgressIndicator alloc] initWithFrame:frame];
    indicator.style = NSProgressIndicatorStyleSpinning;
    indicator.translatesAutoresizingMaskIntoConstraints = NO;
    [indicator startAnimation:nil];
    return indicator;
}

- (NSTextField *)createErrorLabel {
    NSTextField *label = [NSTextField labelWithString:@"加载失败"];
    label.translatesAutoresizingMaskIntoConstraints = NO;
    label.textColor = NSColor.redColor;
    label.font = [NSFont systemFontOfSize:kErrorLabelFontSize];
    label.alignment = NSTextAlignmentCenter;
    return label;
}

@end
