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

import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.SemanticsActions
import com.tencent.kuikly.compose.ui.semantics.SemanticsConfiguration
import com.tencent.kuikly.compose.ui.semantics.SemanticsNode
import com.tencent.kuikly.compose.ui.semantics.SemanticsOwner
import com.tencent.kuikly.compose.ui.semantics.SemanticsProperties
import com.tencent.kuikly.compose.ui.semantics.getAllSemanticsNodes
import com.tencent.kuikly.compose.ui.semantics.getOrNull
import com.tencent.kuikly.core.base.attr.AccessibilityRole

/**
 * Kuikly无障碍语义变更处理器。
 * 
 * 主要功能：
 * 1. 监听并处理 Compose 语义树的变更，自动为节点设置合适的无障碍文本和角色。
 * 2. 感知 stateDescription 的新增、变化和消失，并提供回调接口供业务自定义处理。
 * 3. 内部维护节点 stateDescription 的缓存，支持外部主动清理缓存，防止内存泄漏。
 */
class KuiklySemantisHandler {

    private val lastStateDescriptionMap = mutableMapOf<Int, String?>()

    /**
     * 语义树变更回调，自动为节点设置无障碍文本和角色，并感知 stateDescription 变化。
     * @param semanticsOwner 当前 Compose 语义树的 owner
     */
    fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        val allNodes = semanticsOwner.getAllSemanticsNodes(mergingEnabled = true)
        val currentNodeIds = mutableSetOf<Int>()
        allNodes.forEach { node ->
            val config = node.config
            val role = config.getOrNull(SemanticsProperties.Role)
            val stateDescription = config.getOrNull(SemanticsProperties.StateDescription)
            val nodeId = node.id
            currentNodeIds.add(nodeId)

            val isClickable = config.getOrNull(SemanticsActions.OnClick) != null
            val isLongClickable = config.getOrNull(SemanticsActions.OnLongClick) != null
            (node.layoutNode as? KNode<*>)?.run {
                val accessibility = buildAccessibilityText(node.config)
                if (accessibility != "") {
                    view.getViewAttr().accessibility(accessibility)
                    val kuiklyAccRole = convertComposeRoleToKuiklyRole(role)
                    view.getViewAttr().accessibilityRole(kuiklyAccRole)
                    view.getViewAttr().accessibilityInfo(isClickable, isLongClickable)
                }

                val last = lastStateDescriptionMap[nodeId]
                if (stateDescription != last) {
                    val changeType = when {
                        last == null && stateDescription != null -> StateDescChangeType.ADDED
                        last != null && stateDescription == null -> StateDescChangeType.REMOVED
                        else -> StateDescChangeType.CHANGED
                    }
                    handleStateDescription(node, stateDescription ?: "", changeType)
                }
                lastStateDescriptionMap[nodeId] = stateDescription
            }
        }
        val removedIds = lastStateDescriptionMap.keys - currentNodeIds
        for (removedId in removedIds) {
            val lastDesc = lastStateDescriptionMap[removedId]
            if (lastDesc != null) {
                handleStateDescription(null, lastDesc, StateDescChangeType.REMOVED)
            }
            lastStateDescriptionMap.remove(removedId)
        }
    }

    /**
     * 处理组件的状态描述变更（可重写）
     * @param node 语义节点（被移除时为 null）
     * @param stateDescription 状态描述文本
     * @param changeType 变化类型（新增/变化/消失）
     */
    protected open fun handleStateDescription(
        node: SemanticsNode?,
        stateDescription: String,
        changeType: StateDescChangeType
    ) {
        // 例如：上报埋点、联动UI、日志等
        when(changeType) {
            StateDescChangeType.CHANGED -> {
                (node?.layoutNode as KNode<*>)
                    .view
                    .accessibilityAnnounce(stateDescription)
            }
            else ->  {}
        }
    }

    /**
     * stateDescription 变化类型
     */
    enum class StateDescChangeType {
        ADDED, CHANGED, REMOVED
    }

    /**
     * 构建节点的无障碍文本，按优先级拼接
     */
    private fun buildAccessibilityText(config: SemanticsConfiguration): String {
        val textBuilder = StringBuilder()

        config.getOrNull(SemanticsProperties.StateDescription)?.let {
            textBuilder.append(it)
        }

        // 处理 ContentDescription
        config.getOrNull(SemanticsProperties.ContentDescription)?.let {
            if (textBuilder.isNotEmpty()) {
                textBuilder.append(", ")
            }
            textBuilder.append(it.joinToString(", "))
        }

        // 处理 Text, 当 不是LazyList等容器时, 且没有 ContentDescription 时使用
        if (textBuilder.isEmpty() || config.getOrNull(SemanticsProperties.IsTraversalGroup) == null) {
            config.getOrNull(SemanticsProperties.Text)?.let {
                if (textBuilder.isNotEmpty()) {
                    textBuilder.append(", ")
                }
                textBuilder.append(it.joinToString(", "))
            }
        }

        config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.let {
            if (textBuilder.isNotEmpty()) {
                textBuilder.append(", ")
            }
            textBuilder.append("${it.current}%")
        }

        config.getOrNull(SemanticsProperties.Selected)?.let {
            if (it && textBuilder.isNotEmpty()) {
                textBuilder.append(", 已选择")
            }
        }
        return textBuilder.toString()
    }

    private fun convertComposeRoleToKuiklyRole(role: Role?): AccessibilityRole {
        val kuiklyAccRole = when (role) {
            Role.Image -> AccessibilityRole.IMAGE
            Role.Checkbox -> AccessibilityRole.CHECKBOX
            Role.Switch -> AccessibilityRole.TEXT
            Role.Button -> AccessibilityRole.BUTTON
            Role.RadioButton -> AccessibilityRole.CHECKBOX
            else -> AccessibilityRole.TEXT
        }
        return kuiklyAccRole
    }

    fun clearCache() {
        lastStateDescriptionMap.clear()
    }

}