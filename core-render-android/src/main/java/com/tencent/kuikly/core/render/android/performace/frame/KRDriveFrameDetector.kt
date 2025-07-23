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

package com.tencent.kuikly.core.render.android.performace.frame

import com.tencent.kuikly.core.render.android.IKuiklyRenderViewTreeUpdateListener
import com.tencent.kuikly.core.render.android.context.IKotlinBridgeStatusListener
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import java.util.Collections

/**
 * 此类用于检测某一帧是否由 core 驱动产生的渲染帧
 */
class KRDriveFrameDetector : IKuiklyRenderViewTreeUpdateListener, IKotlinBridgeStatusListener {

    private val updateViewTaskEnqueuedEvents = Collections.synchronizedList(mutableListOf<Long>())
    private val updateViewTaskFinishEvents = mutableListOf<Long>()
    private val transitionToIdleEvents = Collections.synchronizedList(mutableListOf<Long>())
    private val transitionToBusyEvents = Collections.synchronizedList(mutableListOf<Long>())

    private var isLastFrameEndOfIdle = true

    /**
     * 有更新 View Tree 的任务加入队列时调用
     */
    override fun onUpdateViewTreeEnqueued() {
        updateViewTaskEnqueuedEvents.add(System.nanoTime())
    }

    /**
     * 更新 View Tree 任务完成时调用
     */
    override fun onUpdateViewTreeFinish() {
        assert(isMainThread())
        updateViewTaskFinishEvents.add(System.nanoTime())
    }

    /**
     * native2kotlin bridge 处于空闲状态
     */
    override fun onTransitionBridgeIdle() {
        transitionToIdleEvents.add(System.nanoTime())
    }

    /**
     * native2kotlin bridge 处理繁忙状态
     */
    override fun onTransitionBridgeBusy() {
        transitionToBusyEvents.add(System.nanoTime())
    }

    /**
     * 此方法用于判断某一帧是否由 core 驱动的渲染帧
     * 判断条件：
     * 1. 在该帧时间内，有 updateView 完成的操作
     * 2. 在该帧时间内，updateView 任务队列为空，且 native2kotlin 的 Bridge 属于空闲状态
     *
     * @param frameStartTimeNanos 该帧开始的时间戳
     * @param frameEndTimeNanos 该帧结束的时间戳
     */
    fun isDriveFrame(frameStartTimeNanos: Long, frameEndTimeNanos: Long): Boolean {
        assert(isMainThread())
        val finishedUpdateView = hasEventBetweenTimestamps(
            updateViewTaskFinishEvents,
            frameStartTimeNanos,
            frameEndTimeNanos,
            false
        )
        val didEndFrameIdle = didEndFrameIdle(frameStartTimeNanos, frameEndTimeNanos)
        val updateViewQueueIsEmpty = !hasEventBetweenTimestamps(
            updateViewTaskEnqueuedEvents,
            frameStartTimeNanos,
            frameEndTimeNanos
        )

        val isCoreDriveFrame = finishedUpdateView || (didEndFrameIdle && updateViewQueueIsEmpty)
        isLastFrameEndOfIdle = didEndFrameIdle

        cleanEvents(updateViewTaskFinishEvents, frameEndTimeNanos, false)
        cleanEvents(updateViewTaskEnqueuedEvents, frameEndTimeNanos)
        cleanEvents(transitionToIdleEvents, frameEndTimeNanos)
        cleanEvents(transitionToBusyEvents, frameEndTimeNanos)

        return isCoreDriveFrame
    }

    /**
     * 判断 [events] 是否有事件处于 [startTime, endTime) 区间内
     *
     * @param events 事件列表
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param ensureThreadSafe 是否需要保证线程完全，这里为优化效率，对于某些在同个线程操作的队列不进行加锁
     */
    private fun hasEventBetweenTimestamps(
        events: List<Long>,
        startTime: Long,
        endTime: Long,
        ensureThreadSafe: Boolean = true
    ): Boolean {
        if (ensureThreadSafe) {
            synchronized(events) {
                events.forEach { time ->
                    if (time in startTime until endTime) {
                        return true
                    }
                }
            }
        } else {
            events.forEach { time ->
                if (time in startTime until endTime) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 从 [events] 获取处于 [startTime, endTime) 区间内的最后一个事件的时间戳
     */
    private fun getLastEventBetweenTimestamps(
        events: List<Long>,
        startTime: Long,
        endTime: Long
    ): Long {
        var lastEvent = -1L
        synchronized(events) {
            events.forEach { time ->
                if (time in startTime until endTime) {
                    lastEvent = time
                } else if (time >= endTime) {
                    return lastEvent
                }
            }
        }
        return lastEvent
    }

    /**
     * 判断 [startTime, endTime) 区间内，bridge 是否处于空闲状态
     */
    private fun didEndFrameIdle(startTime: Long, endTime: Long): Boolean {
        val lastIdleEvent = getLastEventBetweenTimestamps(transitionToIdleEvents, startTime, endTime)
        val lastBusyEvent = getLastEventBetweenTimestamps(transitionToBusyEvents, startTime, endTime)
        if (lastIdleEvent == -1L && lastBusyEvent == -1L) {
            return isLastFrameEndOfIdle
        }
        return lastIdleEvent > lastBusyEvent
    }

    /**
     * 清理处于 endTime 之前的事件
     *
     * @param events 时间列表
     * @param endTime 时间戳
     * @param ensureThreadSafe 是否需要保证线程完全，这里为优化效率，对于某些在同个线程操作的队列不进行加锁
     */
    private fun cleanEvents(
        events: MutableList<Long>,
        endTime: Long,
        ensureThreadSafe: Boolean = true
    ) {
        if (ensureThreadSafe) {
            synchronized(events) {
                val iterator = events.iterator()
                while (iterator.hasNext()) {
                    val event = iterator.next()
                    if (event < endTime) {
                        iterator.remove()
                    }
                }
            }
        } else {
            val iterator = events.iterator()
            while (iterator.hasNext()) {
                val event = iterator.next()
                if (event < endTime) {
                    iterator.remove()
                }
            }
        }
    }

}