package com.tencent.kuikly.core.render.web.performance.memory

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Memory data class
 */
data class KRMemoryData(
    var initPss: Long = 0L,
    var initHeap: Long = 0L,
    val pssList: MutableList<Long> = mutableListOf(),
    val heapList: MutableList<Long> = mutableListOf()
) {
    /**
     * Initialize memory values
     */
    fun init(pss: Long, heap: Long) {
        this.initPss = pss
        this.initHeap = heap
    }

    fun isValid(): Boolean {
        val isValid = initHeap > 0 && pssList.isNotEmpty() && heapList.isNotEmpty()

        if (!isValid) {
            // Log invalid data
            Log.log(
                KRMemoryMonitor.MONITOR_NAME,
                "$initPss, $initHeap, ${pssList.size}, ${heapList.size}"
            )
        }

        return isValid
    }

    /**
     * Add memory record
     */
    fun record(pss: Long, javaHeap: Long) {
        pssList.add(pss)
        heapList.add(javaHeap)
    }

    /**
     * Get peak PSS memory
     */
    fun getMaxPss(): Long = pssList.getMax() ?: 0

    /**
     * Get peak heap memory
     */
    fun getMaxHeap(): Long = heapList.getMax() ?: 0

    /**
     * Get peak PSS memory increment
     */
    fun getMaxPssIncrement(): Long = pssList.map { it - initPss }.getMax() ?: 0

    /**
     * Get peak VSS memory increment
     */
    fun getMaxHeapIncrement(): Long = heapList.map { it - initHeap }.getMax() ?: 0

    /**
     * Get first frame PSS memory increment
     */
    fun getFirstPssIncrement(): Long {
        if (pssList.size > 0) {
            return pssList[0] - initPss
        }
        return 0
    }

    /**
     * Get first frame VSS memory increment
     */
    fun getFirstDeltaJavaHeap(): Long {
        if (heapList.size > 0) {
            return heapList[0] - initHeap
        }
        return 0
    }

    /**
     * Get average PSS
     */
    fun getAvgPss(): Long {
        if (pssList.size > 0) {
            return pssList.average().toLong()
        }
        return 0
    }

    /**
     * Get average PSS increment
     */
    fun getAvgPssIncrement(): Long {
        if (pssList.size > 0) {
            return pssList.map { it - initPss }.average().toLong()
        }
        return 0
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_AVG_INCREMENT, getAvgPssIncrement())
            put(KEY_PEAK_INCREMENT, getMaxPssIncrement())
            put(KEY_APP_PEAK, getMaxPss())
            put(KEY_APP_AVG, getAvgPss())
        }
    }

    companion object {
        private const val KEY_AVG_INCREMENT = "avgIncrement"
        private const val KEY_PEAK_INCREMENT = "peakIncrement"
        private const val KEY_APP_PEAK = "appPeak"
        private const val KEY_APP_AVG = "appAvg"
    }
}

/**
 * Extend List max function for Kotlin 1.3 compatibility
 */
fun <T : Comparable<T>> Iterable<T>.getMax(): T? {
    val iterator = iterator()
    if (!iterator.hasNext()) return null
    var max = iterator.next()
    while (iterator.hasNext()) {
        val e = iterator.next()
        if (max < e) max = e
    }
    return max
}
