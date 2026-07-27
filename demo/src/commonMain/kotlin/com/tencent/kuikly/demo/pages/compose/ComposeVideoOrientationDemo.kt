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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.PlayState
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.demo.pages.base.BridgeModule
import kotlinx.coroutines.delay

/** 折叠屏展开态最小宽度阈值：宽于此值认为处于展开态 */
private val FoldableExpandedMinWidth = 600.dp

/** 折叠屏展开态右侧推荐视频列宽度 */
private val FoldableRecommendColumnWidth = 280.dp

/** 列表预渲染数量调大，避免快速横竖屏切换后底部露出半截空白。 */
private const val CommentBeyondBoundsItemCount = 30

private const val VIDEO_DEBUG_TAG = "BD_VideoOrientation"

/** 点击横屏后，视频从原始尺寸放到当前窗口长边撑满的动画时间。 */
private const val VIDEO_EXPAND_ANIM_MS = 360

/** 返回竖屏兜底超时：尺寸回调异常时强制卸蒙层。 */
private const val PORTRAIT_DISMISS_TIMEOUT_MS = 1200L

/** 顶部播放器在竖屏列表中的高度。 */
private val PortraitVideoHeaderHeight = 220.dp

private fun videoDebugLog(message: String) {
    println("[$VIDEO_DEBUG_TAG] $message")
}

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

private data class FrozenOverlayLayout(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val videoWidth: Dp,
    val videoHeight: Dp,
)

private fun targetVideoSize(windowWidth: Dp, windowHeight: Dp): Pair<Dp, Dp> {
    return if (windowWidth >= windowHeight) {
        windowWidth to (windowWidth * 9f / 16f)
    } else {
        (windowHeight * 16f / 9f) to windowHeight
    }
}

private data class RecommendedVideo(
    val id: Int,
    val title: String,
    val duration: String,
)

@Page("ComposeVideoOrientationDemo")
internal class ComposeVideoOrientationDemo : ComposeContainer() {
    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        return externalModules
    }

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeVideoOrientationDemoContent()
        }
    }
}

