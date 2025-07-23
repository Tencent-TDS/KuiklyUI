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

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.velseif
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.reactive.handler.observable

/*
 * @brief 复选框组件，可用作单击选中
 * 注：CheckBox组件需要设置宽度和高度（attr { size(xxx, xxx)}
 */
class CheckBoxView : ComposeView<CheckBoxAttr, CheckBoxEvent>() {

    override fun attr(init: CheckBoxAttr.() -> Unit) {
        super.attr(init)
        val ctx = this
        if (attr.defaultViewCreator == null) {
            attr.defaultViewCreator {
                Image {
                    attr {
                        flex(1f)
                        resizeContain()
                        src(ctx.attr.defaultImageSrc)
                    }
                }
            }
        }
        if (attr.checkedViewCreator == null) {
            attr.checkedViewCreator {
                Image {
                    attr {
                        flex(1f)
                        resizeContain()
                        src(ctx.attr.checkedImageSrc)
                    }
                }
            }
        }
        if (attr.disableViewCreator == null && ctx.attr.disableImageSrc.isNotEmpty()) {
            attr.disableViewCreator {
                Image {
                    attr {
                        flex(1f)
                        resizeContain()
                        src(ctx.attr.disableImageSrc)
                    }
                }
            }
        }
    }

    override fun didInit() {
        if (flexNode.styleWidth.valueEquals(Float.undefined) || flexNode.styleHeight.valueEquals(Float.undefined)) {
            throwRuntimeError("CheckBox组件需要设置宽度和高度（attr { size(xxx, xxx)}）")
        }
        super.didInit()
        val ctx = this
        // 监听checked变量值变化
        var isFirst = true
        bindValueChange({attr.checked}) {
            if (!isFirst) {
                ctx.event.checkedDidChangedHandlerFn?.invoke(ctx.attr.checked) // 分发变化事件
            }
        }
        isFirst = false
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {

            }
            event {
                click {
                    ctx.attr.checked = !ctx.attr.checked
                }
            }
            vif({ctx.attr.disable && ctx.attr.disableImageSrc.isNotEmpty()}) {
                 ctx.attr.disableViewCreator?.invoke(this)
            }
            velseif({ctx.attr.checked}) {
                ctx.attr.checkedViewCreator?.invoke(this)
            }
            velse {
                ctx.attr.defaultViewCreator?.invoke(this)
            }
        }
    }

    override fun createAttr(): CheckBoxAttr = CheckBoxAttr()

    override fun createEvent(): CheckBoxEvent = CheckBoxEvent()
}

class CheckBoxAttr : ComposeAttr() {
    internal var checked by observable(false)
    internal var disable by observable(false)
    internal var checkedImageSrc by observable("")
    internal var defaultImageSrc by observable("")
    internal var disableImageSrc by observable("")
    internal var checkedViewCreator: ViewBuilder? = null
    internal var defaultViewCreator: ViewBuilder? = null
    internal var disableViewCreator: ViewBuilder? = null

    /**
     * 设置 CheckBox 是否选中。
     * @param value 选中状态，true 为选中，false 为未选中。
     */
    fun checked(value: Boolean) {
        checked = value
    }

    /**
     * 设置 CheckBox 是否禁用。
     * @param value 禁用状态，true 为禁用，false 为启用。
     */
    fun disable(value: Boolean) {
        disable = value
    }

    /**
     * 设置选中状态的图片资源。
     * @param imageSrc 选中状态的图片资源路径。
     */
    fun checkedImageSrc(imageSrc: String) {
        checkedImageSrc = imageSrc
    }

    /**
     * 设置默认状态的图片资源。
     * @param imageSrc 默认状态的图片资源路径。
     */
    fun defaultImageSrc(imageSrc: String) {
        defaultImageSrc = imageSrc
    }

    /**
     * 设置禁用状态的图片资源。
     * @param imageSrc 禁用状态的图片资源路径。
     */
    fun disableImageSrc(imageSrc: String) {
        disableImageSrc = imageSrc
    }

    /**
     * 设置选中状态的视图创建器。
     * @param creator 选中状态的视图创建器。
     */
    fun checkedViewCreator(creator: ViewBuilder) {
        checkedViewCreator = creator
    }

    /**
     * 设置默认状态的视图创建器。
     * @param creator 默认状态的视图创建器。
     */
    fun defaultViewCreator(creator: ViewBuilder) {
        defaultViewCreator = creator
    }

    /**
     * 设置禁用状态的视图创建器。
     * @param creator 禁用状态的视图创建器。
     */
    fun disableViewCreator(creator: ViewBuilder) {
        disableViewCreator = creator
    }
}

class CheckBoxEvent : ComposeEvent() {
    internal var checkedDidChangedHandlerFn : ((checked : Boolean) -> Unit)? = null
     // 单选框选中状态变化回调
     fun checkedDidChanged(handlerFn: (checked : Boolean) -> Unit) {
         checkedDidChangedHandlerFn = handlerFn
     }
}

/*
 * @brief 复选框组件，可用作单击选中态/非选中态的切换展示
 *  注：CheckBox组件需要设置宽度和高度（attr { size(xxx, xxx)}
 */
fun ViewContainer<*, *>.CheckBox(init: CheckBoxView.() -> Unit) {
    addChild(CheckBoxView(), init)
}
