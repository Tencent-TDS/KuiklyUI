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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.PanGestureParams
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.reactive.handler.observable
import kotlin.math.max
import kotlin.math.min

/*
 * @brief Slider 是一个滑动器组件
 * 注：该组件需要指定设置size。
 */
class SliderView : ComposeView<SliderAttr, SliderEvent>() {
    override fun createAttr(): SliderAttr = SliderAttr()
    override fun createEvent(): SliderEvent = SliderEvent()

    override fun didInit() {
        if (flexNode.styleWidth.valueEquals(Float.undefined) || flexNode.styleHeight.valueEquals(Float.undefined)) {
            throwRuntimeError("Slider组件需要设置宽度和高度（attr { size(xxx, xxx)}）")
        }
        super.didInit()
        val ctx = this
        var isFirst = true
        bindValueChange({attr.currentProgress}) {// 监听进度变化
            // 分发进度变化响应事件
            if (!isFirst) {
                ctx.event.progressDidChangedHandlerFn?.invoke(ctx.attr.currentProgress)
            }
        }
        isFirst = false
    }

    /**
     * 获取当前滑块进度
     * @return 百分比进度
     */
    fun getCurrentProgress(): Float {
        return getViewAttr().currentProgress
    }

    override fun attr(init: SliderAttr.() -> Unit) {
        super.attr(init)
        val isRow = attr.directionHorizontal
        val ctx = this
        if (attr.trackViewCreator == null) {
            attr.trackViewCreator {
                View {
                    attr {
                        if (isRow) {
                            height(ctx.attr.trackThickness)
                        } else {
                            width(ctx.attr.trackThickness)
                        }
                        backgroundColor(ctx.attr.trackColor)
                        borderRadius(ctx.attr.trackThickness / 2f)
                    }
                }
            }
        }
        if (attr.progressViewCreator == null) {
            attr.progressViewCreator {
                View {
                    attr {
                        if (isRow) {
                            height(ctx.attr.trackThickness)
                        } else {
                            width(ctx.attr.trackThickness)
                        }
                        backgroundColor(ctx.attr.progressColor)
                        borderRadius(ctx.attr.trackThickness / 2f)
                    }
                }
            }
        }
        if (attr.thumbViewCreator == null) {
            attr.thumbViewCreator {
                View {
                    attr {
                        size(ctx.attr.thumbSize.width, ctx.attr.thumbSize.height)
                        borderRadius(ctx.attr.thumbSize.height / 2f)
                        backgroundColor(ctx.attr.thumbColor)
                    }
                }
            }
        }

    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                if (ctx.attr.directionHorizontal) {
                    flexDirectionRow()
                } else {
                    flexDirectionColumn()
                }
            }
            event {
                pan {
                    ctx.handlePanGesture(it)
                }
            }
            // 背景轨道
            View {
                attr {
                    absolutePosition(
                        left = ctx.attr.paddingLeft,
                        top = ctx.attr.paddingTop,
                        right = ctx.attr.paddingRight,
                        bottom = ctx.attr.paddingBottom
                    )
                    if (ctx.attr.directionHorizontal) {
                       flexDirectionColumn()
                    } else {
                        flexDirectionRow()
                    }
                    justifyContentCenter()
                }
                ctx.attr.trackViewCreator?.invoke(this)
            }
            // progress
            View {
                attr {
                    justifyContentCenter()
                    if (ctx.attr.directionHorizontal) {
                        flexDirectionColumn()
                        absolutePosition(
                            left = ctx.attr.paddingLeft,
                            top = ctx.attr.paddingTop,
                            bottom = ctx.attr.paddingBottom
                        )
                        width(ctx.attr.contentWidth * ctx.attr.currentProgress)
                    } else {
                        flexDirectionRow()
                        absolutePosition(
                            left = ctx.attr.paddingLeft,
                            top = ctx.attr.paddingTop,
                            right = ctx.attr.paddingRight
                        )
                        height(ctx.attr.contentHeight * ctx.attr.currentProgress)
                    }
                }
                ctx.attr.progressViewCreator?.invoke(this)
            }
            // 滑块
            View {
                attr {
                    allCenter()
                    if (ctx.attr.directionHorizontal) {
                        transform(Translate(-0.5f, 0f))
                        marginLeft(ctx.attr.paddingLeft + ctx.attr.contentWidth * ctx.attr.currentProgress)
                    } else {
                        transform(Translate(0f, -0.5f))
                        marginTop(ctx.attr.paddingTop + ctx.attr.contentHeight * ctx.attr.currentProgress)
                    }

                }
                ctx.attr.thumbViewCreator?.invoke(this)
            }

        }
    }
    private fun handlePanGesture(params: PanGestureParams) {
        var progress = if (attr.directionHorizontal) {
            (params.x - attr.paddingLeft) / (flexNode.styleWidth - attr.paddingLeft - attr.paddingRight)
        } else {
            (params.y - attr.paddingTop) / (flexNode.styleHeight - attr.paddingTop - attr.paddingBottom)
        }
        progress = max(0f, min(progress, 1f))
        attr.currentProgress = progress

        if (params.state == "start") {
            event.beginDragSliderHandlerFn?.invoke(params)
        }
        if (params.state == "end") {
            event.endDragSliderHandlerFn?.invoke(params)
        }
    }
}

