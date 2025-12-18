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

// [macOS]

#include <TargetConditionals.h>

#pragma mark - iOS Platform

#if !TARGET_OS_OSX

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

#pragma mark - UIBezierPath Helper Functions

UIKIT_STATIC_INLINE UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius) {
    return [UIBezierPath bezierPathWithRoundedRect:rect cornerRadius:cornerRadius];
}

UIKIT_STATIC_INLINE void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath) {
    [path appendPath:appendPath];
}

#pragma mark - View Type Aliases

#define KRPlatformView UIView
#define KRUIView UIView

#pragma mark - View Helper Functions

UIKIT_STATIC_INLINE KRPlatformView *KRUIViewHitTestWithEvent(KRPlatformView *view, CGPoint point, __unused UIEvent *__nullable event) {
    return [view hitTest:point withEvent:event];
}

UIKIT_STATIC_INLINE BOOL KRUIViewSetClipsToBounds(KRPlatformView *view) {
    return view.clipsToBounds;
}

UIKIT_STATIC_INLINE void KRUIViewSetContentModeRedraw(UIView *view) {
    view.contentMode = UIViewContentModeRedraw;
}

UIKIT_STATIC_INLINE BOOL KRUIViewIsDescendantOfView(KRPlatformView *view, KRPlatformView *parent) {
    return [view isDescendantOfView:parent];
}

#pragma mark - NSValue Helper Functions

UIKIT_STATIC_INLINE NSValue *NSValueWithCGRect(CGRect rect) {
    return [NSValue valueWithCGRect:rect];
}

UIKIT_STATIC_INLINE NSValue *NSValueWithCGSize(CGSize size) {
    return [NSValue valueWithCGSize:size];
}

UIKIT_STATIC_INLINE CGRect CGRectValue(NSValue *value) {
    return [value CGRectValue];
}

NS_ASSUME_NONNULL_END

#else // TARGET_OS_OSX [


#pragma mark - macOS Platform

#import <AppKit/AppKit.h>
#import "NSView+KRCompat.h"

NS_ASSUME_NONNULL_BEGIN

#pragma mark - UIKit Placeholder Types

// [macOS] UIKit enum/type shims for headers referenced by cross-platform code
// Keep types lightweight to satisfy compile-time; behavior implemented elsewhere or guarded

typedef NSInteger UIUserInterfaceStyle;
enum : NSInteger {
    UIUserInterfaceStyleUnspecified = 0,
    UIUserInterfaceStyleLight = 1,
    UIUserInterfaceStyleDark = 2,
};

typedef NSUInteger UIViewAnimationOptions;
typedef NSUInteger UIViewKeyframeAnimationOptions;

// Animation options
enum : NSUInteger {
    UIViewAnimationOptionAllowUserInteraction = 1 << 0,
    UIViewAnimationOptionRepeat = 1 << 1,
    UIViewKeyframeAnimationOptionCalculationModeCubicPaced = 0,
    UIViewAnimationOptionCurveEaseInOut = 0 << 16,
    UIViewAnimationOptionCurveEaseIn = 1 << 16,
    UIViewAnimationOptionCurveEaseOut = 2 << 16,
    UIViewAnimationOptionCurveLinear = 3 << 16,
};

typedef NS_ENUM(NSInteger, UIViewAnimationCurve) {
    UIViewAnimationCurveEaseInOut = 0,
    UIViewAnimationCurveEaseIn = 1,
    UIViewAnimationCurveEaseOut = 2,
    UIViewAnimationCurveLinear = 3,
};

typedef NS_ENUM(NSInteger, UIKeyboardType) {
    UIKeyboardTypeDefault = 0
};

typedef NS_ENUM(NSInteger, UIReturnKeyType) {
    UIReturnKeyDefault = 0
};

typedef unsigned long long UIAccessibilityTraits;

