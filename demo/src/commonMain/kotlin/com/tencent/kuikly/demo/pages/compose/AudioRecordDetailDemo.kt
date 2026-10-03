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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
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
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 复刻 ima 的 AudioRecordDetailPager（录音纪要详情页）。
 *
 * 原页面依赖大量 ima 内部 ViewModel / Service / 组件，本 Demo 用纯 Kuikly Compose DSL
 * 复刻其 UI 与交互结构，并用本地 mock 状态模拟「录音 -> 实时识别 -> 停止上传」流程，
 * 不依赖任何外部业务模块，可直接在 Demo 中运行。
 */
@Page("AudioRecordDetailDemo")
class AudioRecordDetailDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            CompositionLocalProvider(LocalAudioRecordColors provides AudioRecordLightColors) {
                AudioRecordDetailScreen()
            }
        }
    }
}

// ============================================================
// 颜色主题（对应原页面的 LocalThemeColorDefine）
// ============================================================
private data class AudioRecordColors(
    val grey0: Color,          // 页面背景
    val primaryBlack1: Color,  // 主文本
    val primaryBlack2: Color,  // 次要文本 / 时间戳
    val primaryBlack3: Color,
    val primaryBlack5: Color,  // 弱文本
    val accent: Color,         // 录音主按钮
)

private val AudioRecordLightColors = AudioRecordColors(
    grey0 = Color(0xFFF5F5F5),
    primaryBlack1 = Color(0xFF000000),
    primaryBlack2 = Color(0xFF666666),
    primaryBlack3 = Color(0xFF999999),
    primaryBlack5 = Color(0xFFB2B2B2),
    accent = Color(0xFF1AAD19),
)

private val LocalAudioRecordColors = compositionLocalOf { AudioRecordLightColors }

// ============================================================
// 状态枚举 & 数据模型（对应原页面的 AudioRecordState / RecognitionData）
// ============================================================
private enum class RecordState {
    IDLE, STARTING, RECORDING, PAUSED, STOPPING, STOPPED, UPLOADED, UPLOAD_FAILED, CANCELED
}

private data class RecognitionItem(
    val index: Int,
    val startTime: Long,
    val content: String,
)

private fun formatMillisToCompactTime(millis: Long): String {
    val totalSec = millis / 1000
    val sec = totalSec % 60
    val min = (totalSec / 60) % 60
    val hr = totalSec / 3600
    return if (hr > 0) String.format("%d:%02d:%02d", hr, min, sec)
    else String.format("%02d:%02d", min, sec)
}

// ============================================================
// 主页面
// ============================================================
@Composable
private fun AudioRecordDetailScreen() {
    val colors = LocalAudioRecordColors.current
    val safeArea = LocalConfiguration.current.safeAreaInsets
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var recordState by remember { mutableStateOf(RecordState.IDLE) }
    var durationMs by remember { mutableStateOf(0L) }
    var items by remember { mutableStateOf(listOf<RecognitionItem>()) }
    var recordTips by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFloatBall by remember { mutableStateOf(false) }
    var mediaTitle by remember { mutableStateOf("录音纪要") }
    var itemCounter by remember { mutableStateOf(0) }

    // 计时器：录音时每秒 +1s
    LaunchedEffect(recordState) {
        if (recordState == RecordState.RECORDING) {
            while (recordState == RecordState.RECORDING) {
                delay(1000)
                durationMs += 1000
            }
        }
    }

    // mock 实时识别：录音时每 3s 追加一段识别文本
    LaunchedEffect(recordState) {
        if (recordState == RecordState.RECORDING) {
            while (recordState == RecordState.RECORDING) {
                delay(3000)
                if (recordState != RecordState.RECORDING) break
                itemCounter += 1
                val snippet = when (itemCounter % 3) {
                    0 -> "好的，那我们接下来讨论一下整体方案的落地节奏，以及各端联调的时间点。"
                    1 -> "这边提到需要优先保证 Android 与 iOS 的体验一致性，鸿蒙作为后续补充。"
                    else -> "关于录音转写准确率，建议在安静环境下使用，长语音支持最长两小时。"
                }
                items = items + RecognitionItem(
                    index = itemCounter,
                    startTime = durationMs,
                    content = snippet,
                )
            }
        }
    }

    // 监听滚动：离开底部时显示「回到底部」悬浮球
    LaunchedEffect(listState) {
        snapshotFlow {
            Triple(
                listState.lastScrolledForward,
                listState.lastScrolledBackward,
                listState.canScrollForward,
            )
        }.collect { (_, _, canScrollForward) ->
            showFloatBall = canScrollForward
        }
    }

    // 在底部时，新内容自动滚到底
    LaunchedEffect(items.size) {
        if (items.isNotEmpty() && !showFloatBall) {
            listState.animateScrollToItem(items.lastIndex)
        }
    }

    // 首次进入提示（对应原页面 IKVService 去重逻辑，这里简化为仅首次）
    LaunchedEffect(Unit) {
        if (recordState == RecordState.IDLE) {
            recordTips = "支持中英文录音最长2小时"
            delay(3000)
            recordTips = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.grey0)
            .padding(top = safeArea.top.dp, bottom = safeArea.bottom.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showCancelDialog) {
            Dialog(onDismissRequest = { showCancelDialog = false }) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                "确认放弃录音",
                                fontWeight = FontWeight.W500,
                                fontSize = 17.sp,
                                color = colors.primaryBlack1,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "放弃本次录音，并删除已录内容",
                                fontSize = 14.sp,
                                color = colors.primaryBlack2,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.End) {
                                Text(
                                    "取消",
                                    modifier = Modifier
                                        .clickable { showCancelDialog = false }
                                        .padding(8.dp),
                                    color = colors.primaryBlack2,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    "确认",
                                    modifier = Modifier
                                        .clickable {
                                            showCancelDialog = false
                                            // 模拟「放弃并删除」：回到初始态、清空内容
                                            recordState = RecordState.IDLE
                                            durationMs = 0
                                            items = emptyList()
                                            itemCounter = 0
                                        }
                                        .padding(8.dp),
                                    color = colors.accent,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.W500,
                                )
                            }
                        }
                    }
                }
            }
        }

        TitleBar(
            title = mediaTitle,
            onClose = { showCancelDialog = true },
            onRename = {
                // 原页面打开重命名弹窗；Demo 中简化为切换标题演示
                mediaTitle = if (mediaTitle == "录音纪要") "我的录音" else "录音纪要"
            },
        )

        Box(
            Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (items.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (recordState == RecordState.RECORDING) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                        Text(
                            text = if (recordState == RecordState.RECORDING) "正在识别" else "暂未识别到文字",
                            color = colors.primaryBlack5,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(
                                start = if (recordState == RecordState.RECORDING) 8.dp else 0.dp
                            ),
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        items = items,
                        key = { item -> item.index },
                    ) { item ->
                        ListItem(item = item)
                    }

                    // 最下面垫一个占位，避免最后一条被底部渐变遮罩挡住
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // 底部渐变遮罩（透明 -> 页面背景色）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.grey0.copy(alpha = 0f),
                                colors.grey0.copy(alpha = 1f),
                            )
                        )
                    )
            )

            if (showFloatBall && items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(items.lastIndex)
                            }
                        }
                ) {
                    Text(
                        "↓",
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.primaryBlack1,
                        fontSize = 18.sp,
                    )
                }
            }
        }

        RecorderBar(
            state = recordState,
            durationMs = durationMs,
            tips = recordTips,
            onCancel = { showCancelDialog = true },
            onConfirm = {
                recordState = RecordState.STOPPED
                recordTips = "上传中...0%"
                // 模拟上传完成后重置回初始态
                scope.launch {
                    delay(1500)
                    items = emptyList()
                    itemCounter = 0
                    durationMs = 0
                    recordState = RecordState.IDLE
                    recordTips = ""
                }
            },
            onPause = { recordState = RecordState.PAUSED },
            onResume = { recordState = RecordState.RECORDING },
            onStart = {
                itemCounter = 0
                items = emptyList()
                durationMs = 0
                recordState = RecordState.RECORDING
            },
        )
    }
}

