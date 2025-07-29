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

package com.tencent.kuikly.core.render.android.scheduler

import android.os.ConditionVariable
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import com.tencent.kuikly.core.nvi.NativeBridge

/**
 * KTV页面执行环境调度器
 */
object KuiklyRenderCoreContextScheduler : IKuiklyRenderCoreScheduler {

    private val contextQueueHandlerThread by lazy {
        val ht = object : HandlerThread("HRContextQueueHandlerThread", Process.THREAD_PRIORITY_FOREGROUND) {
            override fun onLooperPrepared() {
                NativeBridge.isContextThread = true
            }
        }
        ht.start()
        ht
    }

    private val handler by lazy {
        Handler(contextQueueHandlerThread.looper)
    }

    override fun scheduleTask(delayMs: Long, task: KuiklyRenderCoreTask) {
        handler.postDelayed(task, delayMs)
    }

    override fun destroy() {
    }

    fun runTaskSyncUnsafely(delayMs: Long = 0, timeout: Long, task: KuiklyRenderCoreTask) {
        handler.runTaskSyncUnsafely(delayMs, timeout, task)
    }

}

private fun Handler.runTaskSyncUnsafely(delayMs: Long, timeout: Long, task: KuiklyRenderCoreTask): Boolean {
    require(timeout >= 0) { "timeout must be non-negative" }

    if (Looper.myLooper() == looper) {
        task()
        return true
    }

    return BlockingRunnable(task).postAndWait(this, delayMs, timeout)
}

private class BlockingRunnable(private val mTask: KuiklyRenderCoreTask) : Runnable {

    private val conditionVariable = ConditionVariable()

    override fun run() {
        try {
            mTask.invoke()
        } finally {
            conditionVariable.open()
        }
    }

    fun postAndWait(handler: Handler, delayMs: Long, timeout: Long): Boolean {
        if (!handler.postDelayed(this, delayMs)) {
            return false
        }

        conditionVariable.block(timeout)
        return true
    }
}
