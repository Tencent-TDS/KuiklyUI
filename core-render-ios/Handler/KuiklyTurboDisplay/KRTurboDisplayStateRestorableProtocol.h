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

NS_ASSUME_NONNULL_BEGIN

/**
 * @brief TurboDisplay状态恢复协议
 * 用于在TB首屏渲染后恢复View的状态（如offset等非属性的状态量）
 */
@protocol KRTurboDisplayStateRestorableProtocol <NSObject>

@required
/**
 * @brief 应用额外缓存内容（恢复状态）
 * @param extraCacheProps 该View对应的缓存属性字典（不含viewName）
 *        例如：{ "contentOffsetX": 0, "contentOffsetY": 350.5 }
 */
- (void)applyTurboDisplayExtraCacheContent:(NSDictionary *)extraCacheProps;

@end

NS_ASSUME_NONNULL_END
