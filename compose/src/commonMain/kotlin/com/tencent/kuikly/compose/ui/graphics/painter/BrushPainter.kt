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

package com.tencent.kuikly.compose.ui.graphics.painter

import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.core.base.DeclarativeBaseView

/**
 * [Painter] implementation used to fill the provided bounds with the specified [Brush].
 * The intrinsic size of this [Painter] is determined by [Brush.intrinsicSize]
 */
class BrushPainter(
    val brush: Brush,
) : Painter() {

    private var alpha: Float = 1.0f

    override val intrinsicSize: Size
        get() = brush.intrinsicSize

    override fun applyTo(view: DeclarativeBaseView<*, *>) {
        brush.applyTo(view, alpha)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BrushPainter) return false

        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        return brush.hashCode()
    }

    override fun toString(): String {
        return "BrushPainter(brush=$brush)"
    }
}