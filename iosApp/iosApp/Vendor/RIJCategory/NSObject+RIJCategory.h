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

#import <UIKit/UIKit.h>
NS_ASSUME_NONNULL_BEGIN

#define  kHexStrColor(hexStr)  ([UIColor  rij_colorWithHexString:(hexStr)])

typedef void(^ IDBlock)(id  obj);

@interface RIJDeallocObject : NSObject

@property(nonatomic, strong) NSMutableArray * blocks;

@property(nonatomic, strong)  NSString * obj;

- (void)addBlockWithBlock:(IDBlock) block;


@end


//-----------------类分割线------------------

@protocol RIJWeakBlockDelegate;
@interface RIJWeakBlock : NSObject
@property(nullable, nonatomic , weak) id target;
@property(nullable, nonatomic , copy) IDBlock block;
@property(nullable, nonatomic , weak) id otherInfo;
@property(nullable, nonatomic , copy) NSString * targetKey;
@end



@interface UIView(RIJCategory)
// 判断View是否显示在屏幕上
- (BOOL)rij_isDisplayedInScreen;
- (UIImage*)rij_convertToImage;
- (UITapGestureRecognizer *)rij_addTapGestureRecognizerWithTarget:(id)target action:(SEL) action;
//引用主工程
- (UIViewController*)viewController;
/**
 *  frame ext
 */
- (CGFloat)rij_width;
- (void)setRij_width:(CGFloat)width;
- (CGFloat)rij_height;
- (void)setRij_height:(CGFloat)height;
- (CGPoint)rij_leftTop;
- (void)setRij_leftTop:(CGPoint)leftTop;
- (CGPoint)rij_rightBottom;
- (void)setRij_rightBottom:(CGPoint)rightBottom;
- (void)setRij_left:(CGFloat)left;
- (CGFloat)rij_left;
- (void)setRij_right:(CGFloat)right;
- (CGFloat)rij_right;
- (void)setRij_x:(CGFloat)x;
- (CGFloat)rij_x;
- (void)setRij_y:(CGFloat)y;
- (CGFloat)rij_y;
- (void)setRij_centerX:(CGFloat)centerX;
- (CGFloat)rij_centerX;
- (void)setRij_centerY:(CGFloat)centerY;
- (CGFloat)rij_centerY;
- (void)setRij_size:(CGSize)size;
- (CGSize)rij_size;
-(CGFloat) rij_top;
- (void)setRij_top:(CGFloat)top;
-(CGFloat) rij_bottom;
- (void)setRij_bottom:(CGFloat)bottom;

- (CGFloat)width;
- (void)setWidth:(CGFloat)width;
- (CGFloat)height;
- (void)setHeight:(CGFloat)height;
- (CGPoint)leftTop;
- (void)setLeftTop:(CGPoint)leftTop;
- (CGPoint)rightBottom;
- (void)setRightBottom:(CGPoint)rightBottom;
- (void)setLeft:(CGFloat)left;
- (CGFloat)left;
- (void)setRight:(CGFloat)right;
- (CGFloat)right;
- (void)setX:(CGFloat)x;
- (CGFloat)x;
- (void)setY:(CGFloat)y;
- (CGFloat)y;
- (void)setCenterX:(CGFloat)centerX;
- (CGFloat)centerX;
- (void)setCenterY:(CGFloat)centerY;
- (CGFloat)centerY;
- (void)setSize:(CGSize)size;
- (CGSize)size;
-(CGFloat) top;
- (void)setTop:(CGFloat)top;
-(CGFloat) bottom;
- (void)setBottom:(CGFloat)bottom;

- (UIColor *)rij_colorOfPoint:(CGPoint)point;
- (UIViewController *)rij_viewController;

@end






//-----------------类分割线------------------



@interface NSObject (RIJCategory)

+ (BOOL)rij_isIphoneX;

+ (NSString *)rij_deviceModel;

- (NSString *)rij_urlEncode;

//每个对象都有一个额外信息存储字段
@property(nonatomic , strong ,nullable) id rij_extraData;
/**
 *  对象地址
 */
