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
#import "KuiklyRenderView.h"

typedef void (^KuiklyContextCodeCallback)(NSString * _Nullable contextCode, NSError * _Nullable error);
@protocol KRPerformanceDataProtocol;

NS_ASSUME_NONNULL_BEGIN
/** Snapshot页面快照Key. */
UIKIT_EXTERN NSString *const KRPageDataSnapshotKey;
@protocol KuiklyRenderViewControllerBaseDelegatorDelegate;
@protocol KRControllerDelegatorLifeCycleProtocol;
/*
 * @brief 接入层对接的页面级粒度入口类，KuiklyRenderViewController对应的真正实现类，如要对接view粒度，请查看使用KuiklyBaseView.h
 */
@interface KuiklyRenderViewControllerBaseDelegator : NSObject
/// delegator代理
@property(nonatomic, weak) id<KuiklyRenderViewControllerBaseDelegatorDelegate> delegate;
/// 渲染根视图
@property (nonatomic, strong, readonly, nullable) KuiklyRenderView *renderView;
/// kuikly 标准的性能数据
@property (nonatomic, strong) id<KRPerformanceDataProtocol> performanceManager;

/*
 * @brief 创建实例对应的初始化方法.
 * @param pageName 页面名 （对应的值为kotlin侧页面注解 @Page("xxxx")中的xxx名）
 * @param params 页面对应的参数（kotlin侧可通过pageData.params获取）
 * @param frameworkName kuikly kmm工程打包的framework名字，如shared.framework,则传入 @"shared"（注：可以通过fetchContextCode接口传入）
 * @return 返回KuiklyRenderViewControllerBaseDelegator实例
 */
- (instancetype)initWithPageName:(NSString *)pageName
                        pageData:(NSDictionary *)pageData
                   frameworkName:(NSString * _Nullable)frameworkName;
- (instancetype)initWithPageName:(NSString *)pageName pageData:(NSDictionary *)pageData;

- (instancetype)init NS_UNAVAILABLE;
+ (instancetype)new NS_UNAVAILABLE;

- (void)viewDidLoadWithView:(UIView *)view;
/*
 * @brief ViewController的viewDidLayoutSubviews时机调用该方法.
 */
- (void)viewDidLayoutSubviews;
/*
 * @brief ViewController的viewWillAppear时机调用该方法.
 */
- (void)viewWillAppear;
/*
 * @brief ViewController的viewDidAppear时机调用该方法.
 */
- (void)viewDidAppear;
/*
 * @brief ViewController的viewWillDisappear时机调用该方法.
 */
- (void)viewWillDisappear;
/*
 * @brief ViewController的viewDidDisappear时机调用该方法.
 */
- (void)viewDidDisappear;
/*
 * @brief 发送事件到KuiklyKotlin侧（支持多线程调用, 非主线程时为同步通信，建议主线程调用）.
 *        注：kotlin侧通过pager的addPagerEventObserver方法监听接收
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data;
/*
 * @brief 添加对Delegator的生命周期时机监听，实现自定义hook
 * @param lifeCycleListener 监听者（内部弱引用该对象）
 */
- (void)addDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener;
/*
 * @brief 删除对Delegator的生命周期时机监听
 * @param lifeCycleListener 要移除的监听者
 */
- (void)removeDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener;
/*
 * @brief 判断PageName是否存在于当前Framework中
 * @param pageName 对应Kotin测@Page注解名字
 * @param frameworkName 编译出来的framework名字
 * @return 是否存在该页面
 */
+ (BOOL)isPageExistWithPageName:(NSString *)pageName frameworkName:(NSString *)frameworkName;

@end

@protocol KuiklyRenderViewControllerBaseDelegatorDelegate<NSObject>
@optional
/*
 * @breif 拉取pageName对应的contextCode (接入方需要实现该方法)
     注意：contextCode值为framework名，如shared.framework,则为@"shared"
 */
- (void)fetchContextCodeWithPageName:(NSString *)pageName resultCallback:(KuiklyContextCodeCallback)callback;
/*
 * @brief fetchContextCodeWithPageName过程中会创建该自定义loadingView
 */
- (UIView *)createLoadingView;
/*
 * @brief fetchContextCodeWithPageName回调失败会创建该自定义loadingView
 */
