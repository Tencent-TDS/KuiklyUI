/*
 * Copyright 2025 The Android Open Source Project
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

package com.tencent.kuikly.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyListScope
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.scroller.isAtTop
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.layout
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Custom offset modifier to adjust child position
 */
private fun Modifier.offsetWithParentAdjustment(
    x: Dp = 0.dp,
    y: Dp = 0.dp
) = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val xPx = x.roundToPx()
    val yPx = y.roundToPx()

    // Calculate actual required width and height
    val width = placeable.width + xPx
    val height = placeable.height + yPx

    layout(width, height) {
        placeable.placeRelative(xPx, yPx)
    }
}

/**
 * Quadruple data class for monitoring multiple states in snapshotFlow
 */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

/**
 * Creates a [PullToRefreshState] that is remembered across compositions.
 *
 * @param isRefreshing the value for [PullToRefreshState.isRefreshing]
 */
@Composable
fun rememberPullToRefreshState(
    isRefreshing: Boolean
): PullToRefreshState = remember {
    PullToRefreshState(isRefreshing)
}.apply { this.isRefreshing = isRefreshing }

/**
 * Pull-to-refresh state enum, inspired by RefreshView design
 */
enum class PullState {
    IDLE,        // Normal idle state
    PULLING,     // Ready to refresh when released
    REFRESHING,  // Currently refreshing
}

/**
 * A state object that can be hoisted to control and observe pull-to-refresh behavior.
 *
 * @param isRefreshing the initial refreshing state
 */
@Stable
class PullToRefreshState(
    isRefreshing: Boolean
) {
    /**
     * Whether this [PullToRefreshState] is currently refreshing or not.
     */
    var isRefreshing by mutableStateOf(isRefreshing)

    /**
     * Internal pull state
     */
    internal var pullState: PullState by mutableStateOf(if (isRefreshing) PullState.REFRESHING else PullState.IDLE)

    /**
     * Pull progress from 0.0 to 1.0
     */
    var pullProgress: Float by mutableStateOf(0f)
        internal set

    /**
     * Whether a pull is currently in progress
     */
    val isPullInProgress: Boolean
        get() = pullState != PullState.IDLE

    internal fun updateProgress(progress: Float) {
        pullProgress = progress.coerceIn(0f, 1f)
    }

    internal fun updatePullState(newState: PullState) {
        pullState = newState
    }
}

