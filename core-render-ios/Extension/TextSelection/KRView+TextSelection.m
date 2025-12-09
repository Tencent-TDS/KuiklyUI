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

#import "KRView+TextSelection.h"
#import "KRLabel.h"
#import "NSObject+KR.h"
#import <objc/runtime.h>

/// Method name constants
NSString *const KRTextSelectionMethodCreateSelection = @"createSelection";
NSString *const KRTextSelectionMethodGetSelection = @"getSelection";
NSString *const KRTextSelectionMethodClearSelection = @"clearSelection";
NSString *const KRTextSelectionMethodCreateSelectionAll = @"createSelectionAll";

/// Associated object keys
static const void *kTextSelectionHelperKey = &kTextSelectionHelperKey;
static const void *kSelectableKey = &kSelectableKey;
static const void *kSelectionColorKey = &kSelectionColorKey;
static const void *kSelectStartCallbackKey = &kSelectStartCallbackKey;
static const void *kSelectChangeCallbackKey = &kSelectChangeCallbackKey;
static const void *kSelectEndCallbackKey = &kSelectEndCallbackKey;
static const void *kSelectCancelCallbackKey = &kSelectCancelCallbackKey;

/// Selectable option enum (matches DivView.kt SelectableOption)
typedef NS_ENUM(NSInteger, KRSelectableOption) {
    KRSelectableOptionInherit = 0,  // Inherit from parent
    KRSelectableOptionEnable = 1,   // Enable text selection
    KRSelectableOptionDisable = 2   // Disable text selection
};

#pragma mark - Internal Delegate Handler

/// Internal class to handle KRTextSelectionHelperDelegate callbacks
@interface KRTextSelectionDelegateHandler : NSObject <KRTextSelectionHelperDelegate>
@property (nonatomic, weak) KRView *targetView;
@end

@implementation KRTextSelectionDelegateHandler

- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didStartWithFrame:(CGRect)frame {
    KuiklyRenderCallback callback = objc_getAssociatedObject(self.targetView, kSelectStartCallbackKey);
    if (callback) {
        callback([self frameToDictionary:frame]);
    }
}

- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didChangeWithFrame:(CGRect)frame {
    KuiklyRenderCallback callback = objc_getAssociatedObject(self.targetView, kSelectChangeCallbackKey);
    if (callback) {
        callback([self frameToDictionary:frame]);
    }
}

- (void)textSelectionHelper:(KRTextSelectionHelper *)helper didEndWithFrame:(CGRect)frame {
    KuiklyRenderCallback callback = objc_getAssociatedObject(self.targetView, kSelectEndCallbackKey);
    if (callback) {
        callback([self frameToDictionary:frame]);
    }
}

- (void)textSelectionHelperDidCancel:(KRTextSelectionHelper *)helper {
    KuiklyRenderCallback callback = objc_getAssociatedObject(self.targetView, kSelectCancelCallbackKey);
    if (callback) {
        callback(nil);
    }
}

- (NSDictionary *)frameToDictionary:(CGRect)frame {
    return @{
        @"x": @(frame.origin.x),
        @"y": @(frame.origin.y),
        @"width": @(frame.size.width),
        @"height": @(frame.size.height)
    };
}

@end

#pragma mark - KRView+TextSelection

@implementation KRView (TextSelection)

#pragma mark - Associated Object Accessors

- (KRTextSelectionHelper *)kr_textSelectionHelper {
    return objc_getAssociatedObject(self, kTextSelectionHelperKey);
}

