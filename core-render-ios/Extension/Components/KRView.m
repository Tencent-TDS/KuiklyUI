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

#import "KRView.h"
#import "KRConvertUtil.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "KRDisplayLink.h"
#import "KRView+Compose.h"

/// 层级置顶方法
#define CSS_METHOD_BRING_TO_FRONT @"bringToFront"


#pragma mark - KRVisualEffectView

/// VisualEffect Wrapper View for KRView
@interface KRVisualEffectView : UIVisualEffectView

/// The wrapped KRView
@property (nonatomic, weak) KRView *wrappedView;

/// Init method
/// - Parameters:
///   - effect: visual effect
///   - wrappedView: wrapped view
- (instancetype)initWithEffect:(UIVisualEffect *)effect wrappedView:(KRView *)wrappedView NS_DESIGNATED_INITIALIZER;
- (instancetype)initWithEffect:(UIVisualEffect *)effect NS_UNAVAILABLE;
- (instancetype)initWithCoder:(NSCoder *)coder NS_UNAVAILABLE;

@end


#pragma mark - KRView

@interface KRView()
/**禁止屏幕刷新帧事件**/
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(screenFramePause);
/**屏幕刷新帧事件(VSYNC信号)**/
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(screenFrame);

/// For iOS's special effect, like `liquid glass`, etc.
@property (nonatomic, weak) KRVisualEffectView *effectView;
/// Whether to enable liquid glass effect
@property (nonatomic, assign) BOOL glassEffectEnable;
/// Tint color of glass effect
@property (nonatomic, strong) UIColor *glassEffectColor;
/// Style of glass effect
@property (nonatomic, strong) NSString *glassEffectStyle;
/// Whether is interactive of glass effect
@property (nonatomic, strong) NSNumber *glassEffectInteractive;
/// Spacing prop of liquid glass container
@property (nonatomic, strong) NSNumber *glassEffectContainerSpacing;

@end


#pragma mark - KRVisualEffectView IMP

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
    _wrappedView.effectView = nil;
    [super removeFromSuperview];
}

@end


#pragma mark - KRView IMP

/*
 * @brief 暴露给Kotlin侧调用的View容器组件
 */
@implementation KRView {
    /// 正在调用HitTest方法
    BOOL _hitTesting;
    /// 屏幕刷新定时器
    KRDisplayLink *_displaylink;
}

@synthesize hr_rootView;
#pragma mark - KuiklyRenderViewExportProtocol
- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_prepareForeReuse {
    KUIKLY_RESET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:CSS_METHOD_BRING_TO_FRONT]) {
        [self.superview bringSubviewToFront:self];
    }
}

#pragma mark - css property

- (void)setCss_screenFramePause:(NSNumber *)css_screenFramePause {
    if (_css_screenFramePause != css_screenFramePause) {
        _css_screenFramePause = css_screenFramePause;
        [_displaylink pause:[css_screenFramePause boolValue]];
    }
}

- (void)setCss_screenFrame:(KuiklyRenderCallback)css_screenFrame {
    if (_css_screenFrame != css_screenFrame) {
        _css_screenFrame = css_screenFrame;
        [_displaylink stop];
        _displaylink = nil;
        if (_css_screenFrame) {
            _displaylink = [[KRDisplayLink alloc] init];
            [_displaylink startWithCallback:^(CFTimeInterval timestamp) {
                if (css_screenFrame) {
                    css_screenFrame(nil);
                }
            }];
        }
    }
}

