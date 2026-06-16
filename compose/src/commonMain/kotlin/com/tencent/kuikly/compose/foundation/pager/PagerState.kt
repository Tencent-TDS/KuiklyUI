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
import androidx.annotation.IntRange as AndroidXIntRange
import com.tencent.kuikly.compose.animation.core.AnimationSpec
import com.tencent.kuikly.compose.animation.core.animate
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.MutatePriority
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollScope
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.gestures.stopScroll
import com.tencent.kuikly.compose.foundation.interaction.InteractionSource
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import com.tencent.kuikly.compose.foundation.lazy.layout.ObservableScopeInvalidator
import com.tencent.kuikly.compose.foundation.lazy.layout.findIndexByKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.layout.AlignmentLine
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.Remeasurement
import com.tencent.kuikly.compose.ui.layout.RemeasurementModifier
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.scroller.applyScrollViewOffsetDelta
import com.tencent.kuikly.compose.scroller.convertAnimationSpecToSpringAnimation
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.compose.profiler.RecompositionProfiler
import com.tencent.kuikly.compose.material3.internal.identityHashCode
import com.tencent.kuikly.core.collection.fastMutableMapOf
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.ranges.IntRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Creates and remember a [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param initialPage The pager that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
@Composable
fun rememberPagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState {
    return rememberSaveable(saver = DefaultPagerState.Saver) {
        DefaultPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount
        )
    }.apply {
        pageCountState.value = pageCount
    }
}

/**
 * Creates a default [PagerState] to be used with a [Pager]
 *
 * Please refer to the sample to learn how to use this API.
 * @sample androidx.compose.foundation.samples.PagerWithStateSample
 *
 * @param currentPage The pager that should be shown first.
 * @param currentPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * This should vary between -0.5 and 0.5 and indicates how to offset the initial page from the
 * snapped position.
 * @param pageCount The amount of pages this Pager will have.
 */
fun PagerState(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    pageCount: () -> Int
): PagerState = DefaultPagerState(currentPage, currentPageOffsetFraction, pageCount)

private class DefaultPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int
) : PagerState(currentPage, currentPageOffsetFraction) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()

    companion object {
        /**
         * To keep current page and current page offset saved.
         * Also saves bridge-layer state (composeOffset, currentContentSize, contentOffset, offsetDirty)
         * to support ScrollerView reuse in nested Pager scenarios.
         */
        val Saver: Saver<DefaultPagerState, *> = listSaver(
            save = {
                val saved = listOf(
                    it.currentPage,                             // [0] Int
                    (it.currentPageOffsetFraction).coerceIn(MinPageOffset, MaxPageOffset), // [1] Float
                    it.pageCount,                               // [2] Int
                    it.kuiklyInfo.composeOffset.toInt(),        // [3] bridge: Compose scroll offset
                    it.kuiklyInfo.currentContentSize,           // [4] bridge: virtual content size
                    it.kuiklyInfo.contentOffset,                // [5] bridge: native scrollView offset
                    if (it.kuiklyInfo.offsetDirty) 1 else 0,   // [6] bridge: offset dirty flag
                )
                saved
            },
            restore = {
                DefaultPagerState(
                    currentPage = it[0] as Int,
                    currentPageOffsetFraction = it[1] as Float,
                    updatedPageCount = { it[2] as Int }
                ).also { state ->
                    if (it.size > 3) { // backward compatibility with old saved data
                        state.kuiklyInfo.composeOffset = (it[3] as Int).toFloat()
                        state.kuiklyInfo.currentContentSize = it[4] as Int
                        state.kuiklyInfo.contentOffset = it[5] as Int
                        state.kuiklyInfo.offsetDirty = (it[6] as Int) == 1
                    }
                }
            }
        )
    }
}

/**
 * The state that can be used to control [VerticalPager] and [HorizontalPager]
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
abstract class PagerState internal constructor(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
//    prefetchScheduler: PrefetchScheduler? = null
) : ScrollableState {

//    /**
//     * @param currentPage The initial page to be displayed
//     * @param currentPageOffsetFraction The offset of the initial page with respect to the start of
//     * the layout.
//     */
//    constructor(
//        currentPage: Int = 0,
//        @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f
//    ) : this(currentPage, currentPageOffsetFraction, null)

    /**
     * The total amount of pages present in this pager. The source of this data should be
     * observable.
     */
    abstract val pageCount: Int

    init {
        require(currentPageOffsetFraction in -0.5..0.5) {
            "currentPageOffsetFraction $currentPageOffsetFraction is " +
                "not within the range -0.5 to 0.5"
        }
    }

    override var contentPadding: PaddingValues = PaddingValues(0.dp)

    /**
     * Difference between the last up and last down events of a scroll event.
     */
    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)
    private val animatedScrollScope = PagerLazyAnimateScrollScope(this)

    internal val debugPagerStateId = identityHashCode(this)

    private val scrollPosition = PagerScrollPosition(currentPage, currentPageOffsetFraction, this)

    internal var firstVisiblePage = currentPage
        private set

    internal var firstVisiblePageOffset = 0
        private set

    private var maxScrollOffset: Long = Long.MAX_VALUE

    private var minScrollOffset: Long = 0L

    private var alignmentLayoutGeneration = 0
    private var lastAlignmentOrientation: Orientation? = null
    private var lastAlignmentLayoutSize = 0
    private var lastAlignmentPageSizeWithSpacing = 0
    private var alignScheduledPageCount = 0

    private var accumulator: Float = 0.0f

    /**
     * The prefetch will act after the measure pass has finished and it needs to know the
     * magnitude and direction of the scroll that triggered the measure pass
     */
    private var previousPassDelta = 0f

    /**
     * The ScrollableController instance. We keep it as we need to call stopAnimation on it once
     * we reached the end of the list.
     */
    internal val scrollableState = ScrollableState { performScroll(it) }

    /**
     * Within the scrolling context we can use absolute positions to
     * determine scroll deltas and max min scrolling.
     */
    private fun performScroll(delta: Float): Float {
        val currentScrollPosition = currentAbsoluteScrollOffset()
        debugLog {
            "\nDelta=$delta " +
                "\ncurrentScrollPosition=$currentScrollPosition " +
                "\naccumulator=$accumulator" +
                "\nmaxScrollOffset=$maxScrollOffset"
        }

        val decimalAccumulation = (delta + accumulator)
        val decimalAccumulationInt = decimalAccumulation.roundToLong()
        accumulator = decimalAccumulation - decimalAccumulationInt

        // nothing to scroll
        if (delta.absoluteValue < 1e-4f) return delta

        /**
         * The updated scroll position is the current position with the integer part of the delta
         * and accumulator applied.
         */
        val updatedScrollPosition = (currentScrollPosition + decimalAccumulationInt)

        /**
         * Check if the scroll position may be larger than the maximum possible scroll.
         */
        val coercedScrollPosition = updatedScrollPosition.coerceIn(minScrollOffset, maxScrollOffset)

        /**
         * Check if we actually coerced.
         */
        val changed = updatedScrollPosition != coercedScrollPosition

        /**
         * Calculated the actual scroll delta to be applied
         */
        val scrollDelta = coercedScrollPosition - currentScrollPosition

        previousPassDelta = scrollDelta.toFloat()

        if (scrollDelta.absoluteValue != 0L) {
            isLastScrollForwardState.value = scrollDelta > 0.0f
            isLastScrollBackwardState.value = scrollDelta < 0.0f
        }

        /**
         * Apply the scroll delta
         */
        val layoutInfo = pagerLayoutInfoState.value
        if (layoutInfo.tryToApplyScrollWithoutRemeasure(-scrollDelta.toInt())) {
            debugLog { "Will Apply Without Remeasure" }
            applyMeasureResult(
                result = layoutInfo,
                visibleItemsStayedTheSame = true
            )
            // we don't need to remeasure, so we only trigger re-placement:
            placementScopeInvalidator.invalidateScope()
            layoutWithoutMeasurement++
        } else {
            debugLog { "Will Apply With Remeasure" }
            scrollPosition.applyScrollDelta(scrollDelta.toInt())
            remeasurement?.forceRemeasure()
            layoutWithMeasurement++
        }

        // Return the consumed value.
        return (if (changed) scrollDelta else delta).toFloat()
    }

    /**
     * Only used for testing to confirm that we're not making too many measure passes
     */
    internal val numMeasurePasses: Int get() = layoutWithMeasurement + layoutWithoutMeasurement

    internal var layoutWithMeasurement: Int = 0
        private set

    private var layoutWithoutMeasurement: Int = 0

    /**
     * Only used for testing to disable prefetching when needed to test the main logic.
     */
    internal var prefetchingEnabled: Boolean = true

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var indexToPrefetch = -1

