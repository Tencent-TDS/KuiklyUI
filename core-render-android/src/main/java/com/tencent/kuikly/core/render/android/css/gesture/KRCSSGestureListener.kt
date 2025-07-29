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

import android.view.GestureDetector
import android.view.MotionEvent
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.lang.ref.WeakReference

/**
 * 手势处理类
 * 目前支持以下三种手势:
 * 1.[TYPE_CLICK]: 单击事件
 * 2.[TYPE_DOUBLE_CLICK]: 双击事件
 * 3.[TYPE_LONG_PRESS]: 长按事件
 * 4.[TYPE_PAN]: 拖拽事件
 */
class KRCSSGestureListener(private val kuiklyContext: IKuiklyRenderContext?) : GestureDetector.SimpleOnGestureListener() {

    /**
     * 事件监听列表
     */
    private val gestureListeners = mutableListOf<GestureObserve>()

    /**
     * 是否正在触发pan事件
     */
    var isPanEventHappening = false

    /**
     * 是否正在触发longPress事件
     */
    var isLongPressEventHappening = false

    private var gestureDetectorWeakRef: WeakReference<GestureDetector>? = null

    fun setGestureDetector(gestureDetector: GestureDetector) {
        gestureDetectorWeakRef = WeakReference(gestureDetector)
    }

    fun setLongPressEnable(enable: Boolean) {
        gestureDetectorWeakRef?.get()?.setIsLongpressEnabled(enable)
    }

    /**
     * 添加感兴趣的事件
     * @param type 事件类型
     * @param callback 事件回调
     */
    fun addListener(type: Int, callback: KuiklyRenderCallback) {
        for (observe in gestureListeners) {
            if (observe.type == type) {
                observe.callback = callback
                return
            }
        }
        gestureListeners.add(GestureObserve(callback, type))
    }

    /**
     * 是否有注册事件
     * @param type 事件类型
     * @return 是否有注册对应的事件
     */
    fun containEvent(type: Int): Boolean {
        for (observer in gestureListeners) {
            if (observer.type == type) {
                return true
            }
        }
        return false
    }

    /**
     * 开始触发pan事件
     * @param e 手势事件
     * @return 是否消费事件
     */
    private fun onPanDown(e: MotionEvent?, e2: MotionEvent): Boolean {
        val downEvent = e ?: e2
        dispatchPanEvent(MotionEvent.ACTION_DOWN, downEvent)
        return true
    }

    /**
     * pan事件触发中
     * @param e 手势事件
     * @return 是否消费事件
     */
    private fun onPanMove(e: MotionEvent): Boolean {
        dispatchPanEvent(MotionEvent.ACTION_MOVE, e)
        return true
    }

    /**
     * pan事件结束
     * @param e 手势事件
     * @return 是否消费事件
     */
    private fun onPanEnd(e: MotionEvent): Boolean {
        dispatchPanEvent(MotionEvent.ACTION_UP, e)
        return false
    }

    /**
     * LongPress事件触发中或结束
     * @param e 手势事件
     * @return 是否消费事件
     */
    fun onLongPressMoveOrEnd(e: MotionEvent): Boolean {
        dispatchEvent(
            TYPE_LONG_PRESS, mapOf(
                KRViewConst.X to kuiklyContext.toDpF(e.x),
                KRViewConst.Y to kuiklyContext.toDpF(e.y),
                EVENT_STATE to convertAction(e.action),
                PAGE_X to kuiklyContext.toDpF(e.rawX),
                PAGE_Y to kuiklyContext.toDpF(e.rawY)
            )
        )
        return true
    }

    override fun onDown(e: MotionEvent): Boolean = true

    fun onUp(e: MotionEvent) {
        if (isPanEventHappening) {
            onPanEnd(e)
        }
        isPanEventHappening = false
    }

