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
