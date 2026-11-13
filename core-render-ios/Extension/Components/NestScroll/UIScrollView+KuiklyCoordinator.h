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

NS_ASSUME_NONNULL_BEGIN
@class NestedScrollCoordinator;
// UIScrollView拓展类，用于处理与宿主的嵌套联动
@interface UIScrollView (KuiklyCoordinator)
/// parentScroll是否处于锁定状态
@property(nonatomic, assign) BOOL isParentLocked;
/// scrollview所在联动控制器
@property(nonatomic, weak) NestedScrollCoordinator *coordinator;

@end

NS_ASSUME_NONNULL_END
