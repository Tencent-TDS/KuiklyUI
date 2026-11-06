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
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Text / RichText 交叉轴拉伸：仅自身显式 alignSelfStretch() 时生效，父容器 alignItemsStretch() 不影响。
 */
@Page("TextStretchRepro")
internal class TextStretchReproPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            NavBar {
                attr {
                    title = "Text Stretch"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF2F2F7))
                    padding(12f)
                }

                sectionTitle("默认：包裹文字")
                Box(column = true) {
                    Text {
                        attr {
                            backgroundColor(TEXT_COLOR)
                            text("Text")
                        }
                    }
                }

                sectionTitle("Column + alignSelfStretch：撑满宽度")
                Box(column = true) {
                    Text {
                        attr {
                            alignSelfStretch()
                            backgroundColor(TEXT_COLOR)
                            text("Text")
                        }
                    }
                    RichText {
                        attr {
                            alignSelfStretch()
                            backgroundColor(RICH_TEXT_COLOR)
                        }
                        Span {
                            text("RichText")
                        }
                    }
                }

                sectionTitle("Row + alignSelfStretch：撑满高度")
                Box(column = false) {
                    Text {
                        attr {
                            alignSelfStretch()
                            backgroundColor(TEXT_COLOR)
                            text("Text")
                        }
                    }
                    RichText {
                        attr {
                            alignSelfStretch()
                            backgroundColor(RICH_TEXT_COLOR)
                        }
                        Span {
                            text("RichText")
                        }
                    }
                }

                sectionTitle("alignItemsFlexStart + alignSelfStretch：撑满宽度")
                View {
                    attr {
                        height(BOX_HEIGHT)
                        backgroundColor(Color.WHITE)
                        alignItemsFlexStart()
                    }
                    Text {
                        attr {
                            alignSelfStretch()
                            backgroundColor(TEXT_COLOR)
                            text("Text")
                        }
                    }
                }

                sectionTitle("显式 width 优先")
                Box(column = true) {
                    Text {
                        attr {
                            alignSelfStretch()
                            width(80f)
                            backgroundColor(TEXT_COLOR)
                            text("width 80")
                        }
                    }
                }
            }
        }
    }

}

private const val BOX_HEIGHT = 64f
private val TEXT_COLOR = Color(0xFFFFB74D)
private val RICH_TEXT_COLOR = Color(0xFF81C784)

private fun ViewContainer<*, *>.Box(column: Boolean, init: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            height(BOX_HEIGHT)
            backgroundColor(Color.WHITE)
            if (column) flexDirectionColumn() else flexDirectionRow()
        }
        init()
    }
}

private fun ViewContainer<*, *>.sectionTitle(title: String) {
    Text {
        attr {
            marginTop(16f)
            marginBottom(6f)
            fontSize(14f)
            fontWeightBold()
            text(title)
        }
    }
}
