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
    /// 玻璃的 contentView：由本控件持有，所有 Kuikly 子视图都插入其中，子内容的圆角裁剪 mask 也挂在这里。
    /// 不能直接把首个 Kuikly 子视图当 contentView：其自身的 borderRadius / clipPath mask 会与这里的 mask 互相覆盖。
    KRUIView *_kr_contentContainer;
}

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        if (@available(macOS 26.0, *)) {
            self.style = NSGlassEffectViewStyleRegular;
            _kr_contentContainer = [[KRUIView alloc] initWithFrame:self.bounds]; // isFlipped = YES，与 Kuikly 坐标系一致
            _kr_contentContainer.wantsLayer = YES;
            _kr_contentContainer.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
            self.contentView = _kr_contentContainer;
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
    // 子视图统一插入自持的内容容器（与 iOS 插入 contentView 的语义一致）
    NSView *container = _kr_contentContainer;
    if (!container) {
        return;
    }
    NSArray<NSView *> *subviews = container.subviews;
    if (index < 0 || index >= (NSInteger)subviews.count) {
        [container addSubview:subView];
    } else {
        [container addSubview:subView positioned:NSWindowBelow relativeTo:subviews[index]];
    }
}

- (void)layout {
    [super layout];
    if (!_kr_contentContainer) {
        return;
    }
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    if (!CGRectEqualToRect(_kr_contentContainer.frame, self.bounds)) {
        _kr_contentContainer.frame = self.bounds;
    }
    CALayer *mask = _kr_contentContainer.layer.mask;
    if (mask && !CGRectEqualToRect(mask.frame, _kr_contentContainer.bounds)) {
        mask.frame = _kr_contentContainer.bounds; // CSSShapeLayer 在 setFrame 时按新尺寸重算圆角路径
    }
    [self kr_syncBorderLayerMask];
    [CATransaction commit];
}

- (void)setFrameSize:(NSSize)newSize {
    [super setFrameSize:newSize];
    self.needsLayout = YES; // 圆角 mask 需按新尺寸重算
}

/// 与 iOS 分支一致：玻璃宿主层撤掉 mask 后，描边（lineWidth = 2 × borderWidth，描在边界线上、一半在外）
/// 只被描边层自身的矩形 bounds 裁剪，圆角处外侧那一半会露出来，看起来描边变粗、探出玻璃轮廓。
/// 这里给描边层挂与内容裁剪同形状的 mask，视觉与普通 View 的描边保持一致。
- (void)kr_syncBorderLayerMask {
    static Class borderLayerClass;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        borderLayerClass = NSClassFromString(@"CSSBorderLayer");
    });
    CALayer *contentMask = _kr_contentContainer.layer.mask;
    for (CALayer *sublayer in self.layer.sublayers) {
        if (![sublayer isKindOfClass:borderLayerClass]) {
            continue;
        }
        // 无圆角，或存在 clipPath（宿主层 mask 仍由基类负责）时，描边不需要额外 mask
        if (![contentMask isKindOfClass:[CAShapeLayer class]] || self.css_clipPath.length > 0) {
            sublayer.mask = nil;
            continue;
        }
        CAShapeLayer *borderMask = [sublayer.mask isKindOfClass:[CAShapeLayer class]]
            ? (CAShapeLayer *)sublayer.mask : [CAShapeLayer layer];
        // 描边层 frame 由其 layoutSublayers 同步为宿主 bounds，此刻可能尚未同步，直接用宿主 bounds
        borderMask.frame = self.bounds;
        borderMask.path = ((CAShapeLayer *)contentMask).path;
        sublayer.mask = borderMask;
    }
}

#pragma mark - CSS properties

/// 同 iOS 分支：mask 对玻璃无效。macOS 27+ 走 cornerConfiguration 支持四角独立半径；
/// macOS 26 只有单一 cornerRadius，四角不一致时统一取左上角（AppKit 系统限制）。
/// 子内容裁剪不依赖 NSGlassEffectView 的行为（无系统契约），而是按 css 圆角在内容容器上重建 mask。
- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    [self kr_updateContentClipMask];
    KRGlassMacApplyCornerRadius(self, self.css_borderRadius);
    self.needsLayout = YES; // 描边层 mask 需跟随新圆角
}

- (void)setCss_border:(NSString *)css_border {
    [super setCss_border:css_border];
    self.needsLayout = YES; // 描边层可能被重建，需重新挂圆角 mask
}

- (void)setCss_clipPath:(NSString *)css_clipPath {
    [super setCss_clipPath:css_clipPath];
    self.needsLayout = YES; // clipPath 增删会改变描边层是否需要圆角 mask
}

/// 每次都按当前 css_borderRadius 的值重建，不根据「宿主层上是否有 mask」推断状态，
/// 保证多次插入子视图、圆角清零、相同值重复下发等场景下结果确定。
- (void)kr_updateContentClipMask {
    if (!_kr_contentContainer || self.css_clipPath.length > 0) {
        return; // 存在 clipPath 时由基类的 clipPath mask 负责，保持与 iOS 分支一致
    }
    self.layer.mask = nil; // 基类挂在玻璃宿主层上的圆角 mask 对玻璃无效
    CSSBorderRadius *radius = self.css_borderRadius.length
        ? [[CSSBorderRadius alloc] initWithCSSBorderRadius:self.css_borderRadius] : nil;
    if (!radius || ([radius isSameBorderCornerRaidus] && radius.topLeftCornerRadius < 0.0001)) {
        _kr_contentContainer.layer.mask = nil;
        return;
    }
    CSSShapeLayer *mask = [[CSSShapeLayer alloc] initWithBorderRadius:radius];
    mask.contentsScale = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    _kr_contentContainer.layer.mask = mask;
    mask.frame = _kr_contentContainer.bounds;
    [CATransaction commit];
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
    // NSGlassEffectView 的交互态从 macOS 27 才提供，macOS 26 无对应 API
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
    if (@available(macOS 27.0, *)) {
        BOOL isInteractive = interactive.boolValue;
        if (self.effectIsInteractive != isInteractive) {
            self.effectIsInteractive = isInteractive;
        }
    }
#endif
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
