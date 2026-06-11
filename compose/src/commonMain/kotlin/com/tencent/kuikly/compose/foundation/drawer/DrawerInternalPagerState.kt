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

import androidx.annotation.FloatRange
import androidx.annotation.IntRange as AndroidXIntRange
import com.tencent.kuikly.compose.animation.core.AnimationSpec
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
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutItemProvider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import com.tencent.kuikly.compose.foundation.gestures.snapping.SnapPosition
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutNearestRangeState
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.layout.AlignmentLine
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.Remeasurement
import com.tencent.kuikly.compose.ui.layout.RemeasurementModifier
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.util.fastFirstOrNull
import com.tencent.kuikly.compose.scroller.applyScrollViewOffsetDelta
import com.tencent.kuikly.compose.scroller.convertAnimationSpecToSpringAnimation
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.compose.material3.internal.identityHashCode
import com.tencent.kuikly.core.collection.fastMutableMapOf
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Creates and remembers a [DrawerInternalPagerState] to be used with [DrawerHorizontalPager]
 * or [DrawerVerticalPager].
 *
 * @param initialPage The page that should be shown first.
 * @param initialPageOffsetFraction The offset of the initial page as a fraction of the page size.
 * @param pageSizeProvider A function that returns the main-axis pixel size for a given page index.
 * @param pageCount The amount of pages this Pager will have.
 */
@Composable
fun rememberDrawerInternalPagerState(
    initialPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) initialPageOffsetFraction: Float = 0f,
    pageSizeProvider: Density.(pageIndex: Int, availableSpace: Int) -> Int,
    pageCount: () -> Int
): DrawerInternalPagerState {
    return remember {
        DefaultDrawerInternalPagerState(
            initialPage,
            initialPageOffsetFraction,
            pageCount,
            DrawerSizeTracker(pageSizeProvider)
        )
    }.apply {
        pageCountState.value = pageCount
        sizeTracker.pageSizeProvider = pageSizeProvider
        sizeTracker.invalidate()
    }
}

