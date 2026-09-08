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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 用 [FoldSplitStackController] 搭的自适应设置页，导航行为对齐设置页原型。
 *
 * 小屏：master 根列表 → 一级详情 → 选项 → 更深页面，左右滑逐级返回。
 * 大屏：master 变成左侧玻璃浮窗，一级详情钉在舞台上，更深页面盖上来时它做画廊后退。
 *
 * 组件只负责窗口尺寸和页面状态；这里的玻璃、圆角、阴影、蒙版和跳转策略都由本页面决定。
 */
@Page("FoldAdaptiveSettingsDemo")
internal class FoldAdaptiveSettingsDemo : BasePager() {

    var selectedSection by observable(FOLD_DEFAULT_SECTION)

    /** 详情列表的滚动位置，用来让置顶标题跟着滚动淡入。 */
    var detailScrollY by observable(0f)

    /** 侧栏里直接带开关的两行，对齐系统的飞行模式与 VPN。 */
    var airplaneOn by observable(false)
    var vpnOn by observable(false)

    var primaryOption by observable(true)
    var secondaryOption by observable(false)
    var automaticOption by observable(true)

    private val navigator = FoldSplitStackController(
        pager = this,
        layout = FoldSplitStackLayout(
            masterWidth = MASTER_WIDTH,
            masterMinWidth = MASTER_MIN_WIDTH,
            masterMaxWidth = MASTER_MAX_WIDTH,
            masterInset = MASTER_INSET,
            detailMinWidth = CONTENT_MIN_WIDTH,
            contentPad = CONTENT_PAD,
            masterTextLeft = MASTER_TEXT_LEFT,
        ),
        masterContent = { controller ->
            foldSettingsMaster(this@FoldAdaptiveSettingsDemo, controller)
        },
        primaryContent = { controller ->
            foldSettingsPrimaryPage(this@FoldAdaptiveSettingsDemo, controller)
        },
    )

    private var primaryList: ViewRef<ListView<*, *>>? = null

    fun selectedItem(): FoldSettingItem =
        FOLD_SETTING_ITEMS.getOrNull(selectedSection)
            ?: FOLD_SETTING_ITEMS[FOLD_DEFAULT_SECTION]

    /** 侧栏点分类：只换一级详情，更深页面由组件收掉；开关行只翻开关不换页。 */
    fun selectSection(index: Int) {
        val item = FOLD_SETTING_ITEMS.getOrNull(index) ?: return
        if (item.toggle) {
            setToggle(item.id, !toggleValue(item.id))
            return
        }
        val changed = selectedSection != index
        selectedSection = index
        if (changed) {
            // 换分类等于换一页内容，列表和置顶标题一起回到顶部。
            detailScrollY = 0f
            primaryList?.view?.setContentOffset(0f, 0f, false)
        }
        navigator.selectPrimary(closeDeeperPages = changed)
    }

    internal fun attachPrimaryList(ref: ViewRef<ListView<*, *>>) {
        primaryList = ref
    }

    /**
     * 置顶标题的显现进度：列表停在顶部时是 0，Hero 里的大标题滚到顶栏下沿时到 1。
     */
    fun detailTitleProgress(): Float =
        ((detailScrollY - FOLD_SETTINGS_TITLE_FADE_START) /
            (FOLD_SETTINGS_TITLE_FADE_END - FOLD_SETTINGS_TITLE_FADE_START))
            .coerceIn(0f, 1f)

    fun toggleValue(id: String): Boolean =
        when (id) {
            "airplane" -> airplaneOn
            "vpn" -> vpnOn
            else -> false
        }

    fun setToggle(id: String, on: Boolean) {
        when (id) {
            "airplane" -> airplaneOn = on
            "vpn" -> vpnOn = on
        }
    }

    fun openOption(title: String, optionId: String) {
        navigator.open(
            id = "option-${selectedItem().id}-$optionId",
            title = title,
        ) { controller, entry ->
            foldSettingsOptionPage(this@FoldAdaptiveSettingsDemo, controller, entry.title)
        }
    }

    fun openDeepDetail(title: String) {
        navigator.open(
            id = "detail-${selectedItem().id}-$title",
            title = title,
        ) { controller, entry ->
            foldSettingsDeepDetailPage(this@FoldAdaptiveSettingsDemo, controller, entry.title)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(FOLD_SETTINGS_BG)
            }
            FoldSplitStack(ctx.navigator)
        }
    }

    companion object {
        const val MASTER_WIDTH = 270f
        const val MASTER_MIN_WIDTH = 196f
        const val MASTER_MAX_WIDTH = 340f
        const val MASTER_INSET = 12f
        const val MASTER_RADIUS = 26f
        const val PAGE_RADIUS = 26f
        const val CONTENT_MIN_WIDTH = 430f
        const val CONTENT_PAD = 26f
        const val COMPACT_PAD = 16f

        /** 侧栏浮窗内边距。 */
        const val MASTER_PAD = 12f

        /** 侧栏行左右内边距。 */
        const val MASTER_ROW_PAD = 12f

        /** 分组行的图标尺寸与右边距，侧栏和详情共用一套，行文字才能对齐。 */
        const val ROW_ICON = 29f
        const val ROW_ICON_GAP = 12f

        /** 侧栏标题左边缘：浮层边距 + 侧栏内边距 + 行内边距 + 图标 + 图标右边距。 */
        const val MASTER_TEXT_LEFT =
            MASTER_INSET + MASTER_PAD + MASTER_ROW_PAD + ROW_ICON + ROW_ICON_GAP
    }
}

