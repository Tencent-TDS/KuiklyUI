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

package com.tencent.kuikly.core.render.android.css.decoration

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.css.drawable.KRCSSBackgroundDrawable
import com.tencent.kuikly.core.render.android.css.ktx.isBeforeM
import com.tencent.kuikly.core.render.android.css.ktx.isBeforeOreoMr1
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import java.lang.ref.WeakReference
import kotlin.math.PI

class KRViewDecoration(targetView: View) : IKRViewDecoration {

    /**
     * 绘制目标View弱引用
     */
    private val targetViewWeakRef = WeakReference(targetView)

    private val kuiklyContext = targetView.context as? IKuiklyRenderContext

    private val path by lazy {
        Path()
    }
    private val rectF by lazy {
        RectF()
    }
    private val paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val layerDrawable by lazy {
        if (!isBeforeM) {
            LayerDrawable(arrayOfNulls(0))
        } else {
            LayerDrawable(arrayOf(
                KRCSSBackgroundDrawable(),
                KRCSSBackgroundDrawable()
            ))
        }
    }

    // 背景颜色相关
    var backgroundColor: Int = Color.TRANSPARENT
        set(value) {
            if (value == field) {
                return
            }
            field = value
            updateBgColorDrawable()
        }
    // 矩阵变换
    var matrix: Matrix? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            targetViewWeakRef.get()?.invalidate()
        }

    // 渐变相关
    var backgroundImage: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            field = value
            updateBgGradientDrawable()
        }

    // 圆角相关
    private var borderRadii: FloatArray? = null
    var borderRadiusF = BORDER_RADIUS_UNSET_VALUE
    var borderRadius: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            field = value
            parseBorderRadius(value)
            setOutlineViewProviderIfNeed()
            updateBgColorDrawable()
            updateBgGradientDrawable()
            updateFgBorderDrawable()
        }

    // 边框相关
    var borderStyle: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            field = value
            updateFgBorderDrawable()
        }

    // 阴影相关
    private var shadowOffsetX = 0f
    private var shadowOffsetY = 0f
    private var shadowColor = 0x0
    private var shadowRadius = 0f
    var boxShadow: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            field = value

            parseShadow(value)
            setOutlineViewProviderIfNeed()
        }

    /**
     * Android 6.0以下不支持foreground属性，这里新增这个属性来模拟foreground；
     * 自定义剪裁路径避免前景被剪裁，也使用这个属性来绘制前景
     */
    private var customForegroundDrawable: Drawable? = null

    private var isCustomClipPathMode: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            if (value && !isBeforeM) {
                targetViewWeakRef.get()?.also {
                    if (it.foreground != null) {
                        customForegroundDrawable = it.foreground
                        it.foreground = null
                    }
                }
            }
            field = value
        }

    /**
     * 使用OutlineViewProvider开关，为了向前兼容，默认开。设为false可以解决zIndex阴影问题
     */
    var useOutline: Boolean = true
        set(value) {
            field = value
            setOutlineViewProviderIfNeed()
        }

    var clipPathData: String = KRCssConst.EMPTY_STRING
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (value == KRCssConst.EMPTY_STRING) {
                path.reset()
            } else {
                parseClipPath(value)
                isCustomClipPathMode = true
            }
            setOutlineViewProviderIfNeed()
            updateBgColorDrawable()
            updateBgGradientDrawable()
            updateFgBorderDrawable()
        }

    override fun drawCommonDecoration(w: Int, h: Int, canvas: Canvas) {
        drawShadow(w, h, canvas) // 绘制阴影
        clipPath(w, h, canvas) // 裁剪圆角或路径
        applyMatrix(w, h, canvas) // 矩阵变换
    }

    override fun drawCommonForegroundDecoration(w: Int, h: Int, canvas: Canvas) {
        if (isBeforeM || isCustomClipPathMode) {
            customForegroundDrawable?.also {
                val bounds = it.bounds
                bounds.set(0, 0, w, h)
                it.bounds = bounds
                it.draw(canvas)
            }
        }
    }

    override fun hasCustomClipPath(): Boolean {
        return isCustomClipPathMode
    }

    private fun clipPath(w: Int, h: Int, canvas: Canvas) {
        val needClip = borderRadiusF != BORDER_RADIUS_UNSET_VALUE ||
                borderRadii?.any { it > 0f } == true ||
                clipPathData.isNotEmpty()
        if (!needClip) { // 没有设置圆角或路径的情况
            return
        }

        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        paint.color = Color.TRANSPARENT
        when {
            clipPathData.isNotEmpty() -> {
                // path 已经在 setClipPathData 里解析好了，这里直接用就行
            }
            borderRadiusF != BORDER_RADIUS_UNSET_VALUE -> { // 设置四个角都是圆角的情况
                path.reset()
                path.addRoundRect(rectF, borderRadiusF, borderRadiusF, Path.Direction.CW)
            }
            borderRadii != null -> { // 非四个角都是圆角的情况
                path.reset()
                path.addRoundRect(rectF, borderRadii!!, Path.Direction.CW)
            }
        }
        canvas.drawPath(path, paint)
        canvas.clipPath(path)
    }

    private fun applyMatrix(w: Int, h: Int, canvas: Canvas) {
        matrix?.also {
            canvas.concat(it)
        }
    }
    private fun drawShadow(w: Int, h: Int, canvas: Canvas) {
        if (shadowRadius == 0f) {
            return
        }

        rectF.set(shadowOffsetX, shadowOffsetY, w + shadowOffsetX, h + shadowOffsetY)
        paint.color = shadowColor
        paint.maskFilter = BlurMaskFilter(shadowRadius, BlurMaskFilter.Blur.NORMAL)
        when {
            clipPathData.isNotEmpty() -> {
                canvas.translate(shadowOffsetX, shadowOffsetY)
                // path 已经在 setClipPathData 里解析好了，这里直接用就行
                canvas.drawPath(path, paint)
                canvas.translate(-shadowOffsetX, -shadowOffsetY)
            }
            borderRadiusF != BORDER_RADIUS_UNSET_VALUE -> {
                canvas.drawRoundRect(rectF, borderRadiusF, borderRadiusF, paint) // 四个角都是圆角的阴影
            }
            borderRadii != null -> { // 非四个角都是圆角的阴影
                path.reset()
                path.addRoundRect(rectF, borderRadii!!, Path.Direction.CW)
                canvas.drawPath(path, paint)
            }
            else -> canvas.drawRect(rectF, paint)
        }
    }

    private fun parseShadow(shadowValue: String) {
        if (shadowValue == KRCssConst.EMPTY_STRING) {
            resetShadow()
        } else {
            BoxShadow(shadowValue, kuiklyContext).let {
                shadowOffsetX = it.shadowOffsetX
                shadowOffsetY = it.shadowOffsetY
                shadowRadius = it.shadowRadius
                shadowColor = it.shadowColor
            }
        }
    }

    private fun resetShadow() {
        shadowOffsetX = 0f
        shadowOffsetY = 0f
        shadowRadius = 0f
        shadowColor = Color.TRANSPARENT
    }

    private fun parseBorderRadius(borderRadius: String) {
        val borders = borderRadius.split(",")
        if (borders.size == KRCSSBackgroundDrawable.BORDER_ELEMENT_SIZE) {
            val tl = kuiklyContext.toPxF(borders[KRCSSBackgroundDrawable.BORDER_TOP_LEFT_INDEX].toFloat())
            val tr = kuiklyContext.toPxF(borders[KRCSSBackgroundDrawable.BORDER_TOP_RIGHT_INDEX].toFloat())
            val bl = kuiklyContext.toPxF(borders[KRCSSBackgroundDrawable.BORDER_BOTTOM_LEFT_INDEX].toFloat())
            val br = kuiklyContext.toPxF(borders[KRCSSBackgroundDrawable.BORDER_BOTTOM_RIGHT_INDEX].toFloat())

            val radii = floatArrayOf(
                tl, tl,
                tr, tr,
                br, br,
                bl, bl
            )
            if (KRCSSBackgroundDrawable.isAllBorderRadiusEqual(radii)) {
                borderRadiusF = tl
                borderRadii = null
            } else {
                borderRadiusF = BORDER_RADIUS_UNSET_VALUE
                borderRadii = radii
            }
        }
    }

    private fun updateBgGradientDrawable() {
        val gradientDrawable =
            layerDrawable.findDrawableByLayerId(LAYER_ID_GRADIENT_DRAWABLE)
                ?: KRCSSBackgroundDrawable().apply {
                    this.kuiklyContext = this@KRViewDecoration.kuiklyContext
                    if (isBeforeM) {
                        val index = if (layerDrawable.findDrawableByLayerId(LAYER_ID_COLOR_DRAWABLE) == null) {
                            0
                        } else {
                            1
                        }
                        layerDrawable.setId(index, LAYER_ID_GRADIENT_DRAWABLE)
                        layerDrawable.setDrawableByLayerId(LAYER_ID_GRADIENT_DRAWABLE, this)
                    } else {
                        layerDrawable.setId(layerDrawable.addLayer(this),
                            LAYER_ID_GRADIENT_DRAWABLE)
                    }
                }
        (gradientDrawable as KRCSSBackgroundDrawable).also {
            it.backgroundImage = backgroundImage
            it.borderRadius = borderRadius
            it.clipPath = if (clipPathData.isNotEmpty()) path else null
        }
        updateLayerDrawable()
    }

    private fun updateBgColorDrawable() {
        val colorDrawable = layerDrawable.findDrawableByLayerId(LAYER_ID_COLOR_DRAWABLE)
            ?: KRCSSBackgroundDrawable().apply {
                this.kuiklyContext = this@KRViewDecoration.kuiklyContext
                if (isBeforeM) {
                    val index = if (layerDrawable.findDrawableByLayerId(LAYER_ID_GRADIENT_DRAWABLE) == null) {
                        0
                    } else {
                        1
                    }
                    layerDrawable.setId(index, LAYER_ID_COLOR_DRAWABLE)
                    layerDrawable.setDrawableByLayerId(LAYER_ID_COLOR_DRAWABLE, this)
                } else {
                    layerDrawable.setId(layerDrawable.addLayer(this), LAYER_ID_COLOR_DRAWABLE)
                }
            }

        (colorDrawable as KRCSSBackgroundDrawable).also {
            it.setColor(backgroundColor)
            it.borderRadius = borderRadius
            it.clipPath = if (clipPathData.isNotEmpty()) path else null
        }
        updateLayerDrawable()
    }

    private fun updateLayerDrawable() {
        targetViewWeakRef.get()?.also {
            if (it.background == layerDrawable) {
                it.invalidate()
            } else {
                it.background = layerDrawable
            }
        }
    }

    private fun updateFgBorderDrawable() {
        targetViewWeakRef.get()?.also { view ->
            val borderDrawable = view.foregroundCompat ?: KRCSSBackgroundDrawable()
            (borderDrawable as KRCSSBackgroundDrawable).also { d ->
                d.kuiklyContext = kuiklyContext
                d.borderStyle = borderStyle
                d.borderRadius = borderRadius
                d.targetView = view
                d.isForeground = true
                d.clipPath = if (clipPathData.isNotEmpty()) path else null
            }
            if (view.foregroundCompat == null) {
                view.foregroundCompat = borderDrawable
            } else {
                view.invalidate()
            }
        }
    }

    private val outlineProvider by lazy(LazyThreadSafetyMode.NONE) {
        object : ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                if (view == null || outline == null) {
                    return
                }

                if (view.width <= 0 || view.height <= 0) {
                    return
                }
                when {
                    clipPathData.isNotEmpty() -> {
                        // since getOutline may be called before we remove outlineProvider,
                        // we need to check this case
                        return
                    }
                    borderRadiusF != BORDER_RADIUS_UNSET_VALUE -> { // 四个角都是圆角
                        if (isBeforeM) {
                            var borderWidth = 0
                            (targetViewWeakRef.get()?.foregroundCompat as? KRCSSBackgroundDrawable)
                                ?.also {
                                    borderWidth = it.borderWidth
                                }
                            // <= 6.0 的系统，前景 border 是我们绘制的，如果业务设置了圆角和 border，需要减去border 的宽度
                            // 不然border 会被限制在 clip 的区域内，只有setRoundRect这个 api 才会
                            outline.setRoundRect(-borderWidth, -borderWidth, view.width + borderWidth, view.height + borderWidth, borderRadiusF)
                        } else {
                            var compatBorderRadiusF = borderRadiusF
                            if (isBeforeOreoMr1 && compatBorderRadiusF < 0.5f) {
                                compatBorderRadiusF = 0.5f
                            }
                            outline.setRoundRect(0, 0, view.width, view.height, compatBorderRadiusF)
                        }
                    }
                    borderRadii != null -> { // 非四个角都是圆角
                        path.reset()
                        rectF.set(0f, 0f, view.width.toFloat(), view.height.toFloat())
                        path.addRoundRect(rectF, borderRadii!!, Path.Direction.CW)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            outline.setPath(path)
                        } else {
                            outline.setConvexPath(path)
                        }
                    }
                }
            }
        }
    }

    private fun setOutlineViewProviderIfNeed() {
        targetViewWeakRef.get()?.also {
            val hasBorderRadius = borderRadiusF != BORDER_RADIUS_UNSET_VALUE ||
                    borderRadii?.any { it > 0f } == true
            val needOutline = useOutline &&
                    // 如果有设置阴影的话，使用clipPath裁剪圆角，不然的话，阴影会无效
                    shadowRadius != 0f &&
                    // 自定义Path可能不是凸的，无法使用outline裁剪，也使用clipPath裁剪
                    clipPathData.isEmpty() &&
                    hasBorderRadius

            if (needOutline) {
                setOutlineViewProvider(it, outlineProvider)
            } else {
                setOutlineViewProvider(it, null)
            }
        }
    }

    private fun setOutlineViewProvider(view: View, outlineProvider: ViewOutlineProvider?) {
        view.outlineProvider = outlineProvider
        view.clipToOutline = outlineProvider != null
        view.invalidate()
    }

    var View.foregroundCompat: Drawable?
        get() {
            return if (isBeforeM || isCustomClipPathMode) {
                customForegroundDrawable
            } else {
                foreground
            }
        }
        set(value) {
            if (isBeforeM || isCustomClipPathMode) {
                customForegroundDrawable = value
            } else {
                foreground = value
            }
        }

    private fun parseClipPath(pathData: String) {
        val values = pathData.split(KRCssConst.BLANK_SEPARATOR)
        var index = 0
        path.rewind()
        try {
            while (index < values.size) {
                val command = values[index]
                when (command) {
                    "M" -> {
                        val x = kuiklyContext.toPxF(values[index + 1].toFloat())
                        val y = kuiklyContext.toPxF(values[index + 2].toFloat())
                        path.moveTo(x, y)
                        index += 3
                    }
                    "L" -> {
                        val x = kuiklyContext.toPxF(values[index + 1].toFloat())
                        val y = kuiklyContext.toPxF(values[index + 2].toFloat())
                        path.lineTo(x, y)
                        index += 3
                    }
                    "R" -> {
                        val cx = kuiklyContext.toPxF(values[index + 1].toFloat())
                        val cy = kuiklyContext.toPxF(values[index + 2].toFloat())
                        val radius = kuiklyContext.toPxF(values[index + 3].toFloat())
                        val startAngle = values[index + 4].toFloat() * KRViewConst.PI_AS_ANGLE / PI
                        val endAngle = values[index + 5].toFloat() * KRViewConst.PI_AS_ANGLE / PI
                        val counterclockwise = values[index + 6] == "1"
                        var sweepAngle = endAngle - startAngle
                        if (counterclockwise) {
                            // Preprocessing for counter-clockwise drawing:
                            // 0. Angles in (-720, 0] require no processing
                            // 1. sweepAngle > 0, startAngle and endAngle represent absolute angles, convert to [-360, 0)
                            // 2. sweepAngle <= -720, drawing exceeds 2 turns, convert to (-720, -360]
                            // Rules 2 and 3 share the same formula; In summary, final sweepAngle is in (-720, 0]
                            if (sweepAngle > 0 || sweepAngle <= -2 * KRViewConst.ROUND_ANGLE) {
                                sweepAngle = sweepAngle % KRViewConst.ROUND_ANGLE - KRViewConst.ROUND_ANGLE
                            }
                        } else {
                            // Preprocessing for clockwise drawing:
                            // 0. Angles in [0, 720) require no processing
                            // 1. sweepAngle < 0, startAngle and endAngle represent absolute angles, convert to (0, 360]
                            // 2. sweepAngle >= 720, drawing exceeds 2 turns, convert to [360, 720)
                            // Rules 2 and 3 share the same formula; In summary, final sweepAngle is in [0, 720)
                            if (sweepAngle < 0 || sweepAngle >= 2 * KRViewConst.ROUND_ANGLE) {
                                sweepAngle = sweepAngle % KRViewConst.ROUND_ANGLE + KRViewConst.ROUND_ANGLE
                            }
                        }

                        if (-KRViewConst.ROUND_ANGLE < sweepAngle && sweepAngle < KRViewConst.ROUND_ANGLE) {
                            // deal with arc less than 2π
                            path.arcTo(
                                cx - radius, cy - radius, cx + radius, cy + radius,
                                startAngle.toFloat(),
                                sweepAngle.toFloat(),
                                false
                            )
                        } else {
                            // deal with arc greater than or equal to 2π
                            val halfSweepAngle = sweepAngle * 0.5f
                            path.arcTo(
                                cx - radius, cy - radius, cx + radius, cy + radius,
                                startAngle.toFloat(),
                                halfSweepAngle.toFloat(),
                                false
                            )
                            path.arcTo(
                                cx - radius, cy - radius, cx + radius, cy + radius,
                                (startAngle + halfSweepAngle).toFloat(),
                                halfSweepAngle.toFloat(),
                                false
                            )
                        }
                        index += 7
                    }
                    "Z" -> {
                        path.close()
                        index += 1
                    }
                    "Q" -> {
                        val cx = kuiklyContext.toPxF(values[index + 1].toFloat())
                        val cy = kuiklyContext.toPxF(values[index + 2].toFloat())
                        val x = kuiklyContext.toPxF(values[index + 3].toFloat())
                        val y = kuiklyContext.toPxF(values[index + 4].toFloat())
                        path.quadTo(cx, cy, x, y)
                        index += 5
                    }
                    "C" -> {
                        val cx1 = kuiklyContext.toPxF(values[index + 1].toFloat())
                        val cy1 = kuiklyContext.toPxF(values[index + 2].toFloat())
                        val cx2 = kuiklyContext.toPxF(values[index + 3].toFloat())
                        val cy2 = kuiklyContext.toPxF(values[index + 4].toFloat())
                        val x = kuiklyContext.toPxF(values[index + 5].toFloat())
                        val y = kuiklyContext.toPxF(values[index + 6].toFloat())
                        path.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        index += 7
                    }
                    else -> {
                        KuiklyRenderLog.e("ClipPath", "Unknown path command: $command")
                        path.rewind()
                        return
                    }
                }
            }
        } catch (e: NumberFormatException) {
            KuiklyRenderLog.e("ClipPath", "Invalid param type: ${e.message}")
            path.rewind()
        } catch (e: IndexOutOfBoundsException) {
            KuiklyRenderLog.e("ClipPath", "Invalid param length: ${e.message}")
            path.rewind()
        }
    }

    companion object {
        private const val LAYER_ID_COLOR_DRAWABLE = 1
        private const val LAYER_ID_GRADIENT_DRAWABLE = 2

        const val BORDER_RADIUS_UNSET_VALUE = -1.0f
    }

}

class BoxShadow(shadowValue: String, private val context: IKuiklyRenderContext?) {

    companion object {
        private const val SHADOW_ELEMENT_SIZE = 4
        private const val SHADOW_OFFSET_X = 0
        private const val SHADOW_OFFSET_Y = 1
        private const val SHADOW_RADIUS = 2
        private const val SHADOW_COLOR = 3
    }

    var shadowOffsetX = 0.0f
    var shadowOffsetY = 0.0f
    var shadowRadius = 0.0f
    var shadowColor = Color.TRANSPARENT

    init {
        val boxShadows = shadowValue.split(KRCssConst.BLANK_SEPARATOR)
        if (boxShadows.size == SHADOW_ELEMENT_SIZE) {
            shadowOffsetX = context.toPxF(boxShadows[SHADOW_OFFSET_X].toFloat())
            shadowOffsetY = context.toPxF(boxShadows[SHADOW_OFFSET_Y].toFloat())
            shadowRadius = context.toPxF(boxShadows[SHADOW_RADIUS].toFloat())
            shadowColor = boxShadows[SHADOW_COLOR].toColor()
        }
    }

    fun isEmpty(): Boolean {
        return shadowOffsetY == 0.0f && shadowOffsetX == 0.0f
    }

}