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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.collection.toFastMutableList
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPagerLayoutEventObserver

interface IScrollerViewEventObserver {
    fun onContentOffsetDidChanged(
        contentOffsetX: Float,
        contentOffsetY: Float,
        params: ScrollParams
    )
    fun subViewsDidLayout()

    /**
     * Same as [scrollerScrollDidEnd], keep for compatibility.
     * Please use [scrollerScrollDidEnd] instead.
     */
    @Deprecated("Use scrollerScrollDidEnd instead")
    fun onScrollEnd(params: ScrollParams) {}

    fun contentSizeDidChanged(width: Float, height: Float) { }

    fun scrollerDragBegin(params: ScrollParams) {}
    fun scrollerScrollDidEnd(params: ScrollParams) {}

    fun scrollFrameDidChanged(frame: Frame) {}
}

open class ScrollerView<A : ScrollerAttr, E : ScrollerEvent> :
    ViewContainer<A, E>() {
    var curOffsetX: Float = 0f
        private set
    var curOffsetY: Float = 0f
        private set
    private var lastFrame = Frame.zero

    var contentView: ScrollerContentView? = null
         private set
    private val scrollerViewEventObserverSet: HashSet<IScrollerViewEventObserver> by lazy(LazyThreadSafetyMode.NONE) {
        hashSetOf<IScrollerViewEventObserver>()
    }

    fun addScrollerViewEventObserver(observer: IScrollerViewEventObserver) {
        scrollerViewEventObserverSet.add(observer)
    }

    fun removeScrollerViewEventObserver(observer: IScrollerViewEventObserver) {
        scrollerViewEventObserverSet.remove(observer)
    }

    open fun createContentView(): ScrollerContentView {
        return ScrollerContentView()
    }

    /**
     * 设置内容的偏移量。
     *
     * @param offsetX X轴的偏移量。
     * @param offsetY Y轴的偏移量。
     * @param animated 是否使用动画进行偏移，默认为 false。
     * @param springAnimation 弹簧动画参数，可为空，默认为 null。
     */
    fun setContentOffset(offsetX: Float, offsetY: Float, animated: Boolean = false, springAnimation: SpringAnimation? = null) {
        performTaskWhenRenderViewDidLoad {
            var springAnimationString = ""
            springAnimation?.also {
                springAnimationString = it.toString()
            }
            renderView?.callMethod("contentOffset", "$offsetX $offsetY ${animated.toInt()}${springAnimationString}")
        }
    }

    fun abortContentOffsetAnimate() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("abortContentOffsetAnimate")
        }
    }

    fun setContentInset(
        top: Float = 0f,
        left: Float = 0f,
        bottom: Float = 0f,
        right: Float = 0f,
        animated: Boolean = false
    ) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("contentInset", "$top $left $bottom $right ${animated.toInt()}")
        }
    }

    fun setContentInsetWhenEndDrag(
        top: Float = 0f,
        left: Float = 0f,
        bottom: Float = 0f,
        right: Float = 0f
    ) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("contentInsetWhenEndDrag", "$top $left $bottom $right")
        }
    }

    override fun <T : DeclarativeBaseView<*, *>> addChild(child: T, init: T.() -> Unit) {
        initScrollerContentComponentIfNeed()
        contentView!!.addChild(child, init)
    }

    override fun realContainerView(): ViewContainer<*, *> {
        if (contentView != null) {
            return contentView!!
        }
        return this
    }

    override fun willInit() {
        super.willInit()
        attr.overflow(true)
    }

    override fun didInit() {
        super.didInit()
        listenScrollEvent()
    }

    override fun createAttr(): A {
        return ScrollerAttr() as A
    }

    override fun createEvent(): E {
        return ScrollerEvent() as E
    }

    override fun viewName(): String {
        return ViewConst.TYPE_SCROLLER
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        scrollerViewEventObserverSet.clear()
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.scrollFrameDidChanged(frame)
        }
        if (!lastFrame.isDefaultValue()
            && (lastFrame.width != frame.width || lastFrame.height != frame.height)) { // scrollView size非首次变化
            subViewsDidLayout()
        }
        lastFrame = frame
    }

    internal fun subViewsDidLayout() {
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.subViewsDidLayout()
        }
    }

    private fun initScrollerContentComponentIfNeed() {
        if (contentView === null) {
            contentView = createContentView()
            contentView?.also {
                it.flexNode.flexDirection = flexNode.flexDirection
                it.flexNode.justifyContent = flexNode.justifyContent
                it.flexNode.alignItems = flexNode.alignItems
                it.flexNode.flexWrap = flexNode.flexWrap
                it.flexNode.setPadding(StyleSpace.Type.TOP, flexNode.getPadding(StyleSpace.Type.TOP))
                it.flexNode.setPadding(StyleSpace.Type.LEFT, flexNode.getPadding(StyleSpace.Type.LEFT))
                it.flexNode.setPadding(StyleSpace.Type.TOP, flexNode.getPadding(StyleSpace.Type.TOP))
                it.flexNode.setPadding(StyleSpace.Type.BOTTOM, flexNode.getPadding(StyleSpace.Type.BOTTOM))
            }
            if (flexNode.flexDirection == FlexDirection.ROW
                || flexNode.flexDirection == FlexDirection.ROW_REVERSE
            ) {
                super.addChild(contentView!!, {
                    attr {
                        absolutePosition(top = 0f, left = 0f, bottom = 0f)
                    }
                }, 0)
            } else {
                super.addChild(contentView!!, {
                    attr {
                        absolutePosition(top = 0f, left = 0f, right = 0f)
                    }
                }, 0)
            }
        }
    }

    private fun handleListDidScroll(offsetX: Float, offsetY: Float, params: ScrollParams) {
        curOffsetX = offsetX
        curOffsetY = offsetY
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.onContentOffsetDidChanged(curOffsetX, curOffsetY, params)
        }
        contentView?.contentOffsetDidChanged(offsetX, offsetY, params)
    }

    private fun handleListDidScrollEnd(params: ScrollParams) {
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.onScrollEnd(params)
        }
    }

    private fun listenScrollEvent() {
        val ctx = this
        val scrollHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.SCROLL)
        getViewEvent().scroll {
            it.also {
                ctx.handleListDidScroll(it.offsetX, it.offsetY, it)
            }
            scrollHandler?.invoke(it)
        }
        val dragBeginHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.DRAG_BEGIN)
        getViewEvent().dragBegin { scrollParam ->
            scrollerViewEventObserverSet.toFastMutableList().forEach {
                it.scrollerDragBegin(scrollParam)
            }
            dragBeginHandler?.invoke(scrollParam)
        }
        val scrollEndHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.SCROLL_END)
        getViewEvent().scrollEnd { scrollParam ->
            scrollerViewEventObserverSet.toFastMutableList().forEach {
                it.onScrollEnd(scrollParam)
                it.scrollerScrollDidEnd(scrollParam)
            }
            scrollEndHandler?.invoke(scrollParam)
        }

    }

    internal fun contentSizeDidChanged(width: Float, height: Float) {
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.contentSizeDidChanged(width, height)
        }
    }
}

