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
import com.tencent.kuikly.core.base.BorderRectRadius
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PathApi
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Page("box_shadow")
internal class BoxShadowExamplePage : BasePager() {

    companion object {
        private fun drawStarShape(context: PathApi, width: Float, height: Float) {
            require(width > 0 && height > 0)
            val size = min(width, height)
            val centerX = width / 2f
            val centerY = height / 2f
            val outerRadius = size / 2f
            val innerRadius = outerRadius * 0.4f

            val points = 5
            val anglePerPoint = (2 * PI / points).toFloat()

            with(context) {
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
    }

    private var fillEnabled by observable(true)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "BoxShadow示例" } }
            Scroller {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        flexDirectionRow()
                        padding(10f)
                        alignItemsCenter()
                    }
                    Switch {
                        attr {
                            isOn(ctx.fillEnabled)
                            margin(left = 10f, right = 10f)
                        }
                        event {
                            switchOnChanged { params ->
                                ctx.fillEnabled = params
                            }
                        }
                    }
                    Text {
                        attr {
                            text(if (ctx.fillEnabled) "Fill: true" else "Fill: false")
                            fontSize(14f)
                        }
                    }
                }

                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                        padding(10f)
                    }
                    View {
                        attr {
                            width(120f)
                            height(120f)
                            margin(10f)
                            backgroundColor(Color(0x66FFFFFF))
                            allCenter()
                            boxShadow(
                                BoxShadow(
                                    offsetX = 5f,
                                    offsetY = 5f,
                                    shadowRadius = 10f,
                                    shadowColor = Color(0xFF4A90E2),
                                    fill = ctx.fillEnabled
                                )
                            )
                        }
                        Text {
                            attr {
                                text("无圆角")
                                fontSize(14f)
                            }
                        }
                    }

                    View {
                        attr {
                            width(120f)
                            height(120f)
                            margin(10f)
                            backgroundColor(Color(0x66FFFFFF))
                            borderRadius(20f)
                            allCenter()
                            boxShadow(
                                BoxShadow(
                                    offsetX = 5f,
                                    offsetY = 5f,
                                    shadowRadius = 10f,
                                    shadowColor = Color(0xFF50C878),
                                    fill = ctx.fillEnabled
                                )
                            )
                        }
                        Text {
                            attr {
                                text("相等圆角")
                                fontSize(14f)
                            }
                        }
                    }

                    View {
                        attr {
                            width(120f)
                            height(120f)
                            margin(10f)
                            backgroundColor(Color(0x66FFFFFF))
                            borderRadius(
                                BorderRectRadius(
                                    topLeftCornerRadius = 10f,
                                    topRightCornerRadius = 30f,
                                    bottomLeftCornerRadius = 40f,
                                    bottomRightCornerRadius = 20f
                                )
                            )
                            allCenter()
                            boxShadow(
                                BoxShadow(
                                    offsetX = 5f,
                                    offsetY = 5f,
                                    shadowRadius = 10f,
                                    shadowColor = Color(0xFFFF6B6B),
                                    fill = ctx.fillEnabled
                                )
                            )
                        }
                        Text {
                            attr {
                                text("不相等圆角")
                                fontSize(14f)
                            }
                        }
                    }

                    View {
                        attr {
                            width(120f)
                            height(120f)
                            margin(10f)
                            backgroundColor(Color(0x66FFFFFF))
                            clipPath { w, h -> drawStarShape(this, w, h) }
                            allCenter()
                            boxShadow(
                                BoxShadow(
                                    offsetX = 5f,
                                    offsetY = 5f,
                                    shadowRadius = 10f,
                                    shadowColor = Color(0xFF9B59B6),
                                    fill = ctx.fillEnabled
                                )
                            )
                        }
                        Text {
                            attr {
                                text("clipPath")
                                fontSize(14f)
                            }
                        }
                    }
                }
            }
        }
    }
}
