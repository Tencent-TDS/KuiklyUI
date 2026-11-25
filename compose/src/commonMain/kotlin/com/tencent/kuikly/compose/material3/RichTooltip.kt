/*
 * Copyright 2023 The Android Open Source Project
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

package com.tencent.kuikly.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.requiredHeightIn
import com.tencent.kuikly.compose.foundation.layout.sizeIn
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * Rich text tooltip that allows the user to pass in a title, text, and action. Tooltips are used to
 * provide a descriptive message.
 *
 * Usually used with [TooltipBox]
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param title An optional title for the tooltip.
 * @param action An optional action for the tooltip.
 * @param caretShape [Shape] for the caret of the tooltip. If a default caret is desired with a
 *   specific dimension please use [TooltipDefaults.caretShape]. To see the default dimensions
 *   please see [TooltipDefaults.caretSize]. If no caret is desired, please pass in null.
 * @param maxWidth the maximum width for the rich tooltip
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param colors [RichTooltipColors] that will be applied to the tooltip's container and content.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param text the composable that will be used to populate the rich tooltip's text.
 */
@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretShape: (Shape)? = null,
    maxWidth: Dp = TooltipDefaults.richTooltipMaxWidth,
    shape: Shape = TooltipDefaults.richTooltipContainerShape,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    text: @Composable () -> Unit,
) {
    val tooltipShape: Shape
    val tooltipModifier: Modifier
    if (caretShape != null) {
        val transformationMatrix = remember { mutableStateOf(Matrix()) }
        val layoutDirection = LocalLayoutDirection.current
        tooltipModifier =
            Modifier.layoutCaret(
                transformationMatrix = transformationMatrix,
                getAnchorLayoutCoordinates = { obtainAnchorBounds() },
                positionProvider = obtainPositionProvider(),
                layoutDirection = layoutDirection,
            )
                .then(modifier)
        tooltipShape =
            remember(shape, caretShape) {
                TooltipCaretShape(transformationMatrix, shape, caretShape)
            }
    } else {
        tooltipShape = shape
        tooltipModifier = modifier
    }

    Surface(
        modifier =
            tooltipModifier.sizeIn(
                minWidth = TooltipMinWidth,
                maxWidth = maxWidth,
                minHeight = TooltipMinHeight,
            ),
        shape = tooltipShape,
        color = colors.containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Column(modifier = Modifier.padding(horizontal = RichTooltipHorizontalPadding)) {
            title?.let {
                Box(modifier = Modifier.padding(top = HeightToSubheadFirstLine)) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.titleContentColor,
                        content = it,
                    )
                }
            }
            Box(modifier = Modifier.textVerticalPadding(title != null, action != null)) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor,
                    content = text,
                )
            }
            action?.let {
                Box(
                    modifier =
                        Modifier.requiredHeightIn(min = ActionLabelMinHeight)
                            .padding(bottom = ActionLabelBottomPadding)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.actionContentColor,
                        content = it,
                    )
                }
            }
        }
    }
}

private val TooltipMinWidth = 40.dp
private val TooltipMinHeight = 24.dp
private val RichTooltipHorizontalPadding = 16.dp
private val HeightToSubheadFirstLine = 28.dp
private val ActionLabelMinHeight = 36.dp
private val ActionLabelBottomPadding = 8.dp

internal fun Modifier.textVerticalPadding(subheadExists: Boolean, actionExists: Boolean): Modifier {
    return if (!subheadExists && !actionExists) {
        this.padding(vertical = 4.dp)
    } else {
        this.padding(top = 24.dp)
            .padding(bottom = 16.dp)
    }
}
