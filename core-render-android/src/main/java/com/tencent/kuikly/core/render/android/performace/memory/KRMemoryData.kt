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

package com.tencent.kuikly.core.render.android.performace.memory

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import org.json.JSONObject

data class KRMemoryData(
    var initPss: Long = 0L,
    var initJavaHeap: Long = 0L,
    val pssList: MutableList<Long> = mutableListOf(),
    val javaHeapList: MutableList<Long> = mutableListOf()
) {

    companion object {
        private const val KEY_AVG_INCREMENT = "avgIncrement"
        private const val KEY_PEAK_INCREMENT = "peakIncrement"
        private const val KEY_APP_PEAK = "appPeak"
        private const val KEY_APP_AVG = "appAvg"
    }

    /**
     * 初始化内存值
     */
    fun init(pss: Long, javaHeap: Long) {
        this.initPss = pss
        this.initJavaHeap = javaHeap
    }

    fun isValid(): Boolean{
        synchronized(pssList) {
            KuiklyRenderLog.d(KRMemoryMonitor.MONITOR_NAME, "$initPss, ${pssList.size}")
            if (initPss <= 0 || pssList.isEmpty()) {
                return false
            }
        }
        synchronized(javaHeapList) {
            KuiklyRenderLog.d(KRMemoryMonitor.MONITOR_NAME, "$initJavaHeap, ${javaHeapList.size}")
            return initJavaHeap > 0 && javaHeapList.isNotEmpty()
        }
    }

    /**
     * 添加内存记录
     */
    fun record(pss: Long, javaHeap: Long) {
        synchronized(pssList) {
            pssList.add(pss)
        }
        synchronized(javaHeapList) {
            javaHeapList.add(javaHeap)
        }
    }

    /**
     * 获取 Pss 内存峰值
     */
    fun getMaxPss(): Long {
        synchronized(pssList) {
            return pssList.getMax() ?: 0
        }
    }

    /**
     * 获取 Java 堆内存峰值
     */
    fun getMaxJavaHeap(): Long = synchronized(javaHeapList) {
        javaHeapList.getMax() ?: 0
    }

    /**
     * 获取 Pss 内存增量峰值
     */
    fun getMaxPssIncrement(): Long {
        synchronized(pssList) {
            return pssList.map { it - initPss }.getMax() ?: 0
        }
    }

    /**
     * 获取 Vss 内存增量峰值
     */
    fun getMaxJavaHeapIncrement(): Long {
        synchronized(javaHeapList) {
            return javaHeapList.map { it - initJavaHeap }.getMax() ?: 0
        }
    }

    /**
     * 获取首帧内存 Pss 内存增量
     */
    fun getFirstPssIncrement(): Long {
        synchronized(pssList) {
            if (pssList.size > 0) {
                return pssList[0] - initPss
            }
            return 0
        }
    }

    /**
     * 获取首帧内存 Vss 内存增量
     */
    fun getFirstDeltaJavaHeap(): Long {
        synchronized(javaHeapList) {
            if (javaHeapList.size > 0) {
                return javaHeapList[0] - initJavaHeap
            }
            return 0
        }
    }

    /**
     * 获取平均 Pss
     */
    fun getAvgPss(): Long {
        synchronized(pssList) {
            if (pssList.size > 0) {
                return pssList.average().toLong()
            }
            return 0
        }
    }

    /**
     * 获取平均 Pss 增量
     */
    fun getAvgPssIncrement(): Long {
        synchronized(pssList) {
            if (pssList.size > 0) {
                return pssList.map { it - initPss }.average().toLong()
            }
            return 0
        }
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_AVG_INCREMENT, getAvgPssIncrement())
            put(KEY_PEAK_INCREMENT, getMaxPssIncrement())
            put(KEY_APP_PEAK, getMaxPss())
            put(KEY_APP_AVG, getAvgPss())
        }
    }

}

/**
 * 扩展 List max 函数，兼容 Kotlin 1.3
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