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
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.demo.pages.base.BridgeModule
import kotlinx.coroutines.delay

private const val VIDEO_EXPAND_ANIM_MS = 360

/** 返回竖屏时先保留蒙层，requestPortrait 后再卸掉 */
private const val PORTRAIT_HOLD_MS = 100L

/** 与 ComposeVideoOrientationDemo 一致：竖屏列表顶栏高度 */
private val PortraitVideoHeaderHeight = 220.dp

/** 返回竖屏时冻结蒙层横屏尺寸，避免 L4 在容器仍横时先缩成竖屏触发错误居中 */
private data class OrientationFrozenLayout(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val videoWidth: Dp,
    val videoHeight: Dp,
)

/** 调试用外框：Kuikly Compose 无 border Modifier，用 padding 模拟粗边框 */
@Composable
private fun DebugFrame(
    color: Color,
    thickness: Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(color)
            .padding(thickness),
    ) {
        content()
    }
}

private fun lerpDp(start: Dp, end: Dp, fraction: Float): Dp = start + (end - start) * fraction

/** 横屏全屏：长边撑满 16:9 */
private fun landscapeFullscreenSize(windowWidth: Dp, windowHeight: Dp): Pair<Dp, Dp> {
    return if (windowWidth >= windowHeight) {
        windowWidth to (windowWidth * 9f / 16f)
    } else {
        (windowHeight * 16f / 9f) to windowHeight
    }
}

/** 蒙层竖屏初始尺寸：宽 = 窗口窄边对应的全宽，高 = 顶栏 220dp */
private fun portraitOverlayVideoSize(windowWidth: Dp, windowHeight: Dp): Pair<Dp, Dp> {
    val width = if (windowWidth < windowHeight) windowWidth else windowHeight
    return width to PortraitVideoHeaderHeight
}

@Page("ComposeOrientationOverlayDemo")
internal class ComposeOrientationOverlayDemo : ComposeContainer() {
    override fun createExternalModules(): Map<String, Module>? {
        return mapOf(BridgeModule.MODULE_NAME to BridgeModule())
    }

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeOrientationOverlayDemoContent()
        }
    }
}

