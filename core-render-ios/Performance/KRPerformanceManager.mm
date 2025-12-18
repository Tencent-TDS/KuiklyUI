//
//  KRPerformanceManager.m
//  KuiklyIOSRender
//
//  Created by luoyibu on 2023/7/6.
//  Copyright © 2023 Tencent. All rights reserved.
//

#import "KRPerformanceManager.h"
#import "KRFPSMonitor.h"
#import "KuiklyRenderThreadManager.h"
#import "KRMemoryMonitor.h"
#import "KuiklyContextParam.h"
#import "KRUIKit.h" // [macOS]
#import "KRDisplayLink.h" // [macOS]
#include <TargetConditionals.h>
#import <objc/message.h>
#import <pthread.h>

@interface KRPerformanceManager ()

@property (nonatomic, strong) KRFPSMonitor *kotlinFPS;
@property (nonatomic, strong) KRFPSMonitor *mainFPS;
@property (nonatomic, strong) KRMemoryMonitor *memoryMonitor;
@property (nonatomic, copy) NSString *pageName;
@property (nonatomic) BOOL isFirstLaunchOfProcess;
@property (nonatomic) BOOL isFirstLaunchOfPage;

@end

@implementation KRPerformanceManager {
    
    #if TARGET_OS_OSX // [macOS]
    KRDisplayLink *_uiDisplayLink; // KRDisplayLink on macOS
    #else
    CADisplayLink *_uiDisplayLink;
    #endif
    dispatch_source_t _kotlinTimer;
    
    NSString *_pageName;
    BOOL _isFirstLaunchOfProcess;
    BOOL _isFirstLaunchOfPage;
    KRFPSMonitor *_mainFPS;
    KRMemoryMonitor *_memoryMonitor;
    
    BOOL _isMoniting;
    
    NSDate *_pageEnterDate;
    
    pthread_rwlock_t _dataLock;
    
    NSMutableDictionary<NSNumber *, NSNumber *> *_stageStartTimes;
    NSMutableDictionary<NSNumber *, NSNumber *> *_stageDurations;
}

static int gLaunchCount = 0;
static NSMutableDictionary<NSString *, NSNumber *> *gLaunchDic = nil;

- (nonnull instancetype)initWithPageName:(nonnull NSString *)pageName {
    if (self = [super init]) {
        _pageName = pageName ?: @"";
        pthread_rwlock_init(&_dataLock, NULL);
        _stageStartTimes = [NSMutableDictionary new];
        _stageDurations = [NSMutableDictionary new];
        _memoryMonitor = [[KRMemoryMonitor alloc] initWithPageName:pageName];
        _pageEnterDate = [NSDate date];
        _pageState = KRPageState_appActive;
        
        if (gLaunchCount++ == 0) {
            _isFirstLaunchOfProcess = YES;
        }
        if (!gLaunchDic) {
            gLaunchDic = [NSMutableDictionary new];
        }
        if (!gLaunchDic[_pageName]) {
            _isFirstLaunchOfPage = YES;
            gLaunchDic[_pageName] = @(YES);
        }
        [self startStage:KRLoadStage_initView];
    }
    return self;
}

