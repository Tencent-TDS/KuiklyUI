/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
#import "KRBaseModule.h"

NS_ASSUME_NONNULL_BEGIN

#if DEBUG
/**
 * Demo/test-only callKotlin interop microbench (DEBUG only).
 * Class name must match Kotlin MODULE_NAME for KRClassFromString lookup.
 */
@interface CallKotlinPerfTestModule : KRBaseModule
@end
#endif

NS_ASSUME_NONNULL_END
