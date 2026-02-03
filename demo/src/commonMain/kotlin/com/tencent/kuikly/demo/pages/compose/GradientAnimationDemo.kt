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
import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.core.CubicBezierEasing
import com.tencent.kuikly.compose.animation.core.FastOutSlowInEasing
import com.tencent.kuikly.compose.animation.core.LinearEasing
import com.tencent.kuikly.compose.animation.core.RepeatMode
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.infiniteRepeatable
import com.tencent.kuikly.compose.animation.core.rememberInfiniteTransition
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.Column
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
import com.tencent.kuikly.compose.material3.HorizontalDivider
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("GradientAnimationDemo")
class GradientAnimationDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                GradientAnimationContent()
            }
        }
    }
}

@Composable
fun ColumnScroll(modifier: Modifier, content: @Composable () -> Unit) {
    LazyColumn(modifier) {
        item {
            content()
        }
    }
}

/**
 * Demo: 基于渐变色 start 和 end 实现渐变色动画
 *
 * 这个Demo展示了如何：
 * 1. 使用 animateColor 在两个颜色之间创建平滑的渐变动画
 * 2. 使用 Brush.linearGradient 创建线性渐变效果
 * 3. 使用 rememberInfiniteTransition 创建无限循环的渐变动画
 * 4. 实现多种渐变方向和动画效果
 */
