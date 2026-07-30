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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

private val ktvAndroidHelpText = """
    1. ScrollViewExample
    2. ScrollViewExample
    3. ScrollViewExample
    4. ScrollViewExample
    5. ScrollViewExample
    6. ScrollViewExample
    7. ScrollViewExample
    8. ScrollViewExample
    9. ScrollViewExample
    10. ScrollViewExample
    11. ScrollViewExample
    12. ScrollViewExample
    13. ScrollViewExample
    14. ScrollViewExample
    15. ScrollViewExample
    16. ScrollViewExample
    17. ScrollViewExample
    18. ScrollViewExample
    19. ScrollViewExample
    20. ScrollViewExample
""".trimIndent()

private class ScrollViewExampleHeaderAttr: ComposeAttr() {
    var title: String = ""
}

private class ScrollViewExampleHeader: ComposeView<ScrollViewExampleHeaderAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                padding(left = 16f, right = 16f)
                height(32f)
            }
            Text {
                attr {
                    fontSize(14f)
                    fontWeight700()
                    color(Color.BLACK)
                    text(this@ScrollViewExampleHeader.attr.title)
                }
            }
        }
    }

    override fun createAttr(): ScrollViewExampleHeaderAttr {
        return ScrollViewExampleHeaderAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

private fun ViewContainer<*, *>.ScrollViewExampleHeader(init: ScrollViewExampleHeader.() -> Unit) {
    addChild(ScrollViewExampleHeader(), init)
}

private class ScrollViewExamplePageViewAttr: ComposeAttr() {
    var title: String = ""
    var content: String = ""
}

private class ScrollViewExamplePageView: ComposeView<ScrollViewExamplePageViewAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                size(width = 320f, height = 80f)
                backgroundLinearGradient(
                    Direction.TO_RIGHT,
                    ColorStop(Color(0x7FFFB300), 0.0f),
                    ColorStop(Color(0x7FFFB300), 0.1f),
                    ColorStop(Color(0x3FFFB300), 0.5f),
                    ColorStop(Color(0x7FFFB300), 0.9f),
                    ColorStop(Color(0x7FFFB300), 1.0f),
                )
            }
            View {
                attr {
                    absolutePosition(left = 8f, top = 8f)
                    size(width = 32f, height = 32f)
                    backgroundColor(Color.YELLOW)
                }
            }
            Text {
                attr {
                    absolutePosition(left = 48f, top = 14f, right = 8f, bottom = 46f)
                    fontSize(16f)
                    fontWeightMedium()
                    textAlignLeft()
                    text(ctx.attr.title)
                }
            }
            Text {
                attr {
                    absolutePosition(left = 12f, top = 48f, right = 12f, bottom = 8f)
                    fontSize(12f)
                    color(0xFF9F9F9F)
                    textAlignLeft()
                    text(ctx.attr.content)
                }
            }
        }
    }

    override fun createAttr(): ScrollViewExamplePageViewAttr {
        return ScrollViewExamplePageViewAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

private fun ViewContainer<*, *>.ScrollViewExamplePageView(init: ScrollViewExamplePageView.() -> Unit) {
    addChild(ScrollViewExamplePageView(), init)
}

private class ScrollViewExampleAvatarAttr: ComposeAttr() {
    var avatarColor: Color = Color()
    var appName: String = ""
}

private class ScrollViewExampleAvatar: ComposeView<ScrollViewExampleAvatarAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr {
                margin(left = 15f, right = 15f)
                alignItemsCenter()
            }
            View {
                attr {
                    size(width = 48f, height = 48f)
                    borderRadius(allBorderRadius = 24f)
                    border(Border(lineWidth = 0.5f, color = ctx.attr.avatarColor, lineStyle = BorderStyle.SOLID))
                    backgroundColor(ctx.backgroundColorFromColor(ctx.attr.avatarColor))
                }
            }
            Text {
                attr {
                    color(Color.BLACK)
                    fontSize(12f)
                    text(ctx.attr.appName)
                    marginTop(8f)
                }
            }
        }
    }

    override fun createAttr(): ScrollViewExampleAvatarAttr {
        return ScrollViewExampleAvatarAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    private fun backgroundColorFromColor(color: Color): Color {
        var colorValue = color.toString().toLong()
        return Color(colorValue % 0x01000000 + 0x9F000000)
    }
}

