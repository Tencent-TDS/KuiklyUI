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
- (BOOL)hr_setImageWithUrl:(nullable NSString *)url forImageView:(UIImageView *)imageView;

@optional

/*
 * 通过文件名从自定义bundle获取图片等资源URL，用于加载图片等资源
 * 适用于模块化、组件化开发，自定义资源bundle（非mainBundle的场景）
 * 通过传入的fileName（fileName带有模块名），确定该资源所在模块bundle的路径，返回该资源的完整URL
 * eg：fileName为 XXXModule/XXXPage/btn_back_ic，该模块资源打包成XXXModule.bundle，业务根据fileName确定模块并返回该资源URL
 * @param fileName 资源图片文件名
 * @param extension 资源图片fileExtension
 * @return 图片等资源URL
 */
- (NSURL *)hr_customBundleUrlForFileName:(NSString *)fileName extension:(NSString *)fileExtension;

/*
 * 自定义实现设置图片
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @param complete 图片处理完成后的回调
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 */
- (BOOL)hr_setImageWithUrl:(nullable NSString *)url forImageView:(UIImageView *)imageView complete:(ImageCompletionBlock)completeBlock;

/*
 * 自定义实现设置图片（带回调和src一致性验证，优先调用该方法）
 * @param url 设置的图片url，如果url为nil，则是取消图片设置，需要view.image = nil
 * @param placeholder 设置的占位图，默认设置为nil
 * @param options 图片加载参数（对应SDWebImageOptions），默认传KRImageOptionAvoidAutoSetImage(1<<10，对应SDWebImageAvoidAutoSetImage)，阻断SDWebImage无感更新ImageView的image
 * @param complete 图片处理完成后的回调，内置src一致性验证
 * @return 是否处理该图片设置，返回值为YES，则交给该代理实现，否则sdk内部自己处理
 *
 * @warning 实现此方法时，必须在图片加载完成后调用completeBlock，SDK通过此回调完成ImageView.image的最终设置，若不调用将导致图片无法显示
 */
- (BOOL)hr_setImageWithUrl:(nonnull NSString *)url forImageView:(nonnull UIImageView *)imageView placeholderImage:(nullable UIImage *)placeholder options:(NSUInteger)options complete:(ImageCompletionBlock)completeBlock;

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
