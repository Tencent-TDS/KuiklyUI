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

#import "KRContentOffsetAnimator.h"

@interface KRContentOffsetAnimator()<CAAnimationDelegate>
@property (nonatomic, weak) UIScrollView *scrollView;
@property (nonatomic, strong) CADisplayLink *displayLink;
@property (nonatomic, strong) CABasicAnimation *animation;
@property (nonatomic, copy) void (^progressBlock)(CGFloat);
@property (nonatomic, copy) void (^completionBlock)(BOOL finished);
@property (nonatomic, assign) CFTimeInterval startTs;
@property (nonatomic, assign) NSTimeInterval duration;
@property (nonatomic, assign) CGPoint startOffset;
@property (nonatomic, assign) CGPoint targetOffset;
@property (nonatomic, strong) CAMediaTimingFunction *timingFunction;
@property (nonatomic, assign) BOOL stoppedByClient;
@end

@implementation KRContentOffsetAnimator

- (instancetype)initWithScrollView:(UIScrollView *)scrollView {
    if (self = [super init]) {
        _scrollView = scrollView;
    }
    return self;
}

- (void)animateToOffset:(CGPoint)targetOffset
               duration:(NSTimeInterval)duration
         timingFunction:(CAMediaTimingFunction *)timing
            onProgress:(void (^ __nullable)(CGFloat))progress
            completion:(void (^ __nullable)(BOOL))completion {
    [self stop];
    self.stoppedByClient = NO;
    self.progressBlock = progress;
    self.completionBlock = [completion copy];
    self.duration = duration;
    self.timingFunction = timing ?: [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];

    self.startOffset = self.scrollView.contentOffset;
    self.targetOffset = targetOffset;

    // 使用 CABasicAnimation 驱动视觉层，避免立刻写模型层
    CABasicAnimation *anim = [CABasicAnimation animationWithKeyPath:@"bounds"];
    CGRect fromBounds = self.scrollView.layer.bounds;
    CGRect toBounds = fromBounds;
    // 同步支持横/竖方向：根据传入的 targetOffset.x / y 设置目标视觉位置
    toBounds.origin.x = targetOffset.x;
    toBounds.origin.y = targetOffset.y;
    anim.fromValue = [NSValue valueWithCGRect:fromBounds];
    anim.toValue = [NSValue valueWithCGRect:toBounds];
    anim.duration = duration;
    anim.timingFunction = self.timingFunction;
    anim.delegate = self;
    anim.removedOnCompletion = NO;
    anim.fillMode = kCAFillModeForwards;

    [self.scrollView.layer addAnimation:anim forKey:@"ku_contentOffset_bounds_anim"];
    self.animation = anim;

    // 启动帧回调：逐帧读取 presentationLayer 的视觉位置，设置模型层以触发 onScroll
    self.startTs = CACurrentMediaTime();
    [self startDisplayLinkIfNeeded];
}

- (void)onDisplayLinkTick {
    [self syncToPresentationIfAvailable];
    CFTimeInterval now = CACurrentMediaTime();
    if ((now - self.startTs) >= self.duration - 1e-3) {
        [self invalidateDisplayLink];
    }
}

- (void)stop {
    self.stoppedByClient = YES;
    // 在中途停止时，将模型层对齐到当前视觉位置，而不是终值
    [self syncToPresentationIfAvailable];
    [self invalidateDisplayLink];
    [self removeLayerAnimation];
    self.progressBlock = nil;
    self.completionBlock = nil;
}

#pragma mark - CAAnimationDelegate
- (void)animationDidStop:(CAAnimation *)anim finished:(BOOL)flag {
    [self invalidateDisplayLink];
    // 根据完成状态选择对齐位置：完成→终点；中断→当前视觉值
    if (flag && !self.stoppedByClient) {
        [CATransaction begin];
        [CATransaction setDisableActions:YES];
        self.scrollView.contentOffset = self.targetOffset;
        [CATransaction commit];
        if (self.completionBlock) self.completionBlock(YES);
    } else {
        [self syncToPresentationIfAvailable];
        if (self.completionBlock) self.completionBlock(NO);
    }
    self.stoppedByClient = NO;
    [self removeLayerAnimation];
    self.progressBlock = nil;
    self.completionBlock = nil;
}

#pragma mark - helpers

- (void)startDisplayLinkIfNeeded {
    if (self.displayLink) return;
    self.displayLink = [CADisplayLink displayLinkWithTarget:self selector:@selector(onDisplayLinkTick)];
    [self.displayLink addToRunLoop:[NSRunLoop mainRunLoop] forMode:NSRunLoopCommonModes];
}

- (void)invalidateDisplayLink {
    if (!self.displayLink) return;
    [self.displayLink invalidate];
    self.displayLink = nil;
}

- (void)removeLayerAnimation {
    if (!self.animation) return;
    [self.scrollView.layer removeAnimationForKey:@"ku_contentOffset_bounds_anim"];
    self.animation = nil;
}

- (BOOL)isAnimating {
    return self.animation != nil;
}

- (void)syncToPresentationIfAvailable {
    CALayer *pres = (CALayer *)self.scrollView.layer.presentationLayer;
    if (!pres) return;
    CGPoint origin = [[pres valueForKeyPath:@"bounds.origin"] CGPointValue];
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    // 将模型层同步为当前视觉层位置，支持 X/Y 方向
    [self.scrollView setContentOffset:CGPointMake(origin.x, origin.y)];
    [CATransaction commit];
}

@end