@Composable
private fun ComposeVideoOrientationDemoContent() {
    val pager = LocalActivity.current.getPager() as Pager
    val bridgeModule = pager.acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
    var showFullscreenOverlay by remember { mutableStateOf(false) }
    var shouldRequestPortrait by remember { mutableStateOf(false) }
    var frozenOverlayLayout by remember { mutableStateOf<FrozenOverlayLayout?>(null) }
    var frozenFoldableExpanded by remember { mutableStateOf<Boolean?>(null) }
    var playControl by remember { mutableStateOf(VideoPlayControl.PLAY) }
    var playResumeCount by remember { mutableIntStateOf(0) }
    var videoDebugStatus by remember { mutableStateOf("init") }
    var videoPlayTimeSec by remember { mutableIntStateOf(0) }
    val comments = remember { (1..30).toList() }
    val recommendedVideos = remember { buildRecommendedVideos() }
    // 低码率短视频，弱网下比原 oceans 视频更容易稳定播放。
    val videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"

    val movableVideo = remember {
        movableContentOf { modifier: Modifier ->
            MovableVideoPlayer(
                videoUrl = videoUrl,
                playControl = playControl,
                modifier = modifier,
                onPlayStateChanged = { state, extInfo ->
                    val detail = extInfo.toString()
                    videoDebugStatus = when (state) {
                        PlayState.PLAY_END -> "PLAY_END（已开启循环，应自动重播）"
                        else -> "${state.name} | $detail"
                    }
                    videoDebugLog("playState=${state.name} ext=$detail playControl=$playControl")
                },
                onPlayTimeChanged = { curTime, totalTime ->
                    videoPlayTimeSec = curTime / 1000
                    if (curTime % 5000 < 200) {
                        videoDebugLog("playTime=${curTime}ms total=${totalTime}ms")
                    }
                },
            )
        }
    }

    // 全屏切换后 Surface 可能解绑，延迟恢复播放。
    LaunchedEffect(showFullscreenOverlay) {
        videoDebugLog("showFullscreenOverlay -> $showFullscreenOverlay")
        delay(if (showFullscreenOverlay) 80 else 0)
        playResumeCount++
        playControl = VideoPlayControl.PLAY
        videoDebugLog("resumePlay #$playResumeCount playControl=PLAY overlay=$showFullscreenOverlay")
    }

    // 返回：先 requestPortrait；蒙层不冻结尺寸，跟窗口 fillMaxSize。
    // 窗口真正回到竖屏后再卸蒙层，避免竖屏上残留 800x360 横屏黑块。
    LaunchedEffect(shouldRequestPortrait) {
        if (shouldRequestPortrait && showFullscreenOverlay) {
            videoDebugLog("exit: requestPortrait first, keep fillMaxSize overlay")
            bridgeModule.requestPortrait()
            delay(PORTRAIT_DISMISS_TIMEOUT_MS)
            if (shouldRequestPortrait && showFullscreenOverlay) {
                videoDebugLog("exit: portrait dismiss timeout, force dismiss")
                showFullscreenOverlay = false
                frozenOverlayLayout = null
                frozenFoldableExpanded = null
                shouldRequestPortrait = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val portraitWidth = if (maxWidth < maxHeight) maxWidth else maxHeight
            val portraitHeight = if (maxWidth < maxHeight) maxHeight else maxWidth
            val isLandscape = maxWidth > maxHeight
            // 用当前窗口宽度判断展开态，比 portraitWidth 更快响应折叠屏展开。
            val isFoldableExpanded = frozenFoldableExpanded
                ?: (maxWidth >= FoldableExpandedMinWidth)

            // 竖屏到位：立刻卸蒙层（此时蒙层已是竖屏全屏，不会残留横屏尺寸块）。
            LaunchedEffect(shouldRequestPortrait, isLandscape, showFullscreenOverlay) {
                if (shouldRequestPortrait && showFullscreenOverlay && !isLandscape) {
                    videoDebugLog("exit: portrait settled ${maxWidth}x$maxHeight, dismiss overlay")
                    showFullscreenOverlay = false
                    frozenOverlayLayout = null
                    frozenFoldableExpanded = null
                    shouldRequestPortrait = false
                }
            }

            val pageModifier = if (isFoldableExpanded && !showFullscreenOverlay) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .width(portraitWidth)
                    .height(portraitHeight)
                    .align(Alignment.TopStart)
            }

            LaunchedEffect(portraitWidth, maxWidth, maxHeight, showFullscreenOverlay, isFoldableExpanded) {
                videoDebugLog(
                    "layout overlay=$showFullscreenOverlay constraints=${maxWidth}x$maxHeight " +
                        "portrait=${portraitWidth}x$portraitHeight foldableExpanded=$isFoldableExpanded",
                )
            }

            Box(
                modifier = pageModifier
                    .background(Color(0xFFF5F6FA)),
            ) {
                PortraitVideoCommentPage(
                    comments = comments,
                    isFoldableExpanded = isFoldableExpanded,
                    recommendedVideos = recommendedVideos,
                    movableVideo = movableVideo,
                    videoInOverlay = showFullscreenOverlay,
                    videoDebugStatus = videoDebugStatus,
                    videoPlayTimeSec = videoPlayTimeSec,
                    playResumeCount = playResumeCount,
                    onEnterFullscreen = {
                        videoDebugLog("enterFullscreen foldableExpanded=$isFoldableExpanded")
                        frozenOverlayLayout = null
                        frozenFoldableExpanded = isFoldableExpanded
                        if (!isFoldableExpanded) {
                            videoDebugLog("requestLandscape immediately on enter")
                            bridgeModule.requestLandscape()
                        }
                        showFullscreenOverlay = true
                    },
                )
            }

            if (showFullscreenOverlay) {
                if (isFoldableExpanded) {
                    FoldableFullscreenOverlay(
                        movableVideo = movableVideo,
                        onBack = {
                            videoDebugLog("exitFullscreen foldable")
                            showFullscreenOverlay = false
                            frozenFoldableExpanded = null
                        },
                    )
                } else {
                    // 退出过程中不冻结旧横屏尺寸：蒙层/视频都跟当前窗口走，竖屏到位后再卸。
                    val exiting = shouldRequestPortrait
                    val (exitVideoW, exitVideoH) = targetVideoSize(maxWidth, maxHeight)
                    PhoneFullscreenHoldOverlay(
                        modifier = Modifier.fillMaxSize(),
                        windowWidth = maxWidth,
                        windowHeight = maxHeight,
                        frozenLayout = if (exiting) {
                            FrozenOverlayLayout(
                                windowWidth = maxWidth,
                                windowHeight = maxHeight,
                                videoWidth = exitVideoW,
                                videoHeight = exitVideoH,
                            )
                        } else {
                            null
                        },
                        movableVideo = movableVideo,
                        onBack = {
                            videoDebugLog("exitFullscreen phone: requestPortrait, wait portrait then dismiss")
                            shouldRequestPortrait = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneFullscreenHoldOverlay(
    modifier: Modifier = Modifier,
    windowWidth: Dp,
    windowHeight: Dp,
    frozenLayout: FrozenOverlayLayout? = null,
    movableVideo: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
) {
    val videoProgress = remember { Animatable(if (frozenLayout != null) 1f else 0f) }
    val layoutFrozen = frozenLayout != null

    LaunchedEffect(layoutFrozen) {
        if (layoutFrozen) return@LaunchedEffect
        videoDebugLog("overlay: expand video (landscape already requested)")
        videoProgress.animateTo(1f, tween(VIDEO_EXPAND_ANIM_MS))
    }

    val progress = if (layoutFrozen) 1f else videoProgress.value.coerceIn(0f, 1f)
    val initialVideoWidth = if (windowWidth < windowHeight) windowWidth else windowHeight
    val initialVideoHeight = PortraitVideoHeaderHeight
    val (targetVideoWidth, targetVideoHeight) = if (layoutFrozen) {
        frozenLayout.videoWidth to frozenLayout.videoHeight
    } else {
        targetVideoSize(windowWidth, windowHeight)
    }
    val videoWidth = if (layoutFrozen) targetVideoWidth else lerpDp(initialVideoWidth, targetVideoWidth, progress)
    val videoHeight = if (layoutFrozen) targetVideoHeight else lerpDp(initialVideoHeight, targetVideoHeight, progress)

    // 外层铺满当前窗口挡背景；退出冻结时内层保持点击返回时的横屏尺寸。
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(windowWidth, windowHeight)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            movableVideo(
                Modifier.size(videoWidth, videoHeight),
            )
            BackButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 32.dp),
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun FoldableFullscreenOverlay(
    movableVideo: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val videoHeight = maxWidth * 9f / 16f
        movableVideo(
            Modifier
                .fillMaxWidth()
                .height(videoHeight)
                .align(Alignment.Center),
        )
        BackButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 32.dp),
            onClick = onBack,
        )
    }
}

@Composable
private fun MovableVideoPlayer(
    videoUrl: String,
    playControl: VideoPlayControl,
    modifier: Modifier = Modifier,
    onPlayStateChanged: ((PlayState, JSONObject) -> Unit)? = null,
    onPlayTimeChanged: ((Int, Int) -> Unit)? = null,
) {
    Video(
        src = videoUrl,
        playControl = playControl,
        modifier = modifier.background(Color.Black),
        onPlayStateChanged = onPlayStateChanged,
        onPlayTimeChanged = onPlayTimeChanged,
    )
}

@Composable
private fun VideoDebugPanel(
    status: String,
    playTimeSec: Int,
    playResumeCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC111111))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text("视频诊断", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("状态: $status", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        Text(
            "进度: ${playTimeSec}s | resume次数: $playResumeCount",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            "日志前缀: $VIDEO_DEBUG_TAG",
            color = Color(0xFF78909C),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun PortraitVideoCommentPage(
    comments: List<Int>,
    isFoldableExpanded: Boolean,
    recommendedVideos: List<RecommendedVideo>,
    movableVideo: @Composable (Modifier) -> Unit,
    videoInOverlay: Boolean,
    videoDebugStatus: String,
    videoPlayTimeSec: Int,
    playResumeCount: Int,
    onEnterFullscreen: () -> Unit,
) {
    if (isFoldableExpanded) {
        // 折叠屏展开态：左侧视频+评论列表，右侧推荐视频占满整页高度。
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 24.dp),
                beyondBoundsItemCount = CommentBeyondBoundsItemCount,
            ) {
                item {
                    FoldableVideoHeader(
                        movableVideo = movableVideo,
                        videoInOverlay = videoInOverlay,
                        onEnterFullscreen = onEnterFullscreen,
                    )
                }

                item {
                    VideoDebugPanel(
                        status = videoDebugStatus,
                        playTimeSec = videoPlayTimeSec,
                        playResumeCount = playResumeCount,
                    )
                }

                item {
                    VideoCommentTitle(isFoldableExpanded = true)
                }

                items(comments) { index ->
                    CommentRow(index)
                }
            }

            RecommendedVideoColumn(
                modifier = Modifier
                    .width(FoldableRecommendColumnWidth)
                    .fillMaxHeight(),
                videos = recommendedVideos,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            beyondBoundsItemCount = CommentBeyondBoundsItemCount,
        ) {
            item {
                PhoneVideoHeader(
                    movableVideo = movableVideo,
                    videoInOverlay = videoInOverlay,
                    onEnterFullscreen = onEnterFullscreen,
                )
            }

            item {
                VideoDebugPanel(
                    status = videoDebugStatus,
                    playTimeSec = videoPlayTimeSec,
                    playResumeCount = playResumeCount,
                )
            }

            item {
                VideoCommentTitle(isFoldableExpanded = false)
            }

            items(comments) { index ->
                CommentRow(index)
            }
        }
    }
}

@Composable
private fun VideoCommentTitle(isFoldableExpanded: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "视频评论",
            color = Color(0xFF111111),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (isFoldableExpanded) {
                "折叠屏展开：右侧推荐视频占满列表高度；全屏时黑色蒙层盖住页面。"
            } else {
                "普通手机：点横屏立刻 requestLandscape，蒙层与视频放大并行；返回时蒙层短暂保留防跳变。"
            },
            color = Color(0xFF666666),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PhoneVideoHeader(
    movableVideo: @Composable (Modifier) -> Unit,
    videoInOverlay: Boolean,
    onEnterFullscreen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black),
    ) {
        if (!videoInOverlay) {
            movableVideo(Modifier.fillMaxSize())
        }
        FullscreenButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            label = "横屏",
            onClick = onEnterFullscreen,
        )
    }
}

@Composable
private fun FoldableVideoHeader(
    movableVideo: @Composable (Modifier) -> Unit,
    videoInOverlay: Boolean,
    onEnterFullscreen: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black),
    ) {
        if (!videoInOverlay) {
            movableVideo(Modifier.fillMaxSize())
        }
        FullscreenButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            label = "全屏",
            onClick = onEnterFullscreen,
        )
    }
}

