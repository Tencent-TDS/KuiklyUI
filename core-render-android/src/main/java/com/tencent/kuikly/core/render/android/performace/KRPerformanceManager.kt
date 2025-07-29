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

package com.tencent.kuikly.core.render.android.performace

import com.tencent.kuikly.core.render.android.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.performace.frame.KRFrameData
import com.tencent.kuikly.core.render.android.performace.frame.KRFrameMonitor
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchMonitor
import com.tencent.kuikly.core.render.android.performace.memory.KRMemoryData
import com.tencent.kuikly.core.render.android.performace.memory.KRMemoryMonitor

/**
 * 性能监控类型
 */
enum class KRMonitorType {
    LAUNCH, // 启动监控
    FRAME,  // FPS监控
    MEMORY  // 内存监控
}

/**
 * 性能监控管理
 */
class KRPerformanceManager(
    private val pageName: String,
    private val executeMode: KuiklyRenderCoreExecuteModeBase,
    monitorTypes: List<KRMonitorType>
) : IKuiklyRenderViewLifecycleCallback {

    private val monitors = mutableListOf<KRMonitor<*>>()

    private var dataCallback: IKRMonitorCallback? = null

    private var initTimeStamps = 0L
    private var isColdLaunch = false
    private var isPageColdLaunch = false

    companion object {
        private const val TAG = "KRPerformanceManager"
        private var sIsColdLaunch = true
        // 用于记录页面是否首次打开
        private val pageRecords = mutableListOf<String>()
    }

    init {
        monitorTypes.forEach { type ->
            when (type) {
                KRMonitorType.LAUNCH -> monitors.add(KRLaunchMonitor().apply {
                    addListener {
                        dataCallback?.onLaunchResult(it)
                    }
                })
                KRMonitorType.FRAME -> monitors.add(KRFrameMonitor())
                KRMonitorType.MEMORY -> monitors.add(KRMemoryMonitor())
            }
        }
        if (!pageRecords.contains(pageName)) {
            pageRecords.add(pageName)
            isPageColdLaunch = true
        }
    }

    fun <T : KRMonitor<*>> getMonitor(name: String): T? {
        return monitors.find { it.name() == name } as? T
    }

    fun setMonitorCallback(dataCallback: IKRMonitorCallback) {
        this.dataCallback = dataCallback
    }

    override fun onInit() {
        KuiklyRenderLog.d(TAG, "--onInit--")
        initTimeStamps = System.currentTimeMillis()
        if (sIsColdLaunch) {
            isColdLaunch = true
            sIsColdLaunch = false
        }
        monitors.forEach {
            it.onInit()
        }
    }

    override fun onPreloadClassFinish() {
        KuiklyRenderLog.d(TAG, "--onPreloadClassFinish--")
        monitors.forEach {
            it.onPreloadClassFinish()
        }
    }

    override fun onInitCoreStart() {
        KuiklyRenderLog.d(TAG, "--onRenderCoreInitStart--")
        monitors.forEach {
            it.onInitCoreStart()
        }
    }

    override fun onInitCoreFinish() {
        KuiklyRenderLog.d(TAG, "--onRenderCoreInitFinish--")
        monitors.forEach {
            it.onInitCoreFinish()
        }
    }

    override fun onInitContextStart() {
        KuiklyRenderLog.d(TAG, "--onRenderContextInitStart--")
        monitors.forEach {
            it.onInitContextStart()
        }
    }

    override fun onInitContextFinish() {
        KuiklyRenderLog.d(TAG, "--onRenderContextInitFinish--")
        monitors.forEach {
            it.onInitContextFinish()
        }
    }

    override fun onCreateInstanceStart() {
        KuiklyRenderLog.d(TAG, "--onCreatePageStart--")
        monitors.forEach {
            it.onCreateInstanceStart()
        }
    }

    override fun onCreateInstanceFinish() {
        KuiklyRenderLog.d(TAG, "--onRenderPageFinish--")
        monitors.forEach {
            it.onCreateInstanceFinish()
        }
    }

    override fun onResume() {
        KuiklyRenderLog.d(TAG, "--onResume--")
        monitors.forEach {
            it.onResume()
        }
    }

    override fun onFirstFramePaint() {
        KuiklyRenderLog.d(TAG, "--onFirstFramePaint--")
        monitors.forEach {
            it.onFirstFramePaint()
        }
    }

    override fun onPause() {
        KuiklyRenderLog.d(TAG, "--onPause--")
        monitors.forEach {
            it.onPause()
        }
    }

    override fun onDestroy() {
        KuiklyRenderLog.d(TAG, "--onDestroy--")
        val performanceData = getPerformanceData()
        dataCallback?.onResult(performanceData)
        monitors.forEach {
            it.onDestroy()
        }
    }

    override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {
        KuiklyRenderLog.d(TAG, "--onRenderException--")
        monitors.forEach {
            it.onRenderException(throwable, errorReason)
        }
    }

    /**
     * 收集所有性能数据
     */
    fun getPerformanceData(): KRPerformanceData {
        val launchData = getLaunchData()
        val frameData = getFrameDate()
        val memoryData = getMemoryData()
        val spentTime = System.currentTimeMillis() - initTimeStamps
        return KRPerformanceData(
            pageName,
            spentTime,
            isColdLaunch,
            isPageColdLaunch,
            executeMode.mode,
            launchData,
            frameData,
            memoryData
        )
    }

    /**
     * 获取启动数据
     */
    fun getLaunchData(): KRLaunchData? = getMonitor<KRLaunchMonitor>(KRLaunchMonitor.MONITOR_NAME)?.getMonitorData()

    /**
     * 获取帧数据
     */
    fun getFrameDate(): KRFrameData? = getMonitor<KRFrameMonitor>(KRFrameMonitor.MONITOR_NAME)?.getMonitorData()

    /**
     * 获取内存数据
     */
    fun getMemoryData(): KRMemoryData? = getMonitor<KRMemoryMonitor>(KRMemoryMonitor.MONITOR_NAME)?.getMonitorData()

}

interface IKRMonitorCallback {

    /**
     * 回调启动数据
     */
    fun onLaunchResult(data: KRLaunchData)

    /**
     * 回调所有性能数据
     */
    fun onResult(data: KRPerformanceData)

}