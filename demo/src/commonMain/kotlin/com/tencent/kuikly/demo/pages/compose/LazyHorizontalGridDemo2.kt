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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.GridItemSpan
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyHorizontalGrid
import com.tencent.kuikly.compose.foundation.lazy.grid.rememberLazyGridState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyHorizontalGridDemo2")
class LazyHorizontalGridDemo2 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyHorizontalGridTest2()
            }
        }
    }

    @Composable
    fun LazyHorizontalGridTest2() {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 测试 key 参数 - 使用自定义key
            Text("1. key = 'grid_item_index':")
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(
                    count = 10,
                    key = { index -> "grid_item_$index" },
                ) { index ->
                    Box(
                        modifier =
                            Modifier
                                .padding(4.dp)
                                .size(120.dp, 40.dp)
                                .background(Color.Red),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index\nKey: grid_item_$index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. 测试 span 参数 - 让某些项目跨越多行
            Text("2. 带有跨行项目的网格:")
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier =
                    Modifier
                        .height(180.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(
                    count = 15,
                    span = { index ->
                        when {
                            index % 5 == 0 -> GridItemSpan(3) // 每5个项目中第一个占据3行
                            index % 5 == 2 -> GridItemSpan(2) // 每5个项目中第三个占据2行
                            else -> GridItemSpan(1) // 其他项目正常占据1行
                        }
                    },
                ) { index ->
                    val spanSize =
                        when {
                            index % 5 == 0 -> 3
                            index % 5 == 2 -> 2
                            else -> 1
                        }

                    val backgroundColor =
                        when {
                            index % 5 == 0 -> Color.Green
                            index % 5 == 2 -> Color.Cyan
                            else -> Color.Blue
                        }

                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, (spanSize * 40).dp)
                                .padding(4.dp)
                                .background(backgroundColor)
                                .border(1.dp, Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index\n跨越 $spanSize 行",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. 测试 contentType 参数 - 为不同类型的项目设置不同的内容类型
            Text("3. 不同内容类型的项目:")
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                modifier =
                    Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                state = rememberLazyGridState(),
            ) {
                items(
                    count = 20,
                    contentType = { index ->
                        when {
                            index % 3 == 0 -> "header"
                            index % 3 == 1 -> "content"
                            else -> "footer"
                        }
                    },
                ) { index ->
                    val contentType =
                        when {
                            index % 3 == 0 -> "header"
                            index % 3 == 1 -> "content"
                            else -> "footer"
                        }

                    val backgroundColor =
                        when (contentType) {
                            "header" -> Color.Magenta
                            "content" -> Color.Yellow
                            else -> Color.Gray // footer
                        }

                    Box(
                        modifier =
                            Modifier
                                .size(120.dp, 40.dp)
                                .padding(4.dp)
                                .background(backgroundColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "项目 $index\n类型: $contentType",
                            color = if (contentType == "content") Color.Black else Color.White,
                        )
                    }
                }
            }
        }
    }
} 
