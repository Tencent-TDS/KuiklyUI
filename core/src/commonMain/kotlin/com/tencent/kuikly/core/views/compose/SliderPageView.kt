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

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.global.GlobalFunctionRef
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListEvent
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.ScrollParams

/// 轮播图组件
class SliderPageView : ComposeView<SliderPageAttr, SliderPageEvent>() {

    lateinit var pageListRef: ViewRef<PageListView<*, *>>
    var currentPageIndex = 0
    private var timeoutTaskCallbackId : GlobalFunctionRef = ""
    private var isDragging = false
    private var currentOffsetX : Float = 0f
    private var currentOffsetY : Float = 0f
    private var isAutoPlaying = false
    private var viewDidLoad = false
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    scrollEnable(ctx.attr.scrollEnable)
                    pageItemWidth(ctx.attr.pageItemWidth)
                    pageItemHeight(ctx.attr.pageItemHeight)
                    defaultPageIndex(ctx.attr.defaultPageIndex + (if (ctx.attr.itemCount > 1) 1 else 0))
                    pageDirection(ctx.attr.isHorizontal)
                    showScrollerIndicator(false)
                    keepItemAlive = true
                }
                if (ctx.attr.lazyCreateItemsTask != null) {
                    apply(ctx.attr.lazyCreateItemsTask!!)
                }

                event {
                    scroll {
                        ctx.resetContentOffsetIfNeed(it)
                    }

                    pageIndexDidChanged {
                        ctx.firePageIndexDidChangedEventIfNeed(it as JSONObject)
                    }

                    dragBegin {
                        ctx.isDragging = true
                        ctx.stopLoopPlayIfNeed()
                    }

                    dragEnd {
                        ctx.isDragging = false
                        ctx.startLoopPlayIfNeed()
                    }
                }
            }
        }
    }

    override fun createAttr(): SliderPageAttr {
        return SliderPageAttr()
    }

    override fun createEvent(): SliderPageEvent {
        return SliderPageEvent()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        viewDidLoad = true
        startLoopPlayIfNeed()
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        stopLoopPlayIfNeed()
    }

     fun startLoopPlayIfNeed() {
        if (attr.itemCount > 1 && !isAutoPlaying && viewDidLoad) {
            autoLoopPlay()
        }
    }

     fun stopLoopPlayIfNeed() {
        if (timeoutTaskCallbackId.isNotEmpty()) {
            GlobalFunctions.destroyGlobalFunction(pagerId, timeoutTaskCallbackId)
            timeoutTaskCallbackId = ""
        }
         isAutoPlaying = false
    }

    private fun autoLoopPlay() {
        isAutoPlaying = true
        if (attr.loopPlayIntervalTimeMs == 0) {
            stopLoopPlayIfNeed()
        } else {
            timeoutTaskCallbackId = setTimeout(pagerId, attr.loopPlayIntervalTimeMs.toInt()) {
                // 滚动到下一个
                // 加个判断，防止在播放时间间隔内修改过IntervalTime
                if (attr.loopPlayIntervalTimeMs != 0) {
                    scrollToNextPageIfNeed()
                    autoLoopPlay()
                } else {
                    isAutoPlaying = false
                }
            }
        }
    }

    fun scrollToPage(index: Int, animation: Boolean = false) {
        val pageListView = pageListRef.view
        pageListView?.let {
            val viewWidth = it.flexNode.layoutFrame.width
            val viewHeight = it.flexNode.layoutFrame.height
            if (it.renderView != null && !isDragging && viewWidth > 0 && viewHeight > 0) {
                var offset = 0f
                if (attr.itemCount == index ) {
                    offset = 0.1f
                }
                if (attr.isHorizontal) {
                    it.setContentOffset((index + 1)  * viewWidth + offset, 0f, animation)
                } else {
                    it.setContentOffset(0f, (index + 1)  * viewHeight + offset, animation)
                }
            }
        }
    }

    private fun scrollToNextPageIfNeed() {
        scrollToPage(currentPageIndex + 1, true)
    }

    private fun firePageIndexDidChangedEventIfNeed(data: JSONObject) {
        var index = data.optInt("index")
        if (attr.itemCount > 1) {
            if (index == 0) {
                index = attr.itemCount - 1
            } else if(index == attr.itemCount + 1) {
                index = 0
            } else {
                index -= 1
            }
        }
        if (currentPageIndex != index) {
            currentPageIndex = index
            data.put("index", index)
            event.onFireEvent(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, data)
        }
    }

    private fun resetContentOffsetIfNeed(scrollParams: ScrollParams) {
        val contentWidth = scrollParams.contentWidth
        val contentHeight = scrollParams.contentHeight
        val viewWidth = scrollParams.viewWidth
        val viewHeight = scrollParams.viewHeight
        val offsetX = scrollParams.offsetX
        val offsetY = scrollParams.offsetY
        if (attr.isHorizontal && contentWidth > 3 * viewWidth - 1) {  // 横向
            val realContentWidth = contentWidth - 2 * viewWidth
            if (offsetX <= 0.1) {
                pageListRef.view?.setContentOffset(offsetX + realContentWidth, 0f )
            } else if (offsetX + 1 >=  contentWidth -  viewWidth) {
                pageListRef.view?.setContentOffset(offsetX - realContentWidth, 0f)
            }
        } else if (contentHeight > 3 * viewHeight - 1){ // 纵向
            val realContentHeight = contentHeight - 2 * viewHeight
            if (offsetY <= 0.1) {
                pageListRef.view?.setContentOffset(0f, offsetY + realContentHeight)
            } else if (offsetY + 1 >=  contentHeight -  viewHeight) {
                pageListRef.view?.setContentOffset(0f, offsetY - realContentHeight)
            }
        }
        currentOffsetX = offsetX
        currentOffsetY = offsetY
    }

}

class SliderPageAttr: ComposeAttr() {
    var defaultPageIndex: Int = 0
    var isHorizontal: Boolean = true
    var pageItemWidth = 0f
    var pageItemHeight = 0f
    var scrollEnable: Boolean = true
    // 轮播时间间隔（ms单位），若 == 0 则不轮播
    var loopPlayIntervalTimeMs :Int = 3000
        set(value) {
            if (value != field) {
                field = value
                (view() as? SliderPageView)?.startLoopPlayIfNeed()
            }
        }
    internal var itemCount = 0
    internal var lazyCreateItemsTask : (PageListView<*, *>.() -> Unit)? = null
    fun <T> initSliderItems(dataList: List<T>, creator: SliderItemCreator<T>) {
        if (dataList.isEmpty()) {
            return
        }
        itemCount = dataList.count()
        val ctx = this
        lazyCreateItemsTask = {
            if (ctx.itemCount > 1) {
                creator(dataList.last())
            }
            dataList.forEach {
                creator(it)
            }
            if (ctx.itemCount > 1) {
                creator(dataList.first())
            }
        }
    }

}
class SliderPageEvent: ComposeEvent() {
    fun pageIndexDidChanged(handler: EventHandlerFn) {
        register(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED,handler)
    }
}

typealias SliderItemCreator<T> = PageListView<*, *>.(item: T) -> Unit

fun ViewContainer<*, *>.SliderPage(init: SliderPageView.() -> Unit) {
    addChild(SliderPageView(), init)
}