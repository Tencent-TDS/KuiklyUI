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

#import "KRGradientRichTextView.h"
#import "KRRichTextView.h"
#import "KRComponentDefine.h"

@interface KRGradientRichTextView()


@end

@implementation KRGradientRichTextView {
    KRRichTextView *_contentTextView;
    BOOL _isGradientMode;
}
@synthesize hr_rootView;
#pragma mark - init

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _contentTextView = [[KRRichTextView alloc] initWithFrame:self.bounds];
        _isGradientMode = NO;
#if !TARGET_OS_OSX // [macOS]
        [self addSubview:_contentTextView];
#endif
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    if ([propKey isEqualToString:@"backgroundImage"] || [propKey isEqualToString:@"frame"]) { // 背景渐变.mask = 文本.layer 实现文本渐变
        [self css_setPropWithKey:propKey value:propValue];
        [self p_setTextGradient];
    } else {
        [_contentTextView hrv_setPropWithKey:propKey propValue:propValue];
    }
}


/*
 * @brief 重置view，准备被复用 (可选实现)
 * 注：主线程调用，若实现该方法则意味着能被复用
 */
- (void)hrv_prepareForeReuse {
    // 1. 移除渐变 layer，解除 mask
    for (CALayer *subLayer in [self.layer.sublayers copy]) {
        if ([subLayer isKindOfClass:[CAGradientLayer class]] && subLayer != _contentTextView.layer) {
            subLayer.mask = nil;
            [subLayer removeFromSuperlayer];
        }
    }

    // 2. 重置 _contentTextView.layer 的 transform / anchorPoint / position
    CALayer *contentLayer = _contentTextView.layer;
    if (!CATransform3DEqualToTransform(contentLayer.transform, CATransform3DIdentity)) {
        contentLayer.transform = CATransform3DIdentity;
    }
#if TARGET_OS_OSX // [macOS
    // macOS 默认 anchorPoint=(0,0)，必须恢复，否则布局偏移导致文字显示不全
    if (!CGPointEqualToPoint(contentLayer.anchorPoint, CGPointMake(0.0, 0.0))) {
        contentLayer.anchorPoint = CGPointMake(0.0, 0.0);
    }
    contentLayer.position = CGPointMake(0, 0);
#else // macOS]
    // iOS 默认 anchorPoint=(0.5,0.5)
    if (!CGPointEqualToPoint(contentLayer.anchorPoint, CGPointMake(0.5, 0.5))) {
        contentLayer.anchorPoint = CGPointMake(0.5, 0.5);
    }
    contentLayer.position = CGPointMake(CGRectGetMidX(self.bounds), CGRectGetMidY(self.bounds));
#endif

    // 3. 恢复 _contentTextView 的 subview 层级
#if TARGET_OS_OSX // [macOS
    if (_isGradientMode || _contentTextView.superview != self) {
        [_contentTextView removeFromSuperview];
        [self addSubview:_contentTextView];
    }
#else // macOS]
    if (_contentTextView.layer.superlayer != self.layer) {
        [_contentTextView removeFromSuperview];
        [self addSubview:_contentTextView];
    }
#endif // [macOS]

    // 4. 重置状态
    _isGradientMode = NO;
    self.css_backgroundImage = nil;

    // 5. 转发给 _contentTextView 做其自身的复用重置
    [_contentTextView hrv_prepareForeReuse];
}
/*
 * @brief 创建shdow对象(可选实现)
 * 注：1.子线程调用, 若实现该方法则意味着需要自定义计算尺寸
 *    2.该shadow对象不能和renderView是同一个对象
 * @return 返回shadow实例
 */
+ (id<KuiklyRenderShadowProtocol> _Nonnull)hrv_createShadow {
    return [KRRichTextView hrv_createShadow];
}
/*
 * @brief 设置当前renderView实例对应的shadow对象 (可选实现, 注：主线程调用)
 * @param shadow shadow实例
 */
- (void)hrv_setShadow:(id<KuiklyRenderShadowProtocol> _Nonnull)shadow {
    [_contentTextView hrv_setShadow:shadow];
}
/*
 * 调用view方法
 */
- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    [_contentTextView hrv_callWithMethod:method params:params callback:callback];
}

#pragma mark - override

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _contentTextView.frame = self.bounds;
}

#pragma mark - private

- (void)p_setTextGradient {
    CAGradientLayer *gradientLayer = nil;
    for (CALayer *subLayer in self.layer.sublayers) {
        if ([subLayer isKindOfClass:[CAGradientLayer class]] && subLayer != _contentTextView.layer) {
            gradientLayer = (CAGradientLayer *)subLayer;
        }
    }
    if (gradientLayer) {
        _isGradientMode = YES;

#if TARGET_OS_OSX // macOS
        // macOS 需要先强制渲染一次，才能将 layer 用作 mask
        [_contentTextView setNeedsDisplay:YES];
        [_contentTextView displayIfNeeded];
        
        // 先将 _contentTextView 从 superview 移除，避免 view/layer 层级不一致
        if (_contentTextView.superview == self) {
            [_contentTextView removeFromSuperview];
        }

        CALayer *maskLayer = _contentTextView.layer;
        // 重置为 Identity 再设置 Y 轴翻转（macOS 坐标系原点在左下角）
        maskLayer.transform = CATransform3DIdentity;
        maskLayer.transform = CATransform3DMakeScale(1.0, -1.0, 1.0);
        maskLayer.anchorPoint = CGPointMake(0.5, 0.5);
        maskLayer.position = CGPointMake(CGRectGetMidX(maskLayer.bounds), CGRectGetMidY(maskLayer.bounds));

        gradientLayer.mask = maskLayer;
#else // macOS]
        gradientLayer.mask = _contentTextView.layer;
#endif // [macOS]
    } else {
        _isGradientMode = NO;

#if TARGET_OS_OSX // [macOS
        // 非渐变模式：确保 _contentTextView 作为 subview 存在
        if (_contentTextView.superview != self) {
            [self addSubview:_contentTextView];
        }
        // 防止从渐变模式切换过来时残留翻转 transform
        if (!CATransform3DEqualToTransform(_contentTextView.layer.transform, CATransform3DIdentity)) {
            _contentTextView.layer.transform = CATransform3DIdentity;
        }
        // 恢复 anchorPoint 到 macOS 默认值 (0,0)，否则布局偏移导致文字显示不全
        if (!CGPointEqualToPoint(_contentTextView.layer.anchorPoint, CGPointMake(0.0, 0.0))) {
            _contentTextView.layer.anchorPoint = CGPointMake(0.0, 0.0);
            _contentTextView.layer.position = CGPointMake(0, 0);
        }
#endif // macOS]
    }
}

@end
