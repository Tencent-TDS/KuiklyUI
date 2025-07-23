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

package com.tencent.kuikly.core.render.android.expand.component.image

import android.graphics.Canvas

internal object NinePatchHelper {
    @JvmStatic
    fun <T> draw(
        canvas: Canvas, func: (c: Canvas, obj: T) -> Unit, obj: T, srcWidth: Int,
        srcHeight: Int, density: Float, distWidth: Int, distHeight: Int, insets: Insets
    ) {
        assert(srcWidth > 0 && srcHeight > 0)
        val centerLeft = 0 + insets.left
        val centerTop = 0 + insets.top
        val centerRight = distWidth - insets.right
        val centerBottom = distHeight - insets.bottom
        val srcInsetsLeft = insets.left / density
        val srcInsetsTop = insets.top / density
        val srcInsetsRight = insets.right / density
        val srcInsetsBottom = insets.bottom / density
        val scaleX = (centerRight - centerLeft) / (srcWidth - srcInsetsLeft - srcInsetsRight)
        val scaleY = (centerBottom - centerTop) / (srcHeight - srcInsetsTop - srcInsetsBottom)
        // top-left
        canvas.save()
        canvas.clipRect(0, 0, centerLeft, centerTop)
        // canvas.translate(0, 0)
        canvas.scale(density, density)
        func(canvas, obj)
        canvas.restore()
        // top-right
        canvas.save()
        canvas.clipRect(centerRight, 0, distWidth, centerTop)
        canvas.scale(density, density, centerRight.toFloat(), 0f)
        canvas.translate(centerRight + srcInsetsRight - srcWidth, 0f)
        func(canvas, obj)
        canvas.restore()
        // bottom-left
        canvas.save()
        canvas.clipRect(0, centerBottom, centerLeft, distHeight)
        canvas.scale(density, density, 0f, centerBottom.toFloat())
        canvas.translate(0f, centerBottom + srcInsetsBottom - srcHeight)
        func(canvas, obj)
        canvas.restore()
        // bottom-right
        canvas.save()
        canvas.clipRect(centerRight, centerBottom, distWidth, distHeight)
        canvas.scale(density, density, centerRight.toFloat(), centerBottom.toFloat())
        canvas.translate(centerRight + srcInsetsRight - srcWidth, centerBottom + srcInsetsBottom - srcHeight)
        func(canvas, obj)
        canvas.restore()
        // left
        canvas.save()
        canvas.clipRect(0, centerTop, centerLeft, centerBottom)
        canvas.scale(density, scaleY, 0f, centerTop.toFloat())
        canvas.translate(0f, centerTop - srcInsetsTop)
        func(canvas, obj)
        canvas.restore()
        // right
        canvas.save()
        canvas.clipRect(centerRight, centerTop, distWidth, centerBottom)
        canvas.scale(density, scaleY, centerRight.toFloat(), centerTop.toFloat())
        canvas.translate(centerRight + srcInsetsRight - srcWidth, centerTop - srcInsetsTop)
        func(canvas, obj)
        canvas.restore()
        // top
        canvas.save()
        canvas.clipRect(centerLeft, 0, centerRight, centerTop)
        canvas.scale(scaleX, density, centerLeft.toFloat(), 0f)
        canvas.translate(centerLeft - srcInsetsLeft, 0f)
        func(canvas, obj)
        canvas.restore()
        // bottom
        canvas.save()
        canvas.clipRect(centerLeft, centerBottom, centerRight, distHeight)
        canvas.scale(scaleX, density, centerLeft.toFloat(), centerBottom.toFloat())
        canvas.translate(centerLeft - srcInsetsLeft, centerBottom + srcInsetsBottom - srcHeight)
        func(canvas, obj)
        canvas.restore()
        // center
        canvas.save()
        canvas.clipRect(centerLeft, centerTop, centerRight, centerBottom)
        canvas.scale(scaleX, scaleY, centerLeft.toFloat(), centerTop.toFloat())
        canvas.translate(centerLeft - srcInsetsLeft, centerTop - srcInsetsTop)
        func(canvas, obj)
        canvas.restore()
    }
}

internal data class Insets(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun isZero() = left == 0 && top == 0 && right == 0 && bottom == 0
}