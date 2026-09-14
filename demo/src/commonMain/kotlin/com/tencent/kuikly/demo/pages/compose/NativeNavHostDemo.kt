/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
import com.tencent.kuikly.compose.animation.core.CubicBezierEasing
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.EnterTransition
import com.tencent.kuikly.compose.animation.ExitTransition
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.navigation.NavHost
import com.tencent.kuikly.compose.material3.navigation.NavHostController
import com.tencent.kuikly.compose.material3.navigation.composable
import com.tencent.kuikly.compose.material3.navigation.rememberNavController
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("NativeNavHostDemo")
class NativeNavHostDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("NavHost Native 位移") {
                NativeNavHostDemoContent()
            }
        }
    }
}

@Composable
private fun NativeNavHostDemoContent() {
    val navController = rememberNavController()
    var useNative by remember { mutableStateOf(true) }
    var transitionMode by remember { mutableStateOf(NavTransitionMode.Push) }
    // UIKit's view-controller transition reports a private curve (raw value 7), not the public
    // easeInOut curve. This cubic closely follows its fast initial travel and long settling tail.
    val iosSystemTransitionEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val iosEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val pushSpec = tween<IntOffset>(durationMillis = 350, easing = iosSystemTransitionEasing)
    val modalSpec = tween<IntOffset>(durationMillis = 500, easing = iosSystemTransitionEasing)
    val fadeSpec = tween<Float>(durationMillis = 220, easing = iosEaseOut)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { useNative = !useNative }) {
                Text(if (useNative) "当前：Native" else "当前：Compose")
            }
            Button(
                onClick = {
                    transitionMode = transitionMode.next()
                }
            ) {
                Text(transitionMode.label)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.fillMaxSize(),
                preferNativeSlideAnimation = useNative,
                enterTransition = {
                    when (transitionMode) {
                        NavTransitionMode.Push -> slideInHorizontally(
                            animationSpec = pushSpec,
                            initialOffsetX = { it }
                        )
                        NavTransitionMode.PushWithIncomingFade ->
                            slideInHorizontally(
                                animationSpec = pushSpec,
                                initialOffsetX = { it }
                            ) + fadeIn(fadeSpec)
                        NavTransitionMode.Modal -> slideInVertically(
                            animationSpec = modalSpec,
                            initialOffsetY = { it }
                        )
                    }
                },
                exitTransition = {
                    when (transitionMode) {
                        NavTransitionMode.Push,
                        NavTransitionMode.PushWithIncomingFade -> slideOutHorizontally(
                            animationSpec = pushSpec,
                            targetOffsetX = { -it / 3 }
                        )
                        NavTransitionMode.Modal ->
                            ExitTransition.KeepUntilTransitionsFinished
                    }
                },
                popEnterTransition = {
                    when (transitionMode) {
                        NavTransitionMode.Push -> slideInHorizontally(
                            animationSpec = pushSpec,
                            initialOffsetX = { -it / 3 }
                        )
                        NavTransitionMode.PushWithIncomingFade ->
                            slideInHorizontally(
                                animationSpec = pushSpec,
                                initialOffsetX = { -it / 3 }
                            ) + fadeIn(fadeSpec)
                        NavTransitionMode.Modal -> EnterTransition.None
                    }
                },
                popExitTransition = {
                    when (transitionMode) {
                        NavTransitionMode.Push,
                        NavTransitionMode.PushWithIncomingFade -> slideOutHorizontally(
                            animationSpec = pushSpec,
                            targetOffsetX = { it }
                        )
                        NavTransitionMode.Modal -> slideOutVertically(
                            animationSpec = modalSpec,
                            targetOffsetY = { it }
                        )
                    }
                }
            ) {
                composable("home") {
                    NativeNavScreen(
                        title = "Home",
                        color = Color(0xFF1565C0),
                        primaryAction = "Push Detail",
                        onPrimaryAction = { navController.navigate("detail") }
                    )
                }
                composable("detail") {
                    NativeNavScreen(
                        title = "Detail",
                        color = Color(0xFF00897B),
                        primaryAction = "Push Third",
                        onPrimaryAction = { navController.navigate("third") },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("third") {
                    NativeNavScreen(
                        title = "Third",
                        color = Color(0xFF6A1B9A),
                        primaryAction = "Pop",
                        onPrimaryAction = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

private enum class NavTransitionMode(val label: String) {
    Push("纯 Slide"),
    PushWithIncomingFade("Slide + 入场 Fade"),
    Modal("Modal");

    fun next(): NavTransitionMode = entries[(ordinal + 1) % entries.size]
}

@Composable
private fun NativeNavScreen(
    title: String,
    color: Color,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            onBack?.let { back ->
                Button(onClick = back) { Text("Back") }
            }
            Button(onClick = onPrimaryAction) { Text(primaryAction) }
        }
    }
}
