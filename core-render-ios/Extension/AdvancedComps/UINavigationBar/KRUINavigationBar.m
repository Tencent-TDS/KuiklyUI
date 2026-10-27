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

#import "KRUINavigationBar.h"
#import "KRComponentDefine.h"
#import "KRConvertUtil.h"
#import "KRUINavigationBarItemPlacement.h"
#import "KRUINavigationBarKit.h"
#import "KuiklyRenderView.h"
#import "NSObject+KR.h"

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270000
#import <UIKit/UIBarMinimization.h>
#endif

#if !TARGET_OS_OSX // [macOS]

static NSString *const kKRUINavigationBarTitleId = @"kuikly_ui_navigation_bar_title";

typedef NS_OPTIONS(NSUInteger, KRUINavigationBarDirty) {
    KRUINavigationBarDirtyInset = 1 << 0,
    KRUINavigationBarDirtyTitle = 1 << 1,
    KRUINavigationBarDirtyLargeTitleMode = 1 << 2,
    KRUINavigationBarDirtyMinimization = 1 << 3,
    KRUINavigationBarDirtyTintAndLargeTitles = 1 << 4,
    KRUINavigationBarDirtyAppearance = 1 << 5,
    KRUINavigationBarDirtyHideScrollEdgeEffects = 1 << 6,
    KRUINavigationBarDirtyContentScroll = 1 << 7,
    KRUINavigationBarDirtyNavigationBarHidden = 1 << 8,
    KRUINavigationBarDirtyBackButton = 1 << 9,
    /// 只改 standard/scrollEdge appearance 上的 title 文案样式（颜色/字体等），不重建栏背景。
    KRUINavigationBarDirtyTitleTextAttributes = 1 << 10,
};

static const KRUINavigationBarDirty KRUINavigationBarDirtyAll =
    KRUINavigationBarDirtyInset | KRUINavigationBarDirtyTitle | KRUINavigationBarDirtyLargeTitleMode
    | KRUINavigationBarDirtyMinimization | KRUINavigationBarDirtyTintAndLargeTitles
    | KRUINavigationBarDirtyAppearance | KRUINavigationBarDirtyHideScrollEdgeEffects
    | KRUINavigationBarDirtyContentScroll | KRUINavigationBarDirtyNavigationBarHidden
    | KRUINavigationBarDirtyBackButton | KRUINavigationBarDirtyTitleTextAttributes;

static NSString *KRNormalizedFontWeight(NSString *weight) {
    if (weight.length == 0) {
        return @"400";
    }
    NSDictionary *alias = @{
        @"regular": @"400",
        @"normal": @"normal",
        @"medium": @"500",
        @"semibold": @"600",
        @"bold": @"bold",
        @"light": @"300",
        @"thin": @"200",
        @"ultralight": @"100",
        @"heavy": @"800",
        @"black": @"900",
    };
    return alias[weight.lowercaseString] ?: weight;
}

@interface KRUINavigationBar ()
@property (nonatomic, copy) NSString *css_title;
@property (nonatomic, strong) id css_tintColor;
@property (nonatomic, copy) NSString *css_leadingInset;
@property (nonatomic, copy) NSString *css_trailingInset;
@property (nonatomic, copy) NSString *css_largeTitle;
@property (nonatomic, copy) NSString *css_contentScrollNativeRef;
@property (nonatomic, copy) NSString *css_navigationBarMinimizationBehavior;
@property (nonatomic, copy) NSString *css_navigationBarMinimizationSafeAreaAdjustment;
@property (nonatomic, copy) NSString *css_navigationBarMinimizationRestoration;
@property (nonatomic, copy) NSString *css_barBackground;
@property (nonatomic, strong) id css_barBackgroundColor;
@property (nonatomic, copy) NSString *css_barScrollEdgeBackground;
@property (nonatomic, strong) id css_barScrollEdgeBackgroundColor;
@property (nonatomic, strong) id css_barShadowColor;
@property (nonatomic, copy) NSString *css_barStyle;
@property (nonatomic, copy) NSString *css_barTranslucent;
@property (nonatomic, strong) id css_hideScrollEdgeEffects;
@property (nonatomic, copy) NSString *css_titleFontFamily;
@property (nonatomic, copy) NSString *css_titleFontWeight;
@property (nonatomic, copy) NSString *css_titleFontSize;
@property (nonatomic, copy) NSString *css_largeTitleFontSize;
@property (nonatomic, strong) id css_titleColor;
@property (nonatomic, copy) NSString *css_titleKern;
@property (nonatomic, strong) id css_titleShadowColor;
@property (nonatomic, copy) NSString *css_titleShadowOffsetX;
@property (nonatomic, copy) NSString *css_titleShadowOffsetY;
@property (nonatomic, copy) NSString *css_titleShadowRadius;
@property (nonatomic, copy) NSString *css_titleOffsetX;
@property (nonatomic, copy) NSString *css_titleOffsetY;
@property (nonatomic, copy) NSString *css_subtitle;
@property (nonatomic, copy) NSString *css_largeSubtitle;
@property (nonatomic, assign) NSInteger boundContentScrollNativeRef;
@property (nonatomic, assign) BOOL syncFlushScheduled;
@property (nonatomic, assign) BOOL capturedBarBaseline;
@property (nonatomic, strong, nullable) UIColor *baselineBarTintColor;
@property (nonatomic, assign) BOOL baselinePrefersLargeTitles;
@property (nonatomic, assign) UIBarStyle baselineBarStyle;
@property (nonatomic, assign) BOOL baselineBarTranslucent;
@property (nonatomic, assign) NSInteger appliedContentScrollRequestRef;
@property (nonatomic, assign) BOOL appliedNavigationBarHidden;
@property (nonatomic, assign) BOOL hasAppliedNavigationBarHidden;
@property (nonatomic, assign) NSInteger appliedLargeTitleDisplayMode;
@property (nonatomic, assign) BOOL hasAppliedLargeTitleDisplayMode;
@property (nonatomic, assign) BOOL appliedHideScrollEdgeEffectsScan;
@property (nonatomic, assign) KRUINavigationBarDirty pendingDirtyFlags;
@property (nonatomic, assign) BOOL hasAppliedBackButtonHiding;
@end

@implementation KRUINavigationBar

@synthesize hr_rootView;

