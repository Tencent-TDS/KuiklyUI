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

#import "KRConvertUtil.h"
#import <JavaScriptCore/JavaScriptCore.h>
#import <CoreText/CoreText.h>
#import <CommonCrypto/CommonDigest.h>
#import "NSObject+KR.h"
#import <CommonCrypto/CommonCrypto.h>
#import "KRLogModule.h"
#import "KuiklyRenderBridge.h"

#define hr_tan(deg)   tan(((deg)/360.f) * (2 * M_PI))



@implementation KRConvertUtil


+ (UIFont *)UIFont:(id)json {
    NSString *fontFamily = json[@"fontFamily"];
    CGFloat fontSize = [self CGFloat:json[@"fontSize"]] ?: 15;
    static dispatch_once_t onceToken;
    static NSDictionary *gFontWeightMap = nil;
    dispatch_once(&onceToken, ^{
        gFontWeightMap =  @{
            @"normal": @(UIFontWeightRegular),
            @"bold": @(UIFontWeightBold),
            @"100": @(UIFontWeightUltraLight),
            @"200": @(UIFontWeightThin),
            @"300": @(UIFontWeightLight),
            @"400": @(UIFontWeightRegular),
            @"500": @(UIFontWeightMedium),
            @"600": @(UIFontWeightSemibold),
            @"700": @(UIFontWeightBold),
            @"800": @(UIFontWeightHeavy),
            @"900": @(UIFontWeightBlack),
        };
    });
    UIFontWeight fontWeight = [(gFontWeightMap[json[@"fontWeight"]?:@""] ?: @(UIFontWeightRegular)) doubleValue];
    
    if (fontFamily.length) {
        UIFont *font = nil;
        if ([[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_fontWithFontFamily:fontSize:fontWeight:)]) {
            font = [[KuiklyRenderBridge componentExpandHandler] hr_fontWithFontFamily:fontFamily fontSize:fontSize fontWeight:fontWeight];
        }
        if (font == nil && [[KuiklyRenderBridge componentExpandHandler] respondsToSelector:@selector(hr_fontWithFontFamily:fontSize:)]) {
            font = [[KuiklyRenderBridge componentExpandHandler] hr_fontWithFontFamily:fontFamily fontSize:fontSize];
        }
        if (font == nil) {
            font = [UIFont fontWithName:fontFamily size:fontSize];
        }
        if (font) {
            return font;
        }
    }
    
    if (json[@"fontStyle"] && [@"italic" isEqualToString:json[@"fontStyle"]]) {
        return [self italicFontWithSize:fontSize bold:fontWeight >=UIFontWeightBold itatic:YES weight:fontWeight];
    }
    
    if (@available(iOS 8.2, *)) {
        return  [UIFont systemFontOfSize:fontSize weight:fontWeight];
    } else {
        if(fontWeight >= UIFontWeightBold){
            return [UIFont boldSystemFontOfSize:fontSize];
        }
        return [UIFont systemFontOfSize:fontSize];
    }
}

+ (UIFont *)italicFontWithSize:(CGFloat)fontSize
                             bold:(BOOL)bold itatic:(BOOL)italic weight:(UIFontWeight)weight  {

    UIFont *font = [UIFont systemFontOfSize:fontSize weight:weight];
    UIFontDescriptorSymbolicTraits symbolicTraits = 0;
    if (italic) {
        symbolicTraits |= UIFontDescriptorTraitItalic;
    }
    if (bold) {
        symbolicTraits |= UIFontDescriptorTraitBold;
    }
    UIFont *specialFont = [UIFont fontWithDescriptor:[[font fontDescriptor] fontDescriptorWithSymbolicTraits:symbolicTraits] size:font.pointSize];
    return specialFont;
}

+ (CGFloat)CGFloat:(id)value {
    if ([value isKindOfClass:[NSNumber class] ]) {
        return [((NSNumber *)value) doubleValue];
    } else if([value isKindOfClass:[NSString class]]) {
        return [((NSString *)value) doubleValue];
    } else if( [value respondsToSelector:@selector(doubleValue)]) {
        return [value doubleValue];
    }
    return 0;
}

+ (NSUInteger)NSUInteger:(id)value {
    if ([value isKindOfClass:[NSNumber class] ]) {
        return [((NSNumber *)value) unsignedIntegerValue];
    } else if([value isKindOfClass:[NSString class]]) {
        return [((NSString *)value) longLongValue];
    } else if( [value respondsToSelector:@selector(integerValue)]) {
        return [value integerValue];
    }
    return 0;
}

