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

#import "KuiklyRenderThreadBridge.h"
#import "KuiklyRenderThreadManager.h"
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>

// C-callable wrappers
void com_tencent_kuikly_ScheduleContextTask(const char* pagerId, void (*onSchedule)(const char* pagerId)) {
    if (!onSchedule) return;
    // Copy pagerId to ensure it remains valid on the context queue
    const char *copied = NULL;
    if (pagerId) {
        size_t len = strlen(pagerId) + 1;
        char *buf = (char *)malloc(len);
        if (buf) {
            memcpy(buf, pagerId, len);
            copied = buf;
        }
    }

    dispatch_block_t block = ^{
        onSchedule(copied);
        // free the duplicated string after callback
        if (copied) free((void *)copied);
    };
    [KuiklyRenderThreadManager performOnContextQueueWithBlock:block sync:NO];
}

bool com_tencent_kuikly_IsCurrentOnContextThread(const char* pagerId) {
    // pagerId currently unused; keep for API compatibility
    return [KuiklyRenderThreadManager isContextQueue];
}

