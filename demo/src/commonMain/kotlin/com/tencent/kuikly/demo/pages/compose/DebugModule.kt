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

package com.tencent.kuikly.demo.pages.compose

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Demo 侧调试模块：封装 Android Trace 抓取能力，用于抓取区间耗时并导出供 AI 分析。
 *
 * 注意：原生实现仅由 demo Android 宿主侧注册；非 Android 平台调用方必须自行做平台守卫，
 * 否则调用会被原生侧静默忽略、拿不到返回值。
 */
internal class DebugModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    fun startTrace(sectionName: String) {
        toNative(
            methodName = START_TRACE,
            param = JSONObject().apply { put(SECTION_NAME, sectionName) }.toString()
        )
    }

    fun stopTrace(): String =
        toNative(methodName = STOP_TRACE, param = null, syncCall = true).toString()

    fun exportTraceFile(): String =
        toNative(methodName = EXPORT_TRACE_FILE, param = null, syncCall = true).toString()

    fun exportPageEventTrace(): String =
        pageTrace?.pageEventTrace?.dump(true) ?: ""

    companion object {
        const val MODULE_NAME = "KRDebugModule"
        private const val START_TRACE = "startTrace"
        private const val STOP_TRACE = "stopTrace"
        private const val EXPORT_TRACE_FILE = "exportTraceFile"
        private const val SECTION_NAME = "sectionName"
    }
}
