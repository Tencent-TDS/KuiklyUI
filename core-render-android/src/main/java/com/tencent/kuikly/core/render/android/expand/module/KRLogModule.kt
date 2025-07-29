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

import android.annotation.SuppressLint
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.scheduler.KRSubThreadScheduler
import com.tencent.kuikly.core.render.android.scheduler.KRSubThreadTask
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Created by kam on 2023/6/30.
 */

class KRLogModule : KuiklyRenderBaseModule() {

    private var logTasks = arrayListOf<KRSubThreadTask>()
    private var needSyncQueue = false

    private val asyncLogEnable by lazy {
        KuiklyRenderAdapterManager.krLogAdapter?.asyncLogEnable == true
    }
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
         when (method) {
            METHOD_LOG_INFO -> {
                logInfo(params)
            }
            METHOD_LOG_DEBUG -> {
                logDebug(params)
            }
            METHOD_LOG_ERROR -> {
                logError(params)
            }
            else -> super.call(method, params, callback)
        }
        return null
    }

    private fun addLogTask(task: KRSubThreadTask) {
        logTasks.add(task)
        setNeedSyncQueue()
    }

    // batch log on next run loop
    private fun setNeedSyncQueue() {
        if (!needSyncQueue) {
            needSyncQueue = true
            KuiklyRenderCoreContextScheduler.scheduleTask(1) {
                needSyncQueue = false
                val tasks = logTasks.toList()
                logTasks = arrayListOf()
                KRSubThreadScheduler.scheduleTask(0) {
                    tasks.forEach {
                        it()
                    }
                }
            }
        }
    }

    private fun logInfo(params: String?) {
        if (asyncLogEnable) {
            val logTime = logDateTime()
            addLogTask {
                val msg = params ?: ""
                KuiklyRenderLog.i(tag(msg), "|${logTime}|${msg}")
            }
        } else {
            val msg = params ?: ""
            KuiklyRenderLog.i(tag(msg), msg)
        }
    }

    private fun logDebug(params: String?) {
        if (asyncLogEnable) {
            val logTime = logDateTime()
            addLogTask {
                val msg = params ?: ""
                KuiklyRenderLog.d(tag(msg), "|${logTime}|${msg}")
            }
        } else {
            val msg = params ?: ""
            KuiklyRenderLog.d(tag(msg), msg)
        }
    }

    private fun logError(params: String?) {
        if (asyncLogEnable) {
            val logTime = logDateTime()
            addLogTask {
                val msg = params ?: ""
                KuiklyRenderLog.e(tag(msg), "|${logTime}|${msg}")
            }
        } else {
            val msg = params ?: ""
            KuiklyRenderLog.e(tag(msg), msg)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun logDateTime(): String {
        val date = Date(System.currentTimeMillis())
        return formatter.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    companion object {
        const val MODULE_NAME = "KRLogModule"
        private const val METHOD_LOG_INFO = "logInfo"
        private const val METHOD_LOG_DEBUG = "logDebug"
        private const val METHOD_LOG_ERROR = "logError"
        val formatter by lazy {
            SimpleDateFormat("HH:mm.ss.SSS")
        }

        fun tag(msg: String): String {
            val beginTime = System.currentTimeMillis()
            val prefix = "[KLog]["
            val suffix = "]:"
            val startIndex = msg.indexOf(prefix)
            if (startIndex != -1) {
                val endIndex = msg.indexOf(suffix, startIndex + prefix.length)
                if (endIndex != -1) {
                    return msg.substring(startIndex + prefix.length, endIndex)
                }
            }
            return ""
        }
    }
}