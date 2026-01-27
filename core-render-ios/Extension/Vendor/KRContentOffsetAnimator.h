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
#import "KRUIKit.h" // [macOS]

NS_ASSUME_NONNULL_BEGIN

static const CGFloat KRMaxAllowedDistance = 6500;
static const float KRContentOffsetAnimatorP1x = 0.20f;
static const float KRContentOffsetAnimatorP1y = 1.0f;
static const float KRContentOffsetAnimatorP2x = 0.48f;
static const float KRContentOffsetAnimatorP2y = 1.0f;

@interface KRContentOffsetAnimator : NSObject

/// Animation target offset
@property (nonatomic, assign, readonly) CGPoint targetOffset;

// 使用 CABasicAnimation + CADisplayLink 驱动滚动，可选逐帧进度回调
- (instancetype)initWithScrollView:(UIScrollView *)scrollView;

- (void)animateToOffset:(CGPoint)targetOffset
               duration:(NSTimeInterval)duration
         timingFunction:(CAMediaTimingFunction *)timing
             onProgress:(void (^ __nullable)(CGFloat progress))progress
             completion:(void (^ __nullable)(BOOL finished))completion;

 - (void)syncToPresentationIfAvailable;

- (void)stop;

// 是否正在进行自定义偏移动画
- (BOOL)isAnimating;


@end

NS_ASSUME_NONNULL_END
