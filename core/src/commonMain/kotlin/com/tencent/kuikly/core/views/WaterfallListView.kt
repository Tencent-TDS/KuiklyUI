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

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import kotlin.math.max

/**
 * 瀑布流组件。
 * @param init 初始化函数。
 */
fun ViewContainer<*, *>.WaterfallList(init: WaterfallListView.() -> Unit) {
    addChild(WaterfallListView(), init)
}
/**
 * 瀑布流列表属性类，继承自 ListAttr。
 */
class WaterfallListAttr : ListAttr() {
    internal var listWidth = 0f
    internal var columnCount = 1
    internal var lineSpacing = 0f
    internal var itemSpacing = 0f
    internal var contentPaddingLeft = 0f
    internal var contentPaddingRight = 0f
    internal var contentPaddingBottom = 0f
    internal var contentPaddingTop = 0f

    /**
     * 设置列表宽度（必须设置）。
     * @param width 列表宽度。
     */
    fun listWidth(width: Float) {
        listWidth = width
        width(width)
    }

    /**
     * 设置列数（默认为1）。
     * @param count 列数。
     */
    fun columnCount(count: Int) {
        if (columnCount != count && count > 0) {
            columnCount = count
            (view() as? WaterfallListView)?.onUpdateColumnCount()
        }
    }

    /**
     * 设置列间距。
     * @param spacing 列间距。
     */
    fun itemSpacing(spacing: Float) {
        itemSpacing = spacing
    }

    /**
     * 设置行间距。
     * @param spacing 行间距。
     */
    fun lineSpacing(spacing: Float) {
        lineSpacing = spacing
    }

    /**
     * 设置内边距。
     * @param top 顶部内边距。
     * @param left 左侧内边距。
     * @param bottom 底部内边距。
     * @param right 右侧内边距。
     */
    fun contentPadding(top: Float, left: Float = 0f, bottom: Float = 0f, right: Float = 0f) {

        if (!top.valueEquals(Float.undefined)) {
            contentPaddingTop = top
        }
        if (!left.valueEquals(Float.undefined)) {
            contentPaddingLeft = left
        }
        if (!bottom.valueEquals(Float.undefined)) {
            contentPaddingBottom = bottom
        }
        if (!right.valueEquals(Float.undefined)) {
            contentPaddingRight = right
        }
    }

    override fun padding(top: Float, left: Float, bottom: Float, right: Float): ContainerAttr {
        contentPadding(top, left, bottom, right)
        return this
    }
}

/**
 * 瀑布流列表事件类，继承自 ListEvent。
 */
class WaterfallListEvent : ListEvent()

class WaterfallListView :
    ListView<WaterfallListAttr, WaterfallListEvent>() {

    override fun createContentView(): ScrollerContentView {
        return WaterfallContentView()
    }

    override fun createAttr(): WaterfallListAttr {
        return WaterfallListAttr()
    }

    override fun createEvent(): WaterfallListEvent {
        return WaterfallListEvent()
    }

    internal fun onUpdateColumnCount() {
        if (lastFrameSize != null) {
            (contentView as? WaterfallContentView)?.updateChildLayout()
        }
    }
}

class WaterfallContentView : ListContentView() {
    companion object {
        private const val KEY_STATIC_WIDTH_NODE = "waterfall_static_width"
        private var Attr.isStaticWidth: Boolean
            get() = extProps[KEY_STATIC_WIDTH_NODE] == true
            set(value) {
                extProps[KEY_STATIC_WIDTH_NODE] = value
            }
    }
    private var firstLayout = false
    private var waitingToNextTickLayout = false
    override fun didInsertDomChild(child: DeclarativeBaseView<*, *>, index: Int) {
        super.didInsertDomChild(child, index)
        val ctx = this
        if (!child.absoluteFlexNode ) {
            child.attr {
                positionAbsolute()
                top(0f)
                left(0f)
                right(Float.undefined)
                bottom(Float.undefined)
                if (child.getViewAttr().flexNode!!.styleWidth.isNaN()
                ) {
                    width(ctx.columnWidth())
                } else {
                    child.getViewAttr().isStaticWidth = true
                }
            }
        }

    }

    override fun createFlexNode() {
        super.createFlexNode()
    }

    // 列宽
    private fun columnWidth(): Float {
        val listView = (parent as WaterfallListView)
        val attr = listView.getViewAttr()
        if (attr.listWidth == 0f) {
            throw RuntimeException("must set Waterfall attr.listWidth ")
        }
        return ((attr.listWidth - attr.contentPaddingLeft - attr.contentPaddingRight)
                - ((attr.columnCount - 1) * attr.itemSpacing)) / attr.columnCount
    }

    private fun columnCount() : Int {
        val listView = (parent as WaterfallListView)
        val attr = listView.getViewAttr()
        return attr.columnCount
    }

    override fun onPagerCalculateLayoutFinish() {
        val listView = (parent as WaterfallListView)
        val attr = listView.getViewAttr()
        val minHeightArray = arrayListOf<Float>()
        var curAbsoluteNodeOffset = 0f
        for (i in 0 until attr.columnCount) {
            minHeightArray.add(attr.contentPaddingTop - attr.lineSpacing)
        }
        needLayoutChildren().forEachIndexed { index, declarativeBaseView ->
            if (declarativeBaseView.absoluteFlexNode) {
                curAbsoluteNodeOffset = max(declarativeBaseView.flexNode.layoutFrame.maxY() , curAbsoluteNodeOffset)
            } else {
                var x = 0f
                var y = 0f
                // 单独占据一行
                if (declarativeBaseView.flexNode.layoutFrame.width > this.columnWidth() + 1) {
                    x = attr.contentPaddingLeft
                    var maxHeight = -Float.MAX_VALUE
                    minHeightArray.forEachIndexed { index, height ->
                        if (maxHeight < height) {
                            maxHeight = height
                        }
                    }
                    y = maxHeight + attr.lineSpacing
                    for (i in 0 until minHeightArray.count()) {
                        minHeightArray[i] = y + declarativeBaseView.flexNode.layoutFrame.height
                    }
                } else {
                    var minHeight = Float.MAX_VALUE
                    var minIndex = 0
                    minHeightArray.forEachIndexed { index, height ->
                        if (minHeight > height) {
                            minHeight = height
                            minIndex = index
                        }
                    }
                    x = attr.contentPaddingLeft + (minIndex) * (columnWidth() + attr.itemSpacing)
                    y = minHeight + attr.lineSpacing
                    minHeightArray[minIndex] = y + declarativeBaseView.flexNode.layoutFrame.height
                }
                declarativeBaseView.flexNode.layoutFrame.also {
                    declarativeBaseView.flexNode.updateLayoutFrame(Frame(x, y, it.width, it.height))
                }
            }

        }
        var maxHeight = -Float.MAX_VALUE
        minHeightArray.forEachIndexed { index, height ->
            if (maxHeight < height) {
                maxHeight = height
            }
        }
        if (maxHeight < 0f) {
            maxHeight = 0f
        }
        val flexNodeLayoutFrame = flexNode.layoutFrame.toMutableFrame()
        flexNodeLayoutFrame.height = max(maxHeight + attr.contentPaddingBottom, curAbsoluteNodeOffset)
        flexNode.updateLayoutFrame(flexNodeLayoutFrame.toFrame())
    }

    override fun updateChildLayout() {
        val ctx = this
        getPager().addNextTickTask {
            needLayoutChildren().forEach {
                if (!it.getViewAttr().isStaticWidth) {
                    it.getViewAttr().width(ctx.columnWidth())
                }
            }
            createRenderViewsOnVisibleRect()
        }
    }
}
