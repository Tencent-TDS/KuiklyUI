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
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 邮件视觉的 [FoldSplitStackController] 完整示例。
 *
 * 小屏从「文件夹与邮件列表」进入摘要；大屏列表成为玻璃浮窗，摘要钉在舞台上。
 * 摘要、正文和附件预览继续使用同一个栈，构成 master → 摘要 → 正文 → 附件四级导航。
 */
@Page("FoldSplitMailDemo")
internal class FoldSplitMailDemo : BasePager() {

    var selectedFolder by observable(0)
    var selectedMailId by observable(1)
    var readMailIds: Set<Int> by observable(setOf(3))
    var starredMailIds: Set<Int> by observable(setOf(1, 4))
    var replyDraft by observable("")
    var sentReplyCount by observable(0)

    private val navigator = FoldSplitStackController(
        pager = this,
        layout = FoldSplitStackLayout(
            masterWidth = 260f,
            masterMinWidth = 220f,
            masterMaxWidth = 360f,
            masterInset = 12f,
            detailMinWidth = 400f,
            contentPad = 24f,
            masterTextLeft = 74f,
        ),
        masterContent = { controller ->
            vif({ controller.isCompact() }) {
                foldMailCompactMaster(this@FoldSplitMailDemo, controller)
            }
            vif({ !controller.isCompact() }) {
                foldMailWideMaster(this@FoldSplitMailDemo, controller)
            }

        },
        primaryContent = { controller ->
            foldMailSummaryPage(this@FoldSplitMailDemo, controller)
        },
    )

    fun selectedMail(): FoldMailMessage =
        FOLD_MAILS.firstOrNull { it.id == selectedMailId } ?: FOLD_MAILS.first()

    fun selectMail(id: Int) {
        selectedMailId = id
        readMailIds = readMailIds + id
        navigator.selectPrimary()
    }

    fun toggleStar(id: Int) {
        starredMailIds = if (starredMailIds.contains(id)) {
            starredMailIds - id
        } else {
            starredMailIds + id
        }
    }

    fun isUnread(id: Int): Boolean = !readMailIds.contains(id)

    fun openBody() {
        val mailId = selectedMailId
        navigator.open(
            id = "mail-body-$mailId",
            title = "邮件正文",
            data = mailId,
        ) { controller, page ->
            foldMailBodyPage(
                this@FoldSplitMailDemo,
                controller,
                page.data as? Int ?: mailId,
            )
        }
    }

    fun openAttachment(mailId: Int, attachmentIndex: Int) {
        val attachment = FoldMailAttachmentPageData(mailId, attachmentIndex)
        navigator.open(
            id = "attachment-$mailId-$attachmentIndex",
            title = "附件预览",
            data = attachment,
        ) { controller, page ->
            foldMailAttachmentPage(
                this@FoldSplitMailDemo,
                controller,
                page.data as? FoldMailAttachmentPageData ?: attachment,
            )
        }
    }

    fun sendReply() {
        if (replyDraft.isBlank()) {
            return
        }
        sentReplyCount++
        replyDraft = ""
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(FOLD_MAIL_STAGE)
            }
            FoldSplitStack(ctx.navigator)
        }
    }
}

internal class FoldMailAttachment(
    val name: String,
    val kind: String,
    val size: String,
)

internal class FoldMailMessage(
    val id: Int,
    val sender: String,
    val address: String,
    val subject: String,
    val time: String,
    val preview: String,
    val highlights: kotlin.collections.List<String>,
    val paragraphs: kotlin.collections.List<String>,
    val attachments: kotlin.collections.List<FoldMailAttachment>,
)

private class FoldMailAttachmentPageData(
    val mailId: Int,
    val attachmentIndex: Int,
)

private val FOLD_MAIL_FOLDERS = listOf("收件箱", "未读", "星标", "附件")

