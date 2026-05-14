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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputSpan
import com.tencent.kuikly.core.views.InputSpans
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.LengthLimitType
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 输入控件对比验收页。
 *
 * 页面通过 pageData 的 "useTextEditorImpl"（"0"/"1"）判断当前走的是老控件还是新控件，
 * 仅用于在 NavBar 标题上展示，组件行为本身由 OHOS 侧全局标志位控制
 * （由 OHOS 宿主 AppKRRouterAdapter.openPage 在真正 pushUrl 之前，根据该参数
 * 调用 libkuikly_entry.so 暴露的 setUseNewTextInputComponent(...) 写入；
 * iOS/Android 直接忽略该参数）。
 *
 * 页面结构：
 *  - 上半部分：Input 单行输入用例 ①~⑦
 *  - 下半部分：TextArea 多行输入用例 ⑧~⑯
 *
 * Input 侧覆盖：基础样式/textAlign/keyboardType(password|number)/
 *   maxTextLength+textLengthBeyondLimit/returnKeyType+inputReturn/editable/
 *   focus-blur/cursorIndex。
 *
 * TextArea 侧在 Input 基础上再额外覆盖 TextArea 相对 Input 的独有/差异化能力：
 *   - 多行渲染 / 行间回车是否触发 inputReturn（关键差异点）
 *   - TextArea 独有的 lineHeight / lines 语义
 *   - inputSpans 富文本（新控件若不支持会暴露问题）
 *   - 基于 MeasureFunction 的自适应高度 / 固定高度内部滚动
 *
 * 每个输入框旁展示最近一次 textDidChange 的回调值与回调次数，便于对比两种实现的行为差异。
 */
@Page("InputCompareDemoPage")
internal class InputCompareDemoPage : BasePager() {

    // 当前标志位（展示用），0=老控件；1=新控件（ARKUI_NODE_TEXT_EDITOR 实现）
    private var useTextEditorImplFlag: String by observable("0")

    // ---- Input 侧 observable ----
    private var basicInputText: String by observable("")
    private var basicInputCount: Int by observable(0)
    private var basicCursor: Int by observable(-1)

    private var alignInputText: String by observable("")
    private var alignInputCount: Int by observable(0)

    private var passwordInputText: String by observable("")
    private var passwordInputCount: Int by observable(0)

    private var numberInputText: String by observable("")
    private var numberInputCount: Int by observable(0)

    private var maxLenInputText: String by observable("")
    private var maxLenInputCount: Int by observable(0)
    private var maxLenBeyondCount: Int by observable(0)

    private var returnInputText: String by observable("")
    private var lastImeAction: String by observable("")
    private var returnCount: Int by observable(0)

    private var focusState: String by observable("blur")

    // ---- TextArea 侧 observable ----
    private var areaBasicText: String by observable("")
    private var areaBasicCount: Int by observable(0)
    private var areaBasicFocusState: String by observable("blur")
    private var areaBasicCursor: Int by observable(-1)

    private var areaAlignText: String by observable("")
    private var areaAlignCount: Int by observable(0)

    private var areaNumberText: String by observable("")
    private var areaNumberCount: Int by observable(0)

    private var areaReturnText: String by observable("")
    private var areaReturnImeAction: String by observable("")
    private var areaReturnCount: Int by observable(0)

    private var areaLimitedText: String by observable("")
    private var areaLimitedCount: Int by observable(0)
    private var areaLimitedBeyondCount: Int by observable(0)

    // TextArea ⑵ inputSpans（富文本）
    private var areaSpans: InputSpans by observable(initialRichSpans())
    private var areaSpansCount: Int by observable(0)

    // TextArea ⑶ 自适应高度
    private var areaAutoHeightText: String by observable("")
    private var areaAutoHeightCount: Int by observable(0)

    // 软键盘高度（来自 Input/TextArea 的 keyboardHeightChange 事件）。
    // 键盘弹起时为实际高度，收起时为 0。用于动态收缩 List 的可视高度，
    // 避免列表底部被键盘遮挡导致无法滚动到看/点击。
    private var keyboardHeight: Float by observable(0f)

    // refs（控制按钮用）
    private lateinit var basicRef: ViewRef<InputView>
    private lateinit var areaBasicRef: ViewRef<TextAreaView>

