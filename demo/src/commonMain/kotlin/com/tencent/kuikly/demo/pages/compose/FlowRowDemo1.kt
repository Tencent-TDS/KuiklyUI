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
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.FlowRowOverflow
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("FlowRowDemo1")
class FlowRowDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                FlowRowTest1()
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun FlowRowTest1() {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. 测试 fillMaxRowHeight
//            Text("1. fillMaxRowHeight 测试:")
            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(4.dp)
                        .background(Color.LightGray),
            ) {
                // 普通项
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .padding(4.dp)
                            .height(40.dp)
                            .background(Color.Blue),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Normal",
                        color = Color.White,
                    )
                }

                // 填充50%行高的项
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .fillMaxRowHeight(0.5f)
                            .padding(4.dp)
                            .background(Color.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "50%",
                        color = Color.White,
                    )
                }

//                 填充100%行高的项
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .fillMaxRowHeight()
                            .padding(4.dp)
                            .background(Color.Green),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "100%",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. 测试 overflow.Clip
            var overflow by remember {
                mutableStateOf(FlowRowOverflow.Clip)
            }
            Text("2. Overflow.Clip 测试: $overflow")
            FlowRow(
                modifier =
                    Modifier
                        .width(300.dp)
                        .height(100.dp)
                        .background(Color.LightGray)
                        .padding(4.dp)
                        .clickable {
                            overflow =
                                if (overflow == FlowRowOverflow.Clip) {
                                    FlowRowOverflow.Visible
                                } else {
                                    FlowRowOverflow.Clip
                                }
                        },
                maxLines = 2,
                overflow = overflow,
            ) {
                repeat(10) { index ->
                    Box(
                        modifier =
                            Modifier
                                .size(60.dp)
                                .padding(4.dp)
                                .background(Color.Magenta),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$index",
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
} 
