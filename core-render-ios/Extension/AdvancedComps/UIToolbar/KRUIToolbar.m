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

#import "KRUIToolbar.h"
#import "KRComponentDefine.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"
#import <objc/runtime.h>

#if !TARGET_OS_OSX // [macOS]

static const void *kKRToolbarLeadingInsetKey = &kKRToolbarLeadingInsetKey;
static const void *kKRToolbarTrailingInsetKey = &kKRToolbarTrailingInsetKey;
static const void *kKRToolbarCenterSampleWidthKey = &kKRToolbarCenterSampleWidthKey;
static const void *kKRToolbarCenterSampleMidKey = &kKRToolbarCenterSampleMidKey;
static const void *kKRToolbarCenterTriesKey = &kKRToolbarCenterTriesKey;
static const void *kKRToolbarCenterInsetKey = &kKRToolbarCenterInsetKey;
static const void *kKRToolbarLeadingSpaceBaseWidthKey = &kKRToolbarLeadingSpaceBaseWidthKey;
static const void *kKRToolbarChromeCapturedKey = &kKRToolbarChromeCapturedKey;
static const void *kKRToolbarSavedTintKey = &kKRToolbarSavedTintKey;
static const void *kKRToolbarSavedToolbarHiddenKey = &kKRToolbarSavedToolbarHiddenKey;
static BOOL kr_shiftingToolbarCapsules = NO;

static const NSInteger kKRToolbarLeadingSpaceTag = -2;
/// 勿用 `kr_toolbar_` 前缀，避免 `KRCollectToolbarButtons` 把 inset 空白当成胶囊按钮采样。
static NSString * const kKRToolbarInsetAnchorIdentifier = @"kuikly_inset_anchor";
static const CGFloat kKRToolbarInsetAnchorDefaultWidth = 16.0;

static BOOL KRToolbarViewIsInsetAnchor(UIView *view) {
    NSString *identifier = view.accessibilityIdentifier;
    return identifier.length > 0 && [identifier isEqualToString:kKRToolbarInsetAnchorIdentifier];
}

static void KRCollectToolbarButtons(UIView *view, NSMutableArray<UIView *> *result) {
    // 只量 Kuikly 按钮本身（`kr_toolbar_*`）；排除 inset 占位（`kuikly_inset_anchor`）。
    if (KRToolbarViewIsInsetAnchor(view)) {
        return;
    }
    if ([view.accessibilityIdentifier hasPrefix:@"kr_toolbar_"] && CGRectGetWidth(view.bounds) < 96.0) {
        [result addObject:view];
        return;
    }
    for (UIView *subview in view.subviews) {
        KRCollectToolbarButtons(subview, result);
    }
}

static UIView *KRToolbarCapsule(UIView *button, UIView *toolbar, UIView *root) {
    UIView *node = button;
    CGFloat fullWidth = CGRectGetWidth(root.bounds);
    while (node.superview && node.superview != toolbar && node.superview != root) {
        CGRect parentRect = [node.superview convertRect:node.superview.bounds toView:root];
        if (CGRectGetWidth(parentRect) > fullWidth - 80.0) {
            break;
        }
        node = node.superview;
    }
    return node;
}

@interface KRUIToolbar ()
@property (nonatomic, strong) id css_tintColor;
@property (nonatomic, copy) NSString *css_leadingInset;
@property (nonatomic, copy) NSString *css_trailingInset;
+ (UIBarButtonItem *)p_makeInsetAnchorSpaceWithWidth:(CGFloat)width;
+ (void)p_ensureLeadingInsetSpacerForViewController:(UIViewController *)viewController;
+ (void)p_restoreLeadingInsetAdjustmentForViewController:(UIViewController *)viewController;
+ (void)p_restoreToolbarChromeForViewController:(UIViewController *)viewController;
+ (void)shiftToolbarCapsulesForViewController:(UIViewController *)viewController;
- (void)p_captureToolbarChromeIfNeeded:(UIViewController *)viewController;
- (void)p_teardownToolbarChrome;
@end

