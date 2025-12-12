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

#import "UIViewController+KRModalDismissGesture.h"
#import <objc/runtime.h>

static const char kKRModalDismissPanGestureKey;
static const char kKRIsDismissingKey;
static const char kKRModalDismissGestureEnabledKey;

@implementation UIViewController (KRModalDismissGesture)

#pragma mark - Associated Objects

- (UIPanGestureRecognizer *)kr_dismissPanGesture {
    return objc_getAssociatedObject(self, &kKRModalDismissPanGestureKey);
}

- (void)kr_setDismissPanGesture:(UIPanGestureRecognizer *)gesture {
    objc_setAssociatedObject(self, &kKRModalDismissPanGestureKey, gesture, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)kr_isDismissing {
    NSNumber *number = objc_getAssociatedObject(self, &kKRIsDismissingKey);
    return number ? [number boolValue] : NO;
}

- (void)kr_setIsDismissing:(BOOL)isDismissing {
    objc_setAssociatedObject(self, &kKRIsDismissingKey, @(isDismissing), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)kr_isModalDismissGestureEnabled {
    NSNumber *number = objc_getAssociatedObject(self, &kKRModalDismissGestureEnabledKey);
    return number ? [number boolValue] : YES; // 默认启用
}

- (void)kr_setModalDismissGestureEnabled:(BOOL)enabled {
    objc_setAssociatedObject(self, &kKRModalDismissGestureEnabledKey, @(enabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    if (enabled && ![self kr_dismissPanGesture]) {
        [self kr_setupModalDismissGesture];
    } else if (!enabled && [self kr_dismissPanGesture]) {
        [self kr_removeModalDismissGesture];
    }
}

#pragma mark - Public Methods

- (void)kr_setupModalDismissGesture {
    if ([self kr_dismissPanGesture]) {
        return; // 已经设置过了
    }
    
    UIPanGestureRecognizer *panGesture = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(kr_handleDismissPanGesture:)];
    panGesture.delegate = self;
    panGesture.maximumNumberOfTouches = 1;
    [self.view addGestureRecognizer:panGesture];
    [self kr_setDismissPanGesture:panGesture];
}

- (void)kr_removeModalDismissGesture {
    UIPanGestureRecognizer *gesture = [self kr_dismissPanGesture];
    if (gesture) {
        [self.view removeGestureRecognizer:gesture];
        [self kr_setDismissPanGesture:nil];
    }
}

#pragma mark - Gesture Handler

- (void)kr_handleDismissPanGesture:(UIPanGestureRecognizer *)gesture {
    // 只在 modal 展示模式下才支持横滑退出
    if (self.presentingViewController == nil) {
        return;
    }
    
    CGPoint translation = [gesture translationInView:self.view];
    CGPoint velocity = [gesture velocityInView:self.view];
    
    switch (gesture.state) {
        case UIGestureRecognizerStateBegan: {
            [self kr_setIsDismissing:NO];
            break;
        }
        case UIGestureRecognizerStateChanged: {
            // 只处理向右滑动（translation.x > 0）
            if (translation.x > 0) {
                CGFloat progress = translation.x / self.view.bounds.size.width;
                progress = MIN(MAX(progress, 0), 1);
                
                // 更新 view 的位置
                self.view.transform = CGAffineTransformMakeTranslation(translation.x, 0);
                
                // 更新背景透明度
                if (self.presentingViewController.view) {
                    CGFloat alpha = 1.0 - progress * 0.5; // 背景透明度从 1.0 降到 0.5
                    self.presentingViewController.view.alpha = alpha;
                }
                
                [self kr_setIsDismissing:YES];
            }
            break;
        }
        case UIGestureRecognizerStateEnded:
        case UIGestureRecognizerStateCancelled: {
            CGFloat progress = translation.x / self.view.bounds.size.width;
            BOOL shouldDismiss = NO;
            
            // 判断是否应该 dismiss：滑动距离超过屏幕宽度的 50% 或速度足够快
            if (progress > 0.5 || velocity.x > 500) {
                shouldDismiss = YES;
            }
            
            if (shouldDismiss && [self kr_isDismissing]) {
                // 执行 dismiss 动画
                [self kr_dismissWithAnimation:YES];
            } else {
                // 回弹动画
                [self kr_cancelDismissAnimation];
            }
            
            [self kr_setIsDismissing:NO];
            break;
        }
        default:
            break;
    }
}

#pragma mark - Animation

- (void)kr_dismissWithAnimation:(BOOL)animated {
    if (animated) {
        [UIView animateWithDuration:0.3
                              delay:0
                            options:UIViewAnimationOptionCurveEaseOut
                         animations:^{
            self.view.transform = CGAffineTransformMakeTranslation(self.view.bounds.size.width, 0);
            if (self.presentingViewController.view) {
                self.presentingViewController.view.alpha = 1.0;
            }
        } completion:^(BOOL finished) {
            [self dismissViewControllerAnimated:NO completion:nil];
        }];
    } else {
        [self dismissViewControllerAnimated:NO completion:nil];
    }
}

- (void)kr_cancelDismissAnimation {
    [UIView animateWithDuration:0.3
                          delay:0
                        options:UIViewAnimationOptionCurveEaseOut
                     animations:^{
        self.view.transform = CGAffineTransformIdentity;
        if (self.presentingViewController.view) {
            self.presentingViewController.view.alpha = 1.0;
        }
    } completion:nil];
}

#pragma mark - UIGestureRecognizerDelegate

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer {
    if (gestureRecognizer == [self kr_dismissPanGesture]) {
        // 检查是否启用手势
        if (![self kr_isModalDismissGestureEnabled]) {
            return NO;
        }
        
        // 只在 modal 展示模式下才启用
        if (self.presentingViewController == nil) {
            return NO;
        }
        
        UIPanGestureRecognizer *panGesture = (UIPanGestureRecognizer *)gestureRecognizer;
        CGPoint velocity = [panGesture velocityInView:self.view];
        CGPoint translation = [panGesture translationInView:self.view];
        
        // 只响应向右滑动且横向速度/位移大于纵向
        BOOL isHorizontalSwipe = fabs(velocity.x) > fabs(velocity.y) || fabs(translation.x) > fabs(translation.y);
        BOOL isRightSwipe = velocity.x > 0 || translation.x > 0;
        
        if (!isHorizontalSwipe || !isRightSwipe) {
            return NO;
        }
        
        return YES;
    }
    return YES;
}

- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
    // 如果是我们的 dismiss 手势，不与其他手势同时识别
    if (gestureRecognizer == [self kr_dismissPanGesture]) {
        return NO;
    }
    return NO;
}

@end
