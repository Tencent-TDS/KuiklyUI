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
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.gestures.draggable2D
import com.tencent.kuikly.compose.foundation.gestures.rememberDraggable2DState
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.geometry.CornerRadius
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.PointMode
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Fill
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.drawscope.clipPath
import com.tencent.kuikly.compose.ui.graphics.drawscope.clipRect
import com.tencent.kuikly.compose.ui.graphics.drawscope.inset
import com.tencent.kuikly.compose.ui.graphics.drawscope.rotate
import com.tencent.kuikly.compose.ui.graphics.drawscope.scale
import com.tencent.kuikly.compose.ui.graphics.drawscope.translate
import com.tencent.kuikly.compose.ui.graphics.drawscope.withTransform
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.DpSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.demo.pages.base.BridgeModule
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Page("CanvasDemo")
class CanvasDemo : ComposeContainer() {

    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        return externalModules
    }

    override fun willInit() {
        super.willInit()

        setContent {
            DemoScaffold("Canvas Demo", back = true) {
                CanvasAPIDemo()
                CanvasDemoContent()
                ArcDemo()
                TranslatePathDemo()
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CanvasDemoContent() {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        // horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val max = 300.dp
        val min = 0.dp
        val (minPx, maxPx) = with(LocalDensity.current) { min.toPx() to max.toPx() }
        // this is the offset we will update while dragging
        var offsetPositionX by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier.size(max, 50.dp)
                .draggable2D(
                    state = rememberDraggable2DState { delta ->
                        val newValueX = offsetPositionX + delta.x
                        offsetPositionX = newValueX.coerceIn(minPx, maxPx)
                    }
                )
                .background(Color.Yellow)
        ) {
            Text("拖动红色方块", modifier = Modifier.align(Alignment.Center))
            Box(
                Modifier.offset(with(LocalDensity.current) { offsetPositionX.toDp() }, 0.dp)
                    .size(50.dp)
                    .background(Color.Red)
            )
        }
        // 画布
        Canvas(
            modifier = Modifier.size(max, 100.dp).background(Color.Gray)
        ) {
            // 绘制一个矩形
            drawRect(
                color = Color.Red,
                topLeft = Offset(offsetPositionX, 0f),
                size = DpSize(50.dp, 50.dp).toSize()
            )
        }
        // 画布
        Canvas(
            modifier = Modifier.size(max, 100.dp).background(Color.Gray).drawBehind {
                drawRect(
                    color = Color.Blue,
                    topLeft = Offset((offsetPositionX + 50.dp.toPx()), 50.dp.toPx()),
                    size = DpSize(50.dp, 50.dp).toSize()
                )
            }
        ) {
            // 绘制一个矩形
            drawRect(
                color = Color.Red,
                topLeft = Offset(offsetPositionX, 0f),
                size = DpSize(50.dp, 50.dp).toSize()
            )
        }
        // 画布
        Canvas(
            modifier = Modifier.size(max, 100.dp).background(Color.Gray)
        ) {
            val path1 = Path().apply {
                moveTo(offsetPositionX, 0f)
                lineTo(50.dp.toPx(), 0f)
                lineTo(50.dp.toPx(), 50.dp.toPx())
                lineTo(0f, 50.dp.toPx())
                close()
            }
            val path2 = Path().apply {
                moveTo(offsetPositionX, 50.dp.toPx())
                relativeLineTo(50.dp.toPx(), 0f)
                relativeLineTo(0f, 50.dp.toPx())
                relativeLineTo(-50.dp.toPx(), 0f)
                close()
            }
            drawPath(
                path = path1,
                color = Color.Blue,
                style = Stroke(width = 5.dp.toPx())
            )
            drawPath(
                path = path2,
                color = Color.Green,
                style = Stroke(width = 5.dp.toPx())
            )
            drawRect(
                color = Color.Yellow,
                topLeft = Offset(offsetPositionX + 50.dp.toPx(), 25.dp.toPx()),
                size = DpSize(100.dp, 50.dp).toSize(),
                style = Stroke(width = 5.dp.toPx())
            )
            drawOval(
                color = Color.Red,
                topLeft = Offset(offsetPositionX + 50.dp.toPx(), 25.dp.toPx()),
                size = DpSize(100.dp, 50.dp).toSize()
            )
        }
    }
}

@Composable
private fun CanvasExample(
    title: String,
    content: DrawScope.() -> Unit
) {
    Column(
        modifier = Modifier.width(105.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 11.sp, maxLines = 1)
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray),
            content
        )
    }
}

