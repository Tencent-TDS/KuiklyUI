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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ScrollerView
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.layout.Column
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.views.ios.UIBarAxis
import com.tencent.kuikly.core.views.ios.UILargeTitleDisplayMode
import com.tencent.kuikly.core.views.ios.UINavigationBar
import com.tencent.kuikly.core.views.ios.UINavigationBarBackgroundStyle
import com.tencent.kuikly.core.views.ios.navbarItems
import com.tencent.kuikly.core.views.ios.text
import com.tencent.kuikly.core.views.ios.titleContent
import com.tencent.kuikly.core.views.ios.UIBarButtonItemPlacement
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationBehavior
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationRestoration
import com.tencent.kuikly.core.views.ios.UINavigationBarMinimizationSafeAreaAdjustment
import com.tencent.kuikly.core.views.ios.UIToolbar
import com.tencent.kuikly.core.views.ios.UIToolbarItemKind
import com.tencent.kuikly.core.views.ios.toolbarItem
import com.tencent.kuikly.core.views.ios.toolbarItemContent

/**
 * [UINavigationBar] 完整示例：
 * - `items { navbarItem }` leading 作返回（自行 `closePage`）
 * - `title { text / titleContent }` 中间区域
 * - `items { vif/velse { navbarItems } }` 整段切换 trailing（双 itemGroup 示例）
 * - `items { }` 右侧 `itemGroup` / `index` → `trailingItemGroups`
 * - `attr { contentScroll / navigationBarMinimization }`（iOS 15 / 27+）
 */
/** Demo：contentScroll 绑定谁（默认不传，交给 UIKit 自动查找）。 */
private const val NAV_DEMO_ICON_PRIMARY = "panda2.png"
private const val NAV_DEMO_ICON_ALT = "penguin2.png"
private const val NAV_DEMO_ICON_REVERT_MS = 3000

internal enum class UINavigationBarContentScrollTarget(val value: String) {
    None("none"),
    OuterScroller("scroller"),
    NestedList("list"),
}

/** 导航标题槽：`title { titleContent }` 胶囊 / 搜索框，或 `title { text }` 系统字符串。 */
internal enum class UINavigationBarDemoTitleMode {
    ContentPill,
    ContentSearch,
    SystemText,
}

@Page("UINavigationBarDemo")
internal class UINavigationBarDemoPage : BasePager() {

    var channelName by observable("Kuikly 频道")
    var channelStatus by observable("3 人在线")
    var unreadCount by observable(5)
    var navTint by observable(Color(0xFF007AFF))
    var navbarItemTint by observable(Color(0xFFFF9500))
    var lastAction by observable("等待操作…")
    var titleMode by observable(UINavigationBarDemoTitleMode.ContentPill)
    var titleSearchText by observable("")
    var scrollNativeRef by observable(0)
    var nestedListNativeRef by observable(0)

    var minimizationBehavior by observable(UINavigationBarMinimizationBehavior.OnScrollDown)
    var barBackgroundStyle by observable(UINavigationBarBackgroundStyle.Transparent)
    var contentScrollTarget by observable(UINavigationBarContentScrollTarget.None)

    var showNavigationBar by observable(true)
    var showNavBackButton by observable(true)
    var showNavItemMore by observable(true)
    var showNavItemTrailingMore by observable(true)
    var showDemoToolbar by observable(true)
    var showToolbarCustomItem by observable(true)
    var toolbarSwapIconName by observable(NAV_DEMO_ICON_PRIMARY)
    private var toolbarSwapRevertToken = 0
    /**
     * 开启后：分享 + 更多同一 `itemGroup=0`、不同 `index`（一颗胶囊里并排）。
     * 关闭时：企鹅 `itemGroup=0` + 更多 `itemGroup=1`（两颗独立胶囊，iOS 16+）。
     */
    var useTrailingDualItemGroups by observable(false)


    /** navbarItemContent index=0：点击切到 [NAV_DEMO_ICON_ALT]，3s 后回到 [NAV_DEMO_ICON_PRIMARY]。 */
    var navSwapIconName by observable(NAV_DEMO_ICON_PRIMARY)
    private var navSwapRevertToken = 0

    var scrollRef: ViewRef<ScrollerView<*, *>>? = null
    var nestedListRef: ViewRef<ListView<*, *>>? = null

