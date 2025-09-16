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

import android.os.Looper
import android.os.Process

/**
 * A HandlerThread support specified StackSize
 */
open class KRHandlerThread : Thread {
    var mPriority: Int

    var threadId: Int = -1
    var mLooper: Looper? = null

    constructor(name: String, priority: Int, stackSize: Long) : super(null, null, name, stackSize) {
        mPriority = priority
    }

    protected open fun onLooperPrepared() {
    }

    override fun run() {
        this.threadId = Process.myTid()
        Looper.prepare()
        synchronized(this) {
            mLooper = Looper.myLooper()!!
            (this as Object).notifyAll()
        }
        Process.setThreadPriority(mPriority)
        onLooperPrepared()
        Looper.loop()
        this.threadId = -1
    }

    val looper: Looper
        get() {
            var wasInterrupted = false
            // If the thread has been started, wait until the looper has been created.
            synchronized(this) {
                while (isAlive() && mLooper == null) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        wasInterrupted = true
                    }
                }
            }
            if (wasInterrupted) {
                currentThread().interrupt()
            }
            return mLooper!!
        }
}
