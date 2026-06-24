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

@file:OptIn(ExperimentalFoundationApi::class)

package com.tencent.kuikly.compose.foundation.drawer

import com.tencent.kuikly.compose.foundation.pager.*

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayout
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsState
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutItemProvider
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import com.tencent.kuikly.compose.foundation.lazy.layout.MutableIntervalList
import com.tencent.kuikly.compose.foundation.lazy.layout.NearestRangeKeyIndexMap
import com.tencent.kuikly.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputChange
import com.tencent.kuikly.compose.ui.input.pointer.changedToUp
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.semantics.pageDown
import com.tencent.kuikly.compose.ui.semantics.pageLeft
import com.tencent.kuikly.compose.ui.semantics.pageRight
import com.tencent.kuikly.compose.ui.semantics.pageUp
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.util.fastAll
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * A Pager that scrolls horizontally with per-page variable sizes.
 * Unlike [HorizontalPager], each page can have a different main-axis size,
 * controlled by the [DrawerInternalPagerState.sizeTracker].
 *
 * @param state The [DrawerInternalPagerState] to control this pager. Create with [rememberDrawerInternalPagerState].
 * @param modifier A modifier instance to be applied to this Pager outer layout.
 * @param contentPadding Padding around the whole content.
 * @param beyondViewportPageCount Pages to compose and layout before and after the visible pages.
 * @param pageSpacing The amount of space between pages.
 * @param verticalAlignment How pages are aligned vertically.
 * @param userScrollEnabled Whether scrolling via user gestures is allowed.
 * @param key A stable and unique key representing the page.
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun DrawerHorizontalPager(
    state: DrawerInternalPagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    userScrollEnabled: Boolean = true,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    DrawerInternalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Horizontal,
        verticalAlignment = verticalAlignment,
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = userScrollEnabled,
        key = key,
        snapPosition = SnapPosition.Start,
        pageContent = pageContent
    )
}

/**
 * A Pager that scrolls vertically with per-page variable sizes.
 * Unlike [VerticalPager], each page can have a different main-axis size,
 * controlled by the [DrawerInternalPagerState.sizeTracker].
 *
 * @param state The [DrawerInternalPagerState] to control this pager. Create with [rememberDrawerInternalPagerState].
 * @param modifier A modifier instance to be applied to this Pager outer layout.
 * @param contentPadding Padding around the whole content.
 * @param beyondViewportPageCount Pages to compose and layout before and after the visible pages.
 * @param pageSpacing The amount of space between pages.
 * @param horizontalAlignment How pages are aligned horizontally.
 * @param userScrollEnabled Whether scrolling via user gestures is allowed.
 * @param key A stable and unique key representing the page.
 * @param pageContent This Pager's page Composable.
 */
@Composable
fun DrawerVerticalPager(
    state: DrawerInternalPagerState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    userScrollEnabled: Boolean = true,
    key: ((index: Int) -> Any)? = null,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    DrawerInternalPager(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        orientation = Orientation.Vertical,
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        key = key,
        snapPosition = SnapPosition.Start,
        pageContent = pageContent
    )
}

// --- Internal DynamicPager composable ---

@Composable
internal fun DrawerInternalPager(
    modifier: Modifier,
    state: DrawerInternalPagerState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean = false,
    orientation: Orientation,
    userScrollEnabled: Boolean,
    beyondViewportPageCount: Int = PagerDefaults.BeyondViewportPageCount,
    pageSpacing: Dp = 0.dp,
    key: ((index: Int) -> Any)?,
    horizontalAlignment: Alignment.Horizontal,
    verticalAlignment: Alignment.Vertical,
    snapPosition: SnapPosition,
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    require(beyondViewportPageCount >= 0) {
        "beyondViewportPageCount should be greater than or equal to 0, you selected $beyondViewportPageCount"
    }

    state.contentPadding = contentPadding
    val pagerItemProvider = rememberDrawerItemProviderLambda(
        state = state,
        pageContent = pageContent,
        key = key
    ) { state.pageCount }

    val coroutineScope = rememberCoroutineScope()
    state.kuiklyInfo.scope = coroutineScope


    val measurePolicy = rememberDrawerMeasurePolicy(
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        orientation = orientation,
        beyondViewportPageCount = beyondViewportPageCount,
        pageSpacing = pageSpacing,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        itemProviderLambda = pagerItemProvider,
        snapPosition = snapPosition,
        coroutineScope = coroutineScope,
        pageCount = { state.pageCount }
    )

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .drawerPagerSemantics(state, orientation == Orientation.Vertical, coroutineScope, userScrollEnabled)
            .lazyLayoutBeyondBoundsModifier(
                state = rememberDrawerBeyondBoundsState(
                    state = state,
                    beyondViewportPageCount = beyondViewportPageCount
                ),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                layoutDirection = LocalLayoutDirection.current,
                orientation = orientation,
                enabled = userScrollEnabled
            )
            .drawerDragDirectionDetector(state),
        measurePolicy = measurePolicy,
        orientation = orientation,
        scrollableState = state,
        userScrollEnabled = userScrollEnabled,
        itemProvider = pagerItemProvider
    )
}

