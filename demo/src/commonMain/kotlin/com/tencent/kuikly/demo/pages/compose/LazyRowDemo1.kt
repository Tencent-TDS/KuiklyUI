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
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyRowDemo1")
class LazyRowDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyRowTest1()
            }
        }
    }

    @Composable
    fun LazyRowTest1() {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 基本使用
            Text("1. 基本 LazyRow:")
            LazyRow(
                modifier =
                    Modifier
                        .height(50.dp)
                        .background(Color.LightGray),
            ) {
                items(20) { index ->
                    var count by remember { mutableStateOf(0) }
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Blue)
                                .padding(4.dp)
                                .clickable {
                                    count += 1
                                },
                    ) {
                        Text(
                            "Item $index $count",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Spacer(Modifier.height(20.dp))

            // 3. 测试 reverseLayout
            Text("3. reverseLayout 测试:")
            LazyRow(
                modifier =
                    Modifier
                        .height(50.dp)
                        .background(Color.LightGray),
            ) {
                items(5) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Red)
                                .padding(4.dp),
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. 测试 horizontalArrangement
            Text("4. horizontalArrangement 测试:")
            LazyRow(
                modifier =
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items(3) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Yellow)
                                .padding(4.dp),
                    ) {
                        Text("Item $index")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. 测试 verticalAlignment
            Text("5. verticalAlignment 测试:")
            LazyRow(
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.LightGray),
                verticalAlignment = Alignment.Bottom,
            ) {
                items(5) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Magenta)
                                .padding(4.dp),
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 7. 不同方向的 contentPadding
            Text("7. 四个方向的 contentPadding:")
            LazyRow(
                modifier =
                    Modifier
                        .height(50.dp)
                        .background(Color.LightGray),
                contentPadding =
                    PaddingValues(
                        start = 40.dp,
                        end = 20.dp,
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
            ) {
                items(5) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Green)
                                .padding(4.dp),
                    ) {
                        Text("Item $index")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

//            // 8. SpaceBetween 布局
//            Text("8. SpaceBetween 布局:")
//            LazyRow(
//                modifier = Modifier
//                    .height(50.dp)
//                    .fillMaxWidth()
//                    .background(Color.LightGray),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                items(3) { index ->
//                    Box(
//                        modifier = Modifier
//                            .size(80.dp)
//                            .background(Color.Yellow)
//                            .padding(4.dp)
//                    ) {
//                        Text("Item $index")
//                    }
//                }
//            }

            Spacer(Modifier.height(20.dp))

            // 9. 禁用滚动
            var scrollEnable by remember { mutableStateOf(true) }
            Box(
                modifier =
                    Modifier.clickable {
                        scrollEnable = !scrollEnable
                    },
            ) {
                Text("9. 禁用滚动 (userScrollEnabled = $scrollEnable):")
            }
            LazyRow(
                modifier =
                    Modifier
                        .height(50.dp)
                        .background(Color.LightGray),
                userScrollEnabled = scrollEnable,
            ) {
                items(20) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Cyan)
                                .padding(4.dp),
                    ) {
                        Text("Item $index", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 10. LazyListState 详细信息
            Text("10. LazyListState 详细信息:")
            val state2 = rememberLazyListState()
            Text(
                "首个可见项索引: ${state2.firstVisibleItemIndex}\n" +
                    "首个可见项滚动偏移: ${state2.firstVisibleItemScrollOffset}\n" +
                    "布局信息: ${state2.layoutInfo.visibleItemsInfo.size} 个可见项\n" +
                    "总项数: ${state2.layoutInfo.totalItemsCount}",
            )
            LazyRow(
                state = state2,
                modifier =
                    Modifier
                        .height(50.dp)
                        .background(Color.LightGray),
            ) {
                items(20) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color.Gray)
                                .padding(4.dp),
                    ) {
                        Text("Item $index", color = Color.White)
                    }
                }
            }
        }
    }
}
