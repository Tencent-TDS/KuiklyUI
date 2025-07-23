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

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager

typealias KRSubThreadTask = () -> Unit
object KRSubThreadScheduler {
    private val logQueueHandlerThread by lazy {
        val ht = HandlerThread("KRSubThreadScheduler", Process.THREAD_PRIORITY_DEFAULT)
        ht.start()
        ht
    }

    private val handler by lazy {
        Handler(logQueueHandlerThread.looper)
    }

    fun scheduleTask(delayMs: Long, task: KRSubThreadTask) {
        if (KuiklyRenderAdapterManager.krThreadAdapter != null) {
            KuiklyRenderAdapterManager.krThreadAdapter?.executeOnSubThread{
                task()
            }
        } else {
            handler.postDelayed(task, delayMs)
        }
    }
}