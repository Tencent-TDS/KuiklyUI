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

import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 创建一个分页组件实例并添加到视图容器中。
 * @param init 一个 PageListView<*, *>.() -> Unit 函数，用于初始化分页组件的属性和子视图。
 */
fun ViewContainer<*, *>.PageList(init: PageListView<*, *>.() -> Unit) {
    addChild(PageListView<PageListAttr, PageListEvent>(), init)
}

open class PageListAttr : ListAttr() {
    internal var defaultPageIndex: Int = 0
    var keepItemAlive: Boolean = false
    internal var pageItemWidth: Float? = null
    internal var pageItemHeight: Float? = null
    internal var pageItemSizeChangeCallback: (() -> Unit)? = null
    internal var offscreenPageLimit: Int? = null

    init {
        firstContentLoadMaxIndex = 1 // 默认首屏分批加载 1 个
    }

    /**
     * 指定 pageItem 宽度，让 pageList 宽度和 pageItem 等宽。
     * 注：若不调用该方法指定 item 宽度，则默认根据 pageItem 自身宽度并和 pageList.left 左部对齐吸附。
     * @param width pageItem 的宽度。
     */
    fun pageItemWidth(width: Float) {
        val changed = pageItemWidth != width
        pageItemWidth = width
        width(width)
        if (changed) {
            pageItemSizeChangeCallback?.invoke()
            updatePreloadViewDistance()
        }
    }

    /**
     * 指定 pageItem 高度，让 pageList 高度和 pageItem 等高。
     * 注：若不调用该方法指定 item 高度，则默认根据 pageItem 自身高度并和 pageList.top 顶部对齐吸附。
     * @param height pageItem 的高度。
     */
    fun pageItemHeight(height: Float) {
        val changed = pageItemHeight != height
        pageItemHeight = height
        height(height)
        if (changed) {
            pageItemSizeChangeCallback?.invoke()
            updatePreloadViewDistance()
        }
    }

    /**
     * 设置 page 排版方向，默认纵向。
     * @param isHorizontal 布尔值，表示是否为水平方向。
     */
    fun pageDirection(isHorizontal: Boolean) {
        if (isHorizontal) {
            flexDirectionRow()
        } else {
            flexDirectionColumn()
        }
    }

    override fun flexDirection(flexDirection: FlexDirection): ContainerAttr {
        return super.flexDirection(flexDirection).also { updatePreloadViewDistance() }
    }

    /**
     * 设置默认 page 索引。
     * @param defaultIndex 默认索引值。
     */
    fun defaultPageIndex(defaultIndex: Int) {
        defaultPageIndex = defaultIndex
        firstContentLoadMaxIndex(firstContentLoadMaxIndex)
    }

    /**
     * 设置 pageItems 是否保活，不进行列表回收和复用。
     * @param alive 布尔值，表示是否保活。
     */
    fun keepItemAlive(alive: Boolean) {
        this.keepItemAlive = alive
    }

    // 首屏分批加载中首次的最大条数（pageItem 个数）
    override fun firstContentLoadMaxIndex(maxIndex: Int) {
        val index = max(maxIndex, defaultPageIndex + 1)
        super.firstContentLoadMaxIndex(index)
    }

    /**
     * 设置离屏 page 个数，仅当[keepItemAlive]为`false`时有效，默认`2`
     */
    fun offscreenPageLimit(value: Int) {
        val realValue = max(1, value)
        if (this.offscreenPageLimit != realValue) {
            this.offscreenPageLimit = realValue
            updatePreloadViewDistance()
        }
    }

    private fun updatePreloadViewDistance() {
        if (offscreenPageLimit == null) {
            return
        }
        val isRow = getProp(DIRECTION_ROW) == 1
        // 把offscreenPageLimit换算成preloadViewDistance，减0.5f避免临界值导致多一页（或少一页）
        if (isRow && pageItemWidth != null) {
            preloadViewDistance((offscreenPageLimit!! - 0.5f) * pageItemWidth!!)
        } else if (!isRow && pageItemHeight != null) {
            preloadViewDistance((offscreenPageLimit!! - 0.5f) * pageItemHeight!!)
        }
    }
}

open class PageListEvent : ListEvent() {

    private val willDragEndHandlers = arrayListOf<(WillEndDragParams) -> Unit>()

    /**
     * 设置页面索引改变时的事件处理器。
     * @param handler 一个函数，当页面索引改变时调用。
     */
    fun pageIndexDidChanged(handler: EventHandlerFn) {
        register(PageListEventConst.PAGE_INDEX_DID_CHANGED, handler)
    }

