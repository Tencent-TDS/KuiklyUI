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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Date
import com.tencent.kuikly.core.views.DatePicker
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ScrollPickerExamplePage")
internal class ScrollPickerExamplePage : BasePager() {

    private var date: Date by observable(Date(0, 0, 0))
    private var dateTimestamp: Long by observable(0L)
    private var initialDate: Date by observable(Date(2025, 1, 22))

    override fun body(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                alignItemsCenter()
                backgroundColor(Color(0xFFB0C4DE))
            }
            NavBar {
                attr {
                    width(pagerData.pageViewWidth)
                    title = "ScrollPickerExamplePage"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    width(pagerData.pageViewWidth)
                }
                View {
                    attr {
                        flexDirectionColumn()
                        alignItemsCenter()
                        width(pagerData.pageViewWidth)
                        marginTop(16f)
                        marginBottom(24f)
                    }
                    Text {
                        attr {
                            text("选中日期: ${ctx.date}, 时间戳: ${ctx.dateTimestamp}")
                            fontSize(12f)
                        }
                    }
                    apply(ctx.dateQuickButtons { ctx.initialDate = it })
                    Text {
                        attr {
                            text("初始日期: ${ctx.initialDate}")
                            fontSize(12f)
                            color(Color.GRAY)
                            marginBottom(4f)
                        }
                    }
                    vbind({ ctx.initialDate }) {
                        DatePicker {
                            attr {
                                width(300f)
                                backgroundColor(Color.WHITE)
                                borderRadius(8f)
                                initialDate(ctx.initialDate)
                                initialScrollAnimated = true
                            }
                            event {
                                chooseEvent {
                                    it.date?.let { date ->
                                        ctx.date = date
                                    }
                                    ctx.dateTimestamp = it.timeInMillis
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dateQuickButtons(onSelect: (Date) -> Unit): ViewBuilder {
        return {
            View {
                attr {
                    flexDirectionRow()
                    justifyContentCenter()
                    marginTop(8f)
                    marginBottom(8f)
                }
                Text {
                    attr {
                        text("今天")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2025, 1, 22)) }
                    }
                }
                Text {
                    attr {
                        text("近一月")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2024, 12, 22)) }
                    }
                }
                Text {
                    attr {
                        text("近三月")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2024, 10, 22)) }
                    }
                }
            }
        }
    }
}
