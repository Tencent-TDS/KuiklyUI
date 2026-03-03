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
}
@synthesize hr_rootView;
#pragma mark - init

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        _contentTextView = [[KRRichTextView alloc] initWithFrame:self.bounds];
#if !TARGET_OS_OSX // [macOS]
        [self addSubview:_contentTextView];
#endif // [macOS]
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
    
    // 1. 遍历 sublayers，找到 CSSGradientLayer（承载实际渐变色的 layer），解除 mask 并移除
    //    - mask = nil：解除 _contentTextView.layer 与 gradientLayer 的 mask 绑定，
    //      避免 gradientLayer 释放时 _contentTextView.layer 的 superlayer 变成野指针（EXC_BAD_ACCESS）
    //    - removeFromSuperlayer：移除 gradientLayer 本身，因为它承载了上一次的渐变色，
    //      若不移除，复用后即使新内容无渐变，残留的渐变色块仍会被渲染（渐变色"扩散"问题的根因）
    for (CALayer *subLayer in [self.layer.sublayers copy]) {
        if ([subLayer isKindOfClass:[CAGradientLayer class]] && subLayer != _contentTextView.layer) {
            // 先解除 mask，否则 _contentTextView.layer 会随 gradientLayer 释放而变成野指针
            subLayer.mask = nil;
            [subLayer removeFromSuperlayer];
        }
    }
    
    // 2. 将 _contentTextView 重新添加回 self，恢复 layer 树至初始状态
    //    因为 p_setTextGradient 中 gradientLayer.mask = _contentTextView.layer 会导致
    //    Core Animation 将 _contentTextView.layer 从 self.layer.sublayers 中摘走，
    //    此时需要通过 addSubview 让 UIKit 重新将 _contentTextView.layer 插回 self.layer
    if (_contentTextView.layer.superlayer != self.layer) {
        [_contentTextView removeFromSuperview];
        [self addSubview:_contentTextView];
    }
    
    // 3. 清除 css_backgroundImage 关联属性，避免残留状态影响下一次复用
    self.css_backgroundImage = nil;
    
    // 4. 转发给 _contentTextView 做其自身的复用重置
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
        // Exclude the layer of _contentTextView, 
        // only look for layers specifically used for gradient background.
        // Prevents misjudgment in case the host project globally sets layerClass to CAGradientLayer.
        if ([subLayer isKindOfClass:[CAGradientLayer class]] && subLayer != _contentTextView.layer) {
            gradientLayer = (CAGradientLayer *)subLayer;
        }
    }
    if (gradientLayer) {
#if TARGET_OS_OSX // [macOS
        // Force render once before using layer as mask
        [_contentTextView setNeedsDisplay:YES];
        [_contentTextView displayIfNeeded];
        
        CALayer *maskLayer = _contentTextView.layer;
        
        // Flip Y-axis for macOS coordinate system (bottom-left origin)
        maskLayer.transform = CATransform3DMakeScale(1.0, -1.0, 1.0);
        maskLayer.anchorPoint = CGPointMake(0.5, 0.5);
        maskLayer.position = CGPointMake(CGRectGetMidX(maskLayer.bounds), CGRectGetMidY(maskLayer.bounds));
        
        // Use layer as mask with rendered content
        gradientLayer.mask = maskLayer;
#else // macOS]
        gradientLayer.mask = _contentTextView.layer;
#endif // [macOS]
        
    } else {
#if TARGET_OS_OSX // [macOS
        [self addSubview:_contentTextView];
#endif // macOS]
    }
}

@end
