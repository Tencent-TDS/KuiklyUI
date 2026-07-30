package com.tencent.kuikly.core.render.web.performance

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData
import com.tencent.kuikly.core.render.web.performance.memory.KRMemoryData

data class KRPerformanceData(
    val pageName: String,
    val spentTime: Long,
    val isColdLaunch: Boolean,
    val isPageColdLaunch: Boolean,
    val executeMode: Int,
    val launchData: KRLaunchData?,
    val memoryData: KRMemoryData?,
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_MODE, executeMode)
            put(KEY_PAGE_EXIST_TIME, spentTime)
            put(KEY_IS_FIRST_PAGE_PROCESS, isColdLaunch)
            put(KEY_IS_FIRST_PAGE_LAUNCH, isPageColdLaunch)
            launchData?.let {
                put(KEY_PAGE_LOAD_TIME, it.toJSONObject())
            }
            memoryData?.let {
                put(KEY_MEMORY, it.toJSONObject())
            }
        }
    }

    override fun toString() = "[KRLaunchMeta] " +
            "pageName: $pageName, " +
            "spentTime: $spentTime, " +
            "isColdLaunch: $isColdLaunch, " +
            "executeMode: $executeMode"

    companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_PAGE_EXIST_TIME = "pageExistTime"
        private const val KEY_IS_FIRST_PAGE_PROCESS = "isFirstLaunchOfProcess"
        private const val KEY_IS_FIRST_PAGE_LAUNCH = "isFirstLaunchOfPage"
        private const val KEY_MAIN_FPS = "mainFPS"
        private const val KEY_KOTLIN_FPS = "kotlinFPS"
        private const val KEY_MEMORY = "memory"
        private const val KEY_PAGE_LOAD_TIME = "pageLoadTime"
    }
}
