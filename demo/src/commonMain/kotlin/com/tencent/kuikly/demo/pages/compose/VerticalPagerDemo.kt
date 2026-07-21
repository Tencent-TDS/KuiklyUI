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
import com.tencent.kuikly.compose.extension.flingEnable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.gestures.awaitFirstDown
import com.tencent.kuikly.compose.foundation.gestures.waitForUpOrCancellation
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.VerticalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.withTimeout

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
                var longPressStatus by remember { mutableStateOf("未触发") }

                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "多指复现：左区单指滑到半页停住 → 右区另一指长按 → 依次抬指，应 snap 到整页",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF8E1))
                            .padding(12.dp),
                    )
                    Text(
                        text = "长按状态：$longPressStatus",
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            VerticalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .flingEnable(false),
                                pageSize = PageSize.Fill,
                                key = { index -> dataList[index].id },
                            ) { page ->
                                val item = dataList[page]
                                Box(
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
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                    }
                                }
                            }

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
                                        .clickable { refreshKey++ },
                                )
                            }
                        }

                        LongPressTouchConsumerOverlay(
                            onLongPress = { longPressStatus = "已触发，touch 已消费" },
                            modifier = Modifier
                                .width(88.dp)
                                .fillMaxHeight()
                                .background(Color.Black.copy(alpha = 0.28f)),
                        )
                    }
                }
            } else {
                Text("empty")
            }
        }
    }
}

/**
 * 模拟业务层长按手势：长按前不消费 touch，长按后在 pointerInput 中对 event 执行 consume。
 */
@Composable
private fun LongPressTouchConsumerOverlay(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val pointerId = down.id
                val longPressTriggered = try {
                    withTimeout(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation(down = down)
                    }
                    false
                } catch (_: PointerEventTimeoutCancellationException) {
                    true
                }
                if (!longPressTriggered) {
                    return@awaitEachGesture
                }

                down.consume()
                onLongPress()
                while (true) {
                    val event = awaitPointerEvent()
                    event.changes.forEach { change ->
                        if (change.id == pointerId) {
                            change.consume()
                        }
                    }
                    if (event.changes.none { it.id == pointerId && it.pressed }) {
                        break
                    }
                }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "长按区\n第二指\n长按",
            fontSize = 13.sp,
            color = Color.White,
        )
    }
}