/**
 * Pull-to-refresh component that should be placed as the first item in LazyColumn.
 * 
 * Usage:
 * ```
 * val pullToRefreshState = rememberPullToRefreshState(isRefreshing)
 * val lazyListState = rememberLazyListState()
 * 
 * LazyColumn(state = lazyListState) {
 *     pullToRefreshItem(
 *         state = pullToRefreshState,
 *         onRefresh = { /* refresh logic */ },
 *         scrollState = lazyListState
 *     )
 *     
 *     items(data) { item ->
 *         // Business content
 *     }
 * }
 * ```
 * 
 * @param state Pull-to-refresh state
 * @param onRefresh Refresh callback
 * @param scrollState LazyListState for monitoring scroll state
 * @param modifier Modifier
 * @param refreshThreshold Threshold to trigger refresh
 * @param content Custom refresh indicator content
 */
fun LazyListScope.pullToRefreshItem(
    state: PullToRefreshState,
    onRefresh: () -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    refreshThreshold: Dp = 80.dp,
    content: @Composable (
        pullProgress: Float,
        isRefreshing: Boolean,
        refreshThreshold: Dp
    ) -> Unit = { progress, refreshing, threshold ->
        DefaultRefreshIndicator(progress, refreshing, threshold)
    }
) {
    // Mark that the current list uses PullToRefresh
    scrollState.kuiklyInfo.hasPullToRefresh = true
    
    item(key = "pull_to_refresh") {
        PullToRefreshItem(
            state = state,
            onRefresh = onRefresh,
            scrollState = scrollState,
            modifier = modifier,
            refreshThreshold = refreshThreshold,
            content = content
        )
    }
}

/**
 * Internal pull-to-refresh component implementation.
 * Use [LazyListScope.pullToRefreshItem] instead for easier usage.
 */
@Composable
internal fun PullToRefreshItem(
    state: PullToRefreshState,
    onRefresh: () -> Unit,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    refreshThreshold: Dp = 80.dp,
    content: @Composable (
        pullProgress: Float,
        isRefreshing: Boolean,
        refreshThreshold: Dp
    ) -> Unit = { progress, refreshing, threshold ->
        DefaultRefreshIndicator(progress, refreshing, threshold)
    }
) {
    val density = LocalDensity.current
    val refreshThresholdPx = with(density) { refreshThreshold.toPx() }
    val refreshThresholdLogical = refreshThresholdPx / density.density
    val updatedOnRefresh by rememberUpdatedState(onRefresh)

    // Monitor scroll state changes inspired by RefreshView logic
    LaunchedEffect(scrollState) {
        snapshotFlow {
            val kuiklyInfo = scrollState.kuiklyInfo
            val isAtTop = scrollState.isAtTop()
            val contentOffset = if (isAtTop) kuiklyInfo.contentOffset else 0
            val isDragging = scrollState.kuiklyInfo.scrollView?.isDragging ?: false
            Quad(contentOffset, isAtTop, isDragging, state.isRefreshing)
        }
        .distinctUntilChanged()
        .collectLatest { (contentOffset, isAtTop, isDragging, _) ->
            if (!isAtTop) {
                // Reset state when not at top
                if (state.pullState != PullState.IDLE) {
                    state.updatePullState(PullState.IDLE)
                    state.updateProgress(0f)
                    scrollState.kuiklyInfo.scrollView?.setContentInsetWhenEndDrag(top = 0f)
                }
                return@collectLatest
            }

            // Handle pull logic when at top
            val scrollView = scrollState.kuiklyInfo.scrollView
            val pullDistance = if (contentOffset < 0) abs(contentOffset.toFloat()) else 0f
            val progress = (pullDistance / refreshThresholdPx).coerceIn(0f, 1f)
            
            state.updateProgress(progress)

            when (state.pullState) {
                PullState.REFRESHING -> {
                    // Do nothing during refresh, just update progress
                }
                PullState.IDLE -> {
                    if (isDragging && pullDistance >= refreshThresholdPx) {
                        state.updatePullState(PullState.PULLING)
                        scrollView?.setContentInsetWhenEndDrag(top = refreshThresholdLogical)
                    }
                }
                PullState.PULLING -> {
                    if (isDragging) {
                        if (pullDistance < refreshThresholdPx) {
                            state.updatePullState(PullState.IDLE)
                            scrollView?.setContentInsetWhenEndDrag(top = 0f)
                        }
                    } else {
                        // Released while pulling, start refresh
                        state.updatePullState(PullState.REFRESHING)
                        updatedOnRefresh()
                    }
                }
            }
        }
    }

    // Handle inset changes based on pull state
    LaunchedEffect(state.pullState) {
        val scrollView = scrollState.kuiklyInfo.scrollView
        when (state.pullState) {
            PullState.REFRESHING -> {
                scrollView?.setContentInset(top = refreshThresholdLogical, animated = true)
            }
            PullState.IDLE -> {
                scrollView?.setContentInset(top = 0f, animated = true)
                scrollView?.setContentInsetWhenEndDrag(top = 0f)
            }
            PullState.PULLING -> {
                // Handled by EndDragInset
            }
        }
    }

    // Sync external refresh state
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            if (state.pullState != PullState.REFRESHING) {
                state.updatePullState(PullState.REFRESHING)
            }
        } else {
            if (state.pullState == PullState.REFRESHING) {
                state.updatePullState(PullState.IDLE)
                state.updateProgress(0f)
            }
        }
    }

    // Refresh indicator UI
    Box(
        modifier = modifier
            .offsetWithParentAdjustment(y = -refreshThreshold)
            .fillMaxWidth()
            .height(refreshThreshold),
        contentAlignment = Alignment.Center
    ) {
        content(state.pullProgress, state.isRefreshing, refreshThreshold)
    }
}

/**
 * Default refresh indicator
 */
@Composable
private fun DefaultRefreshIndicator(
    pullProgress: Float,
    isRefreshing: Boolean,
    refreshThreshold: Dp
) {
    if (isRefreshing) {
        Text(
            text = "🔄 Refreshing...",
            fontSize = 16.sp,
            color = Color.Blue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        val emoji = if (pullProgress >= 1f) "⬆️" else "⬇️"
        val text = if (pullProgress >= 1f) "Release to refresh" else "Pull to refresh"
        
        Text(
            text = "$emoji $text",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}