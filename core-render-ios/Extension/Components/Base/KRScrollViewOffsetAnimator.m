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

#import "KRScrollViewOffsetAnimator.h"

@interface KRScrollViewOffsetAnimator ()

@property (nonatomic, weak) UIScrollView *scrollView;
@property (nonatomic, weak) id<KRScrollViewOffsetAnimatorDelegate> delegate;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, strong) NSDate *animationStartTime;
@property (nonatomic, assign) CGPoint fromOffset;
@property (nonatomic, assign) CGPoint toOffset;
@property (nonatomic, assign) CGPoint lastOffset;

@end

@implementation KRScrollViewOffsetAnimator

- (instancetype)initWithScrollView:(UIScrollView *)scrollView delegate:(id<KRScrollViewOffsetAnimatorDelegate>)delegate {
    self = [super init];
    if (self) {
        _scrollView = scrollView;
        _delegate = delegate;
    }
    return self;
}

- (void)dealloc {
    [self cancel];
}

- (void)cancel {
    [self.displayLink invalidate];
    self.displayLink = nil;
}

- (void)animateToOffset:(CGPoint)offset withVelocity:(CGPoint)velocity {
    [self cancel];
    self.lastOffset = [self getCurScrollContetOffset];
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateScrollViewContentOffset:)];
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
}

- (void)updateScrollViewContentOffset:(CADisplayLink *)displayLink {
    // 在动画过程中，可以通过以下方式获取当前的偏移量
    CGPoint currentOffset = [self getCurScrollContetOffset];
    if (!CGPointEqualToPoint(currentOffset, self.lastOffset)) {
        self.lastOffset = currentOffset;
        if ([self.delegate respondsToSelector:@selector(animateContentOffsetDidChanged:)]) {
            [self.delegate animateContentOffsetDidChanged:currentOffset];
        }
    }
}

- (CGPoint)getCurScrollContetOffset {
    return [(NSValue *)[self.scrollView.layer.presentationLayer valueForKeyPath:@"bounds.origin"] CGPointValue];
}

@end
