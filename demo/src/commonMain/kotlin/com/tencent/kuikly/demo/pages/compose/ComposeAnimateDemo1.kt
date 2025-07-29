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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.Animatable
import com.tencent.kuikly.compose.animation.AnimatedContent
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.ExperimentalAnimationApi
import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.animateContentSize
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.core.FastOutSlowInEasing
import com.tencent.kuikly.compose.animation.core.RepeatMode
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.animateDpAsState
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.animateIntOffsetAsState
import com.tencent.kuikly.compose.animation.core.infiniteRepeatable
import com.tencent.kuikly.compose.animation.core.rememberInfiniteTransition
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.animation.with
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("ComposeAnimateDemo1")
class ComposeAnimateDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                AnimationDemo()
            }
        }
    }

    @Composable
    fun AnimationDemo() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // 1. AnimatedVisibility
            item {
                DemoCard(title = "AnimatedVisibility") {
                    AnimatedVisibilityDemo()
                }
            }

            // 2. animateColorAsState
            item {
                DemoCard(title = "animateColorAsState") {
                    AnimateColorDemo()
                }
            }

            // 3. animateContentSize
            item {
                DemoCard(title = "animateContentSize") {
                    AnimateContentSizeDemo()
                }
            }

            // 4. animateIntOffsetAsState
            item {
                DemoCard(title = "animateIntOffsetAsState") {
                    AnimateOffsetDemo()
                }
            }

            // 5. animateDpAsState
            item {
                DemoCard(title = "animateDpAsState") {
                    AnimateDpDemo()
                }
            }

