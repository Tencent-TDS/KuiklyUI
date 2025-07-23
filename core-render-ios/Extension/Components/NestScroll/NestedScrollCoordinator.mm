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
    

#import "NestedScrollCoordinator.h"
#import "ScrollableProtocol.h"
#import "KRScrollView.h"
#import "KRScrollView+NestedScroll.h"

// 修改NSLogTrace宏定义
//#define NSLogTrace(...) NSLog(@"nested " __VA_ARGS__)

#define NSLogTrace(...)

#define NESTED_OPEN_BOUNCES 1 // Turn off the outer bounces feature for now

typedef NS_ENUM(char, NestedScrollDirection) {
    NestedScrollDirectionNone = 0,
    NestedScrollDirectionLeft,
    NestedScrollDirectionRight,
    NestedScrollDirectionUp,
    NestedScrollDirectionDown,
};

typedef NS_ENUM(char, NestedScrollDragType) {
    NestedScrollDragTypeUndefined = 0,
    NestedScrollDragTypeOuterOnly,
    NestedScrollDragTypeBoth,
};

static CGFloat const kNestedScrollFloatThreshold = 0.1;

@interface NestedScrollCoordinator ()

/// Current drag type, used to judge the sliding order.
@property (nonatomic, assign) NestedScrollDragType dragType;

/// Whether should `unlock` the outerScrollView
/// One thing to note is the OuterScrollView may jitter in PrioritySelf mode since lock is a little bit late,
/// we need to make sure the initial state is NO to lock the outerScrollView.
@property (nonatomic, assign) BOOL shouldUnlockOuterScrollView;

/// Whether should `unlock` the innerScrollView
@property (nonatomic, assign) BOOL shouldUnlockInnerScrollView;

@end

@implementation NestedScrollCoordinator

- (void)setInnerScrollView:(UIScrollView<NestedScrollProtocol> *)innerScrollView {
    _innerScrollView = innerScrollView;
    // Disable inner's bounces when nested scroll.
    _innerScrollView.bounces = NO;
}

- (void)setOuterScrollView:(UIScrollView<NestedScrollProtocol> *)outerScrollView {
    _outerScrollView = outerScrollView;
    _outerScrollView.bounces = NO;
}


#pragma mark - Private

- (BOOL)isDirection:(NestedScrollDirection)direction hasPriority:(NestedScrollPriority)priority {
    // Note that the top and bottom defined in the nestedScroll attribute refer to the finger orientation,
    // as opposed to the page orientation.
    NestedScrollPriority presetPriority = NestedScrollPriorityUndefined;
    switch (direction) {
        case NestedScrollDirectionUp:
            presetPriority = self.nestedScrollBottomPriority;
            break;
        case NestedScrollDirectionDown:
            presetPriority = self.nestedScrollTopPriority;
            break;
        case NestedScrollDirectionLeft:
            presetPriority = self.nestedScrollRightPriority;
            break;
        case NestedScrollDirectionRight:
            presetPriority = self.nestedScrollLeftPriority;
            break;
        default:
            break;
    }
    if ((presetPriority == NestedScrollPriorityUndefined) &&
        (self.nestedScrollPriority == NestedScrollPriorityUndefined)) {
        // Default value is `PrioritySelf`.
        return (NestedScrollPrioritySelf == priority);
    }
    return ((presetPriority == NestedScrollPriorityUndefined) ?
            (self.nestedScrollPriority == priority) :
            (presetPriority == priority));
}

static inline BOOL hasScrollToTheDirectionEdge(const UIScrollView *scrollview,
                                               const NestedScrollDirection direction) {
    if (NestedScrollDirectionDown == direction) {
        return ((scrollview.contentOffset.y + CGRectGetHeight(scrollview.frame))
                >= scrollview.contentSize.height - kNestedScrollFloatThreshold);
    } else if (NestedScrollDirectionUp == direction) {
        return scrollview.contentOffset.y <= kNestedScrollFloatThreshold;
    } else if (NestedScrollDirectionLeft == direction) {
        return scrollview.contentOffset.x <= kNestedScrollFloatThreshold;
    } else if (NestedScrollDirectionRight == direction) {
        return ((scrollview.contentOffset.x + CGRectGetWidth(scrollview.frame))
                >= scrollview.contentSize.width - kNestedScrollFloatThreshold);
    }
    return NO;
}

