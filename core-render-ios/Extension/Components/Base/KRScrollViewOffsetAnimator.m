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
#import "KRDisplayLink.h"
#import <objc/message.h>

@interface KRScrollViewOffsetAnimator ()

@property (nonatomic, weak) UIScrollView *scrollView;
@property (nonatomic, weak) id<KRScrollViewOffsetAnimatorDelegate> delegate;
#if !TARGET_OS_OSX // [macOS]
@property (nonatomic, strong) CADisplayLink *displayLink;
#else // [macOS]
@property (nonatomic, strong) KRDisplayLink *displayLink; // [macOS]
#endif // [macOS]
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
#if TARGET_OS_OSX // [macOS]
    [self.displayLink stop];
    self.displayLink = nil;
#else // [macOS]
    [self.displayLink invalidate];
    self.displayLink = nil;
#endif // [macOS]
}

- (void)animateToOffset:(CGPoint)offset withVelocity:(CGPoint)velocity {
    [self cancel];
    self.lastOffset = [self getCurScrollContetOffset];
#if TARGET_OS_OSX // [macOS]
    KRDisplayLink *link = [KRDisplayLink new];
    __weak typeof(self) weakSelf = self;
    [link startWithCallback:^(__unused CFTimeInterval timestamp) {
        [weakSelf updateScrollViewContentOffset:nil];
    }];
    self.displayLink = link;
#else // [macOS]
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(updateScrollViewContentOffset:)];
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
#endif // [macOS]
}

- (void)updateScrollViewContentOffset:(__unused id)displayLink {
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
#if TARGET_OS_OSX // [macOS]
    NSValue *val = (NSValue *)[self.scrollView.layer.presentationLayer valueForKeyPath:@"bounds.origin"];
    return val ? [val pointValue] : self.scrollView.contentOffset;
#else // [macOS]
    return [(NSValue *)[self.scrollView.layer.presentationLayer valueForKeyPath:@"bounds.origin"] CGPointValue];
#endif // [macOS]
}

@end
