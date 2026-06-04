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

import com.tencent.kuikly.compose.foundation.ScrollState
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.lazy.LazyListItemInfo
import com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyGridState
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.scroller.ScrollableStateConstants.DEFAULT_CONTENT_SIZE
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.util.fastSumBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Calculate content size
 */
internal fun ScrollableState.calculateContentSize(): Int {
    kuiklyInfo.realContentSize = null
    val density = kuiklyInfo.getDensity()
    val minSize = (ScrollableStateConstants.DEFAULT_CONTENT_SIZE * density).toInt()

    val scrollView = kuiklyInfo.scrollView ?: return minSize

    val contentSize = if (kuiklyInfo.orientation == Orientation.Vertical) {
        scrollView.contentView?.renderView?.currentFrame?.height ?: ScrollableStateConstants.DEFAULT_CONTENT_SIZE.toFloat()
    } else {
        scrollView.contentView?.renderView?.currentFrame?.width ?: ScrollableStateConstants.DEFAULT_CONTENT_SIZE.toFloat()
    } * density

    val viewportSize = kuiklyInfo.viewportSize

    // Return exact content height if total compose container height can be calculated
    val realContentSize = totalContentSize()
    if (realContentSize != null) {
        // Compensate for Modifier padding shrinking Compose internal viewport relative to native viewport
        val composeViewport = composeViewportMainAxisSize() ?: viewportSize
        val viewportDelta = viewportSize - composeViewport
        // Compensate for contentPadding which does not affect viewportSize but is excluded from totalContentSize
        val contentPaddingCompensation = (contentPadding.totalPadding(kuiklyInfo.orientation).value * density).roundToInt()
        kuiklyInfo.realContentSize = realContentSize + viewportDelta + contentPaddingCompensation
        return kuiklyInfo.realContentSize!!
    }

    val bottomOffset = kuiklyInfo.composeOffset.toInt() + viewportSize
    // Expand buffer if bottom offset is close to content size
    if (contentSize - bottomOffset < ScrollableStateConstants.CONTENT_SIZE_BUFFER * density) {
        return (contentSize + ScrollableStateConstants.DEFAULT_EXPAND_SIZE* density).toInt()
    }

    return contentSize.toInt()
}

internal fun ScrollableState.calculateAndUpdateContentSize() {
    // 更新当前的contentSize大小
    val oldContentSize = kuiklyInfo.currentContentSize
    val newContentSize = calculateContentSize()

    // 如果contentSize变小了，需要确保composeOffset不会超出边界
    if (newContentSize < oldContentSize) {
        val newMaxScrollOffset = maxOf(0, newContentSize - kuiklyInfo.viewportSize)
        if (kuiklyInfo.composeOffset > newMaxScrollOffset) {
            // 如果composeOffset超出新的边界，增加contentSize来保持composeOffset不变
            val requiredContentSize = kuiklyInfo.composeOffset.toInt() + kuiklyInfo.viewportSize
            kuiklyInfo.currentContentSize = maxOf(newContentSize, requiredContentSize)
        } else {
            kuiklyInfo.currentContentSize = newContentSize
        }
    } else {
        kuiklyInfo.currentContentSize = newContentSize
    }
    kuiklyInfo.updateContentSizeToRender()
}

internal fun PaddingValues.totalPadding(orientation: Orientation): Dp {
    return if (orientation == Orientation.Vertical) {
        calculateTopPadding() + calculateBottomPadding()
    } else {
        val layoutDirection = LayoutDirection.Ltr
        calculateLeftPadding(layoutDirection) + calculateRightPadding(layoutDirection)
    }
}

/**
 * Calculate total content size
 */
internal fun ScrollableState.totalContentSize(): Int? {
    val curOffset = kuiklyInfo.composeOffset
    return when(this) {
        is LazyListState -> calculateLazyListContentSize(curOffset)
        is PagerState -> calculatePagerContentSize(curOffset)
        is LazyGridState -> calculateLazyGridContentSize(curOffset)
        is LazyStaggeredGridState -> calculateLazyStaggeredGridContentSize(curOffset)
        is ScrollState -> calculateScrollStateContentSize()
        else -> null
    }
}

