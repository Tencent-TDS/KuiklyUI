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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.os.SystemClock
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport

class KRActivityIndicatorView(context: Context) :
    FrameLayout(context) , IKuiklyRenderViewExport {
    private var mInnerLoadingView: ProgressBar? = null
    private var mRotateDrawable: HRRotateDrawable? = null

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        mInnerLoadingView = ProgressBar(context).apply {
            layoutParams  =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            addView(this)
        }
        updateRotateDrawable(WHITE_STYLE)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            KRActivityIndicatorView.PROPS_STYLE -> setStyle(propValue as String)
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun setStyle(style: String) : Boolean {
        updateRotateDrawable(style)
        return true
    }

    override fun onAddToParent(parent: ViewGroup) {
        super.onAddToParent(parent)
        startAnimating()
    }

    override fun onRemoveFromParent(parent: ViewGroup) {
        super.onRemoveFromParent(parent)
        stopAnimating()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAnimating()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // 同步drawable大小
        super.onLayout(changed, left, top, right, bottom)
        if (mRotateDrawable != null) {
            mRotateDrawable?.setBounds(0, 0, right - left, bottom - top)
        }
    }

    /* 开始转圈圈 */
    private fun startAnimating() {
        mRotateDrawable?.startAnimate()
    }

    /* 停止转圈圈 */
    private fun stopAnimating() {
        mRotateDrawable?.stopAnimate()
    }

    private fun updateRotateDrawable(style: String) {
        //drawable
        val imageDrawable = if (style == GRAY_STYLE) {
            base64ToDrawable(GRAY_IMAGE)
        } else {
            base64ToDrawable(WHITE_IMAGE)
        }
        if (imageDrawable != null) {
            val rotateDrawable: HRRotateDrawable = HRRotateDrawable(imageDrawable)
            mInnerLoadingView?.indeterminateDrawable = rotateDrawable
            stopAnimating()
            mRotateDrawable = rotateDrawable
            startAnimating()
            mInnerLoadingView?.refreshDrawableState()
        }
    }

    private fun base64ToDrawable(base64Str: String): Drawable? {
        val img = Base64.decode(base64Str, Base64.DEFAULT)
        val bitmap: Bitmap
        if (img != null) {
            bitmap = BitmapFactory.decodeByteArray(img, 0, img.size)
            return BitmapDrawable(bitmap)
        }
        return null
    }

    companion object {
        const val VIEW_NAME = "KRActivityIndicatorView"
        const val PROPS_STYLE = "style"
        const val GRAY_STYLE = "gray"
        const val WHITE_STYLE = "white"
        const val GRAY_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAS1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADmYDp0AAAAGXRSTlMAJk0zQGZzBCITGh8OCggXSTotRDEqXVVqQNv4pgAABP9JREFUeNrs2ctyozAQheHTGKEbEtgk2O//pJOaSSjUQUiAqGHhb+vNX6ZpjIy3t6vTlshqXItT9JdyuBBHkyt1KZooXIammevMl6UZi6ugAK7iP2ZJ83iYI1m+bT1Ku72aL6/b3iyt6IvSKGpovlX7sgx961BQ30zqPVkdTQyKsc2M2p7laMahlKqZEXJzlqIZg1JEM3fbmqVpTqGUJvBy27IkhVDKqwn027I6CqiCsxXqtmR5ChmUQizruSXLUqhFMSPr+szPailkUY5hWUJmZykKOTAlp+uem6WLThbXNkyblyWJkSjqzrKqvCxDIY2ypGBdJifLUUihtE+WNUazzl0O3JN1PWJZ5y4HrmNZL5/KkopCHifoWVedytIU6nAG92JdOpJ18nLgbnxJxF5f08vh7CURf9n3RZeDGUYx9gpLFMvq145GTNbDsDWKlHFIqMU/T5OxJMTaQZLKeBg6yz6NVk0GB3C6Ca0du6WXgzSZd+lDzN0kuCFcXWuHlCq1HHTuDSFHERgVGB8siSdi+GwpCaZV2XdEJ7hKI/TBfqTGtWvfhbfEecSQ+K120akfscpG51129FubyGLGDxle6OkSeqySU5eNDFVmVicWPS1mXPW9tKba1HgZyc7KF3lEjWJZrzHT3obhQyOD1F2ngyhvaJnKWhDc3aMA2VGMxopBxIwPHKYVxRisuouoXuIYQ1EdEuxTxDw9DpCWYpRDkvwYRcRwznelkcXVIqLFbpIiOolcuhKLHthN0yLrsYUaF9cEdutogWqxkbydn6WxgxvOvYhGYh/zPG/krcN+FIxYjQMMG6pD5F1MKlnqz1AtcVTb/1R5HCItG6qDupq9Om7DXwo93t7e3gqQVPfVor4miZK0pQirEdJDtWLQKMYpWqFcUFUl6GJVlDDrkkOVMEiUoShBYUJVEqEITUkaP+oqqUYRlpIsfvRVUo8iKMPVs/60c7Y7DoJAFKVAguInFN7/VTfpbqnedTRt6jA/PE9wVLzKMCD0IQod8kIDQmicSv34SP1UX1xcXHxGm+7zHLys0shg9C/BySkkuawLQUzZrZv1giSjSNlavWb4Ykl3/HxQIU31Ari7zfofsfZygb/rDWLdxZU+6E2amktRY9IEQ8WFu2bWBKbeMud01xS23qJw1iSh3hK60RRzU6/hoNEU0VVsz6AGe+ixIcUPHzazdO83s7TE7HVa/1PYB5mt9afbHlRuZWXsH8kxNEpRWgmuItqCYWora8lBVfB2wcTThIdD/u4V4IJdEDlaFjEg5o2SyM0uCQtfn1PK3n2nwROJumCGre6bNa9rj+ZB7L/TDosk/YvdtE92+271ptATdb/uveZhpDX0pLC1a/LzsqMpRHdCq/U+AbT6p4FZ4MnG9JPoig+8iNksyMxt/CPerHLrk1mQmDc93MDq9VTMCt4tIj1YBUdo8W6oSaDlFaHFuv1oAiujKC3OzVqOCAdCiyskGiJJCS2mkBgtMO5rMYVEJsKB0GLYZEqEA63FtyXXgNakDrRYQqIFq6SOtFhCImI47GqxbY6HzLqpXS22owTo8U5r0SFx0t3q1L4W2zEVGX7g97V4DvXAN7F9X2s4Kbgy5PuuFseBMfhnmtWh1pnH6yA+PMZVp461zj2MCOm9hxglteijmzhBLSnAhEwKMH2VAkz2pQClETGsCkmCKGU3UValSCnoCV5cbPIDQR5TIrqY83gAAAAASUVORK5CYII="
        const val WHITE_IMAGE = "iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAb1BMVEUAAAD///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////8v0wLRAAAAJHRSTlMATWZZv5mM2UUHIDM+FBo4JgwrEVJ9qDCHbbd0XejRx5D2sZ/haBjGAAAFg0lEQVR42uzZW5eaMBSG4W9TMEfOHaxOFUfz/39ju9oZS5AQDnGVC59Lr94VN1tAvLxsXaGJdIFtSTn9wVNsSEp3W+ridMexGQV1bGe+NHVobAVZsBX/MUvkUaTXZKVS1gjtcDK/nd6XZklOv3GJoBLzqVmWpejTHgE15m63JKuiO4VguOmg+VkZdaQBD6uDidlZnOgZx3UyXe9zsyR1cYTyYWV9ZPOyBNkCnpblNi9rT086rauxqTlZNdlyhPLd2No5WZpsGYI5G9u36VkZ2TTC0cZ2EpOzONnSsGve9jY1qyCbQkjyw1g+5LQsQT0CQb0ZWzMtS5GtQFjiZGx6SlZKNo7Qvhnb2ZPlWA7BtcYW+bMk2TTCU/0lUfqyBCdbjSe4GdvOl1WRbY9nyPpLohjPKqlH4Cneje06/viak03iOQQztnzsYb8OuhzyS3xuE44h1N+pY69G1KTlkOWcuErhsWN/XRUGHHsX49iLJD7hNivV034qf7C7S4YHVW/ox167ka3EA6EmXqU/Wcf5INCX9E/L/ZKSU1eFB8XUC0K0zBITeuqT6ThiRE4dXKAn45OvCMX6mgq2Q+8m1S0bO4taU18Nl+/s0Y/UOfUtRmnnvIs9PZKerJ5zJNBRnu9VKUYJ7fiJlkSzshQbFHN0pI354ybg83koSjiGypLCqWXDbgU65CFJDhITlLKqZGl9ktMwDreIubzVCEBU5CIx4sJc2p9YTXJyURj1xpySEuvk5FTBg8fM5VpjBaHJhWfwEtGZOVywgvKMlVf6gzlkWKwkh73AVFXDBq0Ye0mDdI05KB5cE1isWjZUfeJwfnpWgQWyy3O/RCWwjLoymww38jrFct9a1rHDCnvq4BKrlO//RqwRof4MrQTWkslXVQ0EWfN5iRDULj63N8JC/YfCGi8vLy8BiCg5xoOOSSQQUqHJQRewqSYe0SgEk3IawVOrKvZQwarIo9MlmtijEQiDkwfHXRR7RQiiIK8CX5LYK0EQmrw0vhxjryOCoAm2nvWrXbtZchSEogB85RYUoPEnwVJLF/3+TzlVdoeBO9iMUUoWfttkccqQo8LN9EfMdMlnWhCZ1mmuN59cb9W32+32Gc1GLqYmr62ReuLf+i6fjSRluDVns+1WCe5geWxS6p77Hhls6b4Yp+TlG+Cq4P/Cq48LmpEH4LWHK3rmQfLKo6hu4RseFx7cyS++gV13zDmMfEvfXXYojHzTpC47Qmd8y5e8buBA8i3YXTieIXjYXPujc8b8372ja8uyPTzMonnQOPj1IVao4osJV83B0Z8mvKgUONQsfkyR31VJ/CHVzkGpeCxGapgJa4r0H1rVobEyzamZfrkSjhJ+odHRHhnCo0t+bIDoRuFgsYtlmQMji7QgCgWUEa7enSE1jBn3Ecqgq9w14EktZFERtfCMfz9AtsIa3tD3+mgcltZ83wY/FeGrVTPL5irQIyPDwxHt+lIY/tpT+BC+KWQWKltavsio9QE9ifVeDyVzlO8rgb4CEpEkFbOrmzmM7RL0PSGJbiQL3l565rHfR0JBCih8BfweCwb0NZBATVL1KhZLGfQ94HwTidVALBZopCVxupKkmiEeCyT6NJxMBcohHouWhAErTTksEIkV7tQBTvWi5fCKxNoqiQ7OtAifhHis1TNlSeiNcojHgiJhScwk1gDxWOlLYiCpJojG2i6JGs7CSKx6T6wX+io4C+kshD2xoEnVXaNfDt2+WAp9ia6WhF2xaEkUaVqrh72xoEjTXK1wPPfHqtP8EwHJw9/OWFAmqnmba4FPYkGV6Am16td1VcFnseBp1nJo4Wy6aTTAjljEQ+sHpBOPlYtMY5HX11yQl/1ckK2RbHgbSRmx225ZpbKblBn9grdb0B9tgZ0KTsLMSAAAAABJRU5ErkJggg=="
    }

}

