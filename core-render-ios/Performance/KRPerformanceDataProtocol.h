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
#import "KRMemoryMonitor.h"
#import "KRFPSMonitor.h"


NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, KRLoadStage) {
    KRLoadStage_initView          = 0,      // vc从init到 调用loadview
    KRLoadStage_fetchContextCode  = 1,      // fetchContextCode
    KRLoadStage_initRenderContext   = 2,    // 初始化renderContext
    KRLoadStage_pageBuild   = 3,                // 构建业务 shadow tree 耗时，会执行各view的body函数
    KRLoadStage_pageLayout  = 4,                // 页面布局总耗时
    KRLoadStage_createPage    = 5,          // kotlin pager 页面创建（包括init、build、layout）
    KRLoadStage_fristPaint  = 6,            // 启动-首帧渲染出来总耗时
    KRLoadStage_createInstance = 7,         // 初始化环境后，createPage结束
    KRLoadStage_newPage = 8,                // kotlin侧执行newPage耗时
    KRLoadStage_renderFP = 9,               // createPage结束到首帧出来
};

typedef NS_OPTIONS(NSInteger, KRMonitorType) {
    /// 加载时间
    KRMonitorType_LoadTime  = 0,
    /// 主线程FPS
    KRMonitorType_MainFPS   = 1 << 0,
    /// kotlin线程FPS
    KRMonitorType_KotlinFPS = 1 << 1,
    /// 内存增量
    KRMonitorType_Memory    = 1 << 2,
    /// 所有监控全开
    KRMonitorType_ALL       = 0xffffffff,
};

@protocol KRPerformanceDataProtocol <NSObject>

/// 需要获取的性能数据类型
@property (nonatomic, assign) KRMonitorType monitorType;

/// 当前数据对应的pageName
@property (nonatomic, copy, readonly) NSString *pageName;

/// 页面存活时间 单位ms
@property (nonatomic, assign, readonly) NSTimeInterval pageExistTime;

/// 是否是进程第一次启动
@property (nonatomic, readonly) BOOL isFirstLaunchOfProcess;

/// 是否是页面第一次启动
@property (nonatomic, readonly) BOOL isFirstLaunchOfPage;

/// 获取各阶段的加载耗时数据 单位ms
- (int)durationForStage:(KRLoadStage)stage;

/// 主线程fps数据
@property (nonatomic, strong, readonly) KRFPSMonitor *mainFPS;

/// 获取子线程fps数据
@property (nonatomic, strong, readonly) KRFPSMonitor *kotlinFPS;

/// 内存数据
@property (nonatomic, strong, readonly) KRMemoryMonitor *memoryMonitor;

@end


NS_ASSUME_NONNULL_END
