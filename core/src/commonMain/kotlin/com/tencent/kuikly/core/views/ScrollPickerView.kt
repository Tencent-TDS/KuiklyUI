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
import com.tencent.kuikly.core.base.event.didAppear
import com.tencent.kuikly.core.timer.setTimeout
import kotlin.math.max
import kotlin.math.min

internal typealias ScrollPickerDragEndEvent = (centerValue: String, centerItemIndex: Int) -> Unit

class ScrollPickerView(
    private val itemList : Array<String>,
    private val defaultIndex: Int? = null
): ComposeView<ScrollPickerAttr, ScrollPickerEvent>() {

    override fun createAttr() = ScrollPickerAttr()

    override fun createEvent() = ScrollPickerEvent()

    private fun scrollOffset(params: ScrollParams, dataListSize: Int): Float {
        val ctx = this@ScrollPickerView
        var temp = params.offsetY
        if (temp.toInt() % ctx.attr.itemHeight > ctx.attr.itemHeight / 2) {
            temp += ctx.attr.itemHeight
        }
        val offsetValue =
            temp - params.offsetY.toInt() % ctx.attr.itemHeight
        val finOffSet = min(
            max(0f, offsetValue),
            (dataListSize) * ctx.attr.itemHeight - ctx.attr.countPerScreen * ctx.attr.itemHeight
        )
        return finOffSet
    }

    override fun body(): ViewBuilder {
        val ctx  = this@ScrollPickerView
        val itemHeight = ctx.attr.itemHeight
        val itemWidth = ctx.attr.itemWidth
        val offset = ctx.attr.countPerScreen / 2
        return {
            Scroller {
                val dataList = arrayListOf<String>()
                val placeHolderArray = Array<String>(offset) {""}
                dataList.addAll(placeHolderArray)
                dataList.addAll(ctx.itemList)
                dataList.addAll(placeHolderArray)
                val scroller = this@Scroller
                var targetIndex = 0
                attr {
                    showScrollerIndicator(false)
                    width(itemWidth)
                    height(ctx.attr.countPerScreen * itemHeight)
                    flexDirectionColumn()
                    allCenter()
                    bouncesEnable(true)
                }
                event {
                    didAppear {
                        ctx.event.dragEndEvent(dataList[offset], 0)
                        if (ctx.defaultIndex != null && ctx.defaultIndex > 0 && ctx.defaultIndex < ctx.itemList.size) {
                            setTimeout(200) {
                                scroller.setContentOffset(0f, ctx.attr.itemHeight * ctx.defaultIndex, true, SpringAnimation(200,1.0f, 1f))
                                ctx.event.dragEndEvent(dataList[ctx.defaultIndex + offset], ctx.defaultIndex)
                            }
                        }
                    }
                    click { params ->
                        val temp = params.y - 2 * itemHeight
                        val offsetValue =
                            temp - params.y.toInt() % itemHeight
                        val finOffSet = min(
                            max(0f, offsetValue),
                            (dataList.size) * itemHeight - ctx.attr.countPerScreen * itemHeight
                        )
                        scroller.setContentOffset(0f, finOffSet, true)
                        val centerIndex = (finOffSet / itemHeight).toInt()
                        ctx.event.dragEndEvent(dataList[centerIndex + offset], centerIndex)
                    }

                    willDragEndBySync {
                        val params = ScrollParams(offsetX = it.offsetX, offsetY = it.offsetY, contentHeight = it.contentHeight, contentWidth = it.contentWidth, viewHeight = it.viewHeight, viewWidth = it.viewWidth, isDragging = it.isDragging)
                        val finOffSet = ctx.scrollOffset(params, dataList.size)
                        scroller.setContentOffset(0f, finOffSet, true, SpringAnimation(200,1.0f, it.velocityY))
                        targetIndex =
                            (finOffSet / ctx.attr.itemHeight).toInt()
                    }
                    dragEnd {
                        val finOffSet = ctx.scrollOffset(it, dataList.size)
                        scroller.setContentOffset(0f, finOffSet, true, SpringAnimation(200,1.0f,1f))
                        targetIndex =
                            (finOffSet / ctx.attr.itemHeight).toInt()
                        ctx.event.dragEndEvent(dataList[targetIndex + offset], targetIndex)
                    }
                }
                dataList.forEach {
                    View {
                        attr {
                            size(ctx.attr.itemWidth, ctx.attr.itemHeight)
                            allCenter()
                            backgroundColor(ctx.attr.itemBackGroundColor)
                        }
                        Text {
                            attr {
                                text(it)
                                fontSize(17f)
                                color(ctx.attr.itemTextColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

class ScrollPickerAttr: ComposeAttr() {
    // 单个item选项的高度
    var itemWidth: Float = 0f

    // 单个item选项的高度
    var itemHeight: Float = 0f

    // 每屏item的个数
    var countPerScreen: Int = 0

    //每个item的背景色
    var itemBackGroundColor: Color = Color.TRANSPARENT

    //每个item的文字色
    var itemTextColor: Color = Color.BLACK
}

class ScrollPickerEvent: ComposeEvent() {
    lateinit var dragEndEvent : ScrollPickerDragEndEvent
    // 停止滚动后选中item，回调中间的value和index
    fun dragEndEvent(event: ScrollPickerDragEndEvent) {
        dragEndEvent = event
    }
}
fun ViewContainer<*, *>.ScrollPicker(itemList : Array<String>, defaultIndex: Int? = null, init: ScrollPickerView.() -> Unit) {
    addChild(ScrollPickerView(itemList, defaultIndex), init)
}

/*
 * 滚动选择器，用组合用作日期或者地区选择器
 */