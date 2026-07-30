/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.graphics.painter

import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.modulate
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * [Painter] implementation used to fill the provided bounds with the specified color
 */
class ColorPainter(val color: Color) : Painter() {
    private var alpha: Float = 1.0f

//    private var colorFilter: ColorFilter? = null

    override fun applyTo(view: DeclarativeBaseView<*, *>) {
//        drawRect(color = color, alpha = alpha, colorFilter = colorFilter)
        view.getViewAttr().backgroundColor(color.modulate(alpha).toKuiklyColor())
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

//    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
//        this.colorFilter = colorFilter
//        return true
//    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorPainter) return false

        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        return color.hashCode()
    }

    override fun toString(): String {
        return "ColorPainter(color=$color)"
    }

    /**
     * Drawing a color does not have an intrinsic size, return [Size.Unspecified] here
     */
    override val intrinsicSize: Size = Size.Unspecified
}