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

import com.tencent.kuikly.compose.ui.geometry.CornerRadius
import com.tencent.kuikly.compose.ui.geometry.MutableRect
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.geometry.RoundRect
import com.tencent.kuikly.compose.ui.geometry.boundingRect
import com.tencent.kuikly.compose.ui.geometry.toRect
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.degrees
import com.tencent.kuikly.compose.ui.util.fastForEach
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tan

private typealias CanvasOp = CanvasContextEx.() -> Unit

internal inline fun radians(degrees: Float): Float = degrees * (PI / 180f).toFloat()

class KuiklyPath : Path {

    companion object {
        private val CornerRadius.radius: Float get() = min(x, y)

        private const val HALF_PI = PI.toFloat() * 0.5f

        // Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
        private const val NON_ZERO_EPSILON = 0.001f
        private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON

        private fun pointOfArc(radiusX: Float, radiusY: Float, angle: Float): Offset {
            val radians = radians(angle)
            return Offset(radiusX * cos(radians), radiusY * sin(radians))
        }

        private fun CanvasContextEx.drawCircularArc(
            centerX: Float,
            centerY: Float,
            radius: Float,
            startAngle: Float,
            sweepAngle: Float
        ) {
            if (abs(sweepAngle) < 360) {
                arcWithDensity(
                    centerX,
                    centerY,
                    radius,
                    radians(startAngle),
                    radians(startAngle + sweepAngle),
                    sweepAngle < 0
                )
            } else {
                // draw two half arcs to avoid the limitation of the kuikly canvas
                val halfSweepAngle = sign(sweepAngle) * (abs(sweepAngle) % 360 + 360) * 0.5f
                arcWithDensity(
                    centerX,
                    centerY,
                    radius,
                    radians(startAngle),
                    radians(startAngle + halfSweepAngle),
                    sweepAngle < 0
                )
                arcWithDensity(
                    centerX,
                    centerY,
                    radius,
                    radians(startAngle + halfSweepAngle),
                    radians(startAngle + halfSweepAngle * 2),
                    sweepAngle < 0
                )
            }
        }

        private inline val Float.simplyDegrees: Float
            get() = if (this >= 0) this % 360 else this % 360 + 360

        private inline fun nextRadiansForward(current: Float, limit: Float): Float {
            return min(current + HALF_PI, limit)
        }

        private inline fun nextRadiansBackward(current: Float, limit: Float): Float {
            return max(current - HALF_PI, limit)
        }

        private fun CanvasContextEx.drawOvalArc(
            centerX: Float,
            centerY: Float,
            radiusX: Float,
            radiusY: Float,
            startAngle: Float,
            sweepAngle: Float,
            moveTo: Boolean
        ) {
            // 把角度转换到 0-360 范围内，以便下一步通过向上/向下取整计算第一段弧的结束角度
            var rad = radians(startAngle.simplyDegrees)
            val endRad = rad + radians(sweepAngle)
            var nextRad: Float
            val calculateNext: (current: Float, limit: Float) -> Float
            // 找第一段弧的结束角度
            if (sweepAngle > 0) {
                nextRad = min(ceil(rad / HALF_PI) * HALF_PI, endRad)
                calculateNext = ::nextRadiansForward
            } else {
                nextRad = max(floor(rad / HALF_PI) * HALF_PI, endRad)
                calculateNext = ::nextRadiansBackward
            }
            if ((rad - nextRad).isZero()) {
                // 第一段弧的起始角度和结束角度相等，说明起点刚好在一个象限的边上，直接跳过
                rad = nextRad
                nextRad = calculateNext(rad, endRad)
            }
            // move to start
            var startX = centerX + radiusX * cos(rad)
            var startY = centerY + radiusY * sin(rad)
            if (moveTo) {
                moveToWithDensity(startX, startY)
            } else {
                lineToWithDensity(startX, startY)
            }
            do {
                // draw quarter arc
                val endX = centerX + radiusX * cos(nextRad)
                val endY = centerY + radiusY * sin(nextRad)
                val c = tan((nextRad - rad) * 0.25f) * 4f / 3f // 控制点常量
                val ctrlW = radiusX * c
                val ctrlH = radiusY * c
                bezierCurveToWithDensity(
                    startX - ctrlW * sin(rad),
                    startY + ctrlH * cos(rad),
                    endX + ctrlW * sin(nextRad),
                    endY - ctrlH * cos(nextRad),
                    endX,
                    endY
                )
                // 找下一段
                startX = endX
                startY = endY
                rad = nextRad
                nextRad = calculateNext(rad, endRad)
            } while (!(rad - endRad).isZero())
        }

        private inline fun MutableRect.reset() = set(
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )

        private val MutableRect.valid: Boolean
            get() = left != Float.POSITIVE_INFINITY && right != Float.NEGATIVE_INFINITY &&
                    top != Float.POSITIVE_INFINITY && bottom != Float.NEGATIVE_INFINITY

        private fun MutableRect.offset(x: Float, y: Float) {
            left += x
            top += y
            right += x
            bottom += y
        }

        private fun MutableRect.expandPoint(x: Float, y: Float) {
            if (valid) {
                left = min(left, x)
                top = min(top, y)
                right = max(right, x)
                bottom = max(bottom, y)
            } else {
                set(x, y, x, y)
            }
        }

        private fun MutableRect.expandPoint(point: Offset) = expandPoint(point.x, point.y)

        private fun MutableRect.expandRect(rect: Rect) {
            if (valid) {
                left = min(left, rect.left)
                top = min(top, rect.top)
                right = max(right, rect.right)
                bottom = max(bottom, rect.bottom)
            } else {
                set(rect.left, rect.top, rect.right, rect.bottom)
            }
        }

        private fun MutableRect.expandArc(
            rect: Rect,
            startAngleDegrees: Float,
            sweepAngleDegrees: Float
        ) {
            if (abs(sweepAngleDegrees) < 360) {
                val radiusX = rect.width * 0.5f
                val radiusY = rect.height * 0.5f
                val center = rect.center
                val startPoint = center + pointOfArc(radiusX, radiusY, startAngleDegrees)
                val endPoint =
                    center + pointOfArc(radiusX, radiusY, startAngleDegrees + sweepAngleDegrees)
                expandPoint(startPoint)
                expandPoint(endPoint)
                val angle0: Float
                val angle1: Float
                if (sweepAngleDegrees < 0) {
                    angle0 = (startAngleDegrees + sweepAngleDegrees).simplyDegrees
                    angle1 = angle0 - sweepAngleDegrees
                } else {
                    angle0 = startAngleDegrees.simplyDegrees
                    angle1 = startAngleDegrees + sweepAngleDegrees
                }
                if (angle0 < 90 && angle1 > 90) {
                    expandPoint(rect.bottomCenter)
                }
                if (angle0 < 180 && angle1 > 180) {
                    expandPoint(rect.centerLeft)
                }
                if (angle0 < 270 && angle1 > 270) {
                    expandPoint(rect.topCenter)
                }
                if (angle0 < 360 && angle1 > 360) {
                    expandPoint(rect.centerRight)
                }
            } else {
                expandRect(rect)
            }
        }
    }

