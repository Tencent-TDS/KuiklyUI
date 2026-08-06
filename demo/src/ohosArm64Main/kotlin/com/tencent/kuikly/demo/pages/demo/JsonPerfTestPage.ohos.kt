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

import com.tencent.kuikly.core.nvi.serialization.json.JSONEngine
import com.tencent.kuikly.core.nvi.serialization.json.JSONTokener
import com.tencent.kuikly.core.nvi.serialization.json.JSONTokenerOhos
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.time.TimeSource

private const val TOKENER_OPT = "JSONTokenerOhos(opt)"
private const val TOKENER_LEGACY = "JSONTokener(legacy)"

// 跟随 JSONEngine 的开关，这样基准跑的就是当前生效的那一份实现。
actual fun parseWithTokener(json: String): Any? = JSONEngine.parse(json)

actual fun tokenerVariants(): List<TokenerVariant> = listOf(
    TokenerVariant(TOKENER_OPT) { JSONTokenerOhos(it).nextValue() },
    TokenerVariant(TOKENER_LEGACY) { JSONTokener(it).nextValue() }
)

actual fun currentTokenerName(): String =
    if (JSONEngine.useOptimizedTokener) TOKENER_OPT else TOKENER_LEGACY

actual fun selectTokener(name: String) {
    when (name) {
        TOKENER_OPT -> JSONEngine.useOptimizedTokener = true
        TOKENER_LEGACY -> JSONEngine.useOptimizedTokener = false
    }
}

@OptIn(NativeRuntimeApi::class)
actual fun collectGarbage() {
    GC.collect()
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun getAllocatedBytes(): Long {
    // Kotlin 2.0+ 移除了 GC.allocatedBytes，改用最近一次 GC 后各池 totalObjectsSizeBytes 之和。
    val info = GC.lastGCInfo ?: return -1L
    val usage = info.memoryUsageAfter
    if (usage.isEmpty()) return -1L
    var total = 0L
    for (entry in usage.values) {
        total += entry.totalObjectsSizeBytes
    }
    return total
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun getGcEpoch(): Long {
    return GC.lastGCInfo?.epoch ?: -1L
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun getHeapObjectBytes(): Long {
    val info = GC.lastGCInfo ?: return -1L
    val usage = info.memoryUsageAfter
    if (usage.isEmpty()) return -1L
    var total = 0L
    for (entry in usage.values) {
        total += entry.totalObjectsSizeBytes
    }
    return total
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun getLastSweptCount(): Long {
    val info = GC.lastGCInfo ?: return -1L
    val sweep = info.sweepStatistics
    if (sweep.isEmpty()) return -1L
    var total = 0L
    for (entry in sweep.values) {
        total += entry.sweptCount
    }
    return total
}

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
actual fun getGcPoolsDebug(): String {
    val info = GC.lastGCInfo ?: return "lastGCInfo=null"
    val mem = info.memoryUsageAfter.entries.joinToString { "${it.key}=${it.value.totalObjectsSizeBytes}" }
    val sweep = info.sweepStatistics.entries.joinToString {
        "${it.key}:swept=${it.value.sweptCount},kept=${it.value.keptCount}"
    }
    return "epoch=${info.epoch} memAfter=[$mem] sweep=[$sweep]"
}

@OptIn(NativeRuntimeApi::class)
actual fun setGcSuspended(suspended: Boolean) {
    // Kotlin 2.0+ 用 suspend()/resume() 替代 suspendGCToggle。
    if (suspended) {
        GC.suspend()
    } else {
        GC.resume()
    }
}

actual fun getPlatformName(): String {
    return "K/N OHOS (HarmonyOS)"
}

actual fun getTimeNanosPlatform(): Long {
    return TimeSource.Monotonic.markNow().elapsedNow().inWholeNanoseconds
}
