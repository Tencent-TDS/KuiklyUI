/*
 * Copyright 2022 The Android Open Source Project
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

import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.core.LinearEasing
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.updateTransition
import com.tencent.kuikly.compose.foundation.interaction.Interaction
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.requiredWidth
import com.tencent.kuikly.compose.foundation.selection.selectable
import com.tencent.kuikly.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.text.ProvideTextStyle
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.FirstBaseline
import com.tencent.kuikly.compose.ui.layout.LastBaseline
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.layout.Placeable
import com.tencent.kuikly.compose.ui.layout.layoutId
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.util.fastFirst
import kotlin.math.max

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design tab.</a>
 *
 * A default Tab, also known as a Primary Navigation Tab. Tabs organize content across different
 * screens, data sets, and other interactions.
 *
 * ![Tabs
 * image](https://developer.android.com/images/reference/androidx/compose/material3/secondary-tabs.png)
 *
 * A Tab represents a single page of content using a text label and/or icon. It represents its
 * selected state by tinting the text label and/or image with [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * This Tab has slots for [text] and/or [icon] - see the other Tab overload for a generic Tab that
 * is not opinionated about its content.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @see LeadingIconTab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null
) {
    val styledText: @Composable (() -> Unit)? =
        text?.let {
            @Composable {
                val style =
                    PrimaryNavigationTabTokens.LabelTextFont.value.copy(
                        textAlign = TextAlign.Center
                    )
                ProvideTextStyle(style, content = text)
            }
        }
    Tab(
        selected,
        onClick,
        modifier,
        enabled,
        selectedContentColor,
        unselectedContentColor,
        interactionSource
    ) {
        TabBaselineLayout(icon = icon, text = styledText)
    }
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design tab.</a>
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * A LeadingIconTab represents a single page of content using a text label and an icon in front of
 * the label. It represents its selected state by tinting the text label and icon with
 * [selectedContentColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab. Should be 24.dp.
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @see Tab
 */
