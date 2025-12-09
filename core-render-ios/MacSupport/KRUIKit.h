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

#pragma mark - Image Helper Functions

UIKIT_STATIC_INLINE CGFloat UIImageGetScale(UIImage *image) {
    return image.scale;
}

UIKIT_STATIC_INLINE CGImageRef UIImageGetCGImageRef(UIImage *image) {
    return image.CGImage;
}

UIKIT_STATIC_INLINE UIImage *UIImageWithContentsOfFile(NSString *filePath) {
    return [UIImage imageWithContentsOfFile:filePath];
}

UIKIT_STATIC_INLINE UIImage *UIImageWithData(NSData *imageData) {
    return [UIImage imageWithData:imageData];
}

#pragma mark - UIBezierPath Helper Functions

UIKIT_STATIC_INLINE UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius) {
    return [UIBezierPath bezierPathWithRoundedRect:rect cornerRadius:cornerRadius];
}

UIKIT_STATIC_INLINE void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath) {
    [path appendPath:appendPath];
}

#pragma mark - View Type Aliases

#define RCTPlatformView UIView
#define RCTUIView UIView
#define RCTUIScrollView UIScrollView
#define RCTUIColor UIColor

#pragma mark - View Helper Functions

UIKIT_STATIC_INLINE RCTPlatformView *RCTUIViewHitTestWithEvent(RCTPlatformView *view, CGPoint point, __unused UIEvent *__nullable event) {
    return [view hitTest:point withEvent:event];
}

UIKIT_STATIC_INLINE BOOL RCTUIViewSetClipsToBounds(RCTPlatformView *view) {
    return view.clipsToBounds;
}

UIKIT_STATIC_INLINE void RCTUIViewSetContentModeRedraw(UIView *view) {
    view.contentMode = UIViewContentModeRedraw;
}

UIKIT_STATIC_INLINE BOOL RCTUIViewIsDescendantOfView(RCTPlatformView *view, RCTPlatformView *parent) {
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

#pragma mark - Font Helper Functions

UIKIT_STATIC_INLINE UIFont *UIFontWithSize(UIFont *font, CGFloat pointSize) {
    return [font fontWithSize:pointSize];
}

UIKIT_STATIC_INLINE CGFloat UIFontLineHeight(UIFont *font) {
    return [font lineHeight];
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
#define UIScrollView RCTUIScrollView
#define UIImageView RCTUIImageView
#define UIVisualEffectView NSVisualEffectView
#define UIColor RCTUIColor
#define UITouch RCTUITouch
#define UILabel RCTUILabel
#define UITextField RCTUITextField
#define UITextView RCTUITextView

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

// [macOS] Accessibility notification constants
#define UIAccessibilityScreenChangedNotification NSAccessibilityLayoutChangedNotification
#define UIAccessibilityAnnouncementNotification NSAccessibilityAnnouncementRequestedNotification

// [macOS] Accessibility notification functions
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
#define UIActivityIndicatorView RCTUIActivityIndicatorView

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
CGImageRef UIImageGetCGImageRef(NSImage *image);

#ifdef __cplusplus
}
#endif

#pragma mark - Color Alias

#define RCTUIColor NSColor

#pragma mark - Font Helper Functions

NS_INLINE NSFont *UIFontWithSize(NSFont *font, CGFloat pointSize) {
    return [NSFont fontWithDescriptor:font.fontDescriptor size:pointSize];
}

NS_INLINE CGFloat UIFontLineHeight(NSFont *font) {
    return ceilf(font.ascender + ABS(font.descender) + font.leading);
}

#pragma mark - NSFont UIKit Compatibility

// Provide UIKit-like lineHeight on NSFont so call sites can use font.lineHeight
@interface NSFont (KRUIKitCompatLineHeight)
- (CGFloat)lineHeight;
@end


#pragma mark RCTUITextField

@class UITextRange, UITextPosition;

typedef NS_OPTIONS(NSUInteger, UIControlEvents) {
    UIControlEventEditingChanged = 1UL << 0,
    UIControlEventValueChanged = 1UL << 12,
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

@interface RCTUITextField : NSTextField

@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, copy, nullable) NSString *placeholder;
@property (nonatomic, copy, nullable) NSAttributedString *attributedPlaceholder;
@property (nonatomic, assign) NSTextAlignment textAlignment;
@property (nonatomic, assign) BOOL enablesReturnKeyAutomatically;
@property (nonatomic, assign) UIKeyboardType keyboardType;
@property (nonatomic, assign) UIReturnKeyType returnKeyType;
@property (nonatomic, strong, nullable) RCTUIColor *tintColor;
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


#pragma mark - Edge Insets Type (forward declaration for RCTUITextView)

typedef NSEdgeInsets UIEdgeInsets;

NS_INLINE NSEdgeInsets UIEdgeInsetsMake(CGFloat top, CGFloat left, CGFloat bottom, CGFloat right) {
    return NSEdgeInsetsMake(top, left, bottom, right);
}

#pragma mark RCTUITextView

@interface RCTUITextView : NSTextView

@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, assign) NSTextAlignment textAlignment;
@property (nonatomic, assign) BOOL enablesReturnKeyAutomatically;
@property (nonatomic, assign) UIKeyboardType keyboardType;
@property (nonatomic, assign) UIReturnKeyType returnKeyType;
@property (nonatomic, strong, nullable) RCTUIColor *tintColor;
// Override textContainerInset to bridge NSSize -> UIEdgeInsets
@property (nonatomic, assign) UIEdgeInsets textContainerInset;

