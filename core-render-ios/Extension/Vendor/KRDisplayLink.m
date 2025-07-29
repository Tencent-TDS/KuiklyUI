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

@interface _KRDisplayLink : NSObject

@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, copy) DisplayLinkCallback callback;

@end

@implementation _KRDisplayLink

- (void)updateDisplay {
    if (self.callback) {
        self.callback(self.displayLink.timestamp);
    }
}

- (void)startWithCallback:(DisplayLinkCallback)callback {
    [self stop];
    self.callback = callback;
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateDisplay)];
    [self.displayLink addToRunLoop:NSRunLoop.mainRunLoop forMode:NSRunLoopCommonModes];
}

- (void)pause:(BOOL)pause {
    self.displayLink.paused = pause;
}

- (void)stop {
    [self.displayLink invalidate];
    self.displayLink = nil;
    self.callback = nil;
}

@end


@interface KRDisplayLink ()

@property (nonatomic, strong) _KRDisplayLink *displayLink;

@end

@implementation KRDisplayLink


- (void)startWithCallback:(DisplayLinkCallback)callback {
    [self stop];
    self.displayLink = [[_KRDisplayLink alloc] init];
    [self.displayLink startWithCallback:callback];
}

- (void)pause:(BOOL)pause {
    [self.displayLink pause:pause];
}

- (void)stop {
    [self.displayLink stop];
    self.displayLink = nil;
}

- (void)dealloc {
    [self stop];
}

@end
