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

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlin.native.runtime.NativeRuntimeApi::class, kotlin.ExperimentalStdlibApi::class)

package com.tencent.kuikly.demo.pages.base

import kotlin.native.runtime.GC
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// ========== 线程安全说明 ==========
// 以下状态变量（isGCSuspended、lastSuspendTimeMark 等）未使用锁或原子操作进行同步保护。
// 这是因为 suspendGC()/resumeGC() 仅在 Kuikly 主线程中被调用（由 List 的 dragBegin/scrollEnd 回调触发），
// 不存在多线程并发访问的场景，因此无需额外的同步开销。
// 如果后续需要在多线程环境中调用，需要引入 AtomicReference 或其他同步机制。
// ==================================

// GC suspend 状态追踪
private var isGCSuspended = false
// 上次 suspend 的时间标记，用于频率控制
private var lastSuspendTimeMark = TimeSource.Monotonic.markNow()
private var lastSuspendTimeInitialized = false
// 最小 suspend 间隔（毫秒），防止过于频繁的 suspend/resume 切换
private const val MIN_SUSPEND_INTERVAL_MS = 500L

// 使用 @EagerInitialization 在库加载时自动执行 configureGC()
@EagerInitialization
private val gcInitializer = configureGC()

actual fun configureGC() {
    GC.regularGCInterval = 10.seconds
    GC.targetHeapBytes = 256L * 1024 * 1024
    GC.minHeapBytes = 256L * 1024 * 1024
    GC.targetHeapUtilization = 0.65
    // heapGrowthUseAllocatedBytes 在当前 Kotlin 版本不支持，已注释
    // GC.heapGrowthUseAllocatedBytes = true
}

actual fun suspendGC() {
    if (isGCSuspended) return
    if (lastSuspendTimeInitialized) {
        val elapsed = lastSuspendTimeMark.elapsedNow().inWholeMilliseconds
        if (elapsed < MIN_SUSPEND_INTERVAL_MS) return
    }
    lastSuspendTimeMark = TimeSource.Monotonic.markNow()
    lastSuspendTimeInitialized = true
    isGCSuspended = true
    GC.suspend()
}

actual fun resumeGC() {
    if (!isGCSuspended) return
    isGCSuspended = false
    GC.resume()
}
