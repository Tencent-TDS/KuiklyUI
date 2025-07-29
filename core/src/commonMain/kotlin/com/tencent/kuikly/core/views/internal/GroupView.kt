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

package com.tencent.kuikly.core.views.internal

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ImageAttr
import com.tencent.kuikly.core.views.ImageView
import com.tencent.kuikly.core.views.View

private const val BACK_IMAGE_VIEW_Z_INDEX = -999

abstract class GroupView<A : GroupAttr, E : GroupEvent> : ViewContainer<A, E>() {

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun isRenderView(): Boolean {
        return isRenderViewForFlatLayer()
    }

    /**
     * 层级置顶方法
     */
    open fun bringToFront() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("bringToFront")
        }
    }

}

open class GroupAttr : ContainerAttr() {
    internal var backgroundImageView: ImageView? = null
    internal var highlightBackgroundView: GroupView<*, *>? = null
    internal var highlightBackgroundColor: Color? = null
    internal var borderBottomView: GroupView<*, *>? = null
    internal var borderTopView: GroupView<*, *>? = null
    internal var borderLeftView: GroupView<*, *>? = null
    internal var borderRightView: GroupView<*, *>? = null

    /**
     * 暂停屏幕刷新帧事件（提升性能）
     */
    open fun screenFramePause(pause: Boolean) {
        "screenFramePause" with pause.toInt()
    }

    /**
     * 设置下态时高亮背景色
     * 注: 若手指按下区域有存在其他手势（如：单击Click），则不会触发高亮
     */
    fun highlightBackgroundColor(color: Color) {
        if (highlightBackgroundView == null) {
            val groupView = view()
            if (groupView is ViewContainer<*, *>) {
                highlightBackgroundView = DivView()
                val ctx = this
                highlightBackgroundView?.also {
                    groupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePositionAllZero()
                            touchEnable(false)
                            zIndex(BACK_IMAGE_VIEW_Z_INDEX + 1) // 层级仅次于背景IageView
                        }
                    }
                    groupView.insertDomSubView(it, groupView.domChildren().indexOf(it))
                }
                (groupView.getViewEvent() as? GroupEvent)?.apply {
                    addTouchDown(ctx::onTouchDown)

                    addTouchUp(ctx::onTouchUp)
                }


            }
        }
        highlightBackgroundColor = color
    }

    private fun onTouchDown(param: TouchParams) {
        highlightBackgroundView?.getViewAttr()
            ?.backgroundColor(highlightBackgroundColor ?: Color.TRANSPARENT)
    }

    private fun onTouchUp(param: TouchParams) {
        highlightBackgroundView?.getViewAttr()?.backgroundColor(Color.TRANSPARENT)
    }

    /**
     * 设置容器背景图片（默认resize为cover）
     * @param src 图片源(Image组件src能力一致)
     * @param imageAttr 自定义该图片属性
     */
    open fun backgroundImage(src: String, imageAttr: (ImageAttr.() -> Unit)? = null) {
        if (backgroundImageView != null) {
            backgroundImageView?.getViewAttr()?.also {
                it.src(src)
                imageAttr?.invoke(it)
            }
        } else {
            val groupView = view()
            if (groupView is ViewContainer<*, *>) {
                backgroundImageView = ImageView()
                backgroundImageView?.also {
                    groupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePositionAllZero()
                            src(src)
                            zIndex(BACK_IMAGE_VIEW_Z_INDEX) // 层级最低
                            imageAttr?.invoke(this)
                        }
                    }
                    groupView.insertDomSubView(it, groupView.domChildren().indexOf(it))
                }
            }
        }
    }

    open fun borderBottom(border: Border) {
        if (borderBottomView != null) {
            borderBottomView?.domChildren()?.firstOrNull()?.getViewAttr()?.border(border)
        } else {
            val groupView = view()
            if (groupView is ViewContainer<*, *>) {
                borderBottomView = DivView()
                borderBottomView?.also {
                    groupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePosition(
                                bottom = 0f,
                                left = 0f,
                                right = 0f
                            ).height(border.lineWidth).overflow(true).zIndex(999) // 层级最高
                        }
                        View {
                            getViewAttr().apply {
                                absolutePosition(
                                    bottom = 0f,
                                    left = 0f,
                                    right = 0f
                                ).height(border.lineWidth * 3).border(border)
                            }
                        }
                    }
                    groupView.insertDomSubView(it, groupView.domChildren().indexOf(it))
                }
            }
        }
    }

    open fun borderTop(border: Border) {
        if (borderTopView != null) {
            borderTopView?.domChildren()?.firstOrNull()?.getViewAttr()?.border(border)
        } else {
            val groupView = view()
            if (groupView is ViewContainer<*, *>) {
                borderTopView = DivView()
                borderTopView?.also {
                    groupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePosition(
                                top = 0f,
                                left = 0f,
                                right = 0f
                            ).height(border.lineWidth).overflow(true).zIndex(999) // 层级最高

                        }
                        View {
                            getViewAttr().apply {
                                absolutePosition(
                                    top = 0f,
                                    left = 0f,
                                    right = 0f
                                ).height(border.lineWidth * 3).border(border)
                            }
                        }
                    }
                    groupView.insertDomSubView(it, groupView.domChildren().indexOf(it))
                }
            }
        }
    }

    open fun borderLeft(border: Border) {
        if (borderLeftView != null) {
            borderLeftView?.domChildren()?.firstOrNull()?.getViewAttr()?.border(border)
        } else {
            val groupView = view()
            if (groupView is ViewContainer<*, *>) {
                borderLeftView = DivView()
                borderLeftView?.also {
                    groupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePosition(
                                top = 0f,
                                bottom = 0f,
                                left = 0f
                            ).width(border.lineWidth).overflow(true).zIndex(999) // 层级最高
                        }
                        View {
                            getViewAttr().apply {
                                absolutePosition(
                                    top = 0f,
                                    bottom = 0f,
                                    left = 0f
                                ).width(border.lineWidth * 3).border(border)
                            }
                        }
                    }
                    groupView.insertDomSubView(it, groupView.domChildren().indexOf(it))
                }
            }
        }
    }

    open fun borderRight(border: Border) {
        if (borderRightView != null) {
            borderRightView?.domChildren()?.firstOrNull()?.getViewAttr()?.border(border)
        } else {
            val GroupView = view()
            if (GroupView is ViewContainer<*, *>) {
                borderRightView = DivView()
                borderRightView?.also {
                    GroupView.addChild(it) {
                        getViewAttr().apply {
                            absolutePosition(
                                top = 0f,
                                bottom = 0f,
                                right = 0f
                            ).width(border.lineWidth).overflow(true).zIndex(999) // 层级最高
                        }
                        View {
                            getViewAttr().apply {
                                absolutePosition(
                                    top = 0f,
                                    bottom = 0f,
                                    right = 0f
                                ).width(border.lineWidth * 3).border(border)
                            }
                        }
                    }
                    GroupView.insertDomSubView(it, GroupView.domChildren().indexOf(it))
                }
            }
        }
    }

}