    /**
     * 设置将要结束拖动事件处理器。
     * @param handler 一个函数，接收 WillEndDragParams 参数，当将要结束拖动事件发生时调用。
     */
    override fun willDragEndBySync(handler: (WillEndDragParams) -> Unit) {
        if (willDragEndHandlers.isEmpty()) {
            super.willDragEndBySync {
                willDragEndHandlers.forEach { handler ->
                    handler(it)
                }
            }
        }
        willDragEndHandlers.add(handler)
    }

    object PageListEventConst {
        const val PAGE_INDEX_DID_CHANGED = "pageIndexDidChanged"
    }
}
open class PageListView<A : PageListAttr, E : PageListEvent> : ListView<A, E>() {
    /// 自定义分页
    internal var isCustomPaging: Boolean =false
    override fun attr(init: A.() -> Unit) {
        super.attr(init)
        if (attr.isHorizontalDirection && attr.pageItemWidth != null) {
            if (flexNode.positionType == FlexPositionType.RELATIVE && domParent?.getViewAttr()?.isHorizontalDirection == true && flexNode.flex != 0f) {
                useCustomPaging()
            } else {
                attr.pagingEnable(true) // 设置整个pageList宽度分页滚动
            }
        } else if (!attr.isHorizontalDirection && attr.pageItemHeight != null) {
            if (flexNode.positionType == FlexPositionType.RELATIVE && domParent?.getViewAttr()?.isHorizontalDirection == false && flexNode.flex != 0f) {
                useCustomPaging()
            } else {
                attr.pagingEnable(true) // 设置整个pageList宽度分页滚动
            }
        } else {
            useCustomPaging()
        }
    }

    override fun willInit() {
        super.willInit()
        getViewAttr().showScrollerIndicator(false) // PageList默认不显示
    }

    private fun useCustomPaging() {
        isCustomPaging = true
        attr.flingEnable(false)
        event {
            willDragEndBySync {
                val isHorizontal = this@PageListView.attr.isHorizontalDirection
                if (isHorizontal && it.offsetX < 0f) {
                    return@willDragEndBySync
                } else if (!isHorizontal && it.offsetY < 0){
                    return@willDragEndBySync
                }
                this@PageListView.scrollToNextItem(it)
            }
        }
    }

    /*
     * 滚动到某一个pageIndex
     */
    fun scrollToPageIndex(index: Int, animated: Boolean = false) : Boolean {
        if (contentView == null || attr.flexNode == null || contentView!!.flexNode.layoutFrame.isDefaultValue()) {
            return false
        }
        var result = false
        contentView?.getSubview(index)?.flexNode?.layoutFrame?.also {
            val indexSize = contentView?.getSubviews()?.size
            var x = it.x
            var y = it.y
            // 由于浮点计算误差可能导致内容宽高溢出而无法滚动，故此处这里做出判断
            if (indexSize != null && index == indexSize - 1) {
                val density = attr.pagerData.density
                if (attr.isHorizontalDirection) {
                    val pageListViewRenderWidth = (this.flexNode.layoutFrame.width * density).roundToInt()
                    val contentViewRenderWidth = (contentView?.flexNode?.layoutFrame?.width!! * density).roundToInt()
                    val renderMaxX = contentViewRenderWidth - pageListViewRenderWidth
                    val renderX = (it.x * density).roundToInt()
                    if (renderX > renderMaxX) {
                        x = renderMaxX / density
                    }
                } else {
                    val pageListViewRenderHeight = (this.flexNode.layoutFrame.height * density).roundToInt()
                    val contentViewRenderHeight = (contentView?.flexNode?.layoutFrame?.height!! * density).roundToInt()
                    val renderMaxY = contentViewRenderHeight - pageListViewRenderHeight
                    val renderY = (it.y * density).roundToInt()
                    if (renderY > renderMaxY) {
                        y = renderMaxY / density
                    }
                }
            }
            setContentOffset(x, y, animated)
            result = true
        }
        return result
    }

