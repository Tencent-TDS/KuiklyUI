/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy.layout

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.ui.util.trace
import com.tencent.kuikly.compose.ui.util.traceValue
import com.tencent.kuikly.core.datetime.DateTime
import kotlin.math.max

/** Aligns with Android [AndroidPrefetchScheduler] idle detection (2 vsync periods since last draw). */
internal const val KUIKLY_PREFETCH_IDLE_FRAME_MULTIPLIER = 2

@OptIn(ExperimentalFoundationApi::class)
internal interface FramePrefetchScheduler : PrefetchScheduler {
    fun cancelAll()

    fun hasPendingWork(): Boolean

    fun processRequests(
        frameIntervalMillis: Double,
        frameDeadlineMillis: Double,
        isFrameIdle: Boolean,
    ): PrefetchProcessResult
}

@OptIn(ExperimentalFoundationApi::class)
internal class KuiklyPrefetchScheduler :
    FramePrefetchScheduler,
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

    override fun cancelAll() {
        val size = queue.size
        queue.clear()
        if (size > 0) {
            LazyListPrefetchTrace.log("scheduler cancelAll queueSize=$size")
        }
    }

    override fun hasPendingWork(): Boolean = queue.isNotEmpty()

    /**
     * @return spent ns in this prefetch pass; [PrefetchProcessResult.scheduleForNextFrame] mirrors
     *   official `scheduleForNextFrame` (Choreographer post) when work remains or budget is 0.
     */
    override fun processRequests(
        frameIntervalMillis: Double,
        frameDeadlineMillis: Double,
        isFrameIdle: Boolean,
    ): PrefetchProcessResult {
        if (queue.isEmpty()) return PrefetchProcessResult(0L, false)

        scope.isFrameIdle = isFrameIdle
        scope.frameDeadlineMillis = frameDeadlineMillis

        LazyListPrefetchTrace.log(
            "processRequests start isFrameIdle=$isFrameIdle queueSize=${queue.size} frameIntervalMillis=$frameIntervalMillis",
        )

        val startTime = DateTime.nanoTime()
        var scheduleForNextFrame = false
        while (queue.isNotEmpty() && !scheduleForNextFrame) {
            scheduleForNextFrame =
                if (isFrameIdle) {
                    trace("compose:lazy:prefetch:idle_frame") { runRequest() }
                } else {
                    runRequest()
                }
        }
        traceValue("compose:lazy:prefetch:available_time_nanos", 0L)
        val spent = DateTime.nanoTime() - startTime
        LazyListPrefetchTrace.log(
            "processRequests done spentNs=$spent remainingQueue=${queue.size}",
        )
        return PrefetchProcessResult(spent, scheduleForNextFrame)
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
        var frameDeadlineMillis: Double = 0.0

        override fun availableTimeNanos(): Long =
            if (isFrameIdle) {
                Long.MAX_VALUE
            } else {
                val nowMillis = DateTime.nanoTime().toDouble() / NANOS_PER_MILLISECOND
                (max(0.0, frameDeadlineMillis - nowMillis) * NANOS_PER_MILLISECOND).toLong()
            }
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000.0
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class PriorityTask(val priority: Int, val request: PrefetchRequest) {
    companion object {
        const val Low = 0
        const val High = 1
    }
}

internal data class PrefetchProcessResult(
    val spentNs: Long,
    val scheduleForNextFrame: Boolean,
)