//    /**
//     * The handle associated with the current index from [indexToPrefetch].
//     */
//    private var currentPrefetchHandle: LazyLayoutPrefetchState.PrefetchHandle? = null

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to
     * detect the scrolling direction change.
     */
    private var wasPrefetchingForward = false

    /** Backing state for PagerLayoutInfo */
    private var pagerLayoutInfoState =
        mutableStateOf(EmptyLayoutInfo, neverEqualPolicy())

    /**
     * A [PagerLayoutInfo] that contains useful information about the Pager's last layout pass.
     * For instance, you can query which pages are currently visible in the layout.
     *
     * This property is observable and is updated after every scroll or remeasure.
     * If you use it in the composable function it will be recomposed on every change causing
     * potential performance issues including infinity recomposition loop.
     * Therefore, avoid using it in the composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     * @sample androidx.compose.foundation.samples.UsingPagerLayoutInfoForSideEffectSample
     */
    val layoutInfo: PagerLayoutInfo get() = pagerLayoutInfoState.value

    internal val pageSpacing: Int
        get() = pagerLayoutInfoState.value.pageSpacing

    internal val pageSize: Int
        get() = pagerLayoutInfoState.value.pageSize

    internal var density: Density = UnitDensity

    internal val pageSizeWithSpacing: Int
        get() = pageSize + pageSpacing

    /**
     * How far the current page needs to scroll so the target page is considered to be the next
     * page.
     */
    internal val positionThresholdFraction: Float
        get() = with(density) {
            val minThreshold = minOf(DefaultPositionThreshold.toPx(), pageSize / 2f)
            minThreshold / pageSize.toFloat()
        }

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or animated scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource
        get() = internalInteractionSource

    /**
     * The page that sits closest to the snapped position. This is an observable value and will
     * change as the pager scrolls either by gesture or animation.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     *
     */
    val currentPage: Int get() = scrollPosition.currentPage

    /** Native setContentOffset(animated=true) snap is in progress. */
    internal var isSnapAnimating = false

    /** Native content offset that the snap animation is settling to. */
    internal var snapTargetContentOffset = 0

    /**
     * pageCount captured when the snap animation started. Used to detect that the item set
     * changed (e.g. head/middle insertion) during the snap settle window, in which case the
     * native pixel offset becomes stale and must not be used to re-derive the target page.
     */
    internal var snapStartPageCount = 0

    /**
     * Key of the target page item captured when the snap animation started. When items are
     * inserted/removed during the settle window, the target page index shifts; we re-resolve
     * the target index from this key (see [relocateSnapTargetByKey]) instead of trusting the
     * stale native pixel offset.
     */
    internal var snapTargetItemKey: Any? = null

    /** Target page index, kept up to date by key when the item set changes during snap. */
    internal var snapTargetRelocatedPage = -1

    /**
     * Compose<->native desync (in pages) captured when the snap started. When non-zero the native
     * pixel offset cannot be trusted to re-derive the target page at settle (the coordinate systems
     * are shifted), so the settle must drive compose from the key-tracked target page and re-base
     * native onto compose's boundary, exactly like the item-set-changed path.
     */
    internal var snapStartDesyncPages = 0

    private var snapTargetReachedAlignmentRequested = false

    private var snapStallAlignmentRetryRequested = false

    /** Called before native setContentOffset(animated=true). */
    internal fun markSnapAnimationStarted(
        targetContentOffset: Int,
        targetPage: Int = -1,
        targetKey: Any? = null,
        desyncPages: Int = 0
    ) {
        isSnapAnimating = true
        snapTargetContentOffset = targetContentOffset
        snapStartPageCount = pageCount
        snapTargetRelocatedPage = targetPage
        snapTargetItemKey = targetKey
        snapStartDesyncPages = desyncPages
        kuiklyInfo.snapAnchorOffsetCorrection = 0
        snapTargetReachedAlignmentRequested = false
        snapStallAlignmentRetryRequested = false
        pagerSnapDebugLog {
            "snapStarted: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                "targetOffset=$targetContentOffset contentOffset=${kuiklyInfo.contentOffset} " +
                "composeOffset=${currentAbsoluteScrollOffset().toInt()} currentPage=$currentPage " +
                "targetPage=$targetPage targetKey=$targetKey pageCount=$pageCount " +
                "desyncPages=$desyncPages anchorCorrection=${kuiklyInfo.snapAnchorOffsetCorrection}"
        }
    }

    internal fun hasSnapReachedTarget(contentOffset: Int): Boolean {
        return abs(contentOffset - snapTargetContentOffset) <= SNAP_TARGET_OFFSET_TOLERANCE
    }

    internal fun onNativeContentOffsetChanged(contentOffset: Int) {
        if (!isSnapAnimating || snapTargetReachedAlignmentRequested) {
            return
        }

        if (!hasSnapReachedTarget(contentOffset)) {
            return
        }

        snapTargetReachedAlignmentRequested = true
        pagerSnapDebugLog {
            "snapTargetReached: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                "contentOffset=$contentOffset target=$snapTargetContentOffset " +
                "currentPage=$currentPage anchorCorrection=${kuiklyInfo.snapAnchorOffsetCorrection}"
        }
        scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS)
    }

    /** Clears drag/animated snap tracking. Call before instant programmatic scroll. */
    internal fun clearSnapAnimationState() {
        if (isSnapAnimating) {
            pagerSnapDebugLog {
                "clearSnapState: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "target=$snapTargetContentOffset contentOffset=${kuiklyInfo.contentOffset} " +
                    "currentPage=$currentPage snapStartPageCount=$snapStartPageCount " +
                    "pageCount=$pageCount snapTargetPage=$snapTargetRelocatedPage " +
                    "snapTargetKey=$snapTargetItemKey snapStartDesyncPages=$snapStartDesyncPages"
            }
        }
        isSnapAnimating = false
        snapTargetContentOffset = 0
        snapStartPageCount = 0
        snapTargetItemKey = null
        snapTargetRelocatedPage = -1
        snapStartDesyncPages = 0
        snapTargetReachedAlignmentRequested = false
        snapStallAlignmentRetryRequested = false
        kuiklyInfo.snapAnchorOffsetCorrection = 0
        kuiklyInfo.appleScrollViewOffsetJob?.cancel()
    }

    private fun scheduleScrollViewOffsetAlignment(
        delayMs: Long,
        layoutSize: Int = currentLayoutMainAxisSize()
    ) {
        val scheduledOrientation = layoutInfo.orientation
        val scheduledPageSizeWithSpacing = pageSizeWithSpacing
        val scheduledContentOffset = kuiklyInfo.contentOffset
        val scheduledComposeOffset = currentAbsoluteScrollOffset().toInt()
        val scheduledLayoutGeneration = alignmentLayoutGeneration
        val scheduledPageCount = pageCount
        alignScheduledPageCount = scheduledPageCount
        pagerSnapDebugLog {
            "scheduleAlign: stateId=$debugPagerStateId orientation=$scheduledOrientation " +
                "layoutGeneration=$scheduledLayoutGeneration " +
                "layoutSize=$layoutSize pageSizeWithSpacing=$scheduledPageSizeWithSpacing " +
                "contentOffset=$scheduledContentOffset composeOffset=$scheduledComposeOffset " +
                "scheduledPageCount=$scheduledPageCount " +
                "isSnapAnimating=$isSnapAnimating isScrollInProgress=$isScrollInProgress"
        }
        kuiklyInfo.run {
            appleScrollViewOffsetJob?.cancel()
            appleScrollViewOffsetJob = scope?.launch {
                delay(delayMs)
                alignScrollViewOffset(
                    layoutSize,
                    scheduledOrientation,
                    scheduledPageSizeWithSpacing,
                    scheduledContentOffset,
                    scheduledComposeOffset,
                    scheduledLayoutGeneration,
                    scheduledPageCount
                )
            }
        }
    }

    private fun currentLayoutMainAxisSize(): Int {
        val layoutInfo = layoutInfo
        return if (layoutInfo.orientation == Orientation.Horizontal) {
            layoutInfo.viewportSize.width
        } else {
            layoutInfo.viewportSize.height
        }
    }

    private fun nearestPageForOffset(offset: Int): Int {
        val pageSize = pageSizeWithSpacing
        if (pageSize == 0) {
            return currentPage.coerceInPageRange()
        }
        return (offset / pageSize.toFloat()).roundToInt().coerceInPageRange()
    }

    /**
     * Scroll offset where [page] sits on its snap boundary.
     * Last/first page must use [maxScrollOffset]/[minScrollOffset] so after/before contentPadding
     * is not clipped when native is already at the scroll limit.
     */
    private fun pageBoundaryOffset(page: Int): Int {
        if (pageCount == 0 || pageSizeWithSpacing == 0) {
            return 0
        }
        val coercedPage = page.coerceInPageRange()
        var offset = coercedPage * pageSizeWithSpacing
        if (coercedPage == 0) {
            offset = max(offset, minScrollOffset.toInt())
        }
        if (coercedPage == pageCount - 1) {
            offset = min(offset, maxScrollOffset.toInt())
        }
        return offset
    }

    private fun pageBoundaryOffsetFraction(page: Int): Float {
        if (pageSizeWithSpacing == 0) {
            return 0f
        }
        val coercedPage = page.coerceInPageRange()
        return (pageBoundaryOffset(coercedPage) - coercedPage * pageSizeWithSpacing)
            .toFloat() / pageSizeWithSpacing
    }

    internal fun snapScrollOffsetForPage(page: Int): Int = pageBoundaryOffset(page)

    private fun isPageBoundaryOffset(offset: Int): Boolean {
        val boundaryOffset = pageBoundaryOffset(nearestPageForOffset(offset))
        return abs(boundaryOffset - offset) <= SNAP_TARGET_OFFSET_TOLERANCE
    }

    private suspend fun alignScrollViewOffset(
        layoutSize: Int,
        scheduledOrientation: Orientation,
        scheduledPageSizeWithSpacing: Int,
        scheduledContentOffset: Int,
        scheduledComposeOffset: Int,
        scheduledLayoutGeneration: Int,
        scheduledPageCount: Int
    ) {
        val contentOffsetInt = scrollableState.kuiklyInfo.contentOffset

        if (scheduledLayoutGeneration != alignmentLayoutGeneration) {
            pagerSnapDebugLog {
                "staleAlignDetected: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "scheduledOrientation=$scheduledOrientation " +
                    "scheduledGeneration=$scheduledLayoutGeneration " +
                    "currentGeneration=$alignmentLayoutGeneration " +
                    "scheduledPageSizeWithSpacing=$scheduledPageSizeWithSpacing " +
                    "currentPageSizeWithSpacing=$pageSizeWithSpacing " +
                    "scheduledLayoutSize=$layoutSize currentLayoutSize=${currentLayoutMainAxisSize()} " +
                    "scheduledContentOffset=$scheduledContentOffset contentOffset=$contentOffsetInt " +
                    "scheduledComposeOffset=$scheduledComposeOffset " +
                    "composeOffset=${currentAbsoluteScrollOffset().toInt()}"
            }
        }

        if (handleUnreachedSnapTarget(
                contentOffsetInt,
                scheduledContentOffset,
                layoutSize
            )
        ) {
            return
        }

        // While snapping, the target item is pinned to snapTargetContentOffset by
        // snapAnchorOffsetCorrection. Once the native animation reaches that original target,
        // settle the compose position to the key-tracked page and clear the correction by moving
        // native offset and child frames together. This keeps the visual position unchanged while
        // restoring the normal compose/native coordinate system.
        val itemsChangedDuringSnap =
            isSnapAnimating && snapStartPageCount != 0 && pageCount != snapStartPageCount
        val snapStartedDesynced = isSnapAnimating && snapStartDesyncPages != 0
        if (itemsChangedDuringSnap || snapStartedDesynced) {
            val relocatedTarget = if (snapTargetRelocatedPage in 0 until pageCount) {
                snapTargetRelocatedPage
            } else {
                currentPage.coerceInPageRange()
            }
            val relocatedTargetOffset = pageBoundaryOffset(relocatedTarget)
            pagerSnapDebugLog {
                "alignRelocatedSnapTarget: stateId=$debugPagerStateId " +
                    "orientation=${layoutInfo.orientation} relocatedTarget=$relocatedTarget " +
                    "relocatedTargetOffset=$relocatedTargetOffset " +
                    "snapTargetKey=$snapTargetItemKey snapStartPageCount=$snapStartPageCount " +
                    "pageCount=$pageCount snapStartedDesynced=$snapStartedDesynced " +
                    "contentOffset=$contentOffsetInt composeOffset=${currentAbsoluteScrollOffset().toInt()} " +
                    "currentPage=$currentPage"
            }
            updateScrollViewContentSize(layoutSize)
            val relocatedKey = snapTargetItemKey
            if (relocatedKey != null) {
                pagerSnapDebugLog {
                    "alignRelocatedSnapTargetKeepKey: stateId=$debugPagerStateId " +
                        "orientation=${layoutInfo.orientation} relocatedTarget=$relocatedTarget " +
                        "relocatedKey=$relocatedKey pageCount=$pageCount " +
                        "snapStartPageCount=$snapStartPageCount snapStartedDesynced=$snapStartedDesynced"
                }
                scrollPosition.requestPositionAndKeepKnownKey(relocatedTarget, 0f, relocatedKey)
            } else {
                scrollPosition.requestPositionAndForgetLastKnownKey(relocatedTarget, 0f)
            }
            val delta = relocatedTargetOffset - contentOffsetInt
            if (delta != 0) {
                applyScrollViewOffsetDelta(delta)
            } else {
                // requestPosition changes the Compose page raw coordinates. Keep the frame offset
                // on the relocated target boundary; otherwise the viewport can stay in the stale
                // native coordinate system after items are inserted before the snap target.
                kuiklyInfo.composeOffset = relocatedTargetOffset.toFloat()
            }
            kuiklyInfo.snapAnchorOffsetCorrection = 0
            clearSnapTrackingAfterAlignment()
            return
        }

        val normalComposeOffset = currentAbsoluteScrollOffset().toInt()
        val positionCorrupted = isSnapAnimating && normalComposeOffset != contentOffsetInt
        val correctTargetPage = nearestPageForOffset(contentOffsetInt)
        val correctTargetOffset = pageBoundaryOffset(correctTargetPage)
        val composeOffsetInt = if (positionCorrupted) {
            correctTargetOffset
        } else {
            normalComposeOffset
        }

        val needFix = !isScrollInProgress && contentOffsetInt != composeOffsetInt
        val composeOffsetOnBoundary = isPageBoundaryOffset(composeOffsetInt)
        val contentOffsetOnBoundary = isPageBoundaryOffset(contentOffsetInt)

        updateScrollViewContentSize(layoutSize)

        if (alignComposePositionToNativeBoundaryIfNeeded(
                composeOffsetInt,
                contentOffsetInt,
                scheduledPageCount
            )
        ) {
            return
        }

        if (needFix || positionCorrupted || isSnapAnimating) {
            pagerSnapDebugLog {
                "alignJob: needFix=$needFix positionCorrupted=$positionCorrupted " +
                    "stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "scheduledOrientation=$scheduledOrientation " +
                    "layoutGeneration=$alignmentLayoutGeneration " +
                    "composeOffset=$composeOffsetInt contentOffset=$contentOffsetInt " +
                    "scheduledComposeOffset=$scheduledComposeOffset " +
                    "scheduledContentOffset=$scheduledContentOffset " +
                    "normalComposeOffset=$normalComposeOffset currentPage=$currentPage " +
                    "currentPageOffsetFraction=$currentPageOffsetFraction " +
                    "firstVisiblePage=$firstVisiblePage firstVisiblePageOffset=$firstVisiblePageOffset " +
                    "correctTargetPage=$correctTargetPage correctTargetOffset=$correctTargetOffset " +
                    "pageSizeWithSpacing=$pageSizeWithSpacing " +
                    "scheduledPageSizeWithSpacing=$scheduledPageSizeWithSpacing " +
                    "layoutSize=$layoutSize currentLayoutSize=${currentLayoutMainAxisSize()} " +
                    "isScrollInProgress=$isScrollInProgress isSnapAnimating=$isSnapAnimating " +
                    "composeOffsetOnBoundary=$composeOffsetOnBoundary " +
                    "contentOffsetOnBoundary=$contentOffsetOnBoundary target=$snapTargetContentOffset"
            }
        }

        if (needFix) {
            val delta = composeOffsetInt - contentOffsetInt
            pagerSnapDebugLog {
                "fixNativeOffset: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} delta=$delta"
            }
            applyScrollViewOffsetDelta(delta)
        }

        if (positionCorrupted) {
            pagerSnapDebugLog {
                "fixScrollPosition: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "correctTargetPage=$correctTargetPage " +
                    "composeOffset=$composeOffsetInt contentOffset=$contentOffsetInt"
            }
            val targetKey = snapTargetItemKey
            if (targetKey != null) {
                pagerSnapDebugLog {
                    "fixScrollPositionKeepTargetKey: stateId=$debugPagerStateId " +
                        "orientation=${layoutInfo.orientation} correctTargetPage=$correctTargetPage " +
                        "targetKey=$targetKey composeOffset=$composeOffsetInt contentOffset=$contentOffsetInt"
                }
                scrollPosition.requestPositionAndKeepKnownKey(correctTargetPage, 0f, targetKey)
            } else {
                scrollPosition.requestPositionAndForgetLastKnownKey(correctTargetPage, 0f)
            }
            kuiklyInfo.composeOffset = correctTargetOffset.toFloat()
        }

        if (isSnapAnimating) {
            clearSnapTrackingAfterAlignment()
        }
    }

    private fun updateScrollViewContentSize(layoutSize: Int) {
        val requiredContentSize = (maxScrollOffset + layoutSize).toInt()
        if (kuiklyInfo.currentContentSize != requiredContentSize) {
            kuiklyInfo.currentContentSize = requiredContentSize
            kuiklyInfo.updateContentSizeToRender()
        }
    }

    private fun alignComposePositionToNativeBoundaryIfNeeded(
        composeOffset: Int,
        contentOffset: Int,
        scheduledPageCount: Int
    ): Boolean {
        val composeOffsetOnBoundary = isPageBoundaryOffset(composeOffset)
        val shouldSkipComposeOffset = pageSizeWithSpacing != 0 &&
            !isScrollInProgress &&
            !isSnapAnimating &&
            !composeOffsetOnBoundary
        val anchorKey = scrollPosition.anchorKey()
        pagerSnapDebugLog {
            "alignBoundaryCheck: shouldSkip=$shouldSkipComposeOffset " +
                "stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                "composeOffset=$composeOffset contentOffset=$contentOffset " +
                "pageSizeWithSpacing=$pageSizeWithSpacing isScrollInProgress=$isScrollInProgress " +
                "isSnapAnimating=$isSnapAnimating composeOffsetOnBoundary=$composeOffsetOnBoundary " +
                "contentOffsetOnBoundary=${isPageBoundaryOffset(contentOffset)} currentPage=$currentPage " +
                "anchorKey=$anchorKey scheduledPageCount=$scheduledPageCount pageCount=$pageCount"
        }
        if (!shouldSkipComposeOffset) {
            return false
        }

        if (scheduledPageCount != 0 && pageCount != scheduledPageCount) {
            pagerSnapDebugLog {
                "alignBoundarySkippedPageCountChanged: stateId=$debugPagerStateId " +
                    "orientation=${layoutInfo.orientation} scheduledPageCount=$scheduledPageCount " +
                    "pageCount=$pageCount currentPage=$currentPage anchorKey=$anchorKey"
            }
            return false
        }

        val nativePage = nearestPageForOffset(contentOffset)
        val trustedPage = currentPage.coerceInPageRange()
        // native > trusted: forward scroll settled on native first — must follow native, not stale key.
        // native < trusted with large gap: prepend shifted indices while native offset is still low — keep key.
        // native < trusted with gap 1: backward scroll or minor desync — trust native.
        val pageGap = trustedPage - nativePage
        val targetPage = when {
            nativePage == trustedPage -> trustedPage
            nativePage > trustedPage -> nativePage
            anchorKey != null && pageGap > 1 -> trustedPage
            else -> nativePage
        }
        val keepAnchorKey = anchorKey != null && targetPage == trustedPage
        val targetBoundaryOffset = pageBoundaryOffset(targetPage)
        pagerSnapDebugLog {
            "skipNonBoundaryAlign: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                "composeOffset=$composeOffset contentOffset=$contentOffset " +
                "nativePage=$nativePage targetPage=$targetPage pageGap=$pageGap " +
                "targetBoundaryOffset=$targetBoundaryOffset currentPage=$currentPage " +
                "anchorKey=$anchorKey keepAnchorKey=$keepAnchorKey"
        }
        if (!isPageBoundaryOffset(contentOffset) || targetBoundaryOffset != contentOffset) {
            val delta = targetBoundaryOffset - contentOffset
            if (delta != 0) {
                pagerSnapDebugLog {
                    "fixNativeOffsetToBoundary: stateId=$debugPagerStateId " +
                        "orientation=${layoutInfo.orientation} delta=$delta targetPage=$targetPage"
                }
                applyScrollViewOffsetDelta(delta)
            }
        }
        if (keepAnchorKey) {
            val reason = if (nativePage == trustedPage) {
                "alignBoundaryKeepKeySamePage"
            } else {
                "alignBoundaryKeepKeyPrependLag"
            }
            pagerSnapDebugLog {
                "$reason: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "nativePage=$nativePage currentPage=$trustedPage targetPage=$targetPage " +
                    "pageGap=$pageGap anchorKey=$anchorKey"
            }
            scrollPosition.requestPositionAndKeepKnownKey(
                targetPage,
                pageBoundaryOffsetFraction(targetPage),
                anchorKey
            )
        } else {
            val reason = if (anchorKey != null) {
                "alignBoundaryForgetKeyTrustNative"
            } else {
                "alignBoundaryForgetKeyNoAnchor"
            }
            pagerSnapDebugLog {
                "$reason: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "nativePage=$nativePage currentPage=$trustedPage targetPage=$targetPage " +
                    "pageGap=$pageGap anchorKey=$anchorKey"
            }
            scrollPosition.requestPositionAndForgetLastKnownKey(
                targetPage,
                pageBoundaryOffsetFraction(targetPage)
            )
        }
        kuiklyInfo.composeOffset = targetBoundaryOffset.toFloat()
        return true
    }

    private fun clearSnapTrackingAfterAlignment() {
        pagerSnapDebugLog {
            "clearSnapTrackingAfterAlignment: stateId=$debugPagerStateId " +
                "orientation=${layoutInfo.orientation} currentPage=$currentPage " +
                "contentOffset=${kuiklyInfo.contentOffset} " +
                "composeOffset=${currentAbsoluteScrollOffset().toInt()} " +
                "snapTarget=$snapTargetContentOffset snapStartPageCount=$snapStartPageCount " +
                "pageCount=$pageCount snapTargetPage=$snapTargetRelocatedPage " +
                "snapTargetKey=$snapTargetItemKey snapStartDesyncPages=$snapStartDesyncPages"
        }
        isSnapAnimating = false
        snapTargetContentOffset = 0
        snapStartPageCount = 0
        snapTargetItemKey = null
        snapTargetRelocatedPage = -1
        snapStartDesyncPages = 0
        snapTargetReachedAlignmentRequested = false
        snapStallAlignmentRetryRequested = false
        kuiklyInfo.snapAnchorOffsetCorrection = 0
    }

    private fun handleUnreachedSnapTarget(
        contentOffset: Int,
        scheduledContentOffset: Int,
        layoutSize: Int
    ): Boolean {
        if (!isSnapAnimating || hasSnapReachedTarget(contentOffset)) {
            snapStallAlignmentRetryRequested = false
            return false
        }

        if (contentOffset != scheduledContentOffset) {
            snapStallAlignmentRetryRequested = false
            pagerSnapDebugLog {
                "waitAlignDuringSnap: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "scheduledContentOffset=$scheduledContentOffset contentOffset=$contentOffset " +
                    "target=$snapTargetContentOffset currentPage=$currentPage"
            }
            scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
            return true
        }

        if (!snapStallAlignmentRetryRequested) {
            snapStallAlignmentRetryRequested = true
            pagerSnapDebugLog {
                "confirmSnapStall: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                    "contentOffset=$contentOffset target=$snapTargetContentOffset " +
                    "currentPage=$currentPage"
            }
            scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
            return true
        }

        val fallbackPage = if (snapTargetRelocatedPage in 0 until pageCount) {
            snapTargetRelocatedPage
        } else {
            nearestPageForOffset(snapTargetContentOffset)
        }
        val fallbackOffset = pageBoundaryOffset(fallbackPage)
        pagerSnapDebugLog {
            "fixInterruptedSnap: stateId=$debugPagerStateId orientation=${layoutInfo.orientation} " +
                "fallbackPage=$fallbackPage fallbackOffset=$fallbackOffset " +
                "contentOffset=$contentOffset target=$snapTargetContentOffset " +
                "currentPage=$currentPage firstVisiblePage=$firstVisiblePage"
        }
        updateScrollViewContentSize(layoutSize)
        val fallbackKey = snapTargetItemKey
        if (fallbackKey != null) {
            pagerSnapDebugLog {
                "fixInterruptedSnapKeepTargetKey: stateId=$debugPagerStateId " +
                    "orientation=${layoutInfo.orientation} fallbackPage=$fallbackPage " +
                    "fallbackKey=$fallbackKey pageCount=$pageCount " +
                    "snapStartPageCount=$snapStartPageCount snapStartDesyncPages=$snapStartDesyncPages"
            }
            scrollPosition.requestPositionAndKeepKnownKey(fallbackPage, 0f, fallbackKey)
        } else {
            scrollPosition.requestPositionAndForgetLastKnownKey(fallbackPage, 0f)
        }
        val delta = fallbackOffset - contentOffset
        if (delta != 0) {
            applyScrollViewOffsetDelta(delta)
        } else {
            kuiklyInfo.composeOffset = fallbackOffset.toFloat()
        }
        clearSnapTrackingAfterAlignment()
        return true
    }

    /**
     * Re-resolve the snap target index from its captured key. Must be called during measure
     * (where [itemProvider] is available) so that insertions/removals before the target during
     * the snap settle window shift the target to its new index instead of leaving it stale.
     */
    @OptIn(ExperimentalFoundationApi::class)
    internal fun relocateSnapTargetByKey(itemProvider: PagerLazyLayoutItemProvider) {
        if (!isSnapAnimating) {
            return
        }
        val key = snapTargetItemKey
        if (key == null) {
            pagerSnapDebugLog {
                "relocateSnapTargetByKeySkipped: stateId=$debugPagerStateId reason=noTargetKey " +
                    "currentPage=$currentPage pageCount=$pageCount itemCount=${itemProvider.itemCount} " +
                    "snapTargetPage=$snapTargetRelocatedPage"
            }
            return
        }
        if (snapTargetRelocatedPage < 0) {
            pagerSnapDebugLog {
                "relocateSnapTargetByKeySkipped: stateId=$debugPagerStateId reason=noTargetPage " +
                    "targetKey=$key currentPage=$currentPage pageCount=$pageCount " +
                    "itemCount=${itemProvider.itemCount}"
            }
            return
        }
        val oldIndex = snapTargetRelocatedPage
        val newIndex = itemProvider.findIndexByKey(key, snapTargetRelocatedPage)
        val oldIndexKey = if (oldIndex in 0 until itemProvider.itemCount) itemProvider.getKey(oldIndex) else null
        val newIndexKey = if (newIndex in 0 until itemProvider.itemCount) itemProvider.getKey(newIndex) else null
        pagerSnapDebugLog {
            "relocateSnapTargetByKey: stateId=$debugPagerStateId targetKey=$key " +
                "oldIndex=$oldIndex oldIndexKey=$oldIndexKey newIndex=$newIndex " +
                "newIndexKey=$newIndexKey pageCount=$pageCount itemCount=${itemProvider.itemCount} " +
                "currentPage=$currentPage"
        }
        if (newIndex != snapTargetRelocatedPage) {
            snapTargetRelocatedPage = newIndex
        }
    }

    private var programmaticScrollTargetPage by mutableIntStateOf(-1)

    private var settledPageState by mutableIntStateOf(currentPage)

    /**
     * The page that is currently "settled". This is an animation/gesture unaware page in the sense
     * that it will not be updated while the pages are being scrolled, but rather when the
     * animation/scroll settles.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val settledPage by derivedStateOf(structuralEqualityPolicy()) {
        if (isScrollInProgress) {
            settledPageState
        } else {
            this.currentPage
        }
    }

    /**
     * The page this [Pager] intends to settle to.
     * During fling or animated scroll (from [animateScrollToPage] this will represent the page
     * this pager intends to settle to. When no scroll is ongoing, this will be equal to
     * [currentPage].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val targetPage: Int by derivedStateOf(structuralEqualityPolicy()) {
        val finalPage = if (!isScrollInProgress) {
            this.currentPage
        } else if (programmaticScrollTargetPage != -1) {
            programmaticScrollTargetPage
        } else {
            // act on scroll only
            if (abs(this.currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                if (lastScrolledForward) {
                    firstVisiblePage + 1
                } else {
                    firstVisiblePage
                }
            } else {
                this.currentPage
            }
        }
        finalPage.coerceInPageRange()
    }

    /**
     * Indicates how far the current page is to the snapped position, this will vary from
     * -0.5 (page is offset towards the start of the layout) to 0.5 (page is offset towards the end
     * of the layout). This is 0.0 if the [currentPage] is in the snapped position. The value will
     * flip once the current page changes.
     *
     * This property is observable and shouldn't be used as is in a composable function due to
     * potential performance issues. To use it in the composition, please consider using a
     * derived state (e.g [derivedStateOf]) to only have recompositions when the derived
     * value changes.
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ObservingStateChangesInPagerStateSample
     */
    val currentPageOffsetFraction: Float get() = scrollPosition.currentPageOffsetFraction

