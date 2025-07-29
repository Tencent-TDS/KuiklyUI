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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text

@Page("line_break_margin")
internal class LineBreakMarginPager : BasePager() {

    var lineBreak by observable(false)
    var top by observable(0f)
    var left by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            Text {
                attr {
                    fontSize(16f)
                    text("这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本" +
                            "这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本" +
                            "这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普" +
                            "这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普" +
                            "这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普" +
                            "这是一个普通文本这是一个普通文本这是一个普通文本这是一个普通文本这是一个普")
                    color(Color.GRAY)
                    fontStyleItalic()
                    textDecorationLineThrough()
                    lines(2)
                    lineBreakMargin(100f)
                    lineHeight(16f)
                }
                event {
                    onLineBreakMargin {
                        ctx.lineBreak = true
                    }
                    layoutFrameDidChange {
                        ctx.top = it.y + it.height - 16f
                        ctx.left = it.x + it.width - 90f
                    }
                }
            }
            vif({ ctx.lineBreak }) {
                Text {
                    attr {
                        text("更多")
                        fontSize(16f)
                        lineHeight(16f)
                        absolutePosition(top = ctx.top, left = ctx.left)
                    }
                }
            }
        }
    }
}