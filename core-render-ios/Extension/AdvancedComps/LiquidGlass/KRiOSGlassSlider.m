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

#import "KRiOSGlassSlider.h"
#import <objc/runtime.h>

@interface KRiOSGlassSlider ()

/// 数值变化回调
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onValueChanged;

/// 触摸开始回调
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onTouchDown;

/// 触摸结束回调
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onTouchUp;

@end

@implementation KRiOSGlassSlider

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        // 设置默认值
        self.minimumValue = 0.0f;
        self.maximumValue = 1.0f;
        self.value = 0.0f;
        self.continuous = YES;
        
        // 添加值变化事件监听
        [self addTarget:self action:@selector(sliderValueChanged:) forControlEvents:UIControlEventValueChanged];
        [self addTarget:self action:@selector(sliderTouchDown:) forControlEvents:UIControlEventTouchDown];
        [self addTarget:self action:@selector(sliderTouchUp:) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside];
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
}

#pragma mark - CSS Properties

- (void)setCss_value:(NSNumber *)cssValue {
    self.value = [cssValue floatValue];
}

- (void)setCss_minValue:(NSNumber *)minValue {
    self.minimumValue = [minValue floatValue];
}

- (void)setCss_maxValue:(NSNumber *)maxValue {
    self.maximumValue = [maxValue floatValue];
}

- (void)setCss_thumbColor:(NSNumber *)color {
    self.thumbTintColor = [UIView css_color:color];
}

- (void)setCss_trackColor:(NSNumber *)color {
    self.maximumTrackTintColor = [UIView css_color:color];
}

- (void)setCss_progressColor:(NSNumber *)color {
    self.minimumTrackTintColor = [UIView css_color:color];
}

- (void)setCss_continuous:(NSNumber *)continuous {
    self.continuous = [continuous boolValue];
}

- (void)setCss_trackThickness:(NSNumber *)thickness {
    // iOS UISlider doesn't directly support track thickness
    // This would require custom drawing or subclassing
    // For now, we'll store the value for potential future use
    objc_setAssociatedObject(self, @"trackThickness", thickness, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)setCss_thumbSize:(NSDictionary *)sizeDict {
    // iOS UISlider doesn't directly support thumb size customization
    // This would require custom thumb image or subclassing
    // For now, we'll store the value for potential future use
    objc_setAssociatedObject(self, @"thumbSize", sizeDict, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)setCss_directionHorizontal:(NSNumber *)horizontal {
    // iOS UISlider is horizontal by default
    // Vertical orientation would require transform or custom implementation
    if (![horizontal boolValue]) {
        // Apply 90-degree rotation for vertical slider
        self.transform = CGAffineTransformMakeRotation(-M_PI_2);
    } else {
        self.transform = CGAffineTransformIdentity;
    }
}

#pragma mark - Event Handlers

- (void)sliderValueChanged:(UISlider *)slider {
    // 发送值变化事件
    NSDictionary *params = @{
        @"value": @(slider.value)
    };
    if (self.css_onValueChanged) {
        self.css_onValueChanged(params);
    }
}

- (void)sliderTouchDown:(UISlider *)slider {
    // 发送开始拖拽事件
    CGPoint relativePoint = slider.center;
    CGPoint absolutePoint = [slider.superview convertPoint:slider.center toView:nil];
    
    NSDictionary *params = @{
        @"value": @(slider.value),
        @"x": @(relativePoint.x),
        @"y": @(relativePoint.y),
        @"pageX": @(absolutePoint.x),
        @"pageY": @(absolutePoint.y),
    };
    if (self.css_onTouchDown) {
        self.css_onTouchDown(params);
    }
}

- (void)sliderTouchUp:(UISlider *)slider {
    // 发送结束拖拽事件
    CGPoint relativePoint = slider.center;
    CGPoint absolutePoint = [slider.superview convertPoint:slider.center toView:nil];
    
    NSDictionary *params = @{
        @"value": @(slider.value),
        @"x": @(relativePoint.x),
        @"y": @(relativePoint.y),
        @"pageX": @(absolutePoint.x),
        @"pageY": @(absolutePoint.y),
    };
    if (self.css_onTouchUp) {
        self.css_onTouchUp(params);
    }
}

@end
