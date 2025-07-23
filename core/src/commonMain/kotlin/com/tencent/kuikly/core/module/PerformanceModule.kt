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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.base.ExecuteMode
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.PageCreateTrace

/**
 * 性能监控，用于与 Native 的相关通信
 */
class PerformanceModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    fun getPerformanceData(callback: (PerformanceData?) -> Unit) {
        toNative(
            keepCallbackAlive = false,
            methodName = METHOD_GET_PERFORMANCE_DATA,
            syncCall = false,
            param = null,
            callback = {
                callback(PerformanceData.fromJson(it))
            }
        )
    }

    /**
     * 通知 Native 页面已创建完成
     */
    internal fun onPageCreateFinish(trace: PageCreateTrace) {
        toNative(
            keepCallbackAlive = false,
            methodName = METHOD_ON_PAGE_CREATE_FINISH,
            syncCall = false,
            param = trace.dump()
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.PERFORMANCE

        const val METHOD_ON_PAGE_CREATE_FINISH = "onPageCreateFinish"
        const val METHOD_GET_PERFORMANCE_DATA = "getPerformanceData"
    }
}

data class PerformanceData(
    val pageExistTime: Int,
    val mode: ExecuteMode,
    val isFirstLaunchOfProcess: Boolean,
    val isFirstLaunchOfPage: Boolean,
    val pageLoadTime: LaunchData?,
    val mainFPS: Int?,
    val kotlinFPS: Int?,
    val memory: MemoryData?
) {

    companion object {
        private const val KEY_MODE = "mode"
        private const val KEY_PAGE_EXIST_TIME = "pageExistTime"
        private const val KEY_IS_FIRST_PAGE_PROCESS = "isFirstLaunchOfProcess"
        private const val KEY_IS_FIRST_PAGE_LAUNCH = "isFirstLaunchOfPage"
        private const val KEY_MAIN_FPS = "mainFPS"
        private const val KEY_KOTLIN_FPS = "kotlinFPS"
        private const val KEY_MEMORY = "memory"
        private const val KEY_PAGE_LOAD_TIME = "pageLoadTime"

        fun fromJson(jsonObject: JSONObject?) : PerformanceData? {
            if (jsonObject != null) {
                return PerformanceData(
                    mode = ExecuteMode.fromInt(jsonObject.optInt(KEY_MODE)),
                    pageExistTime = jsonObject.optInt(KEY_PAGE_EXIST_TIME),
                    isFirstLaunchOfProcess = jsonObject.optBoolean(KEY_IS_FIRST_PAGE_PROCESS),
                    isFirstLaunchOfPage = jsonObject.optBoolean(KEY_IS_FIRST_PAGE_LAUNCH),
                    pageLoadTime = LaunchData.fromJson(jsonObject.optJSONObject(KEY_PAGE_LOAD_TIME)),
                    mainFPS = jsonObject.optInt(KEY_MAIN_FPS),
                    kotlinFPS = jsonObject.optInt(KEY_KOTLIN_FPS),
                    memory = MemoryData.fromJson(jsonObject.optJSONObject(KEY_MEMORY))
                )
            }
            return null
        }
    }

    override fun toString(): String {
        return "PerformanceData: \n" +
                "pageExistTime=$pageExistTime \n" +
                "mode=$mode \n" +
                "isFirstLaunchOfProcess=$isFirstLaunchOfProcess \n" +
                "isFirstLaunchOfPage=$isFirstLaunchOfPage \n" +
                "pageLoadTime=$pageLoadTime \n" +
                "mainFPS=$mainFPS \n" +
                "kotlinFPS=$kotlinFPS \n" +
                "memory=$memory"
    }

}

