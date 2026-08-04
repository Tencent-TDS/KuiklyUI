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
 * Kuikly Compose 的 PathEffect 目前只承载 dash 参数，不映射到底层 Skia 的 SkPathEffect。
 * 内部通过 KuiklyCanvas 直连 CanvasContext.setLineDash。
 *
 * 预留 cornerPathEffect / chainPathEffect / stampedPathEffect 扩展位，本期只实现 dashPathEffect。
 */
sealed interface PathEffect {
    companion object {
        fun dashPathEffect(intervals: FloatArray, phase: Float = 0f): PathEffect =
            DashPathEffect(intervals, phase)
    }
}

/**
 * 虚线特效：[intervals] 为 dash/gap 长度对（px 语义，与官方 Jetpack Compose 一致），
 * 在 KuiklyCanvas.drawLine 里会除以 densityValue 换算为 dp/pt 后透传给 CanvasContext.setLineDash。
 *
 * 注意：[phase] 当前不生效——跨端 CanvasContext.setLineDash 协议未开放 phase 参数，
 * iOS 桥内 CGContextSetLineDash 的 phase 硬编码为 0。设置 phase 不会报错也不会有视觉差异，
 * 与传 0 等效。若后续启用需三端同步扩展 setLineDash 协议。
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