//            // 6. rememberInfiniteTransition
//            item {
//                DemoCard(title = "rememberInfiniteTransition") {
//                    InfiniteTransitionDemo()
//                }
//            }

            // 7. AnimatedContent
            item {
                DemoCard(title = "AnimatedContent") {
                    AnimatedContentDemo()
                }
            }

            // 8. Animatable与animateTo
            item {
                DemoCard(title = "Animatable与animateTo") {
                    AnimatableDemo()
                }
            }

            // 9. LaunchedEffect + launch + animateTo 串行与并行动画示例
            item {
                DemoCard(title = "LaunchedEffect + launch + animateTo 串行与并行动画示例") {
                    LaunchedEffectAnimationDemo()
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

    // 1. AnimatedVisibility演示
    @Composable
    fun AnimatedVisibilityDemo() {
        var visible by remember { mutableStateOf(true) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { visible = !visible }) {
                Text(if (visible) "点击隐藏" else "点击显示")
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = visible,
//                enter = fadeIn() + expandIn(),
//                exit = fadeOut() + shrinkOut()
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("内容", color = Color.White)
                }
            }
        }
    }

    // 2. animateColorAsState演示
    @Composable
    fun AnimateColorDemo() {
        var colorToggle by remember { mutableStateOf(false) }
        val targetColor = if (colorToggle) Color(0xFFE91E63) else Color(0xFF4CAF50)

        val color by animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 1000),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { colorToggle = !colorToggle }) {
                Text("切换颜色")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text("颜色动画", color = Color.White)
            }
        }
    }

    // 3. animateContentSize演示
    @Composable
    fun AnimateContentSizeDemo() {
        var expanded by remember { mutableStateOf(false) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起" else "展开")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier =
                    Modifier
                        .background(Color(0xFFFF9800))
                        .animateContentSize(
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                        ).size(if (expanded) 150.dp else 80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("大小动画", color = Color.White)
            }
        }
    }

    // 4. animateIntOffsetAsState演示
    @Composable
    fun AnimateOffsetDemo() {
        var moved by remember { mutableStateOf(false) }

        val offset by animateIntOffsetAsState(
            targetValue = if (moved) IntOffset(150, 0) else IntOffset(0, 0),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { moved = !moved }) {
                Text(if (moved) "向左移动" else "向右移动")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                Box(
                    modifier =
                        Modifier
                            .offset { offset }
                            .size(80.dp)
                            .background(Color(0xFF9C27B0)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("位置动画", color = Color.White)
                }
            }
        }
    }

    // 5. animateDpAsState演示
    @Composable
    fun AnimateDpDemo() {
        var large by remember { mutableStateOf(false) }

        val size by animateDpAsState(
            targetValue = if (large) 120.dp else 60.dp,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { large = !large }) {
                Text(if (large) "变小" else "变大")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(0xFF3F51B5)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Dp动画", color = Color.White)
            }
        }
    }

    // 6. rememberInfiniteTransition演示
    @Composable
    fun InfiniteTransitionDemo() {
        // 添加动画状态控制
        var isPlaying by remember { mutableStateOf(true) }

        // 使用键值重组来控制动画状态
        // 当isPlaying变化时，会创建新的transition实例
        val infiniteTransition = rememberInfiniteTransition(label = "InfiniteDemo")

        val color by infiniteTransition.animateColor(
            initialValue = Color(0xFF2196F3),
            targetValue = Color(0xFFE91E63),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

        val size by infiniteTransition.animateFloat(
            initialValue = 60f,
            targetValue = 100f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse,
                ),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 添加控制按钮
            Button(onClick = { isPlaying = !isPlaying }) {
                Text(if (isPlaying) "暂停" else "开始")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 使用条件显示动画元素
            // 当暂停时显示静态元素，运行时显示动画元素
            if (isPlaying) {
                Box(
                    modifier =
                        Modifier
                            .size(size.dp)
                            .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("无限动画", color = Color.White)
                }
            } else {
                // 暂停状态显示静态元素
                Box(
                    modifier =
                        Modifier
                            .size(60.dp)
                            .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("已暂停", color = Color.White)
                }
            }
        }
    }

    // 7. AnimatedContent演示
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun AnimatedContentDemo() {
        var count by remember { mutableStateOf(0) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { count-- }) {
                    Text("-")
                }
                Button(onClick = { count++ }) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .background(Color(0xFF607D8B)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                        } else {
                            slideInVertically { height -> -height } + fadeIn() with
                                slideOutVertically { height -> height } + fadeOut()
                        }
                    },
                ) { targetCount ->
                    Text(
                        text = "$targetCount",
                        fontSize = 24.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }

    // 8. Animatable与animateTo演示
    @Composable
    fun AnimatableDemo() {
        val color = remember { Animatable(Color(0xFF4CAF50)) }
        val coroutineScope = rememberCoroutineScope()

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            color.animateTo(Color(0xFF2196F3), animationSpec = tween(1000))
                        }
                    },
                ) {
                    Text("蓝色")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            color.animateTo(Color(0xFFE91E63), animationSpec = tween(1000))
                        }
                    },
                ) {
                    Text("粉色")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            color.animateTo(Color(0xFF4CAF50), animationSpec = tween(1000))
                        }
                    },
                ) {
                    Text("绿色")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .background(color.value),
                contentAlignment = Alignment.Center,
            ) {
                Text("Animatable", color = Color.White)
            }
        }
    }

    // 9. LaunchedEffect + launch + animateTo 串行与并行动画示例
    @Composable
    fun LaunchedEffectAnimationDemo() {
        var isAnimating by remember { mutableStateOf(false) }
        var animationType by remember { mutableStateOf(0) } // 0: 串行, 1: 并行

        // 用于串行动画
        val offsetY = remember { Animatable(0f) }

        // 用于并行动画
        val offsetX = remember { Animatable(0f) }
        val scale = remember { Animatable(1f) }
        val rotation = remember { Animatable(0f) }

        // 协程作用域用于并行动画
        val scope = rememberCoroutineScope()

        // 动画逻辑
        LaunchedEffect(isAnimating, animationType) {
            if (isAnimating) {
                if (animationType == 0) {
                    // 串行动画 - 一个接一个执行
                    // 向下移动
                    offsetY.animateTo(
                        targetValue = 80f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                    )
                    // 暂停一下
                    kotlinx.coroutines.delay(100)
                    // 向上移动
                    offsetY.animateTo(
                        targetValue = -50f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    )
                    // 回到原位
                    offsetY.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                    )
                } else {
                    // 重置并行动画的值
                    offsetX.snapTo(0f)
                    scale.snapTo(1f)
                    rotation.snapTo(0f)

                    // 并行动画 - 同时执行多个动画
                    scope.launch {
                        offsetX.animateTo(
                            targetValue = 100f,
                            animationSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                        )
                        offsetX.animateTo(0f, tween(500))
                    }

                    scope.launch {
                        scale.animateTo(
                            targetValue = 1.5f,
                            animationSpec = tween(durationMillis = 500),
                        )
                        scale.animateTo(1f, tween(500))
                    }

                    // 旋转动画
                    rotation.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(durationMillis = 1000),
                    )
                    rotation.snapTo(0f)
                }

                isAnimating = false
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = {
                        animationType = 0
                        isAnimating = true
                    },
                ) {
                    Text("串行动画")
                }

                Button(
                    onClick = {
                        animationType = 1
                        isAnimating = true
                    },
                ) {
                    Text("并行动画")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // 动画内容
                Box(
                    modifier =
                        Modifier
                            .offset(
                                x = if (animationType == 1) offsetX.value.dp else 0.dp,
                                y = if (animationType == 0) offsetY.value.dp else 0.dp,
                            ).scale(if (animationType == 1) scale.value else 1f)
                            .size(80.dp)
                            .graphicsLayer {
                                if (animationType == 1) {
                                    rotationZ = rotation.value
                                }
                            }.background(Color(0xFF795548)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (animationType == 0) "串行动画" else "并行动画",
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
} 
