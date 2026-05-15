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
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.KeyboardParams
import com.tencent.kuikly.core.views.LengthLimitType
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextArea
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Multi-line `TextArea` sibling of [InputViewDemoPage].
 *
 * Structure is intentionally kept in strict parity with [InputViewDemoPage] so the
 * two demos are visually/functionally comparable — differences are driven purely
 * by the DSL surface:
 *  - `TextArea { ... }` instead of `Input { ... }`
 *  - `lines(N)` for multi-line rendering
 *  - bigger height to show scrolling/wrapping behavior
 *
 * All recently landed TextArea-capabilities are exercised here, so the page also
 * doubles as a manual-QA surface on H5 / miniApp / iOS / Android:
 *  - `maxTextLength` + `textLengthBeyondLimit` (WX native <textarea> adapter path
 *    synthesizes a `beforeinput` via `bindinput` diff, see MiniTextAreaElement).
 *  - `keyboardHeightChange` (H5 dispatches via VisualViewport; miniApp bridges
 *    `bindkeyboardheightchange`).
 *  - `returnKeyType*` (H5 `enterkeyhint`; miniApp `confirm-type`).
 *  - `placeholder` / `placeholderColor` (miniApp: `placeholder-style` CSS path).
 *  - `tintColor` (caret-color on H5/miniApp).
 *  - `cursorIndex` / `setCursorIndex` (miniApp reads `selection-start`, writes
 *    `selection-start` / `selection-end`).
 */
@Page("TextAreaDemoPage")
internal class TextAreaDemoPage : BasePager() {
    lateinit var textAreaRef: ViewRef<TextAreaView>

    // Keyboard-avoidance state, identical pattern to InputViewDemoPage.
    var keyboardHeight: Float by observable(0f)
    var keyboardAnimation: Animation by observable(Animation.easeInOut(0.25f))

