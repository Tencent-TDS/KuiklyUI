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

import com.tencent.kuikly.compose.animation.core.AnimationSpec
import com.tencent.kuikly.compose.animation.core.AnimationVector1D
import com.tencent.kuikly.compose.animation.core.SpringSpec
import com.tencent.kuikly.compose.animation.core.TweenSpec
import com.tencent.kuikly.compose.animation.core.VectorConverter
import com.tencent.kuikly.compose.animation.core.VectorizedAnimationSpec
import com.tencent.kuikly.compose.animation.core.getDurationMillis
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.pager.PagerMeasureResult
import com.tencent.kuikly.compose.foundation.pager.PagerSnapDistance
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.foundation.pager.pagerSnapDebugLog
import com.tencent.kuikly.compose.ui.util.fastFirstOrNull
import com.tencent.kuikly.core.views.SpringAnimation
import com.tencent.kuikly.core.views.WillEndDragParams
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Handle drag end event
 */
internal fun PagerState.kuiklyWillDragEnd(params: WillEndDragParams, orientation: Orientation) {
    // Clear any previous snap animation flag in case scrollEnd didn't fire
    // (e.g., user interrupted a snap animation by starting a new drag)
    clearSnapAnimationState()

    val effectivePageSizePx = pageSize + pageSpacing
    if (effectivePageSizePx == 0) {
        return
    }

    // Capture compose<->native desync at gesture end. The snap target is still derived from the
    // logical current page and gesture direction, but settle must avoid re-deriving the page from a
    // shifted native pixel offset.
    val nativeContentOffset = kuiklyInfo.contentOffset
    val nativePageFromOffset =
        (nativeContentOffset.toFloat() / effectivePageSizePx).roundToInt()
    val desyncPages = firstVisiblePage - nativePageFromOffset

    val velocity = if (orientation == Orientation.Horizontal) -params.velocityX else -params.velocityY
    val pageDirection = when {
        velocity < 0 -> 1
        velocity > 0 -> -1
        else -> 0
    }
    val snapBasePage = resolveSnapBasePage(pageDirection)
    val targetPage = if (pageDirection == 0) {
        currentPage
    } else {
        snapBasePage + pageDirection
    }.coerceIn(0, pageCount)

    val correctedTargetPage = calculateTargetPage(snapBasePage, targetPage, velocity)
    pagerSnapDebugLog {
        "willDragEndDecision: stateId=$debugPagerStateId orientation=$orientation " +
            "velocity=$velocity pageDirection=$pageDirection snapBasePage=$snapBasePage " +
            "targetPage=$targetPage correctedTargetPage=$correctedTargetPage " +
            "currentPage=$currentPage firstVisiblePage=$firstVisiblePage " +
            "currentPageOffsetFraction=$currentPageOffsetFraction " +
            "nativeContentOffset=$nativeContentOffset nativePageFromOffset=$nativePageFromOffset " +
            "desyncPages=$desyncPages pageCount=$pageCount"
    }
    handleTargetPageScroll(correctedTargetPage, params, orientation, desyncPages)
}

/**
 * When flinging forward, measure may advance [currentPage] to [firstVisiblePage] + 1 once the next
 * page crosses the 50% threshold. Using that advanced page as snap base and adding pageDirection
 * again would skip two pages. Only adjust the base for the velocity != 0 path; zero-velocity release
 * still settles on [currentPage], which already reflects the 50% decision from measure.
 */
private fun PagerState.resolveSnapBasePage(pageDirection: Int): Int {
    return when {
        pageDirection > 0 && currentPage > firstVisiblePage -> firstVisiblePage
        pageDirection < 0 && currentPage < firstVisiblePage -> firstVisiblePage
        else -> currentPage
    }
}

