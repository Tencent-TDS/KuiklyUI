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

#import "UIView+CSS.h"
#import <objc/runtime.h>
#import "KRConvertUtil.h"
#import "KRView.h"
#import "KuiklyRenderBridge.h"
#import "KuiklyRenderViewExportProtocol.h"

#define LAZY_ANIMATION_KEY @"lazyAnimationKey"
#define ANIMATION_KEY @"animation"

/// Default iOS keyboard animation curve value from UIKeyboardAnimationCurveUserInfoKey
static const NSInteger KRDefaultKeyboardAnimationCurve = 7;

@interface CSSBorder : NSObject

@property (nonatomic, assign) KRBorderStyle borderStyle;
@property (nonatomic, assign) CGFloat borderWidth;
@property (nonatomic, strong) UIColor *borderColor;
- (instancetype)initWithCSSBorder:(NSString *)cssBorder;

@end

@interface CSSBorderLayer : CAShapeLayer

@property (nonatomic, strong) CSSBorder *border;
@property (nonatomic, weak) UIView *hostView;

- (instancetype)initWithCSSBorder:(CSSBorder *)border;

@end


@interface CSSAnimation : NSObject

@property (nonatomic, copy) NSString *animationKey;

- (instancetype)initWithCSSAnimation:(NSString *)cssAnimation;

- (void)animationWithBlock:(void (^)(void))block completion:(void (^)(BOOL finished))completion;

- (void)addKeyframeWithRelativeStartTime:(double)frameStartTime relativeDuration:(double)frameDuration animations:(void (^)(void))animations API_AVAILABLE(ios(7.0));

@end

@interface CSSTransform : NSObject


- (instancetype)initWithCSSTransform:(NSString *)cssTransform;
- (void)applyToView:(UIView *)view;
- (void)applyToView:(UIView *)view animation:(CSSAnimation *)animation oldTransform:(CSSTransform *)oldTransform;
+ (void)resetTransformWithView:(UIView *)view;


@end

@interface CSSLazyAnimationImp : NSObject

- (void)setPropWithKey:(NSString *)propKey value:(id)propValue withAnimationKey:(NSString *)animationKey;

- (BOOL)performAnimationWithKey:(NSString *)animationKey withView:(UIView *)view;

@end


@interface UIView() <KuiklyRenderViewLifyCycleProtocol>

@property (nonatomic, strong) CSSAnimation *css_animationImp;
@property (nonatomic, strong) CSSTransform *css_transformImp;
@property (nonatomic, strong) CSSLazyAnimationImp *css_lazyAnimationImp;
@property (nonatomic, strong) CSSGradientLayer *css_gradientLayer;
@property (nonatomic, strong) CSSBorderLayer *css_borderLayer;
@property (nonatomic, strong) UITapGestureRecognizer *css_tapGR;
@property (nonatomic, strong) UITapGestureRecognizer *css_doubleTapGR;
@property (nonatomic, strong) UILongPressGestureRecognizer *css_longPressGR;
@property (nonatomic, strong) UIPanGestureRecognizer *css_panGR;
@property (nonatomic, strong, readonly) NSMutableSet<NSString *> *css_didSetProps;

@end

@implementation UIView (CSS)

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    if ([self css_lazySetPropWithKey:key value:value]) {
        return YES;
    }
    if ([self.kr_commonWrapperView css_setPropWithKey:key value:value]) {
        return YES;
    }
    SEL selector = NSSelectorFromString( [NSString stringWithFormat:@"setCss_%@:", key] );
    if ([self respondsToSelector:selector]) {
        [self.css_didSetProps addObject:key];
        IMP imp = [self methodForSelector:selector];
        void (*func)(id, SEL, id) = (void *)imp;
        if (self.css_animationImp) {
            self.kr_reuseDisable = YES; // animation node should not be reuse
            NSString *animationKey = self.css_animationImp.animationKey;
            __weak typeof(&*self) weakSelf = self;
            [self.css_animationImp animationWithBlock:^{
                func(self, selector, value);
            } completion:^(BOOL finished) {
                if ([key isEqualToString:ANIMATION_KEY]) {
                    return ;
                }
                if ([weakSelf.css_lazyAnimationImp performAnimationWithKey:animationKey withView:weakSelf]) {
                    return ;
                }
                if (weakSelf.css_animationCompletion) {
                    weakSelf.css_animationCompletion(
                    @{
                        @"finish" : @(finished ? 1 : 0),
                        @"attr": key,
                        @"animationKey": animationKey ?: @""
                    });
                }
            }];
           
        } else {
            func(self, selector, value);
        }
        return YES;
    }
    return NO;
}

// 懒设置属性直到动画结束形成连续关键帧动画（避免异步KT侧驱动）
- (BOOL)css_lazySetPropWithKey:(NSString *)key value:(id)value {
    if (key && value
        && ![key isEqualToString:LAZY_ANIMATION_KEY]
        && self.css_lazyAnimationKey.length) { // 校验数据合法性
        [self.css_lazyAnimationImp setPropWithKey:key value:value withAnimationKey:self.css_lazyAnimationKey];
        return YES;
    }
    return NO;
}

- (NSNumber *)css_opacity {
    return objc_getAssociatedObject(self, @selector(css_opacity));
}

- (void)setCss_opacity:(NSNumber *)css_opacity {
    if (self.css_opacity != css_opacity) {
        objc_setAssociatedObject(self, @selector(css_opacity), css_opacity, OBJC_ASSOCIATION_RETAIN);
        self.alpha = !css_opacity ? 1 :  [KRConvertUtil CGFloat:css_opacity];
    }
}

- (NSNumber *)css_visibility {
    return objc_getAssociatedObject(self, @selector(css_visibility));
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    if (self.css_visibility != css_visibility) {
        objc_setAssociatedObject(self, @selector(css_visibility), css_visibility, OBJC_ASSOCIATION_RETAIN);
        self.hidden = !css_visibility ? NO : ( [css_visibility boolValue] ? NO : YES );
    }
}

- (NSNumber *)css_overflow {
    return objc_getAssociatedObject(self, @selector(css_overflow));
}

- (void)setCss_overflow:(NSNumber *)css_overflow {
    if (self.css_overflow != css_overflow) {
        objc_setAssociatedObject(self, @selector(css_overflow), css_overflow, OBJC_ASSOCIATION_RETAIN);
        self.clipsToBounds = [css_overflow boolValue] ? YES : NO;
    }
}

- (NSString *)css_backgroundColor {
    return objc_getAssociatedObject(self, @selector(css_backgroundColor));
}

- (void)setCss_backgroundColor:(NSString *)css_backgroundColor {
    if (self.css_backgroundColor !=css_backgroundColor) {
        objc_setAssociatedObject(self, @selector(css_backgroundColor), css_backgroundColor, OBJC_ASSOCIATION_RETAIN);
        self.backgroundColor = [UIView css_color:css_backgroundColor];
    }
}

- (NSNumber *)css_touchEnable {
    return objc_getAssociatedObject(self, @selector(css_touchEnable));
}

- (void)setCss_touchEnable:(NSNumber *)css_touchEnable {
    if (self.css_touchEnable != css_touchEnable) {
        objc_setAssociatedObject(self, @selector(css_touchEnable), css_touchEnable, OBJC_ASSOCIATION_RETAIN);
        self.userInteractionEnabled = css_touchEnable ? [UIView css_bool:css_touchEnable] : YES;
    }
}

- (NSString *)css_transform {
    return objc_getAssociatedObject(self, @selector(css_transform));
}

- (void)setCss_transform:(NSString *)css_transform {
    css_transform = [UIView css_string:css_transform];
    if (self.css_transform != css_transform) {
        CSSTransform *oldTransform = self.css_transformImp;
        if (css_transform == nil) {
            self.frame = CGRectZero;
            [CSSTransform resetTransformWithView:self];
        }
        objc_setAssociatedObject(self, @selector(css_transform), css_transform, OBJC_ASSOCIATION_RETAIN);
       
        self.css_transformImp = css_transform.length ? [[CSSTransform alloc] initWithCSSTransform:css_transform] : nil;
        [self.css_transformImp applyToView:self animation:self.css_animationImp oldTransform:oldTransform];
    }
}

- (CSSTransform *)css_transformImp {
    return objc_getAssociatedObject(self, @selector(css_transformImp));
}

- (void)setCss_transformImp:(CSSTransform *)css_transformImp {
    objc_setAssociatedObject(self, @selector(css_transformImp), css_transformImp, OBJC_ASSOCIATION_RETAIN);
}

- (NSString *)css_backgroundImage {
    return objc_getAssociatedObject(self, @selector(css_backgroundImage));
}

- (void)setCss_backgroundImage:(NSString *)css_backgroundImage {
    css_backgroundImage = [UIView css_string:css_backgroundImage];
    if (self.css_backgroundImage != css_backgroundImage) {
        objc_setAssociatedObject(self, @selector(css_backgroundImage), css_backgroundImage, OBJC_ASSOCIATION_RETAIN);
        [self.css_gradientLayer removeFromSuperlayer];
        self.css_gradientLayer = nil;
        if (css_backgroundImage.length) {
            self.css_gradientLayer = [[CSSGradientLayer alloc] initWithLayer:nil cssGradient:css_backgroundImage];
            [self.layer insertSublayer:self.css_gradientLayer atIndex:0];
            [self.css_gradientLayer setNeedsLayout];
        }
    }
}

- (CAGradientLayer *)css_gradientLayer {
    return  objc_getAssociatedObject(self, @selector(css_gradientLayer));
}

- (void)setCss_gradientLayer:(CAGradientLayer *)css_gradientLayer {
    objc_setAssociatedObject(self, @selector(css_gradientLayer), css_gradientLayer, OBJC_ASSOCIATION_RETAIN);
}

- (NSNumber *)css_useShadowPath {
    return objc_getAssociatedObject(self, @selector(css_useShadowPath));
}

- (void)setCss_useShadowPath:(NSNumber *)css_useShadowPath {
    objc_setAssociatedObject(self, @selector(css_useShadowPath), css_useShadowPath, OBJC_ASSOCIATION_RETAIN);
}

- (NSString *)css_boxShadow {
    return objc_getAssociatedObject(self, @selector(css_boxShadow));
}

