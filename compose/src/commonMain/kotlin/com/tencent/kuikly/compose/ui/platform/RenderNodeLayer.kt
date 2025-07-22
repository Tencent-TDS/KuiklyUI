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

package com.tencent.kuikly.compose.ui.platform

import com.tencent.kuikly.compose.ui.geometry.MutableRect
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.CompositingStrategy
import com.tencent.kuikly.compose.ui.graphics.DefaultCameraDistance
import com.tencent.kuikly.compose.ui.graphics.DefaultShadowColor
import com.tencent.kuikly.compose.ui.graphics.Fields
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Outline
import com.tencent.kuikly.compose.ui.graphics.RenderEffect
import com.tencent.kuikly.compose.ui.graphics.ReusableGraphicsLayerScope
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.node.KNode.Companion.alpha
import com.tencent.kuikly.compose.ui.node.KNode.Companion.borderRadius
import com.tencent.kuikly.compose.ui.node.KNode.Companion.clip
import com.tencent.kuikly.compose.ui.node.KNode.Companion.measuredSize
import com.tencent.kuikly.compose.ui.node.KNode.Companion.rotate
import com.tencent.kuikly.compose.ui.node.KNode.Companion.scale
import com.tencent.kuikly.compose.ui.node.KNode.Companion.shadow
import com.tencent.kuikly.compose.ui.node.KNode.Companion.translate
import com.tencent.kuikly.compose.ui.node.OwnedLayer
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.max

