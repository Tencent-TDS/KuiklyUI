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

#import "KRView+LiquidGlass.h"
#import "KRConvertUtil.h"
#import "KRComponentDefine.h"
#import "UIView+CSS.h"
#import "KRWeakObject.h"
#import <objc/runtime.h>
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
#import <UIKit/UICornerConfiguration.h>
#import <UIKit/UICornerRadius.h>
#endif

#pragma mark - Associated Object Keys

static const void *kEffectViewKey = &kEffectViewKey;
static const void *kGlassEffectEnableKey = &kGlassEffectEnableKey;
static const void *kGlassEffectColorKey = &kGlassEffectColorKey;
static const void *kGlassEffectStyleKey = &kGlassEffectStyleKey;
static const void *kGlassEffectInteractiveKey = &kGlassEffectInteractiveKey;
static const void *kGlassEffectContainerSpacingKey = &kGlassEffectContainerSpacingKey;

#pragma mark - KRVisualEffectView Implementation (iOS)

#if !TARGET_OS_OSX // [macOS]

@interface KRVisualEffectView ()
- (void)kr_updateShadowPath;
@end

@implementation KRVisualEffectView

- (instancetype)initWithEffect:(UIVisualEffect *)effect wrappedView:(KRView *)wrappedView {
    self = [super initWithEffect:effect];
    if (self) {
        self.wrappedView = wrappedView;
        self.userInteractionEnabled = wrappedView.userInteractionEnabled;
    }
    return self;
}

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    // 内层 KRView 位于 UIVisualEffectView 的 contentView 内，其 layer 阴影会被玻璃内部渲染吞掉，
    // 因此 boxShadow 由包装层承载（玻璃开启时 Kotlin 侧下发透明阴影，关闭后恢复真实阴影）
    if ([key isEqualToString:@"boxShadow"]) {
        [self setCss_boxShadow:value];
        [self kr_updateShadowPath];
        return YES;
    }
    return NO;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _wrappedView.frame = self.bounds;
    [self kr_updateShadowPath];
}

/// UIVisualEffectView 的内容由系统私有图层合成，阴影无法从内容推导，需显式提供与圆角一致的 shadowPath
- (void)kr_updateShadowPath {
    if (self.layer.shadowOpacity <= 0 || CGRectIsEmpty(self.bounds)) {
        self.layer.shadowPath = nil;
        return;
    }
    CSSBorderRadius *radius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:_wrappedView.css_borderRadius];
    self.layer.shadowPath = [KRConvertUtil hr_bezierPathWithRoundedRect:self.bounds
                                                    topLeftCornerRadius:radius.topLeftCornerRadius
                                                   topRightCornerRadius:radius.topRightCornerRadius
                                                 bottomLeftCornerRadius:radius.bottomLeftCornerRadius
                                                bottomRightCornerRadius:radius.bottomRightCornerRadius].CGPath;
}

- (void)removeFromSuperview {
    [_wrappedView removeFromSuperview];
    // 玻璃嵌在阴影包装内部时，槽位指向的是外层阴影包装，不能被这里清掉
    if (_wrappedView.kr_commonWrapperView == self) {
        _wrappedView.kr_commonWrapperView = nil;
    }
    _wrappedView.kr_effectView = nil;
    [super removeFromSuperview];
}

@end

#else

#pragma mark - macOS corner configuration

#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
@implementation KRGlassEffectView {
    id _kr_cornerConfiguration; // NSViewCornerConfiguration（macOS 27+）
}

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

@end

void KRGlassMacApplyCornerRadius(NSGlassEffectView *view, NSString *cssBorderRadius) {
    CGFloat tl = 0, tr = 0, bl = 0, br = 0;
    if (cssBorderRadius.length) {
        CSSBorderRadius *borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:cssBorderRadius];
        tl = borderRadius.topLeftCornerRadius;
        tr = borderRadius.topRightCornerRadius;
        bl = borderRadius.bottomLeftCornerRadius;
        br = borderRadius.bottomRightCornerRadius;
    }
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
    if (@available(macOS 27.0, *)) {
        if ([view respondsToSelector:@selector(kr_setCornerConfiguration:)]) {
            // cornerConfiguration 优先于 cornerRadius，cornerRadius 置 0 避免两者混用
            view.cornerRadius = 0;
            NSViewCornerConfiguration *config = [NSViewCornerConfiguration
                configurationWithTopLeftRadius:[NSViewCornerRadius fixedRadius:tl]
                                topRightRadius:[NSViewCornerRadius fixedRadius:tr]
                              bottomLeftRadius:[NSViewCornerRadius fixedRadius:bl]
                             bottomRightRadius:[NSViewCornerRadius fixedRadius:br]];
            [(id)view kr_setCornerConfiguration:config];
            return;
        }
    }
