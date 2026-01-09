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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.base.Anchor
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.views.KRNestedScrollMode
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ScrollerView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.ScrollParams
import com.tencent.kuikly.core.views.WaterfallList
import com.tencent.kuikly.core.views.WaterfallListView
import kotlin.math.abs
import kotlin.math.roundToInt

// import com.tencent.map.kuiklyapplication.utils.width


public class TMNestStageCardAttr : ComposeAttr() {
    /***
     * 多段页卡高度集合
     * 递增模式
     */
    var levelHeights = listOf(200f, 400f, 600f)

    /**
     * 初始化level
     * 默认小页卡
     */
    var initLevel = 0

    // 顶部通知栏内容
    var topContent: ViewBuilder? = null

    // 空白区域布局里面的内容
    var functionContent: ViewBuilder? = null

    // 吸顶内容
    var headerContent: ViewBuilder? = null

    // 可滚动内容
    // 默认是ListView的样式，瀑布流的也支持
    var scrollContent: ViewBuilder? = null

    // 底部固定的content（跟随滚动有动画） bottomContent是覆盖三段式页卡上面的控件
    var bottomContent: ViewBuilder? = null

    // 底部刷新内容
    var footerContent: ViewBuilder? = null

    // 底部刷新区域宽度
    var footerWidth: Float by observable(0f)

    // 内部悬浮内容的顶部位置
    var innerHoverTop: Float by observable(300f)

    // 手柄距离底部距离
    var handlerMarginBottom: Float by observable(0f)
    var animCompensationHeight: Float by observable(0f)

    // 内部悬浮内容
    var innerHoverContent: ViewBuilder? = null

    // 是否启用底部刷新能力
    var enableFooterRefresh: Boolean = false

    // 顶部下拉刷新内容
    var headerRefreshContent: ViewBuilder? = null

    // 是否启用顶部下拉刷新能力
    var enableHeaderRefresh by observable(false)

    var columnCount: Int = 1

    var itemSpacing: Float = 0f

    var lineSpacing: Float = 0f

    var contentPaddingLeft = 0f
    var contentPaddingRight = 0f
    var contentPaddingBottom = 0f
    var contentPaddingTop = 0f

    /**
     * 列表宽度
     */
    var listWidth: Float = 0f

    // 是否禁用内容滚动
    var disableContentScroll: Boolean = false

    // 组件外层 padding 配置
    var customPaddingLeft: Float = 0f
    var customPaddingRight: Float = 0f
    var customPaddingTop: Float = 0f
    var customPaddingBottom: Float = 0f

    // 是否自定义头部的内容，因为有些场景可能不需要手柄......
    var isCustomHeaderContent by observable(false)

    // 是否启用外部 scroll 滚动
    var enableOuterScroll by observable(true)

    /**
     * 是否启用嵌套滚动
     * 当为true时，启用嵌套滚动机制；当为false时，禁用嵌套滚动
     */
    var nestedScrollEnable by observable(true)

    /**
     * 是否启用内部滚动视图的弹性效果
     * 当为true时，内部滚动视图可以产生弹性效果（bounces）；当为false时，禁用弹性效果
     */
    var enableChildScrollBounces by observable(false)

    /**
     * 自定义边距值
     * 如果设置了此值，将使用该边距值；如果为null，则使用默认值 DEFAULT_MARGIN_VALUE
     */
    var customMarginValue: Float? = null

    /**
     * 是否启用动态边距计算
     * 当为true时，边距会根据卡片高度动态变化（无论是自定义值还是默认值）
     * 当为false时，使用固定边距值（自定义值或默认值）
     */
    var enableDynamicMargin: Boolean = true

    /**
     * 层级
     */
    var bringIndex: Int = 0

    /**
     * 内部悬浮内容的 zIndex 值
     * 用于控制内部悬浮内容的 z 轴层级，值越大层级越高
     * 默认值为 200
     */
    var hoverZIndex: Int = 0

    /**
     * 级别吸附阈值数组
     * 数组长度应该等于 levelHeights.size - 1
     * 每个值的范围是 0.0 到 1.0，表示在两个相邻level之间的阈值比例
     * 例如：[0.3, 0.7] 表示：
     * - 在level0和level1之间，如果滚动位置超过30%则吸附到level1，否则吸附到level0
     * - 在level1和level2之间，如果滚动位置超过70%则吸附到level2，否则吸附到level1
     * 如果不设置或设置为空数组，则使用默认的距离最近原则
     */
    var levelAttachThresholds: List<Float> = listOf()

    /**
     * 速率吸附阈值（像素/毫秒）
     * 当用户手指离开时的滚动速率超过此阈值时，将跳过距离/阈值判断，直接向滚动方向吸附到下一个级别
     * 默认值为0.2f，表示速率超过0.2像素/毫秒时触发速率吸附
     */
    var velocityAttachThreshold: Float = 0.2f

    /**
     * 背景色（浅色模式）
     * 设置整个组件的背景色（包含头部和内容区域）
     */
    var lightBackgroundColor by observable(Color.WHITE)

    /**
     * 背景色（深色模式）
     * 设置整个组件在深色模式下的背景色，如果为null则使用lightBackgroundColor
     */
    var darkBackgroundColor by observable<Color?>(null)

    /**
     * 线性渐变背景方向
     * 设置渐变的方向，如果为null则不使用渐变背景
     */
    var gradientDirection: Direction? by observable(null)

    /**
     * 线性渐变背景颜色停止点
     * 定义渐变的颜色和位置，配合gradientDirection使用
     */
    var gradientColorStops: List<ColorStop> by observable(listOf())

    // 内容设置方法 - 保持单行以提高可读性
    fun topContent(content: ViewBuilder) = apply { this.topContent = content }
    // 页卡上方的区域
    fun functionContent(content: ViewBuilder) = apply { this.functionContent = content }

    fun headerContent(content: ViewBuilder) = apply { this.headerContent = content }
    fun scrollContent(content: ViewBuilder) = apply { this.scrollContent = content }
    fun footerContent(content: ViewBuilder) = apply { this.footerContent = content }
    fun innerHoverContent(content: ViewBuilder) = apply { this.innerHoverContent = content }
    fun headerRefreshContent(content: ViewBuilder) = apply { this.headerRefreshContent = content }

    // 数值设置方法
    fun footerWidth(width: Float) = apply { this.footerWidth = width }
    fun innerHoverTop(top: Float) = apply { this.innerHoverTop = top }

    // 布尔设置方法
    fun enableFooterRefresh(enable: Boolean) = apply { this.enableFooterRefresh = enable }
    fun enableHeaderRefresh(enable: Boolean) = apply { this.enableHeaderRefresh = enable }
    fun bringIndex(enable: Int) = apply { this.bringIndex = enable }
    fun hoverZIndex(zIndex: Int) = apply { this.hoverZIndex = zIndex }
    // 其他配置方法
    fun disableContentScroll(disable: Boolean) = apply { this.disableContentScroll = disable }
    fun levelHeights(content: List<Float>) = apply { this.levelHeights = content }
    fun initLevel(content: Int) = apply { this.initLevel = content }
    fun columnCount(content: Int) = apply { this.columnCount = content }
    fun listWidth(content: Float) = apply { this.listWidth = content }
    fun itemSpacing(content: Float) = apply { this.itemSpacing = content }
    fun lineSpacing(content: Float) = apply { this.lineSpacing = content }
    fun bottomContent(content: ViewBuilder) = apply { this.bottomContent = content }
    fun handlerMarginBottom(content: Float) = apply { this.handlerMarginBottom = content }
    fun animCompensationHeight(content: Float) = apply { this.animCompensationHeight = content }

    /**
     * 设置组件外层内边距（统一设置）
     * @param padding 内边距值
     */
    fun customPadding(padding: Float) = apply {
        this.customPaddingLeft = padding
        this.customPaddingRight = padding
        this.customPaddingTop = padding
        this.customPaddingBottom = padding
    }

    /**
     * 设置组件外层内边距（水平和垂直）
     * @param horizontal 水平内边距
     * @param vertical 垂直内边距
     */
    fun customPadding(horizontal: Float, vertical: Float) = apply {
        this.customPaddingLeft = horizontal
        this.customPaddingRight = horizontal
        this.customPaddingTop = vertical
        this.customPaddingBottom = vertical
    }

    /**
     * 设置组件外层内边距（分别设置）
     * @param left 左侧内边距
     * @param top 顶部内边距
     * @param right 右侧内边距
     * @param bottom 底部内边距
     */
    fun customPadding(left: Float, top: Float, right: Float, bottom: Float) = apply {
        this.customPaddingLeft = left
        this.customPaddingTop = top
        this.customPaddingRight = right
        this.customPaddingBottom = bottom
    }

    /**
     * 设置左侧内边距
     */
    fun customPaddingLeft(padding: Float) = apply { this.customPaddingLeft = padding }