private val FOLD_MAILS = listOf(
    FoldMailMessage(
        id = 1,
        sender = "产品设计组",
        address = "design@example.com",
        subject = "新版首页评审材料",
        time = "10:24",
        preview = "高保真方案已更新，本轮重点调整大屏内容密度与折叠态过渡。",
        highlights = listOf(
            "高保真稿更新到 v3，覆盖单栏与多栏布局",
            "评审会本周四 15:00，会议室 A",
            "请会前补齐各端实现风险",
        ),
        paragraphs = listOf(
            "各位，新版首页高保真方案已经更新到 v3。这一轮主要解决大屏内容过于稀疏的问题，附件中包含完整的尺寸与栅格说明。",
            "折叠屏展开时，希望布局连续变化而不是整页闪烁。请各端按容器可用宽度计算列数，并保留选中项、滚动位置和输入草稿。",
            "评审会定在本周四 15:00，请会前在共享文档补充风险和建议。",
        ),
        attachments = listOf(
            FoldMailAttachment("首页改版_高保真_v3.pdf", "PDF", "8.4 MB"),
            FoldMailAttachment("内容密度对照表.xlsx", "XLS", "216 KB"),
        ),
    ),
    FoldMailMessage(
        id = 2,
        sender = "增长实验室",
        address = "growth@example.com",
        subject = "A/B 实验周报",
        time = "09:15",
        preview = "三组实验中有两组显著正向，建议把方案 B 分批推到全量。",
        highlights = listOf(
            "方案 B 转化率 +3.2%，置信度 97%",
            "方案 C 与对照组无显著差异",
            "建议下周一开始分批放量",
        ),
        paragraphs = listOf(
            "本周三组实验已经结束观测。方案 B 在下单转化上取得稳定提升，新老用户两个分层方向一致。",
            "若无异议，下周一将按 10%、50%、100% 三批放量，每批之间留一天观察大盘指标。",
        ),
        attachments = listOf(
            FoldMailAttachment("实验明细_第36周.xlsx", "XLS", "1.2 MB"),
        ),
    ),
    FoldMailMessage(
        id = 3,
        sender = "运营中心",
        address = "ops@example.com",
        subject = "九月活动排期确认",
        time = "昨天",
        preview = "九月共有四场活动，请在周五前确认各自负责的素材交付时间。",
        highlights = listOf(
            "九月共四场活动",
            "素材交付时间请在周五前回填",
            "主视觉初稿已附在邮件中",
        ),
        paragraphs = listOf(
            "九月排期已经拉齐，其中开学季和会员日需要首页资源位，请设计与前端提前锁定档期。",
            "附件表格中包含素材负责人和交付时间，请在本周五 18:00 前完成回填。",
        ),
        attachments = listOf(
            FoldMailAttachment("九月活动排期.xlsx", "XLS", "96 KB"),
            FoldMailAttachment("会员日主视觉.png", "PNG", "3.1 MB"),
        ),
    ),
    FoldMailMessage(
        id = 4,
        sender = "技术平台",
        address = "platform@example.com",
        subject = "折叠屏适配自查清单",
        time = "周一",
        preview = "重点检查窗口尺寸变化后的状态保持，以及多栏布局的小窗降级。",
        highlights = listOf(
            "窗口变化后状态必须完整保留",
            "布局判断使用容器实测宽度",
            "逐屏检查多栏到单栏的降级路径",
        ),
        paragraphs = listOf(
            "自查清单第一项是状态保持：窗口变化后，选中项、滚动位置和未提交输入都必须原样保留。",
            "第二项是布局依据。所有分支都应按容器实测宽度判断，阈值来自内容需求而不是设备型号。",
            "第三项是降级路径，每个被收起的栏都必须有明确入口能够重新打开。",
        ),
        attachments = listOf(
            FoldMailAttachment("折叠屏适配自查清单.pdf", "PDF", "640 KB"),
        ),
    ),
)

private fun foldMailById(id: Int): FoldMailMessage =
    FOLD_MAILS.firstOrNull { it.id == id } ?: FOLD_MAILS.first()

private fun foldMailVisible(
    page: FoldSplitMailDemo,
    mail: FoldMailMessage,
): Boolean = when (page.selectedFolder) {
    1 -> page.isUnread(mail.id)
    2 -> page.starredMailIds.contains(mail.id)
    3 -> mail.attachments.isNotEmpty()
    else -> true
}

private fun foldMailFolderCount(page: FoldSplitMailDemo, folder: Int): Int =
    FOLD_MAILS.count { mail ->
        when (folder) {
            1 -> page.isUnread(mail.id)
            2 -> page.starredMailIds.contains(mail.id)
            3 -> mail.attachments.isNotEmpty()
            else -> true
        }
    }

// region master

private fun ViewContainer<*, *>.foldMailCompactMaster(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            width(controller.pageWidth())
            height(controller.pageHeight())
            flexDirectionColumn()
            backgroundColor(FOLD_MAIL_STAGE)
        }
        foldMailMasterHeader(page, compact = true)
        foldMailFolderChips(page)
        foldMailSeparator()
        foldMailList(page, controller, wide = false)
    }
}

