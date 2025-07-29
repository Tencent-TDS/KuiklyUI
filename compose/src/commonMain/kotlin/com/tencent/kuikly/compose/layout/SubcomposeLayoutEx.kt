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

package com.tencent.kuikly.compose.layout

import com.tencent.kuikly.compose.foundation.lazy.LazyListMeasureResult
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyGridMeasureResult
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridMeasureResult
import com.tencent.kuikly.compose.foundation.pager.PagerMeasureResult
import com.tencent.kuikly.compose.ui.layout.LayoutNodeSubcompositionsState
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.core.base.Attr

fun MeasureResult.getPositionedItemsKeys(): Set<*> = when (this) {
    is LazyListMeasureResult -> positionedItems.map { it.key }.toSet()
    is LazyGridMeasureResult -> positionedItems.map { it.key }.toSet()
    is LazyStaggeredGridMeasureResult -> positionedItems.map { it.key }.toSet()
    is PagerMeasureResult -> positionedPages.map { it.key }.toSet()
    else -> emptySet<Int>()
}

internal fun LayoutNodeSubcompositionsState.getStickItemKey(result: MeasureResult): Any? {
    return if (result is LazyListMeasureResult) result.stickyItem?.key else null
}

internal fun LayoutNodeSubcompositionsState.checkOffScreenNode(result: MeasureResult) {
    if (result !is LazyListMeasureResult &&
        result !is LazyGridMeasureResult &&
        result !is LazyStaggeredGridMeasureResult &&
        result !is PagerMeasureResult
    ) {
        return
    }

    val positionedItemKeys = result.getPositionedItemsKeys()
    val stickyItemKey = getStickItemKey(result)

    slotIdToNode.forEach { (key, node) ->
        if (node is KNode<*>) {
            if (!positionedItemKeys.contains(key) && key != stickyItemKey) {
                node.hideOffsetScreenView()
            }
        }
    }
}

internal fun KNode<*>.hideOffsetScreenView() {
    when {
        isVirtual -> forEachChild { (it as? KNode<*>)?.hideOffsetScreenView() }
        else -> {
            // 记录下原始的Visible属性
            if (viewVisible == null) {
                viewVisible = view.getViewAttr().getProp(Attr.StyleConst.VISIBILITY) != 0
                view.getViewAttr().visibility(false)
            }
        }
    }
}

internal fun KNode<*>.resetViewVisible() {
    when {
        isVirtual -> forEachChild { (it as? KNode<*>)?.resetViewVisible() }
        else -> {
            // 恢复到原始的Visible属性
            viewVisible?.let {
                view.getViewAttr().visibility(it)
                viewVisible = null
            }
        }
    }
}