/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyGridState
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.views.applyOffsetDelta
import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.gestures.KuiklyScrollableState
import com.tencent.kuikly.core.views.ScrollParams

/**
 * 获取ScrollableState对应的KuiklyScrollInfo实例
 */
internal val ScrollableState.kuiklyInfo: KuiklyScrollInfo
    get() = when (this) {
        is LazyListState -> scrollableState.kuiklyInfo
        is PagerState -> scrollableState.kuiklyInfo
        is LazyGridState -> scrollableState.kuiklyInfo
        is LazyStaggeredGridState -> scrollableState.kuiklyInfo
        is ScrollState -> scrollableState.kuiklyInfo
        is KuiklyScrollableState -> kuiklyInfo
        else -> KuiklyScrollInfo()
    }

/**
 * 处理滚动事件
 * @param delta 滚动偏移量
 * @return 实际消耗的偏移量
 */
internal fun ScrollableState.kuiklyOnScroll(delta: Float): Float = when (this) {
    is LazyListState -> scrollableState.kuiklyOnScroll(delta)
    is PagerState -> scrollableState.kuiklyOnScroll(delta)
    is LazyGridState -> scrollableState.kuiklyOnScroll(delta)
    is LazyStaggeredGridState -> scrollableState.kuiklyOnScroll(delta)
    is ScrollState -> scrollableState.kuiklyOnScroll(delta)
    is KuiklyScrollableState -> kuiklyOnScroll(delta)
    else -> dispatchRawDelta(delta)
}

/**
 * 处理滚动结束事件
 */
internal fun ScrollableState.kuiklyOnScrollEnd(params: ScrollParams) {
    when (this) {
        is LazyListState -> scrollableState.kuiklyOnScrollEnd(params)
        is PagerState -> scrollableState.kuiklyOnScrollEnd(params)
        is LazyGridState -> scrollableState.kuiklyOnScrollEnd(params)
        is LazyStaggeredGridState -> scrollableState.kuiklyOnScrollEnd(params)
        is ScrollState -> scrollableState.kuiklyOnScrollEnd(params)
        is KuiklyScrollableState -> kuiklyOnScrollEnd(params)
        else -> { /* 无需处理 */ }
    }
}

/**
 * 检查是否在顶部
 */
internal fun ScrollableState.isAtTop(): Boolean = when(this) {
    is LazyListState -> firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    is PagerState -> firstVisiblePage == 0 && firstVisiblePageOffset == 0
    is LazyGridState -> firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    is LazyStaggeredGridState -> firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
    is ScrollState -> value == 0
    else -> false
}

/**
 * 检查最后一个index是否可见
 */
internal fun ScrollableState.lastItemVisible(): Boolean = when(this) {
    is LazyListState -> layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    is PagerState -> currentPage == pageCount - 1
    is LazyGridState -> layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    is LazyStaggeredGridState -> layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
    is ScrollState -> value >= maxValue
    else -> false
}

/**
 * 检查偏移量是否有效
 */
internal fun ScrollableState.isValidOffsetDelta(delta: Int): Boolean {
    if (kuiklyInfo.scrollView?.renderView == null || delta == 0) return false
    val newOffset = kuiklyInfo.contentOffset + delta
    return newOffset >= 0 && newOffset <= (kuiklyInfo.currentContentSize - kuiklyInfo.viewportSize)
}

/**
 * 应用滚动视图偏移量
 */
internal fun ScrollableState.applyScrollViewOffsetDelta(delta: Int) {
    if (kuiklyInfo.scrollView == null || delta == 0) return

    val newOffset = kuiklyInfo.scrollView!!.applyOffsetDelta(delta, kuiklyInfo)
    kuiklyInfo.composeOffset = if (kuiklyInfo.orientation == Orientation.Vertical) {
        newOffset.y.toFloat()
    } else {
        newOffset.x.toFloat()
    }
} 