- (void)setCss_boxShadow:(NSString *)css_boxShadow {
    css_boxShadow = [UIView css_string:css_boxShadow];
    if (self.css_boxShadow != css_boxShadow) {
        objc_setAssociatedObject(self, @selector(css_boxShadow), css_boxShadow, OBJC_ASSOCIATION_RETAIN);
        CSSBoxShadow *boxShadow = [[CSSBoxShadow alloc] initWithCSSBoxShadow:css_boxShadow];
        
        // iOS 和 macOS 都使用 CALayer.shadow* 属性
        self.layer.shadowColor = boxShadow.shadowColor.CGColor;
        self.layer.shadowRadius = boxShadow.shadowRadius;
#if TARGET_OS_OSX
        // macOS: Y 轴需要翻转，因为 CALayer 的坐标系和 flipped NSView 不同
        self.layer.shadowOffset = CGSizeMake(boxShadow.offsetX, -boxShadow.offsetY);
#else
        self.layer.shadowOffset = CGSizeMake(boxShadow.offsetX, boxShadow.offsetY);
#endif
        self.layer.shadowOpacity = css_boxShadow ? 1 : 0;
        
#if TARGET_OS_OSX
        // macOS: 同时设置 NSShadow 作为备用（用于没有 shadowPath 的普通阴影）
        // 注意：如果 KRBoxShadowView 使用了 shadowPath，NSShadow 的效果会被覆盖
        if (css_boxShadow) {
            NSShadow *shadow = [[NSShadow alloc] init];
            shadow.shadowColor = boxShadow.shadowColor;
            shadow.shadowOffset = NSMakeSize(boxShadow.offsetX, boxShadow.offsetY);
            shadow.shadowBlurRadius = boxShadow.shadowRadius;
            self.shadow = shadow;
        } else {
            self.shadow = nil;
        }
#endif
        
#if !TARGET_OS_OSX
        if (self.css_useShadowPath) {
            [self p_updateShadowPathForBoxShadow:css_boxShadow];
        }
#endif
    }
}

#if !TARGET_OS_OSX
// iOS 专用：更新 shadowPath
- (void)p_updateShadowPathForBoxShadow:(NSString *)css_boxShadow {
    if (css_boxShadow) {
        CGPathRef shadowPath = NULL;
        
        // 如果存在 clipPath，使用 clipPath 的路径作为阴影形状
        if (self.css_clipPath.length > 0) {
            CGFloat density = [UIScreen mainScreen].scale;
            UIBezierPath *clipPath = [KRConvertUtil hr_parseClipPath:self.css_clipPath density:density];
            if (clipPath) {
                shadowPath = clipPath.CGPath;
            }
        }
        
        // 如果没有 clipPath 或解析失败，使用圆角矩形
        if (!shadowPath) {
            shadowPath = [UIBezierPath bezierPathWithRoundedRect:self.layer.bounds
                                                    cornerRadius:self.layer.cornerRadius].CGPath;
        }
        
        self.layer.shadowPath = shadowPath;
    } else {
        self.layer.shadowPath = nil;
    }
}
#endif


/**
 * css_borderRadius getter
 * 获取当前设置的圆角半径字符串（格式："topLeft,topRight,bottomLeft,bottomRight"）
 */
- (NSString *)css_borderRadius {
    return objc_getAssociatedObject(self, @selector(css_borderRadius));
}

/**
 * css_borderRadius setter - 设置圆角
 *
 * 原理说明：
 * 1. 使用 CAShapeLayer 作为 layer.mask 实现四个角不同半径的圆角
 * 2. 圆角路径通过 hr_bezierPathWithRoundedRect: 构建
 *
 * 与 clipPath 的关系：
 * - clipPath 优先级高于 borderRadius
 * - 如果已有 clipPath，则不设置 borderRadius 的 mask（clipPath 决定裁剪形状）
 * - borderRadius 值仍然保存，供后续 clipPath 清空时恢复使用
 * - border 会根据有无 clipPath 选择使用哪个路径
 *
 * @param css_borderRadius 圆角字符串，格式 "topLeft,topRight,bottomLeft,bottomRight"
 */
- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    // 1. 规范化字符串
    css_borderRadius = [UIView css_string:css_borderRadius];
    
    // 2. 避免重复设置
    if (self.css_borderRadius != css_borderRadius) {
        // 3. 存储新值
        objc_setAssociatedObject(self, @selector(css_borderRadius), css_borderRadius, OBJC_ASSOCIATION_RETAIN);
        
        // 4. 如果已有 clipPath，clipPath 优先，不设置 borderRadius 的 mask
        //    但仍需保存 borderRadius 值，因为：
        //    - border 在没有 clipPath 时需要用 borderRadius 绘制
        //    - clipPath 清空时需要恢复 borderRadius 的 mask
        if (self.css_clipPath.length > 0) {
            // clipPath 优先，刷新 border 即可
            [self.css_borderLayer setNeedsLayout];
            return;
        }
        
        // 5. 解析圆角值
        CSSBorderRadius * borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:css_borderRadius];
        
        // 6. 规避默认圆角值为零时，给 view 增加 mask 致使后续子 view 内容遭剪切
        if ([borderRadius isSameBorderCornerRaidus] && borderRadius.topLeftCornerRadius < 0.0001) {
            self.layer.mask = nil;
        } else {
            // 7. 采用 CAShapeLayer + mask + UIBezierPath 支持圆角实现
            CSSShapeLayer *mask = [[CSSShapeLayer alloc] initWithBorderRadius:borderRadius];
            
            // 8. 设置 contentsScale 防锯齿
#if TARGET_OS_OSX // [macOS]
            mask.contentsScale = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
            mask.contentsScale = [UIScreen mainScreen].scale;
#endif // [macOS]
            self.layer.mask = mask;
            
            // 9. 立即把 mask 的 frame 同步到当前 bounds（防止首次 layout 前为 zero）
            if (!CGSizeEqualToSize(self.bounds.size, CGSizeZero)) {
                [self.layer.mask setFrame:self.bounds];
            }
        }
    }
    
    // 10. 刷新 border（不管 borderRadius 值是否变化都要刷新）
    [self.css_borderLayer setNeedsLayout];
}

- (CSSBorderLayer *)css_borderLayer {
    return objc_getAssociatedObject(self, @selector(css_borderLayer));
}

- (void)setCss_borderLayer:(CSSBorderLayer *)css_borderLayer {
    objc_setAssociatedObject(self, @selector(css_borderLayer), css_borderLayer, OBJC_ASSOCIATION_RETAIN);
    
}



- (NSString *)css_border {
    return objc_getAssociatedObject(self, @selector(css_border));
}

- (void)setCss_border:(NSString *)css_border {
    css_border = [UIView css_string:css_border];
    if (self.css_border != css_border) {
        objc_setAssociatedObject(self, @selector(css_border), css_border, OBJC_ASSOCIATION_RETAIN);
        [self.css_borderLayer removeFromSuperlayer];
        self.css_borderLayer = nil;
        if (css_border.length) {
            CSSBorder *border = [[CSSBorder alloc] initWithCSSBorder:css_border];
            self.css_borderLayer = [[CSSBorderLayer alloc] initWithCSSBorder:border];
            self.css_borderLayer.hostView = self;
            [self.layer addSublayer:self.css_borderLayer];
            [self.css_borderLayer setNeedsLayout];
        }
    }
}


- (NSNumber *)css_zIndex {
    return objc_getAssociatedObject(self, @selector(css_zIndex));
}

- (void)setCss_zIndex:(NSNumber *)css_zIndex {
    if (self.css_zIndex != css_zIndex) {
        objc_setAssociatedObject(self, @selector(css_zIndex), css_zIndex, OBJC_ASSOCIATION_RETAIN);
        self.layer.zPosition = [css_zIndex intValue];
    }
}

- (NSString *)css_accessibility {
    return objc_getAssociatedObject(self, @selector(css_accessibility));
}

- (void)setCss_accessibility:(NSString *)css_accessibility {
    if (self.css_accessibility != css_accessibility) {
        objc_setAssociatedObject(self, @selector(css_accessibility), css_accessibility, OBJC_ASSOCIATION_RETAIN);
        self.accessibilityLabel = css_accessibility;
        self.isAccessibilityElement = css_accessibility.length > 0;
    }
}

- (NSString *)css_accessibilityRole {
    return objc_getAssociatedObject(self, @selector(css_accessibilityRole));
}

- (void)setCss_accessibilityRole:(NSString *)css_accessibilityRole {
    if (self.css_accessibilityRole != css_accessibilityRole) {
        objc_setAssociatedObject(self, @selector(css_accessibilityRole), css_accessibilityRole, OBJC_ASSOCIATION_RETAIN);
        self.accessibilityTraits = [KRConvertUtil kr_accessibilityTraits:css_accessibilityRole];
        self.isAccessibilityElement = self.accessibilityTraits != UIAccessibilityTraitNone;
    }
}

- (void)setCss_scrollIndex:(NSNumber *)css_scrollIndex {
    objc_setAssociatedObject(self, @selector(css_scrollIndex), css_scrollIndex, OBJC_ASSOCIATION_RETAIN);
}
// 在可滚动容器中的位置
- (NSNumber *)css_scrollIndex {
    return objc_getAssociatedObject(self, @selector(css_scrollIndex));
}

- (NSNumber *)css_shouldRasterize {
    return objc_getAssociatedObject(self, @selector(css_shouldRasterize));
}

- (void)setCss_shouldRasterize:(NSNumber *)css_shouldRasterize {
    objc_setAssociatedObject(self, @selector(css_shouldRasterize), css_shouldRasterize, OBJC_ASSOCIATION_RETAIN);
    self.layer.shouldRasterize = [css_shouldRasterize intValue] == 1;
    #if TARGET_OS_OSX // [macOS]
    CGFloat scale = 1.0;
    NSScreen *screen = [NSScreen mainScreen];
    if (screen) {
        scale = screen.backingScaleFactor > 0 ? screen.backingScaleFactor : 1.0;
    }
    self.layer.rasterizationScale = scale;
    #else
    self.layer.rasterizationScale = [UIScreen mainScreen].scale;
    #endif // [macOS]
}

- (NSNumber *)css_turboDisplayAutoUpdateEnable {
    return objc_getAssociatedObject(self, @selector(css_turboDisplayAutoUpdateEnable));
}

- (void)setCss_turboDisplayAutoUpdateEnable:(NSNumber *)css_turboDisplayAutoUpdateEnable {
    objc_setAssociatedObject(self, @selector(css_turboDisplayAutoUpdateEnable),
                             css_turboDisplayAutoUpdateEnable, OBJC_ASSOCIATION_RETAIN);
}

- (NSNumber *)css_autoDarkEnable {
    return objc_getAssociatedObject(self, @selector(css_autoDarkEnable));
}

- (void)setCss_autoDarkEnable:(NSNumber *)css_autoDarkEnable {
    if (self.css_autoDarkEnable != css_autoDarkEnable) {
        objc_setAssociatedObject(self, @selector(css_autoDarkEnable), css_autoDarkEnable, OBJC_ASSOCIATION_RETAIN);
        #if TARGET_OS_OSX // [macOS]
        if (@available(macos 10.14, *)) {
            if ([css_autoDarkEnable boolValue]) {
                // 跟随系统
                self.appearance = nil;
            } else {
                // 强制浅色
                self.appearance = [NSAppearance appearanceNamed:NSAppearanceNameAqua];
            }
        }
        #else
        if (@available(iOS 13.0, *)) {
            if ([css_autoDarkEnable boolValue]) {
                self.overrideUserInterfaceStyle = UIUserInterfaceStyleUnspecified;
            } else {
                self.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
            }
        }
        #endif // [macOS]
    }
}

