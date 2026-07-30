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

/// BackPress 事件回调类型
typedef void (^KuiklyBackPressCompletion)(BOOL consumed);
/**
 * 监听 BackPress 消费状态回调
 */
@interface KRBackPressModule : KRBaseModule

/**
 * 设置返回键事件的 completion 回调
 * @param completion 当 Kotlin 侧返回结果时执行
 */
- (void)setBackPressCompletion:(nullable KuiklyBackPressCompletion)completion;

@end

NS_ASSUME_NONNULL_END
