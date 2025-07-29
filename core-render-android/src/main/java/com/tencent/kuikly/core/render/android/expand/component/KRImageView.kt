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

package com.tencent.kuikly.core.render.android.expand.component

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.NinePatch
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import android.os.Looper
import android.util.SizeF
import android.view.ViewGroup
import android.widget.ImageView
import com.tencent.kuikly.core.render.android.adapter.HRImageLoadOption
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.drawable.KRCSSBackgroundDrawable
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.css.ktx.removeFromParent
import com.tencent.kuikly.core.render.android.css.ktx.toColor
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.expand.component.blur.RenderScriptBlur
import com.tencent.kuikly.core.render.android.expand.component.image.Insets
import com.tencent.kuikly.core.render.android.expand.component.image.NinePatchHelper
import com.tencent.kuikly.core.render.android.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.scheduler.KRSubThreadScheduler
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Kuikly图片组件
 */
open class KRImageView(context: Context) : ImageView(context), IKuiklyRenderViewExport {

    /**
     * 是否首屏完成后再加载
     */
    private var shouldWaitViewDidLoad: Boolean = true

    /**
     * 当View的宽高还是测量出来时, setSrc请求会被缓存到这个变量
     */
    private var setSrcLazyTask: (() -> Unit)? = null

    /**
     * 图片src
     */
    private var src = KRCssConst.EMPTY_STRING

    /**
     * 是否加载为.9图
     */
    private var isNinePatchDrawable = false
    /**
     * 高斯模糊半径
     */
    private var blurRadius = 0f
    /**
     * 图片组件的渐变遮罩
     */
    private var maskLinearGradient: String? = null

    /**
     * image 染色
     */
    private var tintColor: Int? = null

    /**
     * 图片加载成功事件回调
     */
    private var loadSuccessCallback: KuiklyRenderCallback? = null
    /**
     * 图片分辨率成功事件回调
     */
    private var loadResolutionCallback: KuiklyRenderCallback? = null
    /**
     * 图片加载失败事件回调
     */
    private var loadFailureCallback: KuiklyRenderCallback? = null

    /*
     * 源图片
     */
    private var originDrawable: Drawable? = null

    private var paintMaskGradient: Paint? = null
    private var maskLinearGradientSize: SizeF = SizeF(0f, 0f)
    private var needReCreatePaintMaskGradient: Boolean = false
    private var capInsets: Insets? = null

    init {
        shouldWaitViewDidLoad = KuiklyRenderAdapterManager.krImageAdapter?.shouldWaitViewDidLoad ?: true
        scaleType = ScaleType.CENTER_CROP
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_SRC -> setSrc(propValue as String)
            PROP_RESIZE -> setResize(propValue as String)
            PROP_BLUR_RADIUS -> setBlurRadius(propValue as Float)
            PROP_TINT_COLOR -> setTintColor(propValue)
            PROP_MASK_LINEAR_GRADIENT -> setMaskLinearGradient(propValue as String)
            PROP_DOT_NINE_IMAGE -> setIsNineDotImage(propValue)
            PROP_EVENT_LOAD_SUCCESS -> setLoadSuccessCallback(propValue)
            PROP_EVENT_LOAD_RESOLUTION -> setLoadResolutionCallback(propValue)
            PROP_EVENT_LOAD_FAILURE->setLoadFailureCallback(propValue)
            PROP_CAP_INSETS -> setCapInsets(propValue as String)
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun setTintColor(propValue: Any): Boolean {
        if ((propValue as String).isNotEmpty()) {
            this.tintColor = propValue.toColor()
        } else {
            this.tintColor = null
        }
        updateDrawableImage(originDrawable)
        return true
    }

    override val reusable: Boolean
        get() = true

    override fun resetProp(propKey: String): Boolean {
        return when (propKey) {
            PROP_SRC -> resetSrc()
            PROP_RESIZE -> resetResize()
            PROP_BLUR_RADIUS -> resetBlurRadius()
            PROP_TINT_COLOR -> resetTintColor()
            PROP_MASK_LINEAR_GRADIENT -> resetMaskLinearGradient()
            PROP_DOT_NINE_IMAGE -> resetIsNineDotImage()
            PROP_EVENT_LOAD_SUCCESS -> resetLoadSuccessCallback()
            PROP_EVENT_LOAD_RESOLUTION -> resetLoadResolutionCallback()
            PROP_EVENT_LOAD_FAILURE -> resetLoadFailureCallback()
            PROP_CAP_INSETS -> resetCapInsets()
            else -> super.resetProp(propKey)
        }
    }

    private fun resetTintColor(): Boolean {
        tintColor = null
        return true
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        super.setLayoutParams(params)
        performSetSrcLazyTaskOnce()
        setClipBound()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        updatePaintMaskGradientIfNeedOnDraw()
        var saveCount = 0
        paintMaskGradient?.also {
            saveCount = canvas.saveLayer(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), null)
        }
        drawCommonDecoration(frameWidth, frameHeight, canvas)
        if (capInsetsValid()) {
            drawable?.also {
                val density = resources.displayMetrics.density
                val drawableWidth = if (it.intrinsicWidth > 0) (it.intrinsicWidth * density).roundToInt() else frameWidth
                val drawableHeight = if (it.intrinsicHeight > 0) (it.intrinsicHeight * density).roundToInt() else frameHeight
                it.setBounds(0, 0, drawableWidth, drawableHeight)
                NinePatchHelper.draw(canvas, { c, dr -> dr.draw(c) }, it, drawableWidth, drawableHeight, density,
                    frameWidth, frameHeight, capInsets!!)
            }
        } else {
            super.onDraw(canvas)
        }
        drawCommonForegroundDecoration(frameWidth, frameHeight, canvas)
        paintMaskGradient?.also {// 绘制渐变遮罩
            canvas.drawRect(0f, 0f, frameWidth.toFloat(), frameHeight.toFloat(), it)
            canvas.restoreToCount(saveCount)
        }
    }

