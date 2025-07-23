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

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.internal.GroupEvent
import kotlin.math.max

/**
 * 创建一个 ListView 实例并添加到视图容器中。
 * @param init 一个 ListView<*, *>.() -> Unit 函数，用于初始化 ListView 的属性和子视图。
 */
fun ViewContainer<*, *>.List(init: ListView<*, *>.() -> Unit) {
    addChild(ListView<ListAttr, ListEvent>(), init)
}

open class ListAttr : ScrollerAttr() {
    var firstContentLoadMaxIndex = 8
    internal var preloadViewDistance = 0f
    internal var initContentOffset = 0f
    /**
     * 设置首次内容加载的最大条数。
     * @param maxIndex 首次加载的最大条数。
     */
    open fun firstContentLoadMaxIndex(maxIndex: Int) {
        firstContentLoadMaxIndex = max(maxIndex, MIN_LOAD_INDEX)
    }

    /**
     * 设置列表视图预加载距离，默认为 list.height（可理解为多加载一屏视图）。
     * 注意：distance 设置为 0 则代表启用默认预加载距离：list.height，其他 distance 值则顶部预加载 distance / 2, 底部预加载 distance 距离。
     * @param distance 预加载距离。
     */
    fun preloadViewDistance(distance: Float) {
        if (preloadViewDistance != distance) {
            preloadViewDistance = distance
            (view() as? ListView<*, *>)?.preloadViewDistanceDidUpdate()
        }
    }

    companion object {
        const val MIN_LOAD_INDEX = 1 // 最小首屏分批加载数
    }
}

open class ListEvent : ScrollerEvent()

interface IListViewEventObserver: IScrollerViewEventObserver

open class ListView<A : ListAttr, E : ListEvent> : ScrollerView<A, E>() {
    private var prepareForReuse = false
    protected var lastFrameSize: Size? = null
    override fun createContentView(): ScrollerContentView {
        return ListContentView()
    }

    override fun createRenderView() {
        if (prepareForReuse) {
            setContentOffset(offsetX = curOffsetX, offsetY = curOffsetY, animated = false)
        }
        super.createRenderView()
        prepareForReuse = false
    }

    override fun removeRenderView() {
        super.removeRenderView()
        prepareForReuse = true
        lastFrameSize = null
    }

    internal fun preloadViewDistanceDidUpdate() {
        updateContentRenderViewsOnVisibleRectIfNeed()
    }
    override fun createAttr(): A {
        return ListAttr() as A
    }

    override fun createEvent(): E {
        return ListEvent() as E
    }

    private fun updateContentRenderViewsOnVisibleRectIfNeed() {
        if (contentView?.flexNode?.layoutFrame?.isDefaultValue() == false) {
            (contentView as? ListContentView)?.createRenderViewsOnVisibleRect()
        }
    }

    override fun viewName(): String {
        return ViewConst.TYPE_LIST
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        if (lastFrameSize == null) {
            lastFrameSize = Size(frame.width, frame.height)
            updateContentRenderViewsOnVisibleRectIfNeed()
        } else if (lastFrameSize!!.width != frame.width || lastFrameSize!!.height != frame.height) {
            lastFrameSize = Size(frame.width, frame.height)
            (contentView as? ListContentView)?.updateChildLayout()
        }
    }
}

open class ListContentView : ScrollerContentView() {
    private var didFirstLayout = false
    private var waitingToNextTickLayout = false
    private var didAddNextTickUpdateVisibleOffset = false
    private var didInitContentOffset = false