internal data class FoldSettingItem(
    val id: String,
    val icon: String,
    val title: String,
    val color: Color,
    val summary: String,
    /** 侧栏分组序号：序号相同的行属于同一张卡片，序号之间留系统那样的空白。 */
    val group: Int,
    /** 行尾灰字，如蓝牙的「打开」。 */
    val value: String = "",
    /** 行内副标题，如无线局域网下的网络名。 */
    val subtitle: String = "",
    /** 行尾是开关而不是可进入的分类，如飞行模式、VPN。 */
    val toggle: Boolean = false,
    /** 详情页的行分组；留空时用兜底内容。 */
    val detailGroups: List<List<FoldSettingRow>> = emptyList(),
)

/** 详情页里的一条可进入的行。 */
internal data class FoldSettingRow(
    val title: String,
    val icon: String,
    val color: Color,
    val value: String = "",
)

private val FOLD_SETTING_ITEMS = listOf(
    FoldSettingItem(
        id = "airplane",
        icon = "✈",
        title = "飞行模式",
        color = Color(0xFFFF9500),
        summary = "关闭所有无线连接。",
        group = 0,
        toggle = true,
    ),
    FoldSettingItem(
        id = "wifi",
        icon = "◍",
        title = "无线局域网",
        color = Color(0xFF0A84FF),
        summary = "加入网络、管理已知网络和私人无线局域网地址。",
        group = 0,
        subtitle = "Tencent-GuestWiFi",
        detailGroups = listOf(
            listOf(
                FoldSettingRow("Tencent-GuestWiFi", "◍", Color(0xFF0A84FF), "已连接"),
                FoldSettingRow("其他网络", "◍", Color(0xFF8E8E93)),
            ),
            listOf(
                FoldSettingRow("询问是否加入网络", "?", Color(0xFF0A84FF), "通知"),
                FoldSettingRow("自动加入热点", "◈", Color(0xFF30D158), "询问"),
            ),
        ),
    ),
    FoldSettingItem(
        id = "bluetooth",
        icon = "✷",
        title = "蓝牙",
        color = Color(0xFF0A84FF),
        summary = "连接耳机、键盘和附近的配件。",
        group = 0,
        value = "打开",
        detailGroups = listOf(
            listOf(
                FoldSettingRow("我的设备", "▤", Color(0xFF0A84FF), "3 台"),
                FoldSettingRow("其他设备", "◌", Color(0xFF8E8E93)),
            ),
        ),
    ),
    FoldSettingItem(
        id = "battery",
        icon = "▮",
        title = "电池",
        color = Color(0xFF30D158),
        summary = "查看用电情况、充电优化和电池健康。",
        group = 0,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("电池健康与充电", "♡", Color(0xFF30D158), "92%"),
                FoldSettingRow("用电情况", "▦", Color(0xFF0A84FF)),
            ),
        ),
    ),
    FoldSettingItem(
        id = "vpn",
        icon = "◔",
        title = "VPN",
        color = Color(0xFF0A84FF),
        summary = "连接到公司或自定义的 VPN 配置。",
        group = 0,
        toggle = true,
    ),
    FoldSettingItem(
        id = "general",
        icon = "⚙",
        title = "通用",
        color = Color(0xFF8E8E93),
        summary = "管理设备的整体设置和偏好设置，例如软件更新、设备语言、隔空投送等。",
        group = 1,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("关于本机", "▯", Color(0xFF8E8E93)),
                FoldSettingRow("软件更新", "↧", Color(0xFF8E8E93), "1 项可用"),
                FoldSettingRow("设备储存空间", "▤", Color(0xFF8E8E93), "64 GB 可用"),
            ),
            listOf(
                FoldSettingRow("AppleCare 与保修", "✚", Color(0xFFFF3B30)),
            ),
            listOf(
                FoldSettingRow("隔空投送", "◉", Color(0xFF0A84FF), "所有人"),
                FoldSettingRow("隔空播放与连续互通", "▭", Color(0xFF0A84FF)),
                FoldSettingRow("屏幕捕捉", "▣", Color(0xFF8E8E93)),
            ),
            listOf(
                FoldSettingRow("词典", "文", Color(0xFF0A84FF)),
                FoldSettingRow("键盘", "⌨", Color(0xFF8E8E93)),
                FoldSettingRow("字体", "Aa", Color(0xFF8E8E93)),
            ),
            listOf(
                FoldSettingRow("语言与地区", "语", Color(0xFF0A84FF), "中国大陆"),
                FoldSettingRow("日期与时间", "◷", Color(0xFF0A84FF)),
            ),
        ),
    ),
    FoldSettingItem(
        id = "accessibility",
        icon = "◎",
        title = "辅助功能",
        color = Color(0xFF0A84FF),
        summary = "调整视觉、动作交互、听觉和语音体验。",
        group = 1,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("旁白", "◉", Color(0xFF0A84FF), "关闭"),
                FoldSettingRow("缩放", "⊕", Color(0xFF0A84FF)),
                FoldSettingRow("显示与文字大小", "Aa", Color(0xFF0A84FF)),
            ),
            listOf(
                FoldSettingRow("触控", "✋", Color(0xFF30D158)),
                FoldSettingRow("辅助触控", "◍", Color(0xFF30D158), "关闭"),
            ),
        ),
    ),
    FoldSettingItem(
        id = "multitask",
        icon = "▤",
        title = "多任务与手势",
        color = Color(0xFF0A84FF),
        summary = "调整台前调度、分屏浏览和多任务手势。",
        group = 1,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("分屏浏览", "▥", Color(0xFF0A84FF), "打开"),
                FoldSettingRow("台前调度", "▦", Color(0xFF0A84FF), "关闭"),
            ),
        ),
    ),
    FoldSettingItem(
        id = "control",
        icon = "◫",
        title = "控制中心",
        color = Color(0xFF8E8E93),
        summary = "调整控制中心里包含的控件与排序。",
        group = 1,
    ),
    FoldSettingItem(
        id = "wallpaper",
        icon = "❊",
        title = "墙纸",
        color = Color(0xFF32ADE6),
        summary = "更换锁定屏幕和主屏幕的墙纸。",
        group = 1,
    ),
    FoldSettingItem(
        id = "notifications",
        icon = "▣",
        title = "通知",
        color = Color(0xFFFF3B30),
        summary = "管理横幅、声音、标记和锁定屏幕通知。",
        group = 2,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("显示预览", "▤", Color(0xFFFF3B30), "始终"),
                FoldSettingRow("通知分组", "▥", Color(0xFFFF3B30), "自动"),
            ),
        ),
    ),
    FoldSettingItem(
        id = "sound",
        icon = "♪",
        title = "声音与触感",
        color = Color(0xFFFF2D55),
        summary = "调整铃声、提示音和系统触感反馈。",
        group = 2,
    ),
    FoldSettingItem(
        id = "screentime",
        icon = "◷",
        title = "屏幕使用时间",
        color = Color(0xFF5E5CE6),
        summary = "查看使用报告，设置App 限额与停用时间。",
        group = 2,
    ),
    FoldSettingItem(
        id = "privacy",
        icon = "✋",
        title = "隐私与安全性",
        color = Color(0xFF0A84FF),
        summary = "管理定位、相机、麦克风和照片访问权限。",
        group = 3,
        detailGroups = listOf(
            listOf(
                FoldSettingRow("定位服务", "➤", Color(0xFF0A84FF), "打开"),
                FoldSettingRow("相机", "◙", Color(0xFF8E8E93)),
                FoldSettingRow("麦克风", "♪", Color(0xFFFF9500)),
            ),
        ),
    ),
    FoldSettingItem(
        id = "apps",
        icon = "▦",
        title = "App",
        color = Color(0xFFAF52DE),
        summary = "按 App 单独管理通知、权限和蜂窝数据。",
        group = 3,
    ),
)

