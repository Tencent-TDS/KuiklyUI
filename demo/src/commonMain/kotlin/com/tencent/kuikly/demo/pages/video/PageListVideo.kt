package com.tencent.kuikly.demo.pages.video

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.appearPercentage
import com.tencent.kuikly.core.base.event.willDisappear
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.ActivityIndicator
import com.tencent.kuikly.core.views.FooterRefresh
import com.tencent.kuikly.core.views.FooterRefreshEndState
import com.tencent.kuikly.core.views.FooterRefreshState
import com.tencent.kuikly.core.views.FooterRefreshView
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.ktx.setTimeout
import com.tencent.kuikly.demo.pages.video.feed.HeaderBar
import com.tencent.kuikly.demo.pages.video.feed.VideoListCellView
import com.tencent.kuikly.demo.pages.video.feed.VideoPageListCell
import com.tencent.kuikly.demo.pages.video.manager.LoadVideosManager
import com.tencent.kuikly.demo.pages.video.type.VideoItem
import com.tencent.kuikly.demo.pages.video.type.initFirstVideoItem


@Page("PageListVideo")
internal class PageListVideo: BasePager() {
    private var videoInitialHeight = 0f
    private var videoHeight: Float by observable(0f)
    private var videoInitialWidth = 0f
    private var isLoadingVideo: Boolean = false
    private var isDragging = false
    private var videoFeeds by observableList<VideoItem>()
    private var pageMode =  "default"
    private lateinit var pageParams: JSONObject
    private var currentIndex by observable(0)
    private lateinit var pageListRef : ViewRef<PageListView<*, *>>

    private var videoPageListRef : MutableList<ViewRef<VideoListCellView>> = mutableListOf()

    private lateinit var footerRefreshRef : ViewRef<FooterRefreshView>
    private var footerRefreshText by observable( "加载更多")
    private var isFooterAppear = false