//    internal val prefetchState = LazyLayoutPrefetchState(prefetchScheduler)

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll)
     * until layout is ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? by mutableStateOf(null)
        private set

    /**
     * The modifier which provides [remeasurement].
     */
    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@PagerState.remeasurement = remeasurement
        }
    }

    /**
     * Constraints passed to the prefetcher for premeasuring the prefetched items.
     */
    internal var premeasureConstraints = Constraints()

    /**
     * Stores currently pinned pages which are always composed, used by for beyond bound pages.
     */
    internal val pinnedPages = LazyLayoutPinnedItemList()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Scroll (jump immediately) to a given [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.ScrollToPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     */
    suspend fun scrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f
    ) = scroll {
        clearSnapAnimationState()
        debugLog { "Scroll from page=$currentPage to page=$page" }
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        snapToItem(targetPage, pageOffsetFraction, forceRemeasure = true)
    }

//    /**
//     * Jump immediately to a given [page] with a given [pageOffsetFraction] inside
//     * a [ScrollScope]. Use this method to create custom animated scrolling experiences. This will
//     * update the value of [currentPage] and [currentPageOffsetFraction] immediately, but can only
//     * be used inside a [ScrollScope], use [scroll] to gain access to a [ScrollScope].
//     *
//     * Please refer to the sample to learn how to use this API.
//     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
//     *
//     * @param page The destination page to scroll to
//     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
//     * destination page will be offset from its snapped position.
//     */
//    @ExperimentalFoundationApi
//    fun ScrollScope.updateCurrentPage(
//        page: Int,
//        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f
//    ) {
//        snapToItem(page, pageOffsetFraction, forceRemeasure = true)
//    }