private fun ViewContainer<*, *>.foldMailWideMaster(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
) {
    val onGlass = PlatformUtils.isLiquidGlassSupported()
    View {
        attr {
            absolutePositionAllZero()
            borderRadius(FOLD_MAIL_MASTER_RADIUS)
            boxShadow(
                BoxShadow(0f, 8f, 26f, Color(0x28000000)),
                useShadowPath = true,
            )
        }
        View {
            attr {
                absolutePositionAllZero()
                borderRadius(FOLD_MAIL_MASTER_RADIUS)
                overflow(true)
                backgroundColor(Color.TRANSPARENT)
            }
            if (onGlass) {
                LiquidGlass {
                    attr {
                        absolutePositionAllZero()
                        borderRadius(FOLD_MAIL_MASTER_RADIUS)
                        overflow(true)
                        glassEffectTintColor(Color(0x70FFFFFF))
                        glassEffectStyle(GlassEffectStyle.REGULAR)
                        touchEnable(false)
                    }
                }
            } else {
                View {
                    attr {
                        absolutePositionAllZero()
                        backgroundColor(Color(0xF0F8FAFD))
                        touchEnable(false)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                }
                foldMailMasterHeader(page, compact = false, onGlass = onGlass)
                foldMailFolderChips(page, onGlass)
                foldMailSeparator(if (onGlass) Color(0x32FFFFFF) else FOLD_MAIL_DIVIDER)
                foldMailList(page, controller, wide = true)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailMasterHeader(
    page: FoldSplitMailDemo,
    compact: Boolean,
    onGlass: Boolean = false,
) {
    View {
        attr {
            padding(
                left = 16f,
                right = 16f,
                top = if (compact) 16f else 14f,
                bottom = 10f,
            )
            flexDirectionColumn()
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
            }
            Text {
                attr {
                    text(FOLD_MAIL_FOLDERS[page.selectedFolder])
                    flex(1f)
                    color(FOLD_MAIL_TEXT)
                    fontSize(if (compact) 30f else 24f)
                    fontWeightBold()
                }
            }
            View {
                attr {
                    height(24f)
                    paddingLeft(9f)
                    paddingRight(9f)
                    borderRadius(12f)
                    allCenter()
                    backgroundColor(if (onGlass) Color(0x38FFFFFF) else FOLD_MAIL_BLUE_SOFT)
                }
                Text {
                    attr {
                        text("未读 ${foldMailFolderCount(page, 1)}")
                        color(FOLD_MAIL_BLUE)
                        fontSize(11f)
                        fontWeightBold()
                    }
                }
            }
            View {
                attr {
                    size(28f, 28f)
                    marginLeft(8f)
                    borderRadius(14f)
                    allCenter()
                    backgroundColor(if (onGlass) Color(0x38FFFFFF) else FOLD_MAIL_BLUE)
                }
                Text {
                    attr {
                        text("✎")
                        color(if (onGlass) FOLD_MAIL_BLUE else Color.WHITE)
                        fontSize(13f)
                    }
                }
            }
        }
        View {
            attr {
                height(36f)
                marginTop(10f)
                paddingLeft(12f)
                borderRadius(11f)
                justifyContentCenter()
                backgroundColor(if (onGlass) Color(0x32FFFFFF) else FOLD_MAIL_SUNK)
            }
            Text {
                attr {
                    text("⌕  搜索邮件")
                    color(FOLD_MAIL_FAINT)
                    fontSize(13f)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailFolderChips(
    page: FoldSplitMailDemo,
    onGlass: Boolean = false,
) {
    View {
        attr {
            flexDirectionRow()
            flexWrapWrap()
            padding(left = 14f, right = 10f, bottom = 10f)
        }
        FOLD_MAIL_FOLDERS.forEachIndexed { index, title ->
            View {
                attr {
                    height(27f)
                    paddingLeft(10f)
                    paddingRight(10f)
                    marginRight(5f)
                    marginBottom(4f)
                    borderRadius(13.5f)
                    allCenter()
                    backgroundColor(
                        if (page.selectedFolder == index) {
                            FOLD_MAIL_BLUE
                        } else if (onGlass) {
                            Color(0x32FFFFFF)
                        } else {
                            FOLD_MAIL_SUNK
                        },
                    )
                }
                event {
                    click {
                        page.selectedFolder = index
                    }
                }
                Text {
                    attr {
                        text("$title ${foldMailFolderCount(page, index)}")
                        color(if (page.selectedFolder == index) Color.WHITE else FOLD_MAIL_MUTED)
                        fontSize(10f)
                        if (page.selectedFolder == index) {
                            fontWeightBold()
                        } else {
                            fontWeight400()
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailList(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
    wide: Boolean,
) {
    List {
        attr {
            flex(1f)
            width(controller.masterContentWidth())
            bouncesEnable(false)
            showScrollerIndicator(false)
            backgroundColor(Color.TRANSPARENT)
        }
        FOLD_MAILS.forEach { mail ->
            vif({ foldMailVisible(page, mail) }) {
                foldMailRow(page, mail, wide)
            }
        }
        vif({ foldMailFolderCount(page, page.selectedFolder) == 0 }) {
            Text {
                attr {
                    text("这里暂时没有邮件")
                    marginTop(40f)
                    textAlignCenter()
                    color(FOLD_MAIL_FAINT)
                    fontSize(13f)
                }
            }
        }
        View {
            attr {
                height(20f)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailRow(
    page: FoldSplitMailDemo,
    mail: FoldMailMessage,
    wide: Boolean,
) {
    View {
        attr {
            padding(left = 14f, right = 12f, top = 14f, bottom = 13f)
            flexDirectionRow()
            backgroundColor(
                if (wide && page.selectedMailId == mail.id) {
                    Color(0x5CE7F0FF)
                } else {
                    Color.TRANSPARENT
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
                width(12f)
                paddingTop(16f)
            }
            View {
                attr {
                    size(7f, 7f)
                    borderRadius(3.5f)
                    backgroundColor(if (page.isUnread(mail.id)) FOLD_MAIL_BLUE else Color.TRANSPARENT)
                }
            }
        }
        foldMailAvatar(mail.sender.substring(0, 1), mail.id, 38f)
        View {
            attr {
                flex(1f)
                marginLeft(9f)
                flexDirectionColumn()
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
                        lines(1)
                        textOverFlowTail()
                        color(FOLD_MAIL_TEXT)
                        fontSize(13f)
                        if (page.isUnread(mail.id)) {
                            fontWeightBold()
                        } else {
                            fontWeight500()
                        }
                    }
                }
                Text {
                    attr {
                        text(mail.time)
                        marginLeft(6f)
                        color(if (page.isUnread(mail.id)) FOLD_MAIL_BLUE else FOLD_MAIL_FAINT)
                        fontSize(10f)
                    }
                }
                View {
                    attr {
                        size(25f, 22f)
                        allCenter()
                    }
                    event {
                        click {
                            page.toggleStar(mail.id)
                        }
                    }
                    Text {
                        attr {
                            text(if (page.starredMailIds.contains(mail.id)) "★" else "☆")
                            color(
                                if (page.starredMailIds.contains(mail.id)) {
                                    FOLD_MAIL_STAR
                                } else {
                                    FOLD_MAIL_FAINT
                                },
                            )
                            fontSize(14f)
                        }
                    }
                }
            }
            Text {
                attr {
                    text(mail.subject)
                    marginTop(3f)
                    lines(1)
                    textOverFlowTail()
                    color(FOLD_MAIL_TEXT)
                    fontSize(13f)
                    if (page.isUnread(mail.id)) {
                        fontWeightBold()
                    } else {
                        fontWeight400()
                    }
                }
            }
            Text {
                attr {
                    text(mail.preview)
                    marginTop(3f)
                    lines(2)
                    textOverFlowTail()
                    color(FOLD_MAIL_MUTED)
                    fontSize(12f)
                    lineHeight(17f)
                }
            }
            vif({ mail.attachments.isNotEmpty() }) {
                View {
                    attr {
                        marginTop(8f)
                        flexDirectionRow()
                    }
                    View {
                        attr {
                            height(20f)
                            paddingLeft(8f)
                            paddingRight(8f)
                            borderRadius(10f)
                            allCenter()
                            backgroundColor(FOLD_MAIL_BLUE_SOFT)
                        }
                        Text {
                            attr {
                                text("附件 ${mail.attachments.size}")
                                color(FOLD_MAIL_BLUE)
                                fontSize(10f)
                                fontWeightMedium()
                            }
                        }
                    }
                    vif({ page.starredMailIds.contains(mail.id) }) {
                        View {
                            attr {
                                height(20f)
                                marginLeft(6f)
                                paddingLeft(8f)
                                paddingRight(8f)
                                borderRadius(10f)
                                allCenter()
                                backgroundColor(Color(0x22E5A12C))
                            }
                            Text {
                                attr {
                                    text("星标")
                                    color(FOLD_MAIL_STAR)
                                    fontSize(10f)
                                    fontWeightMedium()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    foldMailSeparator(indent = 60f)
}

// endregion

// region detail stack

private fun ViewContainer<*, *>.foldMailSummaryPage(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            width(controller.pageWidth())
            height(controller.pageHeight())
            flexDirectionColumn()
            backgroundColor(FOLD_MAIL_STAGE)
            boxShadow(
                BoxShadow(controller.primaryShift * 0.04f, 5f, 24f, Color(0x1F000000)),
                useShadowPath = true,
            )
        }
        foldMailDetailBar(
            page = page,
            controller = controller,
            title = "邮件摘要",
            compactBackToMaster = true,
            showStar = true,
        )
        List {
            attr {
                flex(1f)
                backgroundColor(FOLD_MAIL_STAGE)
                showScrollerIndicator(false)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(foldMailContentLeft(controller))
                    paddingRight(FOLD_MAIL_PAGE_PAD)
                    paddingTop(12f)
                    paddingBottom(40f)
                }
                foldMailSummaryCard(page)
                foldMailQuickActions(page)
                foldMailReplyComposer(page)
            }
        }
        vif({ !controller.isCompact() && controller.coverShadow > 0f }) {
            View {
                attr {
                    absolutePositionAllZero()
                    backgroundColor(Color(0x000000, controller.coverShadow * 0.12f))
                    touchEnable(false)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailSummaryCard(page: FoldSplitMailDemo) {
    View {
        attr {
            padding(18f)
            borderRadius(20f)
            backgroundColor(Color.WHITE)
            boxShadow(BoxShadow(0f, 3f, 14f, Color(0x12000000)))
        }
        Text {
            attr {
                text(page.selectedMail().subject)
                color(FOLD_MAIL_TEXT)
                fontSize(22f)
                lineHeight(30f)
                fontWeightBold()
            }
        }
        foldMailSenderLine(page.selectedMail())
        Text {
            attr {
                text("收件人：我 · 抄送：设计评审组")
                marginTop(10f)
                color(FOLD_MAIL_FAINT)
                fontSize(12f)
            }
        }
        foldMailSeparator(verticalMargin = 16f)
        Text {
            attr {
                text("重点摘要")
                color(FOLD_MAIL_TEXT)
                fontSize(13f)
                fontWeightBold()
            }
        }
        for (slot in 0 until 3) {
            vif({ page.selectedMail().highlights.size > slot }) {
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(10f)
                    }
                    View {
                        attr {
                            size(6f, 6f)
                            marginTop(6f)
                            marginRight(9f)
                            borderRadius(3f)
                            backgroundColor(FOLD_MAIL_BLUE)
                        }
                    }
                    Text {
                        attr {
                            text(page.selectedMail().highlights.getOrNull(slot) ?: "")
                            flex(1f)
                            color(FOLD_MAIL_BODY)
                            fontSize(13f)
                            lineHeight(20f)
                        }
                    }
                }
            }
        }
        Text {
            attr {
                text(page.selectedMail().preview)
                marginTop(14f)
                color(FOLD_MAIL_MUTED)
                fontSize(13f)
                lineHeight(21f)
            }
        }
        vif({ page.selectedMail().attachments.isNotEmpty() }) {
            Text {
                attr {
                    text("附件")
                    marginTop(16f)
                    marginBottom(4f)
                    color(FOLD_MAIL_TEXT)
                    fontSize(13f)
                    fontWeightBold()
                }
            }
            page.selectedMail().attachments.forEachIndexed { index, attachment ->
                foldMailAttachmentRow(attachment) {
                    page.openAttachment(page.selectedMailId, index)
                }
            }
        }
        View {
            attr {
                height(46f)
                marginTop(18f)
                borderRadius(13f)
                backgroundColor(FOLD_MAIL_BLUE)
                allCenter()
            }
            event {
                click {
                    page.openBody()
                }
            }
            Text {
                attr {
                    text("查看完整邮件  ›")
                    color(Color.WHITE)
                    fontSize(14f)
                    fontWeightBold()
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailQuickActions(page: FoldSplitMailDemo) {
    View {
        attr {
            marginTop(14f)
            flexDirectionRow()
        }
        foldMailQuickAction("归档")
        foldMailQuickAction("回复") {
            page.openBody()
        }
        foldMailQuickAction("转发") {
            page.openBody()
        }
        foldMailQuickAction("删除", FOLD_MAIL_ALERT)
    }
}

private fun ViewContainer<*, *>.foldMailQuickAction(
    title: String,
    color: Color = FOLD_MAIL_BLUE,
    onClick: () -> Unit = {},
) {
    View {
        attr {
            flex(1f)
            height(40f)
            marginRight(8f)
            borderRadius(12f)
            allCenter()
            backgroundColor(Color.WHITE)
            boxShadow(BoxShadow(0f, 1f, 8f, Color(0x0D000000)))
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(title)
                color(color)
                fontSize(13f)
                fontWeightMedium()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailBodyPage(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
    mailId: Int,
) {
    val mail = foldMailById(mailId)
    foldMailPushedPage(controller, "邮件正文") {
        View {
            attr {
                padding(18f)
                borderRadius(20f)
                backgroundColor(Color.WHITE)
                boxShadow(BoxShadow(0f, 3f, 14f, Color(0x12000000)))
            }
            Text {
                attr {
                    text(mail.subject)
                    color(FOLD_MAIL_TEXT)
                    fontSize(22f)
                    lineHeight(30f)
                    fontWeightBold()
                }
            }
            foldMailSenderLine(mail)
            foldMailSeparator(verticalMargin = 16f)
            mail.paragraphs.forEach { paragraph ->
                Text {
                    attr {
                        text(paragraph)
                        marginBottom(14f)
                        color(FOLD_MAIL_BODY)
                        fontSize(14f)
                        lineHeight(24f)
                    }
                }
            }
            if (mail.attachments.isNotEmpty()) {
                Text {
                    attr {
                        text("附件")
                        marginTop(4f)
                        marginBottom(8f)
                        color(FOLD_MAIL_TEXT)
                        fontSize(14f)
                        fontWeightBold()
                    }
                }
                mail.attachments.forEachIndexed { index, attachment ->
                    foldMailAttachmentRow(attachment) {
                        page.openAttachment(mail.id, index)
                    }
                }
            }
        }
        foldMailReplyComposer(page)
    }
}

private fun ViewContainer<*, *>.foldMailAttachmentPage(
    page: FoldSplitMailDemo,
    controller: FoldSplitStackController,
    data: FoldMailAttachmentPageData,
) {
    val mail = foldMailById(data.mailId)
    val attachment = mail.attachments.getOrNull(data.attachmentIndex)
        ?: FoldMailAttachment("附件不可用", "FILE", "--")
    foldMailPushedPage(controller, "附件预览") {
        View {
            attr {
                minHeight(420f)
                padding(24f)
                borderRadius(22f)
                alignItemsCenter()
                backgroundColor(Color.WHITE)
                boxShadow(BoxShadow(0f, 4f, 18f, Color(0x16000000)))
            }
            View {
                attr {
                    size(92f, 116f)
                    marginTop(24f)
                    borderRadius(14f)
                    backgroundColor(foldMailAttachmentTint(attachment.kind))
                    allCenter()
                    boxShadow(BoxShadow(0f, 6f, 16f, Color(0x22000000)))
                }
                Text {
                    attr {
                        text(attachment.kind)
                        color(Color.WHITE)
                        fontSize(20f)
                        fontWeightBold()
                    }
                }
            }
            Text {
                attr {
                    text(attachment.name)
                    marginTop(22f)
                    color(FOLD_MAIL_TEXT)
                    fontSize(18f)
                    fontWeightBold()
                    textAlignCenter()
                }
            }
            Text {
                attr {
                    text("${attachment.size} · 来自「${mail.subject}」")
                    marginTop(8f)
                    color(FOLD_MAIL_FAINT)
                    fontSize(12f)
                    textAlignCenter()
                }
            }
            View {
                attr {
                    width(180f)
                    height(42f)
                    marginTop(28f)
                    borderRadius(12f)
                    backgroundColor(FOLD_MAIL_BLUE_SOFT)
                    allCenter()
                }
                event {
                    click {
                        controller.closeToPrimary()
                    }
                }
                Text {
                    attr {
                        text("完成预览")
                        color(FOLD_MAIL_BLUE)
                        fontSize(14f)
                        fontWeightBold()
                    }
                }
            }
            Text {
                attr {
                    text("可下载、分享或保存到文件")
                    marginTop(18f)
                    color(FOLD_MAIL_FAINT)
                    fontSize(11f)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailPushedPage(
    controller: FoldSplitStackController,
    title: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            width(controller.pageWidth())
            height(controller.pageHeight())
            flexDirectionColumn()
            borderRadius(if (controller.isCompact()) 0f else FOLD_MAIL_PAGE_RADIUS)
            overflow(true)
            backgroundColor(FOLD_MAIL_STAGE)
        }
        foldMailDetailBar(
            page = null,
            controller = controller,
            title = title,
            compactBackToMaster = false,
            showStar = false,
        )
        List {
            attr {
                flex(1f)
                showScrollerIndicator(false)
                backgroundColor(FOLD_MAIL_STAGE)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(foldMailContentLeft(controller))
                    paddingRight(FOLD_MAIL_PAGE_PAD)
                    paddingTop(12f)
                    paddingBottom(42f)
                }
                content()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailDetailBar(
    page: FoldSplitMailDemo?,
    controller: FoldSplitStackController,
    title: String,
    compactBackToMaster: Boolean,
    showStar: Boolean,
) {
    View {
        attr {
            width(controller.pageWidth())
            height(54f)
            paddingLeft(controller.contentInsetLeft())
            flexDirectionRow()
            alignItemsCenter()
            justifyContentCenter()
            backgroundColor(Color(0xF7FFFFFF))
        }
        vif({ !compactBackToMaster || controller.isCompact() }) {
            View {
                attr {
                    positionAbsolute()
                    left(controller.contentInsetLeft() + 12f)
                    top(9f)
                    height(36f)
                    paddingLeft(9f)
                    paddingRight(12f)
                    borderRadius(18f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(Color.WHITE)
                    boxShadow(BoxShadow(0f, 1f, 5f, Color(0x14000000)))
                }
                event {
                    click {
                        if (compactBackToMaster) {
                            controller.backToMaster()
                        } else {
                            controller.close()
                        }
                    }
                }
                Text {
                    attr {
                        text("‹")
                        marginTop(-2f)
                        marginRight(3f)
                        color(FOLD_MAIL_BLUE)
                        fontSize(26f)
                    }
                }
                Text {
                    attr {
                        text(if (compactBackToMaster) "邮件" else "返回")
                        color(FOLD_MAIL_BLUE)
                        fontSize(13f)
                    }
                }
            }
        }
        Text {
            attr {
                text(title)
                color(FOLD_MAIL_TEXT)
                fontSize(16f)
                fontWeightSemiBold()
            }
        }
        if (page != null && showStar) {
            View {
                attr {
                    positionAbsolute()
                    right(14f)
                    top(10f)
                    size(34f, 34f)
                    allCenter()
                }
                event {
                    click {
                        page.toggleStar(page.selectedMailId)
                    }
                }
                Text {
                    attr {
                        text(if (page.starredMailIds.contains(page.selectedMailId)) "★" else "☆")
                        color(
                            if (page.starredMailIds.contains(page.selectedMailId)) {
                                FOLD_MAIL_STAR
                            } else {
                                FOLD_MAIL_MUTED
                            },
                        )
                        fontSize(19f)
                    }
                }
            }
        }
        foldMailSeparator(positionAtBottom = true)
    }
}

// endregion

// region detail atoms

private fun ViewContainer<*, *>.foldMailSenderLine(mail: FoldMailMessage) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            marginTop(16f)
        }
        foldMailAvatar(mail.sender.substring(0, 1), mail.id, 42f)
        View {
            attr {
                flex(1f)
                marginLeft(10f)
                flexDirectionColumn()
            }
            Text {
                attr {
                    text(mail.sender)
                    color(FOLD_MAIL_TEXT)
                    fontSize(14f)
                    fontWeightBold()
                }
            }
            Text {
                attr {
                    text(mail.address)
                    marginTop(3f)
                    color(FOLD_MAIL_FAINT)
                    fontSize(11f)
                }
            }
        }
        Text {
            attr {
                text(mail.time)
                color(FOLD_MAIL_FAINT)
                fontSize(11f)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailAttachmentRow(
    attachment: FoldMailAttachment,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(58f)
            marginTop(7f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(12f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_MAIL_SUNK)
        }
        event {
            click {
                onClick()
            }
        }
        View {
            attr {
                size(38f, 38f)
                borderRadius(9f)
                backgroundColor(foldMailAttachmentTint(attachment.kind))
                allCenter()
            }
            Text {
                attr {
                    text(attachment.kind)
                    color(Color.WHITE)
                    fontSize(9f)
                    fontWeightBold()
                }
            }
        }
        View {
            attr {
                flex(1f)
                marginLeft(10f)
            }
            Text {
                attr {
                    text(attachment.name)
                    lines(1)
                    textOverFlowTail()
                    color(FOLD_MAIL_TEXT)
                    fontSize(12f)
                    fontWeight500()
                }
            }
            Text {
                attr {
                    text(attachment.size)
                    marginTop(3f)
                    color(FOLD_MAIL_FAINT)
                    fontSize(10f)
                }
            }
        }
        Text {
            attr {
                text("预览  ›")
                color(FOLD_MAIL_BLUE)
                fontSize(12f)
                fontWeightBold()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailReplyComposer(page: FoldSplitMailDemo) {
    val initialDraft = page.replyDraft
    View {
        attr {
            marginTop(14f)
            padding(16f)
            borderRadius(18f)
            backgroundColor(Color.WHITE)
            boxShadow(BoxShadow(0f, 2f, 12f, Color(0x10000000)))
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
            }
            Text {
                attr {
                    text("回复 ${page.selectedMail().sender}")
                    flex(1f)
                    color(FOLD_MAIL_TEXT)
                    fontSize(13f)
                    fontWeightBold()
                }
            }
            Text {
                attr {
                    text("已发送 ${page.sentReplyCount}")
                    color(FOLD_MAIL_FAINT)
                    fontSize(10f)
                }
            }
        }
        View {
            attr {
                height(44f)
                marginTop(10f)
                paddingLeft(12f)
                paddingRight(12f)
                borderRadius(11f)
                justifyContentCenter()
                backgroundColor(FOLD_MAIL_SUNK)
            }
            Input {
                attr {
                    text(initialDraft)
                    placeholder("写下回复…")
                    placeholderColor(FOLD_MAIL_FAINT)
                    color(FOLD_MAIL_TEXT)
                    fontSize(13f)
                }
                event {
                    textDidChange {
                        page.replyDraft = it.text
                    }
                }
            }
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginTop(10f)
            }
            Text {
                attr {
                    text("草稿 ${page.replyDraft.length} 字")
                    flex(1f)
                    color(FOLD_MAIL_FAINT)
                    fontSize(10f)
                }
            }
            View {
                attr {
                    height(32f)
                    paddingLeft(16f)
                    paddingRight(16f)
                    borderRadius(10f)
                    allCenter()
                    backgroundColor(
                        if (page.replyDraft.isBlank()) FOLD_MAIL_DISABLED else FOLD_MAIL_BLUE,
                    )
                }
                event {
                    click {
                        page.sendReply()
                    }
                }
                Text {
                    attr {
                        text("发送")
                        color(Color.WHITE)
                        fontSize(12f)
                        fontWeightBold()
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailAvatar(
    label: String,
    seed: Int,
    size: Float,
) {
    View {
        attr {
            size(size, size)
            borderRadius(size / 2f)
            backgroundColor(FOLD_MAIL_AVATARS[seed % FOLD_MAIL_AVATARS.size])
            allCenter()
        }
        Text {
            attr {
                text(label)
                color(Color.WHITE)
                fontSize(size * 0.38f)
                fontWeightBold()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldMailSeparator(
    color: Color = FOLD_MAIL_DIVIDER,
    indent: Float = 0f,
    verticalMargin: Float = 0f,
    positionAtBottom: Boolean = false,
) {
    View {
        attr {
            if (positionAtBottom) {
                positionAbsolute()
                left(0f)
                right(0f)
                bottom(0f)
            } else {
                marginLeft(indent)
                marginTop(verticalMargin)
                marginBottom(verticalMargin)
            }
            height(1f)
            backgroundColor(color)
        }
    }
}

private fun foldMailContentLeft(controller: FoldSplitStackController): Float =
    controller.contentInsetLeft() + FOLD_MAIL_PAGE_PAD

private fun foldMailAttachmentTint(kind: String): Color = when (kind) {
    "PDF" -> Color(0xFFE24B4B)
    "XLS" -> Color(0xFF1AA66A)
    "PNG" -> Color(0xFF7C5CE7)
    else -> Color(0xFF64748B)
}

// endregion

private const val FOLD_MAIL_MASTER_RADIUS = 26f
private const val FOLD_MAIL_PAGE_RADIUS = 26f
private const val FOLD_MAIL_PAGE_PAD = 22f

private val FOLD_MAIL_STAGE = Color(0xFFF2F5FA)
private val FOLD_MAIL_SUNK = Color(0xFFEDF1F7)
private val FOLD_MAIL_DIVIDER = Color(0xFFE1E6EE)
private val FOLD_MAIL_TEXT = Color(0xFF172033)
private val FOLD_MAIL_BODY = Color(0xFF3A4558)
private val FOLD_MAIL_MUTED = Color(0xFF68758A)
private val FOLD_MAIL_FAINT = Color(0xFF98A3B5)
private val FOLD_MAIL_BLUE = Color(0xFF1768E5)
private val FOLD_MAIL_BLUE_SOFT = Color(0xFFE5EFFF)
private val FOLD_MAIL_STAR = Color(0xFFE5A12C)
private val FOLD_MAIL_DISABLED = Color(0xFFB9C2D0)
private val FOLD_MAIL_ALERT = Color(0xFFE24B4B)

private val FOLD_MAIL_AVATARS = listOf(
    Color(0xFF2962D9),
    Color(0xFF7C4DCC),
    Color(0xFF158272),
    Color(0xFFBF6A22),
    Color(0xFFC23B67),
)
