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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Center
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class ViewExamplePadding: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(150f)
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    padding(all = 10f)
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFF99CFFF)
                    }
                    Text { attr { text("PaddingAll: 10") } }
                }
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    padding(left = 5f, top = 10f, right = 15f, bottom = 20f)
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFFFF7FCC)
                    }
                    Text { attr { text("Padding\nleft: 5,\ntop: 10,\nright: 15,\nbottom: 20") } }
                }
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    paddingLeft(5f)
                    paddingTop(10f)
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFFFFEE77)
                    }
                    Text { attr { text("PaddingLeft 5,\nPaddingTop: 15") } }
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ViewExamplePadding(init: ViewExamplePadding.() -> Unit) {
    addChild(ViewExamplePadding(), init)
}

internal class ViewExampleMargin: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(150f)
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFFF9397F)
                        margin(all = 10f)
                    }
                    Text { attr { text("MarginAll: 10") } }
                }
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFF7FFFE9)
                        margin(left = 5f, top = 10f, right = 15f, bottom = 20f)
                    }
                    Text { attr { text("Margin\nleft: 5,\ntop: 10,\nright: 15,\nbottom: 20") } }
                }
            }
            View {
                attr {
                    size(width = 120f, height = 120f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                }
                Center {
                    attr {
                        flex(1f)
                        backgroundColor(0xFF3FF97F)
                        marginLeft(5f)
                        marginTop(15f)
                    }
                    Text { attr { text("MarginLeft: 5,\nMarginTop: 15") } }
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ViewExampleMargin(init: ViewExampleMargin.() -> Unit) {
    addChild(ViewExampleMargin(), init)
}

internal class ViewExampleBackground: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(100f)
            }
            View {
                attr {
                    size(width = 90f, height = 80f)
                    backgroundColor(Color(0xFFAEEEA0))
                }
            }
            View {
                attr {
                    size(width = 90f, height = 80f)
                    backgroundColor(0xFFAAFFFF)
                }
            }
            View {
                attr {
                    size(width = 90f, height = 80f)
                    backgroundLinearGradient(
                        Direction.TO_RIGHT,
                        ColorStop(Color(0xFFFFAA99), 0f),
                        ColorStop(Color(0xFFFFFFAA), 1f),
                    )
                }
            }
            View {
                attr {
                    size(width = 90f, height = 80f)
                    backgroundLinearGradient(
                        Direction.TO_BOTTOM_RIGHT,
                        ColorStop(Color(0xFFFFCC99), 0f),
                        ColorStop(Color(0xFFFFFF88), 0.33f),
                        ColorStop(Color(0xFF77FF77), 0.67f),
                        ColorStop(Color(0xFF77CCFF), 1f),
                    )
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ViewExampleBackground(init: ViewExampleBackground.() -> Unit) {
    addChild(ViewExampleBackground(), init)
}

internal class ViewExampleBorder: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(100f)
            }
            View {
                attr {
                    border(Border(1.0f, BorderStyle.SOLID, Color.BLACK))
                    size(60f, 60f)
                }
            }
            View {
                attr {
                    border(Border(1.0f, BorderStyle.SOLID, Color(0xFFC07FFFL)))
                    size(60f, 60f)
                }
            }
            View {
                attr {
                    border(Border(2.0f, BorderStyle.DASHED, Color(0xFFFF9AD8L)))
                    size(60f, 60f)
                }
            }
            View {
                attr {
                    border(Border(2.0f, BorderStyle.DOTTED, Color(0xFF80FFA1L)))
                    size(60f, 60f)
                }
            }
            View {
                attr {
                    border(Border(1.0f, BorderStyle.SOLID, Color(0xFFFFC098L)))
                    borderRadius(allBorderRadius = 8.0f)
                    size(60f, 60f)
                }
            }
            View {
                attr {
                    border(Border(1.0f, BorderStyle.SOLID, Color(0xFF80E0FFL)))
                    borderRadius(BorderRectRadius(topLeftCornerRadius = 18.0f, topRightCornerRadius = 12.0f, bottomLeftCornerRadius = 6.0f, bottomRightCornerRadius = 0f))
                    size(60f, 60f)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.ViewExampleBorder(init: ViewExampleBorder.() -> Unit) {
    addChild(ViewExampleBorder(), init)
}

@Page("ViewExamplePage")
internal class ViewExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar { attr { title = "View Attr Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "Padding" } }
                ViewExamplePadding {}
                ViewExampleSectionHeader { attr { title = "Margin" } }
                ViewExampleMargin {}
                ViewExampleSectionHeader { attr { title = "Background"} }
                ViewExampleBackground {}
                ViewExampleSectionHeader { attr { title = "Border & BorderRadius"} }
                ViewExampleBorder {}
            }
        }
    }
}