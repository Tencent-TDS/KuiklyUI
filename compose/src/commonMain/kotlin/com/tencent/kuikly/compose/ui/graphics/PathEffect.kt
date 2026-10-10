/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.graphics

/**
 * Effect applied to the geometry of a drawing primitive. For example, this can be used
 * to draw a dashed line.
 *
 * The Kuikly canvas lowers a [PathEffect] to the renderer's `lineDash` command, so only
 * [dashPathEffect] is provided. Corner, stamped and chained effects would need renderer
 * support on every platform first.
 */
interface PathEffect {
    companion object {
        /**
         * Replaces the outline of the shape with a series of dashes.
         *
         * @param intervals Array of "on" and "off" distances for the dashed line segments.
         * Must contain an even number of entries (>= 2). The even indices specify the "on"
         * intervals, the odd indices specify the "off" intervals, in pixels.
         * @param phase Pixel offset into the intervals array. The Kuikly canvas does not
         * support a phase offset on any renderer yet; the value is kept for source
         * compatibility with Jetpack Compose and is currently ignored.
         */
        fun dashPathEffect(intervals: FloatArray, phase: Float = 0f): PathEffect =
            DashPathEffect(intervals, phase)
    }
}

/**
 * Dashed stroke pattern. Compared by content so the canvas can skip re-sending an
 * unchanged `lineDash` command.
 */
internal class DashPathEffect(val intervals: FloatArray, val phase: Float) : PathEffect {

    init {
        require(intervals.size >= 2 && intervals.size % 2 == 0) {
            "dashPathEffect requires an even number of intervals (>= 2), got ${intervals.size}"
        }
        require(intervals.all { it >= 0f }) { "dashPathEffect intervals must not be negative" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashPathEffect) return false
        return phase == other.phase && intervals.contentEquals(other.intervals)
    }

    override fun hashCode(): Int = 31 * intervals.contentHashCode() + phase.hashCode()

    override fun toString(): String =
        "DashPathEffect(intervals=${intervals.contentToString()}, phase=$phase)"
}