@Composable
private fun RecommendedVideoColumn(
    modifier: Modifier = Modifier,
    videos: List<RecommendedVideo>,
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = "推荐视频",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            beyondBoundsItemCount = 6,
        ) {
            items(videos, key = { it.id }) { video ->
                RecommendedVideoRow(video)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun RecommendedVideoRow(video: RecommendedVideo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 48.dp)
                .background(commentAvatarColor(video.id), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = video.duration,
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FullscreenButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("< 返回", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CommentRow(index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = commentAvatarColor(index),
                    shape = RoundedCornerShape(21.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("$index", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "用户 $index",
                color = Color(0xFF222222),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "这是第 $index 条评论，滑动列表时顶部播放器保持在列表头部。",
                color = Color(0xFF666666),
                fontSize = 13.sp,
            )
        }
    }
}

private fun buildRecommendedVideos(): List<RecommendedVideo> {
    return listOf(
        RecommendedVideo(1, "深海探险纪实", "12:34"),
        RecommendedVideo(2, "城市夜景延时", "08:21"),
        RecommendedVideo(3, "极限运动集锦", "15:06"),
        RecommendedVideo(4, "自然风光 4K", "22:18"),
        RecommendedVideo(5, "美食制作教程", "06:45"),
        RecommendedVideo(6, "科技产品评测", "11:02"),
        RecommendedVideo(7, "旅行 Vlog 精选", "18:37"),
        RecommendedVideo(8, "音乐现场 Live", "09:58"),
    )
}

private fun commentAvatarColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4E7CF6),
        Color(0xFF26A69A),
        Color(0xFFFF8A00),
        Color(0xFFAB47BC),
        Color(0xFFE53935),
    )
    return colors[(index - 1) % colors.size]
}
