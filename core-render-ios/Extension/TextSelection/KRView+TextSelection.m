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
#import "KRConvertUtil.h"
#import "KRLogModule.h"
#import <objc/runtime.h>

NSString *const KRTextSelectionMethodCreateSelection = @"createSelection";
NSString *const KRTextSelectionMethodGetSelection = @"getSelection";
NSString *const KRTextSelectionMethodClearSelection = @"clearSelection";
NSString *const KRTextSelectionMethodCreateSelectionAll = @"createSelectionAll";

static const void *kTextSelectionHelperKey = &kTextSelectionHelperKey;
static const void *kSelectionDelegateHandlerKey = &kSelectionDelegateHandlerKey;
static const void *kSelectableKey = &kSelectableKey;
static const void *kSelectionColorKey = &kSelectionColorKey;
static const void *kSelectStartCallbackKey = &kSelectStartCallbackKey;
static const void *kSelectChangeCallbackKey = &kSelectChangeCallbackKey;
static const void *kSelectEndCallbackKey = &kSelectEndCallbackKey;
static const void *kSelectCancelCallbackKey = &kSelectCancelCallbackKey;

typedef NS_ENUM(NSInteger, KRSelectableOption) {
    KRSelectableOptionInherit = 0,
    KRSelectableOptionEnable = 1,
    KRSelectableOptionDisable = 2
};

