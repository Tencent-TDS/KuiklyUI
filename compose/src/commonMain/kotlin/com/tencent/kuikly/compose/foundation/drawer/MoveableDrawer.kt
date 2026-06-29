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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.pager.PagerScope
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.core.views.DivView
import kotlinx.coroutines.launch

/**
 * State for [MoveableDrawer].
 *
 * @see rememberMoveableDrawerState
 */
@Stable
class MoveableDrawerState internal constructor(
    internal val internalState: DrawerInternalPagerState,
    val fullScreen: Boolean = false,
    val drawerWidth: Dp = 300.dp
) {
    /** Whether the drawer is currently open (settled on the drawer page). */
    val isOpen: Boolean
        get() = internalState.currentPage == 0

    /**
     * Drawer open progress. 0 = fully closed, 1 = fully open.
     * Computed from the absolute scroll offset so the value is linear
     * and continuous regardless of which page is currently snapped.
     */
    val progress: Float
        get() {
            val closedOffset = internalState.sizeTracker.getOffsetForPage(1).toFloat()
            if (closedOffset <= 0f) return 0f
            val currentOffset = internalState.currentAbsoluteScrollOffset().toFloat()
            return (1f - currentOffset / closedOffset).coerceIn(0f, 1f)
        }

    /** Open the drawer with animation. */
    suspend fun open() {
        internalState.animateScrollToPage(0)
    }

    /** Close the drawer with animation. */
    suspend fun close() {
        internalState.animateScrollToPage(1)
    }

    /** Toggle the drawer state with animation. */
    suspend fun toggle() {
        if (isOpen) close() else open()
    }
}

/**
 * Creates and remembers a [MoveableDrawerState].
 *
 * @param initiallyOpen Whether the drawer starts open. Default is closed.
 * @param fullScreen When true the drawer panel occupies the full available width;
 *   when false (default) it uses [drawerWidth] or [drawerWidthProvider].
 * @param drawerWidthProvider Optional custom provider for the drawer width in pixels.
 * @param drawerWidth Width of the drawer panel when [fullScreen] is false.
 */
@Composable
fun rememberMoveableDrawerState(
    initiallyOpen: Boolean = false,
    fullScreen: Boolean = false,
    drawerWidthProvider: (Density.(availableSpace: Int) -> Int)? = null,
    drawerWidth: Dp = 300.dp
): MoveableDrawerState {
    val density = LocalDensity.current
    val drawerWidthPx = remember(density, drawerWidth) { with(density) { drawerWidth.toPx().toInt() } }
    val provider: Density.(Int) -> Int = when {
        fullScreen -> { availableSpace: Int -> availableSpace }
        drawerWidthProvider != null -> drawerWidthProvider
        else -> { _: Int -> drawerWidthPx }
    }

    val internalState = rememberDrawerInternalPagerState(
        initialPage = if (initiallyOpen) 0 else 1,
        pageSizeProvider = { pageIndex, availableSpace ->
            when (pageIndex) {
                0 -> provider(availableSpace)
                else -> availableSpace
            }
        },
        pageCount = { 2 }
    )

    // Pre-seed kuiklyInfo so the native ScrollView restores at the closed position
    // when the Drawer is recreated (e.g. via key()). Without this, a fresh state has
    // contentOffset=0 which corresponds to the drawer-open position, causing a visible
    // flash before performImmediateAlignment corrects it.
    //
    // In fullScreen mode the drawer page (page 0) is as wide as the viewport, so the
    // closed offset (page 1 origin) equals the viewport width, NOT drawerWidthPx.
    val configuration = LocalConfiguration.current
    val viewportWidthPx = remember(density, configuration.pageViewWidth) {
        with(density) { configuration.pageViewWidth.dp.toPx().toInt() }
    }
    val closedOffsetPx = if (fullScreen) viewportWidthPx else drawerWidthPx

    if (!initiallyOpen && internalState.kuiklyInfo.contentOffset == 0
        && internalState.kuiklyInfo.scrollView == null
    ) {
        internalState.kuiklyInfo.contentOffset = closedOffsetPx
        internalState.kuiklyInfo.composeOffset = closedOffsetPx.toFloat()
        // Ensure contentSize is large enough that iOS won't clamp contentOffset back to 0.
        // The real contentSize (drawerWidth + viewportWidth) will be set during measure.
        if (internalState.kuiklyInfo.currentContentSize < closedOffsetPx * 3) {
            internalState.kuiklyInfo.currentContentSize = closedOffsetPx * 3
        }
    }

    // Detect drawer configuration changes (fullScreen / drawerWidth) and request
    // immediate native scroll alignment on the next measure pass. This prevents
    // visual glitches when switching modes: drawer content leaking through when
    // going from fullscreen to normal, or showing the wrong initial size when
    // going from normal to fullscreen.
    val prevConfig = remember { intArrayOf(if (fullScreen) 1 else 0, drawerWidthPx) }
    val fullScreenInt = if (fullScreen) 1 else 0
    if (prevConfig[0] != fullScreenInt || prevConfig[1] != drawerWidthPx) {
        prevConfig[0] = fullScreenInt
        prevConfig[1] = drawerWidthPx
        internalState.needsImmediateAlignment = true
    }

    return remember(internalState, fullScreen, drawerWidth) {
        MoveableDrawerState(internalState, fullScreen, drawerWidth)
    }
}

/**
 * A drawer component with smooth sliding animation. The drawer panel slides in from the
 * left, with a scrim overlay on the main content.
 *
 * Built on top of the internal variable-size pager: page 0 is the drawer panel (narrow or
 * full-width when [MoveableDrawerState.fullScreen] is true), page 1 is the main content
 * (full width). Swipe gestures and snap animation are handled automatically.
 *
 * @param state The [MoveableDrawerState] to control this drawer. Create with [rememberMoveableDrawerState].
 * @param modifier Modifier for the drawer container.
 * @param scrimColor Color of the scrim overlay shown when the drawer is open.
 * @param drawerContent Content of the drawer panel.
 * @param content Main content area.
 */
@Composable
fun MoveableDrawer(
    state: MoveableDrawerState,
    modifier: Modifier = Modifier,
    scrimColor: Color = Color.Black.copy(alpha = 0.3f),
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerProgress by remember {
        derivedStateOf { state.progress }
    }

    Box(modifier.fillMaxSize()) {
        DrawerHorizontalPager(
            state = state.internalState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> {
                    val drawerModifier = if (state.fullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.width(state.drawerWidth).fillMaxHeight()
                    }
                    Box(drawerModifier) {
                        // Consume native touch events to prevent passthrough to content behind the drawer.
                        MakeKuiklyComposeNode<DivView>(
                            factory = { DivView() },
                            modifier = Modifier.fillMaxSize(),
                            viewInit = {
                                getViewEvent().click { }
                            },
                        )
                        drawerContent()
                    }
                }
                1 -> {
                    Box(Modifier.fillMaxSize()) {
                        content()
                        if (drawerProgress > 0.01f) {
                            // Block touches on BOTH dispatch paths:
                            // 1. Native path: getViewEvent().click {} on DivView blocks native
                            //    views (e.g. WebView) from receiving touches.
                            // 2. Compose path: Modifier.clickable() consumes pointer events in
                            //    the Compose hit-testing pipeline, preventing Compose clickable
                            //    nodes underneath from firing.
                            MakeKuiklyComposeNode<DivView>(
                                factory = { DivView() },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(scrimColor.copy(alpha = scrimColor.alpha * drawerProgress))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        scope.launch { state.close() }
                                    },
                                viewInit = {
                                    getViewEvent().click {
                                        scope.launch { state.close() }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
