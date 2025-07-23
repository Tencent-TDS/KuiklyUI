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
import com.tencent.kuikly.compose.foundation.gestures.Orientation
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
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import com.tencent.kuikly.compose.foundation.rememberScrollState
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
import kotlin.random.Random

@Page("StaggeredHorizontalGridDemo3")
class StaggeredHorizontalGridDemo3 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                StaggeredHorizontalGridTest3()
            }
        }
    }

    @Composable
    fun StaggeredHorizontalGridTest3() {
        val scrollState = rememberScrollState()
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            item {
                Spacer(Modifier.height(40.dp))

                val gridState = rememberLazyStaggeredGridState()
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
                                            gridState.scrollToItem(max(0, gridState.firstVisibleItemIndex - 6))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollToItem -6", color = Color.White)
                        }

                        Box(
                            modifier =
                                Modifier
                                    .size(140.dp, 40.dp)
                                    .background(Color.Green)
                                    .clickable {
                                        scope.launch {
                                            gridState.scrollToItem(min(29, gridState.firstVisibleItemIndex + 6))
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("scrollToItem +6", color = Color.White)
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

                // 预先生成随机宽度，以便交错网格展示效果更明显
                val randomWidths =
                    remember {
                        List(30) { Random.nextInt(80, 200).dp }
                    }
                val randomHeights =
                    remember {
                        List(30) { if (it % 3 == 0) 60.dp else 40.dp }
                    }

                // 测试用的 LazyHorizontalStaggeredGrid
                Text("滚动交错网格以查看状态变化:")
                LazyHorizontalStaggeredGrid(
                    rows = StaggeredGridCells.Fixed(3),
                    modifier =
                        Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .background(Color.LightGray),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    state = gridState,
                    horizontalItemSpacing = 8.dp,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(30) { index ->
                        Box(
                            modifier =
                                Modifier
                                    .width(randomWidths[index])
                                    .height(randomHeights[index])
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
                                appendLine("主轴方向: ${if (orientation == Orientation.Horizontal) "水平" else "垂直"}")
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
                                appendLine("  偏移: ${itemInfo.offset} 尺寸: ${itemInfo.size}")
                                appendLine("  lane: ${itemInfo.lane}")
                                appendLine("  键(Key): ${itemInfo.key}")
                            }
                            if (gridState.layoutInfo.visibleItemsInfo.size > 6) {
                                appendLine("... 还有 ${gridState.layoutInfo.visibleItemsInfo.size - 6} 个项目")
                            }
                        }
                    }
                }

                // 显示状态信息
                Text("LazyHorizontalStaggeredGrid 状态信息:", modifier = Modifier.padding(top = 16.dp))
                Text(gridInfo)

                Spacer(Modifier.height(40.dp))
            }
        }
    }
} 
