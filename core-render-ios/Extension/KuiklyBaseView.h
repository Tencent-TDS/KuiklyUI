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
#import "KuiklyRenderViewControllerBaseDelegator.h"

NS_ASSUME_NONNULL_BEGIN

// KuiklyView代理
@protocol KuiklyViewBaseDelegate;
/*
 * @brief View粒度入口类（业务可以使用该类作为kuikly接入层入口类）
 */
@interface KuiklyBaseView : UIView
/// 渲染根视图
@property (nonatomic, strong, readonly, nullable) KuiklyRenderView *renderView;
/// kuikly 标准的性能数据
@property (nonatomic, strong, readonly) id<KRPerformanceDataProtocol> performanceData;

/*
 * @brief 创建实例对应的初始化方法.
 * @param frame 初始化frame，该值建议size保持最终尺寸，避免触发二次layout(注:frame.size不能为zero)
 * @param pageName 页面名 （对应的值为kotlin侧页面注解 @Page("xxxx")中的xxx名）
 * @param params 页面对应的参数（kotlin侧可通过pageData.params获取）
 * @param delegate 需要实现的代理(如：fetchContextCodeWithPageName方法)
 * @param frameworkName kuikly kmm工程打包的framework名字，如shared.framework,则传入 @"shared"
 * @return 返回KuiklyView实例
 */
- (instancetype)initWithFrame:(CGRect)frame
                     pageName:(NSString *)pageName
                     pageData:(NSDictionary *)pageData
                     delegate:(id<KuiklyViewBaseDelegate>)delegate
                frameworkName:(NSString * _Nullable)frameworkName;

- (instancetype)initWithFrame:(CGRect)frame
                    delegator:(nonnull KuiklyRenderViewControllerBaseDelegator *)delegator
                     delegate:(nonnull id<KuiklyViewBaseDelegate>)delegate
                frameworkName:(NSString *)frameworkName;

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)initWithFrame:(CGRect)frame NS_UNAVAILABLE;
- (instancetype)initWithCoder:(NSCoder *)coder NS_UNAVAILABLE;
+ (instancetype)new NS_UNAVAILABLE;

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



// KuiklyView代理
@protocol KuiklyViewBaseDelegate <KuiklyRenderViewControllerBaseDelegatorDelegate>
// 查看KuiklyRenderViewControllerBaseDelegatorDelegate
@end

NS_ASSUME_NONNULL_END