+ (NSInteger)NSInteger:(id)value {
    if ([value isKindOfClass:[NSNumber class] ]) {
        return [((NSNumber *)value) integerValue];
    } else if([value isKindOfClass:[NSString class]]) {
        return [((NSString *)value) integerValue];
    } else if( [value respondsToSelector:@selector(integerValue)]) {
        return [value integerValue];
    }
    return 0;
}

+ (UIColor *)UIColor : (id)json {
    if (!json) {
        return nil;
    }
    if ([json isKindOfClass:[NSNumber class]] || [json isKindOfClass:[NSString class]]) {
        NSUInteger argb = [self NSUInteger:json];
        CGFloat a = ((argb >> 24) & 0xFF) / 255.0;
        CGFloat r = ((argb >> 16) & 0xFF) / 255.0;
        CGFloat g = ((argb >> 8) & 0xFF) / 255.0;
        CGFloat b = (argb & 0xFF) / 255.0;
        return [UIColor colorWithRed:r green:g blue:b alpha:a];
    } else {
#if DEBUG
        assert(0); //a UIColor. Did you forget to call processColor() on the JS side
#endif
        return nil;
    }
}


+ (KRBorderStyle)KRBorderStyle:(NSString *)stringValue {
    if ([stringValue isEqualToString:@"solid"]) {
        return KRBorderStyleSolid;
    }
    if ([stringValue isEqualToString:@"dotted"]) {
        return KRBorderStyleDotted;
    }
    if ([stringValue isEqualToString:@"dashed"]) {
        return KRBorderStyleDashed;
    }
    return KRBorderStyleSolid;
}

+ (NSTextAlignment)NSTextAlignment:(NSString *)stringValue {
    if ([stringValue isEqualToString:@"auto"]) {
        return NSTextAlignmentNatural;
    }
    if ([stringValue isEqualToString:@"left"]) {
        return NSTextAlignmentLeft;
    }
    if ([stringValue isEqualToString:@"center"]) {
        return NSTextAlignmentCenter;
    }
    if ([stringValue isEqualToString:@"right"]) {
        return NSTextAlignmentRight;
    }
    if ([stringValue isEqualToString:@"justify"]) {
        return NSTextAlignmentJustified;
    }
    return NSTextAlignmentNatural;
}


+ (KRTextDecorationLineType)KRTextDecorationLineType:(NSString *)stringValue {
    if ([stringValue isEqualToString:@"none"]) {
        return KRTextDecorationLineTypeNone;
    }
    if ([stringValue isEqualToString:@"underline"]) {
        return KRTextDecorationLineTypeUnderline;
    }
    if ([stringValue isEqualToString:@"line-through"]) {
        return KRTextDecorationLineTypeStrikethrough;
    }
    if ([stringValue isEqualToString:@"underline line-through"]) {
        return KRTextDecorationLineTypeUnderlineStrikethrough;
    }
   
    return KRTextDecorationLineTypeNone;
}

+ (NSLineBreakMode)NSLineBreakMode:(NSString *)stringValue {
    if ([stringValue isEqualToString:@"clip"]) {
        return NSLineBreakByClipping;
    }
    if ([stringValue isEqualToString:@"head"]) {
        return NSLineBreakByTruncatingHead;
    }
    if ([stringValue isEqualToString:@"tail"]) {
        return NSLineBreakByTruncatingTail;
    }
    if ([stringValue isEqualToString:@"middle"]) {
        return NSLineBreakByTruncatingMiddle;
    }
    if ([stringValue isEqualToString:@"wordWrapping"]) {
        return NSLineBreakByWordWrapping;
    }
    return NSLineBreakByTruncatingTail;
}


