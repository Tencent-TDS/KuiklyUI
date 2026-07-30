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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import com.tencent.kuikly.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import com.tencent.kuikly.compose.foundation.rememberScrollState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlin.random.Random

@Page("StaggeredHorizontalGridDemo1")
class StaggeredHorizontalGridDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                StaggeredHorizontalGridTest1()
            }
        }
    }

    @Composable
    fun StaggeredHorizontalGridTest1() {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize(),
//                .verticalScroll(scrollState)
        ) {
            // 预先生成随机宽度，以便交错网格展示效果更明显
            val randomWidths =
                remember {
                    List(30) { Random.nextInt(80, 200).dp }
                }

            // 1. 基本使用 - Fixed行
            Text("1. 基本 LazyHorizontalStaggeredGrid (Fixed 3 行):, horizontalItemSpacing: 1.dp")
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Fixed(3),
                horizontalItemSpacing = 1.dp,
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyStaggeredGridState(),
            ) {
                items(20) { index ->
                    Box(
                        modifier =
                            Modifier
                                .padding(vertical = 4.dp)
                                .width(randomWidths[index])
                                .height(30.dp)
                                .background(Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. Adaptive行测试
            Text("2. Adaptive行 (最小行高 50.dp): contentPaddign")
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Adaptive(50.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyStaggeredGridState(),
            ) {
                items(15) { index ->
                    Box(
                        modifier =
                            Modifier
                                .width(randomWidths[index])
                                .height(30.dp)
                                .padding(4.dp)
                                .background(Color.Red),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. 测试项目跨越多行
            Text("5. 项目跨越多行测试:")
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Fixed(3),
                modifier =
                    Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyStaggeredGridState(),
            ) {
                items(
                    count = 15,
                    span = { index ->
                        when {
                            index % 5 == 0 -> StaggeredGridItemSpan.FullLine
                            else -> StaggeredGridItemSpan.SingleLane
                        }
                    },
                ) { index ->
                    val span =
                        when {
                            index % 5 == 0 -> "跨越全部行"
                            else -> "单行"
                        }

                    val backgroundColor =
                        when {
                            index % 5 == 0 -> Color.Cyan
                            index % 7 == 0 -> Color.Yellow
                            else -> Color.Gray
                        }

                    Box(
                        modifier =
                            Modifier
                                .width(randomWidths[index])
                                .height(
                                    when {
                                        index % 5 == 0 -> 50.dp
                                        index % 7 == 0 -> 80.dp
                                        else -> 40.dp
                                    },
                                ).padding(4.dp)
                                .background(backgroundColor)
                                .border(1.dp, Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index\n$span",
                            color = if (index % 7 == 0) Color.Black else Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6. 测试 userScrollEnabled
            var scrollEnabled by remember { mutableStateOf(true) }
            Box(
                modifier =
                    Modifier
                        .background(Color.Yellow)
                        .clickable {
                            scrollEnabled = !scrollEnabled
                        },
            ) {
                Text("6. 点击切换滚动状态 (userScrollEnabled = $scrollEnabled):")
            }
            LazyHorizontalStaggeredGrid(
                rows = StaggeredGridCells.Fixed(2),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyStaggeredGridState(),
                userScrollEnabled = scrollEnabled,
            ) {
                items(12) { index ->
                    Box(
                        modifier =
                            Modifier
                                .width(randomWidths[index])
                                .height(40.dp)
                                .padding(4.dp)
                                .background(Color.DarkGray),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
} 
