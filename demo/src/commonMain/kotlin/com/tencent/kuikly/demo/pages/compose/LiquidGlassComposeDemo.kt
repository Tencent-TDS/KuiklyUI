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
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.LiquidGlassStyle
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.liquidGlass
import com.tencent.kuikly.compose.foundation.liquidGlassContainer
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Surface
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlin.math.PI

@Page("LiquidGlassComposeDemo")
internal class LiquidGlassComposeDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            // 全屏背景
            Box(modifier = Modifier.fillMaxSize()) {
                // 简笔画背景
                SketchBackground()
                
                // 内容层
                DemoScaffold("Liquid Glass Demo", true) {
                    Text("基础效果")
                    ContainerSamples()
                    Text("融合效果")
                    ContainerBlendingSamples()
                    Text("交互示例")
                    InteractiveSamples()
                }
            }
        }
    }

}

@Composable
private fun ContainerSamples() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .liquidGlass(
                    style = LiquidGlassStyle.REGULAR,
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "液态玻璃Box (Regular)",
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .liquidGlass(
                    style = LiquidGlassStyle.CLEAR,
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "液态玻璃Box (Clear)",
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ContainerBlendingSamples() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .liquidGlassContainer(spacing = 20f)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp, 60.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(30.dp))
                    .liquidGlass(
                        style = LiquidGlassStyle.REGULAR,
                    )
            )

            Box(
                modifier = Modifier
                    .size(120.dp, 60.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(30.dp))
                    .liquidGlass(
                        style = LiquidGlassStyle.REGULAR,
                        tintColor = Color.Green.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

@Composable
private fun InteractiveSamples() {
    var clickCount by remember { mutableStateOf(0) }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .liquidGlass(
                    style = LiquidGlassStyle.REGULAR,
                    tintColor = Color.Green.copy(alpha = 0.2f)
                )
                .clickable { clickCount++ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "点击次数: $clickCount",
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }

        Surface(
            onClick = { clickCount++ },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(10.dp))
                .liquidGlass(
                    style = LiquidGlassStyle.CLEAR,
                    tintColor = Color.Yellow.copy(alpha = 0.3f)
                ),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "可点击的液态玻璃Surface",
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private inline fun Float.toRadians(): Float = (this * PI / 180f).toFloat()

@Composable
private fun SketchBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // 浅色背景
        drawRect(Color(0xFFFAFAFA))
        
        // 绘制简单的简笔画图案
        val strokeWidth = 2.dp.toPx()
        
        // 基础网格线
        val gridSize = 50.dp.toPx()
        for (x in 0..size.width.toInt() step gridSize.toInt()) {
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (y in 0..size.height.toInt() step gridSize.toInt()) {
            drawLine(
                color = Color(0xFFE0E0E0),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // 太阳
        drawCircle(
            color = Color(0xFFFFD700),
            radius = 30.dp.toPx(),
            center = Offset(80.dp.toPx(), 80.dp.toPx()),
            style = Stroke(strokeWidth)
        )
        
        // 太阳光线
        for (i in 0..7) {
            val angle = i * 45f
            val radians = (angle.toDouble()).toFloat().toRadians()
            val startRadius = 35.dp.toPx()
            val endRadius = 45.dp.toPx()
            val centerX = 80.dp.toPx()
            val centerY = 80.dp.toPx()

            drawLine(
                color = Color(0xFFFFD700),
                start = Offset(
                    centerX + startRadius * kotlin.math.cos(radians).toFloat(),
                    centerY + startRadius * kotlin.math.sin(radians).toFloat()
                ),
                end = Offset(
                    centerX + endRadius * kotlin.math.cos(radians).toFloat(),
                    centerY + endRadius * kotlin.math.sin(radians).toFloat()
                ),
                strokeWidth = strokeWidth
            )
        }
        
        // 云朵
        val cloudPath = Path().apply {
            moveTo(200.dp.toPx(), 60.dp.toPx())
            quadraticBezierTo(220.dp.toPx(), 40.dp.toPx(), 240.dp.toPx(), 60.dp.toPx())
            quadraticBezierTo(260.dp.toPx(), 50.dp.toPx(), 280.dp.toPx(), 70.dp.toPx())
            quadraticBezierTo(300.dp.toPx(), 60.dp.toPx(), 320.dp.toPx(), 80.dp.toPx())
            quadraticBezierTo(300.dp.toPx(), 100.dp.toPx(), 280.dp.toPx(), 90.dp.toPx())
            quadraticBezierTo(260.dp.toPx(), 110.dp.toPx(), 240.dp.toPx(), 100.dp.toPx())
            quadraticBezierTo(220.dp.toPx(), 120.dp.toPx(), 200.dp.toPx(), 100.dp.toPx())
            close()
        }
        drawPath(
            path = cloudPath,
            color = Color(0xFFE0E0E0),
            style = Stroke(strokeWidth)
        )
        
        // 小山
        val mountainPath = Path().apply {
            moveTo(0f, size.height * 0.8f)
            lineTo(size.width * 0.3f, size.height * 0.5f)
            lineTo(size.width * 0.6f, size.height * 0.7f)
            lineTo(size.width * 0.8f, size.height * 0.4f)
            lineTo(size.width, size.height * 0.6f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = mountainPath,
            color = Color(0xFF90EE90),
            style = Stroke(strokeWidth)
        )
        
        // 房子
        val housePath = Path().apply {
            // 房子主体
            moveTo(150.dp.toPx(), 200.dp.toPx())
            lineTo(150.dp.toPx(), 250.dp.toPx())
            lineTo(200.dp.toPx(), 250.dp.toPx())
            lineTo(200.dp.toPx(), 200.dp.toPx())
            close()
            
            // 屋顶
            moveTo(140.dp.toPx(), 200.dp.toPx())
            lineTo(175.dp.toPx(), 170.dp.toPx())
            lineTo(210.dp.toPx(), 200.dp.toPx())
            close()
        }
        drawPath(
            path = housePath,
            color = Color(0xFF8B4513),
            style = Stroke(strokeWidth)
        )
        
        // 树
        val treePath = Path().apply {
            // 树干
            moveTo(300.dp.toPx(), 220.dp.toPx())
            lineTo(300.dp.toPx(), 250.dp.toPx())
            lineTo(310.dp.toPx(), 250.dp.toPx())
            lineTo(310.dp.toPx(), 220.dp.toPx())
            close()
            
            // 树冠
            moveTo(280.dp.toPx(), 220.dp.toPx())
            quadraticBezierTo(305.dp.toPx(), 180.dp.toPx(), 330.dp.toPx(), 220.dp.toPx())
        }
        drawPath(
            path = treePath,
            color = Color(0xFF228B22),
            style = Stroke(strokeWidth)
        )
        
        // 波浪线
        val wavePath = Path().apply {
            moveTo(0f, size.height * 0.9f)
            for (i in 0..20) {
                val x = (size.width / 20) * i
                val y = size.height * 0.9f + kotlin.math.sin(i * 0.5) * 10.dp.toPx()
                if (i == 0) moveTo(x, y.toFloat()) else lineTo(x, y.toFloat())
            }
        }
        drawPath(
            path = wavePath,
            color = Color(0xFF87CEEB),
            style = Stroke(strokeWidth)
        )
    }
}