// [macOS] Accessibility trait constants (iOS uses bitmask, macOS uses roles)
static const UIAccessibilityTraits UIAccessibilityTraitNone = 0;
static const UIAccessibilityTraits UIAccessibilityTraitButton = (1 << 0);
static const UIAccessibilityTraits UIAccessibilityTraitLink = (1 << 1);
static const UIAccessibilityTraits UIAccessibilityTraitSearchField = (1 << 2);
static const UIAccessibilityTraits UIAccessibilityTraitImage = (1 << 3);
static const UIAccessibilityTraits UIAccessibilityTraitSelected = (1 << 4);
static const UIAccessibilityTraits UIAccessibilityTraitPlaysSound = (1 << 5);
static const UIAccessibilityTraits UIAccessibilityTraitKeyboardKey = (1 << 6);
static const UIAccessibilityTraits UIAccessibilityTraitStaticText = (1 << 7);
static const UIAccessibilityTraits UIAccessibilityTraitSummaryElement = (1 << 8);
static const UIAccessibilityTraits UIAccessibilityTraitNotEnabled = (1 << 9);
static const UIAccessibilityTraits UIAccessibilityTraitUpdatesFrequently = (1 << 10);
static const UIAccessibilityTraits UIAccessibilityTraitStartsMediaSession = (1 << 11);
static const UIAccessibilityTraits UIAccessibilityTraitAdjustable = (1 << 12);
static const UIAccessibilityTraits UIAccessibilityTraitAllowsDirectInteraction = (1 << 13);
static const UIAccessibilityTraits UIAccessibilityTraitCausesPageTurn = (1 << 14);
static const UIAccessibilityTraits UIAccessibilityTraitHeader = (1 << 15);

#pragma mark - Type Aliases

// View aliases
#define UIView NSView
#define UIScreen NSScreen
#define UIScrollView KRUIScrollView
#define UIImageView KRUIImageView
#define UIVisualEffectView NSVisualEffectView
#define UIColor KRUIColor
#define UITouch NSEvent
#define UILabel KRUILabel
#define UITextField KRUITextField
#define UITextView KRUITextView

// Application aliases
#define UIApplication NSApplication

// Image alias
#ifndef UIImage
@compatibility_alias UIImage NSImage;
#endif

// Gesture recognizer aliases
#define UIGestureRecognizer NSGestureRecognizer
#define UIGestureRecognizerDelegate NSGestureRecognizerDelegate
#define UIPanGestureRecognizer NSPanGestureRecognizer
#define UITapGestureRecognizer NSClickGestureRecognizer // [macOS] Use NSClickGestureRecognizer for tap/click events
#define UILongPressGestureRecognizer NSPressGestureRecognizer

// Event aliases
#define UIEvent NSEvent
#define UITouchType NSTouchType
#define UIEventButtonMask NSEventButtonMask
#define UIKeyModifierFlags NSEventModifierFlags

// Font aliases
@compatibility_alias UIFont NSFont;
@compatibility_alias UIFontDescriptor NSFontDescriptor;
typedef NSFontSymbolicTraits UIFontDescriptorSymbolicTraits;
typedef NSFontWeight UIFontWeight;

// View controller and responder aliases
@compatibility_alias UIViewController NSViewController;
@compatibility_alias UIResponder NSResponder; // [macOS]

// Bezier path alias
@compatibility_alias UIBezierPath NSBezierPath;

// Accessibility alias
@compatibility_alias UIAccessibilityCustomAction NSAccessibilityCustomAction;


#pragma mark - Accessibility Notifications

// Accessibility notification constants
#define UIAccessibilityScreenChangedNotification NSAccessibilityLayoutChangedNotification
#define UIAccessibilityAnnouncementNotification NSAccessibilityAnnouncementRequestedNotification

// Accessibility notification functions
NS_INLINE void UIAccessibilityPostNotification(NSAccessibilityNotificationName notification, id _Nullable argument) {
    if ([notification isEqualToString:NSAccessibilityAnnouncementRequestedNotification]) {
        // For announcements, use NSAccessibilityPostNotificationWithUserInfo with the announcement key
        if (argument && [argument isKindOfClass:[NSString class]]) {
            NSDictionary *userInfo = @{
                NSAccessibilityAnnouncementKey: argument,
                NSAccessibilityPriorityKey: @(NSAccessibilityPriorityHigh)
            };
            NSAccessibilityPostNotificationWithUserInfo(
                [NSApp mainWindow],
                NSAccessibilityAnnouncementRequestedNotification,
                userInfo
            );
        }
    } else {
        // For other notifications (like layout changed), post to the specific element
        if (argument) {
            NSAccessibilityPostNotification(argument, notification);
        }
    }
}

// Activity indicator alias
#define UIActivityIndicatorView KRUIActivityIndicatorView

#pragma mark - Notification Name Aliases

