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
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.core.views.ScrollerView

/**
 * Dynamically enable or disable native fling for the underlying Kuikly ScrollerView.
 */
fun Modifier.flingEnable(enable: Boolean): Modifier =
    this.then(FlingEnableElement(enable))

private class FlingEnableElement(
    val enable: Boolean,
) : ModifierNodeElement<FlingEnableNode>() {
    override fun create(): FlingEnableNode = FlingEnableNode(enable)

    override fun hashCode(): Int = enable.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is FlingEnableElement && enable == other.enable
    }

    override fun update(node: FlingEnableNode) {
        node.enable = enable
        node.update()
    }
}

private class FlingEnableNode(
    var enable: Boolean,
) : Modifier.Node() {

    override fun onAttach() {
        super.onAttach()
        update()
    }

    fun update() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val scrollerView = (kNode.view as? ScrollerView<*, *>)
            ?: layoutNode.findFirstChildScrollerView()
            ?: return
        scrollerView.getViewAttr().flingEnable(enable)
    }
}
