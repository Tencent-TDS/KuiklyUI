package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Text Selection Example Page
 * Demonstrates the text selection feature for multi-text components
 */
@Page("TextSelectionExamplePage")
internal class TextSelectionExamplePage : BasePager() {

    // State for selected text display
    private var selectedText by observable("")
    private var preSelectedText by observable("")
    private var postSelectedText by observable("")
    private var selectionFrame by observable("")
    private var selectionStatus by observable("未选择")
    private var selectionType by observable(SelectionType.CHARACTER)

    // Reference to the selectable container
    private var selectableContainer: ViewRef<DivView>? = null

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr { title = "Text Selection Demo" }
            }

            // Main content area
            List {
                attr {
                    flex(1f)
                    padding(all = 16f)
                }

                // Section: Status Display
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
                            color(Color(0xFF999999))
                            marginTop(4f)
                            text("选中前内容: ${ctx.preSelectedText}")
                            lines(1)
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
                            color(Color(0xFF999999))
                            marginTop(4f)
                            text("选中后内容: ${ctx.postSelectedText}")
                            lines(1)
                        }
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF333333))
                            marginTop(4f)
                            text("长按类型: ${ctx.selectionType}")
                            lines(3)
                        }
                    }
                }

                // Section: Control Buttons Row 1

                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceBetween()
                        marginBottom(8f)
                    }
                    // Select Char Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFF009688))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            marginRight(8f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectionType = SelectionType.CHARACTER
                                // Trigger char selection at center of container
                                ctx.selectableContainer?.view?.createSelection(150f, 80f, ctx.selectionType)
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("选择字符")
                            }
                        }
                    }

                    // Select Word Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFF4A90D9))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            marginRight(8f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectionType = SelectionType.WORD
                                // Trigger word selection at center of container
                                ctx.selectableContainer?.view?.createSelection(150f, 80f, ctx.selectionType)
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("选择单词")
                            }
                        }
                    }

                    // Select Sentence Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFFFF9800))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            marginRight(8f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectionType = SelectionType.SENTENCE
                                ctx.selectableContainer?.view?.createSelection(150f, 80f, ctx.selectionType)
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("选择句子")
                            }
                        }
                    }

                    // Select Paragraph Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFF5AAF6A))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectionType = SelectionType.PARAGRAPH
                                ctx.selectableContainer?.view?.createSelection(150f, 80f, ctx.selectionType)
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("选择段落")
                            }
                        }
                    }
                }

                // Section: Control Buttons Row 2
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceBetween()
                        marginBottom(16f)
                    }

                    // Select All Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFF673AB7))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            marginRight(8f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectableContainer?.view?.createSelectionAll()
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                text("全选")
                            }
                        }
                    }

                    // Clear Selection Button
                    View {
                        attr {
                            flex(1f)
                            backgroundColor(Color(0xFFE57373))
                            padding(top = 10f, bottom = 10f)
                            borderRadius(6f)
                            alignItemsCenter()
                        }
                        event {
                            click {
                                ctx.selectableContainer?.view?.clearSelection()
                                ctx.selectedText = ""
                                ctx.preSelectedText = ""
                                ctx.postSelectedText = ""
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

                // Section: Get Selection Button
                View {
                    attr {
                        backgroundColor(Color(0xFF9C27B0))
                        padding(top = 12f, bottom = 12f)
                        borderRadius(6f)
                        marginBottom(16f)
                        alignItemsCenter()
                    }
                    event {
                        click {
                            ctx.selectableContainer?.view?.getSelection { texts ->
                                ctx.selectedText = texts.joinToString(" | ")
                                ctx.preSelectedText = texts.preContent.joinToString(" | ")
                                ctx.postSelectedText = texts.postContent.joinToString(" | ")
                                if (texts.isEmpty()) {
                                    ctx.selectionStatus = "无选中内容"
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color.WHITE)
                            text("获取选中内容")
                        }
                    }
                }

                // Section: Selectable Text Container
                View {
                    ref {
                        ctx.selectableContainer = it
                    }
                    attr {
                        backgroundColor(Color(0xFFFFFDE7))
                        padding(all = 16f)
                        borderRadius(8f)
                        border(Border(1f, BorderStyle.SOLID, Color(0xFFE0E0E0)))
                        // Enable text selection on this container
                        selectable(SelectableOption.ENABLE)
                        // Set custom selection color (blue with transparency)
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
                            KLog.i("TextSelection", "Selection started: $frame")
                        }
                        selectChange { frame ->
                            ctx.selectionStatus = "选择中..."
                            ctx.selectionFrame = "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
                        }
                        selectEnd { frame ->
                            ctx.selectionStatus = "选择结束"
                            ctx.selectionFrame = "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
                            KLog.i("TextSelection", "Selection ended: $frame")
                        }
                        selectCancel {
                            ctx.selectionStatus = "选择取消"
                            ctx.selectionFrame = ""
                            ctx.selectedText = ""
                            ctx.preSelectedText = ""
                            ctx.postSelectedText = ""
                            KLog.i("TextSelection", "Selection cancelled")
                        }
                    }

                    // Title
                    Text {
                        attr {
                            fontSize(18f)
                            fontWeightBold()
                            color(Color(0xFF333333))
                            text("可选择文本区域")
                            marginBottom(12f)
                        }
                    }

                    // Paragraph 1
                    Text {
                        attr {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            lineHeight(22f)
                            text("Kuikly是一个跨平台的UI框架，支持iOS、Android和Web平台。它使用Kotlin Multiplatform技术，让开发者可以使用统一的代码库来构建多平台应用。\nKuikly是一个跨平台的UI框架，支持iOS、Android和Web平台。它使用Kotlin Multiplatform技术，让开发者可以使用统一的代码库来构建多平台应用。\n")
                            marginBottom(12f)
                        }
                    }

                    // Paragraph 2
                    Text {
                        attr {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            lineHeight(22f)
                            text("文本选择功能允许用户在多个文本组件之间进行连续选择，支持单词、段落等多种选择模式。选中的文本可以被复制或进行其他操作。")
                            marginBottom(12f)
                        }
                    }

                    // Paragraph 3 with RichText
                    RichText {
                        attr {
                            marginBottom(12f)
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("这是一段")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFFE53935))
                            fontWeightBold()
                            text("富文本内容")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("，包含")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF1E88E5))
                            textDecorationUnderLine()
                            text("不同样式")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("的文字，同样支持选择。")
                        }
                    }

                    // ImageSpan and Emoji
                    RichText {
                        attr {
                            marginBottom(12f)
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text("Here is some ")
                        }
                        ImageSpan {
                            src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            size(30f, 30f)
                            description("kuikly logo")
                        }
                        ImageSpan {
                            src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                            size(30f, 30f)
                            description("[logo.png]")
                        }
                        Span {
                            fontSize(15f)
                            color(Color(0xFF444444))
                            text(" icons within text.")
                        }
                        Span {
                            text("\uD83C\uDF1A\uD83C\uDF1D\uD83C\uDF1A\uD83C\uDF1D\uD83C\uDF1A")
                        }
                    }

                    // English text
                    Text {
                        attr {
                            fontSize(14f)
                            color(Color(0xFF666666))
                            lineHeight(20f)
                            fontStyleItalic()
                            text("The quick brown fox jumps over the lazy dog. This is a classic pangram that contains every letter of the English alphabet.")
                        }
                    }

                    // punctuation text
                    Text {
                        attr {
                            text("""
                                |凌晨两点的便利店里，自动感应门发出的“叮咚”声在寂静的街道上显得格外清脆。林逸拖着沉重的步子走进来，双眼布满了熬夜后的血丝，他的目标很明确——冷藏柜里那罐能让他撑过最后几小时加班的冰镇黑咖啡。
                                |“又是这个点啊，林哥。今天还是老规矩，‘续命大礼包’？”柜台后的店员小张一边整理着手中的报刊，一边熟络地打招呼。
                                |林逸苦笑着把咖啡和一份三明治放到收银台上，无奈地揉了揉眉心，“差不多吧，项目到了收尾阶段，现在我闭上眼，脑子里转的全是跳动的分号。”
                                |小张利落地扫码、装袋，动作轻快。他看了一眼窗外黑沉沉的天色，压低声音提醒道：“悠着点儿吧，气象预报说再过一小时有雨。你一会儿回家路上注意安全，别淋着了。”
                                |林逸掏出手机支付，听到这话，神色稍微柔和了一些。他忽然想起什么，随口问道：“对了，刚才在门口看到只橘猫，是你平时喂的那只吗？”
                                |“喔，那是‘大黄’。”提到猫，小张笑得更开了，“它精着呢，一到凌晨就准时来收‘保护费’。你要是没急事，可以去喂它两块火腿肠，它这会儿心情好，准能让你摸个够。”
                                |林逸拎起塑料袋，听着塑料纸摩擦出的轻微声响，心情竟奇迹般地放松了一点。他自嘲地笑了笑，轻声说道：“呵，那我可得试试。正好，现在确实需要点除了屏幕以外的‘治愈’。”
                                |他向小张挥了挥手，转身推开那扇玻璃门。随着身后再次响起的一声“慢走”，林逸的身影融入了便利店外昏黄的灯光与微凉的夜色中。
                            """.trimMargin())
                        }
                    }
                }

                // Section: Usage Tips
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
                            text("1. 点击上方按钮可以程序化创建选区（单词/句子/段落/全选）\n2. 拖动选区两端的游标可以调整选区范围\n3. 点击\"获取选中内容\"可以获取当前选中的文本\n4. 点击\"清除选择\"可以取消当前选区")
                        }
                    }
                }
            }
        }
    }
}