    override fun setImageDrawable(drawable: Drawable?) {
        originDrawable = drawable
        updateDrawableImage(drawable)
    }
    override fun onDestroy() {
        super.onDestroy()
        stopAnimatable()
    }

    private fun updateDrawableImage(drawable: Drawable?) {
        if (drawable != null && tintColor != null) {
            val tintDrawable = drawable.mutate()
            tintDrawable.setTint(tintColor as Int)
            superSetImage(tintDrawable)
        } else if (drawable != null && blurRadius > 0f) {
            val tBlurRadius = this.blurRadius
            val tScr = this.src
            // drawable
            KRSubThreadScheduler.scheduleTask(0) {
                // 高斯模糊
                val blurDrawable = RenderScriptBlur.blurImage(drawable, context, tBlurRadius)
                runOnUiThread {
                    drawable.setTintList(null)
                    // 记录结束时间并计算耗时
                    if (this.src == tScr && tBlurRadius == this.blurRadius) {
                        superSetImage(blurDrawable)
                    }
                }
            }
        } else {
            drawable?.setTintList(null)
            superSetImage(drawable)
        }
    }

    private fun superSetImage(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (drawable is Animatable) {
            drawable.start()
        }
        fireLoadSuccessCallback(drawable)
        fireLoadResolutionCallback(drawable)
    }

    private fun resetSrc(): Boolean {
        src = KRCssConst.EMPTY_STRING
        stopAnimatable()
        setImageDrawable(null)
        clipBounds = null
        return true
    }

    private fun resetResize(): Boolean {
        scaleType = ScaleType.CENTER_CROP
        return true
    }

    private fun resetBlurRadius(): Boolean {
        blurRadius = 0f
        return true
    }

    private fun resetMaskLinearGradient(): Boolean {
        maskLinearGradient = null
        paintMaskGradient = null
        return true
    }

    private fun resetIsNineDotImage(): Boolean {
        isNinePatchDrawable = false
        return true
    }

    private fun resetLoadSuccessCallback(): Boolean {
        loadSuccessCallback = null
        return true
    }

    private fun resetLoadResolutionCallback(): Boolean {
        loadResolutionCallback = null
        return true
    }

    private fun resetLoadFailureCallback(): Boolean {
        loadFailureCallback = null
        return true
    }

    private fun setResize(resize: String): Boolean {
        scaleType = when (resize) {
            RESIZE_MODE_COVER -> ScaleType.CENTER_CROP
            RESIZE_MODE_CONTAIN -> ScaleType.FIT_CENTER
            RESIZE_MODE_STRETCH -> ScaleType.FIT_XY
            else -> ScaleType.CENTER_CROP
        }
        return true
    }

    private fun setBlurRadius(blurRadius: Float): Boolean {
        // image
        this.blurRadius = blurRadius
        updateDrawableImage(originDrawable)
        return true
    }

    private fun setMaskLinearGradient(maskGradient: String): Boolean {
        this.maskLinearGradient = maskGradient
        needReCreatePaintMaskGradient = true
        invalidate()
        return true
    }

