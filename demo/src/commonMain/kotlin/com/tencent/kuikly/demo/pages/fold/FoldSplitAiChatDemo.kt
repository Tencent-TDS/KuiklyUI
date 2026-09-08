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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 用 [FoldSplitStackController] 搭的 AI 工作助手，把「聊天列表 + 对话」折进主窗-详情骨架。
 *
 * 小屏：master 是整屏的任务列表 → 一级详情是对话或目的地 → 资料页压在上面。
 * 大屏：master 变成左侧液态玻璃侧栏；点目的地只换一级详情，不从会话再推进去。
 *
 * 这里刻意不放底部 Tab，也不放抽屉：目的地和会话列表都收在 master 里，
 * 小屏靠 [FoldSplitStackController.backToMaster] 回列表，大屏靠常驻侧栏，两个断点共用一份状态。
 */
@Page("FoldSplitAiChatDemo")
internal class FoldSplitAiChatDemo : BasePager() {

    // region 业务状态

    /** 当前会话；小于 0 表示一级详情停在欢迎页或目的地。 */
    var selectedChatId by observable(-1)

    /** 侧栏目的地；非空时一级详情就是该目的地，不再压在会话上面。 */
    var selectedDestId by observable("")

    var conversationTitle by observable(WELCOME_TITLE)
    var chatCount by observable(0)
    var suggestionSet by observable(0)

    /** 草稿只在 [sendDraft] 时读；不回写 Input，避免中文输入被打断。 */
    var draftText by observable("")

    /** 发送后 +1，用来换掉 Input 节点，从而清空输入框。 */
    var composerEpoch by observable(0)

    var chats by observableList<FoldChatSession>()
    var activeMessages by observableList<FoldChatMessage>()

    private val messageStore = mutableMapOf<Int, MutableList<FoldChatMessage>>()
    private var nextChatId = 1

    // endregion

    private val navigator = FoldSplitStackController(
        pager = this,
        layout = FoldSplitStackLayout(
            masterWidth = MASTER_WIDTH,
            masterMinWidth = MASTER_MIN_WIDTH,
            masterMaxWidth = MASTER_MAX_WIDTH,
            masterInset = MASTER_INSET,
            detailMinWidth = CHAT_MIN_WIDTH,
            contentPad = CONTENT_PAD,
            masterTextLeft = MASTER_TEXT_LEFT,
        ),
        masterContent = { controller ->
            vif({ controller.isCompact() }) {
                foldChatCompactRoot(this@FoldSplitAiChatDemo, controller)
            }
            vif({ controller.showsMaster() }) {
                foldChatSidebar(this@FoldSplitAiChatDemo, controller)
            }
        },
        primaryContent = { controller ->
            foldChatPrimaryPage(this@FoldSplitAiChatDemo, controller)
        },
    )

    /** 组件只管到舞台边缘，底部安全区留给内容自己让。 */
    fun safeBottom(): Float = pageData.safeAreaInsets.bottom

    // region 会话

    /** 点会话：清掉目的地和更深页，一级详情换成这一路对话。 */
    fun openChat(id: Int) {
        selectedDestId = ""
        selectedChatId = id
        conversationTitle = chats.firstOrNull { it.id == id }?.title ?: WELCOME_TITLE
        reloadActiveMessages()
        navigator.selectPrimary(closeDeeperPages = true)
    }

    fun createTask(firstUserText: String? = null) {
        val id = nextChatId++
        val title = firstUserText?.take(18)?.ifEmpty { NEW_TASK_TITLE } ?: NEW_TASK_TITLE
        chats.add(0, FoldChatSession(id, title, "刚刚", "本地 mock，无网络请求"))
        val seed = mutableListOf<FoldChatMessage>()
        if (firstUserText != null) {
            seed.add(FoldChatMessage(true, firstUserText))
            seed.add(FoldChatMessage(false, foldChatReply(firstUserText)))
        }
        messageStore[id] = seed
        chatCount = chats.size
        openChat(id)
    }

    fun startPrompt(text: String) {
        draftText = ""
        composerEpoch++
        createTask(text)
    }

    /** 消息全部是本地 mock：用户一条、助手一条，直接落进 [activeMessages]。 */
    fun sendDraft() {
        val text = draftText.trim()
        if (text.isEmpty()) {
            return
        }
        draftText = ""
        composerEpoch++
        if (selectedChatId < 0) {
            createTask(text)
            return
        }
        appendMessage(selectedChatId, FoldChatMessage(true, text))
        appendMessage(selectedChatId, FoldChatMessage(false, foldChatReply(text)))
        renameNewTaskByFirstPrompt(text)
    }

    fun backToWelcome() {
        selectedDestId = ""
        selectedChatId = -1
        conversationTitle = WELCOME_TITLE
        activeMessages.clear()
    }

    private fun renameNewTaskByFirstPrompt(text: String) {
        val index = chats.indexOfFirst { it.id == selectedChatId }
        if (index < 0 || chats[index].title != NEW_TASK_TITLE) {
            return
        }
        val old = chats[index]
        val title = text.take(18)
        // ObservableList 靠增删触发刷新，就地改字段不会通知 UI。
        chats.removeAt(index)
        chats.add(index, FoldChatSession(old.id, title, "刚刚", old.summary))
        conversationTitle = title
    }

    private fun appendMessage(chatId: Int, message: FoldChatMessage) {
        val bucket = messageStore.getOrPut(chatId) { mutableListOf() }
        bucket.add(message)
        if (chatId == selectedChatId) {
            activeMessages.add(message)
        }
    }

    private fun reloadActiveMessages() {
        activeMessages.clear()
        messageStore[selectedChatId]?.forEach { activeMessages.add(it) }
    }

    // endregion

    // region 更深一层

    /**
     * 点目的地：当成一级详情切换，对齐设置页换分类。
     * 不要 [FoldSplitStackController.open] 压在会话上，否则无论点哪个都像从会话滑进来。
     */
    fun openDestination(dest: FoldChatDestination) {
        selectedDestId = dest.id
        navigator.selectPrimary(closeDeeperPages = true)
    }

