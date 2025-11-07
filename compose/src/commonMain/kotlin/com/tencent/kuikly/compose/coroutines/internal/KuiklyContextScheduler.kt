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
package com.tencent.kuikly.compose.coroutines.internal

import com.tencent.kuikly.core.manager.BridgeManager
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal object KuiklyContextScheduler : SynchronizedObject() {

    private val taskMap = mutableMapOf<String, MutableList<(Boolean) -> Unit>>()
    private val scheduleMap = mutableMapOf<String, Boolean>()

    init {
        platformInitScheduler()
    }

    /**
     * 判断当前线程是否是Kuikly线程
     * @param pagerId 页面ID
     * @return 是否是Kuikly线程
     */
    fun isOnKuiklyThread(pagerId: String) = platformIsOnKuiklyThread(pagerId)

    /**
     * 在Kuikly线程执行任务
     * @param pagerId 页面ID
     * @param block 任务
     */
    fun runOnKuiklyThread(pagerId: String, block: (cancel: Boolean) -> Unit) {
        var needSchedule = false
        synchronized(this) {
            val taskList = taskMap[pagerId] ?: mutableListOf<(Boolean) -> Unit>().also { taskMap[pagerId] = it }
            taskList.add(block)
            if (scheduleMap[pagerId] != true) {
                scheduleMap[pagerId] = true
                needSchedule = true
            }
        }
        if (needSchedule) {
            platformScheduleOnKuiklyThread(pagerId)
        }
    }

    /**
     * 执行任务，非线程安全
     * @param pagerId 页面ID
     */
    internal fun runTask(pagerId: String) {
        val cancel = !BridgeManager.containNativeBridge(pagerId)
        var taskList: List<(Boolean) -> Unit>? = null
        synchronized(this) {
            taskList = taskMap.remove(pagerId)
            scheduleMap[pagerId] = false
        }
        if (taskList.isNullOrEmpty()) {
            return
        }
        BridgeManager.currentPageId = pagerId
        for (task in taskList!!) {
            try {
                task(cancel)
            } catch (t: Throwable) {
                platformNotifyKuiklyException(t)
            }
        }
    }

}

internal expect fun platformInitScheduler()

internal expect inline fun platformIsOnKuiklyThread(pagerId: String): Boolean

internal expect inline fun platformScheduleOnKuiklyThread(pagerId: String)

internal expect inline fun platformNotifyKuiklyException(t: Throwable)
