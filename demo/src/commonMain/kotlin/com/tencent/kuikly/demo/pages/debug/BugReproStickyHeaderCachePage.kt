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

@file:OptIn(
    com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi::class,
)

package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

private enum class ReproCase(val label: String) {
    InsertAfterIdle("A: 首帧缓存后再插顶栏"),
    InsertAfterSticky("B: 先滚到吸顶再插顶栏"),
}

/**
 * StickyHeader cache 失效策略验证页。
 */
@Page("BugReproStickyHeaderCachePage")
internal class BugReproStickyHeaderCachePage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            BugReproStickyHeaderCacheContent()
        }
    }
}

private const val TOP_INSERT_HEIGHT_DP = 120

@Composable
private fun BugReproStickyHeaderCacheContent() {
    var reproCase by remember { mutableStateOf(ReproCase.InsertAfterIdle) }
    var topBlockInserted by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(reproCase) {
        topBlockInserted = false
        listState.scrollToItem(0)
        when (reproCase) {
            ReproCase.InsertAfterIdle -> {
                delay(800)
                topBlockInserted = true
            }
            ReproCase.InsertAfterSticky -> {
                delay(800)
                listState.scrollToItem(10)
                delay(800)
                topBlockInserted = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF2F2F2))) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(12.dp),
        ) {
            Text("StickyHeader cache 验证", fontSize = 14.sp)
            Text(
                text = "用例 ${reproCase.label}",
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = if (topBlockInserted) {
                    "顶栏 ${TOP_INSERT_HEIGHT_DP}dp 已插入"
                } else {
                    "等待自动流程…"
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                ReproCase.entries.forEach { case ->
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { reproCase = case }
                            .background(
                                if (case == reproCase) Color(0xFF1976D2) else Color(0xFFBDBDBD),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = case.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(0.dp),
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (topBlockInserted) {
                item(key = "top_async_block") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TOP_INSERT_HEIGHT_DP.dp)
                            .background(Color(0xFF90CAF9)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "异步插入顶栏 ${TOP_INSERT_HEIGHT_DP}dp",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }
            }

            stickyHeader(key = "section_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF43A047)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "Section Header（key=section_header）",
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            items((1..40).toList(), key = { "row_$it" }) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(if (index % 2 == 0) Color.White else Color(0xFFEEEEEE))
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(text = "List item #$index", fontSize = 15.sp)
                }
            }
        }
    }
}