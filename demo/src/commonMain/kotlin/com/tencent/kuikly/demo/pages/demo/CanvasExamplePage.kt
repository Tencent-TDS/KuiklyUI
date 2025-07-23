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

package com.tencent.kuiklydemo.pages.demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.CanvasContext
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI
import kotlin.math.cos

@Page("CanvasExamplePage")
internal class CanvasExamplePage : BasePager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "CanvasView Example"
                }
            }

            List {
                attr {
                    flex(1f)
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)

                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
                CouponBackground {
                    attr {
                        margin(all = 16f)
                        height(200f)
                    }
                }
            }

        }
    }
}

internal fun ViewContainer<*, *>.CouponBackground(init: CouponBackgroundView.() -> Unit) {
    addChild(CouponBackgroundView(), init)
}

internal class CouponBackgroundAttr : ComposeAttr() {
    var rightAreaWidth = 84f // 右边区域宽度
    var couponBgOpacity= 0.5f // 默认透明度
    var showStyleDisable by observable(false) // 显示不可用样式（可选参数）
    var borderColorToken by observable("") // 边框颜色token（可选参数） 注：需要自定义时赋值该属性，默认颜色无法满足时
    var backgroundColorToken by observable("") // 背景色token 注：需要自定义时赋值该属性，默认颜色无法满足时
    var borderColor by observable(0L) // 边框颜色（可选参数） 注：与token不一样，该值为16进制颜色值，举例0xff0000FFL
    var backgroundColor by observable(0L) // 背景色 注：与token不一样，该值为16进制颜色值，举例0xff0000FFL
    var dashLineColorToken by observable("") // 虚线色token 注：需要自定义时赋值该属性，默认颜色无法满足时
}

internal class CouponBackgroundView : ComposeView<CouponBackgroundAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Canvas ({
                attr {
                    opacity(ctx.attr.couponBgOpacity)
                    absolutePosition(0F, 0F, 0F, 0F)
                }
            }) { context, width, height ->
                ctx.renderBackground(context, width, height)
            }

            // 虚线需要一个专门层级
            Canvas ({
                attr {
                    absolutePosition(
                        top = 0F,
                        bottom = 0F,
                        right = ctx.attr.rightAreaWidth - 4.5f
                    )
                    width(4f)
                }
            }) { context, width, height ->
                ctx.renderDashLine(context, width, height)
            }
        }
    }

    fun borderColor(): Color {
        return Color.RED
    }
    fun bgColor() : Color {
        return Color(0xFFFFF2F6)
    }
    fun dashLineColor() : Color {
        return Color.RED
    }

    override fun createAttr(): CouponBackgroundAttr {
        return CouponBackgroundAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
    }

    override fun setFrameToRenderView(frame: Frame) {
        super.setFrameToRenderView(frame)

    }

    private fun renderBackground(context: CanvasContext, width: Float, height: Float) {
        val rightWidth = attr.rightAreaWidth
        val lineHeight = 0.5f
        val edge = lineHeight / 2f
        var x = edge
        var y = edge
        context.beginPath()
        //moveTo()
        val cornerRadius = 4f // 边框角度
        context.lineWidth(lineHeight)
        context.strokeStyle(borderColor())

        context.moveTo(x, y + cornerRadius)

        // left top
        context.arc(
            x + cornerRadius,
            y + cornerRadius,
            cornerRadius,
            PI.toFloat(),
            PI.toFloat() * (3f / 2f),
            false
        )

        val bigCornerRadius = 5f
        x = width - rightWidth - bigCornerRadius
        y = edge
        context.lineTo(x, y)

        var centerX = x
        var centerY = y + bigCornerRadius
        // 1/4 圆角
        context.arc(
            centerX,
            centerY,
            bigCornerRadius,
            PI.toFloat() * (3f / 2f),
            PI.toFloat() * (3f / 2f) + PI.toFloat() / 4,
            false
        )

        // 90℃ 圆角
        val offset = cos(PI.toFloat() / 4f) * (2f * bigCornerRadius)
        context.arc(
            centerX + offset,
            centerY - offset,
            bigCornerRadius,
            PI.toFloat() / 2f + PI.toFloat() / 4f,
            PI.toFloat() / 4f,
            true
        )
        // 对称1/4 圆角
        context.arc(
            centerX + 2 * offset,
            centerY,
            bigCornerRadius,
            PI.toFloat() * (3f / 2f) - PI.toFloat() / 4f,
            PI.toFloat() * (3f / 2f),
            false
        )

        x = width - edge - cornerRadius
        y = edge
        context.lineTo(x, y)
//
        context.arc(x, y + cornerRadius, cornerRadius, PI.toFloat() * (3f / 2f), 2 * PI.toFloat(), false)
//
        x = width - edge
        y = height - edge - cornerRadius
        context.lineTo(x, y)

        context.arc(x - cornerRadius, y, cornerRadius, 0f, PI.toFloat() / 2, false)

        // 对角线一半完成，接着回到原点继续画对称的另一半
        // 回到起始点
        context.moveTo(edge, edge + cornerRadius)

        x = edge
        y = height - edge - cornerRadius
        context.lineTo(x, y)

        //
        context.arc(x + cornerRadius, y, cornerRadius, PI.toFloat(), PI.toFloat() / 2, true)

        x = width - rightWidth - bigCornerRadius
        y = height - edge
        context.lineTo(x, y)

        centerX = x
        centerY = y - bigCornerRadius
        //底边  左1/4 圆角
        context.arc(
            centerX,
            centerY,
            bigCornerRadius,
            PI.toFloat() / 2f,
            PI.toFloat() / 4f,
            true
        )

        //底边  90℃ 圆角
        context.arc(
            centerX + offset,
            centerY + offset,
            bigCornerRadius,
            PI.toFloat() + PI.toFloat() / 4,
            PI.toFloat() + PI.toFloat() / 4 + PI.toFloat() / 2,
            false
        )
        //底边  右1/4 圆角
        context.arc(
            centerX + 2 * offset,
            centerY,
            bigCornerRadius,
            PI.toFloat() / 2 + PI.toFloat() / 4,
            PI.toFloat() / 2,
            true
        )
        context.lineTo(width - edge - cornerRadius, height - edge)
        context.stroke()
        context.fillStyle(bgColor())
        context.fill()
        // ctx.renderDashLine()
    }

    private fun renderDashLine(context: CanvasContext, width: Float, height: Float) {
        // 开始画虚线
        val ctx = this
        context.beginPath()
        context.strokeStyle(ctx.dashLineColor())
        context.lineWidth(0.5f)
        val dashLineX = 2f
        var dashLineY = 8f
        context.moveTo(dashLineX, dashLineY)
        context.lineCapRound()
        dashLineY += 2
        context.lineTo(dashLineX, dashLineY)
        dashLineY += 1.5f
        context.moveTo(dashLineX, dashLineY)
        val top = dashLineY
        val bottom = height - (dashLineY)

        for (i in 0 until ((bottom - top) / 8).toInt()) {
            dashLineY += 2.5f
            context.moveTo(dashLineX, dashLineY)
            dashLineY += 3
            context.lineTo(dashLineX, dashLineY)
            dashLineY += 2.5f
            context.moveTo(dashLineX, dashLineY)
        }
        dashLineY += 1.5f
        context.moveTo(dashLineX, dashLineY)
        context.lineTo(dashLineX, dashLineY + 2f)
        context.stroke()

    }

}
