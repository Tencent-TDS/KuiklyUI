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

NS_ASSUME_NONNULL_BEGIN

#pragma mark - Wrapper View Classes

#if !TARGET_OS_OSX // [macOS]

/// VisualEffect Wrapper View for KRView on iOS
/// Provides UIGlassEffect / UIGlassContainerEffect support (iOS 26.0+)
@interface KRVisualEffectView : UIVisualEffectView

/// The wrapped KRView
@property (nonatomic, weak) KRView *wrappedView;

/// Init method
/// - Parameters:
///   - effect: visual effect
///   - wrappedView: wrapped view
- (instancetype)initWithEffect:(UIVisualEffect *)effect wrappedView:(KRView *)wrappedView NS_DESIGNATED_INITIALIZER;
- (instancetype)initWithEffect:(nullable UIVisualEffect *)effect NS_UNAVAILABLE;
- (instancetype)initWithCoder:(NSCoder *)coder NS_UNAVAILABLE;

@end

#else

/// Glass Effect Wrapper View for KRView on macOS
/// Wraps either NSGlassEffectView or NSGlassEffectContainerView (macOS 26.0+)
API_AVAILABLE(macos(26.0))
@interface KRGlassEffectWrapperView : NSView

/// The wrapped KRView
@property (nonatomic, weak) KRView *wrappedView;

#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
/// The glass effect view (NSGlassEffectView)
@property (nonatomic, strong, nullable) NSGlassEffectView *glassEffectView;
/// The glass container view (NSGlassEffectContainerView)
@property (nonatomic, strong, nullable) NSGlassEffectContainerView *glassContainerView;
#endif

/// Whether this wrapper is for container effect
@property (nonatomic, assign) BOOL isContainer;

/// Init method for glass effect
- (instancetype)initWithGlassEffectWrappedView:(KRView *)wrappedView;
/// Init method for container effect
- (instancetype)initWithContainerEffectWrappedView:(KRView *)wrappedView;

@end

#endif // [macOS]

#pragma mark - KRView Liquid Glass Category

/**
 * @brief KRView category for Liquid Glass effect support
 * Provides iOS 26.0+ UIGlassEffect and macOS 26.0+ NSGlassEffectView support
 */
@interface KRView (LiquidGlass)

#pragma mark - Internal Properties (for category use)

#if !TARGET_OS_OSX // [macOS]
/// For iOS's special effect, like `liquid glass`, etc.
@property (nonatomic, weak, nullable) KRVisualEffectView *kr_effectView;
#else
/// For macOS's glass effect wrapper
@property (nonatomic, weak, nullable) KRGlassEffectWrapperView *kr_effectView API_AVAILABLE(macos(26.0));
#endif // [macOS]

/// Whether to enable liquid glass effect
@property (nonatomic, assign) BOOL kr_glassEffectEnable;
/// Tint color of glass effect
@property (nonatomic, strong, nullable) UIColor *kr_glassEffectColor;
/// Style of glass effect
@property (nonatomic, strong, nullable) NSString *kr_glassEffectStyle;
/// Whether is interactive of glass effect
@property (nonatomic, strong, nullable) NSNumber *kr_glassEffectInteractive;
/// Spacing prop of liquid glass container
@property (nonatomic, strong, nullable) NSNumber *kr_glassEffectContainerSpacing;

#pragma mark - Glass Effect Methods

/// Ensure glass effect wrapper view is created and configured
- (void)ensureGlassEffectWrapperView;

/// Update effect view frame to match self.frame
- (void)updateEffectViewFrame;

/// Update effect view corner radius to match self.layer.cornerRadius
- (void)updateEffectViewCornerRadius;

#if !TARGET_OS_OSX // [macOS]
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
/// Generate UIGlassEffect with current properties (iOS only)
- (UIGlassEffect *)generateGlassEffect API_AVAILABLE(ios(26.0));
#endif
#endif // [macOS]

@end

NS_ASSUME_NONNULL_END

