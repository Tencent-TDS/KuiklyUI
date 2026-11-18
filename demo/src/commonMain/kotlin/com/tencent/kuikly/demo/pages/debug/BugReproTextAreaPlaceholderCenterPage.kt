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

package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Android TextArea 提示文字位置偏上的复现页，也用于修复后的回归检查。
 *
 * 黄块是 TextArea，蓝块是同样字号和行高的普通 Text，作为位置对照。
 * ①~④ 是原始复现与对照；⑤~⑨ 检查属性设置顺序、不设 minHeight、多行文字、
 * 右对齐和空提示文字；底部按钮切换提示文字与行高，检查运行时更新。
 * 打开方式：adb shell am start ... --es pageName BugReproTextAreaPlaceholderCenterPage
 */
@Page("BugReproTextAreaPlaceholderCenterPage")
internal class BugReproTextAreaPlaceholderCenterPage : BasePager() {

    var step by observable(0)
    var dynamicHint by observable("动态提示文字")
    var dynamicLineHeight by observable(24f)
    var dynamicHintColor by observable(Color(0x80111111))

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "TextArea placeholder 居中/边界回归"
                }
            }
            View {
                attr {
                    flex(1f)
                    padding(12f)
                }
                Text {
                    attr {
                        text("黄块是 TextArea，蓝块是同样字号和行高的普通 Text，作为位置对照")
                        fontSize(12f)
                        color(Color.BLACK)
                        marginBottom(10f)
                    }
                }
                cases.forEach { reproRow(it) }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        height(48f)
                        marginBottom(8f)
                    }
                    Text {
                        attr {
                            width(96f)
                            text("⑩ 动态属性")
                            fontSize(11f)
                            color(Color.BLACK)
                        }
                    }
                    TextArea {
                        attr {
                            overflow(true)
                            flex(1f)
                            height(48f)
                            fontSize(12f)
                            lineHeight(ctx.dynamicLineHeight)
                            placeholder(ctx.dynamicHint)
                            placeholderColor(ctx.dynamicHintColor)
                            color(Color.BLACK)
                            backgroundColor(Color.YELLOW.opacity(0.3f))
                        }
                    }
                }
                View {
                    attr {
                        height(36f)
                        allCenter()
                        backgroundColor(Color(0xFF3C6CBDL))
                    }
                    event {
                        click {
                            ctx.step += 1
                            when (ctx.step % 3) {
                                0 -> {
                                    ctx.dynamicHint = "动态提示文字"
                                    ctx.dynamicLineHeight = 24f
                                    ctx.dynamicHintColor = Color(0x80111111)
                                }
                                1 -> {
                                    ctx.dynamicHint = "加长的提示文字"
                                    ctx.dynamicLineHeight = 32f
                                    ctx.dynamicHintColor = Color(0x80FF0000)
                                }
                                else -> {
                                    ctx.dynamicHint = ""
                                    ctx.dynamicLineHeight = 20f
                                    ctx.dynamicHintColor = Color(0x800000FF)
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            text("切换动态属性（第 ${ctx.step} 次）")
                            fontSize(12f)
                            color(Color.WHITE)
                        }
                    }
                }
            }
        }
    }
}

private enum class Align { LEFT, CENTER, RIGHT }

private data class Case(
    val label: String,
    val value: String = "",
    val lineHeight: Float? = 24f,
    val minHeight: Float? = 24f,
    val rowHeight: Float = 48f,
    val align: Align = Align.LEFT,
    val hint: String = "请输入内容",
    val hintFirst: Boolean = false
)

private val cases = listOf(
    Case("① placeholder"),
    Case("② 真实文字", value = "已输入文字"),
    Case("③ 无 lineHeight", lineHeight = null),
    Case("④ 居中对齐", align = Align.CENTER),
    Case("⑤ 先设置提示文字", hintFirst = true),
    Case("⑥ 无 minHeight", minHeight = null),
    Case("⑦ 多行文字", value = "第一行\n第二行", minHeight = null, rowHeight = 60f),
    Case("⑧ 右对齐", align = Align.RIGHT),
    Case("⑨ 空提示文字", hint = "")
)

private fun ViewContainer<*, *>.reproRow(case: Case) {
    View {
        attr {
            flexDirectionRow()
            alignItemsCenter()
            height(case.rowHeight)
            marginBottom(8f)
        }
        Text {
            attr {
                width(96f)
                text(case.label)
                fontSize(11f)
                color(Color.BLACK)
            }
        }
        TextArea {
            attr {
                overflow(true)
                flex(1f)
                case.minHeight?.let { minHeight(it) }
                maxHeight(100f)
                fontSize(12f)
                val applyHint = {
                    placeholder(case.hint)
                    placeholderColor(Color(0x80111111))
                }
                val applyLineHeight = {
                    case.lineHeight?.let { lineHeight(it) }
                }
                if (case.hintFirst) {
                    applyHint()
                    applyLineHeight()
                } else {
                    applyLineHeight()
                    applyHint()
                }
                when (case.align) {
                    Align.CENTER -> textAlignCenter()
                    Align.RIGHT -> textAlignRight()
                    Align.LEFT -> Unit
                }
                color(Color.BLACK)
                text(case.value)
                backgroundColor(Color.YELLOW.opacity(0.3f))
            }
        }
        Text {
            attr {
                width(72f)
                height(24f)
                text("居中基线")
                fontSize(12f)
                lineHeight(24f)
                color(Color(0x80111111))
                backgroundColor(Color(0xFFDDEEFFL))
            }
        }
    }
}
