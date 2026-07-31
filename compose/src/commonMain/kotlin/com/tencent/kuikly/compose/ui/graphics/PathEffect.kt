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

package com.tencent.kuikly.compose.ui.graphics

/**
 * Kuikly Compose 的 PathEffect 当前只承载 dash 参数，通过 KuiklyCanvas 直连 CanvasContext.setLineDash。
 */
sealed interface PathEffect {
    companion object {
        fun dashPathEffect(intervals: FloatArray, phase: Float = 0f): PathEffect =
            DashPathEffect(intervals, phase)
    }
}

/**
 * 虚线特效：[intervals] 为 dash/gap 长度对（px 语义）。
 * [phase] 当前不生效：跨端 setLineDash 协议未开放 phase，iOS 硬编码为 0、等效传 0，启用需三端同步扩展。
 */
internal data class DashPathEffect(
    val intervals: FloatArray,
    val phase: Float
) : PathEffect {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DashPathEffect) return false
        if (phase != other.phase) return false
        if (!intervals.contentEquals(other.intervals)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = intervals.contentHashCode()
        result = 31 * result + phase.hashCode()
        return result
    }
}
