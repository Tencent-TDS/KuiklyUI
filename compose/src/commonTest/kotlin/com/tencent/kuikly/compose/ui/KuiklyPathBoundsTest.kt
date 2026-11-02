/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 贝塞尔曲线的包围盒必须由曲线自身的解析极值点决定，而不是控制点：
 * 控制点一般不在曲线上，用它们扩展只会得到偏大的结果。
 */
class KuiklyPathBoundsTest {

    @Test
    fun quadraticBezierBoundsIgnoreControlPointHeight() {
        val path = KuiklyPath()
        path.moveTo(0f, 0f)
        // 控制点 y = 100，曲线最高点只有 50
        path.quadraticBezierTo(50f, 100f, 100f, 0f)

        val bounds = path.getBounds()

        assertEquals(0f, bounds.left)
        assertEquals(100f, bounds.right)
        assertEquals(0f, bounds.top)
        assertEquals(50f, bounds.bottom)
    }

    @Test
    fun cubicBezierBoundsUseCurveExtremum() {
        val path = KuiklyPath()
        path.moveTo(0f, 0f)
        // x 方向单调递增，y 方向在 t = 0.42265 处取到极值 57.735
        path.cubicTo(10f, 100f, 20f, 50f, 30f, 0f)

        val bounds = path.getBounds()

        assertEquals(0f, bounds.left)
        assertEquals(30f, bounds.right)
        assertEquals(0f, bounds.top)
        assertEquals(57.735f, bounds.bottom, 0.01f)
    }

    @Test
    fun boundsContainEverySampledCubicPoint() {
        val path = KuiklyPath()
        path.moveTo(0f, 0f)
        path.cubicTo(30f, 120f, 70f, -60f, 100f, 40f)

        val bounds = path.getBounds()

        for (step in 1 until 100) {
            val t = step / 100f
            val x = cubicAt(0f, 30f, 70f, 100f, t)
            val y = cubicAt(0f, 120f, -60f, 40f, t)
            assertTrue(
                x >= bounds.left - 1e-3f && x <= bounds.right + 1e-3f,
                "sampled x at t=$t escaped bounds: $x not in $bounds"
            )
            assertTrue(
                y >= bounds.top - 1e-3f && y <= bounds.bottom + 1e-3f,
                "sampled y at t=$t escaped bounds: $y not in $bounds"
            )
        }
    }

    @Test
    fun boundsContainEverySampledQuadraticPoint() {
        val path = KuiklyPath()
        path.moveTo(0f, 0f)
        path.quadraticBezierTo(120f, -80f, 40f, 90f)

        val bounds = path.getBounds()

        for (step in 1 until 100) {
            val t = step / 100f
            val x = quadraticAt(0f, 120f, 40f, t)
            val y = quadraticAt(0f, -80f, 90f, t)
            assertTrue(
                x >= bounds.left - 1e-3f && x <= bounds.right + 1e-3f,
                "sampled x at t=$t escaped bounds: $x not in $bounds"
            )
            assertTrue(
                y >= bounds.top - 1e-3f && y <= bounds.bottom + 1e-3f,
                "sampled y at t=$t escaped bounds: $y not in $bounds"
            )
        }
    }

    @Test
    fun collinearCubicFallsBackToEndpoints() {
        val path = KuiklyPath()
        path.moveTo(0f, 0f)
        // 控制点共线，导数的二次项与一次项同时为零，曲线即直线
        path.cubicTo(1f, 1f, 2f, 2f, 3f, 3f)

        val bounds = path.getBounds()

        assertEquals(0f, bounds.left)
        assertEquals(0f, bounds.top)
        assertEquals(3f, bounds.right)
        assertEquals(3f, bounds.bottom)
    }

    @Test
    fun relativeCubicMeasuresFromCurrentPoint() {
        val path = KuiklyPath()
        path.moveTo(10f, 10f)
        path.relativeCubicTo(0f, 100f, 0f, 100f, 100f, 0f)

        val bounds = path.getBounds()

        assertEquals(10f, bounds.left)
        assertEquals(10f, bounds.top)
        assertEquals(110f, bounds.right)
        // 曲线 y 的极值落在 10 与 110 之间，不应达到控制点所在的 110
        assertTrue(bounds.bottom > 10f, "bottom should follow the curve, got $bounds")
        assertTrue(bounds.bottom < 110f, "control point height must not inflate bounds: $bounds")
    }

    @Test
    fun lineSegmentsStillProduceExactBounds() {
        val path = KuiklyPath()
        path.moveTo(10f, 20f)
        path.lineTo(110f, 220f)

        val bounds = path.getBounds()

        assertEquals(10f, bounds.left)
        assertEquals(20f, bounds.top)
        assertEquals(110f, bounds.right)
        assertEquals(220f, bounds.bottom)
    }

    private fun quadraticAt(p0: Float, p1: Float, p2: Float, t: Float): Float {
        val mt = 1f - t
        return mt * mt * p0 + 2f * mt * t * p1 + t * t * p2
    }

    private fun cubicAt(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val mt = 1f - t
        return mt * mt * mt * p0 + 3f * mt * mt * t * p1 + 3f * mt * t * t * p2 + t * t * t * p3
    }
}
