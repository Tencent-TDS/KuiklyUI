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

// KRDisplayLink.m
#import "KRDisplayLink.h"
#include <TargetConditionals.h>

@interface _KRDisplayLink : NSObject

#if TARGET_OS_OSX
@property (nonatomic, strong) NSTimer *timer;
#else
@property (nonatomic, strong) CADisplayLink *displayLink;
#endif
@property (nonatomic, copy) DisplayLinkCallback callback;

@end

@implementation _KRDisplayLink

#if TARGET_OS_OSX
- (void)updateDisplayTimer:(NSTimer *)__unused timer {
    if (self.callback) {
        self.callback(CFAbsoluteTimeGetCurrent());
    }
}
#else
- (void)updateDisplay {
    if (self.callback) {
        self.callback(self.displayLink.timestamp);
    }
}
#endif

- (void)startWithCallback:(DisplayLinkCallback)callback runLoop:(NSRunLoop *)runLoop {
    [self stop];
    self.callback = callback;
#if TARGET_OS_OSX
    self.timer = [NSTimer timerWithTimeInterval:(1.0/60.0)
                                         target:self
                                       selector:@selector(updateDisplayTimer:)
                                         userInfo:nil
                                          repeats:YES];
    [runLoop addTimer:self.timer forMode:NSRunLoopCommonModes];
#else
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateDisplay)];
    [self.displayLink addToRunLoop:runLoop forMode:NSRunLoopCommonModes];
#endif
}

- (void)pause:(BOOL)pause {
#if TARGET_OS_OSX
    if (self.timer) {
        self.timer.fireDate = pause ? [NSDate distantFuture] : [NSDate dateWithTimeIntervalSinceNow:(1.0/60.0)];
    }
#else
    self.displayLink.paused = pause;
#endif
}

- (void)setPreferredFrameRateRangeWithMinimum:(CGFloat)minimum
                                      maximum:(CGFloat)maximum
                                    preferred:(CGFloat)preferred {
#if !TARGET_OS_OSX
    if (@available(iOS 15.0, *)) {
        self.displayLink.preferredFrameRateRange =
            CAFrameRateRangeMake(minimum, maximum, preferred);
    }
#endif
}

- (void)stop {
#if TARGET_OS_OSX
    [self.timer invalidate];
    self.timer = nil;
#else
    [self.displayLink invalidate];
    self.displayLink = nil;
#endif
    self.callback = nil;
}

@end


@interface KRDisplayLink ()

@property (nonatomic, strong) _KRDisplayLink *displayLink;

@end

@implementation KRDisplayLink


- (void)startWithCallback:(DisplayLinkCallback)callback {
    [self startWithCallback:callback runLoop:NSRunLoop.mainRunLoop];
}

- (void)startWithCallback:(DisplayLinkCallback)callback runLoop:(NSRunLoop *)runLoop {
    [self stop];
    self.displayLink = [[_KRDisplayLink alloc] init];
    [self.displayLink startWithCallback:callback runLoop:runLoop ?: NSRunLoop.mainRunLoop];
}

- (void)pause:(BOOL)pause {
    [self.displayLink pause:pause];
}

- (void)setPreferredFrameRateRangeWithMinimum:(CGFloat)minimum
                                      maximum:(CGFloat)maximum
                                    preferred:(CGFloat)preferred {
    [self.displayLink setPreferredFrameRateRangeWithMinimum:minimum
                                                    maximum:maximum
                                                  preferred:preferred];
}

- (void)stop {
    [self.displayLink stop];
    self.displayLink = nil;
}

- (void)dealloc {
    [self stop];
}

@end
