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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.stopScroll
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

@Page("LazyColumnDemo5")
class LazyColumnDemo5 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumnTest5()
            }
        }
    }

    @Composable
    fun LazyColumnTest5() {
        val state = rememberLazyListState()
        var triggerStop by remember { mutableStateOf(false)}
        LaunchedEffect(triggerStop) {
            // 页面销毁时停止，防止内存泄漏
            if (triggerStop) {
                delay(50)
                try {
                    state.stopScroll()
                } finally {
                    triggerStop = false
                }
            }
        }

        var animateTo30 by remember { mutableStateOf(false)}
        // 是否正在执行滚动并等待到达第30个

        LaunchedEffect(animateTo30) {
            if (animateTo30) {
                // 开始滚动动画到第50个（列表末尾）
                try {
                    state.animateScrollToItem(70)
                } finally {
                    animateTo30 = false
                }
            }
        }

        // 监听滚动位置，当第30个 item 可见时自动停止滚动（abort）
        LaunchedEffect(state.layoutInfo) {
            val visibleItems = state.layoutInfo.visibleItemsInfo
            // 检查第30个 item 是否可见
            val item30Visible = visibleItems.any { it.index == 70 }
            if (item30Visible) {
                state.stopScroll()
            }
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier
                .padding(10.dp)
                .background(Color.Green)
                .padding(30.dp).clickable {
                    triggerStop = true
                }) {
                Text("暂停Fling")
            }

            Box(modifier = Modifier
                .padding(10.dp)
                .background(Color.Green)
                .clip(RoundedCornerShape(10.dp))
                .padding(30.dp)
                .clickable {
                    animateTo30 = true
                }) {
                Text("点击animateScrollToItem滚动到第70个，触发暂停（按钮）")
            }

            Box(modifier = Modifier
                .padding(10.dp)
                .background(Color.Green)
                .padding(30.dp).clickable {

                    animateTo30 = true
                }) {
                Text("手动滑动到第30个，也可触发暂停")
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.width(40.dp))

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
                    stickyHeader {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(Color.Red)){
                            Text("header")
                        }
                    }
                    items(100) { index ->
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