private fun ViewContainer<*, *>.ScrollViewExampleAvatar(init: ScrollViewExampleAvatar.() -> Unit) {
    addChild(ScrollViewExampleAvatar(), init)
}

@Page("ScrollViewExamplePage")
internal class ScrollViewExamplePage: BasePager() {
    private var logHistory: MutableList<String> = MutableList(size = 0, init = { "" })
    private var logViewText by observable("")

    override fun body(): ViewBuilder {
        var ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "ScrollView Attr & Event Example" } }
            View {
                ViewExampleSectionHeader { attr { title = "Example Page" } }
                ScrollViewExampleHeader { attr { title = "使用Kuikly开发app的优势" } }
                Scroller {
                    attr {
                        size(width = 320f, height = 80f)
                        margin(top = 4f, bottom = 4f)
                        alignSelfCenter()
                        flexDirectionRow()
                        pagingEnable(true)
                        showScrollerIndicator(false)
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "高性能"
                            content = "运行平台原生编译产物"
                        }
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "跨平台"
                            content = "基于kotlin跨平台实现多平台一致运行"
                        }
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "动态化"
                            content = "动态下发编译产物达到AOT动态化"
                        }
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "去中心化"
                            content = "面向原生协议扩展原生任意UI组件和模块"
                        }
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "原生体验&生态"
                            content = "原生UI渲染和kotlin原生开发生态"
                        }
                    }
                    ScrollViewExamplePageView {
                        attr {
                            title = "多开发范式"
                            content = "类Compose声明式&传统命令式开发范式"
                        }
                    }
                }
                ScrollViewExampleHeader { attr { title = "Kuikly VerticalScroll Demo开发" } }
                Scroller {
                    attr {
                        height(135f)
                        margin(left = 32f, top = 4f, right = 32f, bottom = 4f)
                        showScrollerIndicator(true)
                        flexDirectionColumn()
                        alignItemsCenter()
                        backgroundColor(Color(0xFFF9F9F9))
                    }
                    Text {
                        attr {
                            margin(all = 8f)
                            color(Color.BLACK)
                            fontSize(12f)
                            text(ktvAndroidHelpText)
                            lines(Int.MAX_VALUE)
                        }
                    }
                }
                ScrollViewExampleHeader { attr { title = "这些App使用了Kuikly进行开发" } }
                Scroller {
                    attr {
                        height(84f)
                        margin(left = 32f, top = 4f, right = 32f, bottom = 4f)
                        showScrollerIndicator(true)
                        flexDirectionRow()
                        alignItemsCenter()
                    }
                    event {
                        scroll {
                            ctx.log("AuthorListViewDidScroll: (x: ${it.offsetX.toInt()}, y: ${it.offsetY.toInt()})")
                        }
                        scrollEnd {
                            ctx.log("AuthorListViewScrollEnd")
                        }
                        dragBegin {
                            ctx.log("AuthorListViewDragBegin")
                        }
                        dragEnd {
                            ctx.log("AuthorListViewDragEnd")
                        }
                    }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFDC143C)
                        appName = "石榴石"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFF87CEFA)
                        appName = "蓝宝石"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFF5F5DC)
                        appName = "碧玺石"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFDA70D6)
                        appName = "紫宝石"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFFF6347)
                        appName = "红玉晶"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFF0000CD)
                        appName = "海蓝晶"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFFFD700)
                        appName = "黄玉晶"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFF00FA9A)
                        appName = "绿翠玉"
                    } }
                    ScrollViewExampleAvatar { attr {
                        avatarColor = Color(0xFFF8F8FF)
                        appName = "白水晶"
                    } }
                }
            }
            ViewExampleSectionHeader { attr { title = "Event Log" } }
            View {
                attr {
                    height(216f)
                    margin(all = 16f)
                    padding(all = 4f)
                    border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                }
                Text {
                    attr {
                        bottom(0f)
                        lines(12)
                        color(Color.BLACK)
                        fontSize(12f)
                        text(ctx.logViewText)
                    }
                }
            }
        }
    }

    private fun log(logText: String) {
        logHistory.add(logText)
        if (logHistory.size > 12) {
            logHistory.removeFirst()
        }
        var text = ""
        for (log in logHistory) {
            text += log + "\n"
        }
        text.removeSuffix("\n")
        logViewText = text
    }
}