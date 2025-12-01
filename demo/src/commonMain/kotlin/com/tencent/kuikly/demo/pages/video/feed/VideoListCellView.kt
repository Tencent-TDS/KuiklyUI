package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.appearPercentage
import com.tencent.kuikly.core.base.event.didAppear
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.PlayState
import com.tencent.kuikly.core.views.Slider
import com.tencent.kuikly.core.views.Video
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.core.views.VideoView
import com.tencent.kuikly.demo.pages.video.type.VideoItem
import com.tencent.kuikly.demo.pages.app.AppTabPage
import com.tencent.kuikly.demo.pages.video.cover.LAYER_BOTTOM

enum class CoverType{
    Cover,
    CoverAndBottomMargin,
    Width
}
fun getCoverType(coverWidth: Float, coverHeight: Float, deviceWidth: Float, deviceHeight: Float):CoverType {
    val imageScale = coverWidth / coverHeight
    val deviceScale = deviceWidth / deviceHeight
    val diffScale = imageScale / deviceScale
    if (diffScale > 0.9 && diffScale < 1.1) {
        return CoverType.Cover
    }
    if(imageScale >= 0.56 && imageScale < 0.57 && deviceScale < 0.56){
        return CoverType.CoverAndBottomMargin
    }
    return CoverType.Width
}

class VideoViewFrame{
    var type = CoverType.CoverAndBottomMargin
    var frame: Frame = Frame.zero
    constructor(type: CoverType, frame: Frame){
        this.type = type
        this.frame = frame
    }
}
fun calculateVideoFrame(coverWidth: Float, coverHeight: Float, deviceWidth: Float, deviceHeight: Float, statusBarHeight: Float):VideoViewFrame{
    val type = getCoverType(coverWidth, coverHeight, deviceWidth, deviceHeight)
    if(type == CoverType.CoverAndBottomMargin){
        val devScale = deviceWidth / deviceHeight
        KLog.i("ruifan", "calculateVideoFrame debug : devScale:$devScale")
        var updatedStatusBarHeight = statusBarHeight
        if(devScale <= 0.46){
            updatedStatusBarHeight += 88f * deviceWidth / 750f
        }
        KLog.i("ruifan", "calculateVideoFrame debug : updatedStatusBarHeight:$updatedStatusBarHeight")
        // 安卓、iOS bottom不一致
        val bottom = LAYER_BOTTOM
        val imageScale = coverWidth / coverHeight
        val realCoverHeight = deviceHeight - bottom - updatedStatusBarHeight
        val realCoverWidth = realCoverHeight * imageScale
        val f = Frame((deviceWidth - realCoverWidth) / 2f, updatedStatusBarHeight, realCoverWidth, realCoverHeight)
        KLog.i("ruifan", "calculateVideoFrame debug : imageScale:$imageScale realCoverHeight:$realCoverHeight, frame = ${f.x} ${f.y} ${f.width} ${f.height}")

        return VideoViewFrame(type, f)
    }else if(type == CoverType.Cover){
        val realWidth = deviceWidth
        val realHeight = deviceWidth * coverHeight / coverWidth
//        if (finalImageViewSize.height > self.coverImageView.frame.size.height) {
//            finalImageViewSize.height = self.coverImageView.frame.size.height;
//        }
        return VideoViewFrame(type, Frame(0f, 0f, realWidth, realHeight))
    }else{
        val realWidth = deviceWidth
        val realHeight = deviceWidth * coverHeight / coverWidth
        return VideoViewFrame(type, Frame(0f, 0f, realWidth, realHeight))
    }
}

/**
 * VideoListCellView
 * 视频页面内容组件，将每一页的内容视作为列表中的一个Cell
 * 视频流页面中PageList中vfor循环按照传递而来的视频数量定义多个VideoListCellView(VideoListCell)
 */
class VideoListCellView : ComposeView<VideoListCellAttr, VideoListCellEvent>() {
    override fun createAttr(): VideoListCellAttr {
        return VideoListCellAttr()
    }
    override fun createEvent(): VideoListCellEvent {
        return VideoListCellEvent()
    }

