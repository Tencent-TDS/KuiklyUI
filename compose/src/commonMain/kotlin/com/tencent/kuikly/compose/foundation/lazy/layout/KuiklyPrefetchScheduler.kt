/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.ui.util.traceValue
import com.tencent.kuikly.core.datetime.DateTime
import kotlin.math.max

internal const val KUIKLY_PREFETCH_FRAME_INTERVAL_NS = 16_666_667L
private const val SAFETY_BUDGET_NS = 2_000_000L
internal const val KUIKLY_PREFETCH_MAX_CONTINUATION_FRAMES = 2

@OptIn(ExperimentalFoundationApi::class)
internal class KuiklyPrefetchScheduler :
    PrefetchScheduler,
    PriorityPrefetchScheduler {

    private val queue = ArrayDeque<PriorityTask>()
    private val scope = PrefetchRequestScopeImpl()

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
        LazyListPrefetchTrace.log(
            "enqueue priority=${task.priority} queueSize=${queue.size}",
        )
    }

    fun cancelAll() {
        queue.clear()
    }

    fun hasPendingWork(): Boolean = queue.isNotEmpty()

    fun processRequests(nanoTime: Long, frameIntervalNs: Long, isFrameIdle: Boolean): Long {
        if (queue.isEmpty()) return 0L

        scope.isFrameIdle = isFrameIdle
        scope.nextFrameTimeNs = nanoTime + frameIntervalNs

        if (!isFrameIdle) {
            val available = scope.availableTimeNanos()
            if (available < SAFETY_BUDGET_NS) {
                LazyListPrefetchTrace.log(
                    "processRequests skip budget: isFrameIdle=$isFrameIdle availableNs=$available queueSize=${queue.size}",
                )
                return 0L
            }
        }

        LazyListPrefetchTrace.log(
            "processRequests start isFrameIdle=$isFrameIdle queueSize=${queue.size}",
        )

        val startTime = DateTime.nanoTime()
        var scheduleForNextFrame = false
        while (queue.isNotEmpty() && !scheduleForNextFrame) {
            scheduleForNextFrame = runRequest()
        }
        traceValue("compose:lazy:prefetch:available_time_nanos", 0L)
        val spent = DateTime.nanoTime() - startTime
        LazyListPrefetchTrace.log(
            "processRequests done spentNs=$spent remainingQueue=${queue.size}",
        )
        return spent
    }

    private fun runRequest(): Boolean {
        val availableTimeNanos = scope.availableTimeNanos()
        traceValue("compose:lazy:prefetch:available_time_nanos", availableTimeNanos)
        if (availableTimeNanos > 0) {
            val task = queue.first()
            val hasMoreWorkToDo = with(task.request) { scope.execute() }
            if (hasMoreWorkToDo) {
                LazyListPrefetchTrace.log("runRequest paused hasMoreWork queueSize=${queue.size}")
                return true
            } else {
                queue.removeFirst()
                LazyListPrefetchTrace.log("runRequest finished queueSize=${queue.size}")
            }
            scope.isFrameIdle = false
        } else {
            return true
        }
        return false
    }

    private class PrefetchRequestScopeImpl : PrefetchRequestScope {
        var isFrameIdle: Boolean = false
        var nextFrameTimeNs: Long = 0L

        override fun availableTimeNanos(): Long =
            if (isFrameIdle) {
                Long.MAX_VALUE
            } else {
                max(0L, nextFrameTimeNs - DateTime.nanoTime())
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class PriorityTask(val priority: Int, val request: PrefetchRequest) {
    companion object {
        const val Low = 0
        const val High = 1
    }
}