    override fun setFrameToRenderView(frame: Frame) {
        super.setFrameToRenderView(frame)
        updateOffsetIfNeed()
    }
    override fun didInsertDomChild(child: DeclarativeBaseView<*, *>, index: Int) {
        super.didInsertDomChild(child, index)
        child.getViewAttr().setProp(SCROLL_INDEX, index)
        val listFlexNode = flexNode
        val ctx = this
        if (child.flexNode.positionType == FlexPositionType.ABSOLUTE) {
            child.absoluteFlexNode = true
        } else {
            child.getViewAttr().apply {
                if (ctx.isRowFlexDirection()) {
                    positionAbsolute()
                    top(max(listFlexNode.getPadding(StyleSpace.Type.TOP), 0f))
                    left(0f)
                    right(Float.undefined)
                    bottom(max(listFlexNode.getPadding(StyleSpace.Type.BOTTOM), 0f))
                } else {
                    positionAbsolute()
                    top(0f)
                    left(max(listFlexNode.getPadding(StyleSpace.Type.LEFT), 0f))
                    right(max(listFlexNode.getPadding(StyleSpace.Type.RIGHT), 0f))
                    bottom(Float.undefined)
                }
            }
        }

    }

    override fun createAttr(): ScrollerAttr {
        return ScrollerAttr()
    }

    override fun createEvent(): GroupEvent {
        return GroupEvent()
    }

    override fun createRenderView() {
        // super.createRenderView()
        createComponentRenderViewIfNeed()
    }

    override fun renderViewDidMoveToParentRenderView() {
        super.renderViewDidMoveToParentRenderView()
        if (!flexNode.layoutFrame.isDefaultValue()) {
            createRenderViewsOnVisibleRect()
        }
    }

    override fun insertSubRenderView(subView: DeclarativeBaseView<*, *>) {
        // nothing to do
    }

    override fun removeRenderView() {
        super.removeRenderView()
    }

    override fun contentOffsetDidChanged(offsetX: Float, offsetY: Float, params: ScrollParams) {
        super.contentOffsetDidChanged(offsetX, offsetY, params)
        createRenderViewsOnVisibleRect()
    }

    override fun onPagerWillCalculateLayoutFinish() {
        val parentDirty = parent?.flexNode?.isDirty
        if (!needLayout && parentDirty == false && !flexNode.isDirty) {
            flexNode.onlyClearChildren()
            return
        }
        val onlyParentDirty = (parentDirty == true && !flexNode.isDirty)
        val dirtyChildren = arrayListOf<FlexNode>()
        val absoluteDirtyChildren = arrayListOf<FlexNode>()
        val domChildren = domChildren()
        domChildren.forEachIndexed { index, declarativeBaseView ->
            if (onlyParentDirty || declarativeBaseView.flexNode.isDirty) {
                if (declarativeBaseView.absoluteFlexNode) {
                    absoluteDirtyChildren.add(declarativeBaseView.flexNode)
                } else {
                    dirtyChildren.add(declarativeBaseView.flexNode)
                }
            }
        }
        flexNode.onlyClearChildren()
        val firstContentLoadMaxIndex =
            (parent as ListView<*, *>).getViewAttr().firstContentLoadMaxIndex  // 首屏默认最多8个
        if (!didFirstLayout && dirtyChildren.count() > firstContentLoadMaxIndex) {
            didFirstLayout = true
            // 分帧加载第一批的maxIndex个数
            dirtyChildren.subList(0, firstContentLoadMaxIndex)
                .forEachIndexed { index, subFlexNode ->
                    flexNode.onlyAddChild(subFlexNode)
                }
            waitingToNextTickLayout = true
            getPager().addNextTickTask {
                waitingToNextTickLayout = false
                flexNode.markDirty()
            }
        } else {
            if (waitingToNextTickLayout) { // maxIndex内的item位置以及layout过的节点继续更新
                val domFlexNodes = arrayListOf<FlexNode>()
                domChildren.forEach {
                    domFlexNodes.add(it.flexNode)
                }
                dirtyChildren.forEach {
                    val index = domFlexNodes.indexOf(it)
                    if ((index in 0 until firstContentLoadMaxIndex) || !it.layoutFrame.isDefaultValue()) {
                        flexNode.onlyAddChild(it)
                    }
                }
            } else {
                if (dirtyChildren.isNotEmpty()) {
                    didFirstLayout = true // 存在布局节点，则补为首次布局过
                }
                dirtyChildren.forEachIndexed { index, subFlexNode ->
                    flexNode.onlyAddChild(subFlexNode)
                }
            }
        }
        absoluteDirtyChildren.forEachIndexed { index, subFlexNode ->
            flexNode.onlyAddChild(subFlexNode) // 绝对布局节点需要不参与分批加载
        }
    }

