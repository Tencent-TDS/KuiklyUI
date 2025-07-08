/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.*
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("BottomSheetDemo1")
class BottomSheetDemo1 : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                var currentDemo by remember { mutableStateOf("basic") }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { currentDemo = "basic" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentDemo == "basic")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("基础示例")
                        }

                        Button(
                            onClick = { currentDemo = "custom" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentDemo == "custom")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("自定义样式")
                        }

                        Button(
                            onClick = { currentDemo = "scroll" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentDemo == "scroll")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("可滚动内容")
                        }
                    }

                    when (currentDemo) {
                        "basic" -> ModalBottomSheetTest()
                        "custom" -> ModalBottomSheetWithCustomStyle()
                        "scroll" -> ModalBottomSheetWithScrollableContent()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModalBottomSheetTest() {
        var showBottomSheet by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showBottomSheet = true },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("显示底部弹窗")
            }

            ModalBottomSheet(
                visible = showBottomSheet,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp)),
                onDismissRequest = { showBottomSheet = false },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "底部弹窗内容",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "这是一个测试底部弹窗的内容区域",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showBottomSheet = false }
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModalBottomSheetWithCustomStyle() {
        var showBottomSheet by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showBottomSheet = true },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("显示自定义样式底部弹窗")
            }

            ModalBottomSheet(
                showBottomSheet,
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                tonalElevation = 8.dp,
                scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "自定义样式底部弹窗",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "这个底部弹窗使用了自定义的颜色、阴影和间距",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { showBottomSheet = false }
                        ) {
                            Text("取消")
                        }
                        OutlinedButton(
                            onClick = { showBottomSheet = false }
                        ) {
                            Text("确认")
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModalBottomSheetWithScrollableContent() {
        var showBottomSheet by remember { mutableStateOf(false) }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                onClick = { showBottomSheet = true },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("显示可滚动内容底部弹窗")
            }

            ModalBottomSheet(
                showBottomSheet,
                onDismissRequest = { showBottomSheet = false }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    items(3) { index ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "项目 ${index + 1}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "这是第 ${index + 1} 个项目的详细描述内容",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 