    /**
     * 设置右侧内边距
     */
    fun customPaddingRight(padding: Float) = apply { this.customPaddingRight = padding }

    /**
     * 设置顶部内边距
     */
    fun customPaddingTop(padding: Float) = apply { this.customPaddingTop = padding }

    /**
     * 设置底部内边距
     */
    fun customPaddingBottom(padding: Float) = apply { this.customPaddingBottom = padding }

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

    /**
     * 设置自定义边距值
     * @param marginValue 边距值
     */
    fun customMarginValue(marginValue: Float) = apply {
        this.customMarginValue = marginValue
    }

    /**
     * 启用或禁用动态边距计算
     * @param enable 是否启用动态边距，默认为true
     */
    fun enableDynamicMargin(enable: Boolean) = apply {
        this.enableDynamicMargin = enable
    }

    /**
     * 设置背景色（单色模式）
     * @param color 组件的背景色，同时用于浅色和深色模式
     */
    fun dmBackgroundColor(color: Color) = apply {
        this.lightBackgroundColor = color
        this.darkBackgroundColor = null
    }

    /**
     * 设置背景色（双色模式）
     * @param lightColor 浅色模式下的背景色
     * @param darkColor 深色模式下的背景色
     */
    fun dmBackgroundColor(lightColor: Color, darkColor: Color) = apply {
        this.lightBackgroundColor = lightColor
        this.darkBackgroundColor = darkColor
    }

    /**
     * 设置线性渐变背景
     * @param direction 渐变方向
     * @param colorStops 颜色停止点，可变参数
     * 
     * 使用示例：
     * backgroundLinearGradient(
     *     Direction.TO_RIGHT,
     *     ColorStop(Color.RED, 0f),
     *     ColorStop(Color.GREEN, 0.3f),
     *     ColorStop(Color.BLACK, 1f)
     * )
     */
    fun cardBackgroundLinearGradient(direction: Direction, vararg colorStops: ColorStop) = apply {
        this.gradientDirection = direction
        this.gradientColorStops = colorStops.toList()
        // 清除纯色背景设置，避免冲突
        this.lightBackgroundColor = Color.TRANSPARENT
        this.darkBackgroundColor = null
    }

    /**
     * 清除渐变背景设置
     */
    fun clearGradientBackground() = apply {
        this.gradientDirection = null
        this.gradientColorStops = listOf()
    }

    /**
     * 设置是否启用嵌套滚动
     * @param enable 是否启用嵌套滚动，默认为true
     */
    fun nestedScrollEnable(enable: Boolean) = apply {
        this.nestedScrollEnable = enable
    }

    /**
     * 设置是否启用内部滚动视图的弹性效果
     * @param enable 是否启用弹性效果，默认为true
     */
    fun enableChildScrollBounces(enable: Boolean) = apply {
        this.enableChildScrollBounces = enable
    }

    /**
     * 设置级别吸附阈值数组
     * @param thresholds 阈值数组，每个值范围0.0-1.0，数组长度应该等于levelHeights.size-1
     * 例如：levelAttachThresholds(listOf(0.3f, 0.7f))
     * 表示在level0-1之间30%阈值，level1-2之间70%阈值
     */
    fun levelAttachThresholds(thresholds: List<Float>) = apply {
        // 验证阈值范围
        val validThresholds = thresholds.filter { it in 0.0f..1.0f }
        this.levelAttachThresholds = validThresholds
    }

    /**
     * 设置速率吸附阈值
     * @param threshold 速率阈值（像素/毫秒），当滚动速率超过此值时触发速率吸附
     * 例如：velocityAttachThreshold(0.8f) 表示速率超过0.8像素/毫秒时触发速率吸附
     */
    fun velocityAttachThreshold(threshold: Float) = apply {
        this.velocityAttachThreshold = threshold
    }

    /**
     * 滚动事件限流时间（毫秒）
     * 控制滚动事件的发送频率，避免过于频繁的事件触发
     * 默认值为10ms
     */
    var scrollEventThrottleMs: Long = 10L

    /**
     * 设置滚动事件限流时间
     * @param throttleMs 限流时间（毫秒），默认为10ms
     */
    fun scrollEventThrottleMs(throttleMs: Long) = apply {
        this.scrollEventThrottleMs = throttleMs
    }

}


/**
 * 分段式页卡组件
 */
public class TMNestStageCardView : ComposeView<TMNestStageCardAttr, TMNestStageCardViewEvent>() {

    var outerScrollView: ViewRef<ScrollerView<*, *>>? = null
    var innerScrollView: ViewRef<WaterfallListView>? = null
    var footerRefreshView: ViewRef<FooterRefreshView>? = null
    var headerRefreshRef: ViewRef<RefreshView>? = null

    // 优化：只保留真正需要触发 UI 重新计算的 observable 变量
    var headerRefreshState: RefreshViewState by observable(RefreshViewState.IDLE)
    var currentLevel: Int by observable(0)
    var footerRefreshState: FooterRefreshState by observable(FooterRefreshState.IDLE)
    var screenHeight: Float by observable(0f)
    var isInitialized: Boolean by observable(false)
    var isOutDragging: Boolean by observable(false)

    /**
     * 基础配置更新
     */
    private var baseConfigChangeFlag: Boolean by observable(false)

    // 优化：改为 private var - 不触发 UI 重新计算（高频更新的变量）
    private var curOuterScrollOffset: Float = 0f

    // 存储最后的滚动速率（用于速率吸附判断）
    private var lastScrollVelocityY: Float = 0f

    // 用于手动计算速率的变量
    private var lastScrollOffset: Float = 0f
    private var lastScrollTime: Long = 0L

    // 拖拽期间速率计算变量
    private var dragBeginOffset: Float = 0f
    private var dragBeginTime: Long = 0L
    private var dragEndOffset: Float = 0f
    private var dragEndTime: Long = 0L
    private var finalDragVelocity: Float = 0f


    // 优化：改为 private var - 不触发 UI 重新计算（高频更新的变量）
    private var scrollContentHeight: Float by observable(0f)
    private var cardHeight: Float = 0f

    // 实时高度
    private var stickContentHeight = 0f

    // 实时高度
    private var headerContentHeight = 0f

    // 手柄之上的高度
    private var topContentHeight = 0f

    // bottom的高度
    private var bottomContentHeight: Float = 0.001f

    // 内部scroll的placeHolder高度
    private var innerScrollPlaceHolderHeight: Float by observable(0f)

    // 渲染时用的高度
    private var stickContentHeightUse: Float by observable(0f)
    var marginValue by observable(0f)
    private var childOffset: Float = 0f
    private var canScroll: Boolean = true
    private var bottomCornerRadius: Float = 20f
    private var previousScrollOffset: Float = 0f
    private var isChildFlingEnable: Boolean by observable(false)

    // 优化：缓存计算结果 - 避免重复计算
    private var cachedHeightRatio: Float = 0f
    private var cachedMarginValue: Float = DEFAULT_MARGIN_VALUE
    private var lastEmitTime: Long = 0L
    private var lastEmittedOffset: Float = 0f
    private var lastEmittedCardHeight: Float = 0f
    private var isFirstLoad: Boolean = true

    // 优化：borderRadius 缓存
    private var cachedTopBorderRadius: Float = 32f
    private var cachedBottomBorderRadius by observable(44f)
    private var lastHeightForRadius: Float = 0f
    // 卡片的mask背景色
    private var cardMaskColor by observable(Color.TRANSPARENT)



    // 优化：内层滚动节流
    private var lastChildScrollTime: Long = 0L

    companion object {
        private const val TAG = "TMNestStageCardView"

        /**
         * 默认边距值常量
         */
        const val DEFAULT_MARGIN_VALUE = 8f

        /**
         * 滚动偏移变化阈值（像素）
         * 只有当偏移变化超过此值时才发送事件，避免微小抖动
         */
        private const val OFFSET_CHANGE_THRESHOLD = 0.5f

        /**
         * 添加容差
         */
        val OFFSET_TOLERANCE = 1.0f
    }

    override fun createAttr(): TMNestStageCardAttr {
        return TMNestStageCardAttr()
    }

    override fun createEvent(): TMNestStageCardViewEvent {
        return TMNestStageCardViewEvent()
    }

    override fun didInit() {
        super.didInit()
        // 初始化marginValue
        initializeMarginValue()
        scrollContentHeight = getScrollContentHeightInner()
    }

    /**
     * 初始化边距值
     */
    private fun initializeMarginValue() {
        marginValue = attr.customMarginValue ?: DEFAULT_MARGIN_VALUE
    }

    private fun getInitOffset(initLevel: Int): Float {
        val initOffset = attr.levelHeights[initLevel] - attr.levelHeights[0]
        return initOffset
    }

