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

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.painter.BrushPainter
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("BrushDemo")
internal class BrushDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        // 这里可以修改一些基本配置
        layoutDirection = LayoutDirection.Ltr

        setContent {
            ComposeNavigationBar {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text("1、垂直渐变色 - 透明到半透明黑")
                        Box(
                            modifier =
                            Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .padding(top = 60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                        listOf(
                                            Color(0x00000000),
                                            Color(0x4C000000),
                                        ),
                                    ),
                                ),
                        )
                    }

                    item {
                        Text("2、横向渐变色 - 彩虹色")
                        Box(
                            modifier =
                            Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .padding(top = 60.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors =
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Blue,
                                            Color.Magenta,
                                        ),
                                    ),
                                ),
                        )
                    }

                    item {
                        Text("6、多色渐变组合")
                        Box(
                            modifier =
                            Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .padding(top = 60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                        listOf(
                                            Color(0xFFE91E63),
                                            Color(0xFF9C27B0),
                                            Color(0xFF2196F3),
                                            Color(0xFF4CAF50),
                                        ),
                                        startY = 0f,
                                        endY = 200f,
                                    ),
                                ),
                        )
                    }

                    item {
                        Text("7、半透明渐变效果")
                        Box(
                            modifier =
                            Modifier
                                .width(200.dp)
                                .height(200.dp)
                                .padding(top = 60.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors =
                                        listOf(
                                            Color(0x00FFFFFF),
                                            Color(0x80FFFFFF),
                                            Color(0xFFFFFFFF),
                                        ),
                                    ),
                                ),
                        )
                    }

                    item {
                        Text(
                            "FillBounds BrushPainterBrushPainterBrushPainterBrushPainterBrushPainterBrushPainterBrushPainter",
                            modifier =
                            Modifier
                                .wrapContentWidth(unbounded = true)
                                .border(1.dp, Color.Red),
                        )
                        val brushPainter =
                            BrushPainter(
                                Brush.verticalGradient(
                                    colors =
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Blue,
                                        Color.Magenta,
                                    ),
                                ),
                            )
                        brushPainter.applyAlpha(0.5f)
                        com.tencent.kuikly.compose.foundation.Image(
                            modifier =
                            Modifier
                                .size(50.dp)
                                .rotate(45f)
                                .border(1.dp, Color.Red)
                                .background(Color.Green),
                            painter = brushPainter,
                            contentDescription = null,
                            alignment = Alignment.TopStart,
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                }
            }
        }
    }
}
