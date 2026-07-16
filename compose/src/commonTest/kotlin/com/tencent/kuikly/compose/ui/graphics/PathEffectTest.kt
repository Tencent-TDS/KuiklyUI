package com.tencent.kuikly.compose.ui.graphics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * drawBehind + PathEffect 主线的纯 Kotlin 回归用例。
 *
 * 覆盖 [PathEffect.dashPathEffect] 工厂与 [DashPathEffect] 值对象语义：
 * intervals 走 contentEquals（内容相等而非引用相等）、phase 默认值、equals/hashCode 一致性、边界输入可构造。
 * 这些是 KuiklyCanvas dash 下沉逻辑的数据契约，属于本 PR 的核心可测面。
 */
class PathEffectTest {

    @Test
    fun dashPathEffect_createsDashPathEffectWithGivenIntervalsAndPhase() {
        val effect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 2f)
        val dash = assertIs<DashPathEffect>(effect)
        assertTrue(floatArrayOf(10f, 5f).contentEquals(dash.intervals))
        assertEquals(2f, dash.phase)
    }

    @Test
    fun dashPathEffect_phaseDefaultsToZero() {
        val effect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        val dash = assertIs<DashPathEffect>(effect)
        assertEquals(0f, dash.phase)
    }

    @Test
    fun equals_sameContentDifferentArrayInstances_areEqual() {
        val a = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 1f)
        val b = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 1f)
        // intervals 走 contentEquals：内容相同即相等，不因数组是不同实例而不等
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun equals_differentIntervals_areNotEqual() {
        val a = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        val b = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))
        assertNotEquals(a, b)
    }

    @Test
    fun equals_differentPhase_areNotEqual() {
        val a = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 0f)
        val b = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), phase = 3f)
        assertNotEquals(a, b)
    }

    @Test
    fun dashPathEffect_acceptsBoundaryIntervals() {
        // 空数组、单值数组均应可构造（非法输入的降级由 KuiklyCanvas 侧兜底，值对象本身不拦截）
        val empty = assertIs<DashPathEffect>(PathEffect.dashPathEffect(floatArrayOf()))
        assertEquals(0, empty.intervals.size)

        val single = assertIs<DashPathEffect>(PathEffect.dashPathEffect(floatArrayOf(4f)))
        assertEquals(1, single.intervals.size)
    }
}
