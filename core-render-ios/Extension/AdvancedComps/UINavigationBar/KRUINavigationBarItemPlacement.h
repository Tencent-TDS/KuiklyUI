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

#import "KRUIKit.h" // [macOS]

#if !TARGET_OS_OSX // [macOS]

NS_ASSUME_NONNULL_BEGIN

/// 将 `UIBarButtonItem` 安装到 `UINavigationItem`（与 KRUIBarButtonItem / ItemContentHost 共用）。
@interface KRUINavigationBarItemPlacement : NSObject

+ (BOOL)isLeadingPlacement:(NSString *)placement;

/// 设置 navigationItem 两侧按钮组的额外避让距离；传 0 移除对应占位。
+ (void)setLeadingInset:(CGFloat)leadingInset
          trailingInset:(CGFloat)trailingInset
         viewController:(UIViewController *)viewController;

+ (void)installItem:(UIBarButtonItem *)item
         identifier:(NSString *)itemIdentifier
          placement:(NSString *)placement
              group:(NSInteger)group
              index:(NSInteger)index
     viewController:(UIViewController *)viewController;

/// `customView` 在竖栏 trailing 需先垫开状态列；`topInset > 0` 时自动插入占位 item。
+ (void)installCustomViewItem:(UIBarButtonItem *)item
                   identifier:(NSString *)itemIdentifier
                    placement:(NSString *)placement
                        group:(NSInteger)group
                        index:(NSInteger)index
               viewController:(UIViewController *)viewController
                     topInset:(CGFloat)topInset;

+ (void)uninstallItem:(nullable UIBarButtonItem *)item
           identifier:(NSString *)itemIdentifier
            placement:(NSString *)placement
       viewController:(UIViewController *)viewController;

+ (void)uninstallCustomViewItem:(nullable UIBarButtonItem *)item
                     identifier:(NSString *)itemIdentifier
                      placement:(NSString *)placement
                 viewController:(UIViewController *)viewController
                       topInset:(CGFloat)topInset;

@end

NS_ASSUME_NONNULL_END

#endif
