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

import android.os.Build
import android.os.Debug
import android.os.Trace
import com.tencent.kuikly.core.render.android.BuildConfig
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.File

/**
 * DebugModule 的原生实现：落地 Android Trace 抓取。
 * - Debug.startMethodTracing / stopMethodTracing：方法级插桩，产出 .trace 文件。
 * - android.os.Trace.beginSection / endSection：在系统 trace 打命名切片（Kuikly:xxx）。
 * 抓取仅在 debug 构建生效（BuildConfig.DEBUG 守卫），release 包不抓 trace。
 */
class KRDebugModule : KuiklyRenderBaseModule() {

    private var currentTraceFile: File? = null

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "startTrace" -> {
                if (!BuildConfig.DEBUG) return null
                val section = params.toJSONObjectSafely().optString("sectionName", "kuikly")
                val dir = context?.filesDir ?: return null
                val file = File(dir, "kuikly_$section.trace")
                currentTraceFile = file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Trace.beginSection("Kuikly:$section")
                }
                // 大 buffer（~1GB）防方法级 tracing 文件截断
                Debug.startMethodTracing(file.absolutePath, 1_000_000_000)
                null
            }
            "stopTrace" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Trace.endSection()
                }
                Debug.stopMethodTracing()
                currentTraceFile?.absolutePath ?: ""
            }
            "exportTraceFile" -> currentTraceFile?.absolutePath ?: ""
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRDebugModule"   // 与 ModuleConst.DEBUG 一致
    }
}
