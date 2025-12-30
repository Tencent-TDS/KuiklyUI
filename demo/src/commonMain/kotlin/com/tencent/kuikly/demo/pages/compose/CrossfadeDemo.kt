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
import com.tencent.kuikly.compose.animation.Crossfade
import com.tencent.kuikly.compose.animation.ExperimentalAnimationApi
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("CrossfadeDemo")
class CrossfadeDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            CrossfadeDemo()
        }
    }

    @Composable
    fun CrossfadeDemo() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 基础 Crossfade 示例
            item {
                DemoCard(title = "基础 Crossfade") {
                    BasicCrossfadeExample()
                }
            }

            // 2. 自定义动画时长的 Crossfade
            item {
                DemoCard(title = "自定义动画时长") {
                    CustomDurationCrossfadeExample()
                }
            }

            // 3. 多状态切换的 Crossfade
            item {
                DemoCard(title = "多状态切换") {
                    MultiStateCrossfadeExample()
                }
            }
        }
    }

    @Composable
    fun DemoCard(
        title: String,
        content: @Composable () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                content()
            }
        }
    }

    // 1. 基础 Crossfade 示例
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun BasicCrossfadeExample() {
        var currentScreen by remember { mutableStateOf("Screen1") }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentScreen = "Screen1" }) {
                    Text("屏幕1")
                }
                Button(onClick = { currentScreen = "Screen2" }) {
                    Text("屏幕2")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(300),
                ) { screen ->
                    when (screen) {
                        "Screen1" -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF2196F3)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("屏幕 1 内容", color = Color.White, fontSize = 20.sp)
                            }
                        }
                        "Screen2" -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("屏幕 2 内容", color = Color.White, fontSize = 20.sp)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // 2. 自定义动画时长的 Crossfade
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun CustomDurationCrossfadeExample() {
        var currentTab by remember { mutableStateOf("Tab1") }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentTab = "Tab1" }) {
                    Text("标签1")
                }
                Button(onClick = { currentTab = "Tab2" }) {
                    Text("标签2")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(durationMillis = 800), // 自定义动画时长为 800ms
                ) { tab ->
                    when (tab) {
                        "Tab1" -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFE91E63)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("标签 1 内容", color = Color.White, fontSize = 18.sp)
                            }
                        }
                        "Tab2" -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFFF9800)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("标签 2 内容", color = Color.White, fontSize = 18.sp)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // 3. 多状态切换的 Crossfade
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun MultiStateCrossfadeExample() {
        var currentView by remember { mutableStateOf(1) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentView = 1 }) {
                    Text("视图1")
                }
                Button(onClick = { currentView = 2 }) {
                    Text("视图2")
                }
                Button(onClick = { currentView = 3 }) {
                    Text("视图3")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(
                    targetState = currentView,
                    animationSpec = tween(400),
                ) { view ->
                    when (view) {
                        1 -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF9C27B0)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("视图 1", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("这是第一个视图的内容", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                        2 -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF00BCD4)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("视图 2", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("这是第二个视图的内容", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                        3 -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFFF5722)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("视图 3", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("这是第三个视图的内容", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

