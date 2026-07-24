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
        if (!isFrameworkMode) {
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
        CFTimeInterval timestamp = NSProcessInfo.processInfo.systemUptime * 1000.0;
        CFTimeInterval frameInterval = 1000.0 / 60.0;
        CFTimeInterval targetTimestamp = timestamp + frameInterval;
#else
        CADisplayLink *nativeDisplayLink = displayLink;
        CFTimeInterval timestamp = nativeDisplayLink.timestamp * 1000.0;
        CFTimeInterval targetTimestamp = nativeDisplayLink.targetTimestamp * 1000.0;
        CFTimeInterval frameInterval = MAX(0, targetTimestamp - timestamp);
#endif
        _tipCb(@{
            @"timestampMillis": @(timestamp),
            @"targetTimestampMillis": @(targetTimestamp),
            @"frameIntervalMillis": @(frameInterval),
        });
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