#pragma mark - override - base touch

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    // 如果走compose(superTouch)，由手势驱动，不由touch驱动事件
    if (_css_touchDown && ![self.css_superTouch boolValue]) {
        _css_touchDown([self p_generateBaseParamsWithEvent:event eventName:@"touchDown"]);
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesEnded:touches withEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchUp"]);
    }
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesMoved:touches withEvent:event];
    if (_css_touchMove && ![self.css_superTouch boolValue]) {
        _css_touchMove([self p_generateBaseParamsWithEvent:event eventName:@"touchMove"]);
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesCancelled:touches withEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchCancel"]);
    }
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    if ([self p_hasZIndexInSubviews]) {
        _hitTesting = YES;
    }

    CALayer *presentationLayer = self.layer.presentationLayer;      // 获取父view 渲染视图
    CALayer *modelLayer = self.layer.modelLayer;                    // 获取父view model视图
    BOOL hasAnimation = !CGRectEqualToRect(presentationLayer.frame, modelLayer.frame);
    if (hasAnimation) {
        // 1.有动画：检查点击是否在动画的当前位置
        if (self.superview) {
            CGPoint pointInSuperView = [self convertPoint:point toView:self.superview];     // 找到point在父视图中的位置
            // 点击位置位于此动画中，返回当前视图
            if (CGRectContainsPoint(presentationLayer.frame, pointInSuperView)) {
                _hitTesting = NO;
                return self;
            }
        }
    }
    // 2. 没有动画：执行原有的穿透逻辑
    UIView *hitView = [super hitTest:point withEvent:event];
    _hitTesting = NO;
    if (hitView == self) {
        // 对齐安卓事件机制，无手势事件监听则将手势穿透
        if (!(self.gestureRecognizers.count > 0 || _css_touchUp || _css_touchMove || _css_touchDown)) {
            return nil;
        }
    }
    return hitView;
}

- (NSArray<__kindof UIView *> *)subviews {
    NSArray<__kindof UIView *> *views = [super subviews];
    if (views.count && _hitTesting) { // 根据zIndex排序，解决zIndex手势响应问题
        views = [[views copy] sortedArrayUsingComparator:^NSComparisonResult(UIView *  _Nonnull obj1, UIView *  _Nonnull obj2) {
            if (obj1.css_zIndex.intValue < obj2.css_zIndex.intValue) {
                return NSOrderedAscending;
            } else if (obj1.css_zIndex.intValue > obj2.css_zIndex.intValue) {
                return NSOrderedDescending;
            } else {
                NSUInteger index1 = [views indexOfObject:obj1];
                NSUInteger index2 = [views indexOfObject:obj2];
                if (index1 < index2) {
                    return NSOrderedAscending;
                } else if (index1 > index2) {
                    return NSOrderedDescending;
                } else {
                    return NSOrderedSame;
                }
            }
        }];
    }
    return views;
}


#pragma mark - Liquid Glass Support

- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    
    // Liquid glass currently does not support layer mask,
    // so, only the cornerRadius attribute is synchronized here.
    if (_effectView) {
        _effectView.layer.cornerRadius = self.layer.cornerRadius;
    }
}

- (void)setCss_glassEffectEnable:(NSNumber *)css_glassEffectEnable {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        BOOL shouldEnable = [css_glassEffectEnable boolValue];
        if (self.glassEffectEnable != shouldEnable) {
            self.glassEffectEnable = shouldEnable;
            if (_effectView) {
                if (!shouldEnable) {
                    UIVisualEffect *effect = [[UIVisualEffect alloc] init];
                    _effectView.effect = effect;
                } else {
                    _effectView.effect = [self generateGlassEffect];
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
}

- (void)setCss_glassEffectSpacing:(NSNumber *)spacing {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        if (spacing && ![self.glassEffectContainerSpacing isEqualToNumber:spacing]) {
            self.glassEffectContainerSpacing = spacing;
            
            if (_effectView) {
                UIVisualEffect *effect = _effectView.effect;
                if ([effect isKindOfClass:UIGlassContainerEffect.class]) {
                    UIGlassContainerEffect *effect = (UIGlassContainerEffect *)_effectView.effect;
                    effect.spacing = spacing.doubleValue;
                    _effectView.effect = effect;
                }
            } else {
                // If wrapper not created yet and spacing is set, create container wrapper now
                [self ensureGlassEffectWrapperView];
            }
        }
    }
#endif
}

- (void)setCss_glassEffectInteractive:(NSNumber *)interactive {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if ((interactive != nil && ![self.glassEffectInteractive isEqualToNumber:interactive]) ||
            (interactive == nil && self.glassEffectInteractive != nil)) {
            self.glassEffectInteractive = interactive;
            
            if (_effectView) {
                UIVisualEffect *effect = _effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *glassEffect = (UIGlassEffect *)_effectView.effect;
                    glassEffect.interactive = [interactive boolValue];
                    _effectView.effect = glassEffect;
                }
            }
        }
#endif
    }
}

- (void)setCss_glassEffectTintColor:(NSNumber *)cssColor {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIColor *color = [UIView css_color:cssColor];
        if (![self.glassEffectColor isEqual:color]) {
            self.glassEffectColor = color;
            
            if (_effectView) {
                UIVisualEffect *effect = _effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *glassEffect = (UIGlassEffect *)_effectView.effect;
                    glassEffect.tintColor = color;
                    _effectView.effect = glassEffect;
                }
            }
        }
