package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.MeasureOutput
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.InputSpan
import com.tencent.kuikly.core.views.InputSpans
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaAttr
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.TextView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max

@Page("input_measure")
internal class InputMeasureDemoPage : BasePager() {
    private var imeHeight by observable(0f)
    private var inputText by observable("")
    private var showEmojiPanel by observable(false)
    private lateinit var inputRef: ViewRef<TextAreaView>
    private var panelHeight = 100f

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "Input Measure Demo"
                    backDisable = false
                }
            }
            View {
                attr {
                    backgroundColor(Color(0xFFEEEEEE))
                    flex(1f)
                }
                List {
                    attr {
                        flex(1f)
                    }
                    LineHeightDemo()
                    ViewLayoutDemo()
                    TextLayoutDemo()
                    InputLayoutDemo()
                }
            }
            /*View {
                attr {
                    padding(left = 10f, right = 10f, bottom = 10f)
                    backgroundColor(Color(0xFFEEEEEE))
                    boxShadow(BoxShadow(0f, 0f, 5f, Color.GRAY))
                    flexDirectionRow()
                    flexWrapWrap()
                    justifyContentFlexEnd()
                    alignItemsFlexEnd()
                }
                View {
                    attr {
                        marginTop(10f)
                        padding(5f)
                        borderRadius(5f)
                        border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                        backgroundColor(Color.WHITE)
                    }
                    CustomText {
                        attr {
                            backgroundColor(Color.GRAY)
                            fontSize(16f)
                            color(Color.BLACK)
                            minWidth(ctx.pageData.pageViewWidth - 30f - 100f)
                            maxWidth(ctx.pageData.pageViewWidth - 30f)
                            minHeight(24f)
                            lineHeight(24f)
                            text(ctx.inputText)
                        }
                    }
                }
                View {
                    attr {
                        size(30f, 30f)
                        backgroundColor(Color.GRAY)
                        margin(left = 5f, bottom = 2f, top = 10f)
                    }
                }
                View {
                    attr {
                        size(60f, 30f)
                        backgroundColor(Color.GRAY)
                        margin(left = 5f, bottom = 2f, top = 10f)
                    }
                }
            }*/
            View {
                attr {
                    padding(left = 10f, right = 10f, bottom = 10f)
                    backgroundColor(Color(0xFFEEEEEE))
                    boxShadow(BoxShadow(0f, 0f, 5f, Color.GRAY))
                    flexDirectionRow()
                    flexWrapWrap()
                    justifyContentFlexEnd()
                    alignItemsFlexEnd()
                }
                // 自定义输入框
                View {
                    attr {
                        marginTop(10f)
                        padding(5f)
                        borderRadius(5f)
                        border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                        backgroundColor(Color.WHITE)
                    }
                    CustomTextArea {
                        ref { ctx.inputRef = it }
                        attr {
                            placeholder("请输入内容")
                            fontSize(16f)
                            color(Color.BLACK)
                            minWidth(ctx.pageData.pageViewWidth - 30f - 100f)
                            maxWidth(ctx.pageData.pageViewWidth - 30f)
                            minHeight(24f)
                            lineHeight(24f)
                        }
                        event {
                            keyboardHeightChange {
                                ctx.imeHeight = it.height
                                if (it.height != 0f) {
                                    ctx.panelHeight = it.height
                                    ctx.showEmojiPanel = false
                                }
                            }
                            textDidChange {
                                ctx.inputText = it.text
                            }
                        }
                    }
                }
                // 加号按钮
                View {
                    attr {
                        size(30f, 30f)
                        borderRadius(25f)
                        border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                        allCenter()
                        margin(left = 5f, bottom = 2f, top = 10f)
                    }
                    event {
                        click {
                            ctx.showEmojiPanel = !ctx.showEmojiPanel
                            ctx.inputRef.view?.blur()
                        }
                    }
                    Text {
                        attr {
                            text("+")
                        }
                    }
                }
                // 发送按钮
                Button {
                    attr {
                        size(60f, 30f)
                        borderRadius(20f)
                        margin(left = 5f, bottom = 2f, top = 10f)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color(0xAA23D3FD), 0f),
                            ColorStop(Color(0xAAAD37FE), 1f)
                        )
                        titleAttr {
                            text("发送")
                            fontSize(17f)
                            color(Color.WHITE)
                        }
                        touchEnable(ctx.inputText.isNotEmpty())
                    }
                    event {
                        click {
                            ctx.send()
                        }
                    }
                }
            }
            // emoji面板
            vif({ ctx.imeHeight == 0f && ctx.showEmojiPanel }) {
                View {
                    attr {
                        height(ctx.panelHeight)
                        backgroundColor(Color(0xFFCCCCCC))
                        border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                        alignItemsCenter()
                    }
                    Text {
                        attr {
                            text("Emoji Panel")
                        }
                    }
                }
            }
            View {
                attr {
                    height(max(ctx.imeHeight, ctx.pageData.safeAreaInsets.bottom))
                }
            }
        }
    }

    private fun send() {
        inputRef.view?.also {
            it.setText("")
            it.blur()
            showEmojiPanel = false
        }
    }
}

