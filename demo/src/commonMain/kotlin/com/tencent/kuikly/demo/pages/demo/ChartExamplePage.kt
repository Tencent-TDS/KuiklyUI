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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.chart.Chart
import com.tencent.kuikly.core.views.chart.ChartLegendPosition
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ChartExamplePage")
internal class ChartExamplePage : BasePager() {
    private var selectionSummary by observable("点击或滑动图表查看数据")

    override fun body(): ViewBuilder {
        val page = this
        return {
            attr {
                backgroundColor(Color(0xFFF5F7FAL))
            }
            NavBar {
                attr { title = "Chart 图表组件" }
            }
            List {
                attr { flex(1f) }

                Text {
                    attr {
                        margin(top = 20f, left = 20f, right = 20f)
                        text("混合折线图与分组柱状图")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Text {
                    attr {
                        margin(top = 6f, left = 20f, right = 20f)
                        text(page.selectionSummary)
                        fontSize(12f)
                        color(Color(0xFF6B7785L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f)
                        size(pagerData.pageViewWidth - 32f, 270f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.TOP)
                        data {
                            categories("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            bar("Orders", Color(0xFF00A870L)) {
                                values(120f, 168f, 142f, 210f, 232f, 198f, 256f)
                                showValues = true
                            }
                            bar("Returns", Color(0xFFED7B2FL)) {
                                values(18f, 21f, 15f, 27f, 24f, 20f, 30f)
                            }
                            line("Revenue (K)", Color(0xFF0052D9L)) {
                                values(92f, 126f, 118f, 168f, 182f, 160f, 205f)
                                lineWidth = 2.5f
                            }
                        }
                    }
                    event {
                        selectionChanged { selection ->
                            val values = selection.values.joinToString { item ->
                                "${item.seriesName}=${item.value ?: "--"}"
                            }
                            page.selectionSummary = "${selection.category}: $values"
                        }
                        selectionCleared {
                            page.selectionSummary = "点击或滑动图表查看数据"
                        }
                    }
                }

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("面积图、负数与缺失数据")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f, bottom = 28f)
                        size(pagerData.pageViewWidth - 32f, 250f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.BOTTOM)
                        areaOpacity(0.22f)
                        data {
                            categories("Jan", "Feb", "Mar", "Apr", "May", "Jun")
                            area("Net change", Color(0xFF7B61FFL)) {
                                values(-12f, 8f, 24f, null, 18f, 36f)
                                lineWidth = 2.5f
                            }
                            line("Baseline", Color(0xFFE34D59L)) {
                                values(0f, 0f, 0f, 0f, 0f, 0f)
                                showPoints = false
                            }
                        }
                    }
                }
            }
        }
    }
}