//    /**
//     * Used to update [targetPage] during a programmatic scroll operation. This can only be called
//     * inside a [ScrollScope] and should be called anytime a custom scroll (through [scroll]) is
//     * executed in order to correctly update [targetPage]. This will not move the pages and it's
//     * still the responsibility of the caller to call [ScrollScope.scrollBy] in order to actually
//     * get to [targetPage]. By the end of the [scroll] block, when the [Pager] is no longer
//     * scrolling [targetPage] will assume the value of [currentPage].
//     *
//     * Please refer to the sample to learn how to use this API.
//     * @sample androidx.compose.foundation.samples.PagerCustomAnimateScrollToPage
//     */
//    @ExperimentalFoundationApi
//    fun ScrollScope.updateTargetPage(targetPage: Int) {
//        programmaticScrollTargetPage = targetPage.coerceInPageRange()
//    }

    internal fun snapToItem(page: Int, offsetFraction: Float, forceRemeasure: Boolean) {
        val distance = animatedScrollScope.calculateDistanceTo(page) + offsetFraction * pageSizeWithSpacing
        dispatchRawDelta(distance)
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the [page] to be at the snapped position during the next remeasure,
     * offset by [pageOffsetFraction], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain
     * the index based on the current page key (when a data set change will also be
     * applied during the next remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param page the index to which to scroll. Must be non-negative.
     * @param pageOffsetFraction the offset fraction that the page should end up after the scroll.
     */
    fun requestScrollToPage(
        @AndroidXIntRange(from = 0) page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f
    ) {
        clearSnapAnimationState()
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            pagerLayoutInfoState.value.coroutineScope.launch {
                stopScroll()
            }
        }
        snapToItem(page.coerceInPageRange(), pageOffsetFraction, forceRemeasure = false)
    }

    /**
     * Scroll animate to a given [page]. If the [page] is too far away from [currentPage] we will
     * not compose all pages in the way. We will pre-jump to a nearer page, compose and animate
     * the rest of the pages until [page].
     *
     * Please refer to the sample to learn how to use this API.
     * @sample androidx.compose.foundation.samples.AnimateScrollPageSample
     *
     * @param page The destination page to scroll to
     * @param pageOffsetFraction A fraction of the page size that indicates the offset the
     * destination page will be offset from its snapped position.
     * @param animationSpec An [AnimationSpec] to move between pages. We'll use a [spring] as the
     * default animation.
     */
    suspend fun animateScrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring()
    ) {
        if (page == currentPage && currentPageOffsetFraction == pageOffsetFraction ||
            pageCount == 0
        ) return
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        val targetPageOffsetToSnappedPosition =
            (pageOffsetFraction * pageSizeWithSpacing)
        val distance: Float = animatedScrollScope.calculateDistanceTo(targetPage) + targetPageOffsetToSnappedPosition
        var targetOffset = distance + kuiklyInfo.contentOffset
        if (targetOffset < 0) {
            targetOffset = minScrollOffset.toFloat()
        } else if (targetOffset + kuiklyInfo.viewportSize > kuiklyInfo.currentContentSize) {
            targetOffset = maxScrollOffset.toFloat()
        }
        
        // Calculate initial and target offsets (for SpringSpec duration calculation)
        val initialOffset = kuiklyInfo.contentOffset.toFloat()
        val finalTargetOffset = targetOffset
        
        // Convert AnimationSpec to SpringAnimation
        val springAnimation = convertAnimationSpecToSpringAnimation(
            animationSpec = animationSpec,
            initialValue = initialOffset,
            targetValue = finalTargetOffset
        )

        markSnapAnimationStarted(finalTargetOffset.toInt())

        kuiklyInfo.run {
            val targetOffsetDp = if (isVertical()) {
                Offset(scrollView?.curOffsetX ?: 0f, max(0f, targetOffset / getDensity() - 0.01f))
            } else {
                Offset(max(0f, targetOffset / getDensity() - 0.01f), scrollView?.curOffsetY ?: 0f)
            }
            scrollView?.setContentOffset(targetOffsetDp.x, targetOffsetDp.y, true, springAnimation)
        }
//
//        animatedScrollScope.animateScrollToPage(
//            targetPage,
//            targetPageOffsetToSnappedPosition,
//            animationSpec,
//            updateTargetPage = { updateTargetPage(it) }
//        )
    }

    private suspend fun awaitScrollDependencies() {
        awaitLayoutModifier.waitForFirstLayout()
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitScrollDependencies()
        // will scroll and it's not scrolling already update settled page
        if (!isScrollInProgress) {
            settledPageState = currentPage
        }
        scrollableState.scroll(scrollPriority, block)
        programmaticScrollTargetPage = -1 // reset animated scroll target page indicator
    }

    override fun dispatchRawDelta(delta: Float): Float {
        return scrollableState.dispatchRawDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    final override var canScrollForward: Boolean by mutableStateOf(false)
        private set
    final override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    private val isLastScrollForwardState = mutableStateOf(false)
    private val isLastScrollBackwardState = mutableStateOf(false)

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = isLastScrollForwardState.value

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = isLastScrollBackwardState.value

    /**
     *  Updates the state with the new calculated scroll position and consumed scroll.
     */
    internal fun applyMeasureResult(
        result: PagerMeasureResult,
        visibleItemsStayedTheSame: Boolean = false
    ) {
        debugLog { "Applying Measure Result" }
        // Hook: record page scroll context event for Recomposition Profiler
        val oldPage = scrollPosition.currentPage
        if (visibleItemsStayedTheSame) {
            scrollPosition.updateCurrentPageOffsetFraction(result.currentPageOffsetFraction)
        } else {
            scrollPosition.updateFromMeasureResult(result)
            cancelPrefetchIfVisibleItemsChanged(result)
        }
        if (RecompositionProfiler.isEnabled) {
            val newPage = scrollPosition.currentPage
            if (newPage != oldPage) {
                RecompositionProfiler.recordScrollContext(
                    listId = "pager_${identityHashCode(this)}",
                    from = oldPage,
                    to = newPage,
                    visibleItemCount = result.visiblePagesInfo.size
                )
            }
        }
        pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = result.canScrollBackward
        result.firstVisiblePage?.let { firstVisiblePage = it.index }
        firstVisiblePageOffset = result.firstVisiblePageScrollOffset
        tryRunPrefetch(result)
        maxScrollOffset = result.calculateNewMaxScrollOffset(pageCount)
        minScrollOffset = result.calculateNewMinScrollOffset(pageCount)
        val layoutSize = if (result.orientation == Orientation.Horizontal)
            result.viewportSize.width else result.viewportSize.height
        debugLog {
            "Finished Applying Measure Result" +
                "\nNew maxScrollOffset=$maxScrollOffset"
        }

        updateAlignmentLayoutGeneration(result.orientation, layoutSize)

        scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
    }

    private fun updateAlignmentLayoutGeneration(orientation: Orientation, layoutSize: Int) {
        val currentPageSizeWithSpacing = pageSizeWithSpacing
        val changed = lastAlignmentOrientation != orientation ||
            lastAlignmentLayoutSize != layoutSize ||
            lastAlignmentPageSizeWithSpacing != currentPageSizeWithSpacing
        if (changed) {
            alignmentLayoutGeneration += 1
            pagerSnapDebugLog {
                "layoutGenerationChanged: stateId=$debugPagerStateId " +
                    "generation=$alignmentLayoutGeneration " +
                    "oldOrientation=$lastAlignmentOrientation newOrientation=$orientation " +
                    "oldLayoutSize=$lastAlignmentLayoutSize newLayoutSize=$layoutSize " +
                    "oldPageSizeWithSpacing=$lastAlignmentPageSizeWithSpacing " +
                    "newPageSizeWithSpacing=$currentPageSizeWithSpacing"
            }
            lastAlignmentOrientation = orientation
            lastAlignmentLayoutSize = layoutSize
            lastAlignmentPageSizeWithSpacing = currentPageSizeWithSpacing
        }
    }

    private fun tryRunPrefetch(result: PagerMeasureResult) = Snapshot.withoutReadObservation {
        if (abs(previousPassDelta) > 0.5f) {
            if (prefetchingEnabled && isGestureActionMatchesScroll(previousPassDelta)) {
                notifyPrefetch(previousPassDelta, result)
            }
        }
    }

    private fun Int.coerceInPageRange() = if (pageCount > 0) {
        coerceIn(0, pageCount - 1)
    } else {
        0
    }

    // check if the scrolling will be a result of a fling operation. That is, if the scrolling
    // direction is in the opposite direction of the gesture movement. Also, return true if there
    // is no applied gesture that causes the scrolling
    private fun isGestureActionMatchesScroll(scrollDelta: Float): Boolean =
        if (layoutInfo.orientation == Orientation.Vertical) {
            sign(scrollDelta) == sign(-upDownDifference.y)
        } else {
            sign(scrollDelta) == sign(-upDownDifference.x)
        } || isNotGestureAction()

    internal fun isNotGestureAction(): Boolean =
        upDownDifference.x.toInt() == 0 && upDownDifference.y.toInt() == 0

    private fun notifyPrefetch(delta: Float, info: PagerLayoutInfo) {
        if (!prefetchingEnabled) {
            return
        }

        if (info.visiblePagesInfo.isNotEmpty()) {
            val isPrefetchingForward = delta > 0
            val indexToPrefetch = if (isPrefetchingForward) {
                info.visiblePagesInfo.last().index + info.beyondViewportPageCount + PagesToPrefetch
            } else {
                info.visiblePagesInfo.first().index - info.beyondViewportPageCount - PagesToPrefetch
            }
            if (indexToPrefetch in 0 until pageCount) {
                if (indexToPrefetch != this.indexToPrefetch) {
                    if (wasPrefetchingForward != isPrefetchingForward) {
                        // the scrolling direction has been changed which means the last prefetched
                        // is not going to be reached anytime soon so it is safer to dispose it.
                        // if this item is already visible it is safe to call the method anyway
                        // as it will be no-op
//                        currentPrefetchHandle?.cancel()
                    }
                    this.wasPrefetchingForward = isPrefetchingForward
                    this.indexToPrefetch = indexToPrefetch
//                    currentPrefetchHandle = prefetchState.schedulePrefetch(
//                        indexToPrefetch, premeasureConstraints
//                    )
                }
                if (isPrefetchingForward) {
                    val lastItem = info.visiblePagesInfo.last()
                    val pageSize = info.pageSize + info.pageSpacing
                    val distanceToReachNextItem =
                        lastItem.offset + pageSize - info.viewportEndOffset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToReachNextItem < delta) {
//                        currentPrefetchHandle?.markAsUrgent()
                    }
                } else {
                    val firstItem = info.visiblePagesInfo.first()
                    val distanceToReachNextItem = info.viewportStartOffset - firstItem.offset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToReachNextItem < -delta) {
//                        currentPrefetchHandle?.markAsUrgent()
                    }
                }
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: PagerLayoutInfo) {
        if (indexToPrefetch != -1 && info.visiblePagesInfo.isNotEmpty()) {
            val expectedPrefetchIndex = if (wasPrefetchingForward) {
                info.visiblePagesInfo.last().index + info.beyondViewportPageCount + PagesToPrefetch
            } else {
                info.visiblePagesInfo.first().index - info.beyondViewportPageCount - PagesToPrefetch
            }
            if (indexToPrefetch != expectedPrefetchIndex) {
                indexToPrefetch = -1
//                currentPrefetchHandle?.cancel()
//                currentPrefetchHandle = null
            }
        }
    }

    /**
     * An utility function to help to calculate a given page's offset. This is an offset that
     * represents how far [page] is from the settled position (represented by [currentPage]
     * offset). The difference here is that [currentPageOffsetFraction] is a value between -0.5 and
     * 0.5 and the value calculated by this function can be larger than these numbers if [page] is
     * different than [currentPage].
     *
     * For instance, if currentPage=0 and we call [getOffsetDistanceInPages] for page 3, the result
     * will be 3, meaning the given page is 3 pages away from the current page (the sign represent
     * the direction of the offset, positive is forward, negative is backwards). Another example is
     * if currentPage=3 and we call [getOffsetDistanceInPages] for page 1, the result would be -2,
     * meaning we're 2 pages away (moving backwards) to the current page.
     *
     * This offset also works in conjunction with [currentPageOffsetFraction], so if [currentPage]
     * is out of its snapped position (i.e. currentPageOffsetFraction!=0) then the calculated value
     * will still represent the offset in number of pages (in this case, not whole pages).
     * For instance, if currentPage=1 and we're slightly offset, currentPageOffsetFraction=0.2,
     * if we call this to page 2, the result would be 0.8, that is 0.8 page away from current page
     * (moving forward).
     *
     * @param page The page to calculate the offset from. This should be between 0 and [pageCount].
     * @return The offset of [page] with respect to [currentPage].
     */
    fun getOffsetDistanceInPages(page: Int): Float {
        require(page in 0..pageCount) {
            "page $page is not within the range 0 to $pageCount"
        }
        return page - currentPage - currentPageOffsetFraction
    }

    /**
     * When the user provided custom keys for the pages we can try to detect when there were
     * pages added or removed before our current page and keep this page as the current one
     * given that its index has been changed.
     */
    internal fun matchScrollPositionWithKey(
        itemProvider: PagerLazyLayoutItemProvider,
        currentPage: Int = Snapshot.withoutReadObservation { scrollPosition.currentPage }
    ): Int = scrollPosition.matchPageWithKey(itemProvider, currentPage)
}

