/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
#import "KuiklyRenderBridge.h"
#import "KuiklyRenderViewExportProtocol.h"


@interface CSSBorderRadius : NSObject

@property(nonatomic, assign) CGFloat topLeftCornerRadius;
@property(nonatomic, assign) CGFloat topRightCornerRadius;
@property(nonatomic, assign) CGFloat bottomLeftCornerRadius;
@property(nonatomic, assign) CGFloat bottomRightCornerRadius;

- (instancetype)initWithCSSBorderRadius:(NSString *)cssBorderRadius;
- (BOOL)isSameBorderCornerRaidus;

@end

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

// ***  CAShapeLayer  ** //
@interface CSSShapeLayer : CAShapeLayer

- (instancetype)initWithBorderRadius:(CSSBorderRadius *)borderRadius;

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



@interface UIView()<KuiklyRenderViewLifyCycleProtocol>

@property (nonatomic, strong) CSSAnimation *css_animationImp;
@property (nonatomic, strong) CSSTransform *css_transformImp;
@property (nonatomic, strong) CSSGradientLayer *css_gradientLayer;
@property (nonatomic, strong) CSSBorderLayer *css_borderLayer;
@property (nonatomic, strong) UITapGestureRecognizer *css_tapGR;
@property (nonatomic, strong) UITapGestureRecognizer *css_doubleTapGR;
@property (nonatomic, strong) UILongPressGestureRecognizer *css_longPressGR;
@property (nonatomic, strong) UIPanGestureRecognizer *css_panGR;
@property (nonatomic, strong, readonly) NSMutableSet<NSString *> *css_didSetProps;

@property (nonatomic, strong) KRBoxShadowView *kr_boxShadowView;

@end