- (void)kr_setTextSelectionHelper:(KRTextSelectionHelper *)helper {
    objc_setAssociatedObject(self, kTextSelectionHelperKey, helper, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (KRTextSelectionDelegateHandler *)kr_delegateHandler {
    static const void *kDelegateHandlerKey = &kDelegateHandlerKey;
    KRTextSelectionDelegateHandler *handler = objc_getAssociatedObject(self, kDelegateHandlerKey);
    if (!handler) {
        handler = [[KRTextSelectionDelegateHandler alloc] init];
        handler.targetView = self;
        objc_setAssociatedObject(self, kDelegateHandlerKey, handler, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return handler;
}

#pragma mark - Public Methods

- (BOOL)kr_handleTextSelectionMethod:(NSString *)method
                              params:(NSString *)params
                            callback:(KuiklyRenderCallback)callback {
    if ([method isEqualToString:KRTextSelectionMethodCreateSelection]) {
        [self kr_handleCreateSelectionWithParams:params];
        return YES;
    } else if ([method isEqualToString:KRTextSelectionMethodGetSelection]) {
        [self kr_handleGetSelectionWithCallback:callback];
        return YES;
    } else if ([method isEqualToString:KRTextSelectionMethodClearSelection]) {
        [self kr_handleClearSelection];
        return YES;
    } else if ([method isEqualToString:KRTextSelectionMethodCreateSelectionAll]) {
        [self kr_handleCreateSelectionAll];
        return YES;
    }
    return NO;
}

- (void)kr_cleanupTextSelection {
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (helper) {
        [helper endSelection];
        [self kr_setTextSelectionHelper:nil];
    }
}

#pragma mark - Property Handling (called via css_setPropWithKey)

// These are automatically handled by the KUIKLY_SET_CSS_COMMON_PROP macro
// through KVO-style property naming (css_selectable, css_selectionColor, etc.)
// The properties are stored as associated objects when set.

#pragma mark - Private Methods

- (void)kr_handleCreateSelectionWithParams:(NSString *)params {
    NSDictionary *paramsDict = [params hr_stringToDictionary];
    if (!paramsDict) return;
    
    CGFloat x = [paramsDict[@"x"] floatValue];
    CGFloat y = [paramsDict[@"y"] floatValue];
    NSInteger type = [paramsDict[@"type"] integerValue];
    
    // Create selection helper if needed
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (!helper) {
        helper = [[KRTextSelectionHelper alloc] init];
        helper.delegate = [self kr_delegateHandler];
        [self kr_setTextSelectionHelper:helper];
    }
    
    // Collect all KRLabel subviews
    NSMutableArray<KRLabel *> *labels = [NSMutableArray array];
    [self kr_findAllKRLabelsInView:self toArray:labels];
    
    if (labels.count == 0) return;
    
    // Sort labels by position (y then x)
    [labels sortUsingComparator:^NSComparisonResult(KRLabel *obj1, KRLabel *obj2) {
        CGRect r1 = [obj1 convertRect:obj1.bounds toView:self];
        CGRect r2 = [obj2 convertRect:obj2.bounds toView:self];
        
        if (ABS(r1.origin.y - r2.origin.y) > 5) { // Different lines
            return r1.origin.y < r2.origin.y ? NSOrderedAscending : NSOrderedDescending;
        } else { // Same line
            return r1.origin.x < r2.origin.x ? NSOrderedAscending : NSOrderedDescending;
        }
    }];
    
    // Apply selection color if set
    [self kr_applySelectionColorToHelper:helper];
    
    // Start selection
    [helper startSelectionWithLabels:labels containerView:self];
    [helper selectAtPoint:CGPointMake(x, y) type:(KRTextSelectionType)type];
}

- (void)kr_handleGetSelectionWithCallback:(KuiklyRenderCallback)callback {
    if (!callback) return;
    
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    NSArray<NSString *> *texts = [helper getSelectedTexts];
    NSDictionary *result = @{
        @"content": texts ?: @[]
    };
    callback(result);
}

- (void)kr_handleClearSelection {
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    [helper endSelection];
}

- (void)kr_handleCreateSelectionAll {
    // Get or create helper
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (!helper) {
        helper = [[KRTextSelectionHelper alloc] init];
        [self kr_setTextSelectionHelper:helper];
        
        // Setup delegate handler
        KRTextSelectionDelegateHandler *delegateHandler = [[KRTextSelectionDelegateHandler alloc] init];
        delegateHandler.targetView = self;
        helper.delegate = delegateHandler;
        // Store delegate handler to prevent deallocation
        objc_setAssociatedObject(helper, @selector(delegate), delegateHandler, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    
    // Find all selectable labels
    NSMutableArray<KRLabel *> *labels = [NSMutableArray array];
    [self kr_findAllKRLabelsInView:self toArray:labels];
    
    if (labels.count == 0) return;
    
    // Apply selection color if set
    [self kr_applySelectionColorToHelper:helper];
    
    // Start selection with labels
    [helper startSelectionWithLabels:labels containerView:self];
    
    // Select all
    [helper selectAll];
}

- (void)kr_findAllKRLabelsInView:(UIView *)view toArray:(NSMutableArray<KRLabel *> *)array {
    for (UIView *subview in view.subviews) {
        if ([subview isKindOfClass:[KRLabel class]]) {
            KRLabel *label = (KRLabel *)subview;
            // Check if this label is selectable based on parent selectable settings
            if ([self kr_isLabelSelectable:label]) {
                [array addObject:label];
            }
        }
        [self kr_findAllKRLabelsInView:subview toArray:array];
    }
}

- (BOOL)kr_isLabelSelectable:(KRLabel *)label {
    // Walk up the view hierarchy from the label to self (the SelectionContainer)
    // Check each view's selectable setting
    // INHERIT (0) = continue checking parent
    // ENABLE (1) = allow selection
    // DISABLE (2) = deny selection
    
    UIView *currentView = label;
    
    while (currentView != nil && currentView != self.superview) {
        NSNumber *selectableNum = objc_getAssociatedObject(currentView, kSelectableKey);
        if (selectableNum != nil) {
            KRSelectableOption selectable = (KRSelectableOption)[selectableNum integerValue];
            
            // ENABLE: allow selection for this subtree
            if (selectable == KRSelectableOptionEnable) {
                return YES;
            }
            
            // DISABLE: deny selection for this subtree
            if (selectable == KRSelectableOptionDisable) {
                return NO;
            }
            
            // INHERIT: continue checking parent
        }
        
        currentView = currentView.superview;
    }
    
    // If we reached the top without finding explicit ENABLE or DISABLE,
    // check if self (the container) has selection enabled or not explicitly disabled.
    // Since createSelection was called on this container, we treat INHERIT/ENABLE as allowing selection.
    NSNumber *selfSelectableNum = objc_getAssociatedObject(self, kSelectableKey);
    if (selfSelectableNum != nil) {
        KRSelectableOption selfSelectable = (KRSelectableOption)[selfSelectableNum integerValue];
        // Only deny if explicitly disabled
        return selfSelectable != KRSelectableOptionDisable;
    }
    
    // Default: allow selection when createSelection is called (implies intent to select)
    return YES;
}

- (void)kr_applySelectionColorToHelper:(KRTextSelectionHelper *)helper {
    NSString *selectionColorStr = objc_getAssociatedObject(self, kSelectionColorKey);
    if (!selectionColorStr || !helper) return;
    
    NSDictionary *colorConfig = [selectionColorStr hr_stringToDictionary];
    if (!colorConfig) return;
    
    // Parse background color
    NSNumber *backgroundColorNum = colorConfig[@"background"];
    if (backgroundColorNum) {
        unsigned long long colorValue = [backgroundColorNum unsignedLongLongValue];
        UIColor *backgroundColor = [UIColor colorWithRed:((colorValue >> 16) & 0xFF) / 255.0
                                                   green:((colorValue >> 8) & 0xFF) / 255.0
                                                    blue:(colorValue & 0xFF) / 255.0
                                                   alpha:((colorValue >> 24) & 0xFF) / 255.0];
        [helper setSelectionColor:backgroundColor];
    }
    
    // Parse cursor color
    NSNumber *cursorColorNum = colorConfig[@"cursor"];
    if (cursorColorNum) {
        unsigned long long colorValue = [cursorColorNum unsignedLongLongValue];
        UIColor *cursorColor = [UIColor colorWithRed:((colorValue >> 16) & 0xFF) / 255.0
                                               green:((colorValue >> 8) & 0xFF) / 255.0
                                                blue:(colorValue & 0xFF) / 255.0
                                               alpha:((colorValue >> 24) & 0xFF) / 255.0];
        [helper setCursorColor:cursorColor];
    }
}

#pragma mark - CSS Property Setters (for dynamic property handling)

// These methods will be called when properties are set via hrv_setPropWithKey
// The naming convention css_<propName> is required for KUIKLY_SET_CSS_COMMON_PROP macro

- (void)setCss_selectable:(NSNumber *)selectable {
    objc_setAssociatedObject(self, kSelectableKey, selectable, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSNumber *)css_selectable {
    return objc_getAssociatedObject(self, kSelectableKey);
}

- (void)setCss_selectionColor:(NSString *)selectionColor {
    objc_setAssociatedObject(self, kSelectionColorKey, selectionColor, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSString *)css_selectionColor {
    return objc_getAssociatedObject(self, kSelectionColorKey);
}

- (void)setCss_selectStart:(KuiklyRenderCallback)callback {
    objc_setAssociatedObject(self, kSelectStartCallbackKey, callback, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

- (KuiklyRenderCallback)css_selectStart {
    return objc_getAssociatedObject(self, kSelectStartCallbackKey);
}

- (void)setCss_selectChange:(KuiklyRenderCallback)callback {
    objc_setAssociatedObject(self, kSelectChangeCallbackKey, callback, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

- (KuiklyRenderCallback)css_selectChange {
    return objc_getAssociatedObject(self, kSelectChangeCallbackKey);
}

- (void)setCss_selectEnd:(KuiklyRenderCallback)callback {
    objc_setAssociatedObject(self, kSelectEndCallbackKey, callback, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

- (KuiklyRenderCallback)css_selectEnd {
    return objc_getAssociatedObject(self, kSelectEndCallbackKey);
}

- (void)setCss_selectCancel:(KuiklyRenderCallback)callback {
    objc_setAssociatedObject(self, kSelectCancelCallbackKey, callback, OBJC_ASSOCIATION_COPY_NONATOMIC);
}

- (KuiklyRenderCallback)css_selectCancel {
    return objc_getAssociatedObject(self, kSelectCancelCallbackKey);
}

@end

