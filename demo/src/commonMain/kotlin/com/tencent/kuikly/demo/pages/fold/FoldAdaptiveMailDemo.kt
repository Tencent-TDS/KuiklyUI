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
import com.tencent.kuikly.core.base.EdgeInsets
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import kotlin.math.max

/**
 * 参考 [FoldAdaptiveSettingsDemo] 的分栏栈 + 系统导航/底栏，做成邮件阅读形态。
 * 小屏：收件箱 → 详情；大屏：左侧玻璃收件箱 + 右侧钉住的邮件正文。
 * 底栏仅三键：删除、归档、撰写（对齐邮件参考稿）。
 */
@Page("FoldAdaptiveMailDemo")
internal class FoldAdaptiveMailDemo : BasePager() {

    var selectedMailId by observable(MAIL_MESSAGES.first().id)
    var starredIds: Set<Int> by observable(setOf(1))

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
        chrome = FoldSplitSystemChrome(
            tintColor = Color.BLACK,
            backSymbol = "chevron.backward",
            items = listOf(
                FoldSplitSystemNavItem(
                    placement = "trailing",
                    symbol = "square.and.arrow.up",
                    title = "分享",
                    index = 0,
                    itemGroup = 1,
                ),
                FoldSplitSystemNavItem(
                    placement = "trailing",
                    symbol = "ellipsis.circle",
                    title = "更多",
                    index = 0,
                    itemGroup = 2,
                ),
            ),
            toolbar = FoldSplitSystemToolbar(
                leadingInset = { it.contentInsetLeft() },
                trailingInset = { if (it.showsMaster()) it.trailingSafeInset() else 0f },
                items = listOf(
                    FoldSplitSystemToolbarItem("trash", "删除", index = 0),
                    FoldSplitSystemToolbarItem("folder", "归档", index = 1),
                    FoldSplitSystemToolbarItem("square.and.pencil", "撰写", index = 2),
                ),
            ),
        ),
        masterContent = { controller ->
            foldAdaptiveMailMaster(this@FoldAdaptiveMailDemo, controller)
        },
        primaryContent = { controller ->
            foldAdaptiveMailDetail(this@FoldAdaptiveMailDemo, controller)
        },
    )

    fun selectedMail(): MailMessage =
        MAIL_MESSAGES.firstOrNull { it.id == selectedMailId } ?: MAIL_MESSAGES.first()

    fun selectMail(id: Int) {
        val changed = selectedMailId != id
        selectedMailId = id
        navigator.selectPrimary(closeDeeperPages = changed)
    }

    fun toggleStar(id: Int) {
        starredIds = if (starredIds.contains(id)) starredIds - id else starredIds + id
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(MAIL_STAGE)
            }
            FoldSplitStack(ctx.navigator)
        }
    }

    companion object {
        const val MASTER_WIDTH = 280f
        const val MASTER_MIN_WIDTH = 220f
        const val MASTER_MAX_WIDTH = 360f
        const val MASTER_INSET = 12f
        const val MASTER_RADIUS = 26f
        const val CONTENT_MIN_WIDTH = 400f
        const val CONTENT_PAD = 24f
        const val COMPACT_PAD = 16f
        /** 竖屏双栏时系统 trailing / 状态列压在右上角，正文需下让一行。 */
        const val DETAIL_CHROME_TOP = 52f
        const val MASTER_PAD = 12f
        const val MASTER_ROW_PAD = 12f
        const val MASTER_TEXT_LEFT =
            MASTER_INSET + MASTER_PAD + MASTER_ROW_PAD + 36f
    }
}

internal data class MailMessage(
    val id: Int,
    val sender: String,
    val subject: String,
    val preview: String,
    val time: String,
    val body: String,
    val accent: Color,
)

