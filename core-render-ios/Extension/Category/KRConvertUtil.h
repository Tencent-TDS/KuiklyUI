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
#import <UIKit/UIKit.h>


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

+ (UIViewAnimationOptions)hr_viewAnimationOptions:(NSString *)value;
+ (UIViewAnimationCurve)hr_viewAnimationCurve:(NSString *)value;
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

@end

NS_ASSUME_NONNULL_END
