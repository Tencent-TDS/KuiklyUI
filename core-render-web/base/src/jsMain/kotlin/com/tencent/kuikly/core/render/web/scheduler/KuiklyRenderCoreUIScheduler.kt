package com.tencent.kuikly.core.render.web.scheduler

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCoreTask
import com.tencent.kuikly.core.render.web.ktx.PreRunKuiklyRenderCoreTask

/**
 * Kuikly UI thread scheduler
 */
class KuiklyRenderCoreUIScheduler(
    private val preRunKuiklyRenderCoreUITask: PreRunKuiklyRenderCoreTask? = null
) : IKuiklyRenderCoreScheduler {
    // Flag to indicate if main queue tasks need to be synchronized
    private var needSyncMainQueueTasks = false

    // Main queue tasks
    private var mainQueueTasks: JsArray<KuiklyRenderCoreTask>? = null

    override fun scheduleTask(delayMs: Int, task: KuiklyRenderCoreTask) {
        addTaskToMainQueue(task)
    }

    private fun setNeedSyncMainQueueTasks() {
        if (!needSyncMainQueueTasks) {
            // Lock queue
            needSyncMainQueueTasks = true
            js("Promise.resolve()").then {
                // Execute pre-scheduled task if exists
                preRunKuiklyRenderCoreUITask?.invoke()
                // Save current pending task queue
                val performTask = mainQueueTasks
                mainQueueTasks = null
                // Unlock queue
                needSyncMainQueueTasks = false
                // Execute tasks
                performTask?.forEach { task ->
                    task()
                }
            }
        }
    }

    /**
     * Add task to main queue for execution
     */
    private fun addTaskToMainQueue(task: KuiklyRenderCoreTask) {
        if (mainQueueTasks == null) {
            // Initialize
            mainQueueTasks = JsArray()
        }
        // Insert task
        mainQueueTasks?.add(task)
        // Set whether to synchronize tasks
        setNeedSyncMainQueueTasks()
    }
}
