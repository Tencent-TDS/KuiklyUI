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

#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN


@protocol KuiklyFontProtocol <NSObject>

@optional
/**
 * 根据设置的原字体大小返回合适的最终缩放大小尺寸
 * 注：若要启用该字体缩放，需要实现（override）Kotlin侧Pager#scaleFontSizeEnable接口返回YES
 */
- (CGFloat)scaleFitWithFontSize:(CGFloat)fontSize;

/**
 * 根据设置的FontFamily和contextParam 实现动态的加载业务侧的字体资源到IOS环境
 */
- (BOOL)hr_loadCustomFont:(NSString *)fontFamily contextParams:(KuiklyContextParam *)contextParam;

/**
 * 字体别名映射：业务将 fontFamily（可为业务别名）映射为字体真实名称（PostScript Name），
 * 框架将使用映射结果完成后续的字体查找与动态注册；多字重字体族可结合 fontWeight 返回对应字重的真实名称
 * 返回 nil 表示业务不处理该 fontFamily，继续走框架默认加载逻辑
 */
- (nullable NSString *)hr_mappedFontNameForFontFamily:(NSString *)fontFamily
                                           fontWeight:(UIFontWeight)fontWeight;

@end

/**
 * 字体模块，用于自定义适配字体需求
 */
@interface KRFontModule : KRBaseModule

/*
 * @brief 注册自定义log实现
 */
+ (void)registerFontHandler:(id<KuiklyFontProtocol>)fontHandler;

/**
 * 向IOS环境内动态加载字体
 */
+ (BOOL)hr_loadCustomFont:(NSString *)fontFamily contextParams:(KuiklyContextParam *)contextParam;

/**
 * 字体别名映射
 */
+ (nullable NSString *)hr_mappedFontNameForFontFamily:(NSString *)fontFamily
                                           fontWeight:(UIFontWeight)fontWeight;

@end

NS_ASSUME_NONNULL_END