    fun onCancel(e: MotionEvent) {
        if (isPanEventHappening) {
            onPanEnd(e)
        }
        isPanEventHappening = false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return if (!containEvent(TYPE_DOUBLE_CLICK)) {
            return dispatchEvent(
                TYPE_CLICK, mapOf(
                    KRViewConst.X to kuiklyContext.toDpF(e.x),
                    KRViewConst.Y to kuiklyContext.toDpF(e.y),
                    PAGE_X to kuiklyContext.toDpF(e.rawX),
                    PAGE_Y to kuiklyContext.toDpF(e.rawY)
                )
            )
        } else {
            false
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return if (containEvent(TYPE_DOUBLE_CLICK)) {
            dispatchEvent(
                TYPE_CLICK, mapOf(
                    KRViewConst.X to kuiklyContext.toDpF(e.x),
                    KRViewConst.Y to kuiklyContext.toDpF(e.y),
                    PAGE_X to kuiklyContext.toDpF(e.rawX),
                    PAGE_Y to kuiklyContext.toDpF(e.rawY)
                )
            )
        } else {
            false
        }
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return if (!containEvent(TYPE_DOUBLE_CLICK)) {
            dispatchEvent(
                TYPE_CLICK, mapOf(
                    KRViewConst.X to kuiklyContext.toDpF(e.x),
                    KRViewConst.Y to kuiklyContext.toDpF(e.y),
                    PAGE_X to kuiklyContext.toDpF(e.rawX),
                    PAGE_Y to kuiklyContext.toDpF(e.rawY)
                )
            )
        } else {
            dispatchEvent(
                TYPE_DOUBLE_CLICK, mapOf(
                    KRViewConst.X to kuiklyContext.toDpF(e.x),
                    KRViewConst.Y to kuiklyContext.toDpF(e.y),
                    PAGE_X to kuiklyContext.toDpF(e.rawX),
                    PAGE_Y to kuiklyContext.toDpF(e.rawY)
                )
            )
        }
    }

    override fun onLongPress(e: MotionEvent) {
        isLongPressEventHappening = true
        dispatchEvent(
            TYPE_LONG_PRESS, mapOf(
                KRViewConst.X to kuiklyContext.toDpF(e.x),
                KRViewConst.Y to kuiklyContext.toDpF(e.y),
                EVENT_STATE to convertAction(e.action),
                PAGE_X to kuiklyContext.toDpF(e.rawX),
                PAGE_Y to kuiklyContext.toDpF(e.rawY)
            )
        )
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        // 通过滚动事件来作为pan事件
        return if (containEvent(TYPE_PAN)) {
            if (!isPanEventHappening) { // 首次发生pan事件时, 补down事件
                onPanDown(e1, e2)
                isPanEventHappening = true
            }
            onPanMove(e2)
            true
        } else {
            false
        }
    }

    private fun dispatchPanEvent(action: Int, e: MotionEvent) {
        dispatchEvent(TYPE_PAN, mapOf(
            KRViewConst.X to kuiklyContext.toDpF(e.x),
            KRViewConst.Y to kuiklyContext.toDpF(e.y),
            EVENT_STATE to convertAction(action),
            PAGE_X to kuiklyContext.toDpF(e.rawX),
            PAGE_Y to kuiklyContext.toDpF(e.rawY)
        ))
    }

    private fun dispatchEvent(type: Int, result: Map<String, Any>): Boolean {
        for (observe in gestureListeners) {
            if (observe.type == type) {
                observe.callback.invoke(result)
                return true
            }
        }
        return false
    }

    private fun convertAction(action: Int): String {
        return when (action) {
            MotionEvent.ACTION_DOWN -> EVENT_STATE_START
            MotionEvent.ACTION_MOVE -> EVENT_STATE_MOVE
            else -> EVENT_STATE_END
        }
    }

    companion object {
        const val TYPE_CLICK = 1
        const val TYPE_DOUBLE_CLICK = 2
        const val TYPE_LONG_PRESS = 3
        const val TYPE_PAN = 4

        const val EVENT_STATE = "state"
        private const val PAGE_X = "pageX"
        private const val PAGE_Y = "pageY"
        const val EVENT_STATE_START = "start"
        private const val EVENT_STATE_MOVE = "move"
        private const val EVENT_STATE_END = "end"
    }

    class GestureObserve(
        var callback: KuiklyRenderCallback,
        val type: Int
    )
}