#define UIApplicationDidBecomeActiveNotification      NSApplicationDidBecomeActiveNotification
#define UIApplicationDidEnterBackgroundNotification   NSApplicationDidHideNotification
#define UIApplicationDidFinishLaunchingNotification   NSApplicationDidFinishLaunchingNotification
#define UIApplicationWillResignActiveNotification     NSApplicationWillResignActiveNotification
#define UIApplicationWillEnterForegroundNotification  NSApplicationWillUnhideNotification

#pragma mark - Keyboard Notifications (compat)

#define UIKeyboardWillShowNotification @"UIKeyboardWillShowNotification"
#define UIKeyboardWillHideNotification @"UIKeyboardWillHideNotification"
#define UIKeyboardFrameEndUserInfoKey @"UIKeyboardFrameEndUserInfoKey"
#define UIKeyboardAnimationDurationUserInfoKey @"UIKeyboardAnimationDurationUserInfoKey"

#pragma mark - Font Descriptor Attribute Aliases

#define UIFontDescriptorFamilyAttribute          NSFontFamilyAttribute
#define UIFontDescriptorNameAttribute            NSFontNameAttribute
#define UIFontDescriptorFaceAttribute            NSFontFaceAttribute
#define UIFontDescriptorSizeAttribute            NSFontSizeAttribute
#define UIFontDescriptorTraitsAttribute          NSFontTraitsAttribute
#define UIFontDescriptorFeatureSettingsAttribute NSFontFeatureSettingsAttribute
#define UIFontSymbolicTrait                      NSFontSymbolicTrait
#define UIFontWeightTrait                        NSFontWeightTrait
#define UIFontFeatureTypeIdentifierKey           NSFontFeatureTypeIdentifierKey
#define UIFontFeatureSelectorIdentifierKey       NSFontFeatureSelectorIdentifierKey

#pragma mark - Font Weight Aliases

#define UIFontWeightUltraLight   NSFontWeightUltraLight
#define UIFontWeightThin         NSFontWeightThin
#define UIFontWeightLight        NSFontWeightLight
#define UIFontWeightRegular      NSFontWeightRegular
#define UIFontWeightMedium       NSFontWeightMedium
#define UIFontWeightSemibold     NSFontWeightSemibold
#define UIFontWeightBold         NSFontWeightBold
#define UIFontWeightHeavy        NSFontWeightHeavy
#define UIFontWeightBlack        NSFontWeightBlack

#pragma mark - Font Descriptor System Design Aliases

#define UIFontDescriptorSystemDesign             NSFontDescriptorSystemDesign
#define UIFontDescriptorSystemDesignDefault      NSFontDescriptorSystemDesignDefault
#define UIFontDescriptorSystemDesignSerif        NSFontDescriptorSystemDesignSerif
#define UIFontDescriptorSystemDesignRounded      NSFontDescriptorSystemDesignRounded
#define UIFontDescriptorSystemDesignMonospaced   NSFontDescriptorSystemDesignMonospaced

#pragma mark - Geometry Constant Aliases

#define UIEdgeInsetsZero NSEdgeInsetsZero
#define UIViewNoIntrinsicMetric -1

#pragma mark - Layout Direction Alias

#define UIUserInterfaceLayoutDirection NSUserInterfaceLayoutDirection

#pragma mark - Gesture Recognizer State Enum

enum {
    UIGestureRecognizerStatePossible    = NSGestureRecognizerStatePossible,
    UIGestureRecognizerStateBegan       = NSGestureRecognizerStateBegan,
    UIGestureRecognizerStateChanged     = NSGestureRecognizerStateChanged,
    UIGestureRecognizerStateEnded       = NSGestureRecognizerStateEnded,
    UIGestureRecognizerStateCancelled   = NSGestureRecognizerStateCancelled,
    UIGestureRecognizerStateFailed      = NSGestureRecognizerStateFailed,
    UIGestureRecognizerStateRecognized  = NSGestureRecognizerStateRecognized,
};

#pragma mark - Font Descriptor Trait Enum

enum {
    UIFontDescriptorTraitItalic    = NSFontItalicTrait,
    UIFontDescriptorTraitBold      = NSFontBoldTrait,
    UIFontDescriptorTraitCondensed = NSFontCondensedTrait,
};

#pragma mark - View Autoresizing Mask Enum

