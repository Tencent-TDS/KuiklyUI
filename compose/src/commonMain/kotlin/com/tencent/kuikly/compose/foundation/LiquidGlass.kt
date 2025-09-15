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

package com.tencent.kuikly.compose.foundation

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.LayoutAwareModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.GlassEffectStyle
import kotlin.math.max

/**
 * Liquid glass effect style enumeration
 * Currently iOS platform supports two styles: Regular and Clear
 */
typealias LiquidGlassStyle = GlassEffectStyle

/**
 * Log tag for LiquidGlass related logs
 */
private const val TAG = "LiquidGlass"

/**
 * Shared extension function to find ViewContainer from LayoutNode
 * This eliminates code duplication between different Node implementations
 */
private fun LayoutAwareModifierNode.findViewContainer(): com.tencent.kuikly.core.base.ViewContainer<*, *>? {
    return try {
        val layoutNode = requireLayoutNode()
        val kNode = layoutNode as? KNode<*> ?: return null
        kNode.view as? com.tencent.kuikly.core.base.ViewContainer<*, *>
    } catch (e: Exception) {
        // Log error but don't crash the app
        KLog.e(TAG, "Error finding ViewContainer - ${e.message}")
        null
    }
}

/**
 * Add liquid glass effect to Compose components
 * 
 * This modifier provides native glass effect only on iOS platform,
 * automatically downgrades to transparent effect on other platforms.
 * 
 * @param style Glass effect style, defaults to Regular (only supports Regular and Clear)
 * @param tintColor Glass tint color, optional
 * @param interactive Whether to enable interactive effects, defaults to true
 * @param enable Whether to enable glass effect, defaults to true
 * 
 * @sample
 * ```
 * Box(
 *     modifier = Modifier
 *         .size(200.dp)
 *         .liquidGlass(
 *             style = LiquidGlassStyle.Clear,
 *             tintColor = Color.Blue,
 *             interactive = true
 *         )
 * ) {
 *     Text("Glass effect content")
 * }
 * ```
 */
fun Modifier.liquidGlass(
    style: LiquidGlassStyle = LiquidGlassStyle.REGULAR,
    tintColor: Color? = null,
    interactive: Boolean = true,
    enable: Boolean = true
): Modifier {
    // Check if enabled
    if (!enable) {
        return this
    }
    
    return this.then(
        LiquidGlassElement(
            style = style,
            tintColor = tintColor,
            interactive = interactive,
            enable = enable
        )
    )
}

/**
 * Create container for liquid glass components, supporting fusion effects between components
 * 
 * When multiple liquid glass components are close to each other,
 * this modifier can create smooth fusion transition effects.
 * Only effective on iOS platform, automatically ignored on other platforms.
 * 
 * @param spacing Spacing between components, used to control the intensity of fusion effects
 * 
 * @sample
 * ```
 * Row(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .liquidGlassContainer(spacing = 16f)
 * ) {
 *     // Child component content
 * }
 * ```
 */
fun Modifier.liquidGlassContainer(
    spacing: Float
): Modifier {
    // Validate spacing parameter
    val validSpacing = max(0f, spacing)
    if (spacing != validSpacing) {
        KLog.i(TAG, "Invalid spacing value $spacing, using $validSpacing instead")
    }
    
    return this.then(
        LiquidGlassContainerElement(spacing = validSpacing)
    )
}

/**
 * ModifierNodeElement implementation for liquid glass effect
 */
private data class LiquidGlassElement(
    val style: LiquidGlassStyle,
    val tintColor: Color?,
    val interactive: Boolean,
    val enable: Boolean
) : ModifierNodeElement<LiquidGlassNode>() {
    
    override fun create(): LiquidGlassNode = LiquidGlassNode(
        style = style,
        tintColor = tintColor,
        interactive = interactive,
        enable = enable
    )
    
    override fun update(node: LiquidGlassNode) {
        node.updateProperties(
            style = style,
            tintColor = tintColor,
            interactive = interactive,
            enable = enable
        )
    }
    
    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlass"
        properties["style"] = style
        properties["tintColor"] = tintColor
        properties["interactive"] = interactive
        properties["enable"] = enable
    }
}

/**
 * ModifierNodeElement implementation for liquid glass container effect
 */
private data class LiquidGlassContainerElement(
    val spacing: Float
) : ModifierNodeElement<LiquidGlassContainerNode>() {
    
    override fun create(): LiquidGlassContainerNode = LiquidGlassContainerNode(
        spacing = spacing
    )
    
    override fun update(node: LiquidGlassContainerNode) {
        node.updateProperties(spacing = spacing)
    }
    
    override fun InspectorInfo.inspectableProperties() {
        name = "liquidGlassContainer"
        properties["spacing"] = spacing
    }
}

/**
 * ModifierNode implementation for liquid glass effect
 */
private class LiquidGlassNode(
    private var style: LiquidGlassStyle,
    private var tintColor: Color?,
    private var interactive: Boolean,
    private var enable: Boolean
) : DrawModifierNode, LayoutAwareModifierNode, Modifier.Node() {
    
    fun updateProperties(
        style: LiquidGlassStyle,
        tintColor: Color?,
        interactive: Boolean,
        enable: Boolean
    ) {
        val shouldInvalidate = this.style != style ||
                this.tintColor != tintColor ||
                this.interactive != interactive ||
                this.enable != enable
        
        this.style = style
        this.tintColor = tintColor
        this.interactive = interactive
        this.enable = enable
        
        if (shouldInvalidate) {
            applyGlassEffect()
        }
    }
    
    override fun onAttach() {
        super.onAttach()
        applyGlassEffect()
    }
    
    private fun applyGlassEffect() {
        // Get underlying ViewContainer and apply glass effect properties
        val viewContainer = findViewContainer()
        if (viewContainer == null) {
            KLog.e(TAG, "Failed to apply glass effect: ViewContainer not found!")
            return
        }

        try {
            // Use ViewContainer's ContainerAttr to set glass effect
            val containerAttr = viewContainer.getViewAttr()
            containerAttr.glassEffectIOS(
                enable = enable,
                interactive = interactive,
                style = style,
                tintColor = tintColor?.toKuiklyColor()
            )
        } catch (e: Exception) {
            KLog.e(TAG, "Error applying glass effect - ${e.message}")
        }
    }
}

/**
 * ModifierNode implementation for liquid glass container effect
 */
private class LiquidGlassContainerNode(
    private var spacing: Float
) : DrawModifierNode, LayoutAwareModifierNode, Modifier.Node() {
    
    fun updateProperties(
        spacing: Float
    ) {
        val shouldInvalidate = this.spacing != spacing
        
        this.spacing = spacing
        
        if (shouldInvalidate) {
            applyGlassContainerEffect()
        }
    }
    
    override fun onAttach() {
        super.onAttach()
        applyGlassContainerEffect()
    }

    private fun applyGlassContainerEffect() {
        // Get underlying ViewContainer and apply glass container effect properties
        val viewContainer = findViewContainer()
        if (viewContainer == null) {
            KLog.e(TAG, "Failed to apply glass container effect: ViewContainer not found!")
            return
        }

        try {
            // Use ViewContainer's ContainerAttr to set glass container effect
            val containerAttr = viewContainer.getViewAttr()
            containerAttr.glassEffectContainerIOS(spacing)
        } catch (e: Exception) {
            KLog.e(TAG, "Error applying glass container effect - ${e.message}")
        }
    }
}