    /**
     * 优化：计算并缓存 borderRadius
     * 只有高度变化超过阈值时才重新计算，避免频繁计算三角函数
     */
    private fun calculateBorderRadius(): Float {
        val heightDelta = kotlin.math.abs(cardHeight - lastHeightForRadius)

        // 高度变化小于 5px 时使用缓存值
        if (heightDelta < 5f) {
            return cachedTopBorderRadius
        }

        val levelHeights = attr.levelHeights
        if (levelHeights.isEmpty() || levelHeights.size < 2) {
            cachedTopBorderRadius = 32f
            lastHeightForRadius = cardHeight
            return cachedTopBorderRadius
        }

        val minHeight = levelHeights[0]
        val maxHeight = levelHeights[levelHeights.size - 1]
        val denominator = maxHeight - minHeight
        val heightRatio = if (denominator != 0f) (cardHeight - minHeight) / denominator else 0f

        cachedTopBorderRadius = 20f * (1f - heightRatio * heightRatio).coerceIn(0f, 1f)
        lastHeightForRadius = cardHeight

        return cachedTopBorderRadius
    }

    /**
     * 更新大卡时的属性
     */
    private fun updateMaxCardAttr() {
        val ctx = this
        val levelHeights = attr.levelHeights
        if (levelHeights.isEmpty()) {
            return
        }
        val maxLevel = levelHeights.size - 1
        val maxOffSet = levelHeights.last() - levelHeights.first()
//        TMLog.i(TAG,"[updateMaxCardAttr] curoffset:${curOuterScrollOffset} int:${curOuterScrollOffset.roundToInt()} maxOffset:${maxOffSet}")
        if (currentLevel >= maxLevel && curOuterScrollOffset.roundToInt() ==  maxOffSet.roundToInt()) {
            ctx.cachedBottomBorderRadius = 0f
            ctx.cardMaskColor = Color(0x99000000)
        } else {
            ctx.cachedBottomBorderRadius = 44f
            ctx.cardMaskColor = Color.TRANSPARENT
        }
    }

    /**
     * 优化方案 2: 更新外层滚动状态 - 集中处理所有计算，避免重复
     *
     * 性能优化策略：
     * 1. 时间节流：scroll 回调入口已做时间过滤（16ms间隔）
     * 2. 数值节流：偏移变化超过阈值才发送事件
     * 3. 智能缓存：缓存计算结果避免重复计算
     *
     * 注意：此函数调用前已通过时间节流检查
     */
    private fun updateOuterScrollState(offsetY: Float) {
        val levelHeights = attr.levelHeights
        if (levelHeights.isNotEmpty() && levelHeights.size > 1) {
            val minHeight = levelHeights[0]
            val maxHeight = levelHeights[levelHeights.size - 1]
            val denominator = maxHeight - minHeight
            cachedHeightRatio = if (denominator != 0f) (cardHeight - minHeight) / denominator else 0f
        }

        // 数值节流：只有偏移变化超过阈值才发送 SCROLL_OFFSET 事件
        val offsetDelta = kotlin.math.abs(curOuterScrollOffset - lastEmittedOffset)
        // 使用默认level为0，并且为通过手势滚动就直接调用snapToLevel不需要attachLevel
        if (this@TMNestStageCardView.isFirstLoad && cardHeight >= attr.levelHeights.last()) {
            this@TMNestStageCardView.isFirstLoad = false
            attachToStage()
        } else {
            this@TMNestStageCardView.isFirstLoad = false
        }
        if (offsetDelta > OFFSET_CHANGE_THRESHOLD) {
            emit(
                TMNestStageCardViewEvent.SCROLL_OFFSET,
                mutableMapOf(
                    "parentOffset" to curOuterScrollOffset,
                    "childOffset" to childOffset,
                    "scrollParams" to null
                )
            )
            lastEmittedOffset = curOuterScrollOffset
        }

        // 仅当卡片高度有明显变化时才发送 CARD_HEIGHT_CHANGE 事件
        val cardHeightDelta = kotlin.math.abs(cardHeight - lastEmittedCardHeight)
        if (cardHeightDelta > OFFSET_CHANGE_THRESHOLD) {
            fireCardHeightEvent()
            lastEmittedCardHeight = cardHeight
        }
    }

