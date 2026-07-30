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

import com.tencent.kuikly.compose.foundation.drawer.DrawerInternalPagerState
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.pager.PagerMeasureResult
import com.tencent.kuikly.compose.foundation.pager.PagerSnapDistance
import com.tencent.kuikly.compose.ui.util.fastFirstOrNull
import com.tencent.kuikly.core.views.SpringAnimation
import com.tencent.kuikly.core.views.WillEndDragParams
import kotlin.math.abs
import kotlin.math.min

internal fun DrawerInternalPagerState.kuiklyWillDragEnd(params: WillEndDragParams, orientation: Orientation) {
    clearSnapAnimationState()
    val currentPageSize = pageSizeForPage(currentPage)
    val currentPageSizeWithSpacing = pageSizeWithSpacingForPage(currentPage)
    if (currentPageSizeWithSpacing == 0) return

    val nativeContentOffset = kuiklyInfo.contentOffset
    val nativePageFromOffset = sizeTracker.getPageForOffset(nativeContentOffset.toLong()).first
    val desyncPages = firstVisiblePage - nativePageFromOffset

    val velocity = if (orientation == Orientation.Horizontal) -params.velocityX else -params.velocityY
    val pageDirection = when { velocity < 0 -> 1; velocity > 0 -> -1; else -> 0 }
    val snapBasePage = currentPage
    val lastPage = (pageCount - 1).coerceAtLeast(0)
    val targetPage = (if (pageDirection == 0) currentPage else snapBasePage + pageDirection).coerceIn(0, lastPage)

    val correctedTargetPage = if (velocity != 0f) {
        PagerSnapDistance.atMost(1).calculateTargetPage(
            snapBasePage,
            targetPage,
            velocity,
            currentPageSize,
            spaceBetweenPages
        ).coerceIn(0, lastPage)
    } else currentPage

    val kuiklyInfo = this.kuiklyInfo
    val pagerMeasureResult = layoutInfo as? PagerMeasureResult ?: return
    pagerMeasureResult.run {
        val allResult = visiblePagesInfo + extraPagesAfter + extraPagesBefore
        val nextPage = allResult.fastFirstOrNull { it.index == correctedTargetPage }
        val density = kuiklyInfo.getDensity()
        val offset = kuiklyInfo.composeOffset.toInt()
        val measureViewportSize = if (pagerMeasureResult.orientation == Orientation.Horizontal) viewportSize.width else viewportSize.height
        val nativeViewportSize = kuiklyInfo.viewportSize
        if (nativeViewportSize > 0 && measureViewportSize > 0 && abs(measureViewportSize - nativeViewportSize) > 1) return
        val maxOffset = kuiklyInfo.currentContentSize - nativeViewportSize
        val composeCandidateOffset = nextPage?.let { offset + it.offset }
        val pageBoundaryOffset = this@kuiklyWillDragEnd.pageBoundaryOffset(correctedTargetPage).toInt()
        var targetOffset = composeCandidateOffset ?: pageBoundaryOffset
        targetOffset = min(targetOffset, maxOffset).coerceAtLeast(0)
        this@kuiklyWillDragEnd.markSnapAnimationStarted(
            targetOffset, correctedTargetPage, nextPage?.key, desyncPages
        )
        val springVelocity = if (orientation == Orientation.Horizontal) {
            params.velocityX
        } else {
            params.velocityY
        }
        val springAnimation = SpringAnimation(
            ScrollableStateConstants.SPRING_ANIMATION_DURATION,
            ScrollableStateConstants.SPRING_ANIMATION_DAMPING,
            springVelocity
        )
        val targetOffsetDp = targetOffset / density
        if (orientation == Orientation.Horizontal) {
            kuiklyInfo.scrollView?.setContentOffset(targetOffsetDp, 0f, true, springAnimation)
        } else {
            kuiklyInfo.scrollView?.setContentOffset(0f, targetOffsetDp, true, springAnimation)
        }
    }
}