    private fun updatePaintMaskGradientIfNeedOnDraw() {
        if (maskLinearGradient?.isNotEmpty() == true && originDrawable != null) {
            val size = SizeF(frameWidth.toFloat(), frameHeight.toFloat())
            if (needReCreatePaintMaskGradient || size != maskLinearGradientSize || paintMaskGradient == null) {
                maskLinearGradientSize = size
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                val gradient = KRCSSBackgroundDrawable.parseLinearGradient(maskLinearGradient ?: "", maskLinearGradientSize, Shader.TileMode.CLAMP)
                paint.shader = gradient
                paintMaskGradient = paint
            }
            needReCreatePaintMaskGradient = false
        } else {
            paintMaskGradient = null
        }
    }

    private fun setLoadSuccessCallback(propValue: Any): Boolean {
        this.loadSuccessCallback = propValue as KuiklyRenderCallback
        fireLoadSuccessCallback(drawable)
        return true
    }

    private fun setLoadResolutionCallback(propValue: Any): Boolean {
        this.loadResolutionCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun setLoadFailureCallback(propValue: Any): Boolean {
        this.loadFailureCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun setIsNineDotImage(propValue: Any): Boolean {
        isNinePatchDrawable = propValue as Int == TYPE_NINE_DOT_DRAWABLE
        return true
    }

    private fun setSrc(url: String): Boolean {
        if (src == url) {
            return true
        }
        src = url
        setImageDrawable(null) // 重置drawable，防止动态更新src时, Drawable错乱

        val setSrcTask = {
            startLoadImage()
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            setSrcLazyTask = setSrcTask // 缓存住，等设置View的宽高后才调用
        } else {
            setSrcTask.invoke()
        }
        return true
    }

    private fun startLoadImage() {
        val tempSrc = src
        if (isBase64Src()) {
            loadBase64Image(tempSrc)
        } else if (tempSrc.isNotEmpty()) {
            fetchDrawable(createImageLoadOption(tempSrc)) { drawable ->
                runOnUiThread {
                    setResultImageDrawable(tempSrc, drawable)
                }
            }
        }
    }

    private fun isBase64Src(): Boolean = src.startsWith(BASE64_IMAGE_PREFIX)

    private fun loadBase64Image(tempSrc: String) {
        val base64StrKey = src
        val resultBitmapOrBase64Str = getBase64Image(base64StrKey)
        if (resultBitmapOrBase64Str is Drawable) {
            setImageDrawable(resultBitmapOrBase64Str)
            return
        }
        if (resultBitmapOrBase64Str == null) {
            return
        }
        assert(resultBitmapOrBase64Str is String)
        fetchDrawable(createImageLoadOption(resultBitmapOrBase64Str as String)) { drawable ->
            runOnUiThread {
                setResultImageDrawable(tempSrc, drawable)
                setBase64Image(drawable)
            }
        }
    }

    private fun createImageLoadOption(src: String): HRImageLoadOption {
        return HRImageLoadOption(
            src,
            frameWidth,
            frameHeight,
            !(isNinePatchDrawable || loadResolutionCallback != null || capInsetsValid()), // .9图不需要resize
            scaleType
        )
    }

    private fun setResultImageDrawable(requestSrc: String, drawable: Drawable?) {
        if (requestSrc != src) {
            return
        }
        if (drawable == null) {
            fireLoadFailureCallback()
            return
        }
        var resultDrawable = drawable
        if (isNinePatchDrawable && drawable is BitmapDrawable) {
            val scaleCenterOffset = 1
            resultDrawable = NinePatchBuilder(resources, drawable.bitmap)
                .addXCenteredRegion(scaleCenterOffset)
                .addYCenteredRegion(scaleCenterOffset)
                .build()
        }
        setImageDrawable(resultDrawable)
    }

    private fun getBase64Image(key: String): Any? {
        return kuiklyRenderContext?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
            ?.get(key)
    }

    private fun setBase64Image(drawable: Drawable?) {
        drawable?.also {
            kuiklyRenderContext?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
                ?.set(src, it)
        }
    }

    private fun performSetSrcLazyTaskOnce() {
        if (setSrcLazyTask == null) {
            return
        }
        if (frameWidth <= 0 || frameHeight <= 0) {
            return
        }

        setSrcLazyTask?.invoke()
        setSrcLazyTask = null
    }

    /**
     * 限制drawable的绘制区域。
     * 兼容 URLDrawable 不支持指定裁剪模式
     */
    private fun setClipBound() {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return
        }
        clipBounds = Rect(0, 0, frameWidth, frameHeight)
    }

    private fun runOnUiThread(task: () -> Unit) {
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            task()
        } else {
            post(task)
        }
    }

