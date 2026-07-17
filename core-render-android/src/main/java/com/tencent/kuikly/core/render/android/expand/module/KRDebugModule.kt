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

    @Volatile
    private var currentTraceFile: File? = null
    @Volatile
    private var isTracing = false

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "startTrace" -> {
                if (!BuildConfig.DEBUG || isTracing) return null
                val section = params.toJSONObjectSafely().optString("sectionName", "kuikly")
                val dir = context?.filesDir ?: return null
                val file = File(dir, "kuikly_$section.trace")
                currentTraceFile = file
                val canTraceSection =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                try {
                    if (canTraceSection) {
                        Trace.beginSection("Kuikly:$section")
                    }
                    // 8MB 内存 buffer（Android 默认值）：写入 .trace 文件时按文件流式落盘，
                    // 无需超大 buffer；1GB 预留会在低内存机型触发 OOM / 被内核截断。
                    Debug.startMethodTracing(file.absolutePath, TRACE_BUFFER_SIZE)
                    isTracing = true
                } catch (e: Exception) {
                    // 起始失败须回收已开启的 trace 区间，避免区间泄漏 / 后续 endSection 抛异常
                    if (canTraceSection) {
                        Trace.endSection()
                    }
                    currentTraceFile = null
                }
                null
            }
            "stopTrace" -> {
                if (!BuildConfig.DEBUG || !isTracing) {
                    return currentTraceFile?.absolutePath ?: ""
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        Trace.endSection()
                    }
                    Debug.stopMethodTracing()
                } finally {
                    isTracing = false
                }
                currentTraceFile?.absolutePath ?: ""
            }
            "exportTraceFile" -> currentTraceFile?.absolutePath ?: ""
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRDebugModule"   // 与 ModuleConst.DEBUG 一致
        // 方法级 tracing 的内存 buffer（字节）。8MB 为 Android 默认值：
        // .trace 文件按流式落盘，无需超大 buffer；1GB 预留会在低内存机型触发 OOM / 被内核截断。
        private const val TRACE_BUFFER_SIZE = 8_000_000
    }
}
