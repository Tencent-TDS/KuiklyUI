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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

private const val READING_FIGURE_BRIDGE =
    "https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg"
private const val READING_FIGURE_STREET =
    "https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/gjCqDSbr.png"

/**
 * 阅读正文最宽 560，更宽时居中，两侧留白。字号不随屏幕放大。
 * 配图：宽屏两张图并排，窄屏上下排列，仍落在同一条阅读列里。
 */
@Page("FoldReading")
internal class FoldReadingPage : BasePager() {

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(FoldPalette.bg)
            }
            NavBar {
                attr {
                    title = "外滩"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                }
                View {
                    attr {
                        foldContentPadding()
                    }
                    View {
                        attr {
                            alignSelfCenter()
                            padding(20f)
                            borderRadius(16f)
                            backgroundColor(FoldPalette.card)
                            val insets = ctx.pageData.safeAreaInsets
                            val available =
                                ctx.pageData.pageViewWidth - insets.left - insets.right - 32f
                            width(available.coerceIn(0f, 560f))
                        }
                    Text {
                        attr {
                            text("傍晚的江面")
                            fontSize(22f)
                            fontWeightSemiBold()
                            color(FoldPalette.ink)
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            text("外白渡桥到金陵东路")
                            fontSize(14f)
                            color(FoldPalette.tertiary)
                        }
                    }
                    foldParagraph(
                        "风从江面贴着栏杆过来。楼群的灯还没全亮，桥洞里先看见一艘很小的船。",
                    )
                    foldParagraph(
                        "再往前，海关大楼的钟和沿江的石库门挨在一起。人不多的时候，这一段适合看，不适合赶。",
                    )
                    vbind({
                        val insets = ctx.pageData.safeAreaInsets
                        val available = ctx.pageData.pageViewWidth - insets.left - insets.right
                        available
                    }) {
                        val insets = ctx.pageData.safeAreaInsets
                        val available = ctx.pageData.pageViewWidth - insets.left - insets.right
                        val wide = available >= FOLD_BREAKPOINT
                        View {
                            attr {
                                marginTop(16f)
                                if (wide) {
                                    flexDirectionRow()
                                } else {
                                    flexDirectionColumn()
                                }
                            }
                            foldReadingFigure(
                                host = this,
                                src = READING_FIGURE_BRIDGE,
                                caption = "外白渡桥",
                                wide = wide,
                                isFirst = true,
                            )
                            foldReadingFigure(
                                host = this,
                                src = READING_FIGURE_STREET,
                                caption = "金陵东路",
                                wide = wide,
                                isFirst = false,
                            )
                        }
                    }
                    foldParagraph(
                        "走到金陵东路，天色通常已经暗下来。回头还能看见刚才那一排窗户，灯光贴着江水，路就短了一截。",
                    )
                    foldParagraph(
                        "同一条路，白天是车和人，傍晚只剩下风，还有远处很轻的钟声。",
                    )
                    }
                }
            }
        }
    }
}

private fun foldReadingFigure(
    host: ViewContainer<*, *>,
    src: String,
    caption: String,
    wide: Boolean,
    isFirst: Boolean,
) {
    host.View {
        attr {
            borderRadius(12f)
            overflow(true)
            backgroundColor(FoldPalette.chip)
            if (wide) {
                flex(1f)
                height(148f)
                if (isFirst) {
                    marginRight(8f)
                } else {
                    marginLeft(8f)
                }
            } else {
                height(168f)
                if (!isFirst) {
                    marginTop(12f)
                }
            }
        }
        Image {
            attr {
                absolutePositionAllZero()
                src(src)
                resizeCover()
            }
        }
        View {
            attr {
                absolutePosition(left = 0f, right = 0f, bottom = 0f)
                paddingLeft(10f)
                paddingRight(10f)
                paddingTop(24f)
                paddingBottom(8f)
                backgroundColor(0x66000000)
            }
            Text {
                attr {
                    text(caption)
                    fontSize(13f)
                    fontWeightSemiBold()
                    color(FoldPalette.card)
                }
            }
        }
    }
}
