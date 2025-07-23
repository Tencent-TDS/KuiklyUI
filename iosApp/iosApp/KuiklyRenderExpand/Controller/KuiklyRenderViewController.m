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
//#import <Bugly/Bugly.h>

#import "KRConvertUtil.h"
#import "UINavigationController+FDFullscreenPopGesture.h"


@interface Delegator  : NSObject<KRControllerDelegatorLifeCycleProtocol>


@end

@implementation Delegator
@synthesize delegator;
/// 对齐所在VC的viewDidLoad时机
- (void)viewDidLoad {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView将要创建时调用
- (void)willInitRenderView {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView创建完成后调用
- (void)didInitRenderView {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// kuiklyRenderView被成功发送事件时调用
- (void)didSendEvent:(NSString *)event {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewWillAppear时机
- (void)viewWillAppear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewDidAppear时机
- (void)viewDidAppear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewWillDisappear时机
- (void)viewWillDisappear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 对齐所在VC的viewDidDisappear时机
- (void)viewDidDisappear {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 将要获取上下文代码时回调
- (void)willFetchContextCode {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 完成获取上下文代码时回调
- (void)didFetchContextCode {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// 内容完成出来时回调用(已上屏)
- (void)contentViewDidLoad {
    NSLog(@"delegator:%s", __FUNCTION__);
}
/// delegator dealloc时调用
- (void)delegatorDealloc {
    NSLog(@"delegator:%s", __FUNCTION__);
}

@end


#define KRWeakSelf __weak typeof(self) weakSelf = self;
@interface KuiklyRenderViewController()<KuiklyRenderViewControllerBaseDelegatorDelegate>

@property (nonatomic, strong) KuiklyRenderViewControllerBaseDelegator *delegator;

@end

@implementation KuiklyRenderViewController {
    NSString *_pageName;
    NSDictionary *_pageData;
    CFTimeInterval _beginTime;
    Delegator * _delegatorProxy;
}

- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData {
    if (self = [super init]) {
        _pageName = pageName;
        pageData = [self p_mergeExtParamsWithOriditalParam:pageData];
        _pageData = pageData;
        _delegatorProxy = [Delegator new];
        _delegator = [[KuiklyRenderViewControllerBaseDelegator alloc] initWithPageName:pageName pageData:pageData];
        [_delegator.performanceManager setMonitorType:KRMonitorType_ALL];
        _delegator.delegate = self;
        [_delegator addDelegatorLifeCycleListener:_delegatorProxy];
        
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleKuiklyException:) name:kKuiklyFatalExceptionNotification object:nil];
    }
    return self;
}

- (void)handleKuiklyException:(NSNotification *)noti {
    if (noti.userInfo && noti.userInfo[@"exception"]) {
        NSString *exceptionString = noti.userInfo[@"exception"];
        NSArray *components = [exceptionString componentsSeparatedByString:@"\n"];
        NSString *exceptionName = [components firstObject];

        NSArray<NSString *> *callStackArray = [components subarrayWithRange:NSMakeRange(1, components.count - 1)];
        // bugly上报示例
        //        [Bugly reportExceptionWithCategory:7 name:exceptionName reason:exceptionName callStack:callStackArray extraInfo:@{} terminateApp:YES];
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    
    self.fd_prefersNavigationBarHidden = YES;
    self.view.backgroundColor = [UIColor whiteColor];
    [_delegator viewDidLoadWithView:self.view];
    [self.navigationController setNavigationBarHidden:YES animated:NO];

}


- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [_delegator viewDidLayoutSubviews];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [_delegator viewWillAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [_delegator viewDidAppear];
    [self.navigationController setNavigationBarHidden:YES animated:NO];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [_delegator viewWillDisappear];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [_delegator viewDidDisappear];
    
    KRPerformanceManager *manager = [_delegator performanceManager];
    NSDictionary *startTimes = manager.stageStartTimes;
    NSDictionary *durations = manager.stageDurations;
    KRMemoryMonitor *mem = [manager memoryMonitor];
    KRFPSMonitor *fps1 = [manager mainFPS];
    KRFPSMonitor *fps2 = [manager kotlinFPS];
    
    int m = [mem avgIncrementMemory] / 1024 / 1024;
    float fps11 = fps1.avgFPS;
    float fps22 = fps2.avgFPS;
    NSLog(@"xxxx, %i, %f %f", m, fps11, fps22);
    NSLog(@"xxxx, start: %@ \n duration%@", startTimes.description, durations.description);

}

- (void)renderViewDidCreated {
    _beginTime = CFAbsoluteTimeGetCurrent();
}

- (void)onUnhandledException:(NSString *)exReason stack:(NSString *)callstackStr mode:(KuiklyContextMode)mode
{
    // report to bugly
}

- (void)onPageLoadComplete:(BOOL)isSucceed error:(NSError *)error mode:(KuiklyContextMode)mode {
    if (error) {
        
    }
    
    id<KRPerformanceDataProtocol> performance = _delegator.performanceManager;
    // 获取performance相关信息
}

#pragma mark - private

- (NSDictionary *)p_mergeExtParamsWithOriditalParam:(NSDictionary *)pageParam {
    NSMutableDictionary *mParam = [(pageParam ?: @{}) mutableCopy];
 
    return mParam;
}

#pragma mark - KuiklyRenderViewControllerDelegatorDelegate

- (UIView *)createLoadingView {
    UIView *loadingView = [[UIView alloc] init];
    loadingView.backgroundColor = [UIColor whiteColor];
    return loadingView;
}


- (UIView *)createErrorView {
    UIView *errorView = [[UIView alloc] init];
    errorView.backgroundColor = [UIColor whiteColor];
    return errorView;
}

- (void)fetchContextCodeWithPageName:(NSString *)pageName resultCallback:(KuiklyContextCodeCallback)callback {
    if (callback) {
        // 返回对应framework名字
        callback(@"shared", nil);
    }
}


- (void)contentViewDidLoad {
    NSLog(@"pageCostTime:%.2lf", (CFAbsoluteTimeGetCurrent() - _beginTime) * 1000.0);
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}


- (NSDictionary<NSString *,NSObject *> *)contextPageData {
    return @{@"dd": @(1)};
}

- (NSString *)turboDisplayKey {
    return _pageName;
}


@end