+ (UIViewContentMode)UIViewContentMode:(NSString *)stringValue {
    static dispatch_once_t onceToken;
    static NSDictionary *gConfigMode = nil;
    dispatch_once(&onceToken, ^{
        gConfigMode =  @{
            @"scale-to-fill": @(UIViewContentModeScaleToFill),
            @"scale-aspect-fit": @(UIViewContentModeScaleAspectFit),
            @"scale-aspect-fill": @(UIViewContentModeScaleAspectFill),
            @"redraw": @(UIViewContentModeRedraw),
            @"center": @(UIViewContentModeCenter),
            @"top": @(UIViewContentModeTop),
            @"bottom": @(UIViewContentModeBottom),
            @"left": @(UIViewContentModeLeft),
            @"right": @(UIViewContentModeRight),
            @"top-left": @(UIViewContentModeTopLeft),
            @"top-right": @(UIViewContentModeTopRight),
            @"bottom-left": @(UIViewContentModeBottomLeft),
            @"bottom-right": @(UIViewContentModeBottomRight),
            // Cross-platform values
            @"cover": @(UIViewContentModeScaleAspectFill),
            @"contain": @(UIViewContentModeScaleAspectFit),
            @"stretch": @(UIViewContentModeScaleToFill),
        };
    });
    NSNumber *value = gConfigMode[stringValue];
    if (value) {
        return (UIViewContentMode)[value integerValue];
    }
    return UIViewContentModeScaleAspectFill;
}




+ (void)hr_setStartPointAndEndPointWithLayer:(CAGradientLayer *)layer direction:(CSSGradientDirection)direction {
    CGSize size = layer.bounds.size;
    if (size.width == 0 || size.height == 0) {
        return ;
    }
    CGFloat deg = 0;
    NSInteger tanDeg = (atan((size.width / size.height)) / (M_PI * 2)) * 360; // 对角线角度 (正方形是45度)
    switch (direction) {
        case CSSGradientDirectionToBottom:
            deg = 180;
            break;
        case CSSGradientDirectionToLeft:
            deg = 270;
            break;
        case CSSGradientDirectionToRight:
            deg = 90;
            break;
        case CSSGradientDirectionToTopRight:
            deg = tanDeg;
            break;
        case CSSGradientDirectionToTopLeft:
            deg = (360 - tanDeg);
            break;
        case CSSGradientDirectionToBottomLeft:
            deg = (180 + tanDeg);
            break;
        case CSSGradientDirectionToBottomRight:
            deg = (180 - tanDeg);
            break;
        default:
            break;
    }
    NSInteger rotateDeg = deg;
    CGPoint startPoint = CGPointZero;
    CGPoint endPoint = CGPointZero;
    if (rotateDeg >= (360 - tanDeg) || rotateDeg <= tanDeg) { // top bottom
        if (rotateDeg >= (360 - tanDeg)) {
            CGFloat x = (size.width / 2 - hr_tan(360 - rotateDeg) * (size.height / 2))   /  size.width;
            endPoint = CGPointMake(x, 0);
            startPoint = CGPointMake(1 - x, 1);
        }else {
            CGFloat x = (size.width / 2 + hr_tan(rotateDeg) * (size.height / 2))   /  size.width;
            endPoint = CGPointMake(x, 0);
            startPoint = CGPointMake(1 - x, 1);
        }
    }else if (rotateDeg >= tanDeg && rotateDeg <= (180 - tanDeg)) { // right left
        if (rotateDeg <= 90) {
            CGFloat y = (size.height / 2 - hr_tan(90 - rotateDeg) * (size.width / 2))   /  size.height;
            endPoint = CGPointMake(1, y);
            startPoint = CGPointMake(0, 1 - y);
        }else {
            CGFloat y = (size.height / 2 + hr_tan(rotateDeg - 90) * (size.width / 2))   /  size.height;
            endPoint = CGPointMake(1, y);
            startPoint = CGPointMake(0, 1 - y);
        }
    }else if (rotateDeg >= (180 - tanDeg) && rotateDeg <= (180 + tanDeg)) { // bottom top
        rotateDeg -= 180;
        rotateDeg = (rotateDeg + 360) % 360;
        if (rotateDeg >= (360 - tanDeg)) {
            CGFloat x = (size.width / 2 - hr_tan(360 - rotateDeg) * (size.height / 2))   /  size.width;
            startPoint = CGPointMake(x, 0);
            endPoint = CGPointMake(1 - x, 1);
        }else {
            CGFloat x = (size.width / 2 + hr_tan(rotateDeg) * (size.height / 2))   /  size.width;
            startPoint = CGPointMake(x, 0);
            endPoint = CGPointMake(1 - x, 1);
        }
    }else if (rotateDeg >= (180 + tanDeg) && rotateDeg <= (360 - tanDeg)) { // left right
        rotateDeg -= 180;
        if (rotateDeg <= 90) {
            CGFloat y = (size.height / 2 - hr_tan(90 - rotateDeg) * (size.width / 2))   /  size.height;
            startPoint = CGPointMake(1, y);
            endPoint = CGPointMake(0, 1 - y);
        }else {
            CGFloat y = (size.height / 2 + hr_tan(rotateDeg - 90) * (size.width / 2))   /  size.height;
            startPoint = CGPointMake(1, y);
            endPoint = CGPointMake(0, 1 - y);
        }
    }
    if (!CGPointEqualToPoint(layer.startPoint, startPoint)) {
        layer.startPoint = startPoint;
    }
    if (!CGPointEqualToPoint(layer.endPoint, endPoint)) {
        layer.endPoint = endPoint;
    }
}