    private var showModal: Boolean by observable(false)     // 给每个视频都配上一个独属的评论区以及一个独属的showModel去控制评论区的显示
    private var dragSlidering by observable(false)
    private var curProcess by observable(0f)
    private var videoWithCommentHeight = 200f
    lateinit var videoRef : ViewRef<VideoView>
    private var isPoster by observable(true)
    private var isVis by observable(false)
    private var duration = 0
    var isPlay by observable(VideoPlayControl.PAUSE)

    // 用于判断出现，快速滑动过后不需preload
    private var isShow = false

    override fun created() {
        super.created()
        duration = attr.vitem.duration
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                width(pagerData.pageViewWidth)
                height(pagerData.pageViewHeight - pagerData.statusBarHeight  - AppTabPage.TAB_BOTTOM_HEIGHT)
                backgroundColor(Color.BLACK)
//                visibility(ctx.isVis)
            }
            event {
                appearPercentage {
                    if (it > 0.8 && !ctx.isShow) {
                        ctx.isShow = true
                    }
                }
            }

            Video {
                ref {
                    ctx.videoRef = it
                }
                attr {
                    width(pagerData.pageViewWidth)
                    height(pagerData.pageViewHeight - pagerData.statusBarHeight  - AppTabPage.TAB_BOTTOM_HEIGHT)
                    src(ctx.attr.vitem.videoUrl)
                    playControl(ctx.isPlay)
                    animation(Animation.springEaseInOut(1.0f, 0.92f, 1f), ctx.showModal)
                }
                event {
                    click {
                        if (ctx.isPlay == VideoPlayControl.PAUSE) {
                            ctx.isPlay = VideoPlayControl.PLAY
                        } else if (ctx.isPlay == VideoPlayControl.PLAY){
                            ctx.isPlay = VideoPlayControl.PAUSE
                        }
                    }
                    appearPercentage { percentage01->
                        // 当视频可见度超过95%时自动播放
                        if (percentage01 > 0.95f) {
                            if (ctx.isPlay != VideoPlayControl.PLAY) {
                                ctx.isPlay = VideoPlayControl.PLAY
                                ctx.event.playHandler?.invoke()
                            }
                        } else if (percentage01 < 0.005f) {
                            // 当视频几乎不可见时自动暂停
                            if (ctx.isPlay != VideoPlayControl.PAUSE) {
                                ctx.isPlay = VideoPlayControl.PAUSE
                            }
                        }
                    }
                    firstFrameDidDisplay {
                        KLog.d("KuiklyVideoView", "kuikly on frame show")
                        // 首帧出现 去处遮罩
                        ctx.isPoster = false
                        if (!ctx.isVis) {
                            ctx.isVis = true
                        }
                        ctx.event.frameShowHandler?.invoke()
                    }
                    playTimeDidChanged { curTime, totalTime ->
                        ctx.curProcess = curTime.toFloat() / totalTime
                    }
                    playStateDidChanged { state, extInfo ->
                        if (state == PlayState.PLAY_END) {
                            // 重新播放
                            ctx.seekToTime(0)
                            // 下滑
                            // ctx.event.endHandler?.invoke()
                        }
                    }


                }

            }



            // todo: 图片遮罩 在首个视频才能使用？ preload会触发 onPlay onPause 新视频遮罩不可见
            vif({ ctx.isPoster }) {
                // 遮罩
                Image {
                    attr {
                        absolutePositionAllZero()
                        src(ctx.attr.vitem.imgUrl)
                        size(pagerData.pageViewWidth, pagerData.pageViewHeight - pagerData.statusBarHeight - AppTabPage.TAB_BOTTOM_HEIGHT)
                        resizeContain()
                    }
                    event {
//                        onLoadEnd {
//                            KLog.d("QBCoverView", "onLoadEnd finish")
//                            if (!ctx.isVis) {
//                                ctx.isVis = true
//                            }
//                        }
                        didAppear {
                            KLog.d("KuiklyCost", "Cover Appear: " + DateTime.currentTimestamp().toString())
                        }
                    }
                }
            }