enum : NSUInteger {
    UIViewAutoresizingNone                 = NSViewNotSizable,
    UIViewAutoresizingFlexibleLeftMargin   = NSViewMinXMargin,
    UIViewAutoresizingFlexibleWidth        = NSViewWidthSizable,
    UIViewAutoresizingFlexibleRightMargin  = NSViewMaxXMargin,
    UIViewAutoresizingFlexibleTopMargin    = NSViewMinYMargin,
    UIViewAutoresizingFlexibleHeight       = NSViewHeightSizable,
    UIViewAutoresizingFlexibleBottomMargin = NSViewMaxYMargin,
};

#pragma mark - View Content Mode Enum

// [macOS] UIViewContentMode mapped to NSViewLayerContentsPlacement
typedef NS_ENUM(NSInteger, UIViewContentMode) {
    UIViewContentModeScaleToFill     = NSViewLayerContentsPlacementScaleAxesIndependently,
    UIViewContentModeScaleAspectFit  = NSViewLayerContentsPlacementScaleProportionallyToFit,
    UIViewContentModeScaleAspectFill = NSViewLayerContentsPlacementScaleProportionallyToFill,
    UIViewContentModeRedraw          = 1000, // Placeholder value
    UIViewContentModeCenter          = NSViewLayerContentsPlacementCenter,
    UIViewContentModeTop             = NSViewLayerContentsPlacementTop,
    UIViewContentModeBottom          = NSViewLayerContentsPlacementBottom,
    UIViewContentModeLeft            = NSViewLayerContentsPlacementLeft,
    UIViewContentModeRight           = NSViewLayerContentsPlacementRight,
    UIViewContentModeTopLeft         = NSViewLayerContentsPlacementTopLeft,
    UIViewContentModeTopRight        = NSViewLayerContentsPlacementTopRight,
    UIViewContentModeBottomLeft      = NSViewLayerContentsPlacementBottomLeft,
    UIViewContentModeBottomRight     = NSViewLayerContentsPlacementBottomRight,
};

#pragma mark - Layout Direction Enum

enum : NSInteger {
    UIUserInterfaceLayoutDirectionLeftToRight = NSUserInterfaceLayoutDirectionLeftToRight,
    UIUserInterfaceLayoutDirectionRightToLeft = NSUserInterfaceLayoutDirectionRightToLeft,
};

#pragma mark - Activity Indicator View Style Enum

typedef NS_ENUM(NSInteger, UIActivityIndicatorViewStyle) {
    UIActivityIndicatorViewStyleLarge,
    UIActivityIndicatorViewStyleMedium,
};

#pragma mark - Glass Effect Style Enum (macOS 26.0+)

// [macOS] Map UIGlassEffectStyle to NSGlassEffectViewStyle for cross-platform compatibility
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000
typedef NSGlassEffectViewStyle UIGlassEffectStyle API_AVAILABLE(macos(26.0));

enum : NSInteger {
    UIGlassEffectStyleRegular API_AVAILABLE(macos(26.0)) = NSGlassEffectViewStyleRegular,
    UIGlassEffectStyleClear API_AVAILABLE(macos(26.0)) = NSGlassEffectViewStyleClear,
};
#endif

#pragma mark - Geometry Helper Functions

NS_INLINE CGRect UIEdgeInsetsInsetRect(CGRect rect, NSEdgeInsets insets) {
    rect.origin.x    += insets.left;
    rect.origin.y    += insets.top;
    rect.size.width  -= (insets.left + insets.right);
    rect.size.height -= (insets.top  + insets.bottom);
    return rect;
}

NS_INLINE BOOL UIEdgeInsetsEqualToEdgeInsets(NSEdgeInsets insets1, NSEdgeInsets insets2) {
    return NSEdgeInsetsEqual(insets1, insets2);
}

NS_INLINE NSString *NSStringFromCGSize(CGSize size) {
    return NSStringFromSize(NSSizeFromCGSize(size));
}

NS_INLINE NSString *NSStringFromCGRect(CGRect rect) {
    return NSStringFromRect(NSRectFromCGRect(rect));
}

#pragma mark - Graphics Context Functions

#ifdef __cplusplus
extern "C" {
#endif

CGContextRef UIGraphicsGetCurrentContext(void);

#ifdef __cplusplus
}
#endif

#pragma mark - Color Alias

#define KRUIColor NSColor


#pragma mark - NSFont UIKit Compatibility

@interface NSFont (KRUIKitCompatLineHeight)

- (CGFloat)lineHeight;

@end


#pragma mark - KRUITextField

@class UITextRange, UITextPosition;

