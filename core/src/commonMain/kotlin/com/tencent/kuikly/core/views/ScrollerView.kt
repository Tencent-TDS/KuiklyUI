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
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.collection.toFastMutableList
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPagerLayoutEventObserver
import com.tencent.kuikly.core.views.internal.GroupEvent

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

    fun contentViewDidSetFrameToRenderView() {}

    fun contentSizeDidChanged(width: Float, height: Float) { }

    fun scrollerDragBegin(params: ScrollParams) {}
    
    fun scrollerDragEnd(params: ScrollParams) {}

    fun scrollerScrollDidEnd(params: ScrollParams) {}

    fun scrollFrameDidChanged(frame: Frame) {}
}

open class ScrollerView<A : ScrollerAttr, E : ScrollerEvent> :
    ViewContainer<A, E>() {
    var curOffsetX: Float = 0f
        private set
    var curOffsetY: Float = 0f
        private set
    var isDragging: Boolean = false

    val contentViewOffsetX: Float
      get() {
          return contentView?.offsetX ?: 0f
      }

    val contentViewOffsetY: Float
        get() {
            return contentView?.offsetY ?: 0f
        }

    private var lastFrame = Frame.zero
    internal var shouldListenWillEndDrag = false

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
        val contentOffset = transformInputSetContentOffset(offsetX, offsetY)
        performTaskWhenRenderViewDidLoad {
            if (!animated && springAnimation == null) {
                contentView?.contentOffsetWillChanged(contentOffset.first, contentOffset.second)
            }
            callContentOffset(contentOffset.first, contentOffset.second, animated, springAnimation)
        }
    }

    fun abortContentOffsetAnimate() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("abortContentOffsetAnimate")
        }
    }

    open fun callContentOffset(offsetX: Float, offsetY: Float, animated: Boolean = false, springAnimation: SpringAnimation? = null) {
        var springAnimationString = ""
        springAnimation?.also {
            springAnimationString = it.toString()
        }
        renderView?.callMethod("contentOffset", "${offsetX} ${offsetY} ${animated.toInt()}${springAnimationString}")

    }

    internal fun contentViewDidSetFrameToRenderView() {
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.contentViewDidSetFrameToRenderView()
        }
    }

    /* 转换输入设置的滚动偏移量 */
    open fun transformInputSetContentOffset(offsetX: Float, offsetY: Float): Pair<Float, Float> {
        return Pair(offsetX, offsetY)
    }

    /* 转换输入设置的滚动偏移量 */
    open fun transformOutputContentOffset(offsetX: Float, offsetY: Float): Pair<Float, Float> {
        return Pair(offsetX, offsetY)
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

    override fun <T : DeclarativeBaseView<*, *>> addChild(
        child: T,
        init: T.() -> Unit,
        index: Int
    ) {
        initScrollerContentComponentIfNeed()
        contentView!!.addChild(child, init, index)
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

    fun initScrollerContentComponentIfNeed() {
        if (contentView === null) {
            contentView = createContentView()
            contentView?.also {
                it.pagerId = this.pagerId
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
            insertDomSubView(contentView!!, 0)
        }
    }

    private fun handleListDidScroll(offsetX: Float, offsetY: Float, params: ScrollParams) {
        curOffsetX = offsetX
        curOffsetY = offsetY
        contentView?.contentOffsetWillChanged(offsetX, offsetY)
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.onContentOffsetDidChanged(curOffsetX, curOffsetY, params)
        }
        contentView?.contentOffsetDidChanged(offsetX, offsetY, params)
    }

    private fun handleListDidScrollEnd(params: ScrollParams) {
        curOffsetX = params.offsetX
        curOffsetY = params.offsetY
        contentView?.contentOffsetWillChanged(params.offsetX, params.offsetY)
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.onScrollEnd(params)
        }
        contentView?.contentOffsetDidChanged(params.offsetX, params.offsetY, params)
    }

    fun listenScrollEvent() {
        val ctx = this
        val scrollHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.SCROLL)
        val scrollEndHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.SCROLL_END)
        val dragBeginHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.DRAG_BEGIN)
        val dragEndHandler = event.handlerWithEventName(ScrollerEvent.ScrollerEventConst.DRAG_END)
        event {
            scroll(ctx.attr.syncScroll || this.syncScroll) {
                val contentOffset = ctx.transformOutputContentOffset(it.offsetX, it.offsetY)
                it.offsetX = contentOffset.first
                it.offsetY = contentOffset.second
                it.also {
                    ctx.handleListDidScroll(it.offsetX, it.offsetY, it)
                }
                scrollHandler?.invoke(it)
            }
            dragBegin { scrollParam ->
                this@ScrollerView.scrollerViewEventObserverSet.toFastMutableList().forEach {
                    it.scrollerDragBegin(scrollParam)
                }
                dragBeginHandler?.invoke(scrollParam)
            }
            dragEnd { scrollParam ->
                this@ScrollerView.scrollerViewEventObserverSet.toFastMutableList().forEach {
                    it.scrollerDragEnd(scrollParam)
                }
                dragEndHandler?.invoke(scrollParam)
            }
            scrollEnd {
                ctx.handleListDidScrollEnd(it)
                scrollEndHandler?.invoke(it)
            }
        }
    }

    internal fun contentSizeDidChanged(width: Float, height: Float) {
        scrollerViewEventObserverSet.toFastMutableList().forEach {
            it.contentSizeDidChanged(width, height)
        }
    }

    /**
     * 是否为横向布局
     */
    fun isRowFlexDirection(): Boolean {
        if ((contentView as? ScrollerContentView) != null) {
            return (contentView as ScrollerContentView).isRowFlexDirection()
        }
        return false
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
    internal var bouncesEnable = true

    // 是否允许手势滚动
    fun scrollEnable(value: Boolean) {
        SCROLL_ENABLED with value.toInt()
    }
    /*
     * 是否允许边界回弹效果
     * @param bouncesEnable 是否允许边界回弹
     * @param limitHeaderBounces 是否禁止顶部回弹(如bouncesEnable为false，该值就无效)
     */
    fun bouncesEnable(bouncesEnable: Boolean, limitHeaderBounces: Boolean = false) {
        this.bouncesEnable = bouncesEnable
        BOUNCES_ENABLE with bouncesEnable.toInt()
        LIMIT_BOUNCES_ENABLE with limitHeaderBounces.toInt()
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

    /**
     * 设置是否父组件滑动联动，即自身滑动到目标方向的边缘时，触发父组件滑动，默认 true
     * @param enable 是否允许父组件滑动联动
     */
    fun scrollWithParent(enable: Boolean) {
        SCROLL_WITH_PARENT with enable.toInt()
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
        const val LIMIT_BOUNCES_ENABLE = "limitHeaderBounces"
        const val SHOW_SCROLLER_INDICATOR = "showScrollerIndicator"
        const val PAGING_ENABLED = "pagingEnabled"
        const val DIRECTION_ROW =  "directionRow"
        const val FLING_ENABLE = "flingEnable"
        const val SCROLL_WITH_PARENT = "scrollWithParent"
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
        val ctx = this
        registerScrollerEvent(ScrollerEventConst.DRAG_BEGIN, handler = {
            (getView() as ScrollerView).isDragging = true
            handler.invoke(it)
        }, sync = false)
    }

    /**
     * 设置结束拖拽滚动事件的处理器。当结束拖拽滚动时，会调用传入的处理器函数。
     *
     * @param handler 一个接收 ScrollParams 参数的函数，当结束拖拽滚动时被调用。
     */
    open fun dragEnd(handler: (ScrollParams) -> Unit) {
        registerScrollerEvent(ScrollerEventConst.DRAG_END, handler = {
            (getView() as ScrollerView).isDragging = false
            handler.invoke(it)
        }, sync = false)
    }

    /**
     * 设置将要结束拖拽滚动事件的处理器。当将要结束拖拽滚动时，会调用传入的处理器函数。此方法会在平台主线程中同步回调。
     * 该方法常用于手松时指定滚动偏移量（setContentOffset）来实现自定义吸附位置
     * @param handler 一个接收 WillEndDragParams 参数的函数，当将要结束拖拽滚动时被调用。
     */
    fun willDragEndBySync(handler: (WillEndDragParams) -> Unit, isSync: Boolean) {
        this.register(ScrollerEventConst.WILL_DRAG_END, {
            if (it is JSONObject) {
                handler(WillEndDragParams.decode(it))
            } else if (it is WillEndDragParams) {
                handler(it)
            }
        }, isSync) // 平台主线程成会同步回调
    }

    open fun willDragEndBySync(handler: (WillEndDragParams) -> Unit) {
        willDragEndBySync(isSync = true, handler = handler)
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
open class ScrollerContentView : ViewContainer<ContainerAttr, GroupEvent>(), IPagerLayoutEventObserver {
    var offsetX: Float = 0f
    var offsetY: Float = 0f
    protected var needLayout = true
    override fun viewName(): String {
        return ViewConst.TYPE_SCROLL_CONTENT_VIEW
    }

    override fun createAttr(): ContainerAttr {
        return ContainerAttr()
    }

    override fun createEvent(): GroupEvent {
        return GroupEvent()
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
        (parent as? ScrollerView<*, *>)?.also {
            it.getViewEvent().contentSizeChangedHandlerFn?.invoke(
                frame.width,
                frame.height
            )
            it.contentSizeDidChanged(width = frame.width, height = frame.height)
        }
    }

    open fun contentOffsetWillChanged(offsetX: Float, offsetY: Float) {
        this.offsetX = offsetX
        this.offsetY = offsetY
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

    override fun didSetFrameToRenderView() {
        super.didSetFrameToRenderView()
        (parent as? ScrollerView<*, *>)?.contentViewDidSetFrameToRenderView()
    }

    fun isRowFlexDirection(): Boolean {
        return flexNode.flexDirection == FlexDirection.ROW || flexNode.flexDirection == FlexDirection.ROW_REVERSE
    }

}

data class ScrollParams(
    var offsetX: Float,  // 列表当前纵轴偏移量
    var offsetY: Float,  // 列表当前横轴偏移量
    val contentWidth: Float, // 列表当前内容总宽度
    val contentHeight: Float, // 列表当前内容总高度
    val viewWidth: Float,  // 列表View宽度
    val viewHeight: Float, // 列表View高度
    val isDragging: Boolean, // 是否在dragging
    val touches: List<Touch> = listOf()   // Touch触摸点信息列表
) { // 当前是否处于拖拽列表滚动中
    companion object {
        fun decode(params: JSONObject): ScrollParams {
            val offsetX = params.optDouble("offsetX").toFloat()
            val offsetY = params.optDouble("offsetY").toFloat()
            val contentWidth = params.optDouble("contentWidth").toFloat()
            val contentHeight = params.optDouble("contentHeight").toFloat()
            val viewWidth = params.optDouble("viewWidth").toFloat()
            val viewHeight = params.optDouble("viewHeight").toFloat()
            val isDragging = params.optInt("isDragging") == 1
            val jsonArray = params.optJSONArray("touches")
            val touches = mutableListOf<Touch>()
            jsonArray?.let {
                for (i in 0 until it.length()) {
                    val touch = Touch.decode(jsonArray.opt(i))
                    touches.add(touch)
                }
            }
            return ScrollParams(
                offsetX,
                offsetY,
                contentWidth,
                contentHeight,
                viewWidth,
                viewHeight,
                isDragging,
                touches
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