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
#import "KuiklyContextParam.h"
#import "KuiklyRenderThreadManager.h"
#import <QuartzCore/QuartzCore.h>
#include <TargetConditionals.h>

#if TARGET_OS_OSX
typedef NSTimer KRVsyncDisplayLink;
#else
typedef CADisplayLink KRVsyncDisplayLink;
#endif

@implementation KRVsyncModule
{
    KuiklyRenderCallback _tipCb;
    KRVsyncDisplayLink *_displayLink;
}

- (void)registerVsync:(NSDictionary *)args {
    KuiklyRenderCallback callback = [args[KR_CALLBACK_KEY] copy];
#if !TARGET_OS_OSX
    BOOL isFrameworkMode = self.hr_contextParam.contextMode.modeId == KuiklyContextMode_Framework;
#endif

    __weak __typeof__(self) weakSelf = self;
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        __strong __typeof__(self) strongSelf = weakSelf;
        if (!strongSelf) {
            return;
        }
        [strongSelf invalidateVsyncOnContextThread];
        strongSelf->_tipCb = callback;
#if TARGET_OS_OSX
        __weak __typeof__(strongSelf) weakModule = strongSelf;
        NSTimer *timer = [NSTimer timerWithTimeInterval:1.0 / 60.0
                                               repeats:YES
                                                 block:^(NSTimer *firedTimer) {
            [weakModule vsyncFire:firedTimer];
        }];
        strongSelf->_displayLink = timer;
        [NSRunLoop.currentRunLoop addTimer:timer forMode:NSRunLoopCommonModes];
#else
        CADisplayLink *displayLink =
            [CADisplayLink displayLinkWithTarget:strongSelf selector:@selector(vsyncFire:)];
        CGFloat maximumFramesPerSecond = UIScreen.mainScreen.maximumFramesPerSecond;
        if (isFrameworkMode) {
            if (@available(iOS 15.0, *)) {
                displayLink.preferredFrameRateRange = CAFrameRateRangeMake(
                    MIN(60.0, maximumFramesPerSecond),
                    maximumFramesPerSecond,
                    maximumFramesPerSecond);
            }
        } else {
            displayLink.preferredFramesPerSecond = 60;
        }
        strongSelf->_displayLink = displayLink;
        [displayLink addToRunLoop:NSRunLoop.currentRunLoop forMode:NSRunLoopCommonModes];
#endif
    }];
}

- (void)vsyncFire:(KRVsyncDisplayLink *)displayLink {
    if (_tipCb) {
#if TARGET_OS_OSX
        int32_t frameIntervalNanos = 16666667;
#else
        CADisplayLink *nativeDisplayLink = displayLink;
        CFTimeInterval frameIntervalSeconds =
            MAX(0, nativeDisplayLink.targetTimestamp - nativeDisplayLink.timestamp);
        int64_t calculatedFrameIntervalNanos =
            (int64_t)(frameIntervalSeconds * 1000000000.0 + 0.5);
        int32_t frameIntervalNanos =
            calculatedFrameIntervalNanos >= 1000000 && calculatedFrameIntervalNanos <= 100000000
                ? (int32_t)calculatedFrameIntervalNanos
                : 16666667;
#endif
        _tipCb(@(frameIntervalNanos));
    }
}

- (void)invalidateVsyncOnContextThread {
    [_displayLink invalidate];
    _displayLink = nil;
    _tipCb = nil;
}

- (void)unRegisterVsync:(NSDictionary *)args {
    __weak __typeof__(self) weakSelf = self;
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        __strong __typeof__(self) strongSelf = weakSelf;
        [strongSelf invalidateVsyncOnContextThread];
    }];
}

- (void)dealloc {
    KRVsyncDisplayLink *displayLink = _displayLink;
    _displayLink = nil;
    _tipCb = nil;
    if (displayLink) {
        [KuiklyRenderThreadManager performOnContextQueueImmediatelyWithBlock:^{
            [displayLink invalidate];
        }];
    }
}

@end
