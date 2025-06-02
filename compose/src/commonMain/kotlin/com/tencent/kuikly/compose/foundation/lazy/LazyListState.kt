/*
 * Copyright 2020 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.lazy

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.compose.animation.core.AnimationState
import com.tencent.kuikly.compose.animation.core.AnimationVector1D
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.VectorConverter
import com.tencent.kuikly.compose.animation.core.animateTo
import com.tencent.kuikly.compose.animation.core.copy
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.MutatePriority
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollScope
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.interaction.InteractionSource
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.lazy.LazyListState.Companion.Saver
import com.tencent.kuikly.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import com.tencent.kuikly.compose.foundation.lazy.layout.ObservableScopeInvalidator
import com.tencent.kuikly.compose.foundation.lazy.layout.animateScrollToItem
import com.tencent.kuikly.compose.ui.layout.AlignmentLine
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.Remeasurement
import com.tencent.kuikly.compose.ui.layout.RemeasurementModifier
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.util.fastFirstOrNull
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.ranges.IntRange
import androidx.annotation.IntRange as AndroidXIntRange

/**
 * Creates a [LazyListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 * [LazyListState.firstVisibleItemScrollOffset]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberLazyListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyListState =
    rememberSaveable(saver = LazyListState.Saver) {
        LazyListState(
            initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset,
        )
    }

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberLazyListState].
 *
 * @param firstVisibleItemIndex the initial value for [LazyListState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 * [LazyListState.firstVisibleItemScrollOffset]
 * @param prefetchStrategy the [LazyListPrefetchStrategy] to use for prefetching content in this
 * list
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class LazyListState
    @ExperimentalFoundationApi
    constructor(
        firstVisibleItemIndex: Int = 0,
        firstVisibleItemScrollOffset: Int = 0,
//    private val prefetchStrategy: LazyListPrefetchStrategy = LazyListPrefetchStrategy(),
    ) : ScrollableState {
        override var contentPadding: PaddingValues = PaddingValues(0.dp)

        internal var hasLookaheadPassOccurred: Boolean = false
            private set
        internal var postLookaheadLayoutInfo: LazyListMeasureResult? = null
            private set

        /**
         * The holder class for the current scroll position.
         */
        private val scrollPosition =
            LazyListScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

        private val animateScrollScope = LazyListAnimateScrollScope(this)

        /**
         * The index of the first item that is visible.
         *
         * Note that this property is observable and if you use it in the composable function it will
         * be recomposed on every change causing potential performance issues.
         *
         * If you want to run some side effects like sending an analytics event or updating a state
         * based on this value consider using "snapshotFlow":
         * @sample androidx.compose.foundation.samples.UsingListScrollPositionForSideEffectSample
         *
         * If you need to use it in the composition then consider wrapping the calculation into a
         * derived state in order to only have recompositions when the derived value changes:
         * @sample androidx.compose.foundation.samples.UsingListScrollPositionInCompositionSample
         */
        val firstVisibleItemIndex: Int get() = scrollPosition.index

        /**
         * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the
         * amount that the item is offset backwards.
         *
         * Note that this property is observable and if you use it in the composable function it will
         * be recomposed on every scroll causing potential performance issues.
         * @see firstVisibleItemIndex for samples with the recommended usage patterns.
         */
        val firstVisibleItemScrollOffset: Int get() = scrollPosition.scrollOffset

        /** Backing state for [layoutInfo] */
        private val layoutInfoState =
            mutableStateOf(
                EmptyLazyListMeasureResult,
                neverEqualPolicy(),
            )

        /**
         * The object of [LazyListLayoutInfo] calculated during the last layout pass. For example,
         * you can use it to calculate what items are currently visible.
         *
         * Note that this property is observable and is updated after every scroll or remeasure.
         * If you use it in the composable function it will be recomposed on every change causing
         * potential performance issues including infinity recomposition loop.
         * Therefore, avoid using it in the composition.
         *
         * If you want to run some side effects like sending an analytics event or updating a state
         * based on this value consider using "snapshotFlow":
         * @sample androidx.compose.foundation.samples.UsingListLayoutInfoForSideEffectSample
         */
        val layoutInfo: LazyListLayoutInfo get() = layoutInfoState.value

        /**
         * [InteractionSource] that will be used to dispatch drag events when this
         * list is being dragged. If you want to know whether the fling (or animated scroll) is in
         * progress, use [isScrollInProgress].
         */
        val interactionSource: InteractionSource get() = internalInteractionSource

        internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

        /**
         * The amount of scroll to be consumed in the next layout pass.  Scrolling forward is negative
         * - that is, it is the amount that the items are offset in y
         */
        internal var scrollToBeConsumed = 0f
            private set

        internal val density: Density get() = layoutInfoState.value.density

        /**
         * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
         * we reached the end of the list.
         */
        internal val scrollableState = ScrollableState { -onScroll(-it) }

        /**
         * Only used for testing to confirm that we're not making too many measure passes
         */
        // @VisibleForTesting
        internal var numMeasurePasses: Int = 0
            private set

        /**
         * Only used for testing to disable prefetching when needed to test the main logic.
         */
        // @VisibleForTesting
        internal var prefetchingEnabled: Boolean = true

        /**
         * The [Remeasurement] object associated with our layout. It allows us to remeasure
         * synchronously during scroll.
         */
        internal var remeasurement: Remeasurement? = null
            private set

        /**
         * The modifier which provides [remeasurement].
         */
        internal val remeasurementModifier =
            object : RemeasurementModifier {
                override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                    this@LazyListState.remeasurement = remeasurement
                }
            }

        /**
         * Provides a modifier which allows to delay some interactions (e.g. scroll)
         * until layout is ready.
         */
        internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

