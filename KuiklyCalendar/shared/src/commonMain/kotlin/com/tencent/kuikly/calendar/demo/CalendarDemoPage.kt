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

package com.tencent.kuikly.calendar.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.calendar.*

/**
 * 日历组件演示页面。
 *
 * 展示 KuiklyUI 日历组件的核心功能：
 * 1. 基础月视图 — 单日期选择
 * 2. 带事件标记的月视图 — 多选模式 + 事件标记
 * 3. 周视图 — 单周日期展示
 * 4. 年视图 — 全年月份概览
 * 5. 国际化切换 — 中文/英文/日文/韩文
 */
@Page("CalendarDemoPage")
class CalendarDemoPage : Pager() {

    /** 选中结果描述文本 */
    private var selectedInfo: String by observable("请选择日期")

    /** 当前国际化标签 */
    private var currentLocale: String by observable("zh-CN")

    override fun body(): ViewBuilder {
        val page = this
        return {
            attr {
                flexDirectionColumn()
                backgroundColor(Color.WHITE)
                flex(1f)
            }

            // 顶部导航栏
            View {
                attr {
                    flexDirectionRow()
                    justifyContentCenter()
                    alignItemsCenter()
                    height(44f + page.pageData.statusBarHeight)
                    paddingTop(page.pageData.statusBarHeight)
                    backgroundColor(Color.WHITE)
                }
                Text {
                    attr {
                        text("日历组件 Demo")
                        fontSize(17f)
                        fontWeightSemiBold()
                        color(Color(0xFF333333))
                    }
                }
            }

            // 内容区域（可滚动）
            Scroller {
                attr { flex(1f) }

                // === Section 1: 基础月视图 — 单日期选择 ===
                page.buildSectionBasicMonthView(this)

                // 分隔线
                page.buildDivider(this)

                // === Section 2: 事件标记 + 多日期选择 ===
                page.buildSectionEventMarksMultiSelect(this)

                // 分隔线
                page.buildDivider(this)

                // === Section 3: 周视图 ===
                page.buildSectionWeekView(this)

                // 分隔线
                page.buildDivider(this)

                // === Section 4: 年视图 ===
                page.buildSectionYearView(this)

                // 分隔线
                page.buildDivider(this)

                // === Section 5: 国际化切换 ===
                page.buildSectionLocaleSwitch(this)
            }
        }
    }

    /**
     * Section 1: 基础月视图，演示单日期选择。
     */
    private fun buildSectionBasicMonthView(container: ViewContainer<*, *>) {
        val page = this
        container.View {
            attr { flexDirectionColumn(); padding(15f) }

            Text {
                attr {
                    text("基础月视图 — 单日期选择")
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginBottom(8f)
                }
            }

            Calendar {
                attr {
                    viewMode = CalendarViewMode.MONTH
                    selectionMode = CalendarSelectionMode.SINGLE
                    locale = CalendarLocale.of(page.currentLocale)
                }
                event {
                    dateSelectedEvent { selection ->
                        page.selectedInfo = "选中: ${selection.dates.firstOrNull()}"
                    }
                    monthChangedEvent { _, _ -> }
                }
            }

            // 选中结果显示
            View {
                attr {
                    marginTop(8f)
                    padding(12f)
                    backgroundColor(Color(0xFFF5F5F5))
                    borderRadius(8f)
                }
                Text {
                    attr {
                        text(page.selectedInfo)
                        fontSize(13f)
                        color(Color(0xFF666666))
                    }
                }
            }
        }
    }

