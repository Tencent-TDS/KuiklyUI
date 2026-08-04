/*
 * Copyright 2024 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.compose.ui.util.fastMap
import com.tencent.kuikly.compose.ui.util.fastMaxBy
import com.tencent.kuikly.core.views.ModalView

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using [popupPositionProvider] relative to the [anchorBounds].
 *
 * @param popupPositionProvider Provides positioning data for the popup.
 * @param anchorBounds The bounds of the anchor element in window coordinates.
 * @param onDismissRequest Executes when the user clicks outside the popup or presses the back
 *   button. If null, the popup will not be dismissible by outside clicks.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@NonRestartableComposable
@Composable
fun Popup(
    popupPositionProvider: PopupPositionProvider,
    anchorBounds: IntRect,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) = PopupLayout(
    popupPositionProvider = popupPositionProvider,
    anchorBounds = anchorBounds,
    onDismissRequest = onDismissRequest,
    properties = properties,
    content = content,
)

private class PopupMeasurePolicy(
    val popupPositionProvider: PopupPositionProvider,
    val anchorBounds: IntRect,
    val layoutDirection: LayoutDirection,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val placeables = measurables.fastMap { it.measure(constraints) }
        val contentWidth = placeables.fastMaxBy { it.width }?.width ?: 0
        val contentHeight = placeables.fastMaxBy { it.height }?.height ?: 0
        val contentSize = IntSize(contentWidth, contentHeight)

        val windowSize = IntSize(constraints.maxWidth, constraints.maxHeight)

        val position = popupPositionProvider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = layoutDirection,
            popupContentSize = contentSize,
        )

        return layout(windowSize.width, windowSize.height) {
            placeables.fastForEach { it.place(position.x, position.y) }
        }
    }
}

@Composable
private fun PopupLayout(
    popupPositionProvider: PopupPositionProvider,
    anchorBounds: IntRect,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    content: @Composable () -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    val slotProvider = LocalSlotProvider.current
    val layoutDirection = LocalLayoutDirection.current

    var slotId = remember { 0 }

    DisposableEffect(Unit) {
        slotId = slotProvider.addSlot {
            ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
                factory = {
                    KNode(ModalView().also {
                        it.inWindow = false
                    })
                },
                update = {
                    set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
                    @OptIn(ExperimentalComposeUiApi::class)
                    set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
                    set(
                        PopupMeasurePolicy(
                            popupPositionProvider,
                            anchorBounds,
                            layoutDirection,
                        ), ComposeUiNode.SetMeasurePolicy
                    )
                    set(Modifier, ComposeUiNode.SetModifier)
                },
                content = {
                    currentContent()
                }
            )
        }

        onDispose {
            slotProvider.removeSlot(slotId)
        }
    }
}
