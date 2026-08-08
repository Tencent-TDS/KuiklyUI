package com.tencent.kuikly.core.performance

import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(NativeRuntimeApi::class, ExperimentalStdlibApi::class)
fun getNativeHeapSize(): Long {
    val memoryMap = GC.lastGCInfo?.memoryUsageAfter
    val nativeHeapSize = if (memoryMap != null) {
        var size : Long = 0
        for (entry in memoryMap) {
            val bytes = entry.value.totalObjectsSizeBytes
            size += entry.value.totalObjectsSizeBytes
        }
        size
    } else {
        0
    }
    return nativeHeapSize
}