package com.tencent.kuikly.demo.pages.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.PathEffect
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 官方 Jetpack Compose 5 场景虚线验证的 Kuikly 移植版。
 * 官方原工程：/Users/zhaozining/CodeBuddy/20260615095947/DashedLineVerify/MainActivity.kt
 *
 * - 5 个场景官方代码原样移植（仅 import androidx→com.tencent.kuikly），全部走 drawBehind+pathEffect 通道。
 * - 场景 1/3/5：drawBehind 整行/多形态虚线 + 实线对照，1:1 对齐。
 * - 场景 2/4：依赖 TextLayoutResult.getBoundingBox / getLineBottom / lineCount，
 *   已通过 native StaticLayout 行度量桥接（KRRichTextView.call lineMetrics/getBoundingBox）
 *   回填到 MultiParagraph，官方写法可直接编译运行，1:1 对齐。
 */
@Page("DashedUnderlineDemo")
class DashedUnderlineDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                item {
                    Text(
                        "Jetpack Compose 虚线验证（Kuikly 移植版）",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ==================== 场景1：Text + drawBehind 整行虚线 ====================
                item {
                    Text("场景1: Text + drawBehind 整行虚线（官方 1:1 对齐）", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    DashedUnderline_Text()
                    Spacer(Modifier.height(24.dp))
                }

                // ==================== 场景2：纯 Text + drawBehind 只画局部虚线 ====================
                item {
                    Text("场景2: 纯 Text 局部虚线（官方 1:1 对齐）", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    DashedUnderline_Text_Span()
                    Spacer(Modifier.height(24.dp))
                }

                // ==================== 场景3：实线下划线对照（TextDecoration.Underline） ====================
                item {
                    Text("场景3: 实线下划线对照（TextDecoration.Underline，已对齐）", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "这是整行实线下划线（Underline）",
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedString {
                            append("普通文字，")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("这里是 Span 实线下划线")
                            }
                            append("。")
                        }
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // ==================== 场景4：多行折行文本逐行虚线 ====================
                item {
                    Text("场景4: 多行折行文本逐行虚线（官方 1:1 对齐）", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    DashedUnderline_Text_MultiLine()
                    Spacer(Modifier.height(24.dp))
                }

                // ==================== 场景5：不同线宽 / 疏密间隔的虚线 ====================
                item {
                    Text("场景5: 不同线宽 / 疏密间隔的虚线对比（官方 1:1 对齐）", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    DashedUnderline_Text_Pattern(8.dp, 4.dp, 1.dp, Color.Red, "细虚线 8-4")
                    Spacer(Modifier.height(8.dp))
                    DashedUnderline_Text_Pattern(12.dp, 6.dp, 2.dp, Color.Blue, "中虚线 12-6")
                    Spacer(Modifier.height(8.dp))
                    DashedUnderline_Text_Pattern(16.dp, 8.dp, 3.dp, Color.Green, "粗虚线 16-8")
                }
            }
        }
    }
}

/**
 * 场景1：Text 组件 + drawBehind 直接画虚线（官方写法原样移植，仅换 import）。
 */
@Composable
fun DashedUnderline_Text() {
    Text(
        text = "这段文字下方有红色虚线（Text + drawBehind）",
        modifier = Modifier.drawBehind {
            drawLine(
                color = Color.Red,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(8f, 4f),
                    phase = 0f
                )
            )
        }
    )
}

/**
 * 通用：把一段文字里 [spanStart, spanEnd) 这截高亮，并用 drawBehind 只在该截下方画虚线。
 * 关键点：onTextLayout 拿到 span 的包围盒（Rect），drawBehind 只在包围盒内画线。
 * 官方写法原样移植（仅换 import）。
 */
@Composable
private fun SpanDashedText(
    full: String,
    spanStart: Int,
    spanEnd: Int,
    color: Color,
    usePathEffect: Boolean
) {
    var spanRect by remember { mutableStateOf<Rect?>(null) }

    Text(
        text = buildAnnotatedString {
            append(full.substring(0, spanStart))
            append(full.substring(spanStart, spanEnd))
            append(full.substring(spanEnd))
        },
        onTextLayout = { result ->
            val start = result.getBoundingBox(spanStart)
            val end = result.getBoundingBox(spanEnd - 1)
            spanRect = Rect(start.left, start.top, end.right, start.bottom)
        },
        modifier = Modifier.drawBehind {
            spanRect?.let { rect ->
                val y = rect.bottom
                val strokeWidth = 1.dp.toPx()
                if (usePathEffect) {
                    drawLine(
                        color = color,
                        start = Offset(rect.left, y),
                        end = Offset(rect.right, y),
                        strokeWidth = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                    )
                } else {
                    val dashWidth = 8.dp.toPx()
                    val gapWidth = 4.dp.toPx()
                    var startX = rect.left
                    while (startX < rect.right) {
                        val endX = minOf(startX + dashWidth, rect.right)
                        drawLine(
                            color = color,
                            start = Offset(startX, y),
                            end = Offset(endX, y),
                            strokeWidth = strokeWidth
                        )
                        startX += dashWidth + gapWidth
                    }
                }
            }
        }
    )
}

/**
 * 场景2：纯 Text + drawBehind 最干净写法，只在局部画虚线（官方写法原样移植，仅换 import）。
 */
@Composable
fun DashedUnderline_Text_Span() {
    SpanDashedText(
        full = "这是一段示例文字，纯 Text 方案只在局部画虚线",
        spanStart = 9,
        spanEnd = 11,
        color = Color.Red,
        usePathEffect = true
    )
}

/**
 * 场景4：多行折行文本的虚线。通过 onTextLayout 拿到每一行的底边坐标，
 * 逐行用 drawBehind 画虚线（官方写法原样移植，仅换 import）。
 */
@Composable
fun DashedUnderline_Text_MultiLine() {
    var lineBottoms by remember { mutableStateOf<List<Float>>(emptyList()) }
    Text(
        text = "这是一段会换行的长文本，用来验证多行文本时虚线下划线是否每行都正确画出，而不是只在最底部画一条横线。",
        onTextLayout = { result ->
            lineBottoms = (0 until result.lineCount).map { result.getLineBottom(it) }
        },
        modifier = Modifier.drawBehind {
            val strokeWidth = 1.dp.toPx()
            lineBottoms.forEach { y ->
                drawLine(
                    color = Color.Magenta,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            }
        }
    )
}

/**
 * 场景5：自定义虚线形态（官方写法原样移植，仅换 import）。传入不同线段长/间隔/线宽验证 pathEffect 可调。
 */
@Composable
fun DashedUnderline_Text_Pattern(dash: Dp, gap: Dp, stroke: Dp, color: Color, caption: String) {
    Text(caption, fontSize = 12.sp, color = Color.Gray)
    Text(
        text = "这段文字下方是 $caption 的虚线",
        modifier = Modifier.drawBehind {
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = stroke.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dash.toPx(), gap.toPx()),
                    phase = 0f
                )
            )
        }
    )
}
