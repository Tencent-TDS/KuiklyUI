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
import com.tencent.kuikly.core.collection.fastArrayListOf
import kotlin.math.max
import kotlin.math.min

/**
 * Callback when scroll ends, returns center item's value and index
 */
internal typealias ScrollPickerScrollEndEvent = (centerValue: String, centerItemIndex: Int) -> Unit

/**
 * Callback during scrolling, returns center item's value and index
 */
internal typealias ScrollPickerScrollEvent = (centerValue: String, centerItemIndex: Int) -> Unit

/**
 * Callback when drag ends, returns center item's value and index
 */
internal typealias ScrollPickerDragEndEvent = (centerValue: String, centerItemIndex: Int) -> Unit

class ScrollPickerView(
    private val itemList : Array<String>,
    private val defaultIndex: Int? = null
): ComposeView<ScrollPickerAttr, ScrollPickerEvent>() {

    /**
     * Last callback index during scrolling, used to avoid duplicate callbacks
     */
    private var lastScrollIndex = -1
    
    /**
     * Internal Scroller reference, used to set initial scroll position in viewDidLayout
     */
    private var scrollerRef: ScrollerView<*, *>? = null
    
    /**
     * Data list (including placeholders), used for callbacks
     */
    private var dataListRef: MutableList<String>? = null
    
    /**
     * Flag indicating whether initial position has been set
     */
    private var hasSetInitialOffset = false

    override fun createAttr() = ScrollPickerAttr()

    override fun createEvent() = ScrollPickerEvent()
    
    override fun viewDidLayout() {
        super.viewDidLayout()
        // Set initial scroll position after layout completes
        if (!hasSetInitialOffset && defaultIndex != null && defaultIndex >= 0 && defaultIndex < itemList.size) {
            hasSetInitialOffset = true
            val offset = attr.countPerScreen / 2
            if (attr.initialScrollAnimated) {
                scrollerRef?.setContentOffset(0f, attr.itemHeight * defaultIndex, true, SpringAnimation(200, 1.0f, 1f))
            } else {
                scrollerRef?.setContentOffset(0f, attr.itemHeight * defaultIndex, false)
            }
            dataListRef?.let { dataList ->
                (event.scrollEndEvent ?: event.dragEndEvent)?.invoke(dataList[defaultIndex + offset], defaultIndex)
            }
        }
    }

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
                val dataList = fastArrayListOf<String>()
                val placeHolderArray = Array<String>(offset) {""}
                dataList.addAll(placeHolderArray)
                dataList.addAll(ctx.itemList)
                dataList.addAll(placeHolderArray)
                val scroller = this@Scroller
                // Save references for setting initial position in viewDidLayout
                ctx.scrollerRef = scroller
                ctx.dataListRef = dataList
                var targetIndex = 0
                var isSnapping = false
                attr {
                    showScrollerIndicator(false)
                    width(itemWidth)
                    height(ctx.attr.countPerScreen * itemHeight)
                    flexDirectionColumn()
                    allCenter()
                    bouncesEnable(true)
                }
                event {
                    click { params ->
                        val temp = params.y - 2 * itemHeight
                        val offsetValue =
                            temp - params.y.toInt() % itemHeight
                        val finOffSet = min(
                            max(0f, offsetValue),
                            (dataList.size) * itemHeight - ctx.attr.countPerScreen * itemHeight
                        )
                        isSnapping = true
                        scroller.setContentOffset(0f, finOffSet, true)
                        val centerIndex = (finOffSet / itemHeight).toInt()
                        (ctx.event.scrollEndEvent ?: ctx.event.dragEndEvent)?.invoke(dataList[centerIndex + offset], centerIndex)
                    }

                    scroll { params ->
                        // Calculate the center item index based on current scroll position
                        val currentIndex = (params.offsetY / ctx.attr.itemHeight).toInt()

                        // Ensure index is within valid range
                        if (currentIndex >= 0 && currentIndex < ctx.itemList.size) {
                            // Only trigger callback when index changes to avoid duplicate triggers
                            if (currentIndex != ctx.lastScrollIndex) {
                                ctx.lastScrollIndex = currentIndex
                                // Trigger scroll callback
                                ctx.event.scrollEvent?.invoke(dataList[currentIndex + offset], currentIndex)
                            }
                        }
                    }
                    willDragEndBySync {
                        ctx.event.dragEndEvent?.run {
                            val params = ScrollParams(offsetX = it.offsetX, offsetY = it.offsetY, contentHeight = it.contentHeight, contentWidth = it.contentWidth, viewHeight = it.viewHeight, viewWidth = it.viewWidth, isDragging = it.isDragging)
                            val finOffSet = ctx.scrollOffset(params, dataList.size)
                            isSnapping = true
                            scroller.setContentOffset(0f, finOffSet, true, SpringAnimation(200, 1.0f, it.velocityY))
                            targetIndex =
                                (finOffSet / ctx.attr.itemHeight).toInt()
                        }
                    }
                    dragEnd { params->
                        ctx.event.dragEndEvent?.let {
                            val finOffSet = ctx.scrollOffset(params, dataList.size)
                            isSnapping = true
                            scroller.setContentOffset(0f, finOffSet, true, SpringAnimation(200,1.0f,1f))
                            targetIndex =
                                (finOffSet / ctx.attr.itemHeight).toInt()
                            ctx.event.dragEndEvent?.invoke(dataList[targetIndex + offset], targetIndex)
                        }
                    }

                    scrollEnd { params->
                        ctx.event.scrollEndEvent?.let {
                            val finOffSet = ctx.scrollOffset(params, dataList.size)
                            if (isSnapping || params.offsetY == finOffSet) {
                                isSnapping = false
                                targetIndex =
                                    (finOffSet / ctx.attr.itemHeight).toInt()
                                ctx.event.scrollEndEvent?.invoke(dataList[targetIndex + offset], targetIndex)
                            } else {
                                isSnapping = true
                                scroller.setContentOffset(0f, finOffSet, true, SpringAnimation(200, 1.0f, 1f))
                            }
                        }
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
    // Width of each item
    var itemWidth: Float = 0f

    // Height of each item
    var itemHeight: Float = 0f

    // Number of items visible per screen
    var countPerScreen: Int = 0

    // Background color of each item
    var itemBackGroundColor: Color = Color.TRANSPARENT

    // Text color of each item
    var itemTextColor: Color = Color.BLACK

    // Whether to animate initial scroll, default true
    var initialScrollAnimated: Boolean = true
}

class ScrollPickerEvent: ComposeEvent() {
    @Deprecated(
        message = "Deprecated: Use scrollEndEvent instead. Drag end events are now unified with scroll end events under scrollEndEvent.",
        level = DeprecationLevel.WARNING
    )
    var dragEndEvent : ScrollPickerDragEndEvent? = null
    var scrollEndEvent : ScrollPickerScrollEndEvent? = null
    var scrollEvent : ScrollPickerScrollEvent? = null

    // Callback when drag ends, returns center item's value and index
    @Deprecated(
        message = "Deprecated: Use scrollEndEvent() instead. Drag end events are now unified with scroll end events under scrollEndEvent.",
        replaceWith = ReplaceWith("scrollEndEvent = event"),
        level = DeprecationLevel.WARNING
    )
    fun dragEndEvent(event: ScrollPickerDragEndEvent) {
        dragEndEvent = event
    }
    // Callback when scroll ends, returns center item's value and index
    fun scrollEndEvent(event: ScrollPickerScrollEndEvent) {
        scrollEndEvent = event
    }
    // Callback during scrolling, returns center item's value and index
    fun scrollEvent(event: ScrollPickerScrollEvent) {
        scrollEvent = event
    }
}
fun ViewContainer<*, *>.ScrollPicker(itemList : Array<String>, defaultIndex: Int? = null, init: ScrollPickerView.() -> Unit) {
    addChild(ScrollPickerView(itemList, defaultIndex), init)
}

/*
 * Scroll picker, can be combined for date or region selection
 */