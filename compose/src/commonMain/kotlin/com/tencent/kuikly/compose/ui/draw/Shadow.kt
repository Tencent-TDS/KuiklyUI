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

package com.tencent.kuikly.compose.ui.draw

import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.DefaultShadowColor
import com.tencent.kuikly.compose.ui.graphics.RectangleShape
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireDensity
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.platform.debugInspectorInfo
import com.tencent.kuikly.compose.ui.platform.inspectable
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.exception.throwRuntimeError

///**
// * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual
// * depth of the physical object. The physical object has a shape specified by [shape].
// *
// * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
// *
// * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order.
// * Use a [com.tencent.kuikly.compose.ui.zIndex] modifier if you want to draw the elements with larger
// * [elevation] after all the elements with a smaller one.
// *
// * Usage of this API renders this composable into a separate graphics layer
// * @see graphicsLayer
// *
// * Example usage:
// *
// * @sample com.tencent.kuikly.compose.ui.samples.ShadowSample
// *
// * @param elevation The elevation for the shadow in pixels
// * @param shape Defines a shape of the physical object
// * @param clip When active, the content drawing clips to the shape.
// */
//@Deprecated(
//    "Replace with shadow which accepts ambientColor and spotColor parameters",
//    ReplaceWith(
//        "Modifier.shadow(elevation, shape, clip, DefaultShadowColor, DefaultShadowColor)",
//        "com.tencent.kuikly.compose.ui.draw"
//    ),
//    DeprecationLevel.HIDDEN
//)
//@Stable
//fun Modifier.shadow(
//    elevation: Dp,
//    shape: Shape = RectangleShape,
//    clip: Boolean = elevation > 0.dp
//) = shadow(
//    elevation,
//    shape,
//    clip,
//    DefaultShadowColor,
//    DefaultShadowColor,
//)

/**
 * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual
 * depth of the physical object. The physical object has a shape specified by [shape].
 *
 * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order.
 * Use a [com.tencent.kuikly.compose.ui.zIndex] modifier if you want to draw the elements with larger
 * [elevation] after all the elements with a smaller one.
 *
 * Usage of this API renders this composable into a separate graphics layer
 * @see graphicsLayer
 *
 * Example usage:
 *
 * @sample com.tencent.kuikly.compose.ui.samples.ShadowSample
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clip When active, the content drawing clips to the shape.
 */
@Stable
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
//    ambientColor: Color = DefaultShadowColor,
//    spotColor: Color = DefaultShadowColor,
) = if (elevation > 0.dp || clip) {
    inspectable(
        inspectorInfo = debugInspectorInfo {
            name = "shadow"
            properties["elevation"] = elevation
            properties["shape"] = shape
            properties["clip"] = clip
        }
    ) {
        this.then(ShadowElement(
            elevation,
            shape,
        ))
    }
} else {
    this
}


internal class ShadowElement(
    private val elevation: Dp,
    private val shape: Shape = RectangleShape,
) : ModifierNodeElement<ShadowNode>() {
    override fun create(): ShadowNode {
        return ShadowNode(
            elevation,
            shape,
        )
    }

    override fun update(node: ShadowNode) {
        node.elevation = elevation
        node.shape = shape
    }

    override fun hashCode(): Int {
        var result = elevation.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? ShadowElement ?: return false
        return elevation == otherModifier.elevation &&
                shape == otherModifier.shape
    }
}

internal class ShadowNode(
    var elevation: Dp,
    var shape: Shape = RectangleShape,
) : DrawModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        if (view == null) {
            throwRuntimeError("view null")
        }

        view?.getViewAttr()?.run {
            boxShadow(BoxShadow(
                offsetX = 0f,
                offsetY = 0f,
                shadowRadius = elevation.value,
                shadowColor = com.tencent.kuikly.core.base.Color.BLACK
            ))
        }

        drawContent()
    }
}
