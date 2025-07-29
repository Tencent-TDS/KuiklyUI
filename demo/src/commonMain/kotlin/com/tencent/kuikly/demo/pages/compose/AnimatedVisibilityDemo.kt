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
import com.tencent.kuikly.compose.animation.ExperimentalAnimationApi
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.VisibilityThreshold
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.scaleIn
import com.tencent.kuikly.compose.animation.scaleOut
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.animation.with
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
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("AnimatedVisibilityDemo")
class AnimatedVisibilityDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                AnimationDemoScreen()
            }
        }
    }

    @Composable
    fun AnimationDemoScreen() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DemoCard(title = "基础淡入淡出效果") {
                    BasicFadeDemo()
                }
            }

            item {
                DemoCard(title = "滑动进入/退出效果") {
                    SlideInOutDemo()
                }
            }

            item {
                DemoCard(title = "弹簧动画效果") {
                    SpringAnimationDemo()
                }
            }

            item {
                DemoCard(title = "内容转换动画") {
                    ContentTransformDemo()
                }
            }

            item {
                DemoCard(title = "缩放动画效果") {
                    ScaleAnimationDemo()
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

    // 1. 基础淡入淡出动画
    @Composable
    fun BasicFadeDemo() {
        var visible by remember { mutableStateOf(true) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }

            Spacer(modifier = Modifier.height(8.dp))

            com.tencent.kuikly.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("淡入淡出", color = Color.White)
                }
            }
        }
    }

    // 2. 滑动进入/退出动画
    @Composable
    fun SlideInOutDemo() {
        var visible by remember { mutableStateOf(true) }
        var slideDirection by remember { mutableStateOf(0) } // 0: 左, 1: 右, 2: 上, 3: 下

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { slideDirection = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("从左侧")
                }

                Button(
                    onClick = { slideDirection = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("从右侧")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { slideDirection = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("从顶部")
                }

                Button(
                    onClick = { slideDirection = 3 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("从底部")
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
                val enter =
                    when (slideDirection) {
                        0 -> slideInHorizontally { width -> -width }
                        1 -> slideInHorizontally { width -> width }
                        2 -> slideInVertically { height -> -height }
                        else -> slideInVertically { height -> height }
                    }

                val exit =
                    when (slideDirection) {
                        0 -> slideOutHorizontally { width -> -width }
                        1 -> slideOutHorizontally { width -> width }
                        2 -> slideOutVertically { height -> -height }
                        else -> slideOutVertically { height -> height }
                    }

                com.tencent.kuikly.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = enter + fadeIn(),
                    exit = exit + fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color(0xFF9C27B0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("滑动动画", color = Color.White)
                    }
                }
            }
        }
    }

//    // 3. 展开/收缩动画
//    @Composable
//    fun ExpandShrinkDemo() {
//        var visible by remember { mutableStateOf(true) }
//        var expandMode by remember { mutableStateOf(0) } // 0: 所有方向, 1: 水平, 2: 垂直
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { visible = !visible }) {
//                Text(if (visible) "隐藏" else "显示")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(
//                    onClick = { expandMode = 0 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("所有方向")
//                }
//
//                Button(
//                    onClick = { expandMode = 1 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("水平方向")
//                }
//
//                Button(
//                    onClick = { expandMode = 2 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("垂直方向")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
//                    .padding(8.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                val enter = when (expandMode) {
//                    0 -> expandIn()
//                    1 -> expandHorizontally()
//                    else -> expandVertically()
//                }
//
//                val exit = when (expandMode) {
//                    0 -> shrinkOut()
//                    1 -> shrinkHorizontally()
//                    else -> shrinkVertically()
//                }
//
//                com.tencent.kuikly.compose.animation.AnimatedVisibility(
//                    visible = visible,
//                    enter = enter,
//                    exit = exit
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(80.dp)
//                            .background(Color(0xFFFF9800)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("展开/收缩", color = Color.White)
//                    }
//                }
//            }
//        }
//    }

//    // 4. 组合动画效果
//    @Composable
//    fun CombinedAnimationsDemo() {
//        var visible by remember { mutableStateOf(true) }
//        var effect by remember { mutableStateOf(0) } // 0-2: 不同效果
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { visible = !visible }) {
//                Text(if (visible) "隐藏" else "显示")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(
//                    onClick = { effect = 0 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("效果1")
//                }
//
//                Button(
//                    onClick = { effect = 1 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("效果2")
//                }
//
//                Button(
//                    onClick = { effect = 2 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("效果3")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
//                    .padding(8.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                val enterExit = when (effect) {
//                    0 -> Pair(
//                        fadeIn() + expandVertically(),
//                        fadeOut() + shrinkVertically()
//                    )
//                    1 -> Pair(
//                        slideInHorizontally { width -> width } + fadeIn(),
//                        slideOutHorizontally { width -> -width } + fadeOut()
//                    )
//                    else -> Pair(
//                        slideInVertically { height -> -height } + expandHorizontally(),
//                        slideOutVertically { height -> height } + shrinkHorizontally()
//                    )
//                }
//
//                com.tencent.kuikly.compose.animation.AnimatedVisibility(
//                    visible = visible,
//                    enter = enterExit.first,
//                    exit = enterExit.second
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(80.dp)
//                            .clip(RoundedCornerShape(8.dp))
//                            .background(Color(0xFF4CAF50)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("组合动画", color = Color.White)
//                    }
//                }
//            }
//        }
//    }

//    // 5. 自定义动画时间
//    @Composable
//    fun CustomTimingDemo() {
//        var visible by remember { mutableStateOf(true) }
//        var speed by remember { mutableStateOf(1) } // 1: 慢, 2: 中, 3: 快
//
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { visible = !visible }) {
//                Text(if (visible) "隐藏" else "显示")
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(
//                    onClick = { speed = 1 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("慢速")
//                }
//
//                Button(
//                    onClick = { speed = 2 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("中速")
//                }
//
//                Button(
//                    onClick = { speed = 3 },
//                    modifier = Modifier.padding(4.dp)
//                ) {
//                    Text("快速")
//                }
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(120.dp)
//                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
//                    .padding(8.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                val duration = when (speed) {
//                    1 -> 1500
//                    2 -> 800
//                    else -> 300
//                }
//
//                com.tencent.kuikly.compose.animation.AnimatedVisibility(
//                    visible = visible,
//                    enter = fadeIn(animationSpec = tween(durationMillis = duration)) +
//                            expandVertically(animationSpec = tween(durationMillis = duration)),
//                    exit = fadeOut(animationSpec = tween(durationMillis = duration)) +
//                            shrinkVertically(animationSpec = tween(durationMillis = duration))
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(80.dp)
//                            .background(Color(0xFF3F51B5)),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text("自定义时间", color = Color.White)
//                    }
//                }
//            }
//        }
//    }

    // 6. 弹簧动画效果
    @Composable
    fun SpringAnimationDemo() {
        var visible by remember { mutableStateOf(true) }
        var springType by remember { mutableStateOf(0) } // 0: 低弹性, 1: 中弹性, 2: 高弹性

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { springType = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("低弹性")
                }

                Button(
                    onClick = { springType = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("中弹性")
                }

                Button(
                    onClick = { springType = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("高弹性")
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
                val springSpec =
                    when (springType) {
                        0 ->
                            spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessLow,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            )
                        1 ->
                            spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            )
                        else ->
                            spring<IntOffset>(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessHigh,
                                visibilityThreshold = IntOffset.VisibilityThreshold,
                            )
                    }

                com.tencent.kuikly.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(animationSpec = springSpec) { height -> height },
                    exit = slideOutVertically(animationSpec = springSpec) { height -> height },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color(0xFFE91E63)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("弹簧动画", color = Color.White)
                    }
                }
            }
        }
    }

    // 7. 内容转换动画
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun ContentTransformDemo() {
        var current by remember { mutableStateOf(1) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { current = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("形状1")
                }

                Button(
                    onClick = { current = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("形状2")
                }

                Button(
                    onClick = { current = 3 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("形状3")
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
                com.tencent.kuikly.compose.animation.AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        fadeIn() + slideInVertically { height -> height } with
                            fadeOut() + slideOutVertically { height -> -height }
                    },
                ) { targetCount ->
                    when (targetCount) {
                        1 ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(80.dp)
                                        .background(Color(0xFF607D8B)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("正方形", color = Color.White)
                            }
                        2 ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(width = 100.dp, height = 60.dp)
                                        .background(Color(0xFFFF5722)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("矩形", color = Color.White)
                            }
                        else ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(40.dp))
                                        .background(Color(0xFF009688)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("圆形", color = Color.White)
                            }
                    }
                }
            }
        }
    }

    // 8. 缩放动画效果 (新增)
    @Composable
    fun ScaleAnimationDemo() {
        var visible by remember { mutableStateOf(true) }
        var transformOrigin by remember { mutableStateOf(0) } // 0-3: 不同的变换原点

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "隐藏" else "显示")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { transformOrigin = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("中心")
                }

                Button(
                    onClick = { transformOrigin = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("左上")
                }

                Button(
                    onClick = { transformOrigin = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("右上")
                }

                Button(
                    onClick = { transformOrigin = 3 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("左下")
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
                val origin =
                    when (transformOrigin) {
                        0 -> TransformOrigin.Center
                        1 -> TransformOrigin(0f, 0f) // 左上
                        2 -> TransformOrigin(1f, 0f) // 右上
                        else -> TransformOrigin(0f, 1f) // 左下
                    }

                com.tencent.kuikly.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter =
                        scaleIn(
                            initialScale = 0.1f,
                            transformOrigin = origin,
                        ) + fadeIn(),
                    exit =
                        scaleOut(
                            targetScale = 0.1f,
                            transformOrigin = origin,
                        ) + fadeOut(),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(Color(0xFF9C27B0)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("缩放动画", color = Color.White)
                    }
                }
            }
        }
    }
} 