    /** 会话信息：从一级详情再压一层。 */
    fun openChatInfo() {
        val chat = chats.firstOrNull { it.id == selectedChatId } ?: return
        navigator.open(
            id = "chat-info-${chat.id}",
            title = "会话信息",
            data = chat,
        ) { controller, entry ->
            foldChatInfoPage(
                page = this@FoldSplitAiChatDemo,
                controller = controller,
                chat = entry.data as? FoldChatSession ?: chat,
            )
        }
    }

    /** 资料页：第三层及以上，用来验证栈能一直往下开。 */
    fun openResource(scope: String, title: String) {
        navigator.open(
            id = "res-$scope-$title",
            title = title,
        ) { controller, entry ->
            foldChatResourcePage(this@FoldSplitAiChatDemo, controller, entry.title)
        }
    }

    // endregion

    override fun created() {
        super.created()
        chats.add(FoldChatSession(1, "你好", "刚刚", "打个招呼，看看助手能做什么"))
        messageStore[1] = mutableListOf(
            FoldChatMessage(true, "你好"),
            FoldChatMessage(
                false,
                "你好！有什么需要我处理的吗？无论是写文档、整理数据、生成代码，还是查资料做分析，告诉我就行。",
            ),
        )
        nextChatId = 2
        chatCount = chats.size
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(FOLD_CHAT_APP_BG)
            }
            FoldSplitStack(ctx.navigator)
        }
    }

    companion object {
        const val MASTER_WIDTH = 268f
        const val MASTER_MIN_WIDTH = 214f
        const val MASTER_MAX_WIDTH = 340f
        const val MASTER_INSET = 10f
        const val MASTER_RADIUS = 26f
        const val PAGE_RADIUS = 26f

        /** 对话区最低可读宽度，和 [MASTER_MIN_WIDTH] 一起决定断点，不查机型。 */
        const val CHAT_MIN_WIDTH = 392f
        const val CONTENT_PAD = 24f
        const val COMPACT_PAD = 16f

        /** 侧栏标题左边缘：浮层边距 + 侧栏内边距 + 行内边距 + 图标列宽。 */
        const val MASTER_TEXT_LEFT = MASTER_INSET + 10f + 10f + 28f

        const val WELCOME_TITLE = "云端工作助手"
        const val NEW_TASK_TITLE = "新任务"
    }
}

// region 数据模型

internal class FoldChatSession(
    val id: Int,
    val title: String,
    val time: String,
    val summary: String,
)

internal class FoldChatMessage(
    val fromUser: Boolean,
    val text: String,
)

internal class FoldChatDestination(
    val id: String,
    val icon: String,
    val title: String,
    val hint: String,
    val color: Color,
)

private val FOLD_CHAT_DESTINATIONS = listOf(
    FoldChatDestination("assistant", "✦", "助理", "问答、写文档、整理数据", Color(0xFF0A84FF)),
    FoldChatDestination("projects", "▣", "项目", "把对话沉淀成可追踪的项目", Color(0xFF5E5CE6)),
    FoldChatDestination("experts", "☺", "专家", "按领域找对应的工作助手", Color(0xFF30D158)),
    FoldChatDestination("automation", "⚡", "自动化", "把重复步骤收成流程", Color(0xFFFF9F0A)),
    FoldChatDestination("library", "▤", "资料库", "文件、知识与引用", Color(0xFF8E8E93)),
)

private val FOLD_CHAT_SUGGESTION_SETS = listOf(
    listOf(
        "⏱" to "进行数据分析及可视化",
        "▤" to "整理一份产品文档",
        "▣" to "做一份幻灯片",
        "▦" to "制作一个工作台",
    ),
    listOf(
        "✎" to "整理本周工作纪要",
        "✉" to "写一封项目周报",
        "</>" to "生成接口说明",
        "☑" to "归纳会议待办",
    ),
)

private fun foldChatReply(userText: String): String = when {
    userText.contains("你好") || userText.contains("嗨") ->
        "你好！有什么需要我处理的吗？无论是写文档、整理数据、生成代码，还是查资料做分析，告诉我就行。"
    userText.contains("幻灯") || userText.contains("PPT") || userText.contains("ppt") ->
        "可以。先告诉我主题、页数和受众，我按大纲 → 逐页要点 → 讲稿的顺序帮你出一版草稿。"
    userText.contains("数据") ->
        "把数据源或关键指标发我。我可以先清洗字段、给出图表建议，再写成一段给领导看的结论。"
    else ->
        "收到：「$userText」。我可以按写文档、整理数据、生成代码或查资料来处理，补充一点背景会更准。"
}

// endregion

// region 尺寸

/** 页面本身铺满整屏，靠左内边距让开主窗，所以左右滑的位移始终是完整一屏。 */
private fun foldChatContentLeft(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldSplitAiChatDemo.COMPACT_PAD
    } else {
        controller.contentInsetLeft() + FoldSplitAiChatDemo.CONTENT_PAD
    }

private fun foldChatContentRight(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) {
        FoldSplitAiChatDemo.COMPACT_PAD
    } else {
        FoldSplitAiChatDemo.CONTENT_PAD
    }

// endregion

// region master：小屏整屏根列表

/** 小屏 master：新建任务 + 会话列表 + 目的地，被 PageList 的占位窗透出来。 */
private fun ViewContainer<*, *>.foldChatCompactRoot(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        List {
            attr {
                flex(1f)
                bouncesEnable(false)
                showScrollerIndicator(false)
                backgroundColor(FOLD_CHAT_APP_BG)
            }
            View {
                attr {
                    // 子项必须写成整屏宽，List 默认按内容收缩。
                    width(controller.pageWidth())
                    paddingLeft(FoldSplitAiChatDemo.COMPACT_PAD)
                    paddingRight(FoldSplitAiChatDemo.COMPACT_PAD)
                    paddingTop(8f)
                    paddingBottom(28f + page.safeBottom())
                }
                Text {
                    attr {
                        text(FoldSplitAiChatDemo.WELCOME_TITLE)
                        marginBottom(14f)
                        fontSize(32f)
                        fontWeightBold()
                        color(FOLD_CHAT_TEXT)
                    }
                }
                foldChatSearchBar(height = 42f, fontSize = 16f)
                foldChatNewTaskCard(page, compact = true)
                foldChatSectionTitle("目的地")
                foldChatGroup {
                    FOLD_CHAT_DESTINATIONS.forEachIndexed { index, dest ->
                        foldChatDestinationRow(
                            page = page,
                            dest = dest,
                            compact = true,
                            showSeparator = index != FOLD_CHAT_DESTINATIONS.lastIndex,
                        )
                    }
                }
                foldChatSectionTitle("最近任务 (${page.chatCount})")
                foldChatGroup {
                    foldChatSessionRows(page, compact = true)
                }
            }
        }
    }
}