@Composable
private fun CanvasAPIDemo() {
    Text("基础图形", fontSize = 16.sp, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("drawLine - 直线") {
            drawLine(
                Color.Red,
                Offset.Zero,
                Offset(size.width, size.height),
                5.dp.toPx()
            )
        }
        CanvasExample("drawRect - 矩形") {
            drawRect(
                Color.Blue,
                topLeft = Offset(10.dp.toPx(), 10.dp.toPx()),
                size = Size(80.dp.toPx(), 80.dp.toPx()),
                style = Stroke(width = 5.dp.toPx())
            )
        }
        CanvasExample("drawRoundRect - 圆角矩形") {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE91E63),
                        Color(0xFF9C27B0),
                        Color(0xFF2196F3),
                        Color(0xFF4CAF50)
                    ),
                    startY = 0f,
                    endY = 200f
                ),
//                    Color.Green,
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Fill
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("drawCircle - 圆形") {
            drawCircle(
                Color.Magenta,
                radius = 40.dp.toPx(),
                center = center,
                style = Stroke(width = 5.dp.toPx())
            )
        }
        CanvasExample("drawOval - 椭圆") {
            drawOval(
                Color.Cyan,
                topLeft = Offset(10.dp.toPx(), 20.dp.toPx()),
                size = Size(80.dp.toPx(), 60.dp.toPx())
            )
        }
        CanvasExample("drawArc - 弧形") {
            drawLine(
                Color.Gray,
                Offset.Zero,
                Offset(size.width, size.height),
                1.dp.toPx()
            )
            drawArc(
                Color.Yellow,
                0f,
                270f,
                true,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                Color.Red,
                45f,
                180f,
                false,
                topLeft = Offset(20.dp.toPx(), 20.dp.toPx()),
                size = Size(60.dp.toPx(), 60.dp.toPx()),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                Color.Blue,
                45f,
                180f,
                false,
                topLeft = Offset(20.dp.toPx(), 30.dp.toPx()),
                size = Size(60.dp.toPx(), 40.dp.toPx()),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Butt)
            )
        }
    }

    // Path API 示例
    Text("Path API 示例", fontSize = 16.sp, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("moveTo/lineTo/close") {
            val path = Path().apply {
                moveTo(20.dp.toPx(), 20.dp.toPx())
                lineTo(80.dp.toPx(), 20.dp.toPx())
                lineTo(80.dp.toPx(), 80.dp.toPx())
                lineTo(20.dp.toPx(), 80.dp.toPx())
                close()
            }
            drawPath(path, Color.Blue, style = Stroke(width = 2.dp.toPx()))
        }
        CanvasExample("quadraticBezierTo") {
            val path = Path().apply {
                moveTo(20.dp.toPx(), 50.dp.toPx())
                quadraticBezierTo(50.dp.toPx(), 0.dp.toPx(), 80.dp.toPx(), 50.dp.toPx())
            }
            drawPath(path, Color.Red, style = Stroke(width = 2.dp.toPx()))
        }
        CanvasExample("cubicTo") {
            val path = Path().apply {
                moveTo(20.dp.toPx(), 50.dp.toPx())
                cubicTo(
                    20.dp.toPx(),
                    0.dp.toPx(),
                    80.dp.toPx(),
                    0.dp.toPx(),
                    80.dp.toPx(),
                    50.dp.toPx()
                )
            }
            drawPath(path, Color.Green, style = Stroke(width = 2.dp.toPx()))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .background(Color.LightGray)
        ) {
            val path = Path().apply {
                // 起点：左上角弧起点
                moveTo(20.dp.toPx(), 30.dp.toPx())
                // 绘制左上角弧，顺时针90度
                arcTo(
                    Rect(20.dp.toPx(), 20.dp.toPx(), 40.dp.toPx(), 40.dp.toPx()),
                    180f,
                    90f,
                    false
                )
                // 绘制上边界
                lineTo(70.dp.toPx(), 20.dp.toPx())
                // 绘制右上角弧，逆时针90度
                arcTo(
                    Rect(70.dp.toPx(), 10.dp.toPx(), 90.dp.toPx(), 30.dp.toPx()),
                    180f,
                    -90f,
                    false
                )
                // 绘制右边界
                lineTo(80.dp.toPx(), 50.dp.toPx())
                // 绘制右下角弧，顺时针90度
                arcTo(Rect(60.dp.toPx(), 40.dp.toPx(), 80.dp.toPx(), 60.dp.toPx()), 0f, 90f, false)
                // 绘制下边界
                lineTo(30.dp.toPx(), 60.dp.toPx())
                // 绘制左下角弧，顺时针90度
                arcTo(Rect(20.dp.toPx(), 40.dp.toPx(), 40.dp.toPx(), 60.dp.toPx()), 90f, 90f, false)
                // 绘制左边界
                lineTo(20.dp.toPx(), 30.dp.toPx())
                close()
            }
            drawPath(path, Color.Magenta, style = Stroke(width = 2.dp.toPx()))
            drawCircle(
                Color.Magenta,
                7.dp.toPx(),
                Offset(80.dp.toPx(), 20.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                Color.Magenta,
                Offset(77.dp.toPx(), 17.dp.toPx()),
                Offset(83.dp.toPx(), 23.dp.toPx()),
                1.dp.toPx()
            )
            drawLine(
                Color.Magenta,
                Offset(83.dp.toPx(), 17.dp.toPx()),
                Offset(77.dp.toPx(), 23.dp.toPx()),
                1.dp.toPx()
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("arcTo") {
            drawLine(
                Color.Gray,
                Offset.Zero,
                Offset(size.width, size.height),
                1.dp.toPx()
            )
            drawPath(Path().apply {
                moveTo(20.dp.toPx(), 50.dp.toPx())
                arcTo(
                    rect = Rect(
                        20.dp.toPx(), 20.dp.toPx(),
                        80.dp.toPx(), 80.dp.toPx()
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }, Color.Magenta, style = Stroke(width = 2.dp.toPx()))
            drawPath(Path().apply {
                moveTo(20.dp.toPx(), 20.dp.toPx())
                arcTo(
                    rect = Rect(
                        20.dp.toPx(), 0f,
                        80.dp.toPx(), 40.dp.toPx()
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }, Color.Cyan, style = Stroke(width = 2.dp.toPx()))
            drawPath(Path().apply {
                arcTo(
                    rect = Rect(
                        20.dp.toPx(), 30.dp.toPx(),
                        80.dp.toPx(), 70.dp.toPx()
                    ),
                    startAngleDegrees = 45f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
            }, Color.Yellow, style = Stroke(width = 2.dp.toPx()))
        }
        CanvasExample("addRect/addOval") {
            val path = Path().apply {
                addRect(Rect(20.dp.toPx(), 20.dp.toPx(), 50.dp.toPx(), 50.dp.toPx()))
                addOval(Rect(40.dp.toPx(), 40.dp.toPx(), 80.dp.toPx(), 80.dp.toPx()))
            }
            drawPath(path, Color.Cyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        CanvasExample("relative移动") {
            val path = Path().apply {
                moveTo(20.dp.toPx(), 20.dp.toPx())
                relativeLineTo(30.dp.toPx(), 0f)
                relativeLineTo(0f, 30.dp.toPx())
                relativeLineTo(-30.dp.toPx(), 0f)
                close()
            }
            drawPath(path, Color.DarkGray, style = Stroke(width = 2.dp.toPx()))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("addPath") {
            val path = Path()
            path.addRect(Rect(0.dp.toPx(), 0.dp.toPx(), 25.dp.toPx(), 25.dp.toPx()))
            path.translate(Offset(30.dp.toPx(), 0f))
            path.addOval(Rect(0.dp.toPx(), 0.dp.toPx(), 25.dp.toPx(), 25.dp.toPx()))

            val path2 = Path()
            path2.addPath(path)
            path2.addPath(path, Offset(0f, 30.dp.toPx()))

            drawPath(path2, Color.Red)
            path2.translate(Offset(40.dp.toPx(), 40.dp.toPx()))
            drawPath(path2, Color.Green)
        }
    }

    // 变换操作
    Text("变换操作", fontSize = 16.sp, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("rotate - 旋转") {
            rotate(45f) {
                drawRect(
                    Color.Red,
                    size = Size(60.dp.toPx(), 60.dp.toPx())
                )
            }
        }
        CanvasExample("scale - 缩放") {
            scale(0.5f) {
                drawRect(Color.Blue, size = Size(100.dp.toPx(), 100.dp.toPx()))
            }
        }
        CanvasExample("translate - 平移") {
            translate(30.dp.toPx(), 30.dp.toPx()) {
                drawRect(Color.Green, size = Size(50.dp.toPx(), 50.dp.toPx()))
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("inset - 内缩") {
            inset(20.dp.toPx()) {
                drawRect(Color.Magenta)
            }
        }
        CanvasExample("withTransform") {
            val dp30 = 30.dp.toPx()
            withTransform({
                rotate(45f)
                scale(0.7f)
                translate(dp30, dp30)
            }) {
                drawRect(Color.Red, size = Size(60.dp.toPx(), 60.dp.toPx()))
            }
        }
        CanvasExample("drawPoints - 点") {
            drawPoints(
                points = listOf(
                    Offset(20.dp.toPx(), 20.dp.toPx()),
                    Offset(50.dp.toPx(), 50.dp.toPx()),
                    Offset(80.dp.toPx(), 20.dp.toPx())
                ),
                pointMode = PointMode.Points,
                color = Color.Blue,
                strokeWidth = 10.dp.toPx()
            )
        }
    }

    // 裁剪操作
    Text("裁剪操作", fontSize = 16.sp, fontWeight = FontWeight.Bold)

    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        CanvasExample("clipRect") {
            clipRect(
                25.dp.toPx(),
                25.dp.toPx(),
                75.dp.toPx(),
                75.dp.toPx()
            ) {
                drawCircle(
                    Color.Blue,
                    radius = 85.dp.toPx(),
                    center = Offset.Zero
                )
            }
        }
        CanvasExample("clipPath") {
            val path = Path().apply {
                moveTo(50.dp.toPx(), 0f)
                lineTo(100.dp.toPx(), 50.dp.toPx())
                lineTo(50.dp.toPx(), 100.dp.toPx())
                lineTo(0f, 50.dp.toPx())
                close()
            }
            clipPath(path) {
                drawRect(Color.Green, size = Size(100.dp.toPx(), 100.dp.toPx()))
            }
        }
        CanvasExample("withTransform - matrix") {
            val matrix = Matrix().apply {
                // 先平移到中心点
                translate(50.dp.toPx(), 50.dp.toPx())
                // 旋转45度
                rotateZ(45f)
                // 缩放0.5倍
                scale(0.5f)
                // 再平移回原点
                translate(-50.dp.toPx(), -50.dp.toPx())
            }
            withTransform({
                transform(matrix)
            }) {
                drawRect(
                    color = Color.Red,
                    size = Size(100.dp.toPx(), 100.dp.toPx())
                )
            }
        }
    }
}

private inline fun Float.toRadians(): Float = (this * PI / 180f).toFloat()

private fun DrawScope.point(
    angle: Float,
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    color: Color = Color.Red
) {
    val radians = angle.toRadians()
    val sin = sin(radians)
    val cos = cos(radians)
    drawRect(
        color = color,
        topLeft = center - Offset(1.dp.toPx(), 1.dp.toPx()) + Offset(radiusX * cos, radiusY * sin),
        size = Size(2.dp.toPx(), 2.dp.toPx()),
        style = Fill
    )
}

@Composable
private fun ArcDemo() {
    Canvas(Modifier.size(300.dp).background(Color.LightGray)) {
        drawLine(
            Color.Gray,
            start = Offset.Zero,
            end = Offset(size.width, size.height),
            strokeWidth = 2.dp.toPx()
        )
        val center = Offset(150.dp.toPx(), 150.dp.toPx())
        val radiusX = 130.dp.toPx()
        val radiusY = 100.dp.toPx()
        drawOval(
            Color.Gray,
            topLeft = center - Offset(radiusX, radiusY),
            size = Size(radiusX * 2, radiusY * 2),
            style = Stroke(width = 2.dp.toPx())
        )
        drawArc(
            Color.Cyan,
            startAngle = 45f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = center - Offset(radiusX, radiusY),
            size = Size(radiusX * 2, radiusY * 2),
            style = Stroke(width = 1.dp.toPx())
        )
        drawArc(
            Color.Magenta,
            startAngle = 45f,
            sweepAngle = -180f,
            useCenter = false,
            topLeft = center - Offset(radiusX, radiusY),
            size = Size(radiusX * 2, radiusY * 2),
            style = Stroke(width = 1.dp.toPx())
        )
        for (i in 0..360 step 5) {
            point(i.toFloat(), center, radiusX, radiusY)
        }
        point(45f, center, radiusX, radiusY, Color.Blue)
        point(225f, center, radiusX, radiusY, Color.Blue)
    }
}

@Composable
private fun TranslatePathDemo() {
    Canvas(Modifier.size(300.dp).background(Color.LightGray)) {
        val path = Path()
        path.addRect(Rect(50.dp.toPx(), 50.dp.toPx(), 75.dp.toPx(), 75.dp.toPx()))
        path.translate(Offset(30.dp.toPx(), 0f))
        path.addOval(Rect(50.dp.toPx(), 50.dp.toPx(), 75.dp.toPx(), 75.dp.toPx()))

        val path2 = Path()
        path2.addPath(path)
        path2.addPath(path, Offset(0f, 30.dp.toPx()))

        drawPath(path2, Color.Red)
        path2.translate(Offset(60.dp.toPx(), 60.dp.toPx()))
        drawPath(path2, Color(0x9900FF00))
    }
}