private val MAIL_MESSAGES = listOf(
    MailMessage(
        id = 1,
        sender = "Kuikly 团队",
        subject = "Kuikly 折叠屏适配指南",
        preview = "展开、横竖屏与分栏阅读，一份给设计同学的速览…",
        time = "14:04",
        body = "你好，\n\n" +
            "我们整理了一份《Kuikly 折叠屏适配指南》，方便你在真机上对照体验：外屏单栏怎么进详情、" +
            "展开后左右怎么同时看列表和正文、横屏时怎样既不挡内容又顺手点到操作按钮。\n\n" +
            "文档里配有示意图和检查清单，评审前可以先过一遍「字号 / 间距 / 可点区域」三项。\n\n" +
            "链接在附件，有问题直接在群里 @我。",
        accent = Color(0xFF1768E5),
    ),
    MailMessage(
        id = 2,
        sender = "产品设计组",
        subject = "邮件阅读稿 · 请帮忙看一眼",
        preview = "想确认展开态下底栏和返回的位置是否顺手…",
        time = "昨天",
        body = "Hi，\n\n" +
            "这版是折叠屏上的邮件阅读示意：小屏还是熟悉的「列表 → 点进详情」；" +
            "展开后左边收件箱、右边正文，底栏保留删除、归档、写邮件三个常用动作。\n\n" +
            "我主要想确认：横屏拿在手里时，返回和分享会不会太远；" +
            "竖屏分栏时标题区会不会显得空。你方便的话今天下班前给两句感受就行，不用长篇。\n\n" +
            "谢谢！",
        accent = Color(0xFF5E5CE6),
    ),
    MailMessage(
        id = 3,
        sender = "行政助手",
        subject = "下周折叠屏机型联调安排",
        preview = "周三下午 3 点，会议室 B + 两台样机…",
        time = "周二",
        body = "各位好，\n\n" +
            "下周三 15:00–16:30 做折叠屏真机走查，已预约会议室 B，并申请两台工程样机。\n" +
            "请自带要演示的页面；走查重点：铰链半开、横竖屏切换、外屏返回动线。\n\n" +
            "无法到场可提前把录屏发到邮件组。",
        accent = Color(0xFF30B0C7),
    ),
)