enum class KRNestedScrollMode(val value: String){
    SELF_ONLY("SELF_ONLY"),
    SELF_FIRST("SELF_FIRST"),
    PARENT_FIRST("PARENT_FIRST"),
    PARALLEL("PARALLEL")
}

open class ScrollerAttr : ContainerAttr() {
    var syncScroll = false
    var visibleAreaIgnoreTopMargin = 0f
    var visibleAreaIgnoreBottomMargin = 0f

    // 是否允许手势滚动
    fun scrollEnable(value: Boolean) {
        SCROLL_ENABLED with value.toInt()
    }
    // 是否允许边界回弹效果
    fun bouncesEnable(value: Boolean) {
        BOUNCES_ENABLE with value.toInt()
    }
    // 是否显示滚动指示进度条（默认显示）
    fun showScrollerIndicator(value: Boolean) {
        SHOW_SCROLLER_INDICATOR with value.toInt()
    }
    // 是否开启分页效果
    fun pagingEnable(enable: Boolean) {
        PAGING_ENABLED with enable.toInt()
    }

    /**
     * 设置计算可见性面积时忽略顶部距离。
     * @param margin 顶部距离。
     */
    fun visibleAreaIgnoreTopMargin(margin: Float) {
        visibleAreaIgnoreTopMargin = margin
    }

    /**
     * 设置计算可见性面积时忽略底部距离。
     * @param margin 底部距离。
     */
    fun visibleAreaIgnoreBottomMargin(margin: Float) {
        visibleAreaIgnoreBottomMargin = margin
    }

    /**
     * 是否允许fling（近for安卓，默认值为true，若设置false，则列表松手时则停止惯性滚动）
     */
    fun flingEnable(enable: Boolean) {
        FLING_ENABLE with enable.toInt()
    }

    /**
     * 设置是否同步滚动, 也可以通过Event.scroll(sync=true){}开启同步滚动
     * @param syncEnable 同步滚动启用状态(当前kotlin线程ui操作与ui线程同步更新)。
     */
    fun syncScroll(syncEnable: Boolean) {
        syncScroll = syncEnable
    }

    override fun flexDirection(flexDirection: FlexDirection): ContainerAttr {
        DIRECTION_ROW with (flexDirection == FlexDirection.ROW || flexDirection == FlexDirection.ROW_REVERSE).toInt()
        return super.flexDirection(flexDirection)
    }

