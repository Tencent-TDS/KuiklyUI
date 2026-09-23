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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.reactive.ObservableProperties
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ImageSpan
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.RichTextView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.SelectableOption
import com.tencent.kuikly.core.views.SelectionType
import com.tencent.kuikly.core.views.Span
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

private class TextJustifyDemoVM(scope: PagerScope) {
    var justifyEnabled by scope.observable(true)
    var clickStatus by scope.observable("还没点")
    var selectionContent by scope.observable("还没读")
    var selectableTextContainer: ViewRef<DivView>? = null

    fun applyChosenAlign(textAttr: TextAttr) {
        if (justifyEnabled) {
            textAttr.textAlignJustify()
        } else {
            textAttr.textAlignLeft()
        }
    }

    fun createSelection(x: Float, y: Float, type: SelectionType) {
        selectableTextContainer?.view?.createSelection(x, y, type)
    }

    fun readSelection() {
        val textView = selectableTextContainer?.view
        if (textView == null) {
            selectionContent = "没有可选文本"
            return
        }
        textView.getSelection { result ->
            val selected = result.content.filter { it.isNotEmpty() }
            selectionContent = if (selected.isEmpty()) {
                "空"
            } else {
                selected.joinToString(" | ")
            }
        }
    }
}

@Page("TextJustifyDemo")
internal class TextJustifyDemo : BasePager() {

    private val vm = TextJustifyDemoVM(this)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }
            NavBar { attr { title = "Text Justify" } }
            View {
                attr {
                    flexDirectionRow()
                    margin(left = 16f, right = 16f, top = 8f, bottom = 12f)
                }
                Button {
                    attr {
                        backgroundColor(Color(0xFF1E88E5))
                        padding(left = 10f, right = 10f, top = 8f, bottom = 8f)
                        testTag("justify_toggle")
                        titleAttr {
                            color(Color.WHITE)
                            fontSize(13f)
                            text(if (ctx.vm.justifyEnabled) "当前：两端对齐" else "当前：左对齐")
                        }
                    }
                    event {
                        click { ctx.vm.justifyEnabled = !ctx.vm.justifyEnabled }
                    }
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    padding(all = 16f)
                    testTag("justify_list")
                }
                JustifyExampleSection(ctx.vm)
                ClickAndSelectionSection(ctx.vm)
                BoundarySection(ctx.vm)
            }
        }
    }

}

private const val SAMPLE_IMAGE_URL =
    "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"

private const val SOLID_PIXEL =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII="

private const val CHINESE_JUSTIFY_PARAGRAPH =
    "两端对齐把一行中尚未占满的空白分配到字或词之间，使左右边缘对齐。" +
            "软换行中间行撑满容器，段落最后一行以及硬换行之前的那一行保持左对齐。"

private class BlockSpanAttr {
    var width: Float = 0f
    var height: Float = 0f
    var color: Color = Color.TRANSPARENT
    var onClick: (() -> Unit)? = null

