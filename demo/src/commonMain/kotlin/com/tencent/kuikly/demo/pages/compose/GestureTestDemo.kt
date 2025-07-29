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
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.combinedClickable
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.detectDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.gestures.draggable
import com.tencent.kuikly.compose.foundation.gestures.draggable2D
import com.tencent.kuikly.compose.foundation.gestures.rememberDraggable2DState
import com.tencent.kuikly.compose.foundation.gestures.rememberDraggableState
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
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
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("GestureTestDemo")
class GestureTestDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                LazyColumn (
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
//                    .verticalScroll(rememberScrollState())
                ) {

                    // 1. 点击相关测试
                    item {
                        ClickableTests()
                        Spacer(Modifier.height(20.dp))
                    }

                    item {
                        // 2. 拖动相关测试
                        DraggableTests()
                        Spacer(Modifier.height(20.dp))
                    }

                    item {


                        Spacer(modifier = Modifier.height(16.dp))
                        // 1. 综合点击事件示例 (单击、双击、长按)
                        var clickInfo by remember { mutableStateOf("等待点击...") }
                        LaunchedEffect(clickInfo) {
                            println("Debug - clickInfo changed to: $clickInfo")
                        }

                        Box(
                            modifier =
                            Modifier
                                .size(120.dp)
                                .background(Color.Blue)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            println("Touch Debug - onTap triggered at: $it")
                                            clickInfo = "单击"
                                        },
                                        onDoubleTap = {
                                            println("Touch Debug - onDoubleTap triggered at: $it")
                                            clickInfo = "双击"
                                        },
                                        onLongPress = {
                                            println("Touch Debug - onLongPress triggered at: $it")
                                            clickInfo = "长按"
                                        },
                                        onPress = {
                                            println("Touch Debug - onPress triggered at: $it")
                                            clickInfo = "按下"
                                        },
                                    )
                                },
                        ) {
                            Text(clickInfo, color = Color.White)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. 高级拖拽示例
                        var offsetX by remember { mutableStateOf(0f) }
                        var offsetY by remember { mutableStateOf(0f) }
                        var isDragging by remember { mutableStateOf(false) }

                        Box(
                            modifier =
                            Modifier
                                .size(120.dp)
                                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                                .background(if (isDragging) Color.Red.copy(alpha = 0.7f) else Color.Red)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isDragging = true },
                                        onDragEnd = { isDragging = false },
                                        onDragCancel = { isDragging = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                        },
                                    )
                                },
                        ) {
                            Text("拖拽示例", color = Color.White)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. 缩放和旋转示例
                        var scale by remember { mutableStateOf(1f) }
                        var rotation by remember { mutableStateOf(0f) }

                        Box(
                            modifier =
                            Modifier
                                .size(150.dp)
                                .background(Color.Green)
                                .scale(scale)
                                .rotate(rotation)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, rot ->
                                        scale *= zoom
                                        rotation += rot

                                        println("detectTransformGestures - zoom: $scale, rot: $rotation")
                                    }
                                },
                        ) {
                            Text("缩放和旋转", color = Color.White)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        // 4. 水平滑动示例
                        var horizontalOffset by remember { mutableStateOf(0f) }
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(Color.Yellow)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        horizontalOffset += dragAmount
                                    }
                                },
                        ) {
                            Text(
                                "水平滑动值: ${horizontalOffset.toInt()}",
                                color = Color.Black,
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 7. 长按后拖动示例
                        var longPressOffset by remember { mutableStateOf(Offset.Zero) }
                        var isLongPressed by remember { mutableStateOf(false) }

                        Box(
                            modifier =
                            Modifier
                                .size(100.dp)
                                .offset {
                                    IntOffset(
                                        longPressOffset.x.toInt(),
                                        longPressOffset.y.toInt()
                                    )
                                }
                                .background(if (isLongPressed) Color.DarkGray else Color.Gray)
                                .pointerInput(Unit) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { isLongPressed = true },
                                        onDragEnd = { isLongPressed = false },
                                        onDragCancel = { isLongPressed = false },
                                        onDrag = { change, dragAmount ->
                                            longPressOffset += Offset(dragAmount.x, dragAmount.y)
                                        },
                                    )
                                },
                        ) {
                            Text("长按后拖动", color = Color.White)
                        }
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ClickableTests() {
        Column {
            Text("1. 点击测试")
            Spacer(Modifier.height(8.dp))

            // 1.1 基础点击
            var clickCount by remember { mutableStateOf(0) }
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.LightGray)
                    .clickable { clickCount++ },
            ) {
                Text("基础点击测试 - 点击次数: $clickCount")
            }
            Spacer(Modifier.height(8.dp))

            // 1.2 组合点击（单击、双击、长按）
            var combinedClickInfo by remember { mutableStateOf("等待点击") }
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color.Blue)
                    .combinedClickable(
                        onClick = { combinedClickInfo = "单击" },
                        onDoubleClick = { combinedClickInfo = "双击" },
                        onLongClick = { combinedClickInfo = "长按" },
                    ),
            ) {
                Text("组合点击测试: $combinedClickInfo")
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DraggableTests() {
        Column {
            Text("2. 拖动测试")
            Spacer(Modifier.height(8.dp))

            // 2.1 水平拖动
            var offsetX by remember { mutableStateOf(0f) }
            val horizontalDragState =
                rememberDraggableState { delta ->
                    offsetX += delta
                }
            Row {
                Box(
                    modifier =
                    Modifier
                        .width(200.dp)
                        .height(40.dp)
                        .background(Color.Gray),
                ) {
                    Box(
                        modifier =
                        Modifier
                            .size(40.dp)
                            .offset {
                                IntOffset(
                                    x = offsetX.toInt(),
                                    y = 0,
                                )
                            }.background(Color.Red)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = horizontalDragState,
                            ),
                    )
                }
                Text("X: ${offsetX.toInt()}")
            }
            Spacer(Modifier.height(8.dp))

            // 2.2 垂直拖动
            var offsetY by remember { mutableStateOf(0f) }
            val verticalDragState =
                rememberDraggableState { delta ->
                    offsetY += delta
                }
            Box(
                modifier =
                Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .background(Color.Gray),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(40.dp)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = offsetY.toInt(),
                            )
                        }.background(Color.Blue)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = verticalDragState,
                        ),
                )
            }
            Text("Y: ${offsetY.toInt()}")
            Spacer(Modifier.height(8.dp))

            // 2.3 二维拖动
            var offset by remember { mutableStateOf(Offset.Zero) }

            // 1. 基础二维拖拽
            val dragState =
                rememberDraggable2DState { delta ->
                    offset += delta
                }

            Box(
                modifier =
                Modifier
                    .size(200.dp)
                    .background(Color.LightGray),
            ) {
                Box(
                    modifier =
                    Modifier
                        .size(50.dp)
                        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                        .background(Color.Blue)
                        .draggable2D(
                            state = dragState,
                            enabled = true,
                            interactionSource = remember { MutableInteractionSource() },
                            startDragImmediately = false,
                            onDragStarted = { startPosition ->
                                println("开始拖拽，起始位置: $startPosition")
                            },
                            onDragStopped = { velocity ->
                                println("结束拖拽，速度: $velocity")
                            },
                        ),
                ) {
                    Text(
                        "拖动我",
                        color = Color.White,
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }

            Text("位置: (${offset.x.toInt()}, ${offset.y.toInt()})")
        }
    }
}
