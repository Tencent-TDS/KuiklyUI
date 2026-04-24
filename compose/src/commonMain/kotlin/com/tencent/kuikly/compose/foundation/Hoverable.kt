/*
 * Copyright 2021 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation

import com.tencent.kuikly.compose.foundation.interaction.HoverInteraction
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * Configure component to be hoverable via pointer enter/exit events.
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [HoverInteraction.Enter] when this element is being hovered.
 * @param enabled Controls the enabled state. When `false`, hover events will be ignored.
 */
fun Modifier.hoverable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true
): Modifier = if (enabled) this.then(HoverableElement(interactionSource)) else this

internal class HoverableElement(
    val interactionSource: MutableInteractionSource
) : ModifierNodeElement<HoverableNode>() {

    override fun create(): HoverableNode = HoverableNode(interactionSource)

    override fun update(node: HoverableNode) {
        node.updateInteractionSource(interactionSource)
    }

    override fun hashCode(): Int = interactionSource.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HoverableElement) return false
        if (interactionSource != other.interactionSource) return false
        return true
    }
}

internal class HoverableNode(
    private var interactionSource: MutableInteractionSource
) : Modifier.Node() {

    private var hoverInteraction: HoverInteraction.Enter? = null
    private var isHovered = false

    override fun onAttach() {
        registerNativeHoverEvents()
    }

    /**
     * Register mouseEnter/mouseExit native events on the KRView
     * corresponding to this node's LayoutNode.
     */
    private fun registerNativeHoverEvents() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val view = kNode.view as? DeclarativeBaseView<*, *> ?: return
        view.getViewEvent().register("mouseEnter") {
            hoverEnter()
        }
        view.getViewEvent().register("mouseExit") {
            hoverExit()
        }
    }

    private fun hoverEnter() {
        if (!isHovered) {
            isHovered = true
            val interaction = HoverInteraction.Enter()
            interactionSource.tryEmit(interaction)
            hoverInteraction = interaction
        }
    }

    private fun hoverExit() {
        if (isHovered) {
            isHovered = false
            hoverInteraction?.let { oldInteraction ->
                interactionSource.tryEmit(HoverInteraction.Exit(oldInteraction))
            }
            hoverInteraction = null
        }
    }

    fun updateInteractionSource(newInteractionSource: MutableInteractionSource) {
        if (interactionSource != newInteractionSource) {
            disposeHover()
            interactionSource = newInteractionSource
        }
    }

    private fun disposeHover() {
        hoverExit()
    }

    override fun onDetach() {
        disposeHover()
    }
}
