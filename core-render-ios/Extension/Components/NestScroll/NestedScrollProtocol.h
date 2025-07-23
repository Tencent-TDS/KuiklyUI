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
    

#ifndef NestedScrollProtocol_h
#define NestedScrollProtocol_h

typedef NS_ENUM(char, NestedScrollPriority) {
    NestedScrollPriorityUndefined = 0,
    NestedScrollPriorityNone,
    NestedScrollPrioritySelfOnly,
    NestedScrollPrioritySelf,
    NestedScrollPriorityParent,
};

/// Delegate for handling nested scrolls' gesture conflict
@protocol NestedScrollGestureDelegate <NSObject>

/// Ask the delegate whether gesture should recognize simultaneously
/// For nested scroll
/// @param view the other view
- (BOOL)shouldRecognizeScrollGestureSimultaneouslyWithView:(UIView *)view;

@end

/// Protocol for nested scrollview
@protocol NestedScrollProtocol <NSObject>

/// Record the last content offset for scroll lock.
@property (nonatomic, assign) CGPoint lContentOffset;

/// Record the current active inner scrollable view.
/// Used to judge the responder when outer has more than one inner scrollview.
@property (nonatomic, weak) UIScrollView<NestedScrollProtocol> *activeInnerScrollView;

/// Record the current active outer scrollable view.
/// Used to pass the cascadeLock when more than three scrollable views nested.
@property (nonatomic, weak) UIScrollView<NestedScrollProtocol> *activeOuterScrollView;

/// Gesture delegate for handling nested scroll.
@property (nonatomic, weak) id<NestedScrollGestureDelegate> nestedGestureDelegate;

/// Cascade lock for nestedScroll
@property (nonatomic, assign) BOOL cascadeLockForNestedScroll;

/// Whether is temporarily locked in current DidScroll callback.
/// It is used to determine whether to block the sending of onScroll events.
@property (nonatomic, assign) BOOL isLockedInNestedScroll;

@end


#endif /* NestedScrollProtocol_h */
