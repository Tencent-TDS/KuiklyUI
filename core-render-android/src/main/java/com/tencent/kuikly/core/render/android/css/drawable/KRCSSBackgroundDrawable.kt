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

package com.tencent.kuikly.core.render.android.css.drawable

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.SizeF
import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import com.tencent.kuikly.core.render.android.css.ktx.toPxI

/**
 * 实现的样式包含:
 * 1.圆角
 * 2.渐变
 * 3.边框
 */
class KRCSSBackgroundDrawable : GradientDrawable() {

    /**
     * 是否为前景, android系统的前景Drawable没适配scrollX, scrollY场景，但是背景Drawable却有适配...
     */
    var isForeground = false
    var targetView: View? = null
    var kuiklyContext: IKuiklyRenderContext? = null


    private var borderRadiusF = BORDER_RADIUS_UNSET_VALUE
    private var borderRadii: FloatArray? = null
    var borderRadius: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }

            val borders = value.split(",")
            if (borders.size == BORDER_ELEMENT_SIZE) {
                val tl = kuiklyContext.toPxF(borders[BORDER_TOP_LEFT_INDEX].toFloat())
                val tr = kuiklyContext.toPxF(borders[BORDER_TOP_RIGHT_INDEX].toFloat())
                val bl = kuiklyContext.toPxF(borders[BORDER_BOTTOM_LEFT_INDEX].toFloat())
                val br = kuiklyContext.toPxF(borders[BORDER_BOTTOM_RIGHT_INDEX].toFloat())

                val raddi = floatArrayOf(
                    tl, tl,
                    tr, tr,
                    br, br,
                    bl, bl
                )
                if (isAllBorderRadiusEqual(raddi)) {
                    cornerRadius = tl
                    borderRadiusF = tl
                } else {
                    cornerRadii = raddi
                    borderRadii = raddi
                }
                field = value
            }
        }

    var borderWidth = BORDER_WIDTH_DEFAULT_VALUE
    var borderStyle: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }

            val borderStyles = value.split(KRCssConst.BLANK_SEPARATOR)
            if (borderStyles.size != BORDER_STYLE_ELEMENT_SIZE) {
                return
            }

            val lindWidth = kuiklyContext.toPxI(borderStyles[BORDER_STYLE_WIDTH_INDEX].toFloat())
            val lineStyle = borderStyles[BORDER_LINE_STYLE_INDEX]
            val lineColor = borderStyles[BORDER_STYLE_LINE_COLOR].toColor()

            when (lineStyle) {
                "solid" -> {
                    setStroke(lindWidth, ColorStateList.valueOf(lineColor))
                }
                "dashed" -> {
                    setStroke(lindWidth, ColorStateList.valueOf(lineColor), kuiklyContext.toPxF(BORDER_DASH_WIDTH), kuiklyContext.toPxF(BORDER_DASH_GAP))
                }
                "dotted" -> {
                    setStroke(lindWidth,
                        ColorStateList.valueOf(lineColor),
                        lindWidth.toFloat(),
                        kuiklyContext.toPxF(BORDER_DASH_GAP)
                    )
                }
            }

            borderWidth = lindWidth
            field = value
        }

    var backgroundImage: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            updateBackgroundImage(value)
            field = value
        }

    private fun updateBackgroundImage(backgroundImage: String) {
        if (backgroundImage == KRCssConst.EMPTY_STRING) {
            colors = intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT) // 清除渐变背景
        } else {
            val backgroundImageTriple = parseBackgroundImage(backgroundImage)
            orientation = backgroundImageTriple.first
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setColors(backgroundImageTriple.second, backgroundImageTriple.third)
            } else {
                colors = backgroundImageTriple.second
            }
        }
    }

    override fun draw(canvas: Canvas) {
        if (isForeground) {
            val scrollX = targetView?.scrollX?.toFloat() ?: 0f
            val scrollY = targetView?.scrollY?.toFloat() ?: 0f
            drawWithScrollXY(scrollX, scrollY, canvas)
        } else {
            super.draw(canvas)
        }
    }

    private fun drawWithScrollXY(scrollX: Float, scrollY: Float, canvas: Canvas) {
        if (scrollX == 0f && scrollY == 0f) {
            super.draw(canvas)
        } else {
            canvas.translate(scrollX, scrollY)
            super.draw(canvas)
            canvas.translate(-scrollX, -scrollY)
        }
    }

    companion object {
        const val BORDER_ELEMENT_SIZE = 4
        const val BORDER_TOP_LEFT_INDEX = 0
        const val BORDER_TOP_RIGHT_INDEX = 1
        const val BORDER_BOTTOM_LEFT_INDEX = 2
        const val BORDER_BOTTOM_RIGHT_INDEX = 3

        private const val BORDER_RADII_TL = 0
        private const val BORDER_RADII_TR = 2
        private const val BORDER_RADII_BL = 4
        private const val BORDER_RADII_BR = 6

        private const val BORDER_STYLE_ELEMENT_SIZE = 3
        private const val BORDER_STYLE_WIDTH_INDEX = 0
        private const val BORDER_LINE_STYLE_INDEX = 1
        private const val BORDER_STYLE_LINE_COLOR = 2

        private const val BORDER_RADIUS_UNSET_VALUE = -1.0f
        private const val BORDER_WIDTH_DEFAULT_VALUE = 0

        private val BORDER_DASH_GAP = 1f.toPxF()
        private val BORDER_DASH_WIDTH = 4f.toPxF()

        private const val BACKGROUND_IMAGE_DIRECTION_INDEX = 0
        private const val BACKGROUND_IMAGE_DIRECTION_BOTTOM_TOP = 0
        private const val BACKGROUND_IMAGE_DIRECTION_TOP_BOTTOM = 1
        private const val BACKGROUND_IMAGE_DIRECTION_RIGHT_LEFT = 2
        private const val BACKGROUND_IMAGE_DIRECTION_LEFT_RIGHT = 3
        private const val BACKGROUND_IMAGE_DIRECTION_BR_TL = 4
        private const val BACKGROUND_IMAGE_DIRECTION_BL_TR = 5
        private const val BACKGROUND_IMAGE_DIRECTION_TR_BL = 6
        private const val BACKGROUND_IMAGE_DIRECTION_TL_BR = 7
        private const val BACKGROUND_IMAGE_COLORS_COLOR_INDEX = 0
        private const val BACKGROUND_IMAGE_COLORS_OFFSET_INDEX = 1

        fun isAllBorderRadiusEqual(radii: FloatArray): Boolean {
            val tl = radii[BORDER_RADII_TL]
            val tr = radii[BORDER_RADII_TR]
            val bl = radii[BORDER_RADII_BL]
            val br = radii[BORDER_RADII_BR]
            return tl == tr && tl == bl && tl == br
        }

        fun parseBackgroundImage(backgroundImage: String): Triple<Orientation, IntArray, FloatArray> {
            val linearGradientPrefix = "linear-gradient("
            val lg = backgroundImage.substring(linearGradientPrefix.length, backgroundImage.length - 1)
            val splits = lg.split(",")

            // parse color
            val colors = IntArray(splits.size - 1) // 因为是从1开始遍历, 所以size要减1
            val offsets = FloatArray(splits.size - 1) // 因为是从1开始遍历, 所以size要减1
            for (i in 1 until splits.size) { // colors在splits数组中的index为1, 因此从1开始遍历
                val colorAndOffset = splits[i].trim().split(KRCssConst.BLANK_SEPARATOR)

                val color = colorAndOffset[BACKGROUND_IMAGE_COLORS_COLOR_INDEX]
                colors[i - 1] = color.toColor()

                offsets[i - 1] = colorAndOffset[BACKGROUND_IMAGE_COLORS_OFFSET_INDEX].toFloat()
            }

            // parse direction
            val direction = convertDirection(splits[BACKGROUND_IMAGE_DIRECTION_INDEX].toInt())

            return Triple(direction, colors, offsets)
        }

        fun parseLinearGradient(backgroundImage: String, size: SizeF, titleMode: Shader.TileMode): LinearGradient? {
            val backgroundImageParseTriple = parseBackgroundImage(backgroundImage)
            val x0: Float
            val x1: Float
            val y0: Float
            val y1: Float
            val r = RectF().apply {
                left = 0f
                top = 0f
                right = size.width
                bottom = size.height
            }

            when (backgroundImageParseTriple.first) {
                GradientDrawable.Orientation.TOP_BOTTOM -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = x0
                    y1 = r.bottom
                }
                GradientDrawable.Orientation.TR_BL -> {
                    x0 = r.right
                    y0 = r.top
                    x1 = r.left
                    y1 = r.bottom
                }
                GradientDrawable.Orientation.RIGHT_LEFT -> {
                    x0 = r.right
                    y0 = r.top
                    x1 = r.left
                    y1 = y0
                }
                GradientDrawable.Orientation.BR_TL -> {
                    x0 = r.right
                    y0 = r.bottom
                    x1 = r.left
                    y1 = r.top
                }
                GradientDrawable.Orientation.BOTTOM_TOP -> {
                    x0 = r.left
                    y0 = r.bottom
                    x1 = x0
                    y1 = r.top
                }
                GradientDrawable.Orientation.BL_TR -> {
                    x0 = r.left
                    y0 = r.bottom
                    x1 = r.right
                    y1 = r.top
                }
                GradientDrawable.Orientation.LEFT_RIGHT -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = r.right
                    y1 = y0
                }
                else -> {
                    x0 = r.left
                    y0 = r.top
                    x1 = r.right
                    y1 = r.bottom
                }
            }

            return LinearGradient(
                x0,
                y0,
                x1,
                y1,
                backgroundImageParseTriple.second,
                backgroundImageParseTriple.third,
                titleMode
            )
        }

        private fun convertDirection(direction: Int): Orientation {
            return when (direction) {
                BACKGROUND_IMAGE_DIRECTION_BOTTOM_TOP -> Orientation.BOTTOM_TOP
                BACKGROUND_IMAGE_DIRECTION_TOP_BOTTOM -> Orientation.TOP_BOTTOM
                BACKGROUND_IMAGE_DIRECTION_RIGHT_LEFT -> Orientation.RIGHT_LEFT
                BACKGROUND_IMAGE_DIRECTION_LEFT_RIGHT -> Orientation.LEFT_RIGHT
                BACKGROUND_IMAGE_DIRECTION_BR_TL -> Orientation.BR_TL
                BACKGROUND_IMAGE_DIRECTION_BL_TR -> Orientation.BL_TR
                BACKGROUND_IMAGE_DIRECTION_TR_BL -> Orientation.TR_BL
                BACKGROUND_IMAGE_DIRECTION_TL_BR -> Orientation.TL_BR
                else -> Orientation.BOTTOM_TOP
            }
        }
    }
}