- (void)setCss_interfaceStyle:(NSString *)style {
    #if TARGET_OS_OSX // [macOS]
    if (@available(macos 10.14, *)) {
        NSString *lower = style.lowercaseString;
        if ([lower isEqualToString:@"dark"]) {
            self.appearance = [NSAppearance appearanceNamed:NSAppearanceNameDarkAqua];
        } else if ([lower isEqualToString:@"light"]) {
            self.appearance = [NSAppearance appearanceNamed:NSAppearanceNameAqua];
        } else {
            // 未指定：跟随系统
            self.appearance = nil;
        }
    }
    #else
    if (@available(iOS 13.0, *)) {
        self.overrideUserInterfaceStyle = [KRConvertUtil KRUserInterfaceStyle:style];
    }
    #endif // [macOS]
}

- (NSString *)css_animation {
    return objc_getAssociatedObject(self, @selector(css_animation));
}

- (void)setCss_animation:(NSString *)css_animation {
    if (self.css_animation != css_animation) {
        objc_setAssociatedObject(self, @selector(css_animation), css_animation, OBJC_ASSOCIATION_RETAIN);
        self.css_animationImp = css_animation.length ? [[CSSAnimation alloc] initWithCSSAnimation:css_animation] : nil;;
    }
}

- (CSSAnimation *)css_animationImp {
    return objc_getAssociatedObject(self, @selector(css_animationImp));
}

- (void)setCss_animationImp:(CSSAnimation *)css_animationImp {
    objc_setAssociatedObject(self, @selector(css_animationImp), css_animationImp, OBJC_ASSOCIATION_RETAIN);
}

- (NSString *)css_lazyAnimationKey {
    return objc_getAssociatedObject(self, @selector(css_lazyAnimationKey));
}

- (void)setCss_lazyAnimationKey:(NSString *)css_lazyAnimationKey {
    if (self.css_lazyAnimationKey != css_lazyAnimationKey) {
        objc_setAssociatedObject(self, @selector(css_lazyAnimationKey), css_lazyAnimationKey, OBJC_ASSOCIATION_RETAIN);
        // reset时调用 self.css_lazyAnimationKey = nil时触发css_lazyAnimationKey == nil条件
        if (css_lazyAnimationKey == nil) {
            self.css_lazyAnimationImp = nil;
        } else if (self.css_lazyAnimationImp == nil) {
            self.css_lazyAnimationImp = [[CSSLazyAnimationImp alloc] init];
        }
    }
}

- (CSSLazyAnimationImp *)css_lazyAnimationImp {
    return objc_getAssociatedObject(self, @selector(css_lazyAnimationImp));
}

- (void)setCss_lazyAnimationImp:(CSSLazyAnimationImp *)css_lazyAnimationImp {
    objc_setAssociatedObject(self, @selector(css_lazyAnimationImp), css_lazyAnimationImp, OBJC_ASSOCIATION_RETAIN);
}

/**
 * css_clipPath getter
 * 获取当前设置的裁剪路径字符串
 */
- (NSString *)css_clipPath {
    return objc_getAssociatedObject(self, @selector(css_clipPath));
}

/**
 * css_clipPathLayer getter
 * 获取当前的裁剪路径图层（CSSClipPathLayer 实例）
 * 此属性用于 CSSBorderLayer 判断是否需要使用 clipPath 绘制边框
 */
- (CAShapeLayer *)css_clipPathLayer {
    return objc_getAssociatedObject(self, @selector(css_clipPathLayer));
}

/**
 * css_clipPathLayer setter
 * 保存裁剪路径图层的引用，供 border 等其他属性使用
 */
- (void)setCss_clipPathLayer:(CAShapeLayer *)css_clipPathLayer {
    objc_setAssociatedObject(self, @selector(css_clipPathLayer), css_clipPathLayer, OBJC_ASSOCIATION_RETAIN);
}

/**
 * css_clipPath setter - 设置自定义裁剪路径
 *
 * 原理说明：
 * 1. clipPath 使用 CAShapeLayer 作为 layer.mask 实现任意形状裁剪
 * 2. clipPath 优先级高于 borderRadius：
 *    - 当 clipPath 存在时，clipPath 决定裁剪形状
 *    - borderRadius 的 mask 效果被 clipPath 取代
 * 3. 当 clipPath 清空时，需要恢复 borderRadius 的 mask（如果有）
 *
 * 与其他属性的关系：
 * - layer.mask 只能有一个，clipPath 和 borderRadius 互斥
 * - border 需要感知 clipPath：边框应该沿着裁剪路径绘制
 * - boxShadow 需要更新 shadowPath：阴影应该沿着裁剪路径绘制
 * - backgroundImage 不冲突：使用 sublayer
 *
 * @param css_clipPath 路径字符串，如 "M 0 40 L 40 0 L 80 40 L 40 80 Z"
 */
- (void)setCss_clipPath:(NSString *)css_clipPath {
    // 1. 规范化字符串（处理 NSNumber 等类型）
    css_clipPath = [UIView css_string:css_clipPath];
    
    // 2. 避免重复设置相同的值
    NSString *oldClipPath = self.css_clipPath;
    if ([oldClipPath isEqualToString:css_clipPath]) {
        return;
    }
    
    // 3. 存储新的 clipPath 值
    objc_setAssociatedObject(self, @selector(css_clipPath), css_clipPath, OBJC_ASSOCIATION_COPY_NONATOMIC);
    
    // 4. 处理 clipPath 清空的情况
    if (!css_clipPath || css_clipPath.length == 0) {
        // 4.1 清空 clipPathLayer 引用
        self.css_clipPathLayer = nil;
        
        // 4.2 检查是否需要恢复 borderRadius 的 mask
        if (self.css_borderRadius.length > 0) {
            // 重新触发 borderRadius 设置，恢复圆角 mask
            // 先清空再设置，强制触发 setter 逻辑
            NSString *borderRadius = self.css_borderRadius;
            objc_setAssociatedObject(self, @selector(css_borderRadius), nil, OBJC_ASSOCIATION_RETAIN);
            self.css_borderRadius = borderRadius;
        } else {
            // 没有 borderRadius，直接清除 mask
            self.layer.mask = nil;
        }
        
        // 4.3 如果有 shadowPath，恢复为圆角矩形
        if (self.layer.shadowPath && self.css_useShadowPath) {
            
#if TARGET_OS_OSX
            CGFloat density = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
            CGFloat density = [UIScreen mainScreen].scale;
#endif
            UIBezierPath *clipPath = [KRConvertUtil hr_parseClipPath:self.css_clipPath density:density];
            if (clipPath) {
                self.layer.shadowPath = clipPath.CGPath;
            } else {
#if TARGET_OS_OSX
                CGPathRef p = CGPathCreateWithRoundedRect(self.layer.bounds, self.layer.cornerRadius, self.layer.cornerRadius, NULL);
                self.layer.shadowPath = p;
                CGPathRelease(p);
#else
                self.layer.shadowPath = [[UIBezierPath bezierPathWithRoundedRect:self.layer.bounds cornerRadius:self.layer.cornerRadius] CGPath];
#endif
            }
        }
                    
        // 4.4 刷新 border，使其使用 borderRadius 路径
        [self.css_borderLayer setNeedsLayout];
        return;
    }
    
    // 5. 创建 clipPath 的 mask 图层
    CSSClipPathLayer *mask = [[CSSClipPathLayer alloc] initWithClipPath:css_clipPath hostView:self];
    
    // 6. 设置 contentsScale 防止锯齿
#if TARGET_OS_OSX
    mask.contentsScale = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
    mask.contentsScale = [UIScreen mainScreen].scale;
#endif
    
    // 7. 设置为 layer 的 mask
    //    这会覆盖 borderRadius 设置的 CSSShapeLayer mask
    self.layer.mask = mask;
    
    // 8. 保存引用，供 CSSBorderLayer 使用
    self.css_clipPathLayer = mask;
    
    // 9. 如果 View 已有尺寸，立即同步 mask 的 frame
    //    确保首次设置时裁剪效果立即生效
    if (!CGSizeEqualToSize(self.bounds.size, CGSizeZero)) {
        [self.layer.mask setFrame:self.bounds];
    }
    
    // 10. 如果有 shadowPath，更新为 clipPath 的形状
    //     这样阴影形状才会和裁剪形状一致（如星形阴影）
    if (self.layer.shadowPath && self.css_useShadowPath) {
#if TARGET_OS_OSX
        CGFloat density = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
        CGFloat density = [UIScreen mainScreen].scale;
#endif
        UIBezierPath *clipPathBezier = [KRConvertUtil hr_parseClipPath:css_clipPath density:density];
        if (clipPathBezier) {
            self.layer.shadowPath = clipPathBezier.CGPath;
        }
    }
    
    // 11. 刷新 border，使其使用新的 clipPath 路径
    [self.css_borderLayer setNeedsLayout];
}




- (NSValue *)css_frame {
    return objc_getAssociatedObject(self, @selector(css_frame));
}

- (void)setCss_frame:(NSValue *)css_frame {
    objc_setAssociatedObject(self, @selector(css_frame), css_frame, OBJC_ASSOCIATION_RETAIN);
    if (!css_frame) {
        self.frame = CGRectZero;
        return ;
    }
    CGRect frame =  [css_frame CGRectValue];
    [self.layer.sublayers enumerateObjectsUsingBlock:^(__kindof CALayer * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                if (![obj isMemberOfClass:[CALayer class]]) {
                    [obj setNeedsLayout];
                }
     }];
    dispatch_block_t setFrameBlock = ^{
        [CSSTransform resetTransformWithView:self];
        self.frame = frame;
        [self p_boundsDidChanged];
        [self.css_transformImp applyToView:self]; // 尺寸发生变化，需要同步2D形变
    };
    // 兼容正在做transform动画场景时修改frame
    if (self.layer.animationKeys.count && !CGAffineTransformEqualToTransform(self.transform, CGAffineTransformIdentity)) {
        // 原子性设置frame，使得UIView动画可以生效的同时，也可以避免影响transform动画
        self.bounds = CGRectMake(0, 0, CGRectGetWidth(frame), CGRectGetHeight(frame));
        self.center = CGPointMake(CGRectGetMidX(frame), CGRectGetMidY(frame));
        // 无动画设置最终的frame，避免影响transform动画
        #if TARGET_OS_OSX // [macOS]
        [CATransaction begin];
        [CATransaction setDisableActions:YES];
        setFrameBlock();
        [CATransaction commit];
        #else
        [UIView performWithoutAnimation:setFrameBlock];
        #endif // [macOS]
    } else {
        setFrameBlock();
    }
    [self p_limitMaxBorderRadisuIfNeed];
}

