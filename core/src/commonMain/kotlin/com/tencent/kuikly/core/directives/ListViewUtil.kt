package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.isVirtualView
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.cleanNextScrollToParams
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.logInfo
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.needScroll
import com.tencent.kuikly.core.directives.LazyLoopDirectivesView.Companion.setScrollEventFilterRule
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.views.ListContentView
import com.tencent.kuikly.core.views.ListView
import kotlin.math.max
import kotlin.math.min

/**
 * 滚动到指定位置
 * @param index item在List中的索引位置
 * @param offset item相对于List的偏移量，默认值为0f
 * @param animate 是否需要动画，默认值为false
 * @see getFirstVisiblePosition
 */
fun ListView<*, *>.scrollToPosition(index: Int, offset: Float = 0f, animate: Boolean = false) {
    logInfo("scrollToPosition index=$index $offset $animate")
    val listViewContent = contentView as ListContentView
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
                val node = domChildren[index - skip].flexNode
                doScroll(this, listViewContent, node, offset, animate, templateChildren)
                return
            } else {
                skip += domChildren.size
            }
        } else {
            if (index - skip == 0) {
                val node = child.flexNode
                doScroll(this, listViewContent, node, offset, animate, templateChildren)
                return
            } else {
                ++skip
            }
        }
    }
    logInfo("can't scroll position invalid")
}

/**
 * 滚动到node对应的位置
 */
private fun doScroll(
    list: ListView<*, *>,
    content: ListContentView,
    node: FlexNode,
    offsetExt: Float,
    animate: Boolean,
    templateChildren: List<DeclarativeBaseView<*, *>>
) {
    if (!node.layoutFrame.isDefaultValue()) {
        val isRow = content.isRowFlexDirection()
        val currentOffset: Float
        val finalOffset: Float
        if (isRow) {
            currentOffset = content.offsetX
            finalOffset = max(
                0f,
                min(
                    node.layoutFrame.x - node.getMargin(StyleSpace.Type.LEFT) + offsetExt,
                    content.frame.width - list.frame.width
                )
            )
            if (!needScroll(currentOffset, finalOffset)) {
                logInfo("doScroll offset not changed, skip")
                return
            }
            list.setContentOffset(finalOffset, 0f, animate)
        } else {
            currentOffset = content.offsetY
            finalOffset = max(
                0f,
                min(
                    node.layoutFrame.y - node.getMargin(StyleSpace.Type.TOP) + offsetExt,
                    content.frame.height - list.frame.height
                )
            )
            if (!needScroll(currentOffset, finalOffset)) {
                logInfo("doScroll offset not changed, skip")
                return
            }
            list.setContentOffset(0f, finalOffset, animate)
        }
        list.setScrollEventFilterRule(currentOffset, finalOffset, animate)
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

/**
 * 获取第一个可见的item位置
 * @return (index: Int, offset: Float) 第一个是可见item的索引位置，第二个是该item相对于List的偏移量
 * @see scrollToPosition
 */
fun ListView<*, *>.getFirstVisiblePosition(): Pair<Int, Float> {
    val listViewContent = contentView as ListContentView
    val templateChildren = listViewContent.templateChildren()
    val isRow = listViewContent.isRowFlexDirection()
    val start = if (isRow) listViewContent.offsetX else listViewContent.offsetY
    val end = start + if (isRow) frame.width else frame.height
    val density = getPager().pageData.density
    var index = 0
    for (child in templateChildren) {
        if (child is LazyLoopDirectivesView<*>) {
            val (childIndex, offset) = child.getFirstVisiblePosition(start, end)
            if (childIndex != -1) {
                return Pair(childIndex + index, offset.roundWithDensity(density))
            } else {
                index += child.getItemCount()
            }
        } else if (child.isVirtualView()) {
            val domChildren = (child as ViewContainer).domChildren()
            if (isRow) {
                if (domChildren.isEmpty() || !isRangeVisible(
                        domChildren.first().frame.x,
                        domChildren.last().frame.maxX(),
                        start,
                        end
                    )
                ) {
                    index += domChildren.size
                    continue
                }
                domChildren.forEachIndexed { childIndex, c ->
                    val frame = c.frame
                    if (isRangeVisible(frame.x, frame.maxX(), start, end)) {
                        val offset = start - (frame.x - c.flexNode.getMargin(StyleSpace.Type.LEFT))
                        return Pair(index + childIndex, offset.roundWithDensity(density))
                    }
                }
            } else {
                if (domChildren.isEmpty() || !isRangeVisible(
                        domChildren.first().frame.y,
                        domChildren.last().frame.maxY(),
                        start,
                        end
                    )
                ) {
                    index += domChildren.size
                    continue
                }
                domChildren.forEachIndexed { childIndex, c ->
                    val frame = c.frame
                    if (isRangeVisible(frame.y, frame.maxY(), start, end)) {
                        val offset = start - (frame.y - c.flexNode.getMargin(StyleSpace.Type.TOP))
                        return Pair(index + childIndex, offset.roundWithDensity(density))
                    }
                }
            }
        } else {
            val frame = child.frame
            if (isRow) {
                if (isRangeVisible(frame.x, frame.maxX(), start, end)) {
                    val offset = start - (frame.x - child.flexNode.getMargin(StyleSpace.Type.LEFT))
                    return Pair(index, offset.roundWithDensity(density))
                }
            } else {
                if (isRangeVisible(frame.y, frame.maxY(), start, end)) {
                    val offset = start - (frame.y - child.flexNode.getMargin(StyleSpace.Type.TOP))
                    return Pair(index, offset.roundWithDensity(density))
                }
            }
            ++index
        }
    }
    return Pair(-1, 0f)
}

private fun Float.roundWithDensity(density: Float): Float {
    return (this * density + 0.5f).toInt() / density
}

internal fun isRangeVisible(
    rangeStart: Float,
    rangeEnd: Float,
    visibleStart: Float,
    visibleEnd: Float
): Boolean {
    return (rangeEnd - rangeStart > 1e-5) && rangeStart < visibleEnd && rangeEnd > visibleStart
}
