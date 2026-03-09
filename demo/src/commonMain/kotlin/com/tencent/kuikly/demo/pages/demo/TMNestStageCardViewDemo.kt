package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshEndState
import com.tencent.kuikly.core.views.KRNestedScrollMode
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import io.ktor.client.request.forms.formData


@Page("w1")
internal class TMNestStageCardViewDemo : BasePager() {

    companion object {
        private const val TAG = "TMNestStageCardViewDemo"
    }

    internal class WaterFallItem : BaseObject() {
        var title: String by observable("")
        var bgColor: Color by observable(Color.WHITE)
        var height: Float by observable(0f)
    }

    var dataList by observableList<WaterFallItem>()
    var levelHeight by observableList<Float>()
    var nestStageCardView: ViewRef<TMNestStageCardView>? = null
    var textViewBgColor: Boolean by observable(false)
    var dataChange: Boolean by observable(true)
    var enableHeaderFresh: Boolean by observable(false)
    var enableOutScrollEnable: Boolean by observable(true)
    var nestedScrollEnable: Boolean by observable(true)
    var enableChildScrollBounces: Boolean by observable(false)
    var firstCardHeight: Float by observable(150f)
    var cardHeight: Float by observable(150f)
    var menuHeight: Float by observable(50f)
    var menuWidth: Float by observable(120f)
    var headerHeight: Float by observable(120f)
    var footerRefreshState: FooterRefreshState by observable(FooterRefreshState.IDLE)
    var showModal: Boolean by observable(false)
    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }


    override fun body(): ViewBuilder {
        val ctx = this@TMNestStageCardViewDemo
        for (index in 0..5) {
            dataList.add(WaterFallItem().apply {
                title = "我是第${this@TMNestStageCardViewDemo.dataList.size + 1}个卡片"
                height = (200..500).random().toFloat()
                bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            })
        }
        levelHeight = ObservableList<Float>()
        levelHeight.add(ctx.firstCardHeight)
        levelHeight.add(400f)
        levelHeight.add(600f)
        return {
            // 主要内容区域
            View {
                attr {
                    flex(1f)
                }
                View {
                    attr { absolutePositionAllZero() }
                    ctx.renderBtns()()
                }


                TMNestStageCardView {
                    ref {
                        ctx.nestStageCardView = it
                    }
                    attr {
                        levelHeights(listOf(ctx.firstCardHeight, 450f, 600f))
//                            levelAttachThresholds(listOf(0.5f,0.1f))
                        initLevel(2)
                        enableFooterRefresh(false)
                        enableHeaderRefresh(ctx.enableHeaderFresh)
                        contentPadding(16f, 16f, 16f, 16f)
                        listWidth(pagerData.pageViewWidth)
//                            cardBackgroundLinearGradient(
//                                Direction.TO_BOTTOM,
//                                ColorStop(Color.RED, 0f),
//                                ColorStop(Color.GREEN, 0.3f),
//                                ColorStop(Color.BLACK, 1f)
//                            )
//                            handlerMarginBottom(80f)
//                            dmBackgroundColor(Color.RED, Color.YELLOW)
                        // 设置组件外层内边距
                        customPadding(16f, 8f) // 水平16f，垂直8f
//                            customMarginValue(0f)
                        enableDynamicMargin(true)
                        // 新增的嵌套滚动和弹性效果控制
                        nestedScrollEnable(ctx.nestedScrollEnable)
                        enableChildScrollBounces(ctx.enableChildScrollBounces)
                        // dmBackgroundColor(Color.RED, Color.BLUE)
                        // 或者可以分别设置：
                        // customPaddingLeft(16f)
                        // customPaddingRight(16f)
                        // customPaddingTop(8f)
                        // customPaddingBottom(8f)
                        // 瀑布流属性，非瀑布流可以不传 start
                        // columnCount(3)
                        // lineSpacing(10f)
                        // itemSpacing(10f)
                        // 瀑布流属性，非瀑布流可以不传 end
//                        outerHoverContent {
//                            View {
//                                attr {
//                                    height(80f)
//                                    backgroundColor(Color(0xFF007AFF)) // 蓝色背景
//                                    borderRadius(12f)
//                                    marginLeft(20f)
//                                    marginRight(20f)
//                                    justifyContentCenter()
//                                    alignItemsCenter()
//                                }
//
//                                View {
//                                    attr {
//                                        flexDirectionRow()
//                                        alignItemsCenter()
//                                        justifyContentSpaceBetween()
//                                        paddingLeft(16f)
//                                        paddingRight(16f)
//                                    }
//
//                                    Text {
//                                        attr {
//                                            text("我是Hover")
//                                            color(Color.WHITE)
//                                            fontSize(16f)
//                                            fontWeightMedium()
//                                        }
//                                    }
//
//                                    View {
//                                        attr {
//                                            width(60f)
//                                            height(30f)
//                                            backgroundColor(Color.WHITE)
//                                            borderRadius(15f)
//                                            justifyContentCenter()
//                                            alignItemsCenter()
//                                        }
//
//                                        Text {
//                                            attr {
//                                                text("点击")
//                                                color(Color(0xFF007AFF))
//                                                fontSize(12f)
//                                                fontWeightMedium()
//                                            }
//                                        }
//
//                                        event {
//                                            click {
//                                                TMLog.i(TAG, "点击了悬浮内容按钮")
//                                                // 可以在这里添加点击后的逻辑，比如显示更多信息
//                                                ctx.showModal = true
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
                        functionContent = {
                            View {
                                attr {
                                    height(100f)
                                    width(100f)
                                    backgroundColor(Color.RED)
                                    absolutePosition(left = 0f, right = 0f, bottom = 0f)
                                }

                                event {
                                    click {
                                        println("tipsContent,12345678")
                                    }
                                }
                            }
                        }
//                            topContent {
//                                View {
//                                    attr {
//                                        allCenter()
//                                        backgroundColor(Color.BLUE)
//                                        marginBottom(10f)
//                                    }
//                                    Text {
//                                        attr {
//                                            text("我是顶部通知栏内容")
//                                        }
//                                    }
//                                }
//                            }
                        headerContent {
                            vif({ctx.dataChange}){
                                View {
                                    attr {
                                        height(ctx.headerHeight)
                                        allCenter()
                                        border(Border(4f, BorderStyle.SOLID, Color.BLACK))
                                    }
                                    View {
                                        attr {
                                            flexDirectionRow()
                                            alignItemsCenter()
                                        }
                                        Text {
                                            attr {
                                                text("高度为${ctx.headerHeight},点击变高度")
                                                fontSize(16f)
                                            }
                                        }
                                        event {
                                            click {
                                                ctx.textViewBgColor = !ctx.textViewBgColor
                                                TMLog.i(TAG, "")
                                                if (ctx.headerHeight == 120f) {
                                                    ctx.headerHeight = 60f
                                                } else {
                                                    ctx.headerHeight = 120f
                                                }
                                            }
                                        }

                                    }
                                }
                            }
                            velse{
                                View {
                                    attr {
                                        height(ctx.headerHeight)
                                        allCenter()
                                    }
                                    View {
                                        attr {
                                            flexDirectionRow()
                                            alignItemsCenter()
                                        }
                                        Text {
                                            attr {
                                                text("主题切换1：")
                                                fontSize(16f)
                                            }
                                        }

                                    }
                                }
                            }


                        }
                        scrollContent {

                            View {
                                attr {
                                    width(ctx.pagerData.pageViewWidth)
                                    flexDirectionRow()
                                }

                                val list = listOf(
                                    "1",
                                    "2",
                                    "3",
                                    "4",
                                    "5",
                                    "6",
                                    "7",
                                    "8",
                                    "9",
                                    "10",
                                    "11",
                                    "12",
                                    "13",
                                    "14",
                                    "15",
                                    "16",
                                    "17",
                                    "18",
                                    "19"
                                )

                                List {
                                    attr {
                                        width(100f)
                                        nestedScroll(
                                            KRNestedScrollMode.PARENT_FIRST,
                                            KRNestedScrollMode.SELF_FIRST
                                        )
                                        height(600f)
                                        backgroundColor(Color.RED)
                                        bouncesEnable(false)
                                    }
                                    for (i in 0..100) {
                                        View {
                                            attr {
                                                width(100f)
                                                height(50f)
                                            }
                                            Text {
                                                attr {
                                                    text("WS-$i")
                                                }
                                            }
                                        }

                                    }
                                }
                                View {
                                    attr {   width(ctx.pagerData.pageViewWidth - 100f)
                                        flexDirectionColumn() }
                                    List {
                                        attr {
                                            height(300f)
                                            backgroundColor(Color.GREEN)
                                            width(ctx.pagerData.pageViewWidth - 100f)
                                            nestedScroll(
                                                KRNestedScrollMode.PARENT_FIRST,
                                                KRNestedScrollMode.SELF_FIRST
                                            )
                                            bouncesEnable(false)
                                        }
                                        for (i in 0..100) {
                                            View {
                                                attr {
                                                    width(ctx.pagerData.pageViewWidth - 100f)
                                                    height(50f)
                                                }
                                                Text {
                                                    attr {
                                                        text("TONY-$i")
                                                    }
                                                }
                                            }

                                        }
                                    }
                                    List {
                                        attr {
                                            height(300f)
                                            backgroundColor(Color.YELLOW)
                                            width(ctx.pagerData.pageViewWidth - 100f)
                                            nestedScroll(
                                                KRNestedScrollMode.PARENT_FIRST,
                                                KRNestedScrollMode.SELF_FIRST
                                            )
                                            bouncesEnable(false)
                                        }
                                        for (i in 0..100) {
                                            View {
                                                attr {
                                                    width(ctx.pagerData.pageViewWidth - 100f)
                                                    height(50f)
                                                }
                                                Text {
                                                    attr {
                                                        text("TONY-$i")
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }

                            }
                        }
                        headerRefreshContent {
                            View {
                                attr {
                                    height(50f)
                                    backgroundColor(Color.RED)
                                    width(pagerData.pageViewWidth)
                                    allCenter()
                                    flexDirectionRow()
                                }
                                Text {
                                    attr {
                                        text("下拉刷新")
                                        fontSize(14f)
                                    }
                                }
                            }
                        }
                        footerContent {
                            View {
                                attr {
                                    height(40f)
                                    allCenter()
                                    backgroundColor(Color.YELLOW)
                                }
                                Text {
                                    attr {
                                        fontSize(14f)
                                        text(
                                            when (ctx.footerRefreshState) {
                                                FooterRefreshState.IDLE -> ""
                                                FooterRefreshState.REFRESHING -> "正在加载"
                                                FooterRefreshState.NONE_MORE_DATA -> "没有更多了"
                                                FooterRefreshState.FAILURE -> "加载失败"
                                            }
                                        )
                                    }
                                }
                            }
                        }
//                            bottomContent {
//                                View {
//                                    attr{
//                                        allCenter()
//                                        size(pagerData.pageViewWidth, 60f)
//                                        dmBackgroundColor(Color.TRANSPARENT)
//                                        border(Border(2f,BorderStyle.SOLID,Color.RED))
//                                    }
//                                    Text {
//                                        attr{
//                                            text("我是底部固定区域")
//                                            textAlignCenter()
//                                        }
//                                    }
//                                }
//                            }
//                            footerWidth(300f)
                        // 新增：内部悬浮内容演示
//                        innerHoverTop(150f) // 设置悬浮内容距离顶部150f
//                        innerHoverContent {
//                            View {
//                                attr {
//                                    height(80f)
//                                    backgroundColor(Color(0xFF007AFF)) // 蓝色背景
//                                    borderRadius(12f)
//                                    marginLeft(20f)
//                                    marginRight(20f)
//                                    justifyContentCenter()
//                                    alignItemsCenter()
//                                }
//
////                                View {
////                                    attr {
////                                        flexDirectionRow()
////                                        alignItemsCenter()
////                                        justifyContentSpaceBetween()
////                                        paddingLeft(16f)
////                                        paddingRight(16f)
////                                    }
////
////                                    Text {
////                                        attr {
////                                            text("🎯 悬浮提示")
////                                            color(Color.WHITE)
////                                            fontSize(16f)
////                                            fontWeightMedium()
////                                        }
////                                    }
////
////                                    View {
////                                        attr {
////                                            width(60f)
////                                            height(30f)
////                                            backgroundColor(Color.WHITE)
////                                            borderRadius(15f)
////                                            justifyContentCenter()
////                                            alignItemsCenter()
////                                        }
////
////                                        Text {
////                                            attr {
////                                                text("点击")
////                                                color(Color(0xFF007AFF))
////                                                fontSize(12f)
////                                                fontWeightMedium()
////                                            }
////                                        }
////
////                                        event {
////                                            click {
////                                                TMLog.i(TAG, "点击了悬浮内容按钮")
////                                                // 可以在这里添加点击后的逻辑，比如显示更多信息
////                                                ctx.showModal = true
////                                            }
////                                        }
////                                    }
////                                }
//                            }
//                        }
                    }
                    event {
                        onCardHeightChange { height ->
                            if (height is Float) {
                                ctx.cardHeight = height
                                ctx.getScrollerRatio()
                                TMLog.i(TAG, "onCardHeightChange: $height")
                            }
                        }
                        onFooterRefresh { state ->
                            if (state is FooterRefreshState) {
                                ctx.footerRefreshState = state
                                if (state == FooterRefreshState.REFRESHING) {
                                    setTimeout(2000) {
                                        for (index in 0..10) {
                                            ctx.dataList.add(WaterFallItem().apply {
                                                title = "新增第${ctx.dataList.count()}个卡片"
                                                height = (200..500).random().toFloat()
                                                bgColor = Color(
                                                    (0..255).random(),
                                                    (0..255).random(),
                                                    (0..255).random(),
                                                    1.0f
                                                )
                                            })
                                        }
                                        ctx.nestStageCardView?.view?.footerRefreshView?.view?.endRefresh(
                                            FooterRefreshEndState.SUCCESS
                                        )
                                        ctx.footerRefreshState = FooterRefreshState.IDLE
                                    }
                                }
                                TMLog.i(TAG, "onFooterRefresh: $state")
                            }
                        }
                        onHeaderRefresh { state ->
                            if (state is com.tencent.kuikly.core.views.RefreshViewState) {
                                when (state) {
                                    com.tencent.kuikly.core.views.RefreshViewState.REFRESHING -> {
                                        TMLog.i(TAG, "顶部刷新中...")
                                        setTimeout(2000) {
                                            // 模拟刷新数据 - 清空现有数据并添加新数据
                                            ctx.dataList.clear()
                                            for (index in 0..20) {
                                                ctx.dataList.add(WaterFallItem().apply {
                                                    title = "刷新后第${index + 1}个卡片"
                                                    height = (200..500).random().toFloat()
                                                    bgColor = Color(
                                                        (0..255).random(),
                                                        (0..255).random(),
                                                        (0..255).random(),
                                                        1.0f
                                                    )
                                                })
                                            }
                                            // 结束刷新
                                            TMLog.i(
                                                TAG,
                                                "endHeaderRefresh ${ctx.nestStageCardView?.view?.headerRefreshRef?.view}"
                                            )
                                            ctx.nestStageCardView?.view?.headerRefreshRef?.view?.endRefresh()
                                        }
                                    }

                                    com.tencent.kuikly.core.views.RefreshViewState.IDLE -> {
                                        TMLog.i("TMNestStageCardView", "顶部刷新空闲")
                                    }

                                    com.tencent.kuikly.core.views.RefreshViewState.PULLING -> {
                                        TMLog.i("TMNestStageCardView", "正在下拉...")
                                    }
                                }
                                TMLog.i("TMNestStageCardView onHeaderRefresh", "$state")
                            }
                        }
                        onLevelStable { levelStable ->
                            TMLog.i(TAG, "levelStable: $levelStable")
                        }
                        scrollOffset { param ->
//                                TMLog.i(TAG, "scroll offset: $param")
                        }

                        // 手柄点击事件
                        onHandleClick { handleClickParams ->
                            TMLog.i(TAG, "手柄被点击，参数: $handleClickParams")
                            // 从参数中获取当前级别，如果获取失败则使用默认值
                            val currentLevel = try {
                                (handleClickParams as? Map<String, Any>)?.get("currentLevel") as? Int
                                    ?: 0
                            } catch (e: Exception) {
                                TMLog.e(TAG, "获取当前级别失败: ${e.message}")
                                0
                            }

                            // 根据当前level，跳转到下一个等级 (level + 1) % 3
                            if (currentLevel == 0) {
                                TMLog.i(TAG, "点击手柄，从级别 $currentLevel 跳转到级别 3")
                                ctx.dataChange = true
                                ctx.nestStageCardView?.view?.snapToLevel(3, true)
                            }

                        }
                        onOuterScrollDragBegin {
                            TMLog.i(TAG, "外部滚动onOuterScrollDragBegin")
                        }
                        onOuterScrollDragEnd {
                            TMLog.i(TAG, "外部滚动onOuterScrollDragEnd")
                        }
                    }
                }

            }

            // Modal 弹窗
            vif({ ctx.showModal }) {
                Modal {
                    // 遮罩层
                    View {
                        attr {
                            absolutePosition(0f, 0f, 0f, 0f)
                            justifyContentCenter()
                            alignItemsCenter()
                            backgroundColor(Color(0, 0, 0, 0.5f)) // 半透明黑色遮罩
                        }

                        // 弹窗内容
                        View {
                            attr {
                                width(300f)
                                height(200f)
                                backgroundColor(Color.WHITE)
                                borderRadius(12f)
                                justifyContentCenter()
                                alignItemsCenter()
                                paddingLeft(20f)
                                paddingRight(20f)
                                paddingTop(20f)
                                paddingBottom(20f)
                            }

                            Text {
                                attr {
                                    text("这是一个 Kuikly DSL Modal 弹窗")
                                    fontSize(16f)
                                    color(Color.BLACK)
                                    textAlignCenter()
                                    marginBottom(20f)
                                }
                            }

                            Button {
                                attr {
                                    titleAttr {
                                        text("关闭")
                                        color(Color.WHITE)
                                    }
                                    backgroundColor(Color.RED)
                                    width(100f)
                                    height(40f)
                                    borderRadius(8f)
                                }
                                event {
                                    click {
                                        ctx.showModal = false
                                        TMLog.i(TAG, "关闭Modal弹窗")
                                    }
                                }
                            }
                        }

                        // 点击遮罩层关闭弹窗
                        event {
                            click {
                                ctx.showModal = false
                                TMLog.i(TAG, "点击遮罩层关闭Modal弹窗")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderBtns(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    marginTop(40f)
                    allCenter()
                    height(ctx.menuHeight)
                    flexDirectionRow()
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("cell高度变化")
                        backgroundColor(if (ctx.textViewBgColor) Color.RED else Color.GREEN)
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.textViewBgColor = !ctx.textViewBgColor
                            TMLog.i(TAG, "我是底图按钮click1")
                            if (ctx.headerHeight == 120f) {
                                ctx.headerHeight = 60f
                            } else {
                                ctx.headerHeight = 120f
                            }

                        }
                    }
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("子滚动页卡下拉刷新：${ctx.enableHeaderFresh}")
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.enableHeaderFresh = !ctx.enableHeaderFresh
                        }
                    }
                }
                Blur {
                    attr {
                        height(100f)
                        width(80f)
                        blurRadius(10f)
                    }
                }

                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("点击设置卡片吸附到level=2")
                        backgroundColor(Color.YELLOW)
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.nestStageCardView?.view?.snapToLevel(2, true)
                            ctx.nestStageCardView?.view?.snapToLevel(2, true)
                            ctx.nestStageCardView?.view?.snapToLevel(2, true)
//                            setTimeout(20){
//                                ctx.nestStageCardView?.view?.snapToLevel(2, true)
//                            }
//                            setTimeout(10){
//                                ctx.nestStageCardView?.view?.snapToLevel(2, true)
//                            }

                        }
                    }
                }
            }

            View {
                attr {
                    allCenter()
                    height(50f)
                    flexDirectionRow()
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("设置特定的滚动距离,子控件滚动100f")
                        backgroundColor(Color.YELLOW)
                        lines(3)
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.nestStageCardView?.view?.scrollToOffset(null, 100f)
                        }
                    }
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        lines(3)
                        text("设置内部的scroll滚动到第4个position")
                        backgroundColor(Color.YELLOW)
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.nestStageCardView?.view?.scrollToItemPosition(4, true)
                        }
                    }
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("外部scroll:${if (ctx.enableOutScrollEnable) "启用" else "禁用"}")
                        backgroundColor(if (ctx.enableOutScrollEnable) Color.GREEN else Color.RED)
                        textAlignCenter()
                    }
                    event {
                        click {
                            ctx.enableOutScrollEnable = !ctx.enableOutScrollEnable
                            ctx.nestStageCardView?.view?.setOuterScrollEnabled(ctx.enableOutScrollEnable)
                            TMLog.i(TAG, "切换外部scroll状态: ${ctx.enableOutScrollEnable}")
                        }
                    }
                }
            }

            // 新增的嵌套滚动和弹性效果控制按钮
            View {
                attr {
                    allCenter()
                    height(50f)
                    flexDirectionRow()
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("嵌套滚动:${if (ctx.nestedScrollEnable) "启用" else "禁用"}")
                        backgroundColor(if (ctx.nestedScrollEnable) Color.GREEN else Color.RED)
                        textAlignCenter()
                        lines(2)
                    }
                    event {
                        click {
                            ctx.nestedScrollEnable = !ctx.nestedScrollEnable
                            ctx.nestStageCardView?.view?.childScrollToBottom(true)
                            // 重新设置属性以应用更改
                            TMLog.i(TAG, "切换嵌套滚动状态: ${ctx.nestedScrollEnable}")
                        }
                    }
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("子滚动弹性:${if (ctx.enableChildScrollBounces) "启用" else "禁用"}")
                        backgroundColor(if (ctx.enableChildScrollBounces) Color.GREEN else Color.RED)
                        textAlignCenter()
                        lines(2)
                    }
                    event {
                        click {
                            ctx.enableChildScrollBounces = !ctx.enableChildScrollBounces
                            // 重新设置属性以应用更改
                            TMLog.i(
                                TAG,
                                "切换子滚动弹性效果状态: ${ctx.enableChildScrollBounces}"
                            )
                        }
                    }
                }
                Text {
                    attr {
                        height(ctx.menuHeight)
                        width(ctx.menuWidth)
                        text("设置第一个卡片高度变化${ctx.firstCardHeight}")
                        backgroundColor(Color.YELLOW)
                        textAlignCenter()
                        lines(2)
                    }
                    event {
                        click {
                            if (ctx.firstCardHeight > 150f) {
                                ctx.firstCardHeight = 150f
                            } else {
                                ctx.firstCardHeight = 250f
                            }
                            ctx.nestStageCardView?.view?.updateLevelHeights(
                                listOf(
                                    ctx.firstCardHeight,
                                    400f,
                                    600f
                                )
                            )
                            // 触发组件重新渲染以应用新的属性设置
                            TMLog.i(TAG, "设置第一个卡片高度变化")
                        }
                    }
                }
            }

            View {
                attr {
                    size(100f, 100f)
                }
                Scroller {
                    attr {
                        absolutePosition(0f, 0f, 0f, 0f)
                        size(100f, 100f)
                    }
                    Text {
                        attr {
                            size(100f, 100f)
                            backgroundColor(Color.RED)
                        }
                    }
                    Image {
                        attr {
                            size(100f, 100f)

                            src("https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/lQ8TO29r.gif")
                        }
                    }
                }

                View {
                    attr {
                        size(100f, 100f)
                    }
                    Blur {
                        attr {
                            width(100f)
                            height(100f)
                            blurRadius(1f)
                        }
                    }
                }

            }
        }
    }


    /**
     * 计算滚动比例
     *
     * 根据当前卡片高度计算滚动进度比例，用于控制卡片展开/收起的动画过渡。
     *
     * 计算逻辑：
     * 1. 计算当前高度与中等高度的差值
     * 2. 除以最大高度与中等高度的差值，得到比例
     * 3. 将比例限制在 [0, 1] 范围内
     *
     * @return 滚动比例值，范围 [0, 1]
     *         - 0: 卡片处于中等高度状态
     *         - 1: 卡片处于完全展开（大卡片）状态
     *         - 0-1: 卡片处于过渡动画中
     */
    private fun getScrollerRatio(): Float {
        return 1f;

        val diff = this.cardHeight - 450f
        var ratio = diff / (600f - 450f)
        ratio = maxOf(ratio, 0f)
        ratio = minOf(ratio, 1f)
        this@TMNestStageCardViewDemo.headerHeight = 120f * (if (ratio < 0.5) 1f else 0.5f)
        TMLog.i(
            TAG,
            "getScrollerRatio->ratio${ratio} height:${this@TMNestStageCardViewDemo.headerHeight}"
        )
        return ratio
    }
}