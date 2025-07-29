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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

internal class TextExampleFontSize: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    fontSize(8f)
                    text("attr { fontSize(8) }")
                }
            }
            Text {
                attr {
                    fontSize(9f)
                    text("attr { fontSize(9) }")
                }
            }
            Text {
                attr {
                    fontSize(10f)
                    text("attr { fontSize(10) }")
                }
            }
            Text {
                attr {
                    fontSize(12f)
                    text("attr { fontSize(12) }")
                }
            }
            Text {
                attr {
                    fontSize(14f)
                    text("attr { fontSize(14) }")
                }
            }
            Text {
                attr {
                    fontSize(16f)
                    text("attr { fontSize(16) }")
                }
            }
            Text {
                attr {
                    fontSize(18f)
                    text("attr { fontSize(18) }")
                }
            }
            Text {
                attr {
                    fontSize(20f)
                    text("attr { fontSize(20) }")
                }
            }
            Text {
                attr {
                    fontSize(22f)
                    text("attr { fontSize(22) }")
                }
            }
            Text {
                attr {
                    fontSize(24f)
                    text("attr { fontSize(24) }")
                }
            }
            Text {
                attr {
                    fontSize(28f)
                    text("attr { fontSize(28) }")
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleFontSize(init: TextExampleFontSize.() -> Unit) {
    addChild(TextExampleFontSize(), init)
}

internal class TextExampleFontWeight: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    fontWeightExtraLight()
                    fontSize(18f)
                    text("attr { fontWeight200() }")
                }
            }
            Text {
                attr {
                    fontWeightLight()
                    fontSize(18f)
                    text("attr { fontWeight300() }")
                }
            }
            Text {
                attr {
                    fontWeight400()
                    fontSize(18f)
                    text("attr { fontWeight400() }")
                }
            }
            Text {
                attr {
                    fontWeight500()
                    fontSize(18f)
                    text("attr { fontWeight500() }")
                }
            }
            Text {
                attr {
                    fontWeight600()
                    fontSize(18f)
                    text("attr { fontWeight600() }")
                }
            }
            Text {
                attr {
                    fontWeight700()
                    fontSize(18f)
                    text("attr { fontWeight700() }")
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleFontWeight(init: TextExampleFontWeight.() -> Unit) {
    addChild(TextExampleFontWeight(), init)
}

internal class TextExampleTextColor: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    text("attr { color(0xFFEE3333) }")
                    fontSize(18f)
                    color(0xFFEE3333)
                }
            }
            Text {
                attr {
                    text("attr { color(0xFFEECC33) }")
                    fontSize(18f)
                    color(0xFFEECC33)
                }
            }
            Text {
                attr {
                    text("attr { color(0xFF33EE33) }")
                    fontSize(18f)
                    color(0xFF33EE33)
                }
            }
            Text {
                attr {
                    text("attr { color(0xFF33EEEE) }")
                    fontSize(18f)
                    color(0xFF33EEEE)
                }
            }
            Text {
                attr {
                    text("attr { color(0xFF3377FF) }")
                    fontSize(18f)
                    color(0xFF3377FF)
                }
            }
            Text {
                attr {
                    text("attr { color(0xFFEE33EE) }")
                    fontSize(18f)
                    color(0xFFEE33EE)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleTextColor(init: TextExampleTextColor.() -> Unit) {
    addChild(TextExampleTextColor(), init)
}
internal class TextExampleTextShadow: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionRow()
                padding(all = 16f)
            }
            Text {
                attr {
                    text("red text shadow")
                    size(100f, 50f)
                    fontSize(18f)
                    backgroundColor(Color.YELLOW)
                    opacity(0.5f)
                    textShadow(2f, 2f, 2f, Color.RED)
                    boxShadow(BoxShadow(2f,2f,2f, Color.GRAY))
                }
            }
            Text {
                attr {
                    marginLeft(50f)
                    text("blue text shadow")
                    size(100f, 50f)
                    fontSize(18f)
                    textShadow(2f, 2f, 2f, Color.BLUE)
                    boxShadow(BoxShadow(2f,2f,2f, Color.GRAY))
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleTextShadow(init: TextExampleTextShadow.() -> Unit) {
    addChild(TextExampleTextShadow(), init)
}

internal class TextExampleTextStroke: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {

                padding(all = 16f)
            }
            Text {
                attr {
                    text("black text stroke")
                    fontSize(18f)
                    color(Color.WHITE)
                    textStroke(Color.BLACK, 3f)
                }
            }
            Text {
                attr {
                    marginTop(20f)
                    text("blue text shadow")
                    fontSize(28f)
                    color(Color.WHITE)
                    textStroke(Color.BLUE, 5f)
                 //   strokeWidth(5f)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleTextStroke(init: TextExampleTextStroke.() -> Unit) {
    addChild(TextExampleTextStroke(), init)
}

internal class TextExampleTextOverflow: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    text("textOverFlowClip() textOverFlowClip() textOverFlowClip() textOverFlowClip()")
                    fontSize(18f)
                    textOverFlowClip()
                    lines(1)
                }
            }
            Text {
                attr {
                    text("textOverFlowTail() textOverFlowTail() textOverFlowTail() textOverFlowTail() ")
                    fontSize(18f)
                    textOverFlowTail()
                    lines(1)
                }
            }
            Text {
                attr {
                    text("textOverFlowMiddle() textOverFlowMiddle() textOverFlowMiddle() textOverFlowMiddle()")
                    fontSize(18f)
                    textOverFlowMiddle()
                    lines(1)
                }
            }
            Text {
                attr {
                    text("textOverFlowWordWrapping() textOverFlowWordWrapping() textOverFlowWordWrapping()")
                    fontSize(18f)
                    textOverFlowWordWrapping()
                    lines(1)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleTextOverflow(init: TextExampleTextOverflow.() -> Unit) {
    addChild(TextExampleTextOverflow(), init)
}

internal class TextExampleTextAlign: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                height(118f)
            }
            Text {
                attr {
                    absolutePosition(left = 16f, right = 16f, top = 16f)
                    backgroundColor(Color(0xFFF0E68C))
                    text("Text Align Left")
                    fontSize(18f)
                    textAlignLeft()
                }
            }
            Text {
                attr {
                    absolutePosition(left = 16f, right = 16f, top = 48f)
                    backgroundColor(Color(0xFFADD8E6))
                    text("Text Align Center")
                    fontSize(18f)
                    textAlignCenter()
                }
            }
            Text {
                attr {
                    absolutePosition(left = 16f, right = 16f, top = 80f)
                    backgroundColor(Color(0xFFFFA07A))
                    text("Text Align Right")
                    fontSize(18f)
                    textAlignRight()
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleTextAlign(init: TextExampleTextAlign.() -> Unit) {
    addChild(TextExampleTextAlign(), init)
}

internal class TextExampleDecoration: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    text("attr { textDecorationUnderLine() }")
                    fontSize(18f)
                    textDecorationUnderLine()
                }
            }
            Text {
                attr {
                    text("attr { textDecorationLineThrough() }")
                    fontSize(18f)
                    textDecorationLineThrough()
                }
            }
            Text {
                attr {
                    text("attr { fontStyleItalic() }")
                    fontSize(18f)
                    fontStyleItalic()
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleDecoration(init: TextExampleDecoration.() -> Unit) {
    addChild(TextExampleDecoration(), init)
}

internal class TextExampleRichText: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            RichText {
                Span {
                    fontSize(16f)
                    text("这是")
                    click {
                        KLog.i("TextExamplePage", "First text span clicked, content: '这是'")
                    }
                }
                Span {
                    fontSize(18f)
                    text("一段")
                    click {
                        KLog.i("TextExamplePage", "Second text span clicked, content: '一段'")
                    }
                }
                Span {
                    fontSize(22f)
                    text("一段")
                    fontWeightMedium()
                    text("富文本")
                    click {
                        KLog.i("TextExamplePage", "Third text span clicked, content: '富文本'")
                    }
                }
            }
            RichText {
                attr {
                    lines(3)
                    textOverFlowClip()
                }
                Span {
                    fontSize(18f).color(Color(0xFFEE3333)).text("富文本")
                }
                Span {
                    fontSize(18f).text("可以指定")
                }
                Span {
                    fontSize(24f).text("文字")
                }
                Span {
                    fontSize(18f).text("的")
                }
                Span {
                    fontSize(18f).color(Color(0xFF33EE33)).text("颜色")
                }
                Span {
                    fontSize(18f).text("、")
                }
                Span {
                    fontSize(18f).fontWeightBold().text("粗细")
                }
                Span {
                    fontSize(18f).text("、")
                }
                Span {
                    fontSize(18f).text("阴影")
                    textShadow(2f, 2f, 0.3f, Color.BLUE)
                }
                Span {
                    fontSize(18f).text("以及")
                }
                Span {
                    fontSize(18f).textDecorationUnderLine().text("风格。")
                }
                Span {
                    fontSize(18f).text("嵌入图片1 ")
                }
                ImageSpan {
                    size(40f, 40f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                    borderRadius(5f)
                }
                Span {
                    fontSize(18f).text(" 嵌入图片2 ")
                }
                ImageSpan {
                    size(60f, 60f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                    borderRadius(5f)
                }
                Span {
                    fontSize(18f).text("第三行将截段成...区域不足以显示图片3")
                }
                ImageSpan {
                    size(60f, 60f)
                    src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif")
                    borderRadius(5f)
                }
            }
            RichText {
                attr {
                    margin(top = 6f, left = 30f, right = 30f)
                    lineHeight(16f)
                }

                Span {
                    text(DIALOG_INFO_PART1)
                    color(QBColor.A1)
                    fontWeight400()
                    fontSize(14f)
                }

                Span {
                    text(DIALOG_INFO_PART2)
                    color(QBColor.NEW_BLUE)
                    fontWeight400()
                    fontSize(14f)

                    click {
                        KLog.i("TextExamplePage", "Second text span clicked, content: '$DIALOG_INFO_PART2'")
                    }
                }

                Span {
                    text(DIALOG_INFO_PART6)
                    color(QBColor.A1)
                    fontWeight400()
                    fontSize(14f)
                }

                Span {
                    text(DIALOG_INFO_PART3)
                    color(QBColor.NEW_BLUE)
                    fontWeight400()
                    fontSize(14f)

                    click {
                        KLog.i("TextExamplePage", "Fourth text span clicked, content: '$DIALOG_INFO_PART3'")
                    }
                }

                Span {
                    text(DIALOG_INFO_PART4)
                    color(QBColor.A1)
                    fontWeight400()
                    fontSize(14f)
                }

                Span {
                    text(DIALOG_INFO_PART3)
                    color(QBColor.NEW_BLUE)
                    fontWeight400()
                    fontSize(14f)

                    click {
                        KLog.i("TextExamplePage", "Sixth text span clicked, content: '$DIALOG_INFO_PART3'")
                    }
                }

                Span {
                    text(DIALOG_INFO_PART5)
                    color(QBColor.A1)
                    fontWeight400()
                    fontSize(14f)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    private companion object {
        private const val DIALOG_TITLE = "软件许可协议和隐私政策"
        private const val DIALOG_INFO_PART1 = "在使用本产品或者服务前，请您充分理解本应用的"
        private const val DIALOG_INFO_PART2 = "《软件许可及服务协议》"
        private const val DIALOG_INFO_PART3 = "《隐私保护指引》"
        private const val DIALOG_INFO_PART4 =
            "的全部条款。App属于某类APP，主要功能包括“互联网内容浏览、搜索、下载等”。同意"
        private const val DIALOG_INFO_PART5 =
            "代表您同意本应用提供搜索、浏览、下载等主要功能时收集、" +
                    "处理您的相关必要个人信息，基于地理位置信息等提供拓展功能收集的个人信息将在您使用具体功能时单独征求您的同意。"
        private const val DIALOG_INFO_PART6 = "、"

    }
}

internal fun ViewContainer<*, *>.TextExampleRichText(init: TextExampleRichText.() -> Unit) {
    addChild(TextExampleRichText(), init)
}

internal class TextExampleLineHeight: ComposeView<ComposeAttr, ComposeEvent>() {
    override fun body(): ViewBuilder {
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                padding(all = 16f)
            }
            Text {
                attr {
                    text("简单文本lineHeight(10f)，简单文本lineHeight(10f)，简单文本lineHeight(10f)，简单文本lineHeight(10f)")
                    fontSize(15f)
                    lineHeight(10f)
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                }
            }
            Text {
                attr {
                    text("简单文本lineHeight(15f)，简单文本lineHeight(15f)，简单文本lineHeight(15f)，简单文本lineHeight(15f)")
                    fontSize(15f)
                    lineHeight(15f)
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                }
            }
            Text {
                attr {
                    text("简单文本lineHeight(25f)，简单文本lineHeight(25f)，简单文本lineHeight(25f)，简单文本lineHeight(25f)")
                    fontSize(15f)
                    lineHeight(25f)
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                }
            }
            RichText {
                attr {
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                    lineHeight(10f)
                }
                Span {
                    text("富文本lineHeight(10f)，富文本lineHeight(10f)，富文本lineHeight(10f)，富文本lineHeight(10f)")
                    fontSize(15f)
                }
            }
            RichText {
                attr {
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                    lineHeight(15f)
                }
                Span {
                    text("富文本lineHeight(15f)，富文本lineHeight(15f)，富文本lineHeight(15f)，富文本lineHeight(15f)")
                    fontSize(15f)
                }
            }
            RichText {
                attr {
                    backgroundColor(0xFFEEEEEE)
                    marginBottom(5f)
                    lineHeight(25f)
                }
                Span {
                    text("富文本lineHeight(25f)，富文本lineHeight(25f)，富文本lineHeight(25f)，富文本lineHeight(25f)")
                    fontSize(15f)
                }
            }
        }
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
}

internal fun ViewContainer<*, *>.TextExampleLineHeight(init: TextExampleLineHeight.() -> Unit) {
    addChild(TextExampleLineHeight(), init)
}

@Page("TextExamplePage")
internal class TextExamplePage: BasePager() {
    override fun body(): ViewBuilder {
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "Text Attr Example" } }
            List {
                attr { flex(1f) }
                ViewExampleSectionHeader { attr { title = "FontSize" } }
                TextExampleFontSize {  }
                ViewExampleSectionHeader { attr { title = "FontWeight" } }
                TextExampleFontWeight {  }
                ViewExampleSectionHeader { attr { title = "TextColor" } }
                TextExampleTextColor {  }
                ViewExampleSectionHeader { attr { title = "TextShadow" } }
                TextExampleTextShadow {  }
                // 文字描边
                ViewExampleSectionHeader { attr { title = "TextStroke" } }
                TextExampleTextStroke {  }
                ViewExampleSectionHeader { attr { title = "TextOverFlow" } }
                TextExampleTextOverflow {  }
                ViewExampleSectionHeader { attr { title = "TextAlign" } }
                TextExampleTextAlign {  }
                ViewExampleSectionHeader { attr { title = "Decoration & FontStyle" } }
                TextExampleDecoration {  }
                ViewExampleSectionHeader { attr { title = "RichText" } }
                TextExampleRichText {  }
                ViewExampleSectionHeader { attr { title = "LineHeight" } }
                TextExampleLineHeight {  }
            }
        }
    }
}

object QBColor {

    //////////////// 基础色板列表 ////////////////

    /**
     * 主色-日间模式
     */
    val NEW_BLUE = Color(0xFF205AEF)

    /**
     * 主色-日间模式-暗色场景
     */
    val NEW_BLUE_DARK = Color(0xFF367AFF)

    /**
     * 主色-夜间模式
     */
    val NEW_BLUE_NIGHT = Color(0xFF2B4DA1)

    /**
     * 主色-夜间模式-暗色场景
     */
    val NEW_BLUE_NIGHT_DARK = Color(0xFF2D57A9)

    /**
     * 红色/警告/强调
     * 日间模式
     */
    val RED = Color(0xFFF44837)

    /**
     * 红色/警告/强调
     * 暗色场景
     */
    val RED_DARK = Color(0xFFFF502A)

    /**
     * 红色/警告/强调
     * 夜间模式
     */
    val RED_NIGHT= Color(0xFF75322D)

    /**
     * Orange/分数/评星/辅助色
     * 日间模式
     */
    val ORANGE = Color(0xFFFF8A14)

    /**
     * Orange/分数/评星/辅助色
     * 夜间模式
     */
    val ORANGE_NIGHT = Color(0xFF794C20)

    /**
     * Yellow/辅助色
     * 日间模式
     */
    val YELLOW = Color(0xFFFFC20D)

    /**
     * Yellow/辅助色
     * 夜间模式
     */
    val YELLOW_NIGHT = Color(0xFF79631D)

    /**
     * Gold/金色/会员/小说/等级体系
     * 日间模式
     */
    val GOLD = Color(0xFFEEBF8A)

    /**
     * Gold/金色/会员/小说/等级体系
     * 夜间模式
     */
    val GOLD_NIGHT = Color(0xFF72614F)

    /**
     * Green/安全/辅助色
     * 日间模式
     */
    val GREEN = Color(0xFF0BB861)

    /**
     * Green/安全/辅助色
     * 夜间模式
     */
    val GREEN_NIGHT = Color(0xFF185E3E)

    /**
     * Cyan/辅助色
     * 日间模式
     */
    val CYAN = Color(0xFF0ACC9B)

    /**
     * Cyan/辅助色
     * 夜间模式
     */
    val CYAN_NIGHT = Color(0xFF067A5D)

    /**
     * Purple/辅助色
     * 日间模式
     */
    val PURPLE = Color(0xFF7632FF)

    /**
     * Purple/辅助色
     * 夜间模式
     */
    val PURPLE_NIGHT = Color(0xFF43297D)

    /**
     * Brown/辅助色
     * 日间模式
     */
    val BROWN = Color(0xFF8E520D)

    /**
     * Brown/辅助色
     * 夜间模式
     */
    val BROWN_NIGHT = Color(0xFF75322D)

    //////////////// 文本&图标颜色 ////////////////

    /**
     * A1
     * 日间模式-实色
     */
    val A1 = Color(0xFF242424)

    /**
     * A1
     * 夜间模式
     */
    val A1_NIGHT = Color(0xFF747A82)

    /**
     * A1
     * 透明色
     */
    val A1_TRANSPARENT = Color(0x000000, 0.86f)

    /**
     * A1
     * 暗色场景
     */
    val A1_DARK = Color(0xFFFFFFFF)

    /**
     * A2
     * 日间模式-实色
     */
    val A2 = Color(0xFF666666)

    /**
     * A2
     * 夜间模式
     */
    val A2_NIGHT = Color(0xFF595E66)

    /**
     * A2
     * 透明色
     */
    val A2_TRANSPARENT = Color(0x000000, 0.60f)

    /**
     * A2
     * 暗色场景
     */
    val A2_DARK = Color(0xFFFFFF, 0.60f)

    /**
     * A3
     * 日间模式-实色
     */
    val A3 = Color(0xFF8f8f8f)

    /**
     * A3
     * 夜间模式
     */
    val A3_NIGHT = Color(0xFF4B5057)

    /**
     * A3
     * 透明色
     */
    val A3_TRANSPARENT = Color(0x000000, 0.44f)

    /**
     * A3
     * 暗色场景
     */
    val A3_DARK = Color(0xFFFFFF, 0.44f)

    /**
     * A4
     * 日间模式-实色
     */
    val A4 = Color(0xFFB3B3B3)

    /**
     * A4
     * 夜间模式
     */
    val A4_NIGHT = Color(0xFF3D4249)

    /**
     * A4
     * 透明色
     */
    val A4_TRANSPARENT = Color(0x000000, 0.30f)

    /**
     * A4
     * 暗色场景
     */
    val A4_DARK = Color(0xFFFFFF, 0.3f)

    //////////////// 背景&框架 ////////////////

    /**
     * 浅灰色背景
     * 日间模式
     */
    val BG_GREY = Color(0xFFF6F7FA)

    /**
     * 浅灰色背景
     * 夜间模式
     */
    val BG_GREY_NIGHT = Color(0xFF202327)

    /**
     * 卡片&列表背景
     * 日间模式
     */
    val BG_WHITE = Color(0xFFFFFFFF)

    /**
     * 卡片&列表背景
     * 夜间模式
     */
    val BG_WHITE_NIGHT = Color(0xFF23282D)

    /**
     * 加载占位符
     * 日间模式
     */
    val BG_FRAME = Color(0xFFF5F5F5)

    /**
     * 加载占位符
     * 夜间模式
     */
    val BG_FRAME_NIGHT = Color(0xFF31363B)

    /**
     * 透明黑背景
     * 日间模式
     */
    val BG_BLACK_TRANSPARENT = Color(0x000000, 0.06f)

    /**
     * 透明黑背景
     * 夜间模式
     */
    val BG_BLACK_TRANSPARENT_NIGHT = Color(0xFFFFFF, 0.06f)

    /**
     * 弹窗类组件蒙层
     * 日间模式
     */
    val MASK = Color(0x000000, 0.4f)

    /**
     * 弹窗类组件蒙层
     * 夜间模式
     */
    val MASK_NIGHT = Color(0x000000, 0.4f)

    //////////////// 线条&描边 ////////////////

    /**
     * 分割线
     * 日间模式
     */
    val LINE = Color(0x000000, 0.08f)

    /**
     * 分割线
     * 夜间模式
     */
    val LINE_NIGHT = Color(0xFF31363B)

    /**
     * 图标&封面描边
     * 日间模式
     */
    val BORDER = Color(0x000000, 0.04f)

    /**
     * 图标&封面描边
     * 夜间模式
     */
    val BORDER_NIGHT = Color(0x000000, 0.04f)
}