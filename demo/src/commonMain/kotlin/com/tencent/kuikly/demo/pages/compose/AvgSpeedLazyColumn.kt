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
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

@Page("AvgSpeedLazyColumn")
class AvgSpeedLazyColumn : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                AvgSpeedLazyColumnDemo()
            }
        }
    }
}

@Composable
fun AvgSpeedLazyColumnDemo() {
    // 共享状态
    var scrollSpeed by remember { mutableStateOf(100f) }
    var isScrolling by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 标题
        Text(
            "匀速滚动 LazyColumn 演示",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 控制面板
        ControlPanel(
            scrollSpeed = scrollSpeed,
            isScrolling = isScrolling,
            autoScroll = autoScroll,
            onScrollSpeedChange = { scrollSpeed = it },
            onScrollingChange = { isScrolling = it },
            onAutoScrollChange = { autoScroll = it }
        )

        // 滚动列表
        ScrollableList(
            scrollSpeed = scrollSpeed,
            isScrolling = isScrolling,
            autoScroll = autoScroll,
            onScrollingChange = { newScrollingState ->
                if (newScrollingState != isScrolling) {
                    isScrolling = newScrollingState
                }
            }
        )
    }
}

@Composable
private fun ControlPanel(
    scrollSpeed: Float,
    isScrolling: Boolean,
    autoScroll: Boolean,
    onScrollSpeedChange: (Float) -> Unit,
    onScrollingChange: (Boolean) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "控制面板",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // 滚动速度控制
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "滚动速度: ${scrollSpeed.toInt()} px/s",
                modifier = Modifier.weight(1f)
            )
            
            listOf(50f, 100f, 200f, 300f, 500f).forEach { speed ->
                Text(
                    "${speed.toInt()}",
                    modifier = Modifier
                        .background(
                            if (scrollSpeed == speed) Color.Blue else Color.Gray
                        )
                        .padding(4.dp)
                        .clickable {
                            onScrollSpeedChange(speed)
                        },
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        // 控制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (isScrolling) "停止滚动" else "开始滚动",
                modifier = Modifier
                    .background(
                        if (isScrolling) Color.Red else Color.Green
                    )
                    .padding(8.dp)
                    .clickable {
                        onScrollingChange(!isScrolling)
                    },
                color = Color.White
            )

            Text(
                if (autoScroll) "关闭自动" else "开启自动",
                modifier = Modifier
                    .background(
                        if (autoScroll) Color.Red else Color.Blue
                    )
                    .padding(8.dp)
                    .clickable {
                        onAutoScrollChange(!autoScroll)
                    },
                color = Color.White
            )
        }

        // 状态显示
        Text(
            "状态: ${if (isScrolling) "滚动中" else "已停止"} | " +
                    "自动: ${if (autoScroll) "开启" else "关闭"}",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun ScrollableList(
    scrollSpeed: Float,
    isScrolling: Boolean,
    autoScroll: Boolean,
    onScrollingChange: (Boolean) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val items = remember { generateTestItems() }
    
    // 自动滚动逻辑
    LaunchedEffect(autoScroll, scrollSpeed) {
        if (autoScroll) {
            while (autoScroll) {
                delay(16) // 约60fps
                
                // 计算每帧移动的像素数
                val pixelsPerFrame = scrollSpeed / 60f
                
                // 检查是否到达底部
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItem = visibleItems.last()
                    val isAtBottom = lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
                    
                    if (isAtBottom) {
                        // 到达底部，重新开始
                        listState.animateScrollToItem(0)
                    } else {
                        // 使用 scrollBy 进行精确的像素级滚动
                        listState.scroll {
                            scrollBy(pixelsPerFrame)
                        }
                    }
                } else {
                    // 如果没有可见项目，直接滚动
                    listState.scroll {
                        scrollBy(pixelsPerFrame)
                    }
                }
            }
        }
    }

    // 手动滚动逻辑
    LaunchedEffect(isScrolling, scrollSpeed) {
        if (isScrolling && !autoScroll) {
            while (isScrolling) {
                delay(16) // 约60fps
                
                // 计算每帧移动的像素数
                val pixelsPerFrame = scrollSpeed / 60f
                
                // 检查是否到达底部
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty()) {
                    val lastVisibleItem = visibleItems.last()
                    val isAtBottom = lastVisibleItem.index >= layoutInfo.totalItemsCount - 1
                    
                    if (isAtBottom) {
                        // 到达底部，停止滚动
                        onScrollingChange(false)
                        break
                    } else {
                        // 使用 scrollBy 进行精确的像素级滚动
                        listState.scroll {
                            scrollBy(pixelsPerFrame)
                        }
                    }
                } else {
                    // 如果没有可见项目，直接滚动
                    listState.scroll {
                        scrollBy(pixelsPerFrame)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White)
            .border(2.dp, Color.Gray)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items) { item ->
                ListItem(item = item)
            }
        }

        // 滚动指示器
        ScrollIndicator(listState = listState, totalItems = items.size)
    }
}