/**
 * Get the main axis size (in pixels) of the Compose internal viewport.
 * Modifier padding shrinks the Compose internal viewport, causing a delta with the native ScrollView viewport.
 */
private fun ScrollableState.composeViewportMainAxisSize(): Int? {
    return when(this) {
        is LazyListState -> {
            if (layoutInfo.orientation == Orientation.Vertical) layoutInfo.viewportSize.height
            else layoutInfo.viewportSize.width
        }
        is LazyGridState -> {
            if (layoutInfo.orientation == Orientation.Vertical) layoutInfo.viewportSize.height
            else layoutInfo.viewportSize.width
        }
        is LazyStaggeredGridState -> {
            if (layoutInfo.orientation == Orientation.Vertical) layoutInfo.viewportSize.height
            else layoutInfo.viewportSize.width
        }
        is ScrollState -> viewportSize
        else -> null
    }
}

private fun LazyListState.calculateLazyListContentSize(curOffset: Float): Int? {
    return layoutInfo.calculateExactContentSizeFromVisualEnd(curOffset)
}

private fun PagerState.calculatePagerContentSize(curOffset: Float): Int? {
    val lastItem = layoutInfo.visiblePagesInfo.lastOrNull()
    return if (lastItem != null && lastItem.index == pageCount - 1) {
        (curOffset + lastItem.offset + pageSize).toInt()
    } else null
}

private fun LazyGridState.calculateLazyGridContentSize(curOffset: Float): Int? {
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
    return if (lastItem != null && lastItem.index == layoutInfo.totalItemsCount - 1) {
        if (layoutInfo.orientation == Orientation.Vertical) {
            (curOffset + lastItem.offset.y + lastItem.size.height).toInt()
        } else {
            (curOffset + lastItem.offset.x + lastItem.size.width).toInt()
        }
    } else null
}

private fun LazyStaggeredGridState.calculateLazyStaggeredGridContentSize(curOffset: Float): Int? {
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
    return if (lastItem != null && lastItem.index == layoutInfo.totalItemsCount - 1) {
        if (layoutInfo.orientation == Orientation.Vertical) {
            (curOffset + lastItem.offset.y + lastItem.size.height).toInt()
        } else {
            (curOffset + lastItem.offset.x + lastItem.size.width).toInt()
        }
    } else null
}

private fun ScrollState.calculateScrollStateContentSize(): Int? {
    return if (maxValue != Int.MAX_VALUE) {
        maxValue + viewportSize
    } else null
}

/**
 * Calculate back expansion size
 */
internal fun ScrollableState.calculateBackExpandSize(offset: Int): Int? {
    if (this !is LazyListState) return null

    val estimateOffset = layoutInfo.estimateVisualTopScrollOffset(
        hasPullToRefresh = kuiklyInfo.hasPullToRefresh
    ) ?: return null
    val density = this.kuiklyInfo.getDensity()

    return if (estimateOffset - offset > ScrollableStateConstants.SCROLL_THRESHOLD * density) {
        estimateOffset - offset + (ScrollableStateConstants.MIN_EXPAND_SIZE * density).toInt()
    } else null
}

/**
 * 尝试扩展起始大小
 */