- (instancetype)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        self.hidden = YES;
        self.alpha = 0;
        self.userInteractionEnabled = NO;
        self.backgroundColor = [UIColor clearColor];
        _css_title = @"";
        _css_leadingInset = @"0";
        _css_trailingInset = @"0";
        _css_largeTitle = @"never";
        _css_contentScrollNativeRef = @"0";
        _css_navigationBarMinimizationBehavior = @"automatic";
        _css_navigationBarMinimizationSafeAreaAdjustment = @"automatic";
        _css_navigationBarMinimizationRestoration = @"automatic";
        _css_barBackground = @"transparent";
        _css_barStyle = @"default";
        _css_barTranslucent = @"auto";
        _boundContentScrollNativeRef = 0;
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

- (void)setCss_title:(NSString *)css_title {
    _css_title = [css_title copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle];
}

- (void)setCss_tintColor:(id)css_tintColor {
    _css_tintColor = css_tintColor;
    KRUINavigationBarDirty flags = KRUINavigationBarDirtyTintAndLargeTitles;
    if (!self.css_titleColor) {
        flags |= KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle;
    }
    [self p_setNavigationBarDirtyOrApply:flags];
}

- (void)setCss_visibility:(NSNumber *)css_visibility {
    [super setCss_visibility:css_visibility];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyNavigationBarHidden];
}

- (void)setCss_leadingInset:(NSString *)css_leadingInset {
    _css_leadingInset = [css_leadingInset copy] ?: @"0";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyInset];
}

- (void)setCss_trailingInset:(NSString *)css_trailingInset {
    _css_trailingInset = [css_trailingInset copy] ?: @"0";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyInset];
}

- (void)setCss_largeTitle:(NSString *)css_largeTitle {
    _css_largeTitle = [css_largeTitle copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyLargeTitleMode | KRUINavigationBarDirtyTitle
                                           | KRUINavigationBarDirtyTintAndLargeTitles
                                           | KRUINavigationBarDirtyAppearance];
}

- (void)setCss_contentScrollNativeRef:(NSString *)css_contentScrollNativeRef {
    _css_contentScrollNativeRef = [css_contentScrollNativeRef copy] ?: @"0";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyContentScroll];
}

- (void)setCss_navigationBarMinimizationBehavior:(NSString *)css_navigationBarMinimizationBehavior {
    _css_navigationBarMinimizationBehavior = [css_navigationBarMinimizationBehavior copy] ?: @"automatic";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyMinimization];
}

- (void)setCss_navigationBarMinimizationSafeAreaAdjustment:(NSString *)css_navigationBarMinimizationSafeAreaAdjustment {
    _css_navigationBarMinimizationSafeAreaAdjustment = [css_navigationBarMinimizationSafeAreaAdjustment copy] ?: @"automatic";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyMinimization];
}

- (void)setCss_navigationBarMinimizationRestoration:(NSString *)css_navigationBarMinimizationRestoration {
    _css_navigationBarMinimizationRestoration = [css_navigationBarMinimizationRestoration copy] ?: @"automatic";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyMinimization];
}

