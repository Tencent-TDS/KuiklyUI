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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.PathApi
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Page("clip")
internal class ClipPathExamplePage : BasePager() {

    companion object {
        private fun drawStarShape(context: PathApi, width: Float, height: Float, alignStart: Boolean) {
            require(width > 0 && height > 0)
            val size = min(width, height)
            val centerX = if (alignStart) size / 2f else width - size / 2f
            val centerY = if (alignStart) size / 2f else height - size / 2f
            val outerRadius = size / 2f
            val innerRadius = outerRadius * 0.4f // Adjust this to change star pointiness

            val points = 5 // Number of star points
            val anglePerPoint = (2 * PI / points).toFloat()

            with (context) {
                beginPath()
                moveTo(
                    centerX + outerRadius * cos(0f),
                    centerY + outerRadius * sin(0f)
                )
                for (i in 1 until points * 2) {
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val angle = anglePerPoint * i / 2
                    lineTo(
                        centerX + radius * cos(angle),
                        centerY + radius * sin(angle)
                    )
                }
                closePath()
            }
        }

        private fun drawMoonShape(context: PathApi, width: Float, height: Float, alignStart: Boolean) {
            require(width > 0 && height > 0)
            val size = min(width, height)
            val centerX = if (alignStart) size / 2f else width - size / 2f
            val centerY = if (alignStart) size / 2f else height - size / 2f
            val radius = size / 2f
            val innerCenterX = centerX + radius / 2f
            val innerCenterY = centerY - radius / 2f
            val innerRadius = radius / sqrt(2f)

            with (context) {
                beginPath()
                arc(centerX, centerY, radius, 0f, (1.5 * PI).toFloat(), false)
                arc(innerCenterX, innerCenterY, innerRadius, (1.25 * PI).toFloat(), (0.25 * PI).toFloat(), true)
                closePath()
            }
        }
    }