internal fun ScrollableState.tryExpandStartSize(offset: Int, isScrolling: Boolean) {
    if (kuiklyInfo.scrollView == null) return

    val density = kuiklyInfo.getDensity()
    val minDelta = (ScrollableStateConstants.DEFAULT_CONTENT_SIZE * density).toInt()
    if (tryHandleReverseLazyNativeRange(
            contentOffset = offset,
            viewportSize = kuiklyInfo.viewportSize,
            currentContentSize = kuiklyInfo.currentContentSize,
            minExpandSize = minDelta,
            epsilon = reverseNativeRangeEpsilon(density),
        )
    ) {
        return
    }
    // scrollview 到顶了，但是compose没到顶
    if (offset <= 0 && !isAtTop() && kuiklyInfo.offsetDirty) {
        var delta = calculateBackExpandSize(offset)
        delta = max(delta ?: minDelta, minDelta)

        val littleDelta = (ScrollableStateConstants.SCROLL_THRESHOLD * density).toInt()
        val maxDelta = kuiklyInfo.currentContentSize - kuiklyInfo.viewportSize - kuiklyInfo.contentOffset

        if ((delta + littleDelta) > maxDelta) {
            // 不够直接扩容offset，先扩容contentSize
            kuiklyInfo.currentContentSize += (delta - maxDelta + minDelta)
            kuiklyInfo.updateContentSizeToRender()
        }

        if (!kuiklyInfo.scrollView!!.isDragging) {
            // 让滑动停下来
            applyScrollViewOffsetDelta(littleDelta)
        }
        kuiklyInfo.offsetDirty = true
        applyScrollViewOffsetDelta(delta)
    } else if (offset > 0 && isAtTop()) {
        // compose 到顶了，但是scrollview没到顶
        applyScrollViewOffsetDelta(-offset)
        kuiklyInfo.offsetDirty = false
    }
}

internal fun ScrollableState.tryExpandStartSizeNoScroll(forceExpand: Boolean = false) {
    if (this is PagerState) return
    kuiklyInfo.run {
        appleScrollViewOffsetJob?.cancel()
        appleScrollViewOffsetJob = scope?.launch {
            delay(150)
            val minDelta = (DEFAULT_CONTENT_SIZE * getDensity()).toInt()
            val epsilon = reverseNativeRangeEpsilon(getDensity())
            val reachBtm = contentOffset + viewportSize - currentContentSize >= -epsilon

            if (this@tryExpandStartSizeNoScroll.tryHandleReverseLazyNativeRange(
                    contentOffset = contentOffset,
                    viewportSize = viewportSize,
                    currentContentSize = currentContentSize,
                    minExpandSize = minDelta,
                    epsilon = epsilon,
                )
            ) {
            } else if (!reverseLayout && contentOffset <= 0 && !isAtTop() && (forceExpand || scrollView?.isDragging != true)) {
                // 整体把offset 加一下
                var delta = calculateBackExpandSize(contentOffset)
                delta = max(delta ?: minDelta, minDelta)
                val maxDelta = currentContentSize - viewportSize - contentOffset
                if (delta > maxDelta) {
                    // 不够直接扩容offset，先扩容contentSize
                    currentContentSize += (delta - maxDelta + minDelta)
                    updateContentSizeToRender()
                }
                if (pageData?.isOhOs == true) {
                    delay(25)   // 鸿蒙扩容后，不会立刻刷新，也没有刷新api，华为建议添加一个delay来处理
                }
                applyScrollViewOffsetDelta(delta)
                offsetDirty = true
            } else if (!reverseLayout && contentOffset > 0 && isAtTop()) {
                // compose 到顶了，但是scrollview没到顶
                applyScrollViewOffsetDelta(-contentOffset)
                offsetDirty = false
            } else if (isAtTop() && realContentSize == null && scrollView?.isDragging != true) {
                // 更新当前的contentSize大小
                currentContentSize = calculateContentSize()
                updateContentSizeToRender()
            } else if (canScrollForward && reachBtm) {
                // 底部无法滑动了，扩容
                currentContentSize += minDelta
                updateContentSizeToRender()
            }
        }
    }
}

private fun reverseNativeRangeEpsilon(density: Float): Int = (0.5 * density).toInt()

