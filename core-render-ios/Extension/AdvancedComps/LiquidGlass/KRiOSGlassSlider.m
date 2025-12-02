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


@interface KRiOSGlassSlider ()

/// Whether is a vertical slider.
@property (nonatomic, assign) BOOL krIsVertical;

/// Custom track thickness (0 means use system default)
@property (nonatomic, assign) CGFloat krTrackThickness;

/// Custom thumb size (CGSizeZero means use system default)
@property (nonatomic, assign) CGSize krThumbSize;

/// Value change callback.
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onValueChanged;

/// Touch down callback.
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onTouchDown;

/// Touch up callback.
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onTouchUp;

@end

@implementation KRiOSGlassSlider

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self addTarget:self action:@selector(sliderValueChanged:) forControlEvents:UIControlEventValueChanged];
        [self addTarget:self action:@selector(sliderTouchDown:) forControlEvents:UIControlEventTouchDown];
        [self addTarget:self action:@selector(sliderTouchUp:) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside];
        _krTrackThickness = 0.0;
        _krThumbSize = CGSizeZero;
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
    // Update track thickness
    self.krTrackThickness = MAX(0.0, thickness.doubleValue);
    [self setNeedsLayout];
    [self layoutIfNeeded];
}

- (void)setCss_thumbSize:(NSDictionary *)sizeDict {
    if (sizeDict && [sizeDict isKindOfClass:[NSDictionary class]]) {
        CGFloat width = [[sizeDict objectForKey:@"width"] ?: @0 floatValue];
        CGFloat height = [[sizeDict objectForKey:@"height"] ?: @0 floatValue];
        self.krThumbSize = CGSizeMake(width, height);
    } else {
        self.krThumbSize = CGSizeZero;
    }
    [self setNeedsLayout];
    [self layoutIfNeeded];
}

- (void)setCss_directionHorizontal:(NSNumber *)horizontal {
    // iOS UISlider is horizontal by default
    // Vertical orientation would require transform or custom implementation
    if (self.krIsVertical != ![horizontal boolValue]) {
        self.krIsVertical = ![horizontal boolValue];
        
        dispatch_async(dispatch_get_main_queue(), ^{
            if (![horizontal boolValue]) {
                // Apply 90-degree rotation for vertical slider
                self.transform = CGAffineTransformMakeRotation(-M_PI_2);
            } else {
                self.transform = CGAffineTransformIdentity;
            }
        });
    }
}

#pragma mark - Event Handlers

- (void)sliderValueChanged:(UISlider *)slider {
    // Send value change event
    NSDictionary *params = @{
        @"value": @(slider.value)
    };
    if (self.css_onValueChanged) {
        self.css_onValueChanged(params);
    }
}

- (void)sliderTouchDown:(UISlider *)slider {
    // Send touch down event
    CGPoint relativePoint = slider.center;
    CGPoint absolutePoint = [self kr_convertLocalPointToRenderRoot:relativePoint];
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
    CGPoint absolutePoint = [self kr_convertLocalPointToRenderRoot:relativePoint];
    
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


#pragma mark - Custom Layout

- (CGRect)trackRectForBounds:(CGRect)bounds {
    // Get the default track rect
    CGRect track = [super trackRectForBounds:bounds];
    CGFloat customHeight = self.krTrackThickness;
    if (customHeight > 0.0) {
            CGFloat centerY = CGRectGetMidY(track);
            track.size.height = customHeight;
            track.origin.y = centerY - customHeight * 0.5f;
    }
    return track;
}

- (CGRect)thumbRectForBounds:(CGRect)bounds trackRect:(CGRect)rect value:(float)value {
    // Get the default thumb rect
    CGRect thumbRect = [super thumbRectForBounds:bounds trackRect:rect value:value];
    
    // If custom thumb size is set, adjust the size while maintaining the center
    if (!CGSizeEqualToSize(self.krThumbSize, CGSizeZero)) {
        CGPoint center = CGPointMake(CGRectGetMidX(thumbRect), CGRectGetMidY(thumbRect));
        CGSize size = self.krThumbSize;
        thumbRect.size = size;
        thumbRect.origin = CGPointMake(center.x - size.width / 2.0, center.y - size.height / 2.0);
    }
    return thumbRect;
}

@end
