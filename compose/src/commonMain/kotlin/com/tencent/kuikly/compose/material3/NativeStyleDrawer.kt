/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.tencent.kuikly.compose.animation.core.AnimationSpec
import com.tencent.kuikly.compose.animation.core.DecayAnimationSpec
import com.tencent.kuikly.compose.animation.core.exponentialDecay
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.AnchoredDraggableState
import com.tencent.kuikly.compose.foundation.gestures.DraggableAnchors
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.anchoredDraggable
import com.tencent.kuikly.compose.foundation.gestures.animateTo
import com.tencent.kuikly.compose.foundation.gestures.snapTo
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.tokens.NavigationDrawerTokens
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ========================================================================================
// Core Drawer Definitions
// All shared Drawer types, state, defaults, and components are defined here.
// NavigationDrawer.kt (BackPress version) imports from this file.
// ========================================================================================

/**
 * Possible values of [DrawerState].
 */
enum class DrawerValue {
    /**
     * The state of the drawer when it is closed.
     */
    Closed,

    /**
     * The state of the drawer when it is open.
     */
    Open
}

/**
 * State of a modal navigation drawer.
 *
 * Backed by [AnchoredDraggableState] for anchor-based drag gestures and animations.
 * Provides anchor-snapping, velocity-based fling, and positional threshold support.
 *
 * @param initialValue The initial value of the drawer state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
class DrawerState(
    initialValue: DrawerValue = DrawerValue.Closed,
    val confirmStateChange: (DrawerValue) -> Boolean = { true }
) {
    /**
     * The internal [AnchoredDraggableState] that manages drag offsets, anchors, and animations.
     */
    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = { distance -> distance * DrawerPositionalThreshold },
        velocityThreshold = { DrawerVelocityThresholdPx },
        snapAnimationSpec = DrawerDefaults.AnimationSpec,
        decayAnimationSpec = DrawerDefaults.DecayAnimationSpec,
        confirmValueChange = confirmStateChange
    )

    /**
     * The current value of the drawer state.
     */
    val currentValue: DrawerValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target value of the drawer state. This is the closest value to the current offset,
     * or the value that the drawer is animating to.
     */
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * Whether the drawer is open.
     */
    val isOpen: Boolean
        get() = currentValue == DrawerValue.Open

    /**
     * Whether the drawer is closed.
     */
    val isClosed: Boolean
        get() = currentValue == DrawerValue.Closed

    /**
     * Whether an animation is currently running.
     */
    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * The current offset of the drawer in pixels.
     * Range: [-drawerWidth, 0]
     * -drawerWidth means fully closed (off-screen to the left)
     * 0 means fully open
     */
    val offset: Float
        get() = anchoredDraggableState.offset

    /**
     * Require the current offset (throws if not initialized).
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /**
     * The progress of the drawer opening, from 0f (closed) to 1f (open).
     */
    val progress: Float
        get() = anchoredDraggableState.progress(DrawerValue.Closed, DrawerValue.Open)

    /**
     * Open the drawer with an animation.
     */
    suspend fun open() {
        anchoredDraggableState.animateTo(DrawerValue.Open)
    }

    /**
     * Close the drawer with an animation.
     */
    suspend fun close() {
        anchoredDraggableState.animateTo(DrawerValue.Closed)
    }

    /**
     * Snap to the target value without animation.
     */
    suspend fun snapTo(target: DrawerValue) {
        anchoredDraggableState.snapTo(target)
    }

    /**
     * Update the anchors based on the actual drawer width (in pixels).
     * Should be called when the drawer width is known or changes.
     */
    internal fun updateAnchors(drawerWidthPx: Float) {
        val newAnchors = DraggableAnchors {
            DrawerValue.Closed at -drawerWidthPx
            DrawerValue.Open at 0f
        }
        anchoredDraggableState.updateAnchors(newAnchors)
    }

    internal var density: Density? = null

    companion object {
        /**
         * The default [Saver] for [DrawerState].
         */
        val Saver = Saver<DrawerState, String>(
            save = { it.currentValue.name },
            restore = { DrawerState(DrawerValue.valueOf(it)) }
        )
    }
}

