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
    

#import "KRScrollView+NestedScroll.h"
#import "NestedScrollProtocol.h"
#import "NestedScrollCoordinator.h"


@implementation KRScrollView (NestedScroll)

/// Return whether is horizontal, optional, default NO.
- (BOOL)horizontal {
    return self.alwaysBounceHorizontal;
}

#pragma mark - gesture
- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    if (self.nestedGestureDelegate &&
        gestureRecognizer == self.panGestureRecognizer &&
        [self.nestedGestureDelegate respondsToSelector:@selector(shouldRecognizeScrollGestureSimultaneouslyWithView:)]) {
        return [self.nestedGestureDelegate shouldRecognizeScrollGestureSimultaneouslyWithView:otherGestureRecognizer.view];
    }
    return NO;
}

- (BOOL)isSelfOnlyPriorityForPan:(UIPanGestureRecognizer *)pan {
    if (!self.nestedScrollCoordinator) {
        return NO;
    }
    CGPoint velocity = [pan velocityInView:pan.view];
    if (fabs(velocity.x) > fabs(velocity.y)) {
        if (velocity.x > 0 && self.nestedScrollCoordinator.nestedScrollRightPriority == NestedScrollPrioritySelfOnly) {
            return YES;
        }
        if (velocity.x < 0 && self.nestedScrollCoordinator.nestedScrollLeftPriority == NestedScrollPrioritySelfOnly) {
            return YES;
        }
    } else {
        if (velocity.y > 0 && self.nestedScrollCoordinator.nestedScrollBottomPriority == NestedScrollPrioritySelfOnly) {
            return YES;
        }
        if (velocity.y < 0 && self.nestedScrollCoordinator.nestedScrollTopPriority == NestedScrollPrioritySelfOnly) {
            return YES;
        }
    }
    return NO;
}

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer {
    if (gestureRecognizer == self.panGestureRecognizer) {
        UIPanGestureRecognizer *pan = (UIPanGestureRecognizer *)gestureRecognizer;
        // 获取手势起点
        CGPoint location = [gestureRecognizer locationInView:self];
        UIView *hitView = [self hitTest:location withEvent:nil];
        // 向上遍历，收集所有 KRScrollView
        NSMutableArray<KRScrollView *> *scrollViews = [NSMutableArray array];
        UIView *current = hitView;
        while (current && current != self) {
            if ([current isKindOfClass:[KRScrollView class]]) {
                [scrollViews addObject:(KRScrollView *)current];
            }
            current = current.superview;
        }
        // 如果子KRScrollView中有和滑动手势方向相同的SelfOnly优先级，则父KRScrollView不识别手势
        for (KRScrollView *scrollView in scrollViews) {
            if ([scrollView isSelfOnlyPriorityForPan:pan]) {
                return NO;
            }
        }
    }
    return YES;
}

#pragma mark - Nested Scroll

- (void)setNestedScrollPriority:(NestedScrollPriority)nestedScrollPriority {
    [self setupNestedScrollCoordinatorIfNeeded];
    [self.nestedScrollCoordinator setNestedScrollPriority:nestedScrollPriority];
}

- (void)setNestedScrollTopPriority:(NestedScrollPriority)nestedScrollTopPriority {
    [self setupNestedScrollCoordinatorIfNeeded];
    [self.nestedScrollCoordinator setNestedScrollTopPriority:nestedScrollTopPriority];
}

- (void)setNestedScrollLeftPriority:(NestedScrollPriority)nestedScrollLeftPriority {
    [self setupNestedScrollCoordinatorIfNeeded];
    [self.nestedScrollCoordinator setNestedScrollLeftPriority:nestedScrollLeftPriority];
}

- (void)setNestedScrollBottomPriority:(NestedScrollPriority)nestedScrollBottomPriority {
    [self setupNestedScrollCoordinatorIfNeeded];
    [self.nestedScrollCoordinator setNestedScrollBottomPriority:nestedScrollBottomPriority];
}

- (void)setNestedScrollRightPriority:(NestedScrollPriority)nestedScrollRightPriority {
    [self setupNestedScrollCoordinatorIfNeeded];
    [self.nestedScrollCoordinator setNestedScrollRightPriority:nestedScrollRightPriority];
}

- (void)setupNestedScrollCoordinatorIfNeeded {
    if (!self.nestedScrollCoordinator) {
        self.nestedScrollCoordinator = [NestedScrollCoordinator new];
        self.nestedScrollCoordinator.innerScrollView = self;
        self.nestedGestureDelegate = self.nestedScrollCoordinator;
        [self addScrollViewDelegate:self.nestedScrollCoordinator];
    }
}


@end
