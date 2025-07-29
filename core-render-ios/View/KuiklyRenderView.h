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

#import <UIKit/UIKit.h>
#import "TDFModuleProtocol.h"
#import "KuiklyRenderViewExportProtocol.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderContextProtocol.h"

NS_ASSUME_NONNULL_BEGIN

@protocol KRPerformanceDataProtocol;
@protocol KuiklyRenderViewDelegate;
@protocol KuiklyRenderLayerProtocol;
/** RootView尺寸变化事件名. */
UIKIT_EXTERN NSString *const KRRootViewSizeDidChangedEventKey;
/**
 * Kuikly by kotlin 渲染视图类，业务用该类作为对KuiklyRender的使用.
 * 注：业务接入层建议对接KuiklyRenderViewControllerDelegator类，因其对KuiklyRenderView进行了封装，所以不建议直接使用KuiklyRenderView类
 */
@interface KuiklyRenderView : UIView
/** delegate for KuiklyRenderView. */
@property (nonatomic, weak, readonly) id<KuiklyRenderViewDelegate> delegate;

/** contextParam 包含contextCode，pageName，url等信息. */
@property (nonatomic, strong) KuiklyContextParam *contextParam;

/** 异常处理block */
@property (nonatomic, strong) OnUnhandledExceptionBlock onExceptionBlock;

/*
 * @brief 创建实例对应的初始化方法.
 * @param size 初始化的视频尺寸大小.
 * @param contextCode 驱动渲染所对应的代码
        注意：该值为framework名，如shared.framework,则为@"shared"
 * @param contextParam 包含contextCode，pageName，url等信息
 * @param params 页面对应的参数（kotlin侧可通过pageData.params获取）
 * @return 返回KuiklyRenderView实例
 */
- (instancetype)initWithSize:(CGSize)size
                 contextCode:(NSString *)contextCode
                contextParam:(KuiklyContextParam *)contextParam
                      params:(NSDictionary * _Nullable)params
                    delegate:(id<KuiklyRenderViewDelegate>)delegate;
- (instancetype)init NS_UNAVAILABLE;
- (instancetype)initWithFrame:(CGRect)frame NS_UNAVAILABLE;
+ (instancetype)new NS_UNAVAILABLE;

/*
 * @brief 通过KuiklyRenderView发送事件到KuiklyKotlin侧（支持多线程调用, 非主线程时为同步通信，建议主线程调用）.
 *        注：kotlin侧通过pager的addPagerEventObserver方法监听接收
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data;

/*
 * @brief 获取模块对应的实例（仅支持在主线程调用）.
 * @param moduleName 模块名
 * @return module实例
 */
- (id<TDFModuleProtocol> _Nullable)moduleWithName:(NSString *)moduleName;

/*
 * @brief 获取tag对应的View实例（仅支持在主线程调用）.
 * @param tag view对应的索引
 * @return view实例
 */
- (id<KuiklyRenderViewExportProtocol> _Nullable)viewWithRefTag:(NSNumber *)tag;

/*
 * @brief 响应kotlin侧闭包
 * @param callbackID GlobalFunctions.createFunction返回的callback id
 * @param data 调用闭包传参
 */
- (void)fireCallbackWithID:(NSString *)callbackID data:(NSDictionary *)data;

/*
 * @brief 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
 */
- (void)syncFlushAllRenderTasks;
/*
 * @brief 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
 * @param task 主线程任务
*/
- (void)performWhenViewDidLoadWithTask:(dispatch_block_t)task;

/*
 * @brief 执行任务当RenderView销毁（仅支持在主线程调用）
 * @param task 销毁时所执行的任务
*/
- (void)performWhenRenderViewDeallocWithTask:(dispatch_block_t)task;
/*
 * @brief RenderView完全创建后调用
 */
- (void)didCreateRenderView;

@end


/*
 * @brief KuiklyRenderView对外交互的代理.
 */
@protocol KuiklyRenderViewDelegate <NSObject>

@optional

/// kuikly 标准的性能数据
@property (nonatomic, strong, readonly) id<KRPerformanceDataProtocol> performanceManager;

/*
 * @brief KuiklyRenderView中的内容视图完成加载后触发该接口调用.
 * @param KuiklyRenderView 发送代理的当前实例对象
 */
- (void)contentViewDidLoadWithrenderView:(KuiklyRenderView *)kuiklyRenderView;

/*
 * @brief KuiklyRenderView中的ScrollView完成布局后触发该接口调用.
 * @param scrollView scrollView
 * @param KuiklyRenderView 发送代理的当前实例对象
 */
- (void)scrollViewDidLayout:(UIScrollView *)scrollView renderView:(KuiklyRenderView *)kuiklyRenderView;

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

