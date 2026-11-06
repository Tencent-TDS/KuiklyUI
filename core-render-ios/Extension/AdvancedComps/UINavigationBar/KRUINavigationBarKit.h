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

@class KuiklyRenderView;

#if !TARGET_OS_OSX // [macOS]

NS_ASSUME_NONNULL_BEGIN

/// UINavigationBar 标题槽是否占用 navigationItem.titleView。
BOOL KRUINavigationBarItemUsesCustomTitle(UINavigationItem *item);
void KRUINavigationBarItemSetUsesCustomTitle(UINavigationItem *item, BOOL usesCustomTitle);

/// `titleView` 从自定义槽卸下后，让页内 [KRUINavigationBar] 重新 apply 系统 title/subtitle。
void KRUINavigationBarRequestBarTitleRefresh(id hr_rootView);

/// 用 Kotlin [nativeRef] 解析 KRScrollView / KRListView（UIScrollView 子类）。
UIScrollView *_Nullable KRUINavigationBarScrollViewForNativeRef(id hr_rootView, NSInteger nativeRef);

/// iOS 27.1+ Duo 竖栏（`verticalBarEdge`）。
BOOL KRUINavigationBarUsesVerticalBar(UIViewController *viewController);

/// 竖栏 trailing 区为避开时间/Wi-Fi 列建议的下移量（pt）。
CGFloat KRUINavigationBarVerticalTrailingTopInset(UIViewController *viewController);

/// 主队列合并一次 flush（同一 runloop 内多次 setter 只执行 block 一次）。
/// `host` 用于异步块执行前校验存活，避免写已释放的 `scheduledToken`（`&_syncFlushScheduled`）。
void KRUINavigationBarScheduleCoalescedMainFlush(id host, BOOL *scheduledToken, dispatch_block_t block);

/// Trailing `itemGroup` / 排序槽位（勿复用 `UIBarButtonItem.tag`）。
NSInteger KRUINavigationBarButtonItemGroupId(UIBarButtonItem *item);
void KRUINavigationBarSetBarButtonItemGroupId(UIBarButtonItem *item, NSInteger group);
NSInteger KRUINavigationBarButtonItemSortIndex(UIBarButtonItem *item);
void KRUINavigationBarSetBarButtonItemSortIndex(UIBarButtonItem *item, NSInteger sortIndex);

/// Trailing 顶部占位 spacer 的排序键（始终排在桶内最前）。
FOUNDATION_EXPORT const NSInteger KRUINavigationBarButtonItemSortIndexTrailingTopSpacer;

/// `kr_nav_kit_*` / `kr_nav_kit_content_*` 等由 Kuikly 安装的 trailing 按钮 identifier。
BOOL KRUINavigationBarIsManagedTrailingBarItemIdentifier(NSString *identifier);
void KRUINavigationBarRegisterTrailingBarItemIdentifier(UIViewController *viewController,
                                                           NSString *itemIdentifier);
void KRUINavigationBarUnregisterTrailingBarItemIdentifier(UIViewController *viewController,
                                                           NSString *itemIdentifier);
BOOL KRUINavigationBarShouldRetainManagedTrailingBarItem(UIViewController *viewController,
                                                          NSString *itemIdentifier);
void KRUINavigationBarClearRegisteredTrailingBarItemIdentifiers(UIViewController *viewController);

NS_ASSUME_NONNULL_END

#endif
