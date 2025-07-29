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
#import <UIKit/UIKit.h>
#import "TDFModuleProtocol.h"
#import "KuiklyRenderContextProtocol.h"
#import "KuiklyRenderViewExportProtocol.h"

extern NSString *_Nonnull const kKuiklyFatalExceptionNotification;

NS_ASSUME_NONNULL_BEGIN

@class KuiklyContextParam;
@protocol KuiklyRenderCoreDelegate;
@protocol KuiklyRenderLayerProtocol;

/**
 * @brief 负责Kuikly by Kotlin渲染流程核心逻辑模块。
 */
@interface KuiklyRenderCore : NSObject

/** @brief 异常处理block */
@property (nonatomic, strong) OnUnhandledExceptionBlock onExceptionBlock;

/**
 * @brief 初始化KuiklyRenderCore实例的方法。
 * @param rootView 宿主根视图，用于渲染层内容。
 * @param contextCode 驱动渲染所对应的代码。该值为framework名，如shared.framework,则为@"shared"
 * @param contextParam 包含contextCode，pageName，url等信息的参数
 * @param params 页面对应的参数（kotlin侧可通过pageData.params获取）
 * @return 返回KuiklyRenderCore实例
 */
- (instancetype)initWithRootView:(UIView *)rootView
                     contextCode:(NSString *)contextCode
                    contextParam:(KuiklyContextParam *)contextParam
                          params:(NSDictionary *_Nullable)params
                        delegate:(id<KuiklyRenderCoreDelegate>)delegate;
/*
 * @brief 完全初始化Core之后调用
 * 注：该时机用于依赖UI组件渲染时所需要的时机，因UI组件渲染可能通过hr_rootView调用相关Api，
 *    其api会依赖core变量的存在，所以这里提供延后初始化的时机(保证hr_rootView中_core已完成初始化)
 */
- (void)didInitCore;
/**
 * @brief 通过KuiklyRenderCore发送事件到KuiklyKotlin侧（支持多线程调用，非主线程时为同步通信）。
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data;

/**
 * @brief 在Core销毁前调用，用于Core提前发送事件到KuiklyKotlin侧销毁内在资源。
 */
- (void)willDealloc;

/**
 * @brief 获取模块对应的实例（仅支持在主线程调用）。
 * @param moduleName 模块名
 * @return module实例
 */
- (id<TDFModuleProtocol> _Nullable)moduleWithName:(NSString *)moduleName;

/**
 * @brief 获取tag对应的View实例（仅支持在主线程调用）。
 * @param tag view对应的索引
 * @return view实例
 */
- (id<KuiklyRenderViewExportProtocol> _Nullable)viewWithTag:(NSNumber *)tag;

/**
 * @brief 响应kotlin侧闭包
 * @param callbackID GlobalFunctions.createFunction返回的callback id
 * @param data 调用闭包传参
 */
- (void)fireCallbackWithID:(NSString *)callbackID data:(NSDictionary *)data;

/**
 * @brief 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
 */
- (void)syncFlushAllRenderTasks;

/**
 * @brief 当首屏完成后执行任务（优化首屏性能，仅支持在主线程调用）
 * @param task 主线程任务
 */
- (void)performWhenViewDidLoadWithTask:(dispatch_block_t)task;
/**
 * @brief 收到手势响应时调用
 */
- (void)didHitTest;

@end

/*
 * @brief KuiklyRenderCoreDelegate
 */
@protocol KuiklyRenderCoreDelegate <NSObject>

@optional
/*
 * @brief 打开TurboDisplay渲染模式技术，实现超原生首屏性能
        （通过直接执行dai二进制产物渲染生成首屏，避免业务代码执行后再生成的首屏等待耗时）
 *
 *注意：如果首屏不精准，可在kotin侧需要通过调用TurboDisplayModule.setCurrentUIAsFirstScreenForNextLaunch()方法生成指定帧二进制产物作为下次首屏
 * @return 返回该页面的TurboDisplayKey（一般可为PageName，若为nil，则为关闭TurboDisplay渲染模式）
 */
- (NSString * _Nullable)turboDisplayKey;

@end

NS_ASSUME_NONNULL_END
