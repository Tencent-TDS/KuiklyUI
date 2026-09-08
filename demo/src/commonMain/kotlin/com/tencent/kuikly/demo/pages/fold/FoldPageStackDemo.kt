/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * [FoldPageStackController] 的四级页面示例。
 *
 * 这个页面刻意不做设置页的侧栏和大屏壳，只验证可复用组件自己的职责：
 * 动态入栈、逐级关闭、跨级关闭、侧滑返回以及系统返回键。
 */
@Page("FoldPageStackDemo")
internal class FoldPageStackDemo : BasePager() {

    private val navigator = FoldPageStackController(
        pager = this,
        rootId = "settings",
        rootTitle = "设置",
    ) { controller, _ ->
        foldDemoRootPage(controller)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(FOLD_DEMO_BG)
            }
            FoldPageStack(ctx.navigator)
        }
    }
}

private fun ViewContainer<*, *>.foldDemoRootPage(controller: FoldPageStackController) {
    foldDemoScreen(controller, "通用导航组件", showBack = false) {
        foldDemoIntro(
            "第 1 级",
            "下面每次点击都会调用 controller.open(...)。页面内容由调用方提供，组件只管理栈和转场。",
        )
        foldDemoGroup {
            foldDemoRow("无线局域网", "进入第 2 级") {
                controller.open("wifi", "无线局域网") { nav, _ ->
                    foldDemoWifiPage(nav)
                }
            }
            foldDemoRow("显示与亮度", "另一个第 2 级入口") {
                controller.open("display", "显示与亮度") { nav, _ ->
                    foldDemoLeafPage(nav, "显示与亮度", "这是从根页面打开的独立分支。")
                }
            }
            foldDemoRow("辅助功能", "可继续扩展任意深度", showSeparator = false) {
                controller.open("accessibility", "辅助功能") { nav, _ ->
                    foldDemoLeafPage(nav, "辅助功能", "业务状态可以继续留在原 Pager 或 ViewModel。")
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldDemoWifiPage(controller: FoldPageStackController) {
    foldDemoScreen(controller, "无线局域网") {
        foldDemoIntro("第 2 级", "侧滑或点返回会调用 close()，并在转场完成后卸掉栈顶页面。")
        foldDemoGroup {
            foldDemoRow("当前网络", "Kuikly Lab") {
                controller.open("known-networks", "已知网络") { nav, _ ->
                    foldDemoKnownNetworksPage(nav)
                }
            }
            foldDemoRow("询问是否加入网络", "通知", showSeparator = false) {
                controller.open("ask-to-join", "询问是否加入网络") { nav, _ ->
                    foldDemoLeafPage(nav, "询问是否加入网络", "这是一个普通的第 3 级页面。")
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldDemoKnownNetworksPage(controller: FoldPageStackController) {
    foldDemoScreen(controller, "已知网络") {
        foldDemoIntro("第 3 级", "栈中的页面都保持挂载，返回后前一页的滚动位置和局部状态不会重建。")
        foldDemoGroup {
            foldDemoRow("Kuikly Lab", "已连接 · WPA3", showSeparator = false) {
                controller.open(
                    id = "network-detail",
                    title = "Kuikly Lab",
                    data = "192.168.31.18",
                ) { nav, entry ->
                    foldDemoNetworkDetailPage(nav, entry.data as? String ?: "")
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldDemoNetworkDetailPage(
    controller: FoldPageStackController,
    address: String,
) {
    foldDemoScreen(controller, "Kuikly Lab") {
        foldDemoIntro(
            "第 4 级",
            "这里同时展示逐级关闭、跨两级关闭和一次回根。当前路径：${controller.pathTitles()}",
        )
        foldDemoGroup {
            foldDemoValueRow("IP 地址", address)
            foldDemoValueRow("安全性", "WPA3", showSeparator = false)
        }
        foldDemoAction("关闭当前页") {
            controller.close()
        }
        foldDemoAction("连续关闭两级") {
            controller.close(levels = 2)
        }
        foldDemoAction("返回根页面", destructive = true) {
            controller.closeToRoot()
        }
    }
}

private fun ViewContainer<*, *>.foldDemoLeafPage(
    controller: FoldPageStackController,
    title: String,
    message: String,
) {
    foldDemoScreen(controller, title) {
        foldDemoIntro("第 ${controller.depth + 1} 级", message)
        foldDemoAction("返回上一页") {
            controller.close()
        }
        foldDemoAction("返回根页面", destructive = true) {
            controller.closeToRoot()
        }
    }
}

private fun ViewContainer<*, *>.foldDemoScreen(
    controller: FoldPageStackController,
    title: String,
    showBack: Boolean = true,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flex(1f)
            backgroundColor(FOLD_DEMO_BG)
        }
        foldDemoNavigationBar(controller, title, showBack)
        List {
            attr {
                flex(1f)
                bouncesEnable(true)
                showScrollerIndicator(false)
                backgroundColor(FOLD_DEMO_BG)
            }
            View {
                attr {
                    padding(left = 18f, top = 20f, right = 18f, bottom = 42f)
                }
                content()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldDemoNavigationBar(
    controller: FoldPageStackController,
    title: String,
    showBack: Boolean,
) {
    View {
        attr {
            height(52f)
            flexDirectionRow()
            alignItemsCenter()
            justifyContentCenter()
            backgroundColor(FOLD_DEMO_BG)
        }
        if (showBack) {
            View {
                attr {
                    positionAbsolute()
                    left(10f)
                    top(7f)
                    height(38f)
                    paddingLeft(8f)
                    paddingRight(12f)
                    borderRadius(19f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(FOLD_DEMO_CARD)
                }
                event {
                    click {
                        controller.close()
                    }
                }
                Text {
                    attr {
                        text("‹")
                        fontSize(28f)
                        color(FOLD_DEMO_BLUE)
                        marginTop(-2f)
                    }
                }
                Text {
                    attr {
                        text("返回")
                        fontSize(15f)
                        color(FOLD_DEMO_BLUE)
                    }
                }
            }
        }
        Text {
            attr {
                text(title)
                fontSize(17f)
                fontWeightSemiBold()
                color(FOLD_DEMO_TEXT)
            }
        }
        Text {
            attr {
                positionAbsolute()
                right(16f)
                top(17f)
                text("${controller.depth + 1} / ${controller.entries.size}")
                fontSize(12f)
                color(FOLD_DEMO_SECONDARY)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldDemoIntro(level: String, message: String) {
    Text {
        attr {
            text(level)
            fontSize(30f)
            fontWeightBold()
            color(FOLD_DEMO_TEXT)
        }
    }
    Text {
        attr {
            text(message)
            marginTop(8f)
            marginBottom(22f)
            fontSize(14f)
            lineHeight(20f)
            color(FOLD_DEMO_SECONDARY)
        }
    }
}

private fun ViewContainer<*, *>.foldDemoGroup(content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            borderRadius(14f)
            overflow(true)
            backgroundColor(FOLD_DEMO_CARD)
            boxShadow(BoxShadow(0f, 1f, 8f, Color(0x0F000000)))
        }
        content()
    }
}

private fun ViewContainer<*, *>.foldDemoRow(
    title: String,
    subtitle: String,
    showSeparator: Boolean = true,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(62f)
            paddingLeft(16f)
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_DEMO_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        View {
            attr {
                flex(1f)
            }
            Text {
                attr {
                    text(title)
                    fontSize(16f)
                    color(FOLD_DEMO_TEXT)
                }
            }
            Text {
                attr {
                    text(subtitle)
                    marginTop(3f)
                    fontSize(12f)
                    color(FOLD_DEMO_SECONDARY)
                }
            }
        }
        Text {
            attr {
                text("›")
                fontSize(24f)
                color(FOLD_DEMO_CHEVRON)
            }
        }
        if (showSeparator) {
            foldDemoSeparator()
        }
    }
}

private fun ViewContainer<*, *>.foldDemoValueRow(
    title: String,
    value: String,
    showSeparator: Boolean = true,
) {
    View {
        attr {
            height(50f)
            paddingLeft(16f)
            paddingRight(16f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_DEMO_CARD)
        }
        Text {
            attr {
                text(title)
                flex(1f)
                fontSize(16f)
                color(FOLD_DEMO_TEXT)
            }
        }
        Text {
            attr {
                text(value)
                fontSize(15f)
                color(FOLD_DEMO_SECONDARY)
            }
        }
        if (showSeparator) {
            foldDemoSeparator()
        }
    }
}

private fun ViewContainer<*, *>.foldDemoSeparator() {
    View {
        attr {
            positionAbsolute()
            left(16f)
            right(0f)
            bottom(0f)
            height(0.33f)
            backgroundColor(FOLD_DEMO_SEPARATOR)
        }
    }
}

private fun ViewContainer<*, *>.foldDemoAction(
    title: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(48f)
            marginTop(16f)
            borderRadius(14f)
            allCenter()
            backgroundColor(FOLD_DEMO_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(title)
                fontSize(16f)
                color(if (destructive) FOLD_DEMO_RED else FOLD_DEMO_BLUE)
            }
        }
    }
}

private val FOLD_DEMO_BG = Color(0xFFF2F2F7)
private val FOLD_DEMO_CARD = Color.WHITE
private val FOLD_DEMO_TEXT = Color(0xFF1C1C1E)
private val FOLD_DEMO_SECONDARY = Color(0xFF7C7C80)
private val FOLD_DEMO_CHEVRON = Color(0xFFC3C3C8)
private val FOLD_DEMO_SEPARATOR = Color(0xFFE4E4E8)
private val FOLD_DEMO_BLUE = Color(0xFF0A84FF)
private val FOLD_DEMO_RED = Color(0xFFFF3B30)
