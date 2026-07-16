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

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 通用调试模块：封装 Android Trace 抓取能力，用于抓取区间耗时并导出供 AI 分析。
 * 原生实现见 core-render-android 的 KRDebugModule（KuiklyRenderBaseModule）。
 */
class DebugModule : Module() {

    override fun moduleName(): String = ModuleConst.DEBUG

    /**
     * 开始一个 Trace 区间，sectionName 例如 "list_scroll"。
     * 通过 toNative 桥接到原生侧 KRDebugModule.startTrace。
     */
    fun startTrace(sectionName: String) {
        toNative(
            methodName = "startTrace",
            param = JSONObject().apply { put("sectionName", sectionName) }.toString()
        )
    }

    /**
     * 结束当前 Trace 区间，返回方法级 .trace 文件绝对路径。
     * 注意：必须 syncCall = true 才能拿到原生返回值，否则 .toString() 取到空串。
     */
    fun stopTrace(): String =
        toNative(methodName = "stopTrace", param = null, syncCall = true).toString()

    /**
     * 返回最近一次 .trace 文件绝对路径（与 stopTrace 返回一致）。
     */
    fun exportTraceFile(): String =
        toNative(methodName = "exportTraceFile", param = null, syncCall = true).toString()

    /**
     * 导出框架页面事件 Trace 文本（阶段级耗时），供 AI 分析。
     * 前置条件：页面 isDebugLogEnable() 返回 true，否则 pageTrace 为空、返回空串。
     */
    fun exportPageEventTrace(): String =
        pageTrace?.pageEventTrace?.dump(true) ?: ""
}
