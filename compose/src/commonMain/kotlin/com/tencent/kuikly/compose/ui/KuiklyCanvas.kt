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

package com.tencent.kuikly.compose.ui

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.geometry.RoundRect
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.ClipOp
import com.tencent.kuikly.compose.ui.graphics.DashPathEffect
import com.tencent.kuikly.compose.ui.graphics.ImageBitmap
import com.tencent.kuikly.compose.ui.graphics.LinearGradient
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Paint
import com.tencent.kuikly.compose.ui.graphics.PaintingStyle
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.PointMode
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.toKuiklyColor
import com.tencent.kuikly.compose.ui.graphics.toKuiklyLinearGradient
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.RenderView
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.CanvasLinearGradient
import com.tencent.kuikly.core.views.CanvasView
import kotlin.math.PI

internal class KuiklyCanvas : Canvas {

    /**
     * 虚线脏标记：仅当上一笔真向原生下发过 setLineDash(intervals) 时才为 true。
     * setLineDash 是真实桥调用（拼 JSON 走 callMethod），未激活场景（从未画过虚线）
     * 必须做到零桥调用，避免 Canvas 密集页面每个图形多一次跨端通信。
     */
    private var dashActive = false

    /**
     * 记录上一次下发的虚线 intervals（已换算成 dp/pt）。intervals 与上次一致时跳过重复的
     * setLineDash 桥调用；null 表示尚未下发过。原生画布 reset 后须同步置空。
     */
    private var lastDashIntervals: List<Float>? = null

    /**
     * 仅在 dashActive 时向原生下发清空虚线指令并复位标记；未激活时零开销。
     */
    private fun CanvasContext.clearLineDashIfActive() {
        if (dashActive) {
            setLineDash(emptyList())
            dashActive = false
            lastDashIntervals = null
        }
    }

    private fun CanvasContext.fillOrStroke(paint: Paint) {
        // 清空上一笔 drawLine 残留的虚线状态，避免同 lambda 内后续描边图形继承虚线
        clearLineDashIfActive()
        val linearGradient = paint.toKuiklyLinearGradient(densityValue)
        if (paint.style == PaintingStyle.Fill) {
            if (linearGradient != null) {
                fillStyle(linearGradient)
            } else {
                fillStyle(paint.toKuiklyColor())
            }
            fill()
        } else {
            if (linearGradient != null) {
                strokeStyle(linearGradient)
            } else {
                strokeStyle(paint.toKuiklyColor())
            }
            lineWidth(paint.strokeWidth / densityValue)
            if (strokeCap != paint.strokeCap) {
                strokeCap = paint.strokeCap
                when (paint.strokeCap) {
                    StrokeCap.Butt -> lineCapButt()
                    StrokeCap.Round -> lineCapRound()
                    StrokeCap.Square -> lineCapSquare()
                }
            }
            stroke()
        }
    }

    override var view: DeclarativeBaseView<*, *>? = null
        set(value) {
            if (value is CanvasView) {
                val renderView = value.renderView
                if (renderView != null) {
                    context = CanvasContext(renderView, value.pagerId, value.nativeRef)
                    densityValue = value.getPager().pagerDensity()
                    renderView.callMethod("reset", "")
                    strokeCap = StrokeCap.Butt
                    dashActive = false  // 原生画布已 reset，虚线状态同步复位
                    lastDashIntervals = null
                } else {
                    context = null
                }
            } else {
                context = null
            }
            field = value
        }

    private var context: CanvasContext? = null
    private var densityValue: Float = 1f
    private var strokeCap = StrokeCap.Butt

    override fun save() {
        context?.save()
    }

    override fun restore() {
        context?.restore()
    }

    override fun saveLayer(bounds: Rect, paint: Paint) {
        context?.saveLayer(
            x = bounds.left / densityValue,
            y = bounds.top / densityValue,
            width = bounds.width / densityValue,
            height = bounds.height / densityValue
        )
    }

    override fun translate(dx: Float, dy: Float) {
        context?.translate(dx / densityValue, dy / densityValue)
    }

    override fun scale(sx: Float, sy: Float) {
        context?.scale(sx, sy)
    }

    override fun rotate(degrees: Float) {
        context?.rotate(radians(degrees))
    }

    override fun skew(sx: Float, sy: Float) {
        context?.skew(sx, sy)
    }

    override fun concat(matrix: Matrix) {
        context?.apply {
            val values = FloatArray(9)
            values.setFrom(matrix)
            values[2] /= densityValue // adjusting the tx (translation x)
            values[5] /= densityValue // adjusting the ty (translation y)
            transform(values)
        }
    }

