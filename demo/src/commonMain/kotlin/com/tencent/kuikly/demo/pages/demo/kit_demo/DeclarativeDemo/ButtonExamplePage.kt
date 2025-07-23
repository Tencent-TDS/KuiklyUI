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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class ButtonExampleStyleView: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(80f)
            }
            Button {
                attr {
                    titleAttr {
                        text("Flat")
                        color(Color(0xFF00CED1))
                    }
                    size(width = 90f, height = 40f)
                }
            }
            Button {
                attr {
                    titleAttr { text("Raised") }
                    backgroundColor(Color(0xFF9ACD32))
                    size(width = 90f, height = 40f)
                    boxShadow(BoxShadow(offsetX = 0f, offsetY = 3f, shadowColor = Color(0x9F030703), shadowRadius = 4f))
                }
            }
            Button {
                attr {
                    titleAttr {
                        text("Outline")
                        color(Color(0xFFFF8C00))
                    }
                    size(width = 90f, height = 40f)
                    borderRadius(20f)
                    border(Border(lineWidth = 1f, color = Color(0xFFFF8C00), lineStyle = BorderStyle.SOLID))
                }
            }
            Button {
                attr {
                    titleAttr {
                        text("Ecommerce")
                        color(Color(0xFFFFFFFF))
                        fontWeight500()
                    }
                    size(width = 90f, height = 40f)
                    borderRadius(4f)
                    backgroundLinearGradient(
                        direction = Direction.TO_RIGHT,
                        ColorStop(Color(0xFFFF46A0), 0f),
                        ColorStop(Color(0xFFFF3355), 1f),
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

internal fun ViewContainer<*, *>.ButtonExampleStyleView(init: ButtonExampleStyleView.() -> Unit) {
    addChild(ButtonExampleStyleView(), init)
}

internal class ButtonExampleEventView: ComposeView<ComposeAttr, ComposeEvent>() {
    private var clickCount = 0
    private var doubleClickCount = 0
    private var longPressCount = 0

    private var clickButtonTitle by observable("点击我触发事件")
    private var doubleClickButtonTitle by observable("双击我触发事件")
    private var longPressButtonTitle by observable("长按我触发事件")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                justifyContentSpaceAround()
                height(80f)
            }
            Button {
                attr {
                    titleAttr {
                        text(ctx.clickButtonTitle)
                    }
                    backgroundColor(Color(0xFFFFD700))
                    size(width = 110f, height = 40f)
                }
                event {
                    click {
                        ctx.clickCount ++
                        ctx.clickButtonTitle = if (ctx.clickCount == 1) "我被点啦" else if (ctx.clickCount == 2) "我又被点啦" else "我被点了${ctx.clickCount}次啦"
                    }
                }
            }
            Button {
                attr {
                    titleAttr {
                        text(ctx.doubleClickButtonTitle)
                    }
                    backgroundColor(Color(0xFF00BFFF))
                    size(width = 110f, height = 40f)
                }
                event {
                    doubleClick {
                        ctx.doubleClickCount ++
                        ctx.doubleClickButtonTitle = if (ctx.doubleClickCount == 1) "我被双击啦" else if (ctx.doubleClickCount == 2) "我又被双击啦" else "我被双击了${ctx.doubleClickCount}次"
                    }
                }
            }
            Button {
                attr {
                    titleAttr {
                        text(ctx.longPressButtonTitle)
                    }
                    backgroundColor(Color(0xFFE6E6FA))
                    size(width = 110f, height = 40f)
                }
                event {
                    longPress {
                        ctx.longPressCount ++
                        ctx.longPressButtonTitle = if (ctx.longPressCount == 1) "我被长按啦" else if (ctx.longPressCount == 2) "我又被长按啦" else "我被长按了${ctx.longPressCount}次"
                    }
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

internal fun ViewContainer<*, *>.ButtonExampleEventView(init: ButtonExampleEventView.() -> Unit) {
    addChild(ButtonExampleEventView(), init)
}

@Page("ButtonExamplePage")
internal class ButtonExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Button Attr & Event Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "KTV中的Button的实现是一个带有文字的View" } }
                ButtonExampleStyleView {  }
                ViewExampleSectionHeader { attr { title = "KTV事件监听" } }
                ButtonExampleEventView {  }
            }
        }
    }
}
