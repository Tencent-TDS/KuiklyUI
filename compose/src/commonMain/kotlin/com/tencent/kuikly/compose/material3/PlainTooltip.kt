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
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.sizeIn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * Plain tooltip that provides a descriptive message.
 *
 * Usually used with [TooltipBox].
 *
 * @param modifier the [Modifier] to be applied to the tooltip.
 * @param caretShape [Shape] for the caret of the tooltip. If null, no caret is drawn.
 *   Pass [TooltipDefaults.plainTooltipContainerShape] or a custom shape for a caret.
 *   Note: Caret rendering requires Layer 2+3 to be implemented.
 * @param maxWidth the maximum width for the plain tooltip.
 * @param shape the [Shape] that should be applied to the tooltip container.
 * @param contentColor [Color] that will be applied to the tooltip's content.
 * @param containerColor [Color] that will be applied to the tooltip's container.
 * @param tonalElevation the tonal elevation of the tooltip.
 * @param shadowElevation the shadow elevation of the tooltip.
 * @param content the composable that will be used to populate the tooltip's content.
 */
@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretShape: Shape? = null,
    maxWidth: Dp = TooltipDefaults.plainTooltipMaxWidth,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val tooltipShape: Shape
    val tooltipModifier: Modifier

    if (caretShape != null) {
        val transformationMatrix = remember { mutableStateOf(Matrix()) }
        val layoutDirection = LocalLayoutDirection.current
        tooltipModifier = Modifier
            .layoutCaret(
                transformationMatrix = transformationMatrix,
                getAnchorLayoutCoordinates = { obtainAnchorBounds() },
                positionProvider = obtainPositionProvider(),
                layoutDirection = layoutDirection,
            )
            .then(modifier)
        tooltipShape = remember(shape, caretShape) {
            TooltipCaretShape(transformationMatrix, shape, caretShape)
        }
    } else {
        tooltipShape = shape
        tooltipModifier = modifier
    }

    Surface(
        modifier = tooltipModifier,
        shape = tooltipShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = PlainTooltipMinWidth,
                    maxWidth = maxWidth,
                    minHeight = PlainTooltipMinHeight,
                )
                .padding(PlainTooltipHorizontalPadding, PlainTooltipVerticalPadding)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                content = content,
            )
        }
    }
}

/**
 * Convenience overload of [PlainTooltip] that does not require a [TooltipScope] receiver.
 * Useful for simple tooltip usage without caret support.
 */
@Composable
fun PlainTooltip(
    modifier: Modifier = Modifier,
    shape: Shape = TooltipDefaults.plainTooltipContainerShape,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier = Modifier
                .sizeIn(
                    minWidth = PlainTooltipMinWidth,
                    maxWidth = TooltipDefaults.plainTooltipMaxWidth,
                    minHeight = PlainTooltipMinHeight,
                )
                .padding(PlainTooltipHorizontalPadding, PlainTooltipVerticalPadding)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                content = content,
            )
        }
    }
}

private val PlainTooltipMinHeight = 24.dp
private val PlainTooltipMinWidth = 40.dp
private val PlainTooltipVerticalPadding = 4.dp
private val PlainTooltipHorizontalPadding = 8.dp