class SliderAttr : ComposeAttr() {
    internal var progressViewCreator : ViewBuilder? = null
    internal var trackViewCreator : ViewBuilder? = null
    internal var thumbViewCreator : ViewBuilder? = null
    internal var progressColor by observable(Color.BLUE)
    internal var trackColor by observable(Color.GRAY)
    internal var thumbColor by observable(Color.WHITE)
    internal var trackThickness by observable(5f)
    internal var thumbSize by observable(Size(10f, 10f))
    internal var currentProgress by observable(0f)
    internal var sliderWidth by observable(0f)
    internal var sliderHeight by observable(0f)
    internal var paddingLeft by observable(0f)
    internal var paddingTop by observable(0f)
    internal var paddingRight by observable(0f)
    internal var paddingBottom by observable(0f)
    internal val contentWidth
        get() = sliderWidth - paddingLeft - paddingRight
    internal val contentHeight
        get() = sliderHeight - paddingTop - paddingBottom

    internal var directionHorizontal = true

    override fun width(width: Float): Attr{
        sliderWidth = width
        return super.width(width)
    }

    override fun height(height: Float): Attr{
        sliderHeight = height
        return super.height(height)
    }

    override fun padding(top: Float, left: Float, bottom: Float, right: Float): ContainerAttr {
        paddingLeft = if (Float.undefined.valueEquals(left)) 0f else left
        paddingTop = if (Float.undefined.valueEquals(top)) 0f else top
        paddingRight = if (Float.undefined.valueEquals(right)) 0f else right
        paddingBottom = if (Float.undefined.valueEquals(bottom)) 0f else bottom
        return this
    }

    // 设置当前进度百分比, 取值范围[0, 1]
    fun currentProgress(progress01 : Float) {
        currentProgress = progress01
    }
    // 当前进度view自定义创建（可选，有默认实现）
    fun progressViewCreator(creator: ViewBuilder) {
        progressViewCreator = creator
    }
    // 背景轨道view自定义创建（可选，有默认实现）
    fun trackViewCreator(creator: ViewBuilder) {
        trackViewCreator = creator
    }
    // 滑块view自定义创建（可选，有默认实现）
    fun thumbViewCreator(creator: ViewBuilder) {
        thumbViewCreator = creator
    }
    // 当前进度颜色
    fun progressColor(color : Color) {
        progressColor = color
    }
    // 背景轨道颜色
    fun trackColor(color : Color) {
        trackColor = color
    }
    // 滑块颜色
    fun thumbColor(color: Color) {
        thumbColor = color
    }
    // 轨道厚度
    fun trackThickness(thickness : Float) {
        trackThickness = thickness
    }
    // 滑块大小（高度和宽度，尺寸）
    fun thumbSize(size : Size) {
        thumbSize = size
    }
    // 滑动方向（横向还是纵向, 默认横向）
    fun sliderDirection(horizontal : Boolean) {
        directionHorizontal = horizontal
    }
}

class SliderEvent : ComposeEvent() {
    internal var progressDidChangedHandlerFn : ((progress : Float) -> Unit)? = null
    internal var beginDragSliderHandlerFn : ((params : PanGestureParams) -> Unit)? = null
    internal var endDragSliderHandlerFn : ((params : PanGestureParams) -> Unit)? = null
    // 进度变化时回调
    fun progressDidChanged(handlerFn: (progress : Float) -> Unit) {
        progressDidChangedHandlerFn = handlerFn
    }
    // 开始拖拽滑动器
    fun beginDragSlider(handlerFn : (params : PanGestureParams) -> Unit) {
        beginDragSliderHandlerFn = handlerFn
    }
    // 结束拖拽滑动器
    fun endDragSlider(handlerFn : (params : PanGestureParams) -> Unit) {
        endDragSliderHandlerFn = handlerFn
    }

}
/*
 * @brief Slider 是一个滑动器组件
 *  注：该组件需要指定设置size。
 */
fun ViewContainer<*, *>.Slider(init: SliderView.() -> Unit) {
    addChild(SliderView(), init)
}