    /**
     * 优化方案 2: 动态计算和缓存边距值
     */
    private fun updateMarginValue() {
        if (!attr.enableDynamicMargin) {
            marginValue = attr.customMarginValue ?: DEFAULT_MARGIN_VALUE
            return
        }

        val levelHeights = attr.levelHeights
        if (levelHeights.isEmpty() || levelHeights.size < 2) {
            marginValue = attr.customMarginValue ?: DEFAULT_MARGIN_VALUE
            return
        }

        val baseMarginValue = attr.customMarginValue ?: DEFAULT_MARGIN_VALUE
        val minHeight = levelHeights[0]
        val level1Height = levelHeights[1]
        val maxHeight = levelHeights.last()

        marginValue = if (cardHeight <= level1Height && levelHeights.size >= 3) {
            // cardHeight 在 levelHeights[0] 到 levelHeights[1] 之间，且 levelHeights 大于等于 3 时，使用固定值
            baseMarginValue
        } else {
            // 其他情况：使用 cardHeight 与最大高度的占比计算
            val denominator = maxHeight - minHeight
            val heightRatio = if (denominator != 0f) (cardHeight - minHeight) / denominator else 0f
            baseMarginValue * (1f - heightRatio).coerceIn(0f, 1f)
        }
//        TMLog.i(
//            TAG,
//            "ScollerView updateMarginValue marginValue${marginValue} cardHeight:${cardHeight} baseMargin：${baseMarginValue}"
//        )
        cachedMarginValue = marginValue
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                absolutePosition(0f, 0f, 0f, 0f)
                justifyContentFlexEnd()
            }
            View {
                // 整体动画属性放外面
                attr {
                    justifyContentFlexEnd()
                    width(pagerData.pageViewWidth)
                    transform(
                        Rotate.DEFAULT,
                        Scale(
                            ctx.calcScrollWidth(ctx.marginValue),
                            ctx.calcScrollHeight(ctx.marginValue)
                        ),
                        Translate.DEFAULT,
                        Anchor.DEFAULT
                    )
                    ctx.updateMaxCardAttr()
                    backgroundColor(ctx.cardMaskColor)
                    // 优化：使用缓存的 borderRadius，避免每次 render 都计算
                    borderRadius(0f, 0f, ctx.cachedBottomBorderRadius, ctx.cachedBottomBorderRadius)
                }
                Scroller {
                    ref {
                        ctx.outerScrollView = it
                    }
                    attr {
                        bouncesEnable(false)
                        flingEnable(false)
                        height(pagerData.pageViewHeight)
                        scrollEnable(ctx.attr.enableOuterScroll)
                        showScrollerIndicator(false)
                    }
                    View{
                        attr {
                            height(ctx.attr.levelHeights.last() + ctx.getContentPaddingTop())
                        }
                    // 上方空白区域
                    View {
                        attr {
                            width(pagerData.pageViewWidth)
                            height(if (ctx.baseConfigChangeFlag) ctx.getContentPaddingTop() else ctx.getContentPaddingTop())
                        }

                        vif({ ctx.attr.functionContent != null }) {
                            ctx.attr.functionContent?.invoke(this)
                        }
                    }
                    // 手柄上面的部分
                    View {
                        // 顶部通知栏内容
                        vif({ ctx.attr.topContent != null }) {
                            ctx.attr.topContent?.invoke(this)
                        }
                        event {
                            layoutFrameDidChange { frame ->
                                if (frame.height != ctx.topContentHeight) {
                                    ctx.topContentHeight = frame.height
                                    ctx.stickContentHeight = frame.height + ctx.headerContentHeight
                                    ctx.stickContentHeightUse =
                                        frame.height + ctx.headerContentHeight
                                    ctx.calcInnerScrollPlaceHolderHeight()
                                }
                            }
                        }
                    }
                    View {
                        attr {
                            borderRadius(
                                ctx.cachedTopBorderRadius,
                                ctx.cachedTopBorderRadius,
                                0f,
                                0f
                            )
                            backgroundColor(Color.YELLOW)

                            // 根据配置决定使用渐变背景还是纯色背景
                            if (ctx.attr.gradientDirection != null && ctx.attr.gradientColorStops.isNotEmpty()) {
                                backgroundLinearGradient(
                                    ctx.attr.gradientDirection!!,
                                    *ctx.attr.gradientColorStops.toTypedArray()
                                )
                            } else {
                            }
                        }
                        ctx.renderStickContent()()
                        // 滚动容器内容
                        ctx.renderChildScrollContent()()
                        // 补偿高度
//                        ctx.renderComposseContent()()
                    }
                    }
                    event {
                        scrollToTop {
                            TMLog.i(
                                TAG,
                                "ScollerView parentScroller scrollToTop"
                            )
                        }

                        scroll { it ->
                            ctx.outerOnScroll(it)
                        }

                        dragBegin { params ->
                            // 暴露 outerScrollView 的 dragBegin 事件
                            this@TMNestStageCardView.emit(
                                TMNestStageCardViewEvent.OUTER_SCROLL_DRAG_BEGIN,
                                params
                            )
                            ctx.isOutDragging = false
                            // 记录拖拽开始时的时间戳和滚动位置
                            this@TMNestStageCardView.dragBeginTime = DateTime.currentTimestamp()
                            this@TMNestStageCardView.dragBeginOffset = params.offsetY

                            TMLog.i(
                                TAG,
                                "ScollerView parentScroller dragBegin - offset: ${params.offsetY}, time: ${this@TMNestStageCardView.dragBeginTime}"
                            )
                        }

                        willDragEndBySync {
                            TMLog.i(
                                TAG,
                                "ScollerView parentScroller willDragEndBySync it: ${it}, velocity: ${this@TMNestStageCardView.lastScrollVelocityY}"
                            )
                            // Android系统滚动内部View时kuikly不回调dragEnd
                            if (ctx.pagerData.isAndroid) {
                                ctx.calcOutScrollYV("willDragEndBySync")
                            }
                        }

                        dragEnd { params ->
                            // 暴露 outerScrollView 的 dragEnd 事件
                            this@TMNestStageCardView.emit(
                                TMNestStageCardViewEvent.OUTER_SCROLL_DRAG_END,
                                params
                            )
                            ctx.isOutDragging = false

                            TMLog.i(TAG, "ScollerView parentScroller drag parent out so pf dragEnd")
                            ctx.calcOutScrollYV("dragEnd")
                            // 使用计算出的最终速率进行吸附判断
                            this@TMNestStageCardView.attachToStageWithVelocity()
                        }

                        scrollEnd {
                            TMLog.i(
                                TAG,
                                "ScollerView parentScroller scrollEnd offsetY:${it.offsetY}"
                            )
                            // 根据当前offset计算对应的level并更新状态
                            this@TMNestStageCardView.updateLevelByOffset(it.offsetY)
                        }

                        // contentSizeDidChanged {
                        //     if (ctx.stopScroll) {
                        //         ctx.outerScrollView?.view?.abortContentOffsetAnimate()
                        //         KLog.i(TAG, "ScollerView parentScroller contentSizeDidChanged")
                        //     }
                        // }
                    }
                }
                ctx.renderBottomContent()()
            }
        }
    }

    private fun renderComposseContent(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr{
                    width(pagerData.pageViewWidth)
                    backgroundColor(Color.GREEN)
                    height(ctx.getAnimHeight())
                }
            }
        }
    }

    private fun getAnimHeight(): Float {
        val ctx = this
        TMLog.i(
            TAG,
            "[getAnimHeight]  height${ctx.attr.animCompensationHeight} "
        )
       return ctx.attr.animCompensationHeight
    }

    private fun renderChildScrollContent(): ViewBuilder {
        val ctx = this
        return {
            WaterfallList {
                Hover {
                    attr {
                        absolutePosition(
                            top = ctx.attr.innerHoverTop,
                            left = 0f,
                            right = 0f
                        )
                        bringIndex(ctx.attr.bringIndex)
                        zIndex(ctx.attr.hoverZIndex)
                        // 修复多列情况下第二列第3列的区域的hover无法点击的bug
                        extProps["waterfall_static_width"] = true
                    }
                    vif({ ctx.attr.innerHoverContent != null }) {
                        ctx.attr.innerHoverContent?.invoke(this)
                    }
                }
                // 顶部下拉刷新组件
                vif({ ctx.attr.enableHeaderRefresh }) {
                    this@WaterfallList.Refresh {
                        ref {
                            ctx.headerRefreshRef = it
                        }
                        attr {
                            allCenter()
                        }
                        event {
                            refreshStateDidChange {
                                ctx.headerRefreshState = it
                                this@TMNestStageCardView.emit(
                                    TMNestStageCardViewEvent.HEADER_REFRESH,
                                    it
                                )
                            }
                        }
                        ctx.attr.headerRefreshContent?.invoke(this)
                    }
                }


                ref {
                    ctx.innerScrollView = it
                }
                attr {
                    paddingLeft(ctx.attr.customPaddingLeft)
                    paddingRight(ctx.attr.customPaddingRight)
                    paddingTop(ctx.attr.customPaddingTop)
                    paddingBottom(ctx.attr.customPaddingBottom)
                    // backgroundColor(Color.RED)
                    // overflow(true)
                    //TODO iOS设备上fling设不设置都无效？待kuikly讨论
                    flingEnable(ctx.isChildFlingEnable)
                    // borderRadius(20f)
                    // borderRadius(0f, 0f, ctx.bottomCornerRadius, ctx.bottomCornerRadius)
                    showScrollerIndicator(false)
                    height(if (ctx.baseConfigChangeFlag) ctx.scrollContentHeight else ctx.scrollContentHeight)
                    bouncesEnable(ctx.attr.enableHeaderRefresh || ctx.attr.enableChildScrollBounces)
                    flexDirection(FlexDirection.COLUMN)
                    nestedScroll(
                        this@TMNestStageCardView.getUpScrollMode(),
                        this@TMNestStageCardView.getDownScrollMode()
                    )
                    // 瀑布流特殊属性
                    listWidth(ctx.attr.listWidth)
                    itemSpacing(ctx.attr.itemSpacing)
                    columnCount(ctx.attr.columnCount)
                    lineSpacing(ctx.attr.lineSpacing)
                    contentPadding(
                        ctx.attr.contentPaddingTop,
                        ctx.attr.contentPaddingLeft,
                        ctx.attr.contentPaddingBottom,
                        ctx.attr.contentPaddingRight
                    )
                }
                event {
                    scrollToTop {
                        TMLog.i(
                            TAG,
                            "ScollerView child-inner scrollToTop"
                        )
                    }

                    layoutFrameDidChange { frame ->
//                        TMLog.i(
//                            TAG,
//                            "inner scroll frame y:${frame.y} height:${frame.height}"
//                        )
                        //                            KLog.i(TAG, "[layoutFrameDidChange]stick height:${frame.height}")
                    }
                    scroll { it ->
                        // 暴露 innerScrollView 的 scroll 事件
                        this@TMNestStageCardView.emit(
                            TMNestStageCardViewEvent.INNER_SCROLL,
                            it
                        )
                        TMLog.i(TAG, "ScollerView-childScroller scroll it: ${it.offsetY}")
                        // 优化：内层滚动也做节流控制，避免高频触发
                        val currentTime = DateTime.currentTimestamp()
                        val timeDelta =
                            currentTime - this@TMNestStageCardView.lastChildScrollTime

                        // 如果时间间隔不足，直接返回
                        if (timeDelta < ctx.attr.scrollEventThrottleMs) {
                            // 但仍需要更新 childOffset 和 flingEnable 状态
                            ctx.childOffset = it.offsetY
                            ctx.isChildFlingEnable = it.offsetY > 0
                            return@scroll
                        }

                        // 更新时间戳
                        this@TMNestStageCardView.lastChildScrollTime = currentTime

                        val lastChildOffset = ctx.childOffset
                        ctx.childOffset = it.offsetY
                        if (!lastChildOffset.valueEquals(it.offsetY)) {
                            this@TMNestStageCardView.emit(
                                TMNestStageCardViewEvent.SCROLL_OFFSET,
                                mutableMapOf(
                                    "parentOffset" to ctx.curOuterScrollOffset,
                                    "childOffset" to ctx.childOffset,
                                    "scrollParams" to it
                                )
                            )
                        }
                        ctx.isChildFlingEnable = it.offsetY > 0
//                                TMLog.i(
//                                    TAG,
//                                    "ScollerView child-inner scroll lastChildOffset:${lastChildOffset} ,curoffset:${ctx.childOffset} childFlingEnable:${ctx.isChildFlingEnable}"
//                                )
                    }
                    scrollEnd {
                        // 暴露 innerScrollView 的 scrollEnd 事件
                        this@TMNestStageCardView.emit(
                            TMNestStageCardViewEvent.INNER_SCROLL_END,
                            it
                        )

                        val lastFlingEnable = ctx.isChildFlingEnable
                        ctx.isChildFlingEnable = it.offsetY > 0
                        TMLog.i(
                            TAG,
                            "ScollerView child-inner scrollEnd lastFlingEnable${lastFlingEnable} ctx.isChildFlingEnable:${ctx.isChildFlingEnable}"
                        )
                    }
                    dragBegin {
                        // 暴露 innerScrollView 的 dragBegin 事件
                        this@TMNestStageCardView.emit(
                            TMNestStageCardViewEvent.INNER_SCROLL_DRAG_BEGIN,
                            it
                        )
                        // 记录拖拽开始时的时间戳和滚动位置,Android 端在滚动触摸的是子Scroll时，只会回调子的begin和end，不会回调父scroller的begin和End
                        this@TMNestStageCardView.dragBeginTime = DateTime.currentTimestamp()
                        this@TMNestStageCardView.dragBeginOffset = ctx.curOuterScrollOffset

                        TMLog.i(TAG, "ScollerView  drag child-inner so pf dragBegin")
                    }

                    willDragEndBySync {
                        TMLog.i(TAG, "ScollerView child willDragEndBySync it: ${it}")
                    }

                    dragEnd { params ->
                        // 解决iOS滚动到大页卡还会继续滚动的bug
                        if (!ctx.isChildFlingEnable && ctx.pagerData.isIOS) {
                            ctx.innerScrollView?.view?.setContentOffset(0f, 0f, true)
                        }
                        // 暴露 innerScrollView 的 dragEnd 事件
                        this@TMNestStageCardView.emit(
                            TMNestStageCardViewEvent.INNER_SCROLL_DRAG_END,
                            params
                        )

                        if (!ctx.attr.enableHeaderRefresh && ctx.attr.nestedScrollEnable) {
                            TMLog.i(
                                TAG,
                                "[dragEnd]ScollerView child attachToStage with calculated velocity"
                            )
                            this@TMNestStageCardView.attachToStageWithVelocity()
                        }
                    }
                }
                vif({ ctx.attr.scrollContent != null }) {
                    ctx.attr.scrollContent?.invoke(this)
                }

                vif({ ctx.attr.enableFooterRefresh }) {
                    FooterRefresh {
                        ref {
                            ctx.footerRefreshView = it
                        }
                        attr {
                            width(if (ctx.attr.footerWidth > 0) ctx.attr.footerWidth else pagerData.pageViewWidth)
                            allCenter()
                        }
                        event {
                            refreshStateDidChange {
                                ctx.footerRefreshState = it
                                this@TMNestStageCardView.emit(
                                    TMNestStageCardViewEvent.FOOTER_REFRESH,
                                    it
                                )
                            }
                        }
                        vif({ ctx.attr.footerContent != null }) {
                            ctx.attr.footerContent?.invoke(this)
                        }
                    }
                }
                View {
                    attr {
                        height(ctx.innerScrollPlaceHolderHeight)
                    }
                }
            }
        }
    }

    private fun renderBottomContent(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    absolutePositionAllZero()
                    justifyContentFlexEnd()
                }
                View {
                    attr {
                    }
                    vif({ ctx.attr.bottomContent != null }) {
                        ctx.attr.bottomContent?.invoke(this)
                    }
                    event {
                        layoutFrameDidChange {
                            if (ctx.bottomContentHeight != it.height) {
                                ctx.bottomContentHeight = it.height
                                ctx.innerScrollPlaceHolderHeight =
                                    ctx.calcInnerScrollPlaceHolderHeight()
                            }
                            TMLog.i(TAG, "bottomContentHeight:${it.height}")
                        }
                    }
                }

            }
        }
    }

    private fun renderStickContent(): ViewBuilder {
        val ctx = this
        return {
           View {
               vif({ ctx.attr.isCustomHeaderContent }) {
                   ctx.attr.headerContent?.invoke(this)
               }

               vif({ !ctx.attr.isCustomHeaderContent }) {
                   View {
                       // 手柄
                       View {
                           attr {
                               width(pagerData.pageViewWidth)
                               height(12f)
                               marginBottom(ctx.attr.handlerMarginBottom)
                               alignSelfCenter()
                               allCenter()
                           }

                           event {
                               click {
                                   // 触发手柄点击事件，传递当前级别、级别高度和卡片高度等参数
                                   val handleClickParams = mutableMapOf<String, Any>(
                                       "currentLevel" to ctx.currentLevel,
                                       "levelHeights" to ctx.attr.levelHeights,
                                       "cardHeight" to ctx.cardHeight
                                   )
                                   this@TMNestStageCardView.emit(
                                       TMNestStageCardViewEvent.HANDLE_CLICK,
                                       handleClickParams
                                   )
                               }
                           }

                           // 拖拽手柄视觉元素
                           View {
                               attr {
                                   width(36f)
                                   height(4f)
                                   borderRadius(4f / 2)
                               }
                           }
                       }
                       // 顶部内容
                       View {
                           vif({ ctx.attr.headerContent != null }) {
                               ctx.attr.headerContent?.invoke(this)
                           }
                       }

                       // View {
                       //     attr {
                       //         absolutePosition(bottom = -100f)
                       //         width(pagerData.pageViewWidth)
                       //         height(42f)
                       //         alignSelfCenter()
                       //         allCenter()
                       //         backgroundColor(Color.RED)
                       //     }
                       // }

                   }
               }

               event {
                   layoutFrameDidChange { frame ->
                       if (frame.height != ctx.headerContentHeight) {
                           ctx.headerContentHeight = frame.height
                           ctx.stickContentHeight = frame.height + ctx.topContentHeight
                           ctx.stickContentHeightUse = frame.height + ctx.topContentHeight
                           ctx.calcInnerScrollPlaceHolderHeight()
                           // 刷新一下卡片的高度
                           ctx.notifyCardHeightChange()
                       }
//                                TMLog.i(TAG, "update listHeight stickContentHeight[layoutFrameDidChange]stick height:${frame.height}")
                   }
               }
           }
       }
    }

    fun notifyCardHeightChange() {
        emit(TMNestStageCardViewEvent.CARD_HEIGHT_CHANGE, getTouchHeight())
        lastEmittedCardHeight = cardHeight
    }


    private fun calcInnerScrollPlaceHolderHeight(): Float {
        if (isOutDragging){
            return 0f
        }
        scrollContentHeight = getScrollContentHeightInner()
        val placeholderViewHeight =
            scrollContentHeight - (attr.levelHeights[currentLevel] - stickContentHeight - bottomContentHeight)
//        TMLog.i(
//            TAG,
//            "calcInnerScrollPlaceHolderHeight:${placeholderViewHeight} curlevel[${attr.levelHeights[currentLevel]}] stickContentHeight:${stickContentHeight}"
//        )
        return placeholderViewHeight
    }

    // 如果设置了外部不滚动
    private fun getTouchHeight(): Float {
        val ctx = this@TMNestStageCardView
        // 返回正常高度
        if (ctx.attr.enableOuterScroll) {
            return ctx.cardHeight
        } else {  // 设置外部不可滚动
            return (ctx.cardHeight - (if (pagerData.isIOS) 0f else ctx.stickContentHeight))
        }

    }


    private fun getContentPaddingTop(): Float {
        val paddingTop =
            pagerData.pageViewHeight - this@TMNestStageCardView.attr.levelHeights[0]
        TMLog.i(
            TAG,
            "[getContentPaddingTop] paddingTop:${paddingTop}  pagerData.pageViewHeight:${pagerData.pageViewHeight}"
        )
        return paddingTop
    }


    fun getCurrentCardHeight(): Float {
        val ctx = this@TMNestStageCardView
        var cardHeight = ctx.curOuterScrollOffset + ctx.attr.levelHeights[0]
        val maxHeight = ctx.attr.levelHeights[ctx.attr.levelHeights.size - 1]
        cardHeight = minOf(cardHeight, maxHeight)
//        cardHeight = 500f
//        TMLog.i(TAG, "[getCurrentCardHeight] cardHeight:${cardHeight}")
        return cardHeight
    }

    private fun getScrollContentHeightInner(): Float {
        if (isOutDragging) {
            return attr.levelHeights.last()
        }
        val height =
            if (curOuterScrollOffset >= 0) attr.levelHeights.last() - attr.animCompensationHeight else attr.levelHeights.last()
        TMLog.i(
            TAG,
            "scroll-inner-Height:${height}  curOuterScrollOffset:${curOuterScrollOffset} stickHeight:${this@TMNestStageCardView.stickContentHeight} stickHeightUse:${this@TMNestStageCardView.stickContentHeightUse} "
        )
        return height
    }


    /**
     * 外层滚动回调处理方法
     * 处理外层滚动事件，包括速率计算、节流控制和状态更新
     */
    private fun outerOnScroll(it: ScrollParams) {
        curOuterScrollOffset = it.offsetY
        cardHeight = getCurrentCardHeight()
        // 动态计算和缓存边距值
        updateMarginValue()
        // 由于限流导致的高度回调不准确的，这里补充一下,非限流版本
        fireCardHeightEvent(true)
        // 点击状态栏会自动滚动到底部，这个回调出去，iOS会自动，Android部分机型会
        if (it.offsetY.valueEquals(0f)) {
            currentLevel = 0
            emit(
                TMNestStageCardViewEvent.STATE_CHANGE,
                currentLevel
            )
        }
        // 通过时间检查后，再进行实际处理
        TMLog.i(TAG, "ScollerView-parentScroller scroll it: ${it.offsetY} ${isFirstLoad}")
        val maxOffset =
            attr.levelHeights.last() - attr.levelHeights.first()
        if (it.offsetY > maxOffset) {
            // 超过最大滚动距立即更新inner的高度,防止滚动超过卡片距离
//                                TMLog.i(TAG,"update listHeight stickContentHeight:${stickContentHeight} first:${isFirstLoad}")
            stickContentHeightUse = stickContentHeight
            scrollContentHeight = getScrollContentHeightInner()
            if (!isFirstLoad) {
                attachToStage()
            }
            TMLog.i(
                TAG,
                " out overscroll ${stickContentHeightUse} maxOffset:${maxOffset} it.offsetY:${it.offsetY}"
            )
            // 首次加载不拦截计算
            if (isFirstLoad) {
                // nothing
            } else {
                return
            }
        }
        // 优化：在回调入口处立即判断是否需要处理，避免无效计算
        val currentTime = DateTime.currentTimestamp()
        val timeDelta = currentTime - lastEmitTime
        val currentOffset = it.offsetY

        if (lastScrollTime > 0) {
            val timeDeltaV =
                currentTime - lastScrollTime
            val offsetDelta =
                currentOffset - lastScrollOffset

            if (timeDeltaV > 0 && timeDelta > 0) {
                // 计算速率（像素/毫秒）
                lastScrollVelocityY =
                    offsetDelta / timeDelta
            }
        }

        lastScrollOffset = currentOffset
        lastScrollTime = currentTime

        // 如果时间间隔不足，直接返回，不做任何处理
        if (timeDelta < attr.scrollEventThrottleMs) {
            return
        }

        // 更新时间戳，确保节流生效
        lastEmitTime = currentTime

        updateOuterScrollState(it.offsetY)
    }

    private fun fireCardHeightEvent(realtime: Boolean = false) {
        if (realtime) {
            emit(TMNestStageCardViewEvent.REALTIME_CARD_HEIGHT_CHANGE, getCurrentCardHeight())
        } else {
            emit(TMNestStageCardViewEvent.CARD_HEIGHT_CHANGE, getTouchHeight())
        }
    }

    /**
     * 根据当前 offset 计算并更新 level 状态
     * levelHeights 相邻的差值表示 offset，其中 levelHeights[0] 的 offset 为 0
     * 如果 level 变化了则发送 STATE_CHANGE 事件
     * @param offsetY 当前滚动偏移量
     */
    private fun updateLevelByOffset(offsetY: Float) {
        val levelHeights = attr.levelHeights
        var matchedLevel = -1
        var accumulatedOffset = 0f
        val maxOffset = levelHeights.last() - levelHeights.first()
        if (offsetY >= maxOffset) {
            matchedLevel = levelHeights.lastIndex
        } else {
            for (i in levelHeights.indices) {
                if (i == 0) {
                    // level 0 的 offset 为 0
                    if (offsetY.roundToInt() == 0) {
                        matchedLevel = 0
                        break
                    }
                } else {
                    // 相邻差值表示 offset
                    accumulatedOffset += levelHeights[i] - levelHeights[i - 1]
                    if (abs(offsetY - accumulatedOffset) <= OFFSET_TOLERANCE) {
                        matchedLevel = i
                        break
                    }
                }
            }
        }
        if (matchedLevel > 0 ) {
            currentLevel = matchedLevel
            emit(TMNestStageCardViewEvent.STATE_CHANGE, currentLevel)
        }
    }

    /**
     * 基于速率的吸附到阶段方法
     * 考虑滚动速率来决定是否跳过阈值判断直接吸附到下一级别
     */
    private fun attachToStageWithVelocity() {
        val velocityThreshold = attr.velocityAttachThreshold
        TMLog.i(
            TAG,
            "attachToStageWithVelocity: finalVelocity=$finalDragVelocity, threshold=$velocityThreshold, lastV=$lastScrollVelocityY"
        )

        // 如果速率超过阈值，使用速率吸附逻辑
        if (abs(lastScrollVelocityY) > velocityThreshold) {
            attachToStageByVelocity(lastScrollVelocityY)
        } else if (abs(finalDragVelocity) > velocityThreshold) {
            // 否则使用原来的吸附逻辑
            attachToStageByVelocity(finalDragVelocity)
        } else {
            // 否则使用原来的吸附逻辑
            attachToStage()
        }
    }


    /**
     * 基于速率方向的吸附逻辑
     * @param velocity 滚动速率，正值表示向下滚动，负值表示向上滚动
     */
    private fun attachToStageByVelocity(velocity: Float) {
        var stageHeight: Float = 0f
        var stageLevel: Int = 0

        val levelHeights = this@TMNestStageCardView.attr.levelHeights
        var outOffset = this@TMNestStageCardView.curOuterScrollOffset
        var curLevelHeight = outOffset + levelHeights[0]
        val previousLevel = currentLevel

        if (levelHeights.isEmpty()) {
            stageHeight = 0f
            stageLevel = 0
        } else if (levelHeights.size == 1) {
            stageHeight = levelHeights[0]
            stageLevel = 0
        } else {
            // 二分查找找到当前位置在哪两个level之间
            if (curLevelHeight <= levelHeights[0]) {
                stageHeight = levelHeights[0]
                stageLevel = 0
            } else if (curLevelHeight >= levelHeights.last()) {
                stageHeight = levelHeights.last()
                stageLevel = levelHeights.size - 1
            } else {
                var left = 0
                var right = levelHeights.size - 1

                while (left < right - 1) {
                    val mid = (left + right) / 2
                    if (curLevelHeight <= levelHeights[mid]) {
                        right = mid
                    } else {
                        left = mid
                    }
                }

                // 基于速率方向决定吸附目标
                if (velocity > 0) {
                    // 向下滚动（正方向），吸附到较高的level（right）
                    stageHeight = levelHeights[right]
                    stageLevel = right
                    TMLog.i(
                        TAG,
                        "速率吸附-向下: velocity=$velocity, 从level $left 吸附到level$right"
                    )
                } else {
                    // 向上滚动（负方向），吸附到较低的level（left）
                    stageHeight = levelHeights[left]
                    stageLevel = left
                    TMLog.i(
                        TAG,
                        "速率吸附-向上: velocity=$velocity, 从level$right 吸附到level$left"
                    )
                }
            }
        }

        currentLevel = stageLevel
        // attach 之后刷新
        stickContentHeightUse = stickContentHeight
        innerScrollPlaceHolderHeight = calcInnerScrollPlaceHolderHeight()
        val needAnim =
            !outOffset.valueEquals(stageHeight - levelHeights[0]) && curLevelHeight < levelHeights.last()
        outerScrollView?.view?.abortContentOffsetAnimate()
        outerScrollView?.view?.setContentOffset(0f, stageHeight - levelHeights[0], needAnim)
        if (!needAnim) {
            this@TMNestStageCardView.emit(TMNestStageCardViewEvent.STATE_CHANGE, stageLevel)
        }

        TMLog.i(
            TAG,
            "attachToStageByVelocity: stageLevel=$stageLevel, stageHeight=$stageHeight, previousLevel=$previousLevel, velocity=$velocity, needAnim=$needAnim"
        )
    }

    private fun attachToStage() {
        var stageHeight: Float = 0f
        var stageLevel: Int = 0

        val levelHeights = this@TMNestStageCardView.attr.levelHeights
        var outOffset = this@TMNestStageCardView.curOuterScrollOffset
        var curLevelHeight = outOffset + levelHeights[0]
        val previousLevel = currentLevel

        // 优化方案 3: 使用二分查找替代线性查找，提高算法效率 O(log n) vs O(n)
        if (levelHeights.isEmpty()) {
            stageHeight = 0f
            stageLevel = 0
        } else if (levelHeights.size == 1) {
            stageHeight = levelHeights[0]
            stageLevel = 0
        } else {
            // 优化: 二分查找替代线性搜索
            if (curLevelHeight <= levelHeights[0]) {
                stageHeight = levelHeights[0]
                stageLevel = 0
            } else if (curLevelHeight >= levelHeights.last()) {
                stageHeight = levelHeights.last()
                stageLevel = levelHeights.size - 1
            } else {
                // 二分查找找到当前位置在哪两个level之间
                var left = 0
                var right = levelHeights.size - 1

                while (left < right - 1) {
                    val mid = (left + right) / 2
                    if (curLevelHeight <= levelHeights[mid]) {
                        right = mid
                    } else {
                        left = mid
                    }
                }

                // 使用阈值判断或距离判断
                val shouldUseThreshold = attr.levelAttachThresholds.isNotEmpty() &&
                        left < attr.levelAttachThresholds.size &&
                        attr.levelAttachThresholds[left] > 0.0f &&
                        attr.levelAttachThresholds[left] < 1.0f

                if (shouldUseThreshold) {
                    // 使用阈值判断
                    val threshold = attr.levelAttachThresholds[left]
                    val levelDistance = levelHeights[right] - levelHeights[left]
                    val thresholdPosition = levelHeights[left] + levelDistance * threshold

                    if (curLevelHeight >= thresholdPosition) {
                        stageHeight = levelHeights[right]
                        stageLevel = right
                    } else {
                        stageHeight = levelHeights[left]
                        stageLevel = left
                    }

                    TMLog.i(
                        TAG, "使用阈值判断: left=$left, right=$right, threshold=$threshold, " +
                                "thresholdPosition=$thresholdPosition, curLevelHeight=$curLevelHeight, " +
                                "选择level=$stageLevel"
                    )
                } else {
                    // 使用原来的距离判断
                    val distanceToLeft = curLevelHeight - levelHeights[left]
                    val distanceToRight = levelHeights[right] - curLevelHeight

                    if (distanceToLeft <= distanceToRight) {
                        stageHeight = levelHeights[left]
                        stageLevel = left
                    } else {
                        stageHeight = levelHeights[right]
                        stageLevel = right
                    }

                    TMLog.i(
                        TAG, "使用距离判断: left=$left, right=$right, " +
                                "distanceToLeft=$distanceToLeft, distanceToRight=$distanceToRight, " +
                                "选择level=$stageLevel"
                    )
                }
            }
        }

        currentLevel = stageLevel
        // attach 之后刷新
        stickContentHeightUse = stickContentHeight
        innerScrollPlaceHolderHeight = calcInnerScrollPlaceHolderHeight()
        val needAnim =
            !outOffset.valueEquals(stageHeight - levelHeights[0]) && curLevelHeight < levelHeights.last()
        outerScrollView?.view?.abortContentOffsetAnimate()
        outerScrollView?.view?.setContentOffset(0f, stageHeight - levelHeights[0], needAnim)
        if (!needAnim) {
            this@TMNestStageCardView.emit(TMNestStageCardViewEvent.STATE_CHANGE, stageLevel)
        }

        TMLog.i(
            TAG,
            "attachToStage stageLevel:$stageLevel, stageHeight:$stageHeight, previousLevel:$previousLevel needAnim：$needAnim ,level0:${levelHeights[0]} anim to${stageHeight - levelHeights[0]} out offset:${this@TMNestStageCardView.curOuterScrollOffset} stickContentHeightUse：${stickContentHeightUse}"
        )
    }

    /**
     * 滚动到底部
     */
    fun childScrollToBottom(
        animated: Boolean = true
    ) {
        TMLog.i(
            TAG,
            "childScrollToBottom  childOffset: $childOffset, animated: $animated"
        )
        val contentHeight = innerScrollView?.view?.contentView?.frame?.height ?: 0f
        val offset = contentHeight - getScrollContentHeightInner()
        if (offset > 0) {
            innerScrollView?.view?.abortContentOffsetAnimate()
            innerScrollView?.view?.setContentOffset(0f, offset, animated)
        }
        TMLog.i(
            TAG,
            "设置内部子滚动容器到底部: $animated contentHeight:$contentHeight offset:$offset "
        )
    }

    /**
     * 滚动到指定位置
     * @param parentOffset 外部父容器的滚动偏移量，为null时不设置父容器滚动位置
     * @param childOffset 内部子滚动容器的滚动偏移量，为null时不设置子容器滚动位置
     * @param animated 是否使用动画，默认为true
     */
    fun scrollToOffset(parentOffset: Float? = null,
                       childOffset: Float? = null,
                       animated: Boolean = true
    ) {
        TMLog.i(
            TAG,
            "scrollToOffset parentOffset: $parentOffset, childOffset: $childOffset, animated: $animated"
        )

        // 设置外部父容器滚动位置
        parentOffset?.let { offset ->
            outerScrollView?.view?.setContentOffset(0f, offset, animated)
            TMLog.i(TAG, "设置外部父容器滚动位置: $offset")
        }

        // 设置内部子滚动容器位置
        childOffset?.let { offset ->
            innerScrollView?.view?.setContentOffset(0f, offset, animated)
            TMLog.i(TAG, "设置内部子滚动容器位置: $offset")
        }
    }

    /**
     * 获取当前滚动位置
     * @return Pair<Float, Float> 返回 (父容器偏移量, 子容器偏移量)
     */
    fun getCurrentScrollOffset(): Pair<Float, Float> {
        return Pair(curOuterScrollOffset, childOffset)
    }

    /**
     * 吸附到指定级别的卡片高度
     * @param level 目标级别，对应 levelHeights 数组的下标
     * @param animated 是否使用动画，默认为true
     * @return Boolean 返回是否成功执行吸附操作
     */
    fun snapToLevel(level: Int, animated: Boolean = true): Boolean {
        TMLog.i(TAG, "snapToLevel level: $level, animated: $animated")

        // 检查level是否在有效范围内
        if (level < 0 || level >= attr.levelHeights.size) {
            TMLog.e(
                TAG,
                "snapToLevel 无效的level: $level, levelHeights.size: ${attr.levelHeights.size}"
            )
            return false
        }

        // 获取目标高度
        val targetHeight = attr.levelHeights[level]

        // 计算需要设置的偏移量（相对于第一个级别的偏移）
        val targetOffset = targetHeight - attr.levelHeights[0]

        // 更新当前级别
        val previousLevel = currentLevel
        currentLevel = level
        // 修复多次调用出现的bug
        outerScrollView?.view?.abortContentOffsetAnimate()
        // 执行滚动动画
        outerScrollView?.view?.setContentOffset(0f, targetOffset, animated)

        if (!animated) {
            // 触发高度变化事件
            this@TMNestStageCardView.cardHeight = targetHeight
            this@TMNestStageCardView.emit(
                TMNestStageCardViewEvent.CARD_HEIGHT_CHANGE,
                targetHeight
            )
            // 触发级别稳定事件
            this@TMNestStageCardView.emit(
                TMNestStageCardViewEvent.STATE_CHANGE,
                currentLevel
            )
        }


        TMLog.i(
            TAG,
            "snapToLevel 成功执行吸附: level=$level, targetHeight=$targetHeight, targetOffset=$targetOffset, previousLevel=$previousLevel"
        )

        return true
    }

    /**
     * 滚动到指定 item 位置
     * @param position item 在列表中的索引位置（从0开始）
     * @param animated 是否使用动画，默认为true
     * @return Boolean 返回是否成功执行滚动操作
     */
    fun scrollToItemPosition(position: Int, animated: Boolean = true): Boolean {
        TMLog.i(TAG, "scrollToItemPosition position: $position, animated: $animated")

        // 检查 position 是否有效
        if (position < 0) {
            TMLog.e(TAG, "scrollToItemPosition 无效的position: $position，position不能小于0")
            return false
        }

        // 检查 innerScrollView 是否可用
        val waterfallListView = innerScrollView?.view
        if (waterfallListView == null) {
            TMLog.e(TAG, "scrollToItemPosition 失败: innerScrollView 未初始化")
            return false
        }

        try {
            // 调用 WaterfallListView 的滚动到指定位置方法
            waterfallListView.scrollToPosition(position, 0f, animated)

            TMLog.i(
                TAG,
                "scrollToItemPosition 成功执行滚动到位置: position=$position, animated=$animated"
            )
            return true
        } catch (e: Exception) {
            TMLog.e(TAG, "scrollToItemPosition 执行失败: ${e.message}")
            return false
        }
    }

    /**
     * 获取向上滚动的嵌套滚动模式
     * @return KRNestedScrollMode 向上滚动的嵌套滚动模式
     */
    private fun getUpScrollMode(): KRNestedScrollMode {
        val mode =
            if (attr.enableHeaderRefresh || !attr.nestedScrollEnable || !attr.enableOuterScroll) KRNestedScrollMode.SELF_ONLY else KRNestedScrollMode.PARENT_FIRST
//        TMLog.i(TAG, "getUpScrollMode: enableHeaderRefresh=${attr.enableHeaderRefresh}, mode=$mode")
        return mode
    }

    /**
     * 获取向下滚动的嵌套滚动模式
     * @return KRNestedScrollMode 向下滚动的嵌套滚动模式
     */
    private fun getDownScrollMode(): KRNestedScrollMode {
        val mode =
            if (attr.enableHeaderRefresh || !attr.nestedScrollEnable || !attr.enableOuterScroll) KRNestedScrollMode.SELF_ONLY else KRNestedScrollMode.SELF_FIRST
//        TMLog.i(
//            TAG,
//            "getDownScrollMode: enableHeaderRefresh=${attr.enableHeaderRefresh}, mode=$mode"
//        )
        return mode
    }

    /**
     * 控制外部 scroll 是否可以滚动
     * @param enable true 表示启用外部滚动，false 表示禁用外部滚动
     */
    fun setOuterScrollEnabled(enable: Boolean) {
        TMLog.i(TAG, "setOuterScrollEnabled enable: $enable")
        attr.enableOuterScroll = enable
        this@TMNestStageCardView.emit(
            TMNestStageCardViewEvent.CARD_HEIGHT_CHANGE,
            this@TMNestStageCardView.getTouchHeight()
        )
    }

    fun calcOutScrollYV(tag: String) {
        // 记录拖拽结束时的时间戳和滚动位置
        this@TMNestStageCardView.dragEndTime = DateTime.currentTimestamp()
        this@TMNestStageCardView.dragEndOffset = curOuterScrollOffset

        // 计算整个拖拽期间的平均速率
        val totalTimeDelta =
            this@TMNestStageCardView.dragEndTime - this@TMNestStageCardView.dragBeginTime
        val totalOffsetDelta =
            this@TMNestStageCardView.dragEndOffset - this@TMNestStageCardView.dragBeginOffset

        if (totalTimeDelta > 0) {
            // 计算最终拖拽速率（像素/毫秒）
            this@TMNestStageCardView.finalDragVelocity = totalOffsetDelta / totalTimeDelta
        }

        TMLog.i(
            TAG,
            "[$tag]ScollerView parentScroller dragEnd - " +
                    "beginOffset: ${this@TMNestStageCardView.dragBeginOffset}, " +
                    "endOffset: ${this@TMNestStageCardView.dragEndOffset}, " +
                    "timeDelta: ${totalTimeDelta}ms, " +
                    "offsetDelta: ${totalOffsetDelta}px, " +
                    "finalVelocity: ${this@TMNestStageCardView.finalDragVelocity}px/ms"
        )
    }

    /**
     * 动态更新页卡高度级别
     * @param newLevelHeights 新的高度级别列表，必须是递增序列
     * @param maintainCurrentLevel 是否保持当前级别，如果为true且当前级别在新列表范围内，则保持当前级别；否则重置到级别0
     */
    fun updateLevelHeights(newLevelHeights: List<Float>, maintainCurrentLevel: Boolean = false) {
        if (newLevelHeights.isEmpty()) {
            TMLog.i(TAG, "updateLevelHeights: newLevelHeights is empty, ignoring update")
            return
        }

        // 验证高度列表是否为递增序列
        for (i in 1 until newLevelHeights.size) {
            if (newLevelHeights[i] <= newLevelHeights[i - 1]) {
                TMLog.i(
                    TAG,
                    "updateLevelHeights: heights must be in ascending order, ignoring update"
                )
                return
            }
        }

        val oldLevelHeights = attr.levelHeights
        val oldCurrentLevel = currentLevel

        TMLog.i(
            TAG,
            "updateLevelHeights: old=$oldLevelHeights, new=$newLevelHeights, currentLevel=$oldCurrentLevel"
        )

        // 更新高度列表
        attr.levelHeights = newLevelHeights

        // 处理当前级别
        val newCurrentLevel = if (maintainCurrentLevel && oldCurrentLevel < newLevelHeights.size) {
            oldCurrentLevel
        } else {
            0 // 重置到级别0
        }

        // 如果当前级别发生变化，需要调整到新的级别
        if (newCurrentLevel != oldCurrentLevel || newLevelHeights != oldLevelHeights) {
            currentLevel = newCurrentLevel

            // 重新计算并应用新的高度
            val newHeight = newLevelHeights[newCurrentLevel]
            baseConfigChangeFlag = !baseConfigChangeFlag
            TMLog.i(
                TAG,
                "updateLevelHeights: adjusting to level $newCurrentLevel with height $newHeight"
            )
            snapToLevel(currentLevel, false)
        }
    }

    private fun calcScrollWidth(marginValue: Float): Float {
        val pageWidth = pagerData.pageViewWidth
        val xScale = if (pageWidth != 0f) (pageWidth - marginValue * 2) / pageWidth else 1f
//        TMLog.i(TAG, "[calcScrollWidth] xScale:${xScale}  marginValue:${marginValue} pageViewWidth:${pagerData.pageViewWidth} enable:${attr.enableDynamicMargin}")
        return if (attr.enableDynamicMargin) xScale else 1f
    }

    private fun calcScrollHeight(marginValue: Float): Float {
        val pageHeight = pagerData.pageViewHeight
        val yScale = if (pageHeight != 0f) (pageHeight - marginValue * 2) / pageHeight else 1f
//        TMLog.i(TAG, "[calcScrollHeight] yScale:${yScale}  enable:${attr.enableDynamicMargin}")
        return if (attr.enableDynamicMargin) yScale else 1f
    }
    /**
     * 结束顶部刷新
     */
    fun endHeaderRefresh() {
        headerRefreshRef?.view?.endRefresh()
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        screenHeight = pagerData.pageViewHeight
        val initOffset = getInitOffset(attr.initLevel)
        this@TMNestStageCardView.outerScrollView?.view?.setContentOffset(
            0f,
            initOffset
        )
        initCardHeight(attr.initLevel)
        TMLog.i(
            TAG,
            "【viewDidLayout】 level:${attr.initLevel}  view:${this@TMNestStageCardView.outerScrollView?.view}  offset：${
                initOffset
            }"
        )
    }

    private fun initCardHeight(initLevel: Int) {
        this.cardHeight = attr.levelHeights[initLevel]
    }

    override fun created() {
        super.created()
        if (!isInitialized) {
            isInitialized = true
            currentLevel = attr.initLevel
            // 触发初始状态的 onLevelStable 回调
            this@TMNestStageCardView.emit(TMNestStageCardViewEvent.STATE_CHANGE, currentLevel)
        }
    }

}

