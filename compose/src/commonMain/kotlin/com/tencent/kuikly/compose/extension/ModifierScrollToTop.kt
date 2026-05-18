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

import androidx.compose.runtime.Stable
import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.views.KuiklyInfoKey
import com.tencent.kuikly.core.views.ScrollerAttr
import com.tencent.kuikly.core.views.ScrollerEvent
import com.tencent.kuikly.core.views.ScrollerView

/**
 * Modifier 扩展函数，用于设置 scrollToTop 事件回调
 *
 * 当触发"回到顶部"事件时（如点击 iOS 状态栏或 Android ColorOS 设备状态栏），
 * 会调用此回调而不是执行默认的滚动到顶部行为。
 *
 * 此 Modifier 会自动从可滚动组件中获取 ScrollableState，无需显式传递。
 *
 * 使用示例：
 * ```kotlin
 * LazyColumn(
 *     modifier = Modifier.scrollToTop {
 *         // 自定义处理逻辑
 *         coroutineScope.launch {
 *             listState.animateScrollToItem(0)
 *         }
 *     }
 * ) {
 *     // items
 * }
 * ```
 *
 * @param onScrollToTop 回调函数，当 scrollToTop 事件触发时调用
 * @return 返回 Modifier 实例，支持链式调用
 */
@Stable
fun Modifier.scrollToTop(
    onScrollToTop: () -> Unit
): Modifier = this.then(ScrollToTopElement(onScrollToTop))

// region ------------------------------ 修饰符节点实现 ------------------------------

internal class ScrollToTopElement(
    private val onScrollToTop: () -> Unit
) : ModifierNodeElement<ScrollToTopNode>() {
    override fun create() = ScrollToTopNode(onScrollToTop)

    override fun update(node: ScrollToTopNode) {
        node.updateCallback(onScrollToTop)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ScrollToTopElement && onScrollToTop == other.onScrollToTop
    }

    override fun hashCode(): Int = onScrollToTop.hashCode()
}

internal class ScrollToTopNode(
    private var onScrollToTop: () -> Unit
) : Modifier.Node() {

    fun updateCallback(newCallback: () -> Unit) {
        onScrollToTop = newCallback
        applyCallback()
    }

    private fun applyCallback() {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return
        val scrollView = kNode.view as? ScrollerView<ScrollerAttr, ScrollerEvent> ?: return
        val kuiklyInfo = scrollView.extProps?.get(KuiklyInfoKey) as? KuiklyScrollInfo ?: return
        kuiklyInfo.scrollToTopCallback = onScrollToTop
    }

    override fun onAttach() {
        applyCallback()
    }
}

// endregion
