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

@file:OptIn(ExperimentalFoundationApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.FloatExponentialDecaySpec
import com.tencent.kuikly.compose.animation.core.generateDecayAnimationSpec
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.AnchoredDraggableState
import com.tencent.kuikly.compose.foundation.gestures.DraggableAnchors
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.anchoredDraggable
import com.tencent.kuikly.compose.foundation.gestures.animateTo
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
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("AnchoredDraggableDemo")
class AnchoredDraggableDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color.White)
                ) {
                    item {
                        BasicHorizontalDraggableDemo()
                    }
                    item {
                        VerticalDraggableDemo()
                    }
                    item {
                        MultiAnchorDraggableDemo()
                    }
                    item {
                        DynamicAnchorDraggableDemo()
                    }
                    item {
                        DraggableStateListenerDemo()
                    }
                    item {
                        CustomThresholdDraggableDemo()
                    }
                    item {
                        DraggableWithAnimationDemo()
                    }
                    item {
                        ComplexDraggableDemo()
                    }
                }
            }
        }
    }

    @Composable
    fun BasicHorizontalDraggableDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "1. 基础水平拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            // 定义锚点
            val anchors = DraggableAnchors {
                0 at 0f      // 起始位置
                1 at 200f    // 结束位置
            }

            val anchoredDraggableState = remember {
                AnchoredDraggableState(
                    initialValue = 0,
                    anchors = anchors,
                    positionalThreshold = { 50f },
                    velocityThreshold = { 500f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            // 显示当前状态
            Text(
                "当前状态: ${if (anchoredDraggableState.currentValue == 0) "关闭" else "打开"}",
                fontSize = 14.sp
            )

            // 拖拽容器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFE0E0E0))
            ) {
                // 可拖拽的滑块
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = anchoredDraggableState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(anchoredDraggableState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "拖拽",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            val coroutineScope = rememberCoroutineScope()
            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "重置",
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                anchoredDraggableState.animateTo(0)
                            }
                        },
                    color = Color.White
                )
                Text(
                    "打开",
                    modifier = Modifier
                        .background(Color.Green)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                anchoredDraggableState.animateTo(1)
                            }
                        },
                    color = Color.White
                )
            }
        }
    }

    @Composable
    fun VerticalDraggableDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "2. 垂直拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            val anchors = DraggableAnchors {
                "top" at 0f
                "middle" at 100f
                "bottom" at 200f
            }

            val verticalDraggableState = remember {
                AnchoredDraggableState(
                    initialValue = "middle",
                    anchors = anchors,
                    positionalThreshold = { 30f },
                    velocityThreshold = { 300f },
                    snapAnimationSpec = tween(400),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            Text(
                "当前位置: ${verticalDraggableState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFFF5F5F5))
            ) {
                // 垂直拖拽滑块
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, verticalDraggableState.offset.toInt()) }
                        .anchoredDraggable(
                            state = verticalDraggableState,
                            orientation = Orientation.Vertical
                        )
                        .size(60.dp)
                        .background(Color.Blue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "垂直\n拖拽",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            val coroutineScope = rememberCoroutineScope()
            // 位置控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("top", "middle", "bottom").forEach { position ->
                    Text(
                        position.replaceFirstChar { it.uppercase() },
                        modifier = Modifier
                            .background(
                                if (verticalDraggableState.currentValue == position) 
                                    Color.Green 
                                else 
                                    Color.Gray
                            )
                            .padding(8.dp)
                            .clickable {
                                coroutineScope.launch {
                                    verticalDraggableState.animateTo(position)
                                }
                            },
                        color = Color.White
                    )
                }
            }
        }
    }

    @Composable
    fun MultiAnchorDraggableDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "3. 多锚点拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            val anchors = DraggableAnchors {
                0 at 0f
                1 at 80f
                2 at 160f
                3 at 240f
                4 at 320f
            }

            val multiAnchorState = remember {
                AnchoredDraggableState(
                    initialValue = 2,
                    anchors = anchors,
                    positionalThreshold = { 40f },
                    velocityThreshold = { 400f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            Text(
                "当前锚点: ${multiAnchorState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFFE8EAF6))
            ) {
                // 多锚点滑块
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = multiAnchorState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(multiAnchorState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "多锚点",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            val coroutineScope = rememberCoroutineScope()
            // 锚点选择按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0..4).forEach { index ->
                    Text(
                        "$index",
                        modifier = Modifier
                            .background(
                                if (multiAnchorState.currentValue == index) 
                                    Color.Green 
                                else 
                                    Color.Gray
                            )
                            .padding(6.dp)
                            .clickable {
                                coroutineScope.launch {
                                    multiAnchorState.animateTo(index)
                                }
                            },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    @Composable
    fun DynamicAnchorDraggableDemo() {
        var anchorCount by remember { mutableStateOf(3) }
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "4. 动态锚点拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            // 动态创建锚点
            val dynamicAnchors = DraggableAnchors {
                repeat(anchorCount) { index ->
                    index at (index * 100f)
                }
            }

            val dynamicState = remember {
                AnchoredDraggableState(
                    initialValue = 0,
                    anchors = dynamicAnchors,
                    positionalThreshold = { 50f },
                    velocityThreshold = { 300f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            // 更新锚点
            LaunchedEffect(anchorCount) {
                dynamicState.updateAnchors(dynamicAnchors)
            }

            Text(
                "锚点数量: $anchorCount, 当前位置: ${dynamicState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFFF3E5F5))
            ) {
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = dynamicState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(dynamicState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(Color.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "动态",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // 控制锚点数量
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "减少锚点",
                    modifier = Modifier
                        .background(Color.Red)
                        .padding(8.dp)
                        .clickable {
                            if (anchorCount > 2) anchorCount--
                        },
                    color = Color.White
                )
                Text(
                    "增加锚点",
                    modifier = Modifier
                        .background(Color.Green)
                        .padding(8.dp)
                        .clickable {
                            if (anchorCount < 6) anchorCount++
                        },
                    color = Color.White
                )
            }
        }
    }

    @Composable
    fun DraggableStateListenerDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "5. 拖拽状态监听演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            val anchors = DraggableAnchors {
                "left" at 0f
                "right" at 200f
            }

            val listenerState = remember {
                AnchoredDraggableState(
                    initialValue = "left",
                    anchors = anchors,
                    positionalThreshold = { 50f },
                    velocityThreshold = { 300f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            var dragInfo by remember { mutableStateOf("") }
            var isAnimating by remember { mutableStateOf(false) }

            // 监听拖拽状态
            LaunchedEffect(listenerState) {
                snapshotFlow { 
                    Triple(
                        listenerState.currentValue,
                        listenerState.offset,
                        listenerState.isAnimationRunning
                    )
                }.collect { (currentValue, offset, isRunning) ->
                    dragInfo = "当前值: $currentValue, 偏移: ${offset.toInt()}px, 动画中: $isRunning"
                    isAnimating = isRunning
                }
            }

            Text(dragInfo, fontSize = 12.sp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFE3F2FD))
            ) {
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = listenerState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(listenerState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(
                            if (isAnimating) Color.Yellow else Color.Green
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isAnimating) "动画中" else "拖拽",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            // 显示进度
            val progress = listenerState.progress
            Text(
                "拖拽进度: ${(progress * 100).toInt()}%",
                fontSize = 14.sp
            )
        }
    }

    @Composable
    fun CustomThresholdDraggableDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "6. 自定义阈值拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            val anchors = DraggableAnchors {
                0 at 0f
                1 at 150f
                2 at 300f
            }

            val customThresholdState = remember {
                AnchoredDraggableState(
                    initialValue = 0,
                    anchors = anchors,
                    // 自定义位置阈值：总距离的25%
                    positionalThreshold = { totalDistance -> totalDistance * 0.25f },
                    // 自定义速度阈值：较高的速度要求
                    velocityThreshold = { 800f },
                    snapAnimationSpec = tween(400),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            Text(
                "当前位置: ${customThresholdState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFFFFF3E0))
            ) {
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = customThresholdState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(customThresholdState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(Color.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "自定义\n阈值",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            Text(
                "说明：需要拖拽超过25%距离或达到800px/s速度才会切换状态",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }

    @Composable
    fun DraggableWithAnimationDemo() {
        val coroutineScope = rememberCoroutineScope()
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "7. 带动画的拖拽演示",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            val anchors = DraggableAnchors {
                "start" at 0f
                "end" at 200f
            }

            val animatedState = remember {
                AnchoredDraggableState(
                    initialValue = "start",
                    anchors = anchors,
                    positionalThreshold = { 50f },
                    velocityThreshold = { 300f },
                    // 使用弹性动画
                    snapAnimationSpec = spring(
                        dampingRatio = 0.8f,
                        stiffness = 300f
                    ),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            Text(
                "状态: ${animatedState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFE8F5E8))
            ) {
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = animatedState,
                            orientation = Orientation.Horizontal
                        )
                        .offset { IntOffset(animatedState.offset.toInt(), 0) }
                        .size(60.dp)
                        .background(Color.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "弹性\n动画",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            // 动画控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "弹性动画",
                    modifier = Modifier
                        .background(Color.Green)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                animatedState.animateTo("end")
                            }
                        },
                    color = Color.White
                )
                Text(
                    "重置",
                    modifier = Modifier
                        .background(Color.Blue)
                        .padding(8.dp)
                        .clickable {
                            coroutineScope.launch {
                                animatedState.animateTo("start")
                            }
                        },
                    color = Color.White
                )
            }
        }
    }

    @Composable
    fun ComplexDraggableDemo() {
        val coroutineScope = rememberCoroutineScope()
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "8. 复杂拖拽演示（多方向）",
                fontSize = 18.sp,
                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold
            )

            // 水平拖拽
            val horizontalAnchors = DraggableAnchors {
                "left" at 0f
                "center" at 100f
                "right" at 200f
            }

            val horizontalState = remember {
                AnchoredDraggableState(
                    initialValue = "center",
                    anchors = horizontalAnchors,
                    positionalThreshold = { 30f },
                    velocityThreshold = { 400f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            // 垂直拖拽
            val verticalAnchors = DraggableAnchors {
                "top" at 0f
                "middle" at 50f
                "bottom" at 100f
            }

            val verticalState = remember {
                AnchoredDraggableState(
                    initialValue = "middle",
                    anchors = verticalAnchors,
                    positionalThreshold = { 20f },
                    velocityThreshold = { 300f },
                    snapAnimationSpec = tween(300),
                    decayAnimationSpec = FloatExponentialDecaySpec().generateDecayAnimationSpec()
                )
            }

            Text(
                "水平: ${horizontalState.currentValue}, 垂直: ${verticalState.currentValue}",
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFFCE4EC))
            ) {
                // 复合拖拽滑块
                Box(
                    modifier = Modifier
                        .anchoredDraggable(
                            state = horizontalState,
                            orientation = Orientation.Horizontal
                        )
                        .anchoredDraggable(
                            state = verticalState,
                            orientation = Orientation.Vertical
                        )
                        .offset { 
                            IntOffset(
                                horizontalState.offset.toInt(),
                                verticalState.offset.toInt()
                            ) 
                        }
                        .size(60.dp)
                        .background(Color.Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "复合\n拖拽",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }

                // 显示网格线
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(i * 66, 0) }
                            .size(1.dp, 150.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, i * 50) }
                            .size(200.dp, 1.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    )
                }
            }

            // 控制按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("left", "center", "right").forEach { pos ->
                    Text(
                        pos.replaceFirstChar { it.uppercase() },
                        modifier = Modifier
                            .background(
                                if (horizontalState.currentValue == pos) 
                                    Color.Green 
                                else 
                                    Color.Gray
                            )
                            .padding(6.dp)
                            .clickable {
                                coroutineScope.launch {
                                    horizontalState.animateTo(pos)
                                }
                            },
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("top", "middle", "bottom").forEach { pos ->
                    Text(
                        pos.replaceFirstChar { it.uppercase() },
                        modifier = Modifier
                            .background(
                                if (verticalState.currentValue == pos) 
                                    Color.Blue 
                                else 
                                    Color.Gray
                            )
                            .padding(6.dp)
                            .clickable {
                                coroutineScope.launch {
                                    verticalState.animateTo(pos)
                                }
                            },
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}