    override fun created() {
        super.created()
        useTextEditorImplFlag = pageData.params.optString(PARAM_USE_TEXT_EDITOR_IMPL).ifEmpty { "0" }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = if (ctx.useTextEditorImplFlag == "1") {
                        "输入控件对比｜新(ARKUI_NODE_TEXT_EDITOR)"
                    } else {
                        "输入控件对比｜老(ARKUI_NODE_TEXT_INPUT)"
                    }
                }
            }

            List {
                attr {
                    // 使用固定高度 = 页面可视高度 - 顶部 NavBar 高度 - 当前键盘高度，
                    // 这样键盘弹起时 List 会相应收缩，底部内容不会被键盘遮挡。
                    // 注：navigationBarHeight = statusBarHeight + 44（见 PagerData）。
                    val listHeight = ctx.pageData.pageViewHeight -
                        ctx.pageData.navigationBarHeight -
                        ctx.keyboardHeight
                    height(if (listHeight > 0f) listHeight else 0f)
                    padding(12f)
                }

                // ---- Header tip ----
                View {
                    attr {
                        padding(8f)
                        backgroundColor(Color(0xFFF2F3F5L))
                        borderRadius(6f)
                        marginBottom(8f)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666L))
                            value(
                                "当前模式: ${if (ctx.useTextEditorImplFlag == "1") "新控件(API>=24)" else "老控件"}。\n" +
                                    "上半部分为 Input(单行) 用例①~⑧，下半部分为 TextArea(多行) 用例⑨~⑱。"
                            )
                        }
                    }
                }

                // ============================================================
                // ========== Part I：Input（单行） ==========================
                // ============================================================
                SectionDivider("Part I · Input（单行）")

                // ===== ① 基础样式 + textDidChange/focus/blur + cursor =====
                SectionTitle("① 基础样式（带初始文本）")
                Input {
                    ref { ctx.basicRef = it }
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        fontWeightMedium()
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("请输入内容…")
                        placeholderColor(Color.GRAY)
                        text("hello-kuikly")
                        tintColor(Color(0xFF3C6CBDL))
                    }
                    event {
                        textDidChange {
                            ctx.basicInputCount += 1
                            ctx.basicInputText = it.text
                            KLog.i(TAG, "basic textDidChange #${ctx.basicInputCount}: '${it.text}'")
                        }
                        inputFocus {
                            ctx.focusState = "focus"
                            KLog.i(TAG, "basic inputFocus: '${it.text}'")
                        }
                        inputBlur {
                            ctx.focusState = "blur"
                            KLog.i(TAG, "basic inputBlur: '${it.text}'")
                        }
                        // 监听键盘高度变化，驱动 List 动态收缩，避免底部内容被键盘遮挡。
                        // 该页面内任一 Input/TextArea 收起/弹出键盘都会触发此事件，
                        // 因此只需在第一个 Input 上注册即可共享给整页。
                        keyboardHeightChange(false) {
                            ctx.keyboardHeight = it.height
                            KLog.i(TAG, "keyboardHeightChange height=${it.height} duration=${it.duration}")
                        }
                    }
                }
                CallbackTip {
                    "回调次数=${ctx.basicInputCount} 焦点=${ctx.focusState} " +
                        "cursor=${ctx.basicCursor} 最近值='${ctx.basicInputText}'"
                }

                // 快捷按钮组：setText / focus / blur
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    SmallButton("setText='abc'") {
                        ctx.basicRef.view?.setText("abc")
                    }
                    SmallButton("清空") {
                        ctx.basicRef.view?.setText("")
                    }
                    SmallButton("focus") {
                        ctx.basicRef.view?.focus()
                    }
                    SmallButton("blur") {
                        ctx.basicRef.view?.blur()
                    }
                }
                // cursor 相关按钮（独立一行放下，不挤压上面按钮）
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    SmallButton("cursor?") {
                        ctx.basicRef.view?.cursorIndex { idx ->
                            ctx.basicCursor = idx
                            KLog.i(TAG, "basic cursorIndex=$idx")
                        }
                    }
                    SmallButton("setCursor=3") {
                        ctx.basicRef.view?.setCursorIndex(3)
                    }
                    SmallButton("setCursor=0") {
                        ctx.basicRef.view?.setCursorIndex(0)
                    }
                    // 占位，保证按钮宽度均匀
                    View { attr { flex(1f) } }
                }

                // ===== ② textAlign 三态 =====
                SectionTitle("② textAlign 左/中/右")
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    Input {
                        attr {
                            flex(1f)
                            height(40f)
                            marginRight(6f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("左对齐")
                            placeholderColor(Color.GRAY)
                            textAlignLeft()
                        }
                        event {
                            textDidChange {
                                ctx.alignInputCount += 1
                                ctx.alignInputText = "L:${it.text}"
                            }
                        }
                    }
                    Input {
                        attr {
                            flex(1f)
                            height(40f)
                            marginRight(6f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("居中")
                            placeholderColor(Color.GRAY)
                            textAlignCenter()
                        }
                        event {
                            textDidChange {
                                ctx.alignInputCount += 1
                                ctx.alignInputText = "C:${it.text}"
                            }
                        }
                    }
                    Input {
                        attr {
                            flex(1f)
                            height(40f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("右对齐")
                            placeholderColor(Color.GRAY)
                            textAlignRight()
                        }
                        event {
                            textDidChange {
                                ctx.alignInputCount += 1
                                ctx.alignInputText = "R:${it.text}"
                            }
                        }
                    }
                }
                CallbackTip { "总回调次数=${ctx.alignInputCount} 最近='${ctx.alignInputText}'" }

                // ===== ③ password =====
                SectionTitle("③ keyboardType=password")
                Input {
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("密码")
                        placeholderColor(Color.GRAY)
                        keyboardTypePassword()
                    }
                    event {
                        textDidChange {
                            ctx.passwordInputCount += 1
                            ctx.passwordInputText = it.text
                        }
                    }
                }
                CallbackTip { "password 回调次数=${ctx.passwordInputCount} 最近长度=${ctx.passwordInputText.length}" }

                // ===== ④ number =====
                SectionTitle("④ keyboardType=number（仅数字）")
                Input {
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("仅数字")
                        placeholderColor(Color.GRAY)
                        keyboardTypeNumber()
                    }
                    event {
                        textDidChange {
                            ctx.numberInputCount += 1
                            ctx.numberInputText = it.text
                        }
                    }
                }
                CallbackTip { "number 回调次数=${ctx.numberInputCount} 最近='${ctx.numberInputText}'" }

                // ===== ⑤ maxTextLength =====
                SectionTitle("⑤ maxTextLength=6（CHARACTER）+ textLengthBeyondLimit")
                Input {
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("最多 6 个字符")
                        placeholderColor(Color.GRAY)
                        maxTextLength(6, LengthLimitType.CHARACTER)
                    }
                    event {
                        textDidChange {
                            ctx.maxLenInputCount += 1
                            ctx.maxLenInputText = it.text
                        }
                        textLengthBeyondLimit {
                            ctx.maxLenBeyondCount += 1
                            KLog.i(TAG, "maxLen textLengthBeyondLimit #${ctx.maxLenBeyondCount}")
                        }
                    }
                }
                CallbackTip {
                    "回调次数=${ctx.maxLenInputCount} 超限次数=${ctx.maxLenBeyondCount} " +
                        "最近='${ctx.maxLenInputText}'(len=${ctx.maxLenInputText.length})"
                }

                // ===== ⑥ returnKeyType =====
                SectionTitle("⑥ returnKeyType=search + inputReturn 回调")
                Input {
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("按键盘 Search 键试试")
                        placeholderColor(Color.GRAY)
                        returnKeyTypeSearch()
                    }
                    event {
                        textDidChange {
                            ctx.returnInputText = it.text
                        }
                        inputReturn {
                            ctx.returnCount += 1
                            ctx.lastImeAction = it.imeAction ?: ""
                            KLog.i(TAG, "inputReturn #${ctx.returnCount} imeAction=${it.imeAction} text='${it.text}'")
                        }
                    }
                }
                CallbackTip {
                    "return 次数=${ctx.returnCount} imeAction=${ctx.lastImeAction} 最近值='${ctx.returnInputText}'"
                }

                // ===== ⑦ editable=false =====
                SectionTitle("⑦ editable=false（不可编辑，仅展示）")
                Input {
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color(0xFF999999L))
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("不可编辑")
                        placeholderColor(Color.GRAY)
                        text("readonly-text")
                        editable(false)
                    }
                }

                // ============================================================
                // ========== Part II：TextArea（多行） ======================
                // ============================================================
                SectionDivider("Part II · TextArea（多行）")

                // ===== ⑧ 基础样式 + focus/blur + cursor =====
                SectionTitle("⑧ 基础样式（带初始多行文本）")
                TextArea {
                    ref { ctx.areaBasicRef = it }
                    attr {
                        height(100f)
                        marginTop(6f)
                        fontSize(16f)
                        fontWeightMedium()
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("请输入多行内容…")
                        placeholderColor(Color.GRAY)
                        text("line1\nline2")
                        tintColor(Color(0xFF3C6CBDL))
                    }
                    event {
                        textDidChange {
                            ctx.areaBasicCount += 1
                            ctx.areaBasicText = it.text
                        }
                        inputFocus {
                            ctx.areaBasicFocusState = "focus"
                        }
                        inputBlur {
                            ctx.areaBasicFocusState = "blur"
                        }
                    }
                }
                CallbackTip {
                    "回调次数=${ctx.areaBasicCount} 焦点=${ctx.areaBasicFocusState} " +
                        "cursor=${ctx.areaBasicCursor} 最近='${ctx.areaBasicText}'"
                }
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    SmallButton("setText") {
                        ctx.areaBasicRef.view?.setText("line-a\nline-b\nline-c")
                    }
                    SmallButton("清空") {
                        ctx.areaBasicRef.view?.setText("")
                    }
                    SmallButton("focus") {
                        ctx.areaBasicRef.view?.focus()
                    }
                    SmallButton("blur") {
                        ctx.areaBasicRef.view?.blur()
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    SmallButton("cursor?") {
                        ctx.areaBasicRef.view?.cursorIndex { idx ->
                            ctx.areaBasicCursor = idx
                            KLog.i(TAG, "area cursorIndex=$idx")
                        }
                    }
                    SmallButton("setCursor=3") {
                        ctx.areaBasicRef.view?.setCursorIndex(3)
                    }
                    SmallButton("setCursor=0") {
                        ctx.areaBasicRef.view?.setCursorIndex(0)
                    }
                    View { attr { flex(1f) } }
                }

                // ===== ⑩ TextArea textAlign 三态（多行） =====
                SectionTitle("⑨ textAlign 左/中/右（多行）")
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    TextArea {
                        attr {
                            flex(1f)
                            height(70f)
                            marginRight(6f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("左")
                            placeholderColor(Color.GRAY)
                            text("行1\n行2")
                            textAlignLeft()
                        }
                        event {
                            textDidChange {
                                ctx.areaAlignCount += 1
                                ctx.areaAlignText = "L:${it.text}"
                            }
                        }
                    }
                    TextArea {
                        attr {
                            flex(1f)
                            height(70f)
                            marginRight(6f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("中")
                            placeholderColor(Color.GRAY)
                            text("行1\n行2")
                            textAlignCenter()
                        }
                        event {
                            textDidChange {
                                ctx.areaAlignCount += 1
                                ctx.areaAlignText = "C:${it.text}"
                            }
                        }
                    }
                    TextArea {
                        attr {
                            flex(1f)
                            height(70f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            placeholder("右")
                            placeholderColor(Color.GRAY)
                            text("行1\n行2")
                            textAlignRight()
                        }
                        event {
                            textDidChange {
                                ctx.areaAlignCount += 1
                                ctx.areaAlignText = "R:${it.text}"
                            }
                        }
                    }
                }
                CallbackTip { "总回调次数=${ctx.areaAlignCount} 最近='${ctx.areaAlignText}'" }

                // ===== ⑪ TextArea keyboardType=number =====
                SectionTitle("⑩ keyboardType=number（多行 + 数字键盘）")
                TextArea {
                    attr {
                        height(70f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("仅数字（多行）")
                        placeholderColor(Color.GRAY)
                        keyboardTypeNumber()
                    }
                    event {
                        textDidChange {
                            ctx.areaNumberCount += 1
                            ctx.areaNumberText = it.text
                        }
                    }
                }
                CallbackTip { "number 回调次数=${ctx.areaNumberCount} 最近='${ctx.areaNumberText}'" }

                // ===== ⑫ TextArea returnKeyType=send + inputReturn =====
                SectionTitle("⑪ returnKeyType=send + inputReturn（多行回车语义）")
                TextArea {
                    attr {
                        height(80f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("多行下回车：换行 or 触发 Send？")
                        placeholderColor(Color.GRAY)
                        returnKeyTypeSend()
                    }
                    event {
                        textDidChange {
                            ctx.areaReturnText = it.text
                        }
                        inputReturn {
                            ctx.areaReturnCount += 1
                            ctx.areaReturnImeAction = it.imeAction ?: ""
                            KLog.i(
                                TAG,
                                "area inputReturn #${ctx.areaReturnCount} imeAction=${it.imeAction} text='${it.text}'"
                            )
                        }
                    }
                }
                CallbackTip {
                    "return 次数=${ctx.areaReturnCount} imeAction=${ctx.areaReturnImeAction} " +
                        "最近值='${ctx.areaReturnText}'"
                }

                // ===== ⑬ TextArea maxTextLength VISUAL_WIDTH =====
                SectionTitle("⑫ maxTextLength=10（VISUAL_WIDTH）+ textLengthBeyondLimit")
                TextArea {
                    attr {
                        height(80f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("最多视觉宽度 10（中文2、英文1）")
                        placeholderColor(Color.GRAY)
                        maxTextLength(10, LengthLimitType.VISUAL_WIDTH)
                    }
                    event {
                        textDidChange {
                            ctx.areaLimitedCount += 1
                            ctx.areaLimitedText = it.text
                        }
                        textLengthBeyondLimit {
                            ctx.areaLimitedBeyondCount += 1
                            KLog.i(TAG, "area textLengthBeyondLimit #${ctx.areaLimitedBeyondCount}")
                        }
                    }
                }
                CallbackTip {
                    "回调次数=${ctx.areaLimitedCount} 超限次数=${ctx.areaLimitedBeyondCount} " +
                        "最近='${ctx.areaLimitedText}'(len=${ctx.areaLimitedText.length})"
                }

                // ===== ⑭ TextArea editable=false =====
                SectionTitle("⑬ editable=false（多行只读）")
                TextArea {
                    attr {
                        height(70f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color(0xFF999999L))
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("不可编辑")
                        placeholderColor(Color.GRAY)
                        text("readonly-line1\nreadonly-line2\nreadonly-line3")
                        editable(false)
                    }
                }

                // ===== ⑮ TextArea lines + lineHeight =====
                SectionTitle("⑭ lines=4 + lineHeight=26（TextArea 独有样式）")
                TextArea {
                    attr {
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("设置可视行数=4，超出应可滚动")
                        placeholderColor(Color.GRAY)
                        text("line1\nline2\nline3\nline4\nline5\nline6")
                        lines(4)
                        lineHeight(26f)
                    }
                }

                // ===== ⑮ TextArea inputSpans（富文本） =====
                SectionTitle("⑮ inputSpans 富文本（初始混排，新控件若不支持会暴露问题）")
                TextArea {
                    attr {
                        height(90f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("预期展示：红色粗体 + 黑色默认 + 蓝色中号")
                        placeholderColor(Color.GRAY)
                        inputSpans(ctx.areaSpans)
                    }
                    event {
                        textDidChange {
                            ctx.areaSpansCount += 1
                        }
                    }
                }
                CallbackTip {
                    "inputSpans textDidChange 次数=${ctx.areaSpansCount}（输入时应保持富文本样式）"
                }
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(6f)
                    }
                    SmallButton("重置 spans") {
                        ctx.areaSpans = initialRichSpans()
                    }
                    SmallButton("追加一段绿色") {
                        ctx.areaSpans = InputSpans().apply {
                            // 重建完整 spans：原 3 段 + 追加绿色段
                            addSpan(InputSpan().apply {
                                text("红色粗体 ")
                                color(Color.RED)
                                fontWeightBold()
                            })
                            addSpan(InputSpan().apply {
                                text("默认 ")
                                color(Color.BLACK)
                            })
                            addSpan(InputSpan().apply {
                                text("蓝色中号 ")
                                color(Color(0xFF3C6CBDL))
                                fontWeightMedium()
                                fontSize(18f)
                            })
                            addSpan(InputSpan().apply {
                                text("[追加绿色]")
                                color(Color(0xFF2EAD4CL))
                                fontWeightBold()
                            })
                        }
                    }
                    View { attr { flex(2f) } }
                }

                // ===== ⑱ TextArea 自适应高度 / 固定高度内部滚动 =====
                SectionTitle("⑯ 自适应高度 vs 固定高度内部滚动（TextArea 独有 MeasureFunction）")
                // 左：不给 height → 内容自动撑高（可配合 minHeight 保底）
                View {
                    attr {
                        marginTop(6f)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666L))
                            value("左图：不设置 height，应随内容自适应（minHeight=40 保底）")
                        }
                    }
                    TextArea {
                        attr {
                            marginTop(4f)
                            minHeight(40f)
                            fontSize(16f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFEAF3FFL))
                            placeholder("输入/回车换行时高度应自动增长")
                            placeholderColor(Color.GRAY)
                            text("auto\nheight")
                        }
                        event {
                            textDidChange {
                                ctx.areaAutoHeightCount += 1
                                ctx.areaAutoHeightText = it.text
                            }
                        }
                    }
                    CallbackTip {
                        "自适应 回调次数=${ctx.areaAutoHeightCount} " +
                            "最近行数=${ctx.areaAutoHeightText.count { c -> c == '\n' } + 1}"
                    }
                }
                // 右：固定 height=60 + 10 行内容 → 内部滚动
                View {
                    attr {
                        marginTop(10f)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(Color(0xFF666666L))
                            value("下图：固定 height=60，10 行初始内容，应在控件内部滚动")
                        }
                    }
                    TextArea {
                        attr {
                            marginTop(4f)
                            height(60f)
                            fontSize(16f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFFFF5E6L))
                            placeholder("固定高度内部滚动")
                            placeholderColor(Color.GRAY)
                            text(
                                "row-1\nrow-2\nrow-3\nrow-4\nrow-5\n" +
                                    "row-6\nrow-7\nrow-8\nrow-9\nrow-10"
                            )
                        }
                    }
                }

                // 底部留一点空白，便于滚动到最后一条内容下方。
                // List 整体高度已通过 keyboardHeight 动态收缩，这里不再需要键盘占位。
                View {
                    attr {
                        height(24f)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "InputCompareDemoPage"
        const val PARAM_USE_TEXT_EDITOR_IMPL = "useTextEditorImpl"
    }
}

