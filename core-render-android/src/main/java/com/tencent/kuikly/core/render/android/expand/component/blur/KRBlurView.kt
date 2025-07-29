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

package com.tencent.kuikly.core.render.android.expand.component.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat
import com.tencent.kuikly.core.render.android.expand.component.blur.SizeScaler.Companion.ROUNDING_VALUE
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport

/**
 * Created by kam on 2023/3/23.
 */
class KRBlurView(context: Context) : FrameLayout(context), IKuiklyRenderViewExport {

    private var internalBitmap: Bitmap? = null
    private var internalCanvas: BlurViewCanvas? = null
    private var initialized = false
    private val rootLocation = IntArray(2)
    private val blurViewLocation = IntArray(2)
    private var blurRadius = 5f
    private val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        RenderEffectBlur(context)
    } else {
        RenderScriptBlur(context)
    }
    private var filterInvalidDraw = false

    private var blurRootViewList = mutableListOf<View>()

    private var targetBlurViewTags = mutableListOf<Int>()

    private var blurOtherLayer = false
    private val otherLayerPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
        }
    }

    private val preDrawListener = OnPreDrawListener {
        if (!filterInvalidDraw) {
            updateBlurBitmap()
        } else {
            filterInvalidDraw = false
        }
        true
    }

    init {
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        krRootView()?.viewTreeObserver?.also {
            it.removeOnPreDrawListener(preDrawListener)
            it.addOnPreDrawListener(preDrawListener)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        krRootView()?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_BLUR_RADIUS -> blurRadius(propValue)
            PROP_TARGET_BLUR_VIEW_NATIVE_REFS -> blurViewTag(propValue)
            PROP_BLUR_OTHER_LAYER -> blurOtherLayer(propValue)
            else ->super.setProp(propKey, propValue)
        }
    }

    override fun draw(canvas: Canvas) {
        if (!initialized) {
            super.draw(canvas)
        } else
            if (canvas !is BlurViewCanvas) {
            internalBitmap?.also {
                val scaleFactorW = width.toFloat() / it.width
                val scaleFactorH = height.toFloat() / it.height
                canvas.save()
                canvas.scale(scaleFactorW, scaleFactorH)
                blur.draw(canvas, it)
                canvas.restore()
            }
            super.draw(canvas)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBlurViewSize(w, h)
    }

    override fun onDestroy() {
        super.onDestroy()
        blur.destroy()
    }

    private fun blurRadius(propValue: Any): Boolean {
        blurRadius = propValue.toNumberFloat() * 2f
        return true
    }

    private fun blurViewTag(propValue: Any): Boolean {
        val tags = propValue as String
        if (tags.isEmpty()) {
            return true
        }

        targetBlurViewTags.clear()
        tags.split("|").forEach {
            val tag = it.toIntOrNull() ?: -1
            if (tag != -1) {
                targetBlurViewTags.add(tag)
            }
        }
        return true
    }

    private fun blurOtherLayer(propValue: Any): Boolean {
        blurOtherLayer = (propValue as Int) == 1
        return true
    }

    private fun updateBlurViewSize(width: Int, height: Int) {
        val sizeScaler = SizeScaler(20f)
        if (sizeScaler.isZeroSized(width, height)) {
            return
        }

        val bitmapSize = sizeScaler.scale(width, height)
        val bitmap = Bitmap.createBitmap(bitmapSize.first, bitmapSize.second, Bitmap.Config.ARGB_8888)
        internalCanvas = BlurViewCanvas(bitmap)
        internalBitmap = bitmap
        initialized = true
    }

    private fun updateBlurBitmap() {
        if (!initialized) {
            return
        }

        val bitmap = internalBitmap ?: return
        val canvas = internalCanvas ?: return
        bitmap.eraseColor(Color.TRANSPARENT)
        getBlurRootViewList().forEach { krRootView ->
            drawBlurContent(canvas, bitmap, krRootView)
        }
        internalBitmap = blur.blur(bitmap, blurRadius)
    }

    private fun drawBlurContent(canvas: Canvas, bitmap: Bitmap, rootView: View) {
        if (targetBlurViewTags.isNotEmpty()) {
            drawBlurContentWithTags(targetBlurViewTags, canvas, bitmap, rootView)
        } else {
            drawBlurContentWithRootView(canvas, bitmap, rootView)
        }
    }

    private fun drawBlurContentWithRootView(canvas: Canvas, bitmap: Bitmap, rootView: View) {
        canvas.save()
        setupInternalCanvasMatrix(rootView, bitmap, canvas)
        val setGoneViewList = setUpperViewVisibleGone() // 不截取BlurView上层的视图, 暂时把位置BlurView上层的View设为不可见
        rootView.draw(canvas)
        tryDrawTextureView(rootView, canvas)
        restoreSetGoneViews(setGoneViewList) // 恢复上层View的可见性
        canvas.restore()
    }

    private fun tryDrawTextureView(rootView: View, canvas: Canvas) {
        if (!blurOtherLayer) {
            return
        }
        findTextureView(rootView)?.bitmap?.also {
            canvas.drawBitmap(it, 0f, 0f, otherLayerPaint)
        }
    }

    private fun findTextureView(view: View?): TextureView? {
        if (view is TextureView) {
            return view
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val tp = findTextureView(view.getChildAt(i))
                if (tp != null) {
                    return tp
                }
            }
        }
        return null
    }

    private fun drawBlurContentWithTags(
        tags: List<Int>,
        canvas: Canvas,
        bitmap: Bitmap,
        rootView: View
    ) {
        tags.forEach {
            findViewWithTag(it, rootView)?.also { blurView ->
                canvas.save()
                setupInternalCanvasMatrix(blurView, bitmap, canvas)
                blurView.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun findViewWithTag(tag: Int, rootView: View): View? {
        var view = kuiklyRenderContext?.getView(tag)
        if (view == null) {
            view = rootView.findViewWithTag<View>(tag)
        }
        return view
    }

    private fun setUpperViewVisibleGone(): Set<View>? {
        val parent = parent as? ViewGroup ?: return null
        val index = parent.indexOfChild(this)
        if (index == -1) {
            return null
        }

        val setGoneViewList = mutableSetOf<View>()
        val size = parent.childCount
        for (i in index + 1 until size) {
            val child = parent.getChildAt(i)
            if (child !is KRBlurView && child.visibility == VISIBLE) {
                child.visibility = GONE
                setGoneViewList.add(child)
                filterInvalidDraw = true
            }
        }
        for (i in 0 until index) {
            parent.getChildAt(i)?.also {
                if (it.z > z && it !is KRBlurView && it.visibility == VISIBLE) {
                    it.visibility = GONE
                    setGoneViewList.add(it)
                    filterInvalidDraw = true
                }
            }
        }
        return setGoneViewList
    }

    private fun restoreSetGoneViews(setGoneViews: Set<View>?) {
        setGoneViews?.forEach {
            it.visibility = View.VISIBLE
        }
    }

    /**
     * Set up matrix to draw starting from blurView's position
     */
    private fun setupInternalCanvasMatrix(rootView: View, bitmap: Bitmap, canvas: Canvas) {
        rootView.getLocationOnScreen(rootLocation)
        getLocationOnScreen(blurViewLocation)
        val left = blurViewLocation[0] - rootLocation[0]
        val top = blurViewLocation[1] - rootLocation[1]

        val scaleFactorW = width.toFloat() / bitmap.width
        val scaleFactorH = height.toFloat() / bitmap.height
        val scaledLeftPosition = -left / scaleFactorW
        val scaledTopPosition = -top / scaleFactorH

        canvas.translate(scaledLeftPosition, scaledTopPosition)
        canvas.scale(1 / scaleFactorW, 1 / scaleFactorH)
    }

    private fun getBlurRootViewList(): List<View> {
        blurRootViewList.clear()
        val activityDecorView = getActivityDecorView()
        activityDecorView?.also {
            blurRootViewList.add(it)
        }
        // 获取Blur所处顶层View，一般情况是与Activity的DecorView是同一个
        // 但是如果在Dialog场景下, Dialog会有独立的DecorView, 此时两者不等
        val blurViewRootView = getBlurViewDecorView()
        if (blurViewRootView != null && blurViewRootView != activityDecorView) {
            blurRootViewList.add(blurViewRootView)
        }
        return blurRootViewList
    }

    private fun getActivityDecorView(): View? {
        return activity?.window?.decorView
    }

    private fun getBlurViewDecorView(): View? {
        return rootView
    }

    companion object {
        const val VIEW_NAME = "KRBlurView"
        private const val PROP_BLUR_RADIUS = "blurRadius"
        private const val PROP_TARGET_BLUR_VIEW_NATIVE_REFS = "targetBlurViewNativeRefs"
        private const val PROP_BLUR_OTHER_LAYER = "blurOtherLayer"
    }
}

/**
 * Scales width and height by [scaleFactor],
 * and then rounds the size proportionally so the width is divisible by [ROUNDING_VALUE]
 */
private class SizeScaler(private val scaleFactor: Float) {

    fun scale(width: Int, height: Int): Pair<Int, Int> {
        val nonRoundedScaledWidth = downscaleSize(width.toFloat())
        val scaledWidth = roundSize(nonRoundedScaledWidth)
        //Only width has to be aligned to ROUNDING_VALUE
        val roundingScaleFactor = width.toFloat() / scaledWidth
        //Ceiling because rounding or flooring might leave empty space on the View's bottom
        val scaledHeight = Math.ceil((height / roundingScaleFactor).toDouble()).toInt()
        return Pair(scaledWidth, scaledHeight)
    }

    fun isZeroSized(measuredWidth: Int, measuredHeight: Int): Boolean {
        return downscaleSize(measuredHeight.toFloat()) == 0 || downscaleSize(measuredWidth.toFloat()) == 0
    }

    /**
     * Rounds a value to the nearest divisible by [.ROUNDING_VALUE] to meet stride requirement
     */
    private fun roundSize(value: Int): Int {
        return if (value % ROUNDING_VALUE == 0) {
            value
        } else {
            value - value % ROUNDING_VALUE + ROUNDING_VALUE
        }
    }

    private fun downscaleSize(value: Float): Int {
        return Math.ceil((value / scaleFactor).toDouble()).toInt()
    }

    companion object {
        // Bitmap size should be divisible by ROUNDING_VALUE to meet stride requirement.
        // This will help avoiding an extra bitmap allocation when passing the bitmap to RenderScript for blur.
        // Usually it's 16, but on Samsung devices it's 64 for some reason.
        private const val ROUNDING_VALUE = 64
    }
}

private class BlurViewCanvas(bitmap: Bitmap) : Canvas(bitmap)