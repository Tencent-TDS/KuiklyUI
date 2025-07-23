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
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyColumnDemo3")
class LazyColumnDemo3 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumnTest3()
            }
        }
    }

    @Composable
    fun LazyColumnTest3() {
        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.width(40.dp))

            val state = rememberLazyListState()
            var layoutInfo by remember { mutableStateOf<LazyListLayoutInfo?>(null) }

            Column {
                // LazyColumn 示例
                Text("LazyListLayoutInfo 测试:")
                LazyColumn(
                    state = state,
                    modifier =
                        Modifier
                            .width(100.dp)
                            .height(300.dp)
                            .background(Color.LightGray),
                    contentPadding = PaddingValues(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(20) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .background(Color.Blue)
                                    .padding(4.dp),
                        ) {
                            Text(
                                "Item $index",
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(20.dp))

            // 使用 LaunchedEffect 监听并更新 layoutInfo
            LaunchedEffect(state.layoutInfo) {
                layoutInfo = state.layoutInfo
            }

            // 显示 LazyListLayoutInfo 的信息
            Column {
                layoutInfo?.let { info ->
                    Column(
                        modifier =
                            Modifier
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .padding(16.dp),
                    ) {
                        Text("1. 可见项信息:")
                        info.visibleItemsInfo.forEach { item ->
                            Text("  - 索引: ${item.index}, 偏移: ${item.offset}, 大小: ${item.size}")
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("2. 视口信息:")
                        Text("  - 起始偏移: ${info.viewportStartOffset}")
                        Text("  - 结束偏移: ${info.viewportEndOffset}")
                        Text("  - 视口大小: ${info.viewportSize}")

                        Spacer(Modifier.height(8.dp))
                        Text("3. 布局信息:")
                        Text("  - 总项数: ${info.totalItemsCount}")
                        Text("  - 方向: ${info.orientation}")
                        Text("  - 是否反向布局: ${info.reverseLayout}")

                        Spacer(Modifier.height(8.dp))
                        Text("4. 内容填充:")
                        Text("  - 前置填充: ${info.beforeContentPadding}")
                        Text("  - 后置填充: ${info.afterContentPadding}")
                        Text("  - 主轴项间距: ${info.mainAxisItemSpacing}")

                        Spacer(Modifier.height(8.dp))
                        Text("5. 滚动状态:")
                        Text("  - 是否正在滚动: ${state.isScrollInProgress}")
                        Text("  - 能否向前滚动: ${state.canScrollForward}")
                        Text("  - 能否向后滚动: ${state.canScrollBackward}")
                        Text("  - 上次是否向前滚动: ${state.lastScrolledForward}")
                        Text("  - 上次是否向后滚动: ${state.lastScrolledBackward}")
                    }
                }
            }
        }
    }
} 
