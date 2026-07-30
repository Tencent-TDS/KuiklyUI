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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas 性能压力测试页面。
 *
 * 模拟 Issue #1193 中提到的时间轴刻度尺场景：在单次 draw 回调中执行大量绘制指令
 * （数百次 moveTo/lineTo/stroke 等），用来评估 batchOps 批处理优化的性能效果。
 *
 * 测试项目：
 * 1. 时间轴刻度尺（大量短线段 + 文本标注）—— Issue #1193 的核心场景
 * 2. 密集网格绘制（行列交叉线）
 * 3. 多段贝塞尔曲线（模拟波形/图表）
 * 4. 综合混合场景（线段 + 圆弧 + 填充 + 文本）
 *
 * 每个测试会统计 draw 回调内的 Kotlin 侧执行耗时（毫秒），
 * 可用于对比 batchOps 开启/关闭时的性能差异。
 */
@Page("CanvasPerfTestPage")
internal class CanvasPerfTestPage : BasePager() {

    // ---- 响应式状态 ----
    private var resultText by observable("点击按钮运行对应的 Canvas 性能测试")
    private var isRunning by observable(false)
    // 批量绘制模式开关（默认 false = 非批量）
    private var useBatchDraw by observable(false)

    // 用于触发 Canvas 重绘的计数器
    private var redrawTrigger by observable(0)
    // 当前选中的测试类型
    private var currentTest by observable(0)
    // 上一次绘制耗时
    private var lastDrawTime by observable(0L)