internal suspend fun PagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

internal suspend fun PagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

internal val DefaultPositionThreshold = 56.dp
private const val MaxPagesForAnimateScroll = 3
internal const val PagesToPrefetch = 1

internal val EmptyLayoutInfo = PagerMeasureResult(
    visiblePagesInfo = emptyList(),
    pageSize = 0,
    pageSpacing = 0,
    afterContentPadding = 0,
    orientation = Orientation.Horizontal,
    viewportStartOffset = 0,
    viewportEndOffset = 0,
    reverseLayout = false,
    beyondViewportPageCount = 0,
    positionedPages = emptyList(),
    firstVisiblePage = null,
    firstVisiblePageScrollOffset = 0,
    currentPage = null,
    currentPageOffsetFraction = 0.0f,
    canScrollForward = false,
    snapPosition = SnapPosition.Start,
    measureResult = object : MeasureResult {
        override val width: Int = 0

        override val height: Int = 0

        @Suppress("PrimitiveInCollection")
        override val alignmentLines: Map<AlignmentLine, Int> = fastMutableMapOf()

        override fun placeChildren() {}
    },
    remeasureNeeded = false,
    coroutineScope = CoroutineScope(EmptyCoroutineContext)
)

private val UnitDensity = object : Density {
    override val density: Float = 1f
    override val fontScale: Float = 1f
}

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.PagerState) {
        println("PagerState: ${generateMsg()}")
    }
}

