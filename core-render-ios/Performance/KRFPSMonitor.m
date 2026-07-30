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

#import "KRFPSMonitor.h"

@implementation KRFPSMonitor {
    NSTimeInterval _prevTime;
    NSUInteger _frameCount;
    
    NSUInteger _frameCountSum;
    NSTimeInterval _duration;
    KRFPSThead _thread;
    NSString *_pageName;
}

- (instancetype)initWithThread:(KRFPSThead)thread pageName:(NSString *)pageName {
    if (self = [super init]) {
        _pageName = pageName;
        _thread = thread;
        _frameCount = -1;
        _prevTime = -1;
        _maxFPS = 0;
        _minFPS = 60;
        _duration = 0;
        _frameCountSum = 0;
    }
    return self;
}

- (void)endMonitor {
    _prevTime = -1;
    _frameCount = -1;
}

- (NSUInteger)avgFPS {
    return round((double)_frameCountSum / _duration);
}

- (void)onTick:(NSTimeInterval)timestamp {
    _frameCount++;
    if (_prevTime == -1) {
        _prevTime = timestamp;
    } else if (timestamp - _prevTime >= 1) {
        _curFPS = round((double)_frameCount / (timestamp - _prevTime));
        _minFPS = MIN(_minFPS, _curFPS);
        _maxFPS = MAX(_maxFPS, _curFPS);
        _duration += timestamp - _prevTime;
        _prevTime = timestamp;
        _frameCountSum += _frameCount;
        _frameCount = 0;
        
//        NSString *threadStr = _thread == KRFPSThead_Main ? @"mainThread" : @"kotlinThead";
//        NSLog(@"【kuikly performance】%@, pagename: %@, curFPS: %lu, avgFPS: %lu, minFPS: %lu, maxFPS: %lu", threadStr,
//              _pageName, self.curFPS, self.avgFPS, self.minFPS, self.maxFPS);
    }
}

@end
