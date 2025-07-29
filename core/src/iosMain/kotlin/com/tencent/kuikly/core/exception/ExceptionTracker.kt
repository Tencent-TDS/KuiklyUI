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
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.tencent.kuikly.core.exception

import com.tencent.kuikly.core.manager.BridgeManager
import kotlin.concurrent.AtomicReference

object ExceptionTracker {

    init {
        wrapUnhandledExceptionHook { throwable ->
            val info = throwable.stackTraceToString()
            BridgeManager.callExceptionMethod(info)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun wrapUnhandledExceptionHook(hook: (Throwable) -> Unit) {
        val prevHook = AtomicReference<ReportUnhandledExceptionHook?>(null)
        val wrappedHook: ReportUnhandledExceptionHook = {
            hook(it)
            prevHook.value?.invoke(it)
            terminateWithUnhandledException(it)
        }
        prevHook.value = setUnhandledExceptionHook(wrappedHook)
    }
}