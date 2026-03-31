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

package com.tencent.kuikly.compose.extension

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.ShadowGraphicsLayerElement
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.LayoutNode
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.views.ScrollerView
import kotlin.math.abs

private const val FRAME_FLOAT_PRECISION = 1e-6f

internal inline fun Float.approximatelyEqual(other: Float): Boolean {
    return abs(this - other) < FRAME_FLOAT_PRECISION
}

/**
 * 判断2个Frame是否相等，精度为Float
 * @receiver Frame
 * @param other Frame
 * @return Boolean
 */
internal fun Frame.approximatelyEqual(other: Frame): Boolean {
    return x.approximatelyEqual(other.x) &&
        y.approximatelyEqual(other.y) &&
        width.approximatelyEqual(other.width) &&
        height.approximatelyEqual(other.height)
}

/**
 * @receiver Frame kuikly的Frame dp单位
 * @param density Float
 * @return IntRect compose的Rect，像素单位
 */
internal fun Frame.toIntRect(density: Float): IntRect {
    return IntRect(
        IntOffset((this.x * density).toInt(), (this.y * density).toInt()),
        IntSize((this.width * density).toInt(), (this.height * density).toInt())
    )
}

fun shouldWrapShadowView(modifier: Modifier): Boolean {
    return modifier.any { element ->
        when (element) {
            is SetPropElement -> element.key == "boxShadow"
            is ShadowGraphicsLayerElement -> true
            else -> false
        }
    }
}

fun Attr.scaleToDensity(density: Density, value: Float): Float {
    return value * density.density / getPager().pagerDensity()
}

private const val MAX_FIND_SCROLLER_VIEW_DEPTH = 2

/**
 * Find the first child node whose view is a ScrollerView, searching up to
 * [MAX_FIND_SCROLLER_VIEW_DEPTH] levels deep in the layout tree.
 *
 * This is useful when a Modifier.Node is attached to a non-scrollable container
 * (e.g. Surface / DivView) but needs to configure the underlying ScrollerView
 * that lives as a descendant in the layout tree.
 */
internal fun LayoutNode.findFirstChildScrollerView(depth: Int = MAX_FIND_SCROLLER_VIEW_DEPTH): ScrollerView<*, *>? {
    if (depth <= 0) return null
    for (child in children) {
        val view = (child as? KNode<*>)?.view as? ScrollerView<*, *>
        if (view != null) return view
        val found = child.findFirstChildScrollerView(depth - 1)
        if (found != null) return found
    }
    return null
}
