/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.isVirtualView
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.MAX_ITEM_COUNT
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.cleanNextScrollToParams
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.logInfo
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.pager.IPagerLayoutEventObserver
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.reactive.collection.CollectionOperation
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.IScrollerViewEventObserver
import com.tencent.kuikly.core.views.ListContentView
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.ScrollParams
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class LazyLoopDirectivesView<T>(
    private val itemList: () -> ObservableList<T>,
    private val maxLoadItem: Int,
    private val itemCreator: LazyLoopDirectivesView<T>.(item: T, index: Int, count: Int) -> Unit
) :
    DirectivesView(), IScrollerViewEventObserver, IPagerLayoutEventObserver {

    companion object {
        const val MAX_ITEM_COUNT = 30
        private const val UPDATE_ITEM_THRESHOLD = 2
        private const val INVALID_DIMENSION = -1f
        private const val BEFORE_COUNT = 1
        private const val AFTER_COUNT = 1
        private const val INVALID_POSITION = -1
        private const val DEFAULT_ITEM_SIZE = 100f
        private const val KEY_NEXT_SCROLL_TO_PARAMS = "lazy_loop_next_scroll_to_params"
        private const val DEBUG_LOG = false
        private const val TAG = "LazyLoop"

        private fun ListView<*, *>.setNextScrollToParams(params: ScrollToParams) {
            extProps[KEY_NEXT_SCROLL_TO_PARAMS] = params
        }

        private fun ListView<*, *>.getNextScrollToParams(): ScrollToParams? {
            return extProps[KEY_NEXT_SCROLL_TO_PARAMS] as? ScrollToParams
        }

        fun ListView<*, *>.cleanNextScrollToParams() {
            extProps.remove(KEY_NEXT_SCROLL_TO_PARAMS)
        }

        internal inline fun logInfo(msg: String) {
            if (DEBUG_LOG) {
                KLog.i(TAG, msg)
            }
        }
    }

    private class ScrollToParams(
        val index: Int,
        val offsetExt: Float,
        val animate: Boolean
    )

    private class ScrollOffsetCorrectInfo(
        val position: Int,
        val offset: Float,
        val size: Float
    )

    private class WaitToApplyState(
        var contentOffset: Boolean,
        var scrollEnd: Boolean
    )

    private lateinit var curList: ObservableList<T>
    private var startSize: Float = 0f
    private var endSize: Float = INVALID_DIMENSION

    // 头部占位View
    private lateinit var itemStart: DivView

    // 尾部占位View
    private lateinit var itemEnd: DivView
    private var currentStart: Int = 0
    private var currentEnd: Int = 0
    private var avgItemSize: Float = DEFAULT_ITEM_SIZE

    private var listView: ListView<*, *>? = null
    private var listViewContent: ListContentView? = null

    private val waitToApplyState = WaitToApplyState(contentOffset = false, scrollEnd = false)

    // 对齐位置的锚点
    private var scrollOffsetCorrectInfo: ScrollOffsetCorrectInfo? = null

    private val Frame.start
        get() = if (isRowDirection()) x else y

    private val Frame.end
        get() = if (isRowDirection()) maxX() else maxY()

    private val Frame.size
        get() = if (isRowDirection()) width else height

    private val FlexNode.range
        get() = if (isRowDirection()) {
            getMargin(StyleSpace.Type.LEFT) + layoutFrame.width + getMargin(StyleSpace.Type.RIGHT)
        } else {
            getMargin(StyleSpace.Type.TOP) + layoutFrame.height + getMargin(StyleSpace.Type.BOTTOM)
        }

    private val hairWidth: Float by lazy(LazyThreadSafetyMode.NONE) {
        1f / getPager().pageData.density
    }

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        listViewContent = parent as? ListContentView ?: throw RuntimeException("vforLazy必须是List子节点")
        listView = listViewContent!!.parent as? ListView<*, *> ?: throw RuntimeException("vforLazy必须是List子节点")
        listView?.addScrollerViewEventObserver(this)
        getPager().addPagerLayoutEventObserver(this)
    }

    override fun willRemoveFromParentView() {
        getPager().removePagerLayoutEventObserver(this)
        listView?.removeScrollerViewEventObserver(this)
        listView = null
        listViewContent = null
        super.willRemoveFromParentView()
    }

    override fun didInit() {
        super.didInit()
        val ctx = this
        // 头部占位View
        addChild(DivView().also { itemStart = it }) {}
        ReactiveObserver.bindValueChange(this) {
            val list = itemList()
            if (!ctx::curList.isInitialized || list != curList) { // 全量更新
                ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                    // remove old list all Element
                    if (ctx::curList.isInitialized && currentEnd - currentStart > 0) {
                        syncRemoveChildOperationToDom(
                            CollectionOperation(
                                CollectionOperation.OPERATION_TYPE_REMOVE,
                                currentStart,
                                currentEnd - currentStart
                            )
                        )
                    }

                    curList = list
                    // 先根据原先的currentStart确定currentEnd
                    if (currentStart + maxLoadItem >= list.count()) {
                        currentEnd = list.count()
                        endSize = 0f
                    } else {
                        currentEnd = currentStart + maxLoadItem
                        endSize = INVALID_DIMENSION
                    }
                    // 再根据新的currentEnd确定currentStart
                    if (currentEnd - maxLoadItem <= 0) {
                        currentStart = 0
                        startSize = 0f
                    } else {
                        currentStart = currentEnd - maxLoadItem
                        startSize = INVALID_DIMENSION
                    }
                    syncAddChildOperationToDom(
                        CollectionOperation(
                            CollectionOperation.OPERATION_TYPE_ADD,
                            currentStart,
                            currentEnd
                        ), list
                    )
                    updatePadding(true)
                    updatePadding(false)
                }
            } else { // 增量更新
                val collectionOperation = list.collectionOperation.toFastList()
                ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                    if (collectionOperation.isNotEmpty()) {
                        collectionOperation.forEach { operation ->
                            syncListOperationToDom(operation)
                        }
                    }
                }
            }
        }
        // 尾部占位View
        addChild(DivView().also { itemEnd = it }) {
            ctx.updatePadding(false)
        }
    }

    override fun scrollerScrollDidEnd(params: ScrollParams) {
        logInfo("handleScrollEnd")
        if (needWaitLayout()) {
            registerAfterLayoutTaskFor(scrollEnd = true)
        } else {
            correctScrollOffsetInScrollEnd(if (isRowDirection()) params.offsetX else params.offsetY)
        }
    }

    private fun syncListOperationToDom(operation: CollectionOperation) {
        if (realParent === null) {
            return
        }
        var needRefreshRange = false
        if (operation.isAddOperation()) {
            if (operation.index < currentStart) { // add到前面
                currentStart += operation.count
                currentEnd += operation.count
                startSize = INVALID_DIMENSION
                updatePadding(true)
                needRefreshRange = true
            } else if (operation.index < currentEnd) { // add到中间
                val addCount = min(currentStart + maxLoadItem - operation.index, operation.count)
                val removeCount = max(0, currentEnd - currentStart + addCount - maxLoadItem)
                currentEnd += addCount - removeCount
                if (addCount != operation.count) {
                    endSize = INVALID_DIMENSION
                }
                syncAddChildOperationToDom(
                    CollectionOperation(
                        CollectionOperation.OPERATION_TYPE_ADD,
                        operation.index,
                        addCount
                    ), curList
                )
                val removedSize = syncRemoveChildOperationToDom(
                    CollectionOperation(
                        CollectionOperation.OPERATION_TYPE_REMOVE,
                        currentEnd,
                        removeCount
                    )
                )
                if (endSize != INVALID_DIMENSION && removedSize != INVALID_DIMENSION) {
                    endSize += removedSize
                }
                updatePadding(false)
            } else { // add到后面
                endSize = INVALID_DIMENSION
                updatePadding(false)
                needRefreshRange = true
            }
        } else if (operation.isRemoveOperation()) {
            if (operation.index + operation.count < currentStart) { // remove前面
                currentStart -= operation.count
                currentEnd -= operation.count
                startSize = if (currentStart == 0) 0f else INVALID_DIMENSION
                updatePadding(true)
            } else if (operation.index < currentEnd) { // remove中间
                val newStart: Int
                val removeStart: Int
                if (operation.index < currentStart) {
                    removeStart = currentStart
                    newStart = operation.index
                    startSize = if (newStart == 0) 0f else INVALID_DIMENSION
                } else {
                    removeStart = operation.index
                    newStart = currentStart
                }
                val newEnd: Int
                val removeEnd: Int
                if (operation.index + operation.count <= currentEnd) {
                    removeEnd = operation.index + operation.count
                    newEnd = currentEnd - operation.count
                } else {
                    removeEnd = currentEnd
                    newEnd = operation.index
                    endSize = if (newEnd == curList.size) 0f else INVALID_DIMENSION
                }
                syncRemoveChildOperationToDom(
                    CollectionOperation(
                        CollectionOperation.OPERATION_TYPE_REMOVE,
                        removeStart,
                        removeEnd - removeStart
                    )
                )
                currentStart = newStart
                currentEnd = newEnd
                if (removeStart != operation.index) {
                    updatePadding(true)
                }
                if (removeEnd != operation.index + operation.count) {
                    updatePadding(false)
                }
                needRefreshRange = true
            } else { // remove后面
                endSize = if (currentEnd == curList.size) 0f else INVALID_DIMENSION
                updatePadding(false)
            }
        }
        if (needRefreshRange) {
            registerAfterLayoutTaskFor(contentOffset = true)
        }
    }

    private fun syncAddChildOperationToDom(operation: CollectionOperation, list: List<T>) {
        val addedChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
        var index = operation.index
        val size = list.size
        while (index < operation.index + operation.count) {
            if (!(index < list.size && index >= 0)) {
                KLog.e(
                    "KuiklyError",
                    "sync add operation out index with index:${index} listSize:${list.size} oIndex:${operation.index} oSize:${operation.count} "
                )
                break
            }
            val item = list[index]
            val child = createChildView(item, index, size)
            children.add(index - currentStart + BEFORE_COUNT, child)
            addedChildren.add(child)
            index++
        }
        realParent?.also { parent ->
            val domChildren = parent.domChildren()
            addedChildren.forEach { child ->
                val insertIndex = domChildren.lastIndexOf(child)
                parent.insertDomSubView(child, insertIndex)
            }
        }
    }

    private fun syncRemoveChildOperationToDom(operation: CollectionOperation): Float {
        var index = operation.index
        val removedChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
        while (index < operation.index + operation.count) {
            val childIndex = index - currentStart + BEFORE_COUNT
            if (!(childIndex < children.size && childIndex >= 0)) {
                KLog.e(
                    "KuiklyError",
                    "sync remove operation out index with index:${index} listSize:${children.size} oIndex:${operation.index} oSize:${operation.count} "
                )
                break
            }
            removedChildren.add(children[childIndex])
            index++
        }
        realParent?.also { parent ->
            removedChildren.forEach { child ->
                parent.removeDomSubView(child)
                removeChild(child)
            }
        }
        var removedSize = 0f
        val isRow = isRowDirection()
        for (child in removedChildren) {
            val frame = child.frame
            if (frame.isDefaultValue()) {
                removedSize = INVALID_DIMENSION
                break
            }
            removedSize += if (isRow) {
                child.flexNode.getMargin(StyleSpace.Type.LEFT) + frame.width + child.flexNode.getMargin(StyleSpace.Type.RIGHT)
            } else {
                child.flexNode.getMargin(StyleSpace.Type.TOP) + frame.height + child.flexNode.getMargin(StyleSpace.Type.BOTTOM)
            }
        }
        return removedSize
    }

    private fun isRowDirection(): Boolean {
        return listViewContent?.isRowFlexDirection() ?: false
    }

    private fun currentListOffset(): Float {
        return listView?.let { if (isRowDirection()) it.curOffsetX else it.curOffsetY } ?: 0f
    }

    private fun createChildView(item: T, index: Int, size: Int): DeclarativeBaseView<*, *> {
        val beforeChildrenSize = children.count()
        itemCreator(item, index, size)
        if (children.count() - beforeChildrenSize != 1) {
            throwRuntimeError("vfor creator闭包内必须需要且仅一个孩子节点的生成")
        }
        val child = children.last()
        if (child.isVirtualView()) {
            throwRuntimeError("vfor creator闭包内子孩子必须为非条件指令，如vif , vfor")
        }
        children.remove(child)
        return child
    }

    override fun onContentOffsetDidChanged(contentOffsetX: Float, contentOffsetY: Float, params: ScrollParams) {
        val contentOffset = if (isRowDirection()) contentOffsetX else contentOffsetY
        // KLog.e("pel", "contentOffsetY=$contentOffsetY paramsHeight=${params.contentHeight} frameHeight=${listViewContent?.frame?.height}")
        if (needWaitLayout()) {
            registerAfterLayoutTaskFor(contentOffset = true, scrollEnd = false)
        } else {
            handleOnContentOffsetDidChanged(contentOffset)
        }
    }

    private fun handleOnContentOffsetDidChanged(contentOffset: Float) {
        createItemByOffset(contentOffset)

        if (currentStart == 0 && itemStart.frame.size > 0f && itemStart.frame.end >= contentOffset) {
            logInfo("onContentOffsetDidChanged reach top ${itemStart.frame.end} contentOffset=$contentOffset")
            listView?.cleanNextScrollToParams()
            scrollOffsetCorrectInfo = null
            listView?.abortContentOffsetAnimate()
            correctScrollOffsetInScrollEnd(contentOffset)
        }
    }

    internal fun createItemByOffset(contentOffset: Float) {
        val firstVisibleChildIndex = children.indexOfFirst {
            it.frame.let { frame -> !frame.isDefaultValue() && contentOffset <= frame.end }
        }

        val itemPosition: Int = when {
            firstVisibleChildIndex == -1 -> {
                // offset greater than last child, result to position end
                curList.size - 1
            }

            firstVisibleChildIndex < BEFORE_COUNT -> {
                val offsetRatio = (contentOffset - itemStart.frame.start) / itemStart.frame.size
                round(offsetRatio * currentStart).toInt()
            }

            firstVisibleChildIndex < children.size - AFTER_COUNT -> {
                currentStart + firstVisibleChildIndex
            }

            else -> {
                val offsetRatio = (contentOffset - itemEnd.frame.start) / itemEnd.frame.size
                round(offsetRatio * (curList.size - currentEnd)).toInt() + currentEnd
            }
        }

        createItemByPosition(itemPosition, contentOffset)
    }

    private fun createItemByPosition(position: Int, contentOffset: Float) {
        val curListSize = curList.size
        // assume the middle 1/3 items show
        val newStart = max(0, min(position - maxLoadItem / 3, curListSize - maxLoadItem))
        val newEnd = min(newStart + maxLoadItem, curListSize)
        if (shouldSkipUpdate(newStart, newEnd)) {
            return
        }
        logInfo("createItemByPosition position=$position [$currentStart-$currentEnd]->[$newStart-$newEnd] offset=$contentOffset")

        if (newStart >= currentEnd || newEnd <= currentStart) {
            // assume removed size, to keep scroll range
            // val removedSize = (newStart - currentStart) * avgItemSize
            // remove all
            syncRemoveChildOperationToDom(
                CollectionOperation(
                    CollectionOperation.OPERATION_TYPE_REMOVE,
                    currentStart,
                    currentEnd - currentStart
                )
            )
            startSize = if (newStart == 0) 0f else INVALID_DIMENSION
            endSize = if (newEnd == curListSize) 0f else INVALID_DIMENSION
            currentStart = newStart
            currentEnd = newEnd
            syncAddChildOperationToDom(
                CollectionOperation(
                    CollectionOperation.OPERATION_TYPE_ADD,
                    newStart,
                    newEnd - newStart
                ), curList
            )
            val maxScrollOffset = max(0f, listViewContent!!.frame.size - listView!!.frame.size)
            scrollOffsetCorrectInfo = if (maxScrollOffset - contentOffset < 1f) {
                // update anchor
                ScrollOffsetCorrectInfo(
                    INVALID_POSITION,
                    INVALID_DIMENSION,
                    itemEnd.frame.end - itemStart.frame.start
                )
            } else {
                ScrollOffsetCorrectInfo(
                    position,
                    contentOffset - itemStart.frame.start,
                    INVALID_DIMENSION
                )
            }
            updatePadding(true)
            updatePadding(false)
        } else {
            // space before
            if (newStart < currentStart) {
                // update anchor
                scrollOffsetCorrectInfo = ScrollOffsetCorrectInfo(
                    currentStart,
                    children[BEFORE_COUNT].frame.start - itemStart.frame.start,
                    INVALID_DIMENSION
                )
                // add item
                startSize = INVALID_DIMENSION
                val operation =
                    CollectionOperation(CollectionOperation.OPERATION_TYPE_ADD, newStart, currentStart - newStart)
                currentStart = newStart
                syncAddChildOperationToDom(operation, curList)
                if (newStart == 0) {
                    startSize = 0f
                    updatePadding(true)
                    scrollOffsetCorrectInfo = null
                }
            } else if (newStart > currentStart) {
                // remove item
                val removedSize = syncRemoveChildOperationToDom(
                    CollectionOperation(
                        CollectionOperation.OPERATION_TYPE_REMOVE,
                        currentStart,
                        newStart - currentStart
                    )
                )
                if (newStart == 0) {
                    startSize = 0f
                } else if (removedSize >= 0 && startSize >= 0f) {
                    startSize += removedSize
                } else {
                    startSize = INVALID_DIMENSION
                }
                currentStart = newStart
                updatePadding(true, removedSize)
            }
            // space after
            if (newEnd < currentEnd) {
                // remove item
                val removedSize = syncRemoveChildOperationToDom(
                    CollectionOperation(
                        CollectionOperation.OPERATION_TYPE_REMOVE,
                        newEnd,
                        currentEnd - newEnd
                    )
                )
                if (newEnd == curListSize) {
                    endSize = 0f
                } else if (removedSize >= 0 && endSize >= 0f) {
                    endSize += removedSize
                } else {
                    endSize = INVALID_DIMENSION
                }
                currentEnd = newEnd
                updatePadding(false)
            } else if (newEnd > currentEnd) {
                // add item
                endSize = INVALID_DIMENSION
                val operation =
                    CollectionOperation(CollectionOperation.OPERATION_TYPE_ADD, currentEnd, newEnd - currentEnd)
                currentEnd = newEnd
                syncAddChildOperationToDom(operation, curList)
                updatePadding(false)
            }
        }
    }

    private fun shouldSkipUpdate(newStart: Int, newEnd: Int): Boolean {
        if (newStart != currentStart && newStart == 0) {
            // 移动到最头部，需要更新
            return false
        }
        if (abs(newStart - currentStart) > UPDATE_ITEM_THRESHOLD) {
            // 避免UI抖动，移动范围达到阈值才更新
            return false
        }
        if (abs(newEnd - currentEnd) > UPDATE_ITEM_THRESHOLD) {
            // 避免UI抖动，移动范围达到阈值才更新
            return false
        }
        if (newEnd != currentEnd && newEnd == curList.size) {
            // 移动到最尾部，需要更新
            return false
        }
        return true
    }

    override fun subViewsDidLayout() {
    }

    private fun correctScrollOffsetInScrollEnd(offset: Float) {
        val isRow = isRowDirection()
        val correctSize = if (startSize >= 0f) startSize else avgItemSize * currentStart
        val currentSize = itemStart.frame.size
        val diff = correctSize - currentSize
        if (diff != 0f) {
            logInfo("correctScrollOffset $currentSize->$correctSize size")
            setItemStartSize(correctSize)
            if (offset > itemStart.frame.start) {
                val toOffset = max(0f, offset + diff)
                logInfo("correctScrollOffset offset=$offset diff=$diff scroll")
                listView?.apply {
                    if (isRow) {
                        // 提前更新ListContentView.offsetX, 避免ListView重复修改item的RenderView闪烁
                        this@LazyLoopDirectivesView.listViewContent?.offsetX = toOffset
                        setContentOffset(toOffset, 0f)
                    } else {
                        // 提前更新ListContentView.offsetY, 避免ListView重复修改item的RenderView闪烁
                        this@LazyLoopDirectivesView.listViewContent?.offsetY = toOffset
                        setContentOffset(0f, toOffset)
                    }
                }
            }
        }
    }

    private fun correctScrollOffsetInLayout(index: Int, offset: Float, size: Float): Boolean {
        // 由于onPagerCalculateLayoutFinish回调不保证顺序，这里无法确定ListView的排版流程是否结束，
        // 因此只能读取frame的width/height，但需要更新frame的x/y。
        val isRow = isRowDirection()
        val currentSize = itemStart.frame.size
        val correctSize = if (size != INVALID_DIMENSION) {
            var range = 0f
            for (i in 0 until currentEnd - currentStart) {
                val child = children[BEFORE_COUNT + i]
                val node = child.flexNode
                if (!node.layoutFrame.isDefaultValue() && !child.absoluteFlexNode) {
                    range += node.range
                }
            }
            max(0f, size - range - itemEnd.frame.size)
        } else if (index in currentStart until currentEnd) {
            var range = 0f
            for (i in 0 until index - currentStart) {
                val child = children[BEFORE_COUNT + i]
                val node = child.flexNode
                if (!node.layoutFrame.isDefaultValue() && !child.absoluteFlexNode) {
                    range += node.range
                }
            }
            max(0f, offset - range)
        } else {
            logInfo("correctScrollOffset failed $index $offset")
            return false
        }

        val diff = correctSize - currentSize
        if (diff != 0f) {
            logInfo("correctScrollOffset $index $offset $size diff=$diff newSize=$correctSize layout")
            if (isRow) {
                itemStart.flexNode.also { node ->
                    node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.width = correctSize; it.toFrame() })
                }
                val startOffset = itemStart.frame.x
                listViewContent?.domChildren()?.forEach { child ->
                    val node = child.flexNode
                    if (!node.layoutFrame.isDefaultValue() && !child.absoluteFlexNode && node.layoutFrame.x > startOffset) {
                        node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.x += diff; it.toFrame() })
                    }
                }
                listViewContent?.flexNode?.also { node ->
                    node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.width += diff; it.toFrame() })
                }
            } else {
                itemStart.flexNode.also { node ->
                    node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.height = correctSize; it.toFrame() })
                }
                val startOffset = itemStart.frame.y
                listViewContent?.domChildren()?.forEach { child ->
                    val node = child.flexNode
                    if (!node.layoutFrame.isDefaultValue() && !child.absoluteFlexNode && node.layoutFrame.y > startOffset) {
                        node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.y += diff; it.toFrame() })
                    }
                }
                listViewContent?.flexNode?.also { node ->
                    node.updateLayoutFrame(node.layoutFrame.toMutableFrame().let { it.height += diff; it.toFrame() })
                }
            }
        }
        return true
    }

    private fun updatePadding(before: Boolean, removedSize: Float = INVALID_DIMENSION) {
        if (before && ::itemStart.isInitialized) {
            val newSize = when {
                startSize >= 0f -> startSize
                removedSize != INVALID_DIMENSION -> itemStart.frame.size + removedSize
                else -> return
            }
            setItemStartSize(newSize)
        } else if (::itemEnd.isInitialized) {
            setItemEndSize(if (endSize >= 0f) endSize else avgItemSize * (curList.size - currentEnd))
        }
    }

    private fun setItemStartSize(size: Float) {
        if (isRowDirection()) {
            itemStart.getViewAttr().width(size)
        } else {
            itemStart.getViewAttr().height(size)
        }
        // we modified itemStart.frame, so markDirty() required here
        itemStart.flexNode.markDirty()
    }

    private fun setItemEndSize(size: Float) {
        if (isRowDirection()) {
            itemEnd.getViewAttr().width(size)
        } else {
            itemEnd.getViewAttr().height(size)
        }
    }

    fun getItemCount() = curList.size

    internal fun scrollToPosition(index: Int, offset: Float, animate: Boolean) {
        if (itemEnd.flexNode.layoutFrame.isDefaultValue()) {
            logInfo("layout not finished, delay scroll")
            listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
            return
        }
        listView?.cleanNextScrollToParams()
        scrollOffsetCorrectInfo = null
        val isRow = isRowDirection()
        val preScrollDistance =
            if (animate) getPager().pageData.let { if (isRow) it.pageViewWidth else it.pageViewHeight } else 0f
        if (index < currentStart) { // scroll up
            // handle scroll up
            listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
            val ratio = index / currentStart.toFloat()
            val estimateOffset = itemStart.frame.start + itemStart.frame.size * ratio + offset
            val finalOffset = min(estimateOffset + preScrollDistance, currentListOffset())
            if (isRow) {
                listViewContent?.offsetX = finalOffset
                listView?.setContentOffset(finalOffset, 0f, false)
            } else {
                listViewContent?.offsetY = finalOffset
                listView?.setContentOffset(0f, finalOffset, false)
            }
            createItemByOffset(finalOffset)
        } else if (index < currentEnd) {
            val frame = children[BEFORE_COUNT + (index - currentStart)].frame
            // 减hairWidth(1像素)，防止size超过Float精度后，setContentOffset不满足执行条件
            val maxScrollOffset = max(0f, listViewContent!!.frame.size - listView!!.frame.size - hairWidth)
            val finalOffset = max(0f, min(frame.start + offset, maxScrollOffset))
            if (isRow) {
                listView!!.setContentOffset(finalOffset, 0f, animate)
            } else {
                listView!!.setContentOffset(0f, finalOffset, animate)
            }
        } else { // scroll down
            listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
            // 减hairWidth(1像素)，防止size超过Float精度后，setContentOffset不满足执行条件
            val maxScrollOffset = max(0f, listViewContent!!.frame.size - listView!!.frame.size - hairWidth)
            val ratio = (index - currentEnd) / (curList.size - currentEnd).toFloat()
            val estimateOffset = min(itemEnd.frame.start + itemEnd.frame.size * ratio, maxScrollOffset)
            val finalOffset = max(estimateOffset - preScrollDistance, currentListOffset())
            if (isRow) {
                listViewContent?.offsetX = finalOffset
                listView?.setContentOffset(finalOffset, 0f, false)
            } else {
                listViewContent?.offsetY = finalOffset
                listView?.setContentOffset(0f, finalOffset, false)
            }
            createItemByOffset(finalOffset)
        }
    }

    override fun onPagerWillCalculateLayoutFinish() {
    }

    override fun onPagerCalculateLayoutFinish() {
        if (scrollOffsetCorrectInfo != null) {
            val info = scrollOffsetCorrectInfo!!
            scrollOffsetCorrectInfo = null
            correctScrollOffsetInLayout(info.position, info.offset, info.size)
        }
    }

    override fun onPagerDidLayout() {
        if (itemEnd.flexNode.layoutFrame.isDefaultValue()) {
            logInfo("layout not finished, skip update")
            return
        }
        avgItemSize = if (currentEnd > currentStart) {
            (itemEnd.frame.start - itemStart.frame.end) / (currentEnd - currentStart)
        } else {
            DEFAULT_ITEM_SIZE
        }
        logInfo("avgItemSize=$avgItemSize")
        if (listView?.getNextScrollToParams() != null) {
            getPager().addNextTickTask {
                val params = listView?.getNextScrollToParams()
                if (params != null) {
                    listView?.cleanNextScrollToParams()
                    scrollToPosition(params.index, params.offsetExt, params.animate)
                }
            }
        }
    }

    private fun needWaitLayout() = listViewContent?.flexNode?.isDirty == true

    private fun registerAfterLayoutTaskFor(contentOffset: Boolean? = null, scrollEnd: Boolean? = null) {
        if (!(waitToApplyState.contentOffset || waitToApplyState.scrollEnd) && (contentOffset == true || scrollEnd == true)) {
            getPager().addTaskWhenPagerUpdateLayoutFinish {
                if (waitToApplyState.contentOffset) {
                    handleOnContentOffsetDidChanged(currentListOffset())
                    waitToApplyState.contentOffset = false
                }
                if (waitToApplyState.scrollEnd) {
                    correctScrollOffsetInScrollEnd(currentListOffset())
                    waitToApplyState.scrollEnd = false
                }
            }
        }
        if (contentOffset != null) {
            waitToApplyState.contentOffset = contentOffset
        }
        if (scrollEnd != null) {
            waitToApplyState.scrollEnd = scrollEnd
        }
    }

}

