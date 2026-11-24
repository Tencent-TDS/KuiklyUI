/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.pullToRefreshItem
import com.tencent.kuikly.compose.material3.rememberPullToRefreshState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 鸿蒙 Compose 嵌套列表下拉刷新回归页。
 * 外层关闭回弹，内层支持刷新，用于比较 SELF_FIRST 和 SELF_ONLY 两种滚动模式。
 */
@Page("BugReproNestedScrollRefreshPage")
internal class BugReproNestedScrollRefreshPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            NestedScrollRefreshRepro()
        }
    }
}

@Composable
private fun NestedScrollRefreshRepro() {
    var mode by remember { mutableStateOf(NestedScrollMode.SELF_FIRST) }
    var refreshCount by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val outerListState = rememberLazyListState()
    val innerListState = rememberLazyListState()
    val innerPtrState = rememberPullToRefreshState(isRefreshing)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Text(
            text = "嵌套列表下拉刷新",
            fontSize = 16.sp,
            modifier = Modifier.padding(12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(NestedScrollMode.SELF_FIRST, NestedScrollMode.SELF_ONLY).forEach { option ->
                ModeButton(option.value, mode == option) {
                    mode = option
                }
            }
        }
        Text(
            text = "内层刷新次数：$refreshCount",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        LazyColumn(
            state = outerListState,
            modifier = Modifier.fillMaxSize().bouncesEnable(false),
        ) {
            items(20) { index ->
                if (index == 1) {
                    key(mode) {
                        LazyColumn(
                            state = innerListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(Color(0xFFFFF9C4))
                                .nestedScroll(scrollUp = mode, scrollDown = mode)
                                .bouncesEnable(true),
                        ) {
                            pullToRefreshItem(
                                state = innerPtrState,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        refreshCount++
                                        delay(1500)
                                        isRefreshing = false
                                    }
                                },
                                scrollState = innerListState,
                            )
                            items(30) { child ->
                                ListItem("内层 Item $child", 44.dp, Color(0xFFFFE082))
                            }
                        }
                    }
                } else {
                    ListItem("外层 Item $index", 56.dp, Color(0xFFECEFF1))
                }
            }
        }
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .background(if (selected) Color(0xFF2196F3) else Color(0xFFBDBDBD))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun ListItem(text: String, height: Dp, color: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().height(height).background(color).padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, fontSize = 14.sp, color = Color.Black)
    }
}
