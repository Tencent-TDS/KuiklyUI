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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.VerticalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 数据项，包含固定的 id 和显示内容
 */
data class PagerItem(
    val id: Int, // 固定 id，用于 key
    val content: String, // 显示内容
)

@Page("VerticalPagerDemo")
class VerticalPagerDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                VerticalPagerTest()
            }
        }
    }

    @Composable
    fun VerticalPagerTest() {
        // 初始数据：10条数据，id 从 1 到 10
        val initialData = remember {
            (1..10).map { index ->
                PagerItem(
                    id = index,
                    content = "数据项 $index",
                )
            }
        }

        // 使用 mutableStateOf 存储数据列表
        var dataList by remember { mutableStateOf(initialData) }

        // 创建 PagerState，页数等于数据列表长度
        // pageCount 使用 lambda，每次重组时都会重新计算，确保页数与数据列表长度一致
        val pagerState = rememberPagerState { dataList.size }
        
        // 创建协程作用域，用于调用 suspend 函数
        val scope = rememberCoroutineScope()
        
        // 标记是否需要刷新
        var refreshKey by remember { mutableStateOf(0) }

        // 当 refreshKey 变化时，先跳转到第 0 页，然后更新数据
        LaunchedEffect(refreshKey) {
            if (refreshKey > 0) {
                // 先跳转到第 0 页（使用 scrollToPage 确保等待布局完成）
                pagerState.scrollToPage(0)
                // 更新数据（此时已经在第 0 页，且没有 key 匹配，应该会保持在第 0 页）
                dataList = dataList.shuffled()
                // 等待数据更新和布局完成
                kotlinx.coroutines.delay(100)
                // 最后再次确保在第 0 页（防止恢复 key 后有任何跳转）
                kotlinx.coroutines.delay(50)
                pagerState.scrollToPage(0)
            }
        }

        HorizontalPager(modifier = Modifier.fillMaxSize(), state = rememberPagerState { 3 }) {

            if (it == 1) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSize = PageSize.Fill,
                    key = { index -> dataList[index].id },
                ) { page ->
                    val item = dataList[page]
                    val density = LocalDensity.current
                    val edgeTriggerWidthPx = with(density) { 72.dp.toPx() }
                    val speedLockDistancePx = with(density) { 120.dp.toPx() }
                    var sliderValue by remember(item.id) { mutableStateOf(0.5f) }
                    var gestureMessage by remember(item.id) { mutableStateOf("等待手势：点击 / 双击 / 中间长按 / 左右边缘长按") }
                    var thresholdReached by remember(item.id) { mutableStateOf(false) }

                    LaunchedEffect(gestureMessage) {
                        println("gestureMessage $gestureMessage")
                    }

                    WSVideoGestureOverlay(
                        key = item.id,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color(
                                    red = (item.id * 25) % 256,
                                    green = (item.id * 50) % 256,
                                    blue = (item.id * 75) % 256,
                                ),
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                        edgeTriggerWidthPx = edgeTriggerWidthPx,
                        speedLockDistancePx = speedLockDistancePx,
                        onLongTouchEdge = {
                            thresholdReached = false
                            gestureMessage = "边缘长按确认，继续向下滑可触发阈值"
                        },
                        onLongTouchCancel = {
                            thresholdReached = false
                            gestureMessage = "边缘长按结束，未达到滑动阈值"
                        },
                        onLongTouchSwipeThreshold = {
                            thresholdReached = true
                            gestureMessage = "边缘阈值已触发"
                        },
                        onLongTouchSwipeThresholdCancel = {
                            thresholdReached = false
                            gestureMessage = "边缘阈值已取消"
                        },
                        onLongTouchAndSwiped = {
                            thresholdReached = false
                            gestureMessage = "边缘长按并下滑完成"
                        },
                        onLongTouchMiddle = {
                            thresholdReached = false
                            gestureMessage = "中间区域长按"
                        },
                        onClickAnyWhere = {
                            thresholdReached = false
                            gestureMessage = "单击任意位置"
                        },
                        onDoubleClick = { xDp, yDp ->
                            thresholdReached = false
                            gestureMessage = "双击成功：x=${xDp.toInt()}dp, y=${yDp.toInt()}dp"
                        },
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = item.content,
                                fontSize = 24.sp,
                                color = Color.White,
                            )
                            Text(
                                text = "ID: ${item.id}",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                text = "页面索引: $page",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                            Text(
                                text = gestureMessage,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                            Text(
                                text = "thresholdReached=$thresholdReached",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Slider 值: ${(sliderValue * 100).toInt()}%",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(100.dp)
                                            .pointerInput(Unit) {
                                                val width = size.width.toFloat()
                                                detectTapGestures { offset ->
                                                    sliderValue = (offset.x / width).coerceIn(0f, 1f)
                                                }
                                            }
                                            .pointerInput(Unit) {
                                                val width = size.width.toFloat()
                                                detectHorizontalDragGestures(
                                                    onDragStart = { startOffset ->
                                                        sliderValue = (startOffset.x / width).coerceIn(0f, 1f)
                                                    },
                                                    onHorizontalDrag = { change, _ ->
                                                        sliderValue = (change.position.x / width).coerceIn(0f, 1f)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Slider(
                                            value = sliderValue,
                                            onValueChange = { sliderValue = it },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 右上角刷新按钮
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Text(
                        text = "刷新",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Blue)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                // 触发刷新流程：在 LaunchedEffect 中先跳转到第 0 页，然后更新数据
                                refreshKey++
                            },
                    )
                }
            } else {
                Text("empty")
            }
        }
    }
}
