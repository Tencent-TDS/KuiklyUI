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
    

#ifndef ScrollableProtocol_h
#define ScrollableProtocol_h

#import <UIKit/UIKit.h>

/**
 * Protocol for any scrollable components inherit from UIScrollView
 */
@protocol ScrollableProtocol <UIScrollViewDelegate>

/// Return whether is horizontal, optional, default NO.
- (BOOL)horizontal;

#pragma mark - Nested Scroll Props

/// Priority of nestedScroll, see `NestedScrollCoordinator` for more
- (void)setNestedScrollPriority:(NestedScrollPriority)nestedScrollPriority;

/// Priority of nestedScroll in specific direction (finger move from bottom to top)
- (void)setNestedScrollTopPriority:(NestedScrollPriority)nestedScrollTopPriority;

/// Priority of nestedScroll in specific direction (finger move from right to left)
- (void)setNestedScrollLeftPriority:(NestedScrollPriority)nestedScrollLeftPriority;

/// Priority of nestedScroll in specific direction (finger move from top to bottom)
- (void)setNestedScrollBottomPriority:(NestedScrollPriority)nestedScrollBottomPriority;

/// Set priority of nestedScroll in specific direction (finger move from left to right)
- (void)setNestedScrollRightPriority:(NestedScrollPriority)nestedScrollRightPriority;

@end


#endif /* ScrollableProtocol_h */
