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

#import <Availability.h>
#if TARGET_OS_OSX

#import <AppKit/AppKit.h>

#define UIEvent NSEvent

// Forward declaration for accessibility compatibility
typedef unsigned long long UIAccessibilityTraits;

NS_ASSUME_NONNULL_BEGIN

@interface NSView (KuiklyCompat)

@property (nonatomic, assign) CGFloat alpha;
@property (nonatomic) CGAffineTransform transform;
@property (nonatomic, copy) NSColor *backgroundColor;
@property (nonatomic, getter=isUserInteractionEnabled) BOOL userInteractionEnabled;
@property (nonatomic, assign) CGPoint center;

#pragma mark -

- (void)bringSubviewToFront:(NSView *)view;
- (void)insertSubview:(NSView *)view atIndex:(NSInteger)index;
- (void)setNeedsLayout;
- (void)setNeedsDisplay;
- (void)layoutIfNeeded;
- (void)layoutSubviews;
- (void)didMoveToSuperview;
- (void)willMoveToSuperview:(nullable NSView *)newSuperview;
- (void)didMoveToWindow;

#pragma mark -

- (void)setIsAccessibilityElement:(BOOL)isAccessibilityElement;

// [macOS] Accessibility traits and hint compatibility
@property (nonatomic, assign) UIAccessibilityTraits accessibilityTraits;
@property (nonatomic, copy, nullable) NSString *accessibilityHint;

#pragma mark -

- (NSView *)hitTest:(CGPoint)point withEvent:(UIEvent *_Nullable)event;
- (BOOL)pointInside:(CGPoint)point withEvent:(UIEvent *_Nullable)event;

@end

NS_ASSUME_NONNULL_END

#endif