typedef NS_OPTIONS(NSUInteger, UIControlEvents) {
    UIControlEventTouchDown           = 1UL << 0,
    UIControlEventTouchUpInside       = 1UL << 6,
    UIControlEventTouchUpOutside      = 1UL << 7,
    UIControlEventValueChanged        = 1UL << 12,
    UIControlEventEditingChanged      = 1UL << 13,
};

@protocol UITextFieldDelegate <NSObject>
@optional
- (void)textFieldDidBeginEditing:(id)textField;
- (void)textFieldDidEndEditing:(id)textField;
- (BOOL)textFieldShouldReturn:(id)textField;
- (BOOL)textField:(id)textField shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string;
@end


@protocol UITextViewDelegate <NSObject>
@optional
- (void)textViewDidChange:(id)textView;
- (void)textViewDidBeginEditing:(id)textView;
- (void)textViewDidEndEditing:(id)textView;
- (BOOL)textView:(id)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text;
@end


@interface UITextPosition : NSObject
@property (nonatomic, assign, readonly) NSInteger index;
+ (instancetype)positionWithIndex:(NSInteger)index;
@end

@interface UITextRange : NSObject
@property (nonatomic, strong, readonly) UITextPosition *start;
@property (nonatomic, strong, readonly) UITextPosition *end;
+ (instancetype)rangeWithStart:(UITextPosition *)start end:(UITextPosition *)end;
@end

@interface KRUITextField : NSTextField <NSTextFieldDelegate>

@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, copy, nullable) NSString *placeholder;
@property (nonatomic, copy, nullable) NSAttributedString *attributedPlaceholder;
@property (nonatomic, assign) NSTextAlignment textAlignment;
@property (nonatomic, assign) BOOL enablesReturnKeyAutomatically;
@property (nonatomic, assign) UIKeyboardType keyboardType;
@property (nonatomic, assign) UIReturnKeyType returnKeyType;
@property (nonatomic, strong, nullable) KRUIColor *tintColor;
@property (nonatomic, assign) BOOL secureTextEntry;

// UITextInput-like compatibility
@property (nonatomic, strong, readonly) UITextPosition *beginningOfDocument;
@property (nonatomic, strong, nullable) UITextRange *selectedTextRange;
@property (nonatomic, strong, readonly, nullable) UITextRange *markedTextRange;
- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset;
- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition;
- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)to;

// UIControl-like compatibility
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events;

@end


#pragma mark - Edge Insets Type (forward declaration for KRUITextView)

typedef NSEdgeInsets UIEdgeInsets;

NS_INLINE NSEdgeInsets UIEdgeInsetsMake(CGFloat top, CGFloat left, CGFloat bottom, CGFloat right) {
    return NSEdgeInsetsMake(top, left, bottom, right);
}

#pragma mark KRUITextView

@interface KRUITextView : NSTextView <NSTextViewDelegate>

@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, assign) NSTextAlignment textAlignment;
@property (nonatomic, assign) BOOL enablesReturnKeyAutomatically;
@property (nonatomic, assign) UIKeyboardType keyboardType;
@property (nonatomic, assign) UIReturnKeyType returnKeyType;
@property (nonatomic, strong, nullable) KRUIColor *tintColor;

// UITextInput-like compatibility
@property (nonatomic, strong, readonly) UITextPosition *beginningOfDocument;
@property (nonatomic, strong, nullable) UITextRange *selectedTextRange;
@property (nonatomic, strong, readonly, nullable) UITextRange *markedTextRange;

- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset;
- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition;
- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)to;

@end

#pragma mark - NSImage UIKit Compatibility

@interface NSImage (KRUIImageCompat)

+ (instancetype)imageWithCGImage:(CGImageRef)cgImage;
+ (instancetype)imageWithData:(NSData *)data;
+ (instancetype)imageWithContentsOfFile:(NSString *)filePath;

/**
 The underlying Core Graphics image object.
 This will actually use `CGImageForProposedRect` with the image size.
 */
@property (nonatomic, readonly, nullable) CGImageRef CGImage;

/**
 The scale factor of the image. This wil actually use `bestRepresentationForRect`
 with image size and pixel size to calculate the scale factor.
 If failed, use the default value 1.0. Should be greater than or equal to 1.0.
 */
@property (nonatomic, readonly) CGFloat scale;

@end

typedef NS_ENUM(NSInteger, UIImageRenderingMode) {
    UIImageRenderingModeAlwaysOriginal,
    UIImageRenderingModeAlwaysTemplate,
};