- (void)p_boundsDidChanged {
    [self.layer.mask setFrame:self.bounds];
    if (self.layer.shadowPath) {
        // 如果存在 clipPath，shadowPath 应该使用 clipPath 的路径
        // 这样阴影形状才会和裁剪形状一致
        if (self.css_clipPath.length > 0) {
#if TARGET_OS_OSX
            CGFloat density = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
            CGFloat density = [UIScreen mainScreen].scale;
#endif
            UIBezierPath *clipPath = [KRConvertUtil hr_parseClipPath:self.css_clipPath density:density];
            if (clipPath) {
                self.layer.shadowPath = clipPath.CGPath;
            }
        } else {
            #if TARGET_OS_OSX // [macOS]
            CGPathRef path = CGPathCreateWithRoundedRect(self.layer.bounds, self.layer.cornerRadius, self.layer.cornerRadius, NULL);
            self.layer.shadowPath = path;
            CGPathRelease(path);
            #else
            self.layer.shadowPath = [[UIBezierPath bezierPathWithRoundedRect:self.layer.bounds cornerRadius:self.layer.cornerRadius] CGPath];
            #endif // [macOS]
        }
    }
}

/// 对齐安卓圆角最大为半圆
- (void)p_limitMaxBorderRadisuIfNeed {
    CGFloat minLength = MIN(CGRectGetHeight(self.bounds), CGRectGetWidth(self.bounds));
    CGFloat maxRadius = minLength / 2;
    if (self.css_borderRadius.length && minLength && maxRadius < self.layer.cornerRadius) {
        self.css_borderRadius = [NSString stringWithFormat:@"%.2lf,%.2lf,%.2lf,%.2lf", maxRadius, maxRadius, maxRadius, maxRadius];
        [self.css_borderLayer setNeedsLayout];// 同步边框
    }
}


- (UITapGestureRecognizer *)css_tapGR {
    return objc_getAssociatedObject(self, @selector(css_tapGR));
}

- (void)setCss_tapGR:(UITapGestureRecognizer *)css_tapGR {
    objc_setAssociatedObject(self, @selector(css_tapGR), css_tapGR, OBJC_ASSOCIATION_RETAIN);
}

- (UITapGestureRecognizer *)css_doubleTapGR {
    return objc_getAssociatedObject(self, @selector(css_doubleTapGR));
}

- (void)setCss_doubleTapGR:(UITapGestureRecognizer *)css_doubleTapGR {
    objc_setAssociatedObject(self, @selector(css_doubleTapGR), css_doubleTapGR, OBJC_ASSOCIATION_RETAIN);
}

- (UILongPressGestureRecognizer *)css_longPressGR {
    return objc_getAssociatedObject(self, @selector(css_longPressGR));
}

- (void)setCss_longPressGR:(UILongPressGestureRecognizer *)css_longPressGR {
    objc_setAssociatedObject(self, @selector(css_longPressGR), css_longPressGR, OBJC_ASSOCIATION_RETAIN);
}

- (UIPanGestureRecognizer *)css_panGR {
    return objc_getAssociatedObject(self, @selector(css_panGR));
}

- (void)setCss_panGR:(UIPanGestureRecognizer *)css_panGR {
    objc_setAssociatedObject(self, @selector(css_panGR), css_panGR, OBJC_ASSOCIATION_RETAIN);
}

- (KuiklyRenderCallback)css_click {
    return objc_getAssociatedObject(self, @selector(css_click));
}

- (void)setCss_click:(KuiklyRenderCallback)css_click {
    if (self.css_click != css_click) {
        objc_setAssociatedObject(self, @selector(css_click), css_click, OBJC_ASSOCIATION_RETAIN);
        if (self.css_tapGR) {
            [self removeGestureRecognizer:self.css_tapGR];
            self.css_tapGR = nil;
        }
        if (css_click != nil) {
            self.css_tapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(css_onClickTapWithSender:)];
            [self addGestureRecognizer:self.css_tapGR];
            #if TARGET_OS_OSX // [macOS]
            // macOS NSGestureRecognizer 无 requireGestureRecognizerToFail 方法，使用 delegate 方式处理手势依赖
            #else
            if (self.css_doubleTapGR) {
                [self.css_tapGR requireGestureRecognizerToFail:self.css_doubleTapGR];
            }
            #endif // [macOS]
            if (!self.css_touchEnable) {
                self.userInteractionEnabled = YES;
            }
        }
    }
}

- (KuiklyRenderCallback)css_doubleClick {
    return objc_getAssociatedObject(self, @selector(css_doubleClick));
}

- (void)setCss_doubleClick:(KuiklyRenderCallback)css_doubleClick {
    if (self.css_doubleClick != css_doubleClick) {
        objc_setAssociatedObject(self, @selector(css_doubleClick), css_doubleClick, OBJC_ASSOCIATION_RETAIN);
        if (self.css_doubleTapGR) {
            [self removeGestureRecognizer:self.css_doubleTapGR];
            self.css_doubleTapGR = nil;
        }
        if (css_doubleClick != nil) {
            #if TARGET_OS_OSX // [macOS]
            self.css_doubleTapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(css_onDoubleClickWithSender:)];
            // macOS 使用 NSClickGestureRecognizer，设置 numberOfClicksRequired 识别双击
            ((NSClickGestureRecognizer *)self.css_doubleTapGR).numberOfClicksRequired = 2;
            #else
            self.css_doubleTapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(css_onDoubleClickWithSender:)];
            self.css_doubleTapGR.numberOfTapsRequired = 2;
            #endif // [macOS]
            [self addGestureRecognizer:self.css_doubleTapGR];
            #if TARGET_OS_OSX // [macOS]
            // macOS NSGestureRecognizer 无 requireGestureRecognizerToFail 方法
            #else
            if (self.css_tapGR) {
                [self.css_tapGR requireGestureRecognizerToFail:self.css_doubleTapGR];
            }
            #endif // [macOS]
            if (!self.css_touchEnable) {
                self.userInteractionEnabled = YES;
            }
        }
    }
}

- (KuiklyRenderCallback)css_longPress {
    return objc_getAssociatedObject(self, @selector(css_longPress));
}

- (void)setCss_longPress:(KuiklyRenderCallback)css_longPress {
    if (self.css_longPress != css_longPress) {
        objc_setAssociatedObject(self, @selector(css_longPress), css_longPress, OBJC_ASSOCIATION_RETAIN);
        if (self.css_longPressGR) {
            [self removeGestureRecognizer:self.css_longPressGR];
            self.css_longPressGR = nil;
        }
        if (css_longPress != nil) {
            self.css_longPressGR = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(css_onLongPressWithSender:)];
           
            [self addGestureRecognizer:self.css_longPressGR];
            if (!self.css_touchEnable) {
                self.userInteractionEnabled = YES;
            }
        }
    }
}

- (KuiklyRenderCallback)css_pan {
    return objc_getAssociatedObject(self, @selector(css_pan));
}

- (void)setCss_pan:(KuiklyRenderCallback)css_pan {
    if (self.css_pan != css_pan) {
        objc_setAssociatedObject(self, @selector(css_pan), css_pan, OBJC_ASSOCIATION_RETAIN);
        if (self.css_panGR) {
            [self removeGestureRecognizer:self.css_panGR];
            self.css_panGR = nil;
        }
        if (css_pan != nil) {
            self.css_panGR = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(css_onPanWithSender:)];
            [self addGestureRecognizer:self.css_panGR];
            if (!self.css_touchEnable) {
                self.userInteractionEnabled = YES;
            }
        }
    }
}

- (KuiklyRenderCallback)css_animationCompletion {
    return objc_getAssociatedObject(self, @selector(css_animationCompletion));
}

- (void)setCss_animationCompletion:(KuiklyRenderCallback)css_animationCompletion {
    if (self.css_animationCompletion != css_animationCompletion) {
        objc_setAssociatedObject(self, @selector(css_animationCompletion), css_animationCompletion, OBJC_ASSOCIATION_RETAIN);
    }
}


- (BOOL)kr_canCancelInScrollView {
    return [objc_getAssociatedObject(self, @selector(kr_canCancelInScrollView)) boolValue];
}

- (void)setKr_canCancelInScrollView:(BOOL)kr_canCancelInScrollView {
    objc_setAssociatedObject(self, @selector(kr_canCancelInScrollView), @(kr_canCancelInScrollView), OBJC_ASSOCIATION_RETAIN);
}

- (BOOL)kr_reuseDisable {
    return [objc_getAssociatedObject(self, @selector(kr_reuseDisable)) boolValue];
}

- (void)setKr_reuseDisable:(BOOL)kr_reuseDisable {
    objc_setAssociatedObject(self, @selector(kr_reuseDisable), @(kr_reuseDisable), OBJC_ASSOCIATION_RETAIN);
}

- (void)css_onClickTapWithSender:(UIGestureRecognizer *)sender {
    CGPoint location = [sender locationInView:self];
    #if TARGET_OS_OSX
    CGPoint pageLocation = [sender locationInView:nil]; // 窗口坐标
    #else
    CGPoint pageLocation = [self kr_convertLocalPointToRenderRoot:location];
    #endif
    NSDictionary *param = @{
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
    };
    if (self.css_click) {
        self.css_click(param);
    }
}

- (void)css_onDoubleClickWithSender:(UIGestureRecognizer *)sender {
    CGPoint location = [sender locationInView:self];
    #if TARGET_OS_OSX
    CGPoint pageLocation = [sender locationInView:nil];
    #else
    CGPoint pageLocation = [self kr_convertLocalPointToRenderRoot:location];
    #endif
    NSDictionary *param = @{
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
    };
    if (self.css_doubleClick) {
        self.css_doubleClick(param);
    }
}

- (void)css_onLongPressWithSender:(UILongPressGestureRecognizer *)sender {
    NSDictionary *config = @{
        @(UIGestureRecognizerStateBegan): @"start",
        @(UIGestureRecognizerStateChanged): @"move",
    };
    CGPoint location = [sender locationInView:self];
    #if TARGET_OS_OSX
    CGPoint pageLocation = [sender locationInView:nil];
    #else
    CGPoint pageLocation = [self kr_convertLocalPointToRenderRoot:location];
    #endif
    NSDictionary *param = @{
        @"state": config[@(sender.state)] ? : @"end",
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
        @"isCancel": @(sender.state == UIGestureRecognizerStateCancelled)   // 增加isCancel参数回传
    };
    if (self.css_longPress) {
        self.css_longPress(param);
    }
}

- (CGPoint)kr_convertLocalPointToRenderRoot:(CGPoint)point{
    UIView *root = nil;
    if ([self respondsToSelector:@selector(hr_rootView)]){
        root = [self performSelector:@selector(hr_rootView)];
    } else if ([self.superview respondsToSelector:@selector(hr_rootView)]){
        root = [self.superview performSelector:@selector(hr_rootView)];
    }
    
    return [self convertPoint:point toView:root];
}

