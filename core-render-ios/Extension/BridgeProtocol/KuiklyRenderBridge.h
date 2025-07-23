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
#import "KRLogModule.h"
#import "KRAPNGView.h"
#import "KRPAGView.h"
#import "KRFontModule.h"
#import "KRNotifyModule.h"
#import "KRCacheManager.h"
NS_ASSUME_NONNULL_BEGIN

@protocol KuiklyRenderBridgeProtocol;
@protocol KuiklyRenderComponentExpandProtocol;

typedef void (^KRBundleResponse)(NSString *_Nullable script , NSError *_Nullable error);

@interface KuiklyRenderBridge : NSObject

/*
 * @brief 注册图片下载实现
 */
+ (void)registerComponentExpandHandler:(id<KuiklyRenderComponentExpandProtocol>)componentExpandHandler;
/*
 * @brief 注册自定义log实现
 */
+ (void)registerLogHandler:(id<KuiklyLogProtocol>)logHandler;

/*
 * @brief 注册自定义APNGView实现
 * @param creator 创建apngView实现者实例
 */
+ (void)registerAPNGViewCreator:(APNGViewCreator)creator;
/*
 * @brief 自定义注册PAGView实现(默认只要pod 'libpag', ">= 4.3.21"，就无需注册)
 * @param creator 创建pageView实例
 */
+ (void)registerPAGViewCreator:(PAGViewCreator)creator;
/*
 * @brief 注册自定义Font实现
 */
+ (void)registerFontHandler:(id<KuiklyFontProtocol>)fontHandler;

/*
 * @brief 注册自定义Cache实现
 */
+ (void)registerCacheHandler:(id<KRCacheProtocol>)cacheHandler;


/*
 * @brief 注册自定义Cache实现
 */
+ (void)registerCacheHandler:(id<KRCacheProtocol>)cacheHandler;


+ (id<KuiklyRenderComponentExpandProtocol>)componentExpandHandler;

@end


typedef void(^ImageCompletionBlock)(UIImage * _Nullable image, NSError * _Nullable error, NSURL * _Nullable imageURL);


@protocol KuiklyRenderComponentExpandProtocol <NSObject>


/*
 * 自定义实现设置图片
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 *
 * 注意：如果同时实现了带完成回调的方法
 *      - (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView
 *                        complete:(ImageCompletionBlock)completeBlock;
 * 则优先调用带回调的方法。
 */
- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView;

@optional


/*
 * 自定义实现设置图片（带完成回调，优先调用该方法）
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @param complete 图片处理完成后的回调
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(NSString *)url forImageView:(UIImageView *)imageView complete:(ImageCompletionBlock)completeBlock;

/*
 * 自定义实现设置颜值
 * @param value 设置的颜色值
 * @return 完成自定义处理的颜色对象
 */
- (UIColor *)hr_colorWithValue:(NSString *)value;

/*
 * 扩展文本后置处理
 * @param attributedString 源文本对象
 * @param textPostProcessor 后置处理标记（由kotlin侧text组件属性设置textPostProcessor()而来）
 * @return 返回新的文本对象
 */
- (NSMutableAttributedString *)hr_customTextWithAttributedString:(NSAttributedString *)attributedString textPostProcessor:(NSString *)textPostProcessor;


/*
 * 自定义字体创建
 * @param fontfamily 字体名
 * @param fontSize 字体大小
 * @return 返回自定义字体 （注：若返回nil，则走sdk自身默认创建字体逻辑）
 */
- (UIFont *)hr_fontWithFontFamily:(NSString *)fontfamily fontSize:(CGFloat)fontSize;

/*
 * 自定义字体创建
 * @param fontfamily 字体名
 * @param fontSize 字体大小
 * @param fontWeight 字体weight
 * @return 返回自定义字体 （注：若返回nil，则走sdk自身默认创建字体逻辑）
 */
- (UIFont *)hr_fontWithFontFamily:(NSString *)fontfamily fontSize:(CGFloat)fontSize fontWeight:(UIFontWeight)fontWeight;

/*
 * 扩展Kotlin文本组件的text属性-后置处理
 * @param text 源文本
 * @param textPostProcessor 后置处理标记（由kotlin侧text组件属性设置textPostProcessor()而来）
 * @return 返回新的文本对象
 */
- (NSString *)kr_customTextWithText:(NSString *)text textPostProcessor:(NSString *)textPostProcessor;

/*
 * 扩展Kotlin文本组件的富文本-后置处理
 * 注:若有插入NSTextAttachment,请其实现KRTextAttachmentStringProtocol协议
 * @param attributedString 源文本对象
 * @param font 字体
 * @param textPostProcessor 后置处理标记（由kotlin侧text组件属性设置textPostProcessor()而来）
 * @return 返回新的文本对象
 */
- (NSMutableAttributedString *)kr_customTextWithAttributedString:(NSAttributedString *)attributedString
                                                            font:(UIFont *)font
                                               textPostProcessor:(NSString *)textPostProcessor;


@end
/*
 * 当对kr_customTextWithAttributedString中插入NSTextAttachment图像时, 需要实现该协议用于还原原来的文本
 */
@protocol KRTextAttachmentStringProtocol <NSObject>

/*
 * 返回TextAttachment前的本来文本
 */
- (NSString *)kr_originlTextBeforeTextAttachment;

@end

NS_ASSUME_NONNULL_END
