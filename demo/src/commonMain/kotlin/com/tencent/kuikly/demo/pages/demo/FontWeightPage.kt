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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button

@Page("fontWeight")
internal class FontWeightPage : BasePager() {

    private var fontWeight by observable(FontWeight.NORMAL)
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {

            // Font weight selector buttons
            View {
                attr {
                    marginTop(60f)
                    flexDirectionRow()
                    flexWrapWrap()
                    padding(10f)
                }

                @Suppress("DEPRECATION")
                FontWeight.entries.filter { it != FontWeight.SEMISOLID }.forEach { weight ->
                    Button {
                        attr {
                            backgroundColor(if (ctx.fontWeight == weight) Color(0xFF007AFF) else Color(0xFFE0E0E0))
                            margin(5f)
                            padding(top = 10f, left = 15f, bottom = 10f, right = 15f)
                            borderRadius(8f)
                            titleAttr {
                                text(weight.name)
                                fontSize(12f)
                                color(if (ctx.fontWeight == weight) Color.WHITE else Color.BLACK)
                            }
                        }
                        event {
                            click {
                                ctx.fontWeight = weight
                            }
                        }
                    }
                }
            }

            Scroller {
                attr {
                    flex(1f)
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(9f)
                        height(30f)
                        lineHeight(30f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(12f)
                        height(30f)
                        lineHeight(30f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(15f)
                        height(30f)
                        lineHeight(30f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(20f)
                        height(30f)
                        lineHeight(30f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(32f)
                        height(50f)
                        lineHeight(50f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(48f)
                        height(60f)
                        lineHeight(60f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(64f)
                        height(80f)
                        lineHeight(80f)
                    }
                }
                Text {
                    attr {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(96f)
                        height(120f)
                        lineHeight(120f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(9f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(12f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(15f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(20f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(32f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(48f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(64f)
                    }
                }
                RichText {
                    Span {
                        text(TEXT)
                        fontWeight(ctx.fontWeight)
                        fontSize(96f)
                    }
                }
            }
        }
    }
}

private const val TEXT = "Hello, 你好, こんにちは, 안녕하세요!"

private fun TextAttr.fontWeight(weight: FontWeight) {
    when (weight) {
        FontWeight.NORMAL -> {
            fontWeightNormal()
        }
        FontWeight.MEDIUM -> {
            fontWeightMedium()
        }
        FontWeight.SEMIBOLD -> {
            fontWeightSemiBold()
        }
        FontWeight.BOLD -> {
            fontWeightBold()
        }
        FontWeight.EXTRABOLD -> {
            fontWeightExtraBold()
        }
        FontWeight.BLACK -> {
            fontWeightBlack()
        }
        else -> {
            throw IllegalArgumentException("Unsupported font weight: $weight")
        }
    }
}