/** Tolerance (px) when comparing native offset to [PagerState.snapTargetContentOffset]. */
private const val SNAP_TARGET_OFFSET_TOLERANCE = 1

/** Initial delay before checking native/compose offset alignment after measure. */
private const val SNAP_MEASURE_JOB_INITIAL_DELAY_MS = 50L

internal fun PagerLayoutInfo.calculateNewMaxScrollOffset(pageCount: Int): Long {
    val pageSizeWithSpacing = pageSpacing + pageSize
    val maxScrollPossible =
        (pageCount.toLong()) * pageSizeWithSpacing + beforeContentPadding + afterContentPadding - pageSpacing
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    /**
     * We need to take into consideration the snap position for max scroll position.
     * For instance, if SnapPosition.Start, the max scroll position is
     * pageCount * pageSize - viewport. Now if SnapPosition.End, it should be pageCount * pageSize.
     * Therefore, the snap position discount varies between 0 and viewport.
     */
    val snapPositionDiscount = layoutSize - (snapPosition.position(
        layoutSize = layoutSize,
        itemSize = pageSize,
        itemIndex = pageCount - 1,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        itemCount = pageCount
    )).coerceIn(0, layoutSize)

    debugLog {
        "maxScrollPossible=$maxScrollPossible" +
            "\nsnapPositionDiscount=$snapPositionDiscount" +
            "\nlayoutSize=$layoutSize"
    }
    return (maxScrollPossible - snapPositionDiscount).coerceAtLeast(0L)
}