    internal val channelPresets = listOf(
        "Kuikly 频道" to "3 人在线",
        "折叠屏实验室" to "讨论中",
        "System Nav" to "仅演示",
    )

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(0xFFF2F2F7))
                paddingLeft(ctx.pageData.safeAreaInsets.left)
                paddingRight(ctx.pageData.safeAreaInsets.right)
            }
            // 与旧 Demo 一致：Scroller 先声明；导航为 absolute 占位壳，放后面避免 KRView 挡列表点击。
            Scroller {
                attr {
                    flex(1f)
                }
                ref {
                    ctx.scrollRef = it
                    if (ctx.scrollNativeRef != it.nativeRef) {
                        ctx.scrollNativeRef = it.nativeRef
                    }
                }
                navigationDemoScrollBody(ctx)
            }
            uiNavigationBarDemo(ctx)
            uiNavigationBarToolbarDemo(ctx)
        }
    }

    internal fun bump(message: String) {
        lastAction = message
        println("UINavigationBarDemo: $message")
    }

    fun cycleChannel() {
        val idx = channelPresets.indexOfFirst { it.first == channelName }
        val next = channelPresets[(idx + 1).coerceAtLeast(0) % channelPresets.size]
        channelName = next.first
        channelStatus = next.second
        bump("点击标题胶囊 → ${next.first}")
    }

    fun onNavSwapIconClick() {
        navSwapIconName = NAV_DEMO_ICON_ALT
        bump("导航图标 → $NAV_DEMO_ICON_ALT（${NAV_DEMO_ICON_REVERT_MS / 1000}s 后恢复）")
        val token = ++navSwapRevertToken
        setTimeout(NAV_DEMO_ICON_REVERT_MS) {
            if (token != navSwapRevertToken) {
                return@setTimeout
            }
            navSwapIconName = NAV_DEMO_ICON_PRIMARY
            bump("导航图标恢复 → $NAV_DEMO_ICON_PRIMARY")
        }
    }

    fun onToolbarSwapIconClick() {
        toolbarSwapIconName = NAV_DEMO_ICON_ALT
        bump("Toolbar 自定义图标 → $NAV_DEMO_ICON_ALT（${NAV_DEMO_ICON_REVERT_MS / 1000}s 后恢复）")
        val token = ++toolbarSwapRevertToken
        setTimeout(NAV_DEMO_ICON_REVERT_MS) {
            if (token != toolbarSwapRevertToken) {
                return@setTimeout
            }
            toolbarSwapIconName = NAV_DEMO_ICON_PRIMARY
            bump("Toolbar 自定义图标恢复 → $NAV_DEMO_ICON_PRIMARY")
        }
    }

    fun applyTrailingDualItemGroups(enabled: Boolean) {
        useTrailingDualItemGroups = enabled
    }

    fun onDualTrailingNavClick(id: String) {
        when (id) {
            "share" -> bump("分享（双 itemGroup）")
            "more" -> bump("更多（双 itemGroup）")
            else -> bump("trailing · $id")
        }
    }
}

