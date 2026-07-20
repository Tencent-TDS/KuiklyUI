@file:OptIn(InternalResourceApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.PathEffect
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * drawBehind 通用组件验证 + 专项测试 Demo。
 *
 * 验证 Modifier.drawBehind 在 Box / Row / Column / Image / LazyColumn item
 * 等非 CanvasView 宿主上的背景绘制，全部采用官方 Compose 写法：
 * 在 DrawScope.size 范围内绘制，搭配 PathEffect 控制线型。
 */
@Page("DrawBehindDemo")
class DrawBehindDemo : ComposeContainer() {
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
                        "drawBehind 通用组件验证",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ---------- Box ----------
                item {
                    SectionTitle("Box + drawBehind（半透明填充 + 虚线边框）")
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .drawBehind {
                                drawRect(color = Color(0x330000FF))
                                val stroke = 2.dp.toPx()
                                val pe = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                drawLine(Color.Blue, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = stroke, pathEffect = pe)
                                drawLine(Color.Blue, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = stroke, pathEffect = pe)
                                drawLine(Color.Blue, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = stroke, pathEffect = pe)
                                drawLine(Color.Blue, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = stroke, pathEffect = pe)
                            }
                    ) {
                        Text("Box", Modifier.align(Alignment.Center))
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ---------- Row ----------
                item {
                    SectionTitle("Row + drawBehind（底部虚线）")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = Color.Red,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                )
                            }
                    ) {
                        Text("Row item 1")
                        Spacer(Modifier.width(16.dp))
                        Text("Row item 2")
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ---------- Column ----------
                item {
                    SectionTitle("Column + drawBehind（右侧实线）")
                    Column(
                        modifier = Modifier
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFF4CAF50),
                                    start = Offset(size.width, 0f),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                    ) {
                        Text("Column item 1")
                        Text("Column item 2")
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ---------- Image ----------
                item {
                    SectionTitle("Image + drawBehind（背景色 + 底部虚线）")
                    Image(
                        painter = painterResource(
                            DrawableResource(
                                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="
                            )
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .drawBehind {
                                drawRect(color = Color(0x33FF9800))
                                drawLine(
                                    color = Color(0xFFFF9800),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                )
                            }
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // ---------- LazyColumn item ----------
                item {
                    SectionTitle("LazyColumn item + drawBehind（item 底部虚线分隔）")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = Color(0xFF9C27B0),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                                )
                            }
                    ) {
                        Text(
                            "模拟 LazyColumn item（底部虚线分隔）",
                            Modifier.padding(vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }

                // ==================== 专项测试：多 pathEffect 对比 ====================
                item {
                    Text(
                        "专项测试：多 pathEffect 对比",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                }

                item {
                    PathEffectSample("实线（无 pathEffect）", null)
                    Spacer(Modifier.height(8.dp))
                    PathEffectSample("虚线 8-4", floatArrayOf(8f, 4f))
                    Spacer(Modifier.height(8.dp))
                    PathEffectSample("虚线 16-8", floatArrayOf(16f, 8f))
                    Spacer(Modifier.height(8.dp))
                    PathEffectSample("点线 2-4", floatArrayOf(2f, 4f))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF666666))
    Spacer(Modifier.height(4.dp))
}

/**
 * 专项测试：在 Box 上用不同 pathEffect 画底部线，对比线型差异。
 */
@Composable
private fun PathEffectSample(label: String, intervals: FloatArray?) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .drawBehind {
                    drawLine(
                        color = Color(0xFF2196F3),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = intervals?.let { PathEffect.dashPathEffect(it, 0f) }
                    )
                }
        )
    }
}
