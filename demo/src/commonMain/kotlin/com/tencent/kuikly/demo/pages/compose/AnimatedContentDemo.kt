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
import com.tencent.kuikly.compose.animation.AnimatedContent
import com.tencent.kuikly.compose.animation.AnimatedContentTransitionScope
import com.tencent.kuikly.compose.animation.ContentTransform
import com.tencent.kuikly.compose.animation.ExperimentalAnimationApi
import com.tencent.kuikly.compose.animation.SizeTransform
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.expandHorizontally
import com.tencent.kuikly.compose.animation.expandIn
import com.tencent.kuikly.compose.animation.expandVertically
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.scaleIn
import com.tencent.kuikly.compose.animation.scaleOut
import com.tencent.kuikly.compose.animation.shrinkHorizontally
import com.tencent.kuikly.compose.animation.shrinkOut
import com.tencent.kuikly.compose.animation.shrinkVertically
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.animation.with
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("AnimatedContentDemo")
class AnimatedContentDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            AnimatedContentDemo()
        }
    }

    @Composable
    fun AnimatedContentDemo() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    "AnimatedContent 示例",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 8.dp, top = 40.dp),
                )
            }

            // 1. 基础用法
            item {
                DemoCard(title = "基础内容切换") {
                    BasicAnimatedContentDemo()
                }
            }

            // 2. 计数器示例
            item {
                DemoCard(title = "数字计数器") {
                    CounterAnimatedContentDemo()
                }
            }

            // 3. 自定义过渡动画
            item {
                DemoCard(title = "自定义过渡动画") {
                    CustomTransitionDemo()
                }
            }

            // 4. 滑动容器动画
            item {
                DemoCard(title = "滑动容器动画") {
                    SlideContainerDemo()
                }
            }

            // 5. 内容对齐方式
            item {
                DemoCard(title = "内容对齐方式") {
                    ContentAlignmentDemo()
                }
            }

            // 6. 大小变换控制
            item {
                DemoCard(title = "大小变换控制") {
                    SizeTransformDemo()
                }
            }

            // 7. Z轴顺序控制
            item {
                DemoCard(title = "Z轴顺序控制") {
                    ZIndexDemo()
                }
            }

            // 8. 卡片翻转效果
            item {
                DemoCard(title = "卡片翻转效果") {
                    CardFlipDemo()
                }
            }

            // 9. 内容键控制
            item {
                DemoCard(title = "内容键控制") {
                    ContentKeyDemo()
                }
            }

            // 10. 展开收缩动画
            item {
                DemoCard(title = "展开收缩动画") {
                    ExpandShrinkDemo()
                }
            }

            // 11. 展开收缩方向控制
            item {
                DemoCard(title = "展开收缩方向控制") {
                    ExpandShrinkDirectionDemo()
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

    // 1. 基础内容切换示例
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun BasicAnimatedContentDemo() {
        var currentState by remember { mutableStateOf(0) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = 0 }) {
                    Text("状态1")
                }

                Button(onClick = { currentState = 1 }) {
                    Text("状态2")
                }

                Button(onClick = { currentState = 2 }) {
                    Text("状态3")
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
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    },
                ) { state ->
                    when (state) {
                        0 ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(80.dp)
                                        .background(Color(0xFF2196F3)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("状态1", color = Color.White)
                            }
                        1 ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(100.dp, 60.dp)
                                        .background(Color(0xFFE91E63)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("状态2", color = Color.White)
                            }
                        else ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("状态3", color = Color.White)
                            }
                    }
                }
            }
        }
    }

    // 2. 计数器示例
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun CounterAnimatedContentDemo() {
        var count by remember { mutableStateOf(0) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
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
                        .fillMaxWidth()
                        .height(120.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        // 通过比较目标状态和初始状态来决定动画方向
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
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7),
                    )
                }
            }
        }
    }

    // 3. 自定义过渡动画
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun CustomTransitionDemo() {
        var currentState by remember { mutableStateOf(0) }
        var transitionType by remember { mutableStateOf(0) } // 0: 淡入淡出, 1: 缩放, 2: 旋转

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = (currentState + 1) % 3 }) {
                    Text("切换状态")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { transitionType = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("淡入淡出")
                }

                Button(
                    onClick = { transitionType = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("缩放")
                }

                Button(
                    onClick = { transitionType = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("旋转")
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
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        when (transitionType) {
                            0 ->
                                fadeIn(animationSpec = tween(600)) with
                                    fadeOut(animationSpec = tween(600))
                            1 ->
                                (fadeIn() + scaleIn(initialScale = 0.5f)) with
                                    (fadeOut() + scaleOut(targetScale = 0.5f))
                            else -> {
                                val rotation = if (targetState > initialState) 90f else -90f
                                (
                                    fadeIn() +
                                        scaleIn(
                                            initialScale = 0.8f,
                                            animationSpec = tween(500),
                                        )
                                ) with (
                                    fadeOut() +
                                        scaleOut(targetScale = 0.8f, animationSpec = tween(500))
                                )
                            }
                        }
                    },
                ) { state ->
                    val colors = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50))
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .background(colors[state])
                                .rotate(if (transitionType == 2) state * 120f else 0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("状态 ${state + 1}", color = Color.White)
                    }
                }
            }
        }
    }

    // 4. 滑动容器动画
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun SlideContainerDemo() {
        var currentState by remember { mutableStateOf(0) }
        var slideDirection by remember { mutableStateOf(0) } // 0: 左, 1: 右, 2: 上, 3: 下

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = (currentState + 1) % 3 }) {
                    Text("切换状态")
                }
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
                    Text("向左")
                }

                Button(
                    onClick = { slideDirection = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("向右")
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
                    Text("向上")
                }

                Button(
                    onClick = { slideDirection = 3 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("向下")
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
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        val direction =
                            when (slideDirection) {
                                0 -> AnimatedContentTransitionScope.SlideDirection.Left
                                1 -> AnimatedContentTransitionScope.SlideDirection.Right
                                2 -> AnimatedContentTransitionScope.SlideDirection.Up
                                else -> AnimatedContentTransitionScope.SlideDirection.Down
                            }

                        slideIntoContainer(towards = direction) with
                            slideOutOfContainer(towards = direction)
                    },
                ) { state ->
                    val sizes =
                        listOf(
                            IntSize(80, 80),
                            IntSize(100, 60),
                            IntSize(60, 100),
                        )
                    val colors = listOf(Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF4CAF50))

                    Box(
                        modifier =
                            Modifier
                                .size(sizes[state].width.dp, sizes[state].height.dp)
                                .background(colors[state]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("状态 ${state + 1}", color = Color.White)
                    }
                }
            }
        }
    }

    // 5. 内容对齐方式
    @Composable
    fun ContentAlignmentDemo() {
        var currentState by remember { mutableStateOf(0) }
        var alignmentType by remember { mutableStateOf(0) }

        val alignments =
            listOf(
                Alignment.TopStart,
                Alignment.TopCenter,
                Alignment.TopEnd,
                Alignment.CenterStart,
                Alignment.Center,
                Alignment.CenterEnd,
                Alignment.BottomStart,
                Alignment.BottomCenter,
                Alignment.BottomEnd,
            )

        val alignmentNames =
            listOf(
                "左上",
                "上",
                "右上",
                "左",
                "中",
                "右",
                "左下",
                "下",
                "右下",
            )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = (currentState + 1) % 3 }) {
                    Text("切换状态")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("选择对齐方式:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(4.dp))

            // 3x3网格的对齐方式选择器
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until 3) {
                        val index = row * 3 + col
                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (alignmentType == index) Color.Blue else Color.Gray,
                                        shape = RoundedCornerShape(4.dp),
                                    ).background(
                                        if (alignmentType == index) Color(0xFFE3F2FD) else Color.Transparent,
                                        RoundedCornerShape(4.dp),
                                    ).clickable { alignmentType = index }
                                    .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                alignmentNames[index],
                                fontSize = 12.sp,
                                color = if (alignmentType == index) Color.Blue else Color.Gray,
                            )
                        }
                    }
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
            ) {
                AnimatedContent(
                    targetState = currentState,
                    contentAlignment = alignments[alignmentType],
                ) { state ->
                    val sizes =
                        listOf(
                            IntSize(80, 80),
                            IntSize(120, 60),
                            IntSize(60, 100),
                        )
                    val colors =
                        listOf(
                            Color(0xFF2196F3),
                            Color(0xFFE91E63),
                            Color(0xFF4CAF50),
                        )

                    Box(
                        modifier =
                            Modifier
                                .size(sizes[state].width.dp, sizes[state].height.dp)
                                .background(colors[state]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("状态 ${state + 1}", color = Color.White)
                    }
                }
            }
        }
    }

    // 6. 大小变换控制
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun SizeTransformDemo() {
        var currentState by remember { mutableStateOf(0) }
        var enableSizeTransform by remember { mutableStateOf(true) }
        var clipContent by remember { mutableStateOf(true) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = (currentState + 1) % 3 }) {
                    Text("切换状态")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { enableSizeTransform = !enableSizeTransform },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(if (enableSizeTransform) "禁用大小动画" else "启用大小动画")
                }

                Button(
                    onClick = { clipContent = !clipContent },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text(if (clipContent) "禁用裁剪" else "启用裁剪")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        fadeIn() with fadeOut() using
                            if (enableSizeTransform) {
                                SizeTransform(
                                    clip = clipContent,
                                    sizeAnimationSpec = { _, _ ->
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        )
                                    },
                                )
                            } else {
                                null
                            }
                    },
                ) { state ->
                    val sizes =
                        listOf(
                            IntSize(80, 80),
                            IntSize(150, 80),
                            IntSize(80, 150),
                        )
                    val colors =
                        listOf(
                            Color(0xFF2196F3),
                            Color(0xFFE91E63),
                            Color(0xFF4CAF50),
                        )

                    Box(
                        modifier =
                            Modifier
                                .size(sizes[state].width.dp, sizes[state].height.dp)
                                .background(colors[state]),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text =
                                when (state) {
                                    0 -> "小正方形"
                                    1 -> "宽矩形"
                                    else -> "高矩形"
                                },
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }

    // 7. Z轴顺序控制
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun ZIndexDemo() {
        var currentState by remember { mutableStateOf(0) }
        var zIndexMode by remember { mutableStateOf(0) } // 0: 默认, 1: 目标在下, 2: 目标在上

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { currentState = (currentState + 1) % 3 }) {
                    Text("切换状态")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { zIndexMode = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("默认顺序")
                }

                Button(
                    onClick = { zIndexMode = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("目标在下")
                }

                Button(
                    onClick = { zIndexMode = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("目标在上")
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
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        val zIndex =
                            when (zIndexMode) {
                                0 -> 0f // 默认
                                1 -> -1f // 目标在下
                                else -> 1f // 目标在上
                            }

                        val contentTransform =
                            fadeIn(
                                animationSpec = tween(600),
                            ) with
                                fadeOut(
                                    animationSpec = tween(600),
                                )

                        contentTransform.apply {
                            // 设置目标内容的Z轴顺序
                            targetContentZIndex = zIndex
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) { state ->
                    Box(
                        modifier =
                            Modifier
                                .size(100.dp)
                                .offset(
                                    x = ((state - 1) * 30).dp,
                                    y = ((state - 1) * 30).dp,
                                ).background(
                                    when (state) {
                                        0 -> Color(0xFF2196F3)
                                        1 -> Color(0xFFE91E63)
                                        else -> Color(0xFF4CAF50)
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "状态 ${state + 1}",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }

    // 8. 卡片翻转效果
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun CardFlipDemo() {
        var isFlipped by remember { mutableStateOf(false) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { isFlipped = !isFlipped }) {
                Text(if (isFlipped) "显示正面" else "显示背面")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isFlipped,
                    transitionSpec = {
                        val rotationDirection = if (targetState) 180f else -180f
                        fadeIn() with fadeOut() using SizeTransform(clip = false)
                    },
                ) { flipped ->
                    Card(
                        modifier =
                            Modifier
                                .size(220.dp, 140.dp)
                                .scale(if (flipped) -1f else 1f, 1f),
                        // 水平翻转
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = if (flipped) Color(0xFF3F51B5) else Color(0xFFFF9800),
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (flipped) {
                                // 卡片背面
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "卡片背面",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "这里是卡片的详细信息",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            } else {
                                // 卡片正面
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "卡片正面",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "点击按钮查看详情",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 9. 内容键控制
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun ContentKeyDemo() {
        var model by remember { mutableStateOf(ContentModel("数据A", 1)) }
        var useContentKey by remember { mutableStateOf(true) }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = {
                        model =
                            model.copy(
                                // 改变内容但保持类型不变
                                content = if (model.content == "数据A") "数据B" else "数据A",
                                // 永远改变ID
                                id = model.id + 1,
                            )
                    },
                ) {
                    Text("更新数据")
                }

                Button(
                    onClick = { useContentKey = !useContentKey },
                ) {
                    Text(if (useContentKey) "使用ID作Key" else "使用整个对象作Key")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = model,
                    // 使用contentKey参数来控制是否触发动画
                    contentKey = { if (useContentKey) it.id else it },
                    transitionSpec = {
                        (slideInHorizontally { width -> width } + fadeIn()) with
                            (slideOutHorizontally { width -> -width } + fadeOut())
                    },
                ) { currentModel ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = currentModel.content,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ID: ${currentModel.id}",
                            fontSize = 16.sp,
                            color = Color(0xFF9E9E9E),
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text =
                                if (useContentKey) {
                                    "当前使用ID作为Key"
                                } else {
                                    "当前使用整个对象作为Key"
                                },
                            fontSize = 12.sp,
                            color = Color(0xFF607D8B),
                        )
                    }
                }
            }
        }
    }

    // 10. 展开收缩动画示例
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun ExpandShrinkDemo() {
        var isExpanded by remember { mutableStateOf(false) }
        var animationType by remember { mutableStateOf(0) } // 0: 全部, 1: 水平, 2: 垂直

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "收起" else "展开")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = { animationType = 0 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("全部方向")
                }

                Button(
                    onClick = { animationType = 1 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("仅水平")
                }

                Button(
                    onClick = { animationType = 2 },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Text("仅垂直")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        when (animationType) {
                            0 -> {
                                if (targetState) {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandIn(
                                                expandFrom = Alignment.Center,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkOut(
                                                shrinkTowards = Alignment.Center,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                } else {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandIn(
                                                expandFrom = Alignment.Center,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkOut(
                                                shrinkTowards = Alignment.Center,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                }
                            }
                            1 -> {
                                if (targetState) {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandHorizontally(
                                                expandFrom = Alignment.End,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkHorizontally(
                                                shrinkTowards = Alignment.End,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                } else {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandHorizontally(
                                                expandFrom = Alignment.End,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkHorizontally(
                                                shrinkTowards = Alignment.End,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                }
                            }
                            else -> {
                                if (targetState) {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                } else {
                                    ContentTransform(
                                        targetContentEnter =
                                            expandVertically(
                                                expandFrom = Alignment.Bottom,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                        initialContentExit =
                                            shrinkVertically(
                                                shrinkTowards = Alignment.Bottom,
                                                animationSpec =
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                            ),
                                    )
                                }
                            }
                        }
                    },
                ) { expanded ->
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    if (expanded) 180.dp else 80.dp,
                                    if (expanded) 180.dp else 80.dp,
                                ).background(
                                    if (expanded) Color(0xFF2196F3) else Color(0xFFE91E63),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (expanded) "展开状态" else "收起状态",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }

    // 11. 展开收缩方向控制示例
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun ExpandShrinkDirectionDemo() {
        var isExpanded by remember { mutableStateOf(false) }
        var expandFrom by remember { mutableStateOf(0) } // 0: 中心, 1: 左上, 2: 右上, 3: 左下, 4: 右下

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "收起" else "展开")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("选择展开方向:", fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(4.dp))

            // 2x2网格的展开方向选择器
            for (row in 0 until 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until 2) {
                        val index = row * 2 + col + 1
                        val alignment =
                            when (index) {
                                1 -> Alignment.TopStart
                                2 -> Alignment.TopEnd
                                3 -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            }
                        val name =
                            when (index) {
                                1 -> "左上"
                                2 -> "右上"
                                3 -> "左下"
                                else -> "右下"
                            }

                        Box(
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .border(
                                        width = 1.dp,
                                        color = if (expandFrom == index) Color.Blue else Color.Gray,
                                        shape = RoundedCornerShape(4.dp),
                                    ).background(
                                        if (expandFrom == index) Color(0xFFE3F2FD) else Color.Transparent,
                                        RoundedCornerShape(4.dp),
                                    ).clickable { expandFrom = index }
                                    .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                name,
                                fontSize = 12.sp,
                                color = if (expandFrom == index) Color.Blue else Color.Gray,
                            )
                        }
                    }
                }
            }

            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .border(
                            width = 1.dp,
                            color = if (expandFrom == 0) Color.Blue else Color.Gray,
                            shape = RoundedCornerShape(4.dp),
                        ).background(
                            if (expandFrom == 0) Color(0xFFE3F2FD) else Color.Transparent,
                            RoundedCornerShape(4.dp),
                        ).clickable { expandFrom = 0 }
                        .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "中心",
                    fontSize = 12.sp,
                    color = if (expandFrom == 0) Color.Blue else Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        val alignment =
                            when (expandFrom) {
                                0 -> Alignment.Center
                                1 -> Alignment.TopStart
                                2 -> Alignment.TopEnd
                                3 -> Alignment.BottomStart
                                else -> Alignment.BottomEnd
                            }

                        if (targetState) {
                            ContentTransform(
                                targetContentEnter =
                                    expandIn(
                                        expandFrom = alignment,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                    ),
                                initialContentExit =
                                    shrinkOut(
                                        shrinkTowards = alignment,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                    ),
                            )
                        } else {
                            ContentTransform(
                                targetContentEnter =
                                    expandIn(
                                        expandFrom = alignment,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                    ),
                                initialContentExit =
                                    shrinkOut(
                                        shrinkTowards = alignment,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                    ),
                            )
                        }
                    },
                ) { expanded ->
                    Box(
                        modifier =
                            Modifier
                                .size(
                                    if (expanded) 180.dp else 80.dp,
                                    if (expanded) 180.dp else 80.dp,
                                ).background(
                                    if (expanded) Color(0xFF2196F3) else Color(0xFFE91E63),
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (expanded) "展开状态" else "收起状态",
                            color = Color.White,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }

    // 数据模型用于ContentKeyDemo
    data class ContentModel(
        val content: String,
        val id: Int,
    )
} 
