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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BorderRectRadius
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Blur
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Created by kam on 2023/9/14.
 */
@Page("WeatherCanvasPage")
internal class WeatherCanvasPage : BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flex(1f)
                flexDirectionColumn()
                backgroundColor(Color(0x436082, 1f))
            }

            Image {
                attr {
                    src("https://qq-weather.cdn-go.cn/weather/latest/rain-bg/rain-comming.png")
                    positionAbsolute() //设置为绝对布局
                    // 设置在父亲中的位置
                    left(0f)
                    right(0f)
                    top(0f)
                    bottom(0f)
                    backgroundColor(Color.BLACK)
                }
            }

            RainFallNodeCanvas {
                attr {
                    positionAbsolute()
                    top(178.5f)
                    left(20f)
                }
            }
        }
    }
}

class RainFallNodeAttr : ComposeAttr() {
    var nodes: List<Float>  by observable(listOf(1.3f, 2.6f, 4.2f, 5.1f, 3.5f, 1.9f, 1.0f, 1.8f, 2.6f, 3.4f, 4.2f, 5.0f, 5.8f, 6.6f, 7.4f, 8.2f, 9.0f, 9.8f, 9.9f, 0.5f, 3.3f, 7.1f, 8.6f, 5.7f))
    var title: String by observable("雨渐大，30分钟后雨会停，40分钟后会下大雨；")

    var dashLine1Hidden: Boolean by observable(false)
    var dashLine2Hidden: Boolean by observable(false)
    var dashLine3Hidden: Boolean by observable(false)
    var dashLine4Hidden: Boolean by observable(false)

    var rainLevel1Title: String by observable("暴雨")
    var rainLevel2Title: String by observable("大雨")
    var rainLevel3Title: String by observable("中雨")
    var rainLevel4Title: String by observable("小雨")
}

class RainFallPoint(val x: Float, val y: Float)

class WeatherRainFallNodeCanvas : ComposeView<RainFallNodeAttr, ComposeEvent>() {
    private val nodeAttr = RainFallNodeAttr()