- (void)css_onPanWithSender:(UIPanGestureRecognizer *)sender {
    NSDictionary *config = @{
        @(UIGestureRecognizerStateBegan): @"start",
        @(UIGestureRecognizerStateChanged): @"move",
    };
    
    CGPoint location = [sender locationInView:self];
    #if TARGET_OS_OSX
    CGPoint pageLocation = [sender locationInView:nil];
    #else
    CGPoint pageLocation = [self kr_convertLocalPointToRenderRoot:location];
    #endif
    NSDictionary *param = @{
        @"state": config[@(sender.state)] ? : @"end",
        @"x": @(location.x),
        @"y": @(location.y),
        @"pageX": @(pageLocation.x),
        @"pageY": @(pageLocation.y),
    };
    if (self.css_pan) {
        self.css_pan(param);
    }
}


+ (NSString *)css_string:(id)value {
    if ([value isKindOfClass:[NSString class]]) {
        return value;
    } else if([value isKindOfClass:[NSObject class]] && [value respondsToSelector:@selector(stringValue)]) {
        return (NSString *)[value stringValue];
    }
    return nil;
}

+ (BOOL)css_bool:(id)value {
    if ([value isKindOfClass:[NSString class]] && [value isEqualToString:@"true"]) {
        return YES;
    }
    if ([value isKindOfClass:[NSString class]] && [value isEqualToString:@"false"]) {
        return NO;
    }
    if([value isKindOfClass:[NSObject class]] && [value respondsToSelector:@selector(boolValue)]) {
        return [(NSString *)value boolValue];
    }
    return NO;
}

+ (UIColor *)css_color:(id)value {
    if ([value isKindOfClass:[NSString class]]) {
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_colorWithValue:)]) {
            UIColor *color = [[KuiklyRenderBridge componentExpandHandler] hr_colorWithValue:value];
            if (color) {
                return color;
            }
        }
        return [KRConvertUtil UIColor:@([(NSString *)value longLongValue])];
    }
    return [KRConvertUtil UIColor:value];
}


- (NSMutableSet<NSString *> *)css_didSetProps {
    NSMutableSet<NSString *> *props = objc_getAssociatedObject(self, @selector(css_didSetProps));
    if (!props) {
        props = [[NSMutableSet alloc] init];
        objc_setAssociatedObject(self, @selector(css_didSetProps), props, OBJC_ASSOCIATION_RETAIN);
    }
    return props;
}

- (void)css_reset {
    self.css_animation = nil;
    [self.css_didSetProps removeObject:@"animation"];
    for (NSString *propKey in [self.css_didSetProps copy]) {
        id resetValue = nil; // reset vlaue
        [self css_setPropWithKey:propKey value:resetValue];
    }
    [self.layer removeAllAnimations];
    [self.css_didSetProps removeAllObjects];
}

- (KRBoxShadowView *)kr_commonWrapperView {
    return  objc_getAssociatedObject(self, @selector(kr_commonWrapperView));
}

- (void)setKr_commonWrapperView:(KRBoxShadowView *)kr_boxShadowView {
    objc_setAssociatedObject(self, @selector(kr_commonWrapperView), kr_boxShadowView, OBJC_ASSOCIATION_RETAIN);
}

- (NSNumber *)css_wrapperBoxShadowView {
    return objc_getAssociatedObject(self, @selector(css_wrapperBoxShadowView));
}
// 当有圆角和阴影同时存在时，iOS上阴影因clipToBounds为YES而失效，故需要wrapperBoxShadowView解决
- (void)setCss_wrapperBoxShadowView:(NSNumber *)css_wrapperBoxShadowView {
    if (self.css_wrapperBoxShadowView != css_wrapperBoxShadowView) {
        objc_setAssociatedObject(self, @selector(css_wrapperBoxShadowView), css_wrapperBoxShadowView, OBJC_ASSOCIATION_RETAIN);
        if ([css_wrapperBoxShadowView boolValue] && !self.kr_commonWrapperView) {
            self.kr_commonWrapperView = [[KRBoxShadowView alloc] initWithContentView:self];
        }
    }
}

#pragma mark - KuiklyRenderViewLifyCycleProtocol

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    UIView *view = subView.kr_commonWrapperView;
    if (view) {
        [self insertSubview:view atIndex:index];
    } else {
        [self insertSubview:subView atIndex:index];
    }
}

- (void)hrv_removeFromSuperview {
    [(self.kr_commonWrapperView ?: self) removeFromSuperview];
}

#pragma mark - view extension
// 设置新瞄点后且frame保持不变
- (void)hr_setAnchorPointAndKeepFrame:(CGPoint)anchorPoint {
    CGPoint oldAnchorPoint = self.layer.anchorPoint;
    CGPoint oldPosition = self.layer.position;
    CGPoint newPosition = CGPointMake(oldPosition.x + (anchorPoint.x - oldAnchorPoint.x) * self.bounds.size.width,
                                      oldPosition.y + (anchorPoint.y - oldAnchorPoint.y) * self.bounds.size.height);
    self.layer.anchorPoint = anchorPoint;
    self.layer.position = newPosition;
}

- (NSString *)css_accessibilityInfo {
    return objc_getAssociatedObject(self, @selector(css_accessibilityInfo));
}

- (void)setCss_accessibilityInfo:(NSString *)css_accessibilityInfo {
    if (self.css_accessibilityInfo != css_accessibilityInfo) {
        objc_setAssociatedObject(self, @selector(css_accessibilityInfo), css_accessibilityInfo, OBJC_ASSOCIATION_RETAIN);
        if (css_accessibilityInfo && css_accessibilityInfo.length > 0) {
            // 解析 "1 1" 格式的值
            NSArray<NSString *> *values = [css_accessibilityInfo componentsSeparatedByString:@" "];
            if (values.count >= 2) {
                BOOL isClickable = [values[0] boolValue];
                BOOL isLongClickable = [values[1] boolValue];

                // 设置无障碍特性
                UIAccessibilityTraits traits = UIAccessibilityTraitNone;
                NSMutableArray<NSString *> *hints = [NSMutableArray array];

                if (isClickable) {
                    traits |= UIAccessibilityTraitButton;
                    [hints addObject:@"点按两次即可激活"];
                }

                if (isLongClickable) {
                    traits |= UIAccessibilityTraitAllowsDirectInteraction;
                    [hints addObject:@"点按两次并按住即可长按"];
                }

                // 合并多个 hint
                if (hints.count > 0) {
                    self.accessibilityHint = [hints componentsJoinedByString:@"，"];
                }

                // 如果有点击功能，确保组件是可访问的
                if (isClickable || isLongClickable) {
                    self.accessibilityTraits = traits;
                }
            }
        }
    }
}

@end



/**
 * CSSClipPathLayer - 自定义裁剪路径图层
 *
 * 原理说明：
 * 1. 继承自 CAShapeLayer，用作 UIView.layer.mask 实现自定义形状裁剪
 * 2. 当 frame 变化时自动重新计算路径（响应 View 尺寸变化）
 * 3. 与 CSSShapeLayer（圆角裁剪）互斥：同一时刻只能有一个 mask
 *
 * 使用场景：
 * - 实现星形、心形、多边形等自定义形状的裁剪
 * - 配合 css_clipPath 属性使用
 *
 * 优先级规则：
 * - clipPath 优先级高于 borderRadius
 * - 当 clipPath 存在时，忽略 borderRadius 的 mask 效果
 */
@implementation CSSClipPathLayer

/**
 * 初始化裁剪路径图层
 * @param clipPath 路径字符串（如 "M 0 40 L 40 0 L 80 40 L 40 80 Z"）
 * @param hostView 宿主视图，用于在 frame 变化时获取新的尺寸
 */
- (instancetype)initWithClipPath:(NSString *)clipPath hostView:(UIView *)hostView {
    if (self = [super init]) {
        _clipPathData = [clipPath copy];
        _hostView = hostView;
        // 设置 fillColor 为不透明颜色，这是 CAShapeLayer 作为 mask 正确工作的关键
        // mask 的工作原理：mask 的 alpha 通道决定哪些区域可见
        // fillColor 不透明的区域会显示内容，透明的区域会被裁剪掉
        self.fillColor = [UIColor blackColor].CGColor;
    }
    return self;
}

/**
 * 重写 setFrame: 方法
 * 当 mask 的 frame 变化时（通常是宿主 View 尺寸变化），重新计算路径
 * 这是响应式更新的关键：确保裁剪路径始终匹配 View 的尺寸
 */
- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    [self updatePath];
}

/**
 * 更新裁剪路径
 * 将路径字符串解析为 UIBezierPath，并设置为 CAShapeLayer 的 path
 */
- (void)updatePath {
    // 1. 路径数据为空时清除 path
    if (!_clipPathData || _clipPathData.length == 0) {
        self.path = nil;
        return;
    }
    
    // 2. 获取屏幕 density（iOS 上实际不使用，但为了 API 兼容性保留）
    //    注意：传入的坐标已经是 dp/point 值，不需要再乘以 scale
#if TARGET_OS_OSX
    CGFloat density = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
    CGFloat density = [UIScreen mainScreen].scale;
#endif
    
    // 3. 解析路径字符串为 UIBezierPath
    UIBezierPath *path = [KRConvertUtil hr_parseClipPath:_clipPathData density:density];
    
    if (path) {
        // 4. 设置 CAShapeLayer 的 path 属性
#if TARGET_OS_OSX
        if (@available(macos 14.0, *)) {
            self.path = path.CGPath;
        } else {
            // macOS 14.0 以下版本退化为矩形裁剪
            CGMutablePathRef p = CGPathCreateMutable();
            CGPathAddRect(p, NULL, self.bounds);
            self.path = p;
            CGPathRelease(p);
        }
#else
        self.path = path.CGPath;
#endif
    }
}

/**
 * 重写 setContents: 方法
 * 禁用隐式动画，避免 mask 变化时出现闪烁
 */
- (void)setContents:(id)contents {
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    [super setContents:contents];
    [CATransaction commit];
}

@end



@implementation CSSGradientLayer {
    CSSGradientDirection _diretion;
    NSMutableArray<UIColor *> *_colors;
    NSMutableArray<NSNumber *> *_locations;
}


- (instancetype)initWithLayer:(id)layer cssGradient:(NSString *)cssGradient {
    if (self = [super initWithLayer:layer]) {
        [self p_tryToParseWithLinearGradient:cssGradient];
    }
    return self;
    
}

