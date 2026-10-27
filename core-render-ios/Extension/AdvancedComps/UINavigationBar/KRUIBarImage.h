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

#import "KRUIKit.h" // [macOS]

#if !TARGET_OS_OSX // [macOS]

NS_ASSUME_NONNULL_BEGIN

@interface KRUIBarImage : NSObject

/// 与 UIFontWeightRegular 等一致；iOS 13+ 用于 SF Symbol 配置。
FOUNDATION_EXPORT const CGFloat KRUIBarImageSymbolFontWeightRegular;

/// src 优先于 SF Symbol。支持 assets://、file://、data:image、http(s)、imageNamed。
+ (void)resolveSrc:(nullable NSString *)src
            symbol:(nullable NSString *)symbol
    symbolFontWeight:(CGFloat)symbolFontWeight
       useTemplate:(BOOL)useTemplate
          rootView:(nullable UIView *)rootView
        completion:(void (^)(UIImage * _Nullable image))completion;

@end

NS_ASSUME_NONNULL_END

#endif