typedef NS_ENUM(NSInteger, UIImageResizingMode) {
    UIImageResizingModeStretch = NSImageResizingModeStretch,
    UIImageResizingModeTile = NSImageResizingModeTile,
};

#pragma mark - Image Helper Functions

NSImage *UIImageResizableImageWithCapInsets(NSImage *image, NSEdgeInsets capInsets, UIImageResizingMode resizingMode);
NSData *UIImagePNGRepresentation(NSImage *image);
NSData *UIImageJPEGRepresentation(NSImage *image, CGFloat compressionQuality);

#pragma mark - UIBezierPath Helper Functions

UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius);
void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath);

#pragma mark - NSBezierPath UIKit Compatibility

// [macOS] Category to provide UIBezierPath-compatible method names on NSBezierPath
@interface NSBezierPath (KRUIKitCompat)

// UIBezierPath-compatible methods that map to NSBezierPath equivalents
- (void)addArcWithCenter:(CGPoint)center
                  radius:(CGFloat)radius
              startAngle:(CGFloat)startAngle
                endAngle:(CGFloat)endAngle
               clockwise:(BOOL)clockwise;

- (void)addLineToPoint:(CGPoint)point;

// moveToPoint: already exists on NSBezierPath with the same signature
// closePath already exists on NSBezierPath with the same signature

@end

#pragma mark - KRUIView

#define KRPlatformView NSView

// [macOS] KRUIView provides macOS-specific extensions beyond NSView+KRCompat
// Note: Basic UIKit compatibility (layoutSubviews, didMoveToSuperview, etc.) is provided by NSView+KRCompat
@interface KRUIView : KRPlatformView

#pragma mark Responder Chain

@property (nonatomic, readonly) BOOL canBecomeFirstResponder;
@property (nonatomic, readonly) BOOL isFirstResponder;
- (BOOL)becomeFirstResponder;

#pragma mark Layout

- (void)setNeedsLayout;
- (void)setNeedsDisplay;

#pragma mark Mouse Events

- (BOOL)hasMouseHoverEvent;
- (NSDictionary *)locationInfoFromDraggingLocation:(NSPoint)locationInWindow;
- (NSDictionary *)locationInfoFromEvent:(NSEvent *)event;

#pragma mark macOS-Specific Properties

/**
 * Specifies whether the view should receive the mouse down event when the
 * containing window is in the background.
 */
@property (nonatomic, assign) BOOL acceptsFirstMouse;

@property (nonatomic, assign) BOOL mouseDownCanMoveWindow;

/**
 * Specifies whether the view participates in the key view loop as user tabs through different controls.
 * This is equivalent to acceptsFirstResponder on macOS.
 */
@property (nonatomic, assign) BOOL focusable;

/**
 * Specifies whether focus ring should be drawn when the view has the first responder status.
 */
@property (nonatomic, assign) BOOL enableFocusRing;

@end

#pragma mark - KRPlatformView Animation Compatibility

// [macOS] UIView animation API compatibility (minimal implementation)
@interface KRPlatformView (AnimationCompat)

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion;

+ (void)animateWithDuration:(NSTimeInterval)duration
                      delay:(NSTimeInterval)delay
     usingSpringWithDamping:(CGFloat)damping
      initialSpringVelocity:(CGFloat)velocity
                    options:(UIViewAnimationOptions)options
                 animations:(void (^)(void))animations
                 completion:(void (^ __nullable)(BOOL finished))completion;

+ (void)animateKeyframesWithDuration:(NSTimeInterval)duration
                                delay:(NSTimeInterval)delay
                              options:(UIViewKeyframeAnimationOptions)options
                           animations:(void (^)(void))animations
                           completion:(void (^ __nullable)(BOOL finished))completion;

+ (void)addKeyframeWithRelativeStartTime:(double)frameStartTime
                        relativeDuration:(double)frameDuration
                               animations:(void (^)(void))animations;

+ (void)setAnimationCurve:(UIViewAnimationCurve)curve;

@end

#pragma mark - KRUIScrollView

@class KRUIScrollView;

@protocol UIScrollViewDelegate <NSObject>
@optional
- (void)scrollViewDidScroll:(UIScrollView *)scrollView;
- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView;
- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate;
- (void)scrollViewWillEndDragging:(UIScrollView *)scrollView
                      withVelocity:(CGPoint)velocity
               targetContentOffset:(inout CGPoint *)targetContentOffset;
- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView;
- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView;
@end

/// Mac version UIScrollView
@interface KRUIScrollView : NSScrollView

@property (nonatomic, assign) CGPoint contentOffset;
@property (nonatomic, assign) UIEdgeInsets contentInset;
@property (nonatomic, assign) CGSize contentSize;
@property (nonatomic, assign) BOOL showsHorizontalScrollIndicator;
@property (nonatomic, assign) BOOL showsVerticalScrollIndicator;
@property (nonatomic, assign) UIEdgeInsets scrollIndicatorInsets;
@property (nonatomic, assign) CGFloat minimumZoomScale;
@property (nonatomic, assign) CGFloat maximumZoomScale;
@property (nonatomic, assign) CGFloat zoomScale;
@property (nonatomic, assign) BOOL alwaysBounceHorizontal;
@property (nonatomic, assign) BOOL alwaysBounceVertical;
@property (nonatomic, assign) BOOL bounces;
@property (nonatomic, assign) BOOL pagingEnabled;
@property (nonatomic, readonly, getter=isDragging) BOOL dragging;
@property (nonatomic, readonly, getter=isDecelerating) BOOL decelerating;
@property (nonatomic, assign, getter=isScrollEnabled) BOOL scrollEnabled;


@property (nonatomic, weak, nullable) id<UIScrollViewDelegate> delegate;
@property (nonatomic, assign) BOOL enableFocusRing;

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated;

// Mouse location tracking for touch position queries
// Returns mouse location in the given view (simulates touch location on macOS)
- (CGPoint)kr_mouseLocationInView:(nullable UIView *)view;

@end


#pragma mark - KRClipView

@interface KRClipView : NSClipView

@property (nonatomic, assign) BOOL constrainScrolling;

@end


#pragma mark - View Helper Functions

NS_INLINE KRPlatformView *KRUIViewHitTestWithEvent(KRPlatformView *view, CGPoint point, __unused UIEvent *__nullable event) {
    // [macOS] IMPORTANT: point is in local coordinate space, but macOS expects superview coordinate space for hitTest
    NSView *superview = [view superview];
    NSPoint pointInSuperview = superview != nil ? [view convertPoint:point toView:superview] : point;
    return [view hitTest:pointInSuperview];
}

BOOL KRUIViewSetClipsToBounds(KRPlatformView *view);

NS_INLINE void KRUIViewSetContentModeRedraw(KRPlatformView *view) {
    view.layerContentsRedrawPolicy = NSViewLayerContentsRedrawDuringViewResize;
}

NS_INLINE BOOL KRUIViewIsDescendantOfView(KRPlatformView *view, KRPlatformView *parent) {
    return [view isDescendantOf:parent];
}

#pragma mark - NSValue Helper Functions

NS_INLINE NSValue *NSValueWithCGRect(CGRect rect) {
    return [NSValue valueWithBytes:&rect objCType:@encode(CGRect)];
}

NS_INLINE NSValue *NSValueWithCGSize(CGSize size) {
    return [NSValue valueWithBytes:&size objCType:@encode(CGSize)];
}

NS_INLINE CGRect CGRectValue(NSValue *value) {
    CGRect rect = CGRectZero;
    [value getValue:&rect];
    return rect;
}

// NSValue class methods to provide UIKit-like valueWithCGSize:/valueWithCGRect: APIs
@interface NSValue (KRUIKitCompatFactory)

+ (instancetype)valueWithCGSize:(CGSize)size;
+ (instancetype)valueWithCGRect:(CGRect)rect;
+ (instancetype)valueWithCGPoint:(CGPoint)point;

- (CGPoint)CGPointValue;
- (CGSize)CGSizeValue;
- (CGRect)CGRectValue;

@end


#endif //


#pragma mark - KRUISlider

#if TARGET_OS_OSX
#define UISlider KRUISlider
@protocol KRUISliderDelegate;

@interface KRUISlider : NSSlider

@property (nonatomic, weak) id<KRUISliderDelegate> delegate;
@property (nonatomic, readonly) BOOL pressed;
@property (nonatomic, assign) float value;
@property (nonatomic, assign) float minimumValue;
@property (nonatomic, assign) float maximumValue;
@property (nonatomic, strong) NSColor *minimumTrackTintColor;
@property (nonatomic, strong) NSColor *maximumTrackTintColor;
@property (nonatomic, strong) NSColor *thumbTintColor;

