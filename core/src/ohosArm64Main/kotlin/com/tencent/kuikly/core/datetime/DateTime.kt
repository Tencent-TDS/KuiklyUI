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

package com.tencent.kuikly.core.datetime

import ohos.com_tencent_kuikly_CurrentTimestamp
import ohos.com_tencent_kuikly_GetThreadCPUTimeInNanoseconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.system.getTimeNanos

actual object DateTime {

    @OptIn(ExperimentalForeignApi::class)
    actual fun currentTimestamp(): Long {
        return com_tencent_kuikly_CurrentTimestamp()
    }

    // Kotlin 2.3 起 kotlin.system.getTimeNanos 被标记为 error 级 deprecation，
    // 但官方替代（measureTime / TimeSource.Monotonic.markNow）均不提供 Long 纳秒语义，
    // 且该 API 物理仍存在，故显式压制；2.1.21 / 2.0.21-mini 侧不受影响。
    @Suppress("DEPRECATION_ERROR")
    actual fun nanoTime(): Long {
        return getTimeNanos()
    }

    @OptIn(ExperimentalForeignApi::class)
    internal actual fun threadLocalTimestamp(): Long {
        return com_tencent_kuikly_GetThreadCPUTimeInNanoseconds() / 1_000_000
    }


}