    private fun scrollToNextItem(params: WillEndDragParams) {
        if (contentView == null || attr.flexNode == null) {
            return
        }
        val crossItemViewInfo = getCrossItemViewInOffset(params.offsetX, params.offsetY)
        if (attr.isHorizontalDirection) {
            crossItemViewInfo.itemView?.also {
                 var targetScrollOffset = crossItemViewInfo.offset
                if (crossItemViewInfo.crossPercentage01 > 0f) {
                    val itemViewWidth = crossItemViewInfo.itemView.flexNode.layoutFrame.width
                    if (params.velocityX > 0f) {
                        targetScrollOffset = crossItemViewInfo.offset + itemViewWidth
                    } else if (params.velocityX == 0f){
                        targetScrollOffset = crossItemViewInfo.offset + (if (crossItemViewInfo.crossPercentage01 >= 0.5) itemViewWidth else 0f)
                    }
                }
                targetScrollOffset = min(targetScrollOffset, contentView!!.flexNode.layoutFrame.width - attr.flexNode!!.layoutFrame.width)
                setContentOffset(targetScrollOffset, 0f, true, SpringAnimation(400, 1f, params.velocityX))
            }
        } else {
            crossItemViewInfo.itemView?.also {
                var targetScrollOffset = crossItemViewInfo.offset
                if (crossItemViewInfo.crossPercentage01 > 0f) {
                    val itemViewWidth = crossItemViewInfo.itemView.flexNode.layoutFrame.height
                    if (params.velocityY > 0f) {
                        targetScrollOffset = crossItemViewInfo.offset + itemViewWidth
                    } else if (params.velocityY == 0f){
                        targetScrollOffset = crossItemViewInfo.offset + (if (crossItemViewInfo.crossPercentage01 >= 0.5) itemViewWidth else 0f)
                    }
                }
                targetScrollOffset = min(targetScrollOffset, contentView!!.flexNode.layoutFrame.height - attr.flexNode!!.layoutFrame.height)
                setContentOffset(0f, targetScrollOffset, true, SpringAnimation(400, 1f, params.velocityY))
            }
        }
    }

    // 获取当前列表偏移量与之相交的pageItemView
    internal fun getCrossItemViewInOffset(offsetX: Float, offsetY: Float) : CrossItemViewInfo {
        var crossItemView : DeclarativeBaseView<*, *>? = null
        var crossPercentage01 : Float = 0f
        var currentOffset = 0f
        val domChildren =  contentView!!.domChildren()
        val inOffset = if (attr.isHorizontalDirection) offsetX else offsetY
        var index = 0
        for (child in domChildren) {
            val childWidth = if (attr.isHorizontalDirection) child.flexNode.layoutFrame.width else child.flexNode.layoutFrame.height
            if (currentOffset <= inOffset && currentOffset + childWidth > inOffset) {
                crossItemView = child
                crossPercentage01 = (inOffset - currentOffset) / childWidth
                break
            }
            currentOffset += childWidth
            index++
        }
        return CrossItemViewInfo(crossItemView, crossPercentage01, currentOffset, index)
    }

    data class CrossItemViewInfo(val itemView : DeclarativeBaseView<*, *>?, val crossPercentage01: Float, val offset : Float, val index: Int)

    override fun createContentView(): ScrollerContentView {
        return PageListContentView()
    }

    override fun createAttr(): A {
        return PageListAttr() as A
    }

    override fun createEvent(): E {
        return PageListEvent() as E
    }

}

open class PageListContentView : ListContentView() {
    var didInitDefaultPageIndex = false
    var currentPageIndex = 0
    override fun contentOffsetDidChanged(offsetX: Float, offsetY: Float, params: ScrollParams) {
        super.contentOffsetDidChanged(offsetX, offsetY, params)
        dispatchPageIndexDidChangedEvent(offsetX, offsetY)
    }

    override fun setFrameToRenderView(frame: Frame) {
        super.setFrameToRenderView(frame)
        syncDefaultPageIndexIfNeed(frame)
    }

    override fun didInsertDomChild(child: DeclarativeBaseView<*, *>, index: Int) {
        super.didInsertDomChild(child, index)
        fillSubViewLayoutAttr(child)
    }

    private fun fillSubViewLayoutAttr(subView: DeclarativeBaseView<*, *>) {
        val parentView = parent!!
        subView.attr {
            val attr = (parentView.getViewAttr() as PageListAttr)
            margin(all = 0f)
            positionRelative()
            this@PageListContentView.fillItemViewSizeIfNeed(subView)
            (parentView.getViewAttr() as PageListAttr).keepItemAlive.also {
                if (it) {
                    keepAlive(true)
                }
            }
        }
    }

    private fun fillItemViewSizeIfNeed(itemView: DeclarativeBaseView<*, *>) {
        parent?.also {
            val parentView = it
            val attr = (parentView.getViewAttr() as PageListAttr)
            if (attr.pageItemWidth != null || attr.pageItemHeight != null) {
                itemView.getViewAttr().width(parentView.getViewAttr().flexNode!!.styleWidth)
                itemView.getViewAttr().height(parentView.getViewAttr().flexNode!!.styleHeight)
                if (attr.pageItemSizeChangeCallback == null) {
                    attr.pageItemSizeChangeCallback = {
                        pageItemSizeDidChanged()
                    }
                }
            }
        }
    }

