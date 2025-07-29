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

package com.tencent.kuikly.compose.scroller

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode

fun Modifier.fixScrollOffset(
    needFix: Boolean
): Modifier = this.then(FixScrollOffsetElement(needFix))

private class FixScrollOffsetElement(
    val needFix: Boolean = true
) : ModifierNodeElement<FixScrollOffsetNode>() {
    override fun create(): FixScrollOffsetNode {
        val node = FixScrollOffsetNode()
        node.needFix = needFix
        return node
    }

    override fun hashCode(): Int {
        return needFix.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FixScrollOffsetElement) return false
        return needFix == other.needFix
    }

    override fun update(node: FixScrollOffsetNode) {
        node.needFix = needFix
        node.update()
    }
}

private class FixScrollOffsetNode : Modifier.Node() {

    var needFix: Boolean = true

    fun update() {
        if (isAttached) {
            (requireLayoutNode() as? KNode<*>)?.apply {
                needFixScrollOffset = needFix
            }
        }
    }

    override fun onAttach() {
        super.onAttach()
        update()
    }

}