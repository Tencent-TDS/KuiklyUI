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
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Table
import com.tencent.kuikly.core.views.TableCellAlignment
import com.tencent.kuikly.core.views.TableTheme
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("TableDemoPage")
internal class TableDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        val pageWidth = pagerData.pageViewWidth
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    flexDirection(FlexDirection.COLUMN)
                }
                NavBar {
                    attr {
                        title = "Table Component Demo"
                    }
                }
                Scroller {
                    attr {
                        flex(1f)
                    }
                    View {
                        attr {
                            flexDirection(FlexDirection.COLUMN)
                        }

                        // Demo 1: basic table with default theme.
                        SectionTitle("Basic Table")
                        Table(
                            width = pageWidth,
                            height = 200f
                        ) {
                            column("Name", width = 120f)
                            column("Age", width = 80f, alignment = TableCellAlignment.CENTER)
                            column("City", width = 120f)
                            row("Alice", 25, "Beijing")
                            row("Bob", 30, "Shanghai")
                            row("Charlie", 28, "Shenzhen")
                            row("David", 35, "Hangzhou")
                        }

                        // Demo 2: themed table with custom alignment.
                        SectionTitle("Themed & Aligned Table")
                        Table(
                            width = pageWidth,
                            height = 200f,
                            theme = TableTheme(
                                headerBackgroundColor = Color(0xFF1976D2),
                                headerTextColor = Color.WHITE,
                                headerFontWeight = FontWeight.SEMIBOLD,
                                cellBackgroundColor = Color(0xFFF5F5F5),
                                borderColor = Color(0xFF1976D2),
                                borderWidth = 1.5f,
                                cellPadding = 12f
                            )
                        ) {
                            column("Product", width = 140f, alignment = TableCellAlignment.START)
                            column("Price", width = 90f, alignment = TableCellAlignment.END)
                            column("Stock", width = 90f, alignment = TableCellAlignment.CENTER)
                            row("iPhone", 999, 12)
                            row("MacBook", 1999, 5)
                            row("AirPods", 199, 48)
                        }

                        // Demo 3: custom cell rendering and bidirectional scrolling.
                        SectionTitle("Custom Cells & Scrolling")
                        Table(
                            width = pageWidth,
                            height = 240f
                        ) {
                            column("ID", width = 60f)
                            column("Task", width = 160f, alignment = TableCellAlignment.START)
                            column("Owner", width = 90f)
                            column("Priority", width = 90f) { _, rowIndex, columnIndex ->
                                val priority = when (rowIndex % 3) {
                                    0 -> "High" to Color(0xFFE53935)
                                    1 -> "Medium" to Color(0xFFFB8C00)
                                    else -> "Low" to Color(0xFF43A047)
                                }
                                View {
                                    attr {
                                        size(60f, 24f)
                                        backgroundColor(priority.second)
                                        borderRadius(12f)
                                        justifyContent(FlexJustifyContent.CENTER)
                                        alignItems(FlexAlign.CENTER)
                                    }
                                    Text {
                                        attr {
                                            text(priority.first)
                                            color(Color.WHITE)
                                            fontSize(12f)
                                        }
                                    }
                                }
                            }
                            column("Progress", width = 100f) { value, _, _ ->
                                val percent = (value as? Int) ?: 0
                                View {
                                    attr {
                                        size(80f, 8f)
                                        backgroundColor(Color(0xFFE0E0E0))
                                        borderRadius(4f)
                                        justifyContent(FlexJustifyContent.FLEX_START)
                                        alignItems(FlexAlign.CENTER)
                                    }
                                    View {
                                        attr {
                                            width(80f * percent / 100f)
                                            height(8f)
                                            backgroundColor(Color(0xFF1976D2))
                                            borderRadius(4f)
                                        }
                                    }
                                }
                            }

                            repeat(20) { index ->
                                row(
                                    index + 1,
                                    "Task ${index + 1} description",
                                    "User ${(index % 5) + 1}",
                                    index,
                                    (index + 1) * 5
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

private fun ViewContainer<*, *>.SectionTitle(title: String) {
    View {
        attr {
            width(pagerData.pageViewWidth)
            height(40f)
            padding(12f, 16f, 8f, 16f)
        }
        Text {
            attr {
                text(title)
                color(Color(0xFF333333))
                fontSize(16f)
                fontWeightBold()
            }
        }
    }
}
