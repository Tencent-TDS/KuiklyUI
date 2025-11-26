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

fun Modifier.bouncesEnable(
    enable: Boolean,
    limitHeaderBounces: Boolean = false,
    limitFooterBounces: Boolean = false
): Modifier = this.then(BouncesEnableElement(enable, limitHeaderBounces, limitFooterBounces))

private class BouncesEnableElement(
    val bouncesEnable: Boolean,
    val limitHeaderBounces: Boolean,
    val limitFooterBounces: Boolean
) : ModifierNodeElement<BouncesEnableNode>() {
    override fun create(): BouncesEnableNode = BouncesEnableNode(bouncesEnable, limitHeaderBounces, limitFooterBounces)

    override fun hashCode(): Int {
        var result = bouncesEnable.hashCode()
        result = 31 * result + limitHeaderBounces.hashCode()
        result = 31 * result + limitFooterBounces.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BouncesEnableElement) return false
        return bouncesEnable == other.bouncesEnable &&
                limitHeaderBounces == other.limitHeaderBounces &&
                limitFooterBounces == other.limitFooterBounces
    }

    override fun update(node: BouncesEnableNode) {
        node.bouncesEnable = bouncesEnable
        node.limitHeaderBounces = limitHeaderBounces
        node.limitFooterBounces = limitFooterBounces
        node.update()
    }
}

private class BouncesEnableNode(
    var bouncesEnable: Boolean,
    var limitHeaderBounces: Boolean,
    var limitFooterBounces: Boolean
) : Modifier.Node() {

    override fun onAttach() {
        super.onAttach()
        update()
    }

    fun update() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val scrollerView = kNode.view as? ScrollerView<*, *> ?: return
        scrollerView.getViewAttr().run {
            bouncesEnable(bouncesEnable, limitHeaderBounces, limitFooterBounces)
        }
    }
}