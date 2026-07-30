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
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.itemsIndexed
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyRowDemo2")
class LazyRowDemo2 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyRowTest2()
            }
        }
    }

    @Composable
    fun LazyRowTest2() {
        Column(modifier = Modifier.fillMaxSize()) {
            ItemDemo()
            Spacer(Modifier.height(20.dp))
            ItemsDemo()
            Spacer(Modifier.height(20.dp))
            ItemsIndexedDemo()
            Spacer(Modifier.height(20.dp))
            FillParentMaxSizeDemo()
            Spacer(Modifier.height(20.dp))
            FillParentMaxWidthDemo()
            Spacer(Modifier.height(20.dp))
            FillParentMaxHeightDemo()
        }
    }

    @Composable
    private fun ItemDemo() {
        Text("1. item API 测试:")
        LazyRow(
            modifier =
                Modifier
                    .height(50.dp)
                    .background(Color.LightGray),
        ) {
            // 基本 item
            item {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color.Blue)
                            .padding(4.dp),
                ) {
                    Text("Single Item", color = Color.White)
                }
            }

            // 带 key 的 item
            item(key = "special_item") {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color.Red)
                            .padding(4.dp),
                ) {
                    Text("Keyed Item", color = Color.White)
                }
            }

            // 带 key 和 contentType 的 item
            item(
                key = "typed_item",
                contentType = "header",
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color.Green)
                            .padding(4.dp),
                ) {
                    Text("Typed Item", color = Color.White)
                }
            }
        }
    }

    @Composable
    private fun ItemsDemo() {
        Text("2. items API 测试:")
        LazyRow(
            modifier =
                Modifier
                    .height(50.dp)
                    .background(Color.LightGray),
        ) {
            // 基本 items
            items(5) { index ->
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

            // 带 key 的 items
            items(
                count = 3,
                key = { index -> "key_$index" },
            ) { index ->
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .background(Color.Magenta)
                            .padding(4.dp),
                ) {
                    Text("Keyed $index", color = Color.White)
                }
            }

            // 带 key 和 contentType 的 items
            items(
                count = 3,
                key = { index -> "typed_$index" },
                contentType = { index -> if (index % 2 == 0) "even" else "odd" },
            ) { index ->
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .background(if (index % 2 == 0) Color.Cyan else Color.Gray)
                            .padding(4.dp),
                ) {
                    Text("Typed $index")
                }
            }
        }
    }

    @Composable
    private fun ItemsIndexedDemo() {
        Text("3. itemsIndexed API 测试:")
        val colors = listOf("Red", "Green", "Blue", "Yellow", "Purple")
        LazyRow(
            modifier =
                Modifier
                    .height(50.dp)
                    .background(Color.LightGray),
        ) {
            // 基本 itemsIndexed
            itemsIndexed(colors) { index, color ->
                Box(
                    modifier =
                        Modifier
                            .padding(4.dp)
                            .size(100.dp)
                            .background(Color.Cyan),
                ) {
                    Text("$index: $color")
                }
            }

            // 带 key 的 itemsIndexed
            itemsIndexed(
                items = listOf("A", "B", "C"),
                key = { index, item -> "key_${index}_$item" },
            ) { index, item ->
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color.Blue)
                            .padding(4.dp),
                ) {
                    Text("$index: $item", color = Color.White)
                }
            }

            // 带 key 和 contentType 的 itemsIndexed
            itemsIndexed(
                items = listOf("X", "Y", "Z"),
                key = { index, item -> "typed_${index}_$item" },
                contentType = { index, _ -> if (index % 2 == 0) "even" else "odd" },
            ) { index, item ->
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(if (index % 2 == 0) Color.Green else Color.Red)
                            .padding(4.dp),
                ) {
                    Text("$index: $item", color = Color.White)
                }
            }
        }
    }

    @Composable
    private fun FillParentMaxSizeDemo() {
        Text("4. LazyItemScope.fillParentMaxSize 测试:")
        LazyRow(
            modifier =
                Modifier
                    .height(150.dp)
                    .background(Color.LightGray),
        ) {
            items(3) { index ->
                Box(
                    modifier =
                        Modifier
                            .fillParentMaxSize() // 填充 LazyRow 的整个可见区域
                            .padding(4.dp)
                            .background(Color.Blue.copy(alpha = 0.3f)),
                ) {
                    Text(
                        "Item $index\nfillParentMaxSize",
                        color = Color.White,
                    )
                }
            }
        }
    }

    @Composable
    private fun FillParentMaxWidthDemo() {
        Text("5. LazyItemScope.fillParentMaxWidth 测试:")
        LazyRow(
            modifier =
                Modifier
                    .height(150.dp)
                    .background(Color.LightGray),
        ) {
            items(3) { index ->
                Box(
                    modifier =
                        Modifier
                            .fillParentMaxWidth() // 填充 LazyRow 的宽度
                            .height(100.dp)
                            .padding(4.dp)
                            .background(Color.Green.copy(alpha = 0.3f)),
                ) {
                    Text(
                        "Item $index\nfillParentMaxWidth",
                        color = Color.Black,
                    )
                }
            }
        }
    }

    @Composable
    private fun FillParentMaxHeightDemo() {
        Text("6. LazyItemScope.fillParentMaxHeight 测试:")
        LazyRow(
            modifier =
                Modifier
                    .height(50.dp)
                    .background(Color.LightGray),
        ) {
            items(3) { index ->
                Box(
                    modifier =
                        Modifier
                            .fillParentMaxHeight() // 填充 LazyRow 的高度
                            .width(150.dp)
                            .padding(4.dp)
                            .background(Color.Magenta.copy(alpha = 0.3f)),
                ) {
                    Text(
                        "Item $index\nfillParentMaxHeight",
                        color = Color.White,
                    )
                }
            }
        }
    }
}
