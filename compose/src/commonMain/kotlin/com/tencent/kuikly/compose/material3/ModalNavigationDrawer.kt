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
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
 * State of the [ModalNavigationDrawer] and [DismissibleNavigationDrawer] composable.
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
class DrawerState(
    initialValue: DrawerValue = DrawerValue.Closed,
    val confirmStateChange: (DrawerValue) -> Boolean = { true }
) {

    internal val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        positionalThreshold = { distance -> distance * DrawerPositionalThreshold },
        velocityThreshold = { DrawerVelocityThresholdPx },
        snapAnimationSpec = DrawerDefaults.AnimationSpec,
        decayAnimationSpec = DrawerDefaults.DecayAnimationSpec,
        confirmValueChange = confirmStateChange
    )

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
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the start the drawer
     * currently in. If a swipe or an animation is in progress, this corresponds the state drawer
     * was in before the swipe or animation started.
     */
    val currentValue: DrawerValue
        get() = anchoredDraggableState.currentValue

    /**
     * Whether the state is currently animating.
     */
    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * Open the drawer with animation and suspend until it if fully opened or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the open animation ended
     */
    suspend fun open() {
        anchoredDraggableState.animateTo(DrawerValue.Open)
    }

    /**
     * Close the drawer with animation and suspend until it if fully closed or animation has been
     * cancelled. This method will throw [CancellationException] if the animation is
     * interrupted
     *
     * @return the reason the close animation ended
     */
    suspend fun close() {
        anchoredDraggableState.animateTo(DrawerValue.Closed)
    }

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: DrawerValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * The target value of the drawer state.
     *
     * If a swipe is in progress, this is the value that the Drawer would animate to if the
     * swipe finishes. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: DrawerValue
        get() = anchoredDraggableState.targetValue

    /**
     * The current position (in pixels) of the drawer sheet, or Float.NaN before the offset is
     * initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float
        get() = anchoredDraggableState.offset

    /**
     * The current position (in pixels) of the drawer sheet.
     * Throws if the offset has not been initialized yet.
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /**
     * The progress of the drawer opening, from 0f (closed) to 1f (open).
     */
    val progress: Float
        get() = anchoredDraggableState.progress(DrawerValue.Closed, DrawerValue.Open)

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
         * The default [Saver] implementation for [DrawerState].
         */
        fun Saver(confirmStateChange: (DrawerValue) -> Boolean) =
            Saver<DrawerState, DrawerValue>(
                save = { it.currentValue },
                restore = { DrawerState(it, confirmStateChange) }
            )
    }
}

private val DrawerPositionalThreshold = 0.5f
private var DrawerVelocityThresholdPx: Float = 400f

/**
 * Create and [remember] a [DrawerState].
 *
 * @param initialValue The initial value of the state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
fun rememberDrawerState(
    initialValue: DrawerValue = DrawerValue.Closed,
    confirmStateChange: (DrawerValue) -> Boolean = { true }
): DrawerState {
    val density = LocalDensity.current
    return rememberSaveable(saver = DrawerState.Saver(confirmStateChange)) {
        DrawerState(initialValue, confirmStateChange)
    }.also {
        it.density = density
        // Update velocity threshold based on density (400.dp → px)
        DrawerVelocityThresholdPx = with(density) { DrawerDefaults.VelocityThreshold.toPx() }
    }
}

/**
 * Object to hold default values for [ModalNavigationDrawer]
 */
object DrawerDefaults {

    /**
     * Default and maximum width of a navigation drawer
     */
    val MaximumDrawerWidth: Dp = NavigationDrawerTokens.ContainerWidth

    /**
     * Default color of the scrim that obscures content when the drawer is open
     */
    val scrimColor: Color = Color.Black.copy(alpha = 0.32f)

    /**
     * Default container color for a navigation drawer
     */
    val containerColor: Color = Color.White

    /**
     * Default Elevation for drawer container in the [ModalNavigationDrawer] as specified in the
     * Material specification
     */
    val ModalDrawerElevation: Dp = 1.dp

    /**
     * Default snap animation spec for drawer open/close transitions
     */
    val AnimationSpec: AnimationSpec<Float> = tween(durationMillis = 256)

    /**
     * Default decay animation spec for drawer fling behavior
     */
    val DecayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()

    /**
     * Width of the edge area that allows swiping to open the drawer
     */
    val EdgeSwipeWidth: Dp = 40.dp

    /**
     * Velocity threshold for fling-based state change.
     * If the fling velocity exceeds this, the drawer will animate to the next anchor
     * regardless of the positional threshold
     */
    val VelocityThreshold: Dp = 400.dp
}

