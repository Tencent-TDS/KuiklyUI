/*
 * Copyright 2023 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.pager

import androidx.annotation.FloatRange
import com.tencent.kuikly.compose.animation.core.AnimationSpec
import com.tencent.kuikly.compose.animation.core.DecayAnimationSpec
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.VisibilityThreshold
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.foundation.gestures.FlingBehavior
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.semantics.pageDown
import com.tencent.kuikly.compose.ui.semantics.pageLeft
import com.tencent.kuikly.compose.ui.semantics.pageRight
import com.tencent.kuikly.compose.ui.semantics.pageUp
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.Velocity
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A Pager that scrolls horizontally. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [beyondViewportPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [snapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the samples to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleHorizontalPagerSample
 * @sample androidx.compose.foundation.samples.HorizontalPagerWithScrollableContent
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be applied to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones. This does not include the pages automatically composed and laid out by the pre-fetcher in
 * the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param verticalAlignment How pages are aligned vertically in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one. If null
 * is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager]
 * behaves with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of pages.
 * Use this to provide different settling to different positions in the layout. This is used by
 * [Pager] as a way to calculate [PagerState.currentPage], currentPage is the page closest
 * to the snap position in the layout (e.g. if the snap position is the start of the layout, then
 * currentPage will be the page closest to that).
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun HorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
//    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
//    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
//    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
//        state,
//        Orientation.Horizontal
//    ),
//    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        horizontalAlignment = Alignment.CenterHorizontally,
//        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
//        reverseLayout = reverseLayout,
        key = key,
//        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = SnapPosition.Start,
        pageContent = pageContent
    )
}

/**
 * A Pager that scrolls vertically. Pages are lazily placed in accordance to the available
 * viewport size. By definition, pages in a [Pager] have the same size, defined by [pageSize] and
 * use a snap animation (provided by [flingBehavior] to scroll pages into a specific position). You
 * can use [beyondViewportPageCount] to place more pages before and after the visible pages.
 *
 * If you need snapping with pages of different size, you can use a [snapFlingBehavior] with a
 * [SnapLayoutInfoProvider] adapted to a LazyList.
 * @see androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider for the implementation
 * of a [SnapLayoutInfoProvider] that uses [androidx.compose.foundation.lazy.LazyListState].
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.SimpleVerticalPagerSample
 *
 * @param state The state to control this pager
 * @param modifier A modifier instance to be apply to this Pager outer layout
 * @param contentPadding a padding around the whole content. This will add padding for the
 * content after it has been clipped, which is not possible via [modifier] param. You can use it
 * to add a padding before the first page or after the last one. Use [pageSpacing] to add spacing
 * between the pages.
 * @param pageSize Use this to change how the pages will look like inside this pager.
 * @param beyondViewportPageCount Pages to compose and layout before and after the list of visible
 * pages. Note: Be aware that using a large value for [beyondViewportPageCount] will cause a lot of
 * pages to be composed, measured and placed which will defeat the purpose of using lazy loading.
 * This should be used as an optimization to pre-load a couple of pages before and after the visible
 * ones. This does not include the pages automatically composed and laid out by the pre-fetcher in
 *  * the direction of the scroll during scroll events.
 * @param pageSpacing The amount of space to be used to separate the pages in this Pager
 * @param horizontalAlignment How pages are aligned horizontally in this Pager.
 * @param flingBehavior The [TargetedFlingBehavior] to be used for post scroll gestures.
 * @param userScrollEnabled whether the scrolling via the user gestures or accessibility actions
 * is allowed. You can still scroll programmatically using [PagerState.scroll] even when it is
 * disabled.
 * @param reverseLayout reverse the direction of scrolling and layout.
 * @param key a stable and unique key representing the item. When you specify the key the scroll
 * position will be maintained based on the key, which means if you add/remove items before the
 * current visible item the item with the given key will be kept as the first visible one. If null
 * is passed the position in the list will represent the key.
 * @param pageNestedScrollConnection A [NestedScrollConnection] that dictates how this [Pager] behaves
 * with nested lists. The default behavior will see [Pager] to consume all nested deltas.
 * @param snapPosition The calculation of how this Pager will perform snapping of Pages.
 * Use this to provide different settling to different positions in the layout. This is used by
 * [Pager] as a way to calculate [PagerState.currentPage], currentPage is the page closest
 * to the snap position in the layout (e.g. if the snap position is the start of the layout, then
 * currentPage will be the page closest to that).
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun VerticalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSize: PageSize = PageSize.Fill,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
//    flingBehavior: TargetedFlingBehavior = PagerDefaults.flingBehavior(state = state),
    userScrollEnabled: Boolean = true,
//    reverseLayout: Boolean = false,
    key: ((index: Int) -> Any)? = null,
//    pageNestedScrollConnection: NestedScrollConnection = PagerDefaults.pageNestedScrollConnection(
//        state,
//        Orientation.Vertical
//    ),
//    snapPosition: SnapPosition = SnapPosition.Start,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    Pager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        pageSize = pageSize,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Vertical,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = horizontalAlignment,
//        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
//        reverseLayout = reverseLayout,
        key = key,
//        pageNestedScrollConnection = pageNestedScrollConnection,
        snapPosition = SnapPosition.Start,
        pageContent = pageContent
    )
}

/**
 * Contains the default values used by [Pager].
 */