@Composable
fun GradientAnimationContent() {
    ColumnScroll(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "渐变色动画 Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // 说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "说明：",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "本Demo展示了两种渐变动画方式：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. 使用 start 和 end 颜色创建渐变色动画",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "2. 使用 start 和 end offset（位置）创建渐变动画",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // Demo 1: 基础渐变动画（水平方向）
        BasicGradientAnimationDemo()

        // Demo 2: 垂直渐变动画
        VerticalGradientAnimationDemo()

        // Demo 3: 对角线渐变动画
        DiagonalGradientAnimationDemo()

        // Demo 4: 多色渐变动画
        MultiColorGradientAnimationDemo()

        // Demo 5: 可控制的渐变动画
        ControllableGradientAnimationDemo()

        // 分隔线
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Demo 7-10: 基于 offset 的渐变动画
        Text(
            text = "基于 Start/End Offset 的渐变动画",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Demo 7: 水平移动的渐变（offset 动画）
        HorizontalOffsetGradientDemo()

//        // Demo 8: 垂直移动的渐变（offset 动画）
        VerticalOffsetGradientDemo()

        // Demo 10: 对角线移动的渐变（offset 动画）
        DiagonalOffsetGradientDemo()

        // Demo 11: 组合颜色和 offset 的渐变动画
        CombinedGradientDemo()

        // 分隔线
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Demo 12: Text 组件基于 brush 的渐变动画
        Text(
            text = "Text 组件 Brush 渐变动画",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Demo 12: Text 渐变扫光动画（像素模式）
        TextGradientOffsetDemo()

        // 分隔线
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Demo 13: Canvas 渐变动画
        Text(
            text = "Canvas 渐变动画",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        CanvasGradientAnimationDemo()

        // 分隔线
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Demo 14: 负数坐标渐变动画
        Text(
            text = "负数坐标渐变动画",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        NegativeOffsetGradientDemo()
    }
}

/**
 * Demo 1: 基础水平渐变动画
 * 使用 animateColor 在 start 和 end 颜色之间创建动画
 */
@Composable
fun BasicGradientAnimationDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "1. 基础水平渐变动画",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // 定义起始和结束颜色
            val startColor = Color(0xFF6C5CE7) // 紫色
            val endColor = Color(0xFF00D2FF)   // 青色

            // 创建无限循环动画
            val infiniteTransition = rememberInfiniteTransition(label = "gradient")

            // 在 start 和 end 颜色之间动画
            val animatedStartColor by infiniteTransition.animateColor(
                initialValue = startColor,
                targetValue = endColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "startColor"
            )

            val animatedEndColor by infiniteTransition.animateColor(
                initialValue = endColor,
                targetValue = startColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "endColor"
            )

            // 使用 Brush.linearGradient 创建水平渐变
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(animatedStartColor, animatedEndColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "水平渐变",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Start: ${animatedStartColor}, End: ${animatedEndColor}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Demo 2: 垂直渐变动画
 */
@Composable
fun VerticalGradientAnimationDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "2. 垂直渐变动画",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFFFF6B6B) // 红色
            val endColor = Color(0xFFFFE66D)   // 黄色

            val infiniteTransition = rememberInfiniteTransition(label = "verticalGradient")

            val animatedStartColor by infiniteTransition.animateColor(
                initialValue = startColor,
                targetValue = endColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "verticalStart"
            )

            val animatedEndColor by infiniteTransition.animateColor(
                initialValue = endColor,
                targetValue = startColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "verticalEnd"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(animatedStartColor, animatedEndColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "垂直渐变",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Demo 3: 对角线渐变动画
 */
@Composable
fun DiagonalGradientAnimationDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "3. 对角线渐变动画",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFF4ECDC4) // 青绿色
            val endColor = Color(0xFF44A08D)   // 深绿色

            val infiniteTransition = rememberInfiniteTransition(label = "diagonalGradient")

            val animatedStartColor by infiniteTransition.animateColor(
                initialValue = startColor,
                targetValue = endColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "diagonalStart"
            )

            val animatedEndColor by infiniteTransition.animateColor(
                initialValue = endColor,
                targetValue = startColor,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "diagonalEnd"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(animatedStartColor, animatedEndColor),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "对角线渐变",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Demo 4: 多色渐变动画
 */
@Composable
fun MultiColorGradientAnimationDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "4. 多色渐变动画",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val color1 = Color(0xFFFF6B9D) // 粉红色
            val color2 = Color(0xFFC44569) // 深粉色
            val color3 = Color(0xFFFFC93C) // 黄色

            val infiniteTransition = rememberInfiniteTransition(label = "multiColorGradient")

            val animatedColor1 by infiniteTransition.animateColor(
                initialValue = color1,
                targetValue = color2,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "color1"
            )

            val animatedColor2 by infiniteTransition.animateColor(
                initialValue = color2,
                targetValue = color3,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "color2"
            )

            val animatedColor3 by infiniteTransition.animateColor(
                initialValue = color3,
                targetValue = color1,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "color3"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(animatedColor1, animatedColor2, animatedColor3)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "多色渐变",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Demo 5: 可控制的渐变动画
 * 用户可以通过按钮控制动画的开始和停止
 */
@Composable
fun ControllableGradientAnimationDemo() {
    var isAnimating by remember { mutableStateOf(true) }
    var toggleState by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "5. 可控制的渐变动画",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFF667EEA) // 蓝紫色
            val endColor = Color(0xFF764BA2)   // 紫色

            // 使用 LaunchedEffect 来控制状态切换
            LaunchedEffect(isAnimating) {
                if (isAnimating) {
                    while (isAnimating) {
                        toggleState = !toggleState
                        kotlinx.coroutines.delay(1500)
                    }
                }
            }

            val targetStartColor = if (toggleState) endColor else startColor
            val targetEndColor = if (toggleState) startColor else endColor

            val animatedStartColor by animateColorAsState(
                targetValue = targetStartColor,
                animationSpec = if (isAnimating) {
                    tween(1500, easing = LinearEasing)
                } else {
                    tween(0)
                },
                label = "controllableStart"
            )

            val animatedEndColor by animateColorAsState(
                targetValue = targetEndColor,
                animationSpec = if (isAnimating) {
                    tween(1500, easing = LinearEasing)
                } else {
                    tween(0)
                },
                label = "controllableEnd"
            )

            Button(
                onClick = { isAnimating = !isAnimating },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isAnimating) "停止动画" else "开始动画")
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(animatedStartColor, animatedEndColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAnimating) "动画中..." else "已停止",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


/**
 * Demo 7: 水平移动的渐变（使用 start 和 end offset）
 * 通过动画化 offset 的 x 坐标来实现渐变位置的移动
 */
@Composable
fun HorizontalOffsetGradientDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "7. 水平移动的渐变（Offset 动画）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFF6C5CE7) // 紫色
            val endColor = Color(0xFF00D2FF)   // 青色

            // 使用 BoxWithConstraints 获取容器尺寸
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val density = LocalDensity.current
                val width = constraints.maxWidth.toFloat()
                val height = with(density) { 100.dp.toPx() }

                val infiniteTransition = rememberInfiniteTransition(label = "horizontalOffset")

                // 动画化 start offset 的 x 坐标
                val startOffsetX by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = width,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "startOffsetX"
                )

                // end offset 的 x 坐标跟随 start offset
                val endOffsetX = startOffsetX + width

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(startColor, endColor),
                                start = Offset(startOffsetX, 0f),
                                end = Offset(endOffsetX, 0f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "水平移动渐变",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "通过动画化 start 和 end offset 的 x 坐标实现渐变移动",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Demo 8: 垂直移动的渐变（使用 start 和 end offset）
 */
@Composable
fun VerticalOffsetGradientDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "8. 垂直移动的渐变（Offset 动画）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFFFF6B6B) // 红色
            val endColor = Color(0xFFFFE66D)   // 黄色

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val density = LocalDensity.current
                val width = constraints.maxWidth.toFloat()
                val height = with(density) { 100.dp.toPx() }

                val infiniteTransition = rememberInfiniteTransition(label = "verticalOffset")

                // 动画化 start offset 的 y 坐标
                val startOffsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = height,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "startOffsetY"
                )

                val endOffsetY = startOffsetY + height

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(startColor, endColor),
                                start = Offset(0f, startOffsetY),
                                end = Offset(0f, endOffsetY)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "垂直移动渐变",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "通过动画化 start 和 end offset 的 y 坐标实现渐变移动",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Demo 10: 对角线移动的渐变（使用 start 和 end offset）
 */
@Composable
fun DiagonalOffsetGradientDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "10. 对角线移动的渐变（Offset 动画）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor = Color(0xFFFF6B9D) // 粉红色
            val endColor = Color(0xFFC44569)   // 深粉色

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val density = LocalDensity.current
                val width = constraints.maxWidth.toFloat()
                val height = with(density) { 100.dp.toPx() }

                val infiniteTransition = rememberInfiniteTransition(label = "diagonalOffset")

                // 同时动画化 x 和 y 坐标
                val offsetX by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = width,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offsetX"
                )

                val offsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = height,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "offsetY"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(startColor, endColor),
                                start = Offset(offsetX, offsetY),
                                end = Offset(offsetX + width, offsetY + height)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "对角线移动渐变",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "通过同时动画化 start 和 end offset 的 x、y 坐标实现对角线移动",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Demo 11: 组合颜色和 offset 的渐变动画
 * 同时动画化颜色和 offset 位置
 */
@Composable
fun CombinedGradientDemo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "11. 组合动画：颜色 + Offset",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val startColor1 = Color(0xFF667EEA) // 蓝紫色
            val endColor1 = Color(0xFF764BA2)   // 紫色
            val startColor2 = Color(0xFFFF9A56) // 橙色
            val endColor2 = Color(0xFFFF6A88)   // 粉红色

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val density = LocalDensity.current
                val width = constraints.maxWidth.toFloat()
                val height = with(density) { 100.dp.toPx() }

                val infiniteTransition = rememberInfiniteTransition(label = "combinedGradient")

                // 动画化颜色
                val animatedStartColor by infiniteTransition.animateColor(
                    initialValue = startColor1,
                    targetValue = startColor2,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "combinedStartColor"
                )

                val animatedEndColor by infiniteTransition.animateColor(
                    initialValue = endColor1,
                    targetValue = endColor2,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "combinedEndColor"
                )

                // 动画化 offset
                val offsetX by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = width,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "combinedOffsetX"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(animatedStartColor, animatedEndColor),
                                start = Offset(offsetX, 0f),
                                end = Offset(offsetX + width, 0f)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "颜色 + Offset 组合",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "同时动画化渐变的颜色和 offset 位置，创造更丰富的视觉效果",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * Demo 12: Text 组件渐变扫光动画（像素模式）
 * 使用 start/end offset 像素值实现文字扫光效果
 */
@Composable
fun TextGradientOffsetDemo() {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "TextGradient")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D47A1)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Text Brush 扫光动画（像素模式）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 金色扫光效果
            AnimatedTextGradient(
                text = "✨ 闪闪发光的文字 ✨",
                colors = listOf(
                    Color(0xFF1A237E),    // 深蓝色
                    Color(0xFFFFD700),   // 金色
                    Color(0xFF1A237E)     // 深蓝色
                ),
                textWidthPx = with(density) { (28.sp.toPx() * 12) },
                scanWidthRatio = 0.2f,
                duration = 2000,
                fontSize = 28.sp,
                infiniteTransition = infiniteTransition,
                label = "goldenScan"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 彩虹扫光效果
            AnimatedTextGradient(
                text = "🌈 彩虹文字效果 🌈",
                colors = listOf(
                    Color(0xFFFF0000),  // 红
                    Color(0xFFFF7F00),  // 橙
                    Color(0xFFFFFF00),  // 黄
                    Color(0xFF00FF00),  // 绿
                    Color(0xFF0000FF),  // 蓝
                    Color(0xFF4B0082),  // 靛
                    Color(0xFF9400D3),  // 紫
                    Color(0xFFFF0000)   // 红（循环）
                ),
                textWidthPx = with(density) { (24.sp.toPx() * 11) },
                scanWidthRatio = 0.4f,
                duration = 2500,
                fontSize = 24.sp,
                infiniteTransition = infiniteTransition,
                label = "rainbowScan"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 金属光泽效果
            AnimatedTextGradient(
                text = "⚡ 金属光泽效果 ⚡",
                colors = listOf(
                    Color(0xFF666666),   // 深灰
                    Color(0xFFCCCCCC),   // 浅灰
                    Color(0xFFFFFFFF),   // 白（高光）
                    Color(0xFFCCCCCC),   // 浅灰
                    Color(0xFF666666)    // 深灰
                ),
                textWidthPx = with(density) { (24.sp.toPx() * 10) },
                scanWidthRatio = 0.3f,
                duration = 1500,
                fontSize = 24.sp,
                infiniteTransition = infiniteTransition,
                label = "metalScan"
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "使用像素值控制渐变位置，y 坐标传入参考宽度",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 可复用的动画文字渐变组件
 * 
 * @param text 要显示的文字
 * @param colors 渐变色列表
 * @param textWidthPx 文本宽度（像素）
 * @param scanWidthRatio 扫光宽度比例（0~1）
 * @param duration 动画时长（毫秒）
 * @param fontSize 字体大小
 * @param infiniteTransition 无限动画过渡对象
 * @param label 动画标签
 */
@Composable
private fun AnimatedTextGradient(
    text: String,
    colors: List<Color>,
    textWidthPx: Float,
    scanWidthRatio: Float,
    duration: Int,
    fontSize: com.tencent.kuikly.compose.ui.unit.TextUnit,
    infiniteTransition: com.tencent.kuikly.compose.animation.core.InfiniteTransition,
    label: String
) {
    val scanWidthPx = textWidthPx * scanWidthRatio
    val scanPositionPx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = textWidthPx - scanWidthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = label
    )

    Text(
        text = text,
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(scanPositionPx, textWidthPx),
                end = Offset(scanPositionPx + scanWidthPx, textWidthPx)
            ),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    )
}

/**
 * Demo 13: Canvas 渐变动画
 * 使用 Canvas 绘制渐变动画效果
 */
@Composable
fun CanvasGradientAnimationDemo() {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "CanvasGradient")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Canvas 渐变动画示例",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 水平渐变扫光动画
            val scanPosition by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "scanPosition"
            )

            val canvasWidthPx = with(density) { 300.dp.toPx() }
            val canvasHeightPx = with(density) { 100.dp.toPx() }
            val scanWidthPx = canvasWidthPx * 0.3f
            val scanStartXPx = scanPosition * (canvasWidthPx - scanWidthPx)
            val scanEndXPx = scanStartXPx + scanWidthPx

            Text(
                text = "水平渐变扫光",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(300.dp, 100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A237E),
                            Color(0xFFFFD700),
                            Color(0xFF1A237E)
                        ),
                        start = Offset(scanStartXPx, canvasHeightPx),
                        end = Offset(scanEndXPx, canvasHeightPx)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 旋转渐变动画
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotationAngle"
            )

            Text(
                text = "旋转渐变",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = kotlin.math.min(size.width, size.height) / 2

                // 计算旋转后的起点和终点
                val angleRad = kotlin.math.PI * rotationAngle / 180.0
                val startX = centerX + radius * kotlin.math.cos(angleRad).toFloat()
                val startY = centerY + radius * kotlin.math.sin(angleRad).toFloat()
                val endX = centerX - radius * kotlin.math.cos(angleRad).toFloat()
                val endY = centerY - radius * kotlin.math.sin(angleRad).toFloat()

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFF4ECDC4),
                            Color(0xFF45B7D1),
                            Color(0xFF96CEB4),
                            Color(0xFFFFEAA7)
                        ),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 对角线渐变动画
            val diagonalOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "diagonalOffset"
            )

            Text(
                text = "对角线渐变",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val offsetX = diagonalOffset * size.width
                val offsetY = diagonalOffset * size.height

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFFF8E53),
                            Color(0xFFFFA07A),
                            Color(0xFFFFB6C1)
                        ),
                        start = Offset(offsetX, offsetY),
                        end = Offset(size.width - offsetX, size.height - offsetY)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 多色渐变波浪动画
            val waveOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "waveOffset"
            )

            Text(
                text = "多色渐变波浪",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(300.dp, 100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val waveWidth = size.width / 4
                val waveStartX = waveOffset * (size.width + waveWidth) - waveWidth

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF667EEA),
                            Color(0xFF764BA2),
                            Color(0xFFF093FB),
                            Color(0xFF4FACFE),
                            Color(0xFF00F2FE)
                        ),
                        start = Offset(waveStartX, size.height),
                        end = Offset(waveStartX + waveWidth, size.height)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "使用 Canvas 绘制渐变动画，支持线性、径向等多种渐变效果",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Demo 14: 负数坐标渐变动画
 * 展示如何使用负数坐标实现特殊的渐变效果
 */
