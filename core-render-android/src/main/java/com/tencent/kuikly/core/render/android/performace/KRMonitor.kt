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

import android.os.HandlerThread
import android.os.Looper
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.android.exception.ErrorReason

abstract class KRMonitor<T> : IKuiklyRenderViewLifecycleCallback {

    companion object {
        private const val MONITOR_THREAD_NAME = "Kuikly_Monitor"

        @Volatile
        private var monitorLooper: Looper? = null

        /**
         * 获取监控线程 Looper
         */
        fun getMonitorThreadLooper(): Looper {
            if (monitorLooper == null) {
                synchronized(KRMonitor::class.java) {
                    if (monitorLooper == null) {
                        val t = HandlerThread(MONITOR_THREAD_NAME)
                        t.start()
                        monitorLooper = t.looper
                    }
                }
            }
            return monitorLooper!!
        }
    }

    abstract fun name(): String

    override fun onInit() {}

    override fun onPreloadClassFinish() {}

    override fun onInitCoreStart() {}

    override fun onInitCoreFinish() {}

    override fun onInitContextStart() {}

    override fun onInitContextFinish() {}

    override fun onCreateInstanceStart() {}

    override fun onCreateInstanceFinish() {}

    override fun onFirstFramePaint() {}

    override fun onResume() {}

    override fun onPause() {}

    override fun onDestroy() {}

    abstract fun getMonitorData(): T?

    override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {}

}