/** 默认停在「通用」，和系统首次进入设置的落点一致。 */
private val FOLD_DEFAULT_SECTION =
    FOLD_SETTING_ITEMS.indexOfFirst { it.id == "general" }.coerceAtLeast(0)

/** 侧栏按 [FoldSettingItem.group] 切成若干张卡片，保持系统那样的分组空白。 */
private val FOLD_SETTING_GROUPS: List<List<Int>> =
    FOLD_SETTING_ITEMS.indices
        .groupBy { FOLD_SETTING_ITEMS[it].group }
        .toList()
        .sortedBy { it.first }
        .map { it.second }

// region master 窗口

/**
 * 一份 master。断点差异写在 attr 里：小屏全屏分组列表，大屏玻璃浮窗。
 * List 和分类行不随 isCompact 卸载，所以改宽时滚动位置还在。
 */
private fun ViewContainer<*, *>.foldSettingsMaster(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
) {
    val onGlass = PlatformUtils.isLiquidGlassSupported()
    val radius = FoldAdaptiveSettingsDemo.MASTER_RADIUS
    View {
        attr {
            absolutePositionAllZero()
            val compact = controller.isCompact()
            borderRadius(if (compact) 0f else radius)
            // 这一层只负责投阴影：一旦 overflow(true)，iOS 会把自己的外阴影一起裁掉。
            overflow(false)
            backgroundColor(if (compact) FOLD_SETTINGS_BG else Color.TRANSPARENT)
            boxShadow(
                if (compact) {
                    BoxShadow(0f, 0f, 0f, Color.TRANSPARENT)
                } else {
                    BoxShadow(0f, 6f, 20f, Color(0x24000000))
                },
                useShadowPath = true,
            )
        }
        // 圆角裁切放内层，玻璃和列表都被它裁住，外层才能保住阴影。
        View {
            attr {
                absolutePositionAllZero()
                borderRadius(if (controller.isCompact()) 0f else radius)
                overflow(!controller.isCompact())
                backgroundColor(Color.TRANSPARENT)
            }
            vif({ !controller.isCompact() }) {
                if (onGlass) {
                    LiquidGlass {
                        attr {
                            absolutePositionAllZero()
                            borderRadius(radius)
                            overflow(true)
                            glassEffectTintColor(Color(0x66FFFFFF))
                            glassEffectStyle(GlassEffectStyle.REGULAR)
                            // 纯装饰层，别参与命中，否则它盖住整个浮窗会吃掉分类行的点击。
                            touchEnable(false)
                        }
                    }
                } else {
                    View {
                        attr {
                            absolutePositionAllZero()
                            borderRadius(radius)
                            overflow(true)
                            backgroundColor(Color(0xE6F8F8FA))
                            touchEnable(false)
                        }
                    }
                }
            }
            // 标题和搜索框固定在 List 外面：List 里只放分类，子项宽度由 List 自己撑，
            // 不用再显式写 width，免得内容比浮窗宽、行被挤出可点区域。
            View {
                attr {
                    flex(1f)
                    paddingTop(if (controller.isCompact()) 6f else 14f)
                }
                Text {
                    attr {
                        text("设置")
                        marginLeft(
                            foldSettingsMasterInset(controller) +
                                if (controller.isCompact()) {
                                    6f
                                } else {
                                    FoldAdaptiveSettingsDemo.MASTER_ROW_PAD
                                },
                        )
                        marginBottom(if (controller.isCompact()) 16f else 14f)
                        fontSize(if (controller.isCompact()) 34f else 26f)
                        fontWeightBold()
                        color(FOLD_SETTINGS_TEXT)
                    }
                }
                foldSettingsSearchBar(controller, onGlass)
                foldSettingsAccountRow(controller)
                List {
                    attr {
                        flex(1f)
                        marginTop(if (controller.isCompact()) 8f else 2f)
                        bouncesEnable(false)
                        showScrollerIndicator(false)
                        backgroundColor(Color.TRANSPARENT)
                    }
                    FOLD_SETTING_GROUPS.forEach { indices ->
                        foldSettingsMasterGroup(page, controller, onGlass, indices)
                    }
                    View {
                        attr {
                            height(if (controller.isCompact()) 28f else 18f)
                        }
                    }
                }
            }
        }
    }
}

