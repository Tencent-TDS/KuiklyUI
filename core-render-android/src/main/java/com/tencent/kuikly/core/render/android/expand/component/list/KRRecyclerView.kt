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

package com.tencent.kuikly.core.render.android.expand.component.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.touchConsumeByNative
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonDecoration
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonForegroundDecoration
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.css.ktx.touchConsumeByKuikly
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

enum class KRNestedScrollMode(val value: String){
    SELF_ONLY("SELF_ONLY"),
    SELF_FIRST("SELF_FIRST"),
    PARENT_FIRST("PARENT_FIRST"),
}

/**
 * Kuikly List组件
 */
class KRRecyclerView : RecyclerView, IKuiklyRenderViewExport, NestedScrollingChild2,
    NestedScrollingParent2 {

    constructor(context: Context) : super(context)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    /**
     * 滚动回调
     */
    private var scrollEventCallback: KuiklyRenderCallback? = null

    /**
     * 开始拖拽回调
     */
    private var dragBeginEventCallback: KuiklyRenderCallback? = null

    /**
     * 结束拖拽回调
     */
    private var dragEndEventCallback: KuiklyRenderCallback? = null

    /**
     * 滚动结束拖拽
     */
    private var scrollEndEventCallback: KuiklyRenderCallback? = null

    /**
     * 开始fling回调
     */
    private var willEndDragEventCallback: KuiklyRenderCallback? = null

    /**
     * RecyclerView滚动监听器
     */
    private var scrollListener: OnScrollListener? = null

    /**
     * 是否为横向List
     */
    var directionRow = false

    /**
     * 是否开启分页
     */
    private var pageEnable = false

    /**
     * 是否开启滚动
     */
    private var scrollEnabled = true

    /**
     * 是否开启List滚动到边缘回弹效果
     */
    private var bouncesEnable = true
    internal var limitHeaderBounces = false

    /**
     * List上一次的滚动状态
     */
    private var preScrollState = SCROLL_STATE_IDLE

    /**
     * 处理边缘回调的处理器
     */
    private var overScrollHandler: OverScrollHandler? = null

    /**
     * 是否正在拖拽
     */
    private var isDragging = false

    private var supportFling = true

    /**
     * 在嵌套滚动且是Overscroll模式时，避免子列表的Fling被触发，导致回弹异常
     */
    internal var skipFlingIfNestOverScroll = false

    // 当滑动长时间停留一个位置时，recycleView不会fling，因此不会回调fireWillEndDragEvent，需要在dragEnd时，提前回调
    private var needFireWillEndDragEvent = true

    private var mNestedScrollAxesTouch = SCROLL_AXIS_NONE

    private var mNestedScrollAxesNonTouch = SCROLL_AXIS_NONE

    /**
     * 向前滑动
     */
    private var scrollForwardMode = KRNestedScrollMode.SELF_FIRST
    /**
     * 向后滑动
     */
    private var scrollBackwardMode = KRNestedScrollMode.SELF_FIRST

    /**
     * 父组件滑动联动，即自身滑动到目标方向的边缘时，触发父组件滑动，默认 true
     */
    private var scrollWithParent = true

    var enableSmallTouchSlop = false
        set(value) {
            if (field == value) {
                return
            }
            field = value

            try {
                val touchSlop = if (value) {
                    ViewConfiguration.get(context).scaledTouchSlop / 2
                } else {
                    ViewConfiguration.get(context).scaledTouchSlop
                }
                val f = RecyclerView::class.java.getDeclaredField("mTouchSlop")
                f.isAccessible = true
                f.set(this, touchSlop)
            } catch (e: Exception) {
                // 由于不清楚系统mTouchSlop底层会抛出哪种类型的异常，因此这里使用顶层异常来处理
                // 并且异常不影响主路径
                KuiklyRenderLog.e(VIEW_NAME, "set mTouchSlop error, $e")
            }
        }

    /**
     * 将要滚动到的offset字符串描述
     */
    private var pendingSetContentOffsetStr = ""

    /**
     * List 高度动态改变时, iOS系统会自动调整 contentOffset
     * Android 对齐iOS 的表现
     */
    private var pendingFireOnScroll = true

    private val contentView: View
        get() = getChildAt(0)

    /**
     * ContentView是否已经添加到RecyclerView上
     * 注意: setupAdapter调用后, RecyclerView不会把ContentView添加上去
     */
    private val isContentViewAttached: Boolean
        get() = getChildAt(0) != null

    private val krRecyclerViewListeners by lazy {
        mutableListOf<IKRRecyclerViewListener>()
    }

    private val scrollConflictHandler by lazy {
        RVScrollConflictHandler(context)
    }

    /**
     * 最近的一个横向RecyclerView父亲, 用于解决横向RecyclerView嵌套横向RecyclerView时的冲突
     */
    private var closestHorizontalRecyclerViewParent: KRRecyclerView? = null

    /**
     * 最近的一个竖向RecyclerView父亲, 用于解决竖向RecyclerView嵌套竖向RecyclerView时的冲突
     */
    private var closestVerticalRecyclerViewParent: KRRecyclerView? = null

    /**
     * 对onInterceptTouchEvent感兴趣的监听者列表
     */
    private var nestedChildInterceptEventListeners: MutableList<INestedChildInterceptor>? = null

    private var nestedHorizontalChildInterceptor: NestedHorizontalChildInterceptor? = null
    private var nestedVerticalChildInterceptor: NestedVerticalChildInterceptor? = null

    var contentOffsetY = 0f
    var contentOffsetX = 0f

    var touchDelegate: IKRRecyclerViewTouchDelegate? = null

    var forceOverScroll: Boolean
        get() = overScrollHandler?.forceOverScroll ?: false
        set(value) {
            overScrollHandler?.forceOverScroll = value
        }

    init {
        overScrollMode = OVER_SCROLL_NEVER
        isFocusableInTouchMode = false
        isNestedScrollingEnabled = true
    }

    fun setContentInsert(contentInset: KRRecyclerContentViewContentInset?, immediately: Boolean = false) {
        val oh = overScrollHandler ?: return
        if (immediately) {
            oh.contentInsetWhenEndDrag = contentInset
            oh.bounceWithContentInset(contentInset ?: KRRecyclerContentViewContentInset(kuiklyRenderContext))
        } else {
            oh.contentInsetWhenEndDrag = contentInset
        }
    }

    fun addScrollListener(listener: IKRRecyclerViewListener) {
        krRecyclerViewListeners.add(listener)
    }

    fun removeListener(listener: IKRRecyclerViewListener) {
        krRecyclerViewListeners.remove(listener)
    }

    fun addNestedChildInterceptEventListener(listener: INestedChildInterceptor) {
        val listeners = nestedChildInterceptEventListeners ?: mutableListOf<INestedChildInterceptor>().apply {
            nestedChildInterceptEventListeners = this
        }
        listeners.add(listener)
    }

    fun removeNestedChildInterceptEventListener(listener: INestedChildInterceptor) {
        nestedChildInterceptEventListeners?.remove(listener)
    }

    /**
     * 设置HRRecyclerView的Prop
     *
     * <p>这里为啥不用使用map<key, handler>来处理?
     *
     * <p>1.属性不会太多, 使用when语句的可读性比map<key，handler>的方式好
     *
     * <p>2.一般只有维护者一人编写
     *
     * <p>3.降低内存开销
     *
     * <p>这里的value类型是与kuiklyCore侧约定好的，因此没判断就使用强转
     *
     * @param propKey 属性key
     * @param propValue 属性值
     * @return 是否处理
     */
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            DRAG_BEGIN -> observeDragBegin(propValue)
            DRAG_END -> observeDragEnd(propValue)
            SCROLL -> observeScroll(propValue)
            SCROLL_END -> observeScrollEnd(propValue)
            WILL_DRAG_END -> observeWillEndDrag(propValue)
            DIRECTION_ROW -> setDirectionRow(propValue)
            PAGING_ENABLED -> setPagingEnable(propValue)
            SHOW_SCROLLER_INDICATOR -> showScrollerIndicator(propValue)
            SCROLL_ENABLED, KRCssConst.TOUCH_ENABLE -> setScrollEnabled(propValue)
            VERTICAL_BOUNCES, BOUNCES_ENABLE, HORIZONTAL_BOUNCES -> setBouncesEnable(propValue)
            LIMIT_HEADER_BOUNCES -> limitHeaderBounces(propValue)
            FLING_ENABLE -> setFlingEnable(propValue)
            SCROLL_WITH_PARENT -> setScrollWithParent(propValue)
            KRCssConst.FRAME -> {
                automaticAdjustContentOffset()
                super.setProp(propKey, propValue)
            }
            NESTED_SCROLL -> {
                setNestedScroll(propValue)
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun limitHeaderBounces(propValue: Any): Boolean {
        limitHeaderBounces = (propValue as Int) == 1
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeDragBegin(propValue: Any): Boolean {
        dragBeginEventCallback = propValue as KuiklyRenderCallback
        addOnScrollListener()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeDragEnd(propValue: Any): Boolean {
        dragEndEventCallback = propValue as KuiklyRenderCallback
        addOnScrollListener()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeScroll(propValue: Any): Boolean {
        scrollEventCallback = propValue as KuiklyRenderCallback
        addOnScrollListener()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeScrollEnd(propValue: Any): Boolean {
        scrollEndEventCallback = propValue as KuiklyRenderCallback
        addOnScrollListener()
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun observeWillEndDrag(propValue: Any): Boolean {
        willEndDragEventCallback = propValue as KuiklyRenderCallback /* = (result: kotlin.Any?) -> kotlin.Unit */
        return true
    }
    private fun setDirectionRow(propValue: Any): Boolean {
        directionRow = (propValue as Int) == 1
        return true
    }

    private fun setPagingEnable(propValue: Any): Boolean {
        pageEnable = (propValue as Int) == 1
        return true
    }

    private fun showScrollerIndicator(propValue: Any): Boolean {
        val enable = (propValue as Int) == 1
        isHorizontalScrollBarEnabled = enable
        isVerticalScrollBarEnabled = enable
        return true
    }

    private fun setScrollEnabled(propValue: Any): Boolean {
        scrollEnabled = (propValue as Int) == 1
        return true
    }

    fun isScrollEnabled() = scrollEnabled

    private fun setBouncesEnable(propValue: Any): Boolean {
        bouncesEnable = (propValue as Int) == 1
        if (bouncesEnable) {
            if (childCount > 0) {
                setupOverscrollHandler(contentView)
            }
        } else {
            overScrollHandler = null
        }
        return true
    }

    private fun setFlingEnable(propValue: Any): Boolean {
        supportFling = (propValue as Int) == 1
        return true
    }

    private fun setScrollWithParent(propValue: Any): Boolean {
        scrollWithParent = (propValue as Int) == 1
        return true
    }

    fun isScrollWithParent() = scrollWithParent

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_CONTENT_OFFSET -> setContentOffset(params)
            METHOD_CONTENT_INSET_WHEN_END_DRAG -> contentInsetWhenEndDrag(params)
            METHOD_CONTENT_INSET -> contentInset(params)
            METHOD_ABORT_CONTENT_OFFSET_ANIMATE -> stopScroll()
            else -> super.call(method, params, callback)
        }
    }

    override fun setScrollingTouchSlop(slopConstant: Int) {
        scrollConflictHandler.setScrollingTouchSlop(slopConstant)
        super.setScrollingTouchSlop(slopConstant)
    }

    override fun draw(c: Canvas) {
        drawCommonDecoration(c)
        super.draw(c)
        drawCommonForegroundDecoration(c)
    }

    override fun addView(child: View, index: Int) {
        if (adapter == null) {
            setupAdapter(child)
        } else {
            super.addView(child, index)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        tryApplyPendingSetContentOffset()
        tryApplyPendingFireOnScroll()
    }

    override fun onDestroy() {
        super.onDestroy()
        nestedHorizontalChildInterceptor?.also { interceptor ->
            closestHorizontalRecyclerViewParent?.removeNestedChildInterceptEventListener(interceptor)
        }

        nestedVerticalChildInterceptor?.also { interceptor ->
            closestVerticalRecyclerViewParent?.removeNestedChildInterceptEventListener(interceptor)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return if (overScrollHandler?.forceOverScroll == true) {
            val r = super.dispatchTouchEvent(ev)
            touchDelegate?.dispatchHRRecyclerViewTouchEvent(ev)
            r
        } else {
            super.dispatchTouchEvent(ev)
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
    	if (touchConsumeByKuikly) {
            return true;
        }
        if (!scrollEnabled || mNestedScrollAxesTouch != SCROLL_AXIS_NONE) {
            return false
        }

        if (dispatchNestedChildInterceptTouchEvent(e)) {
            return false
        }

        if (directionRow && enableSmallTouchSlop) {
            val r = super.onInterceptTouchEvent(e)
            if (r) {
                parent.requestDisallowInterceptTouchEvent(true)
            } else {
                parent.requestDisallowInterceptTouchEvent(false)
            }
            return r
        }

        val result = touchDelegate?.interceptHRRecyclerViewTouchEvent(e) ?: false
        if (result) {
            parent.requestDisallowInterceptTouchEvent(true)
            return true
        }

        return if (scrollConflictHandler.onInterceptTouchEvent(e, this)) {
            false
        } else {
            super.onInterceptTouchEvent(e)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (touchConsumeByKuikly) {
            return true;
        }

        if (!scrollEnabled) {
            return false
        }
        return if (overScrollHandler?.onTouchEvent(e) == true) {
            true
        } else {
            super.onTouchEvent(e)
        }
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        if (overScrollHandler?.overScrolling != true) { // over scroll 时, willDragEnd 由 over scroll handler 处理
            fireWillDragEndEvent(velocityX, velocityY)
        }
        if (!supportFling || skipFlingIfNestOverScroll) {
            // 由于没有调用super.fling(velocityX, velocityY)
            // 导致 RV 内部的状态一直都 DRAGGING，因此在 onInterceptEvent的时候，RV 内部一直拦截事件
            // 导致 RV 内部的横向子 List 无法滑动
            // 触发条件：先在横向子 List 滑动然后触发 cancel
            // forceSetScrollState(SCROLL_STATE_IDLE)
            return true
        }
        return super.fling(velocityX, velocityY)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        trySetupClosestHorizontalRecyclerViewParent()
        trySetupClosestVerticalRecyclerViewParent()
    }

    private fun forceSetScrollState(state: Int) {
        try {
            val f = RecyclerView::class.java.getDeclaredField("mScrollState")
            f.isAccessible = true
            f.set(this, state)
        } catch (e: Exception) {
            // 由于不清楚系统mTouchSlop底层会抛出哪种类型的异常，因此这里使用顶层异常来处理
            // 并且异常不影响主路径
            KuiklyRenderLog.e(VIEW_NAME, "set mTouchSlop error, $e")
        }
    }

    private fun addOnScrollListener() {
        if (scrollListener != null) {
            return
        }

        scrollListener = object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val offset = if (directionRow) {
                    dx
                } else {
                    dy
                }
                if (offset == 0) {
                    return
                }
                val forceOverScrolling = overScrollHandler?.forceOverScroll ?: false
                if (!forceOverScrolling) {
                    fireScrollEvent()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                // 由于setScrollState内部在dispatchOnScrollStateChanged之前可能会再次调用setScrollState，
                // 导致dispatchOnScrollStateChanged逆序回调，因此newState的值不可靠，需要从getScrollState重新获取
                val currentState = recyclerView.scrollState
                if (!touchConsumeByNative) {
                    touchConsumeByNative = newState != SCROLL_STATE_IDLE
                }
                if (overScrollHandler?.forceOverScroll == true) {
                    return
                }
                if (isIdeaStateToDraggingState(currentState) || isSettlingStateToDraggingState(currentState)) {
                    isDragging = true
                    fireBeginDragEvent()
                }

                if (isDraggingStateToSettlingState(currentState) || isDraggingStateToIdeaState(currentState)
                ) {
                    isDragging = false
                    fireEndDragEvent()
                }

                if (isEndState(currentState)) {
                    fireEndScrollEvent()
                }
                preScrollState = currentState
            }
        }.apply {
            addOnScrollListener(this)
        }
    }

    private fun fireBeginDragEvent() {
        needFireWillEndDragEvent = true
        dispatchOnBeginDrag(contentOffsetX, contentOffsetY)
        dragBeginEventCallback?.invoke(getCommonScrollParams())

    }

    private fun fireEndDragEvent() {
        if (needFireWillEndDragEvent) {
            // onFling回调，RecyclerView 内部只有在 velocity > 系统的minVelocity
            // 才会回调, 因此当加速度小于 minVelocity时, 在这里补充回调
            // 此时加速度介于 0 ~ minVelocity之间，这里直接给个 0给 kuikly 侧
            fireWillDragEndEvent(0, 0)
        }
        dispatchOnEndDrag(contentOffsetX, contentOffsetY)
        dragEndEventCallback?.invoke(getCommonScrollParams())
    }

    private fun automaticAdjustContentOffset() {
        if (!isContentViewAttached) {
            return
        }

        pendingFireOnScroll = true
    }

    private fun fireScrollEvent() {
        val cv = contentView
        val offsetX = (-cv.left.toFloat())
        val offsetY = (-cv.top.toFloat()) - contentView.translationY

        if (contentOffsetX == offsetX && contentOffsetY == offsetY) {
            return
        }

        contentOffsetX = offsetX
        contentOffsetY = offsetY
        dispatchOnScroll(offsetX, offsetY)

        val callback = scrollEventCallback ?: return
        callback.invoke(getCommonScrollParams())
    }

    private fun fireWillDragEndEvent(velocityX: Int, velocityY: Int) {
        needFireWillEndDragEvent = false
        val callback = willEndDragEventCallback ?: return
        val paramsMap = getCommonScrollParams()
        paramsMap[VELOCITY_X] = velocityX / MILLISECOND
        paramsMap[VELOCITY_Y] = velocityY / MILLISECOND

        overScrollHandler?.also {
            if (it.overScrolling) {
                paramsMap[OFFSET_X] = kuiklyRenderContext.toDpF(toOverScrollOffset(contentView, it.overScrollX, it.isInStart(), false))
                paramsMap[OFFSET_Y] = kuiklyRenderContext.toDpF(toOverScrollOffset(contentView, it.overScrollY, it.isInStart(), true))
            }
        }
        callback.invoke(paramsMap)
    }

    private fun fireEndScrollEvent() {
        scrollEndEventCallback?.invoke(getCommonScrollParams())
    }

    private fun getCommonScrollParams(): MutableMap<String, Any> {
        val offsetMap = mutableMapOf<String, Any>()
        if (!isContentViewAttached) {
            return offsetMap
        }
        val cv = contentView
        val offsetX = (-cv.left.toFloat())
        val offsetY = (-cv.top.toFloat())
        offsetMap[OFFSET_X] = kuiklyRenderContext.toDpF(offsetX)
        offsetMap[OFFSET_Y] = kuiklyRenderContext.toDpF(offsetY)
        offsetMap[CONTENT_WIDTH] = kuiklyRenderContext.toDpF(cv.frameWidth.toFloat())
        offsetMap[CONTENT_HEIGHT] = kuiklyRenderContext.toDpF(cv.frameHeight.toFloat())
        offsetMap[VIEW_WIDTH] = kuiklyRenderContext.toDpF(frameWidth.toFloat())
        offsetMap[VIEW_HEIGHT] = kuiklyRenderContext.toDpF(frameHeight.toFloat())
        offsetMap[IS_DRAGGING] = getIsDragging(isDragging)
        return offsetMap
    }

    private fun setupAdapter(contentView: View) {
        (contentView as? KRRecyclerContentView)?.also {
            it.addChildCallback = { child ->
                dispatchOnContentViewAddChild(contentView, child)
            }
        }
        adapter = KRRecyclerViewAdapter(contentView as KRRecyclerContentView)
        layoutManager =
            LinearLayoutManager(context, if (directionRow) HORIZONTAL else VERTICAL, false)
        if (pageEnable) {
            KRPagerSnapHelper { index ->

            }.attachToRecyclerView(this)
        }
        if (bouncesEnable) {
            setupOverscrollHandler(contentView)
        }
    }

    private fun setupOverscrollHandler(contentView: View) {
        overScrollHandler = OverScrollHandler(this,
            contentView,
            !directionRow,
            object : OverScrollEventCallback {
                override fun onBeginDragOverScroll(
                    offsetX: Float,
                    offsetY: Float,
                    overScrollStart: Boolean,
                    isDragging: Boolean
                ) {
                    if (isContentViewAttached) {
                        touchConsumeByNative = true
                        fireOverScrollBeginDragEvent(offsetX, offsetY, overScrollStart)
                    }
                }

                override fun onOverScroll(
                    offsetX: Float,
                    offsetY: Float,
                    overScrollStart: Boolean,
                    isDragging: Boolean
                ) {
                    if (isContentViewAttached) { // setupAdapter调用后，RV不会立刻addView
                        fireOverScrollEvent(offsetX, offsetY, overScrollStart, isDragging)
                    }
                }

                override fun onEndDragOverScroll(
                    offsetX: Float,
                    offsetY: Float,
                    velocityX: Float,
                    velocityY: Float,
                    overScrollStart: Boolean,
                    isDragging: Boolean
                ) {
                    if (isContentViewAttached) {
                        fireOverScrollEndDragEvent(
                            offsetX,
                            offsetY,
                            velocityX,
                            velocityY,
                            overScrollStart
                        )
                    }
                }

            })
    }

    private fun toOverScrollOffset(
        contentView: View,
        offset: Float,
        overScrollStart: Boolean,
        isVertical: Boolean
    ): Float {
        return if (overScrollStart) {
            -offset
        } else {
            (-offset + if (isVertical) {
                -contentView.top
            } else {
                -contentView.left
            })
        }
    }

    private fun fireOverScrollBeginDragEvent(offsetX: Float, offsetY: Float, overScrollStart: Boolean) {
        contentOffsetX = toOverScrollOffset(contentView,
            offsetX,
            overScrollStart,
            !directionRow)
        contentOffsetY = toOverScrollOffset(contentView,
            offsetY,
            overScrollStart,
            !directionRow)
        needFireWillEndDragEvent = true

        val callback = dragBeginEventCallback ?: return

        dispatchOnBeginDrag(contentOffsetX, contentOffsetY)
        val paramsMap = getCommonScrollParams()
        paramsMap[OFFSET_X] = kuiklyRenderContext.toDpF(contentOffsetX)
        paramsMap[OFFSET_Y] = kuiklyRenderContext.toDpF(contentOffsetY)
        callback(paramsMap)
    }

    private fun fireOverScrollEvent(
        offsetX: Float,
        offsetY: Float,
        overScrollStart: Boolean,
        isDragging: Boolean
    ) {
        this.isDragging = isDragging
        val cv = contentView
        contentOffsetX = toOverScrollOffset(cv, offsetX, overScrollStart, false)
        contentOffsetY = toOverScrollOffset(cv, offsetY, overScrollStart, true)

        dispatchOnScroll(contentOffsetX, contentOffsetY)

        val callback = scrollEventCallback ?: return
        val paramsMap = getCommonScrollParams()
        paramsMap[OFFSET_X] = kuiklyRenderContext.toDpF(contentOffsetX)
        paramsMap[OFFSET_Y] = kuiklyRenderContext.toDpF(contentOffsetY)
        callback.invoke(paramsMap)
    }

    private fun fireOverScrollEndDragEvent(
        offsetX: Float,
        offsetY: Float,
        velocityX: Float,
        velocityY: Float,
        overScrollStart: Boolean
    ) {
        contentOffsetX = toOverScrollOffset(contentView,
            offsetX,
            overScrollStart,
            false)
        contentOffsetY = toOverScrollOffset(contentView,
            offsetY,
            overScrollStart,
            true)
        if (needFireWillEndDragEvent) {
            fireWillDragEndEvent(velocityX.toInt(), velocityY.toInt())
        }
        val callback = dragEndEventCallback ?: return
        dispatchOnEndDrag(contentOffsetX, contentOffsetY)
        val paramsMap = getCommonScrollParams()
        paramsMap[OFFSET_X] = kuiklyRenderContext.toDpF(contentOffsetX)
        paramsMap[OFFSET_Y] = kuiklyRenderContext.toDpF(contentOffsetY)
        callback(paramsMap)
    }

    private fun setContentOffset(value: String?) {
        val rvLayoutManager = layoutManager
        if (rvLayoutManager == null || !isContentViewAttached) { // 还没设置contentView，所以layoutManager为null，等Layout完再apply
            pendingSetContentOffsetStr = value ?: KRCssConst.EMPTY_STRING
            return
        }

        val params = value ?: return
        val isVertical = rvLayoutManager.canScrollVertically()
        val contentOffsetSplits = params.split(KRCssConst.BLANK_SEPARATOR)
        var offsetX = kuiklyRenderContext.toPxI(contentOffsetSplits[0].toFloat())
        var offsetY = kuiklyRenderContext.toPxI(contentOffsetSplits[1].toFloat())
        val animate = contentOffsetSplits[2] == "1" // "1"为以动画的形式滚动
        var isExpand = true
        if (contentOffsetSplits.size >= 4) {
            isExpand = contentOffsetSplits[3] == "1"
        }

        val originOffsetY = offsetY
        val originOffsetX = offsetX

        pendingSetContentOffsetStr = if (canScrollImmediately(originOffsetX, originOffsetY, isExpand)) {
            internalSetContentOffset(originOffsetX,
                originOffsetY,
                offsetX,
                offsetY,
                isVertical,
                animate)
            KRCssConst.EMPTY_STRING
        } else {
            // KTV侧有可能先更改了contentView的高度或者宽度后, setContentOffset. 此时应该等Layout完后才设置offset
            value
        }
    }

    private fun setNestedScroll(propValue: Any): Boolean {
        if (propValue is String) {
            JSONObject(propValue).apply {
                scrollForwardMode = getNestScrollMode(optString("forward", ""))
                scrollBackwardMode = getNestScrollMode(optString("backward", ""))
            }
        }
        return true
    }

    private fun getNestScrollMode(rule: String): KRNestedScrollMode {
        return when (rule) {
            "" -> KRNestedScrollMode.SELF_FIRST
            else -> KRNestedScrollMode.valueOf(rule)
        }
    }

    private fun internalSetContentOffset(
        originOffsetX: Int,
        originOffsetY: Int,
        offsetX: Int,
        offsetY: Int,
        isVertical: Boolean,
        animate: Boolean
    ) {
        if (isContentViewAttached) {
            var dx = 0
            var dy = 0
            var ox = offsetX
            var oy = offsetY

            if (oy < 0) { // 强制设置为0，负数部分由OverScrollHandler处理
                oy = 0
            }
            if (ox < 0) { // 强制设置为0，负数部分由OverScrollHandler处理
                ox = 0
            }

            if (isVertical) {
                dy = oy - (-contentView.top)
            } else {
                dx = ox - (-contentView.left)
            }
            if (animate) {
                smoothScrollBy(dx, dy)
            } else {
                scrollBy(dx, dy)
            }

            // 超出部分, 使用OverScrollHandler来滚动
            if (isVertical) {
                setVerticalContentOffsetByOverScrollHandler(originOffsetY,
                    frameHeight,
                    contentView.frameHeight,
                    animate)
            } else {
                setHorizontalContentOffsetByOverScrollHandler(originOffsetX,
                    frameWidth,
                    contentView.frameWidth,
                    animate)
            }
        }
    }

    private fun canScrollImmediately(offsetX: Int, offsetY: Int, isExpand: Boolean): Boolean {
        if (!isExpand) {
            // 非扩容情况，默认支持滚动
            return true
        }
        return if (directionRow) {
            offsetX <= contentView.width - width
        } else {
            offsetY <= contentView.height - height
        }
    }

    private fun tryApplyPendingSetContentOffset() {
        if (!isContentViewAttached) {
            return
        }

        if (pendingSetContentOffsetStr.isNotEmpty()) {
            setContentOffset(pendingSetContentOffsetStr)
            pendingSetContentOffsetStr = KRCssConst.EMPTY_STRING
        }
    }

    private fun tryApplyPendingFireOnScroll() {
        if (!isContentViewAttached) {
            return
        }
        if (pendingFireOnScroll) {
            fireScrollEvent()
            pendingFireOnScroll = false
        }
    }

    private fun setVerticalContentOffsetByOverScrollHandler(
        offsetY: Int,
        rvHeight: Int,
        contentHeight: Int,
        animate: Boolean
    ) {

        val scrollOffsetY = if (offsetY < 0) {
            offsetY
        } else if (offsetY > 0 && contentHeight <= rvHeight) {
            offsetY
        } else if (offsetY > 0 && (offsetY + rvHeight) >= contentHeight) {
            offsetY + rvHeight - contentHeight
        } else {
            -1
        }

        if (scrollOffsetY != -1) {
            setContentOffsetByOverScrollHandler(0, scrollOffsetY, animate)
        }
    }

    private fun setHorizontalContentOffsetByOverScrollHandler(
        offsetX: Int,
        rvWidth: Int,
        contentWidth: Int,
        animate: Boolean
    ) {
        val scrollOffsetX = if (offsetX < 0) {
            offsetX
        } else if (offsetX > 0 && contentWidth <= rvWidth) {
            offsetX
        } else if (offsetX > 0 && (offsetX + rvWidth) >= contentWidth) {
            offsetX + rvWidth - contentWidth
        } else {
            -1
        }
        if (scrollOffsetX != -1) {
            setContentOffsetByOverScrollHandler(scrollOffsetX, 0, animate)
        }
    }

    /**
     * 使用[OverScrollHandler]来处理边缘滚动的contentOffset
     */
    private fun setContentOffsetByOverScrollHandler(
        offsetX: Int,
        offsetY: Int,
        animate: Boolean
    ) {
        val contentInset = KRRecyclerContentViewContentInset(kuiklyRenderContext, KRCssConst.EMPTY_STRING).apply {
            top = -offsetY.toFloat()
            left = -offsetX.toFloat()
            this.animate = animate
        }
        if (animate) {
            postDelayed({
                overScrollHandler?.bounceWithContentInset(contentInset)
            }, 0)
        } else {
            overScrollHandler?.bounceWithContentInset(contentInset)
        }
    }

    /**
     * 设置OverScroll后, 停留的内容边距
     * @param contentInset 内容边距
     */
    private fun contentInsetWhenEndDrag(contentInset: String?) {
        val ci = contentInset ?: return
        overScrollHandler?.contentInsetWhenEndDrag = KRRecyclerContentViewContentInset(kuiklyRenderContext, ci)
    }

    /**
     * 设置当前内容边距
     * @param contentInset 内容边距
     */
    private fun contentInset(contentInset: String?) {
        val ci = contentInset ?: return
        overScrollHandler?.bounceWithContentInset(KRRecyclerContentViewContentInset(kuiklyRenderContext, ci))
    }

    private fun dispatchOnScroll(offsetX: Float, offsetY: Float) {
        for (listener in krRecyclerViewListeners) {
            listener.onScroll(offsetX, offsetY)
        }
    }

    private fun dispatchOnBeginDrag(offsetX: Float, offsetY: Float) {
        for (listener in krRecyclerViewListeners) {
            listener.onBeginDrag(offsetX, offsetY)
        }
    }

    private fun dispatchOnEndDrag(offsetX: Float, offsetY: Float) {
        for (listener in krRecyclerViewListeners) {
            listener.onEndDrag(offsetX, offsetY)
        }
    }

    private fun dispatchOnContentViewAddChild(
        contentView: KRRecyclerContentView,
        contentViewChild: View
    ) {
        for (listener in krRecyclerViewListeners) {
            listener.onContentViewAddChild(contentView, contentViewChild)
        }
    }

    private fun getIsDragging(isDragging: Boolean): Int = if (isDragging) 1 else 0

    private fun isIdeaStateToDraggingState(newState: Int): Boolean =
        preScrollState == SCROLL_STATE_IDLE && newState == SCROLL_STATE_DRAGGING

    private fun isSettlingStateToDraggingState(newState: Int): Boolean =
        preScrollState == SCROLL_STATE_SETTLING && newState == SCROLL_STATE_DRAGGING

    private fun isDraggingStateToIdeaState(newState: Int): Boolean =
        preScrollState == SCROLL_STATE_DRAGGING && newState == SCROLL_STATE_IDLE

    private fun isDraggingStateToSettlingState(newState: Int): Boolean =
        preScrollState == SCROLL_STATE_DRAGGING && newState == SCROLL_STATE_SETTLING

    private fun isEndState(newState: Int): Boolean =
        preScrollState != SCROLL_STATE_IDLE && newState == SCROLL_STATE_IDLE

    private fun dispatchNestedChildInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        val interceptors = nestedChildInterceptEventListeners ?: return false
        for (interceptor in interceptors) {
            val result = interceptor.onInterceptTouchEvent(motionEvent)
            if (result) {
                return true
            }
        }
        return false
    }

    private fun trySetupClosestHorizontalRecyclerViewParent() {
        if (!directionRow) {
            return
        }

        findClosestHorizontalRecyclerViewParent()?.also {
            closestHorizontalRecyclerViewParent = it
            val interceptor = NestedHorizontalChildInterceptor(this)
            nestedHorizontalChildInterceptor = interceptor
            it.addNestedChildInterceptEventListener(interceptor)
        }
    }

    private fun trySetupClosestVerticalRecyclerViewParent() {
        if (directionRow) {
            return
        }

        findClosestVerticalRecyclerViewParent()?.also {
            closestVerticalRecyclerViewParent = it
            val interceptor = NestedVerticalChildInterceptor(this)
            nestedVerticalChildInterceptor = interceptor
            it.addNestedChildInterceptEventListener(interceptor)
        }
    }

    private fun findClosestHorizontalRecyclerViewParent(): KRRecyclerView? {
        var rv: KRRecyclerView? = null
        var parent: ViewGroup? = parent as? ViewGroup
        while (parent != null) {
            if (parent is KRRecyclerView && parent.directionRow && (parent.directionRow == directionRow)) {
                rv = parent
                break
            }
            if (parent == rootView) {
                break
            }
            parent = parent.parent as? ViewGroup
        }
        return rv
    }

    private fun findClosestVerticalRecyclerViewParent(): KRRecyclerView? {
        var rv: KRRecyclerView? = null
        var parent: ViewGroup? = parent as? ViewGroup
        while (parent != null) {
            if (parent is KRRecyclerView && !parent.directionRow && parent.directionRow == directionRow) {
                rv = parent
                break
            }
            if (parent == rootView) {
                break
            }
            parent = parent.parent as? ViewGroup
        }
        return rv
    }

    override fun requestChildRectangleOnScreen(child: View, rect: Rect, immediate: Boolean): Boolean {
        // 分页模式下，不响应requestChildRectangleOnScreen，否则可能无法划到下一页
        if (pageEnable) {
            return false
        }
        return super.requestChildRectangleOnScreen(child, rect, immediate)
    }

    companion object {
        const val VIEW_NAME = "KRListView"
        const val VIEW_NAME_SCROLL_VIEW = "KRScrollView"

        private const val DRAG_BEGIN = "dragBegin"
        private const val DRAG_END = "dragEnd"
        private const val SCROLL = "scroll"
        private const val SCROLL_END = "scrollEnd"
        private const val WILL_DRAG_END = "willDragEnd"
        private const val DIRECTION_ROW = "directionRow"
        private const val PAGING_ENABLED = "pagingEnabled"
        private const val SHOW_SCROLLER_INDICATOR = "showScrollerIndicator"
        private const val SCROLL_ENABLED = "scrollEnabled"
        private const val VERTICAL_BOUNCES = "verticalbounces"
        private const val HORIZONTAL_BOUNCES = "horizontalbounces"
        private const val BOUNCES_ENABLE = "bouncesEnable"
        private const val LIMIT_HEADER_BOUNCES = "limitHeaderBounces"
        private const val FLING_ENABLE = "flingEnable"
        private const val SCROLL_WITH_PARENT = "scrollWithParent"

        private const val METHOD_CONTENT_OFFSET = "contentOffset" // 设置内容的偏移量，会把List滚到对应的位置
        private const val METHOD_CONTENT_INSET_WHEN_END_DRAG =
            "contentInsetWhenEndDrag" // 结束拖拽时，设置的ContentInset
        private const val METHOD_CONTENT_INSET = "contentInset" // 设置内容边距
        private const val METHOD_ABORT_CONTENT_OFFSET_ANIMATE = "abortContentOffsetAnimate" // 停止滚动动画
        private const val NESTED_SCROLL = "nestedScroll"

        private const val OFFSET_X = "offsetX"
        private const val OFFSET_Y = "offsetY"
        private const val CONTENT_WIDTH = "contentWidth"
        private const val CONTENT_HEIGHT = "contentHeight"
        private const val VIEW_WIDTH = "viewWidth"
        private const val VIEW_HEIGHT = "viewHeight"
        private const val IS_DRAGGING = "isDragging"
        private const val VELOCITY_X = "velocityX"
        private const val VELOCITY_Y = "velocityY"

        private const val MILLISECOND = 1000f
    }

    private fun computeHorizontallyScrollDistance(dx: Int): Int {
        if (dx < 0) {
            return max(dx.toDouble(), -computeHorizontalScrollOffset().toDouble()).toInt()
        }
        if (dx > 0) {
            val avail = (computeHorizontalScrollRange() - computeHorizontalScrollExtent()
                    - computeHorizontalScrollOffset() - 1)
            return min(dx.toDouble(), avail.toDouble()).toInt()
        }
        return 0
    }

    private fun computeVerticallyScrollDistance(dy: Int): Int {
        if (dy < 0) {
            return max(dy.toDouble(), -computeVerticalScrollOffset().toDouble()).toInt()
        }
        if (dy > 0) {
            if (this.canScrollVertically(dy)) {
               return 0
            } else {
                val avail = (computeVerticalScrollRange() - computeVerticalScrollExtent()
                        - computeVerticalScrollOffset() - 1)
                return min(dy.toDouble(), avail.toDouble()).toInt()
            }
        }
        return 0
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        return onStartNestedScroll(child, target, axes, ViewCompat.TYPE_TOUCH)
    }

    override fun onStartNestedScroll(
        child: View, target: View, axes: Int,
        type: Int
    ): Boolean {
        if (!isScrollEnabled()) {
            return false
        }
        // Determine whether to respond to the nested scrolling event of the child
        val manager = layoutManager ?: return false
        var myAxes = View.SCROLL_AXIS_NONE
        if (manager.canScrollVertically() && (axes and View.SCROLL_AXIS_VERTICAL) != 0) {
            myAxes = myAxes or View.SCROLL_AXIS_VERTICAL
        }
        if (manager.canScrollHorizontally() && (axes and View.SCROLL_AXIS_HORIZONTAL) != 0) {
            myAxes = myAxes or View.SCROLL_AXIS_HORIZONTAL
        }
        if (myAxes != View.SCROLL_AXIS_NONE) {
            if (type == ViewCompat.TYPE_TOUCH) {
                mNestedScrollAxesTouch = myAxes
            } else {
                mNestedScrollAxesNonTouch = myAxes
            }
            return true
        }
        return false
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedScrollAccepted(
        child: View, target: View, axes: Int,
        type: Int
    ) {
        startNestedScroll(
            if (type == ViewCompat.TYPE_TOUCH) mNestedScrollAxesTouch else mNestedScrollAxesNonTouch,
            type
        )
    }

    override fun onStopNestedScroll(child: View) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            mNestedScrollAxesTouch = View.SCROLL_AXIS_NONE
        } else {
            mNestedScrollAxesNonTouch = View.SCROLL_AXIS_NONE
        }
        stopNestedScroll(type)

        overScrollHandler?.let { handler ->
            if ((target as KRRecyclerView).skipFlingIfNestOverScroll) {
                target.skipFlingIfNestOverScroll = false
            }
            if (handler.overScrolling) {
                handler.processBounceBack()
            }
        }
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        onNestedScroll(
            target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
            ViewCompat.TYPE_TOUCH
        )
    }

    override fun onNestedScroll(
        target: View, dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int, type: Int
    ) {
        // Process the current View first
        var dxConsumed = dxConsumed
        var dyConsumed = dyConsumed
        var dxUnconsumed = dxUnconsumed
        var dyUnconsumed = dyUnconsumed
        val myDx = if (dxUnconsumed != 0) computeHorizontallyScrollDistance(dxUnconsumed) else 0
        val myDy = if (dyUnconsumed != 0) computeVerticallyScrollDistance(dyUnconsumed) else 0

        val consumed = intArrayOf(0, 0)
        if (target is KRRecyclerView) {
            scrollParentIfNeeded(target, myDx, myDy, consumed, type)
        }
        if (consumed[0] != 0 || consumed[1] != 0) {
            dxConsumed += consumed[0]
            dyConsumed += consumed[1]
            dxUnconsumed -= consumed[0]
            dyUnconsumed -= consumed[1]
        }

        // Then dispatch to the parent for processing
        val parentDx = dxUnconsumed
        val parentDy = dyUnconsumed
        if (parentDx != 0 || parentDy != 0) {
            dispatchNestedScroll(dxConsumed, dyConsumed, parentDx, parentDy, null, type)
        }

        // 在整个嵌套滚动没法继续消费距离时 停止滚动和FLing
        // FIX: 修复竖向滑动后横向没法立即滑动的问题
        if (directionRow) {
            if (dxConsumed == 0) {
                if(target is KRRecyclerView) {
                    target.stopScroll()
                }
            }
        } else {
            if (dyConsumed == 0) {
                if(target is KRRecyclerView) {
                    target.stopScroll()
                }
            }
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    override fun onNestedPreScroll(
        target: View, dx: Int, dy: Int, consumed: IntArray,
        type: Int
    ) {
        // Dispatch to the parent for processing first
        val parentDx = dx
        val parentDy = dy
        if (parentDx != 0 || parentDy != 0) {
            // Temporarily store `consumed` to reuse the Array
            val consumedX = consumed[0]
            val consumedY = consumed[1]
            consumed[0] = 0
            consumed[1] = 0
            if (target is KRRecyclerView) {
                scrollParentIfNeeded(target ,parentDx, parentDy, consumed, type)
            }
            dispatchNestedPreScroll(parentDx, parentDy, consumed, null, type)
            consumed[0] += consumedX
            consumed[1] += consumedY
        }
    }

    /**
     * 在满足条件的情况下尝试滚动父亲
     *
     * target 子List
     * parentDx 可滑动x轴距离
     * parentDy 可滑动y轴距离
     * consumed 用于返回本次被父亲消费了多少距离
     */
    private fun scrollParentIfNeeded(target: KRRecyclerView,
                                     parentDx: Int,
                                     parentDy: Int,
                                     consumed: IntArray,
                                     touchType: Int) {
        val shouldScrollParentY = when {
            parentDy > 0 && target.scrollForwardMode == KRNestedScrollMode.PARENT_FIRST -> true
            parentDy < 0 && target.scrollBackwardMode == KRNestedScrollMode.PARENT_FIRST -> true
            parentDy > 0 && target.scrollForwardMode == KRNestedScrollMode.SELF_FIRST && !target.canScrollVertically(parentDy) -> true
            parentDy < 0 && target.scrollBackwardMode == KRNestedScrollMode.SELF_FIRST && !target.canScrollVertically(parentDy) -> true
            else -> false
        }

        if (shouldScrollParentY) {
            if (canScrollVertically(parentDy)) {
                scrollBy(0, parentDy)
                consumed[1] = parentDy
            } else {
                if (touchType == ViewCompat.TYPE_TOUCH) {
                    // 只有触摸拖拽模式才处理OverScroll，避免fling直接触发了下拉刷新
                    overScrollHandler?.let{
                        it.setTranslationByNestScrollTouch(parentDy.toFloat())
                        target.skipFlingIfNestOverScroll = true
                        consumed[1] = parentDy
                    }
                }
            }
        }

        val shouldScrollParentX = when {
            parentDx > 0 && target.scrollForwardMode == KRNestedScrollMode.PARENT_FIRST -> true
            parentDx < 0 && target.scrollBackwardMode == KRNestedScrollMode.PARENT_FIRST -> true
            parentDx > 0 && target.scrollForwardMode == KRNestedScrollMode.SELF_FIRST && !target.canScrollHorizontally(parentDx) -> true
            parentDx < 0 && target.scrollBackwardMode == KRNestedScrollMode.SELF_FIRST && !target.canScrollHorizontally(parentDx) -> true
            else -> false
        }

        if (shouldScrollParentX && canScrollHorizontally(parentDx)) {
            scrollBy(parentDx, 0)
            consumed[0] = parentDx
        }
    }

    override fun getNestedScrollAxes(): Int {
        return mNestedScrollAxesTouch or mNestedScrollAxesNonTouch
    }

    private fun smoothScrollWithNestIfNeeded(dx: Int, dy: Int) {
        if (isLayoutSuppressed) {
            return
        }
        layoutManager?.apply {
            var dx = dx
            if (!canScrollHorizontally()) {
                dx = 0
            }
            var dy = dy
            if (!canScrollVertically()) {
                dy = 0
            }
            if (dx != 0 || dy != 0) {
                val withNestedScrolling = isNestScrolling()
                if (withNestedScrolling) {
                    var nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE
                    if (dx != 0) {
                        nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_HORIZONTAL
                    }
                    if (dy != 0) {
                        nestedScrollAxis = nestedScrollAxis or ViewCompat.SCROLL_AXIS_VERTICAL
                    }
                    startNestedScroll(nestedScrollAxis, ViewCompat.TYPE_NON_TOUCH)
                }
                smoothScrollBy(dx, dy)
            }
        }
    }

    private fun isNestScrolling() = nestedScrollAxes != SCROLL_AXIS_NONE
}