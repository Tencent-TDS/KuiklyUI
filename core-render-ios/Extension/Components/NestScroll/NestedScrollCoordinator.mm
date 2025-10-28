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

#define NESTED_OPEN_BOUNCES 1 // Enable the outer bounces feature

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

static inline bool isIntersect(const UIScrollView *outerScrollView, const UIScrollView *innerScrollView) {
    CALayer *outerPresentation = outerScrollView.layer.presentationLayer;
    CALayer *innerPresentation = innerScrollView.layer.presentationLayer;
    CGRect actualOuter = [outerPresentation convertRect:outerPresentation.bounds toLayer:nil];
    CGRect actualInner = [innerPresentation convertRect:innerPresentation.bounds toLayer:nil];
    return CGRectIntersectsRect(actualOuter, actualInner);
}

static inline CGPoint clampContentOffsetToBounds(const UIScrollView *scrollView,
                                                 CGPoint contentOffset,
                                                 NestedScrollDirection direction) {
    if (direction == NestedScrollDirectionLeft || direction == NestedScrollDirectionRight) {
        // 横向滚动边界检查
        CGFloat maxContentOffsetX = scrollView.contentSize.width - scrollView.bounds.size.width + scrollView.contentInset.right;
        if (contentOffset.x > maxContentOffsetX) {
            contentOffset.x = maxContentOffsetX;
        }
    } else if (direction == NestedScrollDirectionUp || direction == NestedScrollDirectionDown) {
        // 纵向滚动边界检查
        CGFloat maxContentOffsetY = scrollView.contentSize.height - scrollView.bounds.size.height + scrollView.contentInset.bottom;
        if (contentOffset.y > maxContentOffsetY) {
            contentOffset.y = maxContentOffsetY;
        }
    }
    return contentOffset;
}

static inline void lockScrollView(const UIScrollView<NestedScrollProtocol> *scrollView,
                                  const CGPoint lastContentOffset) {
    // The value of lastContentOffset may experience redundant repeated updates,
    // primarily to avoid an infinite loop of contentOffset updates
    // caused by inconsistencies in lastContentOffset in multi-layer nested scenarios.
    scrollView.lContentOffset = lastContentOffset;
    scrollView.contentOffset = lastContentOffset;
    scrollView.isLockedInNestedScroll = YES;
}