- (void)startMonitor {
    // 避免多次重复start
    if ((_pageState|KRPageState_viewDidLoad) == 0 || (_pageState|KRPageState_viewDidAppear) == 0 ||
        (_pageState|KRPageState_appActive) == 0 || _isMoniting) {
        return;
    }
    
    _isMoniting = YES;
    
    // main fps
    if ((_monitorType & KRMonitorType_MainFPS)) {
        if (!_mainFPS) {
            _mainFPS = [[KRFPSMonitor alloc] initWithThread:KRFPSThead_Main pageName:_pageName];
        }
#if TARGET_OS_OSX // [macOS]
        // macOS: 使用 KRDisplayLink 垫片（NSTimer）
        KRDisplayLink *link = [KRDisplayLink new];
        __weak typeof(self) weakSelf = self;
        [link startWithCallback:^(CFTimeInterval timestamp) {
            __strong typeof(self) self_ = weakSelf;
            if (!self_) return;
            [self_->_mainFPS onTick:timestamp];
        }];
        _uiDisplayLink = link;
#else
        _uiDisplayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(mainFPSKick:)];
        if (@available(iOS 10.0, *)) {
            _uiDisplayLink.preferredFramesPerSecond = 60;
        }
        [_uiDisplayLink addToRunLoop:[NSRunLoop currentRunLoop] forMode:NSRunLoopCommonModes];
#endif
    }

    // kotlin fps
    if ((_monitorType & KRMonitorType_KotlinFPS)) {
        if (!_kotlinFPS) {
            _kotlinFPS = [[KRFPSMonitor alloc] initWithThread:KRFPSThead_Kotlin pageName:_pageName];
        }

        _kotlinTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, [KuiklyRenderThreadManager contextQueue]);
        dispatch_source_set_timer(_kotlinTimer, DISPATCH_TIME_NOW, NSEC_PER_SEC / 60.0, NSEC_PER_MSEC);
        __weak __typeof__(self) wself = self;
        dispatch_source_set_event_handler(_kotlinTimer, ^{
            __strong __typeof__(self) sself = wself;
            NSTimeInterval now = CFAbsoluteTimeGetCurrent();
            [sself.kotlinFPS onTick:now];
        });
        dispatch_resume(_kotlinTimer);
    }
    
    if ((_monitorType & KRMonitorType_Memory)) {
        if (!_memoryMonitor) {
            _memoryMonitor = [[KRMemoryMonitor alloc] initWithPageName:_pageName];
        }
        [_memoryMonitor startMonitor];
    }
}

- (void)endMonitor {
    _isMoniting = NO;
    if ((_monitorType & KRMonitorType_MainFPS)) {
        #if TARGET_OS_OSX // [macOS]
        [_uiDisplayLink stop];
        _uiDisplayLink = nil;
        #else
        [_uiDisplayLink invalidate];
        _uiDisplayLink = nil;
        #endif
        [_mainFPS endMonitor];
    }

    // main fps
    if ((_monitorType & KRMonitorType_KotlinFPS)) {
        if (_kotlinTimer) {
            dispatch_source_cancel(_kotlinTimer);
            _kotlinTimer = nil;
        }
        [_kotlinFPS endMonitor];
    }
    
    if ((_monitorType & KRMonitorType_Memory)) {
        [_memoryMonitor endMonitor];
    }
}

#pragma mark - load time start

- (void)startStage:(KRLoadStage)stage {
    pthread_rwlock_wrlock(&_dataLock);
    if (_stageStartTimes[@(stage)]) {
        pthread_rwlock_unlock(&_dataLock);
        return;
    }
    NSTimeInterval now = [[NSDate date] timeIntervalSince1970];
    _stageStartTimes[@(stage)] = @(now * 1000);
    pthread_rwlock_unlock(&_dataLock);
    
//    NSLog(@"xxxx stage: %i, start: %f", (int)stage, now);
}

- (void)endStage:(KRLoadStage)stage {
    pthread_rwlock_wrlock(&_dataLock);
    if (_stageDurations[@(stage)]) {
        pthread_rwlock_unlock(&_dataLock);
        return;
    }
    
    NSTimeInterval now = [[NSDate date] timeIntervalSince1970];
    NSTimeInterval start = [_stageStartTimes[@(stage)] doubleValue];
    if (start) {
        int duration = (now * 1000 - start) ; // 毫秒
//        NSLog(@"xxxx stage: %i, duration: %i", (int)stage, duration);

        _stageDurations[@(stage)] = @(duration);
    }
    pthread_rwlock_unlock(&_dataLock);
}