data class LaunchData(
    val initViewCost: Int,
    val preloadClassCost: Int,
    val fetchContextCodeCost: Int,
    val initRenderContextCost: Int,
    val initRenderCoreCost: Int,
    val newPageCost: Int,
    val pageBuildCost: Int,
    val pageLayoutCost: Int,
    val createPageCost: Int,
    val createInstanceCost: Int,
    val renderCost: Int,
    val firstPaintCost: Int
) {

    companion object {
        private const val KEY_FIRST_PAINT_COST = "firstPaintCost"
        private const val KEY_INIT_VIEW_COST = "initViewCost"
        private const val KEY_PRELOAD_CLASS_COST = "preloadClassCost"
        private const val KEY_FETCH_CONTEXT_CODE_COST = "fetchContextCodeCost"
        private const val KEY_INIT_RENDER_CONTEXT_COST = "initRenderContextCost"
        private const val KEY_INIT_RENDER_CORE_COST = "initRenderCoreCost"
        private const val KEY_NEW_PAGE_COST = "newPageCost"
        private const val KEY_PAGE_BUILD_COST = "pageBuildCost"
        private const val KEY_PAGE_LAYOUT_COST = "pageLayoutCost"
        private const val KEY_ON_CREATE_PAGE_COST = "createPageCost"
        private const val KEY_ON_CREATE_INSTANCE_COST = "createInstanceCost"
        private const val KEY_ON_RENDER_COST = "renderCost"

        fun fromJson(jsonObject: JSONObject?): LaunchData? {
            if (jsonObject != null) {
                return LaunchData(
                    initViewCost = jsonObject.optInt(KEY_INIT_VIEW_COST),
                    preloadClassCost = jsonObject.optInt(KEY_PRELOAD_CLASS_COST),
                    fetchContextCodeCost = jsonObject.optInt(KEY_FETCH_CONTEXT_CODE_COST),
                    initRenderContextCost = jsonObject.optInt(KEY_INIT_RENDER_CONTEXT_COST),
                    initRenderCoreCost = jsonObject.optInt(KEY_INIT_RENDER_CORE_COST),
                    newPageCost = jsonObject.optInt(KEY_NEW_PAGE_COST),
                    pageBuildCost = jsonObject.optInt(KEY_PAGE_BUILD_COST),
                    pageLayoutCost = jsonObject.optInt(KEY_PAGE_LAYOUT_COST),
                    createPageCost = jsonObject.optInt(KEY_ON_CREATE_PAGE_COST),
                    createInstanceCost = jsonObject.optInt(KEY_ON_CREATE_INSTANCE_COST),
                    renderCost = jsonObject.optInt(KEY_ON_RENDER_COST),
                    firstPaintCost = jsonObject.optInt(KEY_FIRST_PAINT_COST)
                )
            }
            return null
        }

    }

    override fun toString(): String {
        return "LaunchData(\n" +
                "   firstPaintCost=$firstPaintCost \n" +
                "   initViewCost=$initViewCost, \n" +
                "   preloadClassCost=$preloadClassCost, \n" +
                "   fetchContextCodeCost=$fetchContextCodeCost, \n" +
                "   initRenderCoreCost=$initRenderCoreCost, \n" +
                "   initRenderContextCost=$initRenderContextCost, \n" +
                "   createInstanceCost=$createInstanceCost, \n" +
                "   newPageCost=$newPageCost, \n" +
                "   onCreatePageCost=$createPageCost, \n" +
                "   pageBuildCost=$pageBuildCost, \n" +
                "   pageLayoutCost=$pageLayoutCost, \n" +
                "   renderCost=$renderCost, \n" +
                ")"
    }

}

// 单位 字节
data class MemoryData(
    val avgIncrement: Int,
    val peakIncrement: Int,
    val appPeak: Int,
    val appAvg: Int) {

    companion object {
        private const val KEY_AVG_INCREMENT = "avgIncrement"
        private const val KEY_PEAK_INCREMENT = "peakIncrement"
        private const val KEY_APP_PEAK = "appPeak"
        private const val KEY_APP_AVG = "appAvg"

        fun fromJson(jsonObject: JSONObject?) : MemoryData? {
            if (jsonObject != null) {
                return MemoryData(
                    avgIncrement = jsonObject.optInt(KEY_AVG_INCREMENT),
                    peakIncrement = jsonObject.optInt(KEY_PEAK_INCREMENT),
                    appPeak = jsonObject.optInt(KEY_APP_PEAK),
                    appAvg = jsonObject.optInt(KEY_APP_AVG)
                )
            }
            return null
        }
    }

    override fun toString(): String {
        return "MemoryData(" +
                "avgIncrement=$avgIncrement," +
                " peakIncrement=$peakIncrement," +
                " appPeak=$appPeak," +
                " appAvg=$appAvg)"
    }

}