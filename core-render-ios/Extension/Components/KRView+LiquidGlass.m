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
#import <objc/runtime.h>

#pragma mark - Associated Object Keys

static const void *kEffectViewKey = &kEffectViewKey;
static const void *kGlassEffectEnableKey = &kGlassEffectEnableKey;
static const void *kGlassEffectColorKey = &kGlassEffectColorKey;
static const void *kGlassEffectStyleKey = &kGlassEffectStyleKey;
static const void *kGlassEffectInteractiveKey = &kGlassEffectInteractiveKey;
static const void *kGlassEffectContainerSpacingKey = &kGlassEffectContainerSpacingKey;

#pragma mark - KRVisualEffectView Implementation (iOS)

#if !TARGET_OS_OSX // [macOS]

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
    return NO;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _wrappedView.frame = self.bounds;
}

- (void)removeFromSuperview {
    [_wrappedView removeFromSuperview];
    _wrappedView.kr_commonWrapperView = nil;
    _wrappedView.kr_effectView = nil;
    [super removeFromSuperview];
}

@end

#else

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
            NSGlassEffectView *glassView = [[NSGlassEffectView alloc] initWithFrame:self.bounds];
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
    _wrappedView.kr_commonWrapperView = nil;
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

- (KRVisualEffectView *)kr_effectView {
    return objc_getAssociatedObject(self, kEffectViewKey);
}

- (void)setKr_effectView:(KRVisualEffectView *)effectView {
    objc_setAssociatedObject(self, kEffectViewKey, effectView, OBJC_ASSOCIATION_ASSIGN);
}

#else

- (KRGlassEffectWrapperView *)kr_effectView {
    if (@available(macOS 26.0, *)) {
        return objc_getAssociatedObject(self, kEffectViewKey);
    }
    return nil;
}

- (void)setKr_effectView:(KRGlassEffectWrapperView *)effectView {
    if (@available(macOS 26.0, *)) {
        objc_setAssociatedObject(self, kEffectViewKey, effectView, OBJC_ASSOCIATION_ASSIGN);
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
    // [macOS] NSGlassEffectView does not have interactive property
    // Store the value but do nothing with it
    self.kr_glassEffectInteractive = interactive;
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
                effectView.layer.cornerRadius = self.layer.cornerRadius;
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
                
                self.kr_commonWrapperView = effectView;
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
                
                self.kr_commonWrapperView = effectView;
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
                effectView.layer.cornerRadius = self.layer.cornerRadius;
                
                // Apply stored properties
                if (self.kr_glassEffectColor) {
                    effectView.glassEffectView.tintColor = self.kr_glassEffectColor;
                }
                if (self.kr_glassEffectStyle) {
                    effectView.glassEffectView.style = [KRConvertUtil KRGlassEffectStyle:self.kr_glassEffectStyle];
                }
                
                // Now set contentView after wrapper is configured
                effectView.glassEffectView.contentView = self;
                
                // Add wrapper to parent
                if (parent) {
                    [parent addSubview:effectView positioned:NSWindowAbove relativeTo:idx > 0 ? [parent subviews][idx - 1] : nil];
                }
                
                self.kr_commonWrapperView = effectView;
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
                
                self.kr_commonWrapperView = effectView;
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
    KRVisualEffectView *effectView = self.kr_effectView;
    if (effectView) {
        effectView.layer.cornerRadius = self.layer.cornerRadius;
    }
#else
    if (@available(macOS 26.0, *)) {
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
        KRGlassEffectWrapperView *effectView = self.kr_effectView;
        if (effectView) {
            effectView.layer.cornerRadius = self.layer.cornerRadius;
            if (effectView.glassEffectView) {
                effectView.glassEffectView.cornerRadius = self.layer.cornerRadius;
            }
        }
#endif
    }
#endif // [macOS]
}

@end

