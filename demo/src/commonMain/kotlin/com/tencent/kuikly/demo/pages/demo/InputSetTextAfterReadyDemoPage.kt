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
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 用于验证「ARKUI_NODE_TEXT_EDITOR 在 ready 后再次 setText 是否触发 textDidChange」的 Demo。
 *
 * 新的 KRTextEditorFieldView / KRTextEditorAreaView（OHOS API 24+）实现中，
 * NODE_TEXT_EDITOR_ON_READY 首次到达时会补发一次 textDidChange，对齐老 KRTextFieldView 行为。
 * 对 ready 之后的 callMethod("setText") 能否触发 textDidChange，此前有遗留跟进项，
 * 本页面提供真机验证入口。
 *
 * 操作步骤：
 *  1. 页面加载时，第一个 Input 带有初始文字 "initial-text"，应打印一次 textDidChange（初始化补发）。
 *  2. 第二个 Input 初始为空，不应打印 textDidChange。
 *  3. 3 秒后，页面自动对两个输入框调用 setText("ready+3s ...")：
 *     - 期望两个 Input 都触发 textDidChange；若没触发，说明 ready 后 setText 的行为尚未补齐。
 *  4. 点击按钮可以手动再次 setText / 清空，UI 中的 "回调次数" 会实时变化。
 */
@Page("InputSetTextAfterReadyDemoPage")
internal class InputSetTextAfterReadyDemoPage : BasePager() {

    private lateinit var initialInputRef: ViewRef<InputView>
    private lateinit var emptyInputRef: ViewRef<InputView>
    private lateinit var areaRef: ViewRef<TextAreaView>

    private var initialChangeCount: Int by observable(0)
    private var emptyChangeCount: Int by observable(0)
    private var areaChangeCount: Int by observable(0)

    private var initialLatest: String by observable("")
    private var emptyLatest: String by observable("")
    private var areaLatest: String by observable("")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = "setText after ready"
                }
            }

            View {
                attr {
                    flex(1f)
                    padding(12f)
                }

                // ---- Section 1：初始带文字，期望 ready 后立刻补发一次 textDidChange ----
                Text {
                    attr {
                        fontSize(14f)
                        fontWeightBold()
                        color(Color.BLACK)
                        marginTop(4f)
                        value("① 初始带文字的 Input（期望首次加载补发 1 次 textDidChange）")
                    }
                }
                Input {
                    ref { ctx.initialInputRef = it }
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("initial…")
                        placeholderColor(Color.GRAY)
                        text("initial-text")
                    }
                    event {
                        textDidChange {
                            ctx.initialChangeCount += 1
                            ctx.initialLatest = it.text
                            KLog.i(TAG, "initialInput textDidChange #${ctx.initialChangeCount}: '${it.text}'")
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(12f)
                        color(Color.GRAY)
                        marginTop(4f)
                        value("回调次数=${ctx.initialChangeCount}  最近值='${ctx.initialLatest}'")
                    }
                }

                // ---- Section 2：初始为空，仅验证 ready 之后主动 setText 能否触发 ----
                Text {
                    attr {
                        fontSize(14f)
                        fontWeightBold()
                        color(Color.BLACK)
                        marginTop(18f)
                        value("② 初始为空的 Input（ready 后 3s 自动 setText，期望触发 1 次）")
                    }
                }
                Input {
                    ref { ctx.emptyInputRef = it }
                    attr {
                        height(44f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("empty…")
                        placeholderColor(Color.GRAY)
                    }
                    event {
                        textDidChange {
                            ctx.emptyChangeCount += 1
                            ctx.emptyLatest = it.text
                            KLog.i(TAG, "emptyInput textDidChange #${ctx.emptyChangeCount}: '${it.text}'")
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(12f)
                        color(Color.GRAY)
                        marginTop(4f)
                        value("回调次数=${ctx.emptyChangeCount}  最近值='${ctx.emptyLatest}'")
                    }
                }

                // ---- Section 3：多行 TextArea，同样校验 setText 行为 ----
                Text {
                    attr {
                        fontSize(14f)
                        fontWeightBold()
                        color(Color.BLACK)
                        marginTop(18f)
                        value("③ 多行 TextArea（初始带文字）")
                    }
                }
                TextArea {
                    ref { ctx.areaRef = it }
                    attr {
                        height(88f)
                        marginTop(6f)
                        fontSize(16f)
                        color(Color.BLACK)
                        backgroundColor(Color(0xFFF2F3F5L))
                        placeholder("multiline…")
                        placeholderColor(Color.GRAY)
                        text("line1\nline2")
                    }
                    event {
                        textDidChange {
                            ctx.areaChangeCount += 1
                            ctx.areaLatest = it.text
                            KLog.i(TAG, "textArea textDidChange #${ctx.areaChangeCount}: '${it.text}'")
                        }
                    }
                }
                Text {
                    attr {
                        fontSize(12f)
                        color(Color.GRAY)
                        marginTop(4f)
                        value("回调次数=${ctx.areaChangeCount}  最近值='${ctx.areaLatest}'")
                    }
                }

                // ---- 手动触发按钮区 ----
                View {
                    attr {
                        flexDirectionRow()
                        marginTop(24f)
                    }

                    // 按钮：全部 setText 为随机字符串
                    View {
                        attr {
                            flex(1f)
                            height(40f)
                            marginRight(8f)
                            backgroundColor(Color(0xFF3C6CBDL))
                            allCenter()
                            borderRadius(6f)
                        }
                        event {
                            click {
                                ctx.stampSeq += 1
                                val stamp = ctx.stampSeq
                                ctx.initialInputRef.view?.setText("set@$stamp")
                                ctx.emptyInputRef.view?.setText("set@$stamp")
                                ctx.areaRef.view?.setText("set@$stamp\nline2@$stamp")
                                KLog.i(TAG, "manual setText dispatched at $stamp")
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                value("手动 setText")
                            }
                        }
                    }

                    // 按钮：全部 setText 为空字符串
                    View {
                        attr {
                            flex(1f)
                            height(40f)
                            backgroundColor(Color(0xFF888888L))
                            allCenter()
                            borderRadius(6f)
                        }
                        event {
                            click {
                                ctx.initialInputRef.view?.setText("")
                                ctx.emptyInputRef.view?.setText("")
                                ctx.areaRef.view?.setText("")
                                KLog.i(TAG, "manual setText(empty) dispatched")
                            }
                        }
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color.WHITE)
                                value("清空文本")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // 3s 后主动触发一次 setText，用于验证「ready 之后再次 setText」是否能触发 textDidChange。
        setTimeout(pagerId, 3000) {
            stampSeq += 1
            val stamp = stampSeq
            initialInputRef.view?.setText("auto-$stamp")
            emptyInputRef.view?.setText("auto-$stamp")
            areaRef.view?.setText("auto-$stamp\nfrom-ready+3s")
            KLog.i(TAG, "auto setText after 3s: stamp=$stamp")
        }
    }

    // 单调递增计数，替代真实时间戳，避免引入 Clock 依赖
    internal var stampSeq = 0

    companion object {
        private const val TAG = "InputSetTextAfterReadyDemoPage"
    }
}