/**
 * 分段式页卡事件
 */
public class TMNestStageCardViewEvent : ComposeEvent() {
    fun onLevelStable(handler: EventHandlerFn) {
        registerEvent(STATE_CHANGE, handler)
    }

    fun onCardHeightChange(handler: EventHandlerFn) {
        registerEvent(CARD_HEIGHT_CHANGE, handler)
    }

    fun onRealtimeCardHeightChange(handler: EventHandlerFn) {
        registerEvent(REALTIME_CARD_HEIGHT_CHANGE, handler)
    }

    fun onFooterRefresh(handler: EventHandlerFn) {
        registerEvent(FOOTER_REFRESH, handler)
    }

    fun onHeaderRefresh(handler: EventHandlerFn) {
        registerEvent(HEADER_REFRESH, handler)
    }

    fun scrollOffset(handler: EventHandlerFn) {
        registerEvent(SCROLL_OFFSET, handler)
    }

    // innerScrollView 滚动事件
    fun onInnerScrollDragBegin(handler: EventHandlerFn) {
        registerEvent(INNER_SCROLL_DRAG_BEGIN, handler)
    }

    fun onInnerScrollDragEnd(handler: EventHandlerFn) {
        registerEvent(INNER_SCROLL_DRAG_END, handler)
    }