    override fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: ClipOp) {
        context?.apply {
            beginPath()
            moveTo(left / densityValue, top / densityValue)
            lineTo(right / densityValue, top / densityValue)
            lineTo(right / densityValue, bottom / densityValue)
            lineTo(left / densityValue, bottom / densityValue)
            closePath()
            clip(clipOp == ClipOp.Intersect)
        }
    }

    override fun clipPath(path: Path, clipOp: ClipOp) {
        context?.apply {
            if (path !is KuiklyPath) {
                throw IllegalArgumentException("require KuiklyPath, but got ${path::class.simpleName}")
            }
            beginPath()
            path.draw(this, densityValue)
            clip(clipOp == ClipOp.Intersect)
        }
    }

    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        context?.apply {
            // dash 参数下沉：px → dp/pt 换算后透传给 CanvasContext.setLineDash
            val effect = paint.pathEffect
            if (effect is DashPathEffect) {
                val intervals = effect.intervals
                // 非法输入兜底：intervals 为空或全零时降级画实线，避免透传未定义行为
                if (intervals.isEmpty() || intervals.all { it == 0f }) {
                    clearLineDashIfActive()
                } else {
                    // map 已返回 List，无需再 toList() 复制
                    val scaled = intervals.map { it / densityValue }
                    // intervals 与上次一致时跳过重复的 setLineDash 桥调用
                    if (!dashActive || lastDashIntervals != scaled) {
                        setLineDash(scaled)
                        lastDashIntervals = scaled
                        dashActive = true
                    }
                }
            } else {
                clearLineDashIfActive()  // 仅上一笔真画过虚线时才下发清理，防残留
            }
            beginPath()
            moveTo(p1.x / densityValue, p1.y / densityValue)
            lineTo(p2.x / densityValue, p2.y / densityValue)
            strokeStyle(paint.toKuiklyColor())
            lineWidth(paint.strokeWidth / densityValue)
            stroke()
        }
    }

    override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        context?.apply {
            beginPath()
            moveTo(left / densityValue, top / densityValue)
            lineTo(right / densityValue, top / densityValue)
            lineTo(right / densityValue, bottom / densityValue)
            lineTo(left / densityValue, bottom / densityValue)
            closePath()
            fillOrStroke(paint)
        }
    }

    override fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        paint: Paint
    ) {
        context ?: return
        drawPath(Path().apply {
            addRoundRect(RoundRect(left, top, right, bottom, radiusX, radiusY))
        }, paint)
    }

    override fun drawOval(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        context ?: return
        drawPath(Path().apply {
            addOval(Rect(left, top, right, bottom))
        }, paint)
    }

    override fun drawCircle(center: Offset, radius: Float, paint: Paint) {
        context?.apply {
            beginPath()
            arc(
                center.x / densityValue,
                center.y / densityValue,
                radius / densityValue,
                0f,
                PI.toFloat() * 2f,
                false
            )
            closePath()
            fillOrStroke(paint)
        }
    }

    override fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        context ?: return
        drawPath(Path().apply {
            if (useCenter) {
                moveTo((left + right) / 2, (top + bottom) / 2)
            }
            arcTo(Rect(left, top, right, bottom), startAngle, sweepAngle, !useCenter)
            if (useCenter) {
                close()
            }
        }, paint)
    }

    override fun drawPath(path: Path, paint: Paint) {
        context?.apply {
            if (path !is KuiklyPath) {
                throw IllegalArgumentException("require KuiklyPath, but got ${path::class.simpleName}")
            }
            beginPath()
            path.draw(this, densityValue)
            fillOrStroke(paint)
        }
    }

    override fun drawImage(image: ImageBitmap, topLeftOffset: Offset, paint: Paint) {
        context?.apply {
            val kImage = image as? KuiklyImageBitmap ?: return@apply
            if (!kImage.isReady) {
                return@apply
            }
            drawImage(
                kImage.imageRef,
                topLeftOffset.x / densityValue,
                topLeftOffset.y / densityValue,
                kImage.width.toFloat() / densityValue,
                kImage.height.toFloat() / densityValue
            )
        }
    }

    override fun drawImageRect(
        image: ImageBitmap,
        srcOffset: IntOffset,
        srcSize: IntSize,
        dstOffset: IntOffset,
        dstSize: IntSize,
        paint: Paint
    ) {
        context?.apply {
            val kImage = image as? KuiklyImageBitmap ?: return@apply
            if (!kImage.isReady) {
                return@apply
            }
            drawImage(
                kImage.imageRef,
                srcOffset.x.toFloat(),
                srcOffset.y.toFloat(),
                srcSize.width.toFloat(),
                srcSize.height.toFloat(),
                dstOffset.x.toFloat() / densityValue,
                dstOffset.y.toFloat() / densityValue,
                dstSize.width.toFloat() / densityValue,
                dstSize.height.toFloat() / densityValue
            )
        }
    }

    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        context ?: return
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.Lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.Polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.Points -> drawPoints(points, paint)
        }
    }

    override fun enableZ() {
        // TODO("Not yet implemented")
    }

    override fun disableZ() {
        // TODO("Not yet implemented")
    }

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        context?.apply {
            beginPath()
            val radius = paint.strokeWidth * 0.5f
            points.fastForEach { point ->
                moveTo((point.x - radius) / densityValue, point.y / densityValue)
                lineTo((point.x + radius) / densityValue, point.y / densityValue)
            }
            fillOrStroke(paint)
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step.
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawRawLines
     */
    private fun drawLines(points: List<Offset>, paint: Paint, stepBy: Int) {
        if (points.size >= 2) {
            var i = 0
            while (i < points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                drawLine(p1, p2, paint)
                i += stepBy
            }
        }
    }
}