private fun PagerState.calculateTargetPage(
    startPage: Int,
    targetPage: Int,
    velocity: Float
): Int {
    return if (velocity != 0f) {
        PagerSnapDistance.atMost(1).calculateTargetPage(
            startPage,
            targetPage,
            velocity,
            pageSize,
            pageSpacing
        ).coerceIn(0, pageCount)
    } else {
        currentPage
    }
}

private fun PagerState.handleTargetPageScroll(
    targetPage: Int,
    params: WillEndDragParams,
    orientation: Orientation,
    desyncPages: Int = 0
) {
    val kuiklyInfo = this.kuiklyInfo
    val pagerMeasureResult = layoutInfo as? PagerMeasureResult ?: return
    pagerMeasureResult.run {
        val allResult = visiblePagesInfo + extraPagesAfter + extraPagesBefore
        val nextPage = allResult.fastFirstOrNull { it.index == targetPage }
        val density = kuiklyInfo.getDensity()
        val offset = kuiklyInfo.composeOffset.toInt()
        val nativeOffset = if (orientation == Orientation.Horizontal) {
            params.offsetX.toInt()
        } else {
            params.offsetY.toInt()
        }
        val nativeTargetOffset = if (orientation == Orientation.Horizontal) {
            params.targetContentOffsetX.toInt()
        } else {
            params.targetContentOffsetY.toInt()
        }
        val measureViewportSize = if (pagerMeasureResult.orientation == Orientation.Horizontal) {
            viewportSize.width
        } else {
            viewportSize.height
        }
        val nativeViewportSize = kuiklyInfo.viewportSize

        if (isSnapLayoutSizeUnstable(measureViewportSize, nativeViewportSize)) {
            pagerSnapDebugLog {
                "skipWillDragEndSnapUnstableLayoutSize: " +
                    "stateId=${this@handleTargetPageScroll.debugPagerStateId} " +
                    "eventOrientation=$orientation measureOrientation=${pagerMeasureResult.orientation} " +
                    "targetPage=$targetPage measureViewportSize=$measureViewportSize " +
                    "nativeViewportSize=$nativeViewportSize pageSize=$pageSize " +
                    "pageSpacing=$pageSpacing pageSizeWithSpacing=$pageSizeWithSpacing " +
                    "nativeOffset=$nativeOffset nativeTargetOffset=$nativeTargetOffset " +
                    "kuiklyContentOffset=${kuiklyInfo.contentOffset} " +
                    "composeOffset=${kuiklyInfo.composeOffset.toInt()} " +
                    "currentContentSize=${kuiklyInfo.currentContentSize}"
            }
            return
        }

        val maxOffset = kuiklyInfo.currentContentSize - kuiklyInfo.viewportSize
        val composeCandidateOffset = nextPage?.let { offset + it.offset }
        val pageBoundaryOffset = snapScrollOffsetForPage(targetPage)
        // Snap target selection:
        //  - Aligned (no desync): use the compose-coordinate candidate. Native and compose share the
        //    same coordinate, so this lands exactly on the next page.
        //  - Pre-existing compose<->native desync: the event offset can be stale, but Android
        //    setContentOffset is applied against the contentView's current top/left, which already
        //    includes composeOffset. Use the compose-coordinate target so the native animation moves
        //    in the same direction as the visible item frames.
        var targetOffset = composeCandidateOffset ?: pageBoundaryOffset
        targetOffset = min(targetOffset, maxOffset).coerceAtLeast(0)

        pagerSnapDebugLog {
            "willDragEndSnap: stateId=${this@handleTargetPageScroll.debugPagerStateId} " +
                "eventOrientation=$orientation measureOrientation=${pagerMeasureResult.orientation} " +
                "targetPage=$targetPage targetOffset=$targetOffset " +
                "composeCandidateOffset=$composeCandidateOffset pageBoundaryOffset=$pageBoundaryOffset " +
                "nativeOffset=$nativeOffset kuiklyContentOffset=${kuiklyInfo.contentOffset} " +
                "nativeTargetOffset=$nativeTargetOffset " +
                "composeOffset=$offset nextPageOffset=${nextPage?.offset} " +
                "stateCurrentPage=${this@handleTargetPageScroll.currentPage} " +
                "stateFirstVisiblePage=${this@handleTargetPageScroll.firstVisiblePage} " +
                "measureCurrentPage=${currentPage?.index} measureFirstVisiblePage=${firstVisiblePage?.index} " +
                "pageSizeWithSpacing=$pageSizeWithSpacing maxOffset=$maxOffset " +
                "nextPageFound=${nextPage != null}"
        }
        this@handleTargetPageScroll.markSnapAnimationStarted(
            targetOffset,
            targetPage,
            nextPage?.key,
            desyncPages
        )

        val springAnimation = SpringAnimation(
            ScrollableStateConstants.SPRING_ANIMATION_DURATION,
            ScrollableStateConstants.SPRING_ANIMATION_DAMPING,
            if (orientation == Orientation.Horizontal) params.velocityX else params.velocityY
        )
        val targetOffsetDp = targetOffset / density

        if (orientation == Orientation.Horizontal) {
            kuiklyInfo.scrollView?.setContentOffset(
                targetOffsetDp,
                0f,
                true,
                springAnimation
            )
        } else {
            kuiklyInfo.scrollView?.setContentOffset(
                0f,
                targetOffsetDp,
                true,
                springAnimation
            )
        }
    }
}