    private val ops = mutableListOf<CanvasOp>()

    /**
     * current X for creating Path, not available inside CanvasOp
     */
    private var currentX = 0f

    /**
     * current X for creating Path, not available inside CanvasOp
     */
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f
    internal var closed = false
    private var offsetX = 0f
    private var offsetY = 0f

    private val bounds = MutableRect(
        Float.POSITIVE_INFINITY,
        Float.POSITIVE_INFINITY,
        Float.NEGATIVE_INFINITY,
        Float.NEGATIVE_INFINITY
    )

    override val isConvex: Boolean
        get() = TODO("Not yet implemented")
    override val isEmpty: Boolean
        get() = ops.isEmpty()

    override fun moveTo(x: Float, y: Float) {
        if (isEmpty) {
            startX = x
            startY = y
        }
        currentX = x
        currentY = y
        bounds.expandPoint(x, y)
        ops += { moveToWithDensity(offsetX + x, offsetY + y) }
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        moveTo(currentX + dx, currentY + dy)
    }

    override fun lineTo(x: Float, y: Float) {
        currentX = x
        currentY = y
        bounds.expandPoint(x, y)
        ops += { lineToWithDensity(offsetX + x, offsetY + y) }
    }

    override fun relativeLineTo(dx: Float, dy: Float) {
        lineTo(currentX + dx, currentY + dy)
    }

