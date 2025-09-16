package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.performance.KRPerformanceManager
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchMonitor

/**
 * Performance monitoring module
 */
class KRPerformanceModule(
    private val performanceManager: KRPerformanceManager?
) : KuiklyRenderBaseModule() {
    // Kuikly page performance data
    private var pageTraceData: PageCreateTrace? = null

    /**
     * Page creation completed, pass kuikly side page performance data to combine with host side platform performance data
     */
    private fun onCreatePageFinish(jsonString: String?) {
        jsonString?.let {
            // Convert page performance data
            pageTraceData = PageCreateTrace(it)
            // Insert launch related data into performance manager
            performanceManager?.getMonitor<KRLaunchMonitor>(KRLaunchMonitor.MONITOR_NAME)
                ?.onPageCreateFinish(pageTraceData)
        }
    }

    /**
     * Get performance data
     */
    private fun getPerformanceData(callback: KuiklyRenderCallback?) {
        // Get current performance data
        val performanceData = performanceManager?.getPerformanceData()
        // Callback current page performance data
        callback?.invoke(performanceData?.toJSONObject())
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_ON_CREATE_PAGE_FINISH -> onCreatePageFinish(params)
            METHOD_GET_PERFORMANCE_DATA -> getPerformanceData(callback)
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRPerformanceModule"
        private const val METHOD_ON_CREATE_PAGE_FINISH = "onPageCreateFinish"
        private const val METHOD_GET_PERFORMANCE_DATA = "getPerformanceData"
    }
}

/**
 * Record time-consuming stages of page creation process
 */
class PageCreateTrace(jsonStr: String) {
    val createStartTimeMills: Long
    val newPageStartTimeMills: Long
    val newPageEndTimeMills: Long
    val buildStartTimeMills: Long
    val buildEndTimeMills: Long
    val layoutStartTimeMills: Long
    val layoutEndTimeMills: Long
    val createEndTimeMills: Long

    init {
        val jsonObject = jsonStr.toJSONObjectSafely()
        createStartTimeMills = jsonObject.optLong(EVENT_ON_CREATE_START, -1)
        newPageStartTimeMills = jsonObject.optLong(EVENT_ON_NEW_PAGE_START, -1)
        newPageEndTimeMills = jsonObject.optLong(EVENT_ON_NEW_PAGE_END, -1)
        buildStartTimeMills = jsonObject.optLong(EVENT_ON_BUILD_START, -1)
        buildEndTimeMills = jsonObject.optLong(EVENT_ON_BUILD_END, -1)
        layoutStartTimeMills = jsonObject.optLong(EVENT_ON_LAYOUT_START, -1)
        layoutEndTimeMills = jsonObject.optLong(EVENT_ON_LAYOUT_END, -1)
        createEndTimeMills = jsonObject.optLong(EVENT_ON_CREATE_END, -1)
    }

    override fun toString(): String = "[PageCreateTrace] " +
            "onCreateStartTimeMills: $createStartTimeMills \n" +
            "onCreateEndTimeMills: $createEndTimeMills \n" +
            "newPageStartTimeMills: $newPageStartTimeMills \n" +
            "newPageEndTimeMills: $newPageEndTimeMills \n" +
            "onBuildStartTimeMills: $buildStartTimeMills \n" +
            "onBuildEndTimeMills: $buildEndTimeMills \n" +
            "onLayoutStartTimeMills: $layoutStartTimeMills \n" +
            "onLayoutEndTimeMills: $layoutEndTimeMills"

    companion object {
        private const val EVENT_ON_CREATE_START = "on_create_start"
        private const val EVENT_ON_NEW_PAGE_START = "on_new_page_start"
        private const val EVENT_ON_NEW_PAGE_END = "on_new_page_end"
        private const val EVENT_ON_BUILD_START = "on_build_start"
        private const val EVENT_ON_BUILD_END = "on_build_end"
        private const val EVENT_ON_LAYOUT_START = "on_layout_start"
        private const val EVENT_ON_LAYOUT_END = "on_layout_end"
        private const val EVENT_ON_CREATE_END = "on_create_end"
    }
}
