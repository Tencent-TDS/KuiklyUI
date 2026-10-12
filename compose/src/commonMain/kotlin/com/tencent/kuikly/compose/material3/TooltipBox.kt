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

import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.hoverable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.interaction.collectIsHoveredAsState
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.window.Popup
import com.tencent.kuikly.compose.ui.window.PopupPositionProvider
import com.tencent.kuikly.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Material TooltipBox that wraps a composable with a tooltip.
 *
 * Tooltips provide a descriptive message for an anchor. It can be used to call the users attention
 * to the anchor.
 *
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip relative
 *   to the anchor content.
 * @param tooltip the composable that will be used to populate the tooltip's content.
 *   Receives a [TooltipScope] to access anchor bounds and position provider for caret drawing.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to this TooltipBox.
 * @param onDismissRequest executes when the user clicks outside of the tooltip.
 * @param focusable [Boolean] that determines if the tooltip is focusable.
 * @param enableUserInput [Boolean] which determines if this TooltipBox will handle mouse hover
 *   to trigger the tooltip through the state provided.
 * @param hasAction whether the associated tooltip contains an action.
 * @param enableFreezeOnHover [Boolean] when true, the tooltip stays visible when the mouse moves
 *   from the anchor onto the popup content. Requires desktop platform with hover support.
 *   Defaults to false for backward compatibility.
 * @param freezeDelayMillis [Long] delay in milliseconds before dismissing the tooltip after the
 *   mouse leaves both the anchor and popup regions. Only effective when [enableFreezeOnHover]
 *   is true. Defaults to [TooltipDefaults.FreezeOnHoverDelay].
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    enableFreezeOnHover: Boolean = false,
    freezeDelayMillis: Long = TooltipDefaults.FreezeOnHoverDelay,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var anchorBounds by remember { mutableStateOf(IntRect.Zero) }
    val anchorCoordinates: MutableState<LayoutCoordinates?> = remember { mutableStateOf(null) }
    val tooltipScope = remember(positionProvider) {
        TooltipScopeImpl({ anchorCoordinates.value }, positionProvider)
    }

    // Freeze-on-hover: dual hoverable (anchor + popup) with delay bridge
    val anchorHoverSource = remember { MutableInteractionSource() }
    val popupHoverSource = remember { MutableInteractionSource() }
    val isAnchorHovered by anchorHoverSource.collectIsHoveredAsState()
    val isPopupHovered by popupHoverSource.collectIsHoveredAsState()

    // Legacy single hover source (used when freeze is disabled)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    if (enableFreezeOnHover) {
        LaunchedEffect(isAnchorHovered, isPopupHovered) {
            if ((isAnchorHovered || isPopupHovered) && enableUserInput) {
                scope.launch { state.show() }
            } else if (!isAnchorHovered && !isPopupHovered) {
                delay(freezeDelayMillis)
                if (!isAnchorHovered && !isPopupHovered) {
                    state.dismiss()
                }
            }
        }
    } else {
        LaunchedEffect(isHovered) {
            if (isHovered && enableUserInput) {
                scope.launch { state.show() }
            } else if (!isHovered) {
                state.dismiss()
            }
        }
    }

    val transition = updateTransition(state.transition, label = "tooltip transition")
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 150) },
        label = "tooltip scale",
    ) { if (it) 1f else 0.8f }
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 150) },
        label = "tooltip alpha",
    ) { if (it) 1f else 0f }

    Box {
        if (state.isVisible) {
            Popup(
                popupPositionProvider = positionProvider,
                anchorBounds = anchorBounds,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = focusable, clippingEnabled = false),
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (enableFreezeOnHover) {
                                Modifier.hoverable(popupHoverSource)
                            } else {
                                Modifier
                            }
                        )
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.alpha = alpha
                        }
                ) {
                    tooltipScope.tooltip()
                }
            }
        }

        Box(
            modifier = modifier
                .hoverable(
                    if (enableFreezeOnHover) anchorHoverSource else interactionSource,
                    enabled = enableUserInput
                )
                .onGloballyPositioned { coordinates ->
                    anchorCoordinates.value = coordinates
                    val bounds = coordinates.boundsInRoot()
                    anchorBounds = IntRect(
                        left = bounds.left.toInt(),
                        top = bounds.top.toInt(),
                        right = bounds.right.toInt(),
                        bottom = bounds.bottom.toInt()
                    )
                }
        ) {
            content()
        }
    }

    DisposableEffect(state) { onDispose { state.onDispose() } }
}
