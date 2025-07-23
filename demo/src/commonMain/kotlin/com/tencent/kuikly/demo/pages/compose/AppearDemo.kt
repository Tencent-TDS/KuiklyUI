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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("AppearDemo")
class AppearDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    // 测试1: 基础appearPercentage
                    item {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.LightGray)
                                .padding(8.dp),
                        ) {
                            var appearPercent by remember { mutableStateOf(0f) }
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Blue)
                                    .appearPercentage { percent ->
                                        appearPercent = percent
                                    },
                            )
                            Text(
                                text = "出现比例: ${(appearPercent * 100).toInt()}%",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White,
                                fontSize = 20.sp,
                            )
                        }
                    }

                    // 测试2: 横向滚动appearPercentage
                    item {
                        Text(
                            text = "横向滚动测试",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                        LazyRow(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color.LightGray)
                                .padding(8.dp),
                        ) {
                            items((1..10).toList()) { index ->
                                Box(
                                    modifier =
                                    Modifier
                                        .width(150.dp)
                                        .height(150.dp)
                                        .padding(horizontal = 8.dp),
                                ) {
                                    var appearPercent by remember { mutableStateOf(0f) }
                                    Box(
                                        modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF6200EE))
                                            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                            .appearPercentage { percent ->
                                                appearPercent = percent
                                            },
                                    )
                                    Text(
                                        text = "Item $index\n${(appearPercent * 100).toInt()}%",
                                        modifier = Modifier.align(Alignment.Center),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                    )
                                }
                            }
                        }
                    }

                    // 测试3: 多个元素appearPercentage
                    items((1..10).toList()) { index ->
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(vertical = 8.dp),
                        ) {
                            var appearPercent by remember { mutableStateOf(0f) }
                            Box(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color(0xFF6200EE))
                                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                                    .appearPercentage { percent ->
                                        appearPercent = percent
                                    },
                            )
                            Text(
                                text = "Item $index 出现比例: ${(appearPercent * 100).toInt()}%",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White,
                                fontSize = 16.sp,
                            )
                        }
                    }

                    // 测试4: 嵌套appearPercentage
                    item {
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.LightGray)
                                .padding(8.dp),
                        ) {
                            var parentPercent by remember { mutableStateOf(0f) }
                            var childPercent by remember { mutableStateOf(0f) }

                            Box(
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(Color.Green)
                                    .appearPercentage { percent ->
                                        parentPercent = percent
                                    },
                            ) {
                                Box(
                                    modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(Color.Red)
                                        .appearPercentage { percent ->
                                            childPercent = percent
                                        },
                                )
                            }

                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "父元素出现比例: ${(parentPercent * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = "子元素出现比例: ${(childPercent * 100).toInt()}%",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.appearPercentage(
    onPercentageChanged: (Float) -> Unit
): Modifier = this.then(
    Modifier.onGloballyPositioned { layoutCoordinates ->
        val windowBounds = layoutCoordinates.parentLayoutCoordinates?.size?.let { IntSize(it.width, it.height) }
        val layoutBounds = layoutCoordinates.boundsInRoot()
        windowBounds?.let { bounds ->
            val visibleHeight = layoutBounds.height.coerceAtMost(bounds.height.toFloat())
            val visibleWidth = layoutBounds.width.coerceAtMost(bounds.width.toFloat())
            val percentageHeight = visibleHeight / bounds.height.toFloat()
            val percentageWidth = visibleWidth / bounds.width.toFloat()
            onPercentageChanged(kotlin.math.min(percentageWidth, percentageHeight))
        }
    }
)