// endregion

// region master：大屏液态玻璃侧栏

/**
 * 大屏 master：iOS / macOS 风格的液态玻璃浮窗。
 *
 * 组件只给了 frame，圆角靠外层 overflow 裁切，阴影用 useShadowPath 让玻璃层也能投影。
 */
private fun ViewContainer<*, *>.foldChatSidebar(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    val onGlass = PlatformUtils.isLiquidGlassSupported()
    val radius = FoldSplitAiChatDemo.MASTER_RADIUS
    View {
        attr {
            absolutePositionAllZero()
            borderRadius(radius)
            boxShadow(
                BoxShadow(0f, 6f, 20f, Color(0x24000000)),
                useShadowPath = true,
            )
        }
        View {
            attr {
                absolutePositionAllZero()
                borderRadius(radius)
                overflow(true)
                backgroundColor(Color.TRANSPARENT)
            }
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
                            overflow(true)
                            backgroundColor(Color(0xE6F8F8FA))
                            touchEnable(false)
                        }
                    }
            }
            View {
                attr {
                    flex(1f)
                    // 这条 10f 内边距参与 MASTER_TEXT_LEFT 的推导，改动要一起改。
                    padding(left = 10f, top = 12f, right = 10f, bottom = 10f)
                }
                foldChatSidebarHeader(controller)
                foldChatNewTaskCard(page, compact = false, onGlass = onGlass)
                List {
                    attr {
                        flex(1f)
                        marginTop(6f)
                        bouncesEnable(false)
                        showScrollerIndicator(false)
                        backgroundColor(Color.TRANSPARENT)
                    }
                    foldChatSidebarLabel("目的地")
                    FOLD_CHAT_DESTINATIONS.forEach { dest ->
                        foldChatDestinationRow(
                            page = page,
                            dest = dest,
                            compact = false,
                            showSeparator = false,
                            onGlass = onGlass,
                        )
                    }
                    foldChatSidebarLabel("任务 (${page.chatCount})")
                    foldChatSessionRows(page, compact = false, onGlass = onGlass)
                }
                foldChatProfileFooter(page)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatSidebarHeader(controller: FoldSplitStackController) {
    View {
            attr {
                height(42f)
                paddingLeft(10f)
                paddingRight(6f)
                flexDirectionRow()
                alignItemsCenter()
            }
            View {
                attr {
                    size(28f, 28f)
                    marginRight(8f)
                    borderRadius(8f)
                    allCenter()
                    backgroundColor(FOLD_CHAT_BLUE)
                }
                Text {
                    attr {
                        text("☁")
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                }
                Text {
                    attr {
                        text("云端工作区")
                        fontSize(16f)
                        fontWeightBold()
                        color(FOLD_CHAT_TEXT)
                    }
                }
                Text {
                    attr {
                        text("个人 · 已同步")
                        marginTop(1f)
                        fontSize(11f)
                        color(FOLD_CHAT_SECONDARY)
                    }
                }
            }
        View {
            attr {
                size(28f, 28f)
                allCenter()
                borderRadius(8f)
            }
            event {
                click {
                    controller.collapseMaster()
                }
            }
            Text {
                attr {
                    text("⇤")
                    fontSize(15f)
                    color(FOLD_CHAT_SECONDARY)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatSidebarLabel(title: String) {
    Text {
        attr {
            text(title)
            margin(left = 10f, top = 12f, bottom = 6f)
            fontSize(12f)
            fontWeightMedium()
            color(FOLD_CHAT_SECONDARY)
        }
    }
}

private fun ViewContainer<*, *>.foldChatProfileFooter(page: FoldSplitAiChatDemo) {
    View {
        attr {
            height(48f)
            marginTop(4f)
            paddingLeft(10f)
            paddingRight(10f)
            flexDirectionRow()
            alignItemsCenter()
        }
        View {
            attr {
                size(28f, 28f)
                borderRadius(14f)
                allCenter()
                backgroundColor(FOLD_CHAT_AVATAR)
            }
            Text {
                attr {
                    text("K")
                    fontSize(13f)
                    fontWeightBold()
                    color(Color.WHITE)
                }
            }
        }
        Text {
            attr {
                text("Kuikly")
                flex(1f)
                marginLeft(10f)
                fontSize(13f)
                color(FOLD_CHAT_TEXT)
            }
        }
        Text {
            attr {
                text("${page.chatCount} 个任务")
                fontSize(11f)
                color(FOLD_CHAT_SECONDARY)
            }
        }
    }
}

// endregion

// region master 通用行

/** 纯装饰的搜索条，不接输入，避免和 composer 抢焦点。 */
private fun ViewContainer<*, *>.foldChatSearchBar(height: Float, fontSize: Float) {
    View {
        attr {
            height(height)
            paddingLeft(11f)
            paddingRight(10f)
            borderRadius(11f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(Color(0xFFE9E9ED))
        }
        Text {
            attr {
                text("⌕")
                fontSize(fontSize)
                color(FOLD_CHAT_SECONDARY)
            }
        }
        Text {
            attr {
                text("搜索任务")
                flex(1f)
                marginLeft(7f)
                fontSize(fontSize)
                color(FOLD_CHAT_SECONDARY)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatNewTaskCard(
    page: FoldSplitAiChatDemo,
    compact: Boolean,
    onGlass: Boolean = false,
) {
    View {
        attr {
            height(if (compact) 48f else 42f)
            marginTop(if (compact) 16f else 10f)
            paddingLeft(if (compact) 16f else 10f)
            paddingRight(12f)
            borderRadius(if (compact) 14f else 11f)
            flexDirectionRow()
            alignItemsCenter()
            if (compact) {
                backgroundColor(FOLD_CHAT_CARD)
                boxShadow(BoxShadow(0f, 1f, 8f, Color(0x0F000000)))
            } else {
                backgroundColor(if (onGlass) Color(0x33FFFFFF) else FOLD_CHAT_CARD)
                border(Border(1f, BorderStyle.SOLID, FOLD_CHAT_DIVIDER))
            }
        }
        event {
            click {
                page.createTask()
            }
        }
        Text {
            attr {
                text("＋")
                marginRight(if (compact) 10f else 8f)
                fontSize(if (compact) 18f else 15f)
                color(FOLD_CHAT_BLUE)
            }
        }
        Text {
            attr {
                text("新建任务")
                flex(1f)
                fontSize(if (compact) 16f else 14f)
                fontWeightMedium()
                color(FOLD_CHAT_TEXT)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatDestinationRow(
    page: FoldSplitAiChatDemo,
    dest: FoldChatDestination,
    compact: Boolean,
    showSeparator: Boolean,
    onGlass: Boolean = false,
) {
    View {
        attr {
            height(if (compact) 56f else 42f)
            paddingLeft(if (compact) 14f else 10f)
            paddingRight(if (compact) 14f else 10f)
            borderRadius(if (compact) 0f else 10f)
            flexDirectionRow()
            alignItemsCenter()
                backgroundColor(
                    when {
                        compact -> FOLD_CHAT_CARD
                        page.selectedDestId == dest.id ->
                            if (onGlass) Color(0x1A000000) else FOLD_CHAT_SELECTED
                        else -> Color.TRANSPARENT
                    },
                )
        }
        event {
            click {
                page.openDestination(dest)
            }
        }
        if (compact) {
            View {
                attr {
                    size(28f, 28f)
                    borderRadius(7f)
                    allCenter()
                    backgroundColor(dest.color)
                }
                Text {
                    attr {
                        text(dest.icon)
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }
        } else {
            Text {
                attr {
                    // 这一列的宽度参与 MASTER_TEXT_LEFT 的推导。
                    text(dest.icon)
                    width(28f)
                    fontSize(14f)
                    color(if (onGlass) FOLD_CHAT_TEXT else dest.color)
                }
            }
        }
        Text {
            attr {
                text(dest.title)
                flex(1f)
                marginLeft(if (compact) 10f else 0f)
                fontSize(if (compact) 17f else 14f)
                color(FOLD_CHAT_TEXT)
            }
        }
        if (compact) {
            Text {
                attr {
                    text("›")
                    fontSize(22f)
                    color(FOLD_CHAT_CHEVRON)
                }
            }
        }
        if (showSeparator) {
            foldChatSeparator(52f)
        }
    }
}

/**
 * 会话列表。选中态必须在 `attr {}` 里读 [FoldSplitAiChatDemo.selectedChatId]，
 * 写在函数体里会冻成首帧值，表现为点了会话高亮不换。
 */
private fun ViewContainer<*, *>.foldChatSessionRows(
    page: FoldSplitAiChatDemo,
    compact: Boolean,
    onGlass: Boolean = false,
) {
    vfor({ page.chats }) { session ->
        View {
            attr {
                height(if (compact) 64f else 52f)
                paddingLeft(if (compact) 14f else 10f)
                paddingRight(if (compact) 14f else 10f)
                borderRadius(if (compact) 0f else 12f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(
                    when {
                        compact -> FOLD_CHAT_CARD
                        page.selectedDestId.isEmpty() && page.selectedChatId == session.id ->
                            if (onGlass) Color(0x1A000000) else FOLD_CHAT_SELECTED
                        else -> Color.TRANSPARENT
                    },
                )
            }
            event {
                click {
                    page.openChat(session.id)
                }
            }
            View {
                attr {
                    size(if (compact) 36f else 30f, if (compact) 36f else 30f)
                    borderRadius(if (compact) 18f else 15f)
                    allCenter()
                    backgroundColor(FOLD_CHAT_AVATAR)
                }
                Text {
                    attr {
                        text(session.title.take(1))
                        fontSize(if (compact) 14f else 12f)
                        fontWeightBold()
                        color(Color.WHITE)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    marginLeft(10f)
                    flexDirectionColumn()
                    justifyContentCenter()
                }
                Text {
                    attr {
                        text(session.title)
                        fontSize(if (compact) 16f else 14f)
                        color(FOLD_CHAT_TEXT)
                        lines(1)
                    }
                }
                if (compact) {
                    Text {
                        attr {
                            text(session.summary)
                            marginTop(3f)
                            fontSize(12f)
                            color(FOLD_CHAT_SECONDARY)
                            lines(1)
                        }
                    }
                } else {
                    Text {
                        attr {
                            text(session.summary)
                            marginTop(2f)
                            fontSize(11f)
                            color(FOLD_CHAT_SECONDARY)
                            lines(1)
                        }
                    }
                }
            }
            Text {
                attr {
                    text(session.time)
                    marginLeft(8f)
                    fontSize(11f)
                    color(FOLD_CHAT_FAINT)
                }
            }
            if (compact) {
                foldChatSeparator(14f)
            }
        }
    }
}

// endregion

// region 一级详情：欢迎页 / 当前对话

/**
 * 一级详情。
 *
 * 大屏时被组件钉在 PageList 底下，位移由 [FoldSplitStackController.primaryShift] 驱动；
 * [FoldSplitStackController.coverShadow] 只在被覆盖的滑动过程里大于 0，所以蒙版两端都是全透明。
 */
private fun ViewContainer<*, *>.foldChatPrimaryPage(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            absolutePositionAllZero()
            flexDirectionColumn()
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        foldChatPrimaryHeader(page, controller)
        vif({ page.selectedDestId.isNotEmpty() }) {
            View {
                attr {
                    flex(1f)
                }
                FOLD_CHAT_DESTINATIONS.forEach { dest ->
                    vif({ page.selectedDestId == dest.id }) {
                        foldChatDestinationPane(page, controller, dest)
                    }
                }
            }
        }
        vif({ page.selectedDestId.isEmpty() && page.selectedChatId < 0 }) {
            foldChatWelcomePane(page, controller)
        }
        vif({ page.selectedDestId.isEmpty() && page.selectedChatId >= 0 }) {
            foldChatConversationPane(page, controller)
        }
        vif({ page.selectedDestId.isEmpty() }) {
            foldChatComposer(page, controller)
        }
        vif({ !controller.isCompact() }) {
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

private fun ViewContainer<*, *>.foldChatPrimaryHeader(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            height(52f)
            paddingLeft(foldChatContentLeft(controller))
            paddingRight(foldChatContentRight(controller))
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        vif({ controller.isCompact() }) {
            View {
                attr {
                    height(32f)
                    marginRight(10f)
                    paddingLeft(8f)
                    paddingRight(12f)
                    borderRadius(16f)
                    flexDirectionRow()
                    alignItemsCenter()
                    backgroundColor(FOLD_CHAT_CARD)
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
                        marginRight(2f)
                        fontSize(24f)
                        color(FOLD_CHAT_BLUE)
                    }
                }
                Text {
                    attr {
                        text("任务")
                        fontSize(14f)
                        color(FOLD_CHAT_BLUE)
                    }
                }
            }
        }
        vif({ !controller.isCompact() && controller.masterCollapsed }) {
            View {
                attr {
                    size(32f, 32f)
                    marginRight(10f)
                    allCenter()
                    borderRadius(10f)
                    backgroundColor(FOLD_CHAT_CARD)
                }
                event {
                    click {
                        controller.expandMaster()
                    }
                }
                Text {
                    attr {
                        text("⇥")
                        fontSize(15f)
                        color(FOLD_CHAT_TEXT)
                    }
                }
            }
        }
        Text {
            attr {
                text(
                    FOLD_CHAT_DESTINATIONS.firstOrNull { it.id == page.selectedDestId }?.title
                        ?: if (page.selectedChatId >= 0) {
                            page.conversationTitle
                        } else {
                            FoldSplitAiChatDemo.WELCOME_TITLE
                        },
                )
                flex(1f)
                fontSize(17f)
                fontWeightBold()
                color(FOLD_CHAT_TEXT)
                lines(1)
            }
        }
        vif({ page.selectedDestId.isEmpty() && page.selectedChatId >= 0 }) {
            foldChatHeaderPill("欢迎页") {
                page.backToWelcome()
            }
            foldChatHeaderPill("ⓘ 会话信息") {
                page.openChatInfo()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatHeaderPill(title: String, onClick: () -> Unit) {
    View {
        attr {
            height(28f)
            marginLeft(8f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(14f)
            allCenter()
            backgroundColor(FOLD_CHAT_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(title)
                fontSize(11f)
                color(FOLD_CHAT_SECONDARY)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatWelcomePane(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    List {
        attr {
            flex(1f)
            bouncesEnable(false)
            showScrollerIndicator(false)
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        View {
            attr {
                width(controller.pageWidth())
                paddingLeft(foldChatContentLeft(controller))
                paddingRight(foldChatContentRight(controller))
                paddingTop(if (controller.isCompact()) 24f else 44f)
                paddingBottom(20f)
                flexDirectionColumn()
                alignItemsCenter()
            }
            View {
                attr {
                    size(84f, 84f)
                    marginBottom(16f)
                    borderRadius(28f)
                    allCenter()
                    backgroundColor(FOLD_CHAT_CARD)
                    boxShadow(BoxShadow(0f, 4f, 18f, Color(0x18000000)))
                }
                Text {
                    attr {
                        text("✦")
                        fontSize(38f)
                        color(FOLD_CHAT_BLUE)
                    }
                }
            }
            Text {
                attr {
                    text("${FoldSplitAiChatDemo.WELCOME_TITLE}，我帮你")
                    fontSize(if (controller.isCompact()) 24f else 27f)
                    fontWeightBold()
                    color(FOLD_CHAT_TEXT)
                    textAlignCenter()
                }
            }
            Text {
                attr {
                    text("挑一条开始，或者直接在下面输入。")
                    marginTop(8f)
                    fontSize(13f)
                    color(FOLD_CHAT_SECONDARY)
                    textAlignCenter()
                }
            }
            View {
                attr {
                    marginTop(16f)
                    flexDirectionRow()
                }
                foldChatCapabilityPill("写文档")
                foldChatCapabilityPill("做分析")
                foldChatCapabilityPill("出方案")
            }
            View {
                attr {
                    width(if (controller.isCompact()) 300f else 380f)
                    marginTop(20f)
                    flexDirectionColumn()
                }
                vif({ page.suggestionSet == 0 }) {
                    foldChatSuggestionColumn(page, 0)
                }
                vif({ page.suggestionSet != 0 }) {
                    foldChatSuggestionColumn(page, 1)
                }
            }
            foldChatHeaderPill("↻  换一换") {
                page.suggestionSet = if (page.suggestionSet == 0) 1 else 0
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatSuggestionColumn(
    page: FoldSplitAiChatDemo,
    setIndex: Int,
) {
    FOLD_CHAT_SUGGESTION_SETS[setIndex].forEach { (icon, title) ->
        View {
            attr {
                height(54f)
                marginBottom(10f)
                paddingLeft(14f)
                paddingRight(14f)
                borderRadius(16f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(FOLD_CHAT_CARD)
                boxShadow(BoxShadow(0f, 1f, 8f, Color(0x0C000000)))
            }
            event {
                click {
                    page.startPrompt(title)
                }
            }
            View {
                attr {
                    size(28f, 28f)
                    marginRight(10f)
                    borderRadius(8f)
                    allCenter()
                    backgroundColor(Color(0xFFEAF3FF))
                }
                Text {
                    attr {
                        text(icon)
                        fontSize(14f)
                        color(FOLD_CHAT_BLUE)
                    }
                }
            }
            Text {
                attr {
                    text(title)
                    flex(1f)
                    fontSize(14f)
                    color(FOLD_CHAT_TEXT)
                    lines(1)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatCapabilityPill(title: String) {
    View {
        attr {
            height(26f)
            marginRight(8f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(13f)
            allCenter()
            backgroundColor(Color(0xFFEAF3FF))
        }
        Text {
            attr {
                text(title)
                fontSize(11f)
                fontWeightMedium()
                color(FOLD_CHAT_BLUE)
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatConversationPane(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    List {
        attr {
            flex(1f)
            bouncesEnable(true)
            showScrollerIndicator(false)
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        View {
            attr {
                width(controller.pageWidth())
                paddingLeft(foldChatContentLeft(controller))
                paddingRight(foldChatContentRight(controller))
                paddingTop(8f)
                paddingBottom(16f)
            }
            // vfor 闭包只能有一个顶层子节点，分支要放进这个 View 里做。
            vfor({ page.activeMessages }) { message ->
                View {
                    attr {
                        flexDirectionColumn()
                    }
                    if (message.fromUser) {
                        foldChatUserBubble(message.text)
                    } else {
                        foldChatAssistantBlock(message.text)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatUserBubble(message: String) {
    View {
        attr {
            marginBottom(12f)
            flexDirectionRow()
            justifyContentFlexEnd()
        }
        View {
            attr {
                maxWidth(300f)
                padding(left = 14f, top = 10f, right = 14f, bottom = 10f)
                borderRadius(16f)
                backgroundColor(FOLD_CHAT_BLUE)
            }
            Text {
                attr {
                    text(message)
                    fontSize(15f)
                    lineHeight(22f)
                    color(Color.WHITE)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatAssistantBlock(message: String) {
    View {
        attr {
            marginBottom(16f)
            paddingRight(24f)
            flexDirectionColumn()
        }
        View {
            attr {
                flexDirectionRow()
                alignItemsCenter()
                marginBottom(8f)
            }
            View {
                attr {
                    size(22f, 22f)
                    marginRight(8f)
                    borderRadius(11f)
                    allCenter()
                    backgroundColor(FOLD_CHAT_BLUE)
                }
                Text {
                    attr {
                        text("✦")
                        fontSize(11f)
                        color(Color.WHITE)
                    }
                }
            }
            Text {
                attr {
                    text("云端助手")
                    fontSize(12f)
                    fontWeightMedium()
                    color(FOLD_CHAT_SECONDARY)
                }
            }
        }
        View {
            attr {
                padding(14f)
                borderRadius(16f)
                backgroundColor(FOLD_CHAT_CARD)
            }
            Text {
                attr {
                    text(message)
                    fontSize(15f)
                    lineHeight(23f)
                    color(FOLD_CHAT_BODY)
                }
            }
        }
        View {
            attr {
                marginTop(10f)
                flexDirectionRow()
            }
            listOf("复制", "重试", "分享").forEach { action ->
                View {
                    attr {
                        height(26f)
                        marginRight(8f)
                        paddingLeft(10f)
                        paddingRight(10f)
                        borderRadius(13f)
                        allCenter()
                        backgroundColor(FOLD_CHAT_CARD)
                    }
                    Text {
                        attr {
                            text(action)
                            fontSize(11f)
                            color(FOLD_CHAT_SECONDARY)
                        }
                    }
                }
            }
        }
    }
}

// endregion

// region 输入区

private fun ViewContainer<*, *>.foldChatComposer(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            paddingLeft(foldChatContentLeft(controller))
            paddingRight(foldChatContentRight(controller))
            paddingTop(6f)
            // 组件不管安全区，底部这条由内容自己让。
            paddingBottom(10f + page.safeBottom())
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        View {
            attr {
                height(52f)
                paddingLeft(6f)
                paddingRight(6f)
                borderRadius(26f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(FOLD_CHAT_CARD)
                boxShadow(BoxShadow(0f, 1f, 8f, Color(0x14000000)))
            }
            View {
                attr {
                    size(36f, 36f)
                    allCenter()
                }
                Text {
                    attr {
                        text("＋")
                        fontSize(18f)
                        color(FOLD_CHAT_SECONDARY)
                    }
                }
            }
            View {
                attr {
                    size(36f, 36f)
                    allCenter()
                }
                Text {
                    attr {
                        text("🎙")
                        fontSize(14f)
                        color(FOLD_CHAT_SECONDARY)
                    }
                }
            }
            // 两个分支内容一样，靠 epoch 奇偶换节点来清空输入框。
            vif({ page.composerEpoch % 2 == 0 }) {
                foldChatComposerInput(page)
            }
            vif({ page.composerEpoch % 2 == 1 }) {
                foldChatComposerInput(page)
            }
            View {
                attr {
                    size(36f, 36f)
                    borderRadius(18f)
                    allCenter()
                    backgroundColor(FOLD_CHAT_SEND_BG)
                }
                event {
                    click {
                        page.sendDraft()
                    }
                }
                Text {
                    attr {
                        text("↑")
                        fontSize(16f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatComposerInput(page: FoldSplitAiChatDemo) {
    // 初值只在创建节点时读一次：持续把 draftText 写回 text() 会和 textDidChange 打架，中文输入会被打断。
    val initialDraft = page.draftText
    Input {
        attr {
            flex(1f)
            height(40f)
            fontSize(15f)
            color(FOLD_CHAT_TEXT)
            text(initialDraft)
            placeholder("今天帮你做些什么")
            placeholderColor(FOLD_CHAT_FAINT)
            returnKeyTypeSend()
        }
        event {
            textDidChange {
                page.draftText = it.text
            }
            inputReturn {
                page.sendDraft()
            }
        }
    }
}

// endregion

// region 压上来的页面

/** 目的地作为一级详情，不走 PageList 压栈，避免从会话滑进来。 */
private fun ViewContainer<*, *>.foldChatDestinationPane(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
    dest: FoldChatDestination,
) {
    List {
        attr {
            flex(1f)
            bouncesEnable(false)
            showScrollerIndicator(false)
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        View {
            attr {
                width(controller.pageWidth())
                paddingLeft(foldChatContentLeft(controller))
                paddingRight(foldChatContentRight(controller))
                paddingTop(4f)
                paddingBottom(40f)
            }
            foldChatDestinationBody(page, dest)
        }
    }
}

/** 目的地页内容。 */
private fun ViewContainer<*, *>.foldChatDestinationBody(
    page: FoldSplitAiChatDemo,
    dest: FoldChatDestination,
) {
    foldChatHero(dest.icon, dest.title, dest.hint, dest.color)
    foldChatSectionTitle("推荐")
    foldChatGroup {
        FOLD_CHAT_DESTINATION_ENTRIES.forEachIndexed { index, entry ->
            foldChatNavigationRow(
                title = "${dest.title} · $entry",
                value = "",
                icon = dest.icon,
                iconColor = dest.color,
                showSeparator = index != FOLD_CHAT_DESTINATION_ENTRIES.lastIndex,
            ) {
                page.openResource(dest.id, "${dest.title} · $entry")
            }
        }
    }
    foldChatHint("用它来开一个新任务，或查看模板和最近使用的资料。")
    foldChatSectionTitle("快捷")
    foldChatGroup {
        foldChatNavigationRow("用它开一个新任务", "", "＋", FOLD_CHAT_BLUE, showSeparator = false) {
            page.createTask("用${dest.title}帮我起个头")
        }
    }
}

/** 会话信息页：从一级详情再压一层。 */
private fun ViewContainer<*, *>.foldChatInfoPage(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
    chat: FoldChatSession,
) {
    foldChatPushedPage(controller, "会话信息") {
        foldChatHero("✦", chat.title, chat.summary, FOLD_CHAT_BLUE)
        foldChatSectionTitle("概况")
        foldChatGroup {
            foldChatValueRow("创建时间", chat.time)
            foldChatValueRow("消息条数", "${page.activeMessages.size}")
            foldChatValueRow("模型", "云端助手 Pro", showSeparator = false)
        }
        foldChatSectionTitle("资料")
        foldChatGroup {
            foldChatNavigationRow("引用的资料", "3 项", "▤", FOLD_CHAT_GRAY) {
                page.openResource("chat-${chat.id}", "引用的资料")
            }
            foldChatNavigationRow(
                "运行记录",
                "",
                "⚡",
                FOLD_CHAT_ORANGE,
                showSeparator = false,
            ) {
                page.openResource("chat-${chat.id}", "运行记录")
            }
        }
        foldChatSectionTitle("操作")
        foldChatGroup {
            foldChatNavigationRow("回到对话", "", "‹", FOLD_CHAT_BLUE, showSeparator = false) {
                controller.closeToPrimary()
            }
        }
    }
}

/** 资料页：第三层及以上，验证栈能一直往下开。 */
private fun ViewContainer<*, *>.foldChatResourcePage(
    page: FoldSplitAiChatDemo,
    controller: FoldSplitStackController,
    title: String,
) {
    foldChatPushedPage(controller, title) {
        Text {
            attr {
                text(title)
                fontSize(26f)
                fontWeightBold()
                color(FOLD_CHAT_TEXT)
            }
        }
        Text {
            attr {
                text("来自当前任务的本地资料，可用于核对上下文。")
                marginTop(8f)
                marginBottom(18f)
                fontSize(13f)
                lineHeight(19f)
                color(FOLD_CHAT_SECONDARY)
            }
        }
        foldChatGroup {
            foldChatValueRow("类型", "知识卡片")
            foldChatValueRow("归属任务", page.conversationTitle)
            foldChatValueRow("更新时间", "刚刚", showSeparator = false)
        }
        foldChatSectionTitle("继续")
        foldChatGroup {
            foldChatNavigationRow("再进一层", "", "＋", FOLD_CHAT_GREEN) {
                page.openResource("deep-${controller.depth}", "$title · 更深一层")
            }
            foldChatNavigationRow("返回上一页", "", "‹", FOLD_CHAT_BLUE) {
                controller.close()
            }
            foldChatNavigationRow("回到对话", "", "↺", FOLD_CHAT_RED, showSeparator = false) {
                controller.closeToPrimary()
            }
        }
    }
}

/**
 * 二级及以上页面统一的外壳。
 *
 * 页面自身铺满整屏，靠 [foldChatContentLeft] 让开主窗，所以左右滑的位移是完整一屏。
 */
private fun ViewContainer<*, *>.foldChatPushedPage(
    controller: FoldSplitStackController,
    title: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flex(1f)
            borderRadius(
                if (controller.isCompact()) 0f else FoldSplitAiChatDemo.PAGE_RADIUS,
            )
            overflow(true)
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        foldChatNavigationBar(controller, title)
        List {
            attr {
                flex(1f)
                bouncesEnable(false)
                showScrollerIndicator(false)
                // Android 上 List 默认透明；嵌在 PageList 里会透出底页，必须自己铺底色。
                backgroundColor(FOLD_CHAT_APP_BG)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(foldChatContentLeft(controller))
                    paddingRight(foldChatContentRight(controller))
                    paddingTop(4f)
                    paddingBottom(40f)
                }
                content()
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatNavigationBar(
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
            backgroundColor(FOLD_CHAT_APP_BG)
        }
        Text {
            attr {
                text(title)
                fontSize(17f)
                fontWeightSemiBold()
                color(FOLD_CHAT_TEXT)
                lines(1)
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
                backgroundColor(FOLD_CHAT_CARD)
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
                    color(FOLD_CHAT_BLUE)
                }
            }
            Text {
                attr {
                    text("返回")
                    fontSize(14f)
                    color(FOLD_CHAT_BLUE)
                }
            }
        }
        vif({ controller.masterCollapsed }) {
            View {
                attr {
                    positionAbsolute()
                    right(14f)
                    top(9f)
                    size(34f, 34f)
                    allCenter()
                    borderRadius(10f)
                    backgroundColor(FOLD_CHAT_CARD)
                }
                event {
                    click {
                        controller.expandMaster()
                    }
                }
                Text {
                    attr {
                        text("⇥")
                        fontSize(15f)
                        color(FOLD_CHAT_TEXT)
                    }
                }
            }
        }
    }
}

// endregion

// region 通用视觉

private val FOLD_CHAT_DESTINATION_ENTRIES = listOf("入门", "模板", "最近使用")

private fun ViewContainer<*, *>.foldChatHero(
    icon: String,
    title: String,
    summary: String,
    accent: Color,
) {
    View {
        attr {
            marginTop(8f)
            padding(18f)
            borderRadius(18f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_CHAT_CARD)
            boxShadow(BoxShadow(0f, 1f, 8f, Color(0x0F000000)))
        }
        View {
            attr {
                size(54f, 54f)
                borderRadius(13f)
                allCenter()
                backgroundColor(accent)
            }
            Text {
                attr {
                    text(icon)
                    fontSize(26f)
                    color(Color.WHITE)
                }
            }
        }
        View {
            attr {
                flex(1f)
                marginLeft(14f)
            }
            Text {
                attr {
                    text(title)
                    fontSize(20f)
                    fontWeightBold()
                    color(FOLD_CHAT_TEXT)
                    lines(1)
                }
            }
            Text {
                attr {
                    text(summary)
                    marginTop(5f)
                    fontSize(13f)
                    lineHeight(18f)
                    color(FOLD_CHAT_SECONDARY)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.foldChatSectionTitle(title: String) {
    Text {
        attr {
            text(title)
            margin(left = 16f, top = 22f, bottom = 8f)
            fontSize(13f)
            fontWeightMedium()
            color(FOLD_CHAT_SECONDARY)
        }
    }
}

private fun ViewContainer<*, *>.foldChatGroup(content: ViewContainer<*, *>.() -> Unit) {
    View {
        attr {
            borderRadius(12f)
            overflow(true)
            backgroundColor(FOLD_CHAT_CARD)
        }
        content()
    }
}

private fun ViewContainer<*, *>.foldChatNavigationRow(
    title: String,
    value: String,
    icon: String,
    iconColor: Color,
    showSeparator: Boolean = true,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(48f)
            paddingLeft(12f)
            paddingRight(13f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_CHAT_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        View {
            attr {
                size(28f, 28f)
                borderRadius(7f)
                allCenter()
                backgroundColor(iconColor)
            }
            Text {
                attr {
                    text(icon)
                    fontSize(13f)
                    color(Color.WHITE)
                }
            }
        }
        Text {
            attr {
                text(title)
                flex(1f)
                marginLeft(10f)
                fontSize(16f)
                color(FOLD_CHAT_TEXT)
                lines(1)
            }
        }
        if (value.isNotEmpty()) {
            Text {
                attr {
                    text(value)
                    fontSize(14f)
                    color(FOLD_CHAT_SECONDARY)
                }
            }
        }
        Text {
            attr {
                text("›")
                marginLeft(7f)
                fontSize(22f)
                color(FOLD_CHAT_CHEVRON)
            }
        }
        if (showSeparator) {
            foldChatSeparator(50f)
        }
    }
}

private fun ViewContainer<*, *>.foldChatValueRow(
    title: String,
    value: String,
    showSeparator: Boolean = true,
) {
    View {
        attr {
            height(48f)
            paddingLeft(16f)
            paddingRight(16f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FOLD_CHAT_CARD)
        }
        Text {
            attr {
                text(title)
                fontSize(16f)
                color(FOLD_CHAT_TEXT)
            }
        }
        Text {
            attr {
                text(value)
                flex(1f)
                marginLeft(12f)
                fontSize(14f)
                textAlignRight()
                color(FOLD_CHAT_SECONDARY)
                lines(1)
            }
        }
        if (showSeparator) {
            foldChatSeparator(16f)
        }
    }
}

private fun ViewContainer<*, *>.foldChatHint(message: String) {
    Text {
        attr {
            text(message)
            margin(left = 16f, top = 10f, right = 16f)
            fontSize(13f)
            lineHeight(18f)
            color(FOLD_CHAT_SECONDARY)
        }
    }
}

private fun ViewContainer<*, *>.foldChatSeparator(left: Float) {
    View {
        attr {
            positionAbsolute()
            this.left(left)
            right(0f)
            bottom(0f)
            height(0.33f)
            backgroundColor(FOLD_CHAT_SEPARATOR)
        }
    }
}

// endregion

private val FOLD_CHAT_APP_BG = Color(0xFFF2F2F7)
private val FOLD_CHAT_CARD = Color.WHITE
private val FOLD_CHAT_SELECTED = Color(0xFFE4E4EA)
private val FOLD_CHAT_TEXT = Color(0xFF1C1C1E)
private val FOLD_CHAT_BODY = Color(0xFF3A3A3C)
private val FOLD_CHAT_SECONDARY = Color(0xFF7C7C80)
private val FOLD_CHAT_FAINT = Color(0xFFAEAEB2)
private val FOLD_CHAT_CHEVRON = Color(0xFFC3C3C8)
private val FOLD_CHAT_SEPARATOR = Color(0xFFE4E4E8)
private val FOLD_CHAT_DIVIDER = Color(0xFFD8D8DE)
private val FOLD_CHAT_BUBBLE = Color(0xFFEFEFF4)
private val FOLD_CHAT_SEND_BG = Color(0xFF3A3A3C)
private val FOLD_CHAT_AVATAR = Color(0xFF5E5CE6)
private val FOLD_CHAT_BLUE = Color(0xFF0A84FF)
private val FOLD_CHAT_GREEN = Color(0xFF30D158)
private val FOLD_CHAT_RED = Color(0xFFFF3B30)
private val FOLD_CHAT_ORANGE = Color(0xFFFF9F0A)
private val FOLD_CHAT_GRAY = Color(0xFF8E8E93)
