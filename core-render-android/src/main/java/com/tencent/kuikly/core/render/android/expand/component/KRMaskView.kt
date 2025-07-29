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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import com.tencent.kuikly.core.render.android.const.KRCssConst.VISIBILITY
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.layer.KuiklyRenderLayerHandler.Companion.HR_SET_PROP_OPERATION

/**
 * Created by kam on 2023/6/7.
 */
class KRMaskView(context: Context) : KRView(context) {

    private var maskBitmap: Bitmap? = null
    private var maskBitmapInvalidated = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val porterDuffXferMode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override val reusable: Boolean
        get() = false

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (maskBitmapInvalidated) {
            updateBitmapMask()
            maskBitmapInvalidated = false
        }

        maskBitmap?.also { bitmap ->
            paint.xfermode = porterDuffXferMode
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            paint.xfermode = null
        }
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        if (!maskBitmapInvalidated) {
            getChildAt(0)?.also {
                if (it === child) {
                    maskBitmapInvalidated = true
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            maskBitmapInvalidated = true
        }
    }

    private fun updateBitmapMask() {
        getChildAt(0)?.also { maskView ->
            maskView.visibility = View.VISIBLE
            maskBitmap?.recycle()
            maskBitmap = getBitmapFromView(maskView)
            maskView.visibility = View.GONE
            // 记录设置了VISIBILITY，以免节点复用继承了GONE属性
            val setPropOperationSet =
                kuiklyRenderContext?.getViewData<MutableSet<String>>(maskView, HR_SET_PROP_OPERATION)
                    ?: mutableSetOf<String>().apply {
                        kuiklyRenderContext?.putViewData(maskView, HR_SET_PROP_OPERATION, this)
                    }
            setPropOperationSet.add(VISIBILITY)
        }
    }

    private fun getBitmapFromView(view: View): Bitmap? {
        val w = frameWidth
        val h = frameHeight
        if (w <= 0 || h <= 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    companion object {
        const val VIEW_NAME = "KRMaskView"
    }
}