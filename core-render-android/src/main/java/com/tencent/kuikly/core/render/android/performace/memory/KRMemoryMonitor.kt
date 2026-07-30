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

import android.os.Debug
import android.os.Handler
import android.os.Handler.Callback
import android.os.Message
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.performace.KRMonitor

/**
 * 内存性能追踪
 */
class KRMemoryMonitor : KRMonitor<KRMemoryData>(), Callback {
    private var isStarted = false
    private var isResumed = false
    private var dumpHandler: Handler? = null
    private var dumpMemoryCount = 0
    private var memoryData: KRMemoryData = KRMemoryData()

    override fun name(): String = MONITOR_NAME

    override fun onInit() {
        dumpHandler = Handler(getMonitorThreadLooper(), this)
        dumpHandler?.sendEmptyMessage(MSG_DUMP_INIT_MEMORY)
    }

    private fun start() {
        isStarted = true
        isResumed = true
        dumpHandler?.sendEmptyMessage(MSG_DUMP_UPDATE_MEMORY)
    }

    override fun onFirstFramePaint() {
        start()
    }

    override fun onPause() {
        if (!isStarted) {
            return
        }
        isResumed = false
        dumpHandler?.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        if (!isStarted) {
            return
        }
        isResumed = true
        dumpHandler?.sendEmptyMessageDelayed(MSG_DUMP_UPDATE_MEMORY, UPDATE_MEMORY_INTERVAL)
    }

    override fun onDestroy() {
        dumpHandler?.removeCallbacksAndMessages(null)
        dumpHandler = null
    }

    override fun getMonitorData(): KRMemoryData? {
        if (memoryData.isValid()) {
            return memoryData
        }
        return null
    }

    /**
     * 获取当前pss内存信息
     */
    private fun getPssSize(): Long {
        // 使用 AMS 获取有 5 分钟的限制，这里获取的内存不包含 Graphic
        return Debug.getPss() * BYTES_PER_KILOBYTE
    }

    /**
     * 获取当前的 Java 堆内存信息
     */
    private fun getJavaHeapSize(): Long {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    }

    override fun handleMessage(msg: Message): Boolean {
        val pssSize = getPssSize()
        val javaHeapSize = getJavaHeapSize()
        when (msg.what) {
            MSG_DUMP_INIT_MEMORY -> {
                memoryData.init(pssSize, javaHeapSize)
                KuiklyRenderLog.d(TAG, "initMemory, pssSize: $pssSize, javaHeapSize: $javaHeapSize")
            }

            MSG_DUMP_UPDATE_MEMORY -> {
                if (isResumed) {
                    memoryData.record(pssSize, javaHeapSize)
                    dumpMemoryCount++
                    KuiklyRenderLog.d(
                        TAG,
                        "dumpMemory[$dumpMemoryCount], pssSize: $pssSize, javaHeapSize: $javaHeapSize"
                    )
                    if (dumpMemoryCount < MAX_DUMP_MEMORY_COUNT) {
                        dumpHandler?.sendEmptyMessageDelayed(
                            MSG_DUMP_UPDATE_MEMORY,
                            UPDATE_MEMORY_INTERVAL
                        )
                    }
                }
            }
        }
        return true
    }

    companion object {
        const val MONITOR_NAME = "KRMemoryMonitor"
        private const val TAG = MONITOR_NAME

        private const val BYTES_PER_KILOBYTE = 1024

        private const val MSG_DUMP_INIT_MEMORY = 0
        private const val MSG_DUMP_UPDATE_MEMORY = 1

        private const val MAX_DUMP_MEMORY_COUNT = 10
        private const val UPDATE_MEMORY_INTERVAL = 10 * 1000L
    }
}