    // Latest cursor index read via `cursorIndex { ... }`. Rendered in the demo
    // panel below so you can visually verify that the async callback fired.
    // `-1` means "not yet queried".
    var lastCursorIndex: Int by observable(-1)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "TextArea组件Demo"
                }
            }

            View {
                attr {
                    flex(1f)
                }

                List {
                    attr {
                        height(200f)
                        backgroundColor(Color.BLUE)
                        flex(1f)
                    }
                    View {
                        attr {
                            height(150f)
                            backgroundColor(Color.BLACK)
                        }
                        event {
                            click { ctx.textAreaRef.view?.blur() }
                        }
                    }

                    View {
                        attr {
                            height(150f)
                            backgroundColor(Color.GREEN)
                        }
                        event {
                            click { ctx.textAreaRef.view?.blur() }
                        }
                    }

                    TextArea {
                        ref {
                            ctx.textAreaRef = it
                        }

                        attr {
                            margin(20f)
                            // Use the new two-arg overload so textLengthBeyondLimit can
                            // fire with the unified semantics across platforms. CHARACTER
                            // counts by unicode code points, matching "最多40字" semantics.
                            maxTextLength(40, LengthLimitType.CHARACTER)
                            // Make room for multi-line input. Real height is still
                            // driven by layout; `lines(N)` hints the IME / measure path.
                            height(160f)
                            lines(6)
                            fontSize(18f)
                            fontWeightNormal()

                            returnKeyTypeDone()
                            placeholder("请输入多行内容…（最多40字）")
                            placeholderColor(Color.YELLOW)

                            color(Color.BLACK)
                            autofocus(true)
                            backgroundColor(Color.RED)
                            // Drive the caret/cursor color. On H5 / mini-program this is
                            // translated to the CSS `caret-color` declaration on the
                            // underlying <textarea>; on iOS/Android it maps to the native
                            // text-area tint. Picking RED makes the caret stand out
                            // against the WHITE background.
                            tintColor(Color.YELLOW)
                        }

                        event {
                            textDidChange {
                                KLog.i("TextAreaDemoPage", "textDidChange$it")
                            }

                            inputBlur {
                                KLog.i("TextAreaDemoPage", "inputBlur$it")
                            }

                            inputFocus {
                                KLog.i("TextAreaDemoPage", "inputFocus$it")
                            }

                            keyboardHeightChange {
                                KLog.i(
                                    "TextAreaDemoPage",
                                    "keyboardHeightChange: height=${it.height}, duration=${it.duration}, curve=${it.curve}"
                                )
                                ctx.keyboardAnimation =
                                    this@TextAreaDemoPage.createKeyboardAnimation(it)
                                ctx.keyboardHeight = it.height
                            }

                            inputReturn {
                                // NOTE: on a multi-line <textarea>, Return often means
                                // "newline" rather than "submit"; whether this fires is
                                // platform-dependent and is a good thing to visually
                                // verify in the demo.
                                KLog.i("TextAreaDemoPage", "inputReturn$it")
                            }

                            textLengthBeyondLimit {
                                // Triggered when the user tries to type/paste a character
                                // that would exceed `maxTextLength(40, …)` set above. The
                                // payload carries the current (already-capped) text.
                                KLog.i("TextAreaDemoPage", "textLengthBeyondLimit$it")
                            }
                        }
                    }

                    // --- cursorIndex / setCursorIndex demo panel ---------------------------
                    // Two tappable rows plus a readout label. Tap "Get cursor" to ask the
                    // textarea for its caret position (async callback), tap
                    // "Set cursor -> 5" to programmatically move the caret to offset 5.
                    // The result of the latest read is rendered to the right as "cursor=N".
                    View {
                        attr {
                            height(60f)
                            backgroundColor(Color(0xFF222222L))
                            flexDirectionRow()
                            allCenter()
                        }
                        event {
                            click {
                                ctx.textAreaRef.view?.cursorIndex { idx ->
                                    KLog.i("TextAreaDemoPage", "cursorIndex callback: $idx")
                                    ctx.lastCursorIndex = idx
                                }
                            }
                        }
                        Text {
                            attr {
                                text("Get cursor")
                                fontSize(16f)
                                color(Color.WHITE)
                                marginRight(16f)
                            }
                        }
                        Text {
                            attr {
                                text(
                                    "cursor=${if (ctx.lastCursorIndex < 0) "?" else ctx.lastCursorIndex.toString()}"
                                )
                                fontSize(16f)
                                color(Color.YELLOW)
                            }
                        }
                    }

                    View {
                        attr {
                            height(60f)
                            backgroundColor(Color(0xFF444444L))
                            allCenter()
                        }
                        event {
                            click {
                                ctx.textAreaRef.view?.setCursorIndex(5)
                                KLog.i("TextAreaDemoPage", "setCursorIndex(5) invoked")
                            }
                        }
                        Text {
                            attr {
                                text("Set cursor -> 5")
                                fontSize(16f)
                                color(Color.WHITE)
                            }
                        }
                    }
                    // --- end cursorIndex / setCursorIndex demo panel -----------------------

                    View {
                        attr {
                            height(200f)
                            backgroundColor(Color.BLACK)
                            allCenter()
                        }
                        event {
                            click { ctx.textAreaRef.view?.blur() }
                        }
                    }

                    View {
                        attr {
                            height(200f)
                            backgroundColor(Color.GREEN)
                        }
                        event {
                            click { ctx.textAreaRef.view?.blur() }
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
        // Mirror InputViewDemoPage: after 5s, clear the TextArea and drop focus so
        // the keyboard-avoidance animation can be observed closing down.
        setTimeout(pagerId, 5000) {
            val view = textAreaRef.view ?: return@setTimeout
            @Suppress("DEPRECATION")
            view.setText("")
            view.blur()
        }
    }

    /**
     * Create keyboard animation based on platform.
     * iOS: Use native keyboard animation curve for perfect sync
     * Other platforms: Use easeInOut animation as fallback
     */
    private fun createKeyboardAnimation(params: KeyboardParams): Animation {
        return if (PlatformUtils.isIOS()) {
            Animation.keyboard(params.duration, params.curve)
        } else {
            Animation.easeInOut(params.duration)
        }
    }
}
