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

package com.tencent.kuikly.core.render.android.expand.module

import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.performace.KRPerformanceManager
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchMonitor

/**
 * 性能监控
 */
class KRPerformanceModule(private val performanceManager: KRPerformanceManager?) : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_ON_CREATE_PAGE_FINISH -> onCreatePageFinish(params)
            METHOD_GET_PERFORMANCE_DATA -> getPerformanceData(callback)
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRPerformanceModule"
        const val METHOD_ON_CREATE_PAGE_FINISH = "onPageCreateFinish"
        const val METHOD_GET_PERFORMANCE_DATA = "getPerformanceData"
    }

    private fun onCreatePageFinish(jsonStr: String?) {
        jsonStr?.let {
            performanceManager?.getMonitor<KRLaunchMonitor>(KRLaunchMonitor.MONITOR_NAME)
                ?.onPageCreateFinish(PageCreateTrace(it))
        }
    }

    private fun getPerformanceData(callback: KuiklyRenderCallback?) {
        val performanceData = performanceManager?.getPerformanceData()
        callback?.invoke(performanceData?.toJSONObject())
    }

}

/**
 * 记录页面构建过程耗时
 */
class PageCreateTrace(jsonStr: String) {

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
}