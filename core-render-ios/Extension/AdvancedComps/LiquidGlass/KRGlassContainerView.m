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

#import "KRGlassContainerView.h"

#if TARGET_OS_IOS // [macOS]

@implementation KRGlassContainerView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame: frame]) {
        if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
            self.effect = [[UIGlassContainerEffect alloc] init];
#endif
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

- (void)setCss_glassEffectSpacing:(NSNumber *)spacing {
    if (@available(iOS 26.0, *)) {
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
        UIGlassContainerEffect *effect = (UIGlassContainerEffect *)self.effect;
        if (effect.spacing != spacing.doubleValue) {
            effect.spacing = spacing.doubleValue;
            self.effect = effect;
        }
#endif
    }
}

@end

#endif

// [macOS
#if TARGET_OS_OSX
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000

@implementation KRGlassContainerView

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        // NSGlassEffectContainerView is initialized with default spacing (0)
    }
    return self;
}

- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue {
    KUIKLY_SET_CSS_COMMON_PROP
}

- (void)hrv_insertSubview:(NSView *)subView atIndex:(NSInteger)index {
    // NSGlassEffectContainerView uses contentView for embedding content
    if (@available(macOS 26.0, *)) {
        if (self.contentView) {
            NSArray<NSView *> *subviews = self.contentView.subviews;
            if (index >= (NSInteger)subviews.count) {
                [self.contentView addSubview:subView];
            } else {
                [self.contentView addSubview:subView positioned:NSWindowBelow relativeTo:subviews[index]];
            }
        } else {
            self.contentView = subView;
        }
    }
}

#pragma mark - CSS properties

- (void)setCss_glassEffectSpacing:(NSNumber *)spacing {
    if (@available(macOS 26.0, *)) {
        if (self.spacing != spacing.doubleValue) {
            self.spacing = spacing.doubleValue;
        }
    }
}

@end

#endif
#endif
// macOS]