static inline BOOL isScrollInSpringbackState(const UIScrollView *scrollview,
                                             const NestedScrollDirection direction) {
    if (NestedScrollDirectionDown == direction) {
        return scrollview.contentOffset.y <= -kNestedScrollFloatThreshold;
    } else if (NestedScrollDirectionUp == direction) {
        return (scrollview.contentOffset.y + CGRectGetHeight(scrollview.frame)
                >= scrollview.contentSize.height + kNestedScrollFloatThreshold);
    } if (NestedScrollDirectionLeft == direction) {
        return scrollview.contentOffset.x <= -kNestedScrollFloatThreshold;
    } else if (NestedScrollDirectionRight == direction) {
        return (scrollview.contentOffset.x + CGRectGetWidth(scrollview.frame)
                >= scrollview.contentSize.width - kNestedScrollFloatThreshold);
    }
    return NO;
}

static inline void lockScrollView(const UIScrollView<NestedScrollProtocol> *scrollView) {
    scrollView.contentOffset = scrollView.lContentOffset;
    scrollView.isLockedInNestedScroll = YES;
}

#pragma mark - ScrollEvents Delegate

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    const UIScrollView<NestedScrollProtocol> *sv = (UIScrollView<NestedScrollProtocol> *)scrollView;
    const UIScrollView<NestedScrollProtocol> *outerScrollView = self.outerScrollView;
    const UIScrollView<NestedScrollProtocol> *innerScrollView = self.innerScrollView;
    BOOL isOuter = (sv == outerScrollView);
    BOOL isInner = (sv == innerScrollView);
    
    NSLogTrace(@"%@(%p) did scroll: %@",
                    isOuter ? @"Outer" : @"Inner", sv,
                    isOuter ?
                    NSStringFromCGPoint(outerScrollView.contentOffset) :
                    NSStringFromCGPoint(innerScrollView.contentOffset));
    
    // 0. Exclude irrelevant scroll events using `activeInnerScrollView`
    if (outerScrollView.activeInnerScrollView &&
        outerScrollView.activeInnerScrollView != innerScrollView) {
        NSLogTrace(@"Not active inner return.");
        return;
    }
    
    // 1. Determine direction of scrolling
    NestedScrollDirection direction = NestedScrollDirectionNone;
    if (sv.lContentOffset.y > sv.contentOffset.y) {
        direction = NestedScrollDirectionUp;
    } else if (sv.lContentOffset.y < sv.contentOffset.y) {
        direction = NestedScrollDirectionDown;
    } else if (sv.lContentOffset.x > sv.contentOffset.x) {
        direction = NestedScrollDirectionLeft;
    } else if (sv.lContentOffset.x < sv.contentOffset.x) {
        direction = NestedScrollDirectionRight;
    }
    if (direction == NestedScrollDirectionNone) {
        NSLogTrace(@"No direction return. %p", sv);
        return;
    }
    
    // 2. Lock inner scrollview if necessary
    if ([self isDirection:direction hasPriority:NestedScrollPriorityParent]) {
        if (isOuter || (isInner && !self.shouldUnlockInnerScrollView)) {
            if (hasScrollToTheDirectionEdge(outerScrollView, direction)) {
                // Outer has slipped to the edge,
                // need to further determine whether the Inner can still slide
                if (hasScrollToTheDirectionEdge(innerScrollView, direction)) {
                    self.shouldUnlockInnerScrollView = NO;
                    NSLogTrace(@"set lock inner !");
                } else {
                    self.shouldUnlockInnerScrollView = YES;
                    NSLogTrace(@"set unlock inner ~");
                }
            } else {
                self.shouldUnlockInnerScrollView = NO;
                NSLogTrace(@"set lock inner !!");
            }
        }
        
        // Do lock inner action!
        if (isInner && !self.shouldUnlockInnerScrollView) {
            NSLogTrace(@"lock inner (%p) !!!!", sv);
            lockScrollView(innerScrollView);
        }
        
        // Handle the scenario where the Inner can slide when the Outer's bounces on.
        if (NESTED_OPEN_BOUNCES &&
            self.shouldUnlockInnerScrollView &&
            isOuter && sv.bounces == YES &&
            self.dragType == NestedScrollDragTypeBoth &&
            hasScrollToTheDirectionEdge(outerScrollView, direction)) {
            // When the finger is dragging, the Outer has slipped to the edge and is ready to bounce,
            // but the Inner can still slide.
            // At this time, the sliding of the Outer needs to be locked.
            lockScrollView(outerScrollView);
            NSLogTrace(@"lock outer due to inner scroll");
        }
        
        // Deal with the multi-level nesting (greater than or equal to three layers).
        // If inner has an activeInnerScrollView, that means it has a 'scrollable' nested inside it.
        // In this case, if the outer-layer locks inner, it should be passed to the outer of the inner-layer.
        if (!self.shouldUnlockInnerScrollView &&
            isOuter && innerScrollView.activeInnerScrollView) {
            innerScrollView.cascadeLockForNestedScroll = YES;
            innerScrollView.activeInnerScrollView.cascadeLockForNestedScroll = YES;
            if (outerScrollView.cascadeLockForNestedScroll) {
                outerScrollView.cascadeLockForNestedScroll = NO;
            }
            NSLogTrace(@"set cascadeLock to %p", innerScrollView);
        }
        
        // Also need to handle unlock conflicts when multiple levels are nested
        // (greater than or equal to three levels) and priorities are different.
        // When the inner of the inner-layer and the outer of outer-layer are unlocked at the same time,
        // if the inner layer has locked the outer, the outer of outer layer should be locked too.
        if (self.shouldUnlockInnerScrollView &&
            isInner && outerScrollView.activeOuterScrollView) {
            outerScrollView.activeOuterScrollView.cascadeLockForNestedScroll = YES;
        }
        
        // Do cascade lock action!
        if (isOuter && outerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(outerScrollView);
            NSLogTrace(@"lock outer due to cascadeLock");
            outerScrollView.cascadeLockForNestedScroll = NO;
        } else if (isInner && innerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(innerScrollView);
            NSLogTrace(@"lock outer due to cascadeLock");
            innerScrollView.cascadeLockForNestedScroll = NO;
        }
    }
    
    // 3. Lock outer scrollview if necessary
    else if ([self isDirection:direction hasPriority:NestedScrollPrioritySelf]
             || [self isDirection:direction hasPriority:NestedScrollPrioritySelfOnly]) {
        if (isInner || (isOuter && !self.shouldUnlockOuterScrollView)) {
            if (hasScrollToTheDirectionEdge(innerScrollView, direction)) {
                self.shouldUnlockOuterScrollView = YES;
                NSLogTrace(@"set unlock outer ~");
            } else {
                self.shouldUnlockOuterScrollView = NO;
                NSLogTrace(@"set lock outer !");
            }
        }
        
        // Handle the effect of outerScroll auto bouncing back when bounces is on.
        if (NESTED_OPEN_BOUNCES &&
            !self.shouldUnlockOuterScrollView &&
            isOuter && sv.bounces == YES &&
            self.dragType == NestedScrollDragTypeUndefined &&
            isScrollInSpringbackState(outerScrollView, direction)) {
            self.shouldUnlockOuterScrollView = YES;
        }
        
        // Do lock outer action!
        if (self.dragType != NestedScrollDragTypeOuterOnly &&
            isOuter && (!self.shouldUnlockOuterScrollView || [self isDirection:direction hasPriority:NestedScrollPrioritySelfOnly])) {
            NSLogTrace(@"lock outer (%p) !!!!", sv);
            lockScrollView(outerScrollView);
        }
        
        // Deal with the multi-level nesting (greater than or equal to three layers).
        // If the outer has an activeOuterScrollView, this means it has a scrollable nested around it.
        // At this point, if the inner-layer lock `Outer`, it should be passed to the Inner in outer-layer.
        if (isInner && !self.shouldUnlockOuterScrollView &&
            outerScrollView.activeOuterScrollView) {
            outerScrollView.cascadeLockForNestedScroll = YES;
            outerScrollView.activeOuterScrollView.cascadeLockForNestedScroll = YES;
            NSLogTrace(@"set cascadeLock to %p", innerScrollView);
        }
        
        // Do cascade lock action!
        if (isInner && innerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(innerScrollView);
            NSLogTrace(@"lock outer due to cascadeLock");
            innerScrollView.cascadeLockForNestedScroll = NO;
        } else if (isOuter && outerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(outerScrollView);
            NSLogTrace(@"lock outer due to cascadeLock");
            outerScrollView.cascadeLockForNestedScroll = NO;
        }
    }
    
    // 4. Update the lContentOffset record
    sv.lContentOffset = sv.contentOffset;
    NSLogTrace(@"end handle %@(%p) scroll -------------",
                    isOuter ? @"Outer" : @"Inner", sv);
}


- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView {
    if (scrollView == self.outerScrollView) {
        self.shouldUnlockOuterScrollView = NO;
        NSLogTrace(@"reset outer scroll lock");
    } else if (scrollView == self.innerScrollView) {
        self.shouldUnlockInnerScrollView = NO;
        NSLogTrace(@"reset inner scroll lock");
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (scrollView == self.innerScrollView) {
            // record active scroll for filtering events in scrollViewDidScroll
            self.outerScrollView.activeInnerScrollView = self.innerScrollView;
            self.innerScrollView.activeOuterScrollView = self.outerScrollView;
            
            self.dragType = NestedScrollDragTypeBoth;
        } else if (self.dragType == NestedScrollDragTypeUndefined) {
            self.dragType = NestedScrollDragTypeOuterOnly;
        }
    });
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
    if (!decelerate) {
        self.dragType = NestedScrollDragTypeUndefined;
    }
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView {
    self.dragType = NestedScrollDragTypeUndefined;
}


#pragma mark - NestedScrollGestureDelegate

- (BOOL)shouldRecognizeScrollGestureSimultaneouslyWithView:(UIView *)view {
    // Setup outer scrollview if needed
    if (!self.outerScrollView) {
        KRScrollView *scrollableView = (KRScrollView *)[self.class findNestedOuterScrollView:self.innerScrollView];
        if ([scrollableView isKindOfClass:[KRScrollView class]]) {
            [scrollableView addScrollViewDelegate:self];
            self.outerScrollView = (UIScrollView<NestedScrollProtocol> *)scrollableView;
        }
    }
    
    if (view == self.outerScrollView) {
        if (self.nestedScrollPriority > NestedScrollPriorityNone ||
            self.nestedScrollTopPriority > NestedScrollPriorityNone ||
            self.nestedScrollBottomPriority > NestedScrollPriorityNone ||
            self.nestedScrollLeftPriority > NestedScrollPriorityNone ||
            self.nestedScrollRightPriority > NestedScrollPriorityNone) {
            return YES;
        }
    } else if (self.outerScrollView.nestedGestureDelegate) {
        return [self.outerScrollView.nestedGestureDelegate shouldRecognizeScrollGestureSimultaneouslyWithView:view];
    }
    return NO;
}

#pragma mark - Utils

+ (id<ScrollableProtocol>)findNestedOuterScrollView:(UIScrollView *)innerScrollView {
    UIView<ScrollableProtocol> *innerScrollable = (UIView<ScrollableProtocol> *)innerScrollView;
    UIView *outerScrollView = innerScrollable.superview;
    while (outerScrollView) {
        if ([outerScrollView conformsToProtocol:@protocol(ScrollableProtocol)]) {
            UIView<ScrollableProtocol> *outerScrollable = (UIView<ScrollableProtocol> *)outerScrollView;
            // Make sure to find scrollable with same direction.
            BOOL isInnerHorizontal = [innerScrollable respondsToSelector:@selector(horizontal)] ? [innerScrollable horizontal] : NO;
            BOOL isOuterHorizontal = [outerScrollable respondsToSelector:@selector(horizontal)] ? [outerScrollable horizontal] : NO;
            if (isInnerHorizontal == isOuterHorizontal) {
                break;
            }
        }
        outerScrollView = outerScrollView.superview;
    }
    return (id<ScrollableProtocol>)outerScrollView;
}

@end
