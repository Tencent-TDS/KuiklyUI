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

#import <Foundation/Foundation.h>
#import "KRUIKit.h" // [macOS]
#if TARGET_OS_OSX
#import <QuartzCore/QuartzCore.h> // CAGradientLayer
#endif
@class CAGradientLayer; // forward decl to satisfy header


NS_ASSUME_NONNULL_BEGIN

/// 渐变方向值
typedef NS_ENUM(NSInteger, CSSGradientDirection) {
    CSSGradientDirectionUnknown = -1,
    CSSGradientDirectionToTop = 0, // to top is 0deg
    CSSGradientDirectionToBottom = 1, // to bottom is 180deg
    CSSGradientDirectionToLeft = 2,  // to left is 270deg
    CSSGradientDirectionToRight = 3,  // to right is 90deg
    CSSGradientDirectionToTopLeft = 4,
    CSSGradientDirectionToTopRight = 5,
    CSSGradientDirectionToBottomLeft = 6,
    CSSGradientDirectionToBottomRight = 7,
};

typedef NS_ENUM(NSInteger, KRBorderStyle) {
    KRBorderStyleUnset = 0,
    KRBorderStyleSolid,
    KRBorderStyleDotted,
    KRBorderStyleDashed,
};

typedef NS_ENUM(NSInteger, KRTextDecorationLineType) {
    KRTextDecorationLineTypeNone = 0,
    KRTextDecorationLineTypeUnderline,
    KRTextDecorationLineTypeStrikethrough,
    KRTextDecorationLineTypeUnderlineStrikethrough,
};


@interface KRConvertUtil : NSObject

+ (CGFloat)CGFloat:(id)value;
+ (NSUInteger)NSUInteger:(id)value;
+ (NSInteger)NSInteger:(id)value;
+ (UIFont *)UIFont:(id)json;
+ (UIColor *)UIColor:(id)json;
+ (UIUserInterfaceStyle)KRUserInterfaceStyle:(NSString *)style API_AVAILABLE(ios(12.0));
#if (__IPHONE_OS_VERSION_MAX_ALLOWED >= 260000) || (__MAC_OS_X_VERSION_MAX_ALLOWED >= 260000)
+ (UIGlassEffectStyle)KRGlassEffectStyle:(nullable NSString *)style API_AVAILABLE(ios(26.0), macos(26.0));
#endif
+ (KRBorderStyle)KRBorderStyle:(NSString *)stringValue;
+ (NSTextAlignment)NSTextAlignment:(NSString *)stringValue;
+ (KRTextDecorationLineType)KRTextDecorationLineType:(NSString *)value;
+ (NSLineBreakMode)NSLineBreakMode:(NSString *)stringValue;
+ (UIViewContentMode)UIViewContentMode:(NSString *)stringValue ;


+ (void)hr_setStartPointAndEndPointWithLayer:(CAGradientLayer *)layer direction:(CSSGradientDirection)direction;

+ (UIBezierPath *)hr_bezierPathWithRoundedRect:(CGRect)rect
                           topLeftCornerRadius:(CGFloat)topLeftCornerRadius
                           topRightCornerRadius:(CGFloat)topRightCornerRadius
                           bottomLeftCornerRadius:(CGFloat)bottomLeftCornerRadius
                       bottomRightCornerRadius:(CGFloat)bottomRightCornerRadius;

+ (NSArray *)hr_arrayWithJSONString:(NSString *)JSONString;

+ (UIViewAnimationOptions)hr_viewAnimationOptions:(NSString *)value rawCurve:(nullable NSString *)rawCurve;
+ (UIViewAnimationCurve)hr_viewAnimationCurve:(NSString *)value rawCurve:(nullable NSString *)rawCurve;
+ (UIKeyboardType)hr_keyBoardType:(id)value ;
+ (UIReturnKeyType)hr_toReturnKeyType:(id)value ;
+ (NSString *)hr_base64Decode:(NSString *)string;
+ (CGRect)hr_rectInset:(CGRect)rect insets:(UIEdgeInsets)inset;
+ (NSString *)hr_dictionaryToJSON:(NSDictionary *)dict;
+ (void)hr_alertWithTitle:(NSString *)title message:(NSString *)message;
+ (NSString *)hr_md5StringWithString:(NSString *)string;
+ (CGFloat)statusBarHeight;
+ (NSString *)stringWithInsets:(UIEdgeInsets)insets;
+ (UIEdgeInsets)currentSafeAreaInsets;
+ (CGFloat)toSafeFloat:(CGFloat)value;
+ (CGRect)toSafeRect:(CGRect)rect;
+ (NSString *)sizeStrWithSize:(CGSize)size;
+ (UIAccessibilityTraits)kr_accessibilityTraits:(id)value;
+ (BOOL)hr_isJsonArray:(id)value;
+ (id)nativeObjectToKotlinObject:(id)ocObject;
+ (UIBezierPath *)hr_parseClipPath:(NSString *)pathData density:(CGFloat)density;

#if TARGET_OS_OSX

/// 手动转换 BezierPath 为CGPath 作为  macOS 14.0 之下ClipPath的实现
+ (CGPathRef)hr_convertNSBezierPathToCGPath:(NSBezierPath *)bezierPath;

/// 在 CGContext 中绘制线性渐变
/// @param ctx 目标绘制上下文
/// @param gradientStr 渐变字符串（如 "linear-gradient(180,#ffffff00 0,#ffffffff 1)"）
/// @param size 绘制区域大小（point 单位）
+ (void)hr_drawLinearGradientInContext:(CGContextRef)ctx withGradientStr:(NSString *)gradientStr size:(CGSize)size;

/// 计算渐变的起点和终点（归一化坐标 0-1）
/// @param startPoint 输出参数，渐变起点
/// @param endPoint 输出参数，渐变终点
/// @param direction 渐变方向
/// @param size 绘制区域大小
+ (void)hr_calculateGradientStartPoint:(CGPoint *)startPoint endPoint:(CGPoint *)endPoint direction:(CSSGradientDirection)direction size:(CGSize)size;

#endif

@end

NS_ASSUME_NONNULL_END