    override fun created() {
        super.created()
        videoHeight = pagerData.pageViewHeight
        videoInitialHeight = pagerData.pageViewHeight
        videoInitialWidth = pagerData.pageViewWidth
        pageMode = pagerData.params.optString("default")
        val temp = pagerData.params.optJSONObject("pageParams")
        if (temp != null) {
            pageParams = temp
        }

        val firstItem = initFirstVideoItem(pagerData.params.optString("entryPageUrl"))

        videoFeeds.add(firstItem)
        // todo 拉取数据
        LoadVideosManager.requestFeeds{ feedList->
            videoFeeds.addAll(feedList)
            KLog.d("Video Feeds", "videoFeeds.size: ${videoFeeds.size}")
            // 首屏数据异步加载 触发preLoad第二个视频
            if (currentIndex == 0 && currentIndex + 1 < videoPageListRef.size) {
                videoPageListRef[currentIndex + 1].view?.preloadVideo()
            }
        }
    }


    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                alignItemsStretch()
                backgroundColor(Color.BLACK)
            }

            HeaderBar {
                attr {
                    title = "videoFeedPage"
                    zIndex(10)
                }
            }
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    pageDirection(false)
                    pageItemHeight(pagerData.pageViewHeight) // 设置每个视频页面的高度，启用原生分页
                    showScrollerIndicator(false)
                }
                event {

                    pageIndexDidChanged {
                        ctx.currentIndex = (it as JSONObject).optInt("index")

                        if (ctx.currentIndex == ctx.videoFeeds.size - 2) {
                            // 加载下一批数据
                            ctx.isLoadingVideo = true
                            LoadVideosManager.requestFeeds{ feedList->
                                ctx.videoFeeds.addAll(feedList)
                                ctx.isLoadingVideo = false
                                // 通知底部刷新成功
                                ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.SUCCESS)
                            }
                        }
                        KLog.d("kuikly", ctx.currentIndex.toString())
                        KLog.d("kuikly", ctx.videoFeeds.size.toString())
                        KLog.d("PageListHeight", ctx.pagerData.pageViewHeight.toString())
                    }
                    dragBegin {
                        ctx.isDragging = true
                    }

                    dragEnd {
                        ctx.isDragging = false
                    }

                }

                // 列表视频内容
                vfor({ctx.videoFeeds}) { item ->
                    VideoPageListCell {
                        ref {
                            ctx.videoPageListRef.add(it)
                        }
                        attr {
                            vitem = item
                            videoHeight = ctx.videoHeight
                            videoInitialHeight = ctx.videoInitialHeight
                        }
                        event {
                            videoEnd {
                                ctx.flowNextPage(FlowType.VideoEnd)
                            }
                            // 首帧出现
                            videoFrameShow {
                                val tempIndex = ctx.currentIndex
                                KLog.d("KuiklyVideoView", "videoFrameShow")
                                // 停0.3s后预加载视频
                                setTimeout(300) {
                                    if (!ctx.isDragging) {
                                        if (tempIndex+ 1 < ctx.videoPageListRef.size) {
//                                            ctx.videoPageListRef[ctx.currentIndex + 1].view?.VideoRef?.view?.preload()
                                            ctx.videoPageListRef[tempIndex + 1].view?.preloadVideo()
                                        }
                                        // 返回的时候加载
                                        if (tempIndex - 1 >= 0) {
//                                            ctx.videoPageListRef[ctx.currentIndex - 1].view?.VideoRef?.view?.preload()
                                            ctx.videoPageListRef[tempIndex - 1].view?.preloadVideo()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FooterRefresh {
                    ref {
                        ctx.footerRefreshRef = it
                    }
                    attr {
                        preloadDistance(60f)
                        allCenter()
                        height(120f)
                        flexDirectionRow()
                        backgroundColor(Color.BLACK)
                    }

                    event {
                        appearPercentage { percentage01 ->
                            KLog.d("footer",percentage01.toString())
                            if (percentage01 > 0.99f) {
                                if (!ctx.isFooterAppear) {
                                    ctx.isFooterAppear = true
                                }
                            }
                        }
                        //
                        /*
                        这里footer出现情况有时候只出现了0.99x的情况
                        didAppear事件不会触发
                        */
//                        didAppear {
//                            ctx.isfooterAppear = true
//                        }
                        willDisappear {
                            ctx.isFooterAppear = false
                        }
                        refreshStateDidChange {
                            when(it) {
                                FooterRefreshState.REFRESHING -> {
                                    ctx.footerRefreshText = "加载更多中.."
                                    // 数据只拉到一个 在最后页需要触发新的加载
                                    if (ctx.currentIndex < ctx.videoFeeds.size - 1) {
                                        ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.SUCCESS)
                                    } else if (!ctx.isLoadingVideo && ctx.currentIndex == ctx.videoFeeds.size - 1) {
                                        ctx.isLoadingVideo = true
                                        LoadVideosManager.requestFeeds{ feedList->
                                            ctx.videoFeeds.addAll(feedList)
                                            ctx.isLoadingVideo = false
                                            ctx.footerRefreshRef.view?.endRefresh(
                                                FooterRefreshEndState.SUCCESS)
                                        }
                                    }
                                }
                                FooterRefreshState.IDLE -> {
                                    ctx.flowNextPage(FlowType.FooterEnd)
                                    ctx.footerRefreshText = "加载更多"
                                }
                                FooterRefreshState.NONE_MORE_DATA -> ctx.footerRefreshText = "无更多数据"
                                FooterRefreshState.FAILURE -> ctx.footerRefreshText = "点击重试加载更多"
                                else -> {}
                            }
                        }
                    }
                    vif({ctx.footerRefreshText == "加载更多中.."}) {
                        ActivityIndicator {
                            attr {
                                marginRight(6f)
                            }
                        }
                    }
                    Text {
                        attr {
                            color(Color.WHITE)
                            fontSize(20f)
                            textAlignCenter()
                            text(ctx.footerRefreshText)
                        }
                    }
                }
            }
        }
    }
    enum class FlowType{
        FooterEnd,
        VideoEnd
    }
    private fun flowNextPage(type: FlowType) {
        val pageListView = pageListRef.view
        pageListView?.let {
            val viewWidth = it.flexNode.layoutFrame.width
            val viewHeight = it.flexNode.layoutFrame.height
            if (type == FlowType.VideoEnd) {
                if (it.renderView != null && !isDragging && viewWidth > 0 && viewHeight > 0) {
                    it.scrollToPageIndex(currentIndex + 1, true)
                }
            } else if (type == FlowType.FooterEnd) {
                /*
                 这里用footer可见滑动控制滑动
                 Footer出现过程中isDragging为true，仍在滚动过程
                 */
                if (it.renderView != null && isFooterAppear && viewWidth > 0 && viewHeight > 0) {
                    it.scrollToPageIndex(currentIndex + 1, true)
                }
            }
        }
    }
}

// 在PageListVideo.kt文件末尾添加

internal class PageListVideoView: ComposeView<PageListVideoViewAttr, PageListVideoViewEvent>() {

    private var videoInitialHeight = 0f
    private var videoHeight: Float by observable(0f)
    private var videoInitialWidth = 0f
    private var isLoadingVideo: Boolean = false
    private var isDragging = false
    private var videoFeeds by observableList<VideoItem>()
    private var pageMode =  "default"
    private lateinit var pageParams: JSONObject
    private var currentIndex by observable(0)
    private lateinit var pageListRef : ViewRef<PageListView<*, *>>

    private var videoPageListRef : MutableList<ViewRef<VideoListCellView>> = mutableListOf()

    private lateinit var footerRefreshRef : ViewRef<FooterRefreshView>
    private var footerRefreshText by observable( "加载更多")
    private var isFooterAppear = false



    override fun createAttr(): PageListVideoViewAttr {
        return PageListVideoViewAttr()
    }

    override fun createEvent(): PageListVideoViewEvent {
        return PageListVideoViewEvent()
    }

    override fun created() {
        super.created()
        // 如果设置了自定义高度，使用自定义高度，否则使用默认的pageViewHeight
        val targetHeight = if (attr.customHeight > 0f) attr.customHeight else pagerData.pageViewHeight
        videoHeight = targetHeight
        videoInitialHeight = targetHeight
        videoInitialWidth = pagerData.pageViewWidth
        pageMode = pagerData.params.optString("default")
        val temp = pagerData.params.optJSONObject("pageParams")
        if (temp != null) {
            pageParams = temp
        }

        val firstItem = initFirstVideoItem(pagerData.params.optString("entryPageUrl"))

        videoFeeds.add(firstItem)
        // todo 拉取数据
        LoadVideosManager.requestFeeds{ feedList->
            videoFeeds.addAll(feedList)
            KLog.d("Video Feeds", "videoFeeds.size: ${videoFeeds.size}")
            // 首屏数据异步加载 触发preLoad第二个视频
            if (currentIndex == 0 && currentIndex + 1 < videoPageListRef.size) {
                videoPageListRef[currentIndex + 1].view?.preloadVideo()
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                alignItemsStretch()
                backgroundColor(Color.BLACK)
            }

            HeaderBar {
                attr {
                    title = "videoFeedPage"
                    zIndex(10)
                }
            }
            PageList {
                ref {
                    ctx.pageListRef = it
                }
                attr {
                    val targetHeight = if (ctx.attr.customHeight > 0f) ctx.attr.customHeight else pagerData.pageViewHeight
                    size(pagerData.pageViewWidth, targetHeight)
                    pageDirection(false)
                    pageItemHeight(targetHeight) // 设置每个视频页面的高度，启用原生分页
                    showScrollerIndicator(false)
                }
                event {

                    pageIndexDidChanged {
                        ctx.currentIndex = (it as JSONObject).optInt("index")

                        if (ctx.currentIndex == ctx.videoFeeds.size - 2) {
                            // 加载下一批数据
                            ctx.isLoadingVideo = true
                            LoadVideosManager.requestFeeds{ feedList->
                                ctx.videoFeeds.addAll(feedList)
                                ctx.isLoadingVideo = false
                                // 通知底部刷新成功
                                ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.SUCCESS)
                            }
                        }
                    }
                    dragBegin {
                        ctx.isDragging = true
                    }

                    dragEnd {
                        ctx.isDragging = false
                    }

                }

                // 列表视频内容
                vfor({ctx.videoFeeds}) { item ->
                    VideoPageListCell {
                        ref {
                            ctx.videoPageListRef.add(it)
                        }
                        attr {
                            vitem = item
                            videoHeight = ctx.videoHeight
                            videoInitialHeight = ctx.videoInitialHeight
                        }
                        event {
                            videoEnd {
                                ctx.flowNextPage(FlowType.VideoEnd)
                            }
                            // 首帧出现
                            videoFrameShow {
                                val tempIndex = ctx.currentIndex
                                KLog.d("KuiklyVideoView", "videoFrameShow")
                                // 停0.3s后预加载视频
                                setTimeout(300) {
                                    if (!ctx.isDragging) {
                                        if (tempIndex+ 1 < ctx.videoPageListRef.size) {
                                            ctx.videoPageListRef[tempIndex + 1].view?.preloadVideo()
                                        }
                                        // 返回的时候加载
                                        if (tempIndex - 1 >= 0) {
                                            ctx.videoPageListRef[tempIndex - 1].view?.preloadVideo()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                FooterRefresh {
                    ref {
                        ctx.footerRefreshRef = it
                    }
                    attr {
                        preloadDistance(60f)
                        allCenter()
                        height(120f)
                        flexDirectionRow()
                        backgroundColor(Color.BLACK)
                    }

                    event {
                        appearPercentage { percentage01 ->
                            KLog.d("footer",percentage01.toString())
                            if (percentage01 > 0.99f) {
                                if (!ctx.isFooterAppear) {
                                    ctx.isFooterAppear = true
                                }
                            }
                        }
                        willDisappear {
                            ctx.isFooterAppear = false
                        }
                        refreshStateDidChange {
                            when(it) {
                                FooterRefreshState.REFRESHING -> {
                                    ctx.footerRefreshText = "加载更多中.."
                                    // 数据只拉到一个 在最后页需要触发新的加载
                                    if (ctx.currentIndex < ctx.videoFeeds.size - 1) {
                                        ctx.footerRefreshRef.view?.endRefresh(FooterRefreshEndState.SUCCESS)
                                    } else if (!ctx.isLoadingVideo && ctx.currentIndex == ctx.videoFeeds.size - 1) {
                                        ctx.isLoadingVideo = true
                                        LoadVideosManager.requestFeeds{ feedList->
                                            ctx.videoFeeds.addAll(feedList)
                                            ctx.isLoadingVideo = false
                                            ctx.footerRefreshRef.view?.endRefresh(
                                                FooterRefreshEndState.SUCCESS)
                                        }
                                    }
                                }
                                FooterRefreshState.IDLE -> {
                                    ctx.flowNextPage(FlowType.FooterEnd)
                                    ctx.footerRefreshText = "加载更多"
                                }
                                FooterRefreshState.NONE_MORE_DATA -> ctx.footerRefreshText = "无更多数据"
                                FooterRefreshState.FAILURE -> ctx.footerRefreshText = "点击重试加载更多"
                                else -> {}
                            }
                        }
                    }
                    vif({ctx.footerRefreshText == "加载更多中.."}) {
                        ActivityIndicator {
                            attr {
                                marginRight(6f)
                            }
                        }
                    }
                    Text {
                        attr {
                            color(Color.WHITE)
                            fontSize(20f)
                            textAlignCenter()
                            text(ctx.footerRefreshText)
                        }
                    }
                }
            }
        }
    }

    enum class FlowType{
        FooterEnd,
        VideoEnd
    }

    private fun flowNextPage(type: FlowType) {
        val pageListView = pageListRef.view
        pageListView?.let {
            val viewWidth = it.flexNode.layoutFrame.width
            val viewHeight = it.flexNode.layoutFrame.height
            if (type == FlowType.VideoEnd) {
                if (it.renderView != null && !isDragging && viewWidth > 0 && viewHeight > 0) {
                    it.scrollToPageIndex(currentIndex + 1, true)
                }
            } else if (type == FlowType.FooterEnd) {
                /*
                 这里用footer可见滑动控制滑动
                 Footer出现过程中isDragging为true，仍在滚动过程
                 */
                if (it.renderView != null && isFooterAppear && viewWidth > 0 && viewHeight > 0) {
                    it.scrollToPageIndex(currentIndex + 1, true)
                }
            }
        }
    }
}

internal class PageListVideoViewAttr : ComposeAttr() {
    var customHeight: Float = 0f
}

internal class PageListVideoViewEvent : ComposeEvent()

// 提供扩展函数供外部使用
internal fun ViewContainer<*, *>.PageListVideoView(init: PageListVideoView.() -> Unit) {
    addChild(PageListVideoView(), init)
}