@Composable
private fun ListItem(item: TestItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(item.color)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 序号圆圈
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color.White)
                .border(2.dp, Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${item.id}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 内容
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // 状态指示器
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (item.isActive) Color.Green else Color.Red
                )
        )
    }
}

@Composable
private fun ScrollIndicator(listState: LazyListState, totalItems: Int) {
    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
    val progress = if (totalItems > 0) {
        firstVisibleItemIndex.toFloat() / (totalItems - 1)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 右侧滚动条
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(100.dp)
                .background(Color.Gray.copy(alpha = 0.3f))
                .align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(Color.Blue)
                    .offset(y = (progress * 80).dp) // 80 = 100 - 20
            )
        }

        // 顶部进度指示器
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.9f))
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Text(
                text = "进度: ${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "位置: ${firstVisibleItemIndex + 1}/$totalItems",
                fontSize = 10.sp,
                color = Color.DarkGray
            )
            Text(
                text = "偏移: ${firstVisibleItemScrollOffset}px",
                fontSize = 10.sp,
                color = Color.DarkGray
            )
            Text(
                text = "滚动方式: scrollBy",
                fontSize = 10.sp,
                color = Color.Blue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class TestItem(
    val id: Int,
    val title: String,
    val description: String,
    val color: Color,
    val isActive: Boolean = false
)

private fun generateTestItems(): List<TestItem> {
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFE91E63), // Pink
        Color(0xFF3F51B5), // Indigo
        Color(0xFF8BC34A), // Light Green
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF9E9E9E), // Grey
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF795548), // Brown
        Color(0xFF9E9E9E), // Grey
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF000000), // Black
        Color(0xFFE91E63), // Pink
        Color(0xFFAD1457), // Pink Dark
        Color(0xFF6A1B9A), // Purple Dark
        Color(0xFF4527A0), // Deep Purple Dark
        Color(0xFF283593), // Indigo Dark
        Color(0xFF1565C0), // Blue Dark
        Color(0xFF0277BD), // Light Blue Dark
        Color(0xFF00838F), // Cyan Dark
        Color(0xFF00695C), // Teal Dark
        Color(0xFF2E7D32), // Green Dark
        Color(0xFF558B2F), // Light Green Dark
        Color(0xFF827717), // Lime Dark
        Color(0xFFF57F17), // Yellow Dark
        Color(0xFFFF8F00), // Amber Dark
        Color(0xFFEF6C00), // Orange Dark
        Color(0xFFD84315), // Deep Orange Dark
        Color(0xFF4E342E), // Brown Dark
        Color(0xFF424242), // Grey Dark
        Color(0xFF37474F), // Blue Grey Dark
        Color(0xFF263238)  // Blue Grey Darker
    )

    return (1..50).map { index ->
        TestItem(
            id = index,
            title = "项目 #$index",
            description = "这是第 $index 个测试项目的详细描述信息。",
            color = colors[(index - 1) % colors.size],
            isActive = index % 3 == 0
        )
    }
} 