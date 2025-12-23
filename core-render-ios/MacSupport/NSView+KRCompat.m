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

#import "NSView+KRCompat.h"
#import <objc/runtime.h>
#import <QuartzCore/QuartzCore.h>

static NSComparisonResult KuiklyBringToFrontCompare(__kindof NSView *a, __kindof NSView *b, void *context) {
    NSView *target = (__bridge NSView *)context;
    if (a == target && b != target) return NSOrderedDescending;
    if (b == target && a != target) return NSOrderedAscending;
    return NSOrderedSame;
}

@implementation NSView (KuiklyCompat)

- (void)bringSubviewToFront:(NSView *)view {
    if (!view || view.superview != self) { return; }
    [self sortSubviewsUsingFunction:KuiklyBringToFrontCompare context:(__bridge void * _Nullable)(view)];
}

- (void)insertSubview:(NSView *)view atIndex:(NSInteger)index {
    if (!view) { return; }
    NSArray<__kindof NSView *> *subviews = self.subviews;
    if (index < 0) { index = 0; }
    if ((NSUInteger)index >= subviews.count) {
        [self addSubview:view];
    } else {
        [self addSubview:view positioned:NSWindowBelow relativeTo:subviews[index]];
    }
}

- (void)layoutIfNeeded {
    // 统一 iOS 语义：立即更新约束并布局子树
    // 标记需要布局，确保 layoutSubtreeIfNeeded 会执行
    [self setNeedsLayout:YES];

    // 先更新约束，再执行布局，尽可能对齐 UIKit 的行为
    [self updateConstraintsForSubtreeIfNeeded];
    [self layoutSubtreeIfNeeded];
}

// [macOS] iOS 兼容：无参 setNeedsLayout 与 setNeedsDisplay
- (void)setNeedsLayout {
    self.needsLayout = YES;
}

- (void)setNeedsDisplay {
    [self setNeedsDisplay:YES];
}

// Associated object key for tracking layout recursion
static char kKUIsInLayoutKey;