class HRRotateDrawable(private val imageDrawable: Drawable) : Drawable(),
    Animatable {
    var frameDuration: Long = 70 // 默认值（和iOS经典菊花一样的旋转速度）
    var frameRotateDeg = 30.0f
    private var mCurrentDeg = 0f
    private var mRunning = false

    // 初始化nextFrame
    private val mNextFrame = Runnable {
        if (mRunning) {
            mCurrentDeg += frameRotateDeg
            if (mCurrentDeg > 360.0f - frameRotateDeg) {
                mCurrentDeg = 0.0f
            }
            invalidateSelf()
            nextFrame()
        }
    }

    fun startAnimate() {
        if (!mRunning) {
            mRunning = true
            nextFrame()
        }
    }
    fun stopAnimate() {
        mRunning = false
        unscheduleSelf(mNextFrame)
    }

    override fun draw(canvas: Canvas) {
        val drawable = imageDrawable
        val bounds = drawable.bounds
        val w = bounds.right - bounds.left
        val h = bounds.bottom - bounds.top
        val px = w * 0.5f // 这里固定绕中心旋转 满足 loading需求
        val py = h * 0.5f
        val saveCount = canvas.save()
        canvas.rotate(mCurrentDeg, px + bounds.left, py + bounds.top)
        drawable.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    override fun start() {
    }

    override fun stop() {
    }

    override fun isRunning(): Boolean {
        return mRunning
    }

    // 开始定时执行
    private fun nextFrame() {
        scheduleSelf(mNextFrame, SystemClock.uptimeMillis() + frameDuration)
    }

    override fun setAlpha(i: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getOpacity(): Int {
        return PixelFormat.UNKNOWN
    }

    override fun setBounds( bounds: Rect) {
        super.setBounds(bounds)
        imageDrawable.bounds = bounds
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        imageDrawable.setBounds(left, top, right, bottom)
    }

}