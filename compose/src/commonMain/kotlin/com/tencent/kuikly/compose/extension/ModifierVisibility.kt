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

/**
 * 可见性修饰符
 *
 * @param visible 是否可见（true=显示组件，false=隐藏组件）
 */
@Deprecated(
    message = "已废弃，请使用变量If Else控制显示",
    replaceWith = ReplaceWith("已废弃，请使用变量If Else控制显示"),
    level = DeprecationLevel.WARNING
)
fun Modifier.visibility(
    visible: Boolean
): Modifier = this.then(VisibilityElement(visible))

// region ------------------------------ 核心实现 ------------------------------

/**
 * 自定义Modifier的Element实现
 */
private class VisibilityElement(
    private val visible: Boolean
) : ModifierNodeElement<VisibilityNode>() {

    override fun create() = VisibilityNode(visible)

    override fun update(node: VisibilityNode) {
        node.updateModes(visible)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VisibilityElement) return false
        return visible == other.visible
    }

    override fun hashCode(): Int {
        return visible.hashCode()
    }
}

/**
 * 可见性控制节点
 */
private class VisibilityNode(
    initialVisible: Boolean,
) : Modifier.Node() {
    private var currentVisible = initialVisible

    // 更新可见性模式
    fun updateModes(newVisible: Boolean) {
        if (currentVisible != newVisible) {
            currentVisible = newVisible
            applyVisibility()
        }
    }

    // 应用可见性到视图
    private fun applyVisibility() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        kNode.view.getViewAttr().visibility(visibility = currentVisible)
    }

    override fun onAttach() {
        applyVisibility()
    }
}

// endregion