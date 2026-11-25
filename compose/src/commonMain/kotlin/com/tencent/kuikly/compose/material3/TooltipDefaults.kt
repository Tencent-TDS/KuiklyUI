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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.MutatorMutex
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.DpSize
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.window.PopupPositionProvider

@Stable
object TooltipDefaults {
    val GlobalMutatorMutex = MutatorMutex()

    /** Default spacing between tooltip and anchor */
    val SpacingBetweenTooltipAndAnchor: Dp = 4.dp

    /** Default shape for PlainTooltip container */
    val plainTooltipContainerShape: Shape
        get() = com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape(4.dp)

    /** Default color for PlainTooltip container */
    val plainTooltipContainerColor: Color
        get() = Color(0xFF1A1A1A)

    /** Default color for PlainTooltip content */
    val plainTooltipContentColor: Color
        get() = Color(0xFFFFFFFF)

    /** Default shape for RichTooltip container */
    val richTooltipContainerShape: Shape
        get() = com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape(8.dp)

    /** Default max width for PlainTooltip */
    val plainTooltipMaxWidth: Dp = 200.dp

    /** Default max width for RichTooltip */
    val richTooltipMaxWidth: Dp = 320.dp

    /** Default caret size */
    val caretSize: DpSize = DpSize(16.dp, 8.dp)

    /** Default caret shape */
    val caretShape: Shape = DefaultTooltipCaretShape(caretSize)

    /** Default freeze-on-hover dismiss delay in milliseconds */
    const val FreezeOnHoverDelay: Long = 150L

    /**
     * Method to create a [RichTooltipColors] for [RichTooltip].
     */
    fun richTooltipColors(): RichTooltipColors = defaultRichTooltipColors

    /**
     * Method to create a [RichTooltipColors] for [RichTooltip] with custom overrides.
     */
    fun richTooltipColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionContentColor: Color = Color.Unspecified,
    ): RichTooltipColors =
        defaultRichTooltipColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            titleContentColor = titleContentColor,
            actionContentColor = actionContentColor,
        )

    private val defaultRichTooltipColors: RichTooltipColors by lazy {
        RichTooltipColors(
            containerColor = Color(0xFF1A1A1A),
            contentColor = Color(0xFFFFFFFF),
            titleContentColor = Color(0xFFFFFFFF),
            actionContentColor = Color(0xFF9ECAFF),
        )
    }
}

@Composable
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = TooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    remember(isPersistent, mutatorMutex) {
        TooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex,
        )
    }

fun TooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = TooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    TooltipStateImpl(
        initialIsVisible = initialIsVisible,
        isPersistent = isPersistent,
        mutatorMutex = mutatorMutex,
    )

/**
 * [PopupPositionProvider] that positions the tooltip relative to the anchor.
 * Uses [TooltipAnchorPosition.Above] as default positioning.
 *
 * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
 */
@Composable
fun rememberTooltipPositionProvider(
    spacingBetweenTooltipAndAnchor: Dp = TooltipDefaults.SpacingBetweenTooltipAndAnchor,
): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(tooltipAnchorSpacing) {
        TooltipPositionProviderImpl(
            type = TooltipAnchorPosition.Above,
            tooltipAnchorSpacing = tooltipAnchorSpacing,
        )
    }
}

/**
 * [PopupPositionProvider] that positions the tooltip relative to the anchor with a specified
 * [positioning] direction.
 *
 * @param positioning [TooltipAnchorPosition] that determines where the tooltip is placed
 *   relative to the anchor.
 * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor content.
 */
@Composable
fun rememberTooltipPositionProvider(
    positioning: TooltipAnchorPosition,
    spacingBetweenTooltipAndAnchor: Dp = TooltipDefaults.SpacingBetweenTooltipAndAnchor,
): PopupPositionProvider {
    val tooltipAnchorSpacing = with(LocalDensity.current) { spacingBetweenTooltipAndAnchor.roundToPx() }
    return remember(tooltipAnchorSpacing, positioning) {
        TooltipPositionProviderImpl(
            type = positioning,
            tooltipAnchorSpacing = tooltipAnchorSpacing,
        )
    }
}

internal class TooltipPositionProviderImpl(
    val type: TooltipAnchorPosition,
    val tooltipAnchorSpacing: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        return when (type) {
            TooltipAnchorPosition.Left ->
                leftPositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Right ->
                rightPositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Above ->
                abovePositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Below ->
                belowPositioning(anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.Start ->
                startPositioning(layoutDirection, anchorBounds, popupContentSize, windowSize)
            TooltipAnchorPosition.End ->
                endPositioning(layoutDirection, anchorBounds, popupContentSize, windowSize)
            else -> abovePositioning(anchorBounds, popupContentSize, windowSize)
        }
    }

    private fun leftPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)
        if (x < 0) {
            val xCorrection =
                (anchorBounds.right + tooltipAnchorSpacing + popupContentSize.width - windowSize.width)
                    .coerceAtLeast(0)
            x = anchorBounds.right + tooltipAnchorSpacing - xCorrection
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun rightPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.right + tooltipAnchorSpacing
        if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.left - (popupContentSize.width + tooltipAnchorSpacing)).coerceAtLeast(0)
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun abovePositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            val xCorrection =
                (anchorBounds.left + popupContentSize.width - windowSize.width).coerceAtLeast(0)
            x = anchorBounds.left - xCorrection
        } else if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.right - popupContentSize.width).coerceAtLeast(0)
        }
        var y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
        if (y < 0) y = anchorBounds.bottom + tooltipAnchorSpacing
        return IntOffset(x, y)
    }

    private fun belowPositioning(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        if (x < 0) {
            val xCorrection =
                (anchorBounds.left + popupContentSize.width - windowSize.width).coerceAtLeast(0)
            x = anchorBounds.left - xCorrection
        } else if (x + popupContentSize.width > windowSize.width) {
            x = (anchorBounds.right - popupContentSize.width).coerceAtLeast(0)
        }
        var y = anchorBounds.bottom + tooltipAnchorSpacing
        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height - tooltipAnchorSpacing
        }
        return IntOffset(x, y)
    }

    private fun startPositioning(
        layoutDirection: LayoutDirection,
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        return if (layoutDirection == LayoutDirection.Ltr) {
            leftPositioning(anchorBounds, popupContentSize, windowSize)
        } else {
            rightPositioning(anchorBounds, popupContentSize, windowSize)
        }
    }

    private fun endPositioning(
        layoutDirection: LayoutDirection,
        anchorBounds: IntRect,
        popupContentSize: IntSize,
        windowSize: IntSize,
    ): IntOffset {
        return if (layoutDirection == LayoutDirection.Ltr) {
            rightPositioning(anchorBounds, popupContentSize, windowSize)
        } else {
            leftPositioning(anchorBounds, popupContentSize, windowSize)
        }
    }
}

internal const val TooltipSpacing = 4