    fun size(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    fun backgroundColor(color: Color) {
        this.color = color
    }

    fun click(handler: () -> Unit) {
        onClick = handler
    }
}

private fun RichTextView.BlockSpan(init: BlockSpanAttr.() -> Unit) {
    val attr = BlockSpanAttr().apply(init)
    ImageSpan {
        size(attr.width, attr.height)
        src(SOLID_PIXEL)
        resizeStretch()
        tintColor(attr.color)
        attr.onClick?.also { handler ->
            click { handler() }
        }
    }
}

private const val MIXED_JUSTIFY_PARAGRAPH =
    "Justification distributes the empty space remaining in a line across the gaps between characters or words. Intermediate lines of a soft-wrapped block should fill the container width, while the final line remains left-aligned.\n" +
            "两端对齐把一行中尚未占满的空白分配到字或词之间。软换行的中间行应撑满容器，最后一行保持左对齐。"

private fun ViewContainer<*, *>.JustifyExampleSection(vm: TextJustifyDemoVM) {
    Text {
        attr {
            fontSize(24f)
            fontWeightBold()
            marginBottom(12f)
            text("两端对齐（Justify）")
        }
    }
    View {
        attr {
            backgroundColor(Color(0xFFE8F5E9))
            padding(8f)
            marginBottom(12f)
            testTag("justify_hello")
        }
        Text {
            attr {
                text("你好 Kuikly!")
                vm.applyChosenAlign(this)
            }
        }
    }
    View {
        attr {
            backgroundColor(Color(0xFFF3E5F5))
            padding(8f)
            marginBottom(12f)
            testTag("justify_paragraph")
        }
        Text {
            attr {
                text(MIXED_JUSTIFY_PARAGRAPH)
                vm.applyChosenAlign(this)
            }
        }
    }
    RichText {
        attr {
            backgroundColor(Color(0xFFFFF3E0))
            marginBottom(12f)
            testTag("justify_styles")
            vm.applyChosenAlign(this)
        }
        Span {
            color(Color(0xFF333333))
            text("多样式")
        }
        Span {
            fontSize(18f)
            fontWeightBold()
            color(Color(0xFF1565C0))
            text("两端对齐")
        }
        Span {
            textDecorationUnderLine()
            text("把不同颜色、字号和装饰拼在同一段中。")
        }
        Span {
            text("两端对齐把一行中尚未占满的空白分配到字或词之间。软换行的中间行应撑满容器，最后一行保持左对齐。")
        }
    }
    RichText {
        attr {
            width(300f)
            backgroundColor(Color(0xFFE3F2FD))
            marginBottom(12f)
            testTag("justify_inline_zh")
            fontSize(20f)
            vm.applyChosenAlign(this)
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color(0xFF00FFFF))
        }
        Span {
            fontSize(20f)
            text("一二三四五六")
        }
        ImageSpan {
            src(SAMPLE_IMAGE_URL)
            size(24f, 24f)
        }
        BlockSpan {
            size(150f, 24f)
            backgroundColor(Color(0xFF888888, 0.5f))
        }
        Span {
            fontSize(20f)
            text("中间文字")
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color.YELLOW)
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color(0xFFFF00FF))
        }
        Span {
            fontSize(20f)
            text("末尾")
        }
    }
    RichText {
        attr {
            width(300f)
            backgroundColor(Color(0xFFF8BBD0))
            marginBottom(12f)
            testTag("justify_inline_en")
            fontSize(20f)
            vm.applyChosenAlign(this)
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color(0xFF00FFFF))
        }
        Span {
            fontSize(20f)
            text("ABCDEFGHIJKLMN")
        }
        ImageSpan {
            src(SAMPLE_IMAGE_URL)
            size(24f, 24f)
        }
        Span {
            fontSize(20f)
            text("OPQRSTUVWXYZ")
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color.YELLOW)
        }
        BlockSpan {
            size(24f, 24f)
            backgroundColor(Color(0xFFFF00FF))
        }
        Span {
            fontSize(20f)
            text("THE END")
        }
    }
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF888888))
            marginBottom(4f)
            text("行中图片：前字 + 色块 + 后字，下一行宽块迫使上一行软换行")
        }
    }
    RichText {
        attr {
            width(300f)
            backgroundColor(Color(0xFFE1F5FE))
            marginBottom(12f)
            testTag("justify_image_middle")
            fontSize(20f)
            vm.applyChosenAlign(this)
        }
        Span {
            fontSize(20f)
            text("前字")
        }
        BlockSpan {
            size(36f, 24f)
            backgroundColor(Color(0xFFFF5722))
        }
        Span {
            fontSize(20f)
            text("后字")
        }
        BlockSpan {
            size(220f, 24f)
            backgroundColor(Color(0xFF888888, 0.5f))
        }
        Span {
            fontSize(20f)
            text("换行")
        }
    }
}