    override fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        currentX = x2
        currentY = y2
        bounds.expandPoint(x1, y1) // FIXME: 直接用控制点坐标是不对的，应该换算为解析式的极值点
        bounds.expandPoint(x2, y2)
        ops += {
            quadraticCurveToWithDensity(
                offsetX + x1,
                offsetY + y1,
                offsetX + x2,
                offsetY + y2
            )
        }
    }

    override fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        quadraticBezierTo(currentX + dx1, currentY + dy1, currentX + dx2, currentY + dy2)
    }

    override fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        currentX = x3
        currentY = y3
        bounds.expandPoint(x1, y1) // FIXME: 直接用控制点坐标是不对的，应该换算为解析式的极值点
        bounds.expandPoint(x2, y2) // FIXME: 直接用控制点坐标是不对的，应该换算为解析式的极值点
        bounds.expandPoint(x3, y3)
        ops += {
            bezierCurveToWithDensity(
                offsetX + x1,
                offsetY + y1,
                offsetX + x2,
                offsetY + y2,
                offsetX + x3,
                offsetY + y3
            )
        }
    }

    override fun relativeCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) {
        cubicTo(
            currentX + dx1,
            currentY + dy1,
            currentX + dx2,
            currentY + dy2,
            currentX + dx3,
            currentY + dy3
        )
    }

    override fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    ) {
        val radiusX = rect.width * 0.5f
        val radiusY = rect.height * 0.5f
        val center = rect.center
        val startPoint = center + pointOfArc(radiusX, radiusY, startAngleDegrees)
        val endPoint = center + pointOfArc(radiusX, radiusY, startAngleDegrees + sweepAngleDegrees)
        if (isEmpty) {
            startX = startPoint.x
            startY = startPoint.y
        }
        currentX = endPoint.x
        currentY = endPoint.y
        bounds.expandArc(rect, startAngleDegrees, sweepAngleDegrees)
        if ((radiusX - radiusY).isZero()) {
            val moveTo = forceMoveTo && !isEmpty
            ops += {
                if (moveTo) {
                    moveToWithDensity(offsetX + startPoint.x, offsetY + startPoint.y)
                }
                drawCircularArc(
                    offsetX + center.x,
                    offsetY + center.y,
                    radiusX,
                    startAngleDegrees,
                    sweepAngleDegrees
                )
            }
        } else {
            val moveTo = forceMoveTo || isEmpty
            ops += {
                drawOvalArc(
                    offsetX + center.x,
                    offsetY + center.y,
                    radiusX,
                    radiusY,
                    startAngleDegrees,
                    sweepAngleDegrees,
                    moveTo
                )
            }
        }
    }

    override fun addRect(rect: Rect) {
        val originalX = currentX
        val originalY = currentY
        bounds.expandRect(rect)
        ops += {
            val startX = (rect.left + rect.right) * 0.5f // 从顶边中点开始画，避免strokeJoin问题
            moveToWithDensity(offsetX + startX, offsetY + rect.top)
            lineToWithDensity(offsetX + rect.right, offsetY + rect.top)
            lineToWithDensity(offsetX + rect.right, offsetY + rect.bottom)
            lineToWithDensity(offsetX + rect.left, offsetY + rect.bottom)
            lineToWithDensity(offsetX + rect.left, offsetY + rect.top)
            lineToWithDensity(offsetX + startX, offsetY + rect.top)
            // move back to the original position
            moveToWithDensity(offsetX + originalX, offsetY + originalY)
        }
    }

    override fun addOval(oval: Rect) = addArc(oval, 0f, 360f)

    override fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float) = addArc(
        oval,
        degrees(startAngleRadians),
        degrees(sweepAngleRadians)
    )

    override fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
        val originalX = currentX
        val originalY = currentY
        val radiusX = oval.width * 0.5f
        val radiusY = oval.height * 0.5f
        val center = oval.center
        bounds.expandArc(oval, startAngleDegrees, sweepAngleDegrees)
        if ((radiusX - radiusY).isZero()) {
            val startPoint = center + pointOfArc(radiusX, radiusY, startAngleDegrees)
            ops += {
                moveToWithDensity(offsetX + startPoint.x, offsetY + startPoint.y)
                drawCircularArc(
                    offsetX + center.x,
                    offsetY + center.y,
                    radiusX,
                    startAngleDegrees,
                    sweepAngleDegrees
                )
                moveToWithDensity(offsetX + originalX, offsetY + originalY)
            }
        } else {
            ops += {
                drawOvalArc(
                    offsetX + center.x,
                    offsetY + center.y,
                    radiusX,
                    radiusY,
                    startAngleDegrees,
                    sweepAngleDegrees,
                    true
                )
                moveToWithDensity(offsetX + originalX, offsetY + originalY)
            }
        }
    }

    override fun addRoundRect(roundRect: RoundRect) {
        val originalX = currentX
        val originalY = currentY
        val topLeftRadius = roundRect.topLeftCornerRadius.radius
        val topRightRadius = roundRect.topRightCornerRadius.radius
        val bottomRightRadius = roundRect.bottomRightCornerRadius.radius
        val bottomLeftRadius = roundRect.bottomLeftCornerRadius.radius
        bounds.expandRect(roundRect.boundingRect)
        ops += {
            moveToWithDensity(offsetX + roundRect.left, offsetY + roundRect.top + topLeftRadius)
            arcWithDensity(
                offsetX + roundRect.left + topLeftRadius,
                offsetY + roundRect.top + topLeftRadius,
                topLeftRadius,
                PI.toFloat(),
                1.5f * PI.toFloat(),
                false
            )
            lineToWithDensity(offsetX + roundRect.right - topRightRadius, offsetY + roundRect.top)
            arcWithDensity(
                offsetX + roundRect.right - topRightRadius,
                offsetY + roundRect.top + topRightRadius,
                topRightRadius,
                1.5f * PI.toFloat(),
                2f * PI.toFloat(),
                false
            )
            lineToWithDensity(
                offsetX + roundRect.right,
                offsetY + roundRect.bottom - bottomRightRadius
            )
            arcWithDensity(
                offsetX + roundRect.right - bottomRightRadius,
                offsetY + roundRect.bottom - bottomRightRadius,
                bottomRightRadius,
                0f,
                0.5f * PI.toFloat(),
                false
            )
            lineToWithDensity(
                offsetX + roundRect.left + bottomLeftRadius,
                offsetY + roundRect.bottom
            )
            arcWithDensity(
                offsetX + roundRect.left + bottomLeftRadius,
                offsetY + roundRect.bottom - bottomLeftRadius,
                bottomLeftRadius,
                0.5f * PI.toFloat(),
                PI.toFloat(),
                false
            )
            lineToWithDensity(offsetX + roundRect.left, offsetY + roundRect.top + topLeftRadius)
            moveToWithDensity(offsetX + originalX, offsetY + originalY)
        }
    }

    override fun addPath(path: Path, offset: Offset) {
        if (path !is KuiklyPath) {
            throw IllegalArgumentException("require KuiklyPath, but got ${path::class.simpleName}")
        }
        val originalX = currentX
        val originalY = currentY
        bounds.expandRect(path.getBounds().translate(offset))
        ops += {
            // offset the path
            path.offsetX += offsetX + offset.x
            path.offsetY += offsetY + offset.y
            // draw the path
            path.draw(this)
            if (path.closed) {
                lineToWithDensity(
                    offsetX + path.offsetX + path.startX,
                    offsetY + path.offsetY + path.startY
                )
            }
            // restore the path
            path.offsetX -= offsetX + offset.x
            path.offsetY -= offsetX + offset.y
            moveToWithDensity(offsetX + originalX, offsetY + originalY)
        }
    }

    override fun close() {
        closed = true
    }

    override fun reset() {
        ops.clear()
        closed = false
        currentX = 0f
        currentY = 0f
        startX = 0f
        startY = 0f
        bounds.reset()
    }

    override fun translate(offset: Offset) {
        bounds.offset(offset.x, offset.y)
        offsetX += offset.x
        offsetY += offset.y
        ops += {
            offsetX -= offset.x
            offsetY -= offset.y
        }
    }

    override fun getBounds(): Rect {
        return if (bounds.valid) {
            bounds.toRect()
        } else {
            Rect.Zero
        }
    }

    fun draw(context: CanvasContextEx) {
        val originOffsetX = offsetX
        val originOffsetY = offsetY
        ops.fastForEach { context.it() }
        offsetX = originOffsetX
        offsetY = originOffsetY
    }
}