private fun ViewContainer<*, *>.LineHeightDemo() {
    Text {
        attr {
            text("lineHeight demo")
        }
    }
    // 单行文本
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text("single line editor")
                height(32f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text("single line text")
                height(32f)
                flex(1f)
            }
        }
    }
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text("单行输入框")
                height(32f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text("单行文本")
                height(32f)
                flex(1f)
            }
        }
    }
    // 多行文本
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |multi line editor
                                    |multi line editor
                                    |multi line editor""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |multi line text
                                    |multi line text
                                    |multi line text""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
    }
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |多行输入框
                                    |多行输入框
                                    |多行输入框""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |多行文本
                                    |多行文本
                                    |多行文本""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
    }
    // 混合文本
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |multi line editor
                                    |多行输入框
                                    |😂😂😂""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(32f)
                text(
                    """
                                    |multi line text
                                    |多行文本
                                    |😂😂😂""".trimMargin()
                )
                height(96f)
                flex(1f)
            }
        }
    }
    // 输入和粘贴
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                placeholder("empty to input / paste")
                height(72f)
                flex(1f)
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                text(
                    """
                                    |multi line text
                                    |多行文本
                                    |😂😂😂""".trimMargin()
                )
                height(72f)
                flex(1f)
            }
        }
    }
    // 富文本
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(12f)
                lineHeight(24f)
                placeholder("empty to input / paste")
                height(200f)
                flex(1f)
                inputSpans(InputSpans().apply {
                    addSpan(InputSpan().apply {
                        text("default size default lineHeight\n")
                        color(Color.RED)
                    })
                    addSpan(InputSpan().apply {
                        text("big size default lineHeight\n")
                        color(Color.BLUE)
                        fontSize(30f)
                    })
                    addSpan(InputSpan().apply {
                        text("default size big lineHeight\n")
                        lineHeight(36f)
                    })
                    addSpan(InputSpan().apply {
                        text("default size small lineHeight\n")
                        lineHeight(16f)
                    })
                })
            }
        }
        RichText {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(12f)
                lineHeight(24f)
                height(200f)
                flex(1f)
            }
            Span {
                text("default size default lineHeight\n")
                color(Color.RED)
            }
            Span {
                text("big size default lineHeight\n")
                color(Color.BLUE)
                fontSize(30f)
            }
            Span {
                text("default size big lineHeight\n")
                lineHeight(36f)
            }
            Span {
                text("default size small lineHeight\n")
                lineHeight(16f)
            }
        }
    }
    // 粗体
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightBold()
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightBold()
            }
        }
    }
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightMedium()
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightMedium()
            }
        }
    }
    View {
        attr {
            flexDirectionRow()
        }
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightNormal()
            }
        }
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.GRAY))
                fontSize(16f)
                lineHeight(24f)
                height(72f)
                flex(1f)
                text("WWWWWWWWWWWWW fox jumps over the lazy dog 狐狸跳过了懒狗")
                fontWeightNormal()
            }
        }
    }
}

