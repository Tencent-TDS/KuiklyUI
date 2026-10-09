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
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.TextInputState
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

private const val TAG = "BD_IOS_TEXT_LIMIT"

/** 业务实际长度上限 */
private const val BUSINESS_MAX_LENGTH = 140

/** 业务场景光标测试位置，位于文本中部 */
private const val BUSINESS_CURSOR_INDEX = 68

/** 小样本对照组的初始文本，长度等于上限，避免启动即截断 */
private const val STRESS_INITIAL_TEXT = "123456"

/** 小样本对照组的长度上限 */
private const val STRESS_MAX_LENGTH = 6

/** 小样本对照组光标测试位置 */
private const val STRESS_CURSOR_INDEX = 5

/** 可见性对照组的文本长度上限 */
private const val VISIBILITY_MAX_LENGTH = 30

/** 可见性对照组光标测试位置 */
private const val VISIBILITY_CURSOR_INDEX = 15

/** 可见性对照组宽输入框宽度 */
private const val VISIBILITY_WIDE_WIDTH = 300f

/** 可见性对照组窄输入框宽度 */
private const val VISIBILITY_NARROW_WIDTH = 96f

/** 业务同款窄输入框宽度，用于复刻业务可见性现象 */
private const val BUSINESS_NARROW_WIDTH = 160f

/**
 * 业务场景真实文案，长度等于 [BUSINESS_MAX_LENGTH]。
 * 用于替代重复字符，贴近线上真实输入内容。
 */
private const val BUSINESS_REAL_TEXT =
    "这是一个用于验证输入框长度上限与光标行为的测试文案，内容尽量贴近线上真实的用户搜索输入场景，" +
            "包含常见词语、短句以及连续输入的情况，方便观察在达到一百四十字上限之后继续输入时光标的位置变化与文本滚动表现，" +
            "也能验证框架在超限截断后是否会把光标正确滚动回可视区域，并据此完成修复验证工作"

/**
 * iOS 输入框达到长度上限后继续输入导致光标跳到末尾的复现页。
 *
 * 业务原始配置见 createBusinessInput；对照组用小样本快速触发同一条
 * p_limitTextInput 分支。
 */
@Page("BugReproIosInputCursorJumpPage")
internal class BugReproIosInputCursorJumpPage : BasePager() {
    lateinit var businessInputRef: ViewRef<InputView>
    lateinit var stressInputRef: ViewRef<InputView>
    lateinit var wideInputRef: ViewRef<InputView>
    lateinit var narrowInputRef: ViewRef<InputView>
    lateinit var narrowBusinessInputRef: ViewRef<InputView>
    lateinit var multiLineInputRef: ViewRef<TextAreaView>

