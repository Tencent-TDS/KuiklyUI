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

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TopAppBar
import com.tencent.kuikly.compose.material3.CenterAlignedTopAppBar
import com.tencent.kuikly.compose.material3.ExperimentalMaterial3Api
import com.tencent.kuikly.compose.material3.TopAppBarDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("AppBarDemo")
class AppBarDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            AppBarDemoContent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppBarDemoContent() {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
            // TopAppBar 示例
            TopAppBar(
                title = { Text("TopAppBar Example") },
                navigationIcon = {
                    Box(
                        Modifier.size(40.dp).background(Color.Blue)
                            .clickable {
                                println("TopAppBar navigationIcon clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nav", color = Color.White)
                    }
                },
                actions = {
                    Box(
                        Modifier.size(40.dp).background(Color.Red)
                            .clickable {
                                println("TopAppBar actions clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Act", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
            Spacer(Modifier.height(16.dp))
            // CenterAlignedTopAppBar 示例
            CenterAlignedTopAppBar(
                title = { Text("CenterAlignedTopAppBar") },
                navigationIcon = {
                    Box(
                        Modifier.size(40.dp).background(Color.Green)
                            .clickable {
                                println("CenterAlignedTopAppBar navigationIcon clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nav", color = Color.White)
                    }
                },
                actions = {
                    Box(
                        Modifier.size(40.dp).background(Color.Magenta)
                            .clickable {
                                println("CenterAlignedTopAppBar actions clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Act", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF388E3C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
            Spacer(Modifier.height(16.dp))
            // TopAppBar windowInsets 测试用例
            TopAppBar(
                title = { Text("With WindowInsets") },
                navigationIcon = {
                    Box(
                        Modifier.size(40.dp).background(Color.DarkGray)
                            .clickable {
                                println("WindowInsets TopAppBar navigationIcon clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nav", color = Color.White)
                    }
                },
                actions = {
                    Box(
                        Modifier.size(40.dp).background(Color.Cyan)
                            .clickable {
                                println("WindowInsets TopAppBar actions clicked")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Act", color = Color.White)
                    }
                },
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8E24AA),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
            Spacer(Modifier.height(24.dp))
            // 其他内容
            Box(
                Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("AppBar API Demo Body")
            }
        }
    }
} 