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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.reactive.handler.observable

/*
*  UISwitch开关组件，风格类iOS UISwitch开关，尺寸默认(51f, 31f), 支持手动设置指定size(xx, xx)
*/
class SwitchView : ComposeView<SwitchAttr, SwitchEvent>() {
    private var isOn by observable(false)
    private val width : Float
        get() = flexNode.styleWidth
    private val height : Float
        get() = flexNode.styleHeight

    override fun attr(init: SwitchAttr.() -> Unit) {
        if (flexNode.styleHeight.valueEquals(Float.undefined)) {
            attr.height(31f) // 默认尺寸
        }
        if (flexNode.styleWidth.valueEquals(Float.undefined)) {
            attr.width(51f) // 默认尺寸
        }
        super.attr(init)
    }

    override fun body(): ViewBuilder {
        val margin = this.attr.thumbMargin
        val ctx = this
        return {
            attr {
                overflow(false)
                flexDirectionRow()
                alignItemsCenter()
                justifyContentFlexStart()
            }
            // 独立一个绝对布局的背景色
            View {
                attr {
                    absolutePositionAllZero()
                    borderRadius(ctx.height / 2f)
                    if (ctx.attr.isOn) {
                        backgroundColor(ctx.attr.onColor)
                    } else {
                        backgroundColor(ctx.attr.unOnColor)
                    }
                    animation(Animation.linear(0.2f), ctx.attr.isOn)

                }
            }

            event {
                click {
                    ctx.attr.isOn = !ctx.attr.isOn
                    ctx.event.switchOnChangedHandlerFn?.invoke(ctx.attr.isOn)
                }
            }
            // 圆圈
            View {
                attr {
                    val thumbSize = (ctx.height - 2f * margin)
                    borderRadius(thumbSize / 2f)
                    backgroundColor(ctx.attr.thumbColor)
                    size(thumbSize , thumbSize )
                    margin(margin)
                    if (ctx.attr.isOn) {
                        val percentageX = (ctx.width - 2f * margin - thumbSize) / thumbSize
                        transform(Translate(percentageX, 0f))
                    } else {
                        transform(Translate(0f, 0f))
                    }
                    animation(Animation.linear(0.2f), ctx.attr.isOn)
                }
            }

        }
    }

    //
    override fun createAttr(): SwitchAttr = SwitchAttr()

    override fun createEvent(): SwitchEvent = SwitchEvent()

}

class SwitchAttr : ComposeAttr() {
    // 圆型滑块颜色
    internal var thumbColor  = Color.WHITE
    // onColor
    internal var onColor = Color.GREEN
    // unOnColor
    internal var unOnColor = Color.GRAY
    // 开关
    internal var isOn by observable(false)
    internal var thumbMargin = 2f // 圆块与开关的边距，默认为2f

    // 设置开关
    fun isOn(on : Boolean) {
        isOn = on
    }
    // 设置开关 打开时 的高亮色（背景色）
    fun onColor(color : Color) {
        onColor = color
    }
    // 设置开关 关闭时 的背景色
    fun unOnColor(color : Color) {
        unOnColor = color
    }
    // 设置圆型滑块颜色（关闭和开启同一个颜色）
    fun thumbColor(color : Color) {
        thumbColor = color
    }
    // 圆块与开关的贴边边距，默认为2f
    fun thumbMargin(margin: Float) {
        thumbMargin = margin
    }
}

class SwitchEvent : ComposeEvent() {
    internal var switchOnChangedHandlerFn : ((on : Boolean) -> Unit)? = null
    // 开关变化时回调
    fun switchOnChanged(handlerFn: (on : Boolean) -> Unit) {
        switchOnChangedHandlerFn = handlerFn
    }

}
/*
*  UISwitch开关，风格类iOS UISwitch开关，尺寸默认(51f, 31f), 支持手动设置指定size(xx, xx)
*/
fun ViewContainer<*, *>.Switch(init: SwitchView.() -> Unit) {
    addChild(SwitchView(), init)
}