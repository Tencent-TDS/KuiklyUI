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

/**
 * 计算内容大小
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

    // 如果可以算出来总的compose容器高度了，就返回精确的容器高度
    val realContentSize = totalContentSize()
    if (realContentSize != null) {
        kuiklyInfo.realContentSize =
            realContentSize + (contentPadding.totalPadding(kuiklyInfo.orientation).value * density).toInt()
        return kuiklyInfo.realContentSize!!
    }

    val bottomOffset = kuiklyInfo.composeOffset.toInt() + viewportSize
    // 如果当前的底部距离容器高度较近了，就扩大缓冲区
    if (contentSize - bottomOffset < ScrollableStateConstants.CONTENT_SIZE_BUFFER * density) {
        return (contentSize + ScrollableStateConstants.DEFAULT_EXPAND_SIZE* density).toInt()
    }

    return contentSize.toInt()
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
 * 计算总内容大小
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

private fun LazyListState.calculateLazyListContentSize(curOffset: Float): Int? {
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
    return if (lastItem != null && lastItem.index == layoutInfo.totalItemsCount - 1) {
        (curOffset + lastItem.offset + lastItem.size).toInt()
    } else null
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

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val itemsSum = visibleItems.fastSumBy { it.size }
    val avgSize = itemsSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
    val firstItem = visibleItems.firstOrNull() ?: return null

    // Adjust for PullToRefresh offset if it exists
    val pullToRefreshOffset = if (kuiklyInfo.hasPullToRefresh) 1 else 0
    val adjustedFirstItemIndex = maxOf(0, firstItem.index - pullToRefreshOffset)

    val estimateOffset = adjustedFirstItemIndex * avgSize - firstItem.offset
    val density = this.kuiklyInfo.getDensity()

    return if (estimateOffset - offset > ScrollableStateConstants.SCROLL_THRESHOLD * density) {
        estimateOffset - offset + (ScrollableStateConstants.MIN_EXPAND_SIZE * density).toInt()
    } else null
}

/**
 * 尝试扩展起始大小
 */
internal fun ScrollableState.tryExpandStartSize(offset: Int, isScrolling: Boolean) {
    if (kuiklyInfo.scrollView == null || !kuiklyInfo.offsetDirty) return

    val density = kuiklyInfo.getDensity()
    // scrollview 到顶了，但是compose没到顶
    if (offset <= 0 && !isAtTop()) {
        var delta = calculateBackExpandSize(offset)
        val minDelta = (ScrollableStateConstants.DEFAULT_CONTENT_SIZE * density).toInt()
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

internal fun ScrollableState.tryExpandStartSizeNoScroll() {
    if (this is PagerState) return
    kuiklyInfo.run {
        appleScrollViewOffsetJob?.cancel()
        appleScrollViewOffsetJob = scope?.launch {
            delay(150)
            val minDelta = (DEFAULT_CONTENT_SIZE * getDensity()).toInt()
            val epsilon = 0.5 * getDensity()  // 使用 0.5dp 作为误差值
            val reachBtm = contentOffset + viewportSize - currentContentSize >= -epsilon

            if (contentOffset <= 0 && !isAtTop() && scrollView?.isDragging != true) {
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
            } else if (contentOffset > 0 && isAtTop()) {
                // compose 到顶了，但是scrollview没到顶
                applyScrollViewOffsetDelta(-contentOffset)
                offsetDirty = false
            } else if (isAtTop() && realContentSize == null && lastItemVisible() && scrollView?.isDragging != true) {
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