+ (UIBezierPath *)hr_bezierPathWithRoundedRect:(CGRect)rect
                           topLeftCornerRadius:(CGFloat)topLeftCornerRadius
                           topRightCornerRadius:(CGFloat)topRightCornerRadius
                           bottomLeftCornerRadius:(CGFloat)bottomLeftCornerRadius
                       bottomRightCornerRadius:(CGFloat)bottomRightCornerRadius {
    CGSize size = rect.size;
    UIBezierPath * path = [UIBezierPath bezierPath];
    CGFloat radius = topLeftCornerRadius;
    [path addArcWithCenter:CGPointMake(radius, radius) radius:radius startAngle:M_PI endAngle:M_PI * (3/2.0f) clockwise:true];
    radius = topRightCornerRadius;
    [path addLineToPoint:CGPointMake(size.width - radius, 0)];
    [path addArcWithCenter:CGPointMake(size.width - radius, radius) radius:radius startAngle:M_PI * (3/2.0f) endAngle:2 * M_PI clockwise:true];
    radius = bottomRightCornerRadius;
    [path addLineToPoint:CGPointMake(size.width, size.height - radius)];
    [path addArcWithCenter:CGPointMake(size.width - radius, size.height - radius) radius:radius startAngle:2 * M_PI endAngle:M_PI_2 clockwise:YES];
    radius = bottomLeftCornerRadius;
    [path addLineToPoint:CGPointMake(radius, size.height)];
    [path addArcWithCenter:CGPointMake(radius, size.height - radius) radius:radius startAngle:M_PI_2 endAngle:M_PI clockwise:YES];
    [path closePath];
    return path;
}


+ (NSArray *)hr_arrayWithJSONString:(NSString *)JSONString {
    if ([JSONString isKindOfClass:[NSArray class]]) {
        return (NSArray *)JSONString;
    }
    if (JSONString == nil || [JSONString isKindOfClass:[NSNull class]] || JSONString.length == 0) {
        return nil;
    }
    
    NSData *JSONData = [JSONString dataUsingEncoding:NSUTF8StringEncoding];
    NSError *err;
    NSArray *array;
    @try{
        array = [NSJSONSerialization JSONObjectWithData:JSONData
                                                       options:NSJSONReadingMutableContainers
                                                         error:&err];
    }
    @catch(NSException * e){
        NSString *errorMessage = [NSString stringWithFormat:@"%s_exception:%@",__FUNCTION__, e];
        [KRLogModule logError:errorMessage];
        NSAssert(false, errorMessage);
    }
   
    if(err) {
        return nil;
    }
    if ([array isKindOfClass:[NSArray class]]) {
        return array;
    }
    return array;
}

+ (UIViewAnimationOptions)hr_viewAnimationOptions:(NSString *)value {
    if ([value intValue] == 1) {
        return UIViewAnimationOptionCurveEaseIn;
    }
    if ([value intValue] == 2) {
        return UIViewAnimationOptionCurveEaseOut;
    }
    if ([value intValue] == 3) {
        return UIViewAnimationOptionCurveEaseInOut;
    }
    return UIViewAnimationOptionCurveLinear;
}

+ (UIViewAnimationCurve)hr_viewAnimationCurve:(NSString *)value {
    if ([value intValue] == 1) {
        return UIViewAnimationCurveEaseIn;
    }
    if ([value intValue] == 2) {
        return UIViewAnimationCurveEaseOut;
    }
    if ([value intValue] == 3) {
        return UIViewAnimationCurveEaseInOut;
    }
    return UIViewAnimationCurveLinear;
}


+ (UIKeyboardType)hr_keyBoardType:(id)value {
    NSString *keyboardType = [self hr_toString:value];
    if ([keyboardType isEqualToString:@"password"]) {
        return UIKeyboardTypeAlphabet;
    }
    if ([keyboardType isEqualToString:@"number"]) {
        return UIKeyboardTypeNumberPad;
    }
    if ([keyboardType isEqualToString:@"email"]) {
        return UIKeyboardTypeEmailAddress;
    }
    return UIKeyboardTypeDefault;
}