private fun ViewContainer<*, *>.foldAdaptiveMailMaster(
    page: FoldAdaptiveMailDemo,
    controller: FoldSplitStackController,
) {
    val onGlass = PlatformUtils.isLiquidGlassSupported()
    val radius = FoldAdaptiveMailDemo.MASTER_RADIUS
    View {
        attr {
            absolutePositionAllZero()
            val compact = controller.isCompact()
            borderRadius(if (compact) 0f else radius)
            overflow(false)
            backgroundColor(if (compact) MAIL_STAGE else Color.TRANSPARENT)
            boxShadow(
                if (compact) {
                    BoxShadow(0f, 0f, 0f, Color.TRANSPARENT)
                } else {
                    BoxShadow(0f, 6f, 20f, Color(0x24000000))
                },
                useShadowPath = true,
            )
        }
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
                            touchEnable(false)
                        }
                    }
                } else {
                    View {
                        attr {
                            absolutePositionAllZero()
                            borderRadius(radius)
                            backgroundColor(Color(0xE6F4F7FB))
                            touchEnable(false)
                        }
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    paddingTop(mailMasterPaddingTop(controller))
                }
                Text {
                    attr {
                        text("收件箱")
                        marginLeft(mailMasterPadLeft(controller))
                        marginRight(mailMasterPadRight(controller))
                        marginBottom(12f)
                        fontSize(if (controller.isCompact()) 34f else 26f)
                        fontWeightBold()
                        color(MAIL_TEXT)
                    }
                }
                View {
                    attr {
                        height(36f)
                        marginLeft(mailMasterPadLeft(controller))
                        marginRight(mailMasterPadRight(controller))
                        marginBottom(10f)
                        paddingLeft(12f)
                        borderRadius(10f)
                        justifyContentCenter()
                        backgroundColor(Color(0x18000000))
                    }
                    Text {
                        attr {
                            text("搜索邮件")
                            fontSize(15f)
                            color(MAIL_MUTED)
                        }
                    }
                }
                List {
                    attr {
                        flex(1f)
                        showScrollerIndicator(false)
                        backgroundColor(Color.TRANSPARENT)
                    }
                    MAIL_MESSAGES.forEach { mail ->
                        foldAdaptiveMailRow(page, controller, mail)
                    }
                    View {
                        attr {
                            height(mailMasterListBottomSpacer(controller, page))
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldAdaptiveMailRow(
    page: FoldAdaptiveMailDemo,
    controller: FoldSplitStackController,
    mail: MailMessage,
) {
    View {
        attr {
            marginLeft(mailMasterPadLeft(controller))
            marginRight(mailMasterPadRight(controller))
            marginBottom(6f)
            paddingLeft(FoldAdaptiveMailDemo.MASTER_ROW_PAD)
            paddingRight(12f)
            paddingTop(10f)
            paddingBottom(10f)
            borderRadius(12f)
            flexDirectionRow()
            alignItemsCenter()
            val wideSelected =
                page.selectedMailId == mail.id && !controller.isCompact()
            backgroundColor(
                when {
                    wideSelected -> Color(0x22000000)
                    controller.isCompact() -> Color.WHITE
                    else -> Color.TRANSPARENT
                },
            )
        }
        event {
            click {
                page.selectMail(mail.id)
            }
        }
        View {
            attr {
                size(40f, 40f)
                borderRadius(20f)
                allCenter()
                backgroundColor(mail.accent)
            }
            Text {
                attr {
                    text(mail.sender.take(1))
                    color(Color.WHITE)
                    fontSize(17f)
                    fontWeightBold()
                }
            }
        }
        View {
            attr {
                flex(1f)
                marginLeft(10f)
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text(mail.sender)
                        flex(1f)
                        fontSize(16f)
                        fontWeightMedium()
                        color(MAIL_TEXT)
                        lines(1)
                    }
                }
                Text {
                    attr {
                        text(mail.time)
                        fontSize(13f)
                        color(MAIL_MUTED)
                    }
                }
            }
            Text {
                attr {
                    text(mail.subject)
                    marginTop(2f)
                    fontSize(14f)
                    fontWeightMedium()
                    color(MAIL_TEXT)
                    lines(1)
                }
            }
            Text {
                attr {
                    text(mail.preview)
                    marginTop(2f)
                    fontSize(13f)
                    color(MAIL_MUTED)
                    lines(2)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldAdaptiveMailDetail(
    page: FoldAdaptiveMailDemo,
    controller: FoldSplitStackController,
) {
    List {
        attr {
            flex(1f)
            showScrollerIndicator(false)
            backgroundColor(MAIL_STAGE)
        }
        View {
            attr {
                // 与设置 Demo 一致：子项按整屏宽布局，靠左右 padding 让开主窗与右侧竖轨。
                width(controller.pageWidth())
                paddingLeft(mailContentLeft(controller, page.pageData.safeAreaInsets))
                paddingRight(mailContentRight(controller, page.pageData.safeAreaInsets))
                paddingTop(mailDetailContentTop(controller, page.pageData.safeAreaInsets))
                paddingBottom(mailBottomPad(page, controller))
            }
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    marginBottom(16f)
                }
                View {
                    attr {
                        val mail = page.selectedMail()
                        size(48f, 48f)
                        borderRadius(24f)
                        allCenter()
                        backgroundColor(mail.accent)
                    }
                    Text {
                        attr {
                            text(page.selectedMail().sender.take(1))
                            color(Color.WHITE)
                            fontSize(20f)
                            fontWeightBold()
                        }
                    }
                }
                View {
                    attr {
                        flex(1f)
                        marginLeft(12f)
                    }
                    Text {
                        attr {
                            text(page.selectedMail().sender)
                            fontSize(17f)
                            fontWeightMedium()
                            color(MAIL_TEXT)
                        }
                    }
                    Text {
                        attr {
                            text("收件箱")
                            marginTop(2f)
                            fontSize(14f)
                            color(MAIL_MUTED)
                        }
                    }
                }
            }
            Text {
                attr {
                    text(page.selectedMail().subject)
                    fontSize(28f)
                    fontWeightBold()
                    color(MAIL_TEXT)
                    marginBottom(14f)
                }
            }
            View {
                attr {
                    height(180f)
                    borderRadius(16f)
                    marginBottom(18f)
                    backgroundColor(Color(0xFF2D4A3E))
                    allCenter()
                }
                Text {
                    attr {
                        text("📱")
                        fontSize(48f)
                    }
                }
            }
            Text {
                attr {
                    text(page.selectedMail().body)
                    fontSize(17f)
                    lineHeight(26f)
                    color(MAIL_BODY)
                }
            }
        }
    }
}

/** 竖屏双栏时浮窗已有 [FoldSplitStackController.masterChromeInset]，内边距与设置小屏一致即可。 */
private fun mailMasterPaddingTop(controller: FoldSplitStackController): Float {
    if (controller.isCompact()) {
        return 6f
    }
    val width = controller.pageWidth()
    val height = controller.pageHeight()
    return if (height > width) {
        6f
    } else {
        14f
    }
}

private fun mailDetailContentTop(
    controller: FoldSplitStackController,
    insets: EdgeInsets,
): Float {
    if (controller.isCompact()) {
        return 6f
    }
    if (!controller.usesSystemChrome()) {
        return 0f
    }
    val width = controller.pageWidth()
    val height = controller.pageHeight()
    // 横屏竖轨：按钮在侧边，正文顶对齐主栏即可。
    if (width > height) {
        return 10f
    }
    // 竖屏双栏：时间与分享/更多压在右上，避免与发件人行重叠。
    return max(insets.top, FoldAdaptiveMailDemo.DETAIL_CHROME_TOP)
}

private fun mailMasterInset(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldAdaptiveMailDemo.COMPACT_PAD
    } else {
        FoldAdaptiveMailDemo.MASTER_PAD
    }

private fun mailMasterPadLeft(controller: FoldSplitStackController): Float {
    val pad = mailMasterInset(controller)
    if (!controller.isCompact()) {
        return pad
    }
    return max(pad, controller.leadingSafeInset())
}

private fun mailMasterPadRight(controller: FoldSplitStackController): Float {
    val pad = mailMasterInset(controller)
    if (!controller.isCompact()) {
        return pad
    }
    return max(pad, controller.trailingSafeInset())
}

private fun mailContentLeft(
    controller: FoldSplitStackController,
    insets: EdgeInsets,
): Float =
    if (controller.isCompact()) {
        max(FoldAdaptiveMailDemo.COMPACT_PAD, insets.left)
    } else {
        controller.contentInsetLeft() + FoldAdaptiveMailDemo.CONTENT_PAD
    }

private fun mailContentRight(
    controller: FoldSplitStackController,
    insets: EdgeInsets,
): Float {
    val pad = if (controller.isCompact()) {
        FoldAdaptiveMailDemo.COMPACT_PAD
    } else {
        FoldAdaptiveMailDemo.CONTENT_PAD
    }
    var trail = max(controller.trailingSafeInset(), controller.trailingOcclusionInset())
    if (!controller.isCompact() && controller.usesSystemChrome()) {
        val width = controller.pageWidth()
        val height = controller.pageHeight()
        // 与 Native `p_titleTrailingLimit` 一致：竖屏右侧状态/导航列约 64–88pt。
        if (height > width && trail < 64f) {
            trail = 64f
        }
    }
    return max(max(pad, insets.right), trail)
}

private fun mailMasterListBottomSpacer(
    controller: FoldSplitStackController,
    page: FoldAdaptiveMailDemo,
): Float {
    val chromePad = controller.systemToolbarContentInsetBottom()
    if (chromePad > 0f) {
        return chromePad
    }
    return if (controller.isCompact()) {
        mailBottomPad(page, controller)
    } else {
        18f
    }
}

private fun mailBottomPad(
    page: FoldAdaptiveMailDemo,
    controller: FoldSplitStackController,
): Float {
    val chromePad = controller.systemToolbarContentInsetBottom()
    if (chromePad > 0f) {
        return chromePad
    }
    return max(page.pageData.safeAreaInsets.bottom + 12f, 56f)
}

private val MAIL_STAGE = Color(0xFFF2F5FA)
private val MAIL_TEXT = Color(0xFF172033)
private val MAIL_BODY = Color(0xFF3A4558)
private val MAIL_MUTED = Color(0xFF68758A)