private fun isSnapLayoutSizeUnstable(measureViewportSize: Int, nativeViewportSize: Int): Boolean {
    return nativeViewportSize > 0 &&
        measureViewportSize > 0 &&
        abs(measureViewportSize - nativeViewportSize) > SNAP_LAYOUT_SIZE_TOLERANCE
}

private const val SNAP_LAYOUT_SIZE_TOLERANCE = 1

/**
 * Converts AnimationSpec<Float> to SpringAnimation
 * This is a temporary solution that mainly supports animation duration and basic animation curves
 *
 * @param animationSpec The animation spec to convert
 * @param initialValue Initial value (used for calculating SpringSpec duration)
 * @param targetValue Target value (used for calculating SpringSpec duration)
 * @return The converted SpringAnimation, or null if the type is not supported
 */
internal fun convertAnimationSpecToSpringAnimation(
    animationSpec: AnimationSpec<Float>,
    initialValue: Float = 0f,
    targetValue: Float = 0f
): SpringAnimation? {
    return when (animationSpec) {
        is TweenSpec<*> -> {
            // TweenSpec: Use durationMillis as durationMs
            // Note: Easing curve (animationSpec.easing) is not supported yet,
            // using default damping value instead
            SpringAnimation(
                durationMs = animationSpec.durationMillis,
                damping = 0.8f, // Default damping value (easing curve not supported)
                velocity = 0f
            )
        }
        is SpringSpec<*> -> {
            // SpringSpec: Use dampingRatio as damping, calculate duration via vectorize
            // SpringSpec is physics-based, so duration needs to be calculated from spring parameters
            // Note: getDurationMillis may involve complex calculations (Newton's method, etc.),
            // but it's only called once per animateScrollToPage, not per frame
            val vectorizedSpec: VectorizedAnimationSpec<AnimationVector1D> =
                animationSpec.vectorize<AnimationVector1D>(Float.VectorConverter)
            val initialVector = AnimationVector1D(initialValue)
            val targetVector = AnimationVector1D(targetValue)
            val initialVelocityVector = AnimationVector1D(0f)
            val durationMs = vectorizedSpec.getDurationMillis(
                initialVector,
                targetVector,
                initialVelocityVector
            ).toInt().coerceAtLeast(1)
            SpringAnimation(
                durationMs = durationMs,
                damping = animationSpec.dampingRatio,
                velocity = 0f
            )
        }
        else -> {
            // Unrecognized type, return null
            null
        }
    }
}
