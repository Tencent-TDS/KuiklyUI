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
 * @brief Kuikly页面视图控制器
 *
 * 该控制器负责macOS平台上Kuikly页面的渲染和生命周期管理。
 * 它封装了KuiklyRenderViewControllerBaseDelegator，提供了完整的
 * Kuikly SDK集成，包括性能监控、异常处理等功能。
 *
 * @discussion 使用示例：
 * @code
 * NSDictionary *pageData = @{@"userId": @"12345"};
 * KuiklyRenderViewController *pageVC = [[KuiklyRenderViewController alloc]
 *     initWithPageName:@"HomePage" pageData:pageData];
 * @endcode
 */
@interface KuiklyRenderViewController : NSViewController

/**
 * @brief 指定初始化方法
 *
 * @param pageName 页面名称，对应Kotlin侧@Page注解的值，不能为nil
 * @param pageData 页面参数字典，Kotlin侧可通过pageData.params获取，可以为nil
 * @return 初始化的KuiklyPageViewController实例
 *
 * @warning 此方法为指定初始化方法，请勿使用init或其他初始化方法
 */
- (instancetype)initWithPageName:(NSString *)pageName 
                        pageData:(nullable NSDictionary<NSString *, id> *)pageData NS_DESIGNATED_INITIALIZER;

/**
 * @brief 动态更新页面内容
 *
 * @param pageName 新的页面名称，不能为nil
 * @param pageData 新的页面参数，可以为nil
 *
 * @discussion 此方法用于在不重新创建控制器的情况下更新页面内容。
 *            只有当页面名称或数据发生变化时才会执行更新操作。
 */
- (void)updateWithPageName:(NSString *)pageName 
                  pageData:(nullable NSDictionary<NSString *, id> *)pageData;

#pragma mark - Unavailable Initializers

- (instancetype)init NS_UNAVAILABLE;
- (instancetype)initWithCoder:(NSCoder *)coder NS_UNAVAILABLE;
- (instancetype)initWithNibName:(nullable NSNibName)nibNameOrNil 
                         bundle:(nullable NSBundle *)nibBundleOrNil NS_UNAVAILABLE;

@end

NS_ASSUME_NONNULL_END

