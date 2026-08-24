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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.node.LayoutModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.TextUnitType
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max

/**
 * The default minimum height in terms of minimum number of visible lines.
 *
 * Should not be used in public API and samples unless it's public, too.
 */
internal const val DefaultMinLines = 1

/**
 * The default font size used for line height calculation when no font size is specified.
 */
private val DefaultFontSizeForLineHeight = 14.sp

/**
 * Constraint the height of the text field so that it minLines and maxLines are honored.
 */
internal fun Modifier.heightInLines(
    textStyle: TextStyle,
    minLines: Int = DefaultMinLines,
    maxLines: Int = Int.MAX_VALUE
): Modifier {
    require(minLines >= 1) { "minLines must be >= 1, got $minLines" }
    require(maxLines >= 1) { "maxLines must be >= 1, got $maxLines" }
    require(minLines <= maxLines) { "minLines must be <= maxLines, got minLines=$minLines, maxLines=$maxLines" }

    return this.then(
        HeightInLinesElement(
            minLines = minLines,
            maxLines = maxLines,
            textStyle = textStyle
        )
    )
}

private class HeightInLinesElement(
    val minLines: Int,
    val maxLines: Int,
    val textStyle: TextStyle
) : ModifierNodeElement<HeightInLinesModifierNode>() {
    override fun create(): HeightInLinesModifierNode = HeightInLinesModifierNode(
        minLines = minLines,
        maxLines = maxLines,
        textStyle = textStyle
    )

    override fun update(node: HeightInLinesModifierNode) {
        node.update(
            minLines = minLines,
            maxLines = maxLines,
            textStyle = textStyle
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "heightInLines"
        properties["minLines"] = minLines
        properties["maxLines"] = maxLines
        properties["textStyle"] = textStyle
    }

    override fun hashCode(): Int {
        var result = minLines
        result = 31 * result + maxLines
        result = 31 * result + textStyle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeightInLinesElement) return false

        if (minLines != other.minLines) return false
        if (maxLines != other.maxLines) return false
        return textStyle == other.textStyle
    }
}

private class HeightInLinesModifierNode(
    var minLines: Int,
    var maxLines: Int,
    var textStyle: TextStyle
) : Modifier.Node(), LayoutModifierNode {

    fun update(
        minLines: Int,
        maxLines: Int,
        textStyle: TextStyle
    ) {
        this.minLines = minLines
        this.maxLines = maxLines
        this.textStyle = textStyle
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Compute min and max height in lines
        val lineHeightPx = if (textStyle.lineHeight.isSpecified && textStyle.lineHeight.type == TextUnitType.Sp) {
            textStyle.lineHeight.toPx()
        } else {
            // Fallback: If lineHeight is not specified or not Sp, use fontSize * 1.2 as a rough estimate
            val fontSize = if (textStyle.fontSize.isSpecified && textStyle.fontSize.type == TextUnitType.Sp) {
                textStyle.fontSize
            } else {
                DefaultFontSizeForLineHeight // Material3 default fallback
            }
            fontSize.toPx() * 1.2f
        }

        val minHeight = if (minLines > 1) {
            ceil(lineHeightPx * minLines).toInt()
        } else {
            0
        }
        val maxHeight = if (maxLines != Int.MAX_VALUE) {
            ceil(lineHeightPx * maxLines).toInt()
        } else {
            constraints.maxHeight
        }

        val childConstraints = constraints.copy(
            minHeight = max(constraints.minHeight, minHeight),
            maxHeight = max(max(constraints.minHeight, minHeight), maxHeight)
        )
        val placeable = measurable.measure(childConstraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}