internal class RenderNodeLayer(
    private val invalidateParentLayer: () -> Unit,
    private val drawBlock: (Canvas) -> Unit,
    private val onDestroy: () -> Unit = {},
) : OwnedLayer {
    private var size = IntSize.Zero
    private var position = IntOffset.Zero
    private var outline: Outline? = null
    private var matrixInvalided = true
    // Internal for testing
    internal val matrix = Matrix()
        get() {
            if (matrixInvalided) {
                updateMatrix(field)
                matrixInvalided = false
            }
            return field
        }
    private val inverseMatrix: Matrix
        get() = Matrix().apply {
            matrix.invertTo(this)
        }

    private var isDestroyed = false

    private var transformOrigin: TransformOrigin = TransformOrigin.Center
    private var translationX: Float = 0f
    private var translationY: Float = 0f
    private var rotationX: Float = 0f
    private var rotationY: Float = 0f
    private var rotationZ: Float = 0f
    private var cameraDistance: Float = DefaultCameraDistance
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f
    private var alpha: Float = 1f
    private var clip: Boolean = false
    private var renderEffect: RenderEffect? = null
    private var shadowElevation: Float = 0f
    private var ambientShadowColor: Color = DefaultShadowColor
    private var spotShadowColor: Color = DefaultShadowColor
    private var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    override fun destroy() {
        isDestroyed = true
        onDestroy()
    }

    override fun reuseLayer(drawBlock: (Canvas) -> Unit, invalidateParentLayer: () -> Unit) {
        TODO("Not yet implemented")
    }

    override fun resize(size: IntSize) {
        if (size != this.size) {
            this.size = size
            // updateMatrix()
            matrixInvalided = true
            invalidate()
        }
    }

    override fun move(position: IntOffset) {
        if (position != this.position) {
            this.position = position
            invalidateParentLayer()
        }
    }

    override fun mapOffset(point: Offset, inverse: Boolean): Offset {
        return if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(point)
    }

    override fun mapBounds(rect: MutableRect, inverse: Boolean) {
        if (inverse) {
            inverseMatrix
        } else {
            matrix
        }.map(rect)
    }

    override fun isInLayer(position: Offset): Boolean {
        if (!clip) {
            return true
        }

        val x = position.x
        val y = position.y
        val outline = outline ?: return true

        return isInOutline(outline, x, y)
    }

    private var mutatedFields: Int = 0

    override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
        val maybeChangedFields = scope.mutatedFields or mutatedFields
        this.transformOrigin = scope.transformOrigin
        this.translationX = scope.translationX
        this.translationY = scope.translationY
        this.rotationX = scope.rotationX
        this.rotationY = scope.rotationY
        this.rotationZ = scope.rotationZ
        this.cameraDistance = max(scope.cameraDistance, 0.001f)
        this.scaleX = scope.scaleX
        this.scaleY = scope.scaleY
        this.alpha = scope.alpha
        this.clip = scope.clip
        this.shadowElevation = scope.shadowElevation
        this.renderEffect = scope.renderEffect
        this.ambientShadowColor = scope.ambientShadowColor
        this.spotShadowColor = scope.spotShadowColor
        this.compositingStrategy = scope.compositingStrategy
        this.outline = scope.outline
        if (maybeChangedFields and Fields.MatrixAffectingFields != 0) {
            // updateMatrix()
            matrixInvalided = true
        }
        invalidate()
        mutatedFields = scope.mutatedFields
    }

    private fun updateMatrix(matrix: Matrix) {
        val pivotX = transformOrigin.pivotFractionX * size.width
        val pivotY = transformOrigin.pivotFractionY * size.height
        matrix.reset()
        matrix.translate(x = -pivotX, y = -pivotY)
        matrix *= Matrix().apply {
            rotateZ(rotationZ)
            rotateY(rotationY)
            rotateX(rotationX)
            scale(scaleX, scaleY)
        }
        // Perspective transform should be applied only in case of rotations to avoid
        // multiply application in hierarchies.
        // See Android's frameworks/base/libs/hwui/RenderProperties.cpp for reference
        if (!rotationX.isZero() || !rotationY.isZero()) {
            matrix *= Matrix().apply {
                // The camera location is passed in inches, set in pt
                val depth = cameraDistance * 72f
                this[2, 3] = -1f / depth
            }
        }
        matrix *= Matrix().apply {
            translate(x = pivotX + translationX, y = pivotY + translationY)
        }

        // Third column and row are irrelevant for 2D space.
        // Zeroing required to get correct inverse transformation matrix.
        matrix[2, 0] = 0f
        matrix[2, 1] = 0f
        matrix[2, 3] = 0f
        matrix[0, 2] = 0f
        matrix[1, 2] = 0f
        matrix[3, 2] = 0f
    }

    override fun invalidate() {
        invalidateParentLayer()
    }

    override fun drawLayer(canvas: Canvas) {
        performDrawLayer(canvas)
    }

    override fun transform(matrix: Matrix) {
        matrix.timesAssign(this.matrix)
    }

    override fun inverseTransform(matrix: Matrix) {
        matrix.timesAssign(inverseMatrix)
    }

    private fun performDrawLayer(canvas: Canvas) {
        if (alpha > 0f) {
            canvas.view?.apply {
                // todo deal with position
                measuredSize(size.width, size.height)
                // todo deal with transformOrigin.pivotFractionX, transformOrigin.pivotFractionY
                translate(translationX, translationY)
                scale(scaleX, scaleY)
                rotate(rotationX, rotationY, rotationZ)
                // 0.19f is from the Original Compose Multiplatform, which is the default spotShadowAlpha in Android.
                shadow(shadowElevation, spotShadowColor.copy(alpha = 0.19f * alpha))
                // todo renderEffect
                alpha(alpha)
                if (clip) {
                    (outline as? Outline.Rounded)?.also {
                        borderRadius(it.roundRect)
                    }
                    clip()
                }
            }
            drawBlock(canvas)
        } else {
            canvas.view?.alpha(0f)
        }
    }

    override fun updateDisplayList() = Unit
}

// Copy from Android's frameworks/base/libs/hwui/utils/MathUtils.h
private const val NON_ZERO_EPSILON = 0.001f
private inline fun Float.isZero(): Boolean = abs(this) <= NON_ZERO_EPSILON
