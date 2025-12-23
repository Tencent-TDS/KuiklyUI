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
#import "KRView+LiquidGlass.h"
#import "KRConvertUtil.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "KRDisplayLink.h"
#import "KRView+Compose.h"

/// 层级置顶方法
#define CSS_METHOD_BRING_TO_FRONT @"bringToFront"
/// 无障碍聚焦
#define CSS_METHOD_ACCESSIBILITY_FOCUS @"accessibilityFocus"
/// 无障碍朗读语音
#define CSS_METHOD_ACCESSIBILITY_ANNOUNCE @"accessibilityAnnounce"


#pragma mark - KRView Private Interface

@interface KRView()
/**禁止屏幕刷新帧事件**/
@property (nonatomic, strong) NSNumber *KUIKLY_PROP(screenFramePause);
/**屏幕刷新帧事件(VSYNC信号)**/
@property (nonatomic, strong) KuiklyRenderCallback KUIKLY_PROP(screenFrame);

@end


#pragma mark - KRView Implementation

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
    } else if ([method isEqualToString:CSS_METHOD_ACCESSIBILITY_FOCUS]) {
        // 设置无障碍焦点到当前视图
        UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, self);
    } else if ([method isEqualToString:CSS_METHOD_ACCESSIBILITY_ANNOUNCE]) {
        // 朗读指定的文本内容
        if (params && params.length > 0) {
            UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, params);
        }
    }
}

#pragma mark - CSS Property

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

#pragma mark - Override - Base Touch

#if !TARGET_OS_OSX // [macOS]

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

#else

- (void)touchesBeganWithEvent:(NSEvent *)event {
    [super touchesBeganWithEvent:event];
    // 如果走compose(superTouch)，由手势驱动，不由touch驱动事件
    if (_css_touchDown && ![self.css_superTouch boolValue]) {
        _css_touchDown([self p_generateBaseParamsWithEvent:event eventName:@"touchDown"]);
    }
}

- (void)touchesEndedWithEvent:(UIEvent *)event {
    [super touchesEndedWithEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchUp"]);
    }
}

- (void)touchesMovedWithEvent:(UIEvent *)event {
    [super touchesMovedWithEvent:event];
    if (_css_touchMove && ![self.css_superTouch boolValue]) {
        _css_touchMove([self p_generateBaseParamsWithEvent:event eventName:@"touchMove"]);
    }
}

- (void)touchesCancelledWithEvent:(UIEvent *)event {
    [super touchesCancelledWithEvent:event];
    if (_css_touchUp && ![self.css_superTouch boolValue]) {
        _css_touchUp([self p_generateBaseParamsWithEvent:event eventName:@"touchCancel"]);
    }
}

#endif // [macOS]

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

#pragma mark - CSS Property Override for Liquid Glass Support

- (void)setCss_borderRadius:(NSString *)css_borderRadius {
    [super setCss_borderRadius:css_borderRadius];
    
    // Sync corner radius to liquid glass effect view
    [self updateEffectViewCornerRadius];
}

- (void)setCss_frame:(NSValue *)css_frame {
    [super setCss_frame:css_frame];
    
    // Sync frame to liquid glass effect view
    [self updateEffectViewFrame];
}

#pragma mark - Private Methods

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
#if !TARGET_OS_OSX // [macOS]
    CGPoint locationInSelf = [touch locationInView:self];
    CGPoint locationInRootView = [touch locationInView:self.hr_rootView];
#else
    // TODO
    CGPoint locationInSelf = [touch locationInWindow];
    CGPoint locationInRootView = [touch locationInWindow];
#endif
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

#pragma mark - Dealloc

- (void)dealloc {
    if (self.css_screenFrame) {
        self.css_screenFrame = nil;
    }
}

@end