// UITextInput-like compatibility
@property (nonatomic, strong, readonly) UITextPosition *beginningOfDocument;
@property (nonatomic, strong, nullable) UITextRange *selectedTextRange;
@property (nonatomic, strong, readonly, nullable) UITextRange *markedTextRange;

- (UITextPosition *)positionFromPosition:(UITextPosition *)position offset:(NSInteger)offset;
- (UITextRange *)textRangeFromPosition:(UITextPosition *)fromPosition toPosition:(UITextPosition *)toPosition;
- (NSInteger)offsetFromPosition:(UITextPosition *)from toPosition:(UITextPosition *)to;

@end

#pragma mark - NSImage UIKit Compatibility

// [macOS] NSImage category to provide UIImage-like class constructors
@interface NSImage (KRUIImageCompat)
+ (instancetype)imageWithCGImage:(CGImageRef)cgImage;
+ (instancetype)imageWithData:(NSData *)data;
+ (instancetype)imageWithContentsOfFile:(NSString *)filePath;
// Provide UIImage.CGImage-like getter
- (CGImageRef)CGImage;
@end

typedef NS_ENUM(NSInteger, UIImageRenderingMode) {
    UIImageRenderingModeAlwaysOriginal,
    UIImageRenderingModeAlwaysTemplate,
};

// [macOS
typedef NS_ENUM(NSInteger, UIImageResizingMode) {
    UIImageResizingModeStretch = NSImageResizingModeStretch,
    UIImageResizingModeTile = NSImageResizingModeTile,
};
// macOS]

#pragma mark - Image Helper Functions

#ifdef __cplusplus
extern "C"
#endif
CGFloat UIImageGetScale(NSImage *image);

CGImageRef UIImageGetCGImageRef(NSImage *image);

// [macOS
#ifdef __cplusplus
extern "C"
#endif
NSImage *UIImageResizableImageWithCapInsets(NSImage *image, NSEdgeInsets capInsets, UIImageResizingMode resizingMode);
// macOS]

NS_INLINE UIImage *UIImageWithContentsOfFile(NSString *filePath) {
    return [[NSImage alloc] initWithContentsOfFile:filePath];
}

NS_INLINE UIImage *UIImageWithData(NSData *imageData) {
    return [[NSImage alloc] initWithData:imageData];
}

NSData *UIImagePNGRepresentation(NSImage *image);
NSData *UIImageJPEGRepresentation(NSImage *image, CGFloat compressionQuality);

#pragma mark - UIBezierPath Helper Functions

UIBezierPath *UIBezierPathWithRoundedRect(CGRect rect, CGFloat cornerRadius);
void UIBezierPathAppendPath(UIBezierPath *path, UIBezierPath *appendPath);

#pragma mark - RCTUIView

#define RCTPlatformView NSView

