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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer

/**
 * Liquid glass effect view for iOS 26+ with blur and transparency effects.
 * Provides native glass-like appearance with configurable properties.
 */
fun ViewContainer<*, *>.LiquidGlass(init: LiquidGlassView.() -> Unit) {
    addChild(LiquidGlassView(), init)
}

/**
 * Container view that applies glass effect styling to its children.
 * Useful for grouping multiple glass elements with consistent styling.
 */
fun ViewContainer<*, *>.GlassEffectContainer(init: GlassEffectContainerView.() -> Unit) {
    addChild(GlassEffectContainerView(), init)
}

/**
 * Core liquid glass effect view implementation.
 */
class LiquidGlassView : ViewContainer<LiquidGlassViewAttr, LiquidGlassViewEvent>() {
    override fun createEvent(): LiquidGlassViewEvent = LiquidGlassViewEvent()
    override fun createAttr(): LiquidGlassViewAttr = LiquidGlassViewAttr()
    override fun viewName(): String = ViewConst.TYPE_IOS_LIQUID_GLASS_VIEW
}

/**
 * Glass effect container view implementation.
 */
class GlassEffectContainerView : ViewContainer<GlassEffectContainerAttr, GlassEffectContainerEvent>() {
    override fun createAttr(): GlassEffectContainerAttr = GlassEffectContainerAttr()
    override fun createEvent(): GlassEffectContainerEvent = GlassEffectContainerEvent()
    override fun viewName(): String = ViewConst.TYPE_IOS_GLASS_EFFECT_CONTAINER_VIEW
}

/**
 * Styles for liquid glass effects.
 */
enum class GlassEffectStyle(val value: String) {
    REGULAR("regular"),
    CLEAR("clear")
}

/**
 * Attributes for configuring liquid glass effects.
 */
class LiquidGlassViewAttr : ComposeAttr() {
    /**
     * Controls whether the glass effect responds to user interaction.
     * @param isInteractive true to enable interaction, false for static effect
     */
    fun glassEffectInteractive(isInteractive: Boolean) {
        StyleConst.GLASS_EFFECT_INTERACTIVE with isInteractive
    }

    /**
     * Sets the tint color for the glass effect.
     * @param color The tint color to apply
     */
    fun glassEffectTintColor(color: Color) {
        StyleConst.GLASS_EFFECT_TINT_COLOR with color.toString()
    }

    /**
     * Sets the style of the glass effect.
     * @param style The glass effect style to apply
     */
    fun glassEffectStyle(style: GlassEffectStyle) {
        StyleConst.GLASS_EFFECT_STYLE with style.value
    }
}

/**
 * Attributes for glass effect containers.
 */
class GlassEffectContainerAttr : ComposeAttr() {
    /**
     * Sets the spacing between glass effect elements.
     * @param spacing The spacing in points
     */
    fun spacing(spacing: Float) {
        require(spacing >= 0f) { "Spacing must be non-negative" }
        StyleConst.GLASS_EFFECT_SPACING with spacing
    }
}

/**
 * Events for liquid glass view.
 */
class LiquidGlassViewEvent : ComposeEvent()

/**
 * Events for glass effect container.
 */
class GlassEffectContainerEvent : ComposeEvent()