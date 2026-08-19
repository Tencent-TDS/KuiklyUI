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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.nvi.serialization.json.JSONTokener
import kotlin.js.Date

private const val TOKENER_COMMON = "JSONTokener(common)"

actual fun parseWithTokener(json: String): Any? = JSONTokener(json).nextValue()

actual fun tokenerVariants(): List<TokenerVariant> = listOf(
    TokenerVariant(TOKENER_COMMON) { JSONTokener(it).nextValue() }
)

actual fun currentTokenerName(): String = TOKENER_COMMON

actual fun selectTokener(name: String) {
    // 只有一份实现，无可切换
}

actual fun collectGarbage() {
    // JS: no-op, GC is managed by the JS engine
}

actual fun getAllocatedBytes(): Long {
    // JS: performance.memory not reliably available across all JS hosts
    return -1
}

actual fun getGcEpoch(): Long {
    // JS: GC is opaque to the program
    return -1
}

actual fun getHeapObjectBytes(): Long = -1

actual fun getLastSweptCount(): Long = -1

actual fun getGcPoolsDebug(): String = "unavailable on JS"

actual fun setGcSuspended(suspended: Boolean) {
    // JS: GC is opaque
}

actual fun getPlatformName(): String {
    return "JS (Browser/MiniApp)"
}

actual fun getTimeNanosPlatform(): Long {
    // JS 上没有 kotlin.system.getTimeNanos()，用 Date.now() 的毫秒精度近似。
    return (Date.now()).toLong() * 1_000_000L
}
