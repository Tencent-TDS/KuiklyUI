package com.tencent.kuikly.core.render.web.performance

import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode
import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchMonitor
import com.tencent.kuikly.core.render.web.performance.memory.KRMemoryData
import com.tencent.kuikly.core.render.web.performance.memory.KRMemoryMonitor
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Date

/**
 * Performance monitoring types
 */
enum class KRMonitorType {
    // Launch monitoring
    LAUNCH,

    // FPS monitoring
    FRAME,

    // Memory monitoring
    MEMORY
}

/**
 * Performance monitoring manager
 */
class KRPerformanceManager(
    private val pageName: String,
    private val executeMode: KuiklyRenderCoreExecuteMode,
    monitorTypes: List<KRMonitorType>
) : IKuiklyRenderViewLifecycleCallback {
    private val monitors = mutableListOf<KRMonitor<*>>()

    private var dataCallback: IKRMonitorCallback? = null

    private var initTimeStamps = 0L
    private var isColdLaunch = false
    private var isPageColdLaunch = false

    init {
        monitorTypes.forEach { type ->
            when (type) {
                KRMonitorType.LAUNCH -> monitors.add(KRLaunchMonitor().apply {
                    addListener {
                        dataCallback?.onLaunchResult(it)
                    }
                })

                KRMonitorType.FRAME -> {
                    // Frame monitoring not supported in web yet
                    Log.log("Frame monitoring not supported yet")
                    // monitors.add(KRFrameMonitor())
                }

                KRMonitorType.MEMORY -> {
                    monitors.add(KRMemoryMonitor())
                }
            }
        }
        if (!pageRecords.contains(pageName)) {
            pageRecords.add(pageName)
            isPageColdLaunch = true
        }
    }

    fun <T : KRMonitor<*>> getMonitor(name: String): T? =
        monitors.find { it.name() == name }.unsafeCast<T?>()

    fun setMonitorCallback(dataCallback: IKRMonitorCallback) {
        this.dataCallback = dataCallback
    }

    override fun onInit() {
        Log.log(TAG, "--onInit--")
        initTimeStamps = Date.now().toLong()
        if (sIsColdLaunch) {
            isColdLaunch = true
            sIsColdLaunch = false
        }
        monitors.forEach {
            it.onInit()
        }
    }

    override fun onPreloadDexClassFinish() {
        Log.log(TAG, "--onPreloadDexClassFinish--")
        monitors.forEach {
            it.onPreloadDexClassFinish()
        }
    }

    override fun onInitCoreStart() {
        Log.log(TAG, "--onRenderCoreInitStart--")
        monitors.forEach {
            it.onInitCoreStart()
        }
    }

    override fun onInitCoreFinish() {
        Log.log(TAG, "--onRenderCoreInitFinish--")
        monitors.forEach {
            it.onInitCoreFinish()
        }
    }

    override fun onInitContextStart() {
        Log.log(TAG, "--onRenderContextInitStart--")
        monitors.forEach {
            it.onInitContextStart()
        }
    }

    override fun onInitContextFinish() {
        Log.log(TAG, "--onRenderContextInitFinish--")
        monitors.forEach {
            it.onInitContextFinish()
        }
    }

    override fun onCreateInstanceStart() {
        Log.log(TAG, "--onCreatePageStart--")
        monitors.forEach {
            it.onCreateInstanceStart()
        }
    }

    override fun onCreateInstanceFinish() {
        Log.log(TAG, "--onRenderPageFinish--")
        monitors.forEach {
            it.onCreateInstanceFinish()
        }
    }

    override fun onResume() {
        Log.log(TAG, "--onResume--")
        monitors.forEach {
            it.onResume()
        }
    }

    override fun onFirstFramePaint() {
        Log.log(TAG, "--onFirstFramePaint--")
        monitors.forEach {
            it.onFirstFramePaint()
        }
    }

    override fun onPause() {
        Log.log(TAG, "--onPause--")
        monitors.forEach {
            it.onPause()
        }
    }

    override fun onDestroy() {
        Log.log(TAG, "--onDestroy--")
        val performanceData = getPerformanceData()
        dataCallback?.onResult(performanceData)
        monitors.forEach {
            it.onDestroy()
        }
    }

    override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {
        Log.log(TAG, "--onRenderException--")
        monitors.forEach {
            it.onRenderException(throwable, errorReason)
        }
    }

    /**
     * Collect all performance data
     */
    fun getPerformanceData(): KRPerformanceData {
        val launchData = getLaunchData()
        val memoryData = getMemoryData()
        val spentTime = Date.now().toLong() - initTimeStamps
        return KRPerformanceData(
            pageName,
            spentTime,
            isColdLaunch,
            isPageColdLaunch,
            executeMode.mode,
            launchData,
            memoryData,
        )
    }

    /**
     * Get launch data
     */
    private fun getLaunchData(): KRLaunchData? =
        getMonitor<KRLaunchMonitor>(KRLaunchMonitor.MONITOR_NAME)?.getMonitorData()

    /**
     * Get memory data
     */
    private fun getMemoryData(): KRMemoryData? =
        getMonitor<KRMemoryMonitor>(KRMemoryMonitor.MONITOR_NAME)?.getMonitorData()

    companion object {
        private const val TAG = "KRPerformanceManager"
        private var sIsColdLaunch = true

        // Used to track if page is opened for the first time
        private val pageRecords = mutableListOf<String>()
    }
}

interface IKRMonitorCallback {
    /**
     * Callback for launch data
     */
    fun onLaunchResult(data: KRLaunchData)

    /**
     * Callback for all performance data
     */
    fun onResult(data: KRPerformanceData)
}
