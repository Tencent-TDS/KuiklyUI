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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.input.pointer.consumePositionChange
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog
import kotlin.math.roundToInt

@Page("GestureConflictDemo")
class GestureConflictDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
                    item {
                        Text("触摸冲突处理示例", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // 1. 嵌套滚动示例
                    item {
                        NestedScrollExample()
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // 2. 拖拽与滚动冲突
                    item {
                        DragAndScrollExample()
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        MultiHorizontalGestureExample()
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // 3. 多手势处理
                    item {
                        MultiGestureExample()
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item {
                        // 4. 手势拦截示例
                        GestureInterceptionExample()
                    }

                    item {
                        RotateAndDragExample()
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NestedScrollExample() {
    Text("1. 嵌套滚动示例 (垂直列表中包含水平列表)")
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
    ) {
        items(10) { index ->
            Column {
                Text("垂直项 $index")
                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color.LightGray),
                ) {
                    items(20) { subIndex ->
                        Box(
                            modifier =
                                Modifier
                                    .size(100.dp)
                                    .padding(4.dp)
                                    .background(Color.White)
                                    .border(1.dp, Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("项 $subIndex")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragAndScrollExample() {
    Text("2. 拖拽与滚动冲突 (可拖拽项的滚动列表)")
    Spacer(modifier = Modifier.height(8.dp))

    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
    ) {
        items(10) { index ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(8.dp)
                        .background(Color.LightGray)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                dragOffset += dragAmount
                            }
                        }.offset(x = dragOffset.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("可拖拽项 $index")
            }
        }
    }
}

@Composable
private fun MultiGestureExample() {
    Text("3. 多手势处理 (可缩放和拖拽的框)")
    Spacer(modifier = Modifier.height(8.dp))

    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .scale(scale)
                    .rotate(rotation)
                    .background(Color.Blue)
                    .pointerInput(Unit) {
                        KLog.e("tryFire", "tryFire awaitpointerInput ")
                        detectTransformGestures { _, pan, zoom, rotate ->
                            offset += pan
                            scale *= zoom
                            rotation += rotate
                        }
                    },
        )
    }
}

@Composable
private fun MultiHorizontalGestureExample() {
    Text("3. 多手势处理 (横向滚动)")
    Spacer(modifier = Modifier.height(8.dp))

    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(100.dp)
                    .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .scale(scale)
                    .background(Color.Blue)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consumePositionChange()
                            offset = Offset(offset.x + dragAmount, offset.y)
                        }
                    },
        )
    }
}

@Composable
private fun GestureInterceptionExample() {
    Text("4. 手势拦截示例 (列表滚动与拖拽冲突)")
    Spacer(modifier = Modifier.height(8.dp))

    var outerOffset by remember { mutableStateOf(0f) }
    var innerOffset by remember { mutableStateOf(0f) }
    var isInnerDragging by remember { mutableStateOf(false) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
    ) {
        // 添加一些头部项
        items(3) { index ->
            Text(
                "Header Item $index",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.LightGray),
            )
        }

        // 可拖拽项
        item {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Text(
                    "外层偏移: ${outerOffset.roundToInt()}",
                    modifier = Modifier.align(Alignment.TopStart),
                )

                Box(
                    modifier =
                        Modifier
                            .size(150.dp)
                            .offset(x = outerOffset.dp)
                            .background(Color.LightGray)
                            .align(Alignment.Center)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (!isInnerDragging) {
                                        outerOffset += dragAmount
                                    }
                                }
                            },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(80.dp)
                                .offset(x = innerOffset.dp)
                                .background(Color.Blue)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isInnerDragging = true
                                        },
                                        onDragEnd = {
                                            isInnerDragging = false
                                        },
                                        onDragCancel = {
                                            isInnerDragging = false
                                        },
                                    ) { _, dragAmount ->
                                        innerOffset += dragAmount
                                    }
                                },
                    )

                    Text(
                        "内层偏移: ${innerOffset.roundToInt()}",
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    )
                }
            }
        }

        // 添加一些底部项
        items(3) { index ->
            Text(
                "Footer Item $index",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.LightGray),
            )
        }
    }
}

@Composable
private fun RotateAndDragExample() {
    Text("5. 拖拽与旋转冲突 (可拖拽和旋转的项)")
    Spacer(modifier = Modifier.height(8.dp))

    var dragOffset by remember { mutableStateOf(0f) }
    var rotation by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(1.dp, Color.Gray),
    ) {
        items(10) { index ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(8.dp)
                        .background(Color.LightGray),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(50.dp)
                            .offset(x = dragOffset.dp)
                            .graphicsLayer(
                                rotationZ = rotation,
                            ).background(Color.Blue)
                            .pointerInput(Unit) {
                                detectTransformGestures(
                                    onGesture = { _, pan, gestureRotation, _ ->
                                        dragOffset += pan.x
                                        rotation += gestureRotation
                                    },
                                )
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "项 $index",
                        color = Color.White,
                    )
                }

                Text(
                    "偏移: ${dragOffset.roundToInt()}, 旋转: ${rotation.roundToInt()}°",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                )
            }
        }
    }
} 