- (BOOL)p_tryToParseWithLinearGradient:(NSString *)cssGricent {
    NSString *lineargradientPrefix = @"linear-gradient(";
    if (![cssGricent hasPrefix:lineargradientPrefix]) {
        return NO;
    }
    cssGricent = [cssGricent substringWithRange:NSMakeRange(lineargradientPrefix.length
                              , cssGricent.length - lineargradientPrefix.length - 1)];
    NSArray<NSString *>* splits = [cssGricent componentsSeparatedByString:@","];
    _diretion = [splits.firstObject intValue];
    _colors = [[NSMutableArray alloc] init];
    _locations = [[NSMutableArray alloc] init];
    for (int i = 1; i < splits.count; i++) {
        NSString *colorStopStr = splits[i];
        NSArray<NSString *> *colorAndStop = [colorStopStr componentsSeparatedByString:@" "];
        UIColor *color = [UIView css_color:(NSString *)colorAndStop.firstObject];
        [_colors addObject:(__bridge id)color.CGColor];
        [_locations addObject:@([colorAndStop.lastObject floatValue])];
    }
    return YES;
}

- (void)setContents:(id)contents {
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    [super setContents: contents];
    [CATransaction commit];
}

- (void)layoutSublayers {
    [super layoutSublayers];
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    
   
    if (!CGSizeEqualToSize(self.bounds.size, self.superlayer.bounds.size)) {
        self.frame = self.superlayer.bounds;
    }
    self.zPosition = -1; // 显示层级最低
    if (!self.colors) {
        self.colors = _colors;
    }
    if (!self.locations) {
        self.locations = _locations;
    }
    [KRConvertUtil hr_setStartPointAndEndPointWithLayer:self direction:_diretion];
    [CATransaction commit];
    
}


@end


//

@implementation CSSShapeLayer {
    CSSBorderRadius *_borderRadius;
}

- (instancetype)initWithBorderRadius:(CSSBorderRadius *)borderRadius {
    if (self = [super init]) {
        _borderRadius = borderRadius;
    }
    return self;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    {
        UIBezierPath *path = [KRConvertUtil hr_bezierPathWithRoundedRect:self.bounds
                                            topLeftCornerRadius:_borderRadius.topLeftCornerRadius topRightCornerRadius:_borderRadius.topRightCornerRadius bottomLeftCornerRadius:_borderRadius.bottomLeftCornerRadius bottomRightCornerRadius:_borderRadius.bottomRightCornerRadius];
        #if TARGET_OS_OSX // [macOS]
        if (@available(macos 14.0, *)) {
            self.path = path.CGPath;
        } else {
            // 低于 14：退化为矩形
            CGMutablePathRef p = CGPathCreateMutable();
            CGPathAddRect(p, NULL, self.bounds);
            self.path = p;
            CGPathRelease(p);
        }
        #else // [macOS]
        self.path = path.CGPath;
        #endif // [macOS]
    }
}

- (void)setContents:(id)contents {
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    [super setContents: contents];
    [CATransaction commit];
}


@end

@implementation CSSBorderRadius

- (instancetype)initWithCSSBorderRadius:(NSString *)cssBorderRadius {
    if (self = [super init]) {
        NSArray<NSString *> *radiusArray = [cssBorderRadius componentsSeparatedByString:@","];
        if (radiusArray.count == 4) {
            self.topLeftCornerRadius = [radiusArray[0] doubleValue];
            self.topRightCornerRadius = [radiusArray[1] doubleValue];
            self.bottomLeftCornerRadius = [radiusArray[2] doubleValue];
            self.bottomRightCornerRadius = [radiusArray[3] doubleValue];
        }
    }
    return self;
}

- (BOOL)isSameBorderCornerRaidus {
    return self.topLeftCornerRadius == self.topRightCornerRadius
    && self.bottomLeftCornerRadius == self.bottomRightCornerRadius
    && self.topLeftCornerRadius == self.bottomLeftCornerRadius;
}

@end

/// CSSBorderLayer - 边框绘制图层
///
/// 原理说明：
/// 1. 继承自 CAShapeLayer，通过 strokeColor 和 path 绘制边框
/// 2. 支持 solid（实线）、dashed（虚线）、dotted（点线）三种样式
/// 3. 边框路径需要与裁剪路径保持一致：
///    - 如果有 clipPath，使用 clipPath 作为边框路径
///    - 如果没有 clipPath，使用 borderRadius 作为边框路径
///
/// 与 clipPath 的配合：
/// - CSSBorderLayer 在 layoutSublayers 时检查 hostView.css_clipPath
/// - 如果存在 clipPath，则沿着 clipPath 绘制边框（如星形边框）
/// - 这确保了裁剪形状和边框形状的一致性
@implementation CSSBorderLayer {
    CSSBorder *_border;      // 边框样式（宽度、颜色、样式）
    CGSize _lastSize;        // 上次布局的尺寸，用于避免重复计算
}

- (instancetype)initWithCSSBorder:(CSSBorder *)border {
    if (self = [super init]) {
        _border = border;
    }
    return self;
}

/**
 * 布局子图层时重新计算边框路径
 *
 * 调用时机：
 * - View 尺寸变化时
 * - clipPath 或 borderRadius 变化后手动调用 setNeedsLayout
 *
 * 路径选择策略：
 * 1. 优先使用 clipPath（如果存在）
 * 2. 没有 clipPath 时，使用 borderRadius
 * 3. 两者都没有时，绘制矩形边框
 */
- (void)layoutSublayers {
    [super layoutSublayers];
    
    // 1. 同步 frame 到父图层的 bounds
    if (!CGSizeEqualToSize(self.bounds.size, self.superlayer.bounds.size)) {
        self.frame = self.superlayer.bounds;
    }
    
    // 2. 尺寸未变化时跳过重绘（性能优化）
    if (CGSizeEqualToSize(self.bounds.size, _lastSize)) {
        return ;
    }
    _lastSize = self.bounds.size;
    
    UIBezierPath *path = nil;
    
    // 3. 优先检查 clipPath
    //    如果存在 clipPath，边框应该沿着 clipPath 绘制
    //    这样星形裁剪就能有星形边框
    NSString *clipPath = self.hostView.css_clipPath;
    if (clipPath.length > 0) {
        // 获取 density（iOS 上实际不使用，但为了 API 兼容性保留）
#if TARGET_OS_OSX
        CGFloat density = [NSScreen mainScreen].backingScaleFactor ?: 1.0;
#else
        CGFloat density = [UIScreen mainScreen].scale;
#endif
        // 解析 clipPath 字符串为 UIBezierPath
        path = [KRConvertUtil hr_parseClipPath:clipPath density:density];
    }
        
    // 4. 没有 clipPath 时，使用 borderRadius 构建圆角矩形路径
    if (!path) {
        CSSBorderRadius *borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:self.hostView.css_borderRadius];
        path = [KRConvertUtil hr_bezierPathWithRoundedRect:self.bounds
                                       topLeftCornerRadius:borderRadius.topLeftCornerRadius
                                      topRightCornerRadius:borderRadius.topRightCornerRadius
                                    bottomLeftCornerRadius:borderRadius.bottomLeftCornerRadius
                                   bottomRightCornerRadius:borderRadius.bottomRightCornerRadius];
    }
    
    // 5. 设置边框样式
    self.fillColor = [UIColor clearColor].CGColor;    // 不填充，只描边
    self.strokeColor = _border.borderColor.CGColor;   // 边框颜色
    self.lineWidth = 2 *_border.borderWidth;          // 边框宽度（*2 是因为 stroke 会画在路径两侧）
    self.masksToBounds = YES;                         // 裁剪超出部分
    
    // 6. 根据边框样式设置虚线模式
    CGFloat borderWidth = _border.borderWidth;
    if(_border.borderStyle == KRBorderStyleDashed){
        // 虚线：线段长度 = 间隔长度 = 3 * borderWidth
        self.lineDashPattern = @[@(3 * borderWidth), @(3 * borderWidth)];
    }else if(_border.borderStyle == KRBorderStyleDotted){
        // 点线：线段长度 = 间隔长度 = borderWidth
        self.lineDashPattern = @[@(borderWidth), @(borderWidth)];
    }else {
        // 实线：无虚线模式
        self.lineDashPattern = nil;
    }
    
    // 7. 设置边框路径
    #if TARGET_OS_OSX // [macOS]
    if (@available(macos 14.0, *)) {
        self.path = path.CGPath;
    } else {
        // macOS 14.0 以下退化为矩形边框
        CGMutablePathRef p = CGPathCreateMutable();
        CGPathAddRect(p, NULL, self.bounds);
        self.path = p;
        CGPathRelease(p);
    }
    #else
    self.path = path.CGPath;
    #endif
}

@end

/// CSSBorder
///
@interface CSSBorder()


@end
@implementation CSSBorder

- (instancetype)initWithCSSBorder:(NSString *)cssBorder {
    if (self = [super init]) {
        //
       NSArray<NSString *>* splits = [cssBorder componentsSeparatedByString:@" "];
        if (splits.count == 3) {
            _borderWidth = [KRConvertUtil CGFloat:@([splits[0] doubleValue])];
            _borderStyle =  [KRConvertUtil KRBorderStyle:[splits[1] lowercaseString]];
            _borderColor =  [UIView css_color:splits[2]];
        }
    }
    return self;
}

@end

/// CSSBoxShadow

@implementation CSSBoxShadow

- (instancetype)initWithCSSBoxShadow:(NSString *)boxShadow {
    if (self = [super init]) {
       NSArray<NSString *>* splits = [boxShadow componentsSeparatedByString:@" "];
        if (splits.count == 4) {
            _offsetX = [KRConvertUtil CGFloat:@([splits[0] doubleValue])];
            _offsetY = [KRConvertUtil CGFloat:@([splits[1] doubleValue])];
            _shadowRadius = [KRConvertUtil CGFloat:@([splits[2] doubleValue])];
            _shadowColor = [UIView css_color:splits[3]];
        }
    }
    return self;
}

@end
/// KRBoxShadowView
@interface KRBoxShadowView()

@property (nonatomic, weak) UIView *contentView;

@property (nonatomic, strong) UIView *backgroundView;

@end
@implementation KRBoxShadowView

- (instancetype)initWithContentView:(UIView *)contentView {
    if (self = [super init]) {
        self.frame = contentView.frame;
        _contentView = contentView;
//        _backgroundView = [UIView new];
#if TARGET_OS_OSX
        _backgroundView = [[KRUIView alloc] init];  // macOS 使用 KRUIView
//        self.shadow = nil;
        // 确保阴影不被裁剪
//        self.layer.masksToBounds = NO;
#else
        _backgroundView = [UIView new];
#endif
        _backgroundView.userInteractionEnabled = NO;
        [self addSubview:_backgroundView];
        [self addSubview:contentView];
    }
    return self;
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    _contentView.css_frame = [NSValue valueWithCGRect:self.bounds];
    _backgroundView.css_frame = [NSValue valueWithCGRect:self.bounds];
    [self p_updateShadowPathIfNeed];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self p_updateShadowPathIfNeed];
}

- (void)removeFromSuperview {
    [_contentView removeFromSuperview];
    _contentView.kr_commonWrapperView = nil;
    [super removeFromSuperview];
}

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    if ([key isEqualToString:@"borderRadius"]
        || [key isEqualToString:@"backgroundColor"]
        || [key isEqualToString:@"backgroundImage"]
        || [key isEqualToString:@"clipPath"]
        || [key isEqualToString:@"border"]) {
        if ([key isEqualToString:@"borderRadius"] || [key isEqualToString:@"backgroundColor"] || [key isEqualToString:@"backgroundImage"] || [key isEqualToString:@"clipPath"]) {
            [_backgroundView css_setPropWithKey:key value:value];
            [self p_updateShadowPathIfNeed];
        }
        return !([key isEqualToString:@"borderRadius"] || [key isEqualToString:@"border"] || [key isEqualToString:@"clipPath"]); // return NO 抛给contentView设置
    }
    
#if TARGET_OS_OSX
    // macOS: boxShadow 设置后需要更新 shadowPath
    if ([key isEqualToString:@"boxShadow"]) {
        BOOL result = [super css_setPropWithKey:key value:value];
        [self p_updateShadowPathIfNeed];
        return result;
    }
#endif
    return [super css_setPropWithKey:key value:value];
}

- (void)dealloc {
    
}

#pragma mark - private
// 更新阴影路径
- (void)p_updateShadowPathIfNeed {
    
#if TARGET_OS_OSX // [macOS]
    if (self.layer.shadowOpacity > 0) {
#else
    if (self.layer.shadowPath) {
#endif
        CGPathRef originalPath = NULL;
        // 优先使用 clipPath 的路径（星形、心形等自定义形状）
        if ([_backgroundView.layer.mask isKindOfClass:[CSSClipPathLayer class]]) {
            CSSClipPathLayer *clipPathLayer = (CSSClipPathLayer *)_backgroundView.layer.mask;
            originalPath = clipPathLayer.path;
//            self.layer.shadowPath = clipPathLayer.path;
        } else if ([_backgroundView.layer.mask isKindOfClass:[CSSShapeLayer class]]) {
            // 其次使用 borderRadius 的路径（圆角矩形）
            CSSShapeLayer *shapeLayer = (CSSShapeLayer *)_backgroundView.layer.mask;
            originalPath = shapeLayer.path;
//            self.layer.shadowPath = shapeLayer.path;
        }
        
        if (originalPath) {
#if TARGET_OS_OSX
            // macOS: NSShadow 使用原始坐标系（Y轴向上），需要翻转路径
            CGAffineTransform flipTransform = CGAffineTransformMake(1, 0, 0, -1, 0, self.bounds.size.height);
            CGPathRef flippedPath = CGPathCreateCopyByTransformingPath(originalPath, &flipTransform);
            self.layer.shadowPath = flippedPath;
            CGPathRelease(flippedPath);
#else
            self.layer.shadowPath = originalPath;
#endif
        } else {
            #if TARGET_OS_OSX // [macOS]
            CGPathRef p = CGPathCreateWithRoundedRect(self.bounds, _backgroundView.layer.cornerRadius, _backgroundView.layer.cornerRadius, NULL);
            self.layer.shadowPath = p;
            CGPathRelease(p);
#else
            self.layer.shadowPath = [[UIBezierPath bezierPathWithRoundedRect:self.bounds cornerRadius:_backgroundView.layer.cornerRadius] CGPath];
#endif // [macOS]
        }
    }
}

@end


typedef NS_OPTIONS(NSUInteger, CSSAnimationType) {
    CSSAnimationTypePlain = 0,
    CSSAnimationTypeSpring = 1,
};

/// CSSAnimation
///
@implementation CSSAnimation {
    CSSAnimationType _animationType;
    UIViewAnimationOptions _viewAnimationOption;
    NSTimeInterval _duration;
    CGFloat _damping;
    CGFloat _velocity;
    NSMutableArray *_keyFrameAniamtions;
    NSTimeInterval _delay;
    BOOL _repeatForever;
    UIViewAnimationCurve _viewAnimationCurve;
}


- (instancetype)initWithCSSAnimation:(NSString *)cssAnimation {
    if (self = [super init]) {
        // Format: "animationType timingFunc duration damping velocity delay repeatForever key [rawCurve]"
        NSArray *splits = [cssAnimation componentsSeparatedByString:@" "];
        if (splits.count >= 3) {
            _animationType = [splits[0] intValue];
            NSString *rawCurve = splits.count >= 9 ? splits[8] : [@(KRDefaultKeyboardAnimationCurve) stringValue];
            _viewAnimationOption = [KRConvertUtil hr_viewAnimationOptions:splits[1] rawCurve:rawCurve];
            _viewAnimationCurve = [KRConvertUtil hr_viewAnimationCurve:splits[1] rawCurve:rawCurve];
            _duration = [splits[2] doubleValue];
            if (_animationType == CSSAnimationTypeSpring && splits.count >= 5) { // spring动画
                _damping = [splits[3] floatValue];
                _velocity = [splits[4] floatValue];
            }
            if (splits.count >= 6) {
                _delay = [splits[5] floatValue];
            }
            if (splits.count >= 7 && [splits[6] boolValue]) {
                _repeatForever = YES;
                _viewAnimationOption |= UIViewAnimationOptionRepeat;
            }
            if (splits.count >= 8 && [splits[7] isKindOfClass:[NSString class]]) {
                _animationKey = splits[7];
            }
        }
    }
    return self;
}
// 通用属性动画接口
- (void)animationWithBlock:(void (^)(void))block completion:(void (^)(BOOL finished))completion {
    __block BOOL isKeyFrameAnimation = NO;
    [self performAnimateWithType:_animationType animations:^{
        block();
        isKeyFrameAnimation = [self performKeyFrameAnimationsWithCompletion:completion]; // 属性动画分解出来的关键帧动画
    } completion:^(BOOL finished) {
        if (completion && !isKeyFrameAnimation) {
            completion(finished);
        }
    }];
}
// 单属性动画分解的关键帧动画能力接口
- (void)addKeyframeWithRelativeStartTime:(double)frameStartTime relativeDuration:(double)frameDuration animations:(void (^)(void))animations {
    if (!_keyFrameAniamtions) {
        _keyFrameAniamtions = [[NSMutableArray alloc] init];
    }
    [_keyFrameAniamtions addObject:^(){
        [UIView addKeyframeWithRelativeStartTime:frameStartTime relativeDuration:frameDuration animations:animations];
    }];
}

- (void)performAnimateWithType:(CSSAnimationType)type animations:(void (^)(void))animations completion:(void (^)(BOOL finished))completion {
    if (type == CSSAnimationTypeSpring) {
        [UIView animateWithDuration:_duration delay:_delay usingSpringWithDamping:_damping initialSpringVelocity:_velocity
                            options:_viewAnimationOption | UIViewAnimationOptionAllowUserInteraction
                         animations:animations
                         completion:completion];
    } else if(type == CSSAnimationTypePlain) {
        [UIView animateWithDuration:_duration delay:_delay
                            options: _viewAnimationOption | UIViewAnimationOptionAllowUserInteraction
                         animations:animations
                         completion:completion];
    }
}


- (BOOL)performKeyFrameAnimationsWithCompletion:(void (^)(BOOL finished))completion  {
    if (!_keyFrameAniamtions.count) {
        return NO;
    }
    NSMutableArray *animations = [_keyFrameAniamtions copy];
    _keyFrameAniamtions = nil;
    UIViewKeyframeAnimationOptions option = UIViewKeyframeAnimationOptionCalculationModeCubicPaced;
    if (_repeatForever) {
        option |= UIViewAnimationOptionRepeat;
    }
    [UIView animateKeyframesWithDuration:_duration delay:_delay options:option | UIViewAnimationOptionAllowUserInteraction animations:^{
        UIViewAnimationCurve animationCurve = self->_viewAnimationCurve;
            [animations enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                dispatch_block_t block = obj;
                [UIView setAnimationCurve:animationCurve]; // 设置动画曲线
                block();
            }];
    } completion:^(BOOL finished) {
        if (completion) {
            completion(finished);
        }
    }];
    return YES;
}

@end

#define ROTATE_INDEX 0
#define SCALE_INDEX 1
#define TRANSLATE_INDEX 2
#define ANCHOR_INDEX 3
#define SKEW_INDEX 4
#define ROTATE_X_Y_INDEX 5

/**
 * Represents a 2D or 3D transformation for a view.
 * Encapsulates transform logic to safely apply either CGAffineTransform or CATransform3D.
 */
@interface KRTransformInfo : NSObject

// Using readonly properties to enforce immutability after initialization
@property (nonatomic, readonly, assign) CGAffineTransform affineTransform;
@property (nonatomic, readonly, assign) CATransform3D transform3D;
@property (nonatomic, readonly, assign) BOOL is3D;  // Explicit 3D flag for state clarity

// Designated initializer for 2D transforms
- (instancetype)initWithAffineTransform:(CGAffineTransform)affineTransform;

// Designated initializer for 3D transforms
- (instancetype)initWithTransform3D:(CATransform3D)transform3D;

// Applies stored transform to target view
- (void)applyTransformToView:(UIView *)view;

@end

@implementation KRTransformInfo

#pragma mark - Initializers

- (instancetype)initWithAffineTransform:(CGAffineTransform)affineTransform {
    self = [super init];
    if (self) {
        _affineTransform = affineTransform;
        _transform3D = CATransform3DIdentity;  // Neutral 3D transform
        _is3D = NO;
    }
    return self;
}

- (instancetype)initWithTransform3D:(CATransform3D)transform3D {
    self = [super init];
    if (self) {
        _affineTransform = CGAffineTransformIdentity;  // Neutral 2D transform
        _transform3D = transform3D;
        _is3D = YES;
    }
    return self;
}

#pragma mark - Transform Application

- (void)applyTransformToView:(UIView *)view {
    if (self.is3D) {
        // Clear 2D transform before applying 3D
        view.transform = CGAffineTransformIdentity;
        view.layer.transform = self.transform3D;
    } else {
        // Clear 3D transform before applying 2D
        view.layer.transform = CATransform3DIdentity;
        view.transform = self.affineTransform;
    }
}

@end


/**
 * Represents CSS-style transformations for UIView objects.
 * Parses transform strings and applies 2D/3D transformations with animation support.
 */
@implementation CSSTransform {
    // MARK: - Transformation Parameters
    CGFloat _rotateAngle;           // Rotation angle in degrees [-360, 360]
    CGFloat _rotateXAngle;          // X-axis rotation [-360, 360]
    CGFloat _rotateYAngle;          // Y-axis rotation [-360, 360]
    CGFloat _scaleX;                // X-axis scale factor [0, 1]
    CGFloat _scaleY;                // Y-axis scale factor [0, 1]
    CGFloat _translatePercentageX;  // X translation percentage [-1, 1]
    CGFloat _translatePercentageY;  // Y translation percentage [-1, 1]
    CGFloat _anchorX;               // Anchor point X [0, 1]
    CGFloat _anchorY;               // Anchor point Y [0, 1]
    CGFloat _skewX;                 // X-axis skew [-360, 360]
    CGFloat _skewY;                 // Y-axis skew [-360, 360]
}

- (instancetype)initWithCSSTransform:(NSString *)cssTransform {
    if (self = [super init]) {
        [[cssTransform componentsSeparatedByString:@"|"] enumerateObjectsUsingBlock:^(NSString * _Nonnull obj,
                                                                                      NSUInteger idx,
                                                                                      BOOL * _Nonnull stop) {
            if (idx == ROTATE_INDEX) { // rotate
                _rotateAngle = [obj floatValue];
            } else {
                NSArray *values = [obj componentsSeparatedByString:@" "];
                if (idx == SCALE_INDEX) { // scale
                    _scaleX = [values.firstObject floatValue];
                    _scaleY = [values.lastObject floatValue];
                } else if (idx == TRANSLATE_INDEX) { // tranlate
                    _translatePercentageX = [values.firstObject floatValue];
                    _translatePercentageY = [values.lastObject floatValue];
                } else if (idx == ANCHOR_INDEX) { // anchor
                    _anchorX = [values.firstObject floatValue];
                    _anchorY = [values.lastObject floatValue];
                } else if (idx == SKEW_INDEX) { // skew
                    _skewX = [values.firstObject floatValue];
                    _skewY = [values.lastObject floatValue];
                } else if (idx == ROTATE_X_Y_INDEX) {
                    _rotateXAngle = [values.firstObject floatValue];
                    _rotateYAngle = [values.lastObject floatValue];
                }
            }
        }];
    }
    return self;
}

- (void)applyToView:(UIView *)view {
    [self applyToView:view animation:nil oldTransform:nil];
}

#pragma mark - Transformation Application

/// Resets all transforms to identity state
+ (void)resetTransformWithView:(UIView *)view {
    // Reset anchor point to default if modified
    if (!CGPointEqualToPoint(view.layer.anchorPoint, CGPointMake(0.5, 0.5))) {
        [view hr_setAnchorPointAndKeepFrame:CGPointMake(0.5, 0.5)];
    }
    
    // Reset layer transform
    if (!CATransform3DEqualToTransform(view.layer.transform, CATransform3DIdentity)) {
        view.layer.transform = CATransform3DIdentity;
    }
    
    // Reset view transform
    if (!CGAffineTransformEqualToTransform(view.transform, CGAffineTransformIdentity)) {
        view.transform = CGAffineTransformIdentity;
    }
}

/// Applies transform with optional animation
- (void)applyToView:(UIView *)view
          animation:(CSSAnimation *)animation
       oldTransform:(CSSTransform *)oldTransform {
    CGPoint targetAnchor = CGPointMake(_anchorX, _anchorY);
    
    // Calculate rotation difference for animation decision
    CGFloat rotationDelta = oldTransform ? fabs(_rotateAngle - oldTransform.rotateAngle) : 0;
    
    if (animation && rotationDelta >= 180.0) {
        // Complex rotation animation (multi-step)
        [self applyComplexRotationToView:view
                               animation:animation
                            oldTransform:oldTransform
                            targetAnchor:targetAnchor];
    } else {
        // Simple direct application
        [view hr_setAnchorPointAndKeepFrame:targetAnchor];
        KRTransformInfo *transform = [self generateTransformForFrame:view.bounds
                                                   relativeTransform:oldTransform
                                                       interpolation:1.0];
        [transform applyTransformToView:view];
    }
}

/// Handles complex rotation animations with keyframes
- (void)applyComplexRotationToView:(UIView *)view
                         animation:(CSSAnimation *)animation
                      oldTransform:(CSSTransform *)oldTransform
                      targetAnchor:(CGPoint)anchor
{
    // Apply anchor point change
    [animation addKeyframeWithRelativeStartTime:0 relativeDuration:1 animations:^{
        [view hr_setAnchorPointAndKeepFrame:anchor];
    }];
    
    // Calculate animation steps
    NSUInteger steps = ceil(fabs(_rotateAngle - oldTransform.rotateAngle) / 179.0);
    CGFloat stepDuration = 1.0 / steps;
    
    for (NSUInteger i = 0; i < steps; i++) {
        CGFloat progress = (i + 1) / (CGFloat)steps;
        
        [animation addKeyframeWithRelativeStartTime:i * stepDuration
                                   relativeDuration:stepDuration
                                         animations:^{
            KRTransformInfo *transform = [self generateTransformForFrame:view.bounds
                                                       relativeTransform:oldTransform
                                                           interpolation:progress];
            [transform applyTransformToView:view];
        }];
    }
}

#pragma mark - Transform Generation

/// Generates intermediate transformation state
- (KRTransformInfo *)generateTransformForFrame:(CGRect)frame
                             relativeTransform:(CSSTransform *)relativeTransform
                                 interpolation:(CGFloat)progress {
    // Initialize base transforms
    CGAffineTransform affine = CGAffineTransformIdentity;
    CATransform3D transform3D = CATransform3DIdentity;
    
    // Calculate interpolated values
    CGFloat translateX = [self interpolateFrom:relativeTransform ? relativeTransform->_translatePercentageX : 0
                                            to:_translatePercentageX
                                      progress:progress];
    CGFloat translateY = [self interpolateFrom:relativeTransform ? relativeTransform->_translatePercentageY : 0
                                            to:_translatePercentageY
                                      progress:progress];
    
    CGFloat scaleX = [self interpolateFrom:relativeTransform ? relativeTransform->_scaleX : 1.0
                                        to:_scaleX
                                  progress:progress];
    CGFloat scaleY = [self interpolateFrom:relativeTransform ? relativeTransform->_scaleY : 1.0
                                        to:_scaleY
                                  progress:progress];
    
    CGFloat rotation = [self interpolateFrom:relativeTransform ? relativeTransform.rotateAngle : 0
                                          to:_rotateAngle
                                    progress:progress];
    
    CGFloat rotateX = [self interpolateFrom:relativeTransform ? relativeTransform->_rotateXAngle : 0
                                         to:_rotateXAngle
                                   progress:progress];
    
    CGFloat rotateY = [self interpolateFrom:relativeTransform ? relativeTransform->_rotateYAngle : 0
                                         to:_rotateYAngle
                                   progress:progress];
    
    // Apply translation and scaling
    affine = CGAffineTransformTranslate(affine, translateX * frame.size.width, translateY * frame.size.height);
    affine = CGAffineTransformScale(affine,
                                    scaleX < 0 ? MIN(-0.00001, scaleX) : MAX(scaleX, 0.00001) ,  // Prevent zero scale
                                    scaleY < 0 ? MIN(-0.00001, scaleY) : MAX(scaleY, 0.00001));
    
    // Apply skew if needed
    if (_skewX != 0 || _skewY != 0) {
        CGFloat skewXRad = _skewX * M_PI / 180.0;
        CGFloat skewYRad = _skewY * M_PI / 180.0;
        CGAffineTransform skew = CGAffineTransformMake(1, tan(skewYRad), tan(skewXRad), 1, 0, 0);
        affine = CGAffineTransformConcat(affine, skew);
    }
    
    // Apply rotation (3D or 2D)
    if (_rotateXAngle != 0 || _rotateYAngle != 0) {
        transform3D = CATransform3DMakeAffineTransform(affine);
        transform3D.m34 = -1.0 / 1000.0;  // Perspective effect
        
        // Apply 3D rotations (order: X -> Y -> Z)
        transform3D = CATransform3DRotate(transform3D, rotateX * M_PI / 180.0, 1, 0, 0);
        transform3D = CATransform3DRotate(transform3D, rotateY * M_PI / 180.0, 0, 1, 0);
        transform3D = CATransform3DRotate(transform3D, rotation * M_PI / 180.0, 0, 0, 1);
        return [[KRTransformInfo alloc] initWithTransform3D:transform3D];
    } else {
        // Apply 2D rotation
        affine = CGAffineTransformRotate(affine, rotation * M_PI / 180.0);
        return [[KRTransformInfo alloc] initWithAffineTransform:affine];
    }
}

#pragma mark - Helper Methods

/// Linear interpolation between values
- (CGFloat)interpolateFrom:(CGFloat)start to:(CGFloat)end progress:(CGFloat)progress {
    return start + (end - start) * progress;
}

/// Rotation angle accessor
- (CGFloat)rotateAngle {
    return _rotateAngle;
}

@end

// **** CSSLazyAnimationImp **** //
/**
 懒设置属性直到动画结束形成连续关键帧动画（避免KT侧异步驱动导致的动画无法连贯）
 */
@interface CSSLazyPropInfo : NSObject
@property (nonatomic, strong) NSString *key;
@property (nonatomic, strong) id value;
@end
@implementation CSSLazyPropInfo

@end
@interface CSSLazyAnimationImp()

@property (nonatomic, strong) NSMutableDictionary <NSString *, NSMutableArray<CSSLazyPropInfo *> *> *lazyPropsMap;

@end

@implementation CSSLazyAnimationImp

- (void)setPropWithKey:(NSString *)propKey value:(id)propValue withAnimationKey:(NSString *)animationKey {
    if (!propKey || !propValue || !animationKey) { // 校验数据合法性
        return ;
    }
    NSMutableArray<CSSLazyPropInfo *> *propInfos = self.lazyPropsMap[animationKey];
    if (!propInfos) {
        propInfos = [NSMutableArray new];
        self.lazyPropsMap[animationKey] = propInfos;
    }
    CSSLazyPropInfo *propInfo = [CSSLazyPropInfo new];
    propInfo.key = propKey;
    propInfo.value = propValue;
    [propInfos addObject:propInfo];
}

// 动画结束调用，执行下一个关键帧动画，以此往复，直到最后一帧动画结束
- (BOOL)performAnimationWithKey:(NSString *)animationKey withView:(UIView *)view {
    if (!animationKey || !view) { // 校验数据合法性
        return NO;
    }
    NSMutableArray<CSSLazyPropInfo *> *propInfos = self.lazyPropsMap[animationKey];
    if (propInfos.count) {
        [self.lazyPropsMap removeObjectForKey:animationKey];
        for (CSSLazyPropInfo *propInfo in propInfos) {
            [view css_setPropWithKey:propInfo.key value:propInfo.value];
        }
        return YES;
    }
    return NO;
}

- (NSMutableDictionary<NSString *, NSMutableArray<CSSLazyPropInfo *> *> *)lazyPropsMap {
    if (!_lazyPropsMap) {
        _lazyPropsMap = [NSMutableDictionary new];
    }
    return _lazyPropsMap;
}

@end