private fun ViewContainer<*, *>.ViewLayoutDemo() {
    Text {
        attr {
            text("View layout demo")
        }
    }
    // less than minHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
        }
        // less than minWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(30f, 20f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // between minWidth and maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(80f, 20f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // greater than maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(120f, 20f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
    }
    // between minHeight and maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
        }
        // less than minWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(30f, 40f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // between minWidth and maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(80f, 40f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // greater than maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(120f, 40f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
    }
    // greater than maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
        }
        // less than minWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(30f, 60f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // between minWidth and maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(80f, 60f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
        // greater than maxWidth
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
            }
            View {
                attr {
                    size(120f, 60f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
        }
    }
    // wrap
    View {
        attr {
            marginTop(20f)
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
        }
        // static width
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                width(100f)
                flexDirectionRow()
                flexWrapWrap()
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x9900FF00))
                }
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x990000FF))
                }
            }
        }
        // dynamic width
        View {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f)
                maxWidth(100f)
                flexDirectionRow()
                flexWrapWrap()
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x99FF0000))
                }
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x9900FF00))
                }
            }
            View {
                attr {
                    size(40f, 20f)
                    backgroundColor(Color(0x990000FF))
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.TextLayoutDemo() {
    Text {
        attr {
            text("Text layout demo")
        }
    }
    // less than minHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙")
            }
        }
        // between minWidth and maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁")
            }
        }
        // greater than maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
    }
    // between minHeight and maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(50f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲")
            }
        }
        // between minWidth and maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲乙")
            }
        }
        // greater than maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲乙丙")
            }
        }
    }
    // greater than maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(80f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("A")
            }
        }
        // between minWidth and maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("甲")
            }
        }
        // greater than maxWidth
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("甲乙")
            }
        }
    }
    // wrap
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // static width
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                width(100f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
        // dynamic width
        Text {
            attr {
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f)
                maxWidth(100f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
    }
}

private fun ViewContainer<*, *>.InputLayoutDemo() {
    Text {
        attr {
            text("Editor layout demo")
        }
    }
    // less than minHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙")
            }
        }
        // between minWidth and maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁")
            }
        }
        // greater than maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
    }
    // between minHeight and maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(50f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲")
            }
        }
        // between minWidth and maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲乙")
            }
        }
        // greater than maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(40f)
                lineHeight(40f)
                text("甲乙丙")
            }
        }
    }
    // greater than maxHeight
    View {
        attr {
            flexDirectionRow()
            marginBottom(80f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // less than minWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("H")
            }
        }
        // between minWidth and maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("甲")
            }
        }
        // greater than maxWidth
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f);maxWidth(100f)
                minHeight(30f);maxHeight(50f)
                fontSize(60f)
                lineHeight(60f)
                text("甲乙")
            }
        }
    }
    // wrap
    View {
        attr {
            flexDirectionRow()
            marginBottom(20f)
            alignItemsFlexStart()
            justifyContentSpaceAround()
            backgroundColor(Color.GRAY)
        }
        // static width
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                width(100f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
        // dynamic width
        TextArea {
            attr {
                editable(false)
                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                minWidth(50f)
                maxWidth(100f)
                fontSize(20f)
                lineHeight(20f)
                text("甲乙丙丁戊己")
            }
        }
    }
}

private class CustomMeasureTextView : TextView() {

    private class CustomMeasureTextViewAttr : TextAttr() {
        var collapseWidth = 0f
        var expandWidth = 0f

        override fun minWidth(minWidth: Float): Attr {
            collapseWidth = minWidth
            return super.minWidth(minWidth)
        }

        override fun maxWidth(maxWidth: Float): Attr {
            expandWidth = maxWidth
            return super.maxWidth(maxWidth)
        }
    }

    override fun createAttr(): TextAttr {
        return CustomMeasureTextViewAttr()
    }

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        val collapseWidth = (attr as CustomMeasureTextViewAttr).collapseWidth
        val expandWidth = (attr as CustomMeasureTextViewAttr).expandWidth
        super.measure(node, expandWidth, height, measureOutput)
        measureOutput.width = if (measureOutput.width > collapseWidth) {
            expandWidth
        } else {
            collapseWidth
        }
    }
}

private fun ViewContainer<*, *>.CustomText(init: TextView.() -> Unit) {
    addChild(CustomMeasureTextView(), init)
}

private class CustomMeasureTextAreaView : TextAreaView() {

    private class CustomMeasureTextAreaAttr : TextAreaAttr() {
        var collapseWidth = 0f
        var expandWidth = 0f

        override fun minWidth(minWidth: Float): Attr {
            collapseWidth = minWidth
            return super.minWidth(minWidth)
        }

        override fun maxWidth(maxWidth: Float): Attr {
            expandWidth = maxWidth
            return super.maxWidth(maxWidth)
        }
    }

    override fun createAttr(): TextAreaAttr {
        return CustomMeasureTextAreaAttr()
    }

    override fun measure(
        node: FlexNode,
        width: Float,
        height: Float,
        measureOutput: MeasureOutput
    ) {
        val collapseWidth = (attr as CustomMeasureTextAreaAttr).collapseWidth
        val expandWidth = (attr as CustomMeasureTextAreaAttr).expandWidth
        super.measure(node, expandWidth, height, measureOutput)
        measureOutput.width = if (measureOutput.width > collapseWidth) {
            expandWidth
        } else {
            collapseWidth
        }
    }
}

private fun ViewContainer<*, *>.CustomTextArea(init: TextAreaView.() -> Unit) {
    addChild(CustomMeasureTextAreaView(), init)
}