            // 进度条
            Slider {
                attr {
                    absolutePosition(left = SliderMarginLeft, bottom = SliderMarginBottom)
                    width(pagerData.pageViewWidth - 20f)
                    height(SliderHeight)
                    trackColor(Color.GRAY)
                    if (ctx.dragSlidering) {
                        thumbColor(Color.WHITE)
                    } else {
                        thumbColor(Color.TRANSPARENT)
                    }
                    progressColor(Color.WHITE)
                    currentProgress(ctx.curProcess)
                    backgroundColor(Color.TRANSPARENT_WHITE)
                }
                event {
                    beginDragSlider {
                        ctx.dragSlidering = true
                    }
                    endDragSlider {
                        ctx.dragSlidering = false
                    }
                    progressDidChanged {
                        ctx.curProcess = it
                    }
                }
            }

            // 侧边按钮区：头像 + 点赞 + 转发 + 评论
            SideBar {
                attr {
                    vitem = ctx.attr.vitem
                    likeNum = ctx.attr.vitem.likeNum
                    retweetNum = ctx.attr.vitem.retweetNum
//      todo              commentNum = ctx.attr.vitem.commentList.size    videoItem 中必须要设置commentList
                    commentNum = ctx.attr.vitem.commentNum
                    likeStatus = ctx.attr.vitem.likeStatus
                    favouriteNum = ctx.attr.vitem.collectNum
                }
                event {
                    backCommentViewClick {
                        ctx.showModal = true
                        // 修改视频流的高度，从而能够让暂停图标的位置可以跟随动画区域一同移动
                        // 这里高度调整会出现bug
//                        ctx.attr.videoHeight = ctx.videoWithCommentHeight
                    }
                }
            }

            // 左下底部，视频说明
            VideoDescription{
                attr {
                    vitem = ctx.attr.vitem
                }
            }

            // 底部，搜索部分
            BottomBar {
                attr {

                }
            }

            // 动画的相关设置
            vif({ctx.showModal}){
                Modal {
                    QBVideoComment {
                        attr {
                            vitem = ctx.attr.vitem
//                            commentList = ctx.attr.vitem.commentList        // todo 后续需要补充commentList
                            commentViewHeight = pagerData.pageViewHeight - pagerData.navigationBarHeight - ctx.videoWithCommentHeight
                        }
                        event {
                            close {
//                                setTimeout(200) {
                                    ctx.showModal = false
                                    ctx.attr.videoHeight = ctx.attr.videoInitialHeight
//                                }
                            }
                        }
                    }
                }
            }

        }
    }

    fun preloadVideo() {
        if (!isShow) {
            KLog.d("KuiklyVideoView", "preload: " + attr.vitem.videoUrl)
            performTaskWhenRenderViewDidLoad {
                videoRef.view?.renderView?.callMethod("preload")
            }
        }
    }

    private fun seekToTime(time: Long) {
        performTaskWhenRenderViewDidLoad {
            videoRef.view?.renderView?.callMethod("seekToTime", time.toString())
        }
    }


    override fun willRemoveFromParentView() {
        super.willRemoveFromParentView()
        KLog.i("ruifan", "item will be remove from parent, url: ${attr.vitem.imgUrl}")
    }

    // renderView移除后，重置状态
    override fun removeRenderView() {
        isPoster = true
        curProcess = 0f
        isShow = false
        super.removeRenderView()
    }
}

class VideoListCellAttr : ComposeAttr() {
    lateinit var vitem: VideoItem
    var videoHeight by observable(0f)
    var videoInitialHeight: Float = 0f
    var onPage by observable(false)
}

class VideoListCellEvent : ComposeEvent() {
    var endHandler: (() -> Unit)? = null
    var frameShowHandler: (() -> Unit)? = null
    var playHandler: (() -> Unit)? = null

    fun videoEnd(handler: () -> Unit) {
        endHandler = handler
    }

    // 传递首帧出现事件
    fun videoFrameShow(handler: () -> Unit) {
        frameShowHandler = handler
    }

    // 传递首次播放事件
    fun videoPlay(handler: () -> Unit) {
        playHandler = handler
    }
}

internal fun ViewContainer<*, *>.VideoPageListCell(init: VideoListCellView.() -> Unit) {
    addChild(VideoListCellView(), init)
}