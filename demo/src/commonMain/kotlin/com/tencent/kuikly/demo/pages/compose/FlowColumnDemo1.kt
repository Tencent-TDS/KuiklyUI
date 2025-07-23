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
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowColumn
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("FlowColumnDemo1")
class FlowColumnDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                FlowColumnTest1()
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun FlowColumnTest1() {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. 基本使用
            Text("1. 基本 FlowColumn:")
            FlowColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray)
                        .padding(4.dp),
            ) {
                repeat(8) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(60.dp)
                                .padding(4.dp)
                                .background(Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. 测试 horizontalArrangement 和 verticalArrangement
            Text("2. 排列方式测试:")
            FlowColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.LightGray)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.Center,
            ) {
                repeat(5) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(if (index % 2 == 0) 50.dp else 70.dp)
                                .padding(4.dp)
                                .background(Color.Red),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. 测试 maxItemsInEachColumn
            Text("3. maxItemsInEachColumn = 3:")
            FlowColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.LightGray)
                        .padding(4.dp),
                maxItemsInEachColumn = 3,
            ) {
                repeat(7) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(30.dp)
                                .padding(4.dp)
                                .background(Color.Green),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. 测试 maxColumns
            Text("4. maxLines = 2:")
            FlowColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.LightGray)
                        .padding(4.dp),
                maxLines = 2,
            ) {
                repeat(10) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(30.dp)
                                .padding(4.dp)
                                .background(Color.Magenta),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
} 