private fun PagerMeasureResult.calculateNewMinScrollOffset(pageCount: Int): Long {
    val layoutSize =
        if (orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height

    return snapPosition.position(
        layoutSize = layoutSize,
        itemSize = pageSize,
        itemIndex = 0,
        beforeContentPadding = beforeContentPadding,
        afterContentPadding = afterContentPadding,
        itemCount = pageCount
    ).coerceIn(0, layoutSize).toLong()
}

//@OptIn(ExperimentalFoundationApi::class)
//private suspend fun LazyLayoutAnimateScrollScope.animateScrollToPage(
//    targetPage: Int,
//    targetPageOffsetToSnappedPosition: Float,
//    animationSpec: AnimationSpec<Float>,
//    updateTargetPage: ScrollScope.(Int) -> Unit
//) {
//    scroll {
//        updateTargetPage(targetPage)
//        val forward = targetPage > firstVisibleItemIndex
//        val visiblePages = lastVisibleItemIndex - firstVisibleItemIndex + 1
//        if (((forward && targetPage > lastVisibleItemIndex) ||
//                (!forward && targetPage < firstVisibleItemIndex)) &&
//            abs(targetPage - firstVisibleItemIndex) >= MaxPagesForAnimateScroll
//        ) {
//            val preJumpPosition = if (forward) {
//                (targetPage - visiblePages).coerceAtLeast(firstVisibleItemIndex)
//            } else {
//                (targetPage + visiblePages).coerceAtMost(firstVisibleItemIndex)
//            }
//
//            debugLog {
//                "animateScrollToPage with pre-jump to position=$preJumpPosition"
//            }
//
//            // Pre-jump to 1 viewport away from destination page, if possible
//            snapToItem(preJumpPosition, 0)
//        }
//
//        // The final delta displacement will be the difference between the pages offsets
//        // discounting whatever offset the original page had scrolled plus the offset
//        // fraction requested by the user.
//        val displacement = calculateDistanceTo(targetPage) + targetPageOffsetToSnappedPosition
//
//        debugLog { "animateScrollToPage $displacement pixels" }
//        var previousValue = 0f
//        animate(0f, displacement, animationSpec = animationSpec) { currentValue, _ ->
//            val delta = currentValue - previousValue
//            val consumed = scrollBy(delta)
//            debugLog { "Dispatched Delta=$delta Consumed=$consumed" }
//            previousValue += consumed
//        }
//    }
//}
