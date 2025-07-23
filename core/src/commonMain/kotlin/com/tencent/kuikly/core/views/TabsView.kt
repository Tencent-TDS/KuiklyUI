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
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.pager.IPagerLayoutEventObserver
import com.tencent.kuikly.core.reactive.handler.observable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/*
 * Tabs组件（与分页列表组件配套使用）
 */
class TabsView : ListView<TabsAttr, TabsEvent>(), IPagerLayoutEventObserver {
    private var indicatorViewRef: ViewRef<DivView>? = null
    private var contentViewFrame: Frame = Frame.zero
    private var didInitContentOffset = false
    override fun createAttr() = TabsAttr()
    override fun createEvent() = TabsEvent()

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        getPager().addPagerLayoutEventObserver(this)
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        getPager().removePagerLayoutEventObserver(this)
    }

    private fun contentViewFrameDidChanged() {
        updateIndicatorPositionIfNeed()

    }

    internal fun scrollParamsDidChanged() {
        updateIndicatorPositionIfNeed()
    }

    private fun updateIndicatorPositionIfNeed() {
        if (contentViewFrame === Frame.zero) {
            return
        }
        val tabItems = contentView?.domChildren()?.filter { it is TabItemView } ?: arrayListOf()
        if (tabItems.isEmpty()) {
            this.indicatorViewRef?.view?.getViewAttr()?.visibility(false)
            return
        }
        this.indicatorViewRef?.view?.getViewAttr()?.visibility(true)
        val ctx = this
        var selectedIndex = min(this.attr.initIndex, tabItems.size - 1)
        var left = tabItems[selectedIndex].flexNode.layoutFrame.x
        var width =  tabItems[selectedIndex].flexNode.layoutFrame.width
        var scrollProgress = if (tabItems.size - 1 <= 0) 0f else (selectedIndex.toFloat() / ((tabItems.size - 1).toFloat()))
        this.attr.scrollParams?.also {
            selectedIndex = min((it.offsetX / it.viewWidth + 0.5f).toInt(), tabItems.size - 1)
            val leftIndex = min((it.offsetX / it.viewWidth).toInt(),tabItems.size - 1)
            val rightIndex = min(leftIndex + 1, tabItems.size - 1)
            val letItemFrame = tabItems[leftIndex].flexNode.layoutFrame
            val rightItemFrame = tabItems[rightIndex].flexNode.layoutFrame
            val betweenProgress = (it.offsetX / it.viewWidth) - (it.offsetX / it.viewWidth).toInt()
            left = letItemFrame.x + (rightItemFrame.x - letItemFrame.x) * betweenProgress
            width = letItemFrame.width + (rightItemFrame.width - letItemFrame.width) * betweenProgress
            scrollProgress = it.offsetX / (it.contentWidth - it.viewWidth)
        }

        val selectedItem = tabItems[selectedIndex] as? TabItemView
        var itemSelectedIndexDidChanged = false
        tabItems.forEach {
            val item = it as? TabItemView
            val selected = item === selectedItem
            if (item?.state?.selected != selected) {
                itemSelectedIndexDidChanged = true
                item?.state?.selected = selected // 更新选中态
            }
        }
        // update scroll offset
        var offsetX = if (this.attr.tabAlignCenter) {
            left - (flexNode.layoutFrame.width - width) * 0.5f
        } else {
            scrollProgress * (contentViewFrame.width - flexNode.layoutFrame.width)
        }
        offsetX = max(min(offsetX, (contentViewFrame.width - flexNode.layoutFrame.width)),0f)
        var animated = false
        if (curOffsetX != offsetX) {
            // 判断当前pageList是否完全归位
            var isFullIndexPosition = false
            this.attr.scrollParams?.also {
                val progress = (it.offsetX / it.viewWidth)
                isFullIndexPosition = progress.isCloseToInt(0.05f)
            }
            animated = didInitContentOffset && itemSelectedIndexDidChanged && isFullIndexPosition
            setContentOffset( offsetX, 0f, animated)
        }

        this.indicatorViewRef?.view?.getViewAttr()?.also {
            if (animated) {
                it.setProp(Attr.StyleConst.ANIMATION, Animation.linear(0.2f).toString())
            }
            it.left(left)
            it.top(0f)
            it.size(width, contentViewFrame.height)
            this.indicatorViewRef?.view?.renderView?.setFrame(left, 0f, width, contentViewFrame.height)
            if (animated) {
                it.setProp(Attr.StyleConst.ANIMATION, "")
            }
        }

        didInitContentOffset = true

    }

    // 是否接近整数
    fun Float.isCloseToInt(tolerance: Float = 0.001f): Boolean {
        val roundedValue = round(this)
        return abs(this - roundedValue) <= tolerance
    }

    override fun willInit() {
        super.willInit()
        attr.flexDirectionRow() // 只支持横向
        attr.bouncesEnable(false)
        attr.showScrollerIndicator(false)
    }

    override fun didInit() {
        super.didInit()
        attr.flexDirectionRow() // 只支持横向
        val ctx = this
        if (ctx.attr.flexNode!!.styleHeight.valueEquals(Float.undefined)) {
            throwRuntimeError("Tabs need setup height , like attr { height(50f) }")
        }
        attr.indicatorCreator?.also { creator ->
            View {
                ref {
                    ctx.indicatorViewRef = it
                }
                attr {
                    absolutePosition(top = 0f, left = 0f)
                    zIndex(-1)
                    visibility(false)
                }
                creator.invoke(this)
            }
        }
    }

    /// IPagerLayoutEventObserver - begin
    override fun onPagerWillCalculateLayoutFinish() {}
    override fun onPagerCalculateLayoutFinish() {

    }
    override fun onPagerDidLayout() {
        val ctx = this
        this.contentView?.flexNode?.also {
            if (!(it.layoutFrame.equals(ctx.contentViewFrame))) {
                ctx.contentViewFrame = it.layoutFrame
                contentViewFrameDidChanged()
            }
        }
    }

    /// IPagerLayoutEventObserver - end

}

