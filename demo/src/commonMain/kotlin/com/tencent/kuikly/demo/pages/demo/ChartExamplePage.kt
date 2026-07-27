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
import com.tencent.kuikly.core.views.chart.ChartInsets
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
                        margin(top = 12f, left = 16f, right = 16f)
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

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("固定范围与自定义格式")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Text {
                    attr {
                        margin(top = 6f, left = 20f, right = 20f)
                        text("固定 0%～100% 范围，分别格式化坐标、数值标签和 Tooltip")
                        fontSize(12f)
                        color(Color(0xFF6B7785L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f)
                        size(pagerData.pageViewWidth - 32f, 260f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.BOTTOM)
                        yRange(0f, 1f)
                        yTickCount(6)
                        yLabelFormatter { value -> "${(value * 100f).toInt()}%" }
                        valueLabelFormatter { value -> "${(value * 100f).toInt()}%" }
                        tooltipValueFormatter { value -> "${(value * 100f).toInt()} percent" }
                        data {
                            categories("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug")
                            line("Conversion", Color(0xFF0052D9L)) {
                                values(0.42f, 0.48f, 0.55f, 0.61f, 0.58f, 0.66f, 0.72f, 0.78f)
                                lineWidth = 2.5f
                                showValues = true
                            }
                            line("Target", Color(0xFF00A870L)) {
                                values(0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f)
                                showPoints = false
                            }
                        }
                    }
                }

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("多行图例与自动布局")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Text {
                    attr {
                        margin(top = 6f, left = 20f, right = 20f)
                        text("长图例会自动换行，并同步扩大绘图区边距")
                        fontSize(12f)
                        color(Color(0xFF6B7785L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f)
                        size(pagerData.pageViewWidth - 32f, 290f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.TOP)
                        legendFontSize(11f)
                        yTickCount(4)
                        data {
                            categories("Q1", "Q2", "Q3", "Q4")
                            line("North America Enterprise", Color(0xFF0052D9L)) {
                                values(32f, 46f, 58f, 74f)
                                showPoints = false
                            }
                            line("Europe Consumer", Color(0xFF00A870L)) {
                                values(28f, 41f, 49f, 62f)
                                showPoints = false
                            }
                            line("Asia Pacific Growth", Color(0xFFED7B2FL)) {
                                values(20f, 35f, 54f, 78f)
                                showPoints = false
                            }
                            line("Latin America Online", Color(0xFFE34D59L)) {
                                values(18f, 27f, 38f, 51f)
                                showPoints = false
                            }
                            line("Middle East Partner", Color(0xFF7B61FFL)) {
                                values(12f, 22f, 34f, 46f)
                                showPoints = false
                            }
                            line("Africa New Business", Color(0xFF00A6A6L)) {
                                values(8f, 16f, 25f, 37f)
                                showPoints = false
                            }
                            line("Oceania Direct Channel", Color(0xFF9C6ADEL)) {
                                values(15f, 24f, 36f, 48f)
                                showPoints = false
                            }
                            line("Global Strategic Accounts", Color(0xFF4C7A34L)) {
                                values(36f, 48f, 63f, 81f)
                                showPoints = false
                            }
                        }
                    }
                }

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("纯分组柱状图与自定义样式")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Text {
                    attr {
                        margin(top = 6f, left = 20f, right = 20f)
                        text("关闭图例、网格、Tooltip 和交互，并调整绘图区边距")
                        fontSize(12f)
                        color(Color(0xFF6B7785L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f)
                        size(pagerData.pageViewWidth - 32f, 245f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(
                            background = Color.WHITE,
                            axis = Color(0xFF86909CL),
                            grid = Color(0xFFE5E6EBL),
                            label = Color(0xFF4E5969L),
                            tooltipBackground = Color(0xE61D2129L)
                        )
                        insets(ChartInsets(left = 58f, top = 24f, right = 24f, bottom = 42f))
                        legend(ChartLegendPosition.NONE)
                        gridLines(false)
                        interactive(false)
                        tooltip(false)
                        yLabelFormatter { value -> "${value.toInt()}ms" }
                        valueLabelFormatter { value -> "${value.toInt()}ms" }
                        data {
                            categories("Home", "Search", "Detail", "Checkout")
                            bar("Android", Color(0xFF0052D9L)) {
                                values(82f, 96f, 118f, 136f)
                                showValues = true
                            }
                            bar("iOS", Color(0xFF7B61FFL)) {
                                values(76f, 91f, 109f, 128f)
                                showValues = true
                            }
                        }
                    }
                }

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("常量、单点与自动范围")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Text {
                    attr {
                        margin(top = 6f, left = 20f, right = 20f)
                        text("常量序列会自动扩展上下界，单点与缺失值也能安全绘制")
                        fontSize(12f)
                        color(Color(0xFF6B7785L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f)
                        size(pagerData.pageViewWidth - 32f, 230f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.BOTTOM)
                        data {
                            categories("A", "B", "C", "D")
                            line("Constant", Color(0xFF00A870L)) {
                                values(5f, 5f, 5f, 5f)
                                showValues = true
                            }
                            line("Single point", Color(0xFFE34D59L)) {
                                values(null, null, 8f, null)
                                showValues = true
                            }
                        }
                    }
                }

                Text {
                    attr {
                        margin(top = 28f, left = 20f, right = 20f)
                        text("空数据状态")
                        fontSize(17f)
                        fontWeightMedium()
                        color(Color(0xFF1D2129L))
                    }
                }
                Chart {
                    attr {
                        margin(top = 12f, left = 16f, right = 16f, bottom = 28f)
                        size(pagerData.pageViewWidth - 32f, 180f)
                        backgroundColor(Color.WHITE)
                        borderRadius(12f)
                        colors(background = Color.WHITE)
                        legend(ChartLegendPosition.NONE)
                        emptyText("暂无可展示数据")
                        data {}
                    }
                }
            }
        }
    }
}
