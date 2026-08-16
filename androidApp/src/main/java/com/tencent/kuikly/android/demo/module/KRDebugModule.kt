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

package com.tencent.kuikly.android.demo.module

import android.os.Build
import android.os.Debug
import android.os.Trace
import com.tencent.kuikly.core.render.android.BuildConfig
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.File

/**
 * Demo 侧 DebugModule 的 Android 原生实现：落地 Android Trace 抓取。
 */
class KRDebugModule : KuiklyRenderBaseModule() {

    @Volatile
    private var currentTraceFile: File? = null

    @Volatile
    private var isTracing = false

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            START_TRACE -> {
                if (!BuildConfig.DEBUG || isTracing) return null
                val section = params.toJSONObjectSafely().optString(SECTION_NAME, DEFAULT_SECTION_NAME)
                val dir = context?.filesDir ?: return null
                val file = File(dir, "kuikly_$section.trace")
                currentTraceFile = file
                val canTraceSection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                try {
                    if (canTraceSection) {
                        Trace.beginSection("Kuikly:$section")
                    }
                    Debug.startMethodTracing(file.absolutePath, TRACE_BUFFER_SIZE)
                    isTracing = true
                } catch (e: Exception) {
                    if (canTraceSection) {
                        Trace.endSection()
                    }
                    currentTraceFile = null
                }
                null
            }

            STOP_TRACE -> {
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

            EXPORT_TRACE_FILE -> currentTraceFile?.absolutePath ?: ""
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRDebugModule"
        private const val START_TRACE = "startTrace"
        private const val STOP_TRACE = "stopTrace"
        private const val EXPORT_TRACE_FILE = "exportTraceFile"
        private const val SECTION_NAME = "sectionName"
        private const val DEFAULT_SECTION_NAME = "kuikly"
        private const val TRACE_BUFFER_SIZE = 8_000_000
    }
}
