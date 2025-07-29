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

#import "KuiklyBaseView.h"
#import "KuiklyRenderViewControllerBaseDelegator.h"

@interface KuiklyBaseView ()

@property (nonatomic, weak) id<KuiklyViewBaseDelegate> delegate;
@property (nonatomic, strong) KuiklyRenderViewControllerBaseDelegator *delegator;

@end

@implementation KuiklyBaseView

- (instancetype)initWithFrame:(CGRect)frame
                     pageName:(NSString *)pageName
                     pageData:(NSDictionary *)pageData
                     delegate:(nonnull id<KuiklyViewBaseDelegate>)delegate
                frameworkName:(NSString *)frameworkName {
    _delegator = [[KuiklyRenderViewControllerBaseDelegator alloc] initWithPageName:pageName pageData:pageData frameworkName:frameworkName];
    return [self initWithFrame:frame delegator:_delegator delegate:delegate frameworkName:frameworkName];
}

- (instancetype)initWithFrame:(CGRect)frame
                    delegator:(nonnull KuiklyRenderViewControllerBaseDelegator *)delegator
                     delegate:(nonnull id<KuiklyViewBaseDelegate>)delegate
                frameworkName:(NSString *)frameworkName {
    if (self = [super initWithFrame:frame]) {
        _delegator = delegator;
        _delegator.delegate = delegate;
        NSAssert(frame.size.width && frame.size.height, @"kuikly view init frame.size can't be zero");
        [_delegator viewDidLoadWithView:self];
    }
    return self;
}

- (KuiklyRenderView *)renderView {
    return _delegator.renderView;
}

- (id<KRPerformanceDataProtocol>)performanceData {
    return _delegator.performanceManager;
}

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [_delegator viewDidLayoutSubviews];
}
#pragma mark - public
/*
 * @brief ViewController的viewWillAppear时机调用该方法.
 */
- (void)viewWillAppear {
    [_delegator viewWillAppear];
}
/*
 * @brief ViewController的viewDidAppear时机调用该方法.
 */
- (void)viewDidAppear {
    [_delegator viewDidAppear];
}
/*
 * @brief ViewController的viewWillDisappear时机调用该方法.
 */
- (void)viewWillDisappear {
    [_delegator viewWillDisappear];
}
/*
 * @brief ViewController的viewDidDisappear时机调用该方法.
 */
- (void)viewDidDisappear {
    [_delegator viewDidDisappear];
}

/*
 * @brief 发送事件到KuiklyKotlin侧（支持多线程调用, 非主线程时为同步通信，建议主线程调用）.
 *        注：kotlin侧通过pager的addPagerEventObserver方法监听接收
 * @param event 事件名
 * @param data 事件对应的参数
 */
- (void)sendWithEvent:(NSString *)event data:(NSDictionary *)data {
    [_delegator sendWithEvent:event data:data];
}
/*
 * @brief 添加对Delegator的生命周期时机监听，实现自定义hook
 * @param lifeCycleListener 监听者（内部弱引用该对象）
 */
- (void)addDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener {
    [_delegator addDelegatorLifeCycleListener:lifeCycleListener];
}
/*
 * @brief 删除对Delegator的生命周期时机监听
 * @param lifeCycleListener 要移除的监听者
 */
- (void)removeDelegatorLifeCycleListener:(id<KRControllerDelegatorLifeCycleProtocol>)lifeCycleListener {
    [_delegator removeDelegatorLifeCycleListener:lifeCycleListener];
}
/*
 * @brief 判断PageName是否存在于当前Framework中
 * @param pageName 对应Kotin测@Page注解名字
 * @param frameworkName 编译出来的framework名字
 * @return 是否存在该页面
 */
+ (BOOL)isPageExistWithPageName:(NSString *)pageName frameworkName:(NSString *)frameworkName {
    return [KuiklyRenderViewControllerBaseDelegator isPageExistWithPageName:pageName frameworkName:frameworkName];
}


@end
