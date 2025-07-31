/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Outline
import com.tencent.kuikly.compose.ui.graphics.RectangleShape
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.KNode.Companion.borderRadius
import com.tencent.kuikly.compose.ui.node.LayoutAwareModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.Nodes
import com.tencent.kuikly.compose.ui.node.requireCoordinator
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.platform.debugInspectorInfo
import com.tencent.kuikly.compose.ui.text.style.modulate
import com.tencent.kuikly.compose.ui.unit.toSize
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.exception.throwRuntimeError

/**
 * Draws [shape] with a solid [color] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundColor
 *
 * @param color color to paint background with
 * @param shape desired shape of the background
 */
@Stable
fun Modifier.background(
    color: Color,
    shape: Shape = RectangleShape
): Modifier {
    return this.then(
        BackgroundElement(
            color = color,
            alpha = 1f,
            shape = shape,
            inspectorInfo = debugInspectorInfo {
                name = "background"
                value = color
                properties["color"] = color
                properties["shape"] = shape
            }
        )
    )
}

/**
 * Draws [shape] with [brush] behind the content.
 *
 * @sample androidx.compose.foundation.samples.DrawBackgroundShapedBrush
 *
 * @param brush brush to paint background with
 * @param shape desired shape of the background
 * @param alpha Opacity to be applied to the [brush], with `0` being completely transparent and
 * `1` being completely opaque. The value must be between `0` and `1`.
 */
@Stable
fun Modifier.background(
    brush: Brush,
    shape: Shape = RectangleShape,
    @FloatRange(from = 0.0, to = 1.0)
    alpha: Float = 1.0f
): Modifier {
    val color = if (brush is SolidColor) brush.value else Color.Unspecified
    return this.then(
        BackgroundElement(
            color = color,
            alpha = alpha,
            shape = shape,
            brush = brush,
            inspectorInfo = debugInspectorInfo {
                name = "background"
                properties["alpha"] = alpha
                properties["brush"] = brush
                properties["shape"] = shape
            }
        )
    )
}

private class BackgroundElement(
    private val color: Color = Color.Unspecified,
    private val alpha: Float,
    private val shape: Shape,
    private val inspectorInfo: InspectorInfo.() -> Unit,
    private val brush: Brush? = null,
) : ModifierNodeElement<BackgroundNode>() {
    override fun create(): BackgroundNode {
        return BackgroundNode(
            color,
            alpha,
            shape,
            brush
        )
    }

    override fun update(node: BackgroundNode) {
        node.color = color
        node.alpha = alpha
        node.shape = shape
        node.brush = brush
    }

    override fun InspectorInfo.inspectableProperties() {
        inspectorInfo()
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? BackgroundElement ?: return false
        return color == otherModifier.color &&
                alpha == otherModifier.alpha &&
                shape == otherModifier.shape &&
                brush == otherModifier.brush
    }
}

private class BackgroundNode(
    var color: Color,
    var alpha: Float,
    shape: Shape,
    var brush: Brush? = null,
) : DrawModifierNode, LayoutAwareModifierNode, Modifier.Node() {

    var shape: Shape = shape
        set(value) {
            if (field != value) {
                field = value
                outline = null
            }
        }

    private var outline: Outline? = null

    override fun onReset() {
        super.onReset()
        // todo: jonas 清理view转态
    }

    override fun onMeasureResultChanged() {
        outline = null
    }

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        if (view == null) {
            throwRuntimeError("view null")
        }
        if (outline == null && shape != RectangleShape) {
            val size = requireCoordinator(Nodes.LayoutAware).size.toSize()
            outline = shape.createOutline(size, layoutDirection, this)
        }
        (outline as? Outline.Rounded)?.also {
            view!!.borderRadius(it.roundRect)
        }
        view?.getViewAttr()?.run {
            if (brush != null) {
                brush!!.applyTo(view, alpha)
            } else {
                backgroundColor(color.modulate(alpha).toKuiklyColor())
            }
        }
        drawContent()
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        super.onPlaced(coordinates)

        (requireLayoutNode() as? KNode<*>)?.run {
            if (kuiklyCoordinates == null) {
                kuiklyCoordinates = coordinates
            }
        }
    }
}