#endif
    }
}

- (void)setCss_glassEffectStyle:(NSString *)style {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassEffectStyle curStyle = [KRConvertUtil KRGlassEffectStyle:self.glassEffectStyle];
        UIGlassEffectStyle newStyle = [KRConvertUtil KRGlassEffectStyle:style];
        if (curStyle != newStyle) {
            self.glassEffectStyle = style;
            
            if (_effectView) {
                UIVisualEffect *effect = _effectView.effect;
                if ([effect isKindOfClass:UIGlassEffect.class]) {
                    UIGlassEffect *curEffect = (UIGlassEffect *)_effectView.effect;
                    UIGlassEffect *updatedEffect = [UIGlassEffect effectWithStyle:newStyle];
                    updatedEffect.tintColor = curEffect.tintColor;
                    updatedEffect.interactive = curEffect.interactive;
                    _effectView.effect = updatedEffect;
                }
            }
        }
#endif
    }
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
- (UIGlassEffect *)generateGlassEffect API_AVAILABLE(ios(26.0)) {
    UIGlassEffectStyle style = [KRConvertUtil KRGlassEffectStyle:self.glassEffectStyle];
    UIGlassEffect *glassEffect = [UIGlassEffect effectWithStyle:style];
    glassEffect.tintColor = self.glassEffectColor;
    glassEffect.interactive = self.glassEffectInteractive.boolValue;
    return glassEffect;
}
#endif

- (void)ensureGlassEffectWrapperView {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        if (self.glassEffectEnable) {
            if (!_effectView) {
                UIGlassEffect * glassEffect = [self generateGlassEffect];
                KRVisualEffectView *effectView = [KRVisualEffectView.alloc initWithEffect:glassEffect
                                                                              wrappedView:self];
                _effectView = effectView;
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
        } else if (self.glassEffectContainerSpacing) {
            if (!_effectView) {
                UIGlassContainerEffect *glassContainerEffect = [[UIGlassContainerEffect alloc] init];
                glassContainerEffect.spacing = self.glassEffectContainerSpacing.doubleValue;
                KRVisualEffectView *effectView = [KRVisualEffectView.alloc initWithEffect:glassContainerEffect
                                                                              wrappedView:self];
                _effectView = effectView;
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
                
                self.kr_commonWrapperView = _effectView;
            }
        }
#endif
    }
}

- (void)setCss_frame:(NSValue *)css_frame {
    [super setCss_frame:css_frame];
    if (_effectView) {
        _effectView.frame = self.frame;
    }
}

#pragma mark - private

- (NSDictionary *)p_generateBaseParamsWithEvent:(UIEvent *)event eventName:(NSString *)eventName {
    NSSet<UITouch *> *touches = [event allTouches];
    NSMutableArray *touchesParam = [NSMutableArray new];
    [touches enumerateObjectsUsingBlock:^(UITouch * _Nonnull touchObj, BOOL * _Nonnull stop) {
        [touchesParam addObject:[self p_generateTouchParamWithTouch:touchObj]];
    }];
    __block NSMutableDictionary *result = [([touchesParam firstObject] ?: @{}) mutableCopy];
    result[@"touches"] = touchesParam;
    result[@"action"] = eventName;
    result[@"timestamp"] = @(event.timestamp);
    return result;
}

- (NSDictionary *)p_generateTouchParamWithTouch:(UITouch *)touch {
    CGPoint locationInSelf = [touch locationInView:self];
    CGPoint locationInRootView = [touch locationInView:self.hr_rootView];
    return @{
        @"x" : @(locationInSelf.x),
        @"y" : @(locationInSelf.y),
        @"pageX" : @(locationInRootView.x),
        @"pageY" : @(locationInRootView.y),
        @"hash"  : @(touch.hash),
        @"pointerId" : [NSNumber numberWithUnsignedLong:touch.hash],
    };
}


- (BOOL)p_hasZIndexInSubviews {
    for (UIView *subView in self.subviews) {
        if (subView.css_zIndex) {
            return YES;
        }
    }
    return NO;
}

#pragma mark - dealloc

- (void)dealloc {
    if (self.css_screenFrame) {
        self.css_screenFrame = nil;
    }
}

@end
