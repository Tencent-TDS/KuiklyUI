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

import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.RowScope
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.foundation.layout.WindowInsetsSides
import com.tencent.kuikly.compose.foundation.layout.defaultMinSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.only
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.windowInsetsPadding
import com.tencent.kuikly.compose.foundation.selection.selectableGroup
import com.tencent.kuikly.compose.foundation.selection.selectable
import com.tencent.kuikly.compose.material3.internal.systemBarsForVisualComponents
import com.tencent.kuikly.compose.material3.tokens.NavigationBarTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.graphics.takeOrElse
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/navigation-bar/overview" class="external"
 * target="_blank">Material Design bottom navigation bar</a>.
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * ![Navigation bar image](https://developer.android.com/images/reference/androidx/compose/material3/navigation-bar.png)
 *
 * [NavigationBar] should contain three to five [NavigationBarItem]s, each representing a singular
 * destination.
 *
 * A simple example looks like:
 * @sample androidx.compose.material3.samples.NavigationBarSample
 *
 * See [NavigationBarItem] for configuration specific to each item, and not the overall
 * [NavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 * [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this navigation bar. Defaults to
 * either the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 * overlay is applied on top of the container. A higher tonal elevation value will result in a
 * darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param windowInsets a window insets of the navigation bar.
 * @param content the content of this navigation bar, typically 3-5 [NavigationBarItem]s
 */
@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = NavigationBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = NavigationBarDefaults.Elevation,
    windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .defaultMinSize(minHeight = NavigationBarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            content = content
        )
    }
}

/**
 * Material Design navigation bar item.
 *
 * Navigation bars offer a persistent and convenient way to switch between primary destinations in
 * an app.
 *
 * The recommended configuration for a [NavigationBarItem] depends on how many items there are
 * inside a [NavigationBar]:
 *
 * - Three destinations: Display icons and text labels for all destinations.
 * - Four destinations: Active destinations display an icon and text label. Inactive destinations
 * display icons, and text labels are recommended.
 * - Five destinations: Active destinations display an icon and text label. Inactive destinations
 * use icons, and use text labels if space permits.
 *
 * A [NavigationBarItem] always shows text labels (if it exists) when selected. Showing text
 * labels if not selected is controlled by [alwaysShowLabel].
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param label optional text label for this item
 * @param alwaysShowLabel whether to always show the label for this item. If `false`, the label will
 * only be shown when this item is selected.
 * @param colors [NavigationBarItemColors] that will be used to resolve the colors used for this
 * item in different states. See [NavigationBarItemDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this item. You can use this to change the item's appearance
 * or preview the item in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 */
@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
    colors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    val styledIcon = @Composable {
        val iconColor by colors.iconColor(selected = selected, enabled = enabled)
        CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
    }

    val styledLabel: @Composable (() -> Unit)? = label?.let {
        @Composable {
            val labelColor by colors.textColor(selected = selected, enabled = enabled)
            val textStyle = NavigationBarTokens.LabelTextFont.value
            ProvideContentColorTextStyle(
                contentColor = labelColor,
                textStyle = textStyle,
                content = label
            )
        }
    }

    // The label's alpha is animated between 0f and 1f if alwaysShowLabel is false.
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected || alwaysShowLabel) 1f else 0f,
        animationSpec = tween(durationMillis = ItemAnimationDurationMillis),
        label = "NavigationBarItemLabelAlpha"
    )

    // Indicator color animation
    val indicatorColor by colors.indicatorColor(selected = selected)

    Box(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null // Ripple not fully supported yet in KuiklyCompose
            )
            .weight(1f)
            .defaultMinSize(minHeight = NavigationBarHeight),
        contentAlignment = Alignment.Center
    ) {
        NavigationBarItemLayout(
            icon = styledIcon,
            label = styledLabel,
            labelAlpha = labelAlpha,
            indicatorColor = indicatorColor,
            selected = selected,
        )
    }
}

/**
 * Internal layout for a NavigationBarItem that places icon and optional label vertically.
 */