#pragma mark - ScrollEvents Delegate

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    const UIScrollView<NestedScrollProtocol> *sv = (UIScrollView<NestedScrollProtocol> *)scrollView;    
    // Skip nested scroll lock when setting frame to avoid offset conflicts
    if ([sv isKindOfClass:[KRScrollView class]] && ((KRScrollView *)sv).skipNestScrollLock) {
        return;
    }

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
    } else if (isOuter && !outerScrollView.activeInnerScrollView) {
        if (outerScrollView.shouldHaveActiveInner) {
            // 0.1 If outer should have an active innder but not, ignore.
            NSLogTrace(@"Not active inner return 2.");
            return;
        } else if (!isIntersect(outerScrollView, innerScrollView)) {
            // 0.2 If the two ScrollViews do not intersect at all, ignore.
            NSLogTrace(@"Not Intersect return. %p", sv);
            return;
        }
    }
    
    // 1. Determine direction of scrolling
    NestedScrollDirection direction = NestedScrollDirectionNone;
    
    // 1.1 Find the right lastContentOffset
    // In multi-layer nested scenarios, the scrollable object may have more than two coordinator listeners.
    // In this case, if the first coordinator listener has already updated the lastContentOffset,
    // the subsequent coordinator listeners will not be able to process normally because the direction judgment has become invalid.
    // To handle this, our strategy is to record the original lastContentOffset through tempLastContentOffsetForMultiLayerNested,
    // thereby establishing the connection between multiple coordinators.
    BOOL shouldRecordLastContentOffsetForMultiLayerNested = YES;
    CGPoint lastContentOffset;
    if (sv.tempLastContentOffsetForMultiLayerNested) {
        lastContentOffset = [sv.tempLastContentOffsetForMultiLayerNested CGPointValue];
        sv.tempLastContentOffsetForMultiLayerNested = nil;
        shouldRecordLastContentOffsetForMultiLayerNested = NO;
        NSLogTrace(@"tempLastContentOffset Updated! lastContentOffset=%@. %p", NSStringFromCGPoint(lastContentOffset), sv);
    } else {
        lastContentOffset = sv.lContentOffset;
    }
    
    // 1.2 Do the judge to get direction
    if (lastContentOffset.y > sv.contentOffset.y) {
        direction = NestedScrollDirectionUp;
    } else if (lastContentOffset.y < sv.contentOffset.y) {
        direction = NestedScrollDirectionDown;
    } else if (lastContentOffset.x > sv.contentOffset.x) {
        direction = NestedScrollDirectionLeft;
    } else if (lastContentOffset.x < sv.contentOffset.x) {
        direction = NestedScrollDirectionRight;
    }
    if (direction == NestedScrollDirectionNone) {
        NSLogTrace(@"No direction return. %p", sv);
        return;
    }
    
    // 1.3 If it is a multi-layer nested scroll
    // and the `lastContentOffset` of the current refresh frame has not been recorded,
    // record it for the next coordinator to use.
    if (((isInner && sv.activeInnerScrollView) || (isOuter && sv.activeOuterScrollView)) &&
        !sv.tempLastContentOffsetForMultiLayerNested && shouldRecordLastContentOffsetForMultiLayerNested) {
        sv.tempLastContentOffsetForMultiLayerNested = @(sv.lContentOffset);
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
            NSLogTrace(@"lock inner (%p) to %@ !!!!", sv, NSStringFromCGPoint(lastContentOffset));
            // some times inner may generate a weired huge contentOffset, so double check it
            lastContentOffset = clampContentOffsetToBounds(innerScrollView, lastContentOffset, direction);
            lockScrollView(innerScrollView, lastContentOffset);
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
            lockScrollView(outerScrollView, lastContentOffset);
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
            lockScrollView(outerScrollView, lastContentOffset);
            NSLogTrace(@"lock outer due to cascadeLock");
            outerScrollView.cascadeLockForNestedScroll = NO;
        } else if (isInner && innerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(innerScrollView, lastContentOffset);
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
            } else if (outerScrollView.activeInnerScrollView) {
                self.shouldUnlockOuterScrollView = NO;
                NSLogTrace(@"set lock outer !");
            }
        }
        
        // Special handles the SelfOnly case
        if ([self isDirection:direction hasPriority:NestedScrollPrioritySelfOnly] &&
            self.dragType != NestedScrollDragTypeOuterOnly && isOuter) {
            self.shouldUnlockOuterScrollView = NO;
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
            isOuter && (!self.shouldUnlockOuterScrollView )) {
            NSLogTrace(@"lock outer (%p) !!!!", sv);
            lockScrollView(outerScrollView, lastContentOffset);
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
            lockScrollView(innerScrollView, lastContentOffset);
            NSLogTrace(@"lock outer due to cascadeLock");
            innerScrollView.cascadeLockForNestedScroll = NO;
        } else if (isOuter && outerScrollView.cascadeLockForNestedScroll) {
            lockScrollView(outerScrollView, lastContentOffset);
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
    if (!self.outerScrollView.shouldHaveActiveInner) {
        // Clear any recorded activeInner or activeOuter if shouldHaveActiveInner is NO,
        // this code is executed only if the scrollView is an outer.
        self.outerScrollView.activeInnerScrollView = nil;
        self.innerScrollView.activeOuterScrollView = nil;
    }
    
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
    self.dragType = NestedScrollDragTypeUndefined;
    
    // Reset shouldHaveActiveInner flag when user end dragging.
    if (self.outerScrollView.shouldHaveActiveInner) {
        self.outerScrollView.shouldHaveActiveInner = NO;
    }
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
            self.outerScrollView.shouldHaveActiveInner = YES;
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