/** 侧栏内容到浮窗左右边缘的留白。 */
private fun foldSettingsMasterInset(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldAdaptiveSettingsDemo.COMPACT_PAD
    } else {
        FoldAdaptiveSettingsDemo.MASTER_PAD
    }

/** 侧栏一组分类：大屏是一串独立胶囊行，小屏合成一张带分隔线的白卡。 */
private fun ViewContainer<*, *>.foldSettingsMasterGroup(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
    onGlass: Boolean,
    indices: List<Int>,
) {
    View {
        attr {
            val compact = controller.isCompact()
            marginTop(if (compact) 26f else 18f)
            marginLeft(foldSettingsMasterInset(controller))
            marginRight(foldSettingsMasterInset(controller))
            borderRadius(if (compact) 12f else 0f)
            overflow(compact)
            backgroundColor(if (compact) FOLD_SETTINGS_CARD else Color.TRANSPARENT)
        }
        indices.forEachIndexed { position, index ->
            foldSettingsCategoryRow(
                page = page,
                controller = controller,
                index = index,
                item = FOLD_SETTING_ITEMS[index],
                onGlass = onGlass,
                isLast = position == indices.lastIndex,
            )
        }
    }
}

/** 账户行：系统里搜索框下面那条头像行。 */
private fun ViewContainer<*, *>.foldSettingsAccountRow(
    controller: FoldSplitStackController,
) {
    View {
        attr {
            val compact = controller.isCompact()
            height(if (compact) 78f else 66f)
            marginTop(if (compact) 20f else 14f)
            marginLeft(foldSettingsMasterInset(controller))
            marginRight(foldSettingsMasterInset(controller))
            paddingLeft(
                if (compact) FOLD_SETTINGS_ROW_PAD else FoldAdaptiveSettingsDemo.MASTER_ROW_PAD,
            )
            paddingRight(14f)
            borderRadius(if (compact) 12f else 10f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(if (compact) FOLD_SETTINGS_CARD else Color.TRANSPARENT)
        }
        View {
            attr {
                val avatar = if (controller.isCompact()) 52f else 40f
                size(avatar, avatar)
                borderRadius(avatar / 2f)
                allCenter()
                backgroundColor(Color(0xFFB6B6BE))
            }
            Text {
                attr {
                    text("K")
                    fontSize(if (controller.isCompact()) 22f else 18f)
                    fontWeightMedium()
                    color(Color.WHITE)
                }
            }
        }
        View {
            attr {
                flex(1f)
                marginLeft(if (controller.isCompact()) 14f else 13f)
            }
            Text {
                attr {
                    text("Kuikly 用户")
                    fontSize(if (controller.isCompact()) 19f else 17f)
                    fontWeightMedium()
                    color(FOLD_SETTINGS_TEXT)
                }
            }
            Text {
                attr {
                    text("Apple 账户、iCloud 等")
                    marginTop(2f)
                    fontSize(if (controller.isCompact()) 14f else 12.5f)
                    color(FOLD_SETTINGS_SECONDARY)
                }
            }
        }
        Text {
            attr {
                text("›")
                fontSize(22f)
                color(FOLD_SETTINGS_CHEVRON)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsSearchBar(
    controller: FoldSplitStackController,
    onGlass: Boolean,
) {
    View {
        attr {
            val compact = controller.isCompact()
            height(if (compact) 40f else 36f)
            marginLeft(foldSettingsMasterInset(controller))
            marginRight(foldSettingsMasterInset(controller))
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(10f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(
                if (!compact && onGlass) Color(0x28FFFFFF) else Color(0x12000000),
            )
        }
        Text {
            attr {
                text("⌕")
                marginTop(-1f)
                fontSize(if (controller.isCompact()) 18f else 17f)
                color(FOLD_SETTINGS_SECONDARY)
            }
        }
        Text {
            attr {
                text("搜索")
                flex(1f)
                marginLeft(6f)
                fontSize(if (controller.isCompact()) 17f else 16f)
                color(FOLD_SETTINGS_SECONDARY)
            }
        }
        foldSettingsMicGlyph()
    }
}

/** 搜索框右侧的听写话筒：字体里没有合适字形，用两个小色块拼。 */
private fun ViewContainer<*, *>.foldSettingsMicGlyph() {
    View {
        attr {
            width(14f)
            flexDirectionColumn()
            alignItemsCenter()
        }
        View {
            attr {
                size(7f, 11f)
                borderRadius(3.5f)
                backgroundColor(FOLD_SETTINGS_SECONDARY)
            }
        }
        View {
            attr {
                size(9f, 1.5f)
                marginTop(2f)
                borderRadius(0.75f)
                backgroundColor(FOLD_SETTINGS_SECONDARY)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsCategoryRow(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
    index: Int,
    item: FoldSettingItem,
    onGlass: Boolean,
    isLast: Boolean,
) {
    View {
        attr {
            val compact = controller.isCompact()
            // 选中态在 attr 里读：订阅落在这一行上，不用等父分组重建。
            val selected = !item.toggle && page.selectedSection == index
            height(if (compact) 54f else 48f)
            paddingLeft(
                if (compact) FOLD_SETTINGS_ROW_PAD else FoldAdaptiveSettingsDemo.MASTER_ROW_PAD,
            )
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            borderRadius(if (compact) 0f else 10f)
            backgroundColor(
                when {
                    compact -> FOLD_SETTINGS_CARD
                    selected -> if (onGlass) Color(0x1F000000) else Color(0xFFD9D9DF)
                    else -> Color.TRANSPARENT
                },
            )
        }
        if (!item.toggle) {
            // 开关行不接整行点击，交给行尾的 Switch，和系统一致也避免两次翻转。
            event {
                click {
                    page.selectSection(index)
                }
            }
        }
        foldSettingsRowIcon(item.icon, item.color)
        View {
            attr {
                flex(1f)
                marginLeft(FoldAdaptiveSettingsDemo.ROW_ICON_GAP)
            }
            Text {
                attr {
                    text(item.title)
                    fontSize(if (controller.isCompact()) 17f else 16f)
                    val selected = !item.toggle && page.selectedSection == index
                    color(
                        if (selected && !controller.isCompact()) {
                            FOLD_SETTINGS_BLUE
                        } else {
                            FOLD_SETTINGS_TEXT
                        },
                    )
                }
            }
            if (item.subtitle.isNotEmpty()) {
                Text {
                    attr {
                        text(item.subtitle)
                        marginTop(1f)
                        fontSize(if (controller.isCompact()) 13f else 12.5f)
                        color(FOLD_SETTINGS_SECONDARY)
                    }
                }
            }
        }
        if (!item.toggle && item.value.isNotEmpty()) {
            Text {
                attr {
                    text(item.value)
                    fontSize(if (controller.isCompact()) 16f else 15f)
                    color(FOLD_SETTINGS_SECONDARY)
                }
            }
        }
        if (item.toggle) {
            Switch {
                attr {
                    size(51f, 31f)
                    isOn(page.toggleValue(item.id))
                    onColor(FOLD_SETTINGS_GREEN)
                    unOnColor(Color(0xFFE9E9EB))
                    thumbColor(Color.WHITE)
                    enableGlassEffect(true)
                }
                event {
                    switchOnChanged { on ->
                        if (on != page.toggleValue(item.id)) {
                            page.setToggle(item.id, on)
                        }
                    }
                }
            }
        } else {
            vif({ controller.isCompact() }) {
                Text {
                    attr {
                        text("›")
                        marginLeft(7f)
                        fontSize(22f)
                        color(FOLD_SETTINGS_CHEVRON)
                    }
                }
            }
        }
        vif({ controller.isCompact() && !isLast }) {
            foldSettingsSeparator(
                FOLD_SETTINGS_ROW_PAD + FoldAdaptiveSettingsDemo.ROW_ICON +
                    FoldAdaptiveSettingsDemo.ROW_ICON_GAP,
            )
        }
    }
}

// endregion

// region 详情页

/**
 * 一级详情。
 *
 * 大屏时被组件钉在 PageList 底下，[FoldSplitStackController.coverShadow] 只在被覆盖的
 * 滑动过程里大于 0，所以蒙版两端都是全透明。
 */
private fun ViewContainer<*, *>.foldSettingsPrimaryPage(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(FOLD_SETTINGS_BG)
        }
        List {
            ref {
                page.attachPrimaryList(it)
            }
            attr {
                flex(1f)
                bouncesEnable(true)
                showScrollerIndicator(false)
                backgroundColor(FOLD_SETTINGS_BG)
            }
            event {
                scroll { params ->
                    page.detailScrollY = params.offsetY
                }
            }
            View {
                attr {
                    // 子项必须写成整屏宽，List 默认按内容收缩，大屏会只占右侧一截。
                    width(controller.pageWidth())
                    paddingLeft(foldSettingsContentLeft(controller))
                    paddingRight(foldSettingsContentRight(controller))
                    // 顶栏是浮在列表上的，内容自己让开这一条，滚动时从它下面穿过。
                    paddingTop(
                        FOLD_SETTINGS_NAV_HEIGHT + if (controller.isCompact()) 6f else 10f,
                    )
                    paddingBottom(48f)
                }
                // 内容按 selectedSection 分支挂载：写在函数体里会冻成首帧的「通用」。
                FOLD_SETTING_ITEMS.forEachIndexed { index, item ->
                    vif({ page.selectedSection == index && !item.toggle }) {
                        foldSettingsHero(item)
                        foldSettingsDetailGroups(item).forEach { rows ->
                            foldSettingsGroup {
                                rows.forEachIndexed { position, row ->
                                    foldSettingsNavigationRow(
                                        row.title,
                                        row.value,
                                        row.icon,
                                        row.color,
                                        showSeparator = position != rows.lastIndex,
                                    ) {
                                        page.openOption(row.title, row.title)
                                    }
                                }
                            }
                        }
                        foldSettingsGroup {
                            foldSettingsSwitchRow("启用${item.title}", page.primaryOption) {
                                page.primaryOption = it
                            }
                            foldSettingsSwitchRow(
                                "自动管理",
                                page.automaticOption,
                                showSeparator = false,
                            ) {
                                page.automaticOption = it
                            }
                        }
                    }
                }
            }
        }
        // 顶栏声明在列表之后，才能浮在滚动内容之上。
        foldSettingsPrimaryNavBar(page, controller)
        vif({ !controller.isCompact() }) {
            View {
                attr {
                    absolutePositionAllZero()
                    backgroundColor(Color(0x000000, controller.coverShadow * 0.10f))
                    touchEnable(false)
                }
            }
        }
    }
}

/**
 * 一级详情顶栏。
 *
 * 停在列表顶部时整条是透明的，Hero 的大标题往上滚才淡入标题、底色和分隔线，
 * 对齐系统的 large title 收起过程。小屏的返回按钮不参与淡入，始终可见可点。
 */
private fun ViewContainer<*, *>.foldSettingsPrimaryNavBar(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            positionAbsolute()
            left(0f)
            right(0f)
            top(0f)
            height(FOLD_SETTINGS_NAV_HEIGHT)
            // 顶栏铺满整屏，靠左内边距把标题挤到详情窗口的居中位置。
            paddingLeft(controller.contentInsetLeft())
            flexDirectionRow()
            alignItemsCenter()
            justifyContentCenter()
            // 进度在各自的 attr 里读：写在外面会让整个详情子树跟着滚动重建。
            backgroundColor(Color(0xF2F2F7, page.detailTitleProgress() * 0.94f))
            // 大屏顶栏没有可点元素，别拦住列表滚动；小屏要留给返回按钮。
            touchEnable(controller.isCompact())
        }
        Text {
            attr {
                text(page.selectedItem().title)
                fontSize(17f)
                fontWeightSemiBold()
                color(Color(0x1C1C1E, page.detailTitleProgress()))
            }
        }
        View {
            attr {
                positionAbsolute()
                left(controller.contentInsetLeft())
                right(0f)
                bottom(0f)
                height(0.33f)
                backgroundColor(Color(0xC7C7CC, page.detailTitleProgress()))
            }
        }
        vif({ controller.isCompact() }) {
            View {
                attr {
                    positionAbsolute()
                    left(FoldAdaptiveSettingsDemo.COMPACT_PAD)
                    top(0f)
                    bottom(0f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                event {
                    click {
                        controller.backToMaster()
                    }
                }
                Text {
                    attr {
                        text("‹")
                        marginTop(-2f)
                        marginRight(3f)
                        fontSize(26f)
                        color(FOLD_SETTINGS_BLUE)
                    }
                }
                Text {
                    attr {
                        text("设置")
                        fontSize(17f)
                        color(FOLD_SETTINGS_BLUE)
                    }
                }
            }
        }
    }
}

/** 没写分组数据的分类用一套兜底行，保证每个分类都能往下钻。 */
private fun foldSettingsDetailGroups(item: FoldSettingItem): List<List<FoldSettingRow>> =
    if (item.detailGroups.isNotEmpty()) {
        item.detailGroups
    } else {
        listOf(
            listOf(
                FoldSettingRow("${item.title}概览", item.icon, item.color, "已启用"),
                FoldSettingRow("通知", "▣", Color(0xFFFF3B30), "允许"),
                FoldSettingRow("高级选项", "⚙", Color(0xFF8E8E93)),
            ),
        )
    }

private fun ViewContainer<*, *>.foldSettingsOptionPage(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
    title: String,
) {
    foldSettingsPushedPage(controller, title) {
        foldSettingsSectionTitle("选项")
        foldSettingsGroup(topGap = 0f) {
            foldSettingsSwitchRow("启用此项", page.primaryOption) {
                page.primaryOption = it
            }
            foldSettingsSwitchRow("自动应用", page.automaticOption) {
                page.automaticOption = it
            }
            foldSettingsSwitchRow("允许后台活动", page.secondaryOption, showSeparator = false) {
                page.secondaryOption = it
            }
        }
        foldSettingsHint("这一级由 FoldSplitStackController.open() 追加，返回落稳后才从栈尾卸载。")
        foldSettingsSectionTitle("更多")
        foldSettingsGroup(topGap = 0f) {
            foldSettingsNavigationRow(
                "详细信息",
                "进入下一层",
                "ⓘ",
                Color(0xFF0A84FF),
                showSeparator = false,
            ) {
                page.openDeepDetail("$title · 详细信息")
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsDeepDetailPage(
    page: FoldAdaptiveSettingsDemo,
    controller: FoldSplitStackController,
    title: String,
) {
    foldSettingsPushedPage(controller, title) {
        Text {
            attr {
                text("第 ${controller.depth + 1} 级页面")
                fontSize(28f)
                fontWeightBold()
                color(FOLD_SETTINGS_TEXT)
            }
        }
        Text {
            attr {
                text("当前路径：${page.selectedItem().title} / ${controller.pathTitles()}")
                marginTop(8f)
                marginBottom(20f)
                fontSize(14f)
                lineHeight(20f)
                color(FOLD_SETTINGS_SECONDARY)
            }
        }
        foldSettingsGroup(topGap = 0f) {
            foldSettingsNavigationRow("再进一层", "", "＋", Color(0xFF30D158)) {
                page.openDeepDetail("$title · 更深一层")
            }
            foldSettingsNavigationRow("返回上一页", "", "‹", Color(0xFF0A84FF)) {
                controller.close()
            }
            foldSettingsNavigationRow(
                "回到一级详情",
                "",
                "↺",
                Color(0xFFFF3B30),
                showSeparator = false,
            ) {
                controller.closeToPrimary()
            }
        }
    }
}

/**
 * 二级及以上页面统一的外壳。
 *
 * 页面自身铺满整屏，靠 [foldSettingsContentLeft] 让开主窗，所以左右滑的位移是完整一屏。
 */
private fun ViewContainer<*, *>.foldSettingsPushedPage(
    controller: FoldSplitStackController,
    title: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flex(1f)
            backgroundColor(FOLD_SETTINGS_BG)
            borderRadius(
                if (controller.isCompact()) 0f else FoldAdaptiveSettingsDemo.PAGE_RADIUS,
            )
            overflow(true)
        }
        foldSettingsNavigationBar(controller, title)
        List {
            attr {
                flex(1f)
                bouncesEnable(false)
                showScrollerIndicator(false)
                // Android 上 List 默认透明；嵌在 PageList 里会透出底页，必须自己铺底色。
                backgroundColor(FOLD_SETTINGS_BG)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(foldSettingsContentLeft(controller))
                    paddingRight(foldSettingsContentRight(controller))
                    paddingTop(4f)
                    paddingBottom(40f)
                }
                content()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsNavigationBar(
    controller: FoldSplitStackController,
    title: String,
) {
    View {
        attr {
            height(52f)
            // 顶栏也铺满，标题和返回按钮落在主窗右侧的可读区。
            paddingLeft(controller.contentInsetLeft())
            flexDirectionRow()
            alignItemsCenter()
            justifyContentCenter()
            backgroundColor(FOLD_SETTINGS_BG)
        }
        Text {
            attr {
                text(title)
                fontSize(17f)
                fontWeightSemiBold()
                color(FOLD_SETTINGS_TEXT)
            }
        }
        View {
            attr {
                positionAbsolute()
                left(controller.contentInsetLeft() + 14f)
                top(9f)
                height(34f)
                paddingLeft(8f)
                paddingRight(11f)
                borderRadius(17f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(FOLD_SETTINGS_CARD)
                boxShadow(BoxShadow(0f, 1f, 4f, Color(0x14000000)))
            }
            event {
                click {
                    controller.close()
                }
            }
            Text {
                attr {
                    text("‹")
                    marginTop(-2f)
                    fontSize(26f)
                    color(FOLD_SETTINGS_BLUE)
                }
            }
            Text {
                attr {
                    text("返回")
                    fontSize(14f)
                    color(FOLD_SETTINGS_BLUE)
                }
            }
        }
    }
}

private fun foldSettingsContentLeft(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldAdaptiveSettingsDemo.COMPACT_PAD
    } else {
        controller.contentInsetLeft() + FoldAdaptiveSettingsDemo.CONTENT_PAD
    }

private fun foldSettingsContentRight(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldAdaptiveSettingsDemo.COMPACT_PAD
    } else {
        FoldAdaptiveSettingsDemo.CONTENT_PAD
    }

// endregion

// region 通用行

/** 详情页头卡：系统是图标、标题、说明自上而下排一列。 */
private fun ViewContainer<*, *>.foldSettingsHero(item: FoldSettingItem) {
    View {
        attr {
            padding(22f)
            borderRadius(20f)
            backgroundColor(FOLD_SETTINGS_CARD)
        }
        View {
            attr {
                size(56f, 56f)
                borderRadius(14f)
                allCenter()
                backgroundColor(item.color)
            }
            Text {
                attr {
                    text(item.icon)
                    fontSize(28f)
                    color(Color.WHITE)
                }
            }
        }
        Text {
            attr {
                text(item.title)
                marginTop(18f)
                fontSize(22f)
                fontWeightBold()
                color(FOLD_SETTINGS_TEXT)
            }
        }
        Text {
            attr {
                text(item.summary)
                marginTop(6f)
                fontSize(15f)
                lineHeight(21f)
                color(FOLD_SETTINGS_SECONDARY)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsSectionTitle(title: String) {
    Text {
        attr {
            text(title)
            margin(left = 16f, top = 24f, bottom = 8f)
            fontSize(13f)
            fontWeightMedium()
            color(FOLD_SETTINGS_SECONDARY)
        }
    }
}

/** 分组卡片。[topGap] 是与上一块的间距，系统靠这段空白代替分组标题。 */
private fun ViewContainer<*, *>.foldSettingsGroup(
    topGap: Float = 22f,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            marginTop(topGap)
            borderRadius(16f)
            overflow(true)
            backgroundColor(FOLD_SETTINGS_CARD)
        }
        content()
    }
}

/** 行首的圆角图标块，侧栏和详情共用，保证两列文字左边缘一致。 */
private fun ViewContainer<*, *>.foldSettingsRowIcon(icon: String, iconColor: Color) {
    View {
        attr {
            val side = FoldAdaptiveSettingsDemo.ROW_ICON
            size(side, side)
            borderRadius(7f)
            allCenter()
            backgroundColor(iconColor)
        }
        Text {
            attr {
                text(icon)
                fontSize(14f)
                color(Color.WHITE)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsNavigationRow(
    title: String,
    value: String,
    icon: String,
    iconColor: Color,
    showSeparator: Boolean = true,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(50f)
            paddingLeft(FOLD_SETTINGS_ROW_PAD)
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_SETTINGS_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        foldSettingsRowIcon(icon, iconColor)
        Text {
            attr {
                text(title)
                flex(1f)
                marginLeft(FoldAdaptiveSettingsDemo.ROW_ICON_GAP)
                fontSize(16f)
                color(FOLD_SETTINGS_TEXT)
            }
        }
        if (value.isNotEmpty()) {
            Text {
                attr {
                    text(value)
                    fontSize(15f)
                    color(FOLD_SETTINGS_SECONDARY)
                }
            }
        }
        Text {
            attr {
                text("›")
                marginLeft(7f)
                fontSize(22f)
                color(FOLD_SETTINGS_CHEVRON)
            }
        }
        if (showSeparator) {
            foldSettingsSeparator(
                FOLD_SETTINGS_ROW_PAD + FoldAdaptiveSettingsDemo.ROW_ICON +
                    FoldAdaptiveSettingsDemo.ROW_ICON_GAP,
            )
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsSwitchRow(
    title: String,
    enabled: Boolean,
    showSeparator: Boolean = true,
    onChanged: (Boolean) -> Unit,
) {
    View {
        attr {
            height(50f)
            paddingLeft(FOLD_SETTINGS_ROW_PAD)
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_SETTINGS_CARD)
        }
        Text {
            attr {
                text(title)
                flex(1f)
                fontSize(16f)
                color(FOLD_SETTINGS_TEXT)
            }
        }
        Switch {
            attr {
                size(51f, 31f)
                isOn(enabled)
                onColor(FOLD_SETTINGS_GREEN)
                unOnColor(Color(0xFFE9E9EB))
                thumbColor(Color.WHITE)
                enableGlassEffect(true)
            }
            event {
                switchOnChanged { on ->
                    if (on != enabled) {
                        onChanged(on)
                    }
                }
            }
        }
        if (showSeparator) {
            foldSettingsSeparator(FOLD_SETTINGS_ROW_PAD)
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsSeparator(left: Float) {
    View {
        attr {
            positionAbsolute()
            this.left(left)
            right(0f)
            bottom(0f)
            height(0.33f)
            backgroundColor(FOLD_SETTINGS_SEPARATOR)
        }
    }
}

private fun ViewContainer<*, *>.foldSettingsHint(text: String) {
    Text {
        attr {
            text(text)
            margin(left = 16f, top = 8f, right = 16f)
            fontSize(13f)
            lineHeight(18f)
            color(FOLD_SETTINGS_SECONDARY)
        }
    }
}

// endregion

/** 行内左内边距，同时决定分隔线的左缩进；对齐 iOS 分组列表的 16。 */
private const val FOLD_SETTINGS_ROW_PAD = 16f

/** 详情页置顶栏高度，同时是内容顶部要让开的距离。 */
private const val FOLD_SETTINGS_NAV_HEIGHT = 44f

/** 置顶标题开始淡入、完全显现的滚动距离。 */
private const val FOLD_SETTINGS_TITLE_FADE_START = 40f
private const val FOLD_SETTINGS_TITLE_FADE_END = 100f

private val FOLD_SETTINGS_BG = Color(0xFFF2F2F7)
private val FOLD_SETTINGS_CARD = Color.WHITE
private val FOLD_SETTINGS_TEXT = Color(0xFF1C1C1E)
private val FOLD_SETTINGS_SECONDARY = Color(0xFF8A8A8E)
private val FOLD_SETTINGS_CHEVRON = Color(0xFFC7C7CC)
private val FOLD_SETTINGS_SEPARATOR = Color(0xFFE5E5EA)
private val FOLD_SETTINGS_BLUE = Color(0xFF0A84FF)
private val FOLD_SETTINGS_GREEN = Color(0xFF30D158)
