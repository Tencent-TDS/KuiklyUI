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

package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.isVirtualView
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.MAX_ITEM_COUNT
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
import kotlin.math.ceil
import kotlin.math.floor
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
        private const val KEY_SCROLL_EVENT_FILTER_RULE = "lazy_loop_scroll_event_filter_rule"
        private const val FILTER_TIMEOUT = 2000L
        private const val DEBUG_LOG = false
        private const val TAG = "LazyLoop"

        private fun ListView<*, *>.setNextScrollToParams(params: ScrollToParams) {
            extProps[KEY_NEXT_SCROLL_TO_PARAMS] = params
        }

        private fun ListView<*, *>.getNextScrollToParams(): ScrollToParams? {
            return extProps[KEY_NEXT_SCROLL_TO_PARAMS] as? ScrollToParams
        }

        internal fun ListView<*, *>.cleanNextScrollToParams() {
            extProps.remove(KEY_NEXT_SCROLL_TO_PARAMS)
        }

        internal fun ListView<*, *>.setScrollEventFilterRule(
            from: Float,
            to: Float,
            animate: Boolean
        ) {
            logInfo("setScrollEventFilterRule from=$from to=$to animate=$animate")
            // TODO: 由于不确定滚动偏移量是否有误差，这里加上10f的经验值，待优化
            extProps[KEY_SCROLL_EVENT_FILTER_RULE] = if (animate) {
                Triple(
                    floor(if (from < to) from else to - 10f),
                    ceil(if (from < to) to + 10f else from),
                    DateTime.currentTimestamp() + FILTER_TIMEOUT
                )
            } else {
                Triple(
                    floor(to - 10f),
                    ceil(to + 10f),
                    DateTime.currentTimestamp() + FILTER_TIMEOUT
                )
            }
        }

        /**
         * 过滤scroll事件，解决调用setContentOffset后，异步触发的滚动事件和跳转逻辑冲突
         * @return true表示过滤掉滚动事件，false表示不过滤
         */
        internal fun ListView<*, *>.verifyScrollEventFilterRule(offset: Float): Boolean {
            val (from, to, timestamp) = extProps[KEY_SCROLL_EVENT_FILTER_RULE] as? Triple<*, *, *>
                ?: return false
            if ((timestamp as Long) < DateTime.currentTimestamp()) {
                extProps.remove(KEY_SCROLL_EVENT_FILTER_RULE)
                return false
            }
            if ((from as Float) <= offset && offset <= (to as Float)) {
                logInfo("scroll in range, remove rule")
                extProps.remove(KEY_SCROLL_EVENT_FILTER_RULE)
                return false
            }
            return true
        }

        internal inline fun needScroll(currentOffset: Float, finalOffset: Float): Boolean {
            return abs(currentOffset - finalOffset) >= 1f
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

    private val FlexNode.start
        get() = if (isRowDirection()) {
            layoutFrame.x - getMargin(StyleSpace.Type.LEFT)
        } else {
            layoutFrame.y - getMargin(StyleSpace.Type.TOP)
        }

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
        waitToApplyState.contentOffset = false
        waitToApplyState.scrollEnd = false
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
        val offset = if (isRowDirection()) params.offsetX else params.offsetY
        if (listView!!.verifyScrollEventFilterRule(offset)) {
            logInfo("scrollDidEnd filtered offset=$offset")
            return
        }
        logInfo("handleScrollEnd offset=$offset")
        if (needWaitLayout()) {
            registerAfterLayoutTaskFor(scrollEnd = true)
        } else {
            correctScrollOffsetInScrollEnd(offset)
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
            } else if (operation.index <= currentEnd) { // add到中间
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
        if (operation.count == 0) {
            return
        }
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
        if (operation.count == 0) {
            return 0f
        }
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
        // return listView?.let { if (isRowDirection()) it.curOffsetX else it.curOffsetY } ?: 0f
        return listViewContent?.let { if (isRowDirection()) it.offsetX else it.offsetY } ?: 0f
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
        if (listView!!.verifyScrollEventFilterRule(contentOffset)) {
            logInfo("scroll filtered offset=$contentOffset")
            return
        }
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
            it.frame.let { frame -> frame.size > 0f && contentOffset <= frame.end }
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
                currentStart + firstVisibleChildIndex - BEFORE_COUNT
            }

            else -> {
                val offsetRatio = (contentOffset - itemEnd.frame.start) / itemEnd.frame.size
                round(offsetRatio * (curList.size - currentEnd)).toInt() + currentEnd
            }
        }

        createItemByPosition(itemPosition, contentOffset, true)
    }

    /**
     * 在指定位置创建或更新子节点
     * @param position 目标子节点位置
     * @param offset 目标子节点偏移量
     * @param scrolling 是否正在滚动中，true表示调用来自列表滚动，偏移修正的目标是内容连贯；false表示调用来自跳转
     * API，偏移修正的目标是跳转位置准确
     */
    private fun createItemByPosition(position: Int, offset: Float, scrolling: Boolean) {
        val curListSize = curList.size
        // assume the middle 1/3 items show
        val newStart = max(0, min(position - maxLoadItem / 3, curListSize - maxLoadItem))
        val newEnd = min(newStart + maxLoadItem, curListSize)
        if (scrolling && shouldSkipUpdate(newStart, newEnd)) {
            return
        }
        logInfo("createItemByPosition position=$position [$currentStart-$currentEnd]->[$newStart-$newEnd] offset=$offset scrolling=$scrolling")

        if (newStart >= currentEnd || newEnd <= currentStart) {
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
            scrollOffsetCorrectInfo = scrollOffsetCorrectInfoOf(position, offset)
            // updatePadding(true)
            updatePadding(false)
        } else {
            // space before
            if (newStart < currentStart) {
                // update anchor
                scrollOffsetCorrectInfo = ScrollOffsetCorrectInfo(
                    currentStart,
                    children[BEFORE_COUNT].flexNode.start - itemStart.frame.start,
                    INVALID_DIMENSION
                )
                // add item
                startSize = if (newStart == 0) 0f else INVALID_DIMENSION
                val operation =
                    CollectionOperation(CollectionOperation.OPERATION_TYPE_ADD, newStart, currentStart - newStart)
                currentStart = newStart
                syncAddChildOperationToDom(operation, curList)
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
            if (!scrolling) {
                scrollOffsetCorrectInfo = scrollOffsetCorrectInfoOf(position, offset)
                // 来自API的调用可能没有节点变动，主动markDirty确保触发Layout
                listViewContent?.flexNode?.markDirty()
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

    private fun scrollOffsetCorrectInfoOf(
        position: Int,
        offset: Float
    ) = if (position == curList.size - 1) {
        // 目标位置是最后一个元素，说明滚动到列表底部，偏移修正方式为维持ListContentView的大小不变
        ScrollOffsetCorrectInfo(
            INVALID_POSITION,
            INVALID_DIMENSION,
            itemEnd.frame.end - itemStart.frame.start
        )
    } else {
        ScrollOffsetCorrectInfo(
            position,
            offset - itemStart.frame.start,
            INVALID_DIMENSION
        )
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
                    if (child == itemStart || child.absoluteFlexNode) {
                        return@forEach
                    }
                    val node = child.flexNode
                    if (!node.layoutFrame.isDefaultValue() && node.layoutFrame.x >= startOffset) {
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
                    if (child == itemStart || child.absoluteFlexNode) {
                        return@forEach
                    }
                    val node = child.flexNode
                    if (!node.layoutFrame.isDefaultValue() && node.layoutFrame.y >= startOffset) {
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
        if (listViewContent?.flexNode?.isDirty != false || itemEnd.flexNode.layoutFrame.isDefaultValue()) {
            logInfo("layout not finished, delay scroll")
            listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
            return
        }
        listView?.cleanNextScrollToParams()
        scrollOffsetCorrectInfo = null
        val isRow = isRowDirection()
        if (index < currentStart) { // scroll up
            // handle scroll up
            val ratio = index / currentStart.toFloat()
            val estimateOffset = itemStart.frame.start + itemStart.frame.size * ratio + offset

            if (animate) {
                listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
                val (finalIndex, finalOffset) = calculateAnimateScrollStartOffset(isRow, index, estimateOffset, true)
                scrollToPositionWithoutAnimate(isRow, finalIndex, finalOffset, 0f)
            } else {
                scrollToPositionWithoutAnimate(isRow, index, min(estimateOffset, currentListOffset()), offset)
            }
        } else if (index < currentEnd) {
            val node = children[BEFORE_COUNT + (index - currentStart)].flexNode
            // 减hairWidth(1像素)，防止size超过Float精度后，setContentOffset不满足执行条件
            val maxScrollOffset = max(0f, listViewContent!!.frame.size - listView!!.frame.size - hairWidth)
            val finalOffset = max(0f, min(node.start + offset, maxScrollOffset))
            val currentOffset = if (isRow) {
                listViewContent!!.offsetX
            } else {
                listViewContent!!.offsetY
            }
            if (!needScroll(currentOffset, finalOffset)) {
                logInfo("scrollToPosition offset not changed, skip")
                return
            }
            if (animate) {
                if (isRow) {
                    listView!!.setContentOffset(finalOffset, 0f, true)
                } else {
                    listView!!.setContentOffset(0f, finalOffset, true)
                }
                listView!!.setScrollEventFilterRule(currentOffset, finalOffset, true)
            } else {
                scrollToPositionWithoutAnimate(isRow, index, finalOffset, offset)
            }
        } else { // scroll down
            if (animate) {
                listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
            }
            // 减hairWidth(1像素)，防止size超过Float精度后，setContentOffset不满足执行条件
            val maxScrollOffset = max(0f, listViewContent!!.frame.size - listView!!.frame.size - hairWidth)
            val ratio = (index - currentEnd) / (curList.size - currentEnd).toFloat()
            val estimateOffset = min(itemEnd.frame.start + itemEnd.frame.size * ratio + offset, maxScrollOffset)

            if (animate) {
                listView?.setNextScrollToParams(ScrollToParams(index, offset, animate))
                val (finalIndex, finalOffset) = calculateAnimateScrollStartOffset(isRow, index, estimateOffset, false)
                scrollToPositionWithoutAnimate(isRow, finalIndex, finalOffset, 0f)
            } else {
                scrollToPositionWithoutAnimate(isRow, index, max(estimateOffset, currentListOffset()), offset)
            }
        }
    }

    private fun calculateAnimateScrollStartOffset(
        isRow: Boolean,
        index: Int,
        estimateOffset: Float,
        scrollUp: Boolean
    ): Pair<Int, Float> {
        val distance = min(
            // 通过view尺寸计算的最大偏移
            getPager().pageData.let { if (isRow) it.pageViewWidth else it.pageViewHeight },
            // 通过maxLoadItem计算的最大偏移
            floor(maxLoadItem / 3f) * avgItemSize
        )
        if (scrollUp) {
            val finalOffset = min(estimateOffset + distance, currentListOffset())
            val finalIndex = index + floor((finalOffset - estimateOffset) / avgItemSize).toInt()
            return Pair(finalIndex, finalOffset)
        } else {
            val finalOffset = max(estimateOffset - distance, currentListOffset())
            val finalIndex = index - floor((estimateOffset - finalOffset) / avgItemSize).toInt()
            return Pair(finalIndex, finalOffset)
        }
    }

    private fun scrollToPositionWithoutAnimate(
        isRow: Boolean,
        finalIndex: Int,
        finalOffset: Float,
        offsetExt: Float
    ) {
        if (isRow) {
            listViewContent?.offsetX = finalOffset
            listView?.setContentOffset(finalOffset, 0f, false)
        } else {
            listViewContent?.offsetY = finalOffset
            listView?.setContentOffset(0f, finalOffset, false)
        }
        createItemByPosition(finalIndex, finalOffset - offsetExt, false)
        listView?.setScrollEventFilterRule(finalOffset, finalOffset, false)
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

    internal fun getFirstVisiblePosition(
        visibleStart: Float,
        visibleEnd: Float
    ): Pair<Int, Float> {
        if (!isRangeVisible(itemStart.frame.start, itemEnd.frame.end, visibleStart, visibleEnd)) {
            return Pair(-1, 0f)
        }
        if (isRangeVisible(itemStart.frame.start, itemStart.frame.end, visibleStart, visibleEnd)) {
            return if (itemStart.frame.start < visibleStart) {
                val sizeInvisible = visibleStart - itemStart.frame.start
                val index = floor(sizeInvisible / itemStart.frame.size * currentStart).toInt()
                Pair(index, sizeInvisible - itemStart.frame.size * index / currentStart)
            } else {
                Pair(0, 0f)
            }
        } else {
            for (i in BEFORE_COUNT until children.size - AFTER_COUNT) {
                val child = children[i]
                if (isRangeVisible(child.frame.start, child.frame.end, visibleStart, visibleEnd)) {
                    return Pair(currentStart + i - BEFORE_COUNT, visibleStart - child.flexNode.start)
                }
            }
            if (isRangeVisible(itemEnd.frame.start, itemEnd.frame.end, visibleStart, visibleEnd)) {
                val sizeInvisible = visibleStart - itemEnd.frame.start
                val index = floor(sizeInvisible / itemEnd.frame.size * (curList.size - currentEnd)).toInt()
                return Pair(
                    currentEnd + index,
                    sizeInvisible - itemEnd.frame.size * index / (curList.size - currentEnd)
                )
            }
        }
        // should not reach here
        return Pair(-1, 0f)
    }
}

fun <T> ListView<*, *>.vforLazy(
    itemList: () -> ObservableList<T>,
    maxLoadItem: Int = MAX_ITEM_COUNT,
    itemCreator: LazyLoopDirectivesView<T>.(item: T, index: Int, count: Int) -> Unit
) {
    addChild(LazyLoopDirectivesView(itemList, maxLoadItem, itemCreator)) {}
}
