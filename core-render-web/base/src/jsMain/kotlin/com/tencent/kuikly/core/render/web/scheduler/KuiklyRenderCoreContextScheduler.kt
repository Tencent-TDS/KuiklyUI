package com.tencent.kuikly.core.render.web.scheduler

import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCoreTask
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow

/**
 * Kuikly execution environment scheduler
 */
object KuiklyRenderCoreContextScheduler : IKuiklyRenderCoreScheduler {
    override fun scheduleTask(delayMs: Int, task: KuiklyRenderCoreTask) {
        // Execute task using browser's timeout API
        // TODO For delays <= 0, can use promise.resolve to improve performance, but need to note that microtask queue
        // may not complete execution for UI/I/O operations
        kuiklyWindow.setTimeout({
            task()
        }, delayMs)
    }
}