- (void)mergeKotlinCreatePageTime:(NSDictionary *)params
{
    pthread_rwlock_wrlock(&_dataLock);
    NSTimeInterval fetchContextCodeEnd = [_stageStartTimes[@(KRLoadStage_fetchContextCode)] doubleValue] +
                                            [_stageDurations[@(KRLoadStage_fetchContextCode)] doubleValue];
    NSTimeInterval onCreateStart = [params[@"on_create_start"] doubleValue];
    NSTimeInterval onCreateEnd = [params[@"on_create_end"] doubleValue];
    NSTimeInterval onBuildStart = [params[@"on_build_start"] doubleValue];
    NSTimeInterval onBuildEnd = [params[@"on_build_end"] doubleValue];
    NSTimeInterval onLayoutStart = [params[@"on_layout_start"] doubleValue];
    NSTimeInterval onLayoutEnd = [params[@"on_layout_end"] doubleValue];
    NSTimeInterval onNewPageStart = [params[@"on_new_page_start"] doubleValue];
    NSTimeInterval onNewPageEnd = [params[@"on_new_page_end"] doubleValue];

    _stageStartTimes[@(KRLoadStage_initRenderContext)] = @(fetchContextCodeEnd);
    _stageDurations[@(KRLoadStage_initRenderContext)] = @(onNewPageStart - fetchContextCodeEnd);
    
    _stageStartTimes[@(KRLoadStage_pageBuild)] = @(onBuildStart);
    _stageDurations[@(KRLoadStage_pageBuild)] = @(onBuildEnd - onBuildStart);

    _stageStartTimes[@(KRLoadStage_pageLayout)] = @(onLayoutStart);
    _stageDurations[@(KRLoadStage_pageLayout)] = @(onLayoutEnd - onLayoutStart);

    _stageStartTimes[@(KRLoadStage_createPage)] = @(onCreateStart);
    _stageDurations[@(KRLoadStage_createPage)] = @(onCreateEnd - onCreateStart);
    
    _stageStartTimes[@(KRLoadStage_newPage)] = @(onNewPageStart);
    _stageDurations[@(KRLoadStage_newPage)] = @(onNewPageEnd - onNewPageStart);
    
    _stageStartTimes[@(KRLoadStage_createInstance)] = @(onNewPageStart);
    _stageDurations[@(KRLoadStage_createInstance)] = @(onCreateEnd - onNewPageStart);
        
    pthread_rwlock_unlock(&_dataLock);
}

- (int)durationForStage:(KRLoadStage)stage
{
    pthread_rwlock_rdlock(&_dataLock);
    int duration = [_stageDurations[@(stage)] intValue];
    pthread_rwlock_unlock(&_dataLock);
    return duration;
}

- (NSDictionary<NSNumber *,NSNumber *> *)stageDurations {
    pthread_rwlock_rdlock(&_dataLock);
    NSDictionary *copy = [[NSDictionary alloc] initWithDictionary:_stageDurations copyItems:YES];
    pthread_rwlock_unlock(&_dataLock);
    return copy;
}

- (NSDictionary<NSNumber *,NSNumber *> *)stageStartTimes {
    pthread_rwlock_rdlock(&_dataLock);
    NSDictionary *copy = [[NSDictionary alloc] initWithDictionary:_stageStartTimes copyItems:YES];
    pthread_rwlock_unlock(&_dataLock);
    return copy;
}

- (NSTimeInterval)pageExistTime {
    NSTimeInterval delta = [[NSDate date] timeIntervalSinceDate:_pageEnterDate];
    return (int)(delta * 1000);
}

#pragma mark load time end

#if !TARGET_OS_OSX
- (void)mainFPSKick:(CADisplayLink *)displayLink
{
    [_mainFPS onTick:displayLink.timestamp];
}
#endif

- (NSDictionary*)performanceData{
    NSArray *keysArray = @[@"initViewCost", @"fetchContextCodeCost", @"initRenderCoreCost", @"initRenderContextCost", @"pageBuildCost", @"pageLayoutCost", @"createPageCost", @"firstPaintCost", @"createInstanceCost", @"newPageCost", @"renderCost"];
    NSMutableDictionary *timeMap = [NSMutableDictionary new];
    NSAssert(keysArray.count == KRLoadStage_renderFP + 1, @"keys 与 枚举数量不匹配 ");
    for (int i = KRLoadStage_initView; i <= KRLoadStage_renderFP; i++) {
        int duration = [self durationForStage:(KRLoadStage)i];
        timeMap[keysArray[i]] = @(duration);
    }
    
    return @{
        @"mode": @(self.modeId),
        @"pageExistTime": @(self.pageExistTime),
        @"isFirstLaunchOfProcess": @([self isFirstLaunchOfProcess]),
        @"isFirstLaunchOfPage": @([self isFirstLaunchOfPage]),
        @"pageLoadTime": timeMap,
        @"mainFPS": @(self.mainFPS.avgFPS),
        @"kotlinFPS": @(self.kotlinFPS.avgFPS),
        @"memory": @{
            @"avgIncrement": @(self.memoryMonitor.avgIncrementMemory),
            @"peakIncrement": @(self.memoryMonitor.peakIncrementMemory),
            @"appPeak": @(self.memoryMonitor.appPeakMemory),
            @"appAvg": @(self.memoryMonitor.appAvgMemory),
        },
    };
}
@end