- (void)setCss_barBackground:(NSString *)css_barBackground {
    _css_barBackground = [css_barBackground copy] ?: @"transparent";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barBackgroundColor:(id)css_barBackgroundColor {
    _css_barBackgroundColor = css_barBackgroundColor;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barScrollEdgeBackground:(NSString *)css_barScrollEdgeBackground {
    _css_barScrollEdgeBackground = [css_barScrollEdgeBackground copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barScrollEdgeBackgroundColor:(id)css_barScrollEdgeBackgroundColor {
    _css_barScrollEdgeBackgroundColor = css_barScrollEdgeBackgroundColor;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barShadowColor:(id)css_barShadowColor {
    _css_barShadowColor = css_barShadowColor;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barStyle:(NSString *)css_barStyle {
    _css_barStyle = [css_barStyle copy] ?: @"default";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_barTranslucent:(NSString *)css_barTranslucent {
    _css_barTranslucent = [css_barTranslucent copy] ?: @"auto";
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAppearance];
}

- (void)setCss_hideScrollEdgeEffects:(id)css_hideScrollEdgeEffects {
    _css_hideScrollEdgeEffects = css_hideScrollEdgeEffects;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyHideScrollEdgeEffects];
}

- (void)setCss_titleFontFamily:(NSString *)css_titleFontFamily {
    _css_titleFontFamily = [css_titleFontFamily copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleFontWeight:(NSString *)css_titleFontWeight {
    _css_titleFontWeight = [css_titleFontWeight copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleFontSize:(NSString *)css_titleFontSize {
    _css_titleFontSize = [css_titleFontSize copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_largeTitleFontSize:(NSString *)css_largeTitleFontSize {
    _css_largeTitleFontSize = [css_largeTitleFontSize copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes];
}

- (void)setCss_titleColor:(id)css_titleColor {
    _css_titleColor = css_titleColor;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleKern:(NSString *)css_titleKern {
    _css_titleKern = [css_titleKern copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleShadowColor:(id)css_titleShadowColor {
    _css_titleShadowColor = css_titleShadowColor;
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleShadowOffsetX:(NSString *)css_titleShadowOffsetX {
    _css_titleShadowOffsetX = [css_titleShadowOffsetX copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleShadowOffsetY:(NSString *)css_titleShadowOffsetY {
    _css_titleShadowOffsetY = [css_titleShadowOffsetY copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleShadowRadius:(NSString *)css_titleShadowRadius {
    _css_titleShadowRadius = [css_titleShadowRadius copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleOffsetX:(NSString *)css_titleOffsetX {
    _css_titleOffsetX = [css_titleOffsetX copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle];
}

- (void)setCss_titleOffsetY:(NSString *)css_titleOffsetY {
    _css_titleOffsetY = [css_titleOffsetY copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle];
}

- (void)setCss_subtitle:(NSString *)css_subtitle {
    _css_subtitle = [css_subtitle copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle];
}

- (void)setCss_largeSubtitle:(NSString *)css_largeSubtitle {
    _css_largeSubtitle = [css_largeSubtitle copy];
    [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle];
}

- (void)didMoveToSuperview {
    [super didMoveToSuperview];
    if (self.superview) {
        [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAll];
    } else {
        [self p_teardownNavigationBar];
    }
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    if (self.window) {
        [self p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyAll];
        __weak typeof(self) weakSelf = self;
        [self.hr_rootView performWhenRenderViewDeallocWithTask:^{
            [weakSelf p_teardownNavigationBar];
        }];
    } else {
        [self p_teardownNavigationBar];
    }
}

- (void)removeFromSuperview {
    [self p_teardownNavigationBar];
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

- (UINavigationItemLargeTitleDisplayMode)p_largeTitleMode {
    NSString *value = self.css_largeTitle.lowercaseString ?: @"never";
    if ([value isEqualToString:@"always"]) {
        return UINavigationItemLargeTitleDisplayModeAlways;
    }
    if ([value isEqualToString:@"automatic"]) {
        return UINavigationItemLargeTitleDisplayModeAutomatic;
    }
    return UINavigationItemLargeTitleDisplayModeNever;
}

- (NSInteger)p_requestedContentScrollNativeRef {
    return self.css_contentScrollNativeRef.length ? self.css_contentScrollNativeRef.integerValue : 0;
}

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270000
- (UIBarMinimizationBehavior)p_minimizationBehavior API_AVAILABLE(ios(27.0)) {
    NSString *value = self.css_navigationBarMinimizationBehavior.lowercaseString ?: @"automatic";
    if ([value isEqualToString:@"never"]) {
        return UIBarMinimizationBehaviorNever;
    }
    if ([value isEqualToString:@"onscrolldown"]) {
        return UIBarMinimizationBehaviorOnScrollDown;
    }
    if ([value isEqualToString:@"onscrollup"]) {
        return UIBarMinimizationBehaviorOnScrollUp;
    }
    return UIBarMinimizationBehaviorAutomatic;
}

- (UIBarMinimizationSafeAreaAdjustment)p_minimizationSafeAreaAdjustment API_AVAILABLE(ios(27.0)) {
    NSString *value = self.css_navigationBarMinimizationSafeAreaAdjustment.lowercaseString ?: @"automatic";
    if ([value isEqualToString:@"enabled"]) {
        return UIBarMinimizationSafeAreaAdjustmentEnabled;
    }
    if ([value isEqualToString:@"disabled"]) {
        return UIBarMinimizationSafeAreaAdjustmentDisabled;
    }
    return UIBarMinimizationSafeAreaAdjustmentAutomatic;
}

- (UIBarMinimizationRestorationBehavior)p_minimizationRestoration API_AVAILABLE(ios(27.0)) {
    NSString *value = self.css_navigationBarMinimizationRestoration.lowercaseString ?: @"automatic";
    if ([value isEqualToString:@"atscrolledge"]) {
        return UIBarMinimizationRestorationBehaviorAtScrollEdge;
    }
    return UIBarMinimizationRestorationBehaviorAutomatic;
}

- (void)p_applyNavigationBarMinimizationToItem:(UINavigationItem *)item API_AVAILABLE(ios(27.0)) {
    UIBarMinimization *minimization = item.navigationBarMinimization;
    if (!minimization) {
        minimization = [[UIBarMinimization alloc] init];
    } else {
        minimization = [minimization copy];
    }
    minimization.minimizationBehavior = [self p_minimizationBehavior];
    minimization.safeAreaAdjustment = [self p_minimizationSafeAreaAdjustment];
    minimization.restorationBehavior = [self p_minimizationRestoration];
    item.navigationBarMinimization = minimization;
}
#endif

- (void)p_syncContentScrollViewForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    NSInteger requestedRef = [self p_requestedContentScrollNativeRef];
    UIScrollView *scrollView = KRUINavigationBarScrollViewForNativeRef(self.hr_rootView, requestedRef);
    if (@available(iOS 15.0, *)) {
        if (scrollView) {
            [viewController setContentScrollView:scrollView forEdge:NSDirectionalRectEdgeTop];
            self.boundContentScrollNativeRef = requestedRef;
        } else if (requestedRef <= 0) {
            // 未传 ref：不主动 setContentScrollView(nil)，便于对比 UIKit 是否自行关联主列表。
            // 仅在我们曾显式绑定过时再解除。
            if (self.boundContentScrollNativeRef > 0) {
                [self p_clearContentScrollViewForViewController:viewController];
            }
        } else {
            // Scroller 可能尚未创建，下一帧再试。
            __weak typeof(self) weakSelf = self;
            dispatch_async(dispatch_get_main_queue(), ^{
                __strong typeof(weakSelf) self = weakSelf;
                if (!self || !self.window) {
                    return;
                }
                UIViewController *host = [self p_hostViewController];
                if (!host) {
                    return;
                }
                NSInteger retryRef = [self p_requestedContentScrollNativeRef];
                if (retryRef != requestedRef) {
                    return;
                }
                UIScrollView *retryScroll = KRUINavigationBarScrollViewForNativeRef(self.hr_rootView, retryRef);
                if (retryScroll) {
                    [host setContentScrollView:retryScroll forEdge:NSDirectionalRectEdgeTop];
                    self.boundContentScrollNativeRef = retryRef;
                }
            });
        }
    }
}

- (void)p_clearContentScrollView {
    [self p_clearContentScrollViewForViewController:[self p_hostViewController]];
}


- (NSString *)p_normalizedBackgroundStyle:(NSString *)value fallback:(NSString *)fallback {
    NSString *style = value.length ? value.lowercaseString : fallback.lowercaseString;
    if ([style isEqualToString:@"default"] || [style isEqualToString:@"opaque"] || [style isEqualToString:@"transparent"]) {
        return style;
    }
    return fallback.lowercaseString ?: @"transparent";
}

- (NSString *)p_barBackgroundStyle {
    return [self p_normalizedBackgroundStyle:self.css_barBackground fallback:@"transparent"];
}

- (NSString *)p_barScrollEdgeBackgroundStyle {
    if (self.css_barScrollEdgeBackground.length == 0) {
        return nil;
    }
    return [self p_normalizedBackgroundStyle:self.css_barScrollEdgeBackground fallback:self.css_barBackground];
}

- (UIBarStyle)p_barStyleValue {
    NSString *value = self.css_barStyle.lowercaseString ?: @"default";
    if ([value isEqualToString:@"black"]) {
        return UIBarStyleBlack;
    }
    return UIBarStyleDefault;
}

- (BOOL)p_shouldHideScrollEdgeEffects {
    if (!self.css_hideScrollEdgeEffects) {
        return NO;
    }
    return [UIView css_bool:self.css_hideScrollEdgeEffects];
}

- (void)p_applyTranslucentForBar:(UINavigationBar *)bar standardStyle:(NSString *)style {
    NSString *mode = self.css_barTranslucent.lowercaseString ?: @"auto";
    if ([mode isEqualToString:@"yes"] || [mode isEqualToString:@"true"]) {
        bar.translucent = YES;
        return;
    }
    if ([mode isEqualToString:@"no"] || [mode isEqualToString:@"false"]) {
        bar.translucent = NO;
        return;
    }
    bar.translucent = ![style isEqualToString:@"opaque"];
}

- (void)p_configureAppearance:(UINavigationBarAppearance *)appearance
                         style:(NSString *)style
              backgroundColor:(id)backgroundColorOverride
               titleAttributes:(NSDictionary *)titleAttributes
          largeTitleAttributes:(NSDictionary *)largeTitleAttributes
            subtitleAttributes:(NSDictionary *)subtitleAttributes
        largeSubtitleAttributes:(NSDictionary *)largeSubtitleAttributes
                        offset:(UIOffset)offset {
    if ([style isEqualToString:@"default"]) {
        [appearance configureWithDefaultBackground];
    } else if ([style isEqualToString:@"opaque"]) {
        [appearance configureWithOpaqueBackground];
    } else {
        [appearance configureWithTransparentBackground];
        appearance.backgroundColor = [UIColor clearColor];
        appearance.backgroundEffect = nil;
        appearance.shadowColor = [UIColor clearColor];
    }
    if (backgroundColorOverride) {
        UIColor *overrideColor = [UIView css_color:backgroundColorOverride];
        if (overrideColor) {
            appearance.backgroundColor = overrideColor;
        }
    }
    if (self.css_barShadowColor && ![style isEqualToString:@"transparent"]) {
        UIColor *shadow = [UIView css_color:self.css_barShadowColor];
        if (shadow) {
            appearance.shadowColor = shadow;
        }
    }
    appearance.titleTextAttributes = titleAttributes;
    appearance.largeTitleTextAttributes = largeTitleAttributes ?: titleAttributes;
    appearance.titlePositionAdjustment = offset;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        appearance.subtitleTextAttributes = subtitleAttributes ?: @{};
        appearance.largeSubtitleTextAttributes = largeSubtitleAttributes ?: subtitleAttributes ?: @{};
    }
#endif
}

- (UINavigationBarAppearance *)p_buildAppearanceWithStyle:(NSString *)style
                                      backgroundColor:(id)backgroundColorOverride
                                       titleAttributes:(NSDictionary *)titleAttributes
                                  largeTitleAttributes:(NSDictionary *)largeTitleAttributes
                                    subtitleAttributes:(NSDictionary *)subtitleAttributes
                                largeSubtitleAttributes:(NSDictionary *)largeSubtitleAttributes
                                                 offset:(UIOffset)offset API_AVAILABLE(ios(13.0)) {
    UINavigationBarAppearance *appearance = [[UINavigationBarAppearance alloc] init];
    [self p_configureAppearance:appearance
                          style:style
                backgroundColor:backgroundColorOverride
                titleAttributes:titleAttributes
           largeTitleAttributes:largeTitleAttributes
             subtitleAttributes:subtitleAttributes
         largeSubtitleAttributes:largeSubtitleAttributes
                          offset:offset];
    return appearance;
}

- (void)p_captureNavigationBarBaselineIfNeeded:(UINavigationBar *)bar {
    if (self.capturedBarBaseline || !bar) {
        return;
    }
    self.baselineBarTintColor = bar.tintColor;
    self.baselinePrefersLargeTitles = bar.prefersLargeTitles;
    self.baselineBarStyle = bar.barStyle;
    self.baselineBarTranslucent = bar.isTranslucent;
    self.capturedBarBaseline = YES;
}

- (void)p_restoreNavigationBarBaselineIfNeeded:(UINavigationBar *)bar {
    if (!bar || !self.capturedBarBaseline) {
        return;
    }
    bar.tintColor = self.baselineBarTintColor;
    bar.prefersLargeTitles = self.baselinePrefersLargeTitles;
    bar.barStyle = self.baselineBarStyle;
    bar.translucent = self.baselineBarTranslucent;
    self.capturedBarBaseline = NO;
    self.baselineBarTintColor = nil;
}

- (BOOL)p_colorsEqual:(UIColor *)lhs rhs:(UIColor *)rhs {
    if (lhs == rhs) {
        return YES;
    }
    if (!lhs || !rhs) {
        return NO;
    }
    return CGColorEqualToColor(lhs.CGColor, rhs.CGColor);
}

- (UINavigationBarAppearance *)p_patchedAppearanceFrom:(UINavigationBarAppearance *)appearance
                                      titleAttributes:(NSDictionary *)titleAttributes
                                 largeTitleAttributes:(NSDictionary *)largeTitleAttributes
                                   subtitleAttributes:(NSDictionary *)subtitleAttributes
                               largeSubtitleAttributes:(NSDictionary *)largeSubtitleAttributes
                                                offset:(UIOffset)offset API_AVAILABLE(ios(13.0)) {
    UINavigationBarAppearance *next = appearance ? [appearance copy] : [[UINavigationBarAppearance alloc] init];
    if (!appearance) {
        [next configureWithDefaultBackground];
    }
    next.titleTextAttributes = titleAttributes;
    next.largeTitleTextAttributes = largeTitleAttributes ?: titleAttributes;
    next.titlePositionAdjustment = offset;
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        next.subtitleTextAttributes = subtitleAttributes ?: @{};
        next.largeSubtitleTextAttributes = largeSubtitleAttributes ?: subtitleAttributes ?: @{};
    }
#endif
    return next;
}

- (void)p_applyTitleTextAttributesToNavigationItem:(UINavigationItem *)navigationItem
                                               bar:(UINavigationBar *)bar
                                       customTitle:(BOOL)customTitle
                                   titleAttributes:(NSDictionary *)titleAttributes
                              largeTitleAttributes:(NSDictionary *)largeTitleAttributes
                                subtitleAttributes:(NSDictionary *)subtitleAttributes
                            largeSubtitleAttributes:(NSDictionary *)largeSubtitleAttributes
                                             offset:(UIOffset)offset {
    if (!navigationItem || customTitle) {
        return;
    }
    if (@available(iOS 13.0, *)) {
        if (!navigationItem.standardAppearance) {
            [self p_applyBarAppearanceToItem:navigationItem
                                           bar:bar
                                titleAttributes:titleAttributes
                           largeTitleAttributes:largeTitleAttributes
                             subtitleAttributes:subtitleAttributes
                         largeSubtitleAttributes:largeSubtitleAttributes
                                          offset:offset];
            return;
        }
        navigationItem.standardAppearance =
            [self p_patchedAppearanceFrom:navigationItem.standardAppearance
                            titleAttributes:titleAttributes
                       largeTitleAttributes:largeTitleAttributes
                         subtitleAttributes:subtitleAttributes
                     largeSubtitleAttributes:largeSubtitleAttributes
                                      offset:offset];
        navigationItem.compactAppearance =
            [self p_patchedAppearanceFrom:navigationItem.compactAppearance
                            titleAttributes:titleAttributes
                       largeTitleAttributes:largeTitleAttributes
                         subtitleAttributes:subtitleAttributes
                     largeSubtitleAttributes:largeSubtitleAttributes
                                      offset:offset];
        UINavigationBarAppearance *scrollEdge =
            [self p_patchedAppearanceFrom:navigationItem.scrollEdgeAppearance
                            titleAttributes:titleAttributes
                       largeTitleAttributes:largeTitleAttributes
                         subtitleAttributes:subtitleAttributes
                     largeSubtitleAttributes:largeSubtitleAttributes
                                      offset:offset];
        navigationItem.scrollEdgeAppearance = scrollEdge;
        if (@available(iOS 15.0, *)) {
            navigationItem.compactScrollEdgeAppearance =
                [self p_patchedAppearanceFrom:navigationItem.compactScrollEdgeAppearance
                                titleAttributes:titleAttributes
                           largeTitleAttributes:largeTitleAttributes
                             subtitleAttributes:subtitleAttributes
                         largeSubtitleAttributes:largeSubtitleAttributes
                                          offset:offset];
        }
    }
}

- (void)p_applyBarAppearanceToItem:(UINavigationItem *)navigationItem
                               bar:(UINavigationBar *)bar
                    titleAttributes:(NSDictionary *)titleAttributes
               largeTitleAttributes:(NSDictionary *)largeTitleAttributes
                 subtitleAttributes:(NSDictionary *)subtitleAttributes
             largeSubtitleAttributes:(NSDictionary *)largeSubtitleAttributes
                              offset:(UIOffset)offset {
    if (!navigationItem) {
        return;
    }
    if (@available(iOS 13.0, *)) {
        NSString *standardStyle = [self p_barBackgroundStyle];
        NSString *scrollStyle = [self p_barScrollEdgeBackgroundStyle];
        BOOL splitScrollEdge = scrollStyle.length > 0 && ![scrollStyle isEqualToString:standardStyle];
        UINavigationBarAppearance *standard = [self p_buildAppearanceWithStyle:standardStyle
                                                               backgroundColor:self.css_barBackgroundColor
                                                                titleAttributes:titleAttributes
                                                           largeTitleAttributes:largeTitleAttributes
                                                             subtitleAttributes:subtitleAttributes
                                                         largeSubtitleAttributes:largeSubtitleAttributes
                                                                          offset:offset];
        navigationItem.standardAppearance = standard;
        navigationItem.compactAppearance = standard;
        if (splitScrollEdge) {
            UINavigationBarAppearance *scrollEdge = [self p_buildAppearanceWithStyle:scrollStyle
                                                                     backgroundColor:self.css_barScrollEdgeBackgroundColor ?: self.css_barBackgroundColor
                                                                      titleAttributes:titleAttributes
                                                                 largeTitleAttributes:largeTitleAttributes
                                                                   subtitleAttributes:subtitleAttributes
                                                               largeSubtitleAttributes:largeSubtitleAttributes
                                                                                offset:offset];
            navigationItem.scrollEdgeAppearance = scrollEdge;
            if (@available(iOS 15.0, *)) {
                navigationItem.compactScrollEdgeAppearance = scrollEdge;
            }
        } else {
            navigationItem.scrollEdgeAppearance = standard;
            if (@available(iOS 15.0, *)) {
                navigationItem.compactScrollEdgeAppearance = standard;
            }
        }
        if (bar) {
            bar.barStyle = [self p_barStyleValue];
            [self p_applyTranslucentForBar:bar standardStyle:standardStyle];
        }
    }
}
- (UIColor *)p_titleColorForTitle:(NSString *)title tint:(UIColor *)tint {
    if (self.css_titleColor) {
        UIColor *color = [UIView css_color:self.css_titleColor];
        if (color) {
            return color;
        }
    }
    if (title.length > 0) {
        if (@available(iOS 13.0, *)) {
            return tint ?: [UIColor labelColor];
        }
        return tint ?: [UIColor blackColor];
    }
    return [UIColor clearColor];
}

- (UIFont *)p_fontForSizeString:(NSString *)sizeString defaultSize:(CGFloat)defaultSize {
    BOOL hasSize = sizeString.length > 0 && sizeString.doubleValue > 0;
    BOOL hasWeight = self.css_titleFontWeight.length > 0;
    BOOL hasFamily = self.css_titleFontFamily.length > 0;
    if (!hasSize && !hasWeight && !hasFamily) {
        return nil;
    }
    CGFloat size = hasSize ? sizeString.doubleValue : defaultSize;
    return [KRConvertUtil UIFont:@{
        @"fontSize": @(size),
        @"fontWeight": KRNormalizedFontWeight(self.css_titleFontWeight),
        @"fontFamily": self.css_titleFontFamily ?: @"",
    }];
}

- (UIFont *)p_subtitleFont {
    BOOL hasWeight = self.css_titleFontWeight.length > 0;
    BOOL hasFamily = self.css_titleFontFamily.length > 0;
    if (!hasWeight && !hasFamily) {
        return nil;
    }
    return [KRConvertUtil UIFont:@{
        @"fontSize": @(13),
        @"fontWeight": KRNormalizedFontWeight(self.css_titleFontWeight),
        @"fontFamily": self.css_titleFontFamily ?: @"",
    }];
}

- (NSShadow *)p_titleShadow {
    if (!self.css_titleShadowColor) {
        return nil;
    }
    UIColor *color = [UIView css_color:self.css_titleShadowColor];
    if (!color) {
        return nil;
    }
    NSShadow *shadow = [[NSShadow alloc] init];
    shadow.shadowColor = color;
    shadow.shadowOffset = CGSizeMake(self.css_titleShadowOffsetX.doubleValue, self.css_titleShadowOffsetY.doubleValue);
    shadow.shadowBlurRadius = self.css_titleShadowRadius.doubleValue;
    return shadow;
}

- (NSDictionary *)p_textAttributesWithFont:(UIFont *)font color:(UIColor *)color {
    NSMutableDictionary *attributes = [NSMutableDictionary dictionary];
    if (font) {
        attributes[NSFontAttributeName] = font;
    }
    if (color) {
        attributes[NSForegroundColorAttributeName] = color;
    }
    if (self.css_titleKern.length) {
        attributes[NSKernAttributeName] = @(self.css_titleKern.doubleValue);
    }
    NSShadow *shadow = [self p_titleShadow];
    if (shadow) {
        attributes[NSShadowAttributeName] = shadow;
    }
    return attributes;
}

- (CGFloat)p_titleHostWidth:(UINavigationBar *)bar {
    if (bar.bounds.size.width > 1) {
        return bar.bounds.size.width;
    }
    UIView *root = self.hr_rootView;
    if (root.bounds.size.width > 1) {
        return root.bounds.size.width;
    }
    return UIScreen.mainScreen.bounds.size.width;
}

- (CGFloat)p_titleTrailingLimit:(UINavigationBar *)bar {
    UIViewController *viewController = [self p_hostViewController];
    CGFloat trailing = 0;
    if (bar.safeAreaInsets.right > 1) {
        trailing = bar.safeAreaInsets.right;
    } else if (viewController.view.safeAreaInsets.right > 1) {
        trailing = viewController.view.safeAreaInsets.right;
    }
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270100
    if (@available(iOS 27.1, *)) {
        // 竖栏上的时间和 Wi-Fi 盖在屏幕右边。安全区不够时，标题再让开这一列。
        if (viewController.traitCollection.verticalBarEdge != UIVerticalBarEdgeUnspecified && trailing < 64) {
            trailing = 88;
        }
    }
#endif
    return trailing + 12;
}

- (CGFloat)p_offsetTitleXForWidth:(CGFloat)width
                        labelWidth:(CGFloat)labelWidth
                            offset:(UIOffset)offset
                               bar:(UINavigationBar *)bar {
    // 横屏时系统标题贴在左边。偏移从左边起算，半个屏幕落在中间，而不是整屏右边缘。
    CGFloat x = offset.horizontal;
    CGFloat maxX = width - labelWidth - [self p_titleTrailingLimit:bar];
    if (maxX < 0) {
        maxX = 0;
    }
    if (x > maxX) {
        x = maxX;
    }
    if (x < 0) {
        x = 0;
    }
    return x;
}

- (UIView *)p_offsetTitleViewForTitle:(NSString *)title
                            attributes:(NSDictionary *)attributes
                                offset:(UIOffset)offset
                                   bar:(UINavigationBar *)bar {
    NSMutableDictionary *textAttributes = [NSMutableDictionary dictionaryWithDictionary:attributes ?: @{}];
    if (!textAttributes[NSFontAttributeName]) {
        textAttributes[NSFontAttributeName] = [UIFont systemFontOfSize:17 weight:UIFontWeightSemibold];
    }
    UILabel *label = [[UILabel alloc] init];
    label.attributedText = [[NSAttributedString alloc] initWithString:title attributes:textAttributes];
    label.backgroundColor = [UIColor clearColor];
    [label sizeToFit];
    CGFloat labelWidth = ceil(label.bounds.size.width);
    CGFloat labelHeight = ceil(label.bounds.size.height);
    label.frame = CGRectMake(0, 0, labelWidth, labelHeight);

    // 只占文字大小，导航栏才不会为了标题去挪返回键。位置用 transform 单独移。
    UIView *container = [[UIView alloc] initWithFrame:label.frame];
    container.userInteractionEnabled = NO;
    container.backgroundColor = [UIColor clearColor];
    container.accessibilityIdentifier = kKRUINavigationBarTitleId;
    container.clipsToBounds = NO;
    [container addSubview:label];
    container.transform = [self p_offsetTitleTransformForWidth:[self p_titleHostWidth:bar]
                                                     labelWidth:labelWidth
                                                         offset:offset
                                                            bar:bar];
    return container;
}

- (CGAffineTransform)p_offsetTitleTransformForWidth:(CGFloat)width
                                         labelWidth:(CGFloat)labelWidth
                                             offset:(UIOffset)offset
                                                bar:(UINavigationBar *)bar {
    CGFloat x = [self p_offsetTitleXForWidth:width labelWidth:labelWidth offset:offset bar:bar];
    CGFloat tx = (x + labelWidth / 2.0) - width / 2.0;
    return CGAffineTransformMakeTranslation(tx, offset.vertical);
}

- (void)p_hideScrollEdgeEffectsInView:(UIView *)view {
    if (!view || ![self p_shouldHideScrollEdgeEffects]) {
        return;
    }
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        if ([view isKindOfClass:[UIScrollView class]]) {
            ((UIScrollView *)view).topEdgeEffect.hidden = YES;
        }
        for (UIView *subview in view.subviews) {
            [self p_hideScrollEdgeEffectsInView:subview];
        }
    }
#endif
}

- (void)p_clearContentScrollViewForViewController:(UIViewController *)viewController {
    if (!viewController) {
        return;
    }
    if (@available(iOS 15.0, *)) {
        [viewController setContentScrollView:nil forEdge:NSDirectionalRectEdgeTop];
    }
    self.boundContentScrollNativeRef = 0;
}

/// RenderView 销毁 / 离窗时撤销本组件写入的导航栏状态（自定义 titleView 由 TitleHost 单独 detach）。
- (void)p_teardownNavigationBar {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    [self p_clearContentScrollViewForViewController:viewController];
    [KRUINavigationBarItemPlacement setLeadingInset:0
                                         trailingInset:0
                                        viewController:viewController];
    UINavigationItem *navItem = viewController.navigationItem;
    if (!KRUINavigationBarItemUsesCustomTitle(navItem)) {
        if (navItem.titleView.accessibilityIdentifier.length == 0
            || CGRectGetWidth(navItem.titleView.bounds) <= 1.0) {
            navItem.titleView = nil;
        }
    }
    navItem.hidesBackButton = NO;
    if (@available(iOS 14.0, *)) {
        navItem.backButtonDisplayMode = UINavigationItemBackButtonDisplayModeDefault;
    }
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270000
    if (@available(iOS 27.0, *)) {
        navItem.navigationBarMinimization = [[UIBarMinimization alloc] init];
    }
#endif
    if (@available(iOS 13.0, *)) {
        navItem.standardAppearance = nil;
        navItem.compactAppearance = nil;
        if (@available(iOS 15.0, *)) {
            navItem.scrollEdgeAppearance = nil;
            navItem.compactScrollEdgeAppearance = nil;
        }
    }
    self.appliedContentScrollRequestRef = NSIntegerMin;
    self.hasAppliedNavigationBarHidden = NO;
    self.hasAppliedLargeTitleDisplayMode = NO;
    self.appliedHideScrollEdgeEffectsScan = NO;
    self.pendingDirtyFlags = 0;
    self.hasAppliedBackButtonHiding = NO;
    navItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeAutomatic;
    UINavigationController *navigationController = viewController.navigationController;
    if (navigationController) {
        [self p_restoreNavigationBarBaselineIfNeeded:navigationController.navigationBar];
        [navigationController setNavigationBarHidden:NO animated:NO];
    }
}

- (void)p_applyTitleMetadataToItem:(UINavigationItem *)item
                    viewController:(UIViewController *)viewController
                       customTitle:(BOOL)customTitle
                             title:(NSString *)title
                     showLargeTitle:(BOOL)showLargeTitle
                       hasSubtitle:(BOOL)hasSubtitle
                        shiftTitle:(BOOL)shiftTitle
                            offset:(UIOffset)offset {
    if (!customTitle) {
        viewController.title = title;
        item.title = title;
        if (title.length == 0 && !showLargeTitle && !hasSubtitle) {
            UIView *titleView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 1, 56)];
            titleView.userInteractionEnabled = NO;
            item.titleView = titleView;
        } else if (shiftTitle && title.length > 0 && !showLargeTitle) {
            UINavigationBar *bar = viewController.navigationController.navigationBar;
            UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
            UIColor *titleColor = [self p_titleColorForTitle:title tint:tint];
            NSDictionary *titleAttributes = [self p_textAttributesWithFont:[self p_fontForSizeString:self.css_titleFontSize defaultSize:17]
                                                                     color:titleColor];
            item.titleView = [self p_offsetTitleViewForTitle:title attributes:titleAttributes offset:offset bar:bar];
            item.title = @"";
            viewController.title = @"";
        } else {
            item.titleView = nil;
        }
    } else {
        item.title = @"";
        viewController.title = @"";
    }
#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 260000
    if (@available(iOS 26.0, *)) {
        BOOL customOffsetTitle = !customTitle && shiftTitle && title.length > 0 && !showLargeTitle;
        item.largeTitle = customOffsetTitle ? @"" : (title.length ? title : @"");
        item.subtitle = self.css_subtitle.length ? self.css_subtitle : nil;
        item.largeSubtitle = self.css_largeSubtitle.length ? self.css_largeSubtitle : nil;
    }
#endif
}

- (BOOL)p_canApplyToHostedNavigationBar {
    return [self p_hostViewController] != nil && self.window != nil;
}

/// 与 [KRUIBarButtonItem] 一致：宿主已就绪则立即 apply，否则记入 dirty 等 flush。
- (void)p_setNavigationBarDirtyOrApply:(KRUINavigationBarDirty)flags {
    if (flags == 0) {
        return;
    }
    if ([self p_canApplyToHostedNavigationBar]) {
        [self p_applyNavigationBarDirty:flags];
        return;
    }
    self.pendingDirtyFlags |= flags;
    __weak typeof(self) weakSelf = self;
    KRUINavigationBarScheduleCoalescedMainFlush(self, &_syncFlushScheduled, ^{
        [weakSelf p_flushDirtyNavigationBar];
    });
}

- (void)p_markNavigationBarDirty:(KRUINavigationBarDirty)flags {
    if (flags == 0) {
        return;
    }
    self.pendingDirtyFlags |= flags;
    __weak typeof(self) weakSelf = self;
    KRUINavigationBarScheduleCoalescedMainFlush(self, &_syncFlushScheduled, ^{
        [weakSelf p_flushDirtyNavigationBar];
    });
}

- (void)p_flushDirtyNavigationBar {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController) {
        return;
    }
    KRUINavigationBarDirty dirty = self.pendingDirtyFlags;
    if (dirty == 0) {
        return;
    }
    self.pendingDirtyFlags = 0;
    [self p_applyNavigationBarDirty:dirty];
}

- (void)p_applyNavigationBarDirty:(KRUINavigationBarDirty)dirty {
    UIViewController *viewController = [self p_hostViewController];
    if (!viewController || dirty == 0) {
        return;
    }

    UINavigationItem *item = viewController.navigationItem;
    BOOL customTitle = KRUINavigationBarItemUsesCustomTitle(item);
    if (customTitle && item.titleView == nil) {
        KRUINavigationBarItemSetUsesCustomTitle(item, NO);
        customTitle = NO;
    }
    NSString *title = self.css_title.length ? self.css_title : @"";
    UINavigationItemLargeTitleDisplayMode largeMode = [self p_largeTitleMode];
    BOOL showLargeTitle = largeMode != UINavigationItemLargeTitleDisplayModeNever;
    BOOL hasSubtitle = self.css_subtitle.length > 0 || self.css_largeSubtitle.length > 0;
    UIOffset offset = UIOffsetMake(self.css_titleOffsetX.doubleValue, self.css_titleOffsetY.doubleValue);
    BOOL shiftTitle = fabs(offset.horizontal) > 0.5 || fabs(offset.vertical) > 0.5;

    if (dirty & KRUINavigationBarDirtyInset) {
        [KRUINavigationBarItemPlacement setLeadingInset:MAX(0, self.css_leadingInset.doubleValue)
                                              trailingInset:MAX(0, self.css_trailingInset.doubleValue)
                                             viewController:viewController];
    }

    if (dirty & KRUINavigationBarDirtyTitle) {
        [self p_applyTitleMetadataToItem:item
                          viewController:viewController
                             customTitle:customTitle
                                   title:title
                           showLargeTitle:showLargeTitle
                             hasSubtitle:hasSubtitle
                              shiftTitle:shiftTitle
                                  offset:offset];
    }

    if (dirty & KRUINavigationBarDirtyBackButton) {
        if (!self.hasAppliedBackButtonHiding) {
            item.hidesBackButton = YES;
            if (@available(iOS 14.0, *)) {
                item.backButtonDisplayMode = UINavigationItemBackButtonDisplayModeMinimal;
            }
            self.hasAppliedBackButtonHiding = YES;
        }
    }

    if (dirty & KRUINavigationBarDirtyLargeTitleMode) {
        if (!self.hasAppliedLargeTitleDisplayMode || self.appliedLargeTitleDisplayMode != (NSInteger)largeMode) {
            item.largeTitleDisplayMode = largeMode;
            self.appliedLargeTitleDisplayMode = (NSInteger)largeMode;
            self.hasAppliedLargeTitleDisplayMode = YES;
        }
    }

#if __IPHONE_OS_VERSION_MAX_ALLOWED >= 270000
    if (dirty & KRUINavigationBarDirtyMinimization) {
        if (@available(iOS 27.0, *)) {
            [self p_applyNavigationBarMinimizationToItem:item];
        }
    }
#endif

    UINavigationBar *bar = viewController.navigationController.navigationBar;
    KRUINavigationBarDirty barVisualDirty = dirty & (KRUINavigationBarDirtyTintAndLargeTitles
                                                         | KRUINavigationBarDirtyTitleTextAttributes
                                                         | KRUINavigationBarDirtyAppearance);
    if (bar && barVisualDirty) {
        UIColor *tint = self.css_tintColor ? [UIView css_color:self.css_tintColor] : nil;
        UIColor *titleColor = [self p_titleColorForTitle:customTitle ? @"" : title tint:tint];
        UIColor *subtitleColor = titleColor;
        if (!self.css_titleColor) {
            if (@available(iOS 13.0, *)) {
                subtitleColor = [UIColor secondaryLabelColor];
            } else {
                subtitleColor = [UIColor grayColor];
            }
        }
        NSDictionary *titleAttributes = nil;
        NSDictionary *largeAttributes = nil;
        NSDictionary *subtitleAttributes = nil;
        UIOffset appearanceOffset = UIOffsetZero;
        if (dirty & (KRUINavigationBarDirtyTitleTextAttributes | KRUINavigationBarDirtyAppearance)) {
            titleAttributes = [self p_textAttributesWithFont:[self p_fontForSizeString:self.css_titleFontSize defaultSize:17]
                                                       color:titleColor];
            largeAttributes = [self p_textAttributesWithFont:[self p_fontForSizeString:self.css_largeTitleFontSize defaultSize:34]
                                                       color:titleColor];
            subtitleAttributes = [self p_textAttributesWithFont:[self p_subtitleFont] color:subtitleColor];
            appearanceOffset = (shiftTitle && title.length > 0 && !showLargeTitle && !customTitle) ? UIOffsetZero : offset;
        }

        if (dirty & KRUINavigationBarDirtyTintAndLargeTitles) {
            [self p_captureNavigationBarBaselineIfNeeded:bar];
            if (tint && ![self p_colorsEqual:bar.tintColor rhs:tint]) {
                bar.tintColor = tint;
            }
            if (bar.prefersLargeTitles != showLargeTitle) {
                bar.prefersLargeTitles = showLargeTitle;
            }
        }

        if (dirty & KRUINavigationBarDirtyTitleTextAttributes) {
            [self p_applyTitleTextAttributesToNavigationItem:item
                                                         bar:bar
                                                 customTitle:customTitle
                                             titleAttributes:titleAttributes
                                        largeTitleAttributes:largeAttributes
                                          subtitleAttributes:subtitleAttributes
                                      largeSubtitleAttributes:subtitleAttributes
                                                       offset:appearanceOffset];
        }

        if (dirty & KRUINavigationBarDirtyAppearance) {
            [self p_applyBarAppearanceToItem:item
                                         bar:bar
                              titleAttributes:titleAttributes
                         largeTitleAttributes:largeAttributes
                           subtitleAttributes:subtitleAttributes
                       largeSubtitleAttributes:subtitleAttributes
                                        offset:appearanceOffset];
        }
    }

    if (dirty & KRUINavigationBarDirtyHideScrollEdgeEffects) {
        if ([self p_shouldHideScrollEdgeEffects]) {
            if (!self.appliedHideScrollEdgeEffectsScan) {
                [self p_hideScrollEdgeEffectsInView:self.hr_rootView ?: viewController.view];
                self.appliedHideScrollEdgeEffectsScan = YES;
            }
        } else {
            self.appliedHideScrollEdgeEffectsScan = NO;
        }
    }

    if (dirty & KRUINavigationBarDirtyContentScroll) {
        NSInteger requestedScrollRef = [self p_requestedContentScrollNativeRef];
        if (requestedScrollRef != self.appliedContentScrollRequestRef) {
            [self p_syncContentScrollViewForViewController:viewController];
            self.appliedContentScrollRequestRef = requestedScrollRef;
        }
    }

    if (dirty & KRUINavigationBarDirtyNavigationBarHidden) {
        BOOL hidden = ![self p_shouldShow];
        if (!self.hasAppliedNavigationBarHidden || self.appliedNavigationBarHidden != hidden) {
            [viewController.navigationController setNavigationBarHidden:hidden animated:NO];
            self.appliedNavigationBarHidden = hidden;
            self.hasAppliedNavigationBarHidden = YES;
        }
    }
}

void KRUINavigationBarRequestBarTitleRefresh(id hr_rootView) {
    if (!hr_rootView || ![hr_rootView isKindOfClass:[UIView class]]) {
        return;
    }
    NSMutableArray<UIView *> *stack = [NSMutableArray arrayWithObject:(UIView *)hr_rootView];
    while (stack.count > 0) {
        UIView *view = stack.lastObject;
        [stack removeLastObject];
        if ([view isKindOfClass:[KRUINavigationBar class]]) {
            KRUINavigationBar *bar = (KRUINavigationBar *)view;
            [bar p_setNavigationBarDirtyOrApply:KRUINavigationBarDirtyTitle | KRUINavigationBarDirtyTitleTextAttributes];
            return;
        }
        [stack addObjectsFromArray:view.subviews];
    }
}

@end
#endif // [macOS]
