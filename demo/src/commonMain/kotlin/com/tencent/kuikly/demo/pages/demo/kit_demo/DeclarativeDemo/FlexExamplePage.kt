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
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class FlexExampleViewAttr: ComposeAttr() {
    var text: String = ""
    var color: Color = Color(0x00000000)
    var autoSize: Boolean = false
}

internal class FlexExampleView(): ComposeView<FlexExampleViewAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr {
                backgroundColor(ctx.backgroundColorFromColor(ctx.attr.color))
                border(Border(lineWidth = 2f, lineStyle = BorderStyle.SOLID, color = ctx.attr.color))
                if (!ctx.attr.autoSize) {
                    size(width = 60f, height = 40f)
                }
                margin(all = 4f)
                padding(all = 4f)
                allCenter()
            }
            Text {
                attr {
                    text(ctx.attr.text)
                    fontSize(18f)
                    fontWeightMedium()
                }
            }
        }
    }

    private fun backgroundColorFromColor(color: Color): Color {
        var colorValue = color.toString().toLong()
        return Color(colorValue % 0x01000000 + 0x9F000000)
    }

    override fun createAttr(): FlexExampleViewAttr {
        return FlexExampleViewAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.FlexExampleView(init: FlexExampleView.() -> Unit) {
    addChild(FlexExampleView(), init)
}

internal class FlexExampleJustifyContent: ComposeView<ComposeAttr, ComposeEvent>() {
    private var justifyContentStateTextArray: Array<String> = arrayOf(
        "current: FlexStart",
        "current: FlexEnd",
        "current: Center",
        "current: SpaceAround",
        "current: SpaceBetween",
    )
    private var justifyContentStateArray: Array<FlexJustifyContent> = arrayOf(
        FlexJustifyContent.FLEX_START,
        FlexJustifyContent.FLEX_END,
        FlexJustifyContent.CENTER,
        FlexJustifyContent.SPACE_AROUND,
        FlexJustifyContent.SPACE_BETWEEN,
    )
    private var justifyContentIndex = 0

    private var justifyContentViewState by observable(FlexJustifyContent.FLEX_START)
    private var justifyContentViewText by observable("current: FlexStart")

    private fun exampleView(view: DivView, title: String) {
        view.addChild(FlexExampleView()) { attr {
            text = title
            color = Color(0xFF7FFFC9)
        } }
    }

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    justifyContent(ctx.justifyContentViewState)
                    margin(all = 20f)
                    padding(all = 5f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    alignItemsCenter()
                }
                ctx.exampleView(this, "one")
                ctx.exampleView(this, "two")
                ctx.exampleView(this, "three")
                ctx.exampleView(this, "four")
            }
            Button {
                attr {
                    size(width = 180f, height = 40f)
                    borderRadius(20f)
                    backgroundColor(Color(0x9F33C0D9))
                    alignSelfCenter()
                    marginBottom(40f)
                    titleAttr {
                        text(ctx.justifyContentViewText)
                        fontWeight500()
                    }
                }
                event {
                    click {
                        ctx.justifyContentIndex = (ctx.justifyContentIndex + 1) % ctx.justifyContentStateTextArray.size
                        ctx.justifyContentViewText = ctx.justifyContentStateTextArray[ctx.justifyContentIndex]
                        ctx.justifyContentViewState = ctx.justifyContentStateArray[ctx.justifyContentIndex]
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

internal fun ViewContainer<*, *>.FlexExampleJustifyContent(init: FlexExampleJustifyContent.() -> Unit) {
    addChild(FlexExampleJustifyContent(), init)
}

internal class FlexExampleAlignItems: ComposeView<ComposeAttr, ComposeEvent>() {
    private var alignItemTextArray: Array<String> = arrayOf(
        "current: Center",
        "current: FlexStart",
        "current: FlexEnd",
        "current: Stretch",
    )
    private var alignItemStateArray: Array<FlexAlign> = arrayOf(
        FlexAlign.CENTER,
        FlexAlign.FLEX_START,
        FlexAlign.FLEX_END,
        FlexAlign.STRETCH,
    )
    private var alignItemIndex = 0

    private var alignItemViewState by observable(FlexAlign.CENTER)
    private var alignItemViewText by observable("current: Center")

    private fun exampleView(view: DivView, title: String) {
        view.addChild(FlexExampleView()) { attr {
            text = title
            color = Color(0xFFB9CF3F)
            autoSize = true
        } }
    }

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceAround()
                    alignItems(ctx.alignItemViewState)
                    margin(all = 20f)
                    padding(all = 5f)
                    height(60f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                }
                ctx.exampleView(this, "one")
                ctx.exampleView(this, "two")
                ctx.exampleView(this, "three")
                ctx.exampleView(this, "four")
                ctx.exampleView(this, "five")
                ctx.exampleView(this, "six")
            }
            Button {
                attr {
                    size(width = 180f, height = 40f)
                    borderRadius(20f)
                    backgroundColor(Color(0x9FA033C9))
                    alignSelfCenter()
                    marginBottom(40f)
                    titleAttr {
                        text(ctx.alignItemViewText)
                        fontWeight500()
                    }
                }
                event {
                    click {
                        ctx.alignItemIndex = (ctx.alignItemIndex + 1) % ctx.alignItemTextArray.size
                        ctx.alignItemViewText = ctx.alignItemTextArray[ctx.alignItemIndex]
                        ctx.alignItemViewState = ctx.alignItemStateArray[ctx.alignItemIndex]
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

internal fun ViewContainer<*, *>.FlexExampleAlignItems(init: FlexExampleAlignItems.() -> Unit) {
    addChild(FlexExampleAlignItems(), init)
}

internal class FlexExampleWrap: ComposeView<ComposeAttr, ComposeEvent>() {
    private var flexWrapTextArray: Array<String> = arrayOf(
        "current: Wrap",
        "current: NoWrap",
    )
    private var flexWrapStateArray: Array<FlexWrap> = arrayOf(
        FlexWrap.WRAP,
        FlexWrap.NOWRAP
    )
    private var flexWrapIndex = 0

    private var flexWrapViewState by observable(FlexWrap.NOWRAP)
    private var flexWrapViewText by observable("current: NoWrap")

    private fun exampleView(view: DivView, title: String) {
        view.addChild(FlexExampleView()) { attr {
            text = title
            color = Color(0xFFFF99D9)
        } }
    }

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    flexWrap(ctx.flexWrapViewState)
                    margin(all = 20f)
                    padding(all = 5f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    alignItemsCenter()
                }
                ctx.exampleView(this, "one")
                ctx.exampleView(this, "two")
                ctx.exampleView(this, "three")
                ctx.exampleView(this, "four")
                ctx.exampleView(this, "five")
                ctx.exampleView(this, "six")
                ctx.exampleView(this, "seven")
            }
            Button {
                attr {
                    size(width = 180f, height = 40f)
                    borderRadius(20f)
                    backgroundColor(Color(0x9F33C0D9))
                    alignSelfCenter()
                    marginBottom(40f)
                    titleAttr {
                        text(ctx.flexWrapViewText)
                        fontWeight500()
                    }
                }
                event {
                    click {
                        ctx.flexWrapIndex = (ctx.flexWrapIndex + 1) % ctx.flexWrapTextArray.size
                        ctx.flexWrapViewText = ctx.flexWrapTextArray[ctx.flexWrapIndex]
                        ctx.flexWrapViewState = ctx.flexWrapStateArray[ctx.flexWrapIndex]
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

internal fun ViewContainer<*, *>.FlexExampleWrap(init: FlexExampleWrap.() -> Unit) {
    addChild(FlexExampleWrap(), init)
}

@Page("FlexExamplePage")
internal class FlexExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "View Flex Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "JustifyContent" } }
                FlexExampleJustifyContent {}
                ViewExampleSectionHeader { attr { title = "AlignItems" } }
                FlexExampleAlignItems {}
                ViewExampleSectionHeader { attr { title = "FlexWrap" } }
                FlexExampleWrap {}
            }
        }
    }
}