    companion object {
        private const val TAG = "CanvasPerfTest"

        // 时间轴刻度尺参数
        private const val RULER_TOTAL_SECONDS = 3600  // 1小时，共3600个刻度
        private const val RULER_MAJOR_INTERVAL = 60   // 大刻度间隔（每分钟）
        private const val RULER_MINOR_INTERVAL = 10   // 中刻度间隔（每10秒）

        // 网格参数
        private const val GRID_ROWS = 100
        private const val GRID_COLS = 50

        // 曲线参数
        private const val CURVE_POINTS = 500

        // 综合场景参数
        private const val MIX_CIRCLES = 50
        private const val MIX_LINES = 200
        private const val MIX_TEXTS = 30
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Canvas 性能测试" } }
            List {
                attr { flex(1f) }

                // 说明区域
                View {
                    attr {
                        margin(left = 16f, top = 12f, right = 16f, bottom = 8f)
                    }
                    Text {
                        attr {
                            text("此页面用于量化 Canvas 绘制性能。\n每个测试在单次 draw 回调中执行大量绘制指令，统计 Kotlin 侧执行耗时。\n可通过【批量模式】按钮切换 batchDraw 开/关，对比两种模式的性能差异。")
                            fontSize(13f)
                            color(Color(0xFF666666))
                        }
                    }
                }

                // Canvas 绘制区域
                Canvas(
                    {
                        attr {
                            margin(left = 16f, right = 16f, top = 8f)
                            height(250f)
                            backgroundColor(Color(0xFFF8F8F8))
                        }
                    }
                ) { context, width, height ->
                    // 触发依赖追踪
                    val trigger = ctx.redrawTrigger
                    val test = ctx.currentTest

                    // 根据标志位控制批量模式
                    context.batchDraw = ctx.useBatchDraw

                    val begin = DateTime.currentTimestamp()
                    when (test) {
                        1 -> ctx.drawTimelineRuler(context, width, height)
                        2 -> ctx.drawDenseGrid(context, width, height)
                        3 -> ctx.drawBezierWaveform(context, width, height)
                        4 -> ctx.drawMixedScene(context, width, height)
                        else -> ctx.drawPlaceholder(context, width, height)
                    }
                    val duration = DateTime.currentTimestamp() - begin
                    if (test > 0) {
                        ctx.lastDrawTime = duration
                        KLog.i(TAG, "Test $test draw time: ${duration}ms (trigger=$trigger)")
                    }
                }

                // 测试按钮组
                View {
                    attr {
                        margin(left = 16f, right = 16f, top = 12f)
                        flexDirectionRow()
                        flexWrapWrap()
                        justifyContentSpaceBetween()
                    }
                    ctx.testButton(this, "① 时间轴刻度尺\n(${RULER_TOTAL_SECONDS}刻度)", 1)
                    ctx.testButton(this, "② 密集网格\n(${GRID_ROWS}×${GRID_COLS})", 2)
                    ctx.testButton(this, "③ 贝塞尔曲线\n(${CURVE_POINTS}点)", 3)
                    ctx.testButton(this, "④ 综合混合\n(多类型)", 4)
                }

                // 批量模式切换按钮
                View {
                    attr {
                        margin(left = 16f, right = 16f, top = 8f)
                        alignItemsCenter()
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("批量模式: ${if (ctx.useBatchDraw) "ON ✓" else "OFF"}")
                                color(Color.WHITE)
                                fontSize(14f)
                            }
                            backgroundColor(if (ctx.useBatchDraw) Color(0xFF34C759) else Color(0xFF8E8E93))
                            size(width = 300f, height = 40f)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                ctx.useBatchDraw = !ctx.useBatchDraw
                                // 触发重绘以应用新模式
                                if (ctx.currentTest > 0) ctx.redrawTrigger++
                            }
                        }
                    }
                }

                // 连续重绘测试按钮
                View {
                    attr {
                        margin(left = 16f, right = 16f, top = 12f)
                        alignItemsCenter()
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.isRunning) "测试中..." else "连续重绘 20 次（模拟拖拽）")
                                color(Color.WHITE)
                                fontSize(14f)
                            }
                            backgroundColor(if (ctx.isRunning) Color.GRAY else Color(0xFFFF6B35))
                            size(width = 300f, height = 44f)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                if (!ctx.isRunning && ctx.currentTest > 0) {
                                    ctx.isRunning = true
                                    ctx.resultText = "连续重绘 20 次，请稍候..."
                                    val totalBegin = DateTime.currentTimestamp()
                                    var totalDrawTime = 0L
                                    for (i in 0 until 20) {
                                        ctx.redrawTrigger++
                                        totalDrawTime += ctx.lastDrawTime
                                    }
                                    val totalDuration = DateTime.currentTimestamp() - totalBegin
                                    ctx.resultText = buildString {
                                        append("连续重绘 20 次\n")
                                        append("总耗时: ${totalDuration}ms\n")
                                        append("平均每帧绘制: ${totalDrawTime / 20}ms\n")
                                        append("模式: ${if (ctx.useBatchDraw) "批量模式 (batchDraw ON)" else "非批量模式 (batchDraw OFF)"}")
                                    }
                                    ctx.isRunning = false
                                }
                            }
                        }
                    }
                }

                // 结果展示
                View {
                    attr {
                        margin(left = 16f, top = 16f, right = 16f, bottom = 16f)
                        backgroundColor(Color(0xFFF0F7FF))
                        borderRadius(8f)
                        padding(all = 16f)
                    }
                    Text {
                        attr {
                            text(ctx.resultText)
                            fontSize(14f)
                            color(Color(0xFF333333))
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建测试按钮的辅助方法
     */
    private fun testButton(container: ViewContainer<*, *>, label: String, testId: Int) {
        val ctx = this
        container.Button {
            attr {
                titleAttr {
                    text(label)
                    color(Color.WHITE)
                    fontSize(12f)
                }
                backgroundColor(
                    if (ctx.currentTest == testId) Color(0xFF007AFF)
                    else Color(0xFF5AC8FA)
                )
                size(width = 165f, height = 56f)
                borderRadius(8f)
                marginBottom(8f)
            }
            event {
                click {
                    ctx.currentTest = testId
                    ctx.redrawTrigger++
                    ctx.resultText = buildString {
                        append("测试 $testId 单帧绘制耗时: ${ctx.lastDrawTime}ms\n")
                        append("模式: ${if (ctx.useBatchDraw) "批量模式 (batchDraw ON)" else "非批量模式 (batchDraw OFF)"}")
                    }
                }
            }
        }
    }

    // ========================
    //  测试 1: 时间轴刻度尺
    // ========================
    /**
     * 模拟 Issue #1193 中描述的可拖拽时间轴刻度尺场景。
     * 绘制 [RULER_TOTAL_SECONDS] 个刻度线 + 主刻度文字标注。
     * 这是 batchOps 优化的主要受益场景。
     *
     * 典型指令数量统计：
     * - 每个刻度: beginPath + moveTo + lineTo + strokeStyle + lineWidth + stroke = 6 次
     * - 文字标注: font + fillStyle + fillText = 3 次
     * - 总计约: 3600 × 6 + 60 × 3 ≈ 21,780 次桥调用（优化前）
     */
    private fun drawTimelineRuler(context: CanvasContext, width: Float, height: Float) {
        val pixelsPerSecond = width / 120f  // 可见区域显示120秒
        val rulerY = height * 0.5f
        val majorTickHeight = 30f
        val minorTickHeight = 15f
        val tinyTickHeight = 8f

        // 背景线
        context.beginPath()
        context.strokeStyle(Color(0xFFCCCCCC))
        context.lineWidth(1f)
        context.moveTo(0f, rulerY)
        context.lineTo(width, rulerY)
        context.stroke()

        // 绘制所有刻度
        for (i in 0 until RULER_TOTAL_SECONDS) {
            val x = (i % 120) * pixelsPerSecond  // 循环绘制在可见区域

            context.beginPath()
            when {
                i % RULER_MAJOR_INTERVAL == 0 -> {
                    // 大刻度（每分钟）
                    context.strokeStyle(Color(0xFF333333))
                    context.lineWidth(2f)
                    context.moveTo(x, rulerY - majorTickHeight)
                    context.lineTo(x, rulerY + majorTickHeight)
                    context.stroke()

                    // 时间文字
                    val minutes = i / 60
                    context.font(12f)
                    context.fillStyle(Color(0xFF333333))
                    context.fillText("${minutes}m", x, rulerY + majorTickHeight + 15f)
                }
                i % RULER_MINOR_INTERVAL == 0 -> {
                    // 中刻度（每10秒）
                    context.strokeStyle(Color(0xFF999999))
                    context.lineWidth(1f)
                    context.moveTo(x, rulerY - minorTickHeight)
                    context.lineTo(x, rulerY + minorTickHeight)
                    context.stroke()
                }
                else -> {
                    // 小刻度（每秒）
                    context.strokeStyle(Color(0xFFCCCCCC))
                    context.lineWidth(0.5f)
                    context.moveTo(x, rulerY - tinyTickHeight)
                    context.lineTo(x, rulerY + tinyTickHeight)
                    context.stroke()
                }
            }
        }

        // 当前位置指示器
        context.beginPath()
        context.strokeStyle(Color(0xFFFF0000))
        context.lineWidth(2f)
        context.moveTo(width / 2, 0f)
        context.lineTo(width / 2, height)
        context.stroke()
    }

    // ========================
    //  测试 2: 密集网格
    // ========================
    /**
     * 绘制 [GRID_ROWS]×[GRID_COLS] 的密集网格。
     * 每条线: beginPath + moveTo + lineTo + stroke = 4 次
     * 总计: (100 + 50) × 4 = 600 次桥调用
     * 加上样式设置，总计约 700+ 次。
     */
    private fun drawDenseGrid(context: CanvasContext, width: Float, height: Float) {
        val cellWidth = width / GRID_COLS
        val cellHeight = height / GRID_ROWS

        // 水平线
        context.strokeStyle(Color(0xFFDDDDDD))
        context.lineWidth(0.5f)
        for (i in 0..GRID_ROWS) {
            val y = i * cellHeight
            context.beginPath()
            context.moveTo(0f, y)
            context.lineTo(width, y)
            context.stroke()
        }

        // 垂直线
        for (i in 0..GRID_COLS) {
            val x = i * cellWidth
            context.beginPath()
            context.moveTo(x, 0f)
            context.lineTo(x, height)
            context.stroke()
        }

        // 每10行/列加粗
        context.strokeStyle(Color(0xFF999999))
        context.lineWidth(1.5f)
        for (i in 0..GRID_ROWS step 10) {
            val y = i * cellHeight
            context.beginPath()
            context.moveTo(0f, y)
            context.lineTo(width, y)
            context.stroke()
        }
        for (i in 0..GRID_COLS step 10) {
            val x = i * cellWidth
            context.beginPath()
            context.moveTo(x, 0f)
            context.lineTo(x, height)
            context.stroke()
        }

        // 标注行列号
        context.font(8f)
        context.fillStyle(Color(0xFF666666))
        for (i in 0..GRID_ROWS step 10) {
            context.fillText("$i", 2f, i * cellHeight + 8f)
        }
        for (i in 10..GRID_COLS step 10) {
            context.fillText("$i", i * cellWidth + 2f, 10f)
        }
    }

    // ========================
    //  测试 3: 密集贝塞尔曲线
    // ========================
    /**
     * 绘制多段贝塞尔曲线模拟波形图。
     * 包含 [CURVE_POINTS] 个采样点的三条重叠波形。
     * 每条曲线: beginPath + N×(quadraticCurveTo) + strokeStyle + lineWidth + stroke
     * 总指令约: 3 × (2 + 500 + 3) ≈ 1515 次
     */
    private fun drawBezierWaveform(context: CanvasContext, width: Float, height: Float) {
        val midY = height / 2f
        val amplitude = height * 0.35f
        val step = width / CURVE_POINTS

        // 三条不同频率的波形
        val waves = listOf(
            Triple(Color(0xFFFF4444), 2f, 1.0f),   // 红色，低频
            Triple(Color(0xFF44AA44), 1.5f, 2.5f),  // 绿色，中频
            Triple(Color(0xFF4444FF), 1f, 5.0f)      // 蓝色，高频
        )

        for ((color, lineW, freq) in waves) {
            context.beginPath()
            context.strokeStyle(color)
            context.lineWidth(lineW)

            val startX = 0f
            val startY = midY + amplitude * sin(0.0).toFloat()
            context.moveTo(startX, startY)

            for (i in 1 until CURVE_POINTS) {
                val x = i * step
                val y = midY + amplitude * sin(freq * 2.0 * PI * i / CURVE_POINTS).toFloat()
                val prevX = (i - 1) * step
                val prevY = midY + amplitude * sin(freq * 2.0 * PI * (i - 1) / CURVE_POINTS).toFloat()
                val cpX = (prevX + x) / 2f
                val cpY = (prevY + y) / 2f
                context.quadraticCurveTo(prevX, prevY, cpX, cpY)
            }
            context.stroke()
        }

        // X轴
        context.beginPath()
        context.strokeStyle(Color(0xFF000000))
        context.lineWidth(1f)
        context.moveTo(0f, midY)
        context.lineTo(width, midY)
        context.stroke()
    }

    // ========================
    //  测试 4: 综合混合场景
    // ========================
    /**
     * 混合使用各种 Canvas API：圆弧、填充矩形、线段、文本。
     * 覆盖更多 API 路径，模拟真实业务中复合图形绘制场景。
     *
     * 指令统计:
     * - [MIX_CIRCLES] 个圆弧: 每个约 8 次 (beginPath + arc + closePath + fillStyle + fill + strokeStyle + lineWidth + stroke)
     * - [MIX_LINES] 条线段: 每条约 6 次
     * - [MIX_TEXTS] 个文本: 每个约 4 次
     * - 总计约: 50×8 + 200×6 + 30×4 = 1720 次
     */
    private fun drawMixedScene(context: CanvasContext, width: Float, height: Float) {
        // 填充圆弧
        for (i in 0 until MIX_CIRCLES) {
            val cx = (i * 37 % width.toInt()).toFloat() + 10f
            val cy = (i * 53 % height.toInt()).toFloat() + 10f
            val r = 5f + (i % 10) * 2f
            val hue = (i * 7) % 360

            context.beginPath()
            context.arc(cx, cy, r, 0f, (2 * PI).toFloat(), false)
            context.closePath()
            context.fillStyle(hsvToColor(hue, 0.7f, 0.9f))
            context.fill()
            context.strokeStyle(Color(0xFF000000))
            context.lineWidth(0.5f)
            context.stroke()
        }

        // 随机线段
        context.lineWidth(1f)
        for (i in 0 until MIX_LINES) {
            val x1 = (i * 17 % width.toInt()).toFloat()
            val y1 = (i * 31 % height.toInt()).toFloat()
            val x2 = ((i * 17 + 50) % width.toInt()).toFloat()
            val y2 = ((i * 31 + 30) % height.toInt()).toFloat()
            val alpha = 0.2f + (i % 5) * 0.15f

            context.beginPath()
            context.strokeStyle(Color(0x336699, alpha))
            context.moveTo(x1, y1)
            context.lineTo(x2, y2)
            context.stroke()
        }

        // 文本标注
        context.font(10f)
        for (i in 0 until MIX_TEXTS) {
            val x = (i * 43 % (width - 50).toInt()).toFloat() + 5f
            val y = (i * 67 % (height - 15).toInt()).toFloat() + 12f
            context.fillStyle(Color(0xFF333333))
            context.fillText("P$i", x, y)
        }
    }

    // ========================
    //  占位绘制
    // ========================
    private fun drawPlaceholder(context: CanvasContext, width: Float, height: Float) {
        context.font(16f)
        context.fillStyle(Color(0xFF999999))
        context.fillText("请点击上方按钮选择测试项", width / 2f - 100f, height / 2f)
    }

    /**
     * 简易 HSV 转 Color 工具方法
     */
    private fun hsvToColor(hDeg: Int, s: Float, v: Float): Color {
        val h = hDeg / 60f
        val i = h.toInt() % 6
        val f = h - h.toInt()
        val p = v * (1 - s)
        val q = v * (1 - s * f)
        val t = v * (1 - s * (1 - f))
        val (r, g, b) = when (i) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return Color(
            (r * 255).toInt() shl 16 or
            ((g * 255).toInt() shl 8) or
            (b * 255).toInt()
        )
    }
}