private fun ScrollableState.tryHandleReverseLazyNativeRange(
    contentOffset: Int,
    viewportSize: Int,
    currentContentSize: Int,
    minExpandSize: Int,
    epsilon: Int,
): Boolean {
    val lazyListState = this as? LazyListState ?: return false
    if (!kuiklyInfo.reverseLayout) return false

    if (lazyListState.layoutInfo.shouldExpandReverseNativeEnd(
            contentOffset = contentOffset,
            viewportSize = viewportSize,
            currentContentSize = currentContentSize,
            canScrollForward = lazyListState.canScrollForward,
            canScrollBackward = lazyListState.canScrollBackward,
            epsilon = epsilon,
        )
    ) {
        kuiklyInfo.currentContentSize += minExpandSize
        kuiklyInfo.updateContentSizeToRender()
        kuiklyInfo.offsetDirty = true
        return true
    }

    if (lazyListState.layoutInfo.shouldResetReverseNativeStart(
            contentOffset = contentOffset,
            canScrollForward = lazyListState.canScrollForward,
            canScrollBackward = lazyListState.canScrollBackward,
        )
    ) {
        applyScrollViewOffsetDelta(-contentOffset)
        kuiklyInfo.offsetDirty = false
        return true
    }

    return false
}

internal fun LazyListLayoutInfo.shouldExpandReverseNativeEnd(
    contentOffset: Int,
    viewportSize: Int,
    currentContentSize: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
    epsilon: Int = 0,
): Boolean {
    if (!reverseLayout) return false
    val reachedNativeEnd = contentOffset + viewportSize - currentContentSize >= -epsilon
    return reachedNativeEnd && !isAtVisualTop(canScrollForward, canScrollBackward)
}

internal fun LazyListLayoutInfo.shouldResetReverseNativeStart(
    contentOffset: Int,
    canScrollForward: Boolean,
    canScrollBackward: Boolean,
): Boolean {
    if (!reverseLayout || contentOffset <= 0) return false
    return isAtVisualBottom(canScrollForward, canScrollBackward)
}

internal fun LazyListLayoutInfo.calculateExactContentSizeFromVisualEnd(curOffset: Float): Int? {
    val terminalItem = if (reverseLayout) visualTopItem() else visualBottomItem()
    if (terminalItem?.index != totalItemsCount - 1) return null
    val visualBottomEdge = visibleItemsInfo.maxOfOrNull { visualBottomEdgeOffset(it) } ?: return null
    return (curOffset + visualBottomEdge).toInt()
}

internal fun LazyListLayoutInfo.estimateVisualTopScrollOffset(hasPullToRefresh: Boolean): Int? {
    val visibleItems = visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val avgSize = visibleItems.fastSumBy { it.size } / visibleItems.size + mainAxisItemSpacing
    val visualTopItem = visualTopItem() ?: return null
    val itemsBeforeVisualTop = if (reverseLayout) {
        maxOf(0, totalItemsCount - 1 - visualTopItem.index)
    } else {
        val pullToRefreshOffset = if (hasPullToRefresh) 1 else 0
        maxOf(0, visualTopItem.index - pullToRefreshOffset)
    }
    return itemsBeforeVisualTop * avgSize - visualTopOffset(visualTopItem)
}

private fun LazyListLayoutInfo.visualTopItem(): LazyListItemInfo? =
    if (reverseLayout) visibleItemsInfo.lastOrNull() else visibleItemsInfo.firstOrNull()

private fun LazyListLayoutInfo.visualBottomItem(): LazyListItemInfo? =
    if (reverseLayout) visibleItemsInfo.firstOrNull() else visibleItemsInfo.lastOrNull()

private fun LazyListLayoutInfo.visualTopOffset(item: LazyListItemInfo): Int =
    if (reverseLayout) mainAxisViewportSize() - item.offset - item.size else item.offset

private fun LazyListLayoutInfo.visualBottomEdgeOffset(item: LazyListItemInfo): Int =
    if (reverseLayout) mainAxisViewportSize() - item.offset else item.offset + item.size

private fun LazyListLayoutInfo.mainAxisViewportSize(): Int =
    if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
