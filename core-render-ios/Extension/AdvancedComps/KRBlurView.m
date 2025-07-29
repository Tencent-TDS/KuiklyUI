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

#import "KRBlurView.h"
#import "KRComponentDefine.h"
/*
 * @brief 高斯模糊视图
 */
@interface KRBlurView()


@end

@implementation KRBlurView {
    NSObject *_animator;
}
@synthesize hr_rootView;
- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame: frame]) {
        self.effect = [UIBlurEffect effectWithStyle:(UIBlurEffectStyleLight)];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(onReceiveApplicationDidBecomeActiveNotification:)
                                                     name:UIApplicationDidBecomeActiveNotification object:nil];
    }
    return self;
}

#pragma mark - notifications

- (void)onReceiveApplicationDidBecomeActiveNotification:(NSNotification *)notification {
    if (self.css_blurRadius) {
        [self setCss_blurRadius:self.css_blurRadius];
    }
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)setCss_blurRadius:(NSNumber *)css_blurRadius {
    _css_blurRadius = css_blurRadius;
    if (@available(iOS 10.0, *)) {
        self.effect = nil;
        UIViewPropertyAnimator *animator;
        if ([_animator isKindOfClass:[UIViewPropertyAnimator class]]) {
            animator = (UIViewPropertyAnimator *)_animator;
            [animator stopAnimation:false];
            [animator finishAnimationAtPosition:(UIViewAnimatingPositionCurrent)];
        }
        animator = [[UIViewPropertyAnimator alloc] initWithDuration:1 curve:(UIViewAnimationCurveLinear) animations:^{
            self.effect = [UIBlurEffect effectWithStyle:(UIBlurEffectStyleLight)];
        }];
        animator.fractionComplete = css_blurRadius.floatValue / 10;
        _animator = animator;
    } else {
        // Fallback on earlier versions
    }
}

#pragma mark - dealloc

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