// [macOS] KRUIView provides macOS-specific extensions beyond NSView+KRCompat
// Note: Basic UIKit compatibility (layoutSubviews, didMoveToSuperview, etc.) is provided by NSView+KRCompat
@interface KRUIView : RCTPlatformView

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

#pragma mark - RCTPlatformView Animation Compatibility

// [macOS] UIView animation API compatibility (minimal implementation)
@interface RCTPlatformView (AnimationCompat)

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

#pragma mark - RCTUIScrollView

@class RCTUIScrollView;

// [macOS] UIScrollViewDelegate protocol for compatibility
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

@interface RCTUIScrollView : NSScrollView

#pragma mark UIScrollView Properties

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

#pragma mark Delegate

@property (nonatomic, weak, nullable) id<UIScrollViewDelegate> delegate;

#pragma mark macOS-Specific Properties

@property (nonatomic, assign) BOOL enableFocusRing;

#pragma mark Methods

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated;


#pragma mark Mouse Location Tracking

// [macOS] Mouse location tracking for touch position queries
// Returns mouse location in the given view (simulates touch location on macOS)
- (CGPoint)rct_mouseLocationInView:(nullable UIView *)view;
// macOS]

@end

#pragma mark - RCTClipView

@interface RCTClipView : NSClipView

@property (nonatomic, assign) BOOL constrainScrolling;

@end

#pragma mark - View Helper Functions

NS_INLINE RCTPlatformView *RCTUIViewHitTestWithEvent(RCTPlatformView *view, CGPoint point, __unused UIEvent *__nullable event) {
    // [macOS] IMPORTANT: point is in local coordinate space, but macOS expects superview coordinate space for hitTest
    NSView *superview = [view superview];
    NSPoint pointInSuperview = superview != nil ? [view convertPoint:point toView:superview] : point;
    return [view hitTest:pointInSuperview];
}

BOOL RCTUIViewSetClipsToBounds(RCTPlatformView *view);

NS_INLINE void RCTUIViewSetContentModeRedraw(RCTPlatformView *view) {
    view.layerContentsRedrawPolicy = NSViewLayerContentsRedrawDuringViewResize;
}

NS_INLINE BOOL RCTUIViewIsDescendantOfView(RCTPlatformView *view, RCTPlatformView *parent) {
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

// [macOS] NSValue category to provide CGPoint/CGSize/CGRect value selectors
@interface NSValue (KUCGGeometryCompat)
- (CGPoint)CGPointValue;
- (CGSize)CGSizeValue;
- (CGRect)CGRectValue;
@end

// [macOS] NSValue class methods to provide UIKit-like valueWithCGSize:/valueWithCGRect: APIs
@interface NSValue (KRUIKitCompatFactory)
+ (instancetype)valueWithCGSize:(CGSize)size;
+ (instancetype)valueWithCGRect:(CGRect)rect;
+ (instancetype)valueWithCGPoint:(CGPoint)point;
@end

NS_ASSUME_NONNULL_END

#endif // ] TARGET_OS_OSX

#pragma mark - Cross-Platform Type Aliases

#if !TARGET_OS_OSX
typedef UIApplication RCTUIApplication;
typedef UIWindow RCTPlatformWindow;
typedef UIViewController RCTPlatformViewController;
#else
typedef NSApplication RCTUIApplication;
typedef NSWindow RCTPlatformWindow;
typedef NSViewController RCTPlatformViewController;
#endif

#pragma mark - Fabric Component Types

#pragma mark RCTUISlider

#if !TARGET_OS_OSX
typedef UISlider RCTUISlider;
#else
@protocol RCTUISliderDelegate;

@interface RCTUISlider : NSSlider
NS_ASSUME_NONNULL_BEGIN
@property (nonatomic, weak) id<RCTUISliderDelegate> delegate;
@property (nonatomic, readonly) BOOL pressed;
@property (nonatomic, assign) float value;
@property (nonatomic, assign) float minimumValue;
@property (nonatomic, assign) float maximumValue;
@property (nonatomic, strong) NSColor *minimumTrackTintColor;
@property (nonatomic, strong) NSColor *maximumTrackTintColor;

- (void)setValue:(float)value animated:(BOOL)animated;
NS_ASSUME_NONNULL_END
@end
#endif

#if TARGET_OS_OSX // [macOS

@protocol RCTUISliderDelegate <NSObject>
@optional
NS_ASSUME_NONNULL_BEGIN
- (void)slider:(RCTUISlider *)slider didPress:(BOOL)press;
NS_ASSUME_NONNULL_END
@end


#pragma mark - RCTUILabel

@interface RCTUILabel : NSTextField
NS_ASSUME_NONNULL_BEGIN
@property (nonatomic, copy, nullable) NSString *text;
@property (nonatomic, copy, nullable) NSAttributedString *attributedText;
@property (nonatomic, assign) NSInteger numberOfLines;
@property (nonatomic, assign) NSTextAlignment textAlignment;

// Bridge method for iOS UILabel custom text drawing
// Subclasses (e.g. KRLabel) should override this method to perform custom text drawing
- (void)drawTextInRect:(CGRect)rect;

NS_ASSUME_NONNULL_END
@end

#endif // macOS]


#pragma mark RCTUISwitch

#if TARGET_OS_OSX

// NSSwitch is only available on macOS 10.15+, use NSButton for compatibility
@interface RCTUISwitch : NSButton
NS_ASSUME_NONNULL_BEGIN
@property (nonatomic, getter=isOn) BOOL on;

// Color properties: LIMITED support on macOS
// - onTintColor/tintColor: partial support via contentTintColor (macOS 10.14+), visual effect is subtle
// - thumbTintColor: NO support, NSButton cannot customize thumb color
// Visual appearance is controlled by system theme in most cases
@property (nonatomic, strong, nullable) RCTUIColor *onTintColor;
@property (nonatomic, strong, nullable) RCTUIColor *thumbTintColor;
@property (nonatomic, strong, nullable) RCTUIColor *tintColor;

- (void)setOn:(BOOL)on animated:(BOOL)animated;
- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)events;