- (NSString *)rij_address;
/**
 *  死亡闭包 self 死亡时回调
 */
-(void) rij_performBlockOnDeallocWithBlock:(IDBlock) block;

/**
 * 序列号的字符串转字典
 */
- (NSDictionary *)rij_stringToDictionary;


//对字典的安全取值
- (NSArray *) rij_arrayValueForKey:(NSString *)key;
- (NSString *) rij_stringValueForKey:(NSString *)key;
- (long long) rij_longlongValueForKey:(NSString *)key;
- (unsigned long long) rij_unsignedLonglongValueForKey:(NSString *)key;
- (int) rij_intValueForKey:(NSString *)key;
- (NSObject * ) rij_objectForKey:(NSString * )key;
//对字典的多个key安全取值
- (NSArray *) rij_arrayValueForMultiKeys:(NSArray *)keys;
- (NSString *) rij_stringValueForMultiKeys:(NSArray *)keys;
- (long long) rij_longlongValueForMultiKeys:(NSArray *)keys;
-(id) rij_valueForMultiKey:(id) key , ...;


@end



@interface UIImage (RIJCategory)

- (UIImage *)rij_makeRoundedWithRadius:(CGFloat) radius;
+ (UIImage *)rij_imageWithColor:(UIColor *)color size:(CGSize) size;
- (UIImage *)rij_imageByResizeToSize:(CGSize)size contentMode:(UIViewContentMode)contentMode;
- (UIImage *)rij_imageByResizeToSize:(CGSize)size contentMode:(UIViewContentMode)contentMode scale:(CGFloat)scale;
- (UIImage *)rij_imageByRoundCornerRadius:(CGFloat)radius
                              borderWidth:(CGFloat)borderWidth
                              borderColor:(UIColor *)borderColor;
@end


@interface UIColor (RIJCategory)
+ (UIColor *) rij_colorWithHexString: (NSString *)color;
+ (UIColor *) rij_colorWithHexString: (NSString *)color alpha:(CGFloat) alpha;
+ (BOOL)rij_isLighterColor:(UIColor *)color;
@end

//-----------------类分割线------------------
@interface NSMutableAttributedString (ReadInJoyVideo)


+ (NSMutableAttributedString *)createWithText:(NSString *)text font:(UIFont *)font color:(UIColor *)color;
+ (NSMutableAttributedString *)createForNoneSizeWithText:(NSString *)text font:(UIFont *)font color:(UIColor *)color;
- (CGSize)rij_sizeThatFits:(CGSize)size  numberOfLines:(NSUInteger)lines lineBreakMode:(NSLineBreakMode)mode;

- (CGSize)rij_sizeThatFits:(CGSize)size  numberOfLines:(NSUInteger)lines;

- (CGSize)rij_sizeThatFitsForDefault;

@end


@interface NSArray (RIJCategory)
- (id)rij_objectAtIndex:(NSUInteger)index;

@end

@interface NSMutableDictionary (RIJCategory)
- (void)rij_setObject:(id)anObject forKey:(id<NSCopying>)aKey;
@end

@interface NSDictionary (RIJCategory)


- (NSString * )rij_urlEncodeString;
- (NSString *)vrij_convertToJsonString;

@end

@interface NSString (RIJCategory)
- (NSString *) rij_trimingWhitespaceAndNewLine;
- (NSString *)rij_md5String;
- (NSString *)rij_appendUrlEncodeWithParam:(NSDictionary *)param;
/**
 *  截取URL中的参数
 *
 *  @return NSMutableDictionary parameters
 */
- (NSMutableDictionary *)viola_getURLParameters;
- (NSMutableDictionary *)rij_getURLParameters;
@end

@interface UIApplication (RIJCategory)

- (void)rij_setStatusBarStyle:(UIStatusBarStyle)style animated:(BOOL)animated;
//不可直接调用
- (void)qq_setStatusBarStyle:(UIStatusBarStyle)style animated:(BOOL)animated;



@end



NS_ASSUME_NONNULL_END
