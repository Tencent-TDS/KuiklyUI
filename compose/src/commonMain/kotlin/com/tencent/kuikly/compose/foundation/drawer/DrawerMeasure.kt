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

package com.tencent.kuikly.compose.foundation.drawer

import com.tencent.kuikly.compose.foundation.pager.*

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import com.tencent.kuikly.compose.foundation.layout.Arrangement.Absolute.spacedBy
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutItemProvider
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import com.tencent.kuikly.compose.foundation.lazy.layout.ObservableScopeInvalidator
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.Placeable
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.constrainHeight
import com.tencent.kuikly.compose.ui.unit.constrainWidth
import com.tencent.kuikly.compose.ui.util.fastFilter
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.compose.ui.util.fastMaxBy
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope

/**
 * Measurement function for DynamicPager. Forked from [measurePager] with per-page variable
 * size support via [DrawerSizeTracker].
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutMeasureScope.measureDrawerPager(
    pageCount: Int,
    pagerItemProvider: LazyLayoutItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    spaceBetweenPages: Int,
    currentPage: Int,
    currentPageOffset: Int,
    constraints: Constraints,
    orientation: Orientation,
    verticalAlignment: Alignment.Vertical?,
    horizontalAlignment: Alignment.Horizontal?,
    reverseLayout: Boolean,
    visualPageOffset: IntOffset,
    sizeTracker: DrawerSizeTracker,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    snapPosition: SnapPosition,
    placementScopeInvalidator: ObservableScopeInvalidator,
    coroutineScope: CoroutineScope,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): PagerMeasureResult {

    // Use current page's size as the "nominal" page size for the result
    val currentPageAvailableSize = sizeTracker.getPageSize(currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))

    return if (pageCount <= 0) {
        PagerMeasureResult(
            visiblePagesInfo = emptyList(),
            pageSize = currentPageAvailableSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            orientation = orientation,
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            measureResult = layout(constraints.minWidth, constraints.minHeight) {},
            positionedPages = emptyList(),
            firstVisiblePage = null,
            firstVisiblePageScrollOffset = 0,
            reverseLayout = false,
            beyondViewportPageCount = beyondViewportPageCount,
            canScrollForward = false,
            currentPage = null,
            currentPageOffsetFraction = 0.0f,
            snapPosition = snapPosition,
            remeasureNeeded = false,
            coroutineScope = coroutineScope
        )
    } else {
        var firstVisiblePage = currentPage
        var firstVisiblePageOffset = currentPageOffset

        // Walk backward to find the first visible page
        while (firstVisiblePage > 0 && firstVisiblePageOffset > 0) {
            firstVisiblePage--
            firstVisiblePageOffset -= sizeTracker.getPageSizeWithSpacing(firstVisiblePage)
        }

        val firstVisiblePageScrollOffset = firstVisiblePageOffset * -1

        var currentFirstPage = firstVisiblePage
        var currentFirstPageScrollOffset = firstVisiblePageScrollOffset
        if (currentFirstPage >= pageCount) {
            currentFirstPage = pageCount - 1
            currentFirstPageScrollOffset = 0
        }

        val visiblePages = ArrayDeque<MeasuredPage>()

        val minOffset = -beforeContentPadding + if (spaceBetweenPages < 0) spaceBetweenPages else 0
        val maxOffset = mainAxisAvailableSize

        currentFirstPageScrollOffset += minOffset

        var maxCrossAxis = 0

        // Compose backward
        while (currentFirstPageScrollOffset < 0 && currentFirstPage > 0) {
            val previous = currentFirstPage - 1
            val measuredPage = getAndMeasureDrawer(
                index = previous,
                constraints = constraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                sizeTracker = sizeTracker
            )
            visiblePages.add(0, measuredPage)
            maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
            currentFirstPageScrollOffset += sizeTracker.getPageSizeWithSpacing(previous)
            currentFirstPage = previous
        }

        if (currentFirstPageScrollOffset < minOffset) {
            currentFirstPageScrollOffset = minOffset
        }

        currentFirstPageScrollOffset -= minOffset

        var index = currentFirstPage
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstPageScrollOffset
        var remeasureNeeded = false
        var indexInVisibleItems = 0

        // Skip already composed backward pages
        while (indexInVisibleItems < visiblePages.size) {
            if (currentMainAxisOffset >= maxMainAxis) {
                visiblePages.removeAt(indexInVisibleItems)
                remeasureNeeded = true
            } else {
                currentMainAxisOffset += sizeTracker.getPageSizeWithSpacing(
                    visiblePages[indexInVisibleItems].index
                )
                index++
                indexInVisibleItems++
            }
        }

        // Compose forward
        while (index < pageCount &&
            (currentMainAxisOffset < maxMainAxis ||
                currentMainAxisOffset <= 0 ||
                visiblePages.isEmpty())
        ) {
            val measuredPage = getAndMeasureDrawer(
                index = index,
                constraints = constraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                sizeTracker = sizeTracker
            )

            val thisPageSize = sizeTracker.getPageSize(index)
            val thisPageSizeWithSpacing = sizeTracker.getPageSizeWithSpacing(index)

            currentMainAxisOffset += if (index == pageCount - 1) thisPageSize else thisPageSizeWithSpacing

            if (currentMainAxisOffset <= minOffset && index != pageCount - 1) {
                currentFirstPage = index + 1
                currentFirstPageScrollOffset -= thisPageSizeWithSpacing
                remeasureNeeded = true
            } else {
                maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
                visiblePages.add(measuredPage)
            }
            index++
        }

        // Scroll back if didn't fill viewport
        if (currentMainAxisOffset < maxOffset) {
            val toScrollBack = maxOffset - currentMainAxisOffset
            currentFirstPageScrollOffset -= toScrollBack
            currentMainAxisOffset += toScrollBack
            while (currentFirstPageScrollOffset < beforeContentPadding && currentFirstPage > 0) {
                val previousIndex = currentFirstPage - 1
                val measuredPage = getAndMeasureDrawer(
                    index = previousIndex,
                    constraints = constraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    sizeTracker = sizeTracker
                )
                visiblePages.add(0, measuredPage)
                maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
                currentFirstPageScrollOffset += sizeTracker.getPageSizeWithSpacing(previousIndex)
                currentFirstPage = previousIndex
            }

            if (currentFirstPageScrollOffset < 0) {
                currentMainAxisOffset += currentFirstPageScrollOffset
                currentFirstPageScrollOffset = 0
            }
        }

        require(currentFirstPageScrollOffset >= 0) { "invalid currentFirstPageScrollOffset" }
        val visiblePagesScrollOffset = -currentFirstPageScrollOffset

        var firstPage = visiblePages.first()

        // Skip pages in before content padding for state calculation
        if (beforeContentPadding > 0 || spaceBetweenPages < 0) {
            for (i in visiblePages.indices) {
                val size = sizeTracker.getPageSizeWithSpacing(visiblePages[i].index)
                if (currentFirstPageScrollOffset != 0 && size <= currentFirstPageScrollOffset &&
                    i != visiblePages.lastIndex
                ) {
                    currentFirstPageScrollOffset -= size
                    firstPage = visiblePages[i + 1]
                } else {
                    break
                }
            }
        }

        // Compose extra pages
        val extraPagesBefore = createDrawerPagesBeforeList(
            currentFirstPage, beyondViewportPageCount, pinnedPages
        ) {
            getAndMeasureDrawer(
                index = it, constraints = constraints, pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset, orientation = orientation,
                horizontalAlignment = horizontalAlignment, verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection, reverseLayout = reverseLayout,
                sizeTracker = sizeTracker
            )
        }
        extraPagesBefore.fastForEach { maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize) }

        val extraPagesAfter = createDrawerPagesAfterList(
            visiblePages.last().index, pageCount, beyondViewportPageCount, pinnedPages
        ) {
            getAndMeasureDrawer(
                index = it, constraints = constraints, pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset, orientation = orientation,
                horizontalAlignment = horizontalAlignment, verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection, reverseLayout = reverseLayout,
                sizeTracker = sizeTracker
            )
        }
        extraPagesAfter.fastForEach { maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize) }

        val noExtraPages = firstPage == visiblePages.first() &&
            extraPagesBefore.isEmpty() && extraPagesAfter.isEmpty()

        val layoutWidth = constraints.constrainWidth(
            if (orientation == Orientation.Vertical) maxCrossAxis else currentMainAxisOffset
        )
        val layoutHeight = constraints.constrainHeight(
            if (orientation == Orientation.Vertical) currentMainAxisOffset else maxCrossAxis
        )

        val positionedPages = calculateDrawerPagesOffsets(
            pages = visiblePages,
            extraPagesBefore = extraPagesBefore,
            extraPagesAfter = extraPagesAfter,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            finalMainAxisOffset = currentMainAxisOffset,
            maxOffset = maxOffset,
            pagesScrollOffset = visiblePagesScrollOffset,
            orientation = orientation,
            reverseLayout = reverseLayout,
            density = this,
            sizeTracker = sizeTracker,
            spaceBetweenPages = spaceBetweenPages
        )

        val visiblePagesInfo = if (noExtraPages) positionedPages else positionedPages.fastFilter {
            it.index >= visiblePages.first().index && it.index <= visiblePages.last().index
        }

        val positionedPagesBefore =
            if (extraPagesBefore.isEmpty()) emptyList() else positionedPages.fastFilter {
                it.index < visiblePages.first().index
            }
        val positionedPagesAfter =
            if (extraPagesAfter.isEmpty()) emptyList() else positionedPages.fastFilter {
                it.index > visiblePages.last().index
            }

        // Find current page using per-page sizes
        val newCurrentPage = visiblePagesInfo.fastMaxBy {
            -abs(
                calculateDistanceToDesiredSnapPosition(
                    mainAxisViewPortSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    itemSize = it.size,  // PER-PAGE size
                    itemOffset = it.offset,
                    itemIndex = it.index,
                    snapPosition = snapPosition,
                    itemCount = pageCount
                )
            )
        }

        val newCurrentPageSize = newCurrentPage?.size ?: currentPageAvailableSize
        val newCurrentPageSizeWithSpacing = newCurrentPageSize + spaceBetweenPages

        val snapOffset = snapPosition.position(
            mainAxisAvailableSize,
            newCurrentPageSize,
            beforeContentPadding,
            afterContentPadding,
            newCurrentPage?.index ?: 0,
            pageCount
        )

        val currentPagePositionOffset = newCurrentPage?.offset ?: 0
        val currentPageOffsetFraction = if (newCurrentPageSizeWithSpacing == 0) {
            0.0f
        } else {
            ((snapOffset - currentPagePositionOffset) / newCurrentPageSizeWithSpacing.toFloat()).coerceIn(
                MinPageOffset, MaxPageOffset
            )
        }


        return PagerMeasureResult(
            firstVisiblePage = firstPage,
            firstVisiblePageScrollOffset = currentFirstPageScrollOffset,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedPages.fastForEach { it.place(this) }
                placementScopeInvalidator.attachToScope()
            },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = maxOffset + afterContentPadding,
            visiblePagesInfo = visiblePagesInfo,
            positionedPages = positionedPages,
            reverseLayout = reverseLayout,
            orientation = orientation,
            pageSize = newCurrentPageSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            beyondViewportPageCount = beyondViewportPageCount,
            canScrollForward = index < pageCount || currentMainAxisOffset > maxOffset,
            currentPage = newCurrentPage,
            currentPageOffsetFraction = currentPageOffsetFraction,
            snapPosition = snapPosition,
            remeasureNeeded = remeasureNeeded,
            extraPagesBefore = positionedPagesBefore,
            extraPagesAfter = positionedPagesAfter,
            coroutineScope = coroutineScope
        )
    }
}

// --- Per-page measure ---

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.getAndMeasureDrawer(
    index: Int,
    constraints: Constraints,
    pagerItemProvider: LazyLayoutItemProvider,
    visualPageOffset: IntOffset,
    orientation: Orientation,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    layoutDirection: LayoutDirection,
    reverseLayout: Boolean,
    sizeTracker: DrawerSizeTracker
): MeasuredPage {
    val pageSize = sizeTracker.getPageSize(index)
    val childConstraints = Constraints(
        maxWidth = if (orientation == Orientation.Vertical) constraints.maxWidth else pageSize,
        maxHeight = if (orientation != Orientation.Vertical) constraints.maxHeight else pageSize
    )
    val key = pagerItemProvider.getKey(index)
    val placeable = measure(index, childConstraints)

    return MeasuredPage(
        index = index,
        placeables = placeable,
        visualOffset = visualPageOffset,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        layoutDirection = layoutDirection,
        reverseLayout = reverseLayout,
        size = pageSize,
        orientation = orientation,
        key = key
    )
}

// --- Page offset positioning with variable sizes ---

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.calculateDrawerPagesOffsets(
    pages: List<MeasuredPage>,
    extraPagesBefore: List<MeasuredPage>,
    extraPagesAfter: List<MeasuredPage>,
    layoutWidth: Int,
    layoutHeight: Int,
    finalMainAxisOffset: Int,
    maxOffset: Int,
    pagesScrollOffset: Int,
    orientation: Orientation,
    reverseLayout: Boolean,
    density: Density,
    sizeTracker: DrawerSizeTracker,
    spaceBetweenPages: Int
): MutableList<MeasuredPage> {
    val mainAxisLayoutSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < minOf(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        check(pagesScrollOffset == 0) { "non-zero pagesScrollOffset=$pagesScrollOffset" }
    }
    val positionedPages =
        ArrayList<MeasuredPage>(pages.size + extraPagesBefore.size + extraPagesAfter.size)

    if (hasSpareSpace) {
        require(extraPagesBefore.isEmpty() && extraPagesAfter.isEmpty()) { "No extra pages" }

        val pagesCount = pages.size
        fun Int.reverseAware() = if (!reverseLayout) this else pagesCount - this - 1

        val sizes = IntArray(pagesCount) { pages[it].size }
        val offsets = IntArray(pagesCount) { 0 }

        val arrangement = spacedBy(spaceBetweenPages.toDp())
        if (orientation == Orientation.Vertical) {
            with(arrangement) { density.arrange(mainAxisLayoutSize, sizes, offsets) }
        } else {
            with(arrangement) {
                density.arrange(mainAxisLayoutSize, sizes, LayoutDirection.Ltr, offsets)
            }
        }

        val reverseAwareOffsetIndices =
            if (!reverseLayout) offsets.indices else offsets.indices.reversed()
        for (index in reverseAwareOffsetIndices) {
            val absoluteOffset = offsets[index]
            val page = pages[index.reverseAware()]
            val relativeOffset = if (reverseLayout) {
                mainAxisLayoutSize - absoluteOffset - page.size
            } else {
                absoluteOffset
            }
            page.position(relativeOffset, layoutWidth, layoutHeight)
            positionedPages.add(page)
        }
    } else {
        var currentMainAxis = pagesScrollOffset

        // Extra pages before - walk backward using each page's own size
        extraPagesBefore.fastForEach {
            currentMainAxis -= it.size + spaceBetweenPages
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
        }

        currentMainAxis = pagesScrollOffset
        pages.fastForEach {
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
            currentMainAxis += it.size + spaceBetweenPages
        }

        extraPagesAfter.fastForEach {
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
            currentMainAxis += it.size + spaceBetweenPages
        }
    }
    return positionedPages
}

// --- Helper functions ---

private fun createDrawerPagesAfterList(
    currentLastPage: Int,
    pagesCount: Int,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null
    val end = minOf(currentLastPage + beyondViewportPageCount, pagesCount - 1)
    for (i in currentLastPage + 1..end) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }
    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex in (end + 1) until pagesCount) {
            if (list == null) list = mutableListOf()
            list?.add(getAndMeasure(pageIndex))
        }
    }
    return list ?: emptyList()
}

private fun createDrawerPagesBeforeList(
    currentFirstPage: Int,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null
    val start = maxOf(0, currentFirstPage - beyondViewportPageCount)
    for (i in currentFirstPage - 1 downTo start) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }
    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex < start) {
            if (list == null) list = mutableListOf()
            list?.add(getAndMeasure(pageIndex))
        }
    }
    return list ?: emptyList()
}
