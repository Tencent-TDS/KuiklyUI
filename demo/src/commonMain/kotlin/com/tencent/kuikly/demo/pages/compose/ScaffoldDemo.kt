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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.FloatExponentialDecaySpec
import com.tencent.kuikly.compose.animation.core.generateDecayAnimationSpec
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.gestures.AnchoredDraggableState
import com.tencent.kuikly.compose.foundation.gestures.DraggableAnchors
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.anchoredDraggable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ExperimentalMaterial3Api
import com.tencent.kuikly.compose.material3.SnackbarDuration
import com.tencent.kuikly.compose.material3.SnackbarHost
import com.tencent.kuikly.compose.material3.SnackbarHostState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TopAppBar
import com.tencent.kuikly.compose.material3.TopAppBarDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch


@Page("ScaffoldDemo")
internal class ScaffoldDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ScaffoldDemoImpl()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldDemoImpl() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // AnchoredDraggableState 示例
    val anchors = DraggableAnchors {
        0 at 0f
        1 at 300f
    }
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = 0,
            anchors = anchors,
            positionalThreshold = { 100f },
            velocityThreshold = { 1000f },
            snapAnimationSpec = tween(),
            decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("TopAppBar") },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Green)
            )
        },
        bottomBar = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(50.dp).fillMaxWidth().background(Color.Gray)) {
                Text("BottomBar")
            }
        },
        containerColor = Color.Yellow,
        contentWindowInsets = WindowInsets(0.dp),
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Hello Snackbar!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Text("弹出Snackbar")
                }
                Spacer(Modifier.height(24.dp))
                // AnchoredDraggable演示
                Box(
                    Modifier
                        .size(200.dp, 60.dp)
                        .background(Color(0xFFE0E0E0))
                ) {
                    Box(
                        Modifier
                            .anchoredDraggable(
                                state = anchoredDraggableState,
                                orientation = Orientation.Horizontal
                            )
                            .offset { IntOffset(anchoredDraggableState.offset.toInt(), 0) }
                            .size(60.dp)
                            .background(Color.Red),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Drag")
                    }
                }
            }
        }
    )
}