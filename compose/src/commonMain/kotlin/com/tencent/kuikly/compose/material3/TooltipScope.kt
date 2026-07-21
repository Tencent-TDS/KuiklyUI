/*
 * Copyright 2023 The Android Open Source Project
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

import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.window.PopupPositionProvider

/**
 * Tooltip scope for [TooltipBox] to be used to obtain the [LayoutCoordinates] of the anchor
 * content, and to draw a caret for the tooltip.
 */
sealed interface TooltipScope {
    /**
     * Used to obtain the [LayoutCoordinates] of the anchor content. This can be used to help draw
     * the caret pointing to the anchor content.
     */
    fun obtainAnchorBounds(): LayoutCoordinates?

    /**
     * Used to obtain the [PopupPositionProvider] used. This can be used to help draw the caret
     * pointing to the anchor content.
     */
    fun obtainPositionProvider(): PopupPositionProvider
}

internal class TooltipScopeImpl(
    val getAnchorBounds: () -> LayoutCoordinates?,
    val positionProvider: PopupPositionProvider,
) : TooltipScope {
    override fun obtainAnchorBounds(): LayoutCoordinates? = getAnchorBounds()
    override fun obtainPositionProvider(): PopupPositionProvider = positionProvider
}