- (void)setValue:(float)value animated:(BOOL)animated;

// UIControl-like target-action support
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events;

// Override points for custom track/thumb rendering (subclass support)
- (CGRect)trackRectForBounds:(CGRect)bounds;
- (CGRect)thumbRectForBounds:(CGRect)bounds trackRect:(CGRect)rect value:(float)value;

@end
#endif

#if TARGET_OS_OSX // [macOS

@protocol KRUISliderDelegate <NSObject>
@optional
- (void)slider:(KRUISlider *)slider didPress:(BOOL)press;
@end


#pragma mark - KRUILabel

@interface KRUILabel : NSTextField

@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, assign) NSInteger numberOfLines;
@property (nonatomic, assign) NSTextAlignment textAlignment;

// Bridge method for iOS UILabel custom text drawing
// Subclasses (e.g. KRLabel) should override this method to perform custom text drawing
- (void)drawTextInRect:(CGRect)rect;

@end

#endif // macOS]


#pragma mark - KRUISegmentedControl

#if TARGET_OS_OSX

@interface KRUISegmentedControl : NSSegmentedControl

@property (nonatomic, assign) NSInteger selectedSegmentIndex;

- (instancetype)initWithItems:(NSArray *)items;

- (void)insertSegmentWithTitle:(NSString *)title atIndex:(NSUInteger)segment animated:(BOOL)animated;
- (void)removeAllSegments;
- (NSInteger)numberOfSegments;

// UIControl-like target-action support
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events;

@end

#define UISegmentedControl KRUISegmentedControl

#endif


#pragma mark KRUISwitch

#if TARGET_OS_OSX

// NSSwitch is only available on macOS 10.15+, use NSButton for compatibility
@interface KRUISwitch : NSButton

@property (nonatomic, getter=isOn) BOOL on;

// Color properties: LIMITED support on macOS
// - onTintColor/tintColor: partial support via contentTintColor (macOS 10.14+), visual effect is subtle
// - thumbTintColor: NO support, NSButton cannot customize thumb color
// Visual appearance is controlled by system theme in most cases
@property (nonatomic, strong, nullable) KRUIColor *onTintColor;
@property (nonatomic, strong, nullable) KRUIColor *thumbTintColor;
@property (nonatomic, strong, nullable) KRUIColor *tintColor;

- (void)setOn:(BOOL)on animated:(BOOL)animated;
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events;

@end

typedef KRUISwitch UISwitch;

#endif

#pragma mark KRUIActivityIndicatorView

#if !TARGET_OS_OSX
typedef UIActivityIndicatorView KRUIActivityIndicatorView;
#else
@interface KRUIActivityIndicatorView : NSProgressIndicator

@property (nonatomic, assign) UIActivityIndicatorViewStyle activityIndicatorViewStyle;
@property (nonatomic, assign) BOOL hidesWhenStopped;
@property (nonatomic, strong, nullable) KRUIColor *color;
@property (nonatomic, readonly, getter=isAnimating) BOOL animating;

- (void)startAnimating;
- (void)stopAnimating;

@end
#endif


#pragma mark KRUIImageView

#if !TARGET_OS_OSX
typedef UIImageView KRUIImageView;
#else
@interface KRUIImageView : NSImageView
@property (nonatomic, strong) KRUIColor *tintColor;
@property (nonatomic, assign) UIViewContentMode contentMode;

- (instancetype)initWithImage:(UIImage *)image;
@end
#endif

#pragma mark KRUIGraphicsImageRenderer

#if TARGET_OS_OSX

typedef NSGraphicsContext KRUIGraphicsImageRendererContext;
typedef void (^KRUIGraphicsImageDrawingActions)(KRUIGraphicsImageRendererContext *rendererContext);

@interface KRUIGraphicsImageRendererFormat : NSObject

+ (instancetype)defaultFormat;

@property (nonatomic) CGFloat scale;
@property (nonatomic) BOOL opaque;

@end

@interface KRUIGraphicsImageRenderer : NSObject

- (instancetype)initWithSize:(CGSize)size;
- (instancetype)initWithSize:(CGSize)size format:(KRUIGraphicsImageRendererFormat *)format;
- (NSImage *)imageWithActions:(NS_NOESCAPE KRUIGraphicsImageDrawingActions)actions;

@end

NS_ASSUME_NONNULL_END
#endif
