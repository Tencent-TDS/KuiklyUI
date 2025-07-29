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

import com.tencent.kuikly.core.render.android.performace.frame.KRFrameData
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData
import com.tencent.kuikly.core.render.android.performace.memory.KRMemoryData
import org.json.JSONObject

/**
 * 性能数据
 */
data class KRPerformanceData(
    val pageName: String,
    val spentTime: Long,
    val isColdLaunch: Boolean,
    val isPageColdLaunch: Boolean,
    val executeMode: Int,
    val launchData: KRLaunchData?,
    val frameData: KRFrameData?,
    val memoryData: KRMemoryData?
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
    }

    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_MODE, executeMode)
            put(KEY_PAGE_EXIST_TIME, spentTime)
            put(KEY_IS_FIRST_PAGE_PROCESS, isColdLaunch)
            put(KEY_IS_FIRST_PAGE_LAUNCH, isPageColdLaunch)
            put(KEY_MAIN_FPS, frameData?.getFps() ?: 0)
            put(KEY_KOTLIN_FPS, frameData?.getKuiklyFps() ?: 0)
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

}