    private var dashLine1Y = 0f
    private var dashLine2Y = 0f
    private var dashLine3Y = 0f
    private var dashLine4Y = 0f
    private val canvasTop = 55f

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                size(335f, 250f)
               // backgroundColor(Color(0xffffff, 0.3f))
                borderRadius(BorderRectRadius(16f, 16f, 16f, 16f))
            }

            Blur {
                attr {
                    absolutePosition(0f, 0f, 0f,0f)
                    blurRadius(10.0f)
                }
            }

            View {
                attr {
                    size(335f, 12f)
                }
            }

            View {
                attr {
                    size(335f, 17f)
                    allCenter()
                }
                Text {
                    attr {
                        fontSize(12f)
                        fontWeightMedium()
                        color(Color.WHITE)
                        text(ctx.nodeAttr.title)
                    }
                }
            }

            Canvas(
                {
                    attr {
                        absolutePosition(ctx.canvasTop, 0f, 24f, 0f)
                    }
                }
            ) { context, width, height ->
                context.beginPath()
                context.strokeStyle(Color(0x0085FF, 1f))
                context.lineWidth(5.0f)
                context.moveTo(0f, height)
                context.lineCapRound()

//                var xGap = width / 24
                var lastPoint = RainFallPoint(0f, height)
                var nextPoint = RainFallPoint(0f, 0f)
                for((index, node) in ctx.nodeAttr.nodes.withIndex()) {
                    var x = index * width / 24
                    var y = height - node * height / 10
                    if (index >= 23) {
                        //最后一个点的重点就是这个点，控制点再取一次中
                        nextPoint = RainFallPoint(x, y)
                        val controlPoint = ctx.getMidPoint(lastPoint, nextPoint)
                        context.quadraticCurveTo(controlPoint.x, controlPoint.y, nextPoint.x, nextPoint.y)
                    }
                    else {
                        // 正常的控制点是node，终点是下一个点
                        var controlPoint = RainFallPoint(x, y)
                        x = (index + 1) * width / 24
                        y = height - ctx.nodeAttr.nodes[index + 1] * height / 10
                        val nextNode = RainFallPoint(x, y)

                        if (index == 0) {
                            // 第一次画 node0到node0+node1/2
                            nextPoint = ctx.getMidPoint(lastPoint, nextNode)
                            controlPoint = ctx.getMidPoint(lastPoint, nextPoint)
                        }
                        else {
                            // 正常下一个点是该点和下一个点的中间
                            nextPoint = ctx.getMidPoint(controlPoint, nextNode)
                        }

                        context.quadraticCurveTo(controlPoint.x, controlPoint.y, nextPoint.x, nextPoint.y)
                    }

                    context.moveTo(nextPoint.x, nextPoint.y)
                    lastPoint = nextPoint
                }
//                context.lineTo(width, 0f)
                context.stroke()

                //画虚线
                var dashLineY = 0f
                if (!ctx.nodeAttr.dashLine1Hidden) {
                    ctx.renderDashLine(context, 315f, RainFallPoint(10f, dashLineY))
                    ctx.dashLine1Y = dashLineY
                }
                if (!ctx.nodeAttr.dashLine2Hidden) {
                    dashLineY += 48
                    ctx.renderDashLine(context, 315f, RainFallPoint(10f, dashLineY))
                    ctx.dashLine2Y = dashLineY
                }
                if (!ctx.nodeAttr.dashLine3Hidden) {
                    dashLineY += 48
                    ctx.renderDashLine(context, 315f, RainFallPoint(10f, dashLineY))
                    ctx.dashLine3Y = dashLineY
                }
                if (!ctx.nodeAttr.dashLine4Hidden) {
                    dashLineY += 48
                    ctx.renderDashLine(context, 315f, RainFallPoint(10f, dashLineY))
                    ctx.dashLine4Y = dashLineY
                }

                dashLineY += 16
                ctx.renderXAxis(context, 315f, RainFallPoint(10f, dashLineY))
            }

            Text {
                attr {
                    positionAbsolute()
                    left(10f)
                    top(ctx.canvasTop - 13f)
                    text(ctx.nodeAttr.rainLevel1Title)
                    fontSize(10f)
                    color(Color(0xffffff, 0.5f))
                }
            }

            Text {
                attr {
                    positionAbsolute()
                    left(10f)
                    // 间距1，字高12
                    top(ctx.canvasTop + 48f - 13f)
                    text(ctx.nodeAttr.rainLevel2Title)
                    fontSize(10f)
                    color(Color(0xffffff, 0.5f))
                }
            }

            Text {
                attr {
                    positionAbsolute()
                    left(10f)
                    top(ctx.canvasTop + 96f - 13f)
                    text(ctx.nodeAttr.rainLevel3Title)
                    fontSize(10f)
                    color(Color(0xffffff, 0.5f))
                }
            }

            Text {
                attr {
                    positionAbsolute()
                    left(10f)
                    top(ctx.canvasTop + 154f - 13f)
                    text(ctx.nodeAttr.rainLevel4Title)
                    fontSize(10f)
                    color(Color(0xffffff, 0.5f))
                }
            }

            View {
                attr {
                    absolutePosition(220f, 10f, 17f, 10f)
                }

                Text {
                    attr {
                        positionAbsolute()
                        left(30f)
                        text("现在")
                        fontSize(10f)
                        color(Color(0xffffff, 0.5f))
                    }
                }

                Text {
                    attr {
                        positionAbsolute()
                        left(103f)
                        text("30分钟")
                        fontSize(10f)
                        color(Color(0xffffff, 0.5f))
                    }
                }

                Text {
                    attr {
                        positionAbsolute()
                        left(185f)
                        text("60分钟")
                        fontSize(10f)
                        color(Color(0xffffff, 0.5f))
                    }
                }

                Text {
                    attr {
                        positionAbsolute()
                        left(259f)
                        text("90分钟")
                        fontSize(10f)
                        color(Color(0xffffff, 0.5f))
                    }
                }
            }
        }

    }

    fun getMidPoint(p1: RainFallPoint, p2: RainFallPoint): RainFallPoint {
        val x = (p1.x + p2.x) / 2f
        val y = (p1.y + p2.y) / 2f
        return RainFallPoint(x, y)
    }

    private fun renderDashLine(context: CanvasContext, width: Float, startPoint: RainFallPoint) {
        // 开始画虚线
        context.beginPath()
        context.strokeStyle(Color(0xffffff, 0.2f))
        context.lineWidth(0.5f)
        var dashLineX = startPoint.x
        var dashLineY = startPoint.y
        context.moveTo(dashLineX, dashLineY)
        context.lineCapRound()
//        dashLineY += 2
//        context.lineTo(dashLineX, dashLineY)
//        dashLineY += 1.5f
//        context.moveTo(dashLineX, dashLineY)
//        val top = dashLineY
//        val bottom = height - (dashLineY)

        val shortLineLen = 5f
        val spaceLen = 2.5f
        for (i in 0 until (width / (shortLineLen + spaceLen)).toInt()) {
            dashLineX += shortLineLen
            context.lineTo(dashLineX, dashLineY)
            dashLineX += spaceLen
            context.moveTo(dashLineX, dashLineY)
        }
        context.stroke()
    }

    private fun renderXAxis(context: CanvasContext, width: Float, startPoint: RainFallPoint) {
        context.beginPath()
        context.strokeStyle(Color(0xffffff, 0.5f))
        context.lineWidth(0.5f)

        val verticalLineMargin = width / 4f
        context.moveTo(startPoint.x, startPoint.y)

        var currentX = startPoint.x
        this.renderXAxisVerticalLine(context, RainFallPoint(currentX, startPoint.y))
        for (i in 0 until 4) {
            currentX += verticalLineMargin
            context.lineTo(currentX, startPoint.y)
            this.renderXAxisVerticalLine(context, RainFallPoint(currentX, startPoint.y))
        }
        context.stroke()
    }

    private fun renderXAxisVerticalLine(context: CanvasContext, startPoint: RainFallPoint) {
        context.moveTo(startPoint.x, startPoint.y)
        context.lineTo(startPoint.x, startPoint.y - 4f)
        context.moveTo(startPoint.x, startPoint.y)
    }

    override fun createAttr(): RainFallNodeAttr {
        return nodeAttr
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.RainFallNodeCanvas(init: WeatherRainFallNodeCanvas.() -> Unit) {
    addChild(WeatherRainFallNodeCanvas(), init)
}