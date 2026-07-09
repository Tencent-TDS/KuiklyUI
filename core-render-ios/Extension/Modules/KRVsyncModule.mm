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

#import "KRVsyncModule.h"
#import "KuiklyRenderThreadManager.h"
#include <TargetConditionals.h>
#if !TARGET_OS_OSX
#import <UIKit/UIKit.h>
#endif

#if !TARGET_OS_OSX

// 内部对象持有 CADisplayLink,避免 CADisplayLink 强引用 module 导致无法释放
@interface _KRVsyncDisplayLink : NSObject

@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, copy) void (^callback)(NSTimeInterval timestamp);

@end

@implementation _KRVsyncDisplayLink

- (void)startWithPreferredFPS:(NSInteger)fps {
    [self stop];
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(onVsyncTick:)];
    if (@available(iOS 15.0, *)) {
        // 自适应区间: 动画时跑满屏幕最大刷新率,静止时允许系统降到 60Hz 以省电
        CGFloat minFPS = MIN(60, fps);
        self.displayLink.preferredFrameRateRange = CAFrameRateRangeMake(minFPS, fps, fps);
    } else {
        self.displayLink.preferredFramesPerSecond = fps;
    }
    [self.displayLink addToRunLoop:NSRunLoop.mainRunLoop forMode:NSRunLoopCommonModes];
}

- (void)onVsyncTick:(CADisplayLink *)link {
    if (self.callback) {
        self.callback(link.timestamp);
    }
}

- (void)stop {
    [self.displayLink invalidate];
    self.displayLink = nil;
}

@end

#endif

@implementation KRVsyncModule
{
    KuiklyRenderCallback _tipCb;
#if TARGET_OS_OSX
    dispatch_source_t _kotlinTimer;
#else
    _KRVsyncDisplayLink *_vsyncDisplayLink;
#endif
}

#if TARGET_OS_OSX

- (void)registerVsync:(NSDictionary *)args {
    _tipCb = args[KR_CALLBACK_KEY];

    dispatch_queue_t contextQueue = [KuiklyRenderThreadManager contextQueue];

    if (!contextQueue) {
        return ;
    }
    [self invalidateTimer];
    _kotlinTimer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0, 0, contextQueue);
    dispatch_source_set_timer(_kotlinTimer, DISPATCH_TIME_NOW, NSEC_PER_SEC / 60.0, NSEC_PER_MSEC);
    __weak __typeof__(self) wself = self;
    dispatch_source_set_event_handler(_kotlinTimer, ^{
        __strong __typeof__(self) sself = wself;
        [sself vsyncFire];
    });
    dispatch_resume(_kotlinTimer);
}

- (void)vsyncFire {
    if (_tipCb) {
        _tipCb(@{});
    }
}

- (void)invalidateTimer {
    if (_kotlinTimer) {
        dispatch_source_cancel(_kotlinTimer);
        _kotlinTimer = nil;
    }
}

- (void)unRegisterVsync:(NSDictionary *)args {
    [self invalidateTimer];
}

#else

// iOS: CADisplayLink 驱动,跟随屏幕最大刷新率(ProMotion 120Hz)。
// 需配合 Info.plist 的 CADisableMinimumFrameDurationOnPhone,否则 iPhone 上仍被钳制在 60Hz。
- (void)registerVsync:(NSDictionary *)args {
    _tipCb = args[KR_CALLBACK_KEY];
    __weak __typeof__(self) wself = self;
    // CADisplayLink 需要挂到主 RunLoop
    [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
        __strong __typeof__(self) sself = wself;
        [sself p_startDisplayLinkOnMainThread];
    } sync:NO];
}

- (void)p_startDisplayLinkOnMainThread {
    if (!_tipCb) {
        return;
    }
    NSInteger maxFPS = UIScreen.mainScreen.maximumFramesPerSecond;
    if (!_vsyncDisplayLink) {
        _vsyncDisplayLink = [[_KRVsyncDisplayLink alloc] init];
    }
    __weak __typeof__(self) wself = self;
    _vsyncDisplayLink.callback = ^(NSTimeInterval timestamp) {
        // 主线程 vsync tick,派发到 context 线程触发 Kotlin 侧 renderFrame
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            __strong __typeof__(self) sself = wself;
            [sself p_vsyncFireWithTimestamp:timestamp];
        }];
    };
    [_vsyncDisplayLink startWithPreferredFPS:maxFPS];
}

- (void)p_vsyncFireWithTimestamp:(NSTimeInterval)timestamp {
    if (_tipCb) {
        // 统一以纳秒透传 vsync 时间戳,供 Kotlin 侧作为帧时钟(与 DateTime.nanoTime 同为开机时基)
        _tipCb(@{@"timestamp": @((long long)(timestamp * NSEC_PER_SEC))});
    }
}

- (void)p_stopDisplayLink {
    _KRVsyncDisplayLink *displayLink = _vsyncDisplayLink;
    _vsyncDisplayLink = nil;
    if (displayLink) {
        [KuiklyRenderThreadManager performOnMainQueueWithTask:^{
            [displayLink stop];
        } sync:NO];
    }
}

- (void)unRegisterVsync:(NSDictionary *)args {
    _tipCb = nil;
    [self p_stopDisplayLink];
}

- (void)dealloc {
    [self p_stopDisplayLink];
}

#endif

@end