    override fun didSetFrameToRenderView() {
        super.didSetFrameToRenderView()
        initContentOffsetIfNeed()
    }

    private fun initContentOffsetIfNeed() {
        if (!didInitContentOffset) { // 初始化列表偏移量，默认为0f
            didInitContentOffset = true
            (parent as? ListView<*, *>)?.also {
                if (it.getViewAttr().initContentOffset > 0) {
                    if (isRowFlexDirection()) {
                        it.setContentOffset(it.getViewAttr().initContentOffset, 0f)
                    } else {
                        it.setContentOffset(0f, it.getViewAttr().initContentOffset)
                    }
                }
            }
        }
    }

    protected fun needLayoutChildren(): List<DeclarativeBaseView<*, *>> {
        return domChildren().filterNot {
            it.flexNode.layoutFrame.isDefaultValue()
        }
    }

    override fun onPagerCalculateLayoutFinish() {
        var curOffset = 0f
        var curAbsoluteNodeOffset = 0f
        if (isRowFlexDirection()) {
            curOffset = max(flexNode.getPadding(StyleSpace.Type.LEFT), 0f)
            needLayoutChildren().forEach {
                if (it.absoluteFlexNode ) {
                    curAbsoluteNodeOffset = max(it.flexNode.layoutFrame.maxX(), curAbsoluteNodeOffset)
                } else {
                    val frame = it.flexNode.layoutFrame.toMutableFrame()
                    frame.x = curOffset + it.flexNode.getMargin(StyleSpace.Type.LEFT)
                    curOffset = frame.x + frame.width + it.flexNode.getMargin(StyleSpace.Type.RIGHT)
                    it.flexNode.updateLayoutFrame(frame.toFrame())
                }
            }
            curOffset += max(flexNode.getPadding(StyleSpace.Type.RIGHT), 0f)
        } else {
            curOffset = max(flexNode.getPadding(StyleSpace.Type.TOP), 0f)
            needLayoutChildren().forEach {
                if (it.absoluteFlexNode) {
                    curAbsoluteNodeOffset = max(it.flexNode.layoutFrame.maxY(), curAbsoluteNodeOffset)
                } else {
                    val frame = it.flexNode.layoutFrame.toMutableFrame()
                    frame.y = curOffset + it.flexNode.getMargin(StyleSpace.Type.TOP)
                    curOffset =
                        frame.y + frame.height + it.flexNode.getMargin(StyleSpace.Type.BOTTOM)
                    it.flexNode.updateLayoutFrame(frame.toFrame())
                }
            }
            curOffset += max(flexNode.getPadding(StyleSpace.Type.BOTTOM), 0f)
        }
        val contentHeight = max(curOffset, curAbsoluteNodeOffset)
        val frame = flexNode.layoutFrame.toMutableFrame()
        if (isRowFlexDirection()) frame.width = contentHeight else frame.height = contentHeight
        flexNode.updateLayoutFrame(frame.toFrame())
    }

    override fun onPagerDidLayout() {
        if (needLayout) {
            val success = createRenderViewsOnVisibleRect()
            parent?.also {
                if (parent is ListView<*, *>) {
                    (parent as ListView<*, *>).subViewsDidLayout()
                }
            }
            if (success && !waitingToNextTickLayout) {
                needLayout = false
            }
        }

    }

    private fun shouldRemoveRenderView(view: DeclarativeBaseView<*, *>): Boolean {
        return view.renderView != null && !view.getViewAttr().keepAlive
    }

    private fun shouldCreateRenderView(view: DeclarativeBaseView<*, *>): Boolean {
        return view.renderView == null
    }

    private fun addNextTickUpdateVisibleOffsetIfNeed() {
        if (!didAddNextTickUpdateVisibleOffset) {
            didAddNextTickUpdateVisibleOffset = true
            getPager().addNextTickTask {
                createRenderViewsOnVisibleRect()
            }
        }
    }

