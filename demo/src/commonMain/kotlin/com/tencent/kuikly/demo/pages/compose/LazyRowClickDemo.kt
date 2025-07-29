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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyRowClickDemo")
class LazyRowClickDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                SimpleClickDemo()
            }
        }
    }

    @Composable
    fun SimpleClickDemo() {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
        ) {

            Text(
                "LazyRow点击增加文字示例",
                fontSize = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            // 创建一个只有10个项目的列表，每个项目保存自己的点击次数和尺寸状态
            val items =
                remember {
                    List(10) { index ->
                        index to mutableStateOf(0)
                    }
                }

            // 用于记录每个item的尺寸
            val itemSizes =
                remember {
                    Array(10) { mutableStateOf(IntSize.Zero) }
                }

            // 简单的LazyRow实现
            LazyRow(
                modifier =
                    Modifier
                        .height(100.dp)
                        .bouncesEnable(false)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items) { (index, clickCount) ->
                    // 简单的可点击框，显示项目编号和加号
                    Box(
                        modifier =
                            Modifier
                                .clickable {
                                    // 点击时增加计数
                                    clickCount.value++
                                    // 打印点击信息
                                    println("点击了Item $index，当前点击次数: ${clickCount.value}")
                                }.height(80.dp)
                                .wrapContentWidth(unbounded = true)
                                .background(Color.LightGray)
                                .onGloballyPositioned { coordinates ->
                                    val newSize = coordinates.size
                                    if (itemSizes[index].value != newSize) {
                                        println("Item $index 尺寸变化: ${itemSizes[index].value} -> $newSize")
                                        itemSizes[index].value = newSize
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // 文本内容：项目编号 + 重复的加号
                            val text =
                                "项目$index" +
                                    if (clickCount.value > 0) {
                                        "\n" + "+".repeat(clickCount.value)
                                    } else {
                                        ""
                                    }

                            Text(
                                text = text,
                                color = Color.Black,
                            )

                            // 显示当前宽度
                            Text(
                                text = "宽度:${itemSizes[index].value.width}",
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                            )
                        }
                    }
                }
            }

            // 显示尺寸日志区域
            Spacer(Modifier.height(20.dp))
            Text("尺寸监控:", fontSize = 16.sp)

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            ) {
                items(10) { index ->
                    val size = itemSizes[index].value
                    if (size != IntSize.Zero) {
                        Text(
                            text = "Item $index: 宽度=${size.width}, 高度=${size.height}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
} 