private fun ViewContainer<*, *>.uiNavigationBarDemo(ctx: UINavigationBarDemoPage) {
    UINavigationBar {
        attr {
            visibility(ctx.showNavigationBar)
            tintColor(ctx.navTint)
            when (ctx.contentScrollTarget) {
                UINavigationBarContentScrollTarget.None -> contentScroll(null)
                UINavigationBarContentScrollTarget.OuterScroller ->
                    contentScrollNativeRef(ctx.scrollNativeRef)
                UINavigationBarContentScrollTarget.NestedList ->
                    contentScrollNativeRef(ctx.nestedListNativeRef)
            }
            navigationBarMinimizationBehavior(ctx.minimizationBehavior)
            navigationBarMinimizationSafeAreaAdjustment(
                UINavigationBarMinimizationSafeAreaAdjustment.Enabled,
            )
            navigationBarMinimizationRestoration(
                UINavigationBarMinimizationRestoration.Automatic,
            )
            barBackground(ctx.barBackgroundStyle)
        }
        title {
            vif({ ctx.titleMode == UINavigationBarDemoTitleMode.SystemText }) {
                text {
                    attr {
                        title(ctx.channelName)
                        subtitle(ctx.channelStatus)
                        largeTitle(UILargeTitleDisplayMode.Never)
                    }
                }
            }
            velse {
                titleContent {
                    attr {
                        width(200f)
                        height(44f)
                    }
                    vif({ ctx.titleMode == UINavigationBarDemoTitleMode.ContentSearch }) {
                        demoNavigationTitleSearch(ctx)
                    }
                    velse {
                        demoNavigationTitlePill(ctx)
                    }
                }
            }
        }
        items {
            vif({ ctx.useTrailingDualItemGroups }) {
                navbarItems {
                    navbarItem {
                        attr {
                            symbol("chevron.backward")
                            placement(UIBarButtonItemPlacement.Leading)
                            index(0)
                            tintColor(ctx.navbarItemTint)
                            visibility(ctx.showNavBackButton)
                            axis(UIBarAxis.VerticalPreferred)
                        }
                        event {
                            click {
                                ctx.bump("返回")
                                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                            }
                        }
                    }
                    navbarItem {
                        attr {
                            symbol("square.and.arrow.up")
                            placement(UIBarButtonItemPlacement.Trailing)
                            itemGroup(0)
                            index(0)
                            title("分享")
                            tintColor(ctx.navbarItemTint)
                            axis(UIBarAxis.VerticalPreferred)
                            visibility(true)
                        }
                        event {
                            click { ctx.onDualTrailingNavClick("share") }
                        }
                    }
                    navbarItem {
                        attr {
                            symbol("ellipsis.circle")
                            placement(UIBarButtonItemPlacement.Trailing)
                            itemGroup(0)
                            index(1)
                            title("更多")
                            tintColor(ctx.navbarItemTint)
                            axis(UIBarAxis.VerticalPreferred)
                            visibility(true)
                        }
                        event {
                            click { ctx.onDualTrailingNavClick("more") }
                        }
                    }
                }
            }
            velse {
                navbarItems {
                    navbarItem {
                        attr {
                            symbol("chevron.backward")
                            placement(UIBarButtonItemPlacement.Leading)
                            index(0)
                            tintColor(ctx.navbarItemTint)
                            visibility(ctx.showNavBackButton)
                            axis(UIBarAxis.VerticalPreferred)
                        }
                        event {
                            click {
                                ctx.bump("返回")
                                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                            }
                        }
                    }
                    navbarItemContent {
                        attr {
                            placement(UIBarButtonItemPlacement.Trailing)
                            itemGroup(0)
                            index(0)
                            title("切换图标")
                            axis(UIBarAxis.VerticalPreferred)
                            visibility(ctx.showNavItemMore)
                            height(44f)
                            width(44f)
                            flexDirectionRow()
                            alignItemsCenter()
                            justifyContentCenter()
                            edgeInsetTopAuto()
                        }
                        event {
                            click { ctx.onNavSwapIconClick() }
                        }
                        Image {
                            attr {
                                size(26f, 26f)
                                resizeContain()
                                src(ImageUri.commonAssets(ctx.navSwapIconName))
                            }
                        }
                    }
                    navbarItem {
                        attr {
                            symbol("ellipsis.circle")
                            placement(UIBarButtonItemPlacement.Trailing)
                            itemGroup(1)
                            index(0)
                            title("更多")
                            tintColor(ctx.navbarItemTint)
                            axis(UIBarAxis.VerticalPreferred)
                            visibility(ctx.showNavItemTrailingMore)
                        }
                        event {
                            click {
                                ctx.bump("更多")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.uiNavigationBarToolbarDemo(ctx: UINavigationBarDemoPage) {
    vif({ ctx.showDemoToolbar }) {
        UIToolbar {
            attr {
                visibility(true)
                tintColor(ctx.navTint)
            }
            items {
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.FlexibleSpace)
                        index(0)
                        visibility(true)
                    }
                }
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.Item)
                        symbol("trash")
                        index(1)
                        tintColor(ctx.navTint)
                        axis(UIBarAxis.VerticalPreferred)
                        visibility(true)
                    }
                    event {
                        click { ctx.bump("工具栏 · 删除") }
                    }
                }
                vif({ ctx.showToolbarCustomItem }) {
                    toolbarItemContent {
                        attr {
                            title("切换图标")
                            index(2)
                            axis(UIBarAxis.VerticalPreferred)
                            width(44f)
                            height(44f)
                            flexDirectionRow()
                            alignItemsCenter()
                            justifyContentCenter()
                            visibility(true)
                        }
                        event {
                            click { ctx.onToolbarSwapIconClick() }
                        }
                        Image {
                            attr {
                                size(26f, 26f)
                                resizeContain()
                                src(ImageUri.commonAssets(ctx.toolbarSwapIconName))
                            }
                        }
                    }
                }
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.Item)
                        symbol("square.and.pencil")
                        index(3)
                        tintColor(ctx.navTint)
                        axis(UIBarAxis.VerticalPreferred)
                        visibility(true)
                    }
                    event {
                        click { ctx.bump("工具栏 · 撰写") }
                    }
                }
                toolbarItem {
                    attr {
                        kind(UIToolbarItemKind.FlexibleSpace)
                        index(4)
                        visibility(true)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.navigationDemoScrollBody(ctx: UINavigationBarDemoPage) {
    View {
        attr {
            paddingTop(ctx.pageData.statusBarHeight + 56f)
            paddingBottom(ctx.pageData.safeAreaInsets.bottom + 24f)
            paddingLeft(20f)
            paddingRight(20f)
        }
        Text {
            attr {
                text("UINavigationBar")
                fontSize(34f)
                fontWeightBold()
                color(Color.BLACK)
            }
        }
        Text {
            attr {
                marginTop(8f)
                text("外层 Scroller + 内嵌 List；默认不传 contentScroll，可切换绑定不同滚动源对比栏联动。")
                fontSize(15f)
                color(Color(0xFF6C6C70))
                lineHeight(22f)
            }
        }
        demoSection("滚动联动（contentScroll + minimization）") {
            Text {
                attr {
                    text(
                        "Scroller { ref { scrollRef = it } }\n" +
                            "UINavigationBar {\n" +
                            "  bar { attr {\n" +
                            "    contentScroll(scrollRef)\n" +
                            "    navigationBarMinimizationBehavior(…)\n" +
                            "  } }\n" +
                            "}",
                    )
                    fontSize(12f)
                    fontFamily("Menlo")
                    color(Color(0xFF3C3C43))
                    lineHeight(18f)
                }
            }
            vbind({ ctx.minimizationBehavior }) {
                Text {
                    attr {
                        marginTop(10f)
                        text("当前 minimization：${ctx.minimizationBehavior.value}（iOS 27+ 生效）")
                        fontSize(14f)
                        fontWeightSemiBold()
                        color(Color(0xFF007AFF))
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        text("切换后向下滚动列表，对比导航栏是否收起。")
                        fontSize(13f)
                        color(Color(0xFF8E8E93))
                    }
                }
                vbind({ ctx.minimizationBehavior }) {
                    Row {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        minimizationChip(ctx, "默认", UINavigationBarMinimizationBehavior.Automatic)
                        minimizationChip(ctx, "不收", UINavigationBarMinimizationBehavior.Never)
                        minimizationChip(ctx, "下滑收", UINavigationBarMinimizationBehavior.OnScrollDown)
                        minimizationChip(ctx, "上滑收", UINavigationBarMinimizationBehavior.OnScrollUp)
                    }
                }
            }
            vbind({ ctx.contentScrollBindKey() }) {
                Text {
                    attr {
                        marginTop(10f)
                        text(contentScrollTargetSummary(ctx))
                        fontSize(12f)
                        color(Color(0xFF8E8E93))
                        lineHeight(18f)
                    }
                }
                Row {
                    attr {
                        marginTop(8f)
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    contentScrollTargetChip(ctx, "不传（默认）", UINavigationBarContentScrollTarget.None)
                    contentScrollTargetChip(ctx, "外层 Scroller", UINavigationBarContentScrollTarget.OuterScroller)
                    contentScrollTargetChip(ctx, "内嵌 List", UINavigationBarContentScrollTarget.NestedList)
                }
            }
            vbind({ ctx.barBackgroundStyle }) {
                Text {
                    attr {
                        marginTop(16f)
                        text("栏背景 configure：${ctx.barBackgroundStyle.value}")
                        fontSize(14f)
                        fontWeightSemiBold()
                        color(Color(0xFF007AFF))
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        text("default=磨砂 / opaque=不透明栏色 / transparent=三层置空")
                        fontSize(13f)
                        color(Color(0xFF8E8E93))
                    }
                }
                Row {
                    attr {
                        marginTop(10f)
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    barBackgroundChip(ctx, "磨砂", UINavigationBarBackgroundStyle.Default)
                    barBackgroundChip(ctx, "不透明", UINavigationBarBackgroundStyle.Opaque)
                    barBackgroundChip(ctx, "透明", UINavigationBarBackgroundStyle.Transparent)
                }
            }
        }
        demoSection("内嵌 List（独立滚动）") {
            Text {
                attr {
                    text(
                        "在 Scroller 里再套一层 List。绑定「内嵌 List」时，应主要随灰区列表滚动联动；" +
                            "绑定「外层 Scroller」时随整页滚动；「不传」看 UIKit 自动选中哪一个。",
                    )
                    fontSize(13f)
                    color(Color(0xFF6C6C70))
                    lineHeight(19f)
                }
            }
            List {
                attr {
                    marginTop(10f)
                    height(220f)
                    borderRadius(12f)
                    backgroundColor(Color(0xFFE5E5EA))
                    showScrollerIndicator(true)
                }
                ref {
                    ctx.nestedListRef = it
                    if (ctx.nestedListNativeRef != it.nativeRef) {
                        ctx.nestedListNativeRef = it.nativeRef
                    }
                }
                repeat(16) { index ->
                    View {
                        attr {
                            height(44f)
                            paddingLeft(14f)
                            paddingRight(14f)
                            flexDirectionRow()
                            alignItemsCenter()
                            backgroundColor(
                                if (index % 2 == 0) Color(0xFFF9F9FB) else Color(0xFFEFEFF4),
                            )
                        }
                        Text {
                            attr {
                                text("内嵌 List · 行 ${index + 1}")
                                fontSize(14f)
                                color(Color(0xFF1C1C1E))
                            }
                        }
                    }
                }
            }
            Text {
                attr {
                    marginTop(8f)
                    text("内嵌 List nativeRef = ${ctx.nestedListNativeRef} · 外层 Scroller ref = ${ctx.scrollNativeRef}")
                    fontSize(11f)
                    fontFamily("Menlo")
                    color(Color(0xFF8E8E93))
                }
            }
        }
        demoSection("显隐 visibility") {
            Text {
                attr {
                    text(
                        "Bar：attr { visibility } → setNavigationBarHidden\n" +
                            "navbarItem / navbarItemContent：visibility → 安装或卸载 UIBarButtonItem\n" +
                            "Demo 宿主：本页 fd_prefersNavigationBarHidden=NO；其它 Kuikly 页为 YES（不露出空导航栏）",
                    )
                    fontSize(12f)
                    fontFamily("Menlo")
                    color(Color(0xFF6C6C70))
                    lineHeight(18f)
                }
            }
            vbind({
                "${ctx.showNavigationBar}|${ctx.showNavBackButton}|" +
                    "${ctx.showNavItemMore}|${ctx.showNavItemTrailingMore}"
            }) {
                demoShowSwitchRow("显示导航栏 (Bar.visibility)", { ctx.showNavigationBar }) {
                    ctx.showNavigationBar = it
                    ctx.bump("Bar.visibility = $it")
                }
                demoShowSwitchRow("显示返回键 (leading navbarItem.visibility)", { ctx.showNavBackButton }) {
                    ctx.showNavBackButton = it
                    ctx.bump("返回键.visibility = $it")
                }
                demoShowSwitchRow("显示右侧切换图标 (navbarItemContent.visibility)", { ctx.showNavItemMore }) {
                    ctx.showNavItemMore = it
                    ctx.bump("切换图标.visibility = $it")
                }
                demoShowSwitchRow("显示右侧「更多」 (navbarItem.visibility)", { ctx.showNavItemTrailingMore }) {
                    ctx.showNavItemTrailingMore = it
                    ctx.bump("更多.visibility = $it")
                }
                demoShowSwitchRow("显示底部工具栏 (Toolbar.visibility)", { ctx.showDemoToolbar }) {
                    ctx.showDemoToolbar = it
                    ctx.bump("Toolbar.visibility = $it")
                }
                demoShowSwitchRow("显示自定义 Toolbar 按钮 (toolbarItemContent)", { ctx.showToolbarCustomItem }) {
                    ctx.showToolbarCustomItem = it
                    ctx.bump("toolbarItemContent.visibility = $it")
                }
            }
        }
        demoSection("itemGroup / index（Trailing）") {
            Text {
                attr {
                    text(
                        "itemGroup → Native css_itemGroup → UIBarButtonItemGroup（簇间间距）。\n" +
                            "index → UIBarButtonItem.tag（同一 itemGroup 内排序）。\n" +
                            "仅 itemGroup=0 且全为 0 时走扁平 rightBarButtonItems；否则 trailingItemGroups（iOS 16+）。",
                    )
                    fontSize(13f)
                    fontFamily("Menlo")
                    color(Color(0xFF6C6C70))
                    lineHeight(19f)
                }
            }
            Text {
                attr {
                    marginTop(8f)
                    text(
                        "默认示例：企鹅 navbarItemContent itemGroup=0 · 更多 ellipsis.circle navbarItem itemGroup=1 index=0。",
                    )
                    fontSize(14f)
                    color(Color(0xFF6C6C70))
                    lineHeight(20f)
                }
            }
        }
        demoSection("Trailing 合并 / 拆分（itemGroup）") {
            Text {
                attr {
                    text(
                        "关：企鹅 navbarItemContent itemGroup=0 + 更多 navbarItem itemGroup=1 → 两颗独立胶囊。\n" +
                            "开：分享 + 更多同为 itemGroup=0、index 0/1 → 同一簇并排。\n" +
                            "切换：`items { vif/velse { navbarItems } }`；关=velse，开=vif。",
                    )
                    fontSize(14f)
                    color(Color(0xFF6C6C70))
                    lineHeight(20f)
                }
            }
            demoShowSwitchRow("双 itemGroup 示例", { ctx.useTrailingDualItemGroups }) {
                ctx.applyTrailingDualItemGroups(it)
                ctx.bump("useTrailingDualItemGroups = $it · navbarItems 整段切换")
            }
        }
        demoSection("navbarItem 图标 / 自定义 View") {
            Text {
                attr {
                    text(
                        "切换图标：common/$NAV_DEMO_ICON_PRIMARY，点击切 $NAV_DEMO_ICON_ALT，" +
                            "${NAV_DEMO_ICON_REVERT_MS / 1000}s 后恢复。\n" +
                            "（双 itemGroup 开关开启时隐藏本组。）",
                    )
                    fontSize(14f)
                    color(Color(0xFF6C6C70))
                    lineHeight(20f)
                }
            }
        }
        demoSection("标题 title { text / titleContent }") {
            Text {
                attr {
                    text(
                        "title { titleContent }：自定义 titleView（胶囊 / 搜索，块内可用 vif）。\n" +
                            "title { text { attr } }：系统 title + subtitle。\n" +
                            "text/titleContent 整段切换：title { vif/velse }；titleContent 块内可再 vif（搜索/胶囊）。",
                    )
                    fontSize(14f)
                    color(Color(0xFF6C6C70))
                    lineHeight(20f)
                }
            }
            vbind({ ctx.titleMode }) {
                Row {
                    attr {
                        marginTop(10f)
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    demoChip(
                        "频道胶囊",
                        selected = ctx.titleMode == UINavigationBarDemoTitleMode.ContentPill,
                    ) {
                        ctx.titleMode = UINavigationBarDemoTitleMode.ContentPill
                        ctx.bump("标题：title.titleContent · 频道胶囊")
                    }
                    demoChip(
                        "搜索输入框",
                        selected = ctx.titleMode == UINavigationBarDemoTitleMode.ContentSearch,
                    ) {
                        ctx.titleMode = UINavigationBarDemoTitleMode.ContentSearch
                        ctx.bump("标题：title.titleContent · 搜索 Input")
                    }
                    demoChip(
                        "系统 title.text",
                        selected = ctx.titleMode == UINavigationBarDemoTitleMode.SystemText,
                    ) {
                        ctx.titleMode = UINavigationBarDemoTitleMode.SystemText
                        ctx.bump("标题：title.text · 系统字符串")
                    }
                }
            }
            vbind({ "${ctx.channelName}|${ctx.channelStatus}" }) {
                Row {
                    attr {
                        marginTop(12f)
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    ctx.channelPresets.forEach { (name, status) ->
                        demoChip(name, selected = ctx.channelName == name) {
                            ctx.channelName = name
                            ctx.channelStatus = status
                            ctx.unreadCount = (ctx.unreadCount + 1).coerceAtMost(99)
                            ctx.bump("频道 → $name")
                        }
                    }
                }
            }
        }
        demoSection("Bar tintColor") {
            Text {
                attr {
                    text(
                        "bar { attr { tintColor } } → UINavigationBar.tintColor。\n" +
                            "影响：系统 title.text 标题色（未设 titleColor 时跟 tint）、部分系统控件；\n" +
                            "不影响：title.titleContent 自定义中间区（Kuikly 子 View 需自己设色）、navbarItem 已单独 tintColor 的按钮。",
                    )
                    fontSize(13f)
                    color(Color(0xFF8E8E93))
                    marginBottom(8f)
                }
            }
            vbind({ ctx.navTint }) {
                Row {
                    attr { flexDirectionRow() }
                    demoColorChip(Color(0xFF007AFF), ctx.navTint == Color(0xFF007AFF)) {
                        ctx.navTint = Color(0xFF007AFF)
                        ctx.bump("Bar tint 蓝")
                    }
                    demoColorChip(Color(0xFF34C759), ctx.navTint == Color(0xFF34C759)) {
                        ctx.navTint = Color(0xFF34C759)
                        ctx.bump("Bar tint 绿")
                    }
                    demoColorChip(Color(0xFFFF9500), ctx.navTint == Color(0xFFFF9500)) {
                        ctx.navTint = Color(0xFFFF9500)
                        ctx.bump("Bar tint 橙")
                    }
                    demoColorChip(Color(0xFFAF52DE), ctx.navTint == Color(0xFFAF52DE)) {
                        ctx.navTint = Color(0xFFAF52DE)
                        ctx.bump("Bar tint 紫")
                    }
                }
            }
        }
        demoSection("navbarItem tintColor") {
            Text {
                attr {
                    text(
                        "navbarItem { attr { tintColor } } → 单颗 UIBarButtonItem；\n" +
                            "本页：返回 / 铃铛 / 分享 / 更多用 navbarItemTint，与 Bar tint 可不同。",
                    )
                    fontSize(13f)
                    color(Color(0xFF8E8E93))
                    lineHeight(18f)
                    marginBottom(8f)
                }
            }
            vbind({ ctx.navbarItemTint }) {
                Row {
                    attr { flexDirectionRow() }
                    demoColorChip(Color(0xFF007AFF), ctx.navbarItemTint == Color(0xFF007AFF)) {
                        ctx.navbarItemTint = Color(0xFF007AFF)
                        ctx.bump("Item tint 蓝")
                    }
                    demoColorChip(Color(0xFF34C759), ctx.navbarItemTint == Color(0xFF34C759)) {
                        ctx.navbarItemTint = Color(0xFF34C759)
                        ctx.bump("Item tint 绿")
                    }
                    demoColorChip(Color(0xFFFF9500), ctx.navbarItemTint == Color(0xFFFF9500)) {
                        ctx.navbarItemTint = Color(0xFFFF9500)
                        ctx.bump("Item tint 橙")
                    }
                    demoColorChip(Color(0xFFFF3B30), ctx.navbarItemTint == Color(0xFFFF3B30)) {
                        ctx.navbarItemTint = Color(0xFFFF3B30)
                        ctx.bump("Item tint 红")
                    }
                }
            }
        }
        demoSection("最近操作") {
            Text {
                attr {
                    text(ctx.lastAction)
                    fontSize(16f)
                    fontWeightMedium()
                    color(Color(0xFF1C1C1E))
                }
            }
        }
        repeat(12) { index ->
            View {
                attr {
                    marginTop(12f)
                    height(72f)
                    borderRadius(12f)
                    backgroundColor(Color.WHITE)
                    allCenter()
                }
                Text {
                    attr {
                        text("向下滚动体验 navigationBarMinimization · ${index + 1}")
                        fontSize(15f)
                        color(Color(0xFF3C3C43))
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.demoNavigationTitleSearch(ctx: UINavigationBarDemoPage) {
    View {
        attr {
            flex(1f)
            height(36f)
            marginTop(4f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(10f)
            backgroundColor(Color(0xFFE8E8ED))
            flexDirectionRow()
            alignItemsCenter()
        }
        Input {
            attr {
                flex(1f)
                height(32f)
                fontSize(15f)
                placeholder("搜索 ${ctx.channelName}")
                text(ctx.titleSearchText)
            }
            event {
                textDidChange { params ->
                    ctx.titleSearchText = params.text
                }
                inputReturn {
                    ctx.titleMode = UINavigationBarDemoTitleMode.ContentPill
                    ctx.bump("搜索完成：${ctx.titleSearchText}")
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.demoNavigationTitlePill(ctx: UINavigationBarDemoPage) {
    View {
        attr {
            flex(1f)
            height(36f)
            marginTop(4f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(18f)
            backgroundColor(Color(0xFFE8E8ED))
            flexDirectionRow()
            alignItemsCenter()
            positionRelative()
        }
        event {
            click { ctx.cycleChannel() }
        }
        View {
            attr {
                size(28f, 28f)
                borderRadius(14f)
                backgroundColor(ctx.navTint)
                allCenter()
            }
            Text {
                attr {
                    text(ctx.channelName.take(1).uppercase())
                    fontSize(14f)
                    fontWeightBold()
                    color(Color.WHITE)
                }
            }
        }
        Column {
            attr {
                flex(1f)
                marginLeft(8f)
                marginRight(6f)
                justifyContentCenter()
            }
            Text {
                attr {
                    text(ctx.channelName)
                    fontSize(15f)
                    fontWeightSemiBold()
                    color(Color.BLACK)
                }
            }
            Text {
                attr {
                    text(ctx.channelStatus)
                    fontSize(11f)
                    color(Color(0xFF8E8E93))
                }
            }
        }
        Text {
            attr {
                text("▾")
                fontSize(12f)
                color(Color(0xFF8E8E93))
                marginRight(2f)
            }
        }
        vif({ ctx.unreadCount > 0 }) {
            View {
                attr {
                    absolutePosition(top = 2f, right = 6f)
                    paddingLeft(5f)
                    paddingRight(5f)
                    height(16f)
                    minWidth(16f)
                    borderRadius(8f)
                    backgroundColor(Color(0xFFFF3B30))
                    allCenter()
                }
                Text {
                    attr {
                        val count = ctx.unreadCount
                        text(if (count > 99) "99+" else count.toString())
                        fontSize(10f)
                        fontWeightSemiBold()
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}

/** vbind 比较 bindValue 返回值；勿用 `a; b; c` 否则只比最后一个。 */
private fun UINavigationBarDemoPage.contentScrollBindKey(): String =
    "${contentScrollTarget.name}|$scrollNativeRef|$nestedListNativeRef"

private fun contentScrollTargetSummary(ctx: UINavigationBarDemoPage): String {
    return when (ctx.contentScrollTarget) {
        UINavigationBarContentScrollTarget.None ->
            "contentScroll：未传（native 不 set，UIKit 可自动从层级选 Scroller）"
        UINavigationBarContentScrollTarget.OuterScroller ->
            "contentScroll：外层 Scroller ref = ${ctx.scrollNativeRef}"
        UINavigationBarContentScrollTarget.NestedList ->
            "contentScroll：内嵌 List ref = ${ctx.nestedListNativeRef}"
    }
}

private fun ViewContainer<*, *>.contentScrollTargetChip(
    ctx: UINavigationBarDemoPage,
    label: String,
    target: UINavigationBarContentScrollTarget,
) {
    demoChip(label, selected = ctx.contentScrollTarget == target) {
        ctx.contentScrollTarget = target
        ctx.bump("contentScroll → ${target.value}")
    }
}

private fun ViewContainer<*, *>.minimizationChip(
    ctx: UINavigationBarDemoPage,
    label: String,
    behavior: UINavigationBarMinimizationBehavior,
) {
    demoChip(label, selected = ctx.minimizationBehavior == behavior) {
        ctx.minimizationBehavior = behavior
        ctx.bump("minimization → ${behavior.value}")
    }
}

private fun ViewContainer<*, *>.barBackgroundChip(
    ctx: UINavigationBarDemoPage,
    label: String,
    style: UINavigationBarBackgroundStyle,
) {
    demoChip(label, selected = ctx.barBackgroundStyle == style) {
        ctx.barBackgroundStyle = style
        ctx.bump("barBackground → ${style.value}")
    }
}

private fun ViewContainer<*, *>.demoSection(title: String, block: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            marginTop(20f)
            padding(16f)
            borderRadius(12f)
            backgroundColor(Color.WHITE)
        }
        Text {
            attr {
                text(title)
                fontSize(13f)
                fontWeightSemiBold()
                color(Color(0xFF8E8E93))
            }
        }
        block()
    }
}

private fun ViewContainer<*, *>.demoShowSwitchRow(
    title: String,
    isOn: () -> Boolean,
    onChanged: (Boolean) -> Unit,
) {
    View {
        attr {
            marginTop(10f)
            height(44f)
            flexDirectionRow()
            alignItemsCenter()
        }
        Text {
            attr {
                text(title)
                flex(1f)
                fontSize(15f)
                color(Color(0xFF1C1C1E))
            }
        }
        Switch {
            attr {
                size(51f, 31f)
                isOn(isOn())
                onColor(Color(0xFF34C759))
                unOnColor(Color(0xFFE9E9EB))
                thumbColor(Color.WHITE)
            }
            event {
                // 点击时再读 isOn()，避免闭包捕获旧 Boolean 导致第二次起永远写成同一值
                switchOnChanged {
                    onChanged(!isOn())
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.demoChip(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    View {
        attr {
            marginRight(8f)
            marginTop(8f)
            paddingLeft(12f)
            paddingRight(12f)
            height(36f)
            borderRadius(18f)
            backgroundColor(if (selected) Color(0xFFD6E8FF) else Color(0xFFE5E5EA))
            if (selected) {
                border(Border(1f, BorderStyle.SOLID, Color(0xFF007AFF)))
            }
            allCenter()
        }
        Text {
            attr {
                text(label)
                fontSize(14f)
                color(if (selected) Color(0xFF007AFF) else Color(0xFF3C3C43))
                if (selected) {
                    fontWeightSemiBold()
                } else {
                    fontWeightNormal()
                }
            }
        }
        event {
            click { onClick() }
        }
    }
}

private fun ViewContainer<*, *>.demoColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    View {
        attr {
            marginRight(10f)
            size(36f, 36f)
            borderRadius(18f)
            backgroundColor(color)
            if (selected) {
                border(Border(3f, BorderStyle.SOLID, Color.WHITE))
            }
        }
        event {
            click { onClick() }
        }
    }
}
