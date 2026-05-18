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

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.*
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.compose.setContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.material3.pullToRefreshItem
import com.tencent.kuikly.compose.material3.rememberPullToRefreshState

@Page("PullToRefreshDemo")
class PullToRefreshDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            PullToRefreshExample()
        }
    }

    @Composable
    fun PullToRefreshExample() {
        var isRefreshing by remember { mutableStateOf(false) }
        var itemCount by remember { mutableStateOf(20) }
        val pullToRefreshState = rememberPullToRefreshState(isRefreshing)
        val lazyListState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(40.dp))
            
            // 标题和控制按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    "Pull-to-Refresh Demo",
                    fontSize = 20.sp,
                    color = Color.Black
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    "下拉列表可以触发刷新 - 包含默认和自定义指示器",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(Modifier.height(16.dp))
                
                // 手动刷新按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp, 40.dp)
                            .background(Color.Blue)
                            .clickable {
                                scope.launch {
                                    isRefreshing = true
                                    delay(2000) // 模拟网络请求
                                    itemCount += 5
                                    isRefreshing = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("手动刷新", color = Color.White)
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp, 40.dp)
                            .background(Color.Red)
                            .clickable {
                                itemCount = 20
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("重置数据", color = Color.White)
                    }
                }
            }
            
            // 演示区域
            Column(modifier = Modifier.fillMaxSize()) {
                // 默认指示器演示
                Text(
                    "1. 默认指示器演示",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(2.dp, Color.Black)
                        .background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 使用默认指示器
                    pullToRefreshItem(
                        state = pullToRefreshState,
                        onRefresh = {
                            scope.launch {
                                isRefreshing = true
                                delay(2000)
                                itemCount += 3
                                isRefreshing = false
                            }
                        },
                        scrollState = lazyListState
                    )
                    
                    // 少量列表项用于演示
                    items(5) { index ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(Color.LightGray)
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("默认 Item ${index + 1}", fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 自定义指示器演示
                Text(
                    "2. 自定义指示器演示",
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                val customLazyListState = rememberLazyListState()
                var customIsRefreshing by remember { mutableStateOf(false) }
                var customItemCount by remember { mutableStateOf(15) }
                val customPullToRefreshState = rememberPullToRefreshState(customIsRefreshing)
                
                LazyColumn(
                    state = customLazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(2.dp, Color.Black)
                        .background(Color.White),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 使用自定义指示器
                    pullToRefreshItem(
                        state = customPullToRefreshState,
                        onRefresh = {
                            scope.launch {
                                customIsRefreshing = true
                                delay(3000) // 稍长的刷新时间
                                customItemCount += 8
                                customIsRefreshing = false
                            }
                        },
                        scrollState = customLazyListState,
                        content = { progress, refreshing, threshold ->
                            CustomRefreshIndicator(progress, refreshing, threshold)
                        }
                    )
                    
                    // 列表项
                    items(customItemCount) { index ->
                        if (index == 1) {
                            LazyColumn(modifier = Modifier.height(100.dp).fillMaxSize().bouncesEnable(false)
                                .nestedScroll(scrollUp = NestedScrollMode.SELF_FIRST
                                    , scrollDown = NestedScrollMode.SELF_FIRST))
                            {
                                items(50) {
                                    Text("123")
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        when (index % 3) {
                                            0 -> Color.Cyan.copy(alpha = 0.3f)
                                            1 -> Color.Magenta.copy(alpha = 0.3f)
                                            else -> Color.Yellow.copy(alpha = 0.3f)
                                        }
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    "自定义 Item ${index + 1}",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    
                    // 底部提示
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "自定义演示 Total: $customItemCount items",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun CustomRefreshIndicator(
        pullProgress: Float,
        isRefreshing: Boolean,
        refreshThreshold: Dp
    ) {
        // 根据状态计算背景色和效果
        val backgroundColor = when {
            isRefreshing -> Color.Blue.copy(alpha = 0.2f)
            pullProgress >= 1f -> Color.Green.copy(alpha = 0.15f)
            pullProgress > 0.5f -> Color.Cyan.copy(alpha = 0.1f)
            pullProgress > 0f -> Color.Gray.copy(alpha = 0.05f)
            else -> Color.Transparent
        }
        
        val borderColor = when {
            isRefreshing -> Color.Blue
            pullProgress >= 1f -> Color.Green
            pullProgress > 0.5f -> Color.Cyan
            else -> Color.Gray.copy(alpha = 0.3f)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(refreshThreshold)
                .background(backgroundColor)
                .padding(1.dp)
                .background(Color.White)
                .padding(1.dp)
                .background(borderColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Loading...",
                        fontSize = 18.sp,
                        color = Color.Blue,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "请稍候",
                        fontSize = 12.sp,
                        color = Color.Blue.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Use ASCII arrows: Compose Text on iOS may render emoji as '?'.
                    val icon = when {
                        pullProgress >= 1f -> "↑"
                        pullProgress > 0.5f -> "↑"
                        else -> "↓"
                    }

                    val text = when {
                        pullProgress >= 1f -> "松开立即刷新"
                        pullProgress > 0.5f -> "继续下拉"
                        else -> "下拉刷新数据"
                    }

                    val textColor = when {
                        pullProgress >= 1f -> Color.Green
                        pullProgress > 0.5f -> Color.Cyan
                        else -> Color.Gray
                    }

                    Text(
                        text = icon,
                        fontSize = 24.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = text,
                        fontSize = 14.sp,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    
                    if (pullProgress > 0f) {
                        Text(
                            text = "${(pullProgress * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
} 