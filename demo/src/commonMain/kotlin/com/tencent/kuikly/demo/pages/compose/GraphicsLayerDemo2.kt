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
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.CompositingStrategy
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("graphicslayer2")
internal class GraphicsLayerDemo2 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumn(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        val translatePx = with(LocalDensity.current) { 25.dp.toPx() }
                        // alpha 透明度
                        Text("Alpha 透明度")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        alpha = 0.5f
                                    }.background(Color.Red),
                            )
                        }

                        // translationX/Y 平移
                        Text("Translation 平移")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        translationX = translatePx
                                        translationY = translatePx
                                    }.background(Color.Red),
                            )
                        }

                        // scaleX/Y 缩放
                        Text("Scale 缩放")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        scaleX = 1.5f
                                        scaleY = 0.8f
                                    }.background(Color.Red),
                            )
                        }

                        // rotationX/Y/Z 旋转
                        Text("Rotation 3D旋转")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        rotationX = 45f
                                        rotationY = 45f
                                        rotationZ = 45f
                                    }.background(Color.Red),
                            )
                        }

                        // transformOrigin 变换原点
                        Text("TransformOrigin 变换原点")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        rotationZ = 45f
                                    }.background(Color.Red),
                            )
                        }

                        // cameraDistance 镜头距离
                        Text("CameraDistance 镜头距离")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        cameraDistance = 8f
                                        rotationX = 60f
                                    }.background(Color.Red),
                            )
                        }

                        // shadowElevation 阴影
                        Text("ShadowElevation 阴影")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        shadowElevation = 16f
                                    }.background(Color.Red),
                            )
                        }

                        // shape 形状裁剪
                        Text("Shape 形状裁剪")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        shape = RoundedCornerShape(16.dp)
                                        clip = true
                                    }.background(Color.Red),
                            )
                        }

                        // compositingStrategy 合成策略
                        Text("CompositingStrategy 合成策略")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        alpha = 0.5f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例1: 旋转+缩放
                        Text("组合示例1: 旋转+缩放")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        rotationZ = 45f
                                        scaleX = 1.5f
                                        scaleY = 1.5f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例2: 透明度+阴影
                        Text("组合示例2: 透明度+阴影")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        alpha = 0.7f
                                        shadowElevation = 12f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例3: 形状裁剪+平移
                        Text("组合示例3: 形状裁剪+平移")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        shape = RoundedCornerShape(16.dp)
                                        clip = true
                                        translationX = translatePx
                                        translationY = translatePx
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例4: 3D旋转+阴影
                        Text("组合示例4: 3D旋转+阴影")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        rotationX = 30f
                                        rotationY = 30f
                                        shadowElevation = 16f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例5: 缩放+合成策略
                        Text("组合示例5: 缩放+合成策略")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        scaleX = 1.3f
                                        scaleY = 1.3f
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        alpha = 0.6f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例6: 变换原点+旋转
                        Text("组合示例6: 变换原点+旋转")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0f, 0f) // 左上角为旋转中心
                                        rotationZ = 45f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例7: 多层阴影+形状
                        Text("组合示例7: 多层阴影+形状")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        shape = CircleShape
                                        clip = true
                                        shadowElevation = 20f
                                    }.graphicsLayer {
                                        shadowElevation = 10f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例8: 3D透视+缩放
                        Text("组合示例8: 3D透视+缩放")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        cameraDistance = 8f
                                        rotationX = 45f
                                        scaleX = 1.2f
                                        scaleY = 1.2f
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例9: 渐变合成+位移
                        Text("组合示例9: 渐变合成+位移")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                        translationX = translatePx
                                        translationY = translatePx
                                    }.background(Color.Red),
                            )
                        }

                        // 组合示例10: 裁剪+3D旋转
                        Text("组合示例10: 裁剪+3D旋转")
                        Box(
                            Modifier.size(50.dp).background(Color.LightGray),
                        ) {
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        shape = RoundedCornerShape(25.dp)
                                        clip = true
                                        rotationX = 30f
                                        rotationY = 45f
                                    }.background(Color.Red),
                            )
                        }
                    }
                }
            }
        }
    }
}
