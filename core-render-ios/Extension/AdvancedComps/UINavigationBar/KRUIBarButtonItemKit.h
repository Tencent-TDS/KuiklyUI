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

/// iOS 26+ 液态玻璃 `UIBarButtonItem` 背景（编译 SDK 未声明属性时用 runtime / KVC，与 UINavigationBar 一致）。
void KRUIBarButtonItemApplyGlassBackground(UIBarButtonItem *item, BOOL sharesBackground);

/// iOS 27.1+ `UIBarButtonItem.axisBehavior`（Duo 竖栏 / 横栏）。
void KRUIBarButtonItemApplyAxis(UIBarButtonItem *item, NSString *axis);

/// 是否为 UIToolbar 上的可操作项（排除 FlexibleSpace / FixedSpace）。
BOOL KRUIToolbarBarButtonItemIsAction(UIBarButtonItem *item);

/// 对 toolbar 上所有 action 项统一刷新玻璃背景。
void KRUIToolbarSyncActionItemsChrome(NSArray<UIBarButtonItem *> *toolbarItems,
                                          UIViewController * _Nullable viewController);

NS_ASSUME_NONNULL_END

#endif