    private fun pageItemSizeDidChanged() {
        domChildren().toFastList().forEach {
            fillItemViewSizeIfNeed(it)
        }
        // 归位偏移量
        autoResetOffsetIfNeed()
    }

    private fun autoResetOffsetIfNeed() {
        if ((parent as PageListView<*, *>).isCustomPaging) {
            return
        }
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            // update offset
            val pageListView = parent as? PageListView<*, *>
            if (pageListView == null) {
                KLog.e("KuiklyError", "autoResetOffsetIfNeed: parent is not a PageListView")
            } else {
                val pageListAttr = pageListView.getViewAttr()
                val index = currentPageIndex
                if (pageListAttr.isHorizontalDirection) {
                    pageListView.setContentOffset(index * pageListAttr.flexNode!!.styleWidth, 0f)
                } else {
                    pageListView.setContentOffset(0f, index * pageListAttr.flexNode!!.styleHeight)
                }
            }
        }
    }

    private fun syncDefaultPageIndexIfNeed(frame: Frame) {
        if (!didInitDefaultPageIndex && frame.width > 0 && frame.height > 0) {
            didInitDefaultPageIndex = true
            val pageListAttr = (parent as PageListView<*, *>).getViewAttr()
            val index = pageListAttr.defaultPageIndex
            if (index > 0) {
                if (pageListAttr.isHorizontalDirection) {
                    (parent as PageListView<*, *>).setContentOffset(index * pageListAttr.flexNode!!.styleWidth, 0f)
                } else {
                    (parent as PageListView<*, *>).setContentOffset(0f, index * pageListAttr.flexNode!!.styleHeight)
                }
            }
        }
    }

    private fun getLeftItemIndexInOffset(offsetX: Float, offsetY: Float): Int {
        val children = domChildren()
        val pageListView =  (parent as PageListView<*, *>)
        val pageListAttr = pageListView.getViewAttr()
        if (pageListAttr.isHorizontalDirection) {
            val leftOffsetX = offsetX + pageListView.frame.width * 0.1f
            for (i in children.indices) {
                val itemFrame = children[i].frame
                if (itemFrame.x < leftOffsetX && itemFrame.x + itemFrame.width > leftOffsetX) {
                    return i
                }
            }
        } else {
            val leftOffsetY = offsetY + pageListView.frame.height * 0.1f
            for (i in children.indices) {
                val itemFrame = children[i].frame
                if (itemFrame.y < leftOffsetY && itemFrame.y + itemFrame.height > leftOffsetY) {
                    return i
                }
            }
        }
        return -1
    }

    private fun getDefaultIndexInOffset(offsetX: Float, offsetY: Float): Int {
        val pageListView =  (parent as PageListView<*, *>)
        val pageListAttr = pageListView.getViewAttr()
        val offset = if (pageListAttr.isHorizontalDirection) offsetX else offsetY
        val pageItemWidth = if (pageListAttr.isHorizontalDirection) pageListAttr.flexNode!!.styleWidth else pageListAttr.flexNode!!.styleHeight
        val floatIndex = offset / pageItemWidth
        val decimalPart = floatIndex - floatIndex.toInt()
        val indexRatio = 0.05f
        if (decimalPart > indexRatio && decimalPart < (1 - indexRatio)) {
            return -1
        }
        return (floatIndex + indexRatio).toInt()
    }

    private fun dispatchPageIndexDidChangedEvent(offsetX: Float, offsetY: Float) {
        // 维护index change
        val pageListView =  (parent as PageListView<*, *>)
        val pageListAttr = pageListView.getViewAttr()
        var newIndex = currentPageIndex
        var leftIndex = -1
        if (pageListView.isCustomPaging) { // 自定义分页（PageItem不定宽度）
            leftIndex = getLeftItemIndexInOffset(offsetX, offsetY)
        } else {
            leftIndex = getDefaultIndexInOffset(offsetX, offsetY)
        }
        if (leftIndex >= 0) {
            newIndex = leftIndex
        }
        if (newIndex < 0 ) {
            newIndex = 0
        }
        if (newIndex != currentPageIndex) {
            currentPageIndex = newIndex
            val data = JSONObject()
            data.put("index", newIndex)
            (parent as PageListView<*, *>).getViewEvent()
                .onFireEvent(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, data)
        }
    }

    override fun needUpdateOffset(): Boolean {
        return false
    }

}
