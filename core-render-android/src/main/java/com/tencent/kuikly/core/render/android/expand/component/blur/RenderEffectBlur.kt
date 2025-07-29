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
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Created by kam on 2023/5/1.
 */
@RequiresApi(Build.VERSION_CODES.S)
class RenderEffectBlur(private val context: Context) : IBlur {

    private val node = RenderNode("BlurViewNode")

    private var width = 0
    private var height = 0
    private var lastBlurRadius = 1f

    private var fallbackBlur: IBlur? = null

    private var hadDestroy = false

    override fun blur(bitmap: Bitmap, radius: Float): Bitmap {
        lastBlurRadius = radius

        if (bitmap.width != width || bitmap.height != height) {
            height = bitmap.height
            width = bitmap.width
            node.setPosition(0, 0, width, height)
        }
        val canvas = node.beginRecording()
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        node.endRecording()
        node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR))
        return bitmap
    }

    override fun draw(canvas: Canvas, bitmap: Bitmap) {
        if (canvas.isHardwareAccelerated) {
            canvas.drawRenderNode(node)
        } else {
            if (fallbackBlur == null) {
                fallbackBlur = RenderScriptBlur(context)
            }
            fallbackBlur!!.blur(bitmap, lastBlurRadius)
            fallbackBlur!!.draw(canvas, bitmap)
        }
    }

    override fun destroy() {
        if (hadDestroy) {
            return
        }
        hadDestroy = true
        node.discardDisplayList()
        fallbackBlur?.destroy()
    }
}