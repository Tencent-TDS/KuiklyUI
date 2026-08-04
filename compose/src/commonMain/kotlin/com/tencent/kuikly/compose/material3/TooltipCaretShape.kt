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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Outline
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.PathOperation
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.positionInRoot
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.DpSize
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.window.PopupPositionProvider

/**
 * Default [Shape] of the caret used by tooltips.
 * Generates a triangle pointing upward (before rotation).
 *
 * @param caretSize the size of the caret
 */
class DefaultTooltipCaretShape(val caretSize: DpSize = TooltipDefaults.caretSize) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val caretPath = Path()
        val caretWidthPx: Float
        val caretHeightPx: Float
        with(density) {
            caretWidthPx = caretSize.width.toPx()
            caretHeightPx = caretSize.height.toPx()
        }

        caretPath.apply {
            moveTo(x = 0f, y = 0f)
            lineTo(x = caretWidthPx / 2, y = 0f)
            lineTo(x = 0f, y = caretHeightPx)
            lineTo(x = -caretWidthPx / 2, y = 0f)
            close()
        }

        return Outline.Generic(caretPath)
    }
}

/**
 * A [Shape] that combines the tooltip body shape with a caret shape,
 * producing a unified outline where caret and tooltip form a single continuous shape.
 *
 * The caret position and rotation are controlled by [transformationMatrix].
 */
internal class TooltipCaretShape(
    private val transformationMatrix: MutableState<Matrix>,
    private val tooltipShape: Shape,
    private val caretShape: Shape,
) : Shape {
    private val tooltipPath = Path()
    private val combinedPath = Path()
    private val caretPath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        tooltipPath.reset()
        combinedPath.reset()
        caretPath.reset()

        val tooltipOutline = tooltipShape.createOutline(size, layoutDirection, density)
        val caretOutline = caretShape.createOutline(size, layoutDirection, density)

        when (tooltipOutline) {
            is Outline.Generic -> tooltipPath.addPath(tooltipOutline.path)
            is Outline.Rounded -> tooltipPath.addRoundRect(tooltipOutline.roundRect)
            is Outline.Rectangle -> tooltipPath.addRect(tooltipOutline.rect)
        }

        when (caretOutline) {
            is Outline.Generic -> caretPath.addPath(caretOutline.path)
            is Outline.Rounded -> caretPath.addRoundRect(caretOutline.roundRect)
            is Outline.Rectangle -> caretPath.addRect(caretOutline.rect)
        }

        // Apply transform (position + rotation) to caret
        caretPath.transform(transformationMatrix.value)

        // Union: merge tooltip + caret into one continuous shape
        combinedPath.op(path1 = tooltipPath, path2 = caretPath, operation = PathOperation.Union)

        return Outline.Generic(combinedPath)
    }
}

/**
 * Modifier that computes the caret's position and rotation matrix based on
 * the tooltip's position relative to the anchor.
 *
 * Uses [onGloballyPositioned] as a replacement for the official `onLayoutRectChanged`.
 */