typealias TouchEventHandlerFn = (TouchParams) -> Unit

open class GroupEvent : Event() {
    private val touchDownHandlers by lazy(LazyThreadSafetyMode.NONE) {
        arrayListOf<TouchEventHandlerFn>()
    }
    private val touchUpHandlers by lazy(LazyThreadSafetyMode.NONE) {
        arrayListOf<TouchEventHandlerFn>()
    }

    /**
     * 触摸抬起事件兼容模式开关，为true时touchUp和touchCancel都由触摸抬起事件处理器响应。
     */
    private var touchUpCompatibilityMode = true
    private var touchDownHandler: Pair<TouchEventHandlerFn, Boolean>? = null
    private var touchUpHandler: Pair<TouchEventHandlerFn, Boolean>? = null
    private var touchCancelHandler: Pair<TouchEventHandlerFn, Boolean>? = null

    private inline val touchDownIsRegistered
        get() = touchDownHandler != null || !touchDownHandlers.isEmpty()
    private inline val touchDownIsSync
        get() = touchDownHandler?.second ?: false
    private inline val touchUpIsRegistered
        get() = touchUpHandler != null || touchCancelHandler != null || !touchUpHandlers.isEmpty()
    private inline val touchUpIsSync
        get() = touchUpHandler?.second ?: false || touchCancelHandler?.second ?: false

    override fun unRegister(eventName: String) {
        when (eventName) {
            EventName.TOUCH_DOWN.value -> {
                updateTouchDownRegisterWithBlock {
                    touchDownHandler = null
                }
            }

            EventName.TOUCH_UP.value -> {
                updateTouchUpRegisterWithBlock {
                    touchUpHandler = null
                }
            }

            EventName.TOUCH_CANCEL.value -> {
                updateTouchUpRegisterWithBlock {
                    touchCancelHandler = null
                }
            }

            else -> {
                super.unRegister(eventName)
            }
        }
    }