@implementation KRUIToolbar

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.hidden = YES;
        self.alpha = 0;
        self.userInteractionEnabled = NO;
        self.backgroundColor = [UIColor clearColor];
        _css_leadingInset = @"0";
        _css_trailingInset = @"0";
    }
    return self;
}

#pragma mark - KuiklyRenderViewExportProtocol

- (void)hrv_setPropWithKey:(NSString *)propKey propValue:(id)propValue {
    KUIKLY_SET_CSS_COMMON_PROP;
}

- (void)hrv_callWithMethod:(NSString *)method params:(NSString *)params callback:(KuiklyRenderCallback)callback {
    KUIKLY_CALL_CSS_METHOD;
}

- (void)setCss_tintColor:(id)css_tintColor {
    _css_tintColor = css_tintColor;
    [self p_syncToToolbar];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_syncToToolbar];
}

- (void)setCss_leadingInset:(NSString *)css_leadingInset {
    _css_leadingInset = [css_leadingInset copy];
    [self p_syncToToolbar];
}

- (void)setCss_trailingInset:(NSString *)css_trailingInset {
    _css_trailingInset = [css_trailingInset copy];
    [self p_syncToToolbar];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview) {
        [self p_syncToToolbar];
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    self.hidden = YES;
    self.alpha = 0;
    self.userInteractionEnabled = NO;
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_syncToToolbar];
        __weak typeof(self) weakSelf = self;
        [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
            [weakSelf p_teardownToolbarChrome];
        }];
    } else {
        [self p_teardownToolbarChrome];
    }
}

- (void)removeFromSuperview {
    [self p_teardownToolbarChrome];
    [super removeFromSuperview];
}

#pragma mark - private

- (UIViewController *)p_hostViewController {
    return self.kr_viewController ?: self.hr_rootView.kr_viewController;
}

- (BOOL)p_shouldShow {
    if (self.css_visibility != nil) {
        return self.css_visibility.boolValue;
    }
    return YES;
}