@Composable
private fun ComposeOrientationOverlayDemoContent() {
    val pager = LocalActivity.current.getPager() as Pager
    val bridgeModule = pager.acquireModule<BridgeModule>(BridgeModule.MODULE_NAME)
    var showOverlay by remember { mutableStateOf(false) }
    var shouldRequestPortrait by remember { mutableStateOf(false) }
    var frozenOverlayLayout by remember { mutableStateOf<OrientationFrozenLayout?>(null) }
    /** 点横屏后锁定竖屏蒙层尺寸，直到窗口约束真正变横屏，避免旋转中间态坐标乱跳 */
    var enterPortraitLock by remember { mutableStateOf<OrientationFrozenLayout?>(null) }
    var statusLabel by remember { mutableStateOf("竖屏 · 列表顶栏") }

    // 返回时：冻结横屏蒙层 → requestPortrait → 短暂保留再卸掉，避免 L4 提前缩竖屏。
    LaunchedEffect(shouldRequestPortrait) {
        if (shouldRequestPortrait && showOverlay) {
            bridgeModule.requestPortrait()
            delay(PORTRAIT_HOLD_MS)
            showOverlay = false
            frozenOverlayLayout = null
            enterPortraitLock = null
            shouldRequestPortrait = false
            statusLabel = "竖屏 · 列表顶栏"
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val windowW = maxWidth
        val windowH = maxHeight
        val isLandscape = windowW > windowH
        val portraitWidth = if (maxWidth < maxHeight) maxWidth else maxHeight
        val portraitHeight = if (maxWidth < maxHeight) maxHeight else maxWidth

        LaunchedEffect(isLandscape) {
            if (isLandscape) {
                enterPortraitLock = null
            }
        }

        val activePortraitLock = enterPortraitLock?.takeUnless { isLandscape }
        val activeFrozenLayout = frozenOverlayLayout

        // 蒙层期间 L5 根始终铺满窗口；竖屏锁/冻结尺寸只交给蒙层内部处理，避免 L5 在横屏窗里被裁成竖条看不见蒙层
        val composeRootAlign = if (showOverlay) Alignment.Center else Alignment.TopStart
        val composeRootModifier = if (showOverlay) {
            Modifier.fillMaxSize()
        } else {
            Modifier.size(portraitWidth, portraitHeight)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            DebugFrame(
                color = Color(0xFFFF00FF),
                modifier = composeRootModifier.align(composeRootAlign),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!showOverlay) {
                        DebugFrame(
                            color = Color(0xFF00C853),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            PortraitMockPage(
                                onEnterLandscape = {
                                    val (videoW, videoH) = portraitOverlayVideoSize(windowW, windowH)
                                    enterPortraitLock = OrientationFrozenLayout(
                                        windowWidth = windowW,
                                        windowHeight = windowH,
                                        videoWidth = videoW,
                                        videoHeight = videoH,
                                    )
                                    frozenOverlayLayout = null
                                    bridgeModule.requestLandscape()
                                    showOverlay = true
                                    statusLabel = "横屏 · 等待转屏"
                                },
                            )
                        }
                    }

                    if (showOverlay) {
                        PhoneStyleFullscreenOverlay(
                            isLandscape = isLandscape,
                            portraitLock = activePortraitLock,
                            frozenLayout = activeFrozenLayout,
                            onBack = {
                                val (videoWidth, videoHeight) = landscapeFullscreenSize(windowW, windowH)
                                frozenOverlayLayout = OrientationFrozenLayout(
                                    windowWidth = windowW,
                                    windowHeight = windowH,
                                    videoWidth = videoWidth,
                                    videoHeight = videoHeight,
                                )
                                enterPortraitLock = null
                                shouldRequestPortrait = true
                                statusLabel = "竖屏 · 返回中"
                            },
                        )
                    }

                    if (!showOverlay) {
                        OrientationDebugPanel(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            statusLabel = statusLabel,
                            windowW = windowW,
                            windowH = windowH,
                            lockLabel = "无锁",
                            compact = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrientationDebugPanel(
    statusLabel: String,
    windowW: Dp,
    windowH: Dp,
    lockLabel: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (compact) Alignment.End else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (!compact) {
            Text(
                text = "Compose 转屏调试",
                color = Color(0xFF111111),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = statusLabel,
            color = Color(0xFF1565C0),
            fontSize = if (compact) 11.sp else 12.sp,
        )
        Text(
            text = "窗口 ${windowW.value.toInt()} x ${windowH.value.toInt()} · $lockLabel",
            color = Color(0xFF78909C),
            fontSize = if (compact) 9.sp else 10.sp,
        )
        Text(
            text = if (compact) {
                "红Native 紫Compose 黄蒙层"
            } else {
                "红框=L4 Native | 紫框=L5 Compose根 | 绿框=列表 | 黄框=蒙层"
            },
            color = Color(0xFF78909C),
            fontSize = 9.sp,
        )
    }
}

/** 模拟竖屏视频+评论页：顶栏黑色蒙层里放横条视频 */
@Composable
private fun PortraitMockPage(
    onEnterLandscape: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F6FA)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PortraitVideoHeaderHeight)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            MockVideoRect(
                modifier = Modifier.fillMaxSize(),
                label = "模拟视频",
                subLabel = "竖屏顶栏横条",
            )
            OrientationActionButton(
                label = "横屏",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                onClick = onEnterLandscape,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .padding(16.dp),
        ) {
            Text(
                text = "视频评论",
                color = Color(0xFF111111),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "竖屏：顶栏横条在黑色蒙层上；点横屏后全屏黑蒙层，矩形居中再放大到全屏。",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            repeat(6) { index ->
                Text(
                    text = "评论 #${index + 1}：转屏调试占位内容",
                    color = Color(0xFF333333),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

/**
 * 全屏蒙层：父级 L5 根已锁定尺寸；此处 fillMaxSize 铺满父容器即可。
 */
@Composable
private fun PhoneStyleFullscreenOverlay(
    isLandscape: Boolean,
    portraitLock: OrientationFrozenLayout? = null,
    frozenLayout: OrientationFrozenLayout? = null,
    onBack: () -> Unit,
) {
    val videoProgress = remember { Animatable(if (frozenLayout != null) 1f else 0f) }
    val layoutFrozen = frozenLayout != null
    val portraitLocked = portraitLock != null && !layoutFrozen

    LaunchedEffect(layoutFrozen, portraitLocked, isLandscape) {
        if (layoutFrozen || portraitLocked) return@LaunchedEffect
        if (isLandscape) {
            videoProgress.animateTo(1f, tween(VIDEO_EXPAND_ANIM_MS))
        } else {
            videoProgress.snapTo(0f)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val windowW = maxWidth
        val windowH = maxHeight
        val progress = if (layoutFrozen) 1f else videoProgress.value.coerceIn(0f, 1f)
        val (initialW, initialH) = if (portraitLocked) {
            portraitLock.videoWidth to portraitLock.videoHeight
        } else {
            portraitOverlayVideoSize(windowW, windowH)
        }
        val (targetW, targetH) = if (layoutFrozen) {
            frozenLayout.videoWidth to frozenLayout.videoHeight
        } else {
            landscapeFullscreenSize(windowW, windowH)
        }
        val videoW = if (layoutFrozen) targetW else lerpDp(initialW, targetW, progress)
        val videoH = if (layoutFrozen) targetH else lerpDp(initialH, targetH, progress)

        DebugFrame(
            color = Color(0xFFFFEB3B),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                MockVideoRect(
                    modifier = Modifier.size(videoW, videoH),
                    label = "模拟视频",
                    subLabel = when {
                        layoutFrozen -> "返回竖屏 · 冻结横屏"
                        portraitLocked -> "竖屏锁 ${windowW.value.toInt()}x${windowH.value.toInt()}"
                        !isLandscape -> "竖屏横条 · 等待转屏"
                        progress < 1f -> "横屏 · 放大中"
                        else -> "横屏全屏"
                    },
                )
                OrientationActionButton(
                    label = "竖屏",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 32.dp),
                    onClick = onBack,
                )
            }
        }
    }
}

@Composable
private fun MockVideoRect(
    modifier: Modifier = Modifier,
    label: String,
    subLabel: String,
) {
    Box(
        modifier = modifier.background(Color(0xFF2196F3)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subLabel,
                color = Color(0xFFE3F2FD),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun OrientationActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color(0xCCFFFFFF), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFF111111),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
