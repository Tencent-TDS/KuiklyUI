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
@file:OptIn(ExperimentalForeignApi::class)

package com.tencent.kuikly.compose.coroutines.internal

import com.tencent.kuikly.com_tencent_kuikly_IsCurrentOnContextThread
import com.tencent.kuikly.com_tencent_kuikly_ScheduleContextTask
import com.tencent.kuikly.core.manager.BridgeManager
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString

internal actual fun platformInitScheduler() {
}

internal actual inline fun platformIsOnKuiklyThread(pagerId: String): Boolean =
    com_tencent_kuikly_IsCurrentOnContextThread(pagerId)

internal actual inline fun platformScheduleOnKuiklyThread(pagerId: String) {
    com_tencent_kuikly_ScheduleContextTask(pagerId, staticCFunction { pagerIdBytes: CPointer<ByteVar>? ->
        val idStr = pagerIdBytes?.toKString() ?: return@staticCFunction
        KuiklyContextScheduler.runTask(idStr)
    })
}

internal actual inline fun platformNotifyKuiklyException(t: Throwable) {
    BridgeManager.callExceptionMethod(t.stackTraceToString())
}