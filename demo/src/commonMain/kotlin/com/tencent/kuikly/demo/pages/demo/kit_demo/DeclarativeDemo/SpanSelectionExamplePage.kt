package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.Utils
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Span Selection Example Page
 *
 * 验证 [SelectionType.SPAN] 长按选择能力：
 * - 富文本中每个 Span 节点都是一个独立的可选单元
 * - 长按某个 Span 会自动选中整个 Span 的文字范围
 * - 也可通过按钮程序化触发不同位置的 Span 选择
 */
@Page("SpanSelectionExamplePage")
internal class SpanSelectionExamplePage : BasePager() {

    private var selectedText by observable("")
    private var selectionStatus by observable("未选择")
    private var selectionFrame by observable("")
    private var clickInfo by observable("")
    private var selectionType by observable(SelectionType.SPAN)

    private var selectableContainer: ViewRef<DivView>? = null
    private var clickableRichTextRef: ViewRef<RichTextView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr { title = "Span Selection Demo" }
            }

            List {
                attr {
                    flex(1f)
                    padding(all = 16f)
                }

                // Status display
                View {
                    attr {
                        backgroundColor(Color(0xFFF5F5F5))
                        padding(all = 12f)
                        borderRadius(8f)
                        marginBottom(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color(0xFF666666))
                            text("状态: ${ctx.selectionStatus}")
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF999999))
                            marginTop(4f)
                            text("选区: ${ctx.selectionFrame}")
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(4f)
                            text("选中内容: ${ctx.selectedText}")
                            lines(3)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF00897B))
                            marginTop(4f)
                            text("点击反馈: ${ctx.clickInfo}")
                            lines(2)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF7B1FA2))
                            marginTop(4f)
                            text("当前选择类型: ${ctx.selectionTypeName()}")
                        }
                    }
                }

                // Control buttons
                View {
                    attr {
                        flexDirectionRow()
                        marginBottom(16f)
                    }
                    // 切换选择类型：CHARACTER → WORD → SENTENCE → SPAN → CHARACTER ...
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFF009688))
                            padding(top = 12f, bottom = 12f)
                            borderRadius(6f)
                            marginRight(8f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectionType = when (ctx.selectionType) {
                                    SelectionType.CHARACTER -> SelectionType.WORD
                                    SelectionType.WORD -> SelectionType.SENTENCE
                                    SelectionType.SENTENCE -> SelectionType.PARAGRAPH
                                    SelectionType.PARAGRAPH -> SelectionType.SPAN
                                    SelectionType.SPAN -> SelectionType.CHARACTER
                                }
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("选择类型: ${ctx.selectionTypeName()}")
                            }
                        }
                    }
                    // 清除选择
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFFE57373))
                            padding(top = 12f, bottom = 12f)
                            borderRadius(6f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectableContainer?.view?.clearSelection()
                                ctx.selectedText = ""
                                ctx.selectionFrame = ""
                                ctx.selectionStatus = "已清除"
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("清除选择")
                            }
                        }
                    }
                }

                // Selectable container with rich text spans
                View {
                    ref {
                        ctx.selectableContainer = it
                    }
                    attr {
                        backgroundColor(Color(0xFFFFFDE7))
                        padding(all = 16f)
                        borderRadius(8f)
                        border(Border(1f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                        selectable(SelectableOption.ENABLE)
                        selectionColor(Color(0xFF2196F3))
                    }
                    event {
                        longPress {
                            if (it.state == "start") {
                                ctx.selectableContainer?.view?.createSelection(it.x, it.y, ctx.selectionType)
                            }
                        }
                        selectStart { frame ->
                            ctx.selectionStatus = "选择开始"
                            ctx.selectionFrame = "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
                        }
                        selectChange { frame ->
                            ctx.selectionStatus = "选择中..."
                            ctx.selectionFrame = "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
                        }
                        selectEnd { frame ->
                            ctx.selectionStatus = "选择结束"
                            ctx.selectionFrame = "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
                            ctx.selectableContainer?.view?.getSelection { texts ->
                                ctx.selectedText = texts.joinToString(" | ")
                                if (texts.isEmpty()) {
                                    ctx.selectionStatus = "无选中内容"
                                }
                            }
                        }
                        selectCancel {
                            ctx.selectionStatus = "选择取消"
                            ctx.selectionFrame = ""
                            ctx.selectedText = ""
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("长按以下 Span 试试")
                            marginBottom(12f)
                        }
                    }

                    // Section 1: 带点击的 Span（验证 click 与 SPAN 长按选择共存）
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF888888))
                            text("① 带 click 的 Span（点击弹 toast，长按选中整段）")
                            marginBottom(6f)
                        }
                    }
                    RichText {
                        ref { ctx.clickableRichTextRef = it }
                        attr { marginBottom(16f) }
                        event {
                            longPress {
                                if (it.state == "start") {
                                    ctx.triggerSpanSelection(it.x, it.y)
                                }
                            }
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text("点击 "
                            )
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF1565C0))
                            fontWeightBold()
                            textDecorationUnderLine()
                            text("这段蓝色可点击文字")
                            click {
                                ctx.clickInfo = "点击了「蓝色可点击文字」"
                                Utils.bridgeModule(this).toast("点击了蓝色 Span")
                            }
                            longPress {
                                if (it.state == "start") {
                                    ctx.triggerSpanSelection(it.x, it.y)
                                }
                            }
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text(" 会有 toast 反馈，长按则会选中整个 Span。"
                            )
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFFC62828))
                            fontWeightBold()
                            text("红色可点击Span")
                            click {
                                ctx.clickInfo = "点击了「红色可点击Span」"
                                Utils.bridgeModule(this).toast("点击了红色 Span")
                            }
                            longPress {
                                if (it.state == "start") {
                                    ctx.triggerSpanSelection(it.x, it.y)
                                }
                            }
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text(" 也试试。")
                        }
                    }

                    // Section 2: 基础多 Span（不同颜色样式，验证逐个选中）
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF888888))
                            text("② 多样式 Span（长按每个 Span 独立选中）")
                            marginBottom(6f)
                        }
                    }
                    RichText {
                        attr { marginBottom(16f) }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text("KuiklyUI 是一个")
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFFE53935))
                            fontWeightBold()
                            text("跨平台UI框架")
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text("，支持")
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF1E88E5))
                            textDecorationUnderLine()
                            text("iOS、Android、鸿蒙")
                        }
                        Span {
                            fontSize(16f)
                            color(Color(0xFF444444))
                            text("三端。")
                        }
                    }

                    // Section 3: 长 Span（单个 Span 内容很长，验证长文本选区完整性）
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF888888))
                            text("③ 长 Span（单个 Span 很长，验证选区不截断）")
                            marginBottom(6f)
                        }
                    }
                    RichText {
                        attr { marginBottom(16f) }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF6A1B9A))
                            fontWeightBold()
                            text("这是一个很长的 Span：SPAN 选择类型允许用户通过长按手势一次性选中整个富文本 Span 的内容范围，而不需要手动拖拽游标。这在词条解释、链接预览、标签选择等场景下非常实用，可以大幅提升交互效率。长按此段文字应一次性选中全部内容。")
                        }
                    }

                    // Section 4: 跨行 Span（Span 跨越多行，验证 P1/P2 边界修复）
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF888888))
                            text("④ 跨行 Span（验证多行选区不遗漏末字符、不多选相邻字符）")
                            marginBottom(6f)
                        }
                    }
                    RichText {
                        attr { marginBottom(16f) }
                        Span {
                            fontSize(14f)
                            color(Color(0xFF555555))
                            text("前缀文字 | ")
                        }
                        Span {
                            fontSize(14f)
                            color(Color(0xFFD81B60))
                            fontWeightBold()
                            text("这是跨行 Span 的开始，这段文字会跨越多行显示。当文字内容超过一行宽度时会自动换行，长按此 Span 应选中它在所有行中的完整范围。关键验证点：选区末尾不应遗漏最后一个字符，也不应多选相邻 Span 的第一个字符。这是 SPAN 选择边界正确性的核心验证场景。跨行 Span 结束。")
                        }
                        Span {
                            fontSize(14f)
                            color(Color(0xFF555555))
                            text(" | 后缀文字")
                        }
                    }

                    // Section 5: 英文 Span + ImageSpan + emoji
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF888888))
                            text("⑤ 英文 / ImageSpan / emoji")
                            marginBottom(6f)
                        }
                    }
                    RichText {
                        attr { marginBottom(16f) }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("SPAN selection lets you "
                            )
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF2E7D32))
                            fontWeightBold()
                            text("select an entire styled span"
                            )
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text(" with a single long-press. 富文本也可含 ")
                        }
                        ImageSpan {
                            src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            size(24f, 24f)
                            description("logo")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFFEF6C00))
                            fontWeightBold()
                            text(" emoji 🌙⭐🌙⭐")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("。")
                        }
                    }
                }

                // Tips
                View {
                    attr {
                        backgroundColor(Color(0xFFE3F2FD))
                        padding(all = 12f)
                        borderRadius(8f)
                        marginTop(16f)
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightMedium()
                            color(Color(0xFF1565C0))
                            text("使用说明")
                            marginBottom(8f)
                        }
                    }
                    Text {
                        attr {
                            fontSize(13f)
                            color(Color(0xFF1976D2))
                            lineHeight(20f)
                            text("1. 点击「选择类型」在 字符/WORD/句子/SPAN 间切换\n2. 切换后长按文本按当前类型选择\n3. ① 带 click 的 Span：点击弹 toast，长按选中整段\n4. ③ 长 Span：验证长文本选区不截断\n5. ④ 跨行 Span：验证多行选区不遗漏末字符、不多选相邻字符")
                        }
                    }
                }
            }
        }
    }

    private fun selectionTypeName(): String {
        return when (selectionType) {
            SelectionType.CHARACTER -> "字符"
            SelectionType.WORD -> "WORD"
            SelectionType.SENTENCE -> "句子"
            SelectionType.PARAGRAPH -> "段落"
            SelectionType.SPAN -> "SPAN"
        }
    }

    /**
     * Span longPress 的 it.x/it.y 是相对于 RichText 视图的坐标，
     * 而 createSelection 需要相对于 DivView（selectableContainer）的坐标。
     * Kuikly flex 布局的 frame.x/y 已包含父视图 padding（位置从 padding 边开始累加），
     * 因此直接用 frame 作为偏移即可，无需额外加 padding。
     */
    private fun triggerSpanSelection(spanX: Float, spanY: Float) {
        val richTextFrame = clickableRichTextRef?.view?.frame
        val offsetX = richTextFrame?.x ?: 0f
        val offsetY = richTextFrame?.y ?: 0f
        selectableContainer?.view?.createSelection(
            spanX + offsetX, spanY + offsetY, selectionType
        )
    }
}
