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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.MutatePriority
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyHorizontalGrid
import com.tencent.kuikly.compose.foundation.lazy.grid.rememberLazyGridState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Page("LazyHorizontalGridDemo3")
class LazyHorizontalGridDemo3 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyHorizontalGridTest3()
            }
        }
    }

    @Composable
    fun LazyHorizontalGridTest3() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            item {

                val gridState = rememberLazyGridState()
                val scope = rememberCoroutineScope()

                // 添加滚动控制按钮
                Column {
                    // dispatchRawDelta 测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Blue)
                                    .clickable {
                                        gridState.dispatchRawDelta(-100f)
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("dispatchRawDelta -100", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Blue)
                                    .clickable {
                                        gridState.dispatchRawDelta(100f)
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("dispatchRawDelta +100", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // scroll 方法测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Green)
                                    .clickable {
                                        scope.launch {
                                            gridState.scroll(MutatePriority.UserInput) {
                                                scrollBy(-200f)
                                            }
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollBy -200", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Green)
                                    .clickable {
                                        scope.launch {
                                            gridState.scroll(MutatePriority.UserInput) {
                                                scrollBy(200f)
                                            }
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollBy +200", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // scrollToItem 方法测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Green)
                                    .clickable {
                                        scope.launch {
                                            gridState.scrollToItem(max(0, gridState.firstVisibleItemIndex - 3))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollToItem -3", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Green)
                                    .clickable {
                                        scope.launch {
                                            gridState.scrollToItem(min(29, gridState.firstVisibleItemIndex + 3))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollToItem +3", color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // animateScrollToItem 方法测试按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Red)
                                    .clickable {
                                        scope.launch {
                                            gridState.animateScrollToItem(max(0, gridState.firstVisibleItemIndex - 6))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("动画滚动 -6", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Red)
                                    .clickable {
                                        scope.launch {
                                            gridState.animateScrollToItem(min(29, gridState.firstVisibleItemIndex + 6))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("动画滚动 +6", color = Color.White)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // 测试用的 LazyHorizontalGrid
                Text("滚动网格以查看状态变化:")
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(3),
                    modifier =
                        Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .background(Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(30) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .size(120.dp, 50.dp)
                                    .background(
                                        when (index % 4) {
                                            0 -> Color.Blue
                                            1 -> Color.Red
                                            2 -> Color.Green
                                            else -> Color.Magenta
                                        },
                                    ).border(1.dp, Color.White)
                                    .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "项目 $index",
                                color = Color.White,
                            )
                        }
                    }
                }

                // 使用derivedStateOf监听所有状态变化
                val gridInfo by remember {
                    derivedStateOf {
                        buildString {
                            // 基本信息
                            appendLine("基本信息:")
                            appendLine("第一个可见项目索引: ${gridState.firstVisibleItemIndex}")
                            appendLine("第一个可见项目滚动偏移: ${gridState.firstVisibleItemScrollOffset}")
                            appendLine()

                            // 布局信息
                            appendLine("布局信息:")
                            with(gridState.layoutInfo) {
                                appendLine("总项目数: $totalItemsCount")
                                appendLine("可见项目数量: ${visibleItemsInfo.size}")
                                appendLine("视窗起始偏移: $viewportStartOffset")
                                appendLine("视窗结束偏移: $viewportEndOffset")
                                appendLine("内容后填充: $afterContentPadding")
//                        appendLine("每行项目数: $slotsPerLine")
                                appendLine("主轴间距: ${mainAxisItemSpacing}px")
                            }
                            appendLine()

                            // 滚动状态信息
                            appendLine("滚动状态:")
                            appendLine("是否正在滚动: ${gridState.isScrollInProgress}")
                            appendLine("可向前滚动: ${gridState.canScrollForward}")
                            appendLine("可向后滚动: ${gridState.canScrollBackward}")
                            appendLine("上次是否向前滚动: ${gridState.lastScrolledForward}")
                            appendLine("上次是否向后滚动: ${gridState.lastScrolledBackward}")
                            appendLine()

                            // 可见项目详情
                            appendLine("可见项目详情:")
                            gridState.layoutInfo.visibleItemsInfo.take(6).forEach { itemInfo ->
                                appendLine("项目 ${itemInfo.index}:")
                                appendLine("  位置: ${itemInfo.offset}")
                                appendLine("  尺寸: ${itemInfo.size}")
                                // LazyGridItemInfo特有的行/列信息
                                if (itemInfo.row != null && itemInfo.column != null) {
                                    appendLine("  所在行/列: 行=${itemInfo.row}, 列=${itemInfo.column}")
                                } else {
                                    // 如果是交错网格，则显示lane信息
                                    appendLine("  所在位置: ${itemInfo.offset}")
                                }
                            }
                            if (gridState.layoutInfo.visibleItemsInfo.size > 6) {
                                appendLine("... 还有 ${gridState.layoutInfo.visibleItemsInfo.size - 6} 个项目")
                            }
                        }
                    }
                }

                // 显示状态信息
                Text("LazyHorizontalGrid 状态信息:", modifier = Modifier.padding(top = 16.dp))
                Text(gridInfo)

                Spacer(Modifier.height(40.dp))
            }
        }
    }
} 
