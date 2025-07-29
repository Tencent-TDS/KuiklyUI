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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// 内存监控，每10秒打点一次
@interface KRMemoryMonitor : NSObject

/// 页面的平均内存增量数据: 页面可见期间的平均内存 - 页面进入前的内存， 单位：字节
@property (nonatomic, readonly) int64_t avgIncrementMemory;

/// 页面的峰值增量数据: 页面可见期间的内存峰值 - 页面进入前的内存， 单位：字节
@property (nonatomic, readonly) int64_t peakIncrementMemory;

/// 页面的内存峰值: 页面可见期间的内存峰值， 单位：字节
@property (nonatomic, readonly) int64_t appPeakMemory;

/// 页面的平均内存: 页面可见期间的平均内存， 单位：字节
@property (nonatomic, readonly) int64_t appAvgMemory;

- (instancetype)initWithPageName:(NSString *)pageName;

- (void)startMonitor;

- (void)endMonitor;

@end

NS_ASSUME_NONNULL_END