    /**
     * 设置触摸按下事件处理器。
     * @param handler 一个 TouchEventHandlerFn，当触摸按下事件发生时调用。
     */
    open fun touchDown(isSync: Boolean = false, handler: TouchEventHandlerFn) {
        updateTouchDownRegisterWithBlock {
            touchDownHandler = handler to isSync
        }
    }

    fun addTouchDown(handler: TouchEventHandlerFn) {
        if (handler in touchDownHandlers) {
            return
        }
        updateTouchDownRegisterWithBlock {
            touchUpHandlers.add(handler)
        }
    }

    fun removeTouchDown(handler: TouchEventHandlerFn) {
        updateTouchDownRegisterWithBlock {
            touchDownHandlers.remove(handler)
        }
    }

    private fun updateTouchDownRegisterWithBlock(block: () -> Unit) {
        val regBefore = touchDownIsRegistered
        val syncBefore = touchDownIsSync
        block()
        val regAfter = touchDownIsRegistered
        val syncAfter = touchDownIsSync
        if (regBefore == regAfter && syncBefore == syncAfter) {
            return
        }
        if (regAfter) {
            register(EventName.TOUCH_DOWN.value, {
                val touchParams = TouchParams.decode(it)
                touchDownHandlers.forEach { handler -> handler(touchParams) }
                touchDownHandler?.also { (handler, _) -> handler(touchParams) }
            }, isSync = syncAfter)
        } else {
            super.unRegister(EventName.TOUCH_DOWN.value)
        }
    }

    /**
     * 设置触摸抬起事件处理器。
     * @param handler 一个 TouchEventHandlerFn，当触摸抬起事件发生时调用。
     */
    open fun touchUp(isSync: Boolean = false, handler: TouchEventHandlerFn) {
        updateTouchUpRegisterWithBlock {
            touchUpHandler = handler to isSync
        }
    }

    fun addTouchUp(handler: TouchEventHandlerFn) {
        if (handler in touchUpHandlers) {
            return
        }
        updateTouchUpRegisterWithBlock {
            touchUpHandlers.add(handler)
        }
    }

    fun removeTouchUp(handler: TouchEventHandlerFn) {
        updateTouchUpRegisterWithBlock {
            touchUpHandlers.remove(handler)
        }
    }

    private fun updateTouchUpRegisterWithBlock(block: () -> Unit) {
        val regBefore = touchUpIsRegistered
        val syncBefore = touchUpIsSync
        block()
        val regAfter = touchUpIsRegistered
        val syncAfter = touchUpIsSync
        if (regBefore == regAfter && syncBefore == syncAfter) {
            return
        }
        if (regAfter) {
            register(EventName.TOUCH_UP.value, {
                val touchParams = TouchParams.decode(it)
                touchUpHandlers.forEach { handler -> handler.invoke(touchParams) }
                if (touchUpCompatibilityMode || touchParams.action != EventName.TOUCH_CANCEL.value) {
                    touchUpHandler?.also { (handler, _) -> handler(touchParams) }
                } else {
                    touchCancelHandler?.also { (handler, _) -> handler(touchParams) }
                }
            }, isSync = syncAfter)
        } else {
            super.unRegister(EventName.TOUCH_UP.value)
        }
    }

    open fun touchCancel(isSync: Boolean = false, handler: TouchEventHandlerFn) {
        touchUpCompatibilityMode = false
        updateTouchUpRegisterWithBlock {
            touchCancelHandler = handler to isSync
        }
    }

    /**
     * 设置触摸移动事件处理器。
     * @param handler 一个 TouchEventHandlerFn，当触摸移动事件发生时调用。
     */
    open fun touchMove(isSync: Boolean = false, handler: TouchEventHandlerFn) {
        register(EventName.TOUCH_MOVE.value, {
            handler(TouchParams.decode(it))
        }, isSync)
    }

    /**
     * 屏幕刷新帧回调事件（VSYNC信号）。
     * @param handlerFn 一个无参函数，当屏幕刷新帧回调事件发生时调用。
     * 注意：
     * 1. 该回调内不建议执行消耗性能逻辑，因为该回调在平台 UI 线程同步执行。
     * 2. 在不使用该事件时，通过 attr.screenFramePause 属性设置为 true 暂停 Vsync 信号监听回调，提升性能。
     */
    open fun screenFrame(handlerFn: () -> Unit) {
        this.register(EventName.SCREEN_FRAME.value, {
            handlerFn()
        }, true)
    }
}