#endif
    // macOS 26：NSGlassEffectView 只有单一 cornerRadius，四角不一致时统一取左上角
    view.cornerRadius = tl;
}
#endif

#pragma mark - KRGlassEffectWrapperView Implementation (macOS)

@implementation KRGlassEffectWrapperView

- (instancetype)initWithGlassEffectWrappedView:(KRView *)wrappedView {
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        self = [super initWithFrame:wrappedView.frame];
        if (self) {
            self.wrappedView = wrappedView;
            self.isContainer = NO;
            self.wantsLayer = YES;
            
            // Create NSGlassEffectView
            // Note: Do NOT set contentView here - it will be set after view hierarchy is properly configured
            NSGlassEffectView *glassView = [[KRGlassEffectView alloc] initWithFrame:self.bounds];
            glassView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
            glassView.style = UIGlassEffectStyleRegular;
            self.glassEffectView = glassView;
            
            [self addSubview:glassView];
        }
#endif
    }
    return self;
}

- (instancetype)initWithContainerEffectWrappedView:(KRView *)wrappedView {
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        self = [super initWithFrame:wrappedView.frame];
        if (self) {
            self.wrappedView = wrappedView;
            self.isContainer = YES;
            self.wantsLayer = YES;
            
            // Create NSGlassEffectContainerView
            // Note: Do NOT set contentView here - it will be set after view hierarchy is properly configured
            NSGlassEffectContainerView *containerView = [[NSGlassEffectContainerView alloc] initWithFrame:self.bounds];
            containerView.autoresizingMask = NSViewWidthSizable | NSViewHeightSizable;
            self.glassContainerView = containerView;
            
            [self addSubview:containerView];
        }
#endif
    }
    return self;
}

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    return NO;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _wrappedView.frame = self.bounds;
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        if (_glassEffectView) {
            _glassEffectView.frame = self.bounds;
        }
        if (_glassContainerView) {
            _glassContainerView.frame = self.bounds;
        }
#endif
    }
}

- (void)removeFromSuperview {
    [_wrappedView removeFromSuperview];
    if (_wrappedView.kr_commonWrapperView == self) {
        _wrappedView.kr_commonWrapperView = nil;
    }
    if (@available(macOS 26.0, *)) {
        _wrappedView.kr_effectView = nil;
    }
    [super removeFromSuperview];
}

@end

#endif // [macOS]

#pragma mark - KRView (LiquidGlass) Implementation

@implementation KRView (LiquidGlass)

#pragma mark - Associated Object Accessors

#if !TARGET_OS_OSX // [macOS]

// kr_effectView 声明为 weak：关联对象没有 weak 语义，ASSIGN 在包装层释放后会成为野指针，
// 因此用 KRWeakObject 盒子（RETAIN 持有盒子、盒子 weak 持有包装层）实现真正的弱引用。
- (KRVisualEffectView *)kr_effectView {
    KRWeakObject *box = objc_getAssociatedObject(self, kEffectViewKey);
    return box.weakObject;
}

