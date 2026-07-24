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
#import "NSObject+KR.h"
#import <QuartzCore/QuartzCore.h>
#include <TargetConditionals.h>

@implementation KRVsyncModule
{
    KuiklyRenderCallback _tipCb;
#if TARGET_OS_OSX
    BOOL _kotlinTimerRunning;
#else
    CADisplayLink *_displayLink;
#endif
}

- (void)registerVsync:(NSDictionary *)args {
    KuiklyRenderCallback callback = [args[KR_CALLBACK_KEY] copy];
    id params = args[KR_PARAM_KEY];
    NSDictionary *paramDictionary = nil;
    if ([params isKindOfClass:NSDictionary.class]) {
        paramDictionary = params;
    } else if ([params isKindOfClass:NSString.class]) {
        paramDictionary = [params hr_stringToDictionary];
    }
    NSInteger maxFramesPerSecond = [paramDictionary[@"maxFramesPerSecond"] integerValue];

    __weak __typeof__(self) weakSelf = self;
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
        __strong __typeof__(self) strongSelf = weakSelf;
        if (!strongSelf) {
            return;
        }
        [strongSelf invalidateVsyncOnContextThread];
        strongSelf->_tipCb = callback;
#if TARGET_OS_OSX
        strongSelf->_kotlinTimerRunning = YES;
        [strongSelf scheduleNextMacVsync];
#else
        strongSelf->_displayLink =
            [CADisplayLink displayLinkWithTarget:strongSelf selector:@selector(vsyncFire:)];
        if (maxFramesPerSecond > 0) {
            strongSelf->_displayLink.preferredFramesPerSecond = maxFramesPerSecond;
        }
        [strongSelf->_displayLink addToRunLoop:NSRunLoop.currentRunLoop forMode:NSRunLoopCommonModes];
#endif
    }];
}

#if !TARGET_OS_OSX
- (void)vsyncFire:(CADisplayLink *)displayLink {
    if (_tipCb) {
        CFTimeInterval frameInterval = MAX(0, displayLink.targetTimestamp - displayLink.timestamp);
        _tipCb(@{
            @"timestampSeconds": @(displayLink.timestamp),
            @"targetTimestampSeconds": @(displayLink.targetTimestamp),
            @"frameIntervalSeconds": @(frameInterval),
        });
    }
}
#endif

#if TARGET_OS_OSX
- (void)scheduleNextMacVsync {
    if (!_kotlinTimerRunning) {
        return;
    }
    if (_tipCb) {
        _tipCb(@{});
    }
    __weak __typeof__(self) weakSelf = self;
    [KuiklyRenderThreadManager performOnContextQueueWithTask:^{
        __strong __typeof__(self) strongSelf = weakSelf;
        [strongSelf scheduleNextMacVsync];
    } delay:1.0 / 60.0];
}
#endif

- (void)invalidateVsyncOnContextThread {
#if TARGET_OS_OSX
    _kotlinTimerRunning = NO;
#else
    [_displayLink invalidate];
    _displayLink = nil;
#endif
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
#if TARGET_OS_OSX
    _kotlinTimerRunning = NO;
#else
    [_displayLink invalidate];
#endif
    _tipCb = nil;
}

@end