class TabsAttr : ListAttr() {
    internal var scrollParams : ScrollParams? = null
    internal var initIndex : Int = 0
    internal var indicatorCreator : ViewBuilder? = null
    internal var tabAlignCenter : Boolean = false
    /*
     * 更新scroller滚动信息使得tabs组件'指示条'同步滚动
     * 注：该参数必须设置，才能让tabs组件正常使用，该参数来自PageList等Scroller容器组件中监听scroll事件的参数
     */
    fun scrollParams(scrollParams: ScrollParams) {
        this.scrollParams = scrollParams
        (this.view() as? TabsView)?.scrollParamsDidChanged()
    }
    /*
     * 首次默认初始化的tabs组件对应index
     */
    fun defaultInitIndex(index: Int) {
        initIndex = index
    }
    /*
     * 生成可滚动的指示条，配合scrollParams同步滚动
     */
    fun indicatorInTabItem(creator: ViewBuilder) {
        indicatorCreator = creator
    }
    /*
     * 指示条居中滚动
     */
    fun indicatorAlignCenter() {
        tabAlignCenter = true
    }
    /*
     * 指示条按比例滚动（默认行为）
     */
    fun indicatorAlignAspectRatio() {
        tabAlignCenter = false
    }
}

class TabsEvent : ListEvent()

/*
 * Tabs组件（与分页列表组件配套使用）
 */
fun ViewContainer<*, *>.Tabs(init: TabsView.() -> Unit) {
   addChild(TabsView(), init)
}

/*
 * 与TabsView配套的TabItemView
 */
fun ViewContainer<*, *>.TabItem(init: TabItemView.(newState : TabItemView.ItemState) -> Unit) {
    val itemView = TabItemView()
    addChild(itemView) {
        init.invoke(this, this.state)
    }
}

class TabItemView : ViewContainer<TabItemAttr, TabItemEvent>() {
    internal var state = ItemState()
    class ItemState {
        /// 是否选中（用于更新选中高亮UI）
        var selected by observable(false)
    }
    override fun createAttr() = TabItemAttr()
    override fun createEvent() = TabItemEvent()
    override fun viewName(): String = ViewConst.TYPE_VIEW
}

class TabItemAttr : ContainerAttr()

class TabItemEvent : Event()
