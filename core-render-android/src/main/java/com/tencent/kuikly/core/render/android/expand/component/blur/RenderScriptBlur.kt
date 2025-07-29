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
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import kotlin.math.max
import kotlin.math.min

/**
 * Created by kam on 2023/5/1.
 */
class RenderScriptBlur(context: Context) : IBlur {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val renderScript = RenderScript.create(context)
    private val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
    private var outAllocation: Allocation? = null

    private var lastBitmapWidth = -1
    private var lastBitmapHeight = -1

    private var hadDestroy = false

    override fun blur(bitmap: Bitmap, radius: Float): Bitmap {
        // rs要求半径在(0 < r <= 25)
        // 1.5f为放大系数，用于对齐ScriptBlur与EffectBlur的模糊半径效果
        val r = (radius * 1.5f).let { scaledRadius ->
            if (scaledRadius <= 0f) {
                return bitmap
            } else {
                scaledRadius.coerceAtMost(25f)
            }
        }

        val allocation = Allocation.createFromBitmap(renderScript, bitmap)
        if (!canReuseAllocation(bitmap)) {
            outAllocation?.destroy()
            outAllocation = Allocation.createTyped(renderScript, allocation.type)
            lastBitmapWidth = bitmap.width
            lastBitmapHeight = bitmap.height
        }

        try {
            blurScript.setRadius(r)
            blurScript.setInput(allocation)
            blurScript.forEach(outAllocation)
            outAllocation?.copyTo(bitmap)
        } catch (t: Throwable) {
            KuiklyRenderLog.e("RenderScriptBlur", "blur error: $t")
        }
        allocation.destroy()
        return bitmap
    }

    override fun draw(canvas: Canvas, bitmap: Bitmap) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    override fun destroy() {
        if (hadDestroy) {
            return
        }
        hadDestroy = true
        blurScript.destroy()
        renderScript.destroy()
        outAllocation?.destroy()
    }

    private fun canReuseAllocation(bitmap: Bitmap): Boolean {
        return bitmap.height == lastBitmapHeight && bitmap.width == lastBitmapWidth
    }

    companion object {
        fun blurImage(drawable: Drawable, context: Context, blurRadius: Float): Drawable? {
            if (drawable !is BitmapDrawable) {
                return null
            }
            // resize to 150 px
            // 将Drawable转换为Bitmap
            val bitmap = (drawable as BitmapDrawable).bitmap
            if (bitmap.width == 0) {
                return null
            }

            // 将Drawable等比缩小到宽度为150px的分辨率
            val targetWidth = 150f
            val scaleFactor = targetWidth / bitmap.width
            val targetHeight = bitmap.height * scaleFactor

            // 创建一个空的 ARGB_8888 格式的位图
            val resizedBitmap = Bitmap.createBitmap(
                targetWidth.toInt(),
                targetHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )

            // 使用 Canvas 将原始位图绘制到新创建的位图上
            val canvas = Canvas(resizedBitmap)
            canvas.drawBitmap(
                bitmap,
                Rect(0, 0, bitmap.width, bitmap.height),
                Rect(0, 0, targetWidth.toInt(), targetHeight.toInt()),
                null
            )
            // 使用RenderScript进行高斯模糊
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, resizedBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
            val output = Allocation.createTyped(rs, input.type)
            val script = ScriptIntrinsicBlur.create(rs,  Element.U8_4(rs))

            val reBlurCount = if (blurRadius == 12.5f) 5 else 3
            val nBlurRadius = min(25f, max(0f,blurRadius / 12.5f * 25f))

            for (i in 0..reBlurCount) {
                input.copyFrom(resizedBitmap)
                script.setInput(input)
                script.setRadius(nBlurRadius)
                script.forEach(output)
                output.copyTo(resizedBitmap)
            }
            // 释放资源
            input.destroy()
            output.destroy()
            script.destroy()
            rs.finish()
            
            // 将Bitmap转换回Drawable
            return BitmapDrawable(context.resources, resizedBitmap)

        }
    }
}