NS_ASSUME_NONNULL_END
@end

typedef RCTUISwitch UISwitch;

#endif

#pragma mark RCTUIActivityIndicatorView

#if !TARGET_OS_OSX
typedef UIActivityIndicatorView RCTUIActivityIndicatorView;
#else
@interface RCTUIActivityIndicatorView : NSProgressIndicator
NS_ASSUME_NONNULL_BEGIN
@property (nonatomic, assign) UIActivityIndicatorViewStyle activityIndicatorViewStyle;
@property (nonatomic, assign) BOOL hidesWhenStopped;
@property (nonatomic, strong, nullable) RCTUIColor *color;
@property (nonatomic, readonly, getter=isAnimating) BOOL animating;

- (void)startAnimating;
- (void)stopAnimating;
NS_ASSUME_NONNULL_END
@end
#endif

#pragma mark RCTUITouch

#if !TARGET_OS_OSX
typedef UITouch RCTUITouch;
#else
@interface RCTUITouch : NSEvent
@end
#endif

#pragma mark RCTUIImageView

#if !TARGET_OS_OSX
typedef UIImageView RCTUIImageView;
#else
@interface RCTUIImageView : NSImageView
NS_ASSUME_NONNULL_BEGIN
@property (nonatomic, strong) RCTUIColor *tintColor;
@property (nonatomic, assign) UIViewContentMode contentMode;

- (instancetype)initWithImage:(UIImage *)image;

NS_ASSUME_NONNULL_END
@end
#endif

#pragma mark RCTUIGraphicsImageRenderer

#if TARGET_OS_OSX
NS_ASSUME_NONNULL_BEGIN

typedef NSGraphicsContext RCTUIGraphicsImageRendererContext;
typedef void (^RCTUIGraphicsImageDrawingActions)(RCTUIGraphicsImageRendererContext *rendererContext);

@interface RCTUIGraphicsImageRendererFormat : NSObject

+ (instancetype)defaultFormat;

@property (nonatomic) CGFloat scale;
@property (nonatomic) BOOL opaque;

@end

@interface RCTUIGraphicsImageRenderer : NSObject

- (instancetype)initWithSize:(CGSize)size;
- (instancetype)initWithSize:(CGSize)size format:(RCTUIGraphicsImageRendererFormat *)format;
- (NSImage *)imageWithActions:(NS_NOESCAPE RCTUIGraphicsImageDrawingActions)actions;

@end

NS_ASSUME_NONNULL_END
#endif
