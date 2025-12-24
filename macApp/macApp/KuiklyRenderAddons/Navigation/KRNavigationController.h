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

#import <Cocoa/Cocoa.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * @brief macOS版NavigationController，模拟UINavigationController的堆栈管理
 *
 * 提供类似iOS的push/pop导航功能，使用视图控制器堆栈管理页面跳转。
 * 支持动画过渡效果，自动管理导航栏和返回按钮。
 *
 * @discussion 使用示例：
 * @code
 * KRNavigationController *navController = [[KRNavigationController alloc] initWithRootViewController:rootVC];
 * [navController pushViewController:detailVC animated:YES];
 * @endcode
 */
@interface KRNavigationController : NSViewController

#pragma mark - Properties

/**
 * @brief 视图控制器堆栈（只读）
 *
 * 堆栈底部是根视图控制器，顶部是当前显示的视图控制器
 */
@property (nonatomic, copy, readonly) NSArray<NSViewController *> *viewControllers;

/**
 * @brief 当前显示的顶部视图控制器（只读）
 */
@property (nonatomic, strong, readonly, nullable) NSViewController *topViewController;

/**
 * @brief 根视图控制器（只读）
 */
@property (nonatomic, strong, readonly, nullable) NSViewController *rootViewController;

/**
 * @brief 是否显示导航栏（默认YES）
 */
@property (nonatomic, assign) BOOL showsNavigationBar;

/**
 * @brief 导航栏高度（默认44）
 */
@property (nonatomic, assign) CGFloat navigationBarHeight;

/**
 * @brief 导航栏背景色
 */
@property (nonatomic, strong) NSColor *navigationBarBackgroundColor;

#pragma mark - Initialization

/**
 * @brief 使用根视图控制器初始化
 *
 * @param rootViewController 根视图控制器，不能为nil
 * @return 初始化的NavigationController实例
 */
- (instancetype)initWithRootViewController:(NSViewController *)rootViewController NS_DESIGNATED_INITIALIZER;

#pragma mark - Navigation Methods

/**
 * @brief 将视图控制器压入堆栈
 *
 * @param viewController 要显示的视图控制器，不能为nil
 * @param animated 是否使用动画
 */
- (void)pushViewController:(NSViewController *)viewController animated:(BOOL)animated;

/**
 * @brief 将顶部视图控制器弹出堆栈
 *
 * @param animated 是否使用动画
 * @return 被弹出的视图控制器
 */
- (nullable NSViewController *)popViewControllerAnimated:(BOOL)animated;

/**
 * @brief 弹出到根视图控制器
 *
 * @param animated 是否使用动画
 * @return 被弹出的视图控制器数组
 */
- (nullable NSArray<NSViewController *> *)popToRootViewControllerAnimated:(BOOL)animated;

/**
 * @brief 弹出到指定的视图控制器
 *
 * @param viewController 目标视图控制器，必须在堆栈中
 * @param animated 是否使用动画
 * @return 被弹出的视图控制器数组
 */
- (nullable NSArray<NSViewController *> *)popToViewController:(NSViewController *)viewController 
                                                     animated:(BOOL)animated;

/**
 * @brief 设置整个视图控制器堆栈
 *
 * @param viewControllers 新的视图控制器数组，不能为空
 * @param animated 是否使用动画
 */
- (void)setViewControllers:(NSArray<NSViewController *> *)viewControllers animated:(BOOL)animated;

#pragma mark - Unavailable Initializers

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)initWithNibName:(nullable NSNibName)nibNameOrNil 
                         bundle:(nullable NSBundle *)nibBundleOrNil NS_UNAVAILABLE;

@end

#pragma mark - NSViewController Category

/**
 * @brief NSViewController扩展，提供navigationController访问
 */
@interface NSViewController (KRNavigation)

/**
 * @brief 获取所在的NavigationController（如果有）
 */
@property (nonatomic, readonly, nullable) KRNavigationController *kr_navigationController;

@end

NS_ASSUME_NONNULL_END

