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
        [self addSubview:_contentTextView];
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
        if ([subLayer isKindOfClass:[CAGradientLayer class]]) {
            gradientLayer = (CAGradientLayer *)subLayer;
        }
    }
    if (gradientLayer) {
        gradientLayer.mask = _contentTextView.layer;
    } else {
        
    }
}

@end