    /**
     * Section 2: 带事件标记的月视图，演示多日期选择与事件标记。
     */
    private fun buildSectionEventMarksMultiSelect(container: ViewContainer<*, *>) {
        val page = this
        container.View {
            attr { flexDirectionColumn(); padding(15f) }

            Text {
                attr {
                    text("事件标记 + 多日期选择")
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginBottom(8f)
                }
            }

            // 生成示例事件标记
            val today = CalendarUtils.today()
            val marks = listOf(
                CalendarEventMark(CalendarDate(today.first, today.second, 5), Color(0xFFFF6B6B), "会议"),
                CalendarEventMark(CalendarDate(today.first, today.second, 10), Color(0xFF4A90D9), "生日"),
                CalendarEventMark(CalendarDate(today.first, today.second, 15), Color(0xFF2ECC71), "旅行"),
                CalendarEventMark(CalendarDate(today.first, today.second, 20), Color(0xFFF39C12), "截止日"),
                CalendarEventMark(CalendarDate(today.first, today.second, 25), Color(0xFF9B59B6), "活动"),
            )

            Calendar {
                attr {
                    selectionMode = CalendarSelectionMode.MULTI
                    eventMarks = marks
                    locale = CalendarLocale.of(page.currentLocale)
                }
                event {
                    dateSelectedEvent { selection ->
                        page.selectedInfo = "已选 ${selection.dates.size} 个日期"
                    }
                }
            }
        }
    }

    /**
     * Section 3: 周视图演示。
     */
    private fun buildSectionWeekView(container: ViewContainer<*, *>) {
        val page = this
        container.View {
            attr { flexDirectionColumn(); padding(15f) }

            Text {
                attr {
                    text("周视图")
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginBottom(8f)
                }
            }

            WeekCalendar {
                attr {
                    locale = CalendarLocale.of(page.currentLocale)
                }
                event {
                    onDateClick = { cell ->
                        if (cell.day > 0) {
                            page.selectedInfo = "周视图选中: ${cell.date}"
                        }
                    }
                }
            }
        }
    }

    /**
     * Section 4: 年视图演示。
     */
    private fun buildSectionYearView(container: ViewContainer<*, *>) {
        val page = this
        container.View {
            attr { flexDirectionColumn(); padding(15f) }

            Text {
                attr {
                    text("年视图")
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginBottom(8f)
                }
            }

            YearCalendar {
                attr {
                    locale = CalendarLocale.of(page.currentLocale)
                }
                event {
                    onMonthSelected = { year, month ->
                        page.selectedInfo = "跳转到: ${year}年${month}月"
                    }
                }
            }
        }
    }

    /**
     * Section 5: 国际化切换演示。
     */
    private fun buildSectionLocaleSwitch(container: ViewContainer<*, *>) {
        val page = this
        container.View {
            attr { flexDirectionColumn(); padding(15f); marginBottom(30f) }

            Text {
                attr {
                    text("国际化演示")
                    fontSize(14f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginBottom(8f)
                }
            }

            // 语言切换按钮行
            View {
                attr { flexDirectionRow(); justifyContentSpaceAround() }

                page.buildLocaleButton(this, "zh-CN", "中文")
                page.buildLocaleButton(this, "en-US", "English")
                page.buildLocaleButton(this, "ja-JP", "日本語")
                page.buildLocaleButton(this, "ko-KR", "한국어")
            }

            // 国际化日历
            Calendar {
                attr {
                    locale = CalendarLocale.of(page.currentLocale)
                }
                event {
                    dateSelectedEvent { selection ->
                        page.selectedInfo = "国际化选中: ${selection.dates.firstOrNull()}"
                    }
                }
            }
        }
    }

    /**
     * 构建单个语言切换按钮。
     */
    private fun buildLocaleButton(
        container: ViewContainer<*, *>,
        tag: String,
        label: String
    ) {
        val page = this
        container.View {
            attr {
                padding(8f, 4f, 8f, 4f)
                borderRadius(4f)
                backgroundColor(
                    if (page.currentLocale == tag) Color(0xFF4A90D9) else Color(0xFFEEEEEE)
                )
            }
            event { click { page.currentLocale = tag } }
            Text {
                attr {
                    text(label)
                    fontSize(13f)
                    color(
                        if (page.currentLocale == tag) Color.WHITE else Color(0xFF333333)
                    )
                }
            }
        }
    }

    /**
     * 构建灰色分隔线。
     */
    private fun buildDivider(container: ViewContainer<*, *>) {
        container.View {
            attr { height(8f); backgroundColor(Color(0xFFF5F5F5)) }
        }
    }
}
