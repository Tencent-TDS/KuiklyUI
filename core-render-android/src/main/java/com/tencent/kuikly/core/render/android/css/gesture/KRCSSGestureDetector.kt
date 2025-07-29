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

package com.tencent.kuikly.core.render.android.css.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.lang.ref.WeakReference

/**
 * 手势检测器。在[GestureDetector]的基础上扩展了pan事件
 */
class KRCSSGestureDetector(
    context: Context,
    targetView: View,
    private val listener: KRCSSGestureListener
) : GestureDetector(context, listener) {

    init {
        setIsLongpressEnabled(false)
    }

    private val targetViewWeakRef = WeakReference(targetView)

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (containPanEvent() && ev.action == MotionEvent.ACTION_DOWN) { // 对齐原来逻辑，有pan事件时, 要求父亲不拦截
            disallowParentInterceptEvent(true)
        }

        val handle = super.onTouchEvent(ev)

        if (listener.isPanEventHappening) { // 触发onScroll时，系统不会在KRCSSGestureListener中回调up和cancel，这里手动补
            if (ev.action == MotionEvent.ACTION_UP) {
                listener.onUp(ev)
                disallowParentInterceptEvent(false)
            } else if (ev.action == MotionEvent.ACTION_CANCEL) {
                listener.onCancel(ev)
                disallowParentInterceptEvent(false)
            }
        }

        if (listener.isLongPressEventHappening) {
            if (ev.action == MotionEvent.ACTION_UP || ev.action == MotionEvent.ACTION_CANCEL) {
                listener.isLongPressEventHappening = false
                listener.onLongPressMoveOrEnd(ev)
            } else if (ev.action == MotionEvent.ACTION_MOVE) {
                listener.onLongPressMoveOrEnd(ev)
            }
        }

        return handle
    }

    private fun disallowParentInterceptEvent(disallow: Boolean) =
        targetViewWeakRef.get()?.parent?.requestDisallowInterceptTouchEvent(disallow)

    private fun containPanEvent(): Boolean = listener.containEvent(KRCSSGestureListener.TYPE_PAN)

    fun hasListener(type: Int): Boolean = listener.containEvent(type)
    fun addListener(type: Int, callback: KuiklyRenderCallback) {
        if (type == KRCSSGestureListener.TYPE_LONG_PRESS) {
            setIsLongpressEnabled(true)
            listener.addListener(type) {
                // 对齐逻辑，有longPress事件时，要求父亲不拦截
                (it as? Map<*, *>)?.let { eventMap ->
                    val state = eventMap[KRCSSGestureListener.EVENT_STATE]
                    if (state == KRCSSGestureListener.EVENT_STATE_START) {
                        disallowParentInterceptEvent(true)
                    }
                }
                callback(it)
            }
        } else {
            listener.addListener(type, callback)
        }
    }

    companion object {
        const val GESTURE_TAG = "hr_gesture_tag"
    }
}