    var businessSummary by observable("尚未收到业务输入框状态")
    var stressSummary by observable("尚未收到对照输入框状态")
    var wideSummary by observable("尚未收到宽输入框状态")
    var narrowSummary by observable("尚未收到窄输入框状态")
    var narrowBusinessSummary by observable("尚未收到业务窄输入框状态")
    var multiLineSummary by observable("尚未收到多行输入框状态")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(0xFF101427L))
            }

            View {
                attr {
                    height(56f)
                    backgroundColor(Color(0xFF1B2540L))
                    allCenter()
                }
                Text {
                    attr {
                        text("iOS 输入超限光标跳变复现")
                        color(Color.WHITE)
                        fontSize(17f)
                        fontWeightBold()
                    }
                }
            }

            List {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        padding(all = 12f)
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(14f)
                            fontWeightBold()
                            color(Color(0xFFE2EAFFL))
                            text("复现目标")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(12f)
                            lineHeight(18f)
                            color(Color(0xFF9AA6C8L))
                            text(
                                "业务配置 maxTextLength(140)。填入文本后把光标放到中间，" +
                                        "继续输入或粘贴内容触发超限，观察光标是否跳到文本末尾。"
                            )
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(Color(0xFFFF8A80L))
                            text("日志过滤前缀：BD_IOS_TEXT_LIMIT")
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 1 业务配置 maxTextLength(140)")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        Input {
                            ref {
                                ctx.businessInputRef = it
                            }
                            attr {
                                flex(1f)
                                height(30f)
                                marginLeft(8f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("测试123123")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(BUSINESS_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[Business][textDidChange] length=${it.length}")
                                    ctx.businessInputRef.view?.getTextInputState { state ->
                                        ctx.recordBusinessState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordBusinessState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordBusinessState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[Business][textLengthBeyondLimit] payload=$it")
                                    ctx.readBusinessState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.businessSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充 140 字中文")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Business][action] fillText")
                                    ctx.businessInputRef.view?.setText(
                                        buildRepeatText("中", BUSINESS_MAX_LENGTH)
                                    )
                                    ctx.businessInputRef.view?.focus()
                                    ctx.readBusinessState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $BUSINESS_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Business][action] setCursorIndex($BUSINESS_CURSOR_INDEX)")
                                    ctx.businessInputRef.view?.setCursorIndex(BUSINESS_CURSOR_INDEX)
                                    ctx.businessInputRef.view?.focus()
                                    ctx.readBusinessState("afterSetCursor")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF667085L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("读取状态")
                                }
                            }
                            event {
                                click {
                                    ctx.readBusinessState("manualRead")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFFB42318L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("清空")
                                }
                            }
                            event {
                                click {
                                    ctx.businessInputRef.view?.setText("")
                                    ctx.readBusinessState("afterClear")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 2 对照 maxTextLength($STRESS_MAX_LENGTH) 手动输入")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text("先填充 ${STRESS_MAX_LENGTH} 个中文，再把光标放到 $STRESS_CURSOR_INDEX，输入或粘贴任意内容即可触发超限。")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        Input {
                            ref {
                                ctx.stressInputRef = it
                            }
                            attr {
                                flex(1f)
                                height(30f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("对照输入框")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(STRESS_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[Stress][textDidChange] text='${it.text}' length=${it.length}")
                                    ctx.stressInputRef.view?.getTextInputState { state ->
                                        ctx.recordStressState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordStressState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordStressState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[Stress][textLengthBeyondLimit] payload=$it")
                                    ctx.readStressState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.stressSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充 ${STRESS_MAX_LENGTH} 个中文")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Stress][action] fillText")
                                    ctx.stressInputRef.view?.setText(
                                        buildRepeatText("中", STRESS_MAX_LENGTH)
                                    )
                                    ctx.stressInputRef.view?.focus()
                                    ctx.readStressState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $STRESS_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Stress][action] setCursorIndex($STRESS_CURSOR_INDEX)")
                                    ctx.stressInputRef.view?.setCursorIndex(STRESS_CURSOR_INDEX)
                                    ctx.stressInputRef.view?.focus()
                                    ctx.readStressState("afterSetCursor")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF667085L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("读取状态")
                                }
                            }
                            event {
                                click {
                                    ctx.readStressState("manualRead")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFFB42318L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("重置为 ${STRESS_INITIAL_TEXT}")
                                }
                            }
                            event {
                                click {
                                    ctx.stressInputRef.view?.setText(STRESS_INITIAL_TEXT)
                                    ctx.readStressState("afterReset")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 3 宽输入框 maxTextLength($VISIBILITY_MAX_LENGTH) 光标 $VISIBILITY_CURSOR_INDEX")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text("填充 ${VISIBILITY_MAX_LENGTH} 个中文后把光标放到 $VISIBILITY_CURSOR_INDEX，输入一个字符，观察光标是否保持可见并停在 $VISIBILITY_CURSOR_INDEX。")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        Input {
                            ref {
                                ctx.wideInputRef = it
                            }
                            attr {
                                width(VISIBILITY_WIDE_WIDTH)
                                height(30f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("宽输入框对照")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(VISIBILITY_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[Wide][textDidChange] length=${it.length}")
                                    ctx.wideInputRef.view?.getTextInputState { state ->
                                        ctx.recordWideState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordWideState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordWideState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[Wide][textLengthBeyondLimit] payload=$it")
                                    ctx.readWideState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.wideSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充 ${VISIBILITY_MAX_LENGTH} 个中文")
                                }
                            }
                            event {
                                click {
                                    ctx.wideInputRef.view?.setText(buildRepeatText("中", VISIBILITY_MAX_LENGTH))
                                    ctx.wideInputRef.view?.focus()
                                    ctx.readWideState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $VISIBILITY_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Wide][action] setCursorIndex($VISIBILITY_CURSOR_INDEX)")
                                    ctx.wideInputRef.view?.setCursorIndex(VISIBILITY_CURSOR_INDEX)
                                    ctx.wideInputRef.view?.focus()
                                    ctx.readWideState("afterSetCursor")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 4 窄输入框 maxTextLength($VISIBILITY_MAX_LENGTH) 光标 $VISIBILITY_CURSOR_INDEX")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text("与 Case 3 文本与光标完全一致，只把输入框宽度改小。若这里光标看不见而 Case 3 可见，即可确认是横向滚动未跟随导致光标被滚出可视区。")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        Input {
                            ref {
                                ctx.narrowInputRef = it
                            }
                            attr {
                                width(VISIBILITY_NARROW_WIDTH)
                                height(30f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("窄输入框对照")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(VISIBILITY_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[Narrow][textDidChange] length=${it.length}")
                                    ctx.narrowInputRef.view?.getTextInputState { state ->
                                        ctx.recordNarrowState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordNarrowState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordNarrowState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[Narrow][textLengthBeyondLimit] payload=$it")
                                    ctx.readNarrowState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.narrowSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充 ${VISIBILITY_MAX_LENGTH} 个中文")
                                }
                            }
                            event {
                                click {
                                    ctx.narrowInputRef.view?.setText(buildRepeatText("中", VISIBILITY_MAX_LENGTH))
                                    ctx.narrowInputRef.view?.focus()
                                    ctx.readNarrowState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $VISIBILITY_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[Narrow][action] setCursorIndex($VISIBILITY_CURSOR_INDEX)")
                                    ctx.narrowInputRef.view?.setCursorIndex(VISIBILITY_CURSOR_INDEX)
                                    ctx.narrowInputRef.view?.focus()
                                    ctx.readNarrowState("afterSetCursor")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 5 业务同款窄输入框 maxTextLength(140) 光标 68")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text("宽度 ${BUSINESS_NARROW_WIDTH.toInt()} 复刻业务单行窄输入框，填充一段 ${BUSINESS_MAX_LENGTH} 字真实文案后把光标放到 $BUSINESS_CURSOR_INDEX，输入一个字符，观察光标是否看不见。")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        Input {
                            ref {
                                ctx.narrowBusinessInputRef = it
                            }
                            attr {
                                width(BUSINESS_NARROW_WIDTH)
                                height(30f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("测试123123")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(BUSINESS_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[NarrowBusiness][textDidChange] length=${it.length}")
                                    ctx.narrowBusinessInputRef.view?.getTextInputState { state ->
                                        ctx.recordNarrowBusinessState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordNarrowBusinessState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordNarrowBusinessState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[NarrowBusiness][textLengthBeyondLimit] payload=$it")
                                    ctx.readNarrowBusinessState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.narrowBusinessSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充真实文案")
                                }
                            }
                            event {
                                click {
                                    KLog.i(
                                        TAG,
                                        "[NarrowBusiness][action] fillRealText length=${BUSINESS_REAL_TEXT.length}"
                                    )
                                    ctx.narrowBusinessInputRef.view?.setText(BUSINESS_REAL_TEXT)
                                    ctx.narrowBusinessInputRef.view?.focus()
                                    ctx.readNarrowBusinessState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $BUSINESS_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[NarrowBusiness][action] setCursorIndex($BUSINESS_CURSOR_INDEX)")
                                    ctx.narrowBusinessInputRef.view?.setCursorIndex(BUSINESS_CURSOR_INDEX)
                                    ctx.narrowBusinessInputRef.view?.focus()
                                    ctx.readNarrowBusinessState("afterSetCursor")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(left = 12f, right = 12f, bottom = 12f)
                        padding(all = 12f)
                        borderRadius(8f)
                        backgroundColor(Color(0xFF1B2540L))
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(15f)
                            fontWeightBold()
                            color(Color.WHITE)
                            text("Case 6 多行输入框 maxTextLength($BUSINESS_MAX_LENGTH) 光标 $BUSINESS_CURSOR_INDEX")
                        }
                    }
                    Text {
                        attr {
                            marginTop(6f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text("与 Case 5 同样的文本与光标，只把单行 Input 换成多行 TextArea。用于对比多行走的是 KRTextAreaView，观察拼音是否可见、光标是否移动。")
                        }
                    }
                    View {
                        attr {
                            marginTop(10f)
                            flexDirectionRow()
                        }
                        TextArea {
                            ref {
                                ctx.multiLineInputRef = it
                            }
                            attr {
                                flex(1f)
                                height(80f)
                                fontSize(14f)
                                useDpFontSizeDim(true)
                                color(Color(0xFFE2EAFFL, 0.8f))
                                placeholder("测试123123")
                                placeholderColor(Color(0xFFE2EAFFL, 0.4f))
                                autofocus(false)
                                maxTextLength(BUSINESS_MAX_LENGTH)
                                returnKeyTypeSearch()
                            }
                            event {
                                textDidChange {
                                    KLog.i(TAG, "[MultiLine][textDidChange] length=${it.length}")
                                    ctx.multiLineInputRef.view?.getTextInputState { state ->
                                        ctx.recordMultiLineState("textDidChangeState", state)
                                    }
                                }
                                textInputStateChange {
                                    ctx.recordMultiLineState("textInputStateChange", it)
                                }
                                selectionChange {
                                    ctx.recordMultiLineState("selectionChange", it)
                                }
                                textLengthBeyondLimit {
                                    KLog.i(TAG, "[MultiLine][textLengthBeyondLimit] payload=$it")
                                    ctx.readMultiLineState("afterTextLengthBeyondLimit")
                                }
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(11f)
                            lineHeight(16f)
                            color(Color(0xFF9AA6C8L))
                            text(ctx.multiLineSummary)
                        }
                    }
                    View {
                        attr {
                            marginTop(8f)
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF1677FFL))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("填充真实文案")
                                }
                            }
                            event {
                                click {
                                    KLog.i(
                                        TAG,
                                        "[MultiLine][action] fillRealText length=${BUSINESS_REAL_TEXT.length}"
                                    )
                                    ctx.multiLineInputRef.view?.setText(BUSINESS_REAL_TEXT)
                                    ctx.multiLineInputRef.view?.focus()
                                    ctx.readMultiLineState("afterFill")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF2E7D32L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("光标设为 $BUSINESS_CURSOR_INDEX 并聚焦")
                                }
                            }
                            event {
                                click {
                                    KLog.i(TAG, "[MultiLine][action] setCursorIndex($BUSINESS_CURSOR_INDEX)")
                                    ctx.multiLineInputRef.view?.setCursorIndex(BUSINESS_CURSOR_INDEX)
                                    ctx.multiLineInputRef.view?.focus()
                                    ctx.readMultiLineState("afterSetCursor")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(right = 8f, bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFF667085L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("读取状态")
                                }
                            }
                            event {
                                click {
                                    ctx.readMultiLineState("manualRead")
                                }
                            }
                        }
                        View {
                            attr {
                                margin(bottom = 8f)
                                padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                                borderRadius(4f)
                                backgroundColor(Color(0xFFB42318L))
                            }
                            Text {
                                attr {
                                    fontSize(12f)
                                    color(Color.WHITE)
                                    text("清空")
                                }
                            }
                            event {
                                click {
                                    ctx.multiLineInputRef.view?.setText("")
                                    ctx.readMultiLineState("afterClear")
                                }
                            }
                        }
                    }
                }

                View {
                    attr {
                        padding(all = 12f)
                        flexDirectionColumn()
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            lineHeight(18f)
                            color(Color(0xFF9AA6C8L))
                            text("判定：日志中 uiSelectionAfterAssign 若等于文本长度，说明赋值 attributedText 后 UIKit 会先把选区放到末尾再被纠正；caretVisible 的 caretX 若超出 fieldWidth，说明光标被滚出可视区。")
                        }
                    }
                }
            }
        }
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // 对照组用 setText 初始化一次，避免响应式 text 属性把输入重置
        stressInputRef.view?.setText(STRESS_INITIAL_TEXT)
        readStressState("afterInit")
    }

    private fun recordBusinessState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[Business] $summary")
        businessSummary = summary
    }

    private fun recordStressState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[Stress] $summary")
        stressSummary = summary
    }

    private fun readBusinessState(source: String) {
        businessInputRef.view?.getTextInputState { state ->
            recordBusinessState(source, state)
        }
    }

    private fun readStressState(source: String) {
        stressInputRef.view?.getTextInputState { state ->
            recordStressState(source, state)
        }
    }

    private fun recordWideState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[Wide] $summary")
        wideSummary = summary
    }

    private fun recordNarrowState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[Narrow] $summary")
        narrowSummary = summary
    }

    private fun readWideState(source: String) {
        wideInputRef.view?.getTextInputState { state ->
            recordWideState(source, state)
        }
    }

    private fun readNarrowState(source: String) {
        narrowInputRef.view?.getTextInputState { state ->
            recordNarrowState(source, state)
        }
    }

    private fun recordNarrowBusinessState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[NarrowBusiness] $summary")
        narrowBusinessSummary = summary
    }

    private fun readNarrowBusinessState(source: String) {
        narrowBusinessInputRef.view?.getTextInputState { state ->
            recordNarrowBusinessState(source, state)
        }
    }

    private fun recordMultiLineState(source: String, state: TextInputState) {
        val summary = formatState(source, state)
        KLog.i(TAG, "[MultiLine] $summary")
        multiLineSummary = summary
    }

    private fun readMultiLineState(source: String) {
        multiLineInputRef.view?.getTextInputState { state ->
            recordMultiLineState(source, state)
        }
    }
}

private fun buildRepeatText(unit: String, count: Int): String {
    val builder = StringBuilder()
    repeat(count) {
        builder.append(unit)
    }
    return builder.toString()
}

private fun formatState(source: String, state: TextInputState): String {
    val preview = if (state.text.length > 24) state.text.take(24) + "…" else state.text
    return "$source textLength=${state.text.length} text='$preview' " +
            "selection=${state.selectionStart}-${state.selectionEnd} " +
            "composition=${state.compositionStart}-${state.compositionEnd} length=${state.length}"
}