object PagerDefaults {

//    /**
//     * The default implementation of Pager's pageNestedScrollConnection.
//     *
//     * @param state state of the pager
//     * @param orientation The orientation of the pager. This will be used to determine which
//     * direction the nested scroll connection will operate and react on.
//     */
//    @Composable
//    fun pageNestedScrollConnection(
//        state: PagerState,
//        orientation: Orientation
//    ): NestedScrollConnection {
//        return remember(state, orientation) {
//            DefaultPagerNestedScrollConnection(state, orientation)
//        }
//    }

    /**
     * The default value of beyondViewportPageCount used to specify the number of pages to compose
     * and layout before and after the visible pages. It does not include the pages automatically
     * composed and laid out by the pre-fetcher in the direction of the scroll during scroll events.
     */
    const val BeyondViewportPageCount = 0
}

internal fun SnapPosition.currentPageOffset(
    layoutSize: Int,
    pageSize: Int,
    spaceBetweenPages: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    currentPage: Int,
    currentPageOffsetFraction: Float,
    pageCount: Int
): Int {
    val snapOffset = position(
        layoutSize,
        pageSize,
        beforeContentPadding,
        afterContentPadding,
        currentPage,
        pageCount
    )

    return (snapOffset - currentPageOffsetFraction * (pageSize + spaceBetweenPages)).roundToInt()
}

//private class DefaultPagerNestedScrollConnection(
//    val state: PagerState,
//    val orientation: Orientation
//) : NestedScrollConnection {
//
//    fun Velocity.consumeOnOrientation(orientation: Orientation): Velocity {
//        return if (orientation == Orientation.Vertical) {
//            copy(x = 0f)
//        } else {
//            copy(y = 0f)
//        }
//    }
//
//    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
//        return if (
//        // rounding error and drag only
//            source == NestedScrollSource.UserInput && abs(state.currentPageOffsetFraction) > 1e-6
//        ) {
//            // find the current and next page (in the direction of dragging)
//            val currentPageOffset = state.currentPageOffsetFraction * state.pageSize
//            val pageAvailableSpace = state.layoutInfo.pageSize + state.layoutInfo.pageSpacing
//            val nextClosestPageOffset =
//                currentPageOffset + pageAvailableSpace * -sign(state.currentPageOffsetFraction)
//
//            val minBound: Float
//            val maxBound: Float
//            // build min and max bounds in absolute coordinates for nested scroll
//            if (state.currentPageOffsetFraction > 0f) {
//                minBound = nextClosestPageOffset
//                maxBound = currentPageOffset
//            } else {
//                minBound = currentPageOffset
//                maxBound = nextClosestPageOffset
//            }
//
//            val delta = if (orientation == Orientation.Horizontal) available.x else available.y
//            val coerced = delta.coerceIn(minBound, maxBound)
//            // dispatch and return reversed as usual
//            val consumed = -state.dispatchRawDelta(-coerced)
//            available.copy(
//                x = if (orientation == Orientation.Horizontal) consumed else available.x,
//                y = if (orientation == Orientation.Vertical) consumed else available.y,
//            )
//        } else {
//            Offset.Zero
//        }
//    }
//
//    override fun onPostScroll(
//        consumed: Offset,
//        available: Offset,
//        source: NestedScrollSource
//    ): Offset {
//        if (source == NestedScrollSource.SideEffect && available.mainAxis() != 0f) {
//            throw CancellationException("End of scrollable area reached")
//        }
//        return Offset.Zero
//    }
//
//    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
//        return available.consumeOnOrientation(orientation)
//    }
//
//    private fun Offset.mainAxis(): Float =
//        if (orientation == Orientation.Horizontal) this.x else this.y
//}

internal fun Modifier.pagerSemantics(
    state: PagerState,
    isVertical: Boolean,
    scope: CoroutineScope,
    userScrollEnabled: Boolean
): Modifier {
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch {
                state.animateToNextPage()
            }
            true
        } else {
            false
        }
    }

    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch {
                state.animateToPreviousPage()
            }
            true
        } else {
            false
        }
    }

    return if (userScrollEnabled) {
        this.then(Modifier.semantics {
            if (isVertical) {
                pageUp { performBackwardPaging() }
                pageDown { performForwardPaging() }
            } else {
                pageLeft { performBackwardPaging() }
                pageRight { performForwardPaging() }
            }
        })
    } else {
        this then Modifier
    }
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.MainPagerComposable) {
        println("Pager: ${generateMsg()}")
    }
}

internal object PagerDebugConfig {
    const val MainPagerComposable = false
    const val PagerState = false
    const val MeasureLogic = false
    const val ScrollPosition = false
    const val PagerSnapDistance = false
    const val PagerSnapLayoutInfoProvider = false
}