+ (NSString *)hr_toString:(id)value {
    if ([value isKindOfClass:[NSString class]]) {
        return value;
    } else if([value respondsToSelector:@selector(stringValue)]) {
        return  [value performSelector:@selector(string)];
    }
    return nil;
}

+ (UIReturnKeyType)hr_toReturnKeyType:(id)value {
    NSString *returnKeyType = [self hr_toString:value];
    if ([returnKeyType isEqualToString:@"default"]) {
        return UIReturnKeyDefault;
    } else if ([returnKeyType isEqualToString:@"search"]) {
        return UIReturnKeySearch;
    } else if ([returnKeyType isEqualToString:@"send"]) {
        return UIReturnKeySend;
    } else if ([returnKeyType isEqualToString:@"go"]) {
        return UIReturnKeyGo;
    } else if ([returnKeyType isEqualToString:@"done"]) {
        return UIReturnKeyDone;
    } else if ([returnKeyType isEqualToString:@"next"]) {
        return UIReturnKeyNext;
    } else if ([returnKeyType isEqualToString:@"join"]) {
        return UIReturnKeyJoin;
    } else if ([returnKeyType isEqualToString:@"google"]) {
        return UIReturnKeyGoogle;
    } else if ([returnKeyType isEqualToString:@"yahoo"]) {
        return UIReturnKeyYahoo;
    } else if ([returnKeyType isEqualToString:@"route"]) {
        return UIReturnKeyRoute;
    } else if ([returnKeyType isEqualToString:@"continue"]) {
        return UIReturnKeyContinue;
    } else if ([returnKeyType isEqualToString:@"emergencyCall"]) {
        return UIReturnKeyEmergencyCall;
    }
    return UIReturnKeyDefault;;
    
}

+ (UIAccessibilityTraits)kr_accessibilityTraits:(id)value {
    NSString *returnKeyType = [self hr_toString:value];
    if ([returnKeyType isEqualToString:@"button"]) {
        return UIAccessibilityTraitButton;
    } else if ([returnKeyType isEqualToString:@"text"]) {
        return UIAccessibilityTraitStaticText;
    } else if ([returnKeyType isEqualToString:@"image"]) {
        return UIAccessibilityTraitImage;
    } else if ([returnKeyType isEqualToString:@"search"]) {
        return UIAccessibilityTraitSearchField;
    } else if ([returnKeyType isEqualToString:@"checkbox"]) {
        return UIAccessibilityTraitButton | UIAccessibilityTraitSelected;
    }
    return UIAccessibilityTraitNone;
    
}

+ (NSString *)hr_base64Decode:(NSString *)string {
    NSData *data = [[NSData alloc] initWithBase64EncodedString:string options:0];
    return [[NSString alloc]initWithData:data encoding: NSUTF8StringEncoding];
}

+ (CGRect)hr_rectInset:(CGRect)rect insets:(UIEdgeInsets)insets {
    if (!UIEdgeInsetsEqualToEdgeInsets(insets, UIEdgeInsetsZero)) {
        CGRect newRect = CGRectMake(rect.origin.x - insets.left,
                                     rect.origin.y - insets.top,
                                     rect.size.width + insets.left + insets.right, rect.size.height + insets.top + insets.bottom);
        return newRect;;
    }
    return rect;
}

+ (NSString *)hr_dictionaryToJSON:(NSDictionary *)dict {
    NSError *parseError = nil;
    NSString *jsonString = nil;
    @try {
        NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict
                                                           options:NSJSONWritingFragmentsAllowed
                                                             error:&parseError];
        jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    } @catch (NSException *exception) {
        // 捕获并打印异常信息
        NSString *assertReason = [NSString stringWithFormat:@"%s exception:%@ reason:%@ userinfo:%@", __FUNCTION__, exception.name, exception.reason,exception.userInfo];
        [KRLogModule logError:assertReason];
        NSAssert(false, assertReason);
    }
    return jsonString;
}

+ (BOOL)hr_isJsonArray:(id)value {
    if ([value isKindOfClass:[NSArray class]]) {
        NSArray *array = (NSArray *)value;
        for (NSObject *ele in array) {
            if ([ele isKindOfClass:[NSData class]]) {
                return NO;
            }
        }
        return YES;
    }
    return NO;
}