@Composable
private fun NavigationBarItemLayout(
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    labelAlpha: Float,
    indicatorColor: Color,
    selected: Boolean,
) {
    Column(
        modifier = Modifier.padding(top = NavigationBarItemVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with optional indicator background
        Box(
            modifier = Modifier
                .size(
                    width = NavigationBarTokens.ActiveIndicatorWidth,
                    height = NavigationBarTokens.ActiveIndicatorHeight
                )
                .background(
                    color = if (selected) indicatorColor else Color.Transparent,
                    shape = NavigationBarTokens.ActiveIndicatorShape.value
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(NavigationBarTokens.IconSize)) {
                icon()
            }
        }

        // Label
        if (label != null) {
            Box(
                modifier = Modifier
                    .padding(top = NavigationBarItemLabelPadding)
                    .graphicsLayer { alpha = labelAlpha }
            ) {
                label()
            }
        }
    }
}

/** Defaults used in [NavigationBar]. */
object NavigationBarDefaults {
    /** Default color for a navigation bar. */
    val containerColor: Color
        @Composable get() = NavigationBarTokens.ContainerColor.value

    /** Default elevation for a navigation bar. */
    val Elevation: Dp = NavigationBarTokens.ContainerElevation

    /** Default [WindowInsets] for a navigation bar. */
    val windowInsets: WindowInsets
        @Composable get() = WindowInsets.systemBarsForVisualComponents
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
}

/** Defaults used in [NavigationBarItem]. */
object NavigationBarItemDefaults {
    /**
     * Creates a [NavigationBarItemColors] with the provided colors according to the Material
     * specification.
     *
     * @param selectedIconColor the color to use for the icon when the item is selected.
     * @param selectedTextColor the color to use for the text label when the item is selected.
     * @param indicatorColor the color to use for the indicator when the item is selected.
     * @param unselectedIconColor the color to use for the icon when the item is unselected.
     * @param unselectedTextColor the color to use for the text label when the item is
     * unselected.
     * @param disabledIconColor the color to use for the icon when the item is disabled.
     * @param disabledTextColor the color to use for the text label when the item is disabled.
     * @return the resulting [NavigationBarItemColors] used for [NavigationBarItem]
     */
    @Composable
    fun colors(
        selectedIconColor: Color = NavigationBarTokens.ActiveIconColor.value,
        selectedTextColor: Color = NavigationBarTokens.ActiveLabelTextColor.value,
        indicatorColor: Color = NavigationBarTokens.ActiveIndicatorColor.value,
        unselectedIconColor: Color = NavigationBarTokens.InactiveIconColor.value,
        unselectedTextColor: Color = NavigationBarTokens.InactiveLabelTextColor.value,
        disabledIconColor: Color = NavigationBarTokens.InactiveIconColor.value.copy(alpha = 0.38f),
        disabledTextColor: Color = NavigationBarTokens.InactiveLabelTextColor.value.copy(alpha = 0.38f),
    ): NavigationBarItemColors = NavigationBarItemColors(
        selectedIconColor = selectedIconColor,
        selectedTextColor = selectedTextColor,
        selectedIndicatorColor = indicatorColor,
        unselectedIconColor = unselectedIconColor,
        unselectedTextColor = unselectedTextColor,
        disabledIconColor = disabledIconColor,
        disabledTextColor = disabledTextColor,
    )
}

/**
 * Represents the colors of the various elements of a navigation item.
 *
 * @param selectedIconColor the color to use for the icon when the item is selected.
 * @param selectedTextColor the color to use for the text label when the item is selected.
 * @param selectedIndicatorColor the color to use for the indicator when the item is selected.
 * @param unselectedIconColor the color to use for the icon when the item is unselected.
 * @param unselectedTextColor the color to use for the text label when the item is unselected.
 * @param disabledIconColor the color to use for the icon when the item is disabled.
 * @param disabledTextColor the color to use for the text label when the item is disabled.
 */
@Immutable
class NavigationBarItemColors(
    val selectedIconColor: Color,
    val selectedTextColor: Color,
    val selectedIndicatorColor: Color,
    val unselectedIconColor: Color,
    val unselectedTextColor: Color,
    val disabledIconColor: Color,
    val disabledTextColor: Color,
) {
    /**
     * Returns a copy of this NavigationBarItemColors, optionally overriding some of the values.
     */
    fun copy(
        selectedIconColor: Color = this.selectedIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedIndicatorColor: Color = this.selectedIndicatorColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedTextColor: Color = this.unselectedTextColor,
        disabledIconColor: Color = this.disabledIconColor,
        disabledTextColor: Color = this.disabledTextColor,
    ) = NavigationBarItemColors(
        selectedIconColor.takeOrElse { this.selectedIconColor },
        selectedTextColor.takeOrElse { this.selectedTextColor },
        selectedIndicatorColor.takeOrElse { this.selectedIndicatorColor },
        unselectedIconColor.takeOrElse { this.unselectedIconColor },
        unselectedTextColor.takeOrElse { this.unselectedTextColor },
        disabledIconColor.takeOrElse { this.disabledIconColor },
        disabledTextColor.takeOrElse { this.disabledTextColor },
    )

    /**
     * Represents the icon color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Composable
    internal fun iconColor(selected: Boolean, enabled: Boolean): State<Color> {
        val targetValue = when {
            !enabled -> disabledIconColor
            selected -> selectedIconColor
            else -> unselectedIconColor
        }
        return animateColorAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = ItemAnimationDurationMillis),
            label = "NavigationBarItemIconColor"
        )
    }

    /**
     * Represents the text color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     * @param enabled whether the item is enabled
     */
    @Composable
    internal fun textColor(selected: Boolean, enabled: Boolean): State<Color> {
        val targetValue = when {
            !enabled -> disabledTextColor
            selected -> selectedTextColor
            else -> unselectedTextColor
        }
        return animateColorAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = ItemAnimationDurationMillis),
            label = "NavigationBarItemTextColor"
        )
    }

    /**
     * Represents the indicator color for this item, depending on whether it is [selected].
     *
     * @param selected whether the item is selected
     */
    @Composable
    internal fun indicatorColor(selected: Boolean): State<Color> {
        val targetValue = if (selected) selectedIndicatorColor else Color.Transparent
        return animateColorAsState(
            targetValue = targetValue,
            animationSpec = tween(durationMillis = ItemAnimationDurationMillis),
            label = "NavigationBarItemIndicatorColor"
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is NavigationBarItemColors) return false

        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (unselectedTextColor != other.unselectedTextColor) return false
        if (selectedIndicatorColor != other.selectedIndicatorColor) return false
        if (disabledIconColor != other.disabledIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedIconColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + unselectedTextColor.hashCode()
        result = 31 * result + selectedIndicatorColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        return result
    }
}

// Navigation bar height from Material Design spec
private val NavigationBarHeight = NavigationBarTokens.ContainerHeight

// Item vertical padding
private val NavigationBarItemVerticalPadding = 12.dp

// Label padding from icon
private val NavigationBarItemLabelPadding = 4.dp

// Animation duration for item color/alpha transitions
private const val ItemAnimationDurationMillis = 150
