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

#import "KRiOSGlassSwitch.h"

@interface KRiOSGlassSwitch ()

/// 开关值变化回调
@property (nonatomic, strong, nullable) KuiklyRenderCallback css_onValueChanged;

@end

@implementation KRiOSGlassSwitch

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self addTarget:self action:@selector(switchValueChanged:) forControlEvents:UIControlEventValueChanged];
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue { 
    KUIKLY_SET_CSS_COMMON_PROP
}

- (void)setCss_enabled:(NSString *)cssValue {
    self.enabled = [UIView css_bool:cssValue];
}

- (void)setCss_isOn:(NSString *)cssValue {
    self.on = [UIView css_bool:cssValue];
}

- (void)setCss_onColor:(NSString *)cssValue {
    UIColor *color = [UIView css_color:cssValue];
    if (color) {
        self.onTintColor = color;
    }
}

- (void)setCss_unOnColor:(NSString *)cssValue {
    UIColor *color = [UIView css_color:cssValue];
    if (color) {
        // Map unOnColor to the appropriate property if available
        // For UISwitch, we use tintColor for the off state
        self.tintColor = color;
    }
}

- (void)setCss_thumbColor:(NSString *)cssValue {
    UIColor *color = [UIView css_color:cssValue];
    if (color) {
        self.thumbTintColor = color;
    }
}

#pragma mark - Event Handlers

- (void)switchValueChanged:(UISwitch *)sender {
    // 发送开关值变化事件
    NSDictionary *params = @{
        @"value": @(sender.on)
    };
    if (self.css_onValueChanged) {
        self.css_onValueChanged(params);
    }
}

@end