@Composable
fun NegativeOffsetGradientDemo() {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "NegativeOffset")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "负数坐标渐变动画示例",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "负数坐标可用于：从屏幕外滑入、中心偏移、反向渐变等效果",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 从屏幕外滑入的渐变动画
            val slideInOffset by infiniteTransition.animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "slideInOffset"
            )

            Text(
                text = "1. 从左侧屏幕外滑入",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val width = with(density) { 300.dp.toPx() }
                val height = with(density) { 80.dp.toPx() }
                
                // 使用负数作为起始位置，渐变从屏幕外开始
                val startX = slideInOffset * width  // -width 到 +width
                val endX = startX + width

                Box(
                    modifier = Modifier
                        .size(300.dp, 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00C9FF),
                                    Color(0xFF92FE9D)
                                ),
                                start = Offset(startX, height),
                                end = Offset(endX, height)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "从左侧滑入 (start: ${startX.toInt()}px)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. 反向渐变动画（从右到左）
            val reverseOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "reverseOffset"
            )

            Text(
                text = "2. 反向渐变（使用负数实现从右到左）",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val width = with(density) { 300.dp.toPx() }
                val height = with(density) { 80.dp.toPx() }
                
                // 使用负数实现反向渐变
                // start > end 时，渐变方向反转
                val startX = width - reverseOffset * width  // 从 width 到 0
                val endX = -reverseOffset * width           // 从 0 到 -width

                Box(
                    modifier = Modifier
                        .size(300.dp, 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF416C),
                                    Color(0xFFFF4B2B)
                                ),
                                start = Offset(startX, height),
                                end = Offset(endX, height)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "反向渐变 (end: ${endX.toInt()}px)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. 中心偏移的对角线渐变
            val centerOffset by infiniteTransition.animateFloat(
                initialValue = -0.5f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "centerOffset"
            )

            Text(
                text = "3. 中心偏移的对角线渐变",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val size = with(density) { 150.dp.toPx() }
                
                // 中心点，使用负数偏移
                val centerX = size / 2
                val centerY = size / 2
                val offsetAmount = centerOffset * size  // 偏移量可以是负数
                
                val startX = centerX - size / 2 + offsetAmount
                val startY = centerY - size / 2 + offsetAmount
                val endX = centerX + size / 2 + offsetAmount
                val endY = centerY + size / 2 + offsetAmount

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2),
                                    Color(0xFFFF7EB3)
                                ),
                                start = Offset(startX, startY),
                                end = Offset(endX, endY)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "中心偏移",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "offset: ${offsetAmount.toInt()}px",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Canvas 中的负数坐标
            val canvasNegativeOffset by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 100f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "canvasNegativeOffset"
            )

            Text(
                text = "4. Canvas 中使用负数坐标",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .size(200.dp, 100.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // 使用负数坐标，渐变可以从 Canvas 外部开始
                val startX = canvasNegativeOffset  // 从 -100 到 100
                val endX = startX + size.width
                val refSize = kotlin.math.max(size.width, kotlin.math.abs(canvasNegativeOffset) + size.width)

                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFDEB71),
                            Color(0xFFF8D800),
                            Color(0xFFFF8235),
                            Color(0xFFFF0000)
                        ),
                        start = Offset(startX, refSize),
                        end = Offset(endX, refSize)
                    )
                )
            }

            Text(
                text = "当前 startX: ${canvasNegativeOffset.toInt()}px",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "负数坐标由 Brush.kt 内部使用绝对值处理，确保正确归一化",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}