+ (void)hr_alertWithTitle:(NSString *)title message:(NSString *)message {
    if([UIApplication isAppExtension]){
        return;
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:title
                                                                                 message:message
                                                                          preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction * action = [UIAlertAction actionWithTitle:@"确定" style:(UIAlertActionStyleDefault) handler:nil];
        [alertController addAction:action];
        [[UIApplication sharedApplication].keyWindow.rootViewController presentViewController:alertController animated:YES completion:nil];
    });
  
}

+ (NSString *)hr_md5StringWithString:(NSString *)string {
    const char *cstr = [string UTF8String];
    unsigned char result[16];
    CC_MD5(cstr, (CC_LONG)strlen(cstr), result);
    
    return [NSString stringWithFormat:@"%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X%02X",
            result[0], result[1], result[2], result[3],
            result[4], result[5], result[6], result[7],
            result[8], result[9], result[10], result[11],
            result[12], result[13], result[14], result[15]
            ];
}

+ (CGFloat)statusBarHeight {
    CGFloat statusBarHeight = 0;
    if(![UIApplication isAppExtension]){
        if (@available(iOS 13.0, *)) {
            UIWindowScene *windowScene = (UIWindowScene *)(UIApplication.sharedApplication.connectedScenes.anyObject);
            if (windowScene && [windowScene isKindOfClass:UIWindowScene.class]) {
                statusBarHeight = windowScene.statusBarManager.statusBarFrame.size.height;
            }
        }
        if (!statusBarHeight) {
            statusBarHeight = UIApplication.sharedApplication.statusBarFrame.size.height;
        }
    }
    
    if (@available(iOS 16.0, *)) {
        BOOL needAdjust = (statusBarHeight == 44);
        if (needAdjust) {
            UIWindow* mainWindow = nil;
            for (UIScene* scene in [UIApplication sharedApplication].connectedScenes) {
                if (scene.activationState == UISceneActivationStateForegroundActive && [scene isKindOfClass:[UIWindowScene class]]) {
                    UIWindowScene *windowScene = (UIWindowScene *)scene;
                    mainWindow = windowScene.windows.firstObject;
                    break;
                }
            }
            if (mainWindow && mainWindow.safeAreaInsets.top >= 59) { // 兼容部分场景高度获取不正确
                statusBarHeight = 54;
            }
        }
    }
   
    return statusBarHeight ?: [self defaultStatusBarHeight];
}

+ (CGFloat)defaultStatusBarHeight {
    CGFloat statusBarHeight = 20;
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPhone) {
        CGSize screenSize = UIScreen.mainScreen.bounds.size;
        if (MAX(screenSize.width, screenSize.height) / MIN(screenSize.width, screenSize.height) >= 2.0) {
            statusBarHeight = 44;
        }
    }
    return statusBarHeight;
}

+ (NSString *)stringWithInsets:(UIEdgeInsets)insets {
    return [NSString stringWithFormat:@"%.2lf %.2lf %.2lf %.2lf", insets.top, insets.left, insets.bottom, insets.right];
}


+ (UIEdgeInsets)currentSafeAreaInsets {
    if([UIApplication isAppExtension]){
        return UIEdgeInsetsZero;
    }
    
    if (@available(iOS 11, *)) {
        UIWindow *window = UIApplication.sharedApplication.windows.firstObject;
        return window.safeAreaInsets;
    } else {
        // 在 iOS 11 之前的版本中，您需要根据需要自己计算安全边距
        return UIEdgeInsetsZero;
    }
}

+ (CGFloat)toSafeFloat:(CGFloat)value {
    if (isnan(value) || isinf(value)) {
        [KRLogModule logError:[NSString stringWithFormat:@"has [nan inf] value when safe float"]];
        return 0;
    }
    return value;
}

+ (CGRect)toSafeRect:(CGRect)rect {
    return CGRectMake([self toSafeFloat:rect.origin.x],
                      [self toSafeFloat:rect.origin.y],
                      [self toSafeFloat:rect.size.width],
                      [self toSafeFloat:rect.size.height]);
}

+ (NSString *)sizeStrWithSize:(CGSize)size {
    return [NSString stringWithFormat:@"%.2lf|%.2lf", size.width, size.height];
}

+ (id)nativeObjectToKotlinObject:(id)ocObject {
    if ([ocObject isKindOfClass:[NSDictionary class]] || [KRConvertUtil hr_isJsonArray:ocObject] ) {
        return  [KRConvertUtil hr_dictionaryToJSON:ocObject];
    }
    return ocObject;
}

@end
