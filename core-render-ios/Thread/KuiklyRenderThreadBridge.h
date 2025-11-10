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
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

/// C-callable: Schedule a task on the Kuikly context queue for the given pagerId.
/// The provided callback will be invoked on the context queue with the same pagerId C string.
FOUNDATION_EXPORT void com_tencent_kuikly_ScheduleContextTask(const char * _Nullable pagerId, void (* _Nullable onSchedule)(const char * _Nullable pagerId));

/// C-callable: Return true if the current thread is the context thread.
/// `pagerId` parameter currently ignored but kept for API compatibility.
FOUNDATION_EXPORT bool com_tencent_kuikly_IsCurrentOnContextThread(const char * _Nullable pagerId);

#ifdef __cplusplus
}
#endif

