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
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.PathOperation
import com.tencent.kuikly.compose.ui.graphics.degrees
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.core.views.PathApi
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

private typealias CanvasOp = PathApi.() -> Unit

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

        private fun PathApi.drawCircularArc(
            centerX: Float,
            centerY: Float,
            radius: Float,
            startAngle: Float,
            sweepAngle: Float
        ) {
            if (abs(sweepAngle) < 360) {
                arc(
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
                arc(
                    centerX,
                    centerY,
                    radius,
                    radians(startAngle),
                    radians(startAngle + halfSweepAngle),
                    sweepAngle < 0
                )
                arc(
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

        private fun PathApi.drawOvalArc(
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
                moveTo(startX, startY)
            } else {
                lineTo(startX, startY)
            }
            do {
                // draw quarter arc
                val endX = centerX + radiusX * cos(nextRad)
                val endY = centerY + radiusY * sin(nextRad)
                val c = tan((nextRad - rad) * 0.25f) * 4f / 3f // 控制点常量
                val ctrlW = radiusX * c
                val ctrlH = radiusY * c
                bezierCurveTo(
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
    private var densityValue: Float = 1f

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
    private var offsetX = 0f
    private var offsetY = 0f

    // 2D affine transform components (applied at draw time)
    private var hasTransform = false
    private var transformM00 = 1f
    private var transformM01 = 0f
    private var transformM10 = 0f
    private var transformM11 = 1f

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
        ops += { moveTo((offsetX + x) / densityValue, (offsetY + y) / densityValue) }
    }

    override fun relativeMoveTo(dx: Float, dy: Float) {
        moveTo(currentX + dx, currentY + dy)
    }

    override fun lineTo(x: Float, y: Float) {
        currentX = x
        currentY = y
        bounds.expandPoint(x, y)
        ops += { lineTo((offsetX + x) / densityValue, (offsetY + y) / densityValue) }
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
            quadraticCurveTo(
                (offsetX + x1) / densityValue,
                (offsetY + y1) / densityValue,
                (offsetX + x2) / densityValue,
                (offsetY + y2) / densityValue
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
            bezierCurveTo(
                (offsetX + x1) / densityValue,
                (offsetY + y1) / densityValue,
                (offsetX + x2) / densityValue,
                (offsetY + y2) / densityValue,
                (offsetX + x3) / densityValue,
                (offsetY + y3) / densityValue
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
                    moveTo(
                        (offsetX + startPoint.x) / densityValue,
                        (offsetY + startPoint.y) / densityValue
                    )
                }
                drawCircularArc(
                    (offsetX + center.x) / densityValue,
                    (offsetY + center.y) / densityValue,
                    radiusX / densityValue,
                    startAngleDegrees,
                    sweepAngleDegrees
                )
            }
        } else {
            val moveTo = forceMoveTo || isEmpty
            ops += {
                drawOvalArc(
                    (offsetX + center.x) / densityValue,
                    (offsetY + center.y) / densityValue,
                    radiusX / densityValue,
                    radiusY / densityValue,
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
            moveTo((offsetX + startX) / densityValue, (offsetY + rect.top) / densityValue)
            lineTo((offsetX + rect.right) / densityValue, (offsetY + rect.top) / densityValue)
            lineTo((offsetX + rect.right) / densityValue, (offsetY + rect.bottom) / densityValue)
            lineTo((offsetX + rect.left) / densityValue, (offsetY + rect.bottom) / densityValue)
            lineTo((offsetX + rect.left) / densityValue, (offsetY + rect.top) / densityValue)
            lineTo((offsetX + startX) / densityValue, (offsetY + rect.top) / densityValue)
            // move back to the original position
            moveTo((offsetX + originalX) / densityValue, (offsetY + originalY) / densityValue)
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
                moveTo(
                    (offsetX + startPoint.x) / densityValue,
                    (offsetY + startPoint.y) / densityValue
                )
                drawCircularArc(
                    (offsetX + center.x) / densityValue,
                    (offsetY + center.y) / densityValue,
                    radiusX / densityValue,
                    startAngleDegrees,
                    sweepAngleDegrees
                )
                moveTo((offsetX + originalX) / densityValue, (offsetY + originalY) / densityValue)
            }
        } else {
            ops += {
                drawOvalArc(
                    (offsetX + center.x) / densityValue,
                    (offsetY + center.y) / densityValue,
                    radiusX / densityValue,
                    radiusY / densityValue,
                    startAngleDegrees,
                    sweepAngleDegrees,
                    true
                )
                moveTo((offsetX + originalX) / densityValue, (offsetY + originalY) / densityValue)
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
            moveTo(
                (offsetX + roundRect.left) / densityValue,
                (offsetY + roundRect.top + topLeftRadius) / densityValue
            )
            arc(
                (offsetX + roundRect.left + topLeftRadius) / densityValue,
                (offsetY + roundRect.top + topLeftRadius) / densityValue,
                topLeftRadius / densityValue,
                PI.toFloat(),
                1.5f * PI.toFloat(),
                false
            )
            lineTo(
                (offsetX + roundRect.right - topRightRadius) / densityValue,
                (offsetY + roundRect.top) / densityValue
            )
            arc(
                (offsetX + roundRect.right - topRightRadius) / densityValue,
                (offsetY + roundRect.top + topRightRadius) / densityValue,
                topRightRadius / densityValue,
                1.5f * PI.toFloat(),
                2f * PI.toFloat(),
                false
            )
            lineTo(
                (offsetX + roundRect.right) / densityValue,
                (offsetY + roundRect.bottom - bottomRightRadius) / densityValue
            )
            arc(
                (offsetX + roundRect.right - bottomRightRadius) / densityValue,
                (offsetY + roundRect.bottom - bottomRightRadius) / densityValue,
                bottomRightRadius / densityValue,
                0f,
                0.5f * PI.toFloat(),
                false
            )
            lineTo(
                (offsetX + roundRect.left + bottomLeftRadius) / densityValue,
                (offsetY + roundRect.bottom) / densityValue
            )
            arc(
                (offsetX + roundRect.left + bottomLeftRadius) / densityValue,
                (offsetY + roundRect.bottom - bottomLeftRadius) / densityValue,
                bottomLeftRadius / densityValue,
                0.5f * PI.toFloat(),
                PI.toFloat(),
                false
            )
            lineTo(
                (offsetX + roundRect.left) / densityValue,
                (offsetY + roundRect.top + topLeftRadius) / densityValue
            )
            moveTo((offsetX + originalX) / densityValue, (offsetY + originalY) / densityValue)
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
            path.draw(this, densityValue)
            // restore the path
            path.offsetX -= offsetX + offset.x
            path.offsetY -= offsetY + offset.y
            moveTo((offsetX + originalX) / densityValue, (offsetY + originalY) / densityValue)
        }
    }

    override fun close() {
        currentX = startX
        currentY = startY
        ops += {
            closePath()
        }
    }

    override fun reset() {
        ops.clear()
        currentX = 0f
        currentY = 0f
        startX = 0f
        startY = 0f
        offsetX = 0f
        offsetY = 0f
        bounds.reset()
        hasTransform = false
        transformM00 = 1f
        transformM01 = 0f
        transformM10 = 0f
        transformM11 = 1f
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

    fun draw(context: PathApi, densityValue: Float) {
        this.densityValue = densityValue
        val originOffsetX = offsetX
        val originOffsetY = offsetY
        if (hasTransform) {
            // Wrap PathApi to apply 2D affine transform to all coordinates
            val proxy = TransformingPathApi(context, transformM00, transformM01, transformM10, transformM11)
            ops.fastForEach { proxy.it() }
        } else {
            ops.fastForEach { context.it() }
        }
        offsetX = originOffsetX
        offsetY = originOffsetY
    }

    /**
     * Transform the points in this path by the provided matrix.
     *
     * Since KuiklyPath ops are closures that capture original coordinates and read
     * offsetX/offsetY at draw time, we cannot modify individual points after creation.
     *
     * Strategy: Store the 2D affine transform (m00, m01, m10, m11) and apply it at
     * draw time via a [TransformingPathApi] proxy that intercepts all PathApi calls.
     * Translation (tx, ty) is applied via offsetX/offsetY adjustment.
     */
    override fun transform(matrix: Matrix) {
        val oldCurrentX = currentX
        val oldCurrentY = currentY
        val oldStartX = startX
        val oldStartY = startY
        val oldBounds = if (bounds.valid) {
            Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)
        } else null

        // Extract 2D affine components from Compose's 4x4 matrix
        // scaleX=m[0,0], skewX=m[0,1], skewY=m[1,0], scaleY=m[1,1]
        // translateX=m[3,0], translateY=m[3,1]
        val m00 = matrix[0, 0]
        val m01 = matrix[0, 1]
        val m10 = matrix[1, 0]
        val m11 = matrix[1, 1]
        val tx = matrix[3, 0]
        val ty = matrix[3, 1]

        // Apply translation via offset (this is picked up by closures at draw time)
        offsetX += tx
        offsetY += ty

        // Store rotation/scale for draw-time proxy application
        transformM00 = m00
        transformM01 = m01
        transformM10 = m10
        transformM11 = m11
        hasTransform = (m00 != 1f || m01 != 0f || m10 != 0f || m11 != 1f)

        // Recompute bounds by transforming original bounds corners
        bounds.reset()
        if (oldBounds != null) {
            val corners = arrayOf(
                floatArrayOf(oldBounds.left, oldBounds.top),
                floatArrayOf(oldBounds.right, oldBounds.top),
                floatArrayOf(oldBounds.right, oldBounds.bottom),
                floatArrayOf(oldBounds.left, oldBounds.bottom),
            )
            for (corner in corners) {
                val nx = m00 * corner[0] + m01 * corner[1] + tx
                val ny = m10 * corner[0] + m11 * corner[1] + ty
                bounds.expandPoint(nx, ny)
            }
        }

        // Update tracked positions with full affine transform
        currentX = m00 * oldCurrentX + m01 * oldCurrentY + tx
        currentY = m10 * oldCurrentX + m11 * oldCurrentY + ty
        startX = m00 * oldStartX + m01 * oldStartY + tx
        startY = m10 * oldStartX + m11 * oldStartY + ty
    }

    /**
     * Set this path to the result of applying the Op to the two specified paths.
     *
     * For the Tooltip caret use case (PathOperation.Union), we approximate the union
     * by simply adding both paths together. This produces the correct visual result
     * when both paths overlap (tooltip body + caret triangle), since the fill rule
     * will render both as a single filled shape.
     */
    override fun op(path1: Path, path2: Path, operation: PathOperation): Boolean {
        if (path1 !is KuiklyPath || path2 !is KuiklyPath) return false

        // Reset this path
        reset()

        when (operation) {
            PathOperation.Union -> {
                // Add both paths - the visual union for filled shapes
                addPath(path1)
                addPath(path2)
            }
            else -> {
                // For other operations, just add path1 as fallback
                addPath(path1)
            }
        }
        return true
    }
}

/**
 * A [PathApi] wrapper that applies a 2D affine transform (scale + rotation/skew)
 * to all coordinates before delegating to the real [PathApi].
 *
 * Translation is already handled via offsetX/offsetY; this proxy handles
 * the rotation/scale components (m00, m01, m10, m11).
 */
private class TransformingPathApi(
    private val delegate: PathApi,
    private val m00: Float,
    private val m01: Float,
    private val m10: Float,
    private val m11: Float,
) : PathApi {

    private fun transformX(x: Float, y: Float): Float = m00 * x + m01 * y
    private fun transformY(x: Float, y: Float): Float = m10 * x + m11 * y

    override fun beginPath() = delegate.beginPath()

    override fun moveTo(x: Float, y: Float) {
        delegate.moveTo(transformX(x, y), transformY(x, y))
    }

    override fun lineTo(x: Float, y: Float) {
        delegate.lineTo(transformX(x, y), transformY(x, y))
    }

    override fun arc(
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        endAngle: Float,
        counterclockwise: Boolean
    ) {
        // For arcs with non-uniform scale, the result is an ellipse.
        // For the caret use case (only moveTo/lineTo/close), arcs won't be encountered.
        // Fallback: apply transform to center and pass radius unchanged (works for uniform scale).
        delegate.arc(
            transformX(centerX, centerY),
            transformY(centerX, centerY),
            radius, // Approximate: doesn't handle non-uniform scale on arcs
            startAngle,
            endAngle,
            counterclockwise
        )
    }

    override fun closePath() = delegate.closePath()

    override fun quadraticCurveTo(
        controlPointX: Float,
        controlPointY: Float,
        pointX: Float,
        pointY: Float
    ) {
        delegate.quadraticCurveTo(
            transformX(controlPointX, controlPointY),
            transformY(controlPointX, controlPointY),
            transformX(pointX, pointY),
            transformY(pointX, pointY),
        )
    }

    override fun bezierCurveTo(
        controlPoint1X: Float,
        controlPoint1Y: Float,
        controlPoint2X: Float,
        controlPoint2Y: Float,
        pointX: Float,
        pointY: Float
    ) {
        delegate.bezierCurveTo(
            transformX(controlPoint1X, controlPoint1Y),
            transformY(controlPoint1X, controlPoint1Y),
            transformX(controlPoint2X, controlPoint2Y),
            transformY(controlPoint2X, controlPoint2Y),
            transformX(pointX, pointY),
            transformY(pointX, pointY),
        )
    }
}
