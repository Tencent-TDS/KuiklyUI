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

package com.tencent.kuikly.core.views.compose

import com.tencent.kuikly.core.base.Anchor
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.base.event.TouchParams
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.ImageAttr
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.internal.TouchEventHandlerFn

class ButtonView : ComposeView<ButtonAttr, ButtonEvent>() {
    private var highlightViewBgColor by observable(Color.TRANSPARENT)

    override fun createAttr(): ButtonAttr {
        return ButtonAttr()
    }

    override fun createEvent(): ButtonEvent {
        return ButtonEvent()
    }

    override fun emit(eventName: String, param: Any?) {
        super.emit(eventName, param)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                justifyContentCenter()
                alignItemsCenter()
            }
            ctx.attr.highlightBackgroundColor?.also {
                vif({ctx.highlightViewBgColor != Color.TRANSPARENT}) {
                    View { // 高亮背景view
                        attr {
                            absolutePositionAllZero()
                            backgroundColor(ctx.highlightViewBgColor)
                        }
                    }
                }
            }
            if (ctx.attr.enableForeground) {
                vif({ ctx.attr.foregroundPercent != 0f }) {
                    View {
                        attr {
                            ctx.attr.foregroundColor?.also { foregroundColor ->
                                backgroundColor(foregroundColor)
                            }
                            absolutePositionAllZero()
                            transform(scale = Scale(ctx.attr.foregroundPercent, 1f), anchor = Anchor(0f, 0f))
                        }
                    }
                }
            }

            // 图片
            ctx.attr.imageAttrInit?.also { imageAttr ->
                Image {
                    attr (imageAttr)
                }
            }
            // 文本
            ctx.attr.titleAttrInit?.also { textAttr ->
                Text {
                    if (ctx.attr.imageAttrInit != null) {
                        getViewAttr().marginLeft(5f) // 默认间距
                    }
                    attr (textAttr)
                }
            }
        }
    }

    override fun attr(init: ButtonAttr.() -> Unit) {
        super.attr(init)
        attr.highlightBackgroundColor?.also { color ->
            event {
                touchDown {
                    this@ButtonView.highlightViewBgColor = color
                }
                touchUp {
                    this@ButtonView.highlightViewBgColor = Color.TRANSPARENT
                }
            }
        }
    }
}

class ButtonAttr : ComposeAttr() {

    internal var titleAttrInit: (TextAttr.()->Unit)? = null
    internal var imageAttrInit: (ImageAttr.()->Unit)? = null
    private var setFlexDirection = false
    internal var highlightBackgroundColor : Color? = null

    fun titleAttr(init:TextAttr.()->Unit) {
        titleAttrInit = init
    }
    fun imageAttr(init: ImageAttr.() -> Unit) {
        imageAttrInit = init
        if (!setFlexDirection) {
            super.flexDirection(FlexDirection.ROW) // 默认横向
        }
    }

    // 设置按钮按下态时高亮背景色
    fun highlightBackgroundColor(color: Color) {
        highlightBackgroundColor = color
    }

    override fun flexDirection(flexDirection: FlexDirection): ContainerAttr {
        setFlexDirection = true
        return super.flexDirection(flexDirection)
    }

    var enableForeground = false
    var foregroundColor: Color? = null
    var foregroundPercent by observable(0f)

}

class ButtonEvent : ComposeEvent() {
    private val touchDownHandlers = arrayListOf<TouchEventHandlerFn>()
    private val touchUpHandlers = arrayListOf<TouchEventHandlerFn>()
    fun touchDown(handler: TouchEventHandlerFn) {
        if (touchDownHandlers.isEmpty()) {
            register(EventName.TOUCH_DOWN.value) {
                val touchParams = TouchParams.decode(it)
                touchDownHandlers.forEach { handler ->
                    handler.invoke(touchParams)
                }
            }
        }
        touchDownHandlers.add(handler)
    }
    fun touchUp(handler: TouchEventHandlerFn) {
        if (touchUpHandlers.isEmpty()) {
            register(EventName.TOUCH_UP.value) {
                val touchParams = TouchParams.decode(it)
                touchUpHandlers.forEach { handler ->
                    handler.invoke(touchParams)
                }
            }
        }
        touchUpHandlers.add(handler)
    }

    fun touchMove(handler: TouchEventHandlerFn) {
        register(EventName.TOUCH_MOVE.value) {
            handler(TouchParams.decode(it))
        }
    }
}

fun ViewContainer<*, *>.Button(init: ButtonView.() -> Unit) {
    addChild(ButtonView(), init)
}