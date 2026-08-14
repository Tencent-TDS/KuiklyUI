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

package com.tencent.kuikly.android.demo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * Demo 自定义 View，对齐鸿蒙侧 `KRMyDemoCustomView.ets`：
 * - 支持 `message` 属性：居中显示文字（黄底黑边）
 * - 支持 `onMyViewTapped` 事件：Tap Me 按钮点击回调
 * - 支持 kuikly 侧嵌入的子节点（作为 FrameLayout 由框架 addChild 到本 View 上）
 *
 * 无障碍属性 `accessibility` / `accessibilityRole` / `accessibilityInfo` 走 `KRView` 基类
 * 的通用属性通道（见 `KRCSSViewExtension.kt`），此处无需重写。
 * `accessibilityAnnounce` / `accessibilityFocus` method 走 `IKuiklyRenderViewExport.call`
 * 基类实现，此处无需重写。
 */
class KRMyDemoCustomView(context: Context) : KRView(context) {

    private val decorLayout: LinearLayout
    private val messageText: TextView
    private val tapButton: Button

    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f * context.resources.displayMetrics.density
    }

    private var onMyViewTapped: KuiklyRenderCallback? = null

    init {
        setBackgroundColor(Color.YELLOW)
        setWillNotDraw(false)

        decorLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        messageText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        tapButton = Button(context).apply {
            text = "Tap Me"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setOnClickListener {
                onMyViewTapped?.invoke("{}")
            }
        }
        decorLayout.addView(messageText)
        decorLayout.addView(tapButton)
        // 装饰层放在最下面，kuikly 侧后续 addChild 的子节点会盖在上面（对齐鸿蒙侧 Stack 语义）。
        addView(decorLayout)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_MESSAGE -> {
                messageText.text = propValue as? String ?: ""
                true
            }
            PROP_ON_MY_VIEW_TAPPED -> {
                onMyViewTapped = propValue as? KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        // 绘制黑色边框
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRect(inset, inset, width - inset, height - inset, borderPaint)
    }

    companion object {
        const val VIEW_NAME = "KRMyDemoCustomView"
        private const val PROP_MESSAGE = "message"
        private const val PROP_ON_MY_VIEW_TAPPED = "onMyViewTapped"
    }
}