#pragma mark - Internal Delegate Handler

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
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_setTextSelectionHelper view:%@ helper:%p", self, helper]];
    objc_setAssociatedObject(self, kTextSelectionHelperKey, helper, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (KRTextSelectionDelegateHandler *)kr_delegateHandler {
    KRTextSelectionDelegateHandler *handler = objc_getAssociatedObject(self, kSelectionDelegateHandlerKey);
    if (!handler) {
        handler = [[KRTextSelectionDelegateHandler alloc] init];
        handler.targetView = self;
        objc_setAssociatedObject(self, kSelectionDelegateHandlerKey, handler, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return handler;
}

#pragma mark - Public Methods

- (BOOL)kr_handleTextSelectionMethod:(NSString *)method
                              params:(NSString * _Nullable)params
                            callback:(KuiklyRenderCallback _Nullable)callback {
    if ([method isEqualToString:KRTextSelectionMethodCreateSelection]) {
        [self kr_handleCreateSelectionWithParams:params callback:callback];
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

- (void)kr_handleCreateSelectionWithParams:(NSString *)params callback:(KuiklyRenderCallback)callback {
    NSDictionary *paramDict = [params hr_stringToDictionary];
    if (!paramDict) return;
    
    CGFloat x = [paramDict[@"x"] floatValue];
    CGFloat y = [paramDict[@"y"] floatValue];
    NSInteger type = [paramDict[@"type"] integerValue];
    
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (!helper) {
        helper = [[KRTextSelectionHelper alloc] init];
        helper.delegate = [self kr_delegateHandler];
        [self kr_setTextSelectionHelper:helper];
    }
    
    NSMutableArray<KRLabel *> *labels = [NSMutableArray array];
    [self kr_findAllKRLabelsInView:self toArray:labels];
    if (labels.count == 0) return;
    
    [self kr_applySelectionColorToHelper:helper];
    
    [helper startSelectionWithLabels:labels containerView:self];
    [helper selectAtPoint:CGPointMake(x, y) type:(KRTextSelectionType)type];
}

- (void)kr_handleGetSelectionWithCallback:(KuiklyRenderCallback)callback {
    if (!callback) return;
    
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    NSArray<NSString *> *texts = [helper getSelectedTexts];
    NSArray<NSString *> *preContent = [helper getPreSelectionContent];
    NSArray<NSString *> *postContent = [helper getPostSelectionContent];
    
    NSDictionary *result = @{
        @"content": texts ?: @[],
        @"preContent": preContent ?: @[],
        @"postContent": postContent ?: @[]
    };
    
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] getSelection content:%lu pre:%lu post:%lu",
                          (unsigned long)texts.count,
                          (unsigned long)preContent.count,
                          (unsigned long)postContent.count]];
    callback(result);
}

- (void)kr_handleClearSelection {
    [self kr_cleanupTextSelection];
}

- (void)kr_handleCreateSelectionAll {
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (!helper) {
        helper = [[KRTextSelectionHelper alloc] init];
        helper.delegate = [self kr_delegateHandler];
        [self kr_setTextSelectionHelper:helper];
    }
    
    NSMutableArray<KRLabel *> *labels = [NSMutableArray array];
    [self kr_findAllKRLabelsInView:self toArray:labels];
    if (labels.count == 0) return;
    
    [self kr_applySelectionColorToHelper:helper];
    
    [helper startSelectionWithLabels:labels containerView:self];
    [helper selectAll];
}

- (void)kr_findAllKRLabelsInView:(UIView *)view toArray:(NSMutableArray<KRLabel *> *)array {
    NSInteger beforeCount = array.count;
    for (UIView *subview in view.subviews) {
        if ([subview isKindOfClass:[KRLabel class]]) {
            KRLabel *label = (KRLabel *)subview;
            if ([self kr_isLabelSelectable:label]) {
                [array addObject:label];
            }
        }
        [self kr_findAllKRLabelsInView:subview toArray:array];
    }
    if (array.count > beforeCount) {
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_findAllKRLabelsInView %@ found %ld labels (total:%lu)", view, (long)(array.count - beforeCount), (unsigned long)array.count]];
    }
}

- (BOOL)kr_isLabelSelectable:(KRLabel *)label {
    UIView *currentView = label;
    
    while (currentView != nil && currentView != self.superview) {
        NSNumber *selectableNum = objc_getAssociatedObject(currentView, kSelectableKey);
        if (selectableNum != nil) {
            KRSelectableOption selectable = (KRSelectableOption)[selectableNum integerValue];
            if (selectable == KRSelectableOptionEnable) {
                return YES;
            }
            if (selectable == KRSelectableOptionDisable) {
                [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_isLabelSelectable NO - disabled at %@", currentView]];
                return NO;
            }
        }
        currentView = currentView.superview;
    }
    
    NSNumber *selfSelectableNum = objc_getAssociatedObject(self, kSelectableKey);
    if (selfSelectableNum != nil) {
        KRSelectableOption selfSelectable = (KRSelectableOption)[selfSelectableNum integerValue];
        BOOL result = selfSelectable != KRSelectableOptionDisable;
        if (!result) {
            [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_isLabelSelectable NO - container disabled"]];
        }
        return result;
    }
    
    return YES;
}

- (void)kr_applySelectionColorToHelper:(KRTextSelectionHelper *)helper {
    NSString *selectionColorStr = objc_getAssociatedObject(self, kSelectionColorKey);
    if (!selectionColorStr || !helper) return;
    
    NSDictionary *colorConfig = [selectionColorStr hr_stringToDictionary];
    if (!colorConfig) return;
    
    NSNumber *backgroundColorNum = colorConfig[@"background"];
    if (backgroundColorNum) {
        UIColor *backgroundColor = [KRConvertUtil UIColor:backgroundColorNum];
        [helper setSelectionColor:backgroundColor];
    }
}

- (void)kr_cleanupTextSelection {
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_cleanupTextSelection helper:%p", helper]];
    [helper endSelection];
}

- (void)kr_setupTextSelectionIfNeeded {
    KRTextSelectionHelper *helper = [self kr_textSelectionHelper];
    if (!helper) {
        helper = [[KRTextSelectionHelper alloc] init];
        helper.delegate = [self kr_delegateHandler];
        [self kr_setTextSelectionHelper:helper];
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_setupTextSelectionIfNeeded created new helper:%p", helper]];
    } else {
        [KRLogModule logInfo:[NSString stringWithFormat:@"[TextSelection] kr_setupTextSelectionIfNeeded reuse helper:%p", helper]];
    }
    
    NSMutableArray<KRLabel *> *labels = [NSMutableArray array];
    [self kr_findAllKRLabelsInView:self toArray:labels];
    if (labels.count == 0) {
        [KRLogModule logInfo:@"[TextSelection] kr_setupTextSelectionIfNeeded no labels found"];
        return;
    }
    
    [self kr_applySelectionColorToHelper:helper];
    
    [helper startSelectionWithLabels:labels containerView:self];
}

#pragma mark - CSS Property Setters

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
