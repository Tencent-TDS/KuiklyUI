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

package com.tencent.kuikly.compose.foundation

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
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.toSize
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.exception.throwRuntimeError

/**
 * Modify element to add border with appearance specified with a [border] and a [shape] and clip it.
 *
 * @param border [BorderStroke] class that specifies border appearance, such as size and color
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(border: BorderStroke, shape: Shape = RectangleShape) =
    border(width = border.width, brush = border.brush, shape = shape)

/**
 * Modify element to add border with appearance specified with a [width], a [color] and a [shape]
 * and clip it.
 *
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param color color to paint the border with
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape) =
    border(width, SolidColor(color), shape)

/**
 * Modify element to add border with appearance specified with a [width], a [brush] and a [shape]
 * and clip it.
 *
 * @param width width of the border. Use [Dp.Hairline] for a hairline border.
 * @param brush brush to paint the border with
 * @param shape shape of the border
 */
@Stable
fun Modifier.border(width: Dp, brush: Brush, shape: Shape) =
    this then BorderModifierNodeElement(width, brush, shape)

internal data class BorderModifierNodeElement(
    val width: Dp,
    val brush: Brush,
    val shape: Shape
) : ModifierNodeElement<BorderModifierNode>() {
    override fun create() = BorderModifierNode(width, brush, shape)

    override fun update(node: BorderModifierNode) {
        node.width = width
        node.brush = brush
        node.shape = shape
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "border"
        properties["width"] = width
        if (brush is SolidColor) {
            properties["color"] = brush.value
            value = brush.value
        } else {
            properties["brush"] = brush
        }
        properties["shape"] = shape
    }
}

internal class BorderModifierNode(
    var width: Dp,
    var brush: Brush,
    shape: Shape
) : DrawModifierNode, LayoutAwareModifierNode, Modifier.Node() {

    var shape: Shape = shape
        set(value) {
            if (field != value) {
                field = value
                outline = null
            }
        }

    private var outline: Outline? = null

    override fun onMeasureResultChanged() {
        outline = null
    }

    override fun onReset() {
        super.onReset()
        // todo: jonas 清理状态
    }

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        if (view == null) {
            throwRuntimeError("view null")
        }
        val color = (brush as? SolidColor)?.value ?: Color.Transparent
        if (outline == null && shape != RectangleShape) {
            val size = requireCoordinator(Nodes.LayoutAware).size.toSize()
            outline = shape.createOutline(size, layoutDirection, this)
        }
        (outline as? Outline.Rounded)?.also {
            view!!.borderRadius(it.roundRect)
        }
        view!!.getViewAttr().border(Border(width.value, BorderStyle.SOLID, color.toKuiklyColor()))

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