- (void)setKr_effectView:(KRVisualEffectView *)effectView {
    KRWeakObject *box = effectView ? [[KRWeakObject alloc] initWithObject:effectView] : nil;
    objc_setAssociatedObject(self, kEffectViewKey, box, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

#else

- (KRGlassEffectWrapperView *)kr_effectView {
    if (@available(macOS 26.0, *)) {
        KRWeakObject *box = objc_getAssociatedObject(self, kEffectViewKey);
        return box.weakObject;
    }
    return nil;
}

- (void)setKr_effectView:(KRGlassEffectWrapperView *)effectView {
    if (@available(macOS 26.0, *)) {
        KRWeakObject *box = effectView ? [[KRWeakObject alloc] initWithObject:effectView] : nil;
        objc_setAssociatedObject(self, kEffectViewKey, box, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
}

#endif // [macOS]

- (BOOL)kr_glassEffectEnable {
    return [objc_getAssociatedObject(self, kGlassEffectEnableKey) boolValue];
}

- (void)setKr_glassEffectEnable:(BOOL)enable {
    objc_setAssociatedObject(self, kGlassEffectEnableKey, @(enable), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (UIColor *)kr_glassEffectColor {
    return objc_getAssociatedObject(self, kGlassEffectColorKey);
}

- (void)setKr_glassEffectColor:(UIColor *)color {
    objc_setAssociatedObject(self, kGlassEffectColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)kr_glassEffectStyle {
    return objc_getAssociatedObject(self, kGlassEffectStyleKey);
}

- (void)setKr_glassEffectStyle:(NSString *)style {
    objc_setAssociatedObject(self, kGlassEffectStyleKey, style, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

- (NSNumber *)kr_glassEffectInteractive {
    return objc_getAssociatedObject(self, kGlassEffectInteractiveKey);
}

- (void)setKr_glassEffectInteractive:(NSNumber *)interactive {
    objc_setAssociatedObject(self, kGlassEffectInteractiveKey, interactive, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSNumber *)kr_glassEffectContainerSpacing {
    return objc_getAssociatedObject(self, kGlassEffectContainerSpacingKey);
}

- (void)setKr_glassEffectContainerSpacing:(NSNumber *)spacing {
    objc_setAssociatedObject(self, kGlassEffectContainerSpacingKey, spacing, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

#pragma mark - CSS Properties

- (void)setCss_glassEffectEnable:(NSNumber *)css_glassEffectEnable {
#if !TARGET_OS_OSX // [macOS]
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        BOOL shouldEnable = [css_glassEffectEnable boolValue];
        if (self.kr_glassEffectEnable != shouldEnable) {
            self.kr_glassEffectEnable = shouldEnable;
            KRVisualEffectView *effectView = self.kr_effectView;
            if (effectView) {
                if (!shouldEnable) {
                    UIVisualEffect *effect = [[UIVisualEffect alloc] init];
                    effectView.effect = effect;
                } else {
                    effectView.effect = [self generateGlassEffect];
                }
            } else {
                // If the view has already been inserted without wrapper, create it now
                if (shouldEnable) {
                    [self ensureGlassEffectWrapperView];
                }
            }
        }
    }
#endif
#else
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
    if (@available(macOS 26.0, *)) {
        BOOL shouldEnable = [css_glassEffectEnable boolValue];
        if (self.kr_glassEffectEnable != shouldEnable) {
            self.kr_glassEffectEnable = shouldEnable;
            KRGlassEffectWrapperView *effectView = self.kr_effectView;
            if (effectView) {
                // On macOS, we can't disable the effect in-place like iOS,
                // we would need to remove the wrapper entirely
                // For now, just hide/show the glass effect view
                effectView.glassEffectView.hidden = !shouldEnable;
            } else {
                // If the view has already been inserted without wrapper, create it now
                if (shouldEnable) {
                    [self ensureGlassEffectWrapperView];
                }
            }
        }
    }
#endif
#endif // [macOS]
}

- (void)setCss_glassEffectSpacing:(NSNumber *)spacing {
#if !TARGET_OS_OSX // [macOS]
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        if (spacing && ![self.kr_glassEffectContainerSpacing isEqualToNumber:spacing]) {
            self.kr_glassEffectContainerSpacing = spacing;
            
            KRVisualEffectView *effectView = self.kr_effectView;
            if (effectView) {
                UIVisualEffect *effect = effectView.effect;
                if ([effect isKindOfClass:UIGlassContainerEffect.class]) {
                    UIGlassContainerEffect *containerEffect = (UIGlassContainerEffect *)effectView.effect;
                    containerEffect.spacing = spacing.doubleValue;
                    effectView.effect = containerEffect;
                }
            } else {
                // If wrapper not created yet and spacing is set, create container wrapper now
                [self ensureGlassEffectWrapperView];
            }
        }
    }
#endif
#else
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
    if (@available(macOS 26.0, *)) {
        if (spacing && ![self.kr_glassEffectContainerSpacing isEqualToNumber:spacing]) {
            self.kr_glassEffectContainerSpacing = spacing;
            
            KRGlassEffectWrapperView *effectView = self.kr_effectView;
            if (effectView && effectView.glassContainerView) {
                effectView.glassContainerView.spacing = spacing.doubleValue;
            } else {
                // If wrapper not created yet and spacing is set, create container wrapper now
                [self ensureGlassEffectWrapperView];
            }
        }
    }
#endif
#endif // [macOS]
}

- (void)setCss_glassEffectInteractive:(NSNumber *)interactive {
#if !TARGET_OS_OSX // [macOS]
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if ((interactive != nil && ![self.kr_glassEffectInteractive isEqualToNumber:interactive]) ||
            (interactive == nil && self.kr_glassEffectInteractive != nil)) {
            self.kr_glassEffectInteractive = interactive;
            
            KRVisualEffectView *effectView = self.kr_effectView;
            if (effectView) {
                UIVisualEffect *effect = effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *glassEffect = (UIGlassEffect *)effectView.effect;
                    glassEffect.interactive = [interactive boolValue];
                    effectView.effect = glassEffect;
                }
            }
        }
#endif
    }
#else
    // [macOS] NSGlassEffectView 的交互态（effectIsInteractive）从 macOS 27 才提供；
    // macOS 26 无对应 API，仅保存值，升级到 27 的系统上创建包装层时仍会应用。
    self.kr_glassEffectInteractive = interactive;
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
    if (@available(macOS 27.0, *)) {
        KRGlassEffectWrapperView *effectView = self.kr_effectView;
        if (effectView.glassEffectView) {
            effectView.glassEffectView.effectIsInteractive = interactive.boolValue;
        }
    }
#endif
#endif // [macOS]
}

- (void)setCss_glassEffectTintColor:(NSNumber *)cssColor {
#if !TARGET_OS_OSX // [macOS]
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIColor *color = [UIView css_color:cssColor];
        if (![self.kr_glassEffectColor isEqual:color]) {
            self.kr_glassEffectColor = color;
            
            KRVisualEffectView *effectView = self.kr_effectView;
            if (effectView) {
                UIVisualEffect *effect = effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *glassEffect = (UIGlassEffect *)effectView.effect;
                    glassEffect.tintColor = color;
                    effectView.effect = glassEffect;
                }
            }
        }
#endif
    }
#else
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
    if (@available(macOS 26.0, *)) {
        NSColor *color = [UIView css_color:cssColor];
        if (![self.kr_glassEffectColor isEqual:color]) {
            self.kr_glassEffectColor = color;
            
            KRGlassEffectWrapperView *effectView = self.kr_effectView;
            if (effectView && effectView.glassEffectView) {
                effectView.glassEffectView.tintColor = color;
            }
        }
    }
#endif
#endif // [macOS]
}

- (void)setCss_glassEffectStyle:(NSString *)style {
#if !TARGET_OS_OSX // [macOS]
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassEffectStyle curStyle = [KRConvertUtil KRGlassEffectStyle:self.kr_glassEffectStyle];
        UIGlassEffectStyle newStyle = [KRConvertUtil KRGlassEffectStyle:style];
        if (curStyle != newStyle) {
            self.kr_glassEffectStyle = style;
            
            KRVisualEffectView *effectView = self.kr_effectView;
            if (effectView) {
                UIVisualEffect *effect = effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *curEffect = (UIGlassEffect *)effectView.effect;
                    UIGlassEffect *updatedEffect = [UIGlassEffect effectWithStyle:newStyle];
                    updatedEffect.tintColor = curEffect.tintColor;
                    updatedEffect.interactive = curEffect.interactive;
                    effectView.effect = updatedEffect;
                }
            }
        }
#endif
    }
#else
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
    if (@available(macOS 26.0, *)) {
        UIGlassEffectStyle curStyle = [KRConvertUtil KRGlassEffectStyle:self.kr_glassEffectStyle];
        UIGlassEffectStyle newStyle = [KRConvertUtil KRGlassEffectStyle:style];
        if (curStyle != newStyle) {
            self.kr_glassEffectStyle = style;
            
            KRGlassEffectWrapperView *effectView = self.kr_effectView;
            if (effectView && effectView.glassEffectView) {
                effectView.glassEffectView.style = newStyle;
            }
        }
    }
#endif
#endif // [macOS]
}

#pragma mark - Glass Effect Helper Methods

#if !TARGET_OS_OSX && __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000 // [macOS]
/// 玻璃形状只认视图自身的 cornerConfiguration（layer.mask 对玻璃无效；玻璃上屏后再改
/// layer.cornerRadius 也不会刷新玻璃形状），因此四角一致与 0 也统一走 cornerConfiguration，
/// 保证动态切换时新值覆盖旧值。玻璃只存在于 iOS 26+，无需低版本分支。
void KRGlassApplyCornerConfiguration(UIView *view, NSString *cssBorderRadius) {
    CGFloat tl = 0, tr = 0, bl = 0, br = 0;
    if (cssBorderRadius.length) {
        CSSBorderRadius *borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:cssBorderRadius];
        tl = borderRadius.topLeftCornerRadius;
        tr = borderRadius.topRightCornerRadius;
        bl = borderRadius.bottomLeftCornerRadius;
        br = borderRadius.bottomRightCornerRadius;
    }
    BOOL uniform = (tl == tr && tr == bl && bl == br);
    view.cornerConfiguration = uniform
        ? [UICornerConfiguration configurationWithUniformRadius:[UICornerRadius fixedRadius:tl]]
        : [UICornerConfiguration configurationWithTopLeftRadius:[UICornerRadius fixedRadius:tl]
                                                 topRightRadius:[UICornerRadius fixedRadius:tr]
                                               bottomLeftRadius:[UICornerRadius fixedRadius:bl]
                                              bottomRightRadius:[UICornerRadius fixedRadius:br]];
}
#endif // [macOS]

#if !TARGET_OS_OSX // [macOS]
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
- (UIGlassEffect *)generateGlassEffect {
    if (@available(iOS 26.0, *)) {
        UIGlassEffectStyle style = [KRConvertUtil KRGlassEffectStyle:self.kr_glassEffectStyle];
        UIGlassEffect *glassEffect = [UIGlassEffect effectWithStyle:style];
        glassEffect.tintColor = self.kr_glassEffectColor;
        glassEffect.interactive = self.kr_glassEffectInteractive.boolValue;
        return glassEffect;
    }
    return nil;
}
#endif
#endif // [macOS]

- (void)ensureGlassEffectWrapperView {
#if !TARGET_OS_OSX // [macOS]
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if (self.kr_glassEffectEnable) {
            if (!self.kr_effectView) {
                UIGlassEffect *glassEffect = [self generateGlassEffect];
                KRVisualEffectView *effectView = [[KRVisualEffectView alloc] initWithEffect:glassEffect
                                                                               wrappedView:self];
                self.kr_effectView = effectView;
                KRGlassApplyCornerConfiguration(effectView, self.css_borderRadius);
                // Preserve current parent relationship if already inserted
                UIView *parent = self.superview;
                CGRect oldFrame = self.frame;
                if (parent) {
                    NSUInteger idx = [[parent subviews] indexOfObject:self];
                    [self removeFromSuperview];
                    effectView.frame = oldFrame;
                    [effectView.contentView addSubview:self];
                    [parent insertSubview:effectView atIndex:idx];
                } else {
                    effectView.frame = oldFrame;
                    [effectView.contentView addSubview:self];
                }
                
                // kr_commonWrapperView 是单槽位 + RETAIN。若外层已有阴影包装（KRBoxShadowView），
                // 不能把槽位覆盖成玻璃包装，否则阴影包装会失去唯一强引用而被释放。
                // 玻璃包装此时已经插入到阴影包装内部，保持槽位指向最外层即可让两者同时生效。
                if (!self.kr_commonWrapperView) {
                    self.kr_commonWrapperView = effectView;
                }
            }
        } else if (self.kr_glassEffectContainerSpacing) {
            if (!self.kr_effectView) {
                UIGlassContainerEffect *glassContainerEffect = [[UIGlassContainerEffect alloc] init];
                glassContainerEffect.spacing = self.kr_glassEffectContainerSpacing.doubleValue;
                KRVisualEffectView *effectView = [[KRVisualEffectView alloc] initWithEffect:glassContainerEffect
                                                                               wrappedView:self];
                self.kr_effectView = effectView;
                // Preserve current parent relationship if already inserted
                UIView *parent = self.superview;
                CGRect oldFrame = self.frame;
                if (parent) {
                    NSUInteger idx = [[parent subviews] indexOfObject:self];
                    [self removeFromSuperview];
                    effectView.frame = oldFrame;
                    [effectView.contentView addSubview:self];
                    [parent insertSubview:effectView atIndex:idx];
                } else {
                    effectView.frame = oldFrame;
                    [effectView.contentView addSubview:self];
                }
                
                // 同玻璃分支：槽位已被阴影包装占用时不覆盖，容器嵌在其内部
                if (!self.kr_commonWrapperView) {
                    self.kr_commonWrapperView = effectView;
                }
            }
        }
#endif
    }
#else
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        if (self.kr_glassEffectEnable) {
            if (!self.kr_effectView) {
                // Preserve current parent relationship BEFORE creating wrapper
                NSView *parent = self.superview;
                CGRect oldFrame = self.frame;
                NSUInteger idx = parent ? [[parent subviews] indexOfObject:self] : 0;
                
                // Remove self from current parent first
                if (parent) {
                    [self removeFromSuperview];
                }
                
                // Create wrapper view
                KRGlassEffectWrapperView *effectView = [[KRGlassEffectWrapperView alloc] initWithGlassEffectWrappedView:self];
                self.kr_effectView = effectView;
                effectView.frame = oldFrame;
                // 与 updateEffectViewCornerRadius 对齐：玻璃形状由 NSGlassEffectView 自身决定
                KRGlassMacApplyCornerRadius(effectView.glassEffectView, self.css_borderRadius);
                
                // Apply stored properties
                if (self.kr_glassEffectColor) {
                    effectView.glassEffectView.tintColor = self.kr_glassEffectColor;
                }
                if (self.kr_glassEffectStyle) {
                    effectView.glassEffectView.style = [KRConvertUtil KRGlassEffectStyle:self.kr_glassEffectStyle];
                }
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 270000
                if (@available(macOS 27.0, *)) {
                    // 与 iOS 对齐：interactive 属性可能先于包装层创建下发
                    effectView.glassEffectView.effectIsInteractive = self.kr_glassEffectInteractive.boolValue;
                }
#endif
                
                // Now set contentView after wrapper is configured
                effectView.glassEffectView.contentView = self;
                
                // Add wrapper to parent
                if (parent) {
                    [parent addSubview:effectView positioned:NSWindowAbove relativeTo:idx > 0 ? [parent subviews][idx - 1] : nil];
                }
                
                if (!self.kr_commonWrapperView) {
                    self.kr_commonWrapperView = effectView;
                }
            }
        } else if (self.kr_glassEffectContainerSpacing) {
            if (!self.kr_effectView) {
                // Preserve current parent relationship BEFORE creating wrapper
                NSView *parent = self.superview;
                CGRect oldFrame = self.frame;
                NSUInteger idx = parent ? [[parent subviews] indexOfObject:self] : 0;
                
                // Remove self from current parent first
                if (parent) {
                    [self removeFromSuperview];
                }
                
                // Create wrapper view
                KRGlassEffectWrapperView *effectView = [[KRGlassEffectWrapperView alloc] initWithContainerEffectWrappedView:self];
                self.kr_effectView = effectView;
                effectView.frame = oldFrame;
                
                // Apply stored spacing
                effectView.glassContainerView.spacing = self.kr_glassEffectContainerSpacing.doubleValue;
                
                // Now set contentView after wrapper is configured
                effectView.glassContainerView.contentView = self;
                
                // Add wrapper to parent
                if (parent) {
                    [parent addSubview:effectView positioned:NSWindowAbove relativeTo:idx > 0 ? [parent subviews][idx - 1] : nil];
                }
                
                if (!self.kr_commonWrapperView) {
                    self.kr_commonWrapperView = effectView;
                }
            }
        }
#endif
    }
#endif // [macOS]
}

- (void)updateEffectViewFrame {
#if !TARGET_OS_OSX // [macOS]
    KRVisualEffectView *effectView = self.kr_effectView;
    if (effectView) {
        effectView.frame = self.frame;
    }
#else
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        KRGlassEffectWrapperView *effectView = self.kr_effectView;
        if (effectView) {
            effectView.frame = self.frame;
        }
#endif
    }
#endif // [macOS]
}

- (void)updateEffectViewCornerRadius {
#if !TARGET_OS_OSX // [macOS]
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        KRVisualEffectView *effectView = self.kr_effectView;
        if (effectView) {
            KRGlassApplyCornerConfiguration(effectView, self.css_borderRadius);
            [effectView kr_updateShadowPath];
        }
    }
#endif
#else
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        KRGlassEffectWrapperView *effectView = self.kr_effectView;
        if (effectView.glassEffectView) {
            KRGlassMacApplyCornerRadius(effectView.glassEffectView, self.css_borderRadius);
        }
#endif
    }
#endif // [macOS]
}

@end