@Composable
fun LeadingIconTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null
) {
    // The color of the Ripple should always the be selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
//    val ripple = rippleOrFallbackImplementation(bounded = true, color = selectedContentColor)
    val ripple = null

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Row(
            modifier =
                modifier
                    .height(SmallTabHeight)
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple
                    )
                    .padding(horizontal = HorizontalTextPadding)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(Modifier.requiredWidth(TextDistanceFromLeadingIcon))
            val style =
                PrimaryNavigationTabTokens.LabelTextFont.value.copy(textAlign = TextAlign.Center)
            ProvideTextStyle(style, content = text)
        }
    }
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design tab.</a>
 *
 * Tabs organize content across different screens, data sets, and other interactions.
 *
 * ![Tabs
 * image](https://developer.android.com/images/reference/androidx/compose/material3/secondary-tabs.png)
 *
 * Generic [Tab] overload that is not opinionated about content / color. See the other overload for
 * a Tab that has specific slots for text and / or an icon, as well as providing the correct colors
 * for selected / unselected states.
 *
 * A custom tab using this API may look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * @param selected whether this tab is selected or not
 * @param onClick called when this tab is clicked
 * @param modifier the [Modifier] to be applied to this tab
 * @param enabled controls the enabled state of this tab. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param selectedContentColor the color for the content of this tab when selected, and the color of
 *   the ripple.
 * @param unselectedContentColor the color for the content of this tab when not selected
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this tab. You can use this to change the tab's appearance or
 *   preview the tab in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the content of this tab
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // The color of the Ripple should always the selected color, as we want to show the color
    // before the item is considered selected, and hence before the new contentColor is
    // provided by TabTransition.
//    val ripple = rippleOrFallbackImplementation(bounded = true, color = selectedContentColor)
    val ripple = null

    TabTransition(selectedContentColor, unselectedContentColor, selected) {
        Column(
            modifier =
                modifier
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource,
                        indication = ripple
                    )
                    .wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

/**
 * Transition defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [LocalContentColor] to provide an interpolated value between [activeColor] and
 * [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(selected)
    val color by
        transition.animateColor(
            transitionSpec = {
                if (false isTransitioningTo true) {
                    tween(
                        durationMillis = TabFadeInAnimationDuration,
                        delayMillis = TabFadeInAnimationDelay,
                        easing = LinearEasing
                    )
                } else {
                    tween(durationMillis = TabFadeOutAnimationDuration, easing = LinearEasing)
                }
            }
        ) {
            if (it) activeColor else inactiveColor
        }
    CompositionLocalProvider(LocalContentColor provides color, content = content)
}

/**
 * A [Layout] that positions [text] and an optional [icon] with the correct baseline distances. This
 * Layout will either be [SmallTabHeight] or [LargeTabHeight] depending on its content, and then
 * place the text and/or icon inside with the correct baseline alignment.
 */
@Composable
private fun TabBaselineLayout(text: @Composable (() -> Unit)?, icon: @Composable (() -> Unit)?) {
    Layout({
        if (text != null) {
            Box(Modifier.layoutId("text").padding(horizontal = HorizontalTextPadding)) { text() }
        }
        if (icon != null) {
            Box(Modifier.layoutId("icon")) { icon() }
        }
    }) { measurables, constraints ->
        val textPlaceable =
            text?.let {
                measurables
                    .fastFirst { it.layoutId == "text" }
                    .measure(
                        // Measure with loose constraints for height as we don't want the text to
                        // take up more
                        // space than it needs
                        constraints.copy(minHeight = 0)
                    )
            }

        val iconPlaceable =
            icon?.let { measurables.fastFirst { it.layoutId == "icon" }.measure(constraints) }

        val tabWidth = max(textPlaceable?.width ?: 0, iconPlaceable?.width ?: 0)

        val specHeight =
            if (textPlaceable != null && iconPlaceable != null) {
                    LargeTabHeight
                } else {
                    SmallTabHeight
                }
                .roundToPx()

        val tabHeight =
            max(
                specHeight,
                (iconPlaceable?.height ?: 0) +
                    (textPlaceable?.height ?: 0) +
                    IconDistanceFromBaseline.roundToPx()
            )

        layout(tabWidth, tabHeight) {
            when {
                textPlaceable != null && iconPlaceable != null -> {
                    // 固定间距
                    val spacing = 8.dp.roundToPx()
                    
                    // 计算总高度
                    val totalHeight = iconPlaceable.height + spacing + textPlaceable.height
                    
                    // 计算起始Y坐标，使内容垂直居中
                    val startY = (tabHeight - totalHeight) / 2
                    
                    // 放置图标
                    val iconX = (tabWidth - iconPlaceable.width) / 2
                    val iconY = startY
                    iconPlaceable.placeRelative(iconX, iconY)
                    
                    // 放置文本
                    val textX = (tabWidth - textPlaceable.width) / 2
                    val textY = iconY + iconPlaceable.height + spacing
                    textPlaceable.placeRelative(textX, textY)
                }
                textPlaceable != null -> {
                    // 文本垂直居中
                    val textX = (tabWidth - textPlaceable.width) / 2
                    val textY = (tabHeight - textPlaceable.height) / 2
                    textPlaceable.placeRelative(textX, textY)
                }
                iconPlaceable != null -> {
                    // 图标垂直居中
                    val iconX = (tabWidth - iconPlaceable.width) / 2
                    val iconY = (tabHeight - iconPlaceable.height) / 2
                    iconPlaceable.placeRelative(iconX, iconY)
                }
            }
        }
    }
}

// Tab specifications
private val SmallTabHeight = PrimaryNavigationTabTokens.ContainerHeight
private val LargeTabHeight = 72.dp

// Tab transition specifications
private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100

// The horizontal padding on the left and right of text
internal val HorizontalTextPadding = 0.dp

// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp
// Distance from the end of the leading icon to the start of the text
private val TextDistanceFromLeadingIcon = 8.dp

