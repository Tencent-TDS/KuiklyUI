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
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * 动态设置视图属性修饰符
 * @param key 属性键名（如"alpha"）
 * @param value 属性值（支持任意类型）
 */
fun Modifier.setProp(
    key: String,
    value: Any,
): Modifier = this.then(SetPropElement(key, value))

// region ------------------------------ 修饰符节点实现 ------------------------------
internal class SetPropElement(
    val key: String,
    private val value: Any,
) : ModifierNodeElement<SetPropNode>() {
    override fun create() = SetPropNode(key, value)

    override fun update(node: SetPropNode) {
        node.updateProp(key, value) // 更新时同步最新键值对[1](@ref)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SetPropElement && key == other.key && value == other.value
    }

    override fun hashCode(): Int = 31 * key.hashCode() + value.hashCode()
}

internal class SetPropNode(
    initialKey: String,
    initialValue: Any,
) : Modifier.Node() {
    private val props = mutableMapOf(initialKey to initialValue)

    fun updateProp(
        newKey: String,
        newValue: Any,
    ) {
        if (props[newKey] != newValue) {
            props[newKey] = newValue
            applyProps() // 触发属性更新[1](@ref)
        }
    }

    private fun applyProps() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val view = kNode.view as? DeclarativeBaseView<*, *> ?: return
        props.forEach { (key, value) ->
            view.getViewAttr().setProp(key, value)
        }
    }

    override fun onAttach() {
        applyProps() // 视图挂载时立即应用属性[1](@ref)
    }
}
// endregion

fun Modifier.placeHolder(
    placeholder: String,
    placeholderColor: Color,
): Modifier = this.setProp("placeholder", placeholder).setProp("placeholderColor", placeholderColor.toKuiklyColor().toString())

fun Modifier.lineSpacing(lineSpace: Float?): Modifier {
    if (lineSpace == null) {
        return this
    }
    return setProp("lineSpacing", lineSpace)
}

fun Modifier.placeholderColor(color: Color): Modifier = setProp("placeholderColor", color.toKuiklyColor().toString())

fun Modifier.lineBreakMargin(dp: Dp): Modifier = setProp("lineBreakMargin", dp.value)
