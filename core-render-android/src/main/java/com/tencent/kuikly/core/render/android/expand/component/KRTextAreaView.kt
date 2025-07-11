/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * 多行输入框
 */
class KRTextAreaView(context: Context, softInputMode: Int?) : KRTextFieldView(context, softInputMode) {

    init {
        gravity = Gravity.LEFT or Gravity.TOP
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_GET_INNER_CONTENT_HEIGHT -> getContentHeight(callback)
            else -> super.call(method, params, callback)
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            RETURN_KEY_TYPE -> setReturnKeyType(propValue)
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun getContentHeight(callback: KuiklyRenderCallback?) {
        callback?.invoke(mapOf(
            "height" to kuiklyRenderContext.toDpF(layout.height.toFloat())
        ))
    }

    private fun setReturnKeyType(value: Any): Boolean {
        /**
         * 安卓 EditText 组件，默认情况下 IMEAction 和多行模式有冲突
         * 即默认情况下，如果设置了 isSingleLine=false，再设置 IMEAction 会无效
         *
         * 多行文本输入框需要设置 IMEAction（即 ReturnKeyType）的场景中，实际由于
         * 换行键会被替换成 "go" "next" "search" "send" "done" 等 IMEAction 键，
         * 因此实际上是单行模式（文本无法换行）只是长文本折行显示而已。
         *
         * 因此此处的处理方式为：
         * 1. 设置 isSingleLine=true 改为单行输入模式
         * 2. setHorizontallyScrolling(false) 禁止横向滚动来启用折行展示
         * 3. maxLines = Integer.MAX_VALUE 设置最大行数，否则文本区域会被截断
         * 4. 设置 IMEAction 键盘动作按钮展示
         */
        val method = transformationMethod
        isSingleLine = true
        transformationMethod = method // isSingleLine = true时，会将\n替换成空格, 不符合多行输入框的表现，这里强制把transformationMethod设置回去
        maxLines = Integer.MAX_VALUE
        setHorizontallyScrolling(false)

        imeOptions = when (value as String) {
            "search" -> {
                EditorInfo.IME_ACTION_SEARCH
            }
            "send" -> {
                EditorInfo.IME_ACTION_SEND
            }
            "done" -> {
                EditorInfo.IME_ACTION_DONE
            }
            "next" -> {
                EditorInfo.IME_ACTION_NEXT
            }
            "go" -> {
                EditorInfo.IME_ACTION_GO
            }
            else -> {
                EditorInfo.IME_NULL
            }
        }
        return true
    }

    companion object {
        const val VIEW_NAME = "KRTextAreaView"

        private const val RETURN_KEY_TYPE = "returnKeyType"

        const val METHOD_GET_INNER_CONTENT_HEIGHT = "getInnerContentHeight"
    }

    override fun initSingleLine() {
        isSingleLine = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 解决Scroller内TextArea内容无法滚动
        if (event.actionMasked == MotionEvent.ACTION_DOWN &&
            computeVerticalScrollRange() > computeVerticalScrollExtent()) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.onTouchEvent(event)
    }
}