- (UIView *)createErrorView;
/*
 * @breif 内容视图被加载出来时回调(可当作Kuikly首屏内容完成上屏时机)
 */
- (void)contentViewDidLoad;
/*
 * @breif 根视图被创建完时回调（可作为Kuikly首屏耗时打点的起点时机）
 */
- (void)renderViewDidCreated;
/*
 * @breif 页面中的scrollView didLayout时回调
 */
- (void)scrollViewDidLayout:(UIScrollView *)scrollView;
/*
 * @brief 透传到Kotlin Pager.PageData的上下文扩展数据，该数据会和pageData合并
 * 注：该方法调用时机为fetchContextCodeWithPageName之后，KuiklyRenderView初始化前。
 * @return 字典类型数据，其中value仅支持基础数据结构，如NSString，NSNumber，NSArray，NSDictionary等可被json序列化数据结构
 */
- (NSDictionary<NSString *, NSObject *> * _Nullable)contextPageData;
/*
 * @breif 发生未处理的kotlin代码异常时回调，回调完后，直接crash
 * @param exReason, 异常原因, 如ThrowArrayIndexOutOfBoundsException
 * @param callstackStr, 导致异常的kotlin代码堆栈，分隔符: \n
 * @param mode, 当前的产物类型
 */
- (void)onUnhandledException:(NSString *)exReason
                       stack:(NSString *)callstackStr
                        mode:(KuiklyContextMode)mode;
/*
 * @breif 首屏加载完成回调
 * @param isSucceed 首屏是否正常加载
 * @param error 首屏加载错误
 * @param mode, 当前的产物类型
 */
- (void)onPageLoadComplete:(BOOL)isSucceed
                     error:(nullable NSError *)error
                      mode:(KuiklyContextMode)mode;

/*
 * @brief 首屏是否同步渲染（默认framework模式为同步方式）
 * @return 是否同步渲染首屏
 */
- (BOOL)syncRenderingWhenPageAppear;

/*
 * @brief 打开TurboDisplay渲染模式技术，实现超原生首屏性能
        （通过直接执行dai二进制产物渲染生成首屏，避免业务代码执行后再生成的首屏等待耗时）
 *
 *注意：如果首屏不精准，可在kotin侧需要通过调用TurboDisplayModule.setCurrentUIAsFirstScreenForNextLaunch()方法生成指定帧二进制产物作为下次首屏
 * @return 返回该页面的TurboDisplayKey（一般可为PageName，若为nil，则为关闭TurboDisplay渲染模式）
 */
- (NSString * _Nullable)turboDisplayKey;

@end





/*** KuiklyRenderViewControllerDelegator生命周期协议  */

@protocol KRControllerDelegatorLifeCycleProtocol <NSObject>

@optional

@property (nonatomic, weak) KuiklyRenderViewControllerBaseDelegator *delegator;
/// 对齐所在VC的viewDidLoad时机
- (void)viewDidLoad;
/// 对齐所在VC的viewDidLayoutSubviews时机
- (void)viewDidLayoutSubviews;
/// kuiklyRenderView将要创建时调用
- (void)willInitRenderView;
/// kuiklyRenderView创建完成后调用
- (void)didInitRenderView;
/// kuiklyRenderView被成功发送事件时调用
- (void)didSendEvent:(NSString *)event;
/// 对齐所在VC的viewWillAppear时机
- (void)viewWillAppear;
/// 对齐所在VC的viewDidAppear时机
- (void)viewDidAppear;
/// 对齐所在VC的viewWillDisappear时机
- (void)viewWillDisappear;
/// 对齐所在VC的viewDidDisappear时机
- (void)viewDidDisappear;
/// 将要获取上下文代码时回调
- (void)willFetchContextCode;
/// 完成获取上下文代码时回调
- (void)didFetchContextCode;
/// 内容完成出来时回调用(已上屏)
- (void)contentViewDidLoad;
/// delegator dealloc时调用
- (void)delegatorDealloc;
/// 进入前台时调用(注:得在当前页面可见时才会调用进入前台, 页面不可见时不调用)
- (void)onReceiveApplicationDidBecomeActive;
/// 进入后台时调用(注:得在当前页面可见时才会调用进入后台, 页面不可见时不调用)
- (void)onReceiveApplicationWillResignActive;


@end


NS_ASSUME_NONNULL_END