/**
 * 构造 ⑮ 的初始富文本 spans：红色粗体 + 黑色默认 + 蓝色中号。
 *
 * 放到文件顶层私有函数，避免在 `click { ... }` 闭包内隐式 receiver 作用域
 * 无法解析到类内方法的问题（Kotlin 编译报 'cannot be called in this context
 * with an implicit receiver'）。
 */
private fun initialRichSpans(): InputSpans = InputSpans().apply {
    addSpan(InputSpan().apply {
        text("红色粗体 ")
        color(Color.RED)
        fontWeightBold()
    })
    addSpan(InputSpan().apply {
        text("默认 ")
        color(Color.BLACK)
    })
    addSpan(InputSpan().apply {
        text("蓝色中号 ")
        color(Color(0xFF3C6CBDL))
        fontWeightMedium()
        fontSize(18f)
    })
}

/**
 * 分区分隔条（Part I / Part II 大标题）。
 */
private fun ViewContainer<*, *>.SectionDivider(title: String) {
    View {
        attr {
            marginTop(18f)
            marginBottom(4f)
            height(28f)
            backgroundColor(Color(0xFF3C6CBDL))
            borderRadius(4f)
            allCenter()
            paddingLeft(10f)
            paddingRight(10f)
        }
        Text {
            attr {
                fontSize(14f)
                fontWeightBold()
                color(Color.WHITE)
                value(title)
            }
        }
    }
}

