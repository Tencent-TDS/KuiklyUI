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

/**
 * 内部 API：设置鼠标光标样式 (macOS)。
 * 业务侧请使用 [com.tencent.kuikly.compose.ui.input.pointer.pointerHoverIcon]。
 */
internal fun Modifier.cursor(type: String): Modifier = setProp("cursor", type)

/**
 * Set text post-processor name for text/input components.
 * Works with KRTextPostProcessorAdapter to enable features like emoji shortcode replacement.
 * @param processor processor name, e.g. "input"
 */
fun Modifier.textPostProcessor(processor: String): Modifier = setProp("textPostProcessor", processor)

/**
 * iOS：控制程序化同步 [com.tencent.kuikly.compose.ui.text.input.TextFieldValue]（原生 setTextInputState）时，
 * 非空文本是否自动抢占焦点并弹起键盘。
 *
 * 默认不设置时为 false，避免进页带预填文本时自动弹键盘；需要旧行为时显式传 true。
 */
fun Modifier.autoFocusOnTextInputState(enabled: Boolean): Modifier =
    setProp("autoFocusOnTextInputState", if (enabled) 1 else 0)

/**
 * 设置该节点及其子树能否在 TurboDisplay 首屏缓存中持续更新属性值（默认 true）。
 *
 * 对应自研 DSL 的 `attr { turboDisplayAutoUpdateEnable(enable) }`：设为 false 后，
 * diff-DOM 采集缓存快照时该节点子树被冻结——首次快照的值保留在缓存中，后续真实树
 * 的属性更新只上屏、不写入缓存快照。
 * 典型用途：敏感/时效性字段在缓存首屏中保持占位形态，真实数据仅接管后上屏。
 */
fun Modifier.turboDisplayAutoUpdateEnable(enable: Boolean): Modifier =
    setProp(Attr.StyleConst.TURBO_DISPLAY_AUTO_UPDATE_ENABLE, if (enable) 1 else 0)