// ============================================================
// 子组件
// ============================================================
@Composable
private fun ListItem(item: RecognitionItem) {
    val colors = LocalAudioRecordColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = formatMillisToCompactTime(item.startTime),
            fontSize = 14.sp,
            color = colors.primaryBlack2,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = item.content,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            color = colors.primaryBlack1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TitleBar(
    title: String,
    onClose: () -> Unit,
    onRename: () -> Unit,
) {
    val colors = LocalAudioRecordColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.W500,
                color = colors.primaryBlack1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 225.dp)
                    .clickable { onRename() },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onRename() }
                        .padding(start = 4.dp, end = 10.dp)
                        .padding(vertical = 10.dp)
                        .size(20.dp)
                        .align(Alignment.CenterStart),
                ) {
                    Text(
                        "✎",
                        color = colors.primaryBlack3,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable { onClose() },
        ) {
            Text(
                "✕",
                modifier = Modifier.size(24.dp).align(Alignment.Center),
                color = colors.primaryBlack1,
                fontSize = 20.sp,
            )
        }
    }
}

@Composable
private fun RecorderBar(
    state: RecordState,
    durationMs: Long,
    tips: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStart: () -> Unit,
) {
    val colors = LocalAudioRecordColors.current
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.grey0),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (tips.isNotEmpty()) {
            Text(
                tips,
                fontSize = 13.sp,
                color = colors.primaryBlack5,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (state == RecordState.RECORDING || state == RecordState.PAUSED) {
            Text(
                formatMillisToCompactTime(durationMs),
                fontSize = 14.sp,
                color = colors.primaryBlack2,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            // 取消
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryBlack5.copy(alpha = 0.15f))
                    .clickable { onCancel() },
            ) {
                Text(
                    "✕",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primaryBlack1,
                    fontSize = 20.sp,
                )
            }

            // 中间录音/停止/继续 主按钮
            val centerColor = if (state == RecordState.RECORDING) Color(0xFFE64340) else colors.accent
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(centerColor)
                    .clickable {
                        when (state) {
                            RecordState.IDLE -> onStart()
                            RecordState.RECORDING -> onConfirm()
                            RecordState.PAUSED -> onResume()
                            else -> {}
                        }
                    },
            ) {
                Text(
                    when (state) {
                        RecordState.IDLE -> "开始"
                        RecordState.RECORDING -> "停止"
                        RecordState.PAUSED -> "继续"
                        else -> ""
                    },
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                )
            }

            // 暂停（仅录音中显示）
            if (state == RecordState.RECORDING) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.primaryBlack5.copy(alpha = 0.15f))
                        .clickable { onPause() },
                ) {
                    Text(
                        "⏸",
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.primaryBlack1,
                        fontSize = 18.sp,
                    )
                }
            } else {
                Box(modifier = Modifier.size(44.dp))
            }
        }
    }
}