/** Positional threshold: 50% of drag distance */
internal const val DrawerPositionalThreshold = 0.5f

/** Velocity threshold in px/s for fling-based state change */
internal var DrawerVelocityThresholdPx: Float = 400f

/**
 * Create and remember a [DrawerState].
 *
 * @param initialValue The initial value of the drawer.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue = DrawerValue.Closed,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    val density = LocalDensity.current
    return rememberSaveable(saver = DrawerState.Saver) {
        DrawerState(initialValue, confirmStateChange)
    }.also {
        it.density = density
        // Update velocity threshold based on density (400.dp → px)
        DrawerVelocityThresholdPx = with(density) { DrawerDefaults.VelocityThreshold.toPx() }
    }
}

/**
 * Contains the default values used by Drawer components.
 */
object DrawerDefaults {

    /**
     * Default width of the navigation drawer.
     */
    val MaximumDrawerWidth: Dp = NavigationDrawerTokens.ContainerWidth

    /**
     * Default color of the scrim that obscures content when the drawer is open.
     */
    val scrimColor: Color = Color.Black.copy(alpha = 0.32f)

    /**
     * Default container color for the drawer sheet.
     */
    val containerColor: Color = Color.White

    /**
     * Default elevation for the modal drawer.
     */
    val ModalDrawerElevation: Dp = 1.dp

    /**
     * Default snap animation spec for drawer open/close transitions.
     * Used by [AnchoredDraggableState] when the fling velocity is too low for decay.
     */
    val AnimationSpec: AnimationSpec<Float> = tween(durationMillis = 256)

    /**
     * Default decay animation spec for drawer fling behavior.
     * Used by [AnchoredDraggableState] when the fling velocity is high enough.
     */
    val DecayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()

    /**
     * Width of the edge area that allows swiping to open the drawer.
     */
    val EdgeSwipeWidth: Dp = 40.dp

    /**
     * Velocity threshold for fling-based state change.
     * If the fling velocity exceeds this, the drawer will animate to the next anchor
     * regardless of the positional threshold.
     */
    val VelocityThreshold: Dp = 400.dp
}

/**
 * Content inside a modal navigation drawer.
 *
 * @param modifier The [Modifier] to be applied to this drawer sheet.
 * @param drawerContainerColor The color used for the background of this drawer sheet.
 * @param drawerContentColor The preferred color for content inside this drawer sheet.
 * @param drawerTonalElevation A higher tonal elevation value will result in a darker
 * color in light theme and lighter color in dark theme.
 * @param drawerWidth The width of the drawer sheet. If not specified, uses the width
 * provided by the parent drawer.
 * @param content Content inside the drawer sheet.
 */
@Composable
fun ModalDrawerSheet(
    modifier: Modifier = Modifier,
    drawerContainerColor: Color = DrawerDefaults.containerColor,
    drawerContentColor: Color = contentColorFor(drawerContainerColor),
    drawerTonalElevation: Dp = DrawerDefaults.ModalDrawerElevation,
    drawerWidth: Dp? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = if (drawerWidth != null) {
        modifier
            .fillMaxHeight()
            .width(drawerWidth)
    } else {
        modifier.fillMaxHeight()
    }

    Surface(
        modifier = finalModifier,
        color = drawerContainerColor,
        contentColor = drawerContentColor,
        tonalElevation = drawerTonalElevation,
    ) {
        Column(content = content)
    }
}

/**
 * Material Design navigation drawer item.
 *
 * @param label Text label for this item.
 * @param selected Whether this item is selected.
 * @param onClick Called when this item is clicked.
 * @param modifier The [Modifier] to be applied to this item.
 * @param icon Optional leading icon for this item.
 * @param badge Optional trailing badge for this item.
 */
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(NavigationDrawerTokens.ActiveIndicatorHeight)
            .clickable(onClick = onClick),
        color = backgroundColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                label()
            }
            if (badge != null) {
                Spacer(modifier = Modifier.width(12.dp))
                badge()
            }
        }
    }
}

// ========================================================================================
// NativeStyleModalNavigationDrawer
// Official Compose architecture: anchoredDraggable on the root container Box.
// ========================================================================================

/**
 * Native-style modal navigation drawer that follows the official Jetpack Compose architecture.
 *
 * Key architectural difference from [ModalNavigationDrawer]:
 * - Places [anchoredDraggable] on the **root container Box** (covering the entire screen area)
 * - Does NOT use custom [pointerInput] edge detection
 * - Does NOT support [backPressBoundary] gesture exclusion zones
 * - Relies on the system's EdgeBackGestureHandler to naturally handle gesture conflicts
 *
 * This approach is identical to how the official Compose Material3 ModalNavigationDrawer works:
 * the system intercepts edge swipes as "back gesture" (pilferPointers), and any gesture the
 * system releases will be handled by anchoredDraggable to open/close the drawer.
 *
 * Use this implementation when:
 * - You want behavior identical to native Android Compose Drawer
 * - You don't need boundary-based gesture exclusion
 * - You want the system to naturally handle back gesture vs drawer gesture conflicts
 *
 * @param drawerContent Content inside the drawer (typically [ModalDrawerSheet]).
 * @param modifier The [Modifier] to be applied to this drawer.
 * @param drawerState State of the drawer, used to control or observe the drawer.
 * @param gesturesEnabled Whether the drawer can be interacted by gestures.
 * @param scrimColor Color of the scrim that obscures content when the drawer is open.
 * @param drawerWidth The width of the drawer.
 * @param content Content of the rest of the UI.
 *
 * Example Usage:
 * ```
 * val drawerState = rememberDrawerState()
 * val scope = rememberCoroutineScope()
 *
 * NativeStyleModalNavigationDrawer(
 *     drawerContent = {
 *         ModalDrawerSheet {
 *             Text("Drawer Title", modifier = Modifier.padding(16.dp))
 *             NavigationDrawerItem(
 *                 label = { Text("Item 1") },
 *                 selected = true,
 *                 onClick = { scope.launch { drawerState.close() } }
 *             )
 *         }
 *     },
 *     drawerState = drawerState
 * ) {
 *     // Main content
 *     Button(onClick = { scope.launch { drawerState.open() } }) {
 *         Text("Open Drawer")
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NativeStyleModalNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(),
    gesturesEnabled: Boolean = true,
    scrimColor: Color = DrawerDefaults.scrimColor,
    drawerWidth: Dp = DrawerDefaults.MaximumDrawerWidth,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    // Update anchors using SideEffect (same pattern as official Compose)
    // Closed = drawer off-screen to the left (-drawerWidthPx)
    // Open = drawer fully visible (0f)
    SideEffect {
        drawerState.density = density
        drawerState.updateAnchors(drawerWidthPx)
    }

    val anchoredState = drawerState.anchoredDraggableState
    val currentOffsetX = if (anchoredState.offset.isNaN()) -drawerWidthPx else anchoredState.offset

    // === Official Compose Architecture ===
    // anchoredDraggable is placed on the ROOT Box, covering the entire screen.
    // This means ANY horizontal drag anywhere on the screen will be processed by
    // anchoredDraggable. The system's EdgeBackGestureHandler intercepts edge swipes
    // first (via pilferPointers), so only "released" gestures reach here.
    Box(
        modifier = modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = anchoredState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
            )
    ) {
        // 1. Main content
        Box {
            content()
        }

        // 2. Scrim - visible when drawer is opening or open
        val scrimAlpha = drawerState.progress.coerceIn(0f, 1f)
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = scrimColor.alpha * scrimAlpha))
                    .then(
                        if (drawerState.isOpen || drawerState.isAnimationRunning) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (gesturesEnabled) {
                                        scope.launch { drawerState.close() }
                                    }
                                }
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // 3. Drawer panel - positioned by offset from anchoredDraggableState
        // No anchoredDraggable on the panel itself (unlike the original ModalNavigationDrawer)
        // because the root Box already handles all drag gestures.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth)
                .offset { IntOffset(currentOffsetX.roundToInt(), 0) }
        ) {
            drawerContent()
        }
    }
}