private class DefaultDrawerInternalPagerState(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    updatedPageCount: () -> Int,
    tracker: DrawerSizeTracker
) : DrawerInternalPagerState(currentPage, currentPageOffsetFraction, tracker) {

    var pageCountState = mutableStateOf(updatedPageCount)
    override val pageCount: Int get() = pageCountState.value.invoke()

    companion object {
        val Saver: Saver<DefaultDrawerInternalPagerState, *> = listSaver(
            save = {
                listOf(
                    it.currentPage,
                    it.currentPageOffsetFraction.coerceIn(MinPageOffset, MaxPageOffset),
                    it.pageCount,
                    it.kuiklyInfo.composeOffset.toInt(),
                    it.kuiklyInfo.currentContentSize,
                    it.kuiklyInfo.contentOffset,
                    if (it.kuiklyInfo.offsetDirty) 1 else 0,
                )
            },
            restore = {
                DefaultDrawerInternalPagerState(
                    currentPage = it[0] as Int,
                    currentPageOffsetFraction = it[1] as Float,
                    updatedPageCount = { it[2] as Int },
                    tracker = DrawerSizeTracker { _, availableSpace -> availableSpace }
                ).also { state ->
                    if (it.size > 3) {
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
 * The state that can be used to control [DrawerHorizontalPager] and [DrawerVerticalPager].
 * Supports per-page variable sizes via [DrawerSizeTracker].
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
abstract class DrawerInternalPagerState internal constructor(
    currentPage: Int = 0,
    @FloatRange(from = -0.5, to = 0.5) currentPageOffsetFraction: Float = 0f,
    tracker: DrawerSizeTracker
) : ScrollableState {

    abstract val pageCount: Int

    internal var sizeTracker: DrawerSizeTracker = tracker

    init {
        require(currentPageOffsetFraction in -0.5..0.5) {
            "currentPageOffsetFraction $currentPageOffsetFraction is not within the range -0.5 to 0.5"
        }
    }

    override var contentPadding: PaddingValues = PaddingValues(0.dp)

    internal var upDownDifference: Offset by mutableStateOf(Offset.Zero)
    private val animatedScrollScope = DrawerLazyAnimateScrollScope(this)

    internal val debugPagerStateId = identityHashCode(this)

    internal val scrollPosition = DrawerScrollPosition(currentPage, currentPageOffsetFraction, this)

    internal var firstVisiblePage = currentPage
        private set

    internal var firstVisiblePageOffset = 0
        private set

    internal var maxScrollOffset: Long = Long.MAX_VALUE
    internal var minScrollOffset: Long = 0L

    private var alignmentLayoutGeneration = 0
    private var lastAlignmentOrientation: Orientation? = null
    private var lastAlignmentLayoutSize = 0

    private var accumulator: Float = 0.0f
    private var previousPassDelta = 0f

    internal val scrollableState = ScrollableState { performScroll(it) }

    // --- Per-page size accessors ---

    internal var mainAxisAvailableSpace: Int = 0
    internal var spaceBetweenPages: Int = 0

    internal var density: Density = UnitDensity

    internal fun pageSizeForPage(page: Int): Int {
        return sizeTracker.getPageSize(page)
    }

    internal fun pageSizeWithSpacingForPage(page: Int): Int {
        return sizeTracker.getPageSizeWithSpacing(page)
    }

    /** Uniform fallback used for layout info interface. */
    internal val pageSize: Int
        get() = pagerLayoutInfoState.value.pageSize

    internal val pageSpacing: Int
        get() = pagerLayoutInfoState.value.pageSpacing

    /** Uses the current page's size for threshold. */
    internal val positionThresholdFraction: Float
        get() {
            val currentSize = pageSizeForPage(currentPage)
            if (currentSize == 0) return 0f
            return with(density) {
                val minThreshold = minOf(DefaultPositionThreshold.toPx(), currentSize / 2f)
                minThreshold / currentSize.toFloat()
            }
        }

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    val interactionSource: InteractionSource
        get() = internalInteractionSource

    val currentPage: Int get() = scrollPosition.currentPage

    /**
     * When true, [applyMeasureResult] will synchronously fix the native scroll offset
     * instead of waiting for the delayed alignment job. Set by [rememberMoveableDrawerState]
     * when the drawer configuration (fullScreen / drawerWidth) changes, so that the
     * native scroll position is corrected before the frame is drawn.
     */
    internal var needsImmediateAlignment = false

    internal var isSnapAnimating = false
    internal var snapTargetContentOffset = 0
    internal var snapStartPageCount = 0
    internal var snapTargetItemKey: Any? = null
    internal var snapTargetRelocatedPage = -1
    internal var snapStartDesyncPages = 0
    private var snapTargetReachedAlignmentRequested = false
    private var snapStallAlignmentRetryRequested = false

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
    }

    internal fun hasSnapReachedTarget(contentOffset: Int): Boolean {
        return abs(contentOffset - snapTargetContentOffset) <= SNAP_TARGET_OFFSET_TOLERANCE
    }

    /**
     * Detects unexpected native scroll offset jumps (e.g. HarmonyOS HandleCrashTop()
     * resetting the horizontal offset to 0). Returns true only when the native offset
     * has been reset to near-zero while compose is settled on a distant page boundary,
     * which is the specific signature of HandleCrashTop boundary correction.
     *
     * This does NOT reject gradual offset changes from user swipe gestures, which
     * produce incremental deltas even when starting from a page boundary.
     */
    internal fun shouldRejectNativeScrollOffset(newOffset: Int): Boolean {
        if (isScrollInProgress || isSnapAnimating) return false
        val composeOffset = currentAbsoluteScrollOffset().toInt()
        if (!isPageBoundaryOffset(composeOffset)) return false
        // Only reject if native offset jumped to near-zero (HandleCrashTop signature)
        // while compose is on a non-zero page boundary. User swipes produce gradual
        // offset changes (e.g. 795→779→751→...), never an instantaneous jump to 0.
        val page0Offset = pageBoundaryOffset(0).toInt()
        val isNativeAtPage0 = abs(newOffset - page0Offset) <= SNAP_TARGET_OFFSET_TOLERANCE
        val isComposeAwayFromPage0 = abs(composeOffset - page0Offset) > SNAP_TARGET_OFFSET_TOLERANCE
        return isNativeAtPage0 && isComposeAwayFromPage0
    }

    internal fun onNativeContentOffsetChanged(contentOffset: Int) {
        if (!isSnapAnimating || snapTargetReachedAlignmentRequested) return
        if (!hasSnapReachedTarget(contentOffset)) return
        snapTargetReachedAlignmentRequested = true
        scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS)
    }

    internal fun clearSnapAnimationState() {
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

    // --- Scroll position (using prefix sums) ---

    internal fun currentAbsoluteScrollOffset(): Long {
        val baseOffset = sizeTracker.getOffsetForPage(currentPage)
        val currentPageSizeWS = pageSizeWithSpacingForPage(currentPage)
        return baseOffset + (currentPageOffsetFraction * currentPageSizeWS).roundToLong()
    }

    internal fun nearestPageForOffset(offset: Int): Int {
        return sizeTracker.getPageForOffset(offset.toLong()).first.coerceInPageRange()
    }

    internal fun pageBoundaryOffset(page: Int): Long {
        return sizeTracker.getOffsetForPage(page.coerceInPageRange())
    }

    private fun isPageBoundaryOffset(offset: Int): Boolean {
        val nearest = nearestPageForOffset(offset)
        val boundaryOffset = pageBoundaryOffset(nearest).toInt()
        return abs(boundaryOffset - offset) <= SNAP_TARGET_OFFSET_TOLERANCE
    }

    // --- Scroll logic ---

    private fun performScroll(delta: Float): Float {
        val currentScrollPosition = currentAbsoluteScrollOffset()

        val decimalAccumulation = (delta + accumulator)
        val decimalAccumulationInt = decimalAccumulation.roundToLong()
        accumulator = decimalAccumulation - decimalAccumulationInt

        if (delta.absoluteValue < 1e-4f) return delta

        val updatedScrollPosition = (currentScrollPosition + decimalAccumulationInt)
        val coercedScrollPosition = updatedScrollPosition.coerceIn(minScrollOffset, maxScrollOffset)
        val changed = updatedScrollPosition != coercedScrollPosition
        val scrollDelta = coercedScrollPosition - currentScrollPosition

        previousPassDelta = scrollDelta.toFloat()

        if (scrollDelta.absoluteValue != 0L) {
            isLastScrollForwardState.value = scrollDelta > 0.0f
            isLastScrollBackwardState.value = scrollDelta < 0.0f
        }

        val layoutInfo = pagerLayoutInfoState.value
        if (layoutInfo.tryToApplyScrollWithoutRemeasure(-scrollDelta.toInt())) {
            applyMeasureResult(result = layoutInfo, visibleItemsStayedTheSame = true)
            placementScopeInvalidator.invalidateScope()
        } else {
            scrollPosition.applyScrollDelta(scrollDelta.toInt())
            remeasurement?.forceRemeasure()
        }

        return (if (changed) scrollDelta else delta).toFloat()
    }

    internal var prefetchingEnabled: Boolean = true
    private var indexToPrefetch = -1
    private var wasPrefetchingForward = false

    private var pagerLayoutInfoState =
        mutableStateOf(DrawerEmptyLayoutInfo, neverEqualPolicy())

    val layoutInfo: PagerLayoutInfo get() = pagerLayoutInfoState.value

    private var programmaticScrollTargetPage by mutableIntStateOf(-1)
    private var settledPageState by mutableIntStateOf(currentPage)

    val settledPage by derivedStateOf(structuralEqualityPolicy()) {
        if (isScrollInProgress) settledPageState else currentPage
    }

    val targetPage: Int by derivedStateOf(structuralEqualityPolicy()) {
        val finalPage = if (!isScrollInProgress) {
            currentPage
        } else if (programmaticScrollTargetPage != -1) {
            programmaticScrollTargetPage
        } else {
            if (abs(currentPageOffsetFraction) >= abs(positionThresholdFraction)) {
                if (lastScrolledForward) firstVisiblePage + 1 else firstVisiblePage
            } else {
                currentPage
            }
        }
        finalPage.coerceInPageRange()
    }

    val currentPageOffsetFraction: Float get() = scrollPosition.currentPageOffsetFraction

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    internal var remeasurement: Remeasurement? by mutableStateOf(null)
        private set

    internal val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@DrawerInternalPagerState.remeasurement = remeasurement
        }
    }

    internal var premeasureConstraints = Constraints()
    internal val pinnedPages = LazyLayoutPinnedItemList()
    internal val nearestRange: IntRange by scrollPosition.nearestRangeState
    internal val placementScopeInvalidator = ObservableScopeInvalidator()
    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    suspend fun scrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f
    ) = scroll {
        clearSnapAnimationState()
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        snapToItem(targetPage, pageOffsetFraction, forceRemeasure = true)
    }

    internal fun snapToItem(page: Int, offsetFraction: Float, forceRemeasure: Boolean) {
        val pageSizeWS = pageSizeWithSpacingForPage(page)
        val distance = animatedScrollScope.calculateDistanceTo(page) + offsetFraction * pageSizeWS
        dispatchRawDelta(distance)
    }

    fun requestScrollToPage(
        @AndroidXIntRange(from = 0) page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0.0f
    ) {
        clearSnapAnimationState()
        if (isScrollInProgress) {
            pagerLayoutInfoState.value.coroutineScope.launch { stopScroll() }
        }
        snapToItem(page.coerceInPageRange(), pageOffsetFraction, forceRemeasure = false)
    }

    suspend fun animateScrollToPage(
        page: Int,
        @FloatRange(from = -0.5, to = 0.5) pageOffsetFraction: Float = 0f,
        animationSpec: AnimationSpec<Float> = spring()
    ) {
        if (page == currentPage && currentPageOffsetFraction == pageOffsetFraction || pageCount == 0) return
        awaitScrollDependencies()
        require(pageOffsetFraction in -0.5..0.5) {
            "pageOffsetFraction $pageOffsetFraction is not within the range -0.5 to 0.5"
        }
        val targetPage = page.coerceInPageRange()
        val pageSizeWS = pageSizeWithSpacingForPage(targetPage)
        val targetPageOffsetToSnappedPosition = (pageOffsetFraction * pageSizeWS)
        val distance: Float = animatedScrollScope.calculateDistanceTo(targetPage) + targetPageOffsetToSnappedPosition
        var targetOffset = distance + kuiklyInfo.contentOffset
        if (targetOffset < 0) {
            targetOffset = minScrollOffset.toFloat()
        } else if (targetOffset + kuiklyInfo.viewportSize > kuiklyInfo.currentContentSize) {
            targetOffset = maxScrollOffset.toFloat()
        }

        val finalTargetOffset = targetOffset

        markSnapAnimationStarted(finalTargetOffset.toInt())

        kuiklyInfo.run {
            val targetOffsetDp = if (isVertical()) {
                Offset(scrollView?.curOffsetX ?: 0f, max(0f, targetOffset / getDensity() - 0.01f))
            } else {
                Offset(max(0f, targetOffset / getDensity() - 0.01f), scrollView?.curOffsetY ?: 0f)
            }
            scrollView?.setContentOffset(targetOffsetDp.x, targetOffsetDp.y, true)
        }
    }

    private suspend fun awaitScrollDependencies() {
        awaitLayoutModifier.waitForFirstLayout()
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitScrollDependencies()
        if (!isScrollInProgress) settledPageState = currentPage
        scrollableState.scroll(scrollPriority, block)
        programmaticScrollTargetPage = -1
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

    internal fun applyMeasureResult(
        result: PagerMeasureResult,
        visibleItemsStayedTheSame: Boolean = false
    ) {
        val oldPage = scrollPosition.currentPage
        if (visibleItemsStayedTheSame) {
            scrollPosition.updateCurrentPageOffsetFraction(result.currentPageOffsetFraction)
        } else {
            scrollPosition.updateFromMeasureResult(result)
            cancelPrefetchIfVisibleItemsChanged(result)
        }
        pagerLayoutInfoState.value = result
        canScrollForward = result.canScrollForward
        canScrollBackward = result.canScrollBackward
        result.firstVisiblePage?.let { firstVisiblePage = it.index }
        firstVisiblePageOffset = result.firstVisiblePageScrollOffset
        tryRunPrefetch(result)
        maxScrollOffset = calculateNewMaxScrollOffset(result)
        minScrollOffset = calculateNewMinScrollOffset(result)

        val layoutSize = if (result.orientation == Orientation.Horizontal)
            result.viewportSize.width else result.viewportSize.height
        updateAlignmentLayoutGeneration(result.orientation, layoutSize)

        if (needsImmediateAlignment) {
            needsImmediateAlignment = false
            performImmediateAlignment(layoutSize)
            // Skip the delayed alignment — the immediate fix is sufficient and the
            // delayed job would see stale contentOffset (not yet updated by native
            // callback) and might double-correct the scroll position.
        } else {
            scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
        }
    }

    // --- Max/min scroll offset using prefix sums ---

    private fun calculateNewMaxScrollOffset(result: PagerLayoutInfo): Long {
        val totalContent = sizeTracker.getTotalContentSize() +
            result.beforeContentPadding + result.afterContentPadding
        val layoutSize = if (result.orientation == Orientation.Horizontal)
            result.viewportSize.width else result.viewportSize.height

        val snapPositionDiscount = layoutSize - (result.snapPosition.position(
            layoutSize = layoutSize,
            itemSize = pageSizeForPage((pageCount - 1).coerceAtLeast(0)),
            itemIndex = pageCount - 1,
            beforeContentPadding = result.beforeContentPadding,
            afterContentPadding = result.afterContentPadding,
            itemCount = pageCount
        )).coerceIn(0, layoutSize)

        return (totalContent - snapPositionDiscount).coerceAtLeast(0L)
    }

    private fun calculateNewMinScrollOffset(result: PagerLayoutInfo): Long {
        val layoutSize = if (result.orientation == Orientation.Horizontal)
            result.viewportSize.width else result.viewportSize.height

        return result.snapPosition.position(
            layoutSize = layoutSize,
            itemSize = pageSizeForPage(0),
            itemIndex = 0,
            beforeContentPadding = result.beforeContentPadding,
            afterContentPadding = result.afterContentPadding,
            itemCount = pageCount
        ).coerceIn(0, layoutSize).toLong()
    }

    // --- Alignment ---

    private fun updateAlignmentLayoutGeneration(orientation: Orientation, layoutSize: Int) {
        val changed = lastAlignmentOrientation != orientation || lastAlignmentLayoutSize != layoutSize
        if (changed) {
            alignmentLayoutGeneration += 1
            lastAlignmentOrientation = orientation
            lastAlignmentLayoutSize = layoutSize
        }
    }

    private fun scheduleScrollViewOffsetAlignment(
        delayMs: Long,
        layoutSize: Int = currentLayoutMainAxisSize()
    ) {
        val scheduledOrientation = layoutInfo.orientation
        val scheduledContentOffset = kuiklyInfo.contentOffset
        val scheduledComposeOffset = currentAbsoluteScrollOffset().toInt()
        val scheduledLayoutGeneration = alignmentLayoutGeneration
        kuiklyInfo.run {
            appleScrollViewOffsetJob?.cancel()
            appleScrollViewOffsetJob = scope?.launch {
                delay(delayMs)
                alignScrollViewOffset(
                    layoutSize,
                    scheduledOrientation,
                    scheduledContentOffset,
                    scheduledComposeOffset,
                    scheduledLayoutGeneration
                )
            }
        }
    }

    private fun currentLayoutMainAxisSize(): Int {
        val info = layoutInfo
        return if (info.orientation == Orientation.Horizontal) info.viewportSize.width else info.viewportSize.height
    }

    private suspend fun alignScrollViewOffset(
        layoutSize: Int,
        scheduledOrientation: Orientation,
        scheduledContentOffset: Int,
        scheduledComposeOffset: Int,
        scheduledLayoutGeneration: Int
    ) {
        val contentOffsetInt = scrollableState.kuiklyInfo.contentOffset

        if (scheduledLayoutGeneration != alignmentLayoutGeneration) return

        if (handleUnreachedSnapTarget(contentOffsetInt, scheduledContentOffset, layoutSize)) return

        val itemsChangedDuringSnap =
            isSnapAnimating && snapStartPageCount != 0 && pageCount != snapStartPageCount
        val snapStartedDesynced = isSnapAnimating && snapStartDesyncPages != 0
        if (itemsChangedDuringSnap || snapStartedDesynced) {
            val relocatedTarget = if (snapTargetRelocatedPage in 0 until pageCount) {
                snapTargetRelocatedPage
            } else {
                currentPage.coerceInPageRange()
            }
            val relocatedTargetOffset = pageBoundaryOffset(relocatedTarget).toInt()
            updateScrollViewContentSize(layoutSize)
            scrollPosition.requestPositionAndForgetLastKnownKey(relocatedTarget, 0f)
            val delta = relocatedTargetOffset - contentOffsetInt
            if (delta != 0) {
                applyScrollViewOffsetDelta(delta)
            } else {
                kuiklyInfo.composeOffset = relocatedTargetOffset.toFloat()
            }
            kuiklyInfo.snapAnchorOffsetCorrection = 0
            clearSnapTrackingAfterAlignment()
            return
        }

        val normalComposeOffset = currentAbsoluteScrollOffset().toInt()
        val positionCorrupted = isSnapAnimating && normalComposeOffset != contentOffsetInt
        val correctTargetPage = nearestPageForOffset(contentOffsetInt)
        val correctTargetOffset = pageBoundaryOffset(correctTargetPage).toInt()
        val composeOffsetInt = if (positionCorrupted) correctTargetOffset else normalComposeOffset

        val needFix = !isScrollInProgress && contentOffsetInt != composeOffsetInt
        updateScrollViewContentSize(layoutSize)

        if (alignComposePositionToNativeBoundaryIfNeeded(composeOffsetInt, contentOffsetInt)) return

        if (needFix) {
            val delta = composeOffsetInt - contentOffsetInt
            applyScrollViewOffsetDelta(delta)
        }

        if (positionCorrupted) {
            scrollPosition.requestPositionAndForgetLastKnownKey(correctTargetPage, 0f)
            kuiklyInfo.composeOffset = correctTargetOffset.toFloat()
        }

        if (isSnapAnimating) clearSnapTrackingAfterAlignment()
    }

    private fun updateScrollViewContentSize(layoutSize: Int) {
        val requiredContentSize = (maxScrollOffset + layoutSize).toInt()
        if (kuiklyInfo.currentContentSize != requiredContentSize) {
            kuiklyInfo.currentContentSize = requiredContentSize
            kuiklyInfo.updateContentSizeToRender()
        }
    }

    /**
     * Synchronously corrects the native scroll view offset to match the compose-side
     * page position. Called during measurement when [needsImmediateAlignment] is set,
     * ensuring the fix is applied before the frame is drawn.
     */
    private fun performImmediateAlignment(layoutSize: Int) {
        if (kuiklyInfo.scrollView == null || isScrollInProgress || isSnapAnimating) {
            return
        }
        updateScrollViewContentSize(layoutSize)
        val composeOffset = currentAbsoluteScrollOffset().toInt()
        val nativeOffset = kuiklyInfo.contentOffset
        if (abs(composeOffset - nativeOffset) > SNAP_TARGET_OFFSET_TOLERANCE) {
            applyScrollViewOffsetDelta(composeOffset - nativeOffset)
        }
    }

    private fun alignComposePositionToNativeBoundaryIfNeeded(
        composeOffset: Int,
        contentOffset: Int
    ): Boolean {
        val composeOffsetOnBoundary = isPageBoundaryOffset(composeOffset)
        val currentPageSizeWS = pageSizeWithSpacingForPage(currentPage)
        val shouldSkipComposeOffset = currentPageSizeWS != 0 &&
            !isScrollInProgress &&
            !isSnapAnimating &&
            !composeOffsetOnBoundary
        if (!shouldSkipComposeOffset) return false

        val nativePage = nearestPageForOffset(contentOffset)
        val nativeBoundaryOffset = pageBoundaryOffset(nativePage).toInt()

        // When nativePage differs from currentPage, compare distances to both
        // page boundaries. In fullScreen mode adjacent pages have equal sizes,
        // so an offset like 1079 (page size 1080) falls inside page 0's range
        // but is only 1px from page 1's boundary. Preferring the closer boundary
        // avoids unwanted page jumps regardless of device density or rounding.
        val targetPage: Int
        val targetBoundary: Int
        if (nativePage != currentPage) {
            val currentPageBoundary = pageBoundaryOffset(currentPage).toInt()
            val distToCurrentPage = abs(contentOffset - currentPageBoundary)
            val distToNativePage = abs(contentOffset - nativeBoundaryOffset)
            if (distToCurrentPage <= distToNativePage) {
                targetPage = currentPage
                targetBoundary = currentPageBoundary
            } else {
                targetPage = nativePage
                targetBoundary = nativeBoundaryOffset
            }
        } else {
            targetPage = nativePage
            targetBoundary = nativeBoundaryOffset
        }

        if (!isPageBoundaryOffset(contentOffset)) {
            val delta = targetBoundary - contentOffset
            applyScrollViewOffsetDelta(delta)
        }
        scrollPosition.requestPositionAndForgetLastKnownKey(targetPage, 0f)
        return true
    }

    private fun clearSnapTrackingAfterAlignment() {
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
            scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
            return true
        }
        if (!snapStallAlignmentRetryRequested) {
            snapStallAlignmentRetryRequested = true
            scheduleScrollViewOffsetAlignment(SNAP_MEASURE_JOB_INITIAL_DELAY_MS, layoutSize)
            return true
        }
        val fallbackPage = if (snapTargetRelocatedPage in 0 until pageCount) {
            snapTargetRelocatedPage
        } else {
            nearestPageForOffset(snapTargetContentOffset)
        }
        val fallbackOffset = pageBoundaryOffset(fallbackPage).toInt()
        updateScrollViewContentSize(layoutSize)
        scrollPosition.requestPositionAndForgetLastKnownKey(fallbackPage, 0f)
        val delta = fallbackOffset - contentOffset
        if (delta != 0) applyScrollViewOffsetDelta(delta) else kuiklyInfo.composeOffset = fallbackOffset.toFloat()
        clearSnapTrackingAfterAlignment()
        return true
    }

    @OptIn(ExperimentalFoundationApi::class)
    internal fun relocateSnapTargetByKey(itemProvider: LazyLayoutItemProvider) {
        if (!isSnapAnimating) return
        val key = snapTargetItemKey ?: return
        if (snapTargetRelocatedPage < 0) return
        val newIndex = itemProvider.findIndexByKey(key, snapTargetRelocatedPage)
        if (newIndex != snapTargetRelocatedPage) snapTargetRelocatedPage = newIndex
    }

    private fun tryRunPrefetch(result: PagerMeasureResult) = Snapshot.withoutReadObservation {
        if (abs(previousPassDelta) > 0.5f) {
            if (prefetchingEnabled && isGestureActionMatchesScroll(previousPassDelta)) {
                notifyPrefetch(previousPassDelta, result)
            }
        }
    }

    internal fun Int.coerceInPageRange() = if (pageCount > 0) coerceIn(0, pageCount - 1) else 0

    private fun isGestureActionMatchesScroll(scrollDelta: Float): Boolean =
        if (layoutInfo.orientation == Orientation.Vertical) {
            sign(scrollDelta) == sign(-upDownDifference.y)
        } else {
            sign(scrollDelta) == sign(-upDownDifference.x)
        } || isNotGestureAction()

    internal fun isNotGestureAction(): Boolean =
        upDownDifference.x.toInt() == 0 && upDownDifference.y.toInt() == 0

    private fun notifyPrefetch(delta: Float, info: PagerLayoutInfo) {
        if (!prefetchingEnabled || info.visiblePagesInfo.isEmpty()) return
        val isPrefetchingForward = delta > 0
        val newIndex = if (isPrefetchingForward) {
            info.visiblePagesInfo.last().index + info.beyondViewportPageCount + PagesToPrefetch
        } else {
            info.visiblePagesInfo.first().index - info.beyondViewportPageCount - PagesToPrefetch
        }
        if (newIndex in 0 until pageCount) {
            if (newIndex != indexToPrefetch) {
                wasPrefetchingForward = isPrefetchingForward
                indexToPrefetch = newIndex
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: PagerLayoutInfo) {
        if (indexToPrefetch != -1 && info.visiblePagesInfo.isNotEmpty()) {
            val expected = if (wasPrefetchingForward) {
                info.visiblePagesInfo.last().index + info.beyondViewportPageCount + PagesToPrefetch
            } else {
                info.visiblePagesInfo.first().index - info.beyondViewportPageCount - PagesToPrefetch
            }
            if (indexToPrefetch != expected) indexToPrefetch = -1
        }
    }

    fun getOffsetDistanceInPages(page: Int): Float {
        require(page in 0..pageCount) { "page $page is not within the range 0 to $pageCount" }
        return page - currentPage - currentPageOffsetFraction
    }

    internal fun matchScrollPositionWithKey(
        itemProvider: LazyLayoutItemProvider,
        currentPage: Int = Snapshot.withoutReadObservation { scrollPosition.currentPage }
    ): Int = scrollPosition.matchPageWithKey(itemProvider, currentPage)
}

internal suspend fun DrawerInternalPagerState.animateToNextPage() {
    if (currentPage + 1 < pageCount) animateScrollToPage(currentPage + 1)
}

internal suspend fun DrawerInternalPagerState.animateToPreviousPage() {
    if (currentPage - 1 >= 0) animateScrollToPage(currentPage - 1)
}

// --- DrawerScrollPosition ---

@OptIn(ExperimentalFoundationApi::class)
internal class DrawerScrollPosition(
    currentPage: Int = 0,
    currentPageOffsetFraction: Float = 0.0f,
    val state: DrawerInternalPagerState
) {
    var currentPage by mutableIntStateOf(currentPage)
        private set

    var currentPageOffsetFraction by mutableStateOf(currentPageOffsetFraction)
        private set

    private var hadFirstNotEmptyLayout = false
    private var lastKnownCurrentPageKey: Any? = null

    val nearestRangeState = LazyLayoutNearestRangeState(
        currentPage,
        NearestItemsSlidingWindowSize,
        NearestItemsExtraItemCount
    )

    fun updateFromMeasureResult(measureResult: PagerMeasureResult) {
        lastKnownCurrentPageKey = measureResult.currentPage?.key
        if (hadFirstNotEmptyLayout || measureResult.visiblePagesInfo.isNotEmpty()) {
            hadFirstNotEmptyLayout = true
            update(
                measureResult.currentPage?.index ?: 0,
                measureResult.currentPageOffsetFraction
            )
        }
    }

    fun requestPositionAndForgetLastKnownKey(index: Int, offsetFraction: Float) {
        update(index, offsetFraction)
        lastKnownCurrentPageKey = null
    }

    fun matchPageWithKey(
        itemProvider: LazyLayoutItemProvider,
        index: Int
    ): Int {
        val newIndex = itemProvider.findIndexByKey(lastKnownCurrentPageKey, index)
        if (index != newIndex) {
            currentPage = newIndex
            nearestRangeState.update(index)
        }
        return newIndex
    }

    private fun update(page: Int, offsetFraction: Float) {
        currentPage = page
        nearestRangeState.update(page)
        currentPageOffsetFraction = offsetFraction
    }

    fun updateCurrentPageOffsetFraction(offsetFraction: Float) {
        currentPageOffsetFraction = offsetFraction
    }

    fun applyScrollDelta(delta: Int) {
        val currentPageSizeWS = state.pageSizeWithSpacingForPage(currentPage)
        val fractionUpdate = if (currentPageSizeWS == 0) 0.0f else delta / currentPageSizeWS.toFloat()
        currentPageOffsetFraction += fractionUpdate
    }
}

// --- DrawerLazyAnimateScrollScope ---

@OptIn(ExperimentalFoundationApi::class)
internal fun DrawerLazyAnimateScrollScope(state: DrawerInternalPagerState): LazyLayoutAnimateScrollScope {
    return object : LazyLayoutAnimateScrollScope {
        override val firstVisibleItemIndex: Int get() = state.firstVisiblePage
        override val firstVisibleItemScrollOffset: Int get() = state.firstVisiblePageOffset
        override val lastVisibleItemIndex: Int
            get() = state.layoutInfo.visiblePagesInfo.lastOrNull()?.index ?: 0
        override val itemCount: Int get() = state.pageCount

        override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
            val pageSizeWS = state.pageSizeWithSpacingForPage(index)
            val offsetFraction = if (pageSizeWS == 0) 0f else scrollOffset / pageSizeWS.toFloat()
            state.snapToItem(index, offsetFraction, forceRemeasure = true)
        }

        override fun calculateDistanceTo(targetIndex: Int): Float {
            val visibleItem =
                state.layoutInfo.visiblePagesInfo.fastFirstOrNull { it.index == targetIndex }
            return if (visibleItem == null) {
                val targetOffset = state.sizeTracker.getOffsetForPage(targetIndex)
                val currentOffset = state.currentAbsoluteScrollOffset()
                (targetOffset - currentOffset).toFloat()
            } else {
                visibleItem.offset.toFloat()
            }
        }

        override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
            state.scroll(block = block)
        }
    }
}

private val DrawerEmptyLayoutInfo = PagerMeasureResult(
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

private const val SNAP_TARGET_OFFSET_TOLERANCE = 1
private const val SNAP_MEASURE_JOB_INITIAL_DELAY_MS = 50L