fun <T> ListView<*, *>.vforLazy(
    itemList: () -> ObservableList<T>,
    maxLoadItem: Int = MAX_ITEM_COUNT,
    itemCreator: LazyLoopDirectivesView<T>.(item: T, index: Int, count: Int) -> Unit
) {
    addChild(LazyLoopDirectivesView(itemList, maxLoadItem, itemCreator)) {}
}

fun ListView<*, *>.scrollToPosition(index: Int, offset: Float = 0f, animate: Boolean = false) {
    logInfo("scrollToPosition index=$index $offset $animate")
    val listViewContent = getChild(0) as ListContentView
    val templateChildren = listViewContent.templateChildren()
    var skip = 0
    cleanNextScrollToParams()
    for (child in templateChildren) {
        if (child is LazyLoopDirectivesView<*>) {
            val itemCount = child.getItemCount()
            if (itemCount > index - skip) {
                child.scrollToPosition(index - skip, offset, animate)
                return
            } else {
                skip += itemCount
            }
        } else if (child.isVirtualView()) {
            val domChildren = (child as ViewContainer).domChildren()
            if (domChildren.size > index - skip) {
                val frame = domChildren[index - skip].frame
                doScroll(this, listViewContent, frame, offset, animate, templateChildren)
                return
            } else {
                skip += domChildren.size
            }
        } else {
            if (index - skip == 0) {
                val frame = child.frame
                doScroll(this, listViewContent, frame, offset, animate, templateChildren)
                return
            } else {
                ++skip
            }
        }
    }
    logInfo("can't scroll position invalid")
}

/**
 * 滚动到frame对应的位置
 */
private fun doScroll(
    list: ListView<*, *>,
    content: ListContentView,
    frame: Frame,
    offsetExt: Float,
    animate: Boolean,
    templateChildren: List<DeclarativeBaseView<*, *>>
) {
    if (!frame.isDefaultValue()) {
        val isRow = content.isRowFlexDirection()
        val finalOffset = if (isRow) {
            max(0f, min(frame.x + offsetExt, content.frame.width - list.frame.width))
        } else {
            max(0f, min(frame.y + offsetExt, content.frame.height - list.frame.height))
        }
        if (isRow) {
            list.setContentOffset(finalOffset, 0f, animate)
        } else {
            list.setContentOffset(0f, finalOffset, animate)
        }
        if (!animate) {
            if (isRow) {
                content.offsetX = finalOffset
            } else {
                content.offsetY = finalOffset
            }
            for (child in templateChildren) {
                if (child is LazyLoopDirectivesView<*>) {
                    child.createItemByOffset(finalOffset)
                }
            }
        }
    } else {
        logInfo("can't scroll, frame is zero")
    }
}