    fun nestedScroll(forward: KRNestedScrollMode, backward: KRNestedScrollMode){
        val param = JSONObject()
        param.put("forward", forward.value)
        param.put("backward", backward.value)
        NESTED_SCROLL with param.toString()
    }

    companion object {
        const val SCROLL_ENABLED = "scrollEnabled"
        const val BOUNCES_ENABLE = "bouncesEnable"
        const val SHOW_SCROLLER_INDICATOR = "showScrollerIndicator"
        const val PAGING_ENABLED = "pagingEnabled"
        const val DIRECTION_ROW =  "directionRow"
        const val FLING_ENABLE = "flingEnable"
        const val NESTED_SCROLL = "nestedScroll"
    }

}

open class ScrollerEvent : Event() {
    internal var syncScroll = false
    internal var contentSizeChangedHandlerFn: ((width: Float, height: Float) -> Unit)? = null
    /**
     * 设置滚动事件处理器。当滚动事件发生时，会调用传入的处理器函数。
     *
     * @param handler 一个接收 ScrollParams 参数的函数，当滚动事件发生时被调用。
     */
    open fun scroll(handler: (ScrollParams) -> Unit) {
        scroll(false, handler)
    }

    /**
     * 设置滚动事件处理器，并指定是否同步滚动。当滚动事件发生时，会调用传入的处理器函数。
     *
     * @param sync 是否同步滚动（默认false，若为true，则使得当前kotlin线程对ui的操作与平台UI线程同步生效更新）
     * @param handler 一个接收 ScrollParams 参数的函数，当滚动事件发生时被调用。
     */
    open fun scroll(sync: Boolean, handler: (ScrollParams) -> Unit) {
        syncScroll = sync
        registerScrollerEvent(ScrollerEventConst.SCROLL, handler, sync)
    }

    /**
     * 设置滚动结束事件的处理器。当滚动结束时，会调用传入的处理器函数。
     *
     * @param handler 一个接收 ScrollParams 参数的函数，当滚动结束时被调用。
     */
    open fun scrollEnd(handler: (ScrollParams) -> Unit) {
        registerScrollerEvent(ScrollerEventConst.SCROLL_END, handler, false)
    }

    /**
     * 设置开始拖拽滚动事件的处理器。当开始拖拽滚动时，会调用传入的处理器函数。
     *
     * @param handler 一个接收 ScrollParams 参数的函数，当开始拖拽滚动时被调用。
     */
    open fun dragBegin(handler: (ScrollParams) -> Unit) {
        registerScrollerEvent(ScrollerEventConst.DRAG_BEGIN, handler, false)
    }

    /**
     * 设置结束拖拽滚动事件的处理器。当结束拖拽滚动时，会调用传入的处理器函数。
     *
     * @param handler 一个接收 ScrollParams 参数的函数，当结束拖拽滚动时被调用。
     */
    open fun dragEnd(handler: (ScrollParams) -> Unit) {
        registerScrollerEvent(ScrollerEventConst.DRAG_END, handler, false)
    }

    /**
     * 设置将要结束拖拽滚动事件的处理器。当将要结束拖拽滚动时，会调用传入的处理器函数。此方法会在平台主线程中同步回调。
     * 该方法常用于手松时指定滚动偏移量（setContentOffset）来实现自定义吸附位置
     * @param handler 一个接收 WillEndDragParams 参数的函数，当将要结束拖拽滚动时被调用。
     */
    open fun willDragEndBySync(handler: (WillEndDragParams) -> Unit) {
        this.register(ScrollerEventConst.WILL_DRAG_END, {
            if (it is JSONObject) {
                handler(WillEndDragParams.decode(it))
            } else if (it is WillEndDragParams) {
                handler(it)
            }
        }, true) // 平台主线程成会同步回调
    }

    /**
     * 设置内容尺寸变化事件的处理器。当内容尺寸发生变化时，会调用传入的处理器函数。
     * 一般使用该时机初始化initContentOffset位置
     * @param handler 一个接收宽度和高度参数的函数，当内容尺寸发生变化时被调用。
     */
    open fun contentSizeChanged(handler: (width: Float, height: Float) -> Unit) {
        contentSizeChangedHandlerFn = handler
    }

    private fun registerScrollerEvent(eventName: String, handler: (ScrollParams) -> Unit, sync: Boolean) {
        register(eventName, {
            if (it is JSONObject) {
                handler(ScrollParams.decode(it))
            } else if (it is ScrollParams) {
                handler(it)
            }
        }, sync)
    }

    object ScrollerEventConst {
        const val SCROLL = "scroll"
        const val SCROLL_END = "scrollEnd"
        const val DRAG_BEGIN = "dragBegin"
        const val DRAG_END = "dragEnd"
        const val WILL_DRAG_END = "willDragEnd"
    }
}