//    internal val itemAnimator = LazyLayoutItemAnimator<LazyListMeasuredItem>()

        internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

        /**
         * Stores currently pinned items which are always composed.
         */
        internal val pinnedItems = LazyLayoutPinnedItemList()

        internal val nearestRange: IntRange by scrollPosition.nearestRangeState

        /**
         * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
         * pixels.
         *
         * @param index the index to which to scroll. Must be non-negative.
         * @param scrollOffset the offset that the item should end up after the scroll. Note that
         * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
         * scroll the item further upward (taking it partly offscreen).
         */
        suspend fun scrollToItem(
            @AndroidXIntRange(from = 0)
            index: Int,
            scrollOffset: Int = 0,
        ) {
            scroll {
                snapToItemIndexInternal(index, scrollOffset, forceRemeasure = true)
            }
        }

        internal val measurementScopeInvalidator = ObservableScopeInvalidator()

        /**
         * Requests the item at [index] to be at the start of the viewport during the next
         * remeasure, offset by [scrollOffset], and schedules a remeasure.
         *
         * The scroll position will be updated to the requested position rather than maintain
         * the index based on the first visible item key (when a data set change will also be
         * applied during the next remeasure), but *only* for the next remeasure.
         *
         * Any scroll in progress will be cancelled.
         *
         * @param index the index to which to scroll. Must be non-negative.
         * @param scrollOffset the offset that the item should end up after the scroll. Note that
         * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
         * scroll the item further upward (taking it partly offscreen).
         */
        fun requestScrollToItem(
            @AndroidXIntRange(from = 0)
            index: Int,
            scrollOffset: Int = 0,
        ) {
            // Cancel any scroll in progress.
            if (isScrollInProgress) {
                layoutInfoState.value.coroutineScope.launch {
                    scroll { }
                }
            }

            snapToItemIndexInternal(index, scrollOffset, forceRemeasure = false)
        }

        private fun calculateDistanceTo(targetIndex: Int): Int? {
            if (layoutInfo.visibleItemsInfo.isEmpty()) return 0
            val visibleItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }

            // 距离已知
            if (visibleItem != null) {
                return visibleItem.offset
            }
            return null
        }

        /**
         * Snaps to the requested scroll position. Synchronously executes remeasure if [forceRemeasure]
         * is true, and schedules a remeasure if false.
         */
        internal fun snapToItemIndexInternal(
            index: Int,
            scrollOffset: Int,
            forceRemeasure: Boolean,
        ) {
            if (index < 0 || index >= layoutInfo.totalItemsCount) {
                return
            }

            val positionChanged =
                scrollPosition.index != index ||
                    scrollPosition.scrollOffset != scrollOffset
            // sometimes this method is called not to scroll, but to stay on the same index when
            // the data changes, as by default we maintain the scroll position by key, not index.
            // when this happens we don't need to reset the animations as from the user perspective
            // we didn't scroll anywhere and if there is an offset change for an item, this change
            // should be animated.
            // however, when the request is to really scroll to a different position, we have to
            // reset previously known item positions as we don't want offset changes to be animated.
            // this offset should be considered as a scroll, not the placement change.
            if (positionChanged) {
                kuiklyInfo.offsetDirty = true
            }
            scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)

            if (forceRemeasure) {
                remeasurement?.forceRemeasure()
            } else {
                measurementScopeInvalidator.invalidateScope()
            }
        }

        /**
         * Call this function to take control of scrolling and gain the ability to send scroll events
         * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
         * performed within a [scroll] block (even if they don't call any other methods on this
         * object) in order to guarantee that mutual exclusion is enforced.
         *
         * If [scroll] is called from elsewhere, this will be canceled.
         */
        override suspend fun scroll(
            scrollPriority: MutatePriority,
            block: suspend ScrollScope.() -> Unit,
        ) {
            awaitLayoutModifier.waitForFirstLayout()
            scrollableState.scroll(scrollPriority, block)
        }

        override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

        override val isScrollInProgress: Boolean
            get() = scrollableState.isScrollInProgress

        override var canScrollForward: Boolean by mutableStateOf(false)
            private set
        override var canScrollBackward: Boolean by mutableStateOf(false)
            private set

        @get:Suppress("GetterSetterNames")
        override val lastScrolledForward: Boolean
            get() = scrollableState.lastScrolledForward

        @get:Suppress("GetterSetterNames")
        override val lastScrolledBackward: Boolean
            get() = scrollableState.lastScrolledBackward

        internal val placementScopeInvalidator = ObservableScopeInvalidator()

        // TODO: Coroutine scrolling APIs will allow this to be private again once we have more
        //  fine-grained control over scrolling
        // @VisibleForTesting
        internal fun onScroll(distance: Float): Float {
            if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
                return 0f
            }
            check(abs(scrollToBeConsumed) <= 0.5f) {
                "entered drag with non-zero pending scroll: $scrollToBeConsumed"
            }
            scrollToBeConsumed += distance

            // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
            // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
            // we have less than 0.5 pixels
            if (abs(scrollToBeConsumed) > 0.5f) {
                val layoutInfo = layoutInfoState.value
                val preScrollToBeConsumed = scrollToBeConsumed
                val intDelta = scrollToBeConsumed.fastRoundToInt()
                val postLookaheadInfo = postLookaheadLayoutInfo
                var scrolledWithoutRemeasure =
                    layoutInfo.tryToApplyScrollWithoutRemeasure(
                        delta = intDelta,
                        updateAnimations = !hasLookaheadPassOccurred,
                    )
                if (scrolledWithoutRemeasure && postLookaheadInfo != null) {
                    scrolledWithoutRemeasure =
                        postLookaheadInfo.tryToApplyScrollWithoutRemeasure(
                            delta = intDelta,
                            updateAnimations = true,
                        )
                }
                if (scrolledWithoutRemeasure) {
                    applyMeasureResult(
                        result = layoutInfo,
                        isLookingAhead = hasLookaheadPassOccurred,
                        visibleItemsStayedTheSame = true,
                    )
                    // we don't need to remeasure, so we only trigger re-placement:
                    placementScopeInvalidator.invalidateScope()
                } else {
                    remeasurement?.forceRemeasure()
                }
            }

            // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
            if (abs(scrollToBeConsumed) <= 0.5f) {
                // We consumed all of it - we'll hold onto the fractional scroll for later, so report
                // that we consumed the whole thing
                return distance
            } else {
                val scrollConsumed = distance - scrollToBeConsumed
                // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
                // nested scrolling)
                scrollToBeConsumed = 0f // We're not consuming the rest, give it back
                return scrollConsumed
            }
        }

        /**
         * Animate (smooth scroll) to the given item.
         *
         * @param index the index to which to scroll. Must be non-negative.
         * @param scrollOffset the offset that the item should end up after the scroll. Note that
         * positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
         * scroll the item further upward (taking it partly offscreen).
         */
        suspend fun animateScrollToItem(
            @AndroidXIntRange(from = 0)
            index: Int,
            scrollOffset: Int = 0,
        ) {
            kuiklyInfo.offsetDirty = true
            animateScrollScope.animateScrollToItem(
                index,
                scrollOffset,
                NumberOfItemsToTeleport,
                density,
            )
        }

        /**
         *  Updates the state with the new calculated scroll position and consumed scroll.
         */
        internal fun applyMeasureResult(
            result: LazyListMeasureResult,
            isLookingAhead: Boolean,
            visibleItemsStayedTheSame: Boolean = false,
        ) {
            if (!isLookingAhead && hasLookaheadPassOccurred) {
                // If there was already a lookahead pass, record this result as postLookahead result
                postLookaheadLayoutInfo = result
            } else {
                if (isLookingAhead) {
                    hasLookaheadPassOccurred = true
                }

                canScrollBackward = result.canScrollBackward
                canScrollForward = result.canScrollForward
                scrollToBeConsumed -= result.consumedScroll
                layoutInfoState.value = result

                if (visibleItemsStayedTheSame) {
                    scrollPosition.updateScrollOffset(result.firstVisibleItemScrollOffset)
                } else {
                    scrollPosition.updateFromMeasureResult(result)
                }

                if (isLookingAhead) {
                    updateScrollDeltaForPostLookahead(
                        result.scrollBackAmount,
                        result.density,
                        result.coroutineScope,
                    )
                }
                numMeasurePasses++
            }
        }

        internal val scrollDeltaBetweenPasses: Float
            get() = _scrollDeltaBetweenPasses.value

        private var _scrollDeltaBetweenPasses: AnimationState<Float, AnimationVector1D> =
            AnimationState(Float.VectorConverter, 0f, 0f)

        // Updates the scroll delta between lookahead & post-lookahead pass
        private fun updateScrollDeltaForPostLookahead(
            delta: Float,
            density: Density,
            coroutineScope: CoroutineScope,
        ) {
            if (delta <= with(density) { DeltaThresholdForScrollAnimation.toPx() }) {
                // If the delta is within the threshold, scroll by the delta amount instead of animating
                return
            }

            // Scroll delta is updated during lookahead, we don't need to trigger lookahead when
            // the delta changes.
            Snapshot.withoutReadObservation {
                val currentDelta = _scrollDeltaBetweenPasses.value

                if (_scrollDeltaBetweenPasses.isRunning) {
                    _scrollDeltaBetweenPasses = _scrollDeltaBetweenPasses.copy(currentDelta - delta)
                    coroutineScope.launch {
                        _scrollDeltaBetweenPasses.animateTo(
                            0f,
                            spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.5f),
                            true,
                        )
                    }
                } else {
                    _scrollDeltaBetweenPasses = AnimationState(Float.VectorConverter, -delta)
                    coroutineScope.launch {
                        _scrollDeltaBetweenPasses.animateTo(
                            0f,
                            spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = 0.5f),
                            true,
                        )
                    }
                }
            }
        }

        /**
         * When the user provided custom keys for the items we can try to detect when there were
         * items added or removed before our current first visible item and keep this item
         * as the first visible one even given that its index has been changed.
         * The scroll position will not be updated if [requestScrollToItem] was called since
         * the last time this method was called.
         */
        internal fun updateScrollPositionIfTheFirstItemWasMoved(
            itemProvider: LazyListItemProvider,
            firstItemIndex: Int,
        ): Int = scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

        companion object {
            /**
             * The default [Saver] implementation for [LazyListState].
             */
            val Saver: Saver<LazyListState, *> =
                listSaver(
                    save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                    restore = {
                        LazyListState(
                            firstVisibleItemIndex = it[0],
                            firstVisibleItemScrollOffset = it[1],
                        )
                    },
                )
        }
    }

private val DeltaThresholdForScrollAnimation = 1.dp

private val EmptyLazyListMeasureResult =
    LazyListMeasureResult(
        firstVisibleItem = null,
        firstVisibleItemScrollOffset = 0,
        canScrollForward = false,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        scrollBackAmount = 0f,
        visibleItemsInfo = emptyList(),
        positionedItems = emptyList(),
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        totalItemsCount = 0,
        reverseLayout = false,
        orientation = Orientation.Vertical,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        remeasureNeeded = false,
        coroutineScope = CoroutineScope(EmptyCoroutineContext),
        density = Density(1f),
        childConstraints = Constraints(),
    )

private const val NumberOfItemsToTeleport = 100
