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

@interface KRAsyncDeallocManager : NSObject


+ (instancetype)shareManager;

- (void)asyncDeallocWithObject:(id _Nullable)deallocObject;

/// 将对象的释放投递到 Context 线程，保证与 Context 线程上的布局计算串行，
/// 避免 Global Queue 异步释放 NSTextStorage 中的 UIFont 时与 Context 线程并发产生野指针。
- (void)asyncDeallocOnContextQueueWithObject:(id _Nullable)deallocObject;


@end

NS_ASSUME_NONNULL_END
