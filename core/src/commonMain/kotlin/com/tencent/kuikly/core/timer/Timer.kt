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

package com.tencent.kuikly.core.timer

import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.coroutines.*
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.manager.BridgeManager
/**
 * @brief Timer等价Android的Timer类功能(定时器)。
 */
class Timer {
    private var isRunning = false
    private lateinit var action: () -> Unit
    private var delay: Int = 0
    private var period: Int = 0
    /**
     * @brief schedule()方法用于启动定时器任务。
     * @param delay 定时器任务首次执行的延迟时间，单位为毫秒。
     * @param period 定时器任务执行的周期时间，单位为毫秒。
     * @param action 定时器任务处理逻辑，是一个无参数、无返回值的函数。
     */
    fun schedule(delay: Int, period: Int, action: () -> Unit) {
        this.delay = delay
        this.period = period
        this.action = action
        start()
    }
    /**
     * @brief  cancel()方法用于取消定时器任务。
     */
    fun cancel() {
        isRunning = false
    }

    private fun start() {
        if (isRunning) return
        isRunning = true
        GlobalScope.launch {
            delay(delay)
            while (isRunning) {
                action()
                delay(period)
            }
        }
    }

}

typealias CallbackRef = String
/**
 * @brief 延时调用任务Api
 * @param delay 延时调用时间，单位为毫秒。
 * @return 当前延时任务ID，用于配合cancelPostCallback接口实现取消该延时任务
 */
@Deprecated("Use PagerScope.setTimeout(timeout, callback) instead")
fun postDelayed(delay: Int, callback: () -> Unit): CallbackRef {
    return setTimeout(callback, delay)
}
/*
 * @brief 取消延时任务Api
 */
@Deprecated("Use PagerScope.clearTimeout(timeoutRef) instead")
fun cancelPostCallback(callbackRef: CallbackRef) {
    clearTimeout(callbackRef)
}

@Deprecated(
    "Use setTimeout(pagerId, timeout, callback) instead",
    ReplaceWith("setTimeout(pagerId, timeout, callback)")
)
inline fun setTimeout(pagerId: String, noinline callback: () -> Unit, timeout: Int = 0) =
    setTimeout(pagerId, timeout, callback)

fun setTimeout(pagerId: String, timeout: Int = 0, callback: () -> Unit): String {
    val callbackRef = GlobalFunctions.createFunction(pagerId) { res ->
        callback()
        false
    }
    BridgeManager.setTimeout(pagerId, timeout.toFloat(), callbackRef)
    return callbackRef
}

@Deprecated("Use PagerScope.setTimeout(timeout, callback) instead")
inline fun setTimeout(noinline callback: () -> Unit, timeout: Int = 0): String =
    setTimeout(BridgeManager.currentPageId, timeout, callback)

@Deprecated("Use PagerScope.setTimeout(timeout, callback) instead")
inline fun setTimeout(timeout: Int = 0, noinline callback: () -> Unit): String =
    setTimeout(BridgeManager.currentPageId, timeout, callback)

fun PagerScope.setTimeout(timeout: Int = 0, callback: () -> Unit): String {
    // 用currentPageId兜底，以保持向前兼容
    val pagerId = this.pagerId.ifEmpty { BridgeManager.currentPageId }
    return setTimeout(pagerId, timeout, callback)
}

@Deprecated("Use PagerScope.clearTimeout(timeoutRef) instead")
fun clearTimeout(timeoutRef: String) {
    GlobalFunctions.destroyGlobalFunction(BridgeManager.currentPageId, timeoutRef)
}

fun PagerScope.clearTimeout(timeoutRef: String) {
    // 用currentPageId兜底，以保持向前兼容
    val pagerId = this.pagerId.ifEmpty { BridgeManager.currentPageId }
    GlobalFunctions.destroyGlobalFunction(pagerId, timeoutRef)
}