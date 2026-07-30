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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputSpan
import com.tencent.kuikly.core.views.InputSpans
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("4444")
internal class InputSpanPager : BasePager() {

    var spans by observable(InputSpans())

    lateinit var ref: ViewRef<InputView>
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Input {
                ref {
                    ctx.ref = it
                }
                attr {
                    size(pagerData.pageViewWidth, 400f)
                    inputSpans(ctx.spans)
                    backgroundColor(Color.GREEN)
                    color(Color.BLACK)
                }
                event {
                    textDidChange(true) { it ->
                        val hightSpan = {
                            InputSpan().apply {
                                color(Color.RED)
                                text(if (it.text.length <= 10) it.text else it.text.substring(0, 10))
                            }
                        }
                        val normalSpan = {
                            InputSpan().apply {
                                color(Color.BLACK)
                                text(it.text.substring(10, it.text.length))
                                fontSize(18f)
                            }
                        }
                        val spans = InputSpans()
                        if (it.text.length <= 10) {
                            spans.addSpan(hightSpan.invoke())
                            ctx.spans = spans
                        } else {
//                            spans.addSpan(hightSpan.invoke())
//                            spans.addSpan(normalSpan.invoke())
//                            ctx.spans = spans
                        }
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        getCursor()
        setCursorIndex()
    }

    fun getCursor() {
        setTimeout(2 * 1000) {
            ref.view?.cursorIndex {
                KLog.i("CR7", "indec: $it")
                getCursor()
            }
        }
    }

    fun setCursorIndex() {
        setTimeout(7 * 1000) {
            ref.view?.setCursorIndex(3)
            setCursorIndex()
        }
    }
}