    private var enableClip by observable(true)
    private var expand by observable(false)
    private var alignStart by observable(true)
    private var shapeFlag by observable(false)
    private var conflictSwitch by observable(false)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "ClipPath示例" } }
            Scroller {
                attr {
                    flex(1f)
                }
                // View、Image、Text示例
                View {
                    attr {
                        backgroundColor(Color.GRAY)
                        padding(5f)
                    }
                    Text { attr { text("View、Image、Text示例") } }
                }
                View {
                    attr {
                        flexDirectionRow()
                    }
                    View {
                        attr {
                            flex(1f)
                            height(100f)
                            backgroundColor(Color.GRAY)
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text {
                            attr { text("示例文本示例文本示例文本示例文本示例文本示例文本示例文本示例文本") }
                        }
                        Input {
                            attr {
                                height(24f)
                                placeholder("示例输入框")
                            }
                        }
                    }
                    Image {
                        attr {
                            flex(1f)
                            height(100f)
                            src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }
                    Text {
                        attr {
                            flex(1f)
                            height(100f)
                            text("示例文本示例文本示例文本示例文本示例文本示例文本示例文本示例文本")
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                    }
                    Text { attr { text("View"); fontSize(12f); flex(1f) } }
                    Text { attr { text("Image"); fontSize(12f); flex(1f) } }
                    Text { attr { text("Text"); fontSize(12f); flex(1f) } }
                }
                // 响应式更新
                View {
                    attr {
                        backgroundColor(Color.GRAY)
                        padding(5f)
                        marginTop(5f)
                    }
                    Text { attr { text("响应式更新") } }
                }
                View {
                    attr {
                        height(if (ctx.expand) 200f else 100f)
                        backgroundColor(Color.RED)
                        if (ctx.enableClip) {
                            clipPath { w, h ->
                                if (ctx.shapeFlag) {
                                    drawMoonShape(this, w, h, ctx.alignStart)
                                } else {
                                    drawStarShape(this, w, h, ctx.alignStart)
                                }
                            }
                        } else {
                            clipPath(null)
                        }
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        padding(5f)
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.enableClip) "禁用" else "启用" )
                            }
                            backgroundColor(Color(0xFF007AFF))
                            highlightBackgroundColor(Color(0x66000000))
                            borderRadius(16f)
                            width(64f)
                            height(32f)
                        }
                        event {
                            click {
                                ctx.enableClip = !ctx.enableClip
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.expand) "收起" else "展开" )
                            }
                            backgroundColor(Color(0xFF007AFF))
                            highlightBackgroundColor(Color(0x66000000))
                            borderRadius(16f)
                            width(64f)
                            height(32f)
                            marginLeft(5f)
                        }
                        event {
                            click {
                                ctx.expand = !ctx.expand
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("切换对齐")
                            }
                            backgroundColor(Color(0xFF007AFF))
                            highlightBackgroundColor(Color(0x66000000))
                            borderRadius(16f)
                            width(64f)
                            height(32f)
                            marginLeft(5f)
                        }
                        event {
                            click {
                                ctx.alignStart = !ctx.alignStart
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("切换形状")
                            }
                            backgroundColor(Color(0xFF007AFF))
                            highlightBackgroundColor(Color(0x66000000))
                            borderRadius(16f)
                            width(64f)
                            height(32f)
                            marginLeft(5f)
                        }
                        event {
                            click {
                                ctx.shapeFlag = !ctx.shapeFlag
                            }
                        }
                    }
                }
                // 边框、阴影、背景色、背景图、渐变
                View {
                    attr {
                        backgroundColor(Color.GRAY)
                        padding(5f)
                        marginTop(5f)
                    }
                    Text { attr { text("边框、阴影、背景色、背景图、渐变") } }
                }
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            border(Border(5f, BorderStyle.DASHED, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("边框") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("阴影") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED), true)
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("useShadowPath\n阴影"); textAlignCenter() } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundColor(Color.GREEN)
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("背景色") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundImage("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("背景图") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM_RIGHT,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        Text { attr { text("渐变") } }
                    }
                }
                // 圆角优先级验证
                View {
                    attr {
                        backgroundColor(Color.GRAY)
                        padding(5f)
                        marginTop(5f)
                    }
                    Text { attr { text("圆角优先级验证") } }
                }
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            border(Border(5f, BorderStyle.DASHED, Color.BLACK))
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED))
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED), true)
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundColor(Color.GREEN)
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundImage("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM_RIGHT,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                            borderRadius(0f, 5f, 10f, 80f)
                            if (ctx.conflictSwitch) {
                                clipPath { w, h -> drawStarShape(this, w, h, true) }
                            } else {
                                clipPath(null)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "圆角+裁剪" else "圆角") } }
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            border(Border(5f, BorderStyle.DASHED, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            boxShadow(BoxShadow(5f, 5f, 10f, Color.RED), true)
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundColor(Color.GREEN)
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundImage("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            allCenter()
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM_RIGHT,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                            if (ctx.conflictSwitch) {
                                borderRadius(0f, 5f, 10f, 80f)
                            } else {
                                borderRadius(0f)
                            }
                        }
                        Text { attr { text(if (ctx.conflictSwitch) "裁剪+圆角" else "裁剪") } }
                    }
                }
                View {
                    attr {
                        padding(5f)
                    }
                    Switch {
                        attr {
                            isOn(ctx.conflictSwitch)
                        }
                        event {
                            switchOnChanged { params ->
                                ctx.conflictSwitch = params
                            }
                        }
                    }
                }
                // 边框兼容性验证
                View {
                    attr {
                        backgroundColor(Color.GRAY)
                        padding(5f)
                        marginTop(5f)
                    }
                    Text { attr { text("边框兼容性验证") } }
                }
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    View {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            border(Border(5f, BorderStyle.SOLID, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }
                    Image {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            border(Border(5f, BorderStyle.SOLID, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }
                    Canvas({
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            border(Border(5f, BorderStyle.SOLID, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }) { _, _, _ -> }
                    TextArea {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            border(Border(5f, BorderStyle.SOLID, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                    }
                    Scroller {
                        attr {
                            width(100f)
                            height(100f)
                            margin(5f)
                            border(Border(5f, BorderStyle.SOLID, Color.BLACK))
                            clipPath { w, h -> drawStarShape(this, w, h, true) }
                        }
                        View {}
                    }
                }
            }
        }
    }

}