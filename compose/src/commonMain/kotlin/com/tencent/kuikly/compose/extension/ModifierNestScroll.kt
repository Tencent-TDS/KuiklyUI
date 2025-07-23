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
import com.tencent.kuikly.core.views.KRNestedScrollMode
import com.tencent.kuikly.core.views.ScrollerAttr
import com.tencent.kuikly.core.views.ScrollerView

/**
 * 自定义嵌套滚动模式枚举
 *
 * - SELF_ONLY   : 仅当前控件处理滚动
 * - SELF_FIRST  : 当前控件优先处理滚动，未消费完的滚动量传递给父控件
 * - PARENT_FIRST: 父控件优先处理滚动，未消费完的滚动量传递给当前控件
 * - PARALLEL    : 当前控件和父控件同时处理滚动
 */
enum class NestedScrollMode(val value: String) {
    SELF_ONLY("SELF_ONLY"),
    SELF_FIRST("SELF_FIRST"),
    PARENT_FIRST("PARENT_FIRST"),
    PARALLEL("PARALLEL")
}

// region ------------------------------ 核心实现 ------------------------------

/**
 * 将自定义滚动模式转换为框架原生模式
 */
private fun NestedScrollMode.toFrameworkMode(): KRNestedScrollMode = when (this) {
    NestedScrollMode.SELF_ONLY -> KRNestedScrollMode.SELF_ONLY
    NestedScrollMode.SELF_FIRST -> KRNestedScrollMode.SELF_FIRST
    NestedScrollMode.PARENT_FIRST -> KRNestedScrollMode.PARENT_FIRST
    NestedScrollMode.PARALLEL -> KRNestedScrollMode.PARALLEL
}

/**
 * 自定义Modifier的Element实现
 *
 * 这是Compose修饰符的核心桥接类，负责创建和更新滚动控制节点
 */
private class NestedScrollElement(
    private val scrollUp: NestedScrollMode,
    private val scrollDown: NestedScrollMode
) : ModifierNodeElement<NestedScrollNode>() {

    override fun create() = NestedScrollNode(scrollUp, scrollDown)

    override fun update(node: NestedScrollNode) {
        node.updateModes(scrollUp, scrollDown)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NestedScrollElement) return false
        return scrollUp == other.scrollUp && scrollDown == other.scrollDown
    }

    override fun hashCode(): Int {
        var result = scrollUp.hashCode()
        result = 31 * result + scrollDown.hashCode()
        return result
    }
}

/**
 * 嵌套滚动控制节点
 *
 * 这是实际控制滚动行为的节点，在UI树附加/更新时生效
 */
private class NestedScrollNode(
    initialScrollUp: NestedScrollMode,
    initialScrollDown: NestedScrollMode
) : Modifier.Node() {
    // 当前生效的滚动模式
    private var currentUpMode = initialScrollUp
    private var currentDownMode = initialScrollDown

    // 更新滚动模式（在Element更新时调用）
    fun updateModes(newUp: NestedScrollMode, newDown: NestedScrollMode) {
        if (currentUpMode != newUp || currentDownMode != newDown) {
            currentUpMode = newUp
            currentDownMode = newDown
            applyScrollModes()
        }
    }

    // 当节点附加到UI树时调用
    override fun onAttach() {
        applyScrollModes()
    }

    // 核心逻辑：将滚动模式应用到实际视图
    private fun applyScrollModes() {
        // 1. 获取底层视图
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val scrollerView = kNode.view as? ScrollerView<*, *> ?: return

        // 2. 转换为框架需要的模式
        val krUpMode = currentUpMode.toFrameworkMode()
        val krDownMode = currentDownMode.toFrameworkMode()

        // 3. 应用滚动模式到视图属性
        (scrollerView.getViewAttr() as ScrollerAttr).apply {
            nestedScroll(
                forward = krUpMode,    // 上滑方向对应forward
                backward = krDownMode  // 下滑方向对应backward
            )
        }
    }
}

// endregion

// region ------------------------------ 公开API ------------------------------

/**
 * 嵌套滚动修饰符
 *
 * 用于控制滚动视图的嵌套滚动行为，典型使用场景：
 *
 * 1. 当多个滚动视图嵌套时（如ScrollView内嵌RecyclerView）
 * 2. 需要精细控制滚动事件的分发优先级时
 *
 * @param scrollUp   上滑滚动时的处理策略（如拉到顶部时的越界滚动）
 * @param scrollDown 下滑滚动时的处理策略（如拉到底部时的越界滚动）
 *
 * 示例1：禁止所有越界回弹
 * Modifier.nestedScroll(
 *     scrollUp = NestedScrollMode.SELF_ONLY,
 *     scrollDown = NestedScrollMode.SELF_ONLY
 * )
 *
 * 示例2：允许父容器优先处理下拉刷新
 * Modifier.nestedScroll(
 *     scrollUp = NestedScrollMode.PARENT_FIRST,  // 上滑时父容器优先
 *     scrollDown = NestedScrollMode.SELF_FIRST   // 下拉时自己优先
 * )
 */
fun Modifier.nestedScroll(
    scrollUp: NestedScrollMode = NestedScrollMode.SELF_FIRST,
    scrollDown: NestedScrollMode = NestedScrollMode.SELF_FIRST
): Modifier = this.then(NestedScrollElement(scrollUp, scrollDown))

// endregion