/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSObject (KR)

- (NSDictionary *)hr_stringToDictionary;
- (NSDictionary *)kr_stringToDictionary;

- (NSArray *)hr_stringToArray;
- (NSArray *)kr_stringToArray;

- (NSString *)kr_urlEncode;

- (id)kr_invokeWithSelector:(SEL)selector args:(id)args;

+ (id)kr_performWithTarget:(id)target selector:(SEL)aSelector  withObjects:(NSArray *)objects;

+ (id)kr_performWithClass:(Class)classTarget selector:(SEL)aSelector  withObjects:(NSArray *)objects;

+ (BOOL)kr_swizzleInstanceMethod:(SEL)origSel withMethod:(SEL)altSel;

@end

@interface NSDictionary (KR)

- (NSString *)hr_dictionaryToString;
- (NSString *)kr_dictionaryToString;

@end

@interface NSString (KR)

- (NSString *)kr_appendUrlEncodeWithParam:(NSDictionary *)param;
- (NSString *)kr_md5String;
- (NSString *)kr_base64Encode;
- (NSString *)kr_base64Decode;
- (NSString *)kr_sha256String;
- (NSString *)kr_subStringWithIndex:(NSUInteger)index;
- (NSUInteger)kr_length; // emoji/中文算一个1个长度来统计字符串长度

@end

@interface UIView (KR)

+ (UIImage *)kr_safeAsImageWithLayer:(CALayer *)layer bounds:(CGRect)bounds;
- (UIViewController *)kr_viewController;
@end

@interface NSInvocation (KR)

- (void)kr_setArgumentObject:(id)arguementObject atIndex:(NSInteger)idx;

@end

@interface UIImage (KR)

/**
  使用vImage进行高斯模糊
 @param radius 模糊的范围 可以1~99
 @return 返回已经模糊过的图片
 */
- (UIImage *)kr_blurBlurRadius:(CGFloat)radius;

/**
 * 转换为热力图图片
 */
- (UIImage *)kr_applyHeatmapWithGridentRawData:(unsigned char *)gridentRawData gridentImage:(UIImage *)gridentImage;

/**
 * 获取图片像素数组
 */
- (unsigned char *)kr_getRawData;

/**
 * 染色非透明像素为该颜色
 */
- (UIImage *)kr_tintedImageWithColor:(UIColor *)color;

/**
 * 图片应用颜色滤镜矩阵
 */
- (UIImage *)kr_applyColorFilterWithColorMatrix:(NSString *)colorFilterMatrix;

@end

@interface NSMutableAttributedString (KR)

- (void)kr_addAttribute:(NSAttributedStringKey)name value:(id)value range:(NSRange)range;

@end

@interface UIApplication (KR)
+ (BOOL)isAppExtension;
@end

NS_ASSUME_NONNULL_END
