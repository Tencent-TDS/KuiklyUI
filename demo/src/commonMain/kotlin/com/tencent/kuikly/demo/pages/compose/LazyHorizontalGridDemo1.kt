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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyHorizontalGrid
import com.tencent.kuikly.compose.foundation.lazy.grid.rememberLazyGridState
import com.tencent.kuikly.compose.foundation.rememberScrollState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyHorizontalGridDemo1")
class LazyHorizontalGridDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyHorizontalGridTest1()
            }
        }
    }

    @Composable
    fun LazyHorizontalGridTest1() {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize(),
//                .verticalScroll(scrollState)
        ) {
            // 1. 基本使用 - Fixed行
            Text("1. 基本 LazyHorizontalGrid (Fixed 3 行):")
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier =
                    Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(20) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, 40.dp)
                                .padding(4.dp)
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
            Text("2. Adaptive行 (最小行高 50.dp):")
            LazyHorizontalGrid(
                rows = GridCells.Adaptive(50.dp),
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(15) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, 40.dp)
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

            // 3. FixedSize测试
            Text("3. FixedSize行 (每行固定 60.dp): contentPading")
            LazyHorizontalGrid(
                rows = GridCells.FixedSize(60.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
                modifier =
                    Modifier
                        .height(90.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(18) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, 50.dp)
                                .padding(4.dp)
                                .background(Color.Green),
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

            // 6. 测试水平排列方式
            Text("6. 水平排列方式测试 (SpaceEvenly):")
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items(12) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp, 40.dp)
                                .padding(4.dp)
                                .background(Color.Gray),
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

            // 7. 测试 userScrollEnabled
            var scrollEnabled by remember { mutableStateOf(true) }
            Box(
                modifier =
                    Modifier
                        .background(Color.Yellow)
                        .clickable {
                            scrollEnabled = !scrollEnabled
                        },
            ) {
                Text("7. 点击切换滚动状态 (userScrollEnabled = $scrollEnabled):")
            }
            LazyHorizontalGrid(
                rows = GridCells.Fixed(1),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
                userScrollEnabled = scrollEnabled,
            ) {
                items(20) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(100.dp, 40.dp)
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