    private fun visibleOffset(isRowFlexDirection: Boolean): Float {
        if ((getPager() as? Pager)?.didCreateBody == false) { // 还未创建完首屏，只加载屏幕中的view
            addNextTickUpdateVisibleOffsetIfNeed()
            return 0f
        }

        val preloadViewDistance =
            (parent as? ListView<*, *>)?.getViewAttr()?.preloadViewDistance ?: 0f
        if (preloadViewDistance != 0f) {
            return preloadViewDistance
        }
        if (isRowFlexDirection) {
            return parent?.flexNode?.layoutFrame?.width ?: 0f
        }
        return parent?.flexNode?.layoutFrame?.height ?: 0f
    }

    fun createRenderViewsOnVisibleRect(): Boolean {
        var containUnLayoutNode = false
        if (parent === null || parent!!.flexNode.layoutFrame.isDefaultValue() || renderView == null) {
            return true
        }
        val needRemoveViewViews = arrayListOf<DeclarativeBaseView<*, *>>()
        val needCreateViewViews = arrayListOf<DeclarativeBaseView<*, *>>()
        if (isRowFlexDirection()) {
            val visibleOffset = visibleOffset(true)
            val visibleLeft = offsetX - visibleOffset
            val visibleRight = offsetX + parent!!.flexNode.layoutFrame.width + visibleOffset
            renderChildren().filterNot {
                val default = it.flexNode.layoutFrame.isDefaultValue()
                if (default) {
                    containUnLayoutNode = true
                }
                default
            }.forEach {
                val frame = it.flexNode.layoutFrame
                if (frame.maxX() < visibleLeft || frame.minX() > visibleRight) {
                    if (shouldRemoveRenderView(it)) {
                        needRemoveViewViews.add(it)
                    }
                } else {
                    if (shouldCreateRenderView(it)) {
                        needCreateViewViews.add(it)
                    }
                }
            }
        } else {
            val visibleOffset = visibleOffset(false)
            val visibleTop = offsetY - visibleOffset
            val visibleBottom = offsetY + parent!!.flexNode.layoutFrame.height + visibleOffset
            renderChildren().filterNot {
                val default = it.flexNode.layoutFrame.isDefaultValue()
                if (default) {
                    containUnLayoutNode = true
                }
                default
            }.forEach {
                val frame = it.flexNode.layoutFrame
                if (frame.maxY() < visibleTop || frame.minY() > visibleBottom) {
                    if (shouldRemoveRenderView(it)) {
                        needRemoveViewViews.add(it)
                    }
                } else {
                    if (shouldCreateRenderView(it)) {
                        needCreateViewViews.add(it)
                    }
                }
            }
        }
        needRemoveViewViews.forEach { component ->
            component.removeRenderView()
        }
        needCreateViewViews.forEach { component ->
            component.createRenderView()
            renderView!!.insertSubRenderView(component.nativeRef, -1)
        }
        return !containUnLayoutNode
    }

    internal open fun needUpdateOffset(): Boolean = !flexNode.layoutFrame.isDefaultValue()

    private fun updateOffsetIfNeed() {
        if (!needUpdateOffset()) {
            return
        }
        parent?.also { parentView ->
            var maxOffset = flexNode.layoutFrame.height - parentView.flexNode.layoutFrame.height
            var currentOffset = offsetY
            if (isRowFlexDirection()) {
                maxOffset = flexNode.layoutFrame.width - parentView.flexNode.layoutFrame.width
                currentOffset = offsetX
            }
            if (maxOffset < 0f) {
                maxOffset = 0f
            }
            if (currentOffset >= 0 && currentOffset > maxOffset) {
                if (isRowFlexDirection()) {
                    offsetX = maxOffset
                } else {
                    offsetY = maxOffset
                }
                (parentView as ScrollerView).setContentOffset(offsetX, offsetY, false)
            }
        }
    }

    open fun updateChildLayout() {
        if (!didFirstLayout) {
            return
        }
        getPager().addNextTickTask {
            needLayoutChildren().forEach { it.flexNode.markDirty() }
            createRenderViewsOnVisibleRect()
        }
    }

    companion object {
        const val SCROLL_INDEX = "scrollIndex"
    }
}