private fun ViewContainer<*, *>.ClickAndSelectionSection(vm: TextJustifyDemoVM) {
    SectionTitle("点击与选区")
    Text {
        attr {
            fontSize(13f)
            color(Color(0xFF333333))
            marginBottom(4f)
            testTag("justify_status")
            text("点击: ${vm.clickStatus}")
        }
    }
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF666666))
            marginBottom(8f)
            testTag("justify_selection_status")
            text("选区: ${vm.selectionContent}")
        }
    }
    View {
        attr {
            flexDirectionRow()
            marginBottom(8f)
        }
        Button {
            attr {
                backgroundColor(Color(0xFF00897B))
                padding(left = 10f, right = 10f, top = 8f, bottom = 8f)
                testTag("justify_sel_create")
                titleAttr {
                    color(Color.WHITE)
                    fontSize(13f)
                    text("建立选区")
                }
            }
            event {
                click { vm.createSelection(40f, 22f, SelectionType.CHARACTER) }
            }
        }
    }
    View {
        ref { vm.selectableTextContainer = it }
        attr {
            width(300f)
            backgroundColor(Color(0xFFFFF8E1))
            padding(8f)
            marginBottom(12f)
            testTag("justify_sel_box")
            selectable(SelectableOption.ENABLE)
        }
        event {
            selectStart { vm.selectionContent = "正在选" }
            selectEnd { vm.readSelection() }
            selectCancel { vm.selectionContent = "已取消" }
        }
        RichText {
            attr { vm.applyChosenAlign(this) }
            event { click { vm.clickStatus = "空白，没有点中色块或文字" } }
            BlockSpan {
                size(24f, 24f)
                backgroundColor(Color(0xFF00FFFF))
                click { vm.clickStatus = "青色块" }
            }
            Span {
                fontSize(20f)
                color(Color(0xFFC62828))
                text("一二三四五六")
                click { vm.clickStatus = "红色「一二三四五六」" }
            }
            ImageSpan {
                src(SAMPLE_IMAGE_URL)
                size(24f, 24f)
                click { vm.clickStatus = "图片" }
            }
            BlockSpan {
                size(150f, 24f)
                backgroundColor(Color(0xFF888888, 0.5f))
                click { vm.clickStatus = "灰色宽块" }
            }
            Span {
                fontSize(20f)
                color(Color(0xFF2E7D32))
                text("中间文字")
                click { vm.clickStatus = "绿色「中间文字」" }
            }
            BlockSpan {
                size(24f, 24f)
                backgroundColor(Color.YELLOW)
                click { vm.clickStatus = "黄色块" }
            }
            BlockSpan {
                size(24f, 24f)
                backgroundColor(Color(0xFFFF00FF))
                click { vm.clickStatus = "品红块" }
            }
            Span {
                fontSize(22f)
                color(Color(0xFF1565C0))
                text("右侧")
                click { vm.clickStatus = "蓝色「右侧」" }
            }
            Span {
                fontSize(14f)
                text(" END\n")
                click { vm.clickStatus = "「 END」" }
            }
            Span {
                fontSize(12f)
                text("小字 Small ")
                click { vm.clickStatus = "小字「小字 Small」" }
            }
            Span {
                fontSize(22f)
                text("大字 ")
                click { vm.clickStatus = "大字「大字」" }
            }
            Span {
                fontSize(16f)
                text("中英混排 English mixed \uD83C\uDF1A\uD83C\uDF1D 软换行撑满容器，硬换行前的最后一行保持左对齐。")
                click { vm.clickStatus = "中英混排那段" }
            }
        }
    }
}

private fun ViewContainer<*, *>.BoundarySection(vm: TextJustifyDemoVM) {
    SectionTitle("边界情况")
    LabeledText(vm, "首行缩进", CHINESE_JUSTIFY_PARAGRAPH) {
        firstLineHeadIndent(24f)
    }
    LabeledText(vm, "行末缩进", CHINESE_JUSTIFY_PARAGRAPH + CHINESE_JUSTIFY_PARAGRAPH) {
        lines(2)
        lineBreakMargin(80f)
    }
    LabeledText(vm, "字距", CHINESE_JUSTIFY_PARAGRAPH) {
        letterSpacing(1.5f)
    }
    LabeledText(vm, "行数截断", CHINESE_JUSTIFY_PARAGRAPH + CHINESE_JUSTIFY_PARAGRAPH) {
        lines(2)
        textOverFlowTail()
    }
    FlexibleWidthText(vm)
    LabeledText(vm, "空串", "")
}

private fun ViewContainer<*, *>.FlexibleWidthText(vm: TextJustifyDemoVM) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF888888))
            marginBottom(4f)
            text("不固定宽度")
        }
    }
    Text {
        attr {
            alignSelfFlexStart()
            backgroundColor(Color(0xFFF0F0F0))
            marginBottom(12f)
            testTag("justify_fluid_width")
            fontSize(16f)
            text(CHINESE_JUSTIFY_PARAGRAPH)
            vm.applyChosenAlign(this)
        }
    }
}

private fun ViewContainer<*, *>.LabeledText(
    vm: TextJustifyDemoVM,
    label: String,
    text: String,
    extraAttr: TextAttr.() -> Unit = {}
) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF888888))
            marginBottom(4f)
            text(label)
        }
    }
    Text {
        attr {
            backgroundColor(Color(0xFFF0F0F0))
            marginBottom(12f)
            fontSize(16f)
            text(text)
            extraAttr()
            vm.applyChosenAlign(this)
        }
    }
}

private fun ViewContainer<*, *>.SectionTitle(title: String) {
    Text {
        attr {
            fontSize(18f)
            fontWeightBold()
            marginBottom(8f)
            marginTop(8f)
            text(title)
        }
    }
}