// Swizzled layout method - automatically calls layoutSubviews for subclasses that override it
- (void)ku_layout {
    // Set flag to prevent recursion when layoutSubviews calls [super layoutSubviews] -> layout
    objc_setAssociatedObject(self, &kKUIsInLayoutKey, @YES, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    
    // Call original implementation (swizzled, so this actually calls the original layout)
    [self ku_layout];
    
    // Call UIView-style layoutSubviews if subclass overrides it
    Method categoryMethod = class_getInstanceMethod([NSView class], @selector(layoutSubviews));
    Method instanceMethod = class_getInstanceMethod([self class], @selector(layoutSubviews));
    
    // If the instance method is different from category method, subclass overrode it
    if (instanceMethod && instanceMethod != categoryMethod && self.window != nil) {
        [self layoutSubviews];
    }
    
    // Clear flag after layout is done
    objc_setAssociatedObject(self, &kKUIsInLayoutKey, @NO, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)layoutSubviews {
    // Check if we're already in a layout cycle to prevent recursion
    NSNumber *isInLayout = objc_getAssociatedObject(self, &kKUIsInLayoutKey);
    if ([isInLayout boolValue]) {
        // Already in layout, don't call layout again
        return;
    }
    
    // Call layout to trigger NSView's native layout behavior
    // This allows [super layoutSubviews] in subclasses to work correctly
    [self layout];
}

// [macOS] Swizzled lifecycle methods - these replace the original NSView methods
- (void)ku_viewDidMoveToSuperview {
    // Call original implementation (swizzled, so this actually calls the original viewDidMoveToSuperview)
    [self ku_viewDidMoveToSuperview];
    
    // Call UIView-style method if subclass overrides it
    // Use class_getInstanceMethod to check if subclass really implemented the method
    Method categoryMethod = class_getInstanceMethod([NSView class], @selector(didMoveToSuperview));
    Method instanceMethod = class_getInstanceMethod([self class], @selector(didMoveToSuperview));
    
    // If the instance method is different from category method, subclass overrode it
    if (instanceMethod && instanceMethod != categoryMethod) {
        [self didMoveToSuperview];
    }
}

- (void)ku_viewWillMoveToSuperview:(nullable NSView *)newSuperview {
    // Call UIView-style method if subclass overrides it
    Method categoryMethod = class_getInstanceMethod([NSView class], @selector(willMoveToSuperview:));
    Method instanceMethod = class_getInstanceMethod([self class], @selector(willMoveToSuperview:));
    
    if (instanceMethod && instanceMethod != categoryMethod) {
        [self willMoveToSuperview:newSuperview];
    }
    
    // Call original implementation (swizzled, so this actually calls the original viewWillMoveToSuperview:)
    [self ku_viewWillMoveToSuperview:newSuperview];
}

- (void)ku_viewDidMoveToWindow {
    // Call original implementation (swizzled, so this actually calls the original viewDidMoveToWindow)
    [self ku_viewDidMoveToWindow];
    
    // Call UIView-style method if subclass overrides it
    Method categoryMethod = class_getInstanceMethod([NSView class], @selector(didMoveToWindow));
    Method instanceMethod = class_getInstanceMethod([self class], @selector(didMoveToWindow));
    
    if (instanceMethod && instanceMethod != categoryMethod) {
        [self didMoveToWindow];
    }
}

// Default implementations for subclasses to override (UIView-style API)
- (void)didMoveToSuperview {
    // Default implementation does nothing
    // Subclasses override this method for UIView-like behavior
    // This is called automatically by ku_viewDidMoveToSuperview after the native callback
}

- (void)willMoveToSuperview:(nullable NSView *)newSuperview {
    // Default implementation does nothing
    // Subclasses override this method for UIView-like behavior
    // This is called automatically by ku_viewWillMoveToSuperview before the native callback
}

- (void)didMoveToWindow {
    // Default implementation does nothing
    // Subclasses override this method for UIView-like behavior
    // This is called automatically by ku_viewDidMoveToWindow after the native callback
}

#pragma mark - Alpha & Background Color

- (CGFloat)alpha {
    return self.alphaValue;
}

- (void)setAlpha:(CGFloat)alpha {
    self.alphaValue = alpha;
}

#pragma mark - Center Property

// Bridge iOS UIView.center to NSView frame-based positioning
- (CGPoint)center {
    CGRect frame = self.frame;
    return CGPointMake(CGRectGetMidX(frame), CGRectGetMidY(frame));
}

- (void)setCenter:(CGPoint)center {
    CGRect frame = self.frame;
    frame.origin.x = center.x - frame.size.width * 0.5;
    frame.origin.y = center.y - frame.size.height * 0.5;
    self.frame = frame;
}

- (NSColor *)backgroundColor {
    CGColorRef cg = self.layer ? self.layer.backgroundColor : NULL;
    return cg ? [NSColor colorWithCGColor:cg] : nil;
}

- (void)setBackgroundColor:(NSColor *)backgroundColor {
    self.wantsLayer = YES; // 确保有 layer
    self.layer.backgroundColor = backgroundColor ? backgroundColor.CGColor : NULL;
    // 可选：提升性能，透明颜色不置 opaque
    if (backgroundColor) {
        CGFloat alpha = backgroundColor.alphaComponent;
        self.layer.opaque = (alpha >= 1.0);
    } else {
        self.layer.opaque = NO;
    }
}



#pragma mark - Transform

// 关联键用于存储 transform 值
static char kKUTransformKey;

- (CGAffineTransform)transform {
    NSValue *val = objc_getAssociatedObject(self, &kKUTransformKey);
    if (val) {
        CGAffineTransform t = CGAffineTransformIdentity;
        if (strcmp([val objCType], @encode(CGAffineTransform)) == 0) {
            [val getValue:&t];
        }
        return t;
    }
    if (self.layer) {
        CATransform3D t3d = self.layer.transform;
        if (CATransform3DIsAffine(t3d)) {
            return CATransform3DGetAffineTransform(t3d);
        }
        return self.layer.affineTransform;
    }
    return CGAffineTransformIdentity;
}

- (void)setTransform:(CGAffineTransform)transform {
    objc_setAssociatedObject(self, &kKUTransformKey, [NSValue valueWithBytes:&transform objCType:@encode(CGAffineTransform)], OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    self.wantsLayer = YES;
    self.layer.affineTransform = transform;
    [self setNeedsLayout:YES];
    [self layoutSubtreeIfNeeded];
    [self setNeedsDisplay:YES];
}

// 关联键用于存储用户交互开关
static char kKUUserInteractionEnabledKey;

+ (void)load {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        Class cls = [NSView class];
        
        // Swizzle hitTest:
        Method original = class_getInstanceMethod(cls, @selector(hitTest:));
        Method swizzled = class_getInstanceMethod(cls, @selector(ku_hitTest:));
        if (original && swizzled) {
            method_exchangeImplementations(original, swizzled);
        }
        
        // Swizzle lifecycle methods to bridge UIView-style API
        Method originalMoveToSuperview = class_getInstanceMethod(cls, @selector(viewDidMoveToSuperview));
        Method swizzledMoveToSuperview = class_getInstanceMethod(cls, @selector(ku_viewDidMoveToSuperview));
        if (originalMoveToSuperview && swizzledMoveToSuperview) {
            method_exchangeImplementations(originalMoveToSuperview, swizzledMoveToSuperview);
        }
        
        Method originalMoveToWindow = class_getInstanceMethod(cls, @selector(viewDidMoveToWindow));
        Method swizzledMoveToWindow = class_getInstanceMethod(cls, @selector(ku_viewDidMoveToWindow));
        if (originalMoveToWindow && swizzledMoveToWindow) {
            method_exchangeImplementations(originalMoveToWindow, swizzledMoveToWindow);
        }
        
        Method originalWillMoveToSuperview = class_getInstanceMethod(cls, @selector(viewWillMoveToSuperview:));
        Method swizzledWillMoveToSuperview = class_getInstanceMethod(cls, @selector(ku_viewWillMoveToSuperview:));
        if (originalWillMoveToSuperview && swizzledWillMoveToSuperview) {
            method_exchangeImplementations(originalWillMoveToSuperview, swizzledWillMoveToSuperview);
        }
        
        // Swizzle layout to automatically call layoutSubviews
        Method originalLayout = class_getInstanceMethod(cls, @selector(layout));
        Method swizzledLayout = class_getInstanceMethod(cls, @selector(ku_layout));
        if (originalLayout && swizzledLayout) {
            method_exchangeImplementations(originalLayout, swizzledLayout);
        }
    });
}

- (BOOL)isUserInteractionEnabled {
    NSNumber *val = objc_getAssociatedObject(self, &kKUUserInteractionEnabledKey);
    return val ? val.boolValue : YES;
}

- (void)setUserInteractionEnabled:(BOOL)enabled {
    objc_setAssociatedObject(self, &kKUUserInteractionEnabledKey, @(enabled), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    // 同步当前视图的手势识别器状态，保持与 UIKit 语义一致
    for (NSGestureRecognizer *gr in self.gestureRecognizers) {
        gr.enabled = enabled;
    }
}

- (NSView *)ku_hitTest:(NSPoint)point {
    // 当禁用交互时，直接返回 nil，阻止事件路由到自身及其子视图
    if (!self.userInteractionEnabled) {
        return nil;
    }
    
    // 桥接 iOS 风格的 hitTest:withEvent: 方法
    // 检查子类是否重写了 iOS 风格的 hitTest:withEvent: 方法
    Method categoryMethod = class_getInstanceMethod([NSView class], @selector(hitTest:withEvent:));
    Method instanceMethod = class_getInstanceMethod([self class], @selector(hitTest:withEvent:));
    
    // 如果子类重写了 hitTest:withEvent: 方法，则调用它
    if (instanceMethod && instanceMethod != categoryMethod) {
        // AppKit 的 hitTest: 接收的 point 是在父视图坐标系中
        // UIKit 的 hitTest:withEvent: 接收的 point 是在自身坐标系中
        // 需要将 point 从父视图坐标系转换到自身坐标系
        NSPoint pointInSelf = [self convertPoint:point fromView:self.superview];
        CGPoint cgPoint = NSPointToCGPoint(pointInSelf);
        return [self hitTest:cgPoint withEvent:nil];
    }
    
    return [self ku_hitTest:point];
}

- (NSView *)hitTest:(CGPoint)point withEvent:(__unused UIEvent *)event {
    NSView *superview = self.superview;
    NSPoint pointInSuperview = superview ? [self convertPoint:point toView:superview] : NSPointFromCGPoint(point);
    return [self ku_hitTest:pointInSuperview];
}

- (BOOL)pointInside:(CGPoint)point withEvent:(__unused UIEvent *)event {
    return self.userInteractionEnabled ? NSPointInRect(NSPointFromCGPoint(point), self.bounds) : NO;
}

#pragma mark - Accessibility

- (void)setIsAccessibilityElement:(BOOL)isAccessibilityElement {
    self.accessibilityElement = isAccessibilityElement;
}

// [macOS] Accessibility traits compatibility - map iOS traits to macOS roles
static const void *kAccessibilityTraitsKey = &kAccessibilityTraitsKey;

- (UIAccessibilityTraits)accessibilityTraits {
    NSNumber *traits = objc_getAssociatedObject(self, kAccessibilityTraitsKey);
    return traits ? [traits unsignedLongLongValue] : 0;
}

- (void)setAccessibilityTraits:(UIAccessibilityTraits)accessibilityTraits {
    objc_setAssociatedObject(self, kAccessibilityTraitsKey, @(accessibilityTraits), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    
    // Map iOS traits to macOS accessibility roles
    // iOS uses bitmask traits, macOS uses role strings and separate properties
    
    if (accessibilityTraits & (1 << 0)) { // UIAccessibilityTraitButton
        self.accessibilityRole = NSAccessibilityButtonRole;
    } else if (accessibilityTraits & (1 << 1)) { // UIAccessibilityTraitLink
        self.accessibilityRole = NSAccessibilityLinkRole;
    } else if (accessibilityTraits & (1 << 2)) { // UIAccessibilityTraitSearchField
        self.accessibilityRole = NSAccessibilityTextFieldRole;
    } else if (accessibilityTraits & (1 << 3)) { // UIAccessibilityTraitImage
        self.accessibilityRole = NSAccessibilityImageRole;
    } else if (accessibilityTraits & (1 << 7)) { // UIAccessibilityTraitStaticText
        self.accessibilityRole = NSAccessibilityStaticTextRole;
    } else if (accessibilityTraits & (1 << 15)) { // UIAccessibilityTraitHeader
        self.accessibilityRole = NSAccessibilityGroupRole;
        self.accessibilitySubrole = @"AXHeader";
    }
    
    // Handle selected state
    if (accessibilityTraits & (1 << 4)) { // UIAccessibilityTraitSelected
        self.accessibilitySelected = YES;
    }
    
    // Handle enabled state
    if (accessibilityTraits & (1 << 9)) { // UIAccessibilityTraitNotEnabled
        self.accessibilityEnabled = NO;
    }
}

// [macOS] Accessibility hint compatibility - map to macOS accessibilityHelp
- (NSString *)accessibilityHint {
    return self.accessibilityHelp;
}

- (void)setAccessibilityHint:(NSString *)accessibilityHint {
    self.accessibilityHelp = accessibilityHint;
}


@end

#endif
