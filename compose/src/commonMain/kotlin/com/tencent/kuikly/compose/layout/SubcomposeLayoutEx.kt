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

import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.lazy.LazyListMeasureResult
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyGridMeasureResult
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridMeasureResult
import com.tencent.kuikly.compose.foundation.pager.PagerMeasureResult
import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.compose.ui.layout.LayoutNodeSubcompositionsState
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.KNode.Companion.obtainRenderProps
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.views.KuiklyInfoKey
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.views.ScrollerAttr
import com.tencent.kuikly.core.views.ScrollerEvent
import com.tencent.kuikly.core.views.ScrollerView

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

/**
 * Bind [KuiklyScrollInfo] to the [ScrollerView] and return it.
 */
internal fun bindKuiklyInfo(
    sv: ScrollerView<ScrollerAttr, ScrollerEvent>,
    scrollableState: ScrollableState,
    orientation: Orientation,
): KuiklyScrollInfo {
    val kuiklyInfo = scrollableState.kuiklyInfo
    kuiklyInfo.scrollView = sv
    kuiklyInfo.orientation = orientation
    sv.obtainRenderProps().kuiklyScrollInfo = kuiklyInfo
    sv.extProps[KuiklyInfoKey] = kuiklyInfo as Any
    return kuiklyInfo
}

/**
 * Transfer scrollToTopCallback from [old] to [new].
 *
 * ScrollToTopNode.onAttach() runs before the update block and sets the callback
 * on the old kuiklyInfo; we need to move it to the new one.
 */
internal fun transferScrollToTopCallback(old: KuiklyScrollInfo?, new: KuiklyScrollInfo) {
    if (old != null && old !== new) {
        old.scrollToTopCallback?.let { new.scrollToTopCallback = it }
    }
}

/**
 * Restore ScrollerView state during the update block (both first creation and reuse).
 *
 * Re-applies factory attrs, resets native transient state, clears stale bridge caches,
 * and syncs native contentSize + contentOffset from rememberSaveable-restored values.
 */
internal fun restoreScrollerViewOnReuse(
    sv: ScrollerView<ScrollerAttr, ScrollerEvent>,
    kuiklyInfo: KuiklyScrollInfo,
    isPagerView: Boolean,
    orientation: Orientation,
    oldSvOffset: Int? = null,
) {
    // Re-apply factory attrs (init lambda only runs at creation, not on reuse)
    sv.getViewAttr().run {
        flingEnable(!isPagerView)
        setProp("isComposePager", if (isPagerView) 1 else 0)
        setProp("dynamicSyncScrollDisable", 1)
        if (orientation == Orientation.Vertical) flexDirectionColumn() else flexDirectionRow()
        showScrollerIndicator(false)
    }

    sv.abortContentOffsetAnimate()
    sv.prepareForComposeReuse()

    kuiklyInfo.ignoreScrollOffset = null
    kuiklyInfo.appleScrollViewOffsetJob?.cancel()
    kuiklyInfo.appleScrollViewOffsetJob = null
    kuiklyInfo.realContentSize = null

    // Restore contentSize first (UIKit clamps contentOffset to contentSize bounds)
    kuiklyInfo.updateContentSizeToRender()

    // Restore contentOffset with ignoreScrollOffset protection.
    // Skip ignoreScrollOffset when oldSvOffset == restoreOffset, because setContentOffset
    // won't change the value and iOS won't fire a scroll callback to clear the flag.
    val density = kuiklyInfo.getDensity()
    val restoreOffset = kuiklyInfo.contentOffset
    val offsetInDp = restoreOffset / density
    val restoreOffsetX = if (kuiklyInfo.isVertical()) 0f else offsetInDp
    val restoreOffsetY = if (kuiklyInfo.isVertical()) offsetInDp else 0f

    val needIgnore = oldSvOffset == null || oldSvOffset != restoreOffset
    if (needIgnore) {
        kuiklyInfo.ignoreScrollOffset = IntOffset(
            x = (restoreOffsetX * density).toInt(),
            y = (restoreOffsetY * density).toInt(),
        )
    }
    sv.setContentOffset(restoreOffsetX, restoreOffsetY, animated = false)
}