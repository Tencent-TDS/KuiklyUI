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

package com.tencent.kuikly.core.manager

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.timer.setTimeout

/**
 * 任务管理类
 */
typealias Task = () -> Unit

class TaskManager(pagerId: String) {
    val pagerId = pagerId
    private val taskQueue = fastArrayListOf<Task>()
    fun nextTick(task: Task) {
        if (taskQueue.isEmpty()) {
            setTimeout(pagerId, {
                val queue = taskQueue.toFastList()
                taskQueue.clear()
                queue.forEach { task ->
                    task()
                }
            }, 0) // next runloop
        }
        taskQueue.add(task)
    }

    fun destroy() {
        taskQueue.clear()
    }
}
