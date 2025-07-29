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

#import "KRMemoryMonitor.h"
#import <mach/mach.h>

@implementation KRMemoryMonitor {
    NSTimer *_timer;
    int64_t _preLoadMemory;
    
    int64_t _appAvgMemory;
    int64_t _appPeakMemory;
    int64_t _recordCount;
    
    NSString *_pageName;
}

- (instancetype)initWithPageName:(NSString *)pageName {
    if (self = [super init]) {
        _pageName = pageName;
        _preLoadMemory = [self memoryUsage];
        _appAvgMemory = 0;
        _appPeakMemory = 0;
        _recordCount = 0;
    }
    return self;
}

- (int64_t)memoryUsage {
    int64_t memoryUsageInByte = 0;
    task_vm_info_data_t vmInfo;
    mach_msg_type_number_t count = TASK_VM_INFO_COUNT;
    kern_return_t kernelReturn = task_info(mach_task_self(), TASK_VM_INFO, (task_info_t) &vmInfo, &count);
    if(kernelReturn == KERN_SUCCESS) {
        memoryUsageInByte = (int64_t) vmInfo.phys_footprint;
        NSLog(@"【kuikly performance】pagename: %@, Memory in use: %.3fMB", _pageName, memoryUsageInByte / 1024.0 / 1024);
    }
    return memoryUsageInByte;
}

- (int64_t)avgIncrementMemory {
    return _appAvgMemory - _preLoadMemory;
}

- (int64_t)peakIncrementMemory {
    return _appPeakMemory - _preLoadMemory;
}

- (void)recordCurrentMemory
{
    int64_t curMem = [self memoryUsage];
    _appPeakMemory = MAX(curMem, _appPeakMemory);
    _appAvgMemory = (_appAvgMemory * _recordCount + curMem) / (_recordCount + 1);
    _recordCount++;
    
//    NSLog(@"【kuikly performance】pagename: %@, appPeakMemory: %.3fMB, appAvgMemory: %.3fMB, peakIncrementMemory: %.3fMB, avgIncrementMemory: %.3fMB",
//          _pageName, self.appPeakMemory / 1024.0 / 1024, self.appAvgMemory / 1024.0 / 1024, self.peakIncrementMemory / 1024.0 / 1024, self.avgIncrementMemory / 1024.0 / 1024);
}

- (void)startMonitor {
    _timer = [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(recordCurrentMemory) userInfo:nil repeats:YES];
    // 启动时，记录一次
    [NSThread cancelPreviousPerformRequestsWithTarget:self selector:@selector(recordCurrentMemory) object:nil];
    [self performSelector:@selector(recordCurrentMemory) withObject:nil afterDelay:1];
}

- (void)endMonitor {
    [_timer invalidate];
    _timer = nil;
}

@end
