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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.movableContentOf
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.core.views.PlayState
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.core.annotations.Page

@Page("MovableContentDemo")
class MovableContentDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            MovableContentDemoContent()
        }
    }
}

@Composable
private fun MovableContentDemoContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "movableContentOf 示例",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 8.dp, top = 40.dp),
            )
        }

        item {
            DemoCard(title = "1. 基础：内容在 Column/Row 间移动") {
                BasicMoveDemo()
            }
        }

        item {
            DemoCard(title = "2. 状态保持：移动时保留 remember 状态") {
                StatePreservationDemo()
            }
        }

        item {
            DemoCard(title = "3. 多个 movableContent：列表项重排") {
                MultiMovableContentDemo()
            }
        }

        item {
            DemoCard(title = "4. movableContentOf 带参数") {
            ParameterizedMovableContentDemo()
            }
        }

        item {
            DemoCard(title = "5. 跨容器移动：左右面板切换") {
                CrossContainerMoveDemo()
            }
        }

        item {
            DemoCard(title = "6. VideoView 播放器跨容器移动验证") {
                VideoViewMoveDemo()
            }
        }

        item {
            DemoCard(title = "7. LazyColumn item 内含 movableContent：回收后重进入") {
                LazyItemMovableContentDemo()
            }
        }

        item {
            DemoCard(title = "8. 快速连续 move（竞态压测）") {
                RapidMoveDemo()
            }
        }

        item {
            DemoCard(title = "9. 条件渲染：if(visible) 包裹的 movableContent") {
                ConditionalMovableDemo()
            }
        }
    }
}

@Composable
private fun DemoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun BasicMoveDemo() {
    var isVertical by remember { mutableStateOf(true) }

    // Three differently-sized, differently-colored boxes — in Column they stack top-to-bottom,
    // in Row they line up left-to-right, making the layout change obvious.
    val movableContent = remember {
        movableContentOf {
            MovableBox("红", Color(0xFFE53935), 56.dp, 40.dp)
            MovableBox("绿", Color(0xFF43A047), 72.dp, 52.dp)
            MovableBox("蓝", Color(0xFF1E88E5), 88.dp, 64.dp)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { isVertical = !isVertical }) {
            Text(if (isVertical) "切换为 Row（横排）" else "切换为 Column（竖排）")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (isVertical) "当前：Column — 三色块竖排" else "当前：Row — 三色块横排",
            fontSize = 12.sp,
            color = Color(0xFF757575),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .border(2.dp, if (isVertical) Color(0xFF1E88E5) else Color(0xFFE53935), RoundedCornerShape(8.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isVertical) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    movableContent()
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    movableContent()
                }
            }
        }
    }
}

