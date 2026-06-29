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
import com.tencent.kuikly.compose.foundation.checkScrollableContainerConstraints
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.calculateEndPadding
import com.tencent.kuikly.compose.foundation.layout.calculateStartPadding
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutItemProvider
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import com.tencent.kuikly.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.constrainHeight
import com.tencent.kuikly.compose.ui.unit.constrainWidth
import com.tencent.kuikly.compose.ui.unit.offset
import kotlinx.coroutines.CoroutineScope

/**
 * Measure policy for DynamicPager. Forked from [rememberPagerMeasurePolicy]
 * with per-page variable size support via [DrawerSizeTracker].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberDrawerMeasurePolicy(
    itemProviderLambda: () -> LazyLayoutItemProvider,
    state: DrawerInternalPagerState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    beyondViewportPageCount: Int,
    pageSpacing: Dp,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    snapPosition: SnapPosition,
    coroutineScope: CoroutineScope,
    pageCount: () -> Int,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    state,
    contentPadding,
    reverseLayout,
    orientation,
    horizontalAlignment,
    verticalAlignment,
    pageSpacing,
    snapPosition,
    pageCount,
    beyondViewportPageCount,
    coroutineScope
) {
    { containerConstraints ->
        state.measurementScopeInvalidator.attachToScope()
        val isVertical = orientation == Orientation.Vertical
        checkScrollableContainerConstraints(
            containerConstraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )

        // Resolve content paddings
        val startPadding = if (isVertical) {
            contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
        } else {
            contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        }
        val endPadding = if (isVertical) {
            contentPadding.calculateRightPadding(layoutDirection).roundToPx()
        } else {
            contentPadding.calculateEndPadding(layoutDirection).roundToPx()
        }
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
        val totalVerticalPadding = topPadding + bottomPadding
        val totalHorizontalPadding = startPadding + endPadding
        val totalMainAxisPadding = if (isVertical) totalVerticalPadding else totalHorizontalPadding
        val beforeContentPadding = when {
            isVertical && !reverseLayout -> topPadding
            isVertical && reverseLayout -> bottomPadding
            !isVertical && !reverseLayout -> startPadding
            else -> endPadding
        }
        val afterContentPadding = totalMainAxisPadding - beforeContentPadding
        val contentConstraints =
            containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

        state.density = this

        val spaceBetweenPages = pageSpacing.roundToPx()

        val mainAxisAvailableSize = if (isVertical) {
            containerConstraints.maxHeight - totalVerticalPadding
        } else {
            containerConstraints.maxWidth - totalHorizontalPadding
        }
        val visualItemOffset = if (!reverseLayout || mainAxisAvailableSize > 0) {
            IntOffset(startPadding, topPadding)
        } else {
            IntOffset(
                if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                if (isVertical) topPadding + mainAxisAvailableSize else topPadding
            )
        }

        // Update tracker state
        state.mainAxisAvailableSpace = mainAxisAvailableSize
        state.spaceBetweenPages = spaceBetweenPages
        state.sizeTracker.ensureComputed(this, pageCount(), mainAxisAvailableSize, spaceBetweenPages)

        // Use current page's size for premeasure constraints
        val currentPageSize = state.sizeTracker.getPageSize(
            state.currentPage.coerceIn(0, (pageCount() - 1).coerceAtLeast(0))
        ).coerceAtLeast(0)

        state.premeasureConstraints = Constraints(
            maxWidth = if (orientation == Orientation.Vertical) contentConstraints.maxWidth else currentPageSize,
            maxHeight = if (orientation != Orientation.Vertical) contentConstraints.maxHeight else currentPageSize
        )

        val itemProvider = itemProviderLambda()

        val currentPage: Int
        val currentPageOffset: Int

        Snapshot.withoutReadObservation {
            state.relocateSnapTargetByKey(itemProvider)
            currentPage = state.matchScrollPositionWithKey(itemProvider, state.currentPage)

            // Use the current page's actual size for offset calculation
            val currentPageAvailableSize = state.sizeTracker.getPageSize(
                state.currentPage.coerceIn(0, (pageCount() - 1).coerceAtLeast(0))
            ).coerceAtLeast(0)

            currentPageOffset = snapPosition.currentPageOffset(
                mainAxisAvailableSize,
                currentPageAvailableSize,
                spaceBetweenPages,
                beforeContentPadding,
                afterContentPadding,
                state.currentPage,
                state.currentPageOffsetFraction,
                state.pageCount
            )
        }

        val pinnedPages = itemProvider.calculateLazyLayoutPinnedIndices(
            pinnedItemList = state.pinnedPages,
            beyondBoundsInfo = state.beyondBoundsInfo
        )

        val measureResult = measureDrawerPager(
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            constraints = contentConstraints,
            pageCount = pageCount(),
            spaceBetweenPages = spaceBetweenPages,
            mainAxisAvailableSize = mainAxisAvailableSize,
            visualPageOffset = visualItemOffset,
            sizeTracker = state.sizeTracker,
            beyondViewportPageCount = beyondViewportPageCount,
            orientation = orientation,
            currentPage = currentPage,
            currentPageOffset = currentPageOffset,
            horizontalAlignment = horizontalAlignment,
            verticalAlignment = verticalAlignment,
            pagerItemProvider = itemProvider,
            reverseLayout = reverseLayout,
            pinnedPages = pinnedPages,
            snapPosition = snapPosition,
            placementScopeInvalidator = state.placementScopeInvalidator,
            coroutineScope = coroutineScope,
            layout = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width + totalHorizontalPadding),
                    containerConstraints.constrainHeight(height + totalVerticalPadding),
                    emptyMap(),
                    placement
                )
            }
        )
        state.applyMeasureResult(measureResult)
        measureResult
    }
}
