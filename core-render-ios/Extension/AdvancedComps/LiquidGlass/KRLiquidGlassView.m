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

#import "KRLiquidGlassView.h"
#import "KRConvertUtil.h"
#import "UIView+CSS.h"
#import "KRView+LiquidGlass.h"

#if TARGET_OS_IOS // [macOS]

@implementation KRLiquidGlassView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame: frame]) {
        if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
            self.effect = [UIGlassEffect effectWithStyle:UIGlassEffectStyleRegular];
#endif
        } else {
            // Fallback on earlier versions
        }
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
}

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    [self.contentView insertSubview:subView atIndex:index];
}

#pragma mark - CSS properties

/// 玻璃控件的圆角拆成两部分处理（iOS 26+）：
/// - 玻璃外壳：layer.mask 对 UIGlassEffect 无效，改用 cornerConfiguration（支持四角独立半径）；
/// - 子内容：把基类生成的圆角 mask 从玻璃层挪到 contentView 上，子视图仍按圆角裁剪。
/// iOS 26 以下系统没有玻璃效果，保持基类 mask 行为即可（同样支持四角独立半径）。
- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if (self.css_clipPath.length == 0) {
            CALayer *mask = self.layer.mask;
            self.layer.mask = nil;
            self.contentView.layer.mask = mask;
            mask.frame = self.contentView.bounds;
        }
        KRGlassApplyCornerConfiguration(self, self.css_borderRadius);
        [self setNeedsLayout];
#endif
    }
}

- (void)setCss_border:(NSString *)css_border {
    [super setCss_border:css_border];
    [self setNeedsLayout]; // 描边层可能被重建，需重新挂圆角 mask
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    // contentView 上的圆角 mask 不在基类 frame 同步链路内，这里跟随尺寸更新
    CAShapeLayer *contentMask = (CAShapeLayer *)self.contentView.layer.mask;
    if (contentMask && !CGRectEqualToRect(contentMask.frame, self.contentView.bounds)) {
        contentMask.frame = self.contentView.bounds;
    }
    // 玻璃层撤掉 mask 后，描边（描在边界线上、一半在外）只被矩形 bounds 裁剪，
    // 圆角处外侧那一半会露出来变粗；给描边层挂同形状 mask，与普通 View 的视觉保持一致
    static Class borderLayerClass;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        borderLayerClass = NSClassFromString(@"CSSBorderLayer");
    });
    for (CALayer *sublayer in self.layer.sublayers) {
        if (![sublayer isKindOfClass:borderLayerClass]) {
            continue;
        }
        if (![contentMask isKindOfClass:[CAShapeLayer class]]) {
            sublayer.mask = nil;
            continue;
        }
        CAShapeLayer *borderMask = [sublayer.mask isKindOfClass:[CAShapeLayer class]]
            ? (CAShapeLayer *)sublayer.mask : [CAShapeLayer layer];
        // 描边层 frame 由其 layoutSublayers 同步为宿主 bounds，此刻可能尚未同步，直接用宿主 bounds
        borderMask.frame = self.bounds;
        borderMask.path = contentMask.path;
        sublayer.mask = borderMask;
    }
    [CATransaction commit];
}

- (void)setCss_glassEffectTintColor:(NSNumber *)color {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassEffect *effect = (UIGlassEffect *)self.effect;
        if (![effect.tintColor isEqual:color]) {
            effect.tintColor = [UIView css_color:color];
            self.effect = effect;
        }
#endif
    }
}

- (void)setCss_glassEffectInteractive:(NSNumber *)interactive {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassEffect *effect = (UIGlassEffect *)self.effect;
        if (effect.isInteractive != [interactive boolValue]) {
            effect.interactive = [interactive boolValue];
            self.effect = effect;
        }
#endif
    }
}

- (void)setCss_glassEffectStyle:(NSString *)style {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassEffectStyle glassStyle = [KRConvertUtil KRGlassEffectStyle:style];
        UIGlassEffect *newEffect = [UIGlassEffect effectWithStyle:glassStyle];
        UIGlassEffect *currentEffect = (UIGlassEffect *)self.effect;
        
        // Preserve existing properties
        newEffect.tintColor = currentEffect.tintColor;
        newEffect.interactive = currentEffect.isInteractive;
        
        self.effect = newEffect;
#endif
    }
}

@end

#endif

// [macOS
#if TARGET_OS_OSX
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000

@implementation KRLiquidGlassView {
    id _kr_cornerConfiguration; // NSViewCornerConfiguration（macOS 27+）
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        if (@available(macOS 26.0, *)) {
            self.style = NSGlassEffectViewStyleRegular;
        }
    }
    return self;
}

/// 与 KRGlassEffectView 一致：NSView.cornerConfiguration 只读，由子类重写 getter 提供（macOS 27+）
- (void)kr_setCornerConfiguration:(id)cornerConfiguration {
    _kr_cornerConfiguration = cornerConfiguration;
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
    if (@available(macOS 27.0, *)) {
        [self invalidateCornerConfiguration];
    }
#endif
}

#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
- (NSViewCornerConfiguration *)cornerConfiguration API_AVAILABLE(macos(27.0)) {
    return _kr_cornerConfiguration ?: [super cornerConfiguration];
}
#endif

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
}

- (void)hrv_insertSubview:(NSView *)subView atIndex:(NSInteger)index {
    // NSGlassEffectView uses contentView for embedding content
    if (@available(macOS 26.0, *)) {
        if (self.contentView) {
            NSArray<NSView *> *subviews = self.contentView.subviews;
            if (index >= (NSInteger)subviews.count) {
                [self.contentView addSubview:subView];
            } else {
                [self.contentView addSubview:subView positioned:NSWindowBelow relativeTo:subviews[index]];
            }
        } else {
            self.contentView = subView;
        }
    }
}

#pragma mark - CSS properties

/// 同 iOS 分支：mask 对玻璃无效。macOS 27+ 走 cornerConfiguration 支持四角独立半径；
/// macOS 26 只有单一 cornerRadius，四角不一致时统一取左上角（AppKit 系统限制）。
- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    self.layer.mask = nil;
    KRGlassMacApplyCornerRadius(self, self.css_borderRadius);
}

- (void)setCss_glassEffectTintColor:(NSNumber *)color {
    if (@available(macOS 26.0, *)) {
        NSColor *tintColor = [UIView css_color:color];
        if (![self.tintColor isEqual:tintColor]) {
            self.tintColor = tintColor;
        }
    }
}

- (void)setCss_glassEffectInteractive:(NSNumber *)interactive {
    // macOS NSGlassEffectView does not have interactive property
    // This is iOS-specific for UIGlassEffect
}

- (void)setCss_glassEffectStyle:(NSString *)style {
    if (@available(macOS 26.0, *)) {
        NSGlassEffectViewStyle glassStyle = [KRConvertUtil KRGlassEffectStyle:style];
        if (self.style != glassStyle) {
            self.style = glassStyle;
        }
    }
}

@end

#endif
#endif
// macOS]
