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
package com.tencent.kuikly.compose.coroutines.internal

import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.timer.setTimeout

private const val UNDEFINED = "undefined"

private object KuiklyTimeoutHandler {
    private var handleCounter = 0
    private val handleMap = mutableMapOf<Int, Pair<String, String>>()

    fun setTimeout(handler: () -> Unit, timeout: Int): Int {
        return handleCounter.inc().also { handle ->
            // since pagers are isolated in js mode, it's safe to use currentPageId here
            val pagerId = BridgeManager.currentPageId
            handleMap[handle] = Pair(pagerId, setTimeout(pagerId, timeout) {
                handleMap.remove(handle)
                handler()
            })
        }
    }

    fun clearTimeout(handle: Int) {
        val (pagerId, timeoutRef) = handleMap.remove(handle) ?: return
        // since kuikly-core not offer clearTimeout with pagerId api,
        // we call their internal implementation directly
        GlobalFunctions.destroyGlobalFunction(pagerId, timeoutRef)
    }
}

internal actual fun platformInitScheduler() {
    // check and hook setTimeout
    if (jsTypeOf(js("setTimeout")) == UNDEFINED) {
        val global = js("global")
        if (jsTypeOf(global) != UNDEFINED) {
            global.setTimeout = KuiklyTimeoutHandler::setTimeout
            global.clearTimeout = KuiklyTimeoutHandler::clearTimeout
        }
    }
}

internal actual inline fun platformIsOnKuiklyThread(pagerId: String): Boolean = true

internal actual inline fun platformScheduleOnKuiklyThread(pagerId: String) {
    setTimeout(pagerId, 0) {
        KuiklyContextScheduler.runTask(pagerId)
    }
}

internal actual inline fun platformNotifyKuiklyException(t: Throwable) {
    BridgeManager.callExceptionMethod(t.stackTraceToString())
}