/**
 * Content inside of a modal navigation drawer.
 *
 * @param modifier the [Modifier] to be applied to this drawer's content
 * @param drawerContainerColor the color used for the background of this drawer. Use
 * [Color.Transparent] to have no color.
 * @param drawerContentColor the preferred color for content inside this drawer. Defaults to either
 * the matching content color for [drawerContainerColor], or to the current [LocalContentColor] if
 * [drawerContainerColor] is not a color from the theme.
 * @param drawerTonalElevation when [drawerContainerColor] is [ColorScheme.surface], a translucent
 * primary color overlay is applied on top of the container. A higher tonal elevation value will
 * result in a darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param drawerWidth The width of the drawer sheet. If not specified, uses the width
 * provided by the parent drawer.
 * @param content content inside of a modal navigation drawer
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
 * A [NavigationDrawerItem] represents a destination within drawers, either [ModalNavigationDrawer],
 * [ModalNavigationDrawerLegacy] or [DismissibleNavigationDrawer].
 *
 * @param label text label for this item
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param modifier the [Modifier] to be applied to this item
 * @param icon optional icon for this item, typically an [Icon]
 * @param badge optional badge to show on this item from the end side
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

/**
 * <a href="https://m3.material.io/components/navigation-drawer/overview" class="external" target="_blank">Material Design navigation drawer</a>.
 *
 * Navigation drawers provide ergonomic access to destinations in an app.
 *
 * Modal navigation drawers block interaction with the rest of an app's content with a scrim.
 * They are elevated above most of the app's UI and don't affect the screen's layout grid.
 *
 * ![Navigation drawer image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * This implementation follows the official Jetpack Compose architecture:
 * - Places [anchoredDraggable] on the root container Box (covering the entire screen area)
 * - Relies on the system's EdgeBackGestureHandler to naturally handle gesture conflicts
 *
 * For backPress boundary support, use [ModalNavigationDrawerLegacy].
 *
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param scrimColor color of the scrim that obscures content when the drawer is open
 * @param drawerWidth the width of the drawer
 * @param content content of the rest of the UI
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModalNavigationDrawer(
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .anchoredDraggable(
                state = anchoredState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
            )
    ) {
        // Main content
        Box {
            content()
        }

        // Scrim
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

        // Drawer sheet
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

/**
 * <a href="https://m3.material.io/components/navigation-drawer/overview" class="external" target="_blank">Material Design navigation drawer</a>.
 *
 * Navigation drawers provide ergonomic access to destinations in an app. They're often next to
 * app content and affect the screen's layout grid.
 *
 * ![Navigation drawer image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-drawer.png)
 *
 * Dismissible standard drawers can be used for layouts that prioritize content (such as a
 * photo gallery) or for apps where users are unlikely to switch destinations often. They should
 * use a visible navigation menu icon to open and close the drawer.
 *
 * Unlike [ModalNavigationDrawer], when a dismissible drawer opens, the main content is pushed
 * to the right (offset by the drawer width) instead of being covered by a scrim overlay.
 * Both the drawer panel and the main content animate together.
 *
 * @param drawerContent content inside this drawer
 * @param modifier the [Modifier] to be applied to this drawer
 * @param drawerState state of the drawer
 * @param gesturesEnabled whether or not the drawer can be interacted by gestures
 * @param drawerWidth the width of the drawer
 * @param content content of the rest of the UI
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DismissibleNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(),
    gesturesEnabled: Boolean = true,
    drawerWidth: Dp = DrawerDefaults.MaximumDrawerWidth,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { drawerWidth.toPx() }

    // Update anchors using SideEffect (same pattern as official Compose)
    // Closed = drawer off-screen to the left (-drawerWidthPx)
    // Open = drawer fully visible (0f)
    SideEffect {
        drawerState.density = density
        drawerState.updateAnchors(drawerWidthPx)
    }

    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .anchoredDraggable(
                state = drawerState.anchoredDraggableState,
                orientation = Orientation.Horizontal,
                enabled = gesturesEnabled,
            )
    ) {
        Layout(content = {
            // measurables[0]: drawer content
            Box {
                drawerContent()
            }
            // measurables[1]: main content
            Box {
                content()
            }
        }) { measurables, constraints ->
            val sheetPlaceable = measurables[0].measure(constraints)
            val contentPlaceable = measurables[1].measure(constraints)
            layout(contentPlaceable.width, contentPlaceable.height) {
                // Main content: offset = drawerWidth + currentOffset
                // When closed (offset = -drawerWidthPx): content at 0 (normal position)
                // When open (offset = 0): content at drawerWidthPx (pushed right)
                contentPlaceable.placeRelative(
                    sheetPlaceable.width + drawerState.requireOffset().roundToInt(),
                    0
                )
                // Drawer sheet: offset = currentOffset
                // When closed (offset = -drawerWidthPx): drawer off-screen to the left
                // When open (offset = 0): drawer at left edge, fully visible
                sheetPlaceable.placeRelative(
                    drawerState.requireOffset().roundToInt(),
                    0
                )
            }
        }
    }
}
