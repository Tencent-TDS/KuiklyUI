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
import android.view.SoundEffectConstants
import android.view.View
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.lang.ref.WeakReference
import kotlin.math.hypot

/**
 * 手势检测器。在[GestureDetector]的基础上扩展了pan、pinch事件
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

    // region pinch(捏合)手势
    //
    // 未使用 android.view.ScaleGestureDetector，而是直接跟踪两指间距计算缩放倍数。
    //
    // 原因: ScaleGestureDetector 面向「带阈值的增量式缩放」设计，与 Kuikly 的
    // PinchGestureParams 契约(相对手势起点的累计倍数，两指落下即开始)不一致:
    // 1. 存在 minSpan 门槛(约27mm)与手势识别过程，起手一段位移被吞掉，
    //    表现为手指移动很多而缩放很少，跟手感明显弱于iOS
    // 2. 识别态会反复进出，onScaleBegin 多次触发导致基准间距被重复采集，
    //    表现为缩放过程抖动，且倍数容易被推向极值后无法回退
    //
    // 直接计算指间距可保证: 无阈值、起手即响应、倍数严格等于 当前间距/起始间距，
    // 与 iOS UIPinchGestureRecognizer、鸿蒙 ArkUI pinch 的语义一致。

    /** 参与捏合的两个手指id，[INVALID_POINTER_ID]表示未激活 */
    private var pinchPointerId1 = INVALID_POINTER_ID
    private var pinchPointerId2 = INVALID_POINTER_ID

    /** 手势开始时的两指间距(px)，作为累计倍数的分母 */
    private var pinchInitialDistance = 0f

    /** 最近一次的缩放倍数与焦点，用于手势结束时补发end事件 */
    private var lastPinchScale = 1f
    private var lastPinchFocusX = 0f
    private var lastPinchFocusY = 0f

    /** 坐标映射缓冲区(两个点共4个分量)，避免每帧分配 */
    private val pinchPointBuffer = FloatArray(4)

    /**
     * 是否已因pinch而要求父容器不拦截事件。
     *
     * 用于保证「谁设置谁清除」: 手势结束时必须复位，否则外层滚动容器会永久无法滚动。
     */
    private var hasDisallowInterceptForPinch = false

    /**
     * 处理捏合手势。
     * @return 是否消费了事件
     */
    private fun handlePinchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (!listener.isPinchEventHappening && ev.pointerCount >= PINCH_POINTER_COUNT) {
                    return startPinch(ev)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (listener.isPinchEventHappening) {
                    return movePinch(ev)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // 抬起的是参与捏合的手指时结束手势
                if (listener.isPinchEventHappening) {
                    val liftedId = ev.getPointerId(ev.actionIndex)
                    if (liftedId == pinchPointerId1 || liftedId == pinchPointerId2) {
                        endPinch()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (listener.isPinchEventHappening) {
                    endPinch()
                }
                releasePinchInterceptIfNeeded()
            }
        }
        return false
    }

    private fun startPinch(ev: MotionEvent): Boolean {
        val index1 = 0
        val index2 = 1
        val distance = pointerDistance(ev, index1, index2)
        if (distance <= 0f) {
            return false
        }

        // 第一根手指的移动可能已触发pan，此处需补发pan结束事件，
        // 否则业务侧只收到start/move而收不到end，状态会一直停留在拖拽中
        if (listener.isPanEventHappening) {
            listener.onCancel(ev)
        }

        pinchPointerId1 = ev.getPointerId(index1)
        pinchPointerId2 = ev.getPointerId(index2)
        pinchInitialDistance = distance
        lastPinchScale = 1f
        lastPinchFocusX = (ev.getX(index1) + ev.getX(index2)) * 0.5f
        lastPinchFocusY = (ev.getY(index1) + ev.getY(index2)) * 0.5f
        listener.isPinchEventHappening = true

        // 两指落下即表明缩放意图，此时就要求父容器不拦截。
        //
        // 必须在此刻请求: 外层滚动容器在移动超过 touchSlop 时便会拦截并向本View
        // 派发 ACTION_CANCEL，之后收不到任何事件。若等到手势被识别后再请求，
        // 竖向捏合会被列表滚动抢走，横向捏合也会因竖向抖动而时好时坏。
        if (!hasDisallowInterceptForPinch) {
            hasDisallowInterceptForPinch = true
            disallowParentInterceptEvent(true)
        }

        listener.updatePinchRawOffset(ev)
        listener.dispatchPinchEvent(
            KRCSSGestureListener.EVENT_STATE_START,
            lastPinchFocusX,
            lastPinchFocusY,
            1f
        )
        return true
    }

    private fun movePinch(ev: MotionEvent): Boolean {
        val index1 = ev.findPointerIndex(pinchPointerId1)
        val index2 = ev.findPointerIndex(pinchPointerId2)
        if (index1 < 0 || index2 < 0) { // 手指已离开，结束手势
            endPinch()
            return true
        }

        val distance = pointerDistance(ev, index1, index2)
        if (distance <= 0f || pinchInitialDistance <= 0f) {
            return false
        }

        lastPinchScale = distance / pinchInitialDistance
        lastPinchFocusX = (ev.getX(index1) + ev.getX(index2)) * 0.5f
        lastPinchFocusY = (ev.getY(index1) + ev.getY(index2)) * 0.5f

        listener.updatePinchRawOffset(ev)
        listener.dispatchPinchEvent(
            KRCSSGestureListener.EVENT_STATE_MOVE,
            lastPinchFocusX,
            lastPinchFocusY,
            lastPinchScale
        )
        return true
    }

    private fun endPinch() {
        listener.isPinchEventHappening = false
        pinchPointerId1 = INVALID_POINTER_ID
        pinchPointerId2 = INVALID_POINTER_ID
        pinchInitialDistance = 0f
        listener.dispatchPinchEvent(
            KRCSSGestureListener.EVENT_STATE_END,
            lastPinchFocusX,
            lastPinchFocusY,
            lastPinchScale
        )
    }

    /**
     * 复位「父容器不拦截」标志。
     *
     * 仅在所有手指抬起或事件被取消时调用: 若在手势中途(如仅松开一根手指)复位，
     * 外层容器会立刻抢走剩余手指的移动事件。
     */
    private fun releasePinchInterceptIfNeeded() {
        if (hasDisallowInterceptForPinch) {
            hasDisallowInterceptForPinch = false
            disallowParentInterceptEvent(false)
        }
    }

    /**
     * 计算两指间距，在**父坐标系**下测量。
     *
     * 不能直接用 [MotionEvent.getX] 的局部坐标测距: ViewGroup 派发事件给子View时会
     * 施加子View矩阵的逆变换，故局部指间距约等于「物理指间距 / 当前scale」。
     * 若用它计算 scale 会形成自激振荡:
     *   scale变大 → 局部指间距变小 → 算出的scale变小 → scale变小 → 局部指间距变大 → ...
     * 表现为两指静止不动时画面持续抖动，且scale越大抖动越剧烈。
     *
     * 经 [View.getMatrix] 映射到父坐标系后即可消除本View自身transform的影响，
     * 与 iOS UIPinchGestureRecognizer 在窗口坐标系计算scale的做法一致。
     * 当本View未设置transform时矩阵为单位矩阵，映射为空操作，行为不变。
     */
    private fun pointerDistance(ev: MotionEvent, index1: Int, index2: Int): Float {
        val targetView = targetViewWeakRef.get()
            ?: return hypot(ev.getX(index1) - ev.getX(index2), ev.getY(index1) - ev.getY(index2))

        pinchPointBuffer[0] = ev.getX(index1)
        pinchPointBuffer[1] = ev.getY(index1)
        pinchPointBuffer[2] = ev.getX(index2)
        pinchPointBuffer[3] = ev.getY(index2)
        targetView.matrix.mapPoints(pinchPointBuffer)
        return hypot(
            pinchPointBuffer[0] - pinchPointBuffer[2],
            pinchPointBuffer[1] - pinchPointBuffer[3]
        )
    }

    // endregion

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (containPanEvent() && ev.action == MotionEvent.ACTION_DOWN) { // 对齐原来逻辑，有pan事件时, 要求父亲不拦截
            disallowParentInterceptEvent(true)
        }

        var pinchHandle = false
        if (containPinchEvent()) {
            pinchHandle = handlePinchEvent(ev)
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

        return handle || pinchHandle
    }

    private fun disallowParentInterceptEvent(disallow: Boolean) =
        targetViewWeakRef.get()?.parent?.requestDisallowInterceptTouchEvent(disallow)

    private fun containPanEvent(): Boolean = listener.containEvent(KRCSSGestureListener.TYPE_PAN)

    private fun containPinchEvent(): Boolean = listener.containEvent(KRCSSGestureListener.TYPE_PINCH)

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
        } else if (type == KRCSSGestureListener.TYPE_CLICK) {
            listener.addListener(type) {
                targetViewWeakRef.get()?.playSoundEffect(SoundEffectConstants.CLICK)
                callback(it)
            }
        } else {
            listener.addListener(type, callback)
        }
    }

    companion object {
        const val GESTURE_TAG = "hr_gesture_tag"

        /** 触发pinch所需的手指数 */
        private const val PINCH_POINTER_COUNT = 2

        /** 无效的手指id */
        private const val INVALID_POINTER_ID = -1
    }
}