fun ViewContainer<*, *>.Scroller(init: ScrollerView<*, *>.() -> Unit) {
    addChild(ScrollerView<ScrollerAttr, ScrollerEvent>(), init)
}

/** 内容视图 */
open class ScrollerContentView : ViewContainer<ContainerAttr, Event>(), IPagerLayoutEventObserver {
    var offsetX: Float = 0f
        internal set
    var offsetY: Float = 0f
        internal set
    protected var needLayout = true
    override fun viewName(): String {
        return ViewConst.TYPE_SCROLL_CONTENT_VIEW
    }

    override fun createAttr(): ContainerAttr {
        return ContainerAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }

    override fun createFlexNode() {
        super.createFlexNode()
        flexNode.setNeedDirtyCallback = {
            needLayout = true
        }
    }

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        getPager().addPagerLayoutEventObserver(this)
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        getPager().removePagerLayoutEventObserver(this)
        flexNode.setNeedDirtyCallback = null
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        (parent as? ScrollerView<*, *>)?.getViewEvent()?.contentSizeChangedHandlerFn?.invoke(
            frame.width,
            frame.height
        )
    }

    open fun contentOffsetDidChanged(offsetX: Float, offsetY: Float, params: ScrollParams) {
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    override fun onPagerWillCalculateLayoutFinish() {

    }

    override fun onPagerCalculateLayoutFinish() {

    }

    override fun onPagerDidLayout() {
        if (needLayout) {
            parent?.also {
                if (parent is ScrollerView<*, *>) {
                    (parent as ScrollerView<*, *>).subViewsDidLayout()
                }
            }
            needLayout = false
        }

    }

}

data class ScrollParams(
    val offsetX: Float,  // 列表当前纵轴偏移量
    val offsetY: Float,  // 列表当前横轴偏移量
    val contentWidth: Float, // 列表当前内容总宽度
    val contentHeight: Float, // 列表当前内容总高度
    val viewWidth: Float,  // 列表View宽度
    val viewHeight: Float, // 列表View高度
    val isDragging: Boolean) { // 当前是否处于拖拽列表滚动中
    companion object {
        fun decode(params: JSONObject): ScrollParams {
            val offsetX = params.optDouble("offsetX").toFloat()
            val offsetY = params.optDouble("offsetY").toFloat()
            val contentWidth = params.optDouble("contentWidth").toFloat()
            val contentHeight = params.optDouble("contentHeight").toFloat()
            val viewWidth = params.optDouble("viewWidth").toFloat()
            val viewHeight = params.optDouble("viewHeight").toFloat()
            val isDragging = params.optInt("isDragging") == 1
            return ScrollParams(
                offsetX,
                offsetY,
                contentWidth,
                contentHeight,
                viewWidth,
                viewHeight,
                isDragging
            )
        }
    }

}

class WillEndDragParams(
    val offsetX: Float,  // 列表当前纵轴偏移量
    val offsetY: Float,  // 列表当前横轴偏移量
    val contentWidth: Float, // 列表当前内容总宽度
    val contentHeight: Float, // 列表当前内容总高度
    val viewWidth: Float,  // 列表View宽度
    val viewHeight: Float, // 列表View高度
    val isDragging: Boolean,// 当前是否处于拖拽列表滚动中
    val velocityX: Float, // 纵轴加速度
    val velocityY: Float, // 横轴加速度
    val targetContentOffsetX: Float, // 松手时默认滚动的目标位置X
    val targetContentOffsetY: Float // 松手时默认滚动的目标位置Y
    ) {
    companion object {
        fun decode(params: JSONObject): WillEndDragParams {
            val offsetX = params.optDouble("offsetX").toFloat()
            val offsetY = params.optDouble("offsetY").toFloat()
            val contentWidth = params.optDouble("contentWidth").toFloat()
            val contentHeight = params.optDouble("contentHeight").toFloat()
            val viewWidth = params.optDouble("viewWidth").toFloat()
            val viewHeight = params.optDouble("viewHeight").toFloat()
            val isDragging = params.optInt("isDragging") == 1
            val velocityX = params.optDouble("velocityX").toFloat()
            val velocityY = params.optDouble("velocityY").toFloat()
            val targetContentOffsetX = params.optDouble("targetContentOffsetX").toFloat()
            val targetContentOffsetY = params.optDouble("targetContentOffsetY").toFloat()
            return WillEndDragParams(
                offsetX,
                offsetY,
                contentWidth,
                contentHeight,
                viewWidth,
                viewHeight,
                isDragging,
                velocityX,
                velocityY,
                targetContentOffsetX,
                targetContentOffsetY
            )
        }
    }

}

data class SpringAnimation(val durationMs: Int, val damping: Float, val velocity: Float) {

    override fun toString(): String {
        return " $durationMs $damping $velocity"
    }

}