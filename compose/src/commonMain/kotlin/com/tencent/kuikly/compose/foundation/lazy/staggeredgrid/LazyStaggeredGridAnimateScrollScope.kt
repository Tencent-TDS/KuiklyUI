/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.foundation.lazy.staggeredgrid

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollScope
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import com.tencent.kuikly.compose.ui.util.fastFirstOrNull
import com.tencent.kuikly.compose.ui.util.fastSumBy

@ExperimentalFoundationApi
internal class LazyStaggeredGridAnimateScrollScope(
    private val state: LazyStaggeredGridState
) : LazyLayoutAnimateScrollScope {

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        with(state) {
            snapToItemInternal(index, scrollOffset, forceRemeasure = true)
        }
    }

    override fun calculateDistanceTo(targetIndex: Int): Float {
        val layoutInfo = state.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) return 0f
        val visibleItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
        return if (visibleItem == null) {
            val averageMainAxisItemSize = calculateVisibleItemsAverageSize(layoutInfo)

            val laneCount = state.laneCount
            val lineDiff = targetIndex / laneCount - firstVisibleItemIndex / laneCount
            averageMainAxisItemSize * lineDiff.toFloat() - firstVisibleItemScrollOffset
        } else {
            if (layoutInfo.orientation == Orientation.Vertical) {
                visibleItem.offset.y
            } else {
                visibleItem.offset.x
            }.toFloat()
        }
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }

    private fun calculateVisibleItemsAverageSize(layoutInfo: LazyStaggeredGridLayoutInfo): Int {
        val visibleItems = layoutInfo.visibleItemsInfo
        val itemSizeSum = visibleItems.fastSumBy {
            if (layoutInfo.orientation == Orientation.Vertical) {
                it.size.height
            } else {
                it.size.width
            }
        }
        return itemSizeSum / visibleItems.size + layoutInfo.mainAxisItemSpacing
    }
}
