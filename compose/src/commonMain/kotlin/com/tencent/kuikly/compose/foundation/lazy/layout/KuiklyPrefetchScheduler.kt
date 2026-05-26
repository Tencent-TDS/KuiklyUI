/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
internal class KuiklyPrefetchScheduler :
    PrefetchScheduler,
    PriorityPrefetchScheduler {

    private val queue = ArrayDeque<PriorityTask>()

    override fun schedulePrefetch(prefetchRequest: PrefetchRequest) {
        scheduleHighPriorityPrefetch(prefetchRequest)
    }

    override fun scheduleLowPriorityPrefetch(prefetchRequest: PrefetchRequest) {
        enqueue(PriorityTask(PriorityTask.Low, prefetchRequest))
    }

    override fun scheduleHighPriorityPrefetch(prefetchRequest: PrefetchRequest) {
        enqueue(PriorityTask(PriorityTask.High, prefetchRequest))
    }

    private fun enqueue(task: PriorityTask) {
        queue.addLast(task)
        queue.sortWith(compareByDescending { it.priority })
    }

    fun cancelAll() {
        queue.clear()
    }

    fun hasPendingWork(): Boolean = queue.isNotEmpty()

    /** Stub: filled in Commit 5 frame-loop integration. */
    fun processRequests(nanoTime: Long, frameIntervalNs: Long, isFrameIdle: Boolean): Long = 0L
}

@OptIn(ExperimentalFoundationApi::class)
internal class PriorityTask(val priority: Int, val request: PrefetchRequest) {
    companion object {
        const val Low = 0
        const val High = 1
    }
}