@Stable
internal fun Modifier.layoutCaret(
    transformationMatrix: MutableState<Matrix>,
    windowContainerSize: IntSize = IntSize.Zero,
    getAnchorLayoutCoordinates: () -> LayoutCoordinates?,
    positionProvider: PopupPositionProvider,
    layoutDirection: LayoutDirection,
): Modifier = this.onGloballyPositioned { tooltipCoordinates ->
    val anchorCoordinates = getAnchorLayoutCoordinates()
    if (anchorCoordinates != null && anchorCoordinates.isAttached) {
        val tooltipPos = tooltipCoordinates.positionInRoot()
        val anchorPos = anchorCoordinates.positionInRoot()
        val tooltipWidth = tooltipCoordinates.size.width.toFloat()
        val tooltipHeight = tooltipCoordinates.size.height.toFloat()
        val anchorSize = anchorCoordinates.size
        val anchorBounds = com.tencent.kuikly.compose.ui.geometry.Rect(
            anchorPos,
            com.tencent.kuikly.compose.ui.geometry.Size(anchorSize.width.toFloat(), anchorSize.height.toFloat())
        )

        val windowContainerWidthInPx = windowContainerSize.width
        val screenWidthPx = if (windowContainerWidthInPx > 0) windowContainerWidthInPx else (tooltipWidth * 3).toInt()

        val isBelow = tooltipPos.y > anchorPos.y
        val isToTheRight = tooltipPos.x > anchorPos.x

        val caretY =
            if (positionProvider is TooltipPositionProviderImpl) {
                when (positionProvider.type) {
                    TooltipAnchorPosition.Left,
                    TooltipAnchorPosition.Right,
                    TooltipAnchorPosition.Start,
                    TooltipAnchorPosition.End -> {
                        tooltipHeight / 2
                    }
                    else -> {
                        if (isBelow) 0f else tooltipHeight
                    }
                }
            } else {
                if (isBelow) 0f else tooltipHeight
            }

        val position =
            if (positionProvider is TooltipPositionProviderImpl) {
                when (positionProvider.type) {
                    TooltipAnchorPosition.Left -> {
                        val caretX = if (isToTheRight) 0f else tooltipWidth
                        com.tencent.kuikly.compose.ui.geometry.Offset(x = caretX, y = caretY)
                    }
                    TooltipAnchorPosition.Right -> {
                        val caretX = if (!isToTheRight) tooltipWidth else 0f
                        com.tencent.kuikly.compose.ui.geometry.Offset(x = caretX, y = caretY)
                    }
                    TooltipAnchorPosition.Start -> {
                        val caretX =
                            if (layoutDirection == LayoutDirection.Ltr) {
                                if (isToTheRight) 0f else tooltipWidth
                            } else {
                                if (!isToTheRight) tooltipWidth else 0f
                            }
                        com.tencent.kuikly.compose.ui.geometry.Offset(x = caretX, y = caretY)
                    }
                    TooltipAnchorPosition.End -> {
                        val caretX =
                            if (layoutDirection == LayoutDirection.Ltr) {
                                if (!isToTheRight) tooltipWidth else 0f
                            } else {
                                if (isToTheRight) 0f else tooltipWidth
                            }
                        com.tencent.kuikly.compose.ui.geometry.Offset(x = caretX, y = caretY)
                    }
                    else -> {
                        com.tencent.kuikly.compose.ui.geometry.Offset(
                            x = caretX(tooltipWidth, screenWidthPx, anchorBounds),
                            y = caretY,
                        )
                    }
                }
            } else {
                com.tencent.kuikly.compose.ui.geometry.Offset(x = caretX(tooltipWidth, screenWidthPx, anchorBounds), y = caretY)
            }

        val matrix = Matrix()
        matrix.translate(x = position.x, y = position.y)

        if (positionProvider is TooltipPositionProviderImpl) {
            when (positionProvider.type) {
                TooltipAnchorPosition.Left -> {
                    if (isToTheRight) {
                        matrix.rotateZ(90f)
                    } else {
                        matrix.rotateZ(-90f)
                    }
                }
                TooltipAnchorPosition.Right -> {
                    if (!isToTheRight) {
                        matrix.rotateZ(-90f)
                    } else {
                        matrix.rotateZ(90f)
                    }
                }
                TooltipAnchorPosition.Start -> {
                    if (layoutDirection == LayoutDirection.Ltr) {
                        if (isToTheRight) {
                            matrix.rotateZ(90f)
                        } else {
                            matrix.rotateZ(-90f)
                        }
                    } else {
                        if (!isToTheRight) {
                            matrix.rotateZ(-90f)
                        } else {
                            matrix.rotateZ(90f)
                        }
                    }
                }
                TooltipAnchorPosition.End -> {
                    if (layoutDirection == LayoutDirection.Ltr) {
                        if (!isToTheRight) {
                            matrix.rotateZ(-90f)
                        } else {
                            matrix.rotateZ(90f)
                        }
                    } else {
                        if (isToTheRight) {
                            matrix.rotateZ(90f)
                        } else {
                            matrix.rotateZ(-90f)
                        }
                    }
                }
                else -> {
                    if (isBelow) matrix.rotateX(180f)
                }
            }
        } else {
            if (isBelow) matrix.rotateX(180f)
        }

        transformationMatrix.value = matrix
    }
}

/**
 * Helper to compute caret X for Above/Below positioning.
 */
internal fun caretX(tooltipWidth: Float, screenWidthPx: Int, anchorBounds: com.tencent.kuikly.compose.ui.geometry.Rect): Float {
    val anchorLeft = anchorBounds.left
    val anchorRight = anchorBounds.right
    val anchorMid = (anchorLeft + anchorRight) / 2
    return if (tooltipWidth >= screenWidthPx) {
        // Tooltip is greater than or equal to the width of the screen
        // The horizontal placement just needs to be in the center of the anchor
        anchorMid
    } else if (anchorMid - tooltipWidth / 2 < 0) {
        // The tooltip needs to be start aligned if it would
        // collide with the left side of screen when attempting to center.
        // We have a horizontal correction for the caret if the tooltip will
        // also collide with the right edge of the screen when start aligned
        val horizontalCorrection = maxOf(tooltipWidth - screenWidthPx, -anchorLeft)
        anchorMid + horizontalCorrection
    } else if (anchorMid + tooltipWidth / 2 > screenWidthPx) {
        // The tooltip needs to be end aligned if it would
        // collide with the right side of the screen when attempting to center.
        // We have a horizontal correction for the caret if the tooltip will
        // also collide with the left edge of the screen when end aligned
        val horizontalCorrection = minOf(tooltipWidth - anchorRight, 0f)
        anchorMid + horizontalCorrection
    } else {
        // Tooltip can centered neatly without colliding with screen edge
        tooltipWidth / 2
    }
}