// --- Semantics ---

private fun Modifier.drawerPagerSemantics(
    state: DrawerInternalPagerState,
    isVertical: Boolean,
    scope: CoroutineScope,
    userScrollEnabled: Boolean
): Modifier {
    fun performForwardPaging(): Boolean {
        return if (state.canScrollForward) {
            scope.launch { state.animateToNextPage() }
            true
        } else false
    }
    fun performBackwardPaging(): Boolean {
        return if (state.canScrollBackward) {
            scope.launch { state.animateToPreviousPage() }
            true
        } else false
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

// --- Drag direction detector ---

private fun Modifier.drawerDragDirectionDetector(state: DrawerInternalPagerState) =
    this then Modifier.pointerInput(state) {
        coroutineScope {
            awaitEachGesture {
                val downEvent = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var upEventOrCancellation: PointerInputChange? = null
                state.upDownDifference = Offset.Zero
                while (upEventOrCancellation == null) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.changes.fastAll { it.changedToUp() }) {
                        upEventOrCancellation = event.changes[0]
                    }
                }
                state.upDownDifference = upEventOrCancellation.position - downEvent.position
            }
        }
    }

// --- Beyond bounds state ---

@Composable
private fun rememberDrawerBeyondBoundsState(
    state: DrawerInternalPagerState,
    beyondViewportPageCount: Int
): LazyLayoutBeyondBoundsState {
    return remember(state, beyondViewportPageCount) {
        DrawerBeyondBoundsState(state, beyondViewportPageCount)
    }
}

private class DrawerBeyondBoundsState(
    private val state: DrawerInternalPagerState,
    private val beyondViewportPageCount: Int
) : LazyLayoutBeyondBoundsState {
    override fun remeasure() { state.remeasurement?.forceRemeasure() }
    override val itemCount: Int get() = state.pageCount
    override val hasVisibleItems: Boolean get() = state.layoutInfo.visiblePagesInfo.isNotEmpty()
    override val firstPlacedIndex: Int get() = maxOf(0, state.firstVisiblePage - beyondViewportPageCount)
    override val lastPlacedIndex: Int
        get() = minOf(
            itemCount - 1,
            state.layoutInfo.visiblePagesInfo.last().index + beyondViewportPageCount
        )
}

// --- Item provider ---

@Composable
private fun rememberDrawerItemProviderLambda(
    state: DrawerInternalPagerState,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
    key: ((index: Int) -> Any)?,
    pageCount: () -> Int
): () -> LazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(pageContent)
    val latestKey = rememberUpdatedState(key)
    return remember(state, latestContent, latestKey, pageCount) {
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            DrawerLayoutIntervalContent(latestContent.value, latestKey.value, pageCount())
        }
        val itemProviderState = derivedStateOf(referentialEqualityPolicy()) {
            val intervalContent = intervalContentState.value
            val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
            DrawerLazyLayoutItemProvider(
                state = state,
                intervalContent = intervalContent,
                keyIndexMap = map
            )
        }
        itemProviderState::value
    }
}

private class DrawerLazyLayoutItemProvider(
    private val state: DrawerInternalPagerState,
    private val intervalContent: LazyLayoutIntervalContent<PagerIntervalContent>,
    private val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyLayoutItemProvider {
    private val pagerScopeImpl = PagerScopeImpl

    override val itemCount: Int get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedPages) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(pagerScopeImpl, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawerLazyLayoutItemProvider) return false
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int = intervalContent.hashCode()
}

private class DrawerLayoutIntervalContent(
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
    key: ((index: Int) -> Any)?,
    pageCount: Int
) : LazyLayoutIntervalContent<PagerIntervalContent>() {
    override val intervals = MutableIntervalList<PagerIntervalContent>().apply {
        addInterval(pageCount, PagerIntervalContent(key = key, item = pageContent))
    }
}