@Composable
private fun MovableBox(
    label: String,
    color: Color,
    width: Dp,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(color, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatePreservationDemo() {
    var isTop by remember { mutableStateOf(true) }
    val movableCounter = remember {
        movableContentOf {
            var count by remember { mutableStateOf(0) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { count++ },
                    modifier = Modifier.semantics { testTag = "demo2_btn_increment" },
                ) { Text("+") }
                Text(
                    "计数: $count",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF673AB7),
                    modifier = Modifier.semantics { testTag = "demo2_counter_text" },
                )
                Button(
                    onClick = { count-- },
                    modifier = Modifier.semantics { testTag = "demo2_btn_decrement" },
                ) { Text("-") }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { isTop = !isTop },
            modifier = Modifier.semantics { testTag = "demo2_btn_move" },
        ) {
            Text(if (isTop) "移到底部" else "移到顶部")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
        ) {
            if (isTop) {
                Column {
                    movableCounter()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("↓ 占位区域 ↓", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Column {
                    Text("↑ 占位区域 ↑", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    movableCounter()
                }
            }
        }

        Text("移动后计数应保持不变", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

@Composable
private fun MultiMovableContentDemo() {
    var items by remember { mutableStateOf(listOf("A", "B", "C", "D")) }
    val movableMap = remember { mutableMapOf<String, @Composable () -> Unit>() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = {
                if (items.size >= 2) {
                    items = items.toMutableList().also {
                        val first = it.removeAt(0)
                        it.add(first)
                    }
                }
            }) { Text("循环左移") }

            Button(onClick = {
                if (items.size >= 2) {
                    items = items.toMutableList().also {
                        val last = it.removeAt(it.lastIndex)
                        it.add(0, last)
                    }
                }
            }) { Text("循环右移") }

            Button(onClick = {
                items = items.reversed()
            }) { Text("反转") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val movable = movableMap.getOrPut(item) {
                    var clickCount by remember { mutableStateOf(0) }
                    movableContentOf {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(itemColor(item), RoundedCornerShape(8.dp))
                                .clickable { clickCount++ },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$item\n$clickCount", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                movable()
            }
        }

        Text("每个 item 的点击计数在移动后保持", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

private fun itemColor(label: String): Color = when (label) {
    "A" -> Color(0xFFE91E63)
    "B" -> Color(0xFF2196F3)
    "C" -> Color(0xFF4CAF50)
    "D" -> Color(0xFFFF9800)
    else -> Color.Gray
}

@Composable
private fun ParameterizedMovableContentDemo() {
    var currentIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFFFF9800))
    val labels = listOf("蓝", "粉", "绿", "橙")

    val movableItem = remember {
        movableContentOf { index: Int ->
            var tapCount by remember { mutableStateOf(0) }
            Box(
                modifier = Modifier
                    .size(100.dp, 60.dp)
                    .background(colors[index], RoundedCornerShape(8.dp))
                    .clickable { tapCount++ },
                contentAlignment = Alignment.Center,
            ) {
                Text("${labels[index]} - 点击$tapCount", color = Color.White, fontSize = 14.sp)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { currentIndex = (currentIndex + 1) % colors.size }) {
                Text("切换参数")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            movableItem(currentIndex)
        }

        Text("参数变化时内容重组，点击计数因 remember 而累加", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

@Composable
private fun CrossContainerMoveDemo() {
    var inLeft by remember { mutableStateOf(true) }
    val movablePanel = remember {
        movableContentOf {
            var toggleCount by remember { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("可移动面板", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "切换次数: $toggleCount",
                    fontSize = 14.sp,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.semantics { testTag = "demo5_toggle_count_text" },
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { toggleCount++ },
                    modifier = Modifier.semantics { testTag = "demo5_btn_increment" },
                ) { Text("点击+1") }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { inLeft = !inLeft },
            modifier = Modifier.semantics { testTag = "demo5_btn_move_panel" },
        ) {
            Text(if (inLeft) "移到右面板" else "移到左面板")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (inLeft) {
                    movablePanel()
                } else {
                    Text("空面板", color = Color(0xFF90CAF9), fontSize = 14.sp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFFCE4EC), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!inLeft) {
                    movablePanel()
                } else {
                    Text("空面板", color = Color(0xFFF48FB1), fontSize = 14.sp)
                }
            }
        }

        Text("面板在左右容器间移动，状态保持", fontSize = 12.sp, color = Color(0xFF999999))
    }
}

/**
 * Demo6: VideoView 播放器在容器间移动验证
 *
 * 验证思路：
 *   - movableContent 内嵌真实的 VideoView（KRVideoView）播放网络视频
 *   - playTimeDidChanged 回调把当前播放进度暴露到 Compose 状态，显示在 UI 上
 *   - 若 move 后播放时间继续递增（不归零）→ native player 实例没有重建，资源保留
 *   - 若 move 后时间归零/停止 → view 被重建了，native player 资源丢失
 *
 * 这是真正的 native view 资源连续性验证，不依赖 Compose remember 状态。
 */
@Composable
private fun VideoViewMoveDemo() {
    // 当前视频所在容器：true = 上方，false = 下方
    var isTop by remember { mutableStateOf(true) }
    // move 次数
    var moveCount by remember { mutableIntStateOf(0) }
    // 播放进度（ms），由 VideoView 内部的 playTimeDidChanged 回调写入
    var playTimeMs by remember { mutableIntStateOf(0) }
    // 播放状态文字
    var statusText by remember { mutableStateOf("加载中...") }

    val movableVideo = remember {
        movableContentOf {
            Video(
                src = "http://vjs.zencdn.net/v/oceans.mp4",
                playControl = VideoPlayControl.PLAY,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                onPlayStateChanged = { state, _ ->
                    statusText = when (state) {
                        PlayState.PLAYING -> "▶ 播放中"
                        PlayState.BUFFERING -> "⏳ 缓冲中"
                        PlayState.PAUSED -> "⏸ 已暂停"
                        PlayState.PLAY_END -> "⏹ 播放结束"
                        PlayState.ERROR -> "❌ 播放错误"
                        else -> "..."
                    }
                },
                onPlayTimeChanged = { curTime, _ ->
                    playTimeMs = curTime
                },
            )
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 控制栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { isTop = !isTop; moveCount++ },
                modifier = Modifier.semantics { testTag = "demo6_btn_move" },
            ) {
                Text(if (isTop) "移到下方" else "移到上方")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "移动次数: $moveCount",
                    fontSize = 12.sp,
                    color = Color(0xFF555555),
                    modifier = Modifier.semantics { testTag = "demo6_move_count_text" },
                )
                Text(
                    "播放进度: ${playTimeMs / 1000}s",
                    fontSize = 12.sp,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.semantics { testTag = "demo6_play_time_text" },
                )
                Text(
                    statusText,
                    fontSize = 12.sp,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.semantics { testTag = "demo6_status_text" },
                )
            }
        }

        // 上方容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8EAF6), RoundedCornerShape(8.dp))
                .padding(4.dp),
        ) {
            if (isTop) {
                movableVideo()
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("上方容器（空）", color = Color(0xFF9FA8DA), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下方容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFCE4EC), RoundedCornerShape(8.dp))
                .padding(4.dp),
        ) {
            if (!isTop) {
                movableVideo()
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("下方容器（空）", color = Color(0xFFF48FB1), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "move 后进度继续递增 = native player 未重建 ✓",
            fontSize = 11.sp,
            color = Color(0xFF999999),
        )
    }
}

/**
 * Demo7: LazyColumn item 内含 movableContent
 *
 * 验证：LazyColumn 回收某行 item 后，该行的 movableContent 被销毁；
 * 再次滚动回来时重建，remember 状态应该重置（因为是全新 composition），
 * 不应崩溃或出现错乱。
 */
@Composable
private fun LazyItemMovableContentDemo() {
    var showList by remember { mutableStateOf(true) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { showList = !showList }) {
                Text(if (showList) "隐藏列表" else "显示列表")
            }
        }

        Text(
            "滚动让 item 回收，再滚回来，不应崩溃",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(vertical = 4.dp),
        )

        if (showList) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(20) { index ->
                    // 每个 item 内部都用 movableContentOf 包裹计数器
                    // LazyColumn 回收时对应的 movableContent 会被销毁，
                    // 再次进入屏幕时重建——验证不会崩溃且状态正确初始化
                    val movableItem = remember(index) {
                        movableContentOf {
                            var count by remember { mutableStateOf(0) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "Item #$index",
                                    fontSize = 14.sp,
                                    color = Color(0xFF333333),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(onClick = { count++ }, modifier = Modifier.size(36.dp, 28.dp)) {
                                        Text("+", fontSize = 12.sp)
                                    }
                                    Text(
                                        "$count",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0),
                                    )
                                }
                            }
                        }
                    }
                    movableItem()
                }
            }
        }
    }
}

/**
 * Demo8: 快速连续 move（竞态压测）
 *
 * 验证：用户快速点击移动按钮 10+ 次时，Compose runtime 多帧叠加，
 * 不应出现 parentRef 不一致、崩溃或 view 消失等问题。
 */
@Composable
private fun RapidMoveDemo() {
    var isLeft by remember { mutableStateOf(true) }
    var moveCount by remember { mutableIntStateOf(0) }

    val movableContent = remember {
        movableContentOf {
            var innerCount by remember { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .size(120.dp, 80.dp)
                    .background(Color(0xFF7C4DFF), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("内部计数: $innerCount", color = Color.White, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { innerCount++ },
                    modifier = Modifier.size(80.dp, 28.dp),
                ) {
                    Text("+", fontSize = 12.sp)
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = {
                isLeft = !isLeft
                moveCount++
            }) {
                Text(if (isLeft) "→ 右" else "← 左")
            }
            // 连续快速 move 按钮
            Button(onClick = {
                // 触发 5 次状态变更（Compose 会批量处理，但要确保最终状态一致）
                repeat(5) { isLeft = !isLeft }
                moveCount += 5
            }) {
                Text("快速×5")
            }
        }

        Text(
            "已移动 $moveCount 次，内部计数应保持",
            fontSize = 12.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(vertical = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFE8EAF6), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (isLeft) {
                    movableContent()
                } else {
                    Text("左（空）", color = Color(0xFF9FA8DA), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFFCE4EC), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!isLeft) {
                    movableContent()
                } else {
                    Text("右（空）", color = Color(0xFFF48FB1), fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Demo9: 条件渲染 —— movableContent 的保留 vs 重置边界
 *
 * movableContentOf 的语义：
 *   ✅ A → B（可见且在同一帧内换 slot）：状态保留
 *   ❌ 完全不在任何 slot（visible=false）→ Runtime 调 onRelease 销毁 → 重新出现时状态重置
 *
 * 验证：
 *   1. 在 A/B 间 move（始终可见）→ 计数保留，不崩溃
 *   2. 隐藏再显示 → 计数归零（Runtime 销毁重建，这是正确行为）
 */
@Composable
private fun ConditionalMovableDemo() {
    var visible by remember { mutableStateOf(true) }
    var inBoxA by remember { mutableStateOf(true) }

    val movableContent = remember {
        movableContentOf {
            var count by remember { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("条件内容", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                Spacer(modifier = Modifier.height(4.dp))
                Text("计数: $count", fontSize = 16.sp, color = Color(0xFF00897B))
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = { count++ }) { Text("点击+1") }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }
            Button(
                onClick = { inBoxA = !inBoxA },
                enabled = visible,
            ) {
                Text(if (inBoxA) "移到 B ✅保留" else "移到 A ✅保留")
            }
        }

        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                "✅ 可见时 A↔B 移动：计数保留（movableContent 语义）",
                fontSize = 11.sp,
                color = Color(0xFF388E3C),
            )
            Text(
                "⚠️ 隐藏再显示：计数归零（无 slot 持有 → Runtime 销毁重建，符合预期）",
                fontSize = 11.sp,
                color = Color(0xFFE65100),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (visible && inBoxA) {
                    movableContent()
                } else {
                    Text("A（空）", color = Color(0xFFA5D6A7), fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(6.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (visible && !inBoxA) {
                    movableContent()
                } else {
                    Text("B（空）", color = Color(0xFFFFCC80), fontSize = 12.sp)
                }
            }
        }
    }
}
