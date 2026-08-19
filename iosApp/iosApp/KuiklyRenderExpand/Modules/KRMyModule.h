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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#import <Foundation/Foundation.h>
#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN

/// 对应鸿蒙侧 KRMyModule / Android 侧 KRMyModule，演示 Kuikly Module 的同步与异步调用。
/// module 名 = 类名 "KRMyModule"（KRBaseModule 系统通过 NSClassFromString 自动映射）。
@interface KRMyModule : KRBaseModule

@end

NS_ASSUME_NONNULL_END