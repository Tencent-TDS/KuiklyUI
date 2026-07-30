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
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.KNode.Companion.alpha
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.platform.debugInspectorInfo
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.exception.throwRuntimeError

/**
 * Draw content with modified alpha that may be less than 1.
 *
 * Usage of this API renders this composable into a separate graphics layer.
 * Note when an alpha less than 1.0f is provided, contents are implicitly clipped
 * to their bounds. This is because an intermediate compositing layer is created to
 * render contents into first before being drawn into the destination with the desired alpha.
 * This layer is sized to the bounds of the composable this modifier is configured on, and contents
 * outside of these bounds are omitted.
 *
 * @see graphicsLayer
 *
 * Example usage:
 * @sample com.tencent.kuikly.compose.ui.samples.AlphaSample
 *
 * @param alpha the fraction of children's alpha value and must be between `0` and `1`, inclusive.
 */
@Stable
fun Modifier.alpha(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    alpha: Float
): Modifier {
    return this.then(
        AlphaElement(
            alpha,
            debugInspectorInfo {
                name = "alpha"
                value = alpha
                properties["alpha"] = alpha
            }
        )
    )
}

private class AlphaElement(
    private val alpha: Float,
    private val inspectorInfo: InspectorInfo.() -> Unit
) : ModifierNodeElement<AlphaNode>() {
    override fun create(): AlphaNode {
        return AlphaNode(
            alpha
        )
    }

    override fun hashCode(): Int {
        return alpha.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? AlphaElement ?: return false
        return alpha == otherModifier.alpha
    }

    override fun update(node: AlphaNode) {
        node.alpha = alpha
    }

}

private class AlphaNode(
    var alpha: Float,
) : DrawModifierNode, Modifier.Node() {

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        if (view == null) {
            throwRuntimeError("view null")
        }

        view!!.alpha(alpha)

        drawContent()
    }
}