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
#import "NestedScrollProtocol.h"

NS_ASSUME_NONNULL_BEGIN

/// A coordinator responsible for managing scroll priorities
@interface NestedScrollCoordinator : NSObject <UIScrollViewDelegate, NestedScrollGestureDelegate>

/// Priority of nestedScroll in all direction.
@property (nonatomic, assign) NestedScrollPriority nestedScrollPriority;
/// Priority of nestedScroll in specific direction (finger move from bottom to top).
@property (nonatomic, assign) NestedScrollPriority nestedScrollTopPriority;
/// Priority of nestedScroll in specific direction (finger move from right to left).
@property (nonatomic, assign) NestedScrollPriority nestedScrollLeftPriority;
/// Priority of nestedScroll in specific direction (finger move from top to bottom).
@property (nonatomic, assign) NestedScrollPriority nestedScrollBottomPriority;
/// Priority of nestedScroll in specific direction (finger move from left to right).
@property (nonatomic, assign) NestedScrollPriority nestedScrollRightPriority;

/// The inner scrollable view
@property (nonatomic, weak) UIScrollView<NestedScrollProtocol> *innerScrollView;
/// The outer scrollable view
@property (nonatomic, weak) UIScrollView<NestedScrollProtocol> *outerScrollView;

@end

NS_ASSUME_NONNULL_END
