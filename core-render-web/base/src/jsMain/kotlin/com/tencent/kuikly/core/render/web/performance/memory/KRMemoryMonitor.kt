package com.tencent.kuikly.core.render.web.performance.memory

import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.performance.KRMonitor
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Memory performance monitoring
 */
class KRMemoryMonitor : KRMonitor<KRMemoryData>() {
    private var isStarted = false
    private var isResumed = false
    private var dumpMemoryCount = 0
    private var memoryData: KRMemoryData = KRMemoryData()

    override fun name(): String = MONITOR_NAME

    override fun onInit() {
        // Record initial memory data
        recordMemoryData(true)
    }

    private fun start() {
        isStarted = true
        isResumed = true
        // Record memory update data
        recordMemoryData()
    }

    override fun onFirstFramePaint() {
        start()
    }

    override fun onPause() {
        if (!isStarted) {
            return
        }
        isResumed = false
    }

    override fun onResume() {
        if (!isStarted) {
            return
        }
        isResumed = true
        // Record memory update data after page resume
        recordMemoryData()
    }

    override fun onDestroy() {}

    /**
     * Get recorded memory data
     */
    override fun getMonitorData(): KRMemoryData? {
        if (memoryData.isValid()) {
            return memoryData
        }
        return null
    }

    /**
     * Get current PSS memory information. Not supported in Web
     */
    private fun getPssSize(): Long = 0L

    /**
     * Get current web heap memory information
     */
    private fun getUsedHeapSize(): Long {
        val heapSize = kuiklyWindow.performance.asDynamic().memory.usedJSHeapSize
        return heapSize.unsafeCast<Number>().toLong()
    }

    /**
     * Record memory data usage
     */
    private fun recordMemoryData(isInit: Boolean = false) {
        // Skip if memory property is not supported on iOS
        if (jsTypeOf(kuiklyWindow.performance.asDynamic().memory) != "undefined") {
            val pssSize = getPssSize()
            val usedHeapSize = getUsedHeapSize()
            if (isInit) {
                memoryData.init(pssSize, usedHeapSize)
                Log.log(TAG, "initMemory, pssSize: $pssSize, heapSize: $usedHeapSize")
            } else {
                Log.log(
                    TAG,
                    "dumpMemory[$dumpMemoryCount], pssSize: $pssSize, heapSize: $usedHeapSize"
                )
                memoryData.record(pssSize, usedHeapSize)
            }
        }
    }

    companion object {
        const val MONITOR_NAME = "KRMemoryMonitor"
        private const val TAG = MONITOR_NAME
    }
}