- (void)p_syncToToolbar {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    if (![self p_shouldShow]) {
        [self p_teardownToolbarChrome];
        return;
    }
    [self p_captureToolbarChromeIfNeeded:viewController];
    UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
    if (tint) {
        viewController.navigationController.toolbar.tintColor = tint;
    }
    CGFloat inset = MAX(0, self.css_leadingInset.doubleValue);
    CGFloat trailing = MAX(0, self.css_trailingInset.doubleValue);
    objc_setAssociatedObject(viewController, kKRToolbarLeadingInsetKey, @(inset), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarTrailingInsetKey, @(trailing), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    UINavigationController *navigationController = viewController.navigationController;
    if (navigationController && navigationController.isToolbarHidden) {
        [navigationController setToolbarHidden:NO animated:YES];
    }
    [KRUIToolbar scheduleApplyLeadingInsetForViewController:viewController];
}

- (void)p_captureToolbarChromeIfNeeded:(UIViewController *)viewController {
    if (!viewController || objc_getAssociatedObject(viewController, kKRToolbarChromeCapturedKey)) {
        return;
    }
    UINavigationController *navigationController = viewController.navigationController;
    if (!navigationController) {
        return;
    }
    UIColor *tint = navigationController.toolbar.tintColor;
    objc_setAssociatedObject(viewController,
                             kKRToolbarSavedTintKey,
                             tint ?: [NSNull null],
                             OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController,
                             kKRToolbarSavedToolbarHiddenKey,
                             @(navigationController.isToolbarHidden),
                             OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarChromeCapturedKey, @YES, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

+ (void)p_restoreToolbarChromeForViewController:(UIViewController *)viewController {
    if (!viewController || !objc_getAssociatedObject(viewController, kKRToolbarChromeCapturedKey)) {
        return;
    }
    UINavigationController *navigationController = viewController.navigationController;
    if (navigationController) {
        id savedTint = objc_getAssociatedObject(viewController, kKRToolbarSavedTintKey);
        if (savedTint && savedTint != [NSNull null]) {
            navigationController.toolbar.tintColor = savedTint;
        }
        NSNumber *savedHidden = objc_getAssociatedObject(viewController, kKRToolbarSavedToolbarHiddenKey);
        if (savedHidden) {
            [navigationController setToolbarHidden:savedHidden.boolValue animated:NO];
        }
    }
    objc_setAssociatedObject(viewController, kKRToolbarChromeCapturedKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarSavedTintKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarSavedToolbarHiddenKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

+ (UIBarButtonItem *)p_makeFixedSpaceWithWidth:(CGFloat)width
                                           tag:(NSInteger)tag
                                    identifier:(NSString *)identifier {
    UIBarButtonItem *space = nil;
    if (@available(iOS 14.0, *)) {
        space = [UIBarButtonItem fixedSpaceItemOfWidth:width];
    } else {
        space = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace
                                                             target:nil
                                                             action:nil];
        space.width = width;
    }
    space.tag = tag;
    if (identifier.length) {
        space.accessibilityIdentifier = identifier;
    }
    return space;
}

+ (UIBarButtonItem *)p_makeInsetAnchorSpaceWithWidth:(CGFloat)width {
    return [self p_makeFixedSpaceWithWidth:width
                                       tag:kKRToolbarLeadingSpaceTag
                                identifier:kKRToolbarInsetAnchorIdentifier];
}

+ (void)p_ensureLeadingInsetSpacerForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    NSNumber *stored = objc_getAssociatedObject(viewController, kKRToolbarLeadingInsetKey);
    NSNumber *trailingStored = objc_getAssociatedObject(viewController, kKRToolbarTrailingInsetKey);
    CGFloat inset = MAX(0, stored.doubleValue);
    CGFloat trailing = MAX(0, trailingStored.doubleValue);
    if (inset < 1.0 && trailing < 1.0) {
        return;
    }
    for (UIBarButtonItem *item in viewController.toolbarItems) {
        if (item.tag == kKRToolbarLeadingSpaceTag) {
            return;
        }
    }
    UIBarButtonItem *anchor = [self p_makeInsetAnchorSpaceWithWidth:kKRToolbarInsetAnchorDefaultWidth];
    NSMutableArray<UIBarButtonItem *> *items = [(viewController.toolbarItems ?: @[]) mutableCopy];
    [items insertObject:anchor atIndex:0];
    kr_shiftingToolbarCapsules = YES;
    [viewController setToolbarItems:items animated:NO];
    kr_shiftingToolbarCapsules = NO;
}

+ (void)p_restoreLeadingInsetAdjustmentForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    NSNumber *baseWidth = objc_getAssociatedObject(viewController, kKRToolbarLeadingSpaceBaseWidthKey);
    UIBarButtonItem *leading = nil;
    NSUInteger leadingIndex = NSNotFound;
    NSArray<UIBarButtonItem *> *current = viewController.toolbarItems ?: @[];
    for (NSUInteger index = 0; index < current.count; index++) {
        if (current[index].tag == kKRToolbarLeadingSpaceTag) {
            leading = current[index];
            leadingIndex = index;
            break;
        }
    }
    NSMutableArray<UIBarButtonItem *> *items = [NSMutableArray array];
    BOOL changed = NO;
    for (UIBarButtonItem *item in current) {
        if ([item.accessibilityIdentifier isEqualToString:kKRToolbarInsetAnchorIdentifier]) {
            changed = YES;
            continue;
        }
        [items addObject:item];
    }
    if (baseWidth && leading && leadingIndex != NSNotFound
        && ![leading.accessibilityIdentifier isEqualToString:kKRToolbarInsetAnchorIdentifier]) {
        UIBarButtonItem *replacement =
            [self p_makeFixedSpaceWithWidth:baseWidth.doubleValue
                                         tag:leading.tag
                                  identifier:leading.accessibilityIdentifier ?: @""];
        NSUInteger replacementIndex = [items indexOfObject:leading];
        if (replacementIndex != NSNotFound) {
            items[replacementIndex] = replacement;
            changed = YES;
        }
    }
    if (changed) {
        kr_shiftingToolbarCapsules = YES;
        [viewController setToolbarItems:items animated:NO];
        kr_shiftingToolbarCapsules = NO;
    }
    objc_setAssociatedObject(viewController, kKRToolbarLeadingSpaceBaseWidthKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterInsetKey, nil, OBJC_ASSOCIATION_COPY_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterTriesKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterSampleWidthKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterSampleMidKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

+ (void)shiftToolbarCapsulesForViewController:(UIViewController *)viewController {
    if (!viewController || kr_shiftingToolbarCapsules) {
        return;
    }
    NSNumber *stored = objc_getAssociatedObject(viewController, kKRToolbarLeadingInsetKey);
    if (!stored) {
        return;
    }
    CGFloat inset = MAX(0, stored.doubleValue);
    NSNumber *trailingStored = objc_getAssociatedObject(viewController, kKRToolbarTrailingInsetKey);
    CGFloat trailing = MAX(0, trailingStored.doubleValue);
    if (inset < 1.0 && trailing < 1.0) {
        [self p_restoreLeadingInsetAdjustmentForViewController:viewController];
        return;
    }
    UINavigationController *navigationController = viewController.navigationController;
    UIToolbar *toolbar = navigationController.toolbar;
    UIView *page = navigationController.view;
    UIView *root = page.window ?: page;
    if (!toolbar || !page || !root || navigationController.toolbarHidden) {
        return;
    }
    NSMutableArray<UIView *> *buttons = [NSMutableArray array];
    KRCollectToolbarButtons(root, buttons);
    if (buttons.count == 0) {
        return;
    }
    NSMutableArray<UIView *> *capsules = [NSMutableArray array];
    for (UIView *button in buttons) {
        UIView *capsule = KRToolbarCapsule(button, toolbar, root);
        if (capsule && ![capsules containsObject:capsule]) {
            [capsules addObject:capsule];
        }
    }
    CGFloat fullWidth = CGRectGetWidth(root.bounds);
    NSMutableArray<UIView *> *withBackgrounds = [capsules mutableCopy];
    for (UIView *capsule in capsules) {
        for (UIView *sibling in capsule.superview.subviews) {
            if ([withBackgrounds containsObject:sibling]) {
                continue;
            }
            CGRect siblingRect = [sibling convertRect:sibling.bounds toView:root];
            if (CGRectGetWidth(siblingRect) > fullWidth - 80.0) {
                continue;
            }
            BOOL overlaps = NO;
            for (UIView *button in buttons) {
                CGRect buttonRect = [button convertRect:button.bounds toView:root];
                if (CGRectIntersectsRect(CGRectInset(siblingRect, -8.0, -12.0), buttonRect)) {
                    overlaps = YES;
                    break;
                }
            }
            if (overlaps) {
                [withBackgrounds addObject:sibling];
            }
        }
    }
    NSMutableArray<UIView *> *outer = [NSMutableArray array];
    for (UIView *capsule in withBackgrounds) {
        BOOL nested = NO;
        for (UIView *other in withBackgrounds) {
            if (other != capsule && [capsule isDescendantOfView:other]) {
                nested = YES;
                break;
            }
        }
        if (!nested) {
            [outer addObject:capsule];
        }
    }
    if (outer.count == 0) {
        return;
    }
    CGRect unionRect = CGRectNull;
    for (UIView *capsule in outer) {
        unionRect = CGRectUnion(unionRect, [capsule convertRect:capsule.bounds toView:root]);
    }
    CGRect pageRect = [page convertRect:page.bounds toView:root];
    CGFloat paneLeft = CGRectGetMinX(pageRect) + inset;
    CGFloat paneRight = CGRectGetMaxX(pageRect) - trailing;
    if (paneRight - paneLeft < 1.0) {
        return;
    }
    // 改玻璃的 center 不会跟着动。左边固定空白的宽度会带动胶囊，按实测偏差改这一段宽度。
    CGFloat desiredMid = (paneLeft + paneRight) * 0.5;
    CGFloat actualMid = CGRectGetMidX(unionRect);
    CGFloat error = desiredMid - actualMid;
    NSString *insetSig = [NSString stringWithFormat:@"%.1f-%.1f", inset, trailing];
    NSString *prevSig = objc_getAssociatedObject(viewController, kKRToolbarCenterInsetKey);
    if (![insetSig isEqualToString:prevSig]) {
        objc_setAssociatedObject(viewController, kKRToolbarCenterInsetKey, insetSig, OBJC_ASSOCIATION_COPY_NONATOMIC);
        objc_setAssociatedObject(viewController, kKRToolbarCenterTriesKey, @0, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        objc_setAssociatedObject(viewController, kKRToolbarCenterSampleWidthKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        objc_setAssociatedObject(viewController, kKRToolbarCenterSampleMidKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    if (fabs(error) < 1.0) {
        objc_setAssociatedObject(viewController, kKRToolbarCenterTriesKey, @0, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        return;
    }
    NSInteger tries = [objc_getAssociatedObject(viewController, kKRToolbarCenterTriesKey) integerValue];
    if (tries >= 8) {
        return;
    }
    UIBarButtonItem *leading = nil;
    for (UIBarButtonItem *item in viewController.toolbarItems) {
        if (item.tag == kKRToolbarLeadingSpaceTag) {
            leading = item;
            break;
        }
    }
    if (!leading) {
        return;
    }
    if (!objc_getAssociatedObject(viewController, kKRToolbarLeadingSpaceBaseWidthKey)) {
        objc_setAssociatedObject(viewController,
                                 kKRToolbarLeadingSpaceBaseWidthKey,
                                 @(leading.width),
                                 OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    CGFloat width = leading.width;
    NSNumber *prevWidth = objc_getAssociatedObject(viewController, kKRToolbarCenterSampleWidthKey);
    NSNumber *prevMid = objc_getAssociatedObject(viewController, kKRToolbarCenterSampleMidKey);
    CGFloat gain = 1.0;
    if (prevWidth && prevMid) {
        CGFloat deltaWidth = width - prevWidth.doubleValue;
        CGFloat deltaMid = actualMid - prevMid.doubleValue;
        if (fabs(deltaWidth) > 0.5 && fabs(deltaMid) > 0.5) {
            gain = deltaMid / deltaWidth;
        } else if (fabs(deltaWidth) > 0.5) {
            return;
        }
    }
    if (fabs(gain) < 0.2) {
        gain = gain < 0.0 ? -1.0 : 1.0;
    }
    CGFloat newWidth = MAX(0.0, width + error / gain);
    if (fabs(newWidth - width) < 0.5) {
        return;
    }
    objc_setAssociatedObject(viewController, kKRToolbarCenterSampleWidthKey, @(width), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterSampleMidKey, @(actualMid), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarCenterTriesKey, @(tries + 1), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    UIBarButtonItem *space = nil;
    if (@available(iOS 14.0, *)) {
        space = [UIBarButtonItem fixedSpaceItemOfWidth:newWidth];
    } else {
        space = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:nil action:nil];
        space.width = newWidth;
    }
    space.tag = leading.tag;
    space.accessibilityIdentifier = leading.accessibilityIdentifier;
    NSMutableArray<UIBarButtonItem *> *items = [viewController.toolbarItems mutableCopy];
    NSUInteger index = [items indexOfObject:leading];
    if (index == NSNotFound) {
        return;
    }
    items[index] = space;
    kr_shiftingToolbarCapsules = YES;
    [viewController setToolbarItems:items animated:NO];
    kr_shiftingToolbarCapsules = NO;
    [toolbar setNeedsLayout];
}

+ (void)applyLeadingInsetForViewController:(UIViewController *)viewController {
    [self p_ensureLeadingInsetSpacerForViewController:viewController];
    [self shiftToolbarCapsulesForViewController:viewController];
}

+ (void)scheduleApplyLeadingInsetForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    __weak UIViewController *weakVC = viewController;
    dispatch_async(dispatch_get_main_queue(), ^{
        __strong UIViewController *strongVC = weakVC;
        if (!strongVC) {
            return;
        }
        [KRUIToolbar applyLeadingInsetForViewController:strongVC];
    });
}

- (void)p_teardownToolbarChrome {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    [KRUIToolbar p_restoreLeadingInsetAdjustmentForViewController:viewController];
    objc_setAssociatedObject(viewController, kKRToolbarLeadingInsetKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    objc_setAssociatedObject(viewController, kKRToolbarTrailingInsetKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [KRUIToolbar p_restoreToolbarChromeForViewController:viewController];
}

@end

#endif // [macOS]