/**
 * 小标题
 */
private fun ViewContainer<*, *>.SectionTitle(title: String) {
    Text {
        attr {
            fontSize(14f)
            fontWeightBold()
            color(Color.BLACK)
            marginTop(14f)
            value(title)
        }
    }
}

/**
 * 内嵌 tip 文本。
 *
 * 注意：参数必须是 `() -> String` 而不能是 `String`，否则调用点传入的字符串
 * 只是 `body()` 构建阶段的一次性快照，后续 observable 字段变化时无法驱动
 * 文本刷新。包成 lambda 后，真正在 `attr { value(tipProvider()) }` 里求值，
 * 才能被响应式系统收集到依赖并在变化时重新执行。
 */
private fun ViewContainer<*, *>.CallbackTip(tipProvider: () -> String) {
    Text {
        attr {
            fontSize(12f)
            color(Color(0xFF888888L))
            marginTop(4f)
            value(tipProvider())
        }
    }
}

/**
 * 小按钮
 */
private fun ViewContainer<*, *>.SmallButton(label: String, onClick: () -> Unit) {
    View {
        attr {
            flex(1f)
            height(32f)
            marginRight(6f)
            backgroundColor(Color(0xFF3C6CBDL))
            borderRadius(4f)
            allCenter()
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                fontSize(12f)
                color(Color.WHITE)
                value(label)
            }
        }
    }
}