    fun onInnerScrollEnd(handler: EventHandlerFn) {
        registerEvent(INNER_SCROLL_END, handler)
    }

    fun onInnerScroll(handler: EventHandlerFn) {
        registerEvent(INNER_SCROLL, handler)
    }

    fun onHandleClick(handler: EventHandlerFn) {
        registerEvent(HANDLE_CLICK, handler)
    }

    // outerScrollView 滚动事件
    fun onOuterScrollDragBegin(handler: EventHandlerFn) {
        registerEvent(OUTER_SCROLL_DRAG_BEGIN, handler)
    }

    fun onOuterScrollDragEnd(handler: EventHandlerFn) {
        registerEvent(OUTER_SCROLL_DRAG_END, handler)
    }

    companion object {
        const val STATE_CHANGE = "stateChange"
        const val CARD_HEIGHT_CHANGE = "cardHeightChange"
        const val FOOTER_REFRESH = "footerRefresh"
        const val HEADER_REFRESH = "headerRefresh"

        // 子父scroller的滚动距离
        const val SCROLL_OFFSET = "scrollOffset"

        // innerScrollView 滚动事件常量
        const val INNER_SCROLL_DRAG_BEGIN = "innerScrollDragBegin"
        const val INNER_SCROLL_DRAG_END = "innerScrollDragEnd"
        const val INNER_SCROLL_END = "innerScrollEnd"
        const val INNER_SCROLL = "innerScroll"


        // outerScrollView 滚动事件常量
        const val OUTER_SCROLL_DRAG_BEGIN = "outerScrollDragBegin"
        const val OUTER_SCROLL_DRAG_END = "outerScrollDragEnd"

        // 手柄点击事件常量
        const val HANDLE_CLICK = "handleClick"

        // 实时高度变化事件常量
        const val REALTIME_CARD_HEIGHT_CHANGE = "realtimeCardHeightChange"
    }
}

/**
 * 分段式页卡组件扩展函数
 */
public fun ViewContainer<*, *>.TMNestStageCardView(init: TMNestStageCardView.() -> Unit) {
    addChild(TMNestStageCardView(), init)
}