    private fun fireLoadSuccessCallback(drawable: Drawable?) {
        if (drawable == null) {
            return
        }
        val cb = loadSuccessCallback ?: return
        cb.invoke(mapOf(
            PROP_SRC to src
        ))
    }

    private fun fireLoadResolutionCallback(drawable: Drawable?) {
        if (drawable == null) {
            return
        }
        val cb = loadResolutionCallback ?: return
        val density = resources.displayMetrics.density
        cb.invoke(mapOf(
            IMAGE_WIDTH to drawable.intrinsicWidth * density,
            IMAGE_HEIGHT to drawable.intrinsicHeight * density
        ))
    }

    private fun fireLoadFailureCallback() {
        val cb = loadFailureCallback ?: return
        cb.invoke(mapOf(
            PROP_SRC to src
        ))
    }

    private fun stopAnimatable() {
        val d = drawable
        if (d is Animatable && d.isRunning) {
            d.stop()
        }
    }

    private fun fetchDrawable(imageLoadOption: HRImageLoadOption, callback: (drawable: Drawable?) -> Unit) {
        if (kuiklyRenderContext?.kuiklyRenderRootView != null && shouldWaitViewDidLoad) {
            kuiklyRenderContext?.kuiklyRenderRootView?.performWhenViewDidLoad {
                kuiklyRenderContext?.getImageLoader()?.fetchImageAsync(imageLoadOption, callback)
            }
        } else {
            kuiklyRenderContext?.getImageLoader()?.fetchImageAsync(imageLoadOption, callback)
        }
    }

    private fun setCapInsets(param: String): Boolean {
        capInsets = param.split(" ").takeIf { it.size >= 4 }?.let {
            try {
                // "$top $left $bottom $right"
                val top = it[0].toFloat().toPxI()
                val left = it[1].toFloat().toPxI()
                val bottom = it[2].toFloat().toPxI()
                val right = it[3].toFloat().toPxI()
                Insets(left, top, right, bottom)
            } catch (e: NumberFormatException) {
                null
            }
        }
        invalidate()
        return true
    }

    private fun resetCapInsets(): Boolean {
        capInsets = null
        return true
    }

    private fun capInsetsValid(): Boolean {
        return capInsets?.isZero() == false && scaleType == ScaleType.FIT_XY
    }

    companion object {
        const val VIEW_NAME = "KRImageView"
        const val PROP_SRC = "src" // 外部用引用到，因此不是private
        const val PROP_RESIZE = "resize"
        private const val PROP_DOT_NINE_IMAGE = "dotNineImage"
        private const val PROP_BLUR_RADIUS = "blurRadius"
        private const val PROP_TINT_COLOR = "tintColor"
        private const val PROP_MASK_LINEAR_GRADIENT = "maskLinearGradient"
        private const val PROP_EVENT_LOAD_SUCCESS = "loadSuccess"
        private const val PROP_EVENT_LOAD_RESOLUTION = "loadResolution"
        private const val PROP_EVENT_LOAD_FAILURE = "loadFailure"
        private const val PROP_CAP_INSETS = "capInsets"
        private const val RESIZE_MODE_COVER = "cover"
        private const val RESIZE_MODE_CONTAIN = "contain"
        private const val RESIZE_MODE_STRETCH = "stretch"

        private const val TYPE_NINE_DOT_DRAWABLE = 1

        private const val BASE64_IMAGE_PREFIX = "data:image"
        private const val IMAGE_WIDTH = "imageWidth"
        private const val IMAGE_HEIGHT = "imageHeight"
    }
}

class KRWrapperImageView(context: Context) : KRView(context) {

    private var placeholderView: KRImageView? = null
    private var placeholder = ""

    private var imageView = KRImageView(context).apply {
        this@KRWrapperImageView.addView(this)
    }

    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        var result = super.setProp(propKey, propValue)
        if (!result) {
            result = imageView.setProp(propKey, propValue)
        }
        if (propKey == KRImageView.PROP_RESIZE) {
            placeholderView?.setProp(propKey, propValue)
        } else if (propKey == PROP_PLACEHOLDER) {
            setPlaceholder(propValue as String)
            result = true
        }
        return result
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        params?.also {
            imageView.layoutParams = LayoutParams(it.width, it.height)
            placeholderView?.layoutParams = LayoutParams(it.width, it.height)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return imageView.call(method, params, callback)
    }

    override fun resetProp(propKey: String): Boolean {
        var result = super.resetProp(propKey)
        result = imageView.resetProp(propKey)
        if (propKey == PROP_PLACEHOLDER) {
            removePlaceholder()
            result = true
        }
        return result
    }

    private fun setPlaceholder(src: String) {
        if (placeholder != src) {
            placeholder = src
            placeholderView?.removeFromParent()
            if (placeholder.isNotEmpty()) {
                placeholderView = KRImageView(context).apply {
                    val lp = LayoutParams(this@KRWrapperImageView.frameWidth,
                        this@KRWrapperImageView.frameHeight)
                    this.layoutParams = lp
                    scaleType = imageView.scaleType
                    setProp(KRImageView.PROP_SRC, placeholder)
                    this@KRWrapperImageView.addView(this, 0)
                }
            } else {
                removePlaceholder()
            }
        }
    }

    private fun removePlaceholder() {
        placeholderView?.removeFromParent()
        placeholderView = null
        placeholder = ""
    }

    companion object {
        private const val PROP_PLACEHOLDER = "placeholder"
        const val VIEW_NAME = "KRWrapperImageView"
    }

}

class NinePatchBuilder(private var resources: Resources, private val bitmap: Bitmap) {

    private val width: Int = bitmap.width
    private val height: Int = bitmap.height
    private val xRegions = mutableListOf<Int>()
    private val yRegions = mutableListOf<Int>()

    fun addXCenteredRegion(width: Int): NinePatchBuilder {
        val x = ((this.width - width) / 2)
        xRegions.add(x)
        xRegions.add(x + width)
        return this
    }

    fun addYCenteredRegion(height: Int): NinePatchBuilder {
        val y = ((this.height - height) / 2)
        yRegions.add(y)
        yRegions.add(y + height)
        return this
    }

    fun build(): NinePatchDrawable = NinePatchDrawable(resources, buildNinePatch())

    private fun buildNinePatch(): NinePatch = NinePatch(bitmap, buildChunk(), null)

    private fun buildChunk(): ByteArray {
        if (xRegions.isEmpty()) {
            xRegions.add(DEFAULT_VALUE)
            xRegions.add(width)
        }
        if (yRegions.isEmpty()) {
            yRegions.add(DEFAULT_VALUE)
            yRegions.add(height)
        }

        val byteBuffer =
            ByteBuffer.allocate(getNinePathChunkBufferSize() * INT_BYTE_SIZE)
                .order(ByteOrder.nativeOrder())
        byteBuffer.put(TRANSLATED_VALUE.toByte()) // was translated
        byteBuffer.put(xRegions.size.toByte()) // divisions x
        byteBuffer.put(yRegions.size.toByte()) // divisions y
        byteBuffer.put(COLOR_SIZE.toByte()) // color size

        // skip
        byteBuffer.putInt(DEFAULT_VALUE)
        byteBuffer.putInt(DEFAULT_VALUE)

        // padding -- always 0 -- left right top bottom
        byteBuffer.putInt(DEFAULT_VALUE)
        byteBuffer.putInt(DEFAULT_VALUE)
        byteBuffer.putInt(DEFAULT_VALUE)
        byteBuffer.putInt(DEFAULT_VALUE)

        // skip
        byteBuffer.putInt(DEFAULT_VALUE)
        for (rx in xRegions) { // regions left right left right
            byteBuffer.putInt(rx)
        }
        for (ry in yRegions) { // regions top bottom top bottom
            byteBuffer.putInt(ry)
        }
        for (i in 0 until COLOR_SIZE) {
            byteBuffer.putInt(NO_COLOR)
        }
        return byteBuffer.array()
    }

    private fun getNinePathChunkBufferSize(): Int {
        return NINE_PATH_HEADER_META_SIZE + NINE_PATH_HEADER_SKIP_SIZE +
                NINE_PATH_HEADER_PADDING_SIZE + NINE_PATH_HEADER_SKIP_AFTER_PADDING_SIZE +
                xRegions.size + yRegions.size + COLOR_SIZE
    }

    companion object {
        private const val NO_COLOR = 1
        private const val COLOR_SIZE = 9
        private const val DEFAULT_VALUE = 0
        private const val TRANSLATED_VALUE = 1

        private const val NINE_PATH_HEADER_META_SIZE = 1
        private const val NINE_PATH_HEADER_SKIP_SIZE = 2
        private const val NINE_PATH_HEADER_PADDING_SIZE = 4
        private const val NINE_PATH_HEADER_SKIP_AFTER_PADDING_SIZE = 1
        private const val INT_BYTE_SIZE = 4
    }
}