@implementation UIView (CSS)

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    if ([self.kr_boxShadowView css_setPropWithKey:key value:value]) {
        return YES;
    }
    SEL selector = NSSelectorFromString( [NSString stringWithFormat:@"setCss_%@:", key] );
    if ([self respondsToSelector:selector]) {
        [self.css_didSetProps addObject:key];
        IMP imp = [self methodForSelector:selector];
        void (*func)(id, SEL, id) = (void *)imp;
        if (self.css_animationImp) {
            NSString *animationKey = self.css_animationImp.animationKey;
            __weak typeof(&*self) weakSelf = self;
            [self.css_animationImp animationWithBlock:^{
                func(self, selector, value);
            } completion:^(BOOL finished) {
                if (weakSelf.css_animationCompletion && ![key isEqualToString:@"animation"]) {
                    weakSelf.css_animationCompletion(@{@"finish" : @(finished ? 1 : 0),
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

- (NSString *)css_boxShadow {
    return objc_getAssociatedObject(self, @selector(css_boxShadow));
}

- (void)setCss_boxShadow:(NSString *)css_boxShadow {
    css_boxShadow = [UIView css_string:css_boxShadow];
    if (self.css_boxShadow != css_boxShadow) {
        objc_setAssociatedObject(self, @selector(css_boxShadow), css_boxShadow, OBJC_ASSOCIATION_RETAIN);
        CSSBoxShadow *boxShadow = [[CSSBoxShadow alloc] initWithCSSBoxShadow:css_boxShadow];
        self.layer.shadowColor = boxShadow.shadowColor.CGColor;
        self.layer.shadowRadius = boxShadow.shadowRadius;
        self.layer.shadowOffset = CGSizeMake(boxShadow.offsetX, boxShadow.offsetY);
        self.layer.shadowOpacity = css_boxShadow ? 1 : 0;
    }
}

- (NSString *)css_borderRadius {
    return objc_getAssociatedObject(self, @selector(css_borderRadius));
}


- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    css_borderRadius = [UIView css_string:css_borderRadius];
    if (self.css_borderRadius != css_borderRadius) {
        objc_setAssociatedObject(self, @selector(css_borderRadius), css_borderRadius, OBJC_ASSOCIATION_RETAIN);
        CSSBorderRadius * borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:css_borderRadius];
        if ([borderRadius isSameBorderCornerRaidus]) {
            self.layer.cornerRadius = borderRadius.topLeftCornerRadius;
            self.clipsToBounds = self.layer.cornerRadius ? YES : ([self.css_overflow boolValue] ? YES : NO);;
            self.layer.mask = nil;
        } else {
            self.layer.cornerRadius = 0;
            self.layer.mask = [[CSSShapeLayer alloc] initWithBorderRadius:borderRadius];
            self.clipsToBounds = YES;
            if (!CGSizeEqualToSize(self.bounds.size, CGSizeZero)) {
                [self.layer.mask setFrame:self.bounds];
            }
        }
    }
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
        if (@available(iOS 13.0, *)) {
            if ([css_autoDarkEnable boolValue]) {
                self.overrideUserInterfaceStyle = UIUserInterfaceStyleUnspecified;
            } else {
                self.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
            }
        }
    }
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
    [CSSTransform resetTransformWithView:self];
    self.frame = frame;
    [self.layer.mask setFrame:self.bounds];
    [self.css_transformImp applyToView:self]; // 尺寸发生变化，需要同步2D形变
    [self p_limitMaxBorderRadisuIfNeed];
   
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
            if (self.css_doubleTapGR) {
                [self.css_tapGR requireGestureRecognizerToFail:self.css_doubleTapGR];
            }
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
            self.css_doubleTapGR = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(css_onDoubleClickWithSender:)];
            self.css_doubleTapGR.numberOfTapsRequired = 2;
            [self addGestureRecognizer:self.css_doubleTapGR];
            if (self.css_tapGR) {
                [self.css_tapGR requireGestureRecognizerToFail:self.css_doubleTapGR];
            }
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

- (void)css_onClickTapWithSender:(UIGestureRecognizer *)sender {
    CGPoint location = [sender locationInView:self];
    CGPoint pageLocation = [sender locationInView:self.window];
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
    CGPoint pageLocation = [sender locationInView:self.window];
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
    NSDictionary *param = @{
        @"state": config[@(sender.state)] ? : @"end",
        @"x": @(location.x),
        @"y": @(location.y)
    };
    if (self.css_longPress) {
        self.css_longPress(param);
    }
}

- (void)css_onPanWithSender:(UIPanGestureRecognizer *)sender {
    NSDictionary *config = @{
        @(UIGestureRecognizerStateBegan): @"start",
        @(UIGestureRecognizerStateChanged): @"move",
    };
    
    CGPoint location = [sender locationInView:self];
    CGPoint pageLocation = [sender locationInView:self.window];
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

- (KRBoxShadowView *)kr_boxShadowView {
    return  objc_getAssociatedObject(self, @selector(kr_boxShadowView));
}

- (void)setKr_boxShadowView:(KRBoxShadowView *)kr_boxShadowView {
    objc_setAssociatedObject(self, @selector(kr_boxShadowView), kr_boxShadowView, OBJC_ASSOCIATION_RETAIN);
}

- (NSNumber *)css_wrapperBoxShadowView {
    return objc_getAssociatedObject(self, @selector(css_wrapperBoxShadowView));
}
// 当有圆角和阴影同时存在时，iOS上阴影因clipToBounds为YES而失效，故需要wrapperBoxShadowView解决
- (void)setCss_wrapperBoxShadowView:(NSNumber *)css_wrapperBoxShadowView {
    if (self.css_wrapperBoxShadowView != css_wrapperBoxShadowView) {
        objc_setAssociatedObject(self, @selector(css_wrapperBoxShadowView), css_wrapperBoxShadowView, OBJC_ASSOCIATION_RETAIN);
        if ([css_wrapperBoxShadowView boolValue] && !self.kr_boxShadowView) {
            self.kr_boxShadowView = [[KRBoxShadowView alloc] initWithContentView:self];
        }
    }
}

#pragma mark - KuiklyRenderViewLifyCycleProtocol

- (void)hrv_insertSubview:(UIView *)subView atIndex:(NSInteger)index {
    [self insertSubview:subView.kr_boxShadowView ?: subView atIndex:index];
}

- (void)hrv_removeFromSuperview {
    [(self.kr_boxShadowView ?: self) removeFromSuperview];
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
    self.path = [KRConvertUtil hr_bezierPathWithRoundedRect:self.bounds
                                       topLeftCornerRadius:_borderRadius.topLeftCornerRadius topRightCornerRadius:_borderRadius.topRightCornerRadius bottomLeftCornerRadius:_borderRadius.bottomLeftCornerRadius bottomRightCornerRadius:_borderRadius.bottomRightCornerRadius].CGPath;
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

/// CSSBorderLayer
@implementation CSSBorderLayer {
    CSSBorder *_border;
    CGSize _lastSize;
}

- (instancetype)initWithCSSBorder:(CSSBorder *)border {
    if (self = [super init]) {
        _border = border;
    }
    return self;
}

- (void)layoutSublayers {
    [super layoutSublayers];
    if (!CGSizeEqualToSize(self.bounds.size, self.superlayer.bounds.size)) {
        self.frame = self.superlayer.bounds;
    }
    if (CGSizeEqualToSize(self.bounds.size, _lastSize)) {
        return ;
    }
    _lastSize = self.bounds.size;
    
    CSSBorderRadius *borderRadius = [[CSSBorderRadius alloc] initWithCSSBorderRadius:self.hostView.css_borderRadius];
    
    UIBezierPath *path = [KRConvertUtil hr_bezierPathWithRoundedRect:self.bounds
                                                 topLeftCornerRadius:borderRadius.topLeftCornerRadius topRightCornerRadius:borderRadius.topRightCornerRadius bottomLeftCornerRadius:borderRadius.bottomLeftCornerRadius bottomRightCornerRadius:borderRadius.bottomRightCornerRadius];
    self.fillColor = [UIColor clearColor].CGColor;
    self.strokeColor = _border.borderColor.CGColor;
    self.lineWidth = 2 *_border.borderWidth;
    self.masksToBounds = YES;
    CGFloat borderWidth = _border.borderWidth;
    if(_border.borderStyle == KRBorderStyleDashed){
        self.lineDashPattern = @[@(3 * borderWidth), @(3 * borderWidth)];//画虚线
    }else if(_border.borderStyle == KRBorderStyleDotted){
        self.lineDashPattern = @[@(borderWidth), @(borderWidth)];//画点
    }else {
        self.lineDashPattern = nil;
    }
    self.path = path.CGPath;
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
        _backgroundView = [UIView new];
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
}

- (void)removeFromSuperview {
    [_contentView removeFromSuperview];
    _contentView.kr_boxShadowView = nil;
    [super removeFromSuperview];
}

- (BOOL)css_setPropWithKey:(NSString *)key value:(id)value {
    if ([key isEqualToString:@"borderRadius"]
        || [key isEqualToString:@"backgroundColor"]
        || [key isEqualToString:@"backgroundImage"]
        || [key isEqualToString:@"border"]) {
        if ([key isEqualToString:@"borderRadius"] || [key isEqualToString:@"backgroundColor"] || [key isEqualToString:@"backgroundImage"] ) {
            [_backgroundView css_setPropWithKey:key value:value];
        }
        return !([key isEqualToString:@"borderRadius"] || [key isEqualToString:@"border"]); // return NO 抛给contentView设置
    }
    return [super css_setPropWithKey:key value:value];
}

- (void)dealloc {
    
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
        NSArray *splits = [cssAnimation componentsSeparatedByString:@" "];
        if (splits.count >= 3) {
            _animationType = [splits[0] intValue];
            _viewAnimationOption = [KRConvertUtil hr_viewAnimationOptions:splits[1]];
            _viewAnimationCurve = [KRConvertUtil hr_viewAnimationCurve:splits[1]];
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
    [UIView animateKeyframesWithDuration:_duration delay:_delay options:option animations:^{
    UIViewAnimationCurve animationCurve = _viewAnimationCurve;
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
#define TRNASLATE_INDEX 2
#define ANCHOR_INDEX 3
#define SKEW_INDEX 4

/// CSSTransform
@implementation CSSTransform {
    CGFloat _rotateAngle;           // [-360, 360]
    CGFloat _scaleX;                // [0, 1]
    CGFloat _scaleY;                // [0, 1]
    CGFloat _translatePercentageX;  // [-1, 1]
    CGFloat _translatePercentageY;  // [-1, 1]
    CGFloat _anchorX;               // [0, 1]
    CGFloat _anchorY;               // [0, 1]
    CGFloat _skewX;                 // [-360, 360]
    CGFloat _skewY;                 // [-360, 360]
}

- (instancetype)initWithCSSTransform:(NSString *)cssTransform {
    if (self = [super init]) {
        [[cssTransform componentsSeparatedByString:@"|"] enumerateObjectsUsingBlock:^(NSString * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            if (idx == ROTATE_INDEX) { // rotate
                _rotateAngle = [obj floatValue];
            } else {
                NSArray *values = [obj componentsSeparatedByString:@" "];
                if (idx == SCALE_INDEX) { // scale
                    _scaleX = [values.firstObject floatValue];
                    _scaleY = [values.lastObject floatValue];
                } else if (idx == TRNASLATE_INDEX) { // tranlate
                    _translatePercentageX = [values.firstObject floatValue];
                    _translatePercentageY = [values.lastObject floatValue];
                } else if (idx == ANCHOR_INDEX) { // anchor
                    _anchorX = [values.firstObject floatValue];
                    _anchorY = [values.lastObject floatValue];
                } else if (idx == SKEW_INDEX) { // skew
                    _skewX = [values.firstObject floatValue];
                    _skewY = [values.lastObject floatValue];
                }
            }
        }];
    }
    return self;
}
- (void)applyToView:(UIView *)view {
    [self applyToView:view animation:nil oldTransform:nil];
}



+ (void)resetTransformWithView:(UIView *)view {
    CGPoint anchorPoint = view.layer.anchorPoint;
    if (!CGPointEqualToPoint(anchorPoint, CGPointMake(0.5, 0.5))) {
        [view hr_setAnchorPointAndKeepFrame:CGPointMake(0.5, 0.5)];
    }
    if (!CGAffineTransformEqualToTransform(view.transform, CGAffineTransformIdentity)) {
        view.transform = CGAffineTransformIdentity;
    }
}

- (void)applyToView:(UIView *)view animation:(CSSAnimation *)animation oldTransform:(CSSTransform *)oldTransform {
    CGPoint anchorPoint = CGPointMake(_anchorX, _anchorY);
    CGFloat rotateAngleDifference = fabs(_rotateAngle - [oldTransform rotateAngle]); // 该次目标旋转位置相对上次的绝对差值
    if (animation && rotateAngleDifference >= 180.0) { // 以动画方式&&旋转差值超过半圈，需用关键帧分解多步完成
        [animation addKeyframeWithRelativeStartTime:0 relativeDuration:1 animations:^{
            [view hr_setAnchorPointAndKeepFrame:anchorPoint]; // 设置瞄点并保持frame位置
        }];
        NSUInteger stepCount = ceil(rotateAngleDifference / 179.0);
        for (int i = 0; i < stepCount; i++) {
            CGAffineTransform step = [self generateTransformWithViewFrame:[view.css_frame CGRectValue] relativeCssTransform:oldTransform relativePercentage:1 / (stepCount * 1.0) * (i + 1)];
            [animation addKeyframeWithRelativeStartTime:1 / (stepCount * 1.0) * i relativeDuration:1 / (stepCount * 1.0) animations:^{
                view.transform = step;
            }];
        }
    } else {
        [view hr_setAnchorPointAndKeepFrame:anchorPoint]; // 设置瞄点并保持frame位置
        view.transform = [self generateTransformWithViewFrame:[view.css_frame CGRectValue] relativeCssTransform:nil relativePercentage:1.0];
    }
}

- (CGAffineTransform)generateTransformWithViewFrame:(CGRect)frame relativeCssTransform:(CSSTransform *)relativeTransform relativePercentage:(CGFloat)percentage {
    CGAffineTransform transform = CGAffineTransformIdentity;
    CGFloat relativeTranslatePercentageX = (relativeTransform ? relativeTransform->_translatePercentageX : 0);
    CGFloat relativeTranslatePercentageY = (relativeTransform ? relativeTransform->_translatePercentageY : 0);
    CGFloat relativeScaleX = (relativeTransform ? relativeTransform->_scaleX : 1.0);
    CGFloat relativeScaleY = (relativeTransform ? relativeTransform->_scaleY : 1.0);
    CGFloat relativeRotateAngle = (relativeTransform ? relativeTransform->_rotateAngle : 0.0);
    CGFloat translatePercentageXDifference = _translatePercentageX - relativeTranslatePercentageX;
    CGFloat translatePercentageYDifference = _translatePercentageY - relativeTranslatePercentageY;
    CGFloat scaleXDifference = _scaleX -  relativeScaleX;
    CGFloat scaleYDifference = _scaleY - relativeScaleY;
    CGFloat rotateAngleDifference = _rotateAngle - relativeRotateAngle;
    transform = CGAffineTransformTranslate(transform,
                                           (relativeTranslatePercentageX + translatePercentageXDifference * percentage) * frame.size.width,
                                           (relativeTranslatePercentageY + translatePercentageYDifference * percentage)  * frame.size.height);
    CGFloat scaleX = (relativeScaleX + scaleXDifference * percentage);
    CGFloat scaleY = (relativeScaleY + scaleYDifference * percentage);
    transform = CGAffineTransformScale(transform, scaleX == 0 ? 0.00001 : scaleX, scaleY == 0 ? 0.00001: scaleY);
    CGFloat rotateAngle = relativeRotateAngle + rotateAngleDifference * percentage;
    transform = CGAffineTransformRotate(transform, (rotateAngle / 360.0f) * (M_PI * 2));

    if (_skewX || _skewY) {
        CGFloat horizontalSkewAngleInRadians = _skewX * M_PI / 180;
        CGFloat verticalSkewAngleInRadians = _skewY * M_PI / 180;
        CGAffineTransform skewTransform = CGAffineTransformMake(1, tan(verticalSkewAngleInRadians), tan(horizontalSkewAngleInRadians), 1, 0, 0);
        transform = CGAffineTransformConcat(transform, skewTransform);
    }
    return transform;
}

- (CGFloat)rotateAngle {
    return _rotateAngle;
}

@end


