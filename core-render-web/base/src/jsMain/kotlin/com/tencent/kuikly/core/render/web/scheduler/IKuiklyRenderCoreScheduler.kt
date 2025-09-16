package com.tencent.kuikly.core.render.web.scheduler

import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCoreTask

/**
 * Kuikly core scheduler interface
 */
interface IKuiklyRenderCoreScheduler {
    fun scheduleTask(delayMs: Int = 0, task